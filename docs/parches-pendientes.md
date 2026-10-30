# Parches pendientes

Observaciones que ya existían antes de la rama que las encontró. No bloquean esa rama; se corrigen en un parche aparte. Al resolver una, se borra de esta lista y se cita el commit que la cerró.

Rutas relativas a `src/main/java/com/arcadiadevs/viora/platform/` (código) y `src/test/java/com/arcadiadevs/viora/platform/` (pruebas).

## 3. `PlotAccessPolicy` deja pasar los lotes que no encuentra

- **Encontrado en:** `feature/assistant-question-catalog` (2026-10-30), señalado por la revisión nativa como advertencia.
- **Qué pasa:** `orchard/application/acl/PlotAccessPolicy.java` devuelve `true` cuando el id es nulo, mal formado o no existe. La idea es que cada endpoint responda su propio 400 o 404, pero la autorización queda abierta por defecto: si un endpoint olvida validar la existencia del lote, la ruta queda sin control de dueño.
- **Parche propuesto:** evaluar si los casos de id mal formado o inexistente pueden resolverse antes (por ejemplo, rechazar el formato en la política) sin cambiar los códigos que ya devuelven las rutas. Mientras tanto, todo endpoint nuevo con `plotId` debe validar existencia y dueño en su servicio.
- **Evaluación (2026-10-30):** hoy la política devuelve `true` para un id nulo, mal formado o inexistente, y cada ruta responde su propio código: un id mal formado da **400** `VALIDATION_ERROR` y uno inexistente da **404** `PLOT_NOT_FOUND`; solo un lote ajeno da **403** `USER_FORBIDDEN`. Si la política rechazara los ids mal formados o inexistentes, Spring lanzaría `AccessDeniedException` y las 15 clases de controladores que usan `@plotAccess` pasarían a responder **403** en los tres casos: la app ya no distinguiría "el lote no existe" de "no es tuyo", y habría que cambiar las pruebas que esperan 400 o 404 (por ejemplo, `PlotDataOwnershipIntegrationTest.plotFilteredListsAnswerNotFoundForAnUnknownPlot`). El id nulo tiene que seguir pasando, porque las listas con filtro opcional no lo envían. **Recomendación:** no rechazar en la política mientras la app use esos códigos. El riesgo de olvidar el control ya lo cubre `PlotRouteAuthorizationTest`, que desde esta rama exige `@plotAccess` en toda ruta con `plotId` en la ruta o como parámetro de consulta. Si se quiere cerrar la política, que lance la excepción de recurso no encontrado para un id inexistente (sigue en 404) y deje el formato a cada endpoint. Sigue pendiente.

## 5. Umbrales agroclimáticos distintos a los del informe

- **Encontrado en:** la revisión del informe del proyecto (`ma-viora-report`, especificación de requisitos), 2026-10-30.
- **Qué pasa:** `telemetry/domain/services/AgroclimaticThresholdEvaluator.java` usa valores que no coinciden con los criterios de aceptación:
  - **Calor en floración (US18):** el informe pide más de 32 °C con humedad relativa menor a 20 % durante más de 3 horas seguidas. El código avisa con ≥ 32 °C de máxima diaria, sin humedad ni duración, y marca crítico con ≥ 36 °C (ver ADR-006, sección 5).
  - **Helada:** el informe (diseño táctico, `isFrostRisk`) define riesgo con mínima pronosticada menor a 2,0 °C. El código avisa con ≤ 1,5 °C (`FROST_WARNING_CELSIUS`) y marca crítico con ≤ −2,0 °C.
- **Por qué no se corrigió:** cambiar los umbrales cambia cuándo se abren las alertas que ya ven los productores. Es una decisión agronómica y de negocio, no técnica.
- **Parche propuesto:** que el equipo y el agrónomo decidan los valores; después, llevarlos a propiedades con estado `SYNTHETIC_DEMO` / `AGRONOMIST_APPROVED` y alinear informe y código. El asistente lee siempre los umbrales de Telemetría, así que no necesita cambios.

## 6. La fecha de muestreo se valida con la zona del servidor

- **Encontrado en:** `feature/pending-patches` (2026-10-30), al corregir las pruebas del punto 4.
- **Qué pasa:** `@PastOrPresent` en `TreeSampleResourceItem.samplingDate` (contexto `thinning`) compara con la zona horaria de la JVM, no con `America/Lima`. Con un servidor en una zona atrasada respecto de Lima, la fecha de hoy en Lima se rechaza como futura (400). En Render (UTC) no ocurre, porque UTC va adelante de Lima.
- **Parche propuesto:** validar la fecha en el dominio con `ProducerTimeZone.ZONE` (o el `Clock` inyectado) en lugar de `@PastOrPresent`, o fijar la zona de la JVM en el despliegue.

## 7. Otras pruebas siguen calculando la fecha en UTC

- **Encontrado en:** `feature/pending-patches` (2026-10-30).
- **Qué pasa:** `AgronomicDossierControllerIntegrationTest`, `AgronomicReportCertificationControllerIntegrationTest`, `HarvestSettlementControllerIntegrationTest` y `ThinningExecutionControllerIntegrationTest` usan `LocalDate.now(ZoneOffset.UTC)`. No fallaron en las corridas hechas, pero no se probaron en la franja de 19:00 a 00:00 de Lima.
- **Parche propuesto:** el mismo del punto 4 (`ffba942`): calcular la fecha con `ProducerTimeZone.ZONE`.

## Resueltos

- 1\. El 401 no responde con ProblemDetail — `5f3aad8`.
- 2\. Respuesta distinta ante un lote ajeno en las listas filtradas por `plotId` — `6981e45` (ahora ambas responden 403). Efecto aceptado: el 403 de `/agroclimatic-incidents` pasa de `PLOT_FORBIDDEN` ("Solo el productor titular del lote puede ver sus alertas.") a `USER_FORBIDDEN` ("Acceso denegado"), igual que todas las rutas `/plots/{plotId}/…`. La app solo lee los campos estándar del ProblemDetail (`ApiErrorMapper.kt`), así que no se rompe nada; también cambia el orden: un lote ajeno con un filtro inválido responde 403 antes que 400.
- 4\. Seis pruebas fallan entre las 19:00 y las 00:00 de Lima — `ffba942`.
