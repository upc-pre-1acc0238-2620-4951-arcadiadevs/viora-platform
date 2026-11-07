# ADR-007: Acopio, renovación de licencia y salida de socios del Gestor

- **Estado:** Aceptado
- **Fecha:** 2026-11-07
- **Requisitos:** US32 y TS30 (proyección de acopio), US49 y TS60 (renovación y salida de socios); pantalla G30 de la app Flutter del Gestor
- **Relacionada:** `docs/gestor-mvp-bases-p01-p11.md` del repositorio Flutter (P01, P07, P08, §4.1 y §7.3), ADR-005 (expediente sellado)

## 1. Contexto

La tarjeta une tres cosas que el backend no tenía: la proyección de acopio que el gestor ve en G30, la renovación de la licencia de una organización y la salida de un socio. Viora tiene solo dos segmentos con app, el productor y el gestor. La organización paga la licencia, pero no tiene login, y no existe un rol de soporte.

## 2. Decisiones

### 2.1 Proyección de acopio

- **Solo parcelas estimables, sin extrapolar** (P07). Una parcela es estimable cuando Thinning la estima (`YieldEstimate` en `AVAILABLE`); su aporte es kg/ha por su área. Las demás no suman nada: no se usa el factor 0,85 ni un promedio.
- **Qué estimación suma.** La de después del aclareo, solo si el socio ya lo registró; si no, la de la carga actual. No se supone un aclareo que no ocurrió. **Pendiente de validar con el agrónomo.**
- **Verde y negra** salen de la proporción histórica asentada de cada parcela, marcada como estimación. Sin historial, la parcela va a «Sin clasificar».
- **Cobertura:** parcelas con muestreo sobre parcelas de los socios que comparten. Bajo el 50 % la proyección es «Preliminar»; sin ningún muestreo no hay cifra.
- **Campañas cerradas:** el acopio real asentado. Solo cuentan las cosechas asentadas **desde que el socio se vinculó**: las anteriores son privadas (§4.1).
- **Estado de campaña** (P08): abierta, parcial, cerrada o sin datos.
- **Sin ON/OFF colectivo.** La hoja «Elegir campaña» de G30 lo dibuja, pero no hay regla para calcularlo (P08). No se devuelve hasta tener una; Figma debe ajustarse.

### 2.2 Renovación de la licencia

- La organización renueva por contrato, fuera de la app: ningún segmento la dispara.
- Se implementa solo la regla de dominio. `CooperativeLicense.renew` alarga la vigencia y publica `CooperativeLicenseRenewedEvent`. Un handler de Subscription alarga los planes patrocinados vigentes que terminaban con la licencia, y sus consentimientos activos, **sin códigos nuevos** (P01).
- No hay endpoint. La llamará el script que cree y renueve licencias, que hoy no existe.

### 2.3 Salida de un socio

- **Borrado de cuenta (§7.3, decidido el 2026-11-07):** al borrar el socio su cuenta, su código pasa a `RELEASED` y su plaza y sus hectáreas vuelven a la licencia en ese momento.
- **Desvinculación (P01, «un socio se va, o canje por error»):** existe el endpoint, para el token del gestor de esa licencia, pero **la app no muestra un botón**: la usa soporte cuando corresponde. La lectura del gestor termina en ese momento. El plan sigue hasta el fin de la gracia (30 días, configurable con `viora.subscription.member-release.grace-days`) o de la licencia, lo que llegue antes. Recién entonces se liberan la plaza y las hectáreas.
- Los 30 días eran «por confirmar» en el documento; quedan configurables.

## 3. Consecuencias

- G30 tiene backend propio. Su caché sin conexión la resuelve la app con `evaluatedAt`.
- Una renovación queda lista para el script de licencias, sin rehacer los planes de los socios.
- Una plaza puede quedar ocupada hasta 30 días después de desvincular a un socio. Se libera sola al consultar o emitir códigos, igual que un código vencido.
- Pendiente: validar con el agrónomo qué estimación suma el acopio y una regla de ON/OFF colectivo.
