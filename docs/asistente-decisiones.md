# Asistente Viora · registro de decisiones técnicas

Decisiones menores tomadas durante la implementación del backend del asistente. Las de producto o de negocio no se toman aquí: se anotan como pendientes. El contrato general está en `viora-asistente-spring-ai-contrato.md`; los problemas previos encontrados en el camino, en `parches-pendientes.md`.

Formato: fecha, decisión, porqué. Las más nuevas al final.

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
- **Retención.** Las respuestas de más de 10 días se ocultan pero no se borran; el job diario queda para `feature/assistant-limits-metrics`.

### Seguimiento de la revisión (2026-10-30)

La revisión nativa aprobó los tramos `280922b` (aplicación) y `fa2a46e` (endpoints y persistencia) sin bloqueos. Advertencias anotadas para `feature/assistant-limits-metrics` o un parche posterior:

- **Carreras de idempotencia.** La clave puede quedar en la fila de la respuesta o en `assistant_answer_requests`; dos pedidos simultáneos con la misma clave, uno creando y otro sirviendo caché, no comparten una restricción única entre tablas. Hoy se resuelve volviendo a leer tras el conflicto; falta una prueba de concurrencia y, si hace falta, una sola tabla de claves.
- **Purga.** `deleteAllByProducerId` carga y borra fila por fila, en dos tablas sin transacción explícita. Para cuentas con muchas respuestas conviene un borrado masivo transaccional; la prueba de borrado de cuenta no verifica la tabla de claves.
- **Frescura del historial.** `GET /answers` recalcula los hechos de cada respuesta (hasta 10) y no degrada si una fachada falla. Conviene cachear el `dataVersion` actual por pregunta y lote, y responder `STALE` ante un error.
- **Pruebas con años fijos.** `AccountDeletionIntegrationTest` usa cosechas 2024/2025; derivarlas del `Clock`.
- **Menores.** Nombre del ensamblador de recursos, límite inclusivo/exclusivo del TTL distinto entre caché e historial, comentario de la propiedad `cache-ttl`.

