# IAM y Profiles: lo que falta en el backend

- **Fecha:** 2026-10-13
- **Estado:** 🔴 no existe ningún endpoint. El backend (`viora-platform`, `develop`) corre con un productor "mock" (`DEFAULT_PRODUCER_ID`), sin Spring Security, sin token y con CORS `*`.
- **Sprint:** 3 (diferido a propósito: "sin valor agronómico"). En los Sprints 1–2 la app no necesita login.
- **Responsables:** Fabrizio y Diana (frontend). Backend: por definir.

## Endpoints

| # | Endpoint | Para qué sirve | US | TS |
|---|---|---|---|---|
| 1 | `POST /auth/sign-up` | Crear la cuenta con correo, contraseña y rol | US01 | TS01 |
| 2 | `POST /auth/sign-in` | Iniciar sesión y recibir los tokens JWT | US02 | TS02 |
| 3 | `POST /auth/refresh-tokens` | Renovar la sesión con el refresh token (con rotación) | US02 | TS03 |
| 4 | `GET /profiles/{userId}` | Consultar el perfil del usuario autenticado | US03 | TS04 |
| 5 | `PUT` y `PATCH /profiles/{userId}` | Completar o actualizar nombre, país y teléfono (E.164) | US03, US43 | TS05, TS35 |
| 6 | `PATCH /auth/passwords` | Cambiar la contraseña con la sesión iniciada | US04 | TS36 |
| 7 | `POST /auth/password-reset-tokens` | Pedir el código de restablecimiento por correo | US05 | TS37 |
| 8 | `PUT /auth/password-reset-tokens/{token}` | Restablecer la contraseña con el token de un solo uso | US05 | TS38 |

## Contrato según el informe (el código real manda cuando exista)

| Endpoint | Entrada | Salida | Reglas |
|---|---|---|---|
| `sign-up` | `{email, password, role}` | `UserAccountResource {id, email, role, createdAt}` | Contraseña de 8 o más caracteres alfanuméricos. Roles: `ROLE_PRODUCER`, `ROLE_TECHNICAL_MANAGER` |
| `sign-in` | `{email, password}` | `{accessToken, refreshToken, tokenType, expiresIn}` | JWT de 15 min con claim de rol. `401` con mensaje genérico si las credenciales fallan |
| `refresh-tokens` | refresh token | tokens nuevos | Rotación del refresh token |
| `GET /profiles/{userId}` | — | `{fullName, country, phoneNumber, ...}` | Solo el dueño del perfil |
| `PUT` / `PATCH /profiles/{userId}` | `{fullName, country (ISO alpha-2), phoneNumber (E.164)}` | perfil actualizado | Validación E.164 del teléfono |
| `PATCH /auth/passwords` | contraseña actual y nueva | `204` | — |
| `POST /auth/password-reset-tokens` | `{email}` | `202` | Respuesta igual exista o no el correo |
| `PUT /auth/password-reset-tokens/{token}` | `{newPassword}` | `204` | El token vence a los 15 min y es de un solo uso |

## Qué cambia en la app cuando esto exista

- Hoy no se envía `Authorization`. Habrá que agregar el interceptor, el almacenamiento seguro del token y la renovación con el refresh token (para que `401` no saque al usuario).
- Las pantallas de registro, inicio de sesión, perfil, cambio de contraseña y recuperación no tienen backend contra el cual probar todavía.
- El backend dejará de resolver todo contra el productor mock: los lotes, cosechas y muestreos de prueba se asociarán a cada usuario real.

## Para coordinar con el backend

- Quién implementa el contexto (`iam` y `profiles`) y cuándo se publica el contrato, para que el frontend pueda trabajar contra él.
- Cómo entra el rol `ROLE_TECHNICAL_MANAGER` si el gestor va en Flutter y no en esta app.
- Si el perfil se crea automáticamente al registrarse o es un paso aparte (US43).
