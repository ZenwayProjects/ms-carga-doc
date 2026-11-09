# Bug journal

Cada bug es una lección. Reproducirlo y entender la causa raíz vale más que copiar el fix.

## Plantilla
- **Fecha:**
- **Síntoma:** (mensaje de error, comportamiento observado)
- **Contexto:** (endpoint, archivo, entorno)
- **Hipótesis:** (qué creías que pasaba)
- **Cómo lo reproduje:**
- **Causa raíz:** (la razón real, no el síntoma)
- **Fix:**
- **Lección:**
- **Prevención:** (test, validación, configuración)

## Ejemplo resuelto: conexión cerrada contra LocalStack
- **Síntoma:** `reactor.netty.channel.AbortedException: Connection has been closed`.
- **Contexto:** `POST /api/documentos/carga-masiva` con `endpoint: http://localhost:4566`.
- **Hipótesis:** credenciales o bucket mal configurado.
- **Cómo lo reproduje:** llamada desde curl y revisión del log de LocalStack.
- **Causa raíz:** `localhost` resolvía primero a IPv6 (`::1`) y LocalStack solo escucha IPv4 (`127.0.0.1:4566`).
- **Fix:** usar `http://127.0.0.1:4566`.
- **Lección:** en conexiones locales, preferir IP explícita.
- **Prevención:** dejar `127.0.0.1` fijo en `application.yaml` y en los tests.
