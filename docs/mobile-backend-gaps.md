# Brechas del backend para las historias de la app Android (Productor)

- **Última actualización:** 2026-10-04
- **Backend revisado:** `develop` en la etiqueta **0.26.0** más el cambio pendiente de `feature/bbi-thresholds-campaign-year-range` (cambios desde la 0.21.0 en la sección 4)
- **Fuentes:** reporte (US y escenarios BDD), mockups de Figma (`Viora202602_Mobile_App`, sección App Productor · Kotlin) y el código del backend
- **Para qué sirve:** que cada desarrollador sepa qué le falta al backend para completar las historias que tiene asignadas en la app, antes de empezar la pantalla, y dar seguimiento a los endpoints que requiere la app (sección 3).

**Leyenda.** ✅ listo · 🟡 parcial (sirve, pero falta algo que el diseño usa) · 🔴 falta. **Tamaño:** S (horas), M (1-2 días), L (varios días).

## 1. Resumen por historia

| US | Dueño en la app | Pantallas | Backend hoy | Qué falta | Tamaño | Quién |
|---|---|---|---|---|---|---|
| **US09** Delimitar lote | Victor | P20–P26 | ✅ `POST /plots`, `GET /plots`, `GET /plots/{id}` | Opcional: fecha de registro y método de marcado (el diseño dice "registrado hoy con GPS"). El cupo del plan (P25) depende de suscripción (US06–US08, Sprint 3) | S | Victor |
| **US10** Editar lote | Victor | P27–P29 | ✅ `PUT /plots/{id}` con `If-Match` (nombre, marco, poda, polígono) | ✅ `PUT /plots/{id}` acepta `variety` opcional (si se omite, se conserva). Sin restricción por muestreos o cosechas: el modelo de calibre se autocalibra con los datos del productor | — | Resuelto en `feature/plot-variety-edit` |
| **US11** Baja de lote | Victor | P28, archivados | ✅ `DELETE /plots/{id}` (baja suave). ✅ **Listar archivados** (0.22.0): `GET /plots?status=REMOVED_SOFT_DELETE` | Un lote archivado no se puede abrir por id (404), alcanza con los datos de la lista. ✅ **Restaurar** (`POST /plots/{id}/restore`, rama `feature/plot-restore`): vuelve a ACTIVE con su historial; 409 si el lote no está archivado | — | — |
| **US13** Alta de nodo virtual | Diana | P85–P88 | ✅ `POST /plots/{id}/iot-devices` | — | — | — |
| **US14** Inventario de nodos | Diana | P85 | ✅ `GET /plots/{id}/iot-devices` (estado y última lectura) | — | — | — |
| **US15** Calibrar nodo | Diana | P87 | ✅ `PUT .../iot-devices/{id}` (multiplicador, textura, profundidad) | 🟡 No se puede **renombrar** el nodo (el reporte lo menciona) | S | Diana |
| **US16** Desvincular nodo | Diana | P88 | ✅ `DELETE .../iot-devices/{id}` | — | — | — |
| **US17** Series de telemetría | Diana | P90, P91 | ✅ `GET /plots/{id}/telemetries?startDate&endDate` (lecturas crudas) | 🟡 Sin agregación por ventanas (24 h, 7 d, 30 d): la app recibiría todas las lecturas. Decidir si agrega la app o el backend (`resolution`). El viento actual no viene en la telemetría (solo en el pronóstico) | M | Diana |
| **US18** Alertas automáticas | Fabrizio | T14, T15 | 🔴 **No existe** el contexto de alertas | Modelo de alerta (tipo, severidad crítica / atención / normalizada, lote, ventana horaria, métrica y umbral, serie de valores, "qué hacer", fuente del dato, normalización); reglas (estrés hídrico, golpe de calor / umbral térmico, humedad normalizada); `GET /alerts` (por lote y estado, con conteo de activas) y `GET /alerts/{id}`. Push queda para el Sprint 2 o 3 | **L** | Fabrizio |
| **US19** Pronóstico 7 días | Piero | P90, Home | ✅ `GET /plots/{id}/forecasts` (máx., mín., prob. de lluvia, viento, `isFrostRisk`) | 🟡 El estado del cielo ("Soleado") no viene: se deriva en la app. Confirmar que siempre entrega 7 días y su `syncedAt` | S | Piero |
| **US20** Cosechas históricas y BBI | Jahat | P40, P41 | ✅ `POST/GET /plots/{id}/harvest-records` y `GET .../metrics?name=BBI` (404 si no hay historial; con menos de 3 campañas `sampleSufficiency = INSUFFICIENT`). ✅ Bandas del BBI alineadas con el diseño: `REGULAR` < 0.20, `MODERATE_ALTERNATION` 0.20–0.40, `SEVERE_ALTERNATION` > 0.40. ✅ El año de campaña que se registra va de 2000 al año en curso (las campañas futuras se rechazan) | — | — | — |
| **US21** Rectificar o eliminar cosecha | Jahat | P41 | ✅ `PUT` (rectificar) y ✅ **`DELETE /plots/{id}/harvest-records/{recordId}`** (0.22.0; `If-Match` opcional, recalcula el BBI y la campaña se puede volver a registrar) | — | — | — |
| **US22** Frío acumulado (Erez) | Piero | P80, P81 | ✅ `GET .../metrics?name=CHILLING`: valor, categoría, modelo, umbral, % de avance, inicio de temporada (`seasonStart`), fecha de completado (`completionDate`), días sin acumular (`idleDays`) y estado de temporada (`seasonState`) | Resuelto en 0.23.0 (`phenology`). Incluye `ChillSeasonDetails` y cálculo dinámico de temporada | — | Piero |
| **US23** Anomalía térmica / ENOS | Piero | T15 invierno cálido | 🔴 **No existe** | Regla de detección (máximas semanales sobre el umbral durante días seguidos), señal en la métrica de frío y alerta que se integra con US18 ("qué cambia": frío frenado, floración en riesgo, carga esperada reajustada 9,8 → 7,4 t/ha, que depende de un rendimiento potencial, ver US26) | **L** | Piero, tras el contrato de alertas |
| **US24** Muestreo en campo | Fabrizio | P50–P54 | ✅ `POST /plots/{id}/samplings` (idempotente con `clientBatchId`; `trunkDiameterMm` obligatorio, como en el diseño) | — | — | — |
| **US25** Representatividad e historial | Fabrizio | P50, P54 | ✅ `GET /plots/{id}/samplings` (resumen) y `?view=detailed` (árboles) | — | — | — |
| **US26** Carga frutal sostenible | Victor | P61 | 🟡 La prescripción trae `targetFruitsPerMeter` y `percentageToRemove` | Carga estimada actual (frutos por metro del muestreo), estado óptima / moderada / sobrecarga antes de aclarear y rendimiento potencial (t/ha estimadas y sostenibles). **No hay fórmula de rendimiento definida**: es una decisión de producto. Además el diseño muestra frutos por árbol y el backend trabaja por metro | M y decisión | Victor y el equipo |
| **US27** Prescripción de aclareo | Victor | P60, P61 | ✅ `GET /plots/{id}/thinning-prescriptions` (porcentaje, `windowClosesOn`, `windowOpen`, estado) | 🟡 Falta `windowOpensOn` (el diseño muestra "Desde 3 nov · hasta 30 nov"; el servicio de fenología ya recibe la fecha de inicio pero no se guarda) | S–M | Victor |
| **US28** Confirmar el aclareo | Victor | P62, P63 | ✅ `POST /thinning-prescriptions/{id}/execution-confirmations` (carga resultante y proyección de calibre) | — | — | — |
| **US29** Cierre de campaña | Jahat | P70–P73 | ✅ `POST /plots/{id}/harvest-settlements` y ✅ `GET /plots/{id}/harvest-settlements` + `GET /plots/{id}/harvest-settlements/{campaignYear}` (todas las liquidaciones, de la campaña más reciente a la más antigua, y la de una campaña) | — | — | Jahat |

## 2. Lo que necesita el Home (armazón de Victor)

| Pieza del Home | Backend hoy | Qué falta |
|---|---|---|
| Fase del año (Reposo invernal, Floración, Cuajado, Cosecha) | 🔴 No existe | Decidir si la app la deriva por calendario o si el backend la expone |
| Tarjeta de cada lote (muestreo 3 de 5, vecería, plan de aclareo) | 🟡 Se arma con 3 llamadas por lote (resumen de muestreo, BBI, prescripción) | Opcional: `GET /plots/{id}/overview` para hacerlo en una sola llamada |
| Campana de alertas con conteo | 🔴 Depende de US18 | — |
| Clima y humedad del suelo | ✅ Telemetría y pronóstico | Ver US17 y US19 |

## 3. Seguimiento por desarrollador

Qué endpoints necesita cada quien para sus historias. ✅ listo para consumir · 🟡 usable con una brecha · 🔴 bloqueado por el backend.

### Victor · Lotes, aclareo y armazón del Home (US09, US10, US11, US26, US27, US28)

| Endpoint | US | Estado |
|---|---|---|
| `POST /plots`, `GET /plots`, `GET /plots/{id}`, `PUT /plots/{id}` (`If-Match`, `variety` opcional) | US09, US10 | ✅ |
| `DELETE /plots/{id}?reason=`, `GET /plots?status=REMOVED_SOFT_DELETE`, `POST /plots/{id}/restore` | US11 | ✅ (app terminada en 0.5.0) |
| `GET /plots/{id}/thinning-prescriptions` | US27 | 🟡 falta `windowOpensOn` |
| `POST /thinning-prescriptions/{id}/execution-confirmations` | US28 | ✅ |
| Carga actual y rendimiento potencial | US26 | 🔴 falta la fórmula (decisión de producto) |
| `GET /plots/{id}/samplings`, `.../metrics?name=BBI`, `.../thinning-prescriptions` (tarjeta del lote) | Home | ✅ en 3 llamadas; `overview` opcional |
| Fase del año, conteo de alertas | Home | 🔴 la fase está por decidir; las alertas dependen de US18 |

### Fabrizio · Alertas y luego muestreo (US18, US24, US25)

| Endpoint | US | Estado |
|---|---|---|
| `GET /alerts` (por lote y estado, con conteo de activas) y `GET /alerts/{id}` | US18 | 🔴 el contexto de alertas no existe (L). Primero el contrato, porque Piero (US23) y Victor (Home) dependen de él |
| `POST /plots/{id}/samplings` (idempotente con `clientBatchId`, `trunkDiameterMm` obligatorio) | US24 | ✅ |
| `GET /plots/{id}/samplings` y `?view=detailed` | US25 | ✅ |

### Piero · Frío y pronóstico (US22, US23, US19)

| Endpoint | US | Estado |
|---|---|---|
| `GET /plots/{id}/metrics?name=CHILLING` | US22 | ✅ resuelto: incluye valor, categoría, `seasonStart`, `completionDate`, `idleDays` y `seasonState` |
| Señal de anomalía térmica / ENOS en la métrica de frío y alerta | US23 | 🔴 no existe (L); se enchufa al contrato de alertas de US18 |
| `GET /plots/{id}/forecasts` | US19 | ✅ 🟡 el estado del cielo se deriva en la app; confirmar que siempre entrega 7 días |

### Diana · Telemetría: nodos y clima del lote (US13–US17)

| Endpoint | US | Estado |
|---|---|---|
| `POST /plots/{id}/iot-devices` | US13 | ✅ |
| `GET /plots/{id}/iot-devices` | US14 | ✅ |
| `PUT .../iot-devices/{id}` (calibrar) | US15 | 🟡 no permite renombrar el nodo (S) |
| `DELETE .../iot-devices/{id}` | US16 | ✅ |
| `GET /plots/{id}/telemetries?startDate&endDate` | US17 | 🟡 lecturas crudas, sin ventanas 24 h / 7 d / 30 d; el viento actual no viene (M) |

### Jahat · Fenología y cosecha (US20, US21, US29)

| Endpoint | US | Estado |
|---|---|---|
| `POST/GET /plots/{id}/harvest-records`, `GET .../metrics?name=BBI` | US20 | ✅ el BBI usa las bandas del diseño (`REGULAR` < 0.20, `MODERATE_ALTERNATION` 0.20–0.40, `SEVERE_ALTERNATION` > 0.40) y la cosecha solo se registra de 2000 al año en curso |
| `PUT` y `DELETE /plots/{id}/harvest-records/{recordId}` | US21 | ✅ (el DELETE llegó en la 0.22.0) |
| `POST /plots/{id}/harvest-settlements` | US29 | ✅ |
| Consultar liquidaciones hechas (`GET /plots/{id}/harvest-settlements` y `GET /plots/{id}/harvest-settlements/{campaignYear}`) | US29 | ✅ los dos endpoints; queda pendiente la asignación dentro del equipo mobile |
| `POST /plots/{id}/certifications` (dossier) | fuera de las 4 pestañas del productor | ✅ |

## 4. Cambios del backend desde la 0.21.0 (para actualizar el reporte)

| Release | Rama | Cambio | US |
|---|---|---|---|
| 0.22.0 | `feature/harvest-record-delete-and-plot-status-filter` | `DELETE /plots/{plotId}/harvest-records/{recordId}` | US21 |
| 0.22.0 | igual | `GET /plots?status=ACTIVE\|REMOVED_SOFT_DELETE` | US11 |
| 0.22.0 | igual | Estados reales del lote documentados; este documento | todas |
| 0.23.0 | `feature/plot-variety-edit` | `PUT /plots/{plotId}` acepta `variety` opcional | US10 |
| 0.24.0 | `feature/plot-restore` | `POST /plots/{plotId}/restore` | US11 |
| 0.24.0 | igual | Corrección: las respuestas de PUT y DELETE devolvían la `revision` anterior | US10, US11 |
| 0.27.0 | `feature/bbi-thresholds-campaign-year-range` | Bandas del BBI alineadas con el diseño de "¿Qué es el BBI?" | US20 |
| 0.27.0 | igual | El año de campaña que se registra va de 2000 al año en curso | US20 |

**0.22.0**
1. **`DELETE /api/v1/plots/{plotId}/harvest-records/{recordId}`** (US21, escenario 2): quita el registro, recalcula el BBI y la clasificación sobre las campañas válidas que quedan, y deja volver a registrar esa campaña. `If-Match` opcional. Respuesta 200 con `MessageResource` (no 204); también 400 (UUID o `If-Match` inválido), 404 y 412.
2. **`GET /api/v1/plots?status=`** (US11): `ACTIVE` (por defecto) o `REMOVED_SOFT_DELETE`. Con `updatedSince` el delta devuelve todos los estados y `status` lo acota. Un `status` desconocido da 400.
3. **Documentación corregida:** el estado público de un lote archivado es `REMOVED_SOFT_DELETE`; el OpenAPI decía `INACTIVE`, que no existe. `GET /plots/{id}` de un lote archivado da 404.

**0.23.0**
4. **`PUT /api/v1/plots/{plotId}` acepta `variety`** (CRIOLLA, SEVILLANA, MANZANILLA, ARBEQUINA) para corregir la variedad. Si se omite se conserva. Cuerpo: `{name, rowSpacingM, treeSpacingM, lastPruningDate?, polygonGeoJson, variety?}`.

**0.24.0**
5. **`POST /api/v1/plots/{plotId}/restore`** (US11): trae de vuelta un lote archivado con su historial y su nombre (un archivado sigue reservando su nombre, así que restaurarlo nunca choca con otro). 200 con el lote (`status` ACTIVE y `revision` +1), 404 si no existe o es de otro productor, 409 si no está archivado.
6. **Corrección:** `PlotRepositoryImpl.save` devolvía la entidad antes del flush de `@Version`, y el PUT y el DELETE respondían con la `revision` anterior; el cliente que la guardaba recibía 412 en el siguiente `If-Match`. Ahora usa `saveAndFlush`. El mismo patrón podría existir en los repositorios de otros agregados (sin revisar). Un PUT con datos idénticos no sube la revisión.

**0.27.0**
7. **Bandas del BBI según el diseño** (US20, sheet "¿Qué es el BBI?"): `REGULAR` por debajo de 0.20, `MODERATE_ALTERNATION` de 0.20 a 0.40 inclusive y `SEVERE_ALTERNATION` por encima de 0.40 (antes 0.25 / 0.50). Los nombres de las categorías no cambian, así que la app no se rompe; solo cambia el texto de `qualitativeCategory` en `GET /plots/{id}/metrics?name=BBI` y qué producción cae en cada banda.
8. **Rango del año de campaña al registrar una cosecha** (US20): la regla vive ahora en el dominio (`HarvestCampaignYearPolicy`), que acepta de 2000 al año en curso y rechaza con 400 las campañas futuras. Antes `POST /plots/{id}/harvest-records` aceptaba 1980–2100. El `CampaignYear` compartido sigue siendo la guarda estructural de 1980–2100 porque la liquidación necesita años futuros.

**Para el reporte**
- Baja de lote: `DELETE /plots/{id}?reason=` da 200 con `MessageResource`; la restauración es un `POST .../restore` aparte.
- Agregar `variety` al contrato `UpdatePlot` y los estados `ACTIVE | REMOVED_SOFT_DELETE` a `PlotResource`.
- Las cosechas históricas ya tienen CRUD completo (POST, GET, PUT, DELETE).
- Las bandas del BBI y el rango del año de campaña ya son los del diseño (0.20 / 0.40 y 2000–año en curso); la app puede leer `qualitativeCategory` sin dejar de mapear a ningún lado.
- Ya estaban en la 0.21.0, no son parte de este cambio: resumen de muestreos, prescripción activa, confirmación de ejecución, liquidación de campaña y certificación del dossier.

## 5. Orden sugerido en el backend

1. **Alertas (US18)**: es lo más grande y lo que bloquea el centro de alertas, el Home y la alerta de invierno cálido. Primero el contrato (campos y estados), para que Piero y Victor trabajen contra él.
2. **Frío (US22)** y luego **anomalía térmica (US23)**, que se enchufa a las alertas.
3. **`windowOpensOn` y carga actual (US26, US27)** para el plan del lote.
4. **Series y agregación (US17)** y **pronóstico (US19)**.
5. Detalles pequeños: renombrar nodo (US15), fecha y método de registro del lote (US09).

## 6. Decisiones pendientes

- ¿Quién calcula las **series de 24 h, 7 d y 30 d**: la app o el backend?
- ¿Hay una fórmula de **rendimiento potencial** (t/ha)? La necesitan US26 y la alerta de invierno cálido.
- ¿La **fase del año** la calcula la app o el backend?
