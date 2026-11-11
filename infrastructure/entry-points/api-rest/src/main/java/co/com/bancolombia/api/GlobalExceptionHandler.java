package co.com.bancolombia.api;

import co.com.bancolombia.api.dto.ErrorResponseDto;
import co.com.bancolombia.model.documento.DocumentoNoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DocumentoNoEncontradoException.class)
    public ResponseEntity<ErrorResponseDto> handleNoEncontrado(
            DocumentoNoEncontradoException ex, ServerWebExchange exchange) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), exchange);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleArgumentoInvalido(
            IllegalArgumentException ex, ServerWebExchange exchange) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), exchange);
    }

    private ResponseEntity<ErrorResponseDto> buildResponse(
            HttpStatus status, String message, ServerWebExchange exchange) {
        return ResponseEntity.status(status).body(ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(exchange.getRequest().getPath().value())
                .build());
    }
}
