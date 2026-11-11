package co.com.bancolombia.model.documento;

public class DocumentoNoEncontradoException extends RuntimeException {
    public DocumentoNoEncontradoException(String message) {
        super(message);
    }
}
