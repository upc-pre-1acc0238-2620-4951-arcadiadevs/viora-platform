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
