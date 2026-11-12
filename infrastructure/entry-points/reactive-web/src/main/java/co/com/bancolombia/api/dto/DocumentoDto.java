package co.com.bancolombia.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoDto {
    private String nombreArchivo;
    private String nombreOriginal;
    private String contentType;
    private long tamanio;
    private String rutaAlmacenada;
    private LocalDateTime fechaCarga;
    private String urlDescarga;
}
