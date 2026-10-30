package co.com.bancolombia.model.documento.gateways;

import co.com.bancolombia.model.documento.Documento;
import co.com.bancolombia.model.documento.DocumentoResultado;
import reactor.core.publisher.Flux;

public interface AlmacenamientoGateway {
    Flux<DocumentoResultado> almacenar(Flux<Documento> documentos);
}
