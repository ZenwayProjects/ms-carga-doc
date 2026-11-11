package co.com.bancolombia.s3.adapter;

import co.com.bancolombia.model.documento.Documento;
import co.com.bancolombia.model.documento.DocumentoResultado;
import co.com.bancolombia.s3.config.model.S3ConnectionProperties;
import co.com.bancolombia.s3.operations.S3Operations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3AdapterTest {

    private S3Operations s3Operations;
    private S3Adapter adapter;

    @BeforeEach
    void setUp() {
        s3Operations = mock(S3Operations.class);
        S3ConnectionProperties properties = new S3ConnectionProperties(
                "http://localhost:4566", "us-east-1", "documentos", "usuarios");
        adapter = new S3Adapter(s3Operations, properties);
    }

    @Test
    void debeGuardarUsandoLaEstructuraDeUsuarioCategoriaYNombreOriginal() {
        when(s3Operations.uploadObject(eq("documentos"), anyString(), any(byte[].class), eq("application/pdf")))
                .thenReturn(true);

        Documento documento = Documento.builder()
                .usuarioId("usuario-1")
                .categoria("factura")
                .nombreOriginal("factura.pdf")
                .contentType("application/pdf")
                .contenido("contenido".getBytes(StandardCharsets.UTF_8))
                .build();

        AtomicReference<DocumentoResultado> resultado = new AtomicReference<>();
        StepVerifier.create(adapter.almacenar(Flux.just(documento)))
                .assertNext(resultado::set)
                .verifyComplete();

        assertThat(resultado.get().isExitoso()).isTrue();
        assertThat(resultado.get().getRutaAlmacenada())
                .startsWith("s3://documentos/usuarios/usuario-1/factura/")
                .endsWith("-factura.pdf");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(s3Operations).uploadObject(eq("documentos"), keyCaptor.capture(), any(byte[].class), eq("application/pdf"));
        assertThat(keyCaptor.getValue()).startsWith("usuarios/usuario-1/factura/").endsWith("-factura.pdf");
    }

    @Test
    void debeListarLosObjetosDeLaCategoria() {
        S3Object objeto = S3Object.builder()
                .key("usuarios/usuario-1/factura/550e8400-e29b-41d4-a716-446655440000-factura.pdf")
                .size(120L)
                .lastModified(Instant.parse("2026-11-08T10:00:00Z"))
                .build();
        when(s3Operations.listObjects("documentos", "usuarios/usuario-1/factura/"))
                .thenReturn(List.of(objeto));

        StepVerifier.create(adapter.listar("usuario-1", "factura"))
                .assertNext(documento -> {
                    assertThat(documento.getNombreAlmacenado())
                            .isEqualTo("550e8400-e29b-41d4-a716-446655440000-factura.pdf");
                    assertThat(documento.getNombreOriginal()).isEqualTo("factura.pdf");
                    assertThat(documento.getTamanio()).isEqualTo(120L);
                    assertThat(documento.getContenido()).isNull();
                    assertThat(documento.getRutaAlmacenada())
                            .isEqualTo("s3://documentos/usuarios/usuario-1/factura/"
                                    + "550e8400-e29b-41d4-a716-446655440000-factura.pdf");
                })
                .verifyComplete();
    }

    @Test
    void debeObtenerElContenidoYMetadatosDelObjeto() {
        byte[] contenido = "contenido-factura".getBytes(StandardCharsets.UTF_8);
        GetObjectResponse metadata = GetObjectResponse.builder()
                .contentType("application/pdf")
                .lastModified(Instant.parse("2026-11-08T10:00:00Z"))
                .build();
        when(s3Operations.getObjectBytes("documentos",
                "usuarios/usuario-1/factura/550e8400-e29b-41d4-a716-446655440000-factura.pdf"))
                .thenReturn(ResponseBytes.fromByteArray(metadata, contenido));

        StepVerifier.create(adapter.obtener("usuario-1", "factura",
                        "550e8400-e29b-41d4-a716-446655440000-factura.pdf"))
                .assertNext(documento -> {
                    assertThat(documento.getNombreOriginal()).isEqualTo("factura.pdf");
                    assertThat(documento.getContentType()).isEqualTo("application/pdf");
                    assertThat(documento.getContenido()).containsExactly(contenido);
                })
                .verifyComplete();
    }

    @Test
    void debeRetornarVacioCuandoElObjetoNoExiste() {
        when(s3Operations.getObjectBytes("documentos", "usuarios/usuario-1/factura/no-existe.pdf"))
                .thenThrow(S3Exception.builder().statusCode(404).message("NoSuchKey").build());

        StepVerifier.create(adapter.obtener("usuario-1", "factura", "no-existe.pdf"))
                .verifyComplete();
    }

    @Test
    void debeRechazarRutasConTraversal() {
        StepVerifier.create(adapter.obtener("usuario-1", "factura", "../secreto.pdf"))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}
