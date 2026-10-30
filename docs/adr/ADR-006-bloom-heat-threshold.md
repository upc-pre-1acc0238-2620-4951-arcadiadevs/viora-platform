# ADR-006: Umbral de calor en floración único, definido en Telemetría

- **Estado:** Aceptado
- **Fecha:** 2026-10-29
- **Requisitos:** US18 (alertas automáticas); pantallas T15 (Detalle de alerta) y P10 (Inicio en floración) de la app Kotlin; preguntas `alert.*` y `climate.*` del asistente
- **Relacionada:** ADR-003 (alcance de los incidentes agroclimáticos)

## 1. Contexto

Al preparar las respuestas del asistente sobre golpes de calor apareció el mismo umbral con valores distintos según el lugar:

| Dónde | Valor | Uso |
|---|---|---|
| Backend, `AgroclimaticThresholdEvaluator.evaluateHeatRisk` | 32 °C → `WARNING`, 36 °C → `CRITICAL` | Abre el incidente `HEAT_WAVE` con la máxima del pronóstico (`WeatherForecastIngestedEventHandler`) |
| Figma, T15 · Detalle de alerta | 32 °C ("umbral 32°") | Umbral de daño térmico dibujado en el gráfico de 7 días |
| App, `ForecastDay.EXTREME_HEAT_CELSIUS` | 32 °C | Calor extremo del pronóstico; `HeatLevel` lo documenta como umbral crítico de floración (US18) |
| App, `HomePhaseRules.BLOOM_HEAT_CELSIUS` | **30 °C** | Elige el mensaje de Inicio en floración ("regar temprano") |

Los "Umbral 30" que aparecen en Figma (P80, P81, P90, P91, P95) son **porciones de frío Erez** de la Sevillana, no grados, y no tienen relación con este umbral.

El problema: con una máxima de 30 a 31 °C, Inicio le pide al productor que riegue temprano, pero no se abre ninguna alerta ni la T15 muestra riesgo. La app se contradice, y el asistente, que debe responder "¿Qué hago ante este golpe de calor?", no tendría un valor único que leer.

## 2. Decisión

- **Fuente de verdad: el backend, contexto Telemetría.** Los umbrales son los que ya aplica `AgroclimaticThresholdEvaluator`: **32 °C abre el aviso de calor (`WARNING`)** y **36 °C lo eleva a crítico (`CRITICAL`)**. Coinciden con Figma y con la US18.
- **No hay un tercer nivel preventivo a 30 °C.** El 30 °C de Inicio no tiene respaldo en el diseño ni en la historia de usuario; Inicio pasa a usar 32 °C.
- **Los clientes no definen umbrales propios.** La app y el asistente leen el valor que publica Telemetría (el umbral ya viaja en `ThresholdBreachInfo` de cada incidente). Ningún otro contexto ni cliente vuelve a declarar la constante.
- **El asistente lee los umbrales a través de `TelemetryContextFacade`**, nunca con un literal, y los incluye en sus hechos (`threshold`), así el texto y las tarjetas dicen lo mismo que la alerta.

## 3. Dónde afecta

| Componente | Cambio | Responsable |
|---|---|---|
| Backend, Telemetría | Publicar los umbrales de calor como constantes del evaluador y exponerlos en `TelemetryContextFacade` para otros contextos | Backend (Jahat) |
| Backend, Asistente | Las preguntas `alert.*` y `climate.*` toman el umbral de la fachada | Backend (Jahat) |
| App, Inicio | `HomePhaseRules.BLOOM_HEAT_CELSIUS` pasa de 30 °C a 32 °C, o lee el valor del backend | Frontend (Victor) |
| App, pronóstico | Sin cambio de valor (`ForecastDay.EXTREME_HEAT_CELSIUS` ya es 32 °C) | — |

## 4. Consecuencias

- Inicio, el pronóstico, la T15 y el asistente avisan a partir del mismo valor.
- Inicio deja de avisar entre 30 y 32 °C. Si el equipo quiere un aviso más temprano, se agrega como un nivel nuevo en Telemetría, no como una constante en la app.
- Los valores 32 °C y 36 °C siguen **sin validar por un agrónomo** para la Sevillana. Cuando se validen, o si cambian, se ajustan en un solo lugar. Pasarlos a propiedades de configuración con estado `SYNTHETIC_DEMO` / `AGRONOMIST_APPROVED`, como los perfiles de aclareo, queda como mejora posterior.
