   # Subir el proyecto a GitHub (y dejarlo listo para Railway)

Guía paso a paso para poner el código en GitHub. No hace falta desplegar todavía:
con tenerlo en GitHub, la próxima semana solo conectas Railway y listo.

---

## 1. Instala lo necesario (una sola vez)

- **Git**: descárgalo de https://git-scm.com/download/win e instálalo (deja todo por defecto).
- **Cuenta de GitHub**: crea una gratis en https://github.com si no tienes.

Verifica que Git quedó instalado (abre PowerShell y escribe):

```powershell
git --version
```

## 2. Crea el repositorio vacío en GitHub

1. Entra a https://github.com/new
2. **Repository name**: `zona-aquatica` (o el nombre que quieras).
3. Elige **Private** (recomendado, para que no sea público).
4. **No** marques "Add a README" ni ".gitignore" (ya los tenemos en el proyecto).
5. Clic en **Create repository**. Deja esa página abierta: te muestra la URL del repo.

## 3. Sube el proyecto (desde tu PC)

Abre **PowerShell** en la carpeta del proyecto. La forma fácil: abre la carpeta
`C:\Users\JOEL\Desktop\piscina` en el Explorador, escribe `powershell` en la barra de
dirección y presiona Enter.

Luego ejecuta estos comandos **uno por uno** (cambia la URL por la de tu repo):

```powershell
git init
git add .
git commit -m "Sistema Zona Aquatica - version inicial"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/zona-aquatica.git
git push -u origin main
```

La primera vez te pedirá iniciar sesión en GitHub (se abre una ventana del navegador).

> **Importante:** el archivo `.env` (con contraseñas) **no se sube**, está en
> `.gitignore`. Eso es lo correcto. También se ignoran `node_modules`, `__pycache__`
> y los respaldos.

### Verifica que quedó bien
Entra a tu repositorio en GitHub y confirma que ves las carpetas `backend/`,
`frontend/`, los `docker-compose*.yml`, el `README.md`, etc. Si **no** ves el archivo
`.env`, perfecto (así debe ser).

## 4. Cada vez que cambies algo más adelante

```powershell
git add .
git commit -m "describe el cambio"
git push
```

---

## 5. La próxima semana: conectar Railway (resumen)

Cuando quieras desplegar, en https://railway.app:

1. **New Project → Deploy from GitHub repo** y elige `zona-aquatica`.
2. Agrega una base de datos: **New → Database → PostgreSQL**.
3. **Servicio backend**: apunta el "Root Directory" a `/backend` (usa su `Dockerfile`).
   Variables de entorno:
   - `DATABASE_URL` → referencia a la de Postgres de Railway
   - `SECRET_KEY`, `JWT_SECRET_KEY` → cadenas aleatorias largas
   - `SEED_ADMIN_USERNAME`, `SEED_ADMIN_PASSWORD`
   - `FLASK_ENV=production`
   - `CORS_ORIGINS` → la URL pública del frontend (paso 4)
   - Start command: `gunicorn -w 3 -b 0.0.0.0:$PORT run:app`
4. **Servicio frontend**: root `/frontend`, build `npm run build`, publica la carpeta
   `dist`. Variable `VITE_API_URL` = `https://<url-del-backend>/api`.

En `DESPLIEGUE.md` está el detalle y también la alternativa con un VPS (un solo
comando con `docker-compose.prod.yml` + HTTPS automático).

> Cuando llegue el momento de desplegar, avísame y te acompaño en cada paso. 
