package co.com.bancolombia.filesystem;

import co.com.bancolombia.filesystem.config.StorageProperties;
import co.com.bancolombia.model.documento.Documento;
import co.com.bancolombia.model.documento.DocumentoResultado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class FileSystemAdapterTest {

    @TempDir
    Path tempDir;

    private FileSystemAdapter adapter;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        properties.setBasePath(tempDir.toString());
        adapter = new FileSystemAdapter(properties);
    }

    @Test
    void debeGuardarListarYObtenerUnArchivo() {
        byte[] contenido = "contenido-factura".getBytes(StandardCharsets.UTF_8);
        Documento documento = Documento.builder()
                .usuarioId("usuario-1")
                .categoria("factura")
                .nombreOriginal("Factura Noviembre.pdf")
                .contentType("application/pdf")
                .tamanio(contenido.length)
                .contenido(contenido)
                .build();

        AtomicReference<DocumentoResultado> resultado = new AtomicReference<>();
        StepVerifier.create(adapter.almacenar(Flux.just(documento)))
                .assertNext(resultado::set)
                .verifyComplete();

        assertThat(resultado.get().isExitoso()).isTrue();
        Path archivoGuardado = Path.of(resultado.get().getRutaAlmacenada());
        assertThat(archivoGuardado).exists();
        assertThat(archivoGuardado.getParent()).isEqualTo(tempDir.resolve("usuario-1").resolve("factura"));
        String nombreAlmacenado = archivoGuardado.getFileName().toString();
        assertThat(nombreAlmacenado).endsWith("-Factura_Noviembre.pdf");

        StepVerifier.create(adapter.listar("usuario-1", "factura"))
                .assertNext(listado -> {
                    assertThat(listado.getNombreAlmacenado()).isEqualTo(nombreAlmacenado);
                    assertThat(listado.getNombreOriginal()).isEqualTo("Factura_Noviembre.pdf");
                    assertThat(listado.getTamanio()).isEqualTo(contenido.length);
                    assertThat(listado.getContenido()).isNull();
                    assertThat(listado.getFechaCarga()).isNotNull();
                })
                .verifyComplete();

        StepVerifier.create(adapter.obtener("usuario-1", "factura", nombreAlmacenado))
                .assertNext(recuperado -> {
                    assertThat(recuperado.getNombreOriginal()).isEqualTo("Factura_Noviembre.pdf");
                    assertThat(recuperado.getContenido()).containsExactly(contenido);
                    assertThat(recuperado.getRutaAlmacenada()).contains("usuario-1", "factura");
                })
                .verifyComplete();
    }

    @Test
    void debeRetornarVacioCuandoElArchivoNoExiste() {
        StepVerifier.create(adapter.obtener("usuario-1", "factura", "no-existe.pdf"))
                .verifyComplete();
    }

    @Test
    void debeRechazarRutasConTraversal() {
        StepVerifier.create(adapter.obtener("usuario-1", "factura", "../secreto.pdf"))
                .expectError(IllegalArgumentException.class)
                .verify();

        StepVerifier.create(adapter.listar("..", "factura"))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}
