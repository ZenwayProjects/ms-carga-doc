package co.com.bancolombia.api;

import co.com.bancolombia.model.documento.Documento;
import co.com.bancolombia.model.documento.DocumentoNoEncontradoException;
import co.com.bancolombia.usecase.cargadocumentos.CargarDocumentosMasivamenteUseCase;
import co.com.bancolombia.usecase.consultardocumentos.ListarDocumentosUseCase;
import co.com.bancolombia.usecase.consultardocumentos.ObtenerDocumentoUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentoControllerTest {

    private CargarDocumentosMasivamenteUseCase cargarDocumentosUseCase;
    private ObtenerDocumentoUseCase obtenerDocumentoUseCase;
    private ListarDocumentosUseCase listarDocumentosUseCase;
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        cargarDocumentosUseCase = mock(CargarDocumentosMasivamenteUseCase.class);
        obtenerDocumentoUseCase = mock(ObtenerDocumentoUseCase.class);
        listarDocumentosUseCase = mock(ListarDocumentosUseCase.class);
        client = WebTestClient.bindToController(new DocumentoController(
                        cargarDocumentosUseCase, obtenerDocumentoUseCase, listarDocumentosUseCase))
                .controllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void debeListarLosArchivosDeFacturaDeUnUsuario() {
        Documento documento = Documento.builder()
                .usuarioId("usuario-1")
                .categoria("factura")
                .nombreOriginal("factura.pdf")
                .nombreAlmacenado("uuid-factura.pdf")
                .contentType("application/pdf")
                .tamanio(120L)
                .rutaAlmacenada("s3://documentos/usuarios/usuario-1/factura/uuid-factura.pdf")
                .fechaCarga(LocalDateTime.of(2026, 11, 8, 10, 0))
                .build();
        when(listarDocumentosUseCase.ejecutar("usuario-1", "factura")).thenReturn(Flux.just(documento));

        client.get()
                .uri("/api/documentos/usuarios/usuario-1/factura")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.usuarioId").isEqualTo("usuario-1")
                .jsonPath("$.categoria").isEqualTo("factura")
                .jsonPath("$.total").isEqualTo(1)
                .jsonPath("$.archivos[0].nombreArchivo").isEqualTo("uuid-factura.pdf")
                .jsonPath("$.archivos[0].nombreOriginal").isEqualTo("factura.pdf")
                .jsonPath("$.archivos[0].urlDescarga")
                .isEqualTo("/api/documentos/usuarios/usuario-1/factura/uuid-factura.pdf");
    }

    @Test
    void debeRetornarListaVaciaCuandoNoHayArchivos() {
        when(listarDocumentosUseCase.ejecutar("usuario-1", "factura")).thenReturn(Flux.empty());

        client.get()
                .uri("/api/documentos/usuarios/usuario-1/factura")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.total").isEqualTo(0);
    }

    @Test
    void debeDescargarElArchivoComoAdjunto() {
        byte[] contenido = "contenido-factura".getBytes(StandardCharsets.UTF_8);
        Documento documento = Documento.builder()
                .usuarioId("usuario-1")
                .categoria("factura")
                .nombreOriginal("factura.pdf")
                .nombreAlmacenado("uuid-factura.pdf")
                .contentType("application/pdf")
                .contenido(contenido)
                .build();
        when(obtenerDocumentoUseCase.ejecutar("usuario-1", "factura", "uuid-factura.pdf"))
                .thenReturn(Mono.just(documento));

        client.get()
                .uri("/api/documentos/usuarios/usuario-1/factura/uuid-factura.pdf")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_PDF)
                .expectHeader().valueMatches(HttpHeaders.CONTENT_DISPOSITION, ".*attachment.*factura\\.pdf.*")
                .expectBody(byte[].class)
                .value(bytes -> org.assertj.core.api.Assertions.assertThat(bytes).containsExactly(contenido));
    }

    @Test
    void debeRetornarNotFoundCuandoElArchivoNoExiste() {
        when(obtenerDocumentoUseCase.ejecutar("usuario-1", "factura", "no-existe.pdf"))
                .thenReturn(Mono.error(new DocumentoNoEncontradoException("Archivo no encontrado")));

        client.get()
                .uri("/api/documentos/usuarios/usuario-1/factura/no-existe.pdf")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.message").isEqualTo("Archivo no encontrado");
    }
}
