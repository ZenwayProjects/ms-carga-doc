# Architecture Decision Records (ADR)

Un ADR documenta **por qué** tomaste una decisión técnica. Es lo que separa "funciona" de "entiendo por qué funciona".

## Convención
- Nombre: `ADR-001-titulo-corto.md`, `ADR-002-...`
- Empieza copiando `ADR-000-template.md`.
- Un ADR por decisión relevante.

## ADRs sugeridos para este proyecto
- ADR-001: guardar documentos en S3 en vez de filesystem.
- ADR-002: jerarquía de keys `usuarios/{usuarioId}/{categoria}/{uuid}.{ext}`.
- ADR-003: `usuarioId`/`categoria` por path variables (sin DB aún).
- ADR-004: elección de PostgreSQL + R2DBC (cuando llegues a la Fase 2).
- ADR-005: usuario derivado del token JWT (cuando llegues a la Fase 3).

## Regla
Escríbelo **tú**. La IA solo revisa al final.
