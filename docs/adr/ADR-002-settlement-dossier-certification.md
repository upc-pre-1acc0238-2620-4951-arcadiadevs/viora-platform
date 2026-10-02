# ADR-002: Certificación del expediente agronómico por campaña (PDF, SHA-256 y firma colegiada)

- **Estado:** Aceptado (rama `feature/settlement-certify-agronomic-dossier`, rama 21)
- **Fecha:** 2026-10-02
- **Requisitos:** TS40 (base técnica de US30); depende de ADR-001 (rama 20)

## 1. Contexto

La rama 21 certifica una campaña ya liquidada, genera su expediente PDF, calcula una huella SHA-256 verificable y conserva el documento exacto. Las fuentes (auditoría 21, capítulo 24, TS40 y el backlog del ZIP) divergen en ruta, campos y en el modelo de sellado. Este ADR fija el contrato implementado y deja explícito lo que no se cubre.

## 2. Contrato

### 2.1. Ruta

`POST /api/v1/plots/{plotId}/certifications`

El capítulo 24 usa `/api/v1/plots/{plotId}/agronomic-reports/certifications`. Se adopta la ruta de la auditoría 21 (una sola ruta, sin alias). El capítulo 24 y el OpenAPI de referencia deben actualizarse. El README de auditorías ya refleja la ruta final.

### 2.2. Solicitud

```json
{ "campaignYear": 2026,
  "auditorSignature": "CIP-49120-ING-AGRONOMO-SANCHEZ",
  "certifiedBy": "Agronomist Sanchez",
  "cipNumber": "49120",
  "notes": "Verified campaign records." }
```

| Campo | Regla |
|---|---|
| `campaignYear` | Obligatorio, 2000 a 2100 |
| `auditorSignature` | Obligatorio, no vacío, hasta 120 caracteres |
| `certifiedBy` | Obligatorio, no vacío, hasta 120 caracteres |
| `cipNumber` | Obligatorio, no vacío, hasta 20 caracteres |
| `notes` | Opcional, hasta 1000 caracteres; `certificationNotes` se acepta como alias de entrada |

**Conciliación ZIP frente a TS40.** El ZIP solo pide `auditorSignature` y `notes`; TS40 pide `campaignYear`, `certifiedBy`, `cipNumber` y `certificationNotes`. Se conservan el campo de firma del ZIP y la identidad estructurada de TS40, y se añade el selector de campaña, necesario porque un reporte por parcela contiene varias campañas. No se inventó ninguna expresión regular institucional: solo se validan presencia y longitud. Los textos no se verifican contra ningún registro profesional.

### 2.3. Respuesta 201 (`DossierCertificationResource`)

`certificationId`, `reportId`, `plotId`, `campaignYear`, `verificationHash`, `auditorSignature`, `certifiedBy`, `cipNumber`, `certifiedAt`. El PDF no viaja en la respuesta.

### 2.4. Errores

| Situación | HTTP | Código (`code`) |
|---|---|---|
| UUID de parcela, campaña, firma, nombre, CIP o notas inválidos | 400 | `VALIDATION_ERROR` |
| Parcela inexistente o inactiva (ACL de Orchard) | 404 | `PLOT_NOT_FOUND` |
| La campaña no tiene liquidación (incluye parcela sin reporte) | 422 | `BUSINESS_RULE_VIOLATION` |
| Campaña ya certificada o carrera de certificaciones | 409 | `DOSSIERCERTIFICATION_CONFLICT` / integridad de datos |
| La curva congelada de esa campaña es `INSUFFICIENT_SETTLEMENTS` | 409 | `DOSSIERCERTIFICATION_CONFLICT` |
| Fallo al generar el PDF | 500 | `UNEXPECTED_ERROR`, sin persistir ni publicar |

**Cambio respecto de la auditoría 21.** La auditoría proponía 404 cuando no existe el reporte. Se separa la identidad de la parcela (404) de la precondición de campaña (422, solicitado por TS40): una parcela activa sin liquidación de esa campaña responde 422, exista o no el reporte. No se comprueba la titularidad (no hay 403): el contrato de la rama 21 no la define y el actor depende de IAM, diferido.

## 3. Regla de suficiencia

Solo se rechaza con 409 la campaña cuya curva **congelada** en su liquidación es `INSUFFICIENT_SETTLEMENTS`. Se certifican `EVALUATED`, `INSUFFICIENT_BASELINE` y `NO_BASELINE_ALTERNATION`.

- Consecuencia aceptada: en una parcela con historial previo suficiente, las dos primeras campañas liquidadas no se pueden certificar (la curva de ADR-001 exige 3 campañas seguidas). Se certifican a partir de la tercera.
- Una parcela sin línea base suficiente sí se certifica, y el PDF declara explícitamente que la estabilización y el ARR no son determinables, y por qué (línea base ausente, sin alternancia o liquidaciones insuficientes). El ARR y la meta solo se imprimen con `EVALUATED`.
- Esta regla fue aprobada por el responsable el 2026-10-02.

## 4. Modelo inmutable por campaña

La auditoría proponía un único `dossierMetadata` en el reporte, con una excepción al certificar de nuevo. Eso sellaría la parcela entera con el primer año. Se adopta:

- `DossierCertification` es hijo de `AgronomicReport`, identificado por `reportId + campaignYear`, con restricción única `uq_certification_report_campaign` en `dossier_certifications`.
- El reporte nunca se sella completo; certificar la campaña N+1 añade otra certificación y deja intactos bytes, hash, firma y metadatos de la N. Las liquidaciones no se modifican (`SettlementStatus.AUDITED` queda pendiente).
- Todas las columnas son `updatable = false`; el repositorio solo añade certificaciones nuevas.
- Se guardan los **bytes exactos** del PDF (columna binaria, hasta 10 MiB; `bytea` en PostgreSQL, sin `@Lob` para evitar OID). Regenerar el PDF no reproduce necesariamente los mismos bytes: la biblioteca genera un identificador de archivo, de modo que el artefacto certificado es el almacenado, nunca un nuevo render.
- Concurrencia: bloqueo `PESSIMISTIC_WRITE` del reporte más la restricción única. Un duplicado simultáneo responde 409.

## 5. Cambio de firma del puerto PDF

La auditoría definía `byte[] renderPdf(AgronomicReport report, AuditorSignature signature)`. Se implementa `byte[] renderPdf(AgronomicDossierContent content)`, donde el contenido es un registro de datos **congelados**: identidades, campaña, la liquidación de esa campaña (pesos, calibre, estado, balance de aclareo y curva), certificador, firma, notas y el instante. Así el PDF corresponde exactamente al corte certificado, el dominio no expone el agregado vivo al adaptador y el puerto sigue siendo Java puro (OpenPDF vive solo en `infrastructure/adapters/pdf`). El fallo de renderizado es `DossierRenderingException`, traducida a 500.

## 6. Huella y firma

- La huella es el **SHA-256 de los bytes finales almacenados**, en 64 caracteres hexadecimales en minúscula, calculada por `CryptographicHashService` (Java puro).
- El hash **no se imprime dentro del PDF**: un documento no puede contener su propio digest. El pie del PDF lo indica; el hash se entrega por separado en la respuesta y en el evento.
- Firma textual y CIP son declaraciones almacenadas con el expediente. **No son firma digital ni garantizan no repudio**; el hash solo prueba que unos bytes coinciden con la huella guardada.
- Texto del PDF: Helvetica con codificación WinAnsi (Windows-1252), que cubre acentos, `ñ`, `ü` y signos invertidos. Los caracteres fuera de ese repertorio se sustituyen por `?`; los alfabetos no latinos no son representables sin incrustar una fuente.
- El frío/clima no forma parte del expediente: no hay medición certificada de procedencia. El PDF lo declara.

## 7. Reloj y evento

- Un único bean `java.time.Clock` (UTC) en `shared/infrastructure/time`. Se lee un solo instante por operación (truncado a microsegundos, lo que conserva la base de datos) y se usa en el PDF (fechas de creación y modificación), los metadatos y el evento. Los servicios existentes no se refactorizan.
- `AgronomicDossierGeneratedEvent` (`eventId`, `certificationId`, `reportId`, `plotId`, `campaignYear`, `verificationHash`, `auditorSignature`, `certifiedAt`), solo tipos primitivos o estándar. Se publica después de guardar, igual que en ADR-001, con `ApplicationEventPublisher` (sin entrega durable).

## 8. Pendientes explícitos (no cumplidos aquí)

- US30 **no** queda completa: falta el GET de descarga del PDF y la consulta de metadatos con negociación de contenido. El POST deja el contenido recuperable e inmutable para esa capacidad.
- `SettlementStatus.AUDITED` no se actualiza al certificar.
- Procedencia real de los datos de frío (hoy simulados en otro contexto), sin incluirlos como medición certificada.
- Titularidad/autorización por IAM (hoy no hay 403 en este endpoint).
- Una infraestructura de firma digital profesional (certificados, sellado de tiempo) no está implementada.
- Almacenamiento: los bytes viven en la base de datos; si crece el volumen conviene almacenamiento de objetos inmutable con la misma huella.
- Actualizar en el reporte: ruta del capítulo 24, TS40 (campos, 422, 409 por insuficiencia) y el modelo táctico de Settlement.
