# Glosario / Anki

Escribe la explicación **con tus palabras**. Si no puedes, ese es el hueco.
Luego pásalo a Anki con repaso espaciado.

| Concepto | Mi explicación | Ejemplo en el proyecto |
|---|---|---|
| Gateway | | `AlmacenamientoGateway` |
| Adapter | | `S3Adapter`, `FileSystemAdapter` |
| `@ConditionalOnProperty` | | `storage.type=s3` vs `fs` |
| `Mono` vs `Flux` | | Subida de 1 vs N archivos |
| `flatMap` | | Procesar archivos en paralelo |
| `subscribeOn` vs `publishOn` | | `boundedElastic` en el adapter |
| S3 key / prefix | | `usuarios/id_123/facturas/<uuid>.txt` |
| `forcePathStyle` | | `S3Config` para LocalStack |
| `ProblemDetail` | | Manejo global de errores |
| Backpressure | | Streaming de archivos grandes |
| `StepVerifier` | | Tests de `Mono`/`Flux` |
| Testcontainers | | Postgres + LocalStack en tests |
| R2DBC | | Persistencia reactiva |
| Presigned URL | | Subida/descarga directa a S3 |
| ADR | | `decisions/` |
