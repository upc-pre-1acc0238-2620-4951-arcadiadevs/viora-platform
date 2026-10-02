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
| `notes` | Opcional, hasta 1000 caracteres tras recortar; vacío o en blanco equivale a ausente; `certificationNotes` se acepta como alias de entrada |

**Conciliación ZIP frente a TS40.** El ZIP solo pide `auditorSignature` y `notes`; TS40 pide `campaignYear`, `certifiedBy`, `cipNumber` y `certificationNotes`. Se conservan el campo de firma del ZIP y la identidad estructurada de TS40, y se añade el selector de campaña, necesario porque un reporte por parcela contiene varias campañas. No se inventó ninguna expresión regular institucional: solo se validan presencia y longitud. Cada regla vive en un solo lugar: el comando `CertifyAgronomicDossierCommand` solo verifica nulos, y los objetos de valor `AuditorSignature`, `CertifierIdentity` y `CertificationNotes` (recorte, vacío y longitud) se construyen en el servicio de aplicación **antes** de invocar al dominio; la validación Bean Validation del recurso REST se mantiene. Los textos no se verifican contra ningún registro profesional.

### 2.3. Respuesta 201 (`DossierCertificationResource`)

`certificationId`, `reportId`, `plotId`, `campaignYear`, `verificationHash`, `auditorSignature`, `certifiedBy`, `cipNumber`, `certifiedAt`. El PDF no viaja en la respuesta.

### 2.4. Errores

| Situación | HTTP | Código (`code`) |
|---|---|---|
| UUID de parcela, campaña, firma, nombre, CIP o notas inválidos | 400 | `VALIDATION_ERROR` |
| Parcela inexistente o inactiva (ACL de Orchard) | 404 | `PLOT_NOT_FOUND` |
| La campaña no tiene liquidación (incluye parcela sin reporte) | 422 | `BUSINESS_RULE_VIOLATION` |
| Campaña ya certificada o carrera de certificaciones | 409 | `DOSSIERCERTIFICATION_CONFLICT` / integridad de datos |
| La curva congelada de esa campaña no tiene índice de alternancia gestionada (`managedAlternationIndex == null`: menos de 3 campañas liquidadas consecutivas), con o sin línea base | 409 | `DOSSIERCERTIFICATION_CONFLICT` |
| Fallo al generar el PDF | 500 | `UNEXPECTED_ERROR`, sin persistir ni publicar |

**Mapeo de excepciones.** El servicio solo traduce excepciones de negocio tipadas: `BusinessRuleException` a 422, las dos excepciones de conflicto a 409 (`DossierAlreadyCertifiedException` e `InsufficientSettlementHistoryException`, ambas subclases de `ResourceConflictException`, Java puro) y `DossierRenderingException` a 500. Como toda la entrada del usuario se valida antes de llamar al dominio, un `IllegalStateException` o `IllegalArgumentException` posterior (por ejemplo, la indisponibilidad del algoritmo SHA-256 en `CryptographicHashService`) es un fallo de servidor y llega al manejador global como 500, nunca como 409 o 400.

**Cambio respecto de la auditoría 21.** La auditoría proponía 404 cuando no existe el reporte. Se separa la identidad de la parcela (404) de la precondición de campaña (422, solicitado por TS40): una parcela activa sin liquidación de esa campaña responde 422, exista o no el reporte. No se comprueba la titularidad (no hay 403): el contrato de la rama 21 no la define y el actor depende de IAM, diferido.

## 3. Regla de suficiencia

Se rechaza con 409 la campaña cuya curva **congelada** en su liquidación no tiene índice de alternancia gestionada (`managedAlternationIndex == null`: menos de 3 campañas liquidadas consecutivas, es decir, menos de dos pares consecutivos), cualquiera que sea la línea base.

- Por qué no se usa el estado de la curva: el calculador informa `INSUFFICIENT_BASELINE` antes que `INSUFFICIENT_SETTLEMENTS`. Con el estado como criterio, una parcela sin historial Phenology certificaría con una sola campaña, mientras que una parcela con historial necesitaría tres. El criterio se evalúa sobre el historial liquidado mismo.
- Consecuencia aceptada: en toda parcela las dos primeras campañas liquidadas no se pueden certificar (la curva de ADR-001 exige 3 campañas seguidas). Se certifican a partir de la tercera consecutiva.
- `EVALUATED`, `INSUFFICIENT_BASELINE` y `NO_BASELINE_ALTERNATION` se certifican cuando existe el índice gestionado, y el PDF declara explícitamente que la estabilización y el ARR no son determinables, y por qué (línea base ausente o sin alternancia). El ARR y la meta solo se imprimen con `EVALUATED`.
- Esta regla fue aprobada por el responsable el 2026-10-02 y corregida el mismo día.

## 4. Modelo inmutable por campaña

La auditoría proponía un único `dossierMetadata` en el reporte, con una excepción al certificar de nuevo. Eso sellaría la parcela entera con el primer año. Se adopta:

- `DossierCertification` es hijo de `AgronomicReport`, identificado por `reportId + campaignYear`, con restricción única `uq_certification_report_campaign` en `dossier_certifications`.
- El reporte nunca se sella completo; certificar la campaña N+1 añade otra certificación y deja intactos bytes, hash, firma y metadatos de la N. Las liquidaciones no se modifican (`SettlementStatus.AUDITED` queda pendiente).
- Todas las columnas son `updatable = false`; el repositorio solo añade certificaciones nuevas.
- El agregado conserva **solo metadatos** de la certificación (`DossierCertificationSnapshot`: identidad, hash, firma, certificador, notas e instante); nunca los bytes. Así ninguna carga del reporte (incluida cada liquidación de campaña) trae los PDF.
- `AgronomicReport.certifyCampaign` renderiza, calcula el hash y devuelve `CertifiedDossier` (en `domain.model.aggregates`, junto al snapshot que referencia): los metadatos nuevos más el `DossierDocument`.
- Los bytes viven en su propia tabla `dossier_documents` (clave primaria `certification_id`, UUID asignado por el dominio; columna `content`, todas `updatable = false`), detrás del puerto de dominio `CertifiedDossierDocumentRepository` (`save` y `findByCertificationId`, solo inserción: no hay actualización ni borrado; guardar dos veces el mismo documento falla). La entidad es `DossierDocumentPersistenceEntity`, con `DossierDocumentPersistenceRepository`, `DossierDocumentPersistenceAssembler` y el adaptador `CertifiedDossierDocumentRepositoryImpl`, que solo persiste. `dossier_certifications` ya no tiene `document_content`.
- La referencia de `dossier_documents.certification_id` a `dossier_certifications.id` es **lógica**, sin clave foránea física: la clave primaria asignada por el dominio evita mapear una asociación JPA y cargar la certificación al guardar, y el servicio de aplicación guarda reporte y documento en la misma transacción. La integridad la garantiza ese único escritor transaccional y la clave primaria; añadir la FK física (mismo contexto delimitado, permitida por las directrices) es un endurecimiento posible si se mapea la asociación.
- `CertifyAgronomicDossierCommandServiceImpl` guarda el reporte, luego el documento, en la misma transacción, y después publica los eventos; el resultado de `handle` sigue siendo el snapshot de metadatos, de modo que la respuesta no cambia.
- Se guardan los **bytes exactos** del PDF (columna binaria, hasta 10 MiB; `bytea` en PostgreSQL, sin `@Lob` para evitar OID). Regenerar el PDF no reproduce necesariamente los mismos bytes: la biblioteca genera un identificador de archivo, de modo que el artefacto certificado es el almacenado, nunca un nuevo render.
- Concurrencia: bloqueo `PESSIMISTIC_WRITE` del reporte más la restricción única. Un duplicado simultáneo responde 409.

## 5. Cambio de firma del puerto PDF

La auditoría definía `byte[] renderPdf(AgronomicReport report, AuditorSignature signature)`. Se implementa `byte[] renderPdf(AgronomicDossierContent content)`, donde el contenido es un registro de datos **congelados**: identidades, campaña, la liquidación de esa campaña (pesos, calibre, estado, balance de aclareo y curva), certificador, firma, notas y el instante. Así el PDF corresponde exactamente al corte certificado, el dominio no expone el agregado vivo al adaptador y el puerto sigue siendo Java puro (OpenPDF vive solo en `infrastructure/adapters/pdf`). El fallo de renderizado es `DossierRenderingException`, traducida a 500.

## 6. Huella y firma

- La huella es el **SHA-256 de los bytes finales almacenados**, en 64 caracteres hexadecimales en minúscula, calculada por `CryptographicHashService` (Java puro).
- Los instantes impresos en el PDF (`Certified at (UTC)` y `Settled at (UTC)`) usan un único formato fijo en UTC con seis decimales (`uuuu-MM-dd'T'HH:mm:ss.SSSSSS'Z'`), no `Instant.toString()`, que varía entre 0, 3, 6 y 9 decimales.
- El hash **no se imprime dentro del PDF**: un documento no puede contener su propio digest. El pie del PDF lo indica; el hash se entrega por separado en la respuesta y en el evento.
- Firma textual y CIP son declaraciones almacenadas con el expediente. **No son firma digital ni garantizan no repudio**; el hash solo prueba que unos bytes coinciden con la huella guardada.
- Texto del PDF: Helvetica con codificación WinAnsi (Windows-1252), que cubre acentos, `ñ`, `ü` y signos invertidos. Los caracteres fuera de ese repertorio se sustituyen por `?`; los alfabetos no latinos no son representables sin incrustar una fuente.
- El frío/clima no forma parte del expediente: no hay medición certificada de procedencia. El PDF lo declara.

## 7. Reloj y evento

- Un único bean `java.time.Clock` (UTC) en `shared/infrastructure/time`. Se lee un solo instante por operación (truncado a microsegundos, lo que conserva la base de datos) y se usa en el PDF (fechas de creación y modificación), los metadatos y el evento. `HarvestSettlementCommandServiceImpl` también recibe el `Clock` por constructor (antes usaba `Clock.systemUTC()`), de modo que ambos servicios de Settlement comparten el reloj inyectado.
- `AgronomicDossierGeneratedEvent` (`eventId`, `certificationId`, `reportId`, `plotId`, `campaignYear`, `verificationHash`, `auditorSignature`, `certifiedAt`), solo tipos primitivos o estándar. Se publica después de guardar, igual que en ADR-001, con `ApplicationEventPublisher` (sin entrega durable).

## 8. Pendientes explícitos (no cumplidos aquí)

- US30 **no** queda completa: falta el GET de descarga del PDF y la consulta de metadatos con negociación de contenido. El POST deja el contenido recuperable e inmutable para esa capacidad.
- `SettlementStatus.AUDITED` no se actualiza al certificar.
- Procedencia real de los datos de frío (hoy simulados en otro contexto), sin incluirlos como medición certificada.
- Titularidad/autorización por IAM (hoy no hay 403 en este endpoint).
- Una infraestructura de firma digital profesional (certificados, sellado de tiempo) no está implementada.
- Almacenamiento: los bytes viven en la base de datos; si crece el volumen conviene almacenamiento de objetos inmutable con la misma huella.
- **Decisión pendiente con el responsable de Settlement (Victor) sobre los dos 409.** (a) Hoy "ya certificada" e "historial de liquidaciones insuficiente" comparten el mismo `code` del ProblemDetail (`DOSSIERCERTIFICATION_CONFLICT`) y los clientes solo los distinguen por el `detail` localizado; hay que decidir si el historial insuficiente recibe un código propio para que los clientes puedan reaccionar programáticamente. (b) Hay que decidir si el historial insuficiente sigue siendo 409 (política del ZIP/auditoría 21) o pasa a 422, como la otra precondición de campaña no cumplida (TS40 usa 422 para "sin liquidación"), ya que es una precondición y no un conflicto de estado. No se decide aquí; el código lo señala con comentarios `Pending decision (ADR-002 section 8)` en el agregado, el servicio y el controlador. El estado `INSUFFICIENT_SETTLEMENTS` del adaptador PDF se conserva solo para que el `switch` siga siendo exhaustivo (es inalcanzable en expedientes certificados).
- Despliegue: el esquema se crea con `ddl-auto=update`; en una base que ya hubiera recibido la versión anterior de esta rama, `dossier_certifications.document_content` (no nulo) debe eliminarse a mano. La rama aún no se integró, por lo que no hay datos que migrar.
- Actualizar en el reporte: ruta del capítulo 24, TS40 (campos, 422, 409 por insuficiencia) y el modelo táctico de Settlement.
