package co.com.bancolombia.usecase.consultardocumentos;

import co.com.bancolombia.model.documento.Documento;
import co.com.bancolombia.model.documento.exceptions.DocumentoNoEncontradoException;
import co.com.bancolombia.model.documento.gateways.AlmacenamientoRepository;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ObtenerDocumentoUseCaseTest {

    private final AlmacenamientoRepository almacenamientoRepository = mock(AlmacenamientoRepository.class);
    private final ObtenerDocumentoUseCase useCase = new ObtenerDocumentoUseCase(almacenamientoRepository);

    @Test
    void debeRetornarElDocumentoSolicitado() {
        Documento documento = Documento.builder()
                .usuarioId("usuario-1")
                .categoria("factura")
                .nombreAlmacenado("uuid-factura.pdf")
                .nombreOriginal("factura.pdf")
                .build();
        when(almacenamientoRepository.obtener("usuario-1", "factura", "uuid-factura.pdf"))
                .thenReturn(Mono.just(documento));

        StepVerifier.create(useCase.ejecutar("usuario-1", "factura", "uuid-factura.pdf"))
                .expectNext(documento)
                .verifyComplete();
    }

    @Test
    void debeFallarCuandoElDocumentoNoExiste() {
        when(almacenamientoRepository.obtener("usuario-1", "factura", "no-existe.pdf"))
                .thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar("usuario-1", "factura", "no-existe.pdf"))
                .expectError(DocumentoNoEncontradoException.class)
                .verify();
    }

    @Test
    void debeRechazarSegmentosConRutaInvalida() {
        assertThatThrownBy(() -> useCase.ejecutar("../otro", "factura", "archivo.pdf"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.ejecutar("usuario-1", "factura", "..\\archivo.pdf"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void debeAceptarLasExtensionesPermitidas() {
        List.of("factura.pdf", "factura.xls", "factura.xlsx", "vt1.txt", "FACTURA.PDF")
                .forEach(nombreOriginal -> {
                    String nombreAlmacenado = "uuid-" + nombreOriginal;
                    Documento documento = Documento.builder()
                            .usuarioId("usuario-1")
                            .categoria("factura")
                            .nombreAlmacenado(nombreAlmacenado)
                            .nombreOriginal(nombreOriginal)
                            .build();
                    when(almacenamientoRepository.obtener("usuario-1", "factura", nombreAlmacenado))
                            .thenReturn(Mono.just(documento));

                    StepVerifier.create(useCase.ejecutar("usuario-1", "factura", nombreAlmacenado))
                            .expectNext(documento)
                            .verifyComplete();
                });
    }

    @Test
    void debeRechazarDescargaConExtensionNoPermitida() {
        assertThatThrownBy(() -> useCase.ejecutar("usuario-1", "factura", "uuid-documento.docx"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Formato de archivo no válido [uuid-documento.docx]. "
                        + "Solo se permiten: txt, xls, xlsx, pdf");

        verifyNoInteractions(almacenamientoRepository);
    }
}
