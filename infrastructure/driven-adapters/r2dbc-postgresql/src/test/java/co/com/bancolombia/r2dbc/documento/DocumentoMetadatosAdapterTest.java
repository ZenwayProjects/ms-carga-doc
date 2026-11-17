package co.com.bancolombia.r2dbc.documento;

import co.com.bancolombia.model.documento.Documento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DocumentoMetadatosAdapterTest {

    private DocumentoReactiveRepository repository;
    private DocumentoMetadatosAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = mock(DocumentoReactiveRepository.class);
        adapter = new DocumentoMetadatosAdapter(repository);
    }

    @Test
    void debeInsertarLosMetadatosDelDocumento() {
        LocalDateTime fechaCarga = LocalDateTime.of(2026, 11, 17, 10, 30);
        when(repository.insertarSiNoExiste(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(1));

        Documento documento = documento().fechaCarga(fechaCarga).build();

        StepVerifier.create(adapter.guardar(documento))
                .verifyComplete();

        ArgumentCaptor<LocalDateTime> fechaCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).insertarSiNoExiste(
                eq("uuid-vt1.txt"),
                eq("usuario-1"),
                eq("factura"),
                eq("vt1.txt"),
                eq("text/plain"),
                eq(8L),
                eq("s3://documentos/usuarios/usuario-1/factura/uuid-vt1.txt"),
                fechaCaptor.capture());
        assertThat(fechaCaptor.getValue()).isEqualTo(fechaCarga);
    }

    @Test
    void debeUsarLaFechaActualCuandoNoVieneLaFechaDeCarga() {
        when(repository.insertarSiNoExiste(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(0));

        StepVerifier.create(adapter.guardar(documento().fechaCarga(null).build()))
                .verifyComplete();

        ArgumentCaptor<LocalDateTime> fechaCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).insertarSiNoExiste(any(), any(), any(), any(), any(), any(), any(),
                fechaCaptor.capture());
        assertThat(fechaCaptor.getValue()).isNotNull();
    }

    @Test
    void debeRechazarDocumentoSinNombreAlmacenado() {
        StepVerifier.create(adapter.guardar(documento().nombreAlmacenado(null).build()))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("El campo 'nombreAlmacenado' es obligatorio"))
                .verify();

        verifyNoInteractions(repository);
    }

    @Test
    void debeRechazarDocumentoNulo() {
        StepVerifier.create(adapter.guardar(null))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("El documento es obligatorio"))
                .verify();

        verifyNoInteractions(repository);
    }

    private Documento.DocumentoBuilder documento() {
        return Documento.builder()
                .usuarioId("usuario-1")
                .categoria("factura")
                .nombreOriginal("vt1.txt")
                .nombreAlmacenado("uuid-vt1.txt")
                .contentType("text/plain")
                .tamanio(8L)
                .rutaAlmacenada("s3://documentos/usuarios/usuario-1/factura/uuid-vt1.txt");
    }
}
