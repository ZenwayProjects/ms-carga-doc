package co.com.bancolombia.api;

import co.com.bancolombia.model.documento.Documento;
import co.com.bancolombia.model.documento.exceptions.DocumentoNoEncontradoException;
import co.com.bancolombia.model.documento.DocumentoResultado;
import co.com.bancolombia.usecase.cargadocumentos.CargarDocumentosMasivamenteUseCase;
import co.com.bancolombia.usecase.consultardocumentos.ListarDocumentosUseCase;
import co.com.bancolombia.usecase.consultardocumentos.ObtenerDocumentoUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RouterRestTest {

    private CargarDocumentosMasivamenteUseCase cargarDocumentosUseCase;
    private ObtenerDocumentoUseCase obtenerDocumentoUseCase;
    private ListarDocumentosUseCase listarDocumentosUseCase;
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        cargarDocumentosUseCase = mock(CargarDocumentosMasivamenteUseCase.class);
        obtenerDocumentoUseCase = mock(ObtenerDocumentoUseCase.class);
        listarDocumentosUseCase = mock(ListarDocumentosUseCase.class);
        Handler handler = new Handler(cargarDocumentosUseCase, obtenerDocumentoUseCase, listarDocumentosUseCase);
        client = WebTestClient.bindToRouterFunction(new RouterRest().routerFunction(handler)).build();
    }

    @Test
    void debeCargarArchivosMasivamente() {
        byte[] contenido = "contenido-txt".getBytes(StandardCharsets.UTF_8);
        MultipartBodyBuilder multipart = new MultipartBodyBuilder();
        multipart.part("files", new ByteArrayResource(contenido))
                .filename("vt1.txt")
                .contentType(MediaType.TEXT_PLAIN);
        when(cargarDocumentosUseCase.ejecutar(any())).thenReturn(Flux.just(DocumentoResultado.builder()
                .nombreOriginal("vt1.txt")
                .exitoso(true)
                .rutaAlmacenada("s3://documentos/usuarios/usuario-1/facturas/uuid-vt1.txt")
                .build()));

        client.post()
                .uri("/api/documentos/usuarios/usuario-1/facturas/carga-masiva")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipart.build()))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.total").isEqualTo(1)
                .jsonPath("$.exitosos").isEqualTo(1)
                .jsonPath("$.fallidos").isEqualTo(0)
                .jsonPath("$.resultados[0].nombreOriginal").isEqualTo("vt1.txt")
                .jsonPath("$.resultados[0].exitoso").isEqualTo(true);
    }

    @Test
    void debeRetornarBadRequestCuandoElFormatoDeArchivoNoEsValido() {
        byte[] contenido = "contenido-docx".getBytes(StandardCharsets.UTF_8);
        MultipartBodyBuilder multipart = new MultipartBodyBuilder();
        multipart.part("files", new ByteArrayResource(contenido))
                .filename("documento.docx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM);
        String mensaje = "Formato de archivo no válido [documento.docx]. "
                + "Solo se permiten: txt, xls, xlsx, pdf";
        when(cargarDocumentosUseCase.ejecutar(any()))
                .thenReturn(Flux.error(new IllegalArgumentException(mensaje)));

        client.post()
                .uri("/api/documentos/usuarios/usuario-1/facturas/carga-masiva")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipart.build()))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").isEqualTo(mensaje);
    }

    @Test
    void debeListarLosArchivosDeUnaCategoria() {
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
    void debeDescargarTxtPorNombreAlmacenadoConservandoElNombreOriginal() {
        byte[] contenido = "contenido-txt".getBytes(StandardCharsets.UTF_8);
        String nombreAlmacenado = "8f445a1d-c27c-42b9-8c15-be7401c11b00-vt1.txt";
        Documento documento = Documento.builder()
                .usuarioId("usuario-1")
                .categoria("facturas")
                .nombreOriginal("vt1.txt")
                .nombreAlmacenado(nombreAlmacenado)
                .contentType("application/octet-stream")
                .contenido(contenido)
                .build();
        when(obtenerDocumentoUseCase.ejecutar("usuario-1", "facturas", nombreAlmacenado))
                .thenReturn(Mono.just(documento));

        client.get()
                .uri("/api/documentos/usuarios/usuario-1/facturas/" + nombreAlmacenado)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
                .expectHeader().valueMatches(HttpHeaders.CONTENT_DISPOSITION, ".*attachment.*vt1\\.txt.*")
                .expectBody(byte[].class)
                .value(bytes -> assertThat(bytes).containsExactly(contenido));
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

    @Test
    void debeRetornarBadRequestCuandoLaCategoriaEsInvalida() {
        when(listarDocumentosUseCase.ejecutar("usuario-1", "factura"))
                .thenReturn(Flux.error(new IllegalArgumentException("El campo 'categoria' contiene caracteres invalidos")));

        client.get()
                .uri("/api/documentos/usuarios/usuario-1/factura")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("El campo 'categoria' contiene caracteres invalidos");
    }
}
