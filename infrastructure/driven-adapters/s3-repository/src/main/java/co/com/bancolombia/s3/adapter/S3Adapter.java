package co.com.bancolombia.s3.adapter;

import co.com.bancolombia.model.documento.Documento;
import co.com.bancolombia.model.documento.DocumentoResultado;
import co.com.bancolombia.model.documento.gateways.AlmacenamientoRepository;
import co.com.bancolombia.s3.config.model.S3ConnectionProperties;
import co.com.bancolombia.s3.operations.S3Operations;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.net.URLConnection;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class S3Adapter implements AlmacenamientoRepository {
    private static final Pattern NOMBRE_ALMACENADO_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}-(.+)$");

    private final S3Operations s3Operations;
    private final S3ConnectionProperties properties;

    @Override
    public Flux<DocumentoResultado> almacenar(Flux<Documento> documentos) {
        String bucketName = properties.bucketName();
        return documentos
                .publishOn(Schedulers.boundedElastic())
                .flatMap(documento -> guardarUno(documento, bucketName), 200);
    }

    @Override
    public Mono<Documento> obtener(String usuarioId, String categoria, String nombreArchivo) {
        return Mono.fromCallable(() -> {
            validarSegmento(usuarioId, "usuarioId");
            validarSegmento(categoria, "categoria");
            validarSegmento(nombreArchivo, "nombreArchivo");

            String bucketName = properties.bucketName();
            String objectKey = construirPrefijo(usuarioId, categoria) + nombreArchivo;
            try {
                ResponseBytes<GetObjectResponse> response = s3Operations.getObjectBytes(bucketName, objectKey);
                return toDocumento(usuarioId, categoria, objectKey, bucketName, response);
            } catch (S3Exception e) {
                if (esNoEncontrado(e)) {
                    return null;
                }
                throw e;
            }
        })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(Mono::justOrEmpty);
    }

    @Override
    public Flux<Documento> listar(String usuarioId, String categoria) {
        return Mono.fromCallable(() -> {
            validarSegmento(usuarioId, "usuarioId");
            validarSegmento(categoria, "categoria");

            String bucketName = properties.bucketName();
            String prefix = construirPrefijo(usuarioId, categoria);
            List<S3Object> objetos = s3Operations.listObjects(bucketName, prefix);
            return objetos.stream()
                    .filter(objeto -> objeto.key() != null
                            && !objeto.key().equals(prefix)
                            && !objeto.key().endsWith("/"))
                    .map(objeto -> toDocumento(usuarioId, categoria, bucketName, objeto))
                    .filter(documento -> documento != null)
                    .sorted(Comparator.comparing(Documento::getFechaCarga,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }

    private Mono<DocumentoResultado> guardarUno(Documento documento, String bucketName) {
        return Mono.fromCallable(() -> {
            try {
                String usuario = sanitizar(documento.getUsuarioId());
                String categoria = sanitizar(documento.getCategoria());
                if (usuario == null || categoria == null) {
                    return DocumentoResultado.builder()
                            .nombreOriginal(documento.getNombreOriginal())
                            .exitoso(false)
                            .mensajeError("usuarioId/categoria inválidos")
                            .build();
                }
                String nombreAlmacenado = generarNombreAlmacenado(documento.getNombreOriginal());
                String objectKey = construirPrefijo(usuario, categoria) + nombreAlmacenado;
                byte[] contenido = documento.getContenido() != null ? documento.getContenido() : new byte[0];
                boolean exitoso = s3Operations.uploadObject(bucketName, objectKey, contenido, documento.getContentType());
                if (exitoso) {
                    return DocumentoResultado.builder()
                            .nombreOriginal(documento.getNombreOriginal())
                            .nombreAlmacenado(nombreAlmacenado)
                            .exitoso(true)
                            .rutaAlmacenada("s3://" + bucketName + "/" + objectKey)
                            .build();
                }
                return DocumentoResultado.builder()
                        .nombreOriginal(documento.getNombreOriginal())
                        .exitoso(false)
                        .mensajeError("Error al subir objeto a S3")
                        .build();
            } catch (Exception e) {
                return DocumentoResultado.builder()
                        .nombreOriginal(documento.getNombreOriginal())
                        .exitoso(false)
                        .mensajeError(e.getMessage())
                        .build();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Documento toDocumento(String usuarioId, String categoria, String objectKey,
                                  String bucketName, ResponseBytes<GetObjectResponse> response) {
        byte[] contenido = response.asByteArray();
        GetObjectResponse metadata = response.response();
        String nombreAlmacenado = nombreDeKey(objectKey);
        String nombreOriginal = nombreOriginalDesde(nombreAlmacenado);
        String contentType = (metadata.contentType() != null && !metadata.contentType().isBlank())
                ? metadata.contentType()
                : URLConnection.guessContentTypeFromName(nombreAlmacenado);
        return Documento.builder()
                .nombreOriginal(nombreOriginal)
                .usuarioId(usuarioId)
                .categoria(categoria)
                .contentType(contentType)
                .tamanio(contenido.length)
                .contenido(contenido)
                .nombreAlmacenado(nombreAlmacenado)
                .rutaAlmacenada("s3://" + bucketName + "/" + objectKey)
                .fechaCarga(metadata.lastModified() != null
                        ? LocalDateTime.ofInstant(metadata.lastModified(), ZoneId.systemDefault()) : null)
                .build();
    }

    private Documento toDocumento(String usuarioId, String categoria, String bucketName, S3Object objeto) {
        try {
            String objectKey = objeto.key();
            String nombreArchivo = objectKey.substring(objectKey.lastIndexOf('/') + 1);
            return Documento.builder()
                    .nombreOriginal(nombreOriginalDesde(nombreArchivo))
                    .usuarioId(usuarioId)
                    .categoria(categoria)
                    .contentType(URLConnection.guessContentTypeFromName(nombreArchivo))
                    .tamanio(objeto.size() != null ? objeto.size() : 0L)
                    .nombreAlmacenado(nombreArchivo)
                    .rutaAlmacenada("s3://" + bucketName + "/" + objectKey)
                    .fechaCarga(objeto.lastModified() != null
                            ? LocalDateTime.ofInstant(objeto.lastModified(), ZoneId.systemDefault()) : null)
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

    private String construirPrefijo(String usuarioId, String categoria) {
        String usuario = sanitizar(usuarioId);
        String categoriaSanitizada = sanitizar(categoria);
        if (usuario == null || categoriaSanitizada == null) {
            throw new IllegalArgumentException("usuarioId/categoria invalidos");
        }
        String prefix = (properties.keyPrefix() != null && !properties.keyPrefix().isBlank())
                ? properties.keyPrefix() : "usuarios";
        return String.join("/", prefix, usuario, categoriaSanitizada) + "/";
    }

    private String nombreDeKey(String key) {
        return key.substring(key.lastIndexOf('/') + 1);
    }

    private boolean esNoEncontrado(S3Exception e) {
        if (e.statusCode() == 404) {
            return true;
        }
        return e.awsErrorDetails() != null && "NoSuchKey".equals(e.awsErrorDetails().errorCode());
    }

    private void validarSegmento(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo '" + campo + "' es obligatorio");
        }
        if (valor.contains("/") || valor.contains("\\") || valor.contains("..")) {
            throw new IllegalArgumentException("El campo '" + campo + "' contiene caracteres invalidos");
        }
    }

    private String generarNombreAlmacenado(String nombreOriginal) {
        String nombreSanitizado = sanitizarNombreOriginal(nombreOriginal);
        String prefijo = UUID.randomUUID().toString();
        return nombreSanitizado == null ? prefijo : prefijo + "-" + nombreSanitizado;
    }

    private String sanitizarNombreOriginal(String nombreOriginal) {
        if (nombreOriginal == null || nombreOriginal.isBlank()) {
            return null;
        }
        String nombre = nombreOriginal.trim();
        int separador = Math.max(nombre.lastIndexOf('/'), nombre.lastIndexOf('\\'));
        if (separador >= 0) {
            nombre = nombre.substring(separador + 1);
        }
        nombre = nombre.replaceAll("[^A-Za-z0-9._-]", "_");
        return nombre.isBlank() || nombre.equals(".") || nombre.equals("..") ? null : nombre;
    }

    private String nombreOriginalDesde(String nombreAlmacenado) {
        Matcher matcher = NOMBRE_ALMACENADO_PATTERN.matcher(nombreAlmacenado);
        return matcher.matches() ? matcher.group(1) : nombreAlmacenado;
    }

    private String sanitizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String v = valor.trim();
        if (v.contains("/") || v.contains("\\") || v.contains("..")) {
            return null;
        }
        return v.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
