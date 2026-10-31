# Asistente Viora · contrato para implementar Spring AI

> Documento de traspaso para la próxima sesión (Claude Code). Se escribió el 2026-10-28 al cerrar el diseño de la sección **23 · Asistente Viora** en Figma. Léelo completo antes de tocar código. La revisión del 2026-10-29 (sección 0) cerró la mayoría de decisiones; lo que sigue abierto está en la sección 10.

- Figma: `https://www.figma.com/design/Hg6RBFx6HAqYACFcGwDtjU/Viora202602_Mobile_App`, página **Mobile Prototipo**, sección **"23 · Asistente Viora"** (id `1288:105225`, dentro de "App Productor · Kotlin"). Pantallas P110–P150 conectadas por prototipo.
- Repos: backend `D:\mov-viora-platform` (Spring Boot 4.1.1, Java 21, Maven, DDD por bounded context, paquete base `com.arcadiadevs.viora.platform`) y app `D:\mov-viora-mobile-android` (Kotlin/Compose, solo experiencia Productor).
- Estado de partida: backend 0.59.0 y app 1.16.0 publicados y probados. Piero hace el refactor DDD de la app en `feature/ddd-refactor`; coordina con él antes de crear el módulo del asistente en la app.
- **Responsables (desde 2026-10-29):** Jahat lleva el backend del asistente y decide sus puntos abiertos; Victor lleva las pantallas de la app. Las decisiones de la sección 0 reemplazan a las del resto del documento donde se contradigan.

---

## 0. Revisión del 2026-10-29 (backend)

| Tema | Decisión |
|---|---|
| Proveedor | **Groq** para desarrollo y pruebas (nivel gratuito, modelos abiertos pequeños y rápidos). Se usa con el starter OpenAI de Spring AI apuntando a la URL compatible de Groq. Producción se decide después de medir latencia, calidad y costo. Spring AI **2.0.2** (última estable en Maven Central, compatible con Boot 4.0/4.1). |
| Plan activo | **Obligatorio.** Todo usuario tiene plan, pero puede vencer: se valida con `SubscriptionContextFacade` y se responde 403. |
| Push del resumen | **Descartado** por ahora. |
| Streaming SSE | **Eliminado.** Enviar fragmentos antes de validar mostraría cifras que el validador puede rechazar después. El servidor valida el texto completo y responde JSON; la app muestra "pensando" y escribe el texto localmente con `VioraVoice`. Se elimina la rama `feature/assistant-streaming`. |
| Validador | Los hechos reales van en el prompt. El validador rechaza cualquier número del texto que no esté en los hechos. Refuerzos: reglas en el prompt (no redondear ni calcular), hechos interpretados (por ejemplo `irrigationAdvisable: false`) y frases prohibidas según el contexto. Si rechaza, se usa la plantilla. |
| Riego | `climate.irrigate` se **deshabilita** con `disabledReason` cuando el lote no tiene sondas reales. |
| Validación agronómica | Las plantillas y umbrales del asistente reutilizan el patrón de estado de los perfiles de aclareo (`SYNTHETIC_DEMO` / `AGRONOMIST_APPROVED`). No bloquea el desarrollo; sí bloquea producción. |
| Umbral de calor | **Decidido en [ADR-006](adr/ADR-006-bloom-heat-threshold.md).** Fuente única en Telemetría (`AgroclimaticThresholdEvaluator`): 32 °C abre el aviso (`WARNING`) y 36 °C lo eleva a crítico (`CRITICAL`), igual que Figma y la US18. Inicio de la app deja su 30 °C propio. El asistente lee los umbrales por `TelemetryContextFacade`. |
| Resumen del día | Se mantiene el job diario (Render pasará a plan pago) con generación bajo demanda como respaldo. |
| Modelo vs plantillas | Se mantiene la etapa con modelo aunque las plantillas cubran la función: deja abierta la puerta a RAG u otras funciones. |
| Segunda vuelta de pantallas | Responsabilidad de Victor (frontend). |

---

## 1. Qué se decidió (no reabrir sin hablar con Victor)

1. **No es un chatbot.** El productor nunca escribe texto libre. Toca una **pregunta cerrada** de un catálogo y recibe una respuesta con datos reales de su lote o de su sesión.
2. **La voz fija sigue siendo instantánea.** La línea en cursiva de cada pantalla ("Voz de Viora") se mantiene con texto calculado sin latencia. El SML solo responde cuando el productor toca una pregunta, así la espera es esperada.
3. **Solo lectura.** El asistente no modifica datos. Sus acciones son enlaces profundos a pantallas existentes (por ejemplo "Ir al plan de aclareo").
4. **Dos entradas a la misma hoja de preguntas:**
   - **Botón** circular de 48 px con el isotipo de Viora, debajo del menú ⋮ (arriba a la derecha), donde ese espacio está libre: Alternancia P40, Frío P80, Clima P90, Sensores P85, Resumen de ronda P54, Aclareo registrado P63.
   - **Tarjeta o pastilla** "Pregúntale a Viora" donde combina con el contenido: Inicio (tarjeta combinada con el resumen), Plan del lote P61, Detalle del lote P26, Alerta T15 y Campaña cerrada P73.
5. **Hoja de preguntas** (sheet de 28 dp) con pestañas **Preguntar · Recientes**. Estados: pensando, respuesta, datos nuevos (respuesta guardada con datos viejos), sin conexión, error, historial vacío.
6. **Respuesta = texto de la voz + exactamente 2 tarjetas bento del mismo tamaño + fuente visible + acciones** ("Otra pregunta", "Recientes", y a veces un botón hacia una pantalla).
7. **La respuesta se mantiene** al cerrar la hoja: reabrir la entrada lleva a la última respuesta hasta que el productor pulse "Otra pregunta".
8. **Historial: 10 respuestas durante 10 días.** Cada una marca "Al día" o "Datos nuevos".
9. **Resumen de Inicio precalculado** (una vez al día), sin latencia: tarjeta colapsada (una línea) o expandida (voz + 3 bento + "Ver completo" + "Pregúntale a Viora").
10. El isotipo de Viora aparece **solo** en la línea de voz y en el botón superior; no repetirlo en botones o cabeceras.

---

## 2. Mapa de pantallas (Figma, sección 23)

| Código | Pantalla | Código | Pantalla |
|---|---|---|---|
| P110 | Inicio · tarjeta Viora colapsada | P111 | Inicio · tarjeta expandida |
| P112 | Hoja de preguntas (Inicio, con selector de lote) | P113 | "¿Qué hago esta semana?" (resumen completo) |
| P129 | Recientes (desde Inicio) | P114 | Alternancia + botón |
| P114v | Alternancia con respuesta guardada | P115 | Hoja de preguntas (Alternancia) |
| P116 | Pensando | P117 | Respuesta (se mantiene al reabrir) |
| P118 | Recientes (10/10) | P119 | Datos nuevos (respuesta atenuada + "Volver a preguntar") |
| P120–P122 | Plan del lote: entrada, preguntas, respuesta | P123–P125 | Alerta: entrada, preguntas, respuesta |
| P126 | Sin conexión | P127 | No se pudo responder (Reintentar) |
| P128 | Recientes vacío | P130–P132 | Clima: entrada, preguntas, respuesta |
| P133–P135 | Frío: entrada, preguntas, respuesta | P136–P138 | Detalle del lote |
| P139–P141 | Sensores | P142–P144 | Resumen de ronda |
| P145–P147 | Aclareo registrado | P148–P150 | Campaña cerrada |

Pantallas **no cubiertas todavía** (segunda vuelta): Plan P60, Bitácora P50, Expediente P76, Lotes P20, Mi cuenta/planes. Se añaden con el mismo patrón.

Antes de construir UI, regla de Victor: captura cada frame de Figma y lee las reacciones del prototipo; anota lo que se omite por falta de datos.

---

## 3. Backend: bounded context `assistant`

### 3.1 Estructura (misma forma DDD que `thinning`/`phenology`)

```
platform/assistant/
  domain/model/{aggregates,valueobjects,queries,commands}   # AssistantAnswer, QuestionId, ScreenKey, Freshness, AnswerFact
  domain/services/                                          # QuestionCatalog, AnswerValidator
  application/internal/{queryservices,commandservices,outboundservices}
  application/acl/                                          # AssistantContextFacade (si otro BC lo necesita)
  infrastructure/{persistence/jpa, ai (ChatClient adapter), scheduling (resumen diario, retención)}
  interfaces/rest/{controllers,resources,transform}
```

- Datos de entrada: **solo a través de las fachadas ACL existentes**, nunca repositorios de otros contextos: `OrchardContextFacade`, `PhenologyContextFacade`, `TelemetryContextFacade`, `ThinningContextFacade`, `SubscriptionContextFacade`, `CommercialSizeGradeQueryService`, `ProfileContextFacade`.
- Seguridad: `anyRequest().authenticated()` ya está activo. Rutas con `plotId` usan `@PreAuthorize("@plotAccess.isAccessible(#plotId)")`. Añade a la prueba de arquitectura (`PlotRouteAuthorizationTest`) las rutas del asistente.
- Regla del repo: **1 rama feature = 1 endpoint**, commits de una línea en minúsculas, sin cuerpo y **sin co-autor**; solo ramas `feature/*`, una a la vez; Victor hace push y release. Los commits solo de docs en la plataforma van directo a `develop`.

### 3.2 Dependencia Spring AI

- Boot 4.1.1 / Java 21. **Verifica en Maven Central qué tren de Spring AI es compatible con Boot 4.1.x** (la línea 2.x) antes de añadir el BOM. No asumas el número de versión.
- Usa `ChatClient` + salida estructurada (JSON schema) y un `ChatModel` falso en pruebas (sin red).
- **Proveedor (decidido, sección 0):** Groq con el starter OpenAI de Spring AI y su URL compatible. Dejar el modelo en propiedades (`viora.assistant.model`, `viora.assistant.provider`) para poder cambiarlo a otro proveedor o a un SML propio sin tocar el dominio.
- **Implementado (2026-10-30):** BOM `org.springframework.ai:spring-ai-bom:2.0.2` y starter `spring-ai-starter-model-openai` (usa el SDK oficial `openai-java`). `viora.assistant.provider=template` (por defecto, sin modelo) o `groq`; `viora.assistant.model=qwen/qwen3.8-27b`. Con `groq` también hace falta `SPRING_AI_MODEL_CHAT=openai` (la autoconfiguración de chat está en `none` por defecto). URL base `https://api.groq.com/openai/v1` (el SDK la espera con `/v1`), `spring.ai.openai.timeout=9s` por intento y `max-retries=1` (dos intentos más la espera del SDK quedan dentro de los 20 s totales), temperatura 0,2 y 150 tokens de salida. Detalle en `asistente-decisiones.md`.
- La clave va **solo** en variable de entorno (`GROQ_API_KEY`, en Render y en local). Los repos son **públicos**: nunca commitear claves, prompts con datos reales ni respuestas de ejemplo con datos de producción.

### 3.3 Principio de diseño: el modelo redacta, el servidor calcula

Para cada pregunta, un **ensamblador de hechos** (`FactsAssembler`, uno por `questionId`) arma un JSON determinista con los números ya calculados (índice, porciones, porcentaje, fechas, umbrales…). El SML recibe **solo ese JSON** y devuelve **solo el texto** (2–3 frases, español neutro cercano y técnico, máx. ~280 caracteres). Las **2 tarjetas bento** y la **fuente** las produce el servidor, nunca el modelo.

Controles obligatorios:
- **Validador numérico:** todo número del texto debe existir en los hechos (comparación normalizada, ES/EN). Si falla, se descarta y se usa la **plantilla determinista** (fallback sin LLM).
- **Sin datos suficientes:** la pregunta llega deshabilitada desde el catálogo con `disabledReason`; no se llama al modelo.
- **Nunca inventar:** si un hecho falta, el ensamblador marca la pregunta como no disponible.
- **No enviar PII al modelo:** ni correo, teléfono, nombre completo ni coordenadas exactas. Nombre del lote y cifras agronómicas sí.
- **Sin texto del usuario en el prompt:** al ser preguntas cerradas, el riesgo de inyección es mínimo; mantener así.
- Idioma por `Accept-Language` (es/en), igual que el resto de la API.
- Declarar la naturaleza de los datos: nodos virtuales = "dato simulado"; **no afirmar riego** con humedad virtual (decisión abierta: alerta hídrica solo con sondas reales).
- No dar consejo médico, legal ni financiero. Redacción agronómica pendiente de validar con un agrónomo.

---

## 4. Contrato REST (base `/api/v1/assistant`)

Errores: `ProblemDetail` (RFC 7807) con las claves i18n habituales. Todas las rutas requieren sesión; las de lote, además acceso al lote.

### 4.1 Catálogo de preguntas

`GET /assistant/questions?screen={ScreenKey}&plotId={uuid?}`

```json
{
  "screen": "ALTERNATION",
  "plotId": "…",
  "questions": [
    { "questionId": "alternation.why_index", "label": "¿Por qué mi índice es 0,51?", "icon": "calendar", "enabled": true },
    { "questionId": "alternation.next_year", "label": "¿Cuánto cosecharé en 2027?", "icon": "harvest", "enabled": false, "disabledReason": "Faltan datos de calibre · registra un muestreo" }
  ]
}
```

- `label` viene **localizado y con cifras reales** (por ejemplo el 0,51). La app no compone etiquetas.
- `icon` es una clave del catálogo de iconos de Viora (calendar, pruning, book, alert, harvest, fruit, lote, temp, water, sensor, count, history, reg, view, prio). La app la traduce al icono.
- Sin lote (`plotId` ausente) solo para pantallas de ámbito campo (Inicio).

### 4.2 Responder

`POST /assistant/answers`

```json
{ "questionId": "plan.why_percent", "plotId": "…", "clientRequestId": "uuid", "forceRefresh": false }
```

Respuesta (JSON completo; sin SSE, ver sección 0):

```json
{
  "answerId": "uuid",
  "questionId": "plan.why_percent",
  "plotId": "…",
  "createdAt": "2026-10-28T12:00:00Z",
  "dataVersion": "sha256:…",
  "freshness": "UP_TO_DATE",
  "text": "Tu carga está en 0,57 frutos por brote y el árbol sostiene 0,40. Quitar 3 de cada 10 evita que la vecería se alargue a 2027.",
  "facts": [
    { "value": "0,57", "label": "frutos por brote · estimado", "pill": "objetivo 0,40", "tone": "PRIMARY" },
    { "value": "−30 %", "label": "de los frutos · quedan 12 días", "pill": "ventana abierta", "tone": "HIGHLIGHT" }
  ],
  "sources": [ { "label": "Basado en 5 árboles, ronda del 29 de octubre" } ],
  "actions": [ { "type": "DEEPLINK", "label": "Registrar aclareo", "target": "thinning/execute?plotId=…" } ],
  "model": { "provider": "…", "name": "…", "fallback": false }
}
```

- `facts` son **exactamente 2** (la UI exige tarjetas bento iguales). `tone`: `PRIMARY` (verde bosque) o `HIGHLIGHT` (amarillo claro).
- `fallback: true` cuando el texto salió de la plantilla (modelo caído o validador rechazó).
- Idempotencia: `clientRequestId` (UUID). Mismo id → misma respuesta, sin segunda llamada al modelo.
- `forceRefresh=true` = "Volver a preguntar con datos nuevos" (limitado más estrictamente).

### 4.3 Historial y respuesta guardada

- `GET /assistant/answers?limit=10` → últimas 10 respuestas **no vencidas (10 días)** del productor, más recientes primero. Cada una trae `freshness`.
- `GET /assistant/answers/{answerId}` → respuesta completa (para reabrir desde Recientes).
- `GET /assistant/answers/latest?screen=…&plotId=…` → la última respuesta del contexto actual (para que la entrada abra directo en ella hasta "Otra pregunta").
- **`freshness`:** `UP_TO_DATE` si el `dataVersion` guardado coincide con el actual; `STALE` si cambió (nuevo muestreo, aclareo, cosecha, alerta, clima relevante). La app atenúa la respuesta y ofrece "Volver a preguntar".

### 4.4 Resumen del día (Inicio)

`GET /assistant/daily-summary?plotId={uuid?}` → mismo formato que una respuesta, con `questionId: "home.daily_summary"` y 3 hechos para el bento de 3 tarjetas (los tres del mismo tamaño). Se **precalcula una vez al día** (zona `America/Lima`, p. ej. 06:30) con un job, y se genera bajo demanda si falta. Cache por `dataVersion`. El push de "tu resumen está listo" quedó descartado por ahora.

### 4.5 Códigos de error

| HTTP | Cuándo | Pantalla |
|---|---|---|
| 400 | `questionId` desconocido o no válido para la pantalla | — |
| 403 | sin acceso al lote, o plan inactivo (si se decide exigirlo) | sin acceso |
| 404 | respuesta o lote inexistente | — |
| 409 | pregunta deshabilitada por falta de datos (`assistant.question.unavailable`) | P115 fila deshabilitada |
| 429 | límite de uso, con `Retry-After` | aviso en la hoja |
| 503 | modelo no disponible **y** sin plantilla posible (`assistant.unavailable`) | **P127** "No se pudo responder" |

Si el modelo falla pero existe plantilla, se responde 200 con `fallback: true` (el productor no ve error).

---

## 5. Catálogo de preguntas por pantalla

`ScreenKey` ↔ pantalla ↔ preguntas (`questionId`). La columna "Datos" indica de dónde sale cada hecho; "Deshabilitada si" es lo que devuelve el catálogo como `disabledReason`.

### HOME (Inicio P110–P113, ámbito lote o todo el campo)
| questionId | Etiqueta | Datos | Deshabilitada si |
|---|---|---|---|
| `home.week_plan` | ¿Qué hago esta semana? | prescripción activa y ventana (thinning), máxima del pronóstico (telemetry), alertas, muestreos pendientes | sin lotes activos |
| `home.campaign_vs_previous` | ¿Cómo va mi campaña frente a la anterior? | cosechas históricas, BBI (phenology) | menos de 2 campañas registradas |
| `home.alerts_affecting` | ¿Qué alertas afectan a este lote? | incidentes/alertas activas | — |
| `home.harvest_estimate` | ¿Cuánto cosecharé este año? | proyección de calibre (US28) | calibre no estimado ("Faltan datos de calibre") |
| `home.daily_summary` | (resumen precalculado) | todo lo anterior, 3 hechos: días de ventana, máxima de la semana, alertas activas | — |

Sin `plotId` (todo el campo) las respuestas cuentan los lotes activos del productor sin nombrarlos y se guardan con `plot_id` nulo, aparte de las de cada lote. Diferencias con la tabla: la etiqueta de alertas pasa a "¿Qué alertas afectan a mis lotes?"; la campaña suma solo los lotes con cosecha en los dos últimos años y muestra el BBI solo con un lote; la cosecha estimada del campo es un rango de kg/ha entre los lotes con calibre estimado, no un total. Detalle en `asistente-decisiones.md` (Pantalla Inicio).

### ALTERNATION (P40, P114–P119)
`alternation.why_index` (¿Por qué mi índice es 0,51?: BBI y banda, hechos 0,51 y ⅓ con 9,6 → 3,1 t/ha), `alternation.break_cycle` (¿Qué hago para romper la vecería?), `alternation.vs_campaigns` (¿Cómo va este lote frente a mis campañas?), `alternation.next_year` (¿Cuánto cosecharé en 2027?, deshabilitada sin calibre).

### PLAN (P61, P120–P122)
`plan.why_percent` (¿Por qué 30 %?: carga 0,57 vs 0,40 y −30 % con ventana), `plan.if_less` (¿Y si aclaro solo 20 %?), `plan.if_none` (¿Y si no aclaro?: efecto OFF en 2027), `plan.best_time` (¿Cuándo es mejor aclarear?). Deshabilitadas sin prescripción activa o con muestra incompleta (`Faltan N árboles`).

### ALERT (T15, P123–P125)
`alert.what_to_do` (¿Qué hago ante este golpe de calor?: máxima 34 °C, umbral 32 °C, 5 h), `alert.fruitset_effect`, `alert.when_ends`, `alert.plots_at_risk`. El umbral de calor sale de Telemetría (ADR-006: 32 °C aviso, 36 °C crítico); el catálogo y las respuestas lo leen de `TelemetryContextFacade`, nunca de un literal.

### CLIMATE (P90, P130–P132)
`climate.irrigate` (¿Debo regar esta semana?), `climate.fruitset_effect`, `climate.heat_ends`, `climate.sensors_say`. `climate.irrigate` se deshabilita con `disabledReason` si el lote no tiene sondas reales; el resto de respuestas con humedad **virtual** dice "dato simulado".

### WINTER_CHILL (P80, P133–P135)
`chill.missing` (24/30, faltan 6), `chill.reach_30` (fecha estimada 4 ago vs brotación 31 ago), `chill.vs_last_winter`, `chill.if_incomplete`. Fuente: estación de frío EREZ. Pendiente validar con agrónomo la regla de >24 °C y las 30 porciones.

Implementado (2026-10-30): del 1 de junio al 31 de agosto responde sobre el invierno en curso y el resto del año sobre el último invierno terminado, con su año en las etiquetas. Sin datos de frío se deshabilitan las cuatro ("Sin datos de frío para este lote todavía"); `chill.reach_30` se deshabilita con menos de 14 días evaluados y `chill.vs_last_winter` sin datos del invierno pasado. La fuente se muestra como "Modelo de frío de Erez con el clima horario de Open-Meteo". Detalle en `asistente-decisiones.md` (Pantalla Frío).

### PLOT_DETAIL (P26, P136–P138)
`plot.how_is` (vecería 0,51 + 12 días), `plot.today`, `plot.last_sampling`, `plot.next_year_harvest` (deshabilitada sin calibre).

Implementado (2026-10-30): la pantalla lee el lote como HOME lo lee para un solo lote, así que no hay fachadas nuevas. `plot.how_is` da el índice de vecería y dónde está el aclareo; con la ventana abierta, los días que quedan, hoy incluido ("12 días de ventana · ventana abierta"). Sin índice y sin muestreo se deshabilita. `plot.today` resume el aclareo, el pronóstico de hoy y las alertas, y siempre se ofrece. `plot.last_sampling` resume el muestreo de la campaña con el día de la última ronda y se deshabilita sin muestreo. `plot.next_year_harvest` responde como `alternation.next_year` y se deshabilita sin calibre. Detalle en `asistente-decisiones.md` (Pantalla Detalle de lote).

### SENSORS (P85, P139–P141)
`sensors.what_say` (2 nodos activos, 27,4 °C), `sensors.why_paused`, `sensors.reliability`, `sensors.how_link`. Datos de nodos virtuales etiquetados como simulados.

Implementado (2026-10-30): `sensors.what_say` responde como `climate.sensors_say`, con "dato simulado" para nodos virtuales. `sensors.reliability` dice el origen de las lecturas y los días con lecturas de los últimos 7. `sensors.how_link` explica cómo vincular un nodo, sin botón hasta que la app tenga la ruta. `sensors.why_paused` se ofrece siempre deshabilitada ("Ningún nodo de este lote está pausado") porque Telemetría todavía no permite pausar nodos. Por qué, cómo y qué falta para activarla: `asistente-decisiones.md` (Pantalla Sensores).

### SAMPLING_SUMMARY (P54, P142–P144)
`sampling.enough_trees` (5/5 y 0,59 frutos por brote vs objetivo 0,40; mínimo 5 árboles), `sampling.meaning`, `sampling.next`, `sampling.correct_tree`.

Implementado (2026-10-30): `sampling.enough_trees` da los árboles sobre el mínimo de 5 y la carga frente al objetivo. `sampling.meaning` explica cuántas veces la carga supera al objetivo y el porcentaje a quitar. `sampling.next` da el paso siguiente según la ventana de aclareo, con "Registrar aclareo" mientras está abierta. `sampling.correct_tree` se ofrece siempre deshabilitada ("Corregir un árbol todavía no está disponible") porque Raleo no permite corregir un árbol. Por qué, cómo, el hallazgo de la carga duplicada y qué falta para activarla: `asistente-decisiones.md` (Pantalla Resumen de muestreo).

### THINNING_DONE (P63, P145–P147)
`thinning.harvest_change` (8,2 t/ha y 2027 más estable), `thinning.why_target`, `thinning.next_sampling`.

### CAMPAIGN_CLOSED (P73, P148–P150)
`campaign.vs_previous` (8,3 t/ha, +36 % sobre el promedio), `campaign.caliber_change`, `campaign.plan_next`.

> Las cifras de este documento son las de los mockups (plausibles, no validadas por un agrónomo). Los hechos reales siempre salen del ensamblador.

---

## 6. Persistencia, caché y retención

Tabla `assistant_answers` (nombres a ajustar a la convención de tablas del repo; ojo con la librería de pluralización pendiente):

`id, producer_id, plot_id?, question_id, locale, data_version, text, facts_json, sources_json, actions_json, fallback, model_name, input_tokens, output_tokens, created_at`

- **Clave de caché:** `(producer_id, question_id, plot_id, locale, data_version)`. Si existe y no venció (TTL corto, p. ej. 6 h), se devuelve sin llamar al modelo.
- **`data_version`:** hash de las revisiones/fechas de los agregados que alimentan la pregunta (muestreo, prescripción, cosechas, alertas, última lectura relevante). Debe poder calcularse barato, sin ensamblar los hechos completos.
- **Retención:** job diario que borra respuestas con más de **10 días** (mismo patrón que la retención de telemetría de 90 días, `viora.telemetry.retention.*`). El historial muestra las **10** más recientes.
- Mapear `@Version`/flush correctamente: usar `saveAndFlush` donde se devuelva el agregado (bug ya visto en `PlotRepositoryImpl`).
- Borrado del usuario (eliminar cuenta): incluir `assistant_answers` en la purga.

---

## 7. Límites, costos y resiliencia

- **Rate limit** con `SlidingWindowCounter` (`shared/infrastructure/ratelimit`) y `ClientAddress`: propuesta inicial **20 respuestas/hora por productor**, **5 `forceRefresh`/hora**, 60 consultas de catálogo/hora. Ajustar con datos reales. Respuesta 429 con `Retry-After`.
- **Timeouts:** 8 s hasta el primer token, 20 s totales; reintento una vez; **circuit breaker** (Resilience4j o equivalente) que abre tras N fallos y fuerza la plantilla.
- **Presupuesto de tokens:** entrada ≤ ~600 tokens (JSON de hechos + instrucciones), salida ≤ 150. Registrar tokens por respuesta para vigilar el costo.
- **Cuota/plan:** el asistente exige sesión, acceso al lote y **plan activo** (`SubscriptionContextFacade`); con el plan vencido responde 403.
- **Observabilidad:** métricas Micrometer por pregunta (latencia, tasa de fallback, tasa de rechazo del validador, 429/503) y log sin datos personales.
- Render gratuito: arranque en frío puede pasar de 100 s. El resumen del día precalculado evita que Inicio dependa del modelo.

---

## 8. Orden de implementación sugerido

**Backend (ramas `feature/*`, una a la vez, un endpoint por rama):**
1. `feature/assistant-question-catalog`: `GET /questions` con ensambladores de disponibilidad (sin LLM).
2. `feature/assistant-template-answers`: `POST /answers` con **plantillas deterministas** + persistencia + historial + `freshness` + caché. Con esto la app ya puede integrarse de punta a punta.
3. `feature/assistant-spring-ai`: `ChatClient` con Groq, salida estructurada, validador numérico con sus refuerzos, fallback a plantilla, propiedades de modelo, pruebas con `ChatModel` falso.
4. `feature/assistant-daily-summary`: job diario + `GET /daily-summary`.
5. `feature/assistant-limits-metrics`: rate limits, circuit breaker, métricas, retención.

(La rama de streaming SSE se eliminó; ver sección 0.)

Cada rama: pruebas unitarias de cada ensamblador, prueba de arquitectura de autorización, `mvn test` verde (ver recetas locales abajo). Lanzar el backend y publicarlo en Render **antes** de usar la versión de la app que lo consuma.

**App Android (después del merge del refactor de Piero, o coordinado con él):**
- Nuevo feature `features/assistant` con capas domain/application/infrastructure/presentation; sin reglas de negocio en la UI.
- Reutilizar `VioraVoice` (`core/designsystem/component/VioraVoice.kt`): ya soporta `Thinking`, `Streaming(text)` y `Ready(text, fromAssistant = true)`; respeta la preferencia "Voz de Viora animada" y la configuración del sistema que quita animaciones. La respuesta del asistente siempre se escribe.
- Hoja con `ModalBottomSheet` (esquinas superiores 28 dp), pestañas Preguntar/Recientes, filas de pregunta con icono de Viora, tarjetas bento **de la misma altura**, fuente visible, botones "Otra pregunta" y "Recientes".
- Estado de la hoja en ViewModel: pregunta elegida, respuesta, `freshness`; la entrada abre en la última respuesta hasta "Otra pregunta" (usa `GET /answers/latest`).
- Sin conexión: preguntas deshabilitadas con "Requiere conexión"; historial local en Room (10 respuestas, 10 días) para consultar offline. Recordar que Room 3 usa `androidx.room3`.
- Manejo de errores: 429 (aviso con espera), 503 (P127 con "Reintentar"), 409 (pregunta no disponible).
- Cadenas en es/en; accesibilidad: TalkBack lee el texto completo, nunca letra a letra; objetivos táctiles ≥ 48 dp.
- Entradas: botón superior derecho (48 dp, fondo amarillo claro, isotipo, misma sombra que los botones de la cabecera) o pastilla según la pantalla (sección 1, punto 4). Alerta T15: el botón no va arriba (choca con la fecha); usa pastilla bajo "Qué hacer".
- Versionado: la app sube versión **después** del backend; release de Victor.

---

## 9. Pruebas

- Unitarias por ensamblador (hechos correctos, preguntas deshabilitadas, `disabledReason`).
- Pruebas del **validador numérico** con textos que inventan cifras (deben caer a plantilla).
- Pruebas de contrato del JSON (exactamente 2 hechos, `tone` válido, `freshness`).
- Prueba de arquitectura: toda ruta del asistente exige autenticación y acceso al lote (extender `PlotRouteAuthorizationTest`).
- Pruebas de límite de uso (429) y de caché (misma `clientRequestId` no llama al modelo dos veces).
- En Linux: JDK 21 y Maven 3.9.16 (`mvn test`; el repo no trae el script `mvnw`). Entre las 19:00 y las 00:00 de Lima fallan 6 pruebas que mezclan la fecha UTC con la de Lima; es un problema previo, no del asistente.
- E2E local: `spring-boot:run` en 8080 con H2, o jar con JDK 25 y archivo H2 + Shell (ver memoria de despliegue). Con JDK 25 el JIT C2 falla en esta PC: ejecutar maven con `MAVEN_OPTS` y `-DargLine="-XX:TieredStopAtLevel=1"`. Sin `mvn` en el PATH: usar el wrapper de `.m2` (`mvn.cmd -o test`) desde PowerShell.

---

## 10. Decisiones pendientes

Las decisiones 1, 2, 3, 4, 5 y 7 de la versión anterior se cerraron en la sección 0. Quedan abiertas:

1. Validación con un agrónomo de la redacción agronómica y de sus reglas: los umbrales de calor de 32 °C y 36 °C, la regla de frío >24 °C y las 30 porciones.
2. Proveedor y modelo para producción, después de medir con Groq.

---

## 11. Recordatorios de convenciones

- Commits: una línea en minúsculas, sin cuerpo, **sin co-autor**. Solo ramas `feature/*`, una a la vez. Victor hace push y release (salvo que pida lo contrario).
- Los repos son **públicos**: ningún secreto, ningún dato real en commits ni en la documentación.
- Docs solo-docs en la plataforma van directo a `develop`.
- Antes de etiquetar, `git ls-remote --tags origin` (hubo un choque `0.16.0` vs `1.16.0` en la app).
- Fuente de verdad de endpoints existentes: el código y `docs/mobile-backend-gaps.md` de la plataforma; la nota del informe puede estar desfasada.
- Tipografía: en la máquina de Victor falta la fuente Caros usada por los rótulos "Flujo_NN" de Figma; el rótulo F15 de la sección 23 se armó con Roboto/Newsreader.
