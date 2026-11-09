# Semana 1 — Replay y fundamentos de Reactor

**Objetivo:** que el flujo actual (subir archivo a S3 con jerarquía) sea tuyo, no de la IA.
**Tiempo:** 2 sesiones de ~2 h.

## Rama base
La rama con WebFlux + S3 es **`feature/webflux-migration`** (no `main`).
`main` está en `v2` y no tiene WebFlux ni S3.

## Paso 0 — Commitear el trabajo actual
Antes de practicar, la jerarquía de keys debe estar commiteada en la rama base:

```bash
git checkout feature/webflux-migration
git add applications domain infrastructure test_stress.sh
git commit -m "S3 hierarchical keys by user and category"
```
> Si no quieres versionar `docs/` todavía, omítelo. Lo importante es el código.

## Paso 1 — Crear la rama de práctica
```bash
git checkout feature/webflux-migration
git checkout -b learning/replay
```

> Red de seguridad: los originales están en `feature/webflux-migration`.
> Restaurar: `git checkout feature/webflux-migration -- <ruta>`.

## Sesión A (2 h) — Reproducir
- [ ] Lee solo los nombres de archivos, no el contenido: `DocumentoController`, `S3Adapter`, `S3Operations`, `S3Config`.
- [ ] Dibuja en papel el flujo: request → controller → use case → gateway → adapter → S3.
- [ ] Implementa de memoria un endpoint `POST /api/documentos/usuarios/{usuarioId}/{categoria}/carga-masiva` que suba a `usuarios/{usuarioId}/{categoria}/{uuid}.{ext}`.
- [ ] Compáralo con el código real (solo al final):
  ```bash
  git show feature/webflux-migration:infrastructure/driven-adapters/s3-repository/src/main/java/co/com/bancolombia/s3/adapter/S3Adapter.java
  git diff feature/webflux-migration -- infrastructure/entry-points/api-rest/src/main/java/co/com/bancolombia/api/DocumentoController.java
  ```
- [ ] Anota diferencias en `learning-log.md`.
- [ ] Commit: `learning: replay subida S3`.

## Sesión B (2 h) — Entender Reactor
- [ ] Micro-lección: `Mono` vs `Flux`, `map` vs `flatMap`, `subscribeOn` vs `publishOn`.
- [ ] Responde por escrito: ¿por qué el adapter usa `publishOn(boundedElastic)` y `subscribeOn(boundedElastic)`?
- [ ] Escribe un test mínimo con `StepVerifier` (agrega `reactor-test` si falta).
- [ ] Crea 10 tarjetas Anki desde `glossary.md`.
- [ ] Commit: `learning: reactor basics + anki`.

## Entregable
- Rama `learning/replay` con el flujo rehecho.
- `learning-log.md` con la comparación y las respuestas.
- 10 tarjetas Anki.

## Criterio de autonomía
- Puedes dibujar el flujo de memoria.
- Puedes explicar `Mono`/`Flux` y los schedulers sin notas.
- Identificaste al menos 2 diferencias entre tu replay y el código real.

## Repaso programado
- Día 1: relee tu explicación de schedulers.
- Día 3: rehaz el key jerárquico de memoria.
- Día 7: explica el flujo a alguien (o a la IA) y pide huecos.
