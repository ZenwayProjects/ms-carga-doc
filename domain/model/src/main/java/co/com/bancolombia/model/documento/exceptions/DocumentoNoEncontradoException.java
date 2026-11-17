package co.com.bancolombia.model.documento.exceptions;

public class DocumentoNoEncontradoException extends RuntimeException {
    public DocumentoNoEncontradoException(String message) {
        super(message);
    }
}
