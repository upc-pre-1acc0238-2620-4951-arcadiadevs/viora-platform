# Brechas del backend para las historias de la app Android (Productor)

- **Fecha:** 2026-10-03
- **Backend revisado:** `develop` en la etiqueta 0.21.0, más la rama `feature/harvest-record-delete-and-plot-status-filter`
- **Fuentes:** reporte (US y escenarios BDD), mockups de Figma (`Viora202602_Mobile_App`, sección App Productor · Kotlin) y el código del backend
- **Para qué sirve:** que cada desarrollador sepa qué le falta al backend para completar las historias que tiene asignadas en la app, antes de empezar la pantalla.

**Leyenda.** ✅ listo · 🟡 parcial (sirve, pero falta algo que el diseño usa) · 🔴 falta. **Tamaño:** S (horas), M (1-2 días), L (varios días).

## 1. Resumen por historia

| US | Dueño en la app | Pantallas | Backend hoy | Qué falta | Tamaño | Quién |
|---|---|---|---|---|---|---|
| **US09** Delimitar lote | Victor | P20–P26 | ✅ `POST /plots`, `GET /plots`, `GET /plots/{id}` | Opcional: fecha de registro y método de marcado (el diseño dice "registrado hoy con GPS"). El cupo del plan (P25) depende de suscripción (US06–US08, Sprint 3) | S | Victor |
| **US10** Editar lote | Victor | P27–P29 | ✅ `PUT /plots/{id}` con `If-Match` (nombre, marco, poda, polígono) | ✅ `PUT /plots/{id}` acepta `variety` opcional (si se omite, se conserva). Sin restricción por muestreos o cosechas: el modelo de calibre se autocalibra con los datos del productor | — | Resuelto en `feature/plot-variety-edit` |
| **US11** Baja de lote | Victor | P28, archivados | ✅ `DELETE /plots/{id}` (baja suave). ✅ **Listar archivados** (esta rama): `GET /plots?status=REMOVED_SOFT_DELETE` | Un lote archivado no se puede abrir por id (404), alcanza con los datos de la lista. No existe "restaurar" y el diseño no lo pide | — | — |
| **US13** Alta de nodo virtual | Diana | P85–P88 | ✅ `POST /plots/{id}/iot-devices` | — | — | — |
| **US14** Inventario de nodos | Diana | P85 | ✅ `GET /plots/{id}/iot-devices` (estado y última lectura) | — | — | — |
| **US15** Calibrar nodo | Diana | P87 | ✅ `PUT .../iot-devices/{id}` (multiplicador, textura, profundidad) | 🟡 No se puede **renombrar** el nodo (el reporte lo menciona) | S | Diana |
| **US16** Desvincular nodo | Diana | P88 | ✅ `DELETE .../iot-devices/{id}` | — | — | — |
| **US17** Series de telemetría | Diana | P90, P91 | ✅ `GET /plots/{id}/telemetries?startDate&endDate` (lecturas crudas) | 🟡 Sin agregación por ventanas (24 h, 7 d, 30 d): la app recibiría todas las lecturas. Decidir si agrega la app o el backend (`resolution`). El viento actual no viene en la telemetría (solo en el pronóstico) | M | Diana |
| **US18** Alertas automáticas | Fabrizio | T14, T15 | 🔴 **No existe** el contexto de alertas | Modelo de alerta (tipo, severidad crítica / atención / normalizada, lote, ventana horaria, métrica y umbral, serie de valores, "qué hacer", fuente del dato, normalización); reglas (estrés hídrico, golpe de calor / umbral térmico, humedad normalizada); `GET /alerts` (por lote y estado, con conteo de activas) y `GET /alerts/{id}`. Push queda para el Sprint 2 o 3 | **L** | Fabrizio |
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
| **US29** Cierre de campaña | Jahat | P70–P73 | ✅ `POST /plots/{id}/harvest-settlements` | 🟡 No se pueden **consultar** las liquidaciones hechas (la Bitácora muestra "Cosecha asentada"); sin dueño por ahora | S | Pendiente de asignar |

## 2. Lo que necesita el Home (armazón de Victor)

| Pieza del Home | Backend hoy | Qué falta |
|---|---|---|
| Fase del año (Reposo invernal, Floración, Cuajado, Cosecha) | 🔴 No existe | Decidir si la app la deriva por calendario o si el backend la expone |
| Tarjeta de cada lote (muestreo 3 de 5, vecería, plan de aclareo) | 🟡 Se arma con 3 llamadas por lote (resumen de muestreo, BBI, prescripción) | Opcional: `GET /plots/{id}/overview` para hacerlo en una sola llamada |
| Campana de alertas con conteo | 🔴 Depende de US18 | — |
| Clima y humedad del suelo | ✅ Telemetría y pronóstico | Ver US17 y US19 |

## 3. Hecho en la rama `feature/harvest-record-delete-and-plot-status-filter`

1. **`DELETE /api/v1/plots/{plotId}/harvest-records/{recordId}`** (US21, escenario 2): quita el registro, recalcula el BBI sobre las campañas válidas que quedan y deja volver a registrar esa campaña. `If-Match` opcional. Respuestas: 200, 400 (`If-Match` malformado), 404 y 412.
2. **`GET /api/v1/plots?status=`** (US11): `ACTIVE` (por defecto) o `REMOVED_SOFT_DELETE` para listar los archivados. Con `updatedSince` el delta sigue devolviendo todos los estados, y `status` lo acota.
3. **Documentación corregida:** el estado público de un lote archivado es `REMOVED_SOFT_DELETE`; el OpenAPI decía `INACTIVE`, que no existe.

## 4. Orden sugerido en el backend

1. **Alertas (US18)**: es lo más grande y lo que bloquea el centro de alertas, el Home y la alerta de invierno cálido. Primero el contrato (campos y estados), para que Piero y Victor trabajen contra él.
2. **Frío (US22)** y luego **anomalía térmica (US23)**, que se enchufa a las alertas.
3. **`windowOpensOn` y carga actual (US26, US27)** para el plan del lote.
4. **Series y agregación (US17)** y **pronóstico (US19)**.
5. Detalles pequeños: renombrar nodo (US15), consultar liquidaciones (US29), fecha y método de registro del lote (US09).

## 5. Decisiones pendientes

- ¿Quién calcula las **series de 24 h, 7 d y 30 d**: la app o el backend?
- ¿Hay una fórmula de **rendimiento potencial** (t/ha)? La necesitan US26 y la alerta de invierno cálido.
- ¿La **fase del año** la calcula la app o el backend?
- ¿Se asigna a alguien la **consulta de liquidaciones** (US29)?
