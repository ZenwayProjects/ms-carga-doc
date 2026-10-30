package co.com.bancolombia.filesystem;

import co.com.bancolombia.filesystem.config.StorageProperties;
import co.com.bancolombia.model.documento.Documento;
import co.com.bancolombia.model.documento.DocumentoResultado;
import co.com.bancolombia.model.documento.gateways.AlmacenamientoGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FileSystemAdapter implements AlmacenamientoGateway {
    private final StorageProperties storageProperties;

    @Override
    public Flux<DocumentoResultado> almacenar(Flux<Documento> documentos) {
        return documentos
                .publishOn(Schedulers.boundedElastic())
                .flatMap(this::guardarUno);
    }

    private Mono<DocumentoResultado> guardarUno(Documento documento) {
        return Mono.fromCallable(() -> {
            try {
                Path basePath = storageProperties.getBasePathAsPath();
                Files.createDirectories(basePath);

                String extension = obtenerExtension(documento.getNombreOriginal());
                String nombreAlmacenado = UUID.randomUUID().toString() + extension;
                Path archivoDestino = basePath.resolve(nombreAlmacenado);

                if (documento.getContenido() != null) {
                    Files.write(archivoDestino, documento.getContenido(),
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                } else {
                    Files.createFile(archivoDestino);
                }

                return DocumentoResultado.builder()
                        .nombreOriginal(documento.getNombreOriginal())
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

    private String obtenerExtension(String nombreOriginal) {
        if (nombreOriginal == null || !nombreOriginal.contains(".")) {
            return "";
        }
        return nombreOriginal.substring(nombreOriginal.lastIndexOf("."));
    }
}
