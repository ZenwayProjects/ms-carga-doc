package co.com.bancolombia.usecase.cargadocumentos;

import co.com.bancolombia.model.documento.Documento;
import co.com.bancolombia.model.documento.DocumentoResultado;
import co.com.bancolombia.model.documento.gateways.AlmacenamientoRepository;
import co.com.bancolombia.model.documento.gateways.DocumentoMetadatosRepository;
import co.com.bancolombia.model.documento.validations.ExtensionArchivo;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class CargarDocumentosMasivamenteUseCase {
    private static final Logger LOGGER = Logger.getLogger(CargarDocumentosMasivamenteUseCase.class.getName());
    private static final String ERROR_METADATOS = "Archivo almacenado en S3 pero falló el guardado de metadatos: ";

    private final AlmacenamientoRepository almacenamientoRepository;
    private final DocumentoMetadatosRepository documentoMetadatosRepository;

    public Flux<DocumentoResultado> ejecutar(Flux<Documento> documentos) {
        LOGGER.info("Iniciando carga masiva con WebFlux");
        return documentos
                .collectList()
                .flatMapMany(lista -> {
                    lista.forEach(documento -> ExtensionArchivo.validar(documento.getNombreOriginal()));
                    return Flux.fromIterable(lista);
                })
                .flatMap(this::almacenarYGuardarMetadatos, 200)
                .doOnComplete(() -> LOGGER.info("Carga masiva finalizada"));
    }

    private Mono<DocumentoResultado> almacenarYGuardarMetadatos(Documento documento) {
        return almacenamientoRepository.almacenar(Flux.just(documento))
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(res -> LOGGER.info(() -> String.format("Procesando [%s] en thread=%s virtual=%s",
                        res.getNombreOriginal() != null ? res.getNombreOriginal() : documento.getNombreOriginal(),
                        Thread.currentThread().getName(),
                        Thread.currentThread().isVirtual())))
                .defaultIfEmpty(DocumentoResultado.builder()
                        .nombreOriginal(documento.getNombreOriginal())
                        .exitoso(false)
                        .mensajeError("Sin resultado de almacenamiento")
                        .build())
                .next()
                .flatMap(resultado -> {
                    if (!resultado.isExitoso()) {
                        return Mono.just(resultado);
                    }
                    if (resultado.getNombreAlmacenado() == null || resultado.getNombreAlmacenado().isBlank()) {
                        return Mono.just(conErrorMetadatos(resultado, "nombre almacenado vacío"));
                    }
                    return documentoMetadatosRepository.guardar(construirMetadatos(documento, resultado))
                            .thenReturn(resultado)
                            .onErrorResume(error -> Mono.just(conErrorMetadatos(resultado, mensajeError(error))));
                });
    }

    private Documento construirMetadatos(Documento documento, DocumentoResultado resultado) {
        return Documento.builder()
                .usuarioId(documento.getUsuarioId())
                .categoria(documento.getCategoria())
                .nombreOriginal(resultado.getNombreOriginal())
                .nombreAlmacenado(resultado.getNombreAlmacenado())
                .contentType(documento.getContentType())
                .tamanio(tamanio(documento))
                .rutaAlmacenada(resultado.getRutaAlmacenada())
                .fechaCarga(documento.getFechaCarga() != null ? documento.getFechaCarga() : LocalDateTime.now())
                .build();
    }

    private long tamanio(Documento documento) {
        if (documento.getTamanio() > 0) {
            return documento.getTamanio();
        }
        return documento.getContenido() != null ? documento.getContenido().length : 0L;
    }

    private DocumentoResultado conErrorMetadatos(DocumentoResultado resultado, String detalle) {
        return DocumentoResultado.builder()
                .nombreOriginal(resultado.getNombreOriginal())
                .nombreAlmacenado(resultado.getNombreAlmacenado())
                .exitoso(false)
                .rutaAlmacenada(resultado.getRutaAlmacenada())
                .mensajeError(ERROR_METADATOS + detalle)
                .build();
    }

    private String mensajeError(Throwable error) {
        return error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
    }
}
