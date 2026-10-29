package co.com.bancolombia.usecase.cargadocumentos;

import co.com.bancolombia.model.documento.Documento;
import co.com.bancolombia.model.documento.DocumentoResultado;
import co.com.bancolombia.model.documento.gateways.AlmacenamientoGateway;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class CargarDocumentosMasivamenteUseCase {
    private static final Logger LOGGER = Logger.getLogger(CargarDocumentosMasivamenteUseCase.class.getName());
    private final AlmacenamientoGateway almacenamientoGateway;
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public List<DocumentoResultado> ejecutar(List<Documento> documentos) {
        if (documentos == null || documentos.isEmpty()) {
            return List.of();
        }

//        LOGGER.info(() -> String.format("Iniciando carga masiva: total=%d, usando hilos virtuales con limite 200 por request", documentos.size()));
        Semaphore semaphore = new Semaphore(200);
        List<CompletableFuture<DocumentoResultado>> futures = documentos.stream()
                .map(documento -> CompletableFuture.supplyAsync(() -> {
                    try {
                        semaphore.acquire();
                        try {
                            /*LOGGER.info(() -> String.format("Procesando [%s] en thread=%s virtual=%s",
                                    documento.getNombreOriginal(), 
                                    Thread.currentThread().getName(), 
                                    Thread.currentThread().isVirtual()));*/
                            List<Documento> single = new ArrayList<>();
                            single.add(documento);
                            List<DocumentoResultado> resultados = almacenamientoGateway.almacenar(single);
                            return resultados.isEmpty() ? DocumentoResultado.builder()
                                    .nombreOriginal(documento.getNombreOriginal())
                                    .exitoso(false)
                                    .mensajeError("Sin resultado de almacenamiento")
                                    .build() : resultados.get(0);
                        } finally {
                            semaphore.release();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return DocumentoResultado.builder()
                                .nombreOriginal(documento.getNombreOriginal())
                                .exitoso(false)
                                .mensajeError("Proceso interrumpido")
                                .build();
                    } catch (Exception e) {
                        return DocumentoResultado.builder()
                                .nombreOriginal(documento.getNombreOriginal())
                                .exitoso(false)
                                .mensajeError(e.getMessage())
                                .build();
                    }
                }, virtualThreadExecutor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        LOGGER.info(() -> String.format("Carga masiva finalizada: total=%d", documentos.size()));
        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }
}
