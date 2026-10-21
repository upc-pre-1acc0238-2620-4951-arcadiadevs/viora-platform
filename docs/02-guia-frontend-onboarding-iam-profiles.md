# Guía de Integración Frontend/Mobile: Onboarding Personalizado, IAM y Profiles

- **Fecha:** 2026-10-13
- **Destinatarios:** Fabrizio y Diana (Frontend / Mobile), Equipo Backend Viora
- **Bounded Contexts:** `bc9 (Identity & Access Management)` y `bc7 (User Profiles)`
- **Referencia base:** [`docs/01-iam-y-profiles-faltante.md`](file:///home/santi2007939/workspace/apps/1acc0238/viora-platform/docs/01-iam-y-profiles-faltante.md)
- **Estado:** ✅ Especificación Acordada (Opción B: Onboarding Asistido + Registro Unificado)

---

## 1. Contexto y Decisión Arquitectónica (El Patrón "Duolingo")

### El requerimiento de la App Móvil
En la aplicación móvil, para maximizar la conversión y ofrecer una experiencia más humana y personalizada, el usuario no empieza con un formulario frío de email y contraseña. Primero responde:
1. Su nombre completo (`fullName`).
2. Su país de operación agrícola (`country`, código ISO-3166-1 alpha-2, ej. `PE`, `ES`, `CL`).
3. Su teléfono de contacto (`phoneNumber`, formato internacional E.164, ej. `+51987654321`).
4. (Futuro: preguntas agronómicas iniciales de personalización).

Al final del recorrido, la app muestra la pantalla: **"Guarda tu cuenta y tus parcelas"**, donde el usuario ingresa su email y contraseña.

### Decisión de Backend: Opción B (Client-Side State + Registro Unificado)
Para evitar crear perfiles "huérfanos" o registros basura en la base de datos de usuarios que abandonen el onboarding antes de registrarse:
1. **La app móvil acumula los datos localmente** (en memoria o en almacenamiento seguro de la app) a lo largo del Wizard/Pasos de onboarding.
2. **El backend provee un endpoint unificado de registro:** `POST /api/v1/auth/sign-up` que acepta las credenciales de la cuenta y, opcionalmente, el bloque inicial de perfil.
3. **Verificación del correo antes de la sesión:** El backend crea la cuenta en `IAM` como pendiente (`PENDING_VERIFICATION`), formaliza el perfil en `Profiles` y envía un código de 6 dígitos al correo. **El sign-up ya no devuelve tokens:** la sesión llega al verificar el código (T04a), con `POST /api/v1/auth/email-verifications`.

---

## 2. Especificación de Endpoints y Contratos

### 2.1. Registro Unificado (`POST /api/v1/auth/sign-up`) — T04

Crea la cuenta pendiente de verificación y su perfil en una sola operación atómica, y envía el código de verificación al correo.

- **URL:** `POST /api/v1/auth/sign-up`
- **Autenticación requerida:** Ninguna (Público)

#### Payload de Solicitud (Request Body):
```json
{
  "email": "productor.demo@viora.ag",
  "password": "PasswordSegura123!",
  "role": "ROLE_PRODUCER",
  "termsAccepted": true,
  "profile": {
    "fullName": "Carlos Mendoza",
    "country": "PE",
    "phoneNumber": "+51987654321"
  }
}
```

#### Reglas de Validación:
* `email`: Formato válido de correo electrónico. Si ya pertenece a una **cuenta activa** responde `409` (`user.email.already_exists`).
* `password`: 8 caracteres o más, con letras y números. Si no cumple, responde `400` con **todas** las reglas incumplidas en `violations` (ver la sección 2.7).
* `role`: Obligatorio. Solo se acepta `ROLE_PRODUCER`; cualquier otro responde `400` (`user.role.sign_up_not_allowed`).
* `termsAccepted`: Obligatorio y en `true`. El backend registra la versión vigente de los términos y la fecha (Ley 29733).
* `profile.*`: Validados por `Profiles` con `libphonenumber`. El teléfono debe ser un **celular del país elegido** (`profile.phone.not_mobile` si es fijo, `profile.phone.country_mismatch` si es de otro país). Si el perfil es inválido, **no se crea la cuenta**.

> **Volver a registrarse con un correo pendiente** (por ejemplo, desde "Cambiar correo" o si la persona cierra la app) **reutiliza la misma cuenta**: actualiza la contraseña y el perfil y envía un código nuevo. Si se hace antes de 45 s del código anterior responde `429` con `Retry-After`.

> **Cuentas sin verificar:** una cuenta pendiente sin actividad durante **7 días** (sin registrarse de nuevo, reenviar el código ni intentar verificarlo) se borra junto con su perfil. Después, el correo queda libre para registrarse otra vez.

#### Respuesta Exitosa (`202 Accepted`) — sin tokens:
```json
{
  "userId": "a64b9b7e-913a-4e6f-99a6-12c4c8108991",
  "email": "productor.demo@viora.ag",
  "status": "PENDING_VERIFICATION"
}
```

---

### 2.1.1. Verificar el Correo (`POST /api/v1/auth/email-verifications`) — T04a

- **Payload:** `{"email": "productor.demo@viora.ag", "code": "482913"}`
- **Respuesta (`200 OK`):** la misma sesión que `sign-in` (`accessToken`, `refreshToken`, `userId`, `role`, ...). La cuenta pasa a `ACTIVE`.
- **Errores (`422`, campo `reason`):**

| `reason` | Qué mostrar |
|---|---|
| `user.email_verification.invalid_code` | "Te quedan N intentos", con `remainingAttempts` |
| `user.email_verification.attempts_exceeded` | Tras 3 códigos incorrectos: pedir un código nuevo |
| `user.email_verification.code_expired` | El código vence a los 10 minutos: pedir uno nuevo |
| `user.email_verification.no_active_code` | No hay código activo para ese correo |

### 2.1.2. Reenviar el Código (`POST /api/v1/auth/email-verification-codes`) — T04a "Reenviar código"

- **Payload:** `{"email": "productor.demo@viora.ag"}`
- **Respuesta:** `202 Accepted` (también para correos inexistentes o ya verificados).
- **Antes de 45 s:** `429 Too Many Requests` con cabecera `Retry-After: <segundos>` y el campo `retryAfterSeconds`, para el contador "Reenviar código en 0:45".

---

### 2.2. Inicio de Sesión (`POST /api/v1/auth/sign-in`) — T03

- **URL:** `POST /api/v1/auth/sign-in`
- **Autenticación requerida:** Ninguna (Público)

#### Request Body:
```json
{
  "email": "productor.demo@viora.ag",
  "password": "PasswordSegura123!"
}
```

#### Respuesta Exitosa (`200 OK`):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "d8f076b1-4b7e-4054-949e-ec7f37452da0",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "userId": "a64b9b7e-913a-4e6f-99a6-12c4c8108991",
  "role": "ROLE_PRODUCER"
}
```

#### Errores:
* Correo o contraseña incorrectos: `403`, `code: USER_FORBIDDEN` (mensaje genérico, no dice cuál de los dos falló).
* Correo sin verificar (solo si la contraseña es correcta): `403`, `code: EMAIL_VERIFICATION_FORBIDDEN`. La app lleva a T04a y puede reenviar el código.

---

### 2.3. Renovación de Sesión con Rotación (`POST /api/v1/auth/refresh-tokens`)

El `accessToken` dura **15 minutos** y el `refreshToken` **7 días**. Con el refresh token se obtiene un par nuevo sin volver a pedir la contraseña.

- **URL:** `POST /api/v1/auth/refresh-tokens`
- **Autenticación requerida:** Ninguna (se envía el refresh token en el cuerpo)

#### Request Body:
```json
{
  "refreshToken": "Q2h3b0p3...-x9"
}
```

#### Respuesta Exitosa (`200 OK`):
La misma sesión que `sign-in`, con un **`refreshToken` nuevo**.

#### Reglas importantes:
* **Rotación:** cada refresh token sirve **una sola vez**. La app debe guardar siempre el último `refreshToken` recibido.
* **Reuso = robo:** si se envía un refresh token que ya se usó, el backend cierra **todas** las sesiones de la cuenta y responde `403` (`code: USER_FORBIDDEN`). La app debe ir al login.
* Hay que enviar el **refresh token**, no el access token.
* También responde `403` si el refresh token venció o si la contraseña cambió después de emitirlo.

---

### 2.3.1. Cambiar la Contraseña (`PATCH /api/v1/auth/passwords`)

- **Cabecera requerida:** `Authorization: Bearer <accessToken>`
- **Payload:** `{"currentPassword": "PasswordSegura123", "newPassword": "NuevaClave456"}`
- **Respuesta:** `200 OK` con una **sesión nueva** (los mismos campos que `sign-in`). Se cierran las **otras** sesiones; este teléfono sigue conectado, pero debe **reemplazar sus dos tokens** por los de la respuesta, porque los anteriores dejan de servir.
- **Errores:** `401` sin token; `422` con `reason: user.password.current_invalid` si la contraseña actual es incorrecta; `400` con `violations` o `422` con `reason: user.password.reused` según las reglas de la sección 2.7.

---

### 2.3.2. Cerrar Sesión (`POST /api/v1/auth/sign-out`) — P95 "Cerrar sesión"

- **Payload:** `{"refreshToken": "<el refresh token guardado>"}`
- **Respuesta:** `204 No Content` siempre, aunque la sesión ya estuviera cerrada o el token no exista.
- **Efecto:** ese refresh token deja de servir. La app debe además **borrar sus tokens** guardados. El access token vigente vence solo en, como máximo, 15 minutos.

---

### 2.4. Consulta de Perfil (`GET /api/v1/profiles/{userId}`)

El perfil se crea al registrarse (sección 2.1); no hay endpoint para crearlo. **Cada usuario solo puede ver y modificar su propio perfil:** `{userId}` debe ser el `userId` de la sesión.

- **URL:** `GET /api/v1/profiles/{userId}`
- **Cabecera requerida:** `Authorization: Bearer <accessToken>`
- **Errores:** `401` sin sesión; `403` con `code: USER_FORBIDDEN` si `{userId}` no es el usuario autenticado; `404` si la cuenta no tiene perfil.

#### Respuesta Exitosa (`200 OK`):
```json
{
  "id": "e3b0c442-98fc-1c14-9afbf4c8996fb924",
  "userId": "a64b9b7e-913a-4e6f-99a6-12c4c8108991",
  "fullName": "Carlos Mendoza",
  "country": "PE",
  "phoneNumber": "+51987654321",
  "createdAt": "2026-10-13T20:15:00Z",
  "updatedAt": "2026-10-13T20:15:00Z"
}
```

---

### 2.5. Actualización de Perfil (`PUT /api/v1/profiles/{userId}`)

Permite modificar los datos de contacto del perfil creado al registrarse. No crea perfiles: `POST /api/v1/profiles/{userId}` responde `405` (cabecera `Allow: GET, PATCH, PUT`).

- **URL:** `PUT /api/v1/profiles/{userId}` (o `PATCH`, con el mismo cuerpo)
- **Cabecera requerida:** `Authorization: Bearer <accessToken>`
- **Errores:** `401` sin sesión; `403` con `code: USER_FORBIDDEN` si `{userId}` no es el usuario autenticado; `404` si la cuenta no tiene perfil; `400` si el teléfono no es un **celular** válido del país elegido (`profile.phone.invalid_e164`, `profile.phone.not_mobile` o `profile.phone.country_mismatch`).

#### Request Body:
```json
{
  "fullName": "Carlos Mendoza Alarcón",
  "country": "PE",
  "phoneNumber": "+51987654321"
}
```

#### Respuesta Exitosa (`200 OK`):
Retorna el perfil actualizado.

---

### 2.6. Recuperación de Contraseña por Enlace (T06–T09)

La recuperación usa un **enlace de un solo uso** que abre la app directamente (Android App Links).

Si el teléfono abre el enlace en el navegador (el APK instalado está firmado con una huella que no figura en `assetlinks.json`, la verificación del dominio no corrió o la app de correo usa su propio navegador), `GET /password-resets/{token}` responde una **página de Viora** que abre la app con un `intent://…#Intent;scheme=https;package=pe.edu.upc.viora;end`. Ese intent nombra el paquete, así que no depende de la verificación. La página no valida el token: lo hace la app con `GET /api/v1/auth/password-reset-tokens/{token}`.

#### Paso 1: Pedir el enlace (`POST /api/v1/auth/password-reset-tokens`) — T06/T07
- **Payload:** `{"email": "productor.demo@viora.ag"}`
- **Respuesta:** `202 Accepted` siempre, exista o no el correo.
- **Efecto:** si la cuenta existe y está verificada, Brevo envía un correo con el botón **"Crear nueva contraseña"**, que apunta a `https://<dominio>/password-resets/<token>`. El enlace vence en **30 minutos**, es de un solo uso y pedir uno nuevo invalida el anterior.

#### Paso 2: Abrir el enlace en la app
- Android abre la app con la URL completa. La app toma el `token` del **último segmento de la ruta** (`/password-resets/{token}`).
- La app comprueba el enlace con `GET /api/v1/auth/password-reset-tokens/{token}`:
  - `204 No Content`: el enlace sirve, se muestra **T08**.
  - `404` con `code: PASSWORD_RESET_TOKEN_NOT_FOUND`: venció, ya se usó o fue reemplazado; se muestra **T09** ("Este enlace ya venció") con el botón para pedir uno nuevo.

#### Paso 3: Crear la nueva contraseña (`PUT /api/v1/auth/password-reset-tokens/{token}`) — T08
- **Payload:** `{"newPassword": "NuevaClave456"}`. **Ya no lleva `email`:** el token identifica la cuenta.
- **Respuesta:** `204 No Content`. Se cierran todas las sesiones: la app debe pedir iniciar sesión de nuevo.
- **Errores:** `400` con `violations` si la contraseña no cumple las reglas; `422` con `reason: user.password.reused` si es una de las últimas 5 (el enlace sigue sirviendo); `404 PASSWORD_RESET_TOKEN_NOT_FOUND` si el enlace ya no sirve (T09).

#### Configuración en la app (`AndroidManifest.xml`)
```xml
<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="https"
          android:host="<dominio>"
          android:pathPrefix="/password-resets/" />
</intent-filter>
```
Para comprobarlo con el APK instalado: `adb shell pm get-app-links <package>` debe mostrar el dominio como `verified`.

#### Configuración en el backend
Ya viene configurada por defecto en `application.properties`; las variables de entorno solo hacen falta para cambiarla.

| Variable | Valor actual | Cuándo cambia |
|---|---|---|
| `VIORA_DEEP_LINK_BASE_URL` | `https://viora-platform.onrender.com` | Si los enlaces pasan a un subdominio propio (ej. `https://viora.arcadiadevs.com`) |
| `ANDROID_PACKAGE_NAME` | `pe.edu.upc.viora` | Nunca, salvo que cambie el application id |
| `ANDROID_SHA256_CERT_FINGERPRINTS` | `8C:B1:E1:62:88:15:5E:46:61:0D:A7:C8:60:BA:E7:2B:A2:42:48:49:C4:2C:C3:88:C6:02:21:FA:04:B4:F4:12` | Solo si cambia el keystore; al publicar en Play Store se **agrega** la huella de Play App Signing, separada por coma |

`https://viora-platform.onrender.com/.well-known/assetlinks.json` debe devolver ese paquete y esa huella.

#### Firma de la app y Firebase App Distribution
- La huella es la del **certificado del keystore** que firma el APK (`CN=Viora, OU=ArcadiaDevs, O=UPC, L=Lima, ST=Lima, C=PE`), no la de cada APK: **no cambia entre versiones** mientras se firme con ese keystore. Guárdenlo de forma segura; si se pierde, la huella cambia y los enlaces dejan de abrir la app.
- Firebase App Distribution **no vuelve a firmar los APK**: los testers instalan exactamente lo que ustedes suben. Si algún día suben un **AAB**, Firebase lo firma con Play App Signing y hay que agregar esa huella.
- Si compilan en un **CI**, firmen con ese mismo keystore guardado como secreto. Con el keystore de debug del runner la huella cambiaría en cada build.
- Para leer la huella de un APK, usen `apksigner verify --print-certs app.apk`. `keytool` no sirve con este APK porque solo tiene firma v2.
- Android verifica el dominio **al instalar**: quien tenía la app instalada antes de publicar `assetlinks.json` debe reinstalarla o actualizarla desde Firebase. Con el teléfono conectado: `adb shell pm verify-app-links --re-verify pe.edu.upc.viora` y luego `adb shell pm get-app-links pe.edu.upc.viora`.
- **Brevo:** debe estar desactivado el **seguimiento de clics** de los correos transaccionales; si no, el botón apunta a Brevo y Android no abre la app.

---

### 2.6.1. Token vencido o inválido → `401`

Si una petición trae `Authorization: Bearer` y el token no autentica (venció, fue alterado o es anterior al último cambio de contraseña), el backend responde **`401`** en cualquier ruta que no sea pública (`/api/v1/auth/**`, `/.well-known/**`, `/password-resets/**`). La app renueva la sesión (sección 2.3) y repite la petición. Sin cabecera `Authorization` la petición sigue siendo anónima, como antes.

### 2.6.2. Cuenta demo

Con `VIORA_DEMO_ACCOUNT_EMAIL` y `VIORA_DEMO_ACCOUNT_PASSWORD` configuradas, el arranque crea una cuenta **verificada** de productor cuyo `userId` es el productor demo (`DEFAULT_PRODUCER_ID`, dueño de los datos sembrados) y su perfil (`VIORA_DEMO_ACCOUNT_FULL_NAME`, `VIORA_DEMO_ACCOUNT_COUNTRY`, `VIORA_DEMO_ACCOUNT_PHONE`). Así los datos de demostración se ven iniciando sesión. Las credenciales nunca van al repositorio; una cuenta existente no se modifica (para cambiarlas, borrar la cuenta y reiniciar).

### 2.7. Reglas de Contraseña (registro, cambio y recuperación)

* 8 caracteres o más, con letras y números. Si no se cumplen, `400` con `code: VALIDATION_ERROR` y la lista `violations`:
  ```json
  { "status": 400, "code": "VALIDATION_ERROR", "violations": ["user.password.min_length", "user.password.letters_and_digits"] }
  ```
  La app puede marcar cada ✓ con esas claves (`user.password.min_length`, `user.password.letters_and_digits`).
* No puede ser ninguna de las **últimas 5 contraseñas** de la cuenta, incluida la actual: `422` con `reason: user.password.reused` ("una que no hayas usado antes en Viora").

---

## 3. Consideraciones Clave para el Frontend / Mobile

1. **Gestión de Estado del Onboarding:**
   - Mantener las respuestas del onboarding en el gestor de estado de la app (Bloc, Riverpod, Provider, Pinia, etc.).
   - Persistir temporalmente en `SharedPreferences` o `SecureStorage` para que, si la app se cierra inesperadamente en el paso 3, el productor no pierda sus datos al reabrirla.
   - Limpiar el estado local solo tras verificar el correo (`200 OK` de `email-verifications`), no tras el `202` del `sign-up`.

2. **Validación E.164 Previa en el Cliente:**
   - Para evitar rechazos `400 Bad Request`, la app móvil debe validar el teléfono con librerías cliente compatibles con Google libphonenumber (ej. `phone_number_lookup` o `intl_phone_field` en Flutter): debe ser un **celular** y del **país elegido** (no se aceptan fijos ni números de otro país).
   - Siempre enviar el número con prefijo internacional (`+51...`).

3. **Intersector HTTP y Renovación Silenciosa (Silent Refresh):**
   - Configurar un interceptor en el cliente HTTP (Dio, Axios, etc.).
   - Cuando una petición a endpoints protegidos (ej. `/api/v1/plots`) falle con `401 Unauthorized`:
     1. Pausar las peticiones en cola.
     2. Invocar `POST /api/v1/auth/refresh-tokens` con el `refreshToken` almacenado, **una sola vez** aunque haya varias peticiones fallidas: como el refresh token se usa una vez, dos renovaciones en paralelo cierran todas las sesiones.
     3. Si la renovación es exitosa, guardar **los dos** tokens nuevos y reintentar la petición original.
     4. Si la renovación falla (`403`), redirigir a la pantalla de Login.
   - Tras **cambiar** la contraseña desde la app, guardar los tokens nuevos de la respuesta y seguir en la app. Tras **recuperarla** con el enlace, ir al Login.

4. **Tratamiento de Errores Normalizados (RFC 7807 ProblemDetail):**
   - El backend responderá todos los errores en formato estándar `application/problem+json`:
   ```json
   {
     "type": "https://api.viora.com/errors/user-conflict",
     "title": "Conflict",
     "status": 409,
     "detail": "Este correo ya tiene una cuenta. Inicia sesión o usa otro correo",
     "instance": "/api/v1/auth/sign-up",
     "code": "USER_CONFLICT",
     "timestamp": "2026-10-19T15:00:00Z"
   }
   ```
   - `detail` llega **ya traducido** según la cabecera `Accept-Language` (`es` o `en`), listo para mostrar.
   - Para decidir qué hacer, usar `code` y, cuando vengan, los campos extra: `reason`, `remainingAttempts`, `retryAfterSeconds` o `violations`. No comparar el texto de `detail`.
