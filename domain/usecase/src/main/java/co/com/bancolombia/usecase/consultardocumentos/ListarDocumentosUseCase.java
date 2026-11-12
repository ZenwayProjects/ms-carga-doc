package co.com.bancolombia.usecase.consultardocumentos;

import co.com.bancolombia.model.documento.Documento;
import co.com.bancolombia.model.documento.gateways.AlmacenamientoRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class ListarDocumentosUseCase {
    private final AlmacenamientoRepository almacenamientoRepository;

    public Flux<Documento> ejecutar(String usuarioId, String categoria) {
        validarSegmento(usuarioId, "usuarioId");
        validarSegmento(categoria, "categoria");

        return almacenamientoRepository.listar(usuarioId, categoria);
    }

    private void validarSegmento(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo '" + campo + "' es obligatorio");
        }
        if (valor.contains("/") || valor.contains("\\") || valor.contains("..")) {
            throw new IllegalArgumentException("El campo '" + campo + "' contiene caracteres invalidos");
        }
    }
}
