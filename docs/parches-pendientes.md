# Parches pendientes

Observaciones que ya existían antes de la rama que las encontró. No bloquean esa rama; se corrigen en un parche aparte. Al resolver una, se borra de esta lista y se cita el commit que la cerró.

Rutas relativas a `src/main/java/com/arcadiadevs/viora/platform/` (código) y `src/test/java/com/arcadiadevs/viora/platform/` (pruebas).

Resueltos en `feature/backend-pending-patches` (2026-10-30): el 401 sin ProblemDetail (4afea97), la respuesta distinta ante un lote ajeno en las listas filtradas por `plotId` (a38003c), `PlotAccessPolicy` que deja pasar lotes que no encuentra (3c2fa16, con una prueba que exige 404 en toda ruta de un lote inexistente) y las seis pruebas que fallaban entre las 19:00 y las 00:00 de Lima (9a70b25).

## 1. `GET /plots/{plotId}/iot-devices` responde 200 con lista vacía para un lote inexistente

- **Encontrado en:** `feature/backend-pending-patches` (2026-10-30), por `shared/interfaces/rest/security/UnknownPlotRoutesIntegrationTest`.
- **Qué pasa:** `telemetry/interfaces/rest/controllers/IoTDeviceController.listIoTDevices` consulta `IoTDeviceQueryServiceImpl.handle(GetIoTDevicesByPlotIdQuery)`, que solo busca dispositivos por `plotId` y nunca pregunta si el lote existe. Las demás rutas de un lote inexistente responden 404 (`PLOT_NOT_FOUND`); esta responde `200 []`, que se lee como "lote sin sensores".
- **Qué no es:** no filtra datos ajenos. La ruta tiene `@plotAccess` a nivel de clase, y calibrar y desactivar comprueban que el dispositivo pertenezca al lote (`IoTDeviceCommandServiceImpl`, líneas 126 y 171).
- **Parche propuesto:** que el servicio de consulta compruebe el lote con `ExternalOrchardService.existsActivePlot` (como `TelemetryQueryServiceImpl`) y devuelva `Result` con `ApplicationError.notFound("Plot", …)`. Decidir antes qué responde un lote archivado: hoy `[]`; con `existsActivePlot` sería 404. La app no abre sensores de lotes archivados, pero conviene decidirlo. Al corregirlo, quitar la ruta de `ANSWERS_200_FOR_NOW` en `UnknownPlotRoutesIntegrationTest`.
