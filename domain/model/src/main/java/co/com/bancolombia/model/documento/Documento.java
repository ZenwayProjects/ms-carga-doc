package co.com.bancolombia.model.documento;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder(toBuilder = true)
public class Documento {
    private String nombreOriginal;
    private String contentType;
    private long tamanio;
    private byte[] contenido;
    private String nombreAlmacenado;
    private String rutaAlmacenada;
    private LocalDateTime fechaCarga;
}
