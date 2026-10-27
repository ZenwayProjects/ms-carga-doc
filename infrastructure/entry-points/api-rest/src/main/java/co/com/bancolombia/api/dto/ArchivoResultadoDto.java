package co.com.bancolombia.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchivoResultadoDto {
    private String nombreOriginal;
    private boolean exitoso;
    private String rutaAlmacenada;
    private String mensajeError;
}
