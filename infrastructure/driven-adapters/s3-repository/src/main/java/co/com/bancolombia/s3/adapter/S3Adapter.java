package co.com.bancolombia.s3.adapter;

import co.com.bancolombia.model.documento.Documento;
import co.com.bancolombia.model.documento.DocumentoResultado;
import co.com.bancolombia.model.documento.gateways.AlmacenamientoGateway;
import co.com.bancolombia.s3.config.model.S3ConnectionProperties;
import co.com.bancolombia.s3.operations.S3Operations;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class S3Adapter implements AlmacenamientoGateway {

    private final S3Operations s3Operations;
    private final S3ConnectionProperties properties;

    @Override
    public Flux<DocumentoResultado> almacenar(Flux<Documento> documentos) {
        String bucketName = properties.bucketName();
        return documentos
                .publishOn(Schedulers.boundedElastic())
                .flatMap(documento -> guardarUno(documento, bucketName), 200);
    }

    private Mono<DocumentoResultado> guardarUno(Documento documento, String bucketName) {
        return Mono.fromCallable(() -> {
            try {
                String extension = obtenerExtension(documento.getNombreOriginal());
                String prefix = (properties.keyPrefix() != null && !properties.keyPrefix().isBlank())
                        ? properties.keyPrefix() : "usuarios";
                String usuario = sanitizar(documento.getUsuarioId());
                String categoria = sanitizar(documento.getCategoria());
                if (usuario == null || categoria == null) {
                    return DocumentoResultado.builder()
                            .nombreOriginal(documento.getNombreOriginal())
                            .exitoso(false)
                            .mensajeError("usuarioId/categoria inválidos")
                            .build();
                }
                String objectKey = String.join("/", prefix, usuario, categoria,
                        UUID.randomUUID() + extension);
                byte[] contenido = documento.getContenido() != null ? documento.getContenido() : new byte[0];
                boolean exitoso = s3Operations.uploadObject(bucketName, objectKey, contenido);
                if (exitoso) {
                    return DocumentoResultado.builder()
                            .nombreOriginal(documento.getNombreOriginal())
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

    private String obtenerExtension(String nombreOriginal) {
        if (nombreOriginal == null || !nombreOriginal.contains(".")) {
            return "";
        }
        return nombreOriginal.substring(nombreOriginal.lastIndexOf("."));
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
