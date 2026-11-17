package co.com.bancolombia.model.documento;

import co.com.bancolombia.model.documento.validations.ExtensionArchivo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExtensionArchivoTest {

    @Test
    void debeAceptarLasExtensionesPermitidas() {
        List.of("vt1.txt", "factura.xls", "factura.xlsx", "factura.pdf", "FACTURA.PDF")
                .forEach(nombre -> assertThatCode(() -> ExtensionArchivo.validar(nombre))
                        .doesNotThrowAnyException());
    }

    @Test
    void debeRechazarExtensionesNoPermitidas() {
        List.of("documento.docx", "archivo.exe", "archivo", "archivo.", ".pdf")
                .forEach(nombre -> assertThatThrownBy(() -> ExtensionArchivo.validar(nombre))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Formato de archivo no válido")
                        .hasMessageContaining("txt, xls, xlsx, pdf"));
    }

    @Test
    void debeRechazarNombreNuloOVacio() {
        assertThatThrownBy(() -> ExtensionArchivo.validar(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Formato de archivo no válido");
        assertThatThrownBy(() -> ExtensionArchivo.validar("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Formato de archivo no válido");
    }
}
