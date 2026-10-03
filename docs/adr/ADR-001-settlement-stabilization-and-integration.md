# ADR-001: Liquidación de cosecha, curva de estabilización e integración Thinning ↔ Settlement

- **Estado:** Aceptado (rama `feature/settlement-settle-campaign-harvest`, rama 20)
- **Fecha:** 2026-10-02
- **Requisitos:** US29, TS39 (y base para TS40); integra TS27/US28 (rama 19)

## 1. Contexto

El reporte nombra la curva de estabilización (`baselineYield`, varianza, ARR, meta del 30 %), pero no define la fórmula. Tampoco define el balance contra la prescripción ni el contrato entre Thinning y Settlement. Este ADR fija esas decisiones para que no se presenten como fórmulas del reporte.

## 2. Endpoint y contrato

`POST /api/v1/plots/{plotId}/harvest-settlements`

```json
{ "campaignYear": 2026, "greenOlivesKg": 8200.0, "blackOlivesKg": 6050.0,
  "commercialFruitsPerKg": 105.0, "notes": "Campaign weights verified." }
```

- Nombres canónicos: `greenOlivesKg`, `blackOlivesKg` y `notes` (los del modelo táctico). No se aceptan alias (`greenYieldKg`, `settlementNotes`); TS39 debe actualizarse.
- `commercialFruitsPerKg` es opcional y nuevo. Es el calibre de venta (escala COI) y alimenta la proyección de calibre de Thinning.
- Respuesta 201 `HarvestSettlementResource`: identidad (`id`, `reportId`, `plotId`), pesos, `totalYieldKg`, `status`, `settledAt`, `thinningBalance` y `stabilization`.

| Situación | HTTP |
|---|---|
| Liquidación válida | 201 |
| UUID, campaña (2000–2100), peso negativo/no finito, total ≤ 0, calibre ≤ 0, notas > 1000 | 400 |
| Parcela inexistente o inactiva | 404 |
| El actor no es el productor titular | 403 |
| Campaña ya liquidada o carrera de duplicados (`uq_settlement_report_campaign`, `uq_agronomic_report_plot`) | 409 |

**Titularidad:** mientras IAM esté diferido, el actor es `viora.security.mock.default-producer-id`. Se compara con el productor que publica Orchard (`OrchardContextFacade.findPlotProducerId`). JWT reemplazará la fuente del actor.

## 3. Curva de estabilización

- **Índice de alternancia (Hoblyn, 1936)**, solo sobre pares de campañas **consecutivas**: `I = mean(|Y(t+1) − Y(t)| / (Y(t+1) + Y(t)))`. Un año faltante rompe el par, en lugar de comparar campañas no contiguas.
- **Línea base:** historial de Phenology (US20) de las campañas **anteriores a la primera liquidación** (`PhenologyContextFacade.findHistoricalYields`). Las campañas del periodo liquidado se excluyen de la base, así una cosecha nunca se cuenta dos veces.
- **Periodo gestionado:** campañas liquidadas en Settlement.
- **Suficiencia:** cada índice necesita **2 pares consecutivos**, es decir, al menos 3 campañas seguidas. Esto es coherente con la regla de BBI de ≥ 3 campañas.
- **ARR** = `(I_base − I_gestión) / I_base`, como fracción. Es negativa si la alternancia creció. **Meta:** `ARR ≥ 0.30`.
- **Varianza:** muestral (n − 1) de los kg liquidados, en kg². También se calcula el **coeficiente de variación** (desviación estándar / media), que no depende del tamaño del lote. Ambos están disponibles desde 2 liquidaciones.
- Se usan kg totales, no kg/ha: el índice de Hoblyn es un cociente y no depende de la escala.

| Estado | Significado |
|---|---|
| `EVALUATED` | Hay ARR y se indica si se cumple la meta |
| `INSUFFICIENT_BASELINE` | Menos de 2 pares consecutivos de historial previo |
| `INSUFFICIENT_SETTLEMENTS` | Menos de 2 pares consecutivos liquidados |
| `NO_BASELINE_ALTERNATION` | La base no alterna (I = 0): no hay amplitud que reducir |

Una sola campaña **nunca** se presenta como reducción demostrada. Solo `EVALUATED` lleva ARR.

Ejemplo verificado por test: base 10000/2000/9000/3000 kg y liquidaciones 7000/5000/6500 kg dan ARR = 0.7528 y la meta se cumple.

## 4. Balance contra la prescripción

Solo se comparan magnitudes comparables: **% de frutos prescrito frente a % retirado** (`deviationPercentagePoints = actual − prescrito`). Los kg cosechados no se comparan con los kg raleados.

- `EXECUTED_ON_TIME`, `EXECUTED_LATE` y `NOT_RECORDED`. `NOT_RECORDED` **no es incumplimiento**: un lote equilibrado puede no necesitar aclareo.
- El balance y la curva se **congelan en cada liquidación**. Liquidar la campaña N+1 no modifica la N.

## 5. Eventos e integración

| Evento | Productor | Consumidor | Fase | Efecto |
|---|---|---|---|---|
| `ThinningExecutionConfirmedEvent` (ahora con `prescribedRemovalPercentage`) | Thinning | Settlement `ThinningExecutionConfirmedEventHandler` | `BEFORE_COMMIT` | Proyección `settlement_thinning_executions`, única por evento, confirmación y (lote, campaña); los eventos repetidos se ignoran |
| `CampaignHarvestSettledEvent` (`eventId`, `reportId`, `settlementId`, `plotId`, `campaignYear`, pesos, `totalYieldKg`, `commercialFruitsPerKg`, `occurredOn`) | Settlement | Thinning `CampaignHarvestSettledEventHandler` | `BEFORE_COMMIT` | Si hay calibre y el aclareo de esa campaña fue a tiempo (y dejó carga residual), guarda una `CaliberCalibrationObservation` |

`BEFORE_COMMIT` hace atómica la proyección: el registro de origen y su proyección se guardan juntos o no se guarda ninguno. `ApplicationEventPublisher` no es entrega durable. Si los contextos se separan en procesos distintos, se necesitará outbox.

Phenology es un consumidor previsto de `CampaignHarvestSettledEvent`, pero **no está implementado** en esta rama.

## 6. Persistencia

- `agronomic_reports`: único por `plot_id`, con `@Version`. La liquidación bloquea el reporte con `PESSIMISTIC_WRITE`.
- `harvest_settlements`: único por `(report_id, campaign_year)`. Todas las columnas son `updatable = false` salvo `status`, reservado para `AUDITED` en TS40.
- `settlement_thinning_executions`: proyección propia de Settlement.
- Sin `schema = "settlement"`: se sigue la política actual (`ddl-auto`, esquema por defecto).

## 7. Entrega a la rama 21 (certificación, Jahat)

- La selección por campaña es `AgronomicReport.settlementOf(CampaignYear)`, que devuelve el snapshot congelado.
- La certificación debe referenciar `reportId + campaignYear` y no sellar el reporte completo, para que las campañas futuras sigan siendo posibles.
- `SettlementStatus.AUDITED` ya existe. La entidad de certificación, el PDF y el hash SHA-256 son de la rama 21 (decisiones en [ADR-002](ADR-002-settlement-dossier-certification.md)).

## 8. Pendientes explícitos (no cumplidos aquí)

- Consumidor de Phenology para `CampaignHarvestSettledEvent`.
- APIs de consulta de liquidaciones y reportes (fuera del alcance de "1 rama = 1 endpoint").
- El BBI de Phenology todavía empareja campañas no consecutivas; conviene alinearlo con la regla de pares consecutivos de este ADR.
- Actualizar en el reporte: TS39 (nombres de campos, balance, curva), el modelo táctico de Settlement y el contexto estratégico (relación Settlement → Thinning).
