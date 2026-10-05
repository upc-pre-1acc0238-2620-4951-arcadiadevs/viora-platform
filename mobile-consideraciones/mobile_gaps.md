# Brechas del backend para las historias de la app Android (Productor)

- **Fecha:** 2026-10-03
- **Backend revisado:** `develop` en la etiqueta 0.21.0, más la rama `feature/harvest-record-delete-and-plot-status-filter`
- **Fuentes:** reporte (US y escenarios BDD), mockups de Figma (`Viora202602_Mobile_App`, sección App Productor · Kotlin) y el código del backend (`viora-platform`)
- **Para qué sirve:** que cada desarrollador sepa qué le falta al backend para completar las historias que tiene asignadas en la app, antes de empezar la pantalla.

**Leyenda:** ✅ listo · 🟡 parcial (sirve, pero falta algo que el diseño usa) · 🔴 falta.  
**Tamaño:** S (horas), M (1-2 días), L (varios días).

---

## 1. Resumen por Historia de Usuario

| US | Dueño en la app | Pantallas | Backend hoy | Qué falta | Tamaño | Quién |
|---|---|---|---|---|---|---|
| **US09** Delimitar lote | Victor | P20–P26 | ✅ `POST /plots`, `GET /plots`, `GET /plots/{id}` | Opcional: fecha de registro y método de marcado (el diseño dice "registrado hoy con GPS"). El cupo del plan (P25) depende de suscripción (US06–US08, Sprint 3) | S | Victor |
| **US10** Editar lote | Victor | P27–P29 | ✅ `PUT /plots/{id}` con `If-Match` (nombre, marco, poda, polígono) | ✅ `PUT /plots/{id}` acepta `variety` opcional (si se omite, se conserva). Sin restricción por muestreos o cosechas: el modelo de calibre se autocalibra con los datos del productor | — | Resuelto en `feature/plot-variety-edit` |
| **US11** Baja de lote | Victor | P28, archivados | ✅ `DELETE /plots/{id}` (baja suave). ✅ **Listar archivados** (esta rama): `GET /plots?status=REMOVED_SOFT_DELETE` | Un lote archivado no se puede abrir por id (404), alcanza con los datos de la lista. ✅ **Restaurar** (`POST /plots/{id}/restore`, rama `feature/plot-restore`): vuelve a ACTIVE con su historial; 409 si el lote no está archivado | — | — |
| **US13** Alta de nodo virtual | Diana | P85–P88 | ✅ `POST /plots/{id}/iot-devices` | — | — | — |
| **US14** Inventario de nodos | Diana | P85 | ✅ `GET /plots/{id}/iot-devices` (estado y última lectura) | — | — | — |
| **US15** Calibrar nodo | Diana | P87 | ✅ `PUT .../iot-devices/{id}` (multiplicador, textura, profundidad) | 🟡 No se puede **renombrar** el nodo (el reporte lo menciona) | S | Diana |
| **US16** Desvincular nodo | Diana | P88 | ✅ `DELETE .../iot-devices/{id}` | — | — | — |
| **US17** Series de telemetría | Diana | P90, P91 | ✅ `GET /plots/{id}/telemetries?startDate&endDate` (lecturas crudas) | 🟡 Sin agregación por ventanas (24 h, 7 d, 30 d): la app recibiría todas las lecturas. Decidir si agrega la app o el backend (`resolution`). El viento actual no viene en la telemetría (solo en el pronóstico) | M | Diana |
| **US18** Alertas automáticas | Fabrizio | T14, T15, Home | ✅ `GET /agroclimatic-incidents`, `GET /plots/{id}/agroclimatic-incidents`, `GET .../agroclimatic-incidents/{id}`, `POST .../postponements`, `PUT .../mitigation-steps/{stepId}` | — (ADR-011: delimitado a telemetría; incluye checklist interactivo de "Qué hacer" adaptativo por día/hora e i18n `Accept-Language`, contadores para la campana y tendencia semanal de 7 días) | — | Resuelto en `feature/telemitry-agroclimatic-incidents` |
| **US19** Pronóstico 7 días | Piero | P90, Home | ✅ `GET /plots/{id}/forecasts` (máx., mín., prob. de lluvia, viento, `isFrostRisk`) | 🟡 El estado del cielo ("Soleado") no viene: se deriva en la app. Confirmar que siempre entrega 7 días y su `syncedAt` | S | Piero |
| **US20** Cosechas históricas y BBI | Jahat | P40, P41 | ✅ `POST/GET /plots/{id}/harvest-records` y `GET .../metrics?name=BBI` (404 si no hay historial; con menos de 3 campañas `sampleSufficiency = INSUFFICIENT`) | — | — | — |
| **US21** Rectificar o eliminar cosecha | Jahat | P41 | ✅ `PUT` (rectificar) y ✅ **`DELETE /plots/{id}/harvest-records/{recordId}`** (esta rama; `If-Match` opcional, recalcula el BBI y la campaña se puede volver a registrar) | — | — | — |
| **US22** Frío acumulado (Erez) | Piero | P80, P81 | 🟡 `GET .../metrics?name=CHILLING`: valor, categoría, modelo, umbral y % de avance | Inicio de temporada ("Inicio · 1 jun"), fecha de completado ("Frío completo el 18 de agosto"), días sin acumular (estado "frenado") y estado de temporada (en curso / completa / fuera de temporada) | M | Piero |
| **US23** Anomalía térmica / ENOS | Piero | T15 invierno cálido | 🔴 **No existe** | Regla de detección (máximas semanales sobre el umbral durante días seguidos), señal en la métrica de frío y alerta que se integra con US18 ("qué cambia": frío frenado, floración en riesgo, carga esperada reajustada 9,8 → 7,4 t/ha, que depende de un rendimiento potencial, ver US26) | **L** | Piero, tras el contrato de alertas |
| **US24** Muestreo en campo | Fabrizio | P50–P54 | ✅ `POST /plots/{id}/samplings` (idempotente con `clientBatchId`; `trunkDiameterMm` obligatorio, como en el diseño) | — | — | — |
| **US25** Representatividad e historial | Fabrizio | P50, P54 | ✅ `GET /plots/{id}/samplings` (resumen) y `?view=detailed` (árboles) | — | — | — |
| **US26** Carga frutal sostenible | Victor | P61 | 🟡 La prescripción trae `targetFruitsPerMeter` y `percentageToRemove` | Carga estimada actual (frutos por metro del muestreo), estado óptima / moderada / sobrecarga antes de aclarear y rendimiento potencial (t/ha estimadas y sostenibles). **No hay fórmula de rendimiento definida**: es una decisión de producto. Además el diseño muestra frutos por árbol y el backend trabaja por metro | M y decisión | Victor y el equipo |
| **US27** Prescripción de aclareo | Victor | P60, P61 | ✅ `GET /plots/{id}/thinning-prescriptions` (porcentaje, `windowClosesOn`, `windowOpen`, estado) | 🟡 Falta `windowOpensOn` (el diseño muestra "Desde 3 nov · hasta 30 nov"; el servicio de fenología ya recibe la fecha de inicio pero no se guarda) | S–M | Victor |
| **US28** Confirmar el aclareo | Victor | P62, P63 | ✅ `POST /thinning-prescriptions/{id}/execution-confirmations` (carga resultante y proyección de calibre) | — | — | — |
| **US29** Cierre de campaña | Jahat | P70–P73 | ✅ `POST /plots/{id}/harvest-settlements` y ✅ `GET /plots/{id}/harvest-settlements` + `GET /plots/{id}/harvest-settlements/{campaignYear}` | La consulta ya está en el backend; falta asignar la pantalla de la Bitácora ("Cosecha asentada"), sin dueño por ahora | — | Pendiente de asignar |

---

## 2. Lo que necesita el Home (Armazón de Victor)

| Pieza del Home | Backend hoy | Qué falta |
|---|---|---|
| **Fase del año** (Reposo invernal, Floración, Cuajado, Cosecha) | 🔴 No existe | Decidir si la app la deriva por calendario o si el backend la expone |
| **Tarjeta de cada lote** (muestreo 3 de 5, vecería, plan de aclareo) | 🟡 Se arma con 3 llamadas por lote (resumen de muestreo, BBI, prescripción) | Opcional: `GET /plots/{id}/overview` para hacerlo en una sola llamada |
| **Campana de alertas con conteo** | ✅ Resuelto en US18 (`GET /api/v1/agroclimatic-incidents`) | Ninguna. Entrega `summary.activeCount` y `summary.criticalCount` para el badge del Home |
| **Clima y humedad del suelo** | ✅ Telemetría y pronóstico | Ver US17 y US19 |

---

## 3. Hecho en la rama `feature/harvest-record-delete-and-plot-status-filter`

1. **`DELETE /api/v1/plots/{plotId}/harvest-records/{recordId}`** (US21, escenario 2): quita el registro, recalcula el BBI sobre las campañas válidas que quedan y deja volver a registrar esa campaña. `If-Match` opcional. Respuestas: 200, 400 (`If-Match` malformado), 404 y 412.
2. **`GET /api/v1/plots?status=`** (US11): `ACTIVE` (por defecto) o `REMOVED_SOFT_DELETE` para listar los archivados. Con `updatedSince` el delta sigue devolviendo todos los estados, y `status` lo acota.
3. **Documentación corregida:** el estado público de un lote archivado es `REMOVED_SOFT_DELETE`; el OpenAPI decía `INACTIVE`, que no existe.
4. **`POST /api/v1/plots/{id}/restore`** (US11): trae de vuelta un lote archivado, con su historial y su nombre (un lote archivado sigue reservando su nombre, así que restaurarlo nunca choca con otro). 200 con el lote (`status` ACTIVE y `revision` +1), 404 si no existe o es de otro productor, 409 si el lote no está archivado.

---

## 3.1. Hecho en la rama `feature/telemitry-agroclimatic-incidents` (US18 Alertas Agroclimáticas y Mitigación)

1. **Delimitación estricta a Telemetría (ADR-011):**
   - El módulo de alertas gestiona exclusivamente incidentes originados en telemetría y meteorología: `HYDRIC_STRESS`, `HEAT_WAVE`, `FROST_WARNING`.
   - Las anomalías de otros BCs (sobrecarga frutal de `thinning`, déficit de frío de `phenology`) se mantienen en sus contextos respectivos y se despachan mediante Notificaciones Push con Deep Links hacia sus pantallas nativas (ej. P60).
2. **`GET /api/v1/agroclimatic-incidents?plotId&status&severity` (T14 · Centro de Alertas y Badge en Home):**
   - Objeto `summary` con conteos para el badge de la campana: `activeCount`, `criticalCount`, `warningCount`, `normalizedCount`.
   - Lista de `incidents` con proyección de lectura enriquecida con `plotName` y `plotVariety` (vía ACL Orchard) para evitar llamadas adicionales desde la app.
3. **`GET /api/v1/plots/{plotId}/agroclimatic-incidents`:**
   - Listado de incidentes filtrado por lote.
4. **`GET /api/v1/agroclimatic-incidents/{incidentId}` (T15 · Detalle de Alerta y Checklist interactivo):**
   - **Checklist adaptativo de mitigación (`mitigationSteps`):**
     - Cada paso expone: `id`, `instructionKey`, `instruction`, `completed` y `completedAt`.
     - `instruction` resuelve dinámicamente el texto contextual relativo al día/hora del evento (ej. para ola de calor: *"Riega el miércoles por la tarde"*, *"Riega el jueves antes de las 9 a. m."*, *"No podes ni aclares ese día"*) a partir del `triggeredAt` de la alerta y la zona horaria del huerto (`America/Lima`).
     - **Soporte de Internacionalización (i18n):** Respeta la cabecera `Accept-Language` (`es` / `en`) entregando el texto ya traducido (ej. *"Irrigate on Wednesday afternoon"* vs *"Riega el miércoles por la tarde"*).
   - **Tendencia semanal (`weeklyTrend`):**
     - Array de puntos cronológicos de los últimos 7 días con `{ timestamp, value, threshold }` para graficar directamente la curva y la línea de umbral en la app móvil.
   - **Metadatos completos:**
     - Severidad (`CRITICAL`, `WARNING`), Estado (`ACTIVE`, `SNOOZED`, `NORMALIZED`), `headlineKey`, métrica (`currentValue`, `thresholdValue`, `unit`), `triggeredAt`, `dateFormatted`, `timeWindow`, `stressDurationMinutes`, `snoozedUntil`.
5. **`POST /api/v1/agroclimatic-incidents/{incidentId}/postponements` (Silenciar / Postergar):**
   - Permite posponer la alerta (`{ "durationHours": 6 }`), cambiando el estado a `SNOOZED` y actualizando `snoozedUntil`.
6. **`PUT /api/v1/agroclimatic-incidents/{incidentId}/mitigation-steps/{stepId}` (Checklist interactivo):**
   - Marca una tarea de la checklist como completada (`completed: true`), actualiza `completedAt` y emite `MitigationStepCompletedEvent`.

---

## 4. Orden Sugerido en el Backend

1. ~~**Alertas (US18)**~~: ✅ **COMPLETADO al 100%** en `feature/telemitry-agroclimatic-incidents`. Los contratos, endpoints, i18n, contadores de Home y checklist T15 están listos.
2. **Frío (US22)** y luego **anomalía térmica (US23)**, que se enchufa a las alertas.
3. **`windowOpensOn` y carga actual (US26, US27)** para el plan del lote.
4. **Series y agregación (US17)** y **pronóstico (US19)**.
5. **Detalles pequeños**: renombrar nodo (US15), fecha y método de registro del lote (US09).

---

## 5. Decisiones Pendientes

- ¿Quién calcula las **series de 24 h, 7 d y 30 d**: la app o el backend?
- ¿Hay una fórmula de **rendimiento potencial** (t/ha)? La necesitan US26 y la alerta de invierno cálido.
- ¿La **fase del año** la calcula la app o el backend?
- ¿Se asigna a alguien la **consulta de liquidaciones** (US29)? El backend ya la entrega (`GET /plots/{id}/harvest-settlements` y `GET /plots/{id}/harvest-settlements/{campaignYear}`); lo que falta es la asignación dentro del equipo mobile.
