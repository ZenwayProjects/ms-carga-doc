package co.com.bancolombia.r2dbc.documento;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface DocumentoReactiveRepository extends ReactiveCrudRepository<DocumentoData, String> {

    @Modifying
    @Query("INSERT INTO documentos (nombre_almacenado, usuario_id, categoria, nombre_original, "
            + "content_type, tamanio, ruta_almacenada, fecha_carga) "
            + "VALUES (:nombreAlmacenado, :usuarioId, :categoria, :nombreOriginal, "
            + ":contentType, :tamanio, :rutaAlmacenada, :fechaCarga) "
            + "ON CONFLICT (nombre_almacenado) DO NOTHING")
    Mono<Integer> insertarSiNoExiste(
            @Param("nombreAlmacenado") String nombreAlmacenado,
            @Param("usuarioId") String usuarioId,
            @Param("categoria") String categoria,
            @Param("nombreOriginal") String nombreOriginal,
            @Param("contentType") String contentType,
            @Param("tamanio") Long tamanio,
            @Param("rutaAlmacenada") String rutaAlmacenada,
            @Param("fechaCarga") LocalDateTime fechaCarga);
}
