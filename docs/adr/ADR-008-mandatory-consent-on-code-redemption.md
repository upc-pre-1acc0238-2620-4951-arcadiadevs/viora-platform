# ADR-008: Consentimiento obligatorio al canjear un código de cooperativa

- **Estado:** Aceptado
- **Fecha:** 2026-11-09
- **Requisitos:** US45, US46, TS57, TS58 y TS59
- **Relacionada:** ADR-007 (salida de socios)

## 1. Contexto

`POST /cooperative-code-redemptions` aceptaba un canje sin `consentTextVersion`: el plan se activaba y el gestor veía al socio como `NOT_SHARING`. Una licencia patrocinada existe para que la organización vea los datos de sus socios, así que un socio sin consentimiento ocupa una plaza sin aportar nada.

## 2. Decisión

- `consentTextVersion` es **obligatorio** al canjear. Sin él, o en blanco, la respuesta es 400 y el código no se consume.
- Una versión distinta de la vigente sigue respondiendo 409 `subscription.consent.text_outdated`.
- Canjear registra siempre el consentimiento, en la misma transacción que el plan.

## 3. Consecuencias

- Las apps que envían solo `code` reciben 400 hasta actualizarse. Android 1.20.3 está en ese caso y necesita la versión con la pantalla de consentimiento.
- `NOT_SHARING` se mantiene: lo siguen teniendo los planes canjeados antes de este cambio y los consentimientos vencidos.
