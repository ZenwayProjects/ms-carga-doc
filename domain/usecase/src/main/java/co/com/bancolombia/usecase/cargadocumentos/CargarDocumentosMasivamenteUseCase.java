package co.com.bancolombia.usecase.cargadocumentos;

import co.com.bancolombia.model.documento.Documento;
import co.com.bancolombia.model.documento.DocumentoResultado;
import co.com.bancolombia.model.documento.gateways.AlmacenamientoGateway;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.logging.Logger;

@RequiredArgsConstructor
public class CargarDocumentosMasivamenteUseCase {
    private static final Logger LOGGER = Logger.getLogger(CargarDocumentosMasivamenteUseCase.class.getName());
    private final AlmacenamientoGateway almacenamientoGateway;

    public Flux<DocumentoResultado> ejecutar(Flux<Documento> documentos) {
        LOGGER.info("Iniciando carga masiva con WebFlux");
        return documentos
                .flatMap(documento -> almacenamientoGateway.almacenar(Flux.just(documento))
                        .publishOn(Schedulers.boundedElastic())
                        .doOnNext(res -> LOGGER.info(() -> String.format("Procesando [%s] en thread=%s virtual=%s",
                                res.getNombreOriginal() != null ? res.getNombreOriginal() : documento.getNombreOriginal(),
                                Thread.currentThread().getName(),
                                Thread.currentThread().isVirtual())))
                        .defaultIfEmpty(DocumentoResultado.builder()
                                .nombreOriginal(documento.getNombreOriginal())
                                .exitoso(false)
                                .mensajeError("Sin resultado de almacenamiento")
                                .build()), 200)
                .doOnComplete(() -> LOGGER.info("Carga masiva finalizada"));
    }
}
