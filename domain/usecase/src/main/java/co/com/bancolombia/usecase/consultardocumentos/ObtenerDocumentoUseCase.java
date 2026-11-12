package co.com.bancolombia.usecase.consultardocumentos;

import co.com.bancolombia.model.documento.Documento;
import co.com.bancolombia.model.documento.DocumentoNoEncontradoException;
import co.com.bancolombia.model.documento.gateways.AlmacenamientoRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class ObtenerDocumentoUseCase {
    private final AlmacenamientoRepository almacenamientoRepository;

    public Mono<Documento> ejecutar(String usuarioId, String categoria, String nombreArchivo) {
        validarSegmento(usuarioId, "usuarioId");
        validarSegmento(categoria, "categoria");
        validarSegmento(nombreArchivo, "nombreArchivo");

        return almacenamientoRepository.obtener(usuarioId, categoria, nombreArchivo)
                .switchIfEmpty(Mono.error(new DocumentoNoEncontradoException(String.format(
                        "Archivo [%s] no encontrado para usuario [%s] en categoria [%s]",
                        nombreArchivo, usuarioId, categoria))));
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
