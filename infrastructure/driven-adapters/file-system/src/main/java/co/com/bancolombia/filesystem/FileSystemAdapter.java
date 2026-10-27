package co.com.bancolombia.filesystem;

import co.com.bancolombia.filesystem.config.StorageProperties;
import co.com.bancolombia.model.documento.Documento;
import co.com.bancolombia.model.documento.DocumentoResultado;
import co.com.bancolombia.model.documento.gateways.AlmacenamientoGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FileSystemAdapter implements AlmacenamientoGateway {
    private final StorageProperties storageProperties;

    @Override
    public List<DocumentoResultado> almacenar(List<Documento> documentos) {
        List<DocumentoResultado> resultados = new ArrayList<>();
        try {
            Path basePath = storageProperties.getBasePathAsPath();
            Files.createDirectories(basePath);

            for (Documento documento : documentos) {
                try {
                    String extension = obtenerExtension(documento.getNombreOriginal());
                    String nombreAlmacenado = UUID.randomUUID().toString() + extension;
                    Path archivoDestino = basePath.resolve(nombreAlmacenado);

                    if (documento.getContenido() != null) {
                        Files.write(archivoDestino, documento.getContenido(),
                                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    } else {
                        Files.createFile(archivoDestino);
                    }

                    resultados.add(DocumentoResultado.builder()
                            .nombreOriginal(documento.getNombreOriginal())
                            .exitoso(true)
                            .rutaAlmacenada(archivoDestino.toAbsolutePath().toString())
                            .build());
                } catch (Exception e) {
                    resultados.add(DocumentoResultado.builder()
                            .nombreOriginal(documento.getNombreOriginal())
                            .exitoso(false)
                            .mensajeError(e.getMessage())
                            .build());
                }
            }
        } catch (IOException e) {
            for (Documento documento : documentos) {
                resultados.add(DocumentoResultado.builder()
                        .nombreOriginal(documento.getNombreOriginal())
                        .exitoso(false)
                        .mensajeError("Error creando directorio de almacenamiento: " + e.getMessage())
                        .build());
            }
        }
        return resultados;
    }

    private String obtenerExtension(String nombreOriginal) {
        if (nombreOriginal == null || !nombreOriginal.contains(".")) {
            return "";
        }
        return nombreOriginal.substring(nombreOriginal.lastIndexOf("."));
    }
}
