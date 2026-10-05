# ADR-004: Entradas y procedencia de la prescripción de aclareo

* **Estado:** Aceptada
* **Contexto:** Crop Load Regulation and Thinning Advisory (Thinning) y la app Android (US26, US27)
* **Fecha:** 2026-10-04
* **Relacionada:** `docs/mobile-backend-gaps.md`, propuesta de Astra del 2026-10-04 sobre ventana y carga objetivo

---

## 1. Problema

La prescripción de aclareo no se emitía nunca: nada llamaba a `FruitThinningPrescription.determineSustainableCropLoad` y `GET /plots/{id}/thinning-prescriptions` (que filtraba por `PRESCRIBED`) respondía 404 para cualquier lote. Al conectarla aparecieron cuatro números que el sistema tenía fijos o que no existían en ninguna fuente:

1. **Longitud del brote:** `SamplingCoverageEvaluator` dividía frutos por brote entre `0.20` para obtener "frutos por metro". Esa longitud no se mide y no tiene respaldo.
2. **Carga objetivo:** `CropLoadBalancingCalculatorService` recibía un umbral que nadie configuraba. El reporte dice que Thinning "calcula la tasa sostenible" pero no da método ni valor.
3. **Multiplicador `1 + 0.3 × BBI`:** presente en el código, ausente del reporte.
4. **Ventana de intervención:** el reporte habla de ~680 GDD para el endurecimiento del carozo, pero Fenología no implementa el modelo (falta fecha de plena floración, histórico de temperatura por lote y una temperatura base validada).

## 2. Decisión

Regla general: **la plataforma no sustituye un dato desconocido por una constante.** Si falta una entrada, la respuesta dice cuál falta.

* **Unidad:** la carga es `FRUITS_PER_SHOOT`: frutos contados entre brotes contados, a precisión completa (solo la capa REST redondea). Los campos pasan de `…FruitsPerMeter` a `…FruitsPerShoot` y las respuestas traen `loadUnit`. Los porcentajes y la regresión del calibre no cambian porque son invariantes a la escala. Las columnas viejas de la prescripción y de la confirmación (`target_fruits_m`, `…_fruits_per_meter`) quedan sin leer. La excepción es `caliber_calibration_observations.residual_fruits_per_meter`: es `NOT NULL`, así que se conserva su nombre (ahora guarda frutos por brote) para no dejar una columna huérfana que haría fallar cada inserción en una base ya creada. Una prescripción nunca llegó a emitirse en producción, así que no se espera ningún dato histórico en ellas.
* **Porcentaje:** `p = 100 × max(0, 1 − T / L)`, con `T` y `L` en la misma unidad. Sin multiplicador por BBI; el BBI sigue disponible como contexto en Fenología.
* **Perfil técnico por variedad** (`viora.thinning.profiles.<variedad>.*`): carga objetivo, apertura y cierre de la ventana en días después de la plena floración, estado (`AGRONOMIST_APPROVED`, `PROVISIONAL`, `SYNTHETIC_DEMO`), versión, fuente y quién lo aprobó. **No hay valores por defecto.** Un perfil incompleto o incoherente se ignora con una advertencia en el log. Cada prescripción guarda la versión y el estado del perfil que la produjo.
* **Plena floración observada:** `PUT /plots/{id}/thinning-prescriptions/full-bloom` registra la fecha por lote y campaña (no futura, dentro del año de la campaña). La ventana se cuenta desde ella: `windowOpensOn = floración + apertura`, `windowClosesOn = floración + cierre`, con `windowBasis = FULL_BLOOM_PLUS_PROFILE_OFFSETS`. Corregir la fecha recalcula una prescripción emitida que aún no fue confirmada.
* **Qué falta:** el `GET` devuelve la prescripción en el estado en que esté y `blockers` con lo que impide emitirla: `SAMPLING_NOT_REPRESENTATIVE`, `TARGET_NOT_CONFIGURED`, `FULL_BLOOM_MISSING`. `status=ACTIVE` sigue devolviendo solo la emitida.
* **Cuándo se emite:** al completarse un muestreo representativo, al registrar la floración y al leer (por si el perfil se aprobó después). Un fallo al emitir nunca rompe el muestreo que lo disparó.
* **Demostraciones:** el perfil de Spring `demo` (`application-demo.properties`) trae valores sintéticos marcados `SYNTHETIC_DEMO`. Nunca son valores por defecto de producción.

## 3. Lo que queda pendiente y por qué

* **Valores reales del perfil.** Los debe aportar un técnico o un ensayo cuya aplicabilidad a Tacna se haya evaluado. La literatura revisada (Plants 2022, DOI 10.3390/plants11243541: unos 49 días desde plena floración hasta el final del período inicial del carozo en Cornicabra, Arbequina y Manzanilla, en España) no es una validación local, no cubre el endurecimiento completo y su método y sus tablas discrepan en la temperatura base (15 °C frente a 5 °C).
* **Modelo de grados-día (GDD).** La fórmula diaria es `max(0, (Tmáx + Tmín)/2 − Tb)` acumulada desde la plena floración, pero la base, el umbral y el hito que predicen deben definirse juntos y validarse con fechas observadas. Open-Meteo da temperatura de modelos/reanálisis, no del lote. Se habilita cuando mejore el error en días frente al calendario desde floración, y se guardará su procedencia.
* **Calibración posterior.** Registrar floración y producción del año siguiente, riego, poda y método de aclareo; no prometer autoajuste con un solo par de años.

## 4. Consecuencias

* El Plan del lote de la app puede mostrar "qué te falta" en lugar de un 404 o una cifra inventada.
* Sin un perfil aprobado para la variedad no se emite ninguna prescripción: es el comportamiento deseado hasta tener valores con fuente.
* Los clientes que leían `targetFruitsPerMeter`, `meanFruitsPerMeter` o los campos `…FruitsPerMeter` de la confirmación deben pasar a `…FruitsPerShoot` (la app Android aún no los consumía).
