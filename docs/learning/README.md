# Learning workspace

Este directorio es tu "segundo cerebro" para volverte autónomo en proyectos como `ms-carga-doc`.
El proyecto es el dojo; lo que buscas es competencia transferible.

## Rama base
El trabajo de WebFlux + S3 vive en **`feature/webflux-migration`** (no `main`).
La rama de práctica es `learning/replay`, creada desde esa base.

## Reglas de oro
- **Aprender haciendo:** cada semana una tarea real del proyecto.
- **Recuperación antes que relectura:** cierra el editor y reproduce de memoria.
- **Repaso espaciado:** 1, 3, 7, 14 y 30 días.
- **Intercalado:** mezcla Reactor, tests, S3 y arquitectura.
- **IA como revisora, no como conductora:** intenta 20 minutos antes de preguntar.

## Cadencia (3-5 h/semana)
- 2 sesiones de ~2 h o 3 de ~80 min.
- 1 revisión semanal de 30 min.
- Si no terminas una semana, la repites. No acumules deuda.

## Plantilla de sesión (80 min)
1. 5 min: recall (Anki / explicar lo anterior sin mirar).
2. 15 min: ejemplo mínimo (docs o un test existente).
3. 40 min: implementar sin mirar (práctica deliberada).
4. 10 min: escribir el test.
5. 10 min: journal + commit + explain-back.

## Archivos
- `learning-log.md`: qué hiciste, qué te costó, próximos pasos.
- `bug-journal.md`: bugs, causa raíz y lecciones.
- `autonomy-checklist.md`: evidencia de que puedes hacerlo solo.
- `glossary.md`: conceptos para Anki.
- `decisions/`: ADRs (por qué tomaste cada decisión).
- `week-01.md`: plan detallado de la primera semana.

## Cómo usar la IA sin depender de ella
1. Intenta 20 min.
2. Revisa el error y la documentación.
3. Pide una **pista**, no la solución.
4. Si te dan código, bórralo y rehazlo de memoria.
5. Pide **revisión** y preguntas, no respuestas.

## Criterio de graduación
- Implementas endpoint + adapter + tests nuevos sin referencias.
- Depuras con logs/tests, no adivinando.
- Justificas decisiones por escrito (ADR).
- Explicas el flujo en 3 minutos sin notas.
