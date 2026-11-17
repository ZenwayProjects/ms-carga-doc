package co.com.bancolombia.model.documento.gateways;

import co.com.bancolombia.model.documento.Documento;
import reactor.core.publisher.Mono;

public interface DocumentoMetadatosRepository {
    Mono<Void> guardar(Documento documento);
}
