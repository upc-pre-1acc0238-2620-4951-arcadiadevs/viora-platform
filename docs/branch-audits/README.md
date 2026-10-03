# Viora Backend: Branch Audit Specifications (Master Index)

Este catálogo centraliza las **Especificaciones de Auditoría por Rama** para el desarrollo del Backend API de Viora (`viora-platform`) bajo la regla inviolable de **1 Rama `feature/` = 1 Endpoint REST**, aplicando el **Modelo A (Modelo Dual Hexagonal)** y las directrices de [26-tactical-ddd.md](file:///home/santi2007939/workspace/apps/1acc0238/viora-platform/docs/26-tactical-ddd.md).

Cualquier agente desarrollador o auditor ([backend-ddd-auditor](file:///home/santi2007939/workspace/apps/1acc0238/viora-platform/.agents/skills/backend-ddd-auditor/SKILL.md)) debe contrastar el código de la rama activa contra su respectivo archivo de especificación antes de autorizar la fusión a `develop`.

---

## 1. Bounded Context: Olive Orchard and Plot Management (`orchard`)

| Orden | Rama `feature/` | Endpoint Único | Archivo de Especificación de Auditoría | Estado |
| :---: | :--- | :--- | :--- | :---: |
| **01** | `feature/orchard-create-plot` | `POST /api/v1/plots` | [01-feature-orchard-create-plot.md](file:///home/santi2007939/workspace/apps/1acc0238/viora-platform/docs/branch-audits/01-feature-orchard-create-plot.md) | **COMPLETADO** |
| **02** | `feature/orchard-list-plots` | `GET /api/v1/plots` | [02-feature-orchard-list-plots.md](file:///home/santi2007939/workspace/apps/1acc0238/viora-platform/docs/branch-audits/02-feature-orchard-list-plots.md) | **COMPLETADO** |
| **03** | `feature/orchard-get-plot-by-id` | `GET /api/v1/plots/{plotId}` | [03-feature-orchard-get-plot-by-id.md](file:///home/santi2007939/workspace/apps/1acc0238/viora-platform/docs/branch-audits/03-feature-orchard-get-plot-by-id.md) | **COMPLETADO** |
| **04** | `feature/orchard-update-plot` | `PUT /api/v1/plots/{plotId}` | [04-feature-orchard-update-plot.md](file:///home/santi2007939/workspace/apps/1acc0238/viora-platform/docs/branch-audits/04-feature-orchard-update-plot.md) | **COMPLETADO** |
| **05** | `feature/orchard-delete-plot` | `DELETE /api/v1/plots/{plotId}` | [05-feature-orchard-delete-plot.md](file:///home/santi2007939/workspace/apps/1acc0238/viora-platform/docs/branch-audits/05-feature-orchard-delete-plot.md) | Pendiente |

---

## 2. Bounded Context: Agroclimatic Telemetry & Sensor Monitoring (`telemetry`)

| Orden | Rama `feature/` | Endpoint Único | Archivo de Especificación de Auditoría | Estado |
| :---: | :--- | :--- | :--- | :---: |
| **06** | `feature/telemetry-register-iot-device` | `POST /api/v1/plots/{plotId}/iot-devices` | - | **COMPLETADO** |
| **07** | `feature/telemetry-list-plot-iot-devices` | `GET /api/v1/plots/{plotId}/iot-devices` | - | **COMPLETADO** |
| **08** | `feature/telemetry-calibrate-iot-device` | `PUT /api/v1/plots/{plotId}/iot-devices/{deviceId}` | - | **COMPLETADO** |
| **09** | `feature/telemetry-deactivate-iot-device` | `DELETE /api/v1/plots/{plotId}/iot-devices/{deviceId}` | - | **COMPLETADO** |
| **10** | `feature/telemetry-get-series` | `GET /api/v1/plots/{plotId}/telemetries` | [10-feature-telemetry-get-series.md](10-feature-telemetry-get-series.md) | **COMPLETADO** |
| **11** | `feature/telemetry-get-weather-forecast` | `GET /api/v1/plots/{plotId}/forecasts` | [11-feature-telemetry-get-weather-forecast.md](11-feature-telemetry-get-weather-forecast.md) | **COMPLETADO** |

---

## 3. Bounded Context: Phenology and Historical Bearing Analytics (`phenology`)

| Orden | Rama `feature/` | Endpoint Único | Archivo de Especificación de Auditoría | Estado |
| :---: | :--- | :--- | :--- | :---: |
| **12** | `feature/phenology-record-harvest-yield` | `POST /api/v1/plots/{plotId}/harvest-records` | [12-feature-phenology-record-harvest-yield.md](file:///home/santi2007939/workspace/apps/1acc0238/viora-platform/docs/branch-audits/12-feature-phenology-record-harvest-yield.md) | **COMPLETADO** |
| **13** | `feature/phenology-list-harvest-records` | `GET /api/v1/plots/{plotId}/harvest-records` | [13-feature-phenology-list-harvest-records.md](file:///home/santi2007939/workspace/apps/1acc0238/viora-platform/docs/branch-audits/13-feature-phenology-list-harvest-records.md) | Pendiente |
| **14** | `feature/phenology-rectify-harvest-yield` | `PUT /api/v1/plots/{plotId}/harvest-records/{recordId}` | [14-feature-phenology-rectify-harvest-yield.md](file:///home/santi2007939/workspace/apps/1acc0238/viora-platform/docs/branch-audits/14-feature-phenology-rectify-harvest-yield.md) | Pendiente |
| **15** | `feature/phenology-get-bearing-metrics` | `GET /api/v1/plots/{plotId}/metrics` | [15-feature-phenology-get-bearing-metrics.md](file:///home/santi2007939/workspace/apps/1acc0238/viora-platform/docs/branch-audits/15-feature-phenology-get-bearing-metrics.md) | Pendiente |

---

## 4. Bounded Context: Crop Load Regulation and Thinning Advisory (`thinning`)

| Orden | Rama `feature/` | Endpoint Único | Archivo de Especificación de Auditoría | Estado |
| :---: | :--- | :--- | :--- | :---: |
| **16** | `feature/thinning-submit-sampling` | `POST /api/v1/plots/{plotId}/samplings` | [16-feature-thinning-submit-sampling.md](16-feature-thinning-submit-sampling.md) | **COMPLETADO** |
| **17** | `feature/thinning-get-sampling-summary` | `GET /api/v1/plots/{plotId}/samplings` | [17-feature-thinning-get-sampling-summary.md](17-feature-thinning-get-sampling-summary.md) | Pendiente |
| **18** | `feature/thinning-get-active-prescription` | `GET /api/v1/plots/{plotId}/thinning-prescriptions` | [18-feature-thinning-get-active-prescription.md](18-feature-thinning-get-active-prescription.md) | Pendiente |
| **19** | `feature/thinning-confirm-execution` | `POST /api/v1/thinning-prescriptions/{id}/execution-confirmations` | [19-feature-thinning-confirm-execution.md](19-feature-thinning-confirm-execution.md) | Pendiente |

---

## 5. Bounded Context: Harvest Settlement and Performance Reporting (`settlement`)

| Orden | Rama `feature/` | Endpoint Único | Propósito Táctico DDD |
| :---: | :--- | :--- | :--- |
| **20** | `feature/settlement-settle-campaign-harvest` | `POST /api/v1/plots/{plotId}/harvest-settlements` | Balance comercial definitivo de aceituna verde y negra. |
| **21** | `feature/settlement-certify-agronomic-dossier` | `POST /api/v1/plots/{plotId}/certifications` | Certificación oficial colegiada con hash criptográfico SHA-256. |

---

## 6. Los 7 Vectores Forenses de Auditoría Obligatorios
Todo Pull Request que implemente cualquiera de estas ramas debe ser contrastado contra:
1. **Pureza de Dominio:** 100% Java puro, cero anotaciones JPA o Spring en `domain`.
2. **Encapsulación Absoluta:** Cero `@Setter`, mutaciones por métodos de negocio, lecturas por `snapshot()`.
3. **Identidad en Dominio:** Identificadores tipados encapsulando `UUID`, nunca autoincrementales de base de datos.
4. **Value Objects Inmutables:** Java 21 `record` con invariantes en el constructor compacto.
5. **Desacoplamiento Inter-Contexto:** Referencias lógicas cruzadas por identificador (UUID), sin claves foráneas ORM físicas.
6. **Modelo Dual Hexagonal:** `[Name]PersistenceAssembler` unificado y `Jpa[Name]RepositoryAdapter`.
7. **Calidad de Código y ABET:** 100% inglés, Javadoc completo (`@param`, `@return`, `@throws`), y Conventional Commits.
