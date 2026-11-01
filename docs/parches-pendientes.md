# Parches pendientes

Observaciones que ya existían antes de la rama que las encontró. No bloquean esa rama; se corrigen en un parche aparte. Al resolver una, se borra de esta lista y se cita el commit que la cerró.

Rutas relativas a `src/main/java/com/arcadiadevs/viora/platform/` (código) y `src/test/java/com/arcadiadevs/viora/platform/` (pruebas).

Resueltos en `feature/backend-pending-patches` (2026-10-30): el 401 sin ProblemDetail (4afea97), la respuesta distinta ante un lote ajeno en las listas filtradas por `plotId` (a38003c), `PlotAccessPolicy` que deja pasar lotes que no encuentra (3c2fa16, con una prueba que exige 404 en toda ruta de un lote inexistente) y las seis pruebas que fallaban entre las 19:00 y las 00:00 de Lima (9a70b25).

## 1. `GET /plots/{plotId}/iot-devices` responde 200 con lista vacía para un lote inexistente

- **Encontrado en:** `feature/backend-pending-patches` (2026-10-30), por `shared/interfaces/rest/security/UnknownPlotRoutesIntegrationTest`.
- **Qué pasa:** `telemetry/interfaces/rest/controllers/IoTDeviceController.listIoTDevices` consulta `IoTDeviceQueryServiceImpl.handle(GetIoTDevicesByPlotIdQuery)`, que solo busca dispositivos por `plotId` y nunca pregunta si el lote existe. Las demás rutas de un lote inexistente responden 404 (`PLOT_NOT_FOUND`); esta responde `200 []`, que se lee como "lote sin sensores".
- **Qué no es:** no filtra datos ajenos. La ruta tiene `@plotAccess` a nivel de clase, y calibrar y desactivar comprueban que el dispositivo pertenezca al lote (`IoTDeviceCommandServiceImpl`, líneas 126 y 171).
- **Parche propuesto:** que el servicio de consulta compruebe el lote con `ExternalOrchardService.existsActivePlot` (como `TelemetryQueryServiceImpl`) y devuelva `Result` con `ApplicationError.notFound("Plot", …)`. Decidir antes qué responde un lote archivado: hoy `[]`; con `existsActivePlot` sería 404. La app no abre sensores de lotes archivados, pero conviene decidirlo. Al corregirlo, quitar la ruta de `ANSWERS_200_FOR_NOW` en `UnknownPlotRoutesIntegrationTest`.

## 2. Umbrales agroclimáticos distintos a los del informe

- **Encontrado en:** la revisión del informe del proyecto (`ma-viora-report`, especificación de requisitos), 2026-10-30.
- **Qué pasa:** `telemetry/domain/services/AgroclimaticThresholdEvaluator.java` usa valores que no coinciden con los criterios de aceptación:
  - **Calor en floración (US18):** el informe pide más de 32 °C con humedad relativa menor a 20 % durante más de 3 horas seguidas. El código avisa con ≥ 32 °C de máxima diaria, sin humedad ni duración, y marca crítico con ≥ 36 °C (ver ADR-006, sección 5).
  - **Helada:** el informe (diseño táctico, `isFrostRisk`) define riesgo con mínima pronosticada menor a 2,0 °C. El código avisa con ≤ 1,5 °C (`FROST_WARNING_CELSIUS`) y marca crítico con ≤ −2,0 °C.
- **Por qué no se corrigió:** cambiar los umbrales cambia cuándo se abren las alertas que ya ven los productores. Es una decisión agronómica y de negocio, no técnica.
- **Parche propuesto:** que el equipo y el agrónomo decidan los valores; después, llevarlos a propiedades con estado `SYNTHETIC_DEMO` / `AGRONOMIST_APPROVED` y alinear informe y código. El asistente lee siempre los umbrales de Telemetría, así que no necesita cambios.

## 3. La fecha de muestreo se valida con la zona del servidor

- **Encontrado en:** `feature/pending-patches` (2026-10-30).
- **Qué pasa:** `@PastOrPresent` en `TreeSampleResourceItem.samplingDate` (contexto `thinning`) compara con la zona horaria de la JVM, no con `America/Lima`. Con un servidor en una zona atrasada respecto de Lima, la fecha de hoy en Lima se rechaza como futura (400). En Render (UTC) no ocurre, porque UTC va adelante de Lima.
- **Parche propuesto:** validar la fecha en el dominio con `ProducerTimeZone.ZONE` (o el `Clock` inyectado) en lugar de `@PastOrPresent`, o fijar la zona de la JVM en el despliegue.

## 4. Otras pruebas siguen calculando la fecha en UTC

- **Encontrado en:** `feature/pending-patches` (2026-10-30).
- **Qué pasa:** `AgronomicDossierControllerIntegrationTest`, `AgronomicReportCertificationControllerIntegrationTest`, `HarvestSettlementControllerIntegrationTest` y `ThinningExecutionControllerIntegrationTest` usan `LocalDate.now(ZoneOffset.UTC)`. No fallaron en las corridas hechas, pero no se probaron en la franja de 19:00 a 00:00 de Lima.
- **Parche propuesto:** el mismo que cerró las seis pruebas de la franja de Lima (9a70b25): calcular la fecha con `ProducerTimeZone.ZONE`.
