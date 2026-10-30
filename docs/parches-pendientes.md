# Parches pendientes

Observaciones que ya existían antes de la rama que las encontró. No bloquean esa rama; se corrigen en un parche aparte. Al resolver una, se borra de esta lista y se cita el commit que la cerró.

Rutas relativas a `src/main/java/com/arcadiadevs/viora/platform/` (código) y `src/test/java/com/arcadiadevs/viora/platform/` (pruebas).

## 1. El 401 no responde con ProblemDetail

- **Encontrado en:** `feature/assistant-question-catalog` (2026-10-30), por el verificador con la cadena de seguridad real.
- **Qué pasa:** `iam/infrastructure/authorization/sfs/pipeline/UnauthorizedRequestHandlerEntryPoint.java:24` llama a `response.sendError(401, …)`. Spring devuelve su cuerpo de error por defecto (`{timestamp, status, error, path}`) en lugar de un ProblemDetail (RFC 7807) con clave i18n como el resto de errores del API.
- **Afecta:** todas las rutas protegidas. La app no puede tratar el 401 igual que los demás errores.
- **Parche propuesto:** escribir un ProblemDetail con `ErrorResponseAssembler` (o equivalente) en el entry point, con una clave i18n (`error.unauthorized`) y `Content-Type: application/problem+json`. Agregar una prueba con servidor real y JWT, porque las pruebas con MockMvc no pasan por la cadena de seguridad.

## 2. Respuesta distinta ante un lote ajeno en las listas filtradas por `plotId`

- **Encontrado en:** `feature/assistant-question-catalog` (2026-10-30).
- **Qué pasa:** dos endpoints reciben `plotId` como parámetro de consulta, sin `@PreAuthorize("@plotAccess.isAccessible(#plotId)")`, y validan al dueño dentro del servicio. No se filtran datos, pero responden distinto al mismo caso:
  - `GET /api/v1/agroclimatic-incidents?plotId=<lote ajeno>` (`telemetry/interfaces/rest/controllers/AgroclimaticIncidentController.java`) → **403** `PLOT_FORBIDDEN`.
  - `GET /api/v1/thinning-events?plotId=<lote ajeno>` (`thinning/interfaces/rest/controllers/ThinningEventController.java`) → **404** `PLOT_NOT_FOUND`.
- **Parche propuesto:** decidir una sola respuesta (403, como las rutas `/plots/{plotId}/…`) y aplicar `@PreAuthorize` en ambos. Después, ampliar la regla de `shared/interfaces/rest/security/PlotRouteAuthorizationTest.java`, que hoy solo exige el control en el paquete `assistant`, a todos los controladores con `plotId` como parámetro de consulta.

## 3. `PlotAccessPolicy` deja pasar los lotes que no encuentra

- **Encontrado en:** `feature/assistant-question-catalog` (2026-10-30), señalado por la revisión nativa como advertencia.
- **Qué pasa:** `orchard/application/acl/PlotAccessPolicy.java` devuelve `true` cuando el id es nulo, mal formado o no existe. La idea es que cada endpoint responda su propio 400 o 404, pero la autorización queda abierta por defecto: si un endpoint olvida validar la existencia del lote, la ruta queda sin control de dueño.
- **Parche propuesto:** evaluar si los casos de id mal formado o inexistente pueden resolverse antes (por ejemplo, rechazar el formato en la política) sin cambiar los códigos que ya devuelven las rutas. Mientras tanto, todo endpoint nuevo con `plotId` debe validar existencia y dueño en su servicio.

## 4. Seis pruebas fallan entre las 19:00 y las 00:00 de Lima

- **Encontrado en:** la preparación del entorno (2026-10-29).
- **Qué pasa:** las pruebas toman la fecha con `LocalDate.now(ZoneOffset.UTC)` y el código usa la zona del productor (`America/Lima`). Entre las 19:00 y las 00:00 de Lima ya es el día siguiente en UTC y fallan:
  - `iam/interfaces/rest/AccountDeletionIntegrationTest.deletesEverything`
  - `orchard/interfaces/rest/FieldNoteControllerIntegrationTest.{attachesAPhoto, listsNewestFirst, storesANoteOnce}`
  - `thinning/interfaces/rest/controllers/ThinningPrescriptionFlowIntegrationTest.confirmationEstimatesTheYieldAfterThinning`
  - `thinning/interfaces/rest/transform/PrescriptionResourceFromEntityAssemblerTest.windowIsOpenOnlyBetweenItsDates`
- **Parche propuesto:** que las pruebas calculen la fecha con `ProducerTimeZone.ZONE`, o mejor, que usen el `Clock` inyectado con una fecha fija, como hace el catálogo del asistente.
