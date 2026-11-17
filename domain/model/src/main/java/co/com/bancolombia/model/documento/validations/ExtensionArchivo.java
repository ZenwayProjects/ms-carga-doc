package co.com.bancolombia.model.documento.validations;

import java.util.Locale;
import java.util.Set;

public final class ExtensionArchivo {

    private static final Set<String> PERMITIDAS = Set.of("txt", "xls", "xlsx", "pdf");
    private static final String MENSAJE = "Formato de archivo no válido [%s]. Solo se permiten: txt, xls, xlsx, pdf";

    private ExtensionArchivo() {
    }

    public static void validar(String nombreArchivo) {
        if (!esPermitida(nombreArchivo)) {
            throw new IllegalArgumentException(String.format(MENSAJE, nombreArchivo));
        }
    }

    public static boolean esPermitida(String nombreArchivo) {
        String extension = extension(nombreArchivo);
        return extension != null && PERMITIDAS.contains(extension);
    }

    private static String extension(String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isBlank()) {
            return null;
        }
        String nombre = nombreArchivo.trim();
        int punto = nombre.lastIndexOf('.');
        if (punto <= 0 || punto == nombre.length() - 1) {
            return null;
        }
        return nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
    }
}
