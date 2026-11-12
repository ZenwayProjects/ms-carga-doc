package co.com.bancolombia.model.documento.gateways;

import co.com.bancolombia.model.documento.Documento;
import co.com.bancolombia.model.documento.DocumentoResultado;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AlmacenamientoRepository {
    Flux<DocumentoResultado> almacenar(Flux<Documento> documentos);

    Mono<Documento> obtener(String usuarioId, String categoria, String nombreArchivo);

    Flux<Documento> listar(String usuarioId, String categoria);
}
