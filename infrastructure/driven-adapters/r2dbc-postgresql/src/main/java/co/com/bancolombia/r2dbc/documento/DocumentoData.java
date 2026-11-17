package co.com.bancolombia.r2dbc.documento;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("documentos")
public class DocumentoData {
    @Id
    @Column("nombre_almacenado")
    private String nombreAlmacenado;
    @Column("usuario_id")
    private String usuarioId;
    @Column("categoria")
    private String categoria;
    @Column("nombre_original")
    private String nombreOriginal;
    @Column("content_type")
    private String contentType;
    @Column("tamanio")
    private Long tamanio;
    @Column("ruta_almacenada")
    private String rutaAlmacenada;
    @Column("fecha_carga")
    private LocalDateTime fechaCarga;
}
