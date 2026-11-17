package co.com.bancolombia.usecase.cargadocumentos;

import co.com.bancolombia.model.documento.Documento;
import co.com.bancolombia.model.documento.DocumentoResultado;
import co.com.bancolombia.model.documento.gateways.AlmacenamientoRepository;
import co.com.bancolombia.model.documento.gateways.DocumentoMetadatosRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CargarDocumentosMasivamenteUseCaseTest {

    private final AlmacenamientoRepository almacenamientoRepository = mock(AlmacenamientoRepository.class);
    private final DocumentoMetadatosRepository documentoMetadatosRepository = mock(DocumentoMetadatosRepository.class);
    private final CargarDocumentosMasivamenteUseCase useCase =
            new CargarDocumentosMasivamenteUseCase(almacenamientoRepository, documentoMetadatosRepository);

    @Test
    void debeGuardarMetadatosCuandoElAlmacenamientoEsExitoso() {
        when(almacenamientoRepository.almacenar(any()))
                .thenReturn(Flux.just(resultadoExitoso("vt1.txt", "uuid-vt1.txt")));
        when(documentoMetadatosRepository.guardar(any())).thenReturn(Mono.empty());

        Documento documento = documento("vt1.txt").toBuilder()
                .contentType("text/plain")
                .tamanio(8L)
                .contenido("contenido".getBytes(StandardCharsets.UTF_8))
                .build();

        StepVerifier.create(useCase.ejecutar(Flux.just(documento)))
                .assertNext(resultado -> assertThat(resultado.isExitoso()).isTrue())
                .verifyComplete();

        ArgumentCaptor<Documento> captor = ArgumentCaptor.forClass(Documento.class);
        verify(documentoMetadatosRepository).guardar(captor.capture());
        Documento metadatos = captor.getValue();
        assertThat(metadatos.getUsuarioId()).isEqualTo("usuario-1");
        assertThat(metadatos.getCategoria()).isEqualTo("factura");
        assertThat(metadatos.getNombreOriginal()).isEqualTo("vt1.txt");
        assertThat(metadatos.getNombreAlmacenado()).isEqualTo("uuid-vt1.txt");
        assertThat(metadatos.getContentType()).isEqualTo("text/plain");
        assertThat(metadatos.getTamanio()).isEqualTo(8L);
        assertThat(metadatos.getRutaAlmacenada()).isEqualTo("s3://documentos/ruta/uuid-vt1.txt");
        assertThat(metadatos.getFechaCarga()).isNotNull();
        assertThat(metadatos.getContenido()).isNull();
    }

    @Test
    void debeProcesarLoteConExtensionesPermitidas() {
        when(almacenamientoRepository.almacenar(any()))
                .thenReturn(Flux.just(resultadoExitoso("vt1.txt", "uuid-vt1.txt")));
        when(documentoMetadatosRepository.guardar(any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(Flux.just(
                        documento("vt1.txt"),
                        documento("factura.xls"),
                        documento("factura.xlsx"),
                        documento("factura.pdf"))))
                .expectNextCount(4)
                .verifyComplete();

        verify(documentoMetadatosRepository, times(4)).guardar(any());
    }

    @Test
    void debeAceptarExtensionesEnMayusculas() {
        when(almacenamientoRepository.almacenar(any()))
                .thenReturn(Flux.just(resultadoExitoso("FACTURA.PDF", "uuid-FACTURA.PDF")));
        when(documentoMetadatosRepository.guardar(any())).thenReturn(Mono.empty());

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

        verifyNoInteractions(almacenamientoRepository, documentoMetadatosRepository);
    }

    @Test
    void debeMarcarFallidoSiFallaElGuardadoDeMetadatos() {
        when(almacenamientoRepository.almacenar(any()))
                .thenReturn(Flux.just(resultadoExitoso("vt1.txt", "uuid-vt1.txt")));
        when(documentoMetadatosRepository.guardar(any()))
                .thenReturn(Mono.error(new IllegalStateException("base de datos no disponible")));

        StepVerifier.create(useCase.ejecutar(Flux.just(documento("vt1.txt"))))
                .assertNext(resultado -> {
                    assertThat(resultado.isExitoso()).isFalse();
                    assertThat(resultado.getNombreOriginal()).isEqualTo("vt1.txt");
                    assertThat(resultado.getNombreAlmacenado()).isEqualTo("uuid-vt1.txt");
                    assertThat(resultado.getRutaAlmacenada())
                            .isEqualTo("s3://documentos/ruta/uuid-vt1.txt");
                    assertThat(resultado.getMensajeError())
                            .isEqualTo("Archivo almacenado en S3 pero falló el guardado de metadatos: "
                                    + "base de datos no disponible");
                })
                .verifyComplete();
    }

    @Test
    void noDebeGuardarMetadatosSiElAlmacenamientoFalla() {
        when(almacenamientoRepository.almacenar(any()))
                .thenReturn(Flux.just(DocumentoResultado.builder()
                        .nombreOriginal("vt1.txt")
                        .exitoso(false)
                        .mensajeError("S3 no disponible")
                        .build()));

        StepVerifier.create(useCase.ejecutar(Flux.just(documento("vt1.txt"))))
                .assertNext(resultado -> assertThat(resultado.isExitoso()).isFalse())
                .verifyComplete();

        verify(documentoMetadatosRepository, never()).guardar(any());
    }

    @Test
    void debeMarcarFallidoSiElAlmacenamientoNoDevuelveNombreAlmacenado() {
        when(almacenamientoRepository.almacenar(any()))
                .thenReturn(Flux.just(DocumentoResultado.builder()
                        .nombreOriginal("vt1.txt")
                        .exitoso(true)
                        .build()));

        StepVerifier.create(useCase.ejecutar(Flux.just(documento("vt1.txt"))))
                .assertNext(resultado -> {
                    assertThat(resultado.isExitoso()).isFalse();
                    assertThat(resultado.getMensajeError())
                            .isEqualTo("Archivo almacenado en S3 pero falló el guardado de metadatos: "
                                    + "nombre almacenado vacío");
                })
                .verifyComplete();

        verifyNoInteractions(documentoMetadatosRepository);
    }

    private DocumentoResultado resultadoExitoso(String nombreOriginal, String nombreAlmacenado) {
        return DocumentoResultado.builder()
                .nombreOriginal(nombreOriginal)
                .nombreAlmacenado(nombreAlmacenado)
                .exitoso(true)
                .rutaAlmacenada("s3://documentos/ruta/" + nombreAlmacenado)
                .build();
    }

    private Documento documento(String nombreOriginal) {
        return Documento.builder()
                .usuarioId("usuario-1")
                .categoria("factura")
                .nombreOriginal(nombreOriginal)
                .build();
    }
}
