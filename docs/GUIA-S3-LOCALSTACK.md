# Bloc de notas — Cargar documentos a S3 con LocalStack

## 0. Qué vas a aprender
- Cómo se conecta una app Spring WebFlux a S3 usando AWS SDK v2.
- Cómo emular AWS localmente con LocalStack.
- Cómo encaja esto en Clean Architecture (gateway + adapter).
- Cómo se guardan y se recuperan archivos usando "carpetas" en S3.
- Cómo probar todo con curl y JMeter.

---

## 1. Conceptos base (léelos primero)

| Concepto | Qué es |
|---|---|
| **Clean Architecture** | Separar dominio (reglas), casos de uso (orquestación), entry-points (HTTP) y driven-adapters (infra: S3, FS, DB). |
| **Gateway** | Interfaz definida en el dominio (ej. `AlmacenamientoGateway`). El dominio no sabe si guardas en disco o en S3. |
| **Adapter** | Implementación del gateway en infraestructura (`S3Adapter`, `FileSystemAdapter`). |
| **S3** | Almacenamiento de objetos. Tiene **bucket** (contenedor) y **objetos** (archivos). |
| **Key** | El "nombre" del objeto dentro del bucket. Ej: `usuarios/id_123/facturas/abc.pdf`. |
| **Prefijo (prefix)** | S3 **no tiene carpetas reales**; los `/` en el key se muestran como carpetas. |
| **LocalStack** | Emulador local de servicios AWS (S3 incluido) en `http://127.0.0.1:4566`. |
| **multipart/form-data** | Formato HTTP para subir archivos. |
| **Flux / Mono** | Tipos reactivos: `Flux` = 0..N elementos, `Mono` = 0..1. |

---

## 2. Arquitectura del proyecto
```
ms-carga-doc/
├── domain/
│   ├── model/        → Documento, DocumentoResultado, AlmacenamientoGateway
│   └── usecase/      → CargarDocumentosMasivamenteUseCase
├── infrastructure/
│   ├── entry-points/
│   │   └── api-rest/ → DocumentoController (endpoint HTTP)
│   └── driven-adapters/
│       ├── file-system/   → FileSystemAdapter (guarda en disco)
│       └── s3-repository/ → S3Adapter, S3Operations, S3Config (guarda en S3)
└── applications/
    └── app-service/  → main, application.yaml, une todo
```

**Regla de oro:** las dependencias de infraestructura van en el `build.gradle` del adapter, **nunca** en domain/usecase.

---

## 3. Flujo de una subida (entiéndelo de memoria)
```
Cliente (curl/JMeter)
   │  POST multipart con archivos + usuarioId/categoria en la URL
   ▼
DocumentoController            (entry-point)  → convierte FilePart a Documento
   ▼
CargarDocumentosMasivamenteUseCase (dominio)  → orquesta
   ▼
AlmacenamientoGateway          (interfaz dominio)
   ▼
S3Adapter                      (infra)        → arma el key y sube
   ▼
S3Operations → S3Client → LocalStack (S3)
   ▼
DocumentoResultado (éxito/fallo + rutaAlmacenada)
```

---

## 4. Preparar el entorno
```bash
# Java 21 y Gradle wrapper ya están en el repo
java -version
./gradlew.bat -v

# Levantar LocalStack con persistencia (conserva datos al reiniciar)
lstk start --persist

# Ver estado
lstk status
```
- LocalStack escucha en `127.0.0.1:4566` (¡solo IPv4!).
- Sin `--persist`, al reiniciar se borra todo.

---

## 5. Cómo se creó el adapter S3
El adapter se generó con la tarea del plugin de Clean Architecture:
```bash
./gradlew.bat generateDrivenAdapter --type=S3 --name=s3-repository
```
Luego se conectó:
- `s3-repository/build.gradle`: `implementation project(':model')` + `reactor-core`.
- `app-service/build.gradle`: `implementation project(':s3-repository')`.

---

## 6. Configurar S3
### 6.1 `S3ConnectionProperties` (record)
```java
@ConfigurationProperties(prefix = "adapters.aws.s3")
public record S3ConnectionProperties(String endpoint, String region,
                                     String bucketName, String keyPrefix) {}
```

### 6.2 `S3Config` (perfil `local`)
Puntos clave:
- `endpointOverride(URI.create("http://127.0.0.1:4566"))`
- `forcePathStyle(true)` → necesario en LocalStack.
- Credenciales de prueba `test/test`.
- Checksums `WHEN_REQUIRED` → evita errores con LocalStack.
- Timeouts cortos.

### 6.3 `application.yaml`
```yaml
spring:
  profiles:
    active: local
storage:
  type: s3
adapters:
  aws:
    s3:
      endpoint: http://127.0.0.1:4566
      region: us-east-1
      bucketName: documentos
      key-prefix: usuarios
```

---

## 7. Cómo se elige el adapter (S3 vs disco)
Cada adapter se activa por propiedad:
```java
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")   // S3Adapter
@ConditionalOnProperty(name = "storage.type", havingValue = "fs", matchIfMissing = true) // FileSystemAdapter
```
Cambias `storage.type: fs` o `s3` y la app usa uno u otro **sin tocar código**.

---

## 8. El bug que nos tomó tiempo (apréndelo)
**Síntoma:** `reactor.netty.channel.AbortedException: Connection has been closed`.

**Causa:** `localhost` resolvía primero a IPv6 (`::1`), pero LocalStack solo escucha en IPv4 (`127.0.0.1:4566`).

**Solución:** usar `http://127.0.0.1:4566` en el endpoint (no `localhost`).

> Lección: en conexiones locales, prefiere la IP explícita.

---

## 9. Cómo se organizan los archivos (carpetas en S3)
S3 **no crea carpetas**: solo nombras el key con `/`. El adapter arma:
```
s3://documentos/usuarios/{usuarioId}/{categoria}/{uuid}.{ext}
```
Ejemplo:
```
s3://documentos/usuarios/id_123/facturas/550e8400-e29b-41d4-a716-446655440000.pdf
```
- `usuarioId` y `categoria` llegan como **path variables** del request.
- El prefijo `usuarios` es configurable con `adapters.aws.s3.key-prefix`.
- Cada segmento se **sanitiza** (rechaza `/`, `\`, `..` y vacíos).

### Endpoint
```
POST /api/documentos/usuarios/{usuarioId}/{categoria}/carga-masiva
```
Categorías típicas: `facturas`, `actas`, `cedulas`, etc. (una por request).

---

## 10. Probar la subida
```bash
# 1) Correr la app (el bucket se crea solo en perfil local)
./gradlew.bat :app-service:bootRun

# 2) Subir un archivo
curl -X POST "http://127.0.0.1:8080/api/documentos/usuarios/id_123/facturas/carga-masiva" \
  -F "files=@C:/Software/proyectos/poc/ms-carga-doc-test-data/vt1.txt"

# 3) Ver objetos del usuario/categoría
curl -s "http://127.0.0.1:4566/documentos?list-type=2&prefix=usuarios/id_123/facturas/" \
     -H "Host: s3.localhost.localstack.cloud:4566"
```
Respuesta esperada:
```json
{"total":1,"exitosos":1,"fallidos":0,
 "resultados":[{"nombreOriginal":"vt1.txt","exitoso":true,
 "rutaAlmacenada":"s3://documentos/usuarios/id_123/facturas/<uuid>.txt"}]}
```

### Descargar un archivo
```bash
curl "http://127.0.0.1:4566/documentos/usuarios/id_123/facturas/<uuid>.txt" \
     -H "Host: s3.localhost.localstack.cloud:4566" -o archivo.txt
```

### JMeter / stress
- Plan: `CargaMasiva.jmx` (ya apunta a `127.0.0.1` y al endpoint jerárquico).
- Script: `test_stress.sh` (usa `USUARIO_ID` y `CATEGORIA`; por defecto `id_123` y `facturas`).
  ```bash
  USUARIO_ID=id_456 CATEGORIA=actas ./test_stress.sh
  ```
- Verifica que exista `C:\Software\proyectos\poc\ms-carga-doc-test-data\` con los `vtN.txt`.

---

## 11. Comandos del día a día
```bash
# Prender todo
lstk stop 2>/dev/null || true
lstk start --persist
cd /c/Software/proyectos/poc/ms-carga-doc
./gradlew.bat :app-service:bootRun

# Apagar
# Ctrl+C en la app, luego:
lstk stop
```

---

## 12. Errores comunes

| Error | Causa probable | Solución |
|---|---|---|
| `Connection has been closed` | `localhost` → IPv6 | usar `127.0.0.1` |
| `NoSuchBucket` | bucket no existe | `S3BucketInitializer` lo crea en local |
| Datos desaparecen al reiniciar | sin persistencia | `lstk start --persist` |
| `Connection refused :4566` | LocalStack apagado | `lstk start --persist` |
| JMeter no encuentra archivos | ruta sin `poc` | corregir ruta |
| `Port 8080 already in use` | app duplicada | cerrar IntelliJ o el jar |
| `usuarioId/categoria inválidos` | caracteres no permitidos | usar solo `A-Z a-z 0-9 . _ -` |

---

## 13. Ejercicios para practicar
1. Cambia `storage.type` a `fs` y verifica que guarda en disco.
2. Sube 3 archivos en un solo request y revisa `total/exitosos`.
3. Sube el mismo archivo como `actas` y `cedulas` y compara los keys.
4. Agrega un endpoint para listar objetos por prefijo usando `S3Operations.listObjects`.
5. Agrega un endpoint de descarga usando `S3Operations.getObject`.

---

## 14. Glosario rápido
- **Bucket**: contenedor de objetos en S3.
- **Object/Key**: archivo y su nombre/ruta dentro del bucket.
- **Prefix**: parte inicial del key; simula carpetas.
- **Endpoint**: URL del servicio S3 (LocalStack: `127.0.0.1:4566`).
- **Path style**: `http://host/bucket/key` en vez de `http://bucket.host/key` (obligatorio en LocalStack).
- **Gateway**: contrato del dominio hacia infraestructura.
