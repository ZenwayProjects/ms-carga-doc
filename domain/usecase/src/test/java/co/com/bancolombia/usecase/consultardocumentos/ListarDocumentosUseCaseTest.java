package co.com.bancolombia.usecase.consultardocumentos;

import co.com.bancolombia.model.documento.Documento;
import co.com.bancolombia.model.documento.gateways.AlmacenamientoGateway;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListarDocumentosUseCaseTest {

    private final AlmacenamientoGateway almacenamientoGateway = mock(AlmacenamientoGateway.class);
    private final ListarDocumentosUseCase useCase = new ListarDocumentosUseCase(almacenamientoGateway);

    @Test
    void debeListarLosDocumentosDeLaCategoria() {
        Documento factura = Documento.builder()
                .usuarioId("usuario-1")
                .categoria("factura")
                .nombreAlmacenado("uuid-factura.pdf")
                .build();
        when(almacenamientoGateway.listar("usuario-1", "factura")).thenReturn(Flux.just(factura));

        StepVerifier.create(useCase.ejecutar("usuario-1", "factura"))
                .expectNext(factura)
                .verifyComplete();
    }

    @Test
    void debeRetornarVacioCuandoNoHayDocumentos() {
        when(almacenamientoGateway.listar("usuario-1", "factura")).thenReturn(Flux.empty());

        StepVerifier.create(useCase.ejecutar("usuario-1", "factura"))
                .verifyComplete();
    }

    @Test
    void debeRechazarCamposVacios() {
        assertThatThrownBy(() -> useCase.ejecutar("", "factura"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.ejecutar("usuario-1", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
