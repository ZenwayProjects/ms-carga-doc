package co.com.bancolombia.filesystem;

import co.com.bancolombia.filesystem.config.StorageProperties;
import co.com.bancolombia.model.documento.Documento;
import co.com.bancolombia.model.documento.DocumentoResultado;
import co.com.bancolombia.model.documento.gateways.AlmacenamientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "fs", matchIfMissing = true)
public class FileSystemAdapter implements AlmacenamientoRepository {
    private static final Pattern NOMBRE_ALMACENADO_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}-(.+)$");

    private final StorageProperties storageProperties;

    @Override
    public Flux<DocumentoResultado> almacenar(Flux<Documento> documentos) {
        return documentos
                .publishOn(Schedulers.boundedElastic())
                .flatMap(this::guardarUno);
    }

    @Override
    public Mono<Documento> obtener(String usuarioId, String categoria, String nombreArchivo) {
        return Mono.fromCallable(() -> {
            Path archivo = resolverArchivo(usuarioId, categoria, nombreArchivo);
            if (!Files.isRegularFile(archivo)) {
                return null;
            }
            return toDocumento(usuarioId, categoria, archivo, Files.readAllBytes(archivo));
        })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(Mono::justOrEmpty);
    }

    @Override
    public Flux<Documento> listar(String usuarioId, String categoria) {
        return Mono.fromCallable(() -> {
            validarSegmento(usuarioId, "usuarioId");
            validarSegmento(categoria, "categoria");

            Path directorio = resolverDirectorio(usuarioId, categoria);
            if (!Files.isDirectory(directorio)) {
                return List.<Documento>of();
            }
            try (Stream<Path> archivos = Files.list(directorio)) {
                return archivos
                        .filter(Files::isRegularFile)
                        .map(archivo -> toDocumento(usuarioId, categoria, archivo, null))
                        .filter(documento -> documento != null)
                        .sorted(Comparator.comparing(Documento::getFechaCarga,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                        .toList();
            }
        })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }

    private Mono<DocumentoResultado> guardarUno(Documento documento) {
        return Mono.fromCallable(() -> {
            try {
                validarSegmento(documento.getUsuarioId(), "usuarioId");
                validarSegmento(documento.getCategoria(), "categoria");

                Path directorio = resolverDirectorio(documento.getUsuarioId(), documento.getCategoria());
                Files.createDirectories(directorio);

                String nombreAlmacenado = generarNombreAlmacenado(documento.getNombreOriginal());
                Path archivoDestino = directorio.resolve(nombreAlmacenado).normalize();
                validarDentroDeBase(archivoDestino);

                if (documento.getContenido() != null) {
                    Files.write(archivoDestino, documento.getContenido(),
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                } else {
                    Files.createFile(archivoDestino);
                }

                return DocumentoResultado.builder()
                        .nombreOriginal(documento.getNombreOriginal())
                        .nombreAlmacenado(nombreAlmacenado)
                        .exitoso(true)
                        .rutaAlmacenada(archivoDestino.toAbsolutePath().toString())
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

    private Documento toDocumento(String usuarioId, String categoria, Path archivo, byte[] contenido) {
        try {
            BasicFileAttributes atributos = Files.readAttributes(archivo, BasicFileAttributes.class);
            String nombreAlmacenado = archivo.getFileName().toString();
            String contentType = Files.probeContentType(archivo);
            if (contentType == null) {
                contentType = URLConnection.guessContentTypeFromName(nombreAlmacenado);
            }
            return Documento.builder()
                    .nombreOriginal(nombreOriginalDesde(nombreAlmacenado))
                    .usuarioId(usuarioId)
                    .categoria(categoria)
                    .contentType(contentType)
                    .tamanio(atributos.size())
                    .contenido(contenido)
                    .nombreAlmacenado(nombreAlmacenado)
                    .rutaAlmacenada(archivo.toAbsolutePath().toString())
                    .fechaCarga(LocalDateTime.ofInstant(atributos.lastModifiedTime().toInstant(), ZoneId.systemDefault()))
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

    private Path resolverArchivo(String usuarioId, String categoria, String nombreArchivo) {
        validarSegmento(usuarioId, "usuarioId");
        validarSegmento(categoria, "categoria");
        validarSegmento(nombreArchivo, "nombreArchivo");

        Path archivo = resolverDirectorio(usuarioId, categoria).resolve(nombreArchivo).normalize();
        validarDentroDeBase(archivo);
        return archivo;
    }

    private Path resolverDirectorio(String usuarioId, String categoria) {
        Path basePath = storageProperties.getBasePathAsPath().toAbsolutePath().normalize();
        Path directorio = basePath.resolve(usuarioId).resolve(categoria).normalize();
        if (!directorio.startsWith(basePath)) {
            throw new IllegalArgumentException("Ruta de almacenamiento invalida");
        }
        return directorio;
    }

    private void validarDentroDeBase(Path path) {
        Path basePath = storageProperties.getBasePathAsPath().toAbsolutePath().normalize();
        if (!path.startsWith(basePath)) {
            throw new IllegalArgumentException("Ruta de almacenamiento invalida");
        }
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
}
