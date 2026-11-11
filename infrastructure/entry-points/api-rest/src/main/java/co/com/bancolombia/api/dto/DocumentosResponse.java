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
public class DocumentosResponse {
    private String usuarioId;
    private String categoria;
    private int total;
    private List<DocumentoDto> archivos;
}
