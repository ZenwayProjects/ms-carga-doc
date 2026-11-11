package co.com.bancolombia.usecase.consultardocumentos;

import co.com.bancolombia.model.documento.Documento;
import co.com.bancolombia.model.documento.DocumentoNoEncontradoException;
import co.com.bancolombia.model.documento.gateways.AlmacenamientoGateway;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ObtenerDocumentoUseCaseTest {

    private final AlmacenamientoGateway almacenamientoGateway = mock(AlmacenamientoGateway.class);
    private final ObtenerDocumentoUseCase useCase = new ObtenerDocumentoUseCase(almacenamientoGateway);

    @Test
    void debeRetornarElDocumentoSolicitado() {
        Documento documento = Documento.builder()
                .usuarioId("usuario-1")
                .categoria("factura")
                .nombreAlmacenado("uuid-factura.pdf")
                .nombreOriginal("factura.pdf")
                .build();
        when(almacenamientoGateway.obtener("usuario-1", "factura", "uuid-factura.pdf"))
                .thenReturn(Mono.just(documento));

        StepVerifier.create(useCase.ejecutar("usuario-1", "factura", "uuid-factura.pdf"))
                .expectNext(documento)
                .verifyComplete();
    }

    @Test
    void debeFallarCuandoElDocumentoNoExiste() {
        when(almacenamientoGateway.obtener("usuario-1", "factura", "no-existe.pdf"))
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
}
