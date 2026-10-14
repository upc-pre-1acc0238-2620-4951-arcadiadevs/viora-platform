# Architectural Decision Record: ADR-011
## Delimitación de Alertas a Incidentes Agroclimáticos (`AgroclimaticIncident`) y Separación de Notificaciones Push

* **Status:** Accepted
* **Context:** Agroclimatic Telemetry & Sensor Monitoring (`bc5`) & Mobile Android App
* **Date:** 2026-10-04
* **Authors:** Fabrizio Santi / Backend & Mobile Team
* **Related Documentation:**
  * [docs/context-map.md](file:///home/santi2007939/workspace/apps/1acc0238/viora-platform/docs/context-map.md)
  * [docs/26-tactical-ddd.md](file:///home/santi2007939/workspace/apps/1acc0238/viora-platform/docs/26-tactical-ddd.md) (bc5 Telemetry, tablas 66-68)
  * [docs/adr-010-telemetry-phenology-chill-integration.md](file:///home/santi2007939/workspace/apps/1acc0238/viora-platform/docs/adr-010-telemetry-phenology-chill-integration.md)
  * [mobile-consideraciones/propuesta_alertas_backend_ddd.md](file:///home/santi2007939/workspace/apps/1acc0238/viora-platform/mobile-consideraciones/propuesta_alertas_backend_ddd.md)
  * [mobile-consideraciones/us18_alertas_mockups.md](file:///home/santi2007939/workspace/apps/1acc0238/viora-platform/mobile-consideraciones/us18_alertas_mockups.md)

---

### 1. Contexto y Problema

Durante el análisis para la implementación de la **US18 (Alertas automáticas)** y el soporte a las pantallas de la app Android **T14 (Centro de alertas)**, **T15 (Detalle de alerta)** y **P10 (Widget en Home)**, se evaluó cómo debía estructurarse el concepto de "Alerta":

1. ¿Debería el sistema de alertas unificar todos los avisos de la plataforma en un único modelo genérico (incluyendo estrés hídrico de `Telemetry`, sobrecarga frutal de `Thinning`, anomalías de `Phenology` o cobros de `Subscription`)?
2. ¿Debería crearse un Bounded Context nuevo (un 10.º contexto) o situarse en un `shared` común?
3. ¿Cómo coexisten las alertas con las futuras notificaciones push (FCM) y la navegación en la aplicación móvil?

---

### 2. Decisión Adoptada

Se aprueban las siguientes directrices arquitectónicas:

1. **El módulo de Alertas gestiona exclusivamente Incidentes Agroclimáticos (`AgroclimaticIncident`) y reside 100% en el Bounded Context de `Telemetry` (`bc5`).**
2. **No se crea un nuevo Bounded Context ni se utiliza un `Shared Kernel` en `shared`.**
3. **Las advertencias o estados de otros Bounded Contexts (como la sobrecarga frutal en `Thinning` o reportes en `Harvest`) conservan su ciclo de vida y sus pantallas naturales en sus respectivos contextos.**
4. **Las Notificaciones Push se desacoplan como un servicio transversal de infraestructura** que reacciona a eventos de dominio publicados por cualquier Bounded Context (`AgroclimaticIncidentRaisedEvent`, `OverloadRiskDetectedEvent`, etc.) y despacha mensajes al móvil con enlaces profundos (*Deep Links*) directos a la pantalla donde se atiende cada necesidad.

```mermaid
flowchart TD
    subgraph BC5["bc5: Agroclimatic Telemetry & Sensor Monitoring"]
        AI["AgroclimaticIncident (Aggregate Root)\n• HYDRIC_STRESS\n• HEAT_WAVE\n• FROST_WARNING\n• WARM_WINTER_ENOS"]
        TE["AgroclimaticThresholdEvaluator"]
        AI -->|Emite| E1["AgroclimaticIncidentRaisedEvent"]
    end

    subgraph BC1["bc1: Crop Load & Thinning Advisory"]
        TH["FruitThinningPrescription (AR)"]
        TH -->|Emite| E2["OverloadRiskDetectedEvent"]
    end

    subgraph INFRA_NOTIF["Cross-Cutting Notification Service (Infra / FCM)"]
        LST["Domain Event Listeners\n(Spring @EventListener)"]
        FCM["Firebase Cloud Messaging Dispatcher"]
        LST --> FCM
    end

    subgraph MOBILE["Mobile App (Android)"]
        T14["T14: Centro de Alertas Agroclimáticas\n(GET /api/v1/agroclimatic-incidents)"]
        T15["T15: Detalle de Incidente & Checklist\n(GET .../agroclimatic-incidents/{id})"]
        P60["P60: Plan de Aclareo Frutal\n(Pantalla nativa de Thinning)"]
    end

    E1 -.->|Spring Event| LST
    E2 -.->|Spring Event| LST

    FCM -->|Push con Deep Link viora://incidents/{id}| T15
    FCM -->|Push con Deep Link viora://plots/{id}/thinning| P60

    T14 -->|Ver qué hacer| T15
```

---

### 3. Justificación y Razones del Cambio

#### 3.1. Diferencia Ontológica y de Ciclo de Vida
* **Un Incidente Agroclimático es un estado físico continuo y auditable:**
  * Se origina a partir de lecturas físicas de sondas de suelo o previsiones meteorológicas.
  * Posee un cronómetro agronómico: mide **minutos de estrés fisiológico continuo**.
  * Posee un protocolo de mitigación activo (checklist de riego matutino, suspensión de aplicaciones foliares).
  * **Se normaliza de manera autónoma:** Cuando la sonda o el pronóstico registra valores seguros, el incidente transiciona a `NORMALIZED` sin intervención manual, registrando la duración total del estrés.
* **La sobrecarga frutal o el balance de cosecha son estructurales:**
  * La sobrecarga (`Thinning`) se origina a partir de un muestreo manual en campo y se resuelve mediante cuadrillas de operarios podando ramas. No se normaliza con un riego ni tiene ventana horaria ("de 11 a.m. a 4 p.m."). Forzarla a compartir la estructura de telemetría violaría la cohesión de dominio.

#### 3.2. Prohibición de Shared Kernel y Respeto al Context Map
* El mapa de contextos oficial ([context-map.md](file:///home/santi2007939/workspace/apps/1acc0238/viora-platform/docs/context-map.md)) prohíbe explícitamente el uso de *Shared Kernel*:
  > *"Forbidden patterns: no new Shared Kernel without explicit approval. No Shared Kernel applied in recommended map."*
* Un agregado `Alert` genérico en `shared` crearía un modelo anémico con atributos heterogéneos y opcionales (`cropLoad`, `weeklyTemperatures`, `fruitCount`), acoplando transversalmente módulos independientes.
* Mantenerlo dentro de `Telemetry` preserva los **9 Bounded Contexts** oficiales del proyecto sin desviaciones metodológicas.

#### 3.3. Fidelidad con los Mockups de Figma (T14, T15, P10)
* Los mockups de Figma aprobados para la US18:
  * [T14](file:///home/santi2007939/workspace/apps/1acc0238/viora-platform/mobile-consideraciones/us18_alertas_mockups.md#L23) expone filtros de incidentes críticos, atención y normalizados con indicadores en °C y %.
  * [T15](file:///home/santi2007939/workspace/apps/1acc0238/viora-platform/mobile-consideraciones/us18_alertas_mockups.md#L62) expone una evolución gráfica de temperatura de 7 días, un umbral de daño térmico (32 °C) y un checklist de mitigación de riego.
  * Todo su vocabulario y comportamiento pertenecen orgánicamente a `Telemetry`.

---

### 4. Beneficios para Mobile y Backend

1. **Experiencia de Usuario Directa (Sin Pantallas Forzadas):**
   * El centro de alertas T14/T15 solo muestra incidentes que tienen sentido físico y accionable inmediato.
   * Las alertas de otros contextos redirigen con *Deep Link* directamente a la pantalla especializada donde se soluciona el problema (por ejemplo, P60 para aclareo).
2. **Cero Retrabajo en el Dominio al Implementar Push:**
   * El Aggregate Root `AgroclimaticIncident` no contiene dependencias de Firebase (FCM) ni formatos de notificación móvil.
   * Solo publica `AgroclimaticIncidentRaisedEvent`. Cuando se implemente el módulo Push en el Sprint 3, bastará con conectar un listener de eventos que lo despache, dejando el núcleo de dominio intacto.
3. **Mantenibilidad y Testeabilidad:**
   * Las pruebas unitarias de umbrales agroclimáticos se ejecutan de forma aislada, determinista y rápida sin requerir dependencias de otros contextos.

---

### 5. Consecuencias y Próximos Pasos

* **Próximo hito:** Ejecución del plan de acción táctico para implementar `AgroclimaticIncident` en `telemetry` siguiendo el pipeline estricto *Inside-Out* (Modelo A Dual):
  1. **Dominio:** Value Objects inmutables (`record`), Aggregate Root `AgroclimaticIncident` (con `snapshot()` y `reconstitute()`), `AgroclimaticThresholdEvaluator`, eventos y puerto `AgroclimaticIncidentRepository`.
  2. **Aplicación:** `AgroclimaticIncidentCommandService` y `AgroclimaticIncidentQueryService` utilizando `Result<T, ApplicationError>`.
  3. **Infraestructura:** Entidades JPA, repositorios Spring Data, adaptadores y assemblers de persistencia unificados.
  4. **Interfaces REST:** Controlador `/api/v1/agroclimatic-incidents` con verbos y nombres de recursos REST puros (sin RPC).

### 6. Invierno cálido (`WARM_WINTER`, US23), 2026-10-14

* **Regla, local y térmica:** un día es cálido si su máxima pasa de 24 °C; la alerta se abre cuando hay **más de 3 días cálidos seguidos dentro de la temporada de frío** (1 de junio al 31 de agosto). No se usa un índice de El Niño: Viora no tiene una fuente de ENOS por lote y la regla del escenario 1 de US23 ya es térmica. Es la misma regla de `ChillSeasonEvaluator`, que ya publicaba `thermalAnomaly` en la métrica de frío.
* **Quién evalúa:** Fenología, dueña del modelo de frío. `WinterWarmthMonitor` evalúa todos los lotes activos y publica `WinterWarmthEvaluatedEvent`. Corre un minuto después de arrancar (el host gratuito duerme y despierta con la primera petición) y luego cada 6 horas, solo en temporada y hasta 7 días después, para cerrar alertas abiertas. Se apaga con `viora.phenology.warm-winter-monitor.enabled=false` (así corren los tests, sin llamar al servicio de clima).
* **Quién alerta:** Telemetría escucha el evento (`WinterWarmthEvaluatedEventHandler`). Con racha activa abre un único `WARM_WINTER` `CRITICAL` por lote (activo o pospuesto, nunca dos), con métrica `DAILY_MAX_TEMPERATURE`, el valor del último día evaluado y umbral 24 °C. Cuando la racha termina, o termina la temporada, lo normaliza.
* **Qué hacer:** tres pasos que solo remiten a lo que Viora ya mide, sin consejo agronómico inventado: seguir el frío, esperar la floración antes de decidir el aclareo y registrar la plena floración.
* **Gráfica de la alerta (T15):** las máximas diarias de los 7 días anteriores al disparo, leídas de Fenología (`PhenologyContextFacade.findDailyMaxTemperatures`), es decir, los mismos días que vio la regla. Si no hay clima se usa la serie del nodo, como en los otros tipos.
* **Fuera de alcance:** la carga esperada reajustada que mostraba el diseño original (9,8 → 7,4 t/ha) no tiene fórmula que relacione el calor invernal con la floración, así que no se calcula; el plan de aclareo se ajusta con el muestreo de cuajado real. Las notificaciones push siguen fuera (sección 2).
* **Compatibilidad:** la app Android 1.1.x mapea los tipos desconocidos a `UNKNOWN`, así que recibir `WARM_WINTER` no la rompe.
