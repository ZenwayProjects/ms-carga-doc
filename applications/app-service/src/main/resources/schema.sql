CREATE TABLE IF NOT EXISTS documentos (
    nombre_almacenado VARCHAR(255) PRIMARY KEY,
    usuario_id VARCHAR(100) NOT NULL,
    categoria VARCHAR(100) NOT NULL,
    nombre_original VARCHAR(255) NOT NULL,
    content_type VARCHAR(150),
    tamanio BIGINT NOT NULL DEFAULT 0,
    ruta_almacenada TEXT NOT NULL,
    fecha_carga TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_documentos_usuario_categoria
    ON documentos (usuario_id, categoria);
