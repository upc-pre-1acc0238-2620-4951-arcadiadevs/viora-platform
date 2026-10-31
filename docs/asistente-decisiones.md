# Asistente Viora · registro de decisiones técnicas

Decisiones menores tomadas durante la implementación del backend del asistente. Las de producto o de negocio no se toman aquí: se anotan como pendientes. El contrato general está en `viora-asistente-spring-ai-contrato.md`; los problemas previos encontrados en el camino, en `parches-pendientes.md`.

Formato: fecha, decisión, porqué. Las más nuevas al final.

## Port sobre la implementación de producción (0.63.0, 2026-10-31)

El backend del asistente se escribió primero en ramas locales; mientras tanto, producción publicó su propia versión (0.63.0, la que usa la app 1.18.0). Se decidió conservar la de producción como base y portar encima lo local: pantallas, ámbito de campo, resumen del día y fachadas. **Donde una entrada más abajo choca con esta sección, manda esta sección.**

- **Contrato de producción intacto.** Misma forma de respuesta (incluido `question` y el historial `{"answers":[...]}`), `POST /answers` siempre 200, `latest` 404 `ANSWER_NOT_FOUND`, id mal formado 400, 409 `ASSISTANT_CONFLICT`, 429 `ASSISTANT_TOO_MANY_REQUESTS`, tabla `assistant_answers` y propiedades (`viora.assistant.model.enabled`, `GROQ_API_KEY`, modelo `openai/gpt-oss-20b`). Las pruebas de producción conservan sus aserciones; solo cambió la entrada de `AssistantAnswerControllerIntegrationTest.rejectsInvalidRequests` (`home.week_plan` → `dashboard.week_plan`), porque HOME ahora existe y la prueba necesita un espacio de preguntas inexistente.
- **No portado** (la app tendría que cambiar): 429 del catálogo y su límite `catalog-per-hour`, tabla `assistant_answer_requests`, respuestas 201/204, 409 `ASSISTANT_REQUEST_CONFLICT` (un `clientRequestId` repetido con otra pregunta se repite sin error, como en producción), el punto de entrada 401 local (queda el de producción), `AssistantMetrics`, `FactGlossary` y el respaldo pegajoso de 10 minutos (`fallback-cache-ttl`). Las entradas de abajo sobre esos puntos quedan como historia.
- **Estructura.** `ScreenFacts` sellado con un registro por pantalla, un lector de hechos por pantalla (`application/internal/queryservices`) y una clase de preguntas y plantillas por pantalla (`domain/services`). El catálogo y la respuesta leen los mismos hechos. Reemplaza a `FactsAssemblerRegistry` y los ensambladores de las entradas de abajo.
- **Una fuente por respuesta.** `AnswerDraft` de producción exige exactamente una fuente. Donde la versión local citaba varias, se cita la principal; por ejemplo, el plan de la semana cita el plan, si no el pronóstico, si no las alertas abiertas.
- **Textos.** Los mensajes usan el prefijo de producción `assistant.answer.*` (antes `assistant.template.*`). En inglés las tarjetas escriben `+25%` sin espacio y las frases `30 %`, como producción.
- **CLIMATE nunca menciona riego en el texto del modelo** (lista de términos prohibidos de producción); la versión local lo permitía con alerta crítica.
- **Vigencia por pantalla.** HOME y PLOT_DETAIL incluyen el día en su `dataVersion`: sus respuestas pasan a `STALE` al día siguiente.
- **Inicio sin lote.** La respuesta del campo se guarda con `plot_id` nulo y solo se reutiliza o devuelve como última para ese productor y ese ámbito (consultas propias con `plot_id is null`). Las demás pantallas siguen exigiendo `plotId` (400 "Indica el lote de esta pantalla"). La columna se relaja con `scripts/sql/2026-10-30-assistant-answers-field-scope.sql`, que hay que correr a mano antes del release: `ddl-auto=update` nunca quita un `NOT NULL`.
- **Resumen del día con su propio agregado.** `DailySummary` (exactamente 3 tarjetas), recurso `AssistantDailySummaryResource` con `summaryId` y tabla `assistant_daily_summaries` (script `scripts/sql/2026-10-31-assistant-daily-summaries.sql`, opcional porque `ddl-auto` crea la tabla). No se guarda en `assistant_answers`, así que no se reabre por `GET /answers/{id}`. Un resumen de plantilla no se reutiliza; servir uno existente no gasta cupo y escribir uno nuevo gasta una respuesta. El job de las 06:30 no gasta cupo. Retención por `history-days` con su propio job, y la purga de cuenta borra también los resúmenes. Pendiente para el dueño: si se vuelve a una pausa antes de reintentar el modelo tras una plantilla.
- **Forma de trabajo de esta ronda.** Ramas `feature/assistant-*` encadenadas desde `origin/develop` y publicadas como ramas propias; `develop` y `main` no se tocan. Reemplaza las dos entradas de "Forma de trabajo".

## Forma de trabajo

- **2026-10-30 · Ramas locales encadenadas.** Cada rama `feature/assistant-*` sale de `develop`. Al terminar, con la suite en verde, se fusiona en `develop` con `--no-ff` (como el historial del repo) y se borra la rama local. Así la rama siguiente parte de la anterior. Nada se publica sin que lo pida el responsable.
- **2026-10-30 · Commits.** Una línea, minúsculas, sin cuerpo ni coautor. Los commits solo de documentación van directo a `develop`.

## Catálogo de preguntas (ALTERNATION y PLAN)

- **2026-10-30 · Prescripción emitida.** Una prescripción cuenta como emitida en estado `PRESCRIBED`, `CONFIRMED` o `EXECUTED`, con porcentaje calculado. Así las preguntas del plan siguen disponibles después del aclareo para explicar lo que se hizo.
- **2026-10-30 · "Faltan N árboles".** Solo aparece cuando hay al menos un árbol muestreado y faltan árboles para el mínimo de 5 (US25). Sin muestreo, o con muestreo completo esperando la plena floración, el motivo es "Sin plan de aclareo activo".
- **2026-10-30 · Orden de controles.** Dueño del lote (`@PreAuthorize`) → validación (400) → plan activo (403) → lote existente (404). Sin plan, un lote inexistente responde 403; no filtra información.
- **2026-10-30 · Año de campaña.** Año calendario en `America/Lima`, leído del `Clock` inyectado (igual que `GetActiveThinningPrescriptionQuery`).
- **2026-10-30 · `disabledReason` nulo.** Se envía `null` cuando la pregunta está habilitada; el repo no omite campos nulos de forma global.
- **2026-10-30 · Cifras en etiquetas.** Las formatea el propio mensaje (`{0,number,0.00}`), así el idioma decide la coma o el punto decimal.

## Contexto del informe del proyecto (`ma-viora-report`)

- **2026-10-30 · Sin requisito del asistente.** El informe no tiene historias de usuario del asistente: el alcance sale del contrato y del diseño de Figma (sección 23).
- **2026-10-30 · Voz y registro.** El informe define la voz como "un técnico de confianza: claro, directo y con datos del campo", serena ante alertas críticas, y la interfaz trata de **tú**. Las plantillas y el prompt usan ese registro.
- **2026-10-30 · Redacción prudente.** Las plantillas siguen los patrones de los criterios de aceptación: decir cuando faltan datos sin bloquear, indicar cuántos árboles faltan, "en calibración" o "no aplicable" en lugar de extrapolar.
- **2026-10-30 · Umbrales distintos.** El calor (US18) y la helada difieren entre el informe y el código. No se cambian (decisión agronómica); ver ADR-006 sección 5 y `parches-pendientes.md` punto 5.

## Respuestas con plantilla

- **2026-10-30 · Disponibilidad compartida.** `POST /answers` reutiliza la evaluación del catálogo (`QuestionCatalogQueryService`): mismo control de plan, lote y datos. Una pregunta deshabilitada responde 409 sin leer los datos del lote ni ensamblar hechos.
- **2026-10-30 · Forma del 409.** Código `ASSISTANT_QUESTION_CONFLICT`, `detail` con el `disabledReason` localizado y la propiedad `reason: assistant.question.unavailable`. No se usa un código terminado en `_UNAVAILABLE` porque el repo lo traduce a 503.
- **2026-10-30 · Falta una cifra.** Si el catálogo habilita la pregunta pero el ensamblador no encuentra una cifra (por ejemplo, la carga del muestreo), responde 409 con "Faltan datos para responder esta pregunta". Nunca se inventa el número.
- **2026-10-30 · Orden de controles del POST.** Dueño del lote (`@PreAuthorize` sobre el cuerpo, `#resource?.plotId()`) → validación (400) → plan activo (403) → repetición del `clientRequestId` → lote (404) y disponibilidad (409) → caché → respuesta nueva.
- **2026-10-30 · Idempotencia.** El mismo `clientRequestId` devuelve la respuesta guardada tal como se sirvió (200, `freshness: UP_TO_DATE`), sin volver a ensamblar hechos. La frescura real se consulta con los GET. Si el id se reusa con otra pregunta o lote, 409 `assistant.request.reused`. Si dos pedidos con el mismo id compiten, se devuelve el que quedó guardado (restricción única por productor). Una respuesta servida desde la caché también queda ligada al `clientRequestId` del pedido (tabla `assistant_answer_requests`), así un reintento con ese id, incluso pasado el TTL o con datos nuevos, devuelve la misma respuesta (hallazgo del verificador).
- **2026-10-30 · Idioma guardado.** Se guarda `es` o `en`; cualquier otro idioma cae en inglés, como los mensajes por defecto. La caché distingue el idioma.
- **2026-10-30 · `dataVersion`.** `sha256:` de los hechos con claves ordenadas y números normalizados (0.4 y 0.40 son iguales). Los hechos del plan incluyen los días que quedan de ventana, así que esas respuestas pasan a `STALE` al día siguiente: su texto dice "quedan N días".
- **2026-10-30 · Días de ventana.** Cuentan el día de hoy: el último día de la ventana es "queda 1 día".
- **2026-10-30 · Acción "Registrar aclareo".** Solo en `plan.why_percent` y `plan.best_time`, con la ventana abierta y sin aclareo registrado. Destino `thinning/execute?plotId=…`.
- **2026-10-30 · Cálculos del servidor.** Solo derivaciones directas de hechos ya calculados, y cada resultado queda en los hechos: carga tras quitar p % (`carga × (1 − p/100)`), carga entre objetivo, diferencia porcentual de la última campaña frente al promedio de las anteriores y días de ventana. BBI, cobertura del muestreo, calibre y rendimiento se leen de sus contextos.
- **2026-10-30 · Fachada de aclareo.** `ThinningContextFacade.findThinningPlanDetail` (aditivo) reutiliza `ThinningPrescriptionOutlookService`: la misma carga, objetivo y muestreo que muestra la pantalla del plan, y el rendimiento tras el aclareo. La fachada de fenología no necesitó cambios.
- **2026-10-30 · Kilos por campaña.** Las comparaciones entre campañas usan los kilos totales del lote tal como se registran, no t/ha.
- **2026-10-30 · Validador numérico.** `TextNumbers` (dominio) extrae los números de un texto según el idioma (coma o punto decimal, separador de miles) y los compara con los hechos; los números dentro de textos de hechos (fechas ISO, grado "171/190") también cuentan. Una prueba recorre todas las variantes de plantilla en es y en. Las plantillas no llevan cifras literales fuera de los hechos.
- **2026-10-30 · Estado de las plantillas.** `viora.assistant.templates.status=SYNTHETIC_DEMO` en `application.properties`; no cambia el comportamiento. Producción requiere `AGRONOMIST_APPROVED`, igual que los perfiles de aclareo.
- **2026-10-30 · Columna `model_provider`.** Se añade a `assistant_answers` (el contrato no la tenía) para guardar el proveedor del paso 3 sin deducirlo del nombre del modelo.
- **2026-10-30 · Historial y respuesta guardada.** `GET /answers` devuelve un arreglo, como las demás listas del repo. `GET /answers/{id}` con un id mal formado, de otro productor o de más de 10 días responde 404. `GET /answers/latest` responde 404 si el lote no existe (igual que el catálogo) y 204 si no hay respuesta.
- **2026-10-30 · Borrado de cuenta.** `AssistantAccountDataPurger` escucha `UserAccountDeletedEvent` y borra las respuestas del productor; `AccountDeletionIntegrationTest` lo comprueba.

### Pendientes

- **`plan.if_none`.** La plataforma no estima el efecto de no aclarar en la próxima cosecha. La plantilla lo dice y solo muestra la carga frente al objetivo. Falta decidir si se calcula y con qué modelo (decisión agronómica).
- **`alternation.next_year`.** La etiqueta pregunta por el año siguiente, pero la plataforma solo proyecta el rendimiento y el calibre de la campaña actual tras el aclareo. La respuesta habla de "la cosecha de esta campaña". Falta confirmar si esa cosecha cae en el año de la etiqueta o ajustar la etiqueta.
- **`alternation.break_cycle`.** La única medida que conoce la plataforma es el aclareo. Otras prácticas (poda, riego, nutrición) quedan fuera hasta que un agrónomo las valide.
- **Redacción agronómica.** Frases como "antes del endurecimiento del hueso", "la que el árbol puede sostener" o "los años altos y bajos suelen alternarse" esperan la validación del agrónomo.
- **Prueba de arquitectura.** `PlotRouteAuthorizationTest` solo revisa `plotId` en parámetros de consulta. El POST lo recibe en el cuerpo; extender la prueba queda fuera de esta rama.
- **Retención.** Las respuestas de más de 10 días se ocultan pero no se borran; el job diario queda para `feature/assistant-limits-metrics`. Resuelto en esta rama: `AssistantRetentionScheduler` (ver "Límites, métricas y retención").

### Seguimiento de la revisión (2026-10-30)

La revisión nativa aprobó los tramos `280922b` (aplicación) y `fa2a46e` (endpoints y persistencia) sin bloqueos. Advertencias anotadas para `feature/assistant-limits-metrics` o un parche posterior:

- **Carreras de idempotencia.** La clave puede quedar en la fila de la respuesta o en `assistant_answer_requests`; dos pedidos simultáneos con la misma clave, uno creando y otro sirviendo caché, no comparten una restricción única entre tablas. Hoy se resuelve volviendo a leer tras el conflicto; falta una prueba de concurrencia y, si hace falta, una sola tabla de claves. Resuelto en esta rama: toda clave tiene fila en `assistant_answer_requests` y comparten su restricción única; pruebas de concurrencia y de H2.
- **Purga.** `deleteAllByProducerId` carga y borra fila por fila, en dos tablas sin transacción explícita. Para cuentas con muchas respuestas conviene un borrado masivo transaccional; la prueba de borrado de cuenta no verifica la tabla de claves. Resuelto en esta rama: dos `delete` masivos en una transacción, y la prueba verifica ambas tablas.
- **Frescura del historial.** `GET /answers` recalcula los hechos de cada respuesta (hasta 10) y no degrada si una fachada falla. Conviene cachear el `dataVersion` actual por pregunta y lote, y responder `STALE` ante un error.
- **Pruebas con años fijos.** `AccountDeletionIntegrationTest` usa cosechas 2024/2025; derivarlas del `Clock`. Resuelto en esta rama: años de cosecha del `Clock` inyectado en la zona de Lima.
- **Menores.** Nombre del ensamblador de recursos, límite inclusivo/exclusivo del TTL distinto entre caché e historial, comentario de la propiedad `cache-ttl`.


## Redacción con modelo (Spring AI)

- **2026-10-30 · Dependencia.** `spring-ai-bom:2.0.2` (importado en `dependencyManagement`) y `spring-ai-starter-model-openai`, verificados en Maven Central y en la referencia 2.0.2. Desde 2.0 el starter usa el SDK oficial `openai-java`, que espera la URL base con su ruta de versión: para Groq es `https://api.groq.com/openai/v1` (sin `/v1` el SDK no llega al API compatible). Comprobado en la prueba en vivo.
- **2026-10-30 · Conflicto con springdoc.** El starter trae `swagger-annotations-jakarta` 2.2.38, que ganaba sobre la 2.2.47 de springdoc y rompía `/v3/api-docs` (`NoSuchMethodError` en `Schema.$dynamicRef`, lo detectó `OpenApiContractIntegrationTest`). Se excluye del starter en el `pom.xml` y queda la de springdoc; no es una dependencia nueva.
- **2026-10-30 · Interruptor del proveedor.** `viora.assistant.provider=template` por defecto: no hay `ChatModel`, no se lee clave ni se usa la red, y así arrancan la aplicación y todas las pruebas. `groq` activa el escritor con modelo. La autoconfiguración de chat de Spring AI 2.0.2 se apaga con `spring.ai.model.chat=none` (la propiedad `spring.ai.openai.chat.enabled` ya no existe); queda en `none` en `application.properties` y se enciende con la variable `SPRING_AI_MODEL_CHAT=openai` junto con `VIORA_ASSISTANT_PROVIDER=groq` y `GROQ_API_KEY`. Si se pide `groq` sin modelo de chat, la aplicación no arranca y lo dice, en lugar de responder con plantillas en silencio. Embeddings, imágenes, audio y moderación quedan siempre en `none`. Sin clave, 2.0.2 no falla al arrancar (usa un marcador sin autenticación), pero se prefirió apagarla.
- **2026-10-30 · Límites de la llamada.** Temperatura 0,2, 150 tokens de salida, `spring.ai.openai.timeout=9s` por intento y `spring.ai.openai.max-retries=1`: el SDK aplica el timeout a cada intento, así que dos intentos más su espera quedan dentro de los 20 s totales del contrato (hallazgo del verificador; una prueba fija la suma) (el reintento lo hace el SDK; `OpenAiChatModel` 2.0.2 no usa el `RetryTemplate` de Spring AI). `replay-reasoning-content=false` porque Groq rechaza ese campo. El tiempo hasta el primer token (8 s del contrato) no aplica: no hay streaming. El circuit breaker queda para `feature/assistant-limits-metrics`.
- **2026-10-30 · Puerto.** `AnswerWriter` (aplicación) con dos implementaciones: `TemplateAnswerWriter` (el texto de la plantilla, igual que antes) y `SpringAiAnswerWriter` (`infrastructure/ai`, `ChatClient`). El servicio solo llama al escritor al crear una respuesta nueva; repetición del `clientRequestId` y caché nunca llaman al modelo (prueba en `AssistantAnswerCommandServiceImplTest` y en la de integración). Tarjetas, fuentes y acciones siempre salen del servidor.
- **2026-10-30 · Qué recibe el modelo.** Mensaje de sistema con la voz ("técnico de confianza"), las reglas (2–3 frases, ≤ 280 caracteres, solo números de los hechos tal cual, sin redondear ni calcular, solo acciones de `allowedActions`, "dato simulado" cuando un hecho lo diga, sin riego si `irrigationAdvisable` es falso), qué pregunta se responde y qué significa cada hecho presente. Mensaje de usuario: solo el JSON de hechos y el formato de salida. Hechos interpretados añadidos: `questionId`, `language`, `irrigationAdvisable` (siempre `false`: ninguna pregunta de ALTERNATION o PLAN lee sondas) y `allowedActions` (solo las etiquetas). Nunca van productor, lote, destino de acciones, correo, teléfono, nombre ni coordenadas; los hechos actuales no los tienen y una prueba lo comprueba. Los hechos interpretados no entran en `dataVersion`.
- **2026-10-30 · Salida estructurada.** `BeanOutputConverter<ModelText>` (un solo campo `text`): sus instrucciones de formato van al final del mensaje de usuario y la conversión se hace aparte, así un JSON roto se distingue de un fallo de la llamada y se guardan los tokens del intento.
- **2026-10-30 · Validador.** `AnswerTextValidator` (dominio) reutiliza `TextNumbers`. Rechaza, en este orden: texto vacío, más de 280 caracteres, más de 3 frases (corte en `.`, `!`, `?` o `…` seguido de espacio), un número que no está en los hechos y frases prohibidas. Frases prohibidas, a propósito simples (patrones de palabras en es y en, no gramática): cualquier mención de riego (`regar`, `riega`, `riego`, `irrig…`, `water the/your…`) salvo que `irrigationAdvisable` sea verdadero, y "registra/registrar el aclareo" o "record/register the thinning" cuando esa acción no está en `allowedActions`. Se valida contra los mismos hechos que recibe el modelo.
- **2026-10-30 · Respaldo.** Rechazo, error, tiempo agotado o salida mal formada → texto de la plantilla con `model = {template, template-v1, fallback: true}` y los tokens del intento si la respuesta los trae. El motivo (`TextRejection`: `BLANK`, `TOO_LONG`, `TOO_MANY_SENTENCES`, `UNBACKED_NUMBER`, `FORBIDDEN_PHRASE`, `MALFORMED_OUTPUT`, `TIMEOUT`, `MODEL_ERROR`) va en el resultado del escritor y en el log; no se guarda en la tabla (no hay columna y no se quiso cambiar el esquema en esta rama). Texto aceptado → `{groq, <modelo>, fallback: false}` con los tokens de la respuesta en `input_tokens` y `output_tokens`.
- **2026-10-30 · Log.** Una línea por intento: `question`, `latencyMs`, `outcome` (`accepted`, `rejected:<motivo>`, `error:<motivo>` con la clase de la excepción) y tokens. INFO si se acepta, WARN si no. Nunca hechos, texto ni datos personales.
- **2026-10-30 · Prueba en vivo y modelo por defecto.** Groq sirve hoy `openai/gpt-oss-20b`, `openai/gpt-oss-120b`, `qwen/qwen3.8-27b`, `allam-2-7b` y modelos de audio o guardia (ya no `llama-3.1-8b-instant`). Diez llamadas con hechos sintéticos (`AnswerDataSamples`, es y en, ALTERNATION y PLAN), fuera del repo: `openai/gpt-oss-20b` agota los 150 tokens razonando y devuelve JSON incompleto en 10 de 10 llamadas (5 de 10 con `reasoning-effort=low`); `qwen/qwen3.8-27b` devolvió JSON válido en 10 de 10, 420–960 ms (la primera ~2,2 s), 50–75 tokens de salida. Se elige `qwen/qwen3.8-27b`. Una sola ronda de ajuste del prompt: se añadió qué pregunta se responde y el significado de cada hecho, porque antes el modelo atribuyó 0,46 (carga tras quitar el 20 %) al 30 % del plan; después de la ronda las 10 respuestas se aceptaron y esa confusión desapareció.

### Pendientes (redacción con modelo)

- **Proveedor y modelo de producción.** Sigue abierto (sección 10 del contrato); Groq y `qwen/qwen3.8-27b` son para desarrollo y pruebas.
- **Presupuesto de entrada.** Con el glosario y el formato del conversor, la entrada medida es de 610–785 tokens, sobre los ~600 del contrato. Se puede recortar el formato del conversor o el glosario; decidir si el límite se ajusta.
- **Calidad que el validador no ve.** En la prueba en vivo el modelo aún escribió el valor crudo `MODERATE_ALTERNATION`, cambió "brote" por "sarmiento" y dijo "tienes 12 días para registrar el aclareo". Los números eran correctos. Opciones: localizar los hechos de texto antes de enviarlos o añadir esas frases al validador. Requiere revisión del agrónomo antes de producción, igual que las plantillas.
- **Motivo del respaldo persistido.** Para las métricas de `feature/assistant-limits-metrics` falta decidir si `TextRejection` se guarda en una columna o solo va a Micrometer. Resuelto en esta rama: solo va a Micrometer (etiqueta `reason`) y al log; no se cambia el esquema.

### Seguimiento de la revisión de Spring AI (2026-10-30)

La revisión nativa aprobó el tramo `63a9d88`..`728a3c5` sin bloqueos. Advertencias anotadas para `feature/assistant-limits-metrics` o un parche posterior:

- **Respaldo pegajoso.** Si el modelo falla, la respuesta con plantilla queda en caché 6 h para esos hechos: nadie vuelve a intentar con el modelo hasta que cambien los datos o pase el TTL. Opción: no cachear respuestas con `fallback=true` del proveedor `groq`, o darles un TTL corto. Resuelto en esta rama: TTL corto `viora.assistant.fallback-cache-ttl=PT10M`.
- **Llamadas duplicadas en paralelo.** Dos pedidos simultáneos con claves distintas para la misma pregunta pueden llamar dos veces al modelo antes de que exista la caché.
- **Presupuesto de reintentos.** El reintento del SDK suma latencia a cada pedido; con el circuit breaker de la siguiente rama conviene revisar si se mantiene. Resuelto en esta rama: se mantiene `max-retries=1`; el breaker cuenta el intento completo (con su reintento) como un fallo.
- **Validador fuera del bloque de respaldo.** Una excepción inesperada del validador no cae a la plantilla; envolverla igual que los errores del modelo. Resuelto en esta rama: motivo `VALIDATION_ERROR`, plantilla y nunca 5xx.
- **Prueba faltante.** El arranque con `provider=groq` sin modelo de chat (debe negarse a arrancar) no tiene prueba. Resuelto en esta rama: `AssistantGroqWithoutModelTest`.
- **Menores.** Los límites del prompt (280 caracteres, 2–3 frases) se repiten en el prompt y en el validador.

## Límites, métricas y retención

Rama `feature/assistant-limits-metrics` (paso 5 del contrato) más los seguimientos de las revisiones.

- **2026-10-30 · Límites por productor.** `viora.assistant.rate-limit.answers-per-hour=20`, `refreshes-per-hour=5` y `catalog-per-hour=60`, con `SlidingWindowCounter` en una ventana deslizante de una hora y la clave = productor (no la IP: todas las rutas exigen sesión, así que no se usa `ClientAddress`). Al pasarse: 429 con `ApplicationError.tooManyRequests`, código `ASSISTANT_TOO_MANY_REQUESTS`, `detail` localizado (`assistant.rate_limited`), propiedad `retryAfterSeconds` y cabecera `Retry-After`, la misma forma que el canje de códigos. Los contadores viven en memoria de una instancia y se vacían al reiniciar, como los demás límites del repo.
- **2026-10-30 · Qué cuenta.** El POST cuenta después del plan activo y de la búsqueda del `clientRequestId`: una repetición idempotente no gasta cupo y no cuesta nada extra, porque esa búsqueda ya se hacía primero (una o dos consultas por índice). Sí cuentan la caché, la creación y el 409 de pregunta deshabilitada. `forceRefresh` cuenta en su límite y además en el de respuestas; se revisa primero el de refrescos, así un refresco rechazado no gasta una respuesta (al revés sí se pierde el cupo de refresco, el contador no devuelve eventos). El catálogo cuenta en el controlador, no en `QuestionCatalogQueryService`, porque responder una pregunta también evalúa el catálogo y eso no debe gastar ese límite. Puerto `AssistantRateLimits` (aplicación) e implementación `SlidingWindowAssistantRateLimits` (infraestructura).
- **2026-10-30 · Circuit breaker.** Clase propia `ModelCircuitBreaker` (`infrastructure/ai`), sin Resilience4j ni otra dependencia. Se abre tras `viora.assistant.breaker.failure-threshold=5` fallos seguidos del modelo (error, tiempo agotado o salida mal formada), queda abierto `viora.assistant.breaker.open-for=PT2M` y luego deja pasar una sola llamada de prueba (semiabierto): éxito lo cierra, fallo lo abre otra vez. Abierto: plantilla directa con `fallback=true`, motivo `BREAKER_OPEN` y sin llamar al modelo. Un rechazo del validador cuenta como modelo sano (respondió). Una excepción del validador (`VALIDATION_ERROR`) es un error nuestro, no del modelo: tampoco abre el breaker. Sincronizado; la prueba con varios hilos comprueba una sola llamada de prueba. El breaker vive por instancia.
- **2026-10-30 · Respaldo pegajoso.** Con un escritor con modelo (`AnswerWriter.usesModel()`), una respuesta guardada con `fallback=true` solo se sirve de la caché durante `viora.assistant.fallback-cache-ttl=PT10M`; las del modelo siguen con `cache-ttl` (6 h). Se detecta con la columna `fallback` que ya existía, sin cambiar el esquema: con `groq` toda respuesta de plantilla es un respaldo (modelo caído, texto rechazado, breaker abierto) o es anterior al cambio de proveedor; en ambos casos conviene reintentar pronto. Con `provider=template` todas las respuestas son de plantilla y mantienen el TTL normal.
- **2026-10-30 · Validador robusto.** Una excepción inesperada dentro de la validación cae a la plantilla con motivo `VALIDATION_ERROR`, se registra en el log (solo la clase de la excepción) y en la métrica; nunca un 5xx.
- **2026-10-30 · Métricas.** `micrometer-core` 1.17.1 llega transitivo por `spring-ai-openai` (no se añadió dependencia; si algún día se quita Spring AI hay que declararlo). Hoy no existe un bean `MeterRegistry` (no hay actuator; una prueba lo fija), así que `AssistantMetrics` usa el registro global de Micrometer, que descarta las mediciones hasta que se añada un registro; Spring Boot agrega su registro al global, de modo que exponerlas solo requiere la decisión de operaciones (actuator, Prometheus u otro), pendiente. Medidores: `viora.assistant.answers` (contador, etiquetas `questionId` y `outcome` = created, cache, replay, unavailable, rate_limited), `viora.assistant.model` (timer por intento, etiquetas `outcome` = accepted, rejected, error, breaker_open y `reason` = el `TextRejection` o `none`) y `viora.assistant.tokens` (resumen de distribución, etiqueta `type` = input, output). Nunca ids de productor ni de lote. Los 429 del catálogo no van a `viora.assistant.answers` (no tienen pregunta); el 403 y el 409 por clave reusada tampoco se cuentan.
- **2026-10-30 · Retención.** `AssistantRetentionScheduler`: `@Scheduled(cron = viora.assistant.retention.cron, zone = America/Lima)`, por defecto `0 30 3 * * *`, `viora.assistant.retention.days=10` (no acepta menos que los 10 días del historial) y `viora.assistant.retention.enabled` (apagado en las pruebas). Nota: el job de telemetría usa `fixedDelay` cada 24 h, no un cron; aquí se pidió cron en hora de Lima. Si la instancia gratuita duerme a esa hora la corrida se pierde, sin daño: las respuestas de más de 10 días ya están ocultas. Borra en una transacción y con dos `delete` masivos: primero las claves creadas antes del corte, las que apuntan a respuestas anteriores al corte y las huérfanas; luego las respuestas anteriores al corte.
- **2026-10-30 · Una sola restricción para las claves.** Para cerrar la carrera de idempotencia sin una tabla nueva, `save` guarda también la clave de la respuesta nueva en `assistant_answer_requests` dentro de la misma transacción. La restricción única `(producer_id, client_request_id)` de esa tabla es ahora el único punto de choque entre un pedido que crea y otro que sirve la caché con la misma clave: el que llega segundo falla y relee la respuesta del primero. La clave sigue también en la fila de la respuesta. Las respuestas anteriores a esta rama solo tienen la clave en su fila; `bindRequest` sigue revisándola. Limitación: si los dos pedidos llegan antes de que exista la caché, igual pueden llamar dos veces al modelo (seguimiento "Llamadas duplicadas en paralelo", sigue abierto); el resultado final sí es una sola respuesta por clave.
- **2026-10-30 · Purga.** `deleteAllByProducerId` hace dos `delete` masivos (claves y respuestas) en una transacción. `AccountDeletionIntegrationTest` crea una respuesta y una clave servida desde la caché, comprueba que ambas tablas quedan vacías para el productor y toma los años de cosecha del `Clock`.

### Pendientes (límites, métricas y retención)

- **Exponer las métricas.** Actuator, Prometheus u otro destino: decisión de operaciones.
- **Límites y breaker por instancia.** Con más de una instancia cada una cuenta aparte; aceptable mientras Render use una.
- **Llamadas duplicadas en paralelo.** Sigue abierto (ver seguimiento de Spring AI).
- **Valores de los límites.** 20/5/60 son la propuesta del contrato; ajustar con datos reales.

### Seguimiento de la revisión de límites (2026-10-30)

La revisión nativa aprobó `28d6c47` sin bloqueos; el verificador independiente tampoco encontró bloqueos. Pendiente para un parche:

- **Circuit breaker semiabierto sin salida.** Si la llamada de prueba no informa éxito ni fallo (por ejemplo, lanza un `Error` en lugar de una excepción), el breaker queda semiabierto y rechaza todas las llamadas hasta reiniciar. Arreglo propuesto: liberar el turno de prueba en un `finally` o darle un plazo máximo. Lo señalaron el verificador y dos lentes de la revisión.
- **Éxito tardío.** Una llamada iniciada antes de abrirse el breaker que termina tarde puede cerrarlo o reabrirlo como si fuera la de prueba; impacto bajo.
- **Cuota con claves concurrentes.** Pedidos simultáneos con la misma clave nueva consumen cuota cada uno antes de la deduplicación.
- **Menores.** Nombre del helper de rechazo en `SpringAiAnswerWriter`, la retención del repositorio en memoria de las pruebas difiere de la de JPA.


### Seguimiento de la revisión de alertas y clima (2026-10-30)

Rama `feature/assistant-alert-climate` (pantallas ALERT y CLIMATE). El rango completo superó el presupuesto de contexto de la revisión nativa, así que se revisó solo el commit de Telemetría `76f76f3` (lentes risk, resilience, reliability): aprobado sin bloqueos. El commit del asistente `bdfcbd4` quedó sin revisión nativa por decisión del dueño (partirlo exigía reescribir `develop`). Pendiente para un parche, sin verificar todavía contra el repositorio:

- **Pronóstico caído sin degradación.** Vencido el TTL de 30 min, si Open-Meteo falla se devuelve vacío en lugar del último pronóstico bueno, y no se guarda el fallo: cada pedido vuelve a llamar al proveedor. Tampoco se capturan excepciones de `fetchSevenDayForecast`/`findPlotCentroid`.
- **Caché del pronóstico.** `get` + `put` sin `computeIfAbsent` (estampida al vencer) y sin desalojo de entradas.
- **`countPlotsWithOpenIncident`.** Si todos los ids son inválidos pasa una lista vacía a `findAllByPlotIds` (comprobar que no lance ni ignore el filtro; devolver 0 antes). Además carga todo el historial de incidentes y filtra en memoria.
- **Última lectura.** `findPlotSensors` toma `readings.getLast()` asumiendo orden ascendente del repositorio; fijarlo o ordenar.
- **Prueba del TTL.** Falta el caso en que el reloj pasa los 30 min y se vuelve a pedir el pronóstico.

## Pantalla Inicio (HOME): un lote o todo el campo

El dueño eligió "completo": HOME responde para un lote o, sin `plotId`, para todo el campo del productor (todos sus lotes activos). `home.daily_summary` queda para el paso del resumen diario.

- **2026-10-30 · Ámbito campo.** `plotId` ausente solo se acepta en pantallas donde `ScreenKey.requiresPlot()` es falso; hoy solo `HOME`. `AskQuestionCommand` lo decide por la pantalla de la pregunta y `GetLatestAnswerQuery` y `GetQuestionCatalogQuery` por la pantalla pedida: el resto sigue respondiendo 400 `assistant.plot.required`. Sin lote no hay control de lote existente (404) y `@plotAccess.isAccessible(null)` deja pasar; los lotes del campo salen siempre de `findActivePlotIdsByProducerId` con el productor autenticado, así que nunca se lee un lote ajeno.
- **2026-10-30 · Respuestas sin lote.** `assistant_answers.plot_id` pasa a admitir nulo (Riesgo 1, formato persistido). En PostgreSQL hay que correr `scripts/sql/2026-10-30-assistant-answers-field-scope.sql`: `ddl-auto=update` no quita un NOT NULL. Las consultas derivadas de Spring Data convierten el argumento nulo en `plot_id IS NULL`, así que una respuesta del campo nunca se sirve para un lote ni al revés (caché y última respuesta); lo prueba `AssistantAnswerRepositoryIntegrationTest`. La idempotencia compara el lote con `Objects.equals`: una clave usada para el campo y reenviada con un lote responde 409 `assistant.request.reused`. La API devuelve `plotId: null`.
- **2026-10-30 · Un solo lote activo.** Si el campo tiene un único lote activo, los textos hablan de "tu lote" (es el mismo lote); el hecho `scope` sigue diciendo `FIELD`. La etiqueta de alertas sí cambia por ámbito: "¿Qué alertas afectan a este lote?" con lote y "¿Qué alertas afectan a mis lotes?" sin él.
- **2026-10-30 · Lotes contados, nunca nombrados.** Las respuestas del campo cuentan lotes ("2 de tus 5 lotes") y no los nombran: el nombre de un lote puede llevar cifras que el validador numérico no puede respaldar, y el modelo no recibe identificadores de lote.
- **2026-10-30 · "¿Qué hago esta semana?".** Tres frases en este orden: aclareo, máxima del pronóstico y alertas. "Esta semana" es hoy y los 6 días siguientes. En el aclareo manda la ventana abierta (la que cierra antes), luego la que abre esta semana y luego el muestreo a medias; sin nada de eso lo dice. Con un lote se muestra el porcentaje del plan. La máxima es la más alta del pronóstico de todos los lotes (en empate, la del día más temprano) y "alcanza el umbral" lo decide Telemetría (`heatRisk`), nunca el asistente. Deshabilitada sin lotes activos (`assistant.reason.plots_missing`, "Sin lotes activos · registra un lote").
- **2026-10-30 · Muestreos pendientes.** No hay fachada de muestreos pendientes; se usa como aproximación un plan sin emitir con al menos un árbol muestreado y árboles que aún faltan (`treesNeeded > 0` de `PrescriptionAvailability`). Un lote nunca muestreado no cuenta como pendiente: no sabemos si esta campaña le toca. Decisión conservadora; revisar con el agrónomo si debe avisar también de lotes sin muestreo.
- **2026-10-30 · Alertas que cuentan.** Solo calor, helada y estrés hídrico (los tipos de ALERT); el invierno cálido pertenece a la pantalla de frío. La más grave se elige con la misma regla que ALERT (crítica primero, luego la más reciente).
- **2026-10-30 · Campaña frente a la anterior.** Se compara la última cosecha registrada con la anterior, no con el promedio (eso ya lo hace `alternation.vs_campaigns`), en kilos totales. Para el campo: los dos últimos años con cosecha de cualquier lote, sumando solo los lotes que registraron ambos años, para comparar los mismos lotes; el resto se cuenta como "fuera". Sin dos campañas comparables, o con 0 kg en la anterior, se deshabilita con "Registra al menos 2 campañas". El índice de vecería solo aparece con un lote: promediar índices entre lotes exigiría ponderarlos.
- **2026-10-30 · Cosecha estimada del campo.** Se da el rango de kg/ha estimados entre los lotes con proyección de calibre (US28), nunca una suma ni un promedio. La fachada de Orchard sí expone la superficie (`findPlotAreaHectares`), pero es el área del polígono, no la superficie plantada, y sumar kilos por área es una decisión de negocio: queda pendiente para el dueño. Con un lote, la respuesta es la misma proyección que `alternation.next_year`, dicha "para esta campaña". Deshabilitada sin calibre estimado ("Faltan datos de calibre · registra un muestreo").
- **2026-10-30 · Lecturas.** El catálogo de HOME solo lee planes de aclareo y cosechas por lote; la respuesta lee además alertas, índice, disponibilidad del muestreo y pronóstico (una llamada por lote, con la caché de Telemetría). Con muchos lotes son varias llamadas por pregunta; aceptable con los tamaños actuales.

### Pendientes (Inicio)

- **Total de kilos del campo.** Si el dueño quiere un total, decidir qué superficie usar (polígono o plantada) y si se suma kg/ha × ha.
- **Lotes sin muestreo en "esta semana".** Decidir con el agrónomo si un lote sin ningún muestreo en temporada debe aparecer como pendiente.

## Resumen del día (home.daily_summary)

Paso 4 del contrato (sección 4.4): `GET /api/v1/assistant/daily-summary?plotId={uuid?}` con un job diario. El dueño pidió avanzar dejando documentados los detalles.

- **2026-10-30 · Qué dice.** Es la respuesta de "¿Qué hago esta semana?" (`home.week_plan`): mismos hechos, mismo texto (aclareo, máxima del pronóstico y alertas) y mismas fuentes, para un lote o, sin `plotId`, para todo el campo. Cambian el `questionId` (`home.daily_summary`) y las tarjetas: tres del mismo tamaño, en este orden: días de ventana, máxima de la semana y alertas activas (las dos últimas son las del plan de la semana). No se inventa ningún dato nuevo; lo único que se calcula aparte son los días de la ventana.
- **2026-10-30 · Tarjeta de días de ventana.** Usa la ventana que ya elige el plan de la semana. Abierta (la que cierra antes): "N días de ventana · cierra el …", contando hoy, como `plan.best_time` (hecho `daysLeft`). Si abre en los próximos 7 días: "N días para abrir · abre el …" (hecho `daysUntilOpen`). Sin ninguna de las dos (aunque haya un muestreo a medias): "Sin ventana · ventana de aclareo · próximos 7 días", sin número. Las reglas de dato simulado del contrato no cambian: la máxima y las alertas salen de Telemetría igual que en el plan de la semana.
- **2026-10-30 · Disponibilidad.** No está en el catálogo (no es un chip de pregunta). Está disponible exactamente cuando lo está `home.week_plan`; sin lotes activos responde 409 con su motivo ("Sin lotes activos · registra un lote"). `POST /answers` con `home.daily_summary` responde 400 `assistant.question.invalid`: el resumen solo se pide por su ruta.
- **2026-10-30 · Tres tarjetas sin tocar las demás preguntas.** `QuestionId.factCards()` dice cuántas tarjetas lleva cada respuesta: 3 para el resumen, 2 para toda pregunta del catálogo. `AssistantAnswer` exige ese número exacto por pregunta y `AssembledFacts` acepta 2 o 3; lo prueban `AssistantAnswerTest` y `DailySummaryFactsAssemblerTest`. Sin cambio de esquema: las tarjetas van en `facts_json` (2000 caracteres alcanzan).
- **2026-10-30 · Job diario.** `AssistantDailySummaryScheduler`, a las 06:30 de Lima (`viora.assistant.daily-summary.cron`, mismo estilo que la retención). Prepara el resumen del campo, en español, de cada productor con lotes activos; los productores salen de la fachada de Orchard (`findAllActivePlotIds` y el dueño de cada lote, sin cambiar Orchard). Los resúmenes por lote y en inglés se generan bajo demanda. Un productor que falla no detiene al resto: se registra sin identificadores y se cuenta; uno sin plan activo o con el resumen del día ya vigente se salta. Como tope, `viora.assistant.daily-summary.max-producers` (1000); el resto lo genera su primer GET. Se apaga con `viora.assistant.daily-summary.enabled=false`. Una corrida perdida (instancia gratuita dormida) solo hace que el primer GET del día lo genere.
- **2026-10-30 · Bajo demanda y caché.** Se sirve el resumen guardado si es del mismo productor, ámbito (lote o campo), idioma y `dataVersion`, y se creó hoy en Lima (desde la medianoche); no aplica el TTL de 6 h de las respuestas: el resumen es "del día". Si los datos cambiaron (otra alerta, otra ventana, otro pronóstico) se genera uno nuevo y nunca se sirve el viejo como vigente; por eso `freshness` sale siempre `UP_TO_DATE`, y el viejo, abierto por `/answers/{id}`, se ve `STALE`. Con modelo, un resumen de plantilla (fallback) solo se reutiliza durante `fallback-cache-ttl`, como las respuestas.
- **2026-10-30 · Idempotencia.** Es un GET sin `clientRequestId`: cada resumen guarda una clave propia generada en el servidor (la tabla la exige y es única por productor). Dos GET simultáneos pueden guardar dos resúmenes iguales; se sirve el más reciente. Es inocuo y evita bloqueos.
- **2026-10-30 · Límites.** El job no gasta los límites del productor. Cada GET lee las fachadas como una lectura del catálogo y cuenta contra ese límite (60/h); generar un resumen nuevo bajo demanda cuenta además como una respuesta (20/h). Servir el guardado no gasta respuestas. Pasado un límite: 429 con `Retry-After`. Métricas: el mismo contador `viora.assistant.answers` con `questionId=home.daily_summary` (created, cache, unavailable, rate_limited).
- **2026-10-30 · Historial y última respuesta.** Los resúmenes se guardan con las respuestas (misma retención de 10 días y misma purga de cuenta), pero no salen en `GET /answers` (Recientes) ni en `GET /answers/latest`: no son preguntas que el productor hizo, y la entrada de Inicio debe seguir abriendo en su última pregunta. Sí se pueden reabrir por `GET /answers/{id}`.
- **2026-10-30 · Seguridad.** Mismas reglas que las respuestas: sesión (401 ProblemDetail), plan activo (403), `@plotAccess.isAccessible(#plotId)` (403 lote ajeno), lote inexistente 404, `plotId` mal formado 400. Sin `plotId` se leen solo los lotes activos del productor autenticado.

### Pendientes (resumen del día)

- **Idioma del job.** Se precalcula en español porque no hay preferencia de idioma del productor expuesta por una fachada; si se agrega, el job puede usarla.
- **Aviso "tu resumen está listo".** Sigue descartado (contrato 4.4).

## Pantalla Campaña cerrada (CAMPAIGN_CLOSED)

Preguntas del contrato: `campaign.vs_previous` (8,3 t/ha, +36 % sobre el promedio), `campaign.caliber_change` y `campaign.plan_next`. La pantalla lee el lote como PLOT_DETAIL (`ScreenKey.readsOnePlot`: cosechas, índice de vecería, plan de aclareo) y, solo al responder, la superficie del lote y el aclareo de la campaña cerrada. No se agregó nada a las fachadas de Raleo ni de Fenología: `findThinningOutcome` y `findPlotAreaHectares` ya existían.

### Qué es la campaña cerrada (decisión menor, 2026-10-30)

- Es la última campaña con cosecha registrada en Fenología (`POST /api/v1/plots/{plotId}/harvest-records`), no el año calendario. Fenología solo acepta cosechas de años que ya pasaron o del año en curso, así que registrar la cosecha es lo que cierra la campaña.
- No se usa la liquidación (`HarvestSettlement`) para decidirlo: no todos los productores liquidan y Liquidación no tiene fachada para otros contextos.
- Sin cosecha registrada, `vs_previous` y `plan_next` se deshabilitan con "Sin cosecha registrada · registra la cosecha de la campaña" (`assistant.reason.harvest_missing`). Las etiquetas nombran el año de la campaña cerrada (`vs_previous`) y el de la siguiente (`plan_next`); sin cosecha usan una etiqueta sin año.
- Si ya empezó la campaña siguiente (por ejemplo, ya registraste la floración), `plan_next` igual repite los pasos: la pantalla es la del cierre y no mezcla datos de la campaña en curso.

### `campaign.vs_previous` en t/ha (decisión menor, 2026-10-30)

- Compara la cosecha de la campaña cerrada con el promedio de todas las campañas anteriores, igual que `alternation.vs_campaigns`, y no solo con la anterior (eso lo hace `home.campaign_vs_previous`). Así sale el "+36 % sobre el promedio" del contrato.
- El contrato pide t/ha. La superficie sale de `OrchardContextFacade.findPlotAreaHectares`: es el área del polígono del lote, la misma que usan el expediente agronómico (su tabla de cosechas en t/ha) y la estimación de cosecha de Raleo. En HOME no se usó para el campo porque sumar lotes es una decisión de negocio; para un solo lote no hay suma, así que aquí sí se usa. La fuente de la respuesta lo dice: "Superficie del lote según su polígono: N ha".
- Las t/ha van con un decimal y los kilos entre paréntesis. El porcentaje se calcula con los kilos, así el redondeo de las toneladas nunca lo cambia.
- Sin superficie (un lote activo siempre la tiene, pero se cubre igual) la respuesta queda en kilos.
- Se deshabilita con menos de 2 campañas ("Registra al menos 2 campañas", el motivo que ya usaba `alternation.vs_campaigns`).

### `campaign.caliber_change`: se ofrece siempre deshabilitada (decisión menor, 2026-10-30)

**Por qué.** El contrato pide comparar el calibre de la campaña cerrada con el de las anteriores, pero el asistente no tiene ese dato:

- El calibre real de cada campaña (`commercialFruitsPerKg`) solo se guarda en Liquidación (`HarvestSettlement`), cuando el productor liquida la cosecha. Liquidación no expone una fachada de solo lectura a otros contextos, y este cambio no podía tocar ese módulo.
- Fenología no guarda calibre en sus registros de cosecha.
- Raleo guarda observaciones de calibración (`CaliberCalibrationObservation`) con ese mismo calibre, pero solo de campañas con aclareo a tiempo, y las busca por variedad. Compararlas sería comparar un subconjunto sesgado de campañas como si fueran todas.
- La proyección de calibre de Raleo es una estimación de la campaña en curso, no el calibre de una campaña cerrada.

Responder con cualquiera de esos datos obligaría a inventar o a sesgar la comparación. Se evaluaron dos caminos:

- **A (elegido):** mostrar la pregunta deshabilitada con un motivo, como `sensors.why_paused` y `sampling.correct_tree`.
- **B:** responder con las observaciones de calibración de Raleo. Se descartó porque deja fuera las campañas sin aclareo o con aclareo tardío y no lo diría la cifra.

**Cómo.**

- `QuestionCatalog.campaignClosed` ofrece la pregunta con `enabled: false` y `disabledReason` "El calibre de cada campaña aún no llega al asistente" (`assistant.reason.caliber_history_unavailable`), con o sin cosechas.
- No hay ensamblador registrado para ella. Si la app la pide igual, la API responde 409 con ese mismo motivo.
- Lo fijan `CampaignClosedQuestionCatalogTest`, `CampaignClosedFactsAssemblersTest.registeredAndNeedTheHarvest` y `AssistantCampaignClosedScreenIntegrationTest`.

**Para qué.**

- La app muestra el catálogo completo del contrato, con la pregunta en su lugar, sin una respuesta falsa.
- El equipo ve en un solo lugar qué falta para activarla.

**Cuándo activarla.** Cuando el calibre liquidado llegue al asistente:

1. una fachada de solo lectura en Liquidación (por ejemplo `settlement/interfaces/acl/SettlementContextFacade.java`, nueva) que devuelva el calibre (`commercialFruitsPerKg` y su categoría comercial) por lote y campaña;
2. en el asistente, un servicio ACL que la lea, habilitar la pregunta solo cuando la campaña cerrada tenga calibre y al menos una campaña anterior también, y agregar su ensamblador (calibre de la cerrada frente al de las anteriores, en frutos por kilo y categoría).

### `campaign.plan_next` (decisión menor, 2026-10-30)

- Solo dice lo que Viora ya sabe; nunca proyecta la cosecha de la campaña siguiente, porque ningún contexto lo hace (igual que en THINNING_DONE). Un test verifica que la respuesta no trae kilos.
- Tres partes:
  - **Vecería.** El índice y su banda, de Fenología. Con alternancia moderada o severa agrega que la herramienta del plan es el aclareo según la carga de frutos, la misma idea de `alternation.break_cycle`. Con producción regular solo da el índice. Sin índice (menos de 2 campañas) se omite esta parte.
  - **Aclareo de la campaña cerrada.** Lo lee de `findThinningOutcome(lote, año cerrado)`: si se registró, en qué fecha y si fue dentro de la ventana o fuera de ella. Fuera de la ventana dice que su efecto sobre la vecería es menor, como el aviso que Raleo ya muestra al registrarlo tarde. Sin aclareo confirmado lo dice.
  - **Pasos de la campaña siguiente.** Registrar la plena floración y muestrear para que Viora calcule el plan. Si el aclareo de la campaña cerrada trae los días de la ventana del perfil de la variedad, los da (de N a M días después de la floración); no da fechas porque no hay datos para darlas.
- Se ofrece siempre que haya una cosecha registrada.
- Tarjetas: la campaña siguiente ("próximo muestreo", "tras la plena floración") y el índice de vecería; sin índice, el aclareo de la campaña cerrada ("Registrado" con su fecha o "Sin aclareo").

## Pantalla Aclareo registrado (THINNING_DONE)

Preguntas del contrato: `thinning.harvest_change` (8,2 t/ha y 2027 más estable), `thinning.why_target` y `thinning.next_sampling`. La pantalla lee el lote como PLOT_DETAIL (`ScreenKey.readsOnePlot`) y además el resultado del aclareo confirmado.

### Fachada nueva de Raleo: `findThinningOutcome` (solo lectura, 2026-10-30)

**Por qué.** Raleo ya calcula la cosecha estimada con la carga de antes del aclareo (`ThinningPrescriptionOutlookService.outlookOf`, la misma que muestra la pantalla del plan), pero `findThinningPlanDetail` solo exponía la de después. Tampoco exponía la carga que quedó, el estado de esa carga, el porcentaje que el productor dice haber quitado, el estado del perfil de la variedad ni los días de la ventana después de la floración.

**Cómo.**

- Se agregó `ThinningContextFacade.findThinningOutcome(plotId, campaignYear)` con el record `ThinningOutcome`. Es un método nuevo: `findThinningPlanDetail` no cambió.
- Solo devuelve algo con el aclareo confirmado. Las cosechas sin estimación (menos de 3 cosechas anteriores o sin superficie) vienen en null.
- Lee las mismas estimaciones que la pantalla del plan, así el asistente nunca muestra otra cifra.
- Lo cubren dos tests nuevos en `ThinningContextFacadeImplTest`.

### "2027 más estable": no se afirma (decisión menor, 2026-10-30)

El mockup del contrato promete que el año siguiente será "más estable". Ningún contexto proyecta la campaña siguiente: el índice de vecería de Fenología sale solo de cosechas pasadas, y Raleo no relaciona el aclareo con ese índice. Afirmarlo sería inventar. En su lugar, `harvest_change` dice dónde quedó la carga frente al objetivo: dentro, hasta un 30 % por encima, o más de un 30 % por encima ("el árbol sigue sobrecargado"), con los umbrales de `LoadState` de Raleo. El glosario del modelo le pide no afirmar nada sobre la campaña siguiente, y un test verifica que la respuesta no nombra ese año.

### Resto de la pantalla (2026-10-30)

- **`thinning.harvest_change`.** Da la cosecha estimada después del aclareo frente a la de sin aclarear. Cuando la variedad no tiene modelo de calibre (`FRUIT_COUNT_ONLY`), la cifra sin aclarear es un techo, porque con más fruta el calibre baja, y la respuesta lo dice. Se deshabilita sin estimación ("Faltan cosechas registradas o la superficie del lote para estimar la cosecha").
- **`thinning.why_target`.** El objetivo no se calcula del lote: sale del perfil de aclareo configurado para la variedad (`viora.thinning.profiles.<variedad>.*`). La respuesta lo explica, dice si el perfil está aprobado por un agrónomo, es provisional o es de demostración (`SYNTHETIC_DEMO`, el de los datos demo), y cuenta dónde quedó la carga después del aclareo.
- **`thinning.next_sampling`.** Raleo no tiene una regla de "cuándo volver a muestrear". La respuesta dice que esta campaña ya no pide muestreos y que en la siguiente hay que registrar la plena floración y muestrear antes de que abra la ventana, que va de N a M días después de la floración según el perfil. No da fechas porque no hay datos para darlas.
- **Antes del aclareo.** Las tres preguntas se deshabilitan con "Sin aclareo registrado en esta campaña".

## Pantalla Resumen de muestreo (SAMPLING_SUMMARY)

Preguntas del contrato: `sampling.enough_trees` (5/5 y 0,59 frutos por brote vs objetivo 0,40; mínimo 5 árboles), `sampling.meaning`, `sampling.next` y `sampling.correct_tree`. La pantalla lee el lote como PLOT_DETAIL (`ScreenKey.readsOnePlot`: plan de aclareo y árboles que faltan de Raleo); no se agregó nada a Raleo.

### `sampling.correct_tree`: se ofrece siempre deshabilitada (decisión del dueño, 2026-10-30)

**Por qué.** El contrato pide explicar cómo corregir un árbol mal registrado, pero Raleo no lo permite:

- El único endpoint de muestreo es `POST /api/v1/plots/{plotId}/samplings`, que agrega una ronda nueva. No hay forma de editar ni de borrar el registro de un árbol.
- Volver a registrar el árbol con la misma etiqueta tampoco lo corrige; ver el hallazgo de abajo.

Decirle al productor "vuelve a registrarlo" sería un mal consejo. Se evaluaron dos caminos:

- **A (elegido):** mostrar la pregunta deshabilitada con un motivo.
- **B:** implementar la corrección en Raleo, haciendo que el último registro de una etiqueta reemplace al anterior o agregando un endpoint de edición. Se descartó por ahora: cambia cómo se calcula la carga de las prescripciones (y con ella el porcentaje a quitar y el calibre), así que es una decisión de negocio y un cambio de mayor riesgo en otro contexto.

**Cómo.**

- `QuestionCatalog.sampling` ofrece la pregunta con `enabled: false` y `disabledReason` "Corregir un árbol todavía no está disponible" (`assistant.reason.tree_correction_unavailable`).
- No hay ensamblador registrado para ella. Si la app la pide igual, la API responde 409 con ese mismo motivo.
- Lo fijan `SamplingQuestionCatalogTest`, `SamplingFactsAssemblersTest.registered` y `AssistantSamplingSummaryScreenIntegrationTest`.

**Para qué.**

- La app muestra el catálogo completo del contrato sin dar un consejo que empeore el plan.
- El equipo ve en un solo lugar qué falta para activarla.

**Hallazgo para Raleo: un árbol registrado dos veces distorsiona la carga.**

- `SamplingCoverageEvaluator.countUniqueEvaluatedTrees` cuenta etiquetas únicas, así que el árbol repetido cuenta una sola vez para la representatividad.
- `computeMeanFruitsPerShoot` en cambio suma todos los registros, así que los brotes y frutos del árbol entran dos veces en la carga (el registro malo y el nuevo).
- Resultado: si un productor intenta "corregir" un árbol cargándolo de nuevo, la carga, el porcentaje a quitar y el calibre proyectado se calculan con datos duplicados, sin ningún aviso.
- No se tocó; queda para que el equipo decida la regla (por ejemplo, que el último registro de cada etiqueta reemplace al anterior).

**Cuándo activarla.** Cuando Raleo permita corregir un árbol:

1. una regla o un endpoint que reemplace el registro de una etiqueta en la campaña;
2. el promedio de la carga calculado con un solo registro por etiqueta;
3. en el asistente, habilitar la pregunta y agregar su ensamblador con los pasos.

### Resto de la pantalla (2026-10-30)

- **Mínimo de árboles.** Raleo solo expone los árboles que faltan, no el mínimo. El asistente muestra 5, el mismo `SamplingCoverageEvaluator.MIN_REPRESENTATIVE_TREES` de Raleo; un test falla si dejan de coincidir.
- **`sampling.enough_trees`.** Árboles muestreados sobre el mínimo ("5/5 · muestra representativa" o "3/5 · faltan 2") y, con la prescripción emitida, la carga frente al objetivo. Se deshabilita sin muestreo en la campaña.
- **`sampling.meaning`.** La carga frente al objetivo, cuántas veces lo supera y el porcentaje que quita el plan. La razón se calcula en decimal con las cifras que se muestran (0,59 / 0,40 = 1,48), para que coincida con la cuenta que haría el productor. Se deshabilita sin prescripción ("Sin plan de aclareo activo").
- **`sampling.next`.** El paso siguiente según el aclareo: muestrear los árboles que faltan, revisar el plan si aún no se calcula, esperar la ventana, aclarear (con el botón "Registrar aclareo" de PLAN mientras la ventana está abierta), consultar al técnico si la ventana cerró, o nada si el aclareo ya se registró.
- **Redacción.** Se usa "aclarear", como en PLAN. Se corrigió "aclarar" en `plot.today`.

## Pantalla Sensores (SENSORS)

Preguntas del contrato: `sensors.what_say` (2 nodos activos, 27,4 °C), `sensors.why_paused`, `sensors.reliability` y `sensors.how_link`. Todo sale de Telemetría por la fachada (`findPlotSensors` y `findTelemetryCoverage`, ambas de solo lectura y ya existentes); no se agregó nada a Telemetría.

### `sensors.why_paused`: se ofrece siempre deshabilitada (decisión del dueño, 2026-10-30)

**Por qué.** El contrato pide explicar por qué un nodo está en pausa, pero hoy ningún nodo puede estarlo:

- `DeviceStatus.PAUSED` existe en el enum, pero ningún método de `IoTDevice`, comando, endpoint ni proceso automático lo asigna. Los nodos solo pasan de `ACTIVE` (al registrarse) a `UNLINKED` (al desvincularse).
- No se guarda ni el motivo ni la fecha de una pausa.
- `findPlotSensors` devuelve solo los nodos activos.

Responder "por qué está en pausa" obligaría al asistente a inventar un motivo, y la regla del asistente es no inventar nunca una cifra ni una causa. Se evaluaron dos caminos:

- **A (elegido):** mostrar la pregunta deshabilitada con un motivo.
- **B:** implementar la pausa en Telemetría, con un comando que guarde el motivo y la fecha. Se descartó por ahora porque es una decisión de negocio (quién pausa, con qué motivos, qué pasa con las lecturas y las alertas del nodo pausado) y un cambio grande en otro contexto.

**Cómo.**

- `QuestionCatalog.sensors` ofrece la pregunta con `enabled: false` y `disabledReason` "Ningún nodo de este lote está pausado" (`assistant.reason.no_paused_nodes`).
- No hay ensamblador registrado para ella. Si la app la pide igual, la API responde 409 con ese mismo motivo, como cualquier pregunta deshabilitada.
- Lo fijan `SensorsQuestionCatalogTest`, `SensorsFactsAssemblersTest.registered` y `AssistantSensorsScreenIntegrationTest`.

**Para qué.**

- La app muestra el catálogo completo del contrato, con la pregunta en su lugar, sin una respuesta falsa.
- El equipo ve en un solo lugar qué falta para activarla.

**Cuándo activarla.** Cuando Telemetría tenga la pausa:

1. un comando y un endpoint que pasen el nodo a `PAUSED` guardando el motivo y la fecha;
2. un método de solo lectura en `TelemetryContextFacade` que devuelva también los nodos pausados con esos datos;
3. en el asistente, habilitar la pregunta solo cuando haya un nodo pausado (si no, mantener el mismo motivo) y agregar su ensamblador con el motivo y la fecha.

### Resto de la pantalla (2026-10-30)

- **`sensors.what_say`.** Es la misma respuesta que `climate.sensors_say` (`SensorsSayFactsAssembler` ahora recibe la pregunta) con sus propios textos. Se deshabilita igual: sin nodos ("Vincula un nodo al lote para ver sus lecturas") o sin lectura en las últimas 24 horas ("Sin lecturas recientes de los nodos"). Con lecturas simuladas lo dice ("dato simulado"), como pide el contrato.
- **`sensors.reliability`.** Dice de dónde salen las lecturas y en cuántos de los últimos 7 días (hoy incluido, zona de Lima) hubo al menos una lectura, con el día de la última. Hoy todos los nodos se reportan simulados (`NODES_SIMULATED = true` en `TelemetryContextFacadeImpl`), así que la respuesta pide usarlas como referencia. Se deshabilita sin nodos.
- **`sensors.how_link`.** Siempre se ofrece. Cuenta los nodos de microclima y las sondas de suelo del lote y explica lo que Telemetría pide para vincular uno: nombre único en el lote, tipo y, para la sonda, la profundidad de 1 a 200 cm. No tiene botón (DEEPLINK) porque no hay una ruta acordada con la app móvil hacia el formulario de nodos; cuando exista, se agrega la acción.
- **Lecturas por lote.** Telemetría guarda las lecturas por lote, no por nodo, y `lastReadingTimestamp` del nodo nunca se actualiza. Por eso la confiabilidad se mide por lote, no por nodo.

## Pantalla Detalle de lote (PLOT_DETAIL)

Preguntas del contrato: `plot.how_is` (vecería 0,51 + 12 días), `plot.today`, `plot.last_sampling` y `plot.next_year_harvest` (deshabilitada sin calibre). La pantalla lee el lote igual que HOME lo lee para un solo lote (`HomeDataReader`: plan de aclareo, árboles que faltan, alertas, cosechas, índice de vecería y pronóstico); no se agregó ningún método a las fachadas de otros contextos.

- **2026-10-30 · Sin fachada nueva de Raleo.** El plan del ejemplo (L15) era agregar a `ThinningContextFacade` un método de "última ronda". No hizo falta: `findThinningPlanDetail` ya expone los árboles muestreados, el día del último muestreo, la carga medida y el objetivo. Raleo resume las rondas y nunca las expone una por una, así que `plot.last_sampling` habla del "muestreo de esta campaña" y da el día de la última ronda; la carga es la de todo el muestreo, no la de esa ronda. Si producto quiere las cifras de una sola ronda, hay que agregar un método de solo lectura a la fachada (decisión pendiente).
- **2026-10-30 · "vecería 0,51 + 12 días".** `plot.how_is` da el índice con su banda y dónde está el aclareo de la campaña. Los "12 días" son los que quedan de la ventana abierta, hoy incluido, la misma regla que PLAN (`PlanFactsAssembler`). Con los datos de muestra (ventana hasta el 10 de noviembre, hoy 30 de octubre, índice 0,5123) da justo el ejemplo del contrato; un test lo fija. Fuera de la ventana dice si está por abrir, si cerró sin aclareo, si el aclareo ya se registró o cuántos árboles le faltan al muestreo. Se deshabilita solo sin índice y sin muestreo ("Sin índice de vecería ni muestreo para este lote todavía").
- **2026-10-30 · `plot.today`.** Es `home.week_plan` reducido a hoy y a un lote: el aclareo (ventana abierta, por abrir o muestreo a medias), el pronóstico de hoy frente a los umbrales de calor y helada de Telemetría, y las alertas abiertas. Siempre se ofrece; sin nada pendiente lo dice. Sin el día de hoy en el pronóstico dice "No hay pronóstico disponible para hoy".
- **2026-10-30 · `plot.last_sampling`.** Se deshabilita sin árboles muestreados en la campaña ("Sin muestreo en esta campaña · registra un muestreo"). Con la prescripción emitida explica la carga frente al objetivo y el porcentaje; sin ella, los árboles que faltan, o que el plan aún no se calcula.
- **2026-10-30 · `plot.next_year_harvest`.** Responde con la misma lógica y la misma redacción que `alternation.next_year` (`NextYearFactsAssembler` ahora recibe la pregunta) y se deshabilita igual que `home.harvest_estimate`, sin calibre estimado ("Faltan datos de calibre · registra un muestreo"). Hereda la duda ya registrada: la etiqueta dice el año siguiente, pero la cifra es la cosecha que Raleo proyecta para esta campaña.

## Pantalla Frío (WINTER_CHILL)

Preguntas del contrato: `chill.missing`, `chill.reach_30`, `chill.vs_last_winter` y `chill.if_incomplete`. Los datos salen de Fenología (`PhenologyContextFacade.findChillSeason`), con el mismo modelo dinámico de Erez, el mismo clima horario de Open-Meteo y el mismo requisito varietal (`DEFAULT_VARIETAL_CHILL_THRESHOLD`, 30) que la métrica de frío; el asistente nunca recalcula porciones.

- **2026-10-30 · Qué invierno.** El de la métrica de frío (`ChillSeasonEvaluator.seasonYearFor`): del 1 de junio al 31 de agosto, el invierno en curso; del 1 de septiembre al 31 de mayo, el último invierno terminado. Fuera de temporada no se deshabilita la pantalla: responde sobre ese invierno y las etiquetas nombran su año ("¿Cuánto frío acumuló el invierno 2026?"). Es lo mismo que ya muestra la pantalla de frío fuera de temporada.
- **2026-10-30 · Invierno pasado.** Es el año anterior al invierno de la pantalla, evaluado en toda su temporada. En temporada se compara a la misma fecha (porciones del invierno pasado el mismo día del año) y se dice cómo terminó; si ese día no tiene datos, solo su total. Fuera de temporada se comparan los dos totales y los días sobre 24 °C.
- **2026-10-30 · Brotación.** La fecha de brotación de la pantalla es el 31 de agosto, fin de la temporada de frío, como en el mockup del contrato ("fecha estimada 4 ago vs brotación 31 ago"); no es una brotación observada en el lote. Pendiente de validar con el agrónomo junto con la regla de >24 °C y las 30 porciones.
- **2026-10-30 · Fecha estimada.** `chill.reach_30` da la fecha que proyecta Fenología con el ritmo de los últimos 14 días (`PROJECTED`) y la llama "estimación". Si a ese ritmo no llega antes del 31 de agosto (`NOT_REACHABLE_IN_SEASON`) lo dice y da lo que falta. Con menos de 14 días evaluados (`INSUFFICIENT_DATA`) la pregunta se deshabilita: "Faltan días de invierno para estimar la fecha". Si ya completó el requisito, da el día en que lo hizo.
- **2026-10-30 · Sin datos.** Sin ubicación del lote, con el clima no disponible o sin ningún día evaluado (1 de junio) las cuatro preguntas se deshabilitan con "Sin datos de frío para este lote todavía". Sin datos del invierno pasado solo se deshabilita la comparación ("Sin datos del invierno pasado para este lote").
- **2026-10-30 · Cifras.** Las porciones se muestran con un decimal y lo que falta es el requisito menos las porciones mostradas, así las dos cifras suman el requisito (24,4 + 5,6 = 30).
- **2026-10-30 · "¿Qué pasa si no llego?".** Redacción conservadora y sin cifras de efecto: con frío insuficiente la brotación y la floración pueden salir más tardías y desparejas, y se remite al técnico (mismo tono que las alertas). Viora no estima el efecto en la cosecha. Pendiente de validar con el agrónomo.
- **2026-10-30 · Fuente.** "Modelo de frío de Erez con el clima horario de Open-Meteo, hasta el {día}". El contrato dice "estación de frío EREZ", pero el dato no sale de una estación en el lote sino del clima horario del punto central del lote; la fuente lo dice así para no inducir a error.
- **2026-10-30 · Identificador `chill.reach_30`.** Se mantiene el del contrato; el formato de `QuestionId` ahora admite dígitos en las palabras después de la primera.
- **2026-10-30 · Caché de Fenología.** `findChillSeason` lee el clima (segundos), así que guarda cada temporada por lote y año: 30 minutos un invierno en curso y 24 horas uno terminado (Open-Meteo corrige días recientes). Un fallo del clima o de Orchard devuelve vacío y no se guarda. Un candado por lote y temporada evita que pedidos simultáneos llamen varias veces al proveedor; solo se guardan lotes con ubicación y las entradas vencidas se borran al guardar. Corre fuera de transacción. La métrica de frío y la fachada comparten la lectura en `ChillSeasonReader`; `WinterWarmthMonitor` queda igual.

### Seguimiento de la verificación de frío (2026-10-30)

El verificador independiente aprobó la rama `feature/assistant-winter-chill` sin bloqueos (la fachada coincide con la métrica CHILLING campo por campo; 300 pedidos concurrentes sobre 3 lotes × 2 temporadas hicieron 6 lecturas del clima). La revisión nativa se omitió por decisión del dueño. Pendiente para un parche:

- **Redondeo justo debajo del umbral.** Con 29,96 porciones sin completar, el redondeo a un decimal muestra "30 de 30 … le faltan 0" (`ChillFactsAssembler`). Mostrar "le falta menos de 0,1" o no redondear hacia el umbral.
- **Mapa de locks sin limpieza.** `seasonLocks` en `PhenologyContextFacadeImpl` guarda un lock por lote y temporada consultada durante toda la vida del proceso (acotado por lotes reales, unas 2 por lote al año).
- **Desfase por la caché.** Hasta 30 minutos después de medianoche o del 31 de agosto la fachada puede mostrar un día menos evaluado o el estado anterior que la métrica.
- **Temporadas antiguas.** `findChillSeason` acepta cualquier temporada pasada de un lote real y la guarda; hoy solo se piden la actual y la anterior.
