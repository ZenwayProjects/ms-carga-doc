package co.com.bancolombia.usecase.cargadocumentos;

import co.com.bancolombia.model.documento.Documento;
import co.com.bancolombia.model.documento.DocumentoResultado;
import co.com.bancolombia.model.documento.gateways.AlmacenamientoRepository;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CargarDocumentosMasivamenteUseCaseTest {

    private final AlmacenamientoRepository almacenamientoRepository = mock(AlmacenamientoRepository.class);
    private final CargarDocumentosMasivamenteUseCase useCase =
            new CargarDocumentosMasivamenteUseCase(almacenamientoRepository);

    @Test
    void debeProcesarLoteConExtensionesPermitidas() {
        when(almacenamientoRepository.almacenar(any()))
                .thenReturn(Flux.just(DocumentoResultado.builder().exitoso(true).build()));

        StepVerifier.create(useCase.ejecutar(Flux.just(
                        documento("vt1.txt"),
                        documento("factura.xls"),
                        documento("factura.xlsx"),
                        documento("factura.pdf"))))
                .expectNextCount(4)
                .verifyComplete();
    }

    @Test
    void debeAceptarExtensionesEnMayusculas() {
        when(almacenamientoRepository.almacenar(any()))
                .thenReturn(Flux.just(DocumentoResultado.builder().exitoso(true).build()));

        StepVerifier.create(useCase.ejecutar(Flux.just(documento("FACTURA.PDF"))))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void debeRechazarLoteCompletoSiUnArchivoTieneExtensionNoPermitida() {
        StepVerifier.create(useCase.ejecutar(Flux.just(
                        documento("vt1.txt"),
                        documento("documento.docx"))))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("Formato de archivo no válido [documento.docx]. "
                                + "Solo se permiten: txt, xls, xlsx, pdf"))
                .verify();

        verifyNoInteractions(almacenamientoRepository);
    }

    private Documento documento(String nombreOriginal) {
        return Documento.builder()
                .usuarioId("usuario-1")
                .categoria("factura")
                .nombreOriginal(nombreOriginal)
                .build();
    }
}
