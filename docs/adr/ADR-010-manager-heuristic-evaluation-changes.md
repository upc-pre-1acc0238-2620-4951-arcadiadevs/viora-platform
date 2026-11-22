# ADR-010: Cambios del backend por la evaluación heurística del Gestor

- **Estado:** Aceptado
- **Fecha:** 2026-11-22
- **Requisitos:** US12, US31, US32, US45, US55; TS24, TS29, TS30, TS60, TS108
- **Relacionada:** `viora-report/docs/requirements-specifications/manager-feedback-changes.md`, ADR-007 (acopio)

## 1. Contexto

La evaluación heurística de la app del gestor encontró cinco problemas. Tres necesitan datos que el backend no daba: el socio de cada parcela, el calibre de la proyección de acopio y un nivel de confianza graduado.

## 2. Decisiones

### 2.1 Código de socio

- `memberCode` es el código de activación que el socio canjeó, **enmascarado** (`COOP-••2Q`), el mismo que ya muestra el detalle del socio.
- No se muestra completo: el código cifrado se borra al canjearse (`InvitationCode.redeem`) y esa regla no cambia. Ya canjeado no sirve, pero mantenerlo cifrado no aporta y amplía lo que se guarda.
- Viene en `/member-plots` y en `/member-plots/{id}`, con el mismo valor.

### 2.2 Calibre del muestreo

- Una ronda de `POST /plots/{id}/samplings` acepta `fruitSample` opcional: `fruitCount` (1–1000) y `weightGrams` (> 0, hasta 10 000).
- El calibre es `fruitsPerKg = fruitCount × 1000 / weightGrams` y debe caer en la escala comercial que ya usa Thinning (`CommercialSizeScale`): **de 60 a 2000 frutos por kilo**. Fuera de ella, 400 y la ronda no se registra.
- Cuenta el calibre de la **última ronda que pesó frutos**: una muestra posterior dice cómo crecieron.
- Solo se registra con una ronda, y una ronda solo se acepta mientras la prescripción no está emitida. Pesar frutos después de emitirla queda para un cambio posterior.

### 2.3 Rangos de calibre

- Agregado `CaliberRangeSet`, uno por licencia, en `GET/PUT /api/v1/caliber-ranges`. Solo el gestor.
- Los rangos van del fruto más grande al más chico, **consecutivos**: sin superposición ni huecos, dentro de 60–2000 y como máximo 20. Un rango en conflicto da 400 con `reason` y `rangeIndex`, y la configuración vigente se conserva.
- Una lista vacía borra la configuración.

### 2.4 Proyección por calibre

- `caliberBreakdown` reparte las toneladas de cada parcela estimable en el rango de su calibre, en verde, negra y sin clasificar como el resto de la proyección.
- `unsizedTons`: parcelas estimables sin calibre. `outOfRangeTons`: parcelas cuyo calibre cae fuera de todos los rangos; no estaba en el documento, pero sin él esos kilos desaparecerían del desglose.
- Sin rangos configurados, `caliberBreakdown` va vacío y los dos totales en `null`.

### 2.5 Confianza graduada

- `confidenceLevel` es `LOW`, `MEDIUM` o `HIGH` según la **menor** de dos coberturas: parcelas muestreadas y hectáreas muestreadas. Así, unas pocas parcelas grandes o muchas chicas no bastan por sí solas.
- Umbrales de 50 % y 80 %, configurables con `viora.intake.confidence.medium-percent` y `viora.intake.confidence.high-percent`.
- `pendingPlots` y `pendingHectares` dicen cuánto falta para el siguiente nivel; en `HIGH`, cero.
- `status` y `coveragePercent` se mantienen para los clientes existentes.

## 3. Consecuencias

- Hasta que la app del productor (Android) envíe `fruitSample`, todo el volumen estimable cae en `unsizedTons`.
- Las columnas nuevas de `sampling_rounds` y las tablas `caliber_range_sets` y `caliber_ranges` las crea `ddl-auto=update`; no hay restricciones `CHECK` nuevas.
- Pendiente de validar con el agrónomo: la escala de calibre en el momento del muestreo de cuajado y los umbrales de confianza.
