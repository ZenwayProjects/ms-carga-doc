package co.com.bancolombia.model.documento;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DocumentoResultado {
    private String nombreOriginal;
    private boolean exitoso;
    private String rutaAlmacenada;
    private String mensajeError;
}
