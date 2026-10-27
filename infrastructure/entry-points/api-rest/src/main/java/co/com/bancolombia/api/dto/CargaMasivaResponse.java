package co.com.bancolombia.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CargaMasivaResponse {
    private int total;
    private int exitosos;
    private int fallidos;
    private List<ArchivoResultadoDto> resultados;
}
