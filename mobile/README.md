# Zona Aquática — App móvil (Android)

App Android nativa para el **personal** de la piscina/escuela de natación.
Es solo el front: consume el mismo backend Flask + PostgreSQL que ya corre en
Docker en la carpeta `piscina/`.

**Stack:** Kotlin + Jetpack Compose (Material 3) · Retrofit + OkHttp + Moshi ·
ViewModel + StateFlow · sin generadores de código (nada de kapt/KSP).

---

## Cobertura: las 11 pantallas del panel web, completas

| Pantalla | Rol | Equivalente web |
|---|---|---|
| Login (+ configurar IP del servidor) | — | `LoginPage.jsx` |
| Crear contraseña (obligatorio la 1ª vez) | ambos | `CambiarPasswordPage.jsx` |
| **Clientes** — pestañas activo/inactivo/eliminado, búsqueda, alta/edición, ficha, cobros (efectivo/QR/mixto con saldo), renovar membresía, **recibo PDF**, inactivar/eliminar/recuperar | ambos | `ClientesPage.jsx` |
| **Asistencia** — pasar lista por día y horario, presente/falta/justificada con días de extensión | ambos | `AsistenciaPage.jsx` |
| **Horarios** — lista con ocupación, clientes asignados, CRUD | ambos (CRUD solo admin) | `HorariosPage.jsx` |
| **Panel** — indicadores, caja del día, ocupación, distribución de planes | admin | `admin/DashboardPage.jsx` |
| **Membresías** — CRUD de planes, días de acceso, nº de clases | admin | `admin/MembresiasPage.jsx` |
| **Ingresos y egresos** — balance por período, gráfico día a día, egresos por categoría, ingresos por cliente, cierre de caja con recibos | admin | `admin/FinanzasPage.jsx` |
| **Gastos** — CRUD de egresos, filtros por fecha y categoría, total del período | admin | `admin/GastosPage.jsx` |
| **Personal** — usuarios, contraseña temporal, reset, activar/desactivar | admin | `admin/PersonalPage.jsx` |
| **Actividad** — registro de auditoría con filtros por acción y módulo | admin | `admin/RegistroActividadPage.jsx` |
| **Página pública** — editor completo de la landing, con subida de fotos desde el teléfono | admin | `admin/ContenidoPublicoPage.jsx` |

El rol **recepcionista** está cubierto al 100 % y el **admin** también.

---

## Puesta en marcha

### 1. Levanta el backend

Desde la carpeta `piscina/`:

```bash
docker compose up -d
```

Comprueba: <http://localhost:5000/api/health>

### 2. Prepara la red para el celular

Clic derecho en `mobile/scripts/preparar-red.ps1` → **Ejecutar con PowerShell**
(como Administrador la primera vez, para que pueda crear la regla del Firewall).

El script hace tres cosas:

1. Detecta la IP WiFi de tu PC.
2. Crea la regla de Firewall entrante para **TCP 5000** (perfil Privado).
3. Te dice qué IP escribir en la app.

> No hace falta tocar `CORS_ORIGINS` del `.env`: CORS es una política del
> navegador y una app nativa no envía cabecera `Origin`.

### 3. Abre el proyecto en Android Studio

**File → Open** y elige la carpeta `piscina/mobile` (no `piscina`).
El primer sync descarga las dependencias.

Ya está configurado para tu equipo:

| | |
|---|---|
| AGP | 9.3.1 |
| Kotlin | 2.2.10 |
| Gradle | 9.5.0 (wrapper incluido) |
| compileSdk / targetSdk | 37 |
| minSdk | 26 (Android 8.0) |
| JDK | el que trae Android Studio (JBR) |

### 4. Instala en el celular

1. En el celular: **Opciones de desarrollador → Depuración por USB**.
2. Conéctalo por USB y dale **Permitir** a la huella RSA.
3. En Android Studio elige el dispositivo y pulsa **Run ▶**.

También puedes instalar el APK a mano:

```
mobile/app/build/outputs/apk/debug/app-debug.apk
```

### 5. Apunta la app al backend

En la pantalla de login, botón **Servidor**: escribe la IP que te dio el
script (ej. `10.7.3.147`). La app completa sola `:5000/api`.

| Dónde pruebas | Qué escribir |
|---|---|
| Celular por WiFi | la IP LAN de tu PC, ej. `192.168.1.20` |
| Emulador de Android Studio | `10.0.2.2` |

La IP queda guardada; puedes cambiarla sin recompilar.

### 6. Entra

Usuario `admin`, contraseña `Admin123!` (los valores de `SEED_ADMIN_*` del
`.env`). Si creas usuarios desde **Personal**, cada uno recibe una contraseña
temporal que se muestra **una sola vez** y que deberá cambiar al entrar.

---

## Cambiar la IP por defecto al compilar

En `mobile/gradle.properties`:

```properties
zonaApiUrl=http://192.168.1.20:5000/api/
```

Se compila en `BuildConfig.DEFAULT_API_URL` y es el valor inicial de la app
(mientras el usuario no lo cambie desde la pantalla de login).

---

## Estructura

```
mobile/app/src/main/java/bo/zonaaquatica/movil/
├── MainActivity.kt
├── ZonaApp.kt                  Application + Contenedor (DI a mano)
├── core/
│   ├── Prefs.kt                tokens JWT + URL del backend
│   ├── Resultado.kt            Ok/Error + traducción de errores HTTP
│   ├── Formato.kt              fechas, montos en Bs, días de la semana
│   └── Recibos.kt              guarda el PDF y lo abre vía FileProvider
├── data/
│   ├── model/Modelos.kt        espejo de los to_dict() de Flask
│   ├── api/PiscinaApi.kt       interfaz Retrofit (todos los endpoints)
│   ├── api/Red.kt              OkHttp: URL dinámica, Bearer, refresh en 401
│   └── repo/Repositorios.kt    Auth, Clientes, Catálogo, Asistencia, Personal,
│                               Dashboard, Finanzas, Gastos, Auditoría,
│                               Contenido, Recibos
└── ui/
    ├── theme/Tema.kt           paleta aqua/sol + paleta de gráficos validada
    ├── common/Componentes.kt   tarjetas, etiquetas, campos, selector de días…
    ├── common/Graficos.kt      líneas y barras dibujadas con Canvas
    ├── nav/                    AppNav (login/password/panel) + PanelShell (cajón)
    ├── login/ · password/
    ├── clientes/ · asistencia/ · horarios/
    ├── dashboard/ · membresias/ · personal/
    └── finanzas/ · gastos/ · actividad/ · contenido/
```

### Cómo se conecta con el backend

* **URL dinámica.** `InterceptorUrlBase` reescribe esquema/host/puerto de cada
  petición con lo que haya en `Prefs`, así que cambiar de servidor no requiere
  reconstruir Retrofit ni reiniciar la app.
* **JWT.** `InterceptorAuth` agrega `Authorization: Bearer <access>`.
  `AutenticadorJwt` reacciona a un 401, llama a `POST /api/auth/refresh` con el
  refresh token y reintenta la petición — el mismo comportamiento que el
  interceptor de axios en `frontend/src/services/api.js`.
* **Nulos explícitos.** El converter usa `withNullSerialization()` porque el
  backend distingue "campo ausente" de "campo en null" (`if campo in data`).
* **Roles.** El menú y los botones se filtran por rol igual que `Sidebar.jsx`,
  pero la autoridad real es el backend (`@rol_requerido`). La app muestra el
  mensaje de error que devuelve Flask.
* **Recibos PDF.** `GET /api/pagos/{id}/recibo` se descarga a la caché de la app
  y se abre con el visor del teléfono mediante `FileProvider` (pasar un `file://`
  a otra app lanzaría `FileUriExposedException` desde Android 7). Disponible
  para ambos roles, igual que en el backend.
* **Subida de fotos.** El editor de la página pública usa el selector de fotos
  del sistema (`PickVisualMedia`), que **no pide permiso de almacenamiento**, y
  manda la imagen como `multipart/form-data` al campo `file` de
  `POST /api/contenido/upload` con el `tipo` (logo | hero | galeria), igual que
  el editor web. Límite de 8 MB por archivo.

### Sobre los gráficos

Están dibujados con `Canvas` de Compose (sin librería de charts) siguiendo las
reglas de visualización del proyecto:

* **Un solo eje.** Ingresos y egresos están los dos en Bs, así que comparten
  escala: nunca hay doble eje.
* **Paleta validada.** Las dos series usan slots categóricos verificados con el
  validador de paletas (banda de luminosidad, piso de croma, separación para
  daltonismo, piso de visión normal y contraste contra la superficie): pasan los
  cinco checks en modo claro **y** oscuro. Están en `ui/theme/Tema.kt` con la
  advertencia de no cambiarlas sin volver a validar.
* **Un color por serie, no por tamaño.** Las barras de categorías son de una
  sola serie, así que llevan un solo color: la longitud ya codifica el valor.
* **El color nunca es el único canal.** Hay leyenda siempre, totales en texto,
  y el botón **Ver tabla** da el gemelo accesible del gráfico de líneas. El
  color de estado (balance positivo/negativo) va con icono y texto.
* **Toca un día** en el gráfico para ver ingresos y egresos de esa fecha; el
  área de toque es toda la franja vertical, muy por encima del mínimo táctil.

---

## Notas y limitaciones conocidas

* **Sin prueba contra el backend real.** El código compila sin errores y los
  DTOs coinciden campo por campo con los `to_dict()` de Flask, pero no se pudo
  hacer una corrida end-to-end (Docker estaba apagado al generar el proyecto).
  Tampoco hay captura de pantalla de los gráficos renderizados. Eso se verifica
  en la primera ejecución.
* **HTTP en claro.** `usesCleartextTraffic="true"` y
  `res/xml/network_security_config.xml` permiten HTTP porque el backend corre
  sin TLS en la red local. Si algún día lo pones detrás de HTTPS (el
  `Caddyfile` del proyecto), cambia `cleartextTrafficPermitted` a `false`.
* **Tokens en SharedPreferences** (almacenamiento privado de la app, sin
  cifrado adicional). El `Authenticator` de OkHttp necesita leerlos de forma
  sincrónica. Para uso interno en la piscina es razonable; si se distribuye
  más, conviene `EncryptedSharedPreferences`.
* **`isMinifyEnabled = false` en release**, porque Moshi usa reflexión de
  Kotlin. Si se quiere minificar, hay que pasar a `moshi-kotlin-codegen` (KSP).
* **Sin modo offline.** Cada pantalla pide datos a la API; si no hay red se
  muestra el error con las causas probables. No hay caché local.
* **Las imágenes no se previsualizan.** El editor de la página pública muestra
  la URL de cada imagen, no una miniatura: cargarlas exigiría una librería de
  imágenes (Coil) y resolver las rutas relativas `/uploads/...` contra la IP
  configurada. Las fotos se ven en la web pública.
* **El modelo 3D** (`.glb`/`.gltf`) se puede subir desde la app, pero no se
  previsualiza; el recorrido se ve en la web pública.
* El dispositivo y la PC deben estar en la **misma red**, y la red WiFi de
  Windows marcada como **Privada** (si es "Pública", el Firewall bloquea la
  regla).

---

## Compilar desde la terminal

```bash
cd mobile
./gradlew assembleDebug          # Linux/Mac/Git Bash
.\gradlew.bat assembleDebug      # Windows
```

En Windows, si `java` no está en el PATH:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
```
