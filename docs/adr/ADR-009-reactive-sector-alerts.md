# ADR-009: Alertas de sector reactivas a los eventos de la parcela

- **Estado:** Aceptado
- **Fecha:** 2026-11-10
- **Requisitos:** US31 y TS61

## 1. Contexto

US31 pide que el semáforo territorial refleje «de inmediato» la sobrecarga frutal mayor al 30 % o el riesgo de helada en las parcelas socias. Las alertas de sector solo se evaluaban en `SectorAlertScheduler`, cada 15 minutos, así que un muestreo con sobrecarga podía tardar hasta 15 minutos en llegar al centro de alertas del gestor.

El color de cada parcela no se guarda: `MemberPlotReader` lo calcula al leer, a partir de Thinning, Telemetry y Orchard. La evaluación de una licencia lee todas las parcelas de sus socios y puede esperar varios segundos la respuesta del pronóstico del clima.

## 2. Decisión

- `ReevaluateSectorAlertsOnPlotRiskChangedHandler` reevalúa la licencia del socio tras los eventos que cambian el color de una parcela:
  - de Thinning: `SamplingRoundCompletedEvent`, `SustainableCropLoadDeterminedEvent` y `ThinningExecutionConfirmedEvent`;
  - de Telemetry: `AgroclimaticIncidentRaisedEvent` y `AgroclimaticIncidentNormalizedEvent`;
  - de Orchard: `PlotUpdatedEvent`, `PlotRemovedEvent` y `PlotRestoredEvent`.
- El recorrido es: parcela → dueño activo → licencia que lo patrocina → `EvaluateSectorAlertsCommand`. Una parcela sin dueño activo, o un productor sin licencia, no evalúa nada.
- El handler escucha con `@TransactionalEventListener(phase = AFTER_COMMIT)` y `@Async`. Es el primer handler del repositorio que lo hace así; los demás usan `BEFORE_COMMIT` o `@EventListener`. Con este patrón:
  - un fallo al evaluar las alertas nunca revierte el muestreo, el incidente o la parcela del productor;
  - la evaluación, y la llamada al clima que incluye, no demoran la respuesta al productor;
  - el fallo queda en el log y el scheduler lo corrige en su siguiente ejecución.
- `SectorAlertEvaluator` serializa las evaluaciones de una misma licencia con un lock en memoria, retenido durante toda la transacción. El handler y el scheduler pasan por él. Sin el lock, dos evaluaciones simultáneas podían abrir dos alertas del mismo tipo en un sector, porque ninguna restricción de la base lo impide.
- El scheduler de 15 minutos se mantiene como red de seguridad para los cambios que no publican evento:
  - el pronóstico de helada, que se lee con un caché de 30 minutos;
  - el retiro del consentimiento y la salida de un socio;
  - la asignación o resolución del sector de una parcela;
  - las ventanas que se desplazan con el paso de los días.

## 3. Consecuencias

- El centro de alertas del gestor refleja la sobrecarga y los incidentes segundos después de registrarlos.
- El lock en memoria solo serializa una instancia de la plataforma, que es como se despliega en Render. Para escalar a varias instancias hay que cambiarlo por un lock en la base de datos.
- Una helada solo pronosticada sigue tardando hasta 15 minutos en abrir su alerta. Hacerla reactiva requiere un evento de pronóstico actualizado, que hoy no existe.
