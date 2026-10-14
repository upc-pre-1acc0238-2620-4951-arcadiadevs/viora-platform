# ADR-005: Expediente agronómico sellado por el productor (US30)

- **Estado:** Aceptado
- **Fecha:** 2026-10-14
- **Requisitos:** US30 escenarios 1 y 2 (descarga del PDF y verificación por SHA-256); pantallas P76, P77 y P78 de la app Kotlin
- **Relacionada:** ADR-002 (certificación con firma colegiada), ADR-001 (liquidación y curva de estabilización)

## 1. Contexto

ADR-002 dejó la certificación de una campaña: PDF, SHA-256 y firma con CIP. Para la app del productor faltaban tres cosas:

1. **Quién lo genera.** La certificación exige firma y número de colegiatura de un agrónomo. En el diseño (P76) el productor abre su expediente y lo descarga; nunca firma.
2. **Desde cuándo.** La certificación solo acepta la tercera campaña liquidada seguida. Un productor nuevo no tendría expediente durante dos años.
3. **Leerlo.** No había forma de descargar el PDF, consultar sus datos ni verificar el código.

## 2. Decisión

- **Agregado propio, `SealedDossier`, separado de la certificación.** El productor sella el expediente de una campaña liquidada; la certificación del agrónomo (ADR-002) no cambia y sigue siendo un trámite aparte, pensado para la app del Gestor (Flutter). Ambas apps usan este mismo backend, así que cuando el Gestor certifique, el expediente del productor lo muestra en `certification`.
- **Desde la primera campaña liquidada.** La regla de tres campañas existe para certificar una estabilización. El expediente sellado no la afirma: el PDF dice en palabras qué no se puede medir todavía.
- **Uno por lote y campaña, inmutable.** Lo que lee (la liquidación, el balance de aclareo, la prescripción) ya no cambia, así que sellar otra vez devuelve el mismo expediente (200) con los mismos bytes y el mismo código. Restricción única `(plot_id, campaign_year)` y otra sobre el código.
- **Hechos congelados.** Los valores que imprime el PDF se guardan con el expediente (`DossierFacts`), así el JSON (P76) y el PDF dicen siempre lo mismo.
- **Contenido del PDF, en español:**
  1. Identificación: lote, variedad, superficie, productor, campaña e instante del sello.
  2. Historial productivo: las cosechas registradas antes de la campaña (Fenología) más el peso liquidado de esta, en kg y t/ha.
  3. Carga frutal y aclareo: carga antes de aclarear y objetivo en frutos por brote, exceso sobre el objetivo, prescripción, ventana, aclareo registrado y si fue a tiempo (Thinning).
  4. Cosecha asentada: verde, negra, total, t/ha, calibre de venta, comprobante, boleta de molino, fecha de pesaje.
  5. Clima y telemetría: nodos vinculados y días con lecturas desde el 1 de enero hasta el asiento (Telemetry). Solo cuenta lo guardado; no dispara la simulación del nodo virtual.
  6. Estabilización de la vecería: los valores de la curva congelada en la liquidación, con la frase que explica cuando aún no es medible.
  Lo que nunca se registró se escribe "Sin registro"; no se estima nada.
- **Identidad del titular.** Hasta que Perfiles (IAM) exista, el PDF imprime el ID del productor. El nombre se agregará cuando el contexto de perfiles lo publique.
- **Datos de otros contextos por ACL:** `ThinningContextFacade.findCampaignThinningPlan`, `TelemetryContextFacade.findTelemetryCoverage` (nuevo), `OrchardContextFacade` (nombre, variedad, área) y `PhenologyContextFacade.findHistoricalYields`.
- **Almacenamiento:** metadatos y hechos en `sealed_dossiers`; los bytes exactos en `sealed_dossier_documents`, para que listar nunca cargue los PDF. Todo es solo inserción.

## 3. Contrato

| Método y ruta | Uso | Respuestas |
|---|---|---|
| `POST /api/v1/plots/{plotId}/agronomic-dossiers` `{campaignYear}` | Sellar (P73 → P76) | 201 sellado, 200 ya existía, 400, 403 lote ajeno, 404 lote, 422 campaña sin liquidar, 500 PDF |
| `GET /api/v1/plots/{plotId}/agronomic-dossiers` | Expedientes del lote, campaña más reciente primero | 200, 403, 404 |
| `GET /api/v1/plots/{plotId}/agronomic-dossiers/{campaignYear}` con `Accept: application/json` | Datos de P76, código SHA-256 y certificación del agrónomo si existe | 200, 403, 404 |
| Misma ruta con `Accept: application/pdf` | Descarga de los bytes exactos (P77), con `Content-Disposition: attachment` y `X-Verification-Hash` | 200; en error solo el código de estado, porque un cliente de PDF no lee JSON |
| `GET /api/v1/agronomic-dossiers/verifications/{verificationHash}` | Verificar un PDF por su código (P78). Pública: no exige que el lote siga activo | 200 coincide, 400 código inválido, 404 ningún expediente con ese código |

El código no va dentro del PDF (un archivo no puede contener su propio SHA-256); el pie lo explica.

## 4. Consecuencias

- US30 queda cubierta para el productor: generar, descargar y verificar.
- Si un día se quiere un expediente "vivo" que se regenere, se necesita versionarlo; hoy no hace falta porque sus fuentes son inmutables.
- Pendiente fuera de este ADR: nombre del titular y cooperativa (Perfiles), firma digital profesional (ADR-002 sección 8).
