# Guía para subir Zona Aquática a internet

Esta guía te ayuda a pasar del sistema local a una **página web real** con dominio y
HTTPS (candado de seguridad). Incluye dos caminos y una recomendación económica.

> Precios aproximados de mediados de 2026 (en dólares/euros). Pueden variar.

---

## ¿Qué necesitas en cualquier caso?

1. **Un dominio** (ej: `zonaaquatica.com`) — desde **Namecheap**, **Cloudflare** o
   **Porkbun**. Cuesta **~$10–12 por año** (.com). Un `.bo` boliviano se registra vía
   NIC Bolivia y suele ser algo más caro.
2. **Un lugar donde correr la app** (servidor o plataforma) — ver opciones abajo.
3. Los archivos de producción que ya dejé en el proyecto:
   `docker-compose.prod.yml`, `Caddyfile`, `frontend/Dockerfile.prod` y `.env.prod.ejemplo`.

---

## Opción A (recomendada): un VPS con Docker  ← más económica y con control total

Un **VPS** es un servidor en la nube que administras tú. Corres el mismo Docker que
ya usas, más **Caddy** (incluido en los archivos de producción) que da **HTTPS
automático y gratis**. Es la opción más barata a largo plazo.

### Proveedores de VPS (2026)

| Proveedor | Plan base | Precio aprox. | Nota |
|---|---|---|---|
| **Hetzner** | CX22 · 2 vCPU / 4 GB | **~€4.50/mes** | Mejor precio/rendimiento. Data centers en EU y EE.UU. |
| **DigitalOcean** | Droplet básico | **~$4–6/mes** | Fácil, buena documentación, región EE.UU. cercana a Bolivia. |
| **Vultr** | 1 vCPU / 1 GB | **~$6/mes** | Muchas regiones (incluye EE.UU.). |
| **Contabo** | mucha RAM (8 GB) | **~$7/mes** | Mucho recurso por poco dinero. |

**Para tu caso** (piscina en Bolivia): elige una **región en EE.UU.** para menor
latencia. Con **DigitalOcean ($6)** o **Hetzner (€4.50)** te sobra para este sistema.

### Pasos (una sola vez)

1. Crea el VPS (Ubuntu 24.04) y apunta tu dominio a la **IP** del servidor
   (registro DNS tipo **A**: `zonaaquatica.com → IP`, y `www` igual).
2. Conéctate por SSH e instala Docker:
   ```bash
   curl -fsSL https://get.docker.com | sh
   ```
3. Sube el proyecto al servidor (con `git clone` de tu repo, o `scp` de la carpeta).
4. Crea el archivo `.env` con valores **reales y seguros** (copia de `.env.prod.ejemplo`)
   y pon tu dominio en `DOMAIN`.
5. Levanta todo en modo producción:
   ```bash
   docker compose -f docker-compose.prod.yml up -d --build
   ```
6. Abre `https://tudominio.com`. Caddy saca el certificado solo en el primer arranque.

Listo: la web pública queda en `https://tudominio.com` y el sistema del personal en
`https://tudominio.com/login`.

---

## Opción B (más fácil, sin administrar servidor): una plataforma (PaaS)

Subes el código y la plataforma se encarga del resto. Ideal si no quieres tocar un
servidor. Cuesta un poco más.

| Plataforma | Costo aprox. mensual | Para quién |
|---|---|---|
| **Railway** | **~$5–13/mes** (todo incluido, Postgres medido por uso) | La más simple. Recomendada aquí. |
| **Render** | **~$21–28/mes** (web $7 + Postgres $7 + extras) | Costos fijos y predecibles, estilo Heroku. |
| **Fly.io** | Por uso (por segundo) | Si quieres despliegue global/multiregión. |

En **Railway**: conectas tu repositorio de GitHub, creas un servicio para el backend,
otro para el frontend y una base **PostgreSQL**; configuras las variables de entorno
(las mismas del `.env`) y listo. Es el camino con menos mantenimiento.

---

## Cambios importantes para producción (ya contemplados)

- **Contraseñas y claves nuevas**: cambia `SECRET_KEY`, `JWT_SECRET_KEY`,
  `POSTGRES_PASSWORD` y `SEED_ADMIN_PASSWORD` en el `.env`. Nunca uses los de prueba.
- **HTTPS**: lo da Caddy automáticamente (Opción A) o la plataforma (Opción B).
- **Frontend compilado**: en producción el `Dockerfile.prod` compila React (no usa el
  modo desarrollo). Caddy sirve esos archivos y redirige `/api` y `/uploads` al backend.
- **CORS**: en `docker-compose.prod.yml` ya se limita a tu dominio.
- **Datos persistentes**: la base de datos y las imágenes subidas quedan en volúmenes
  de Docker, así no se pierden al reiniciar.

---

## Respaldos (muy importante)

Programa un respaldo diario de la base. En el servidor puedes usar `cron`:

```bash
# Editar tareas programadas
crontab -e
# Respaldo todos los días a las 2 AM (ajusta la ruta del proyecto)
0 2 * * * cd /ruta/al/proyecto && docker compose -f docker-compose.prod.yml exec -T db pg_dump -U piscina_admin piscina_db > backups/piscina_$(date +\%Y\%m\%d).sql
```

Y de vez en cuando copia esos respaldos a otro lugar (tu PC, Google Drive, etc.).

---

## Resumen de costos (lo más económico)

| Concepto | Costo aprox. |
|---|---|
| Dominio (.com) | ~$10–12 al año |
| VPS (Hetzner/DigitalOcean) | ~$5–6 al mes |
| HTTPS (Caddy) | gratis |
| **Total** | **~$70–85 el primer año** |

Con la Opción B (Railway) el total mensual ronda **$5–13** más el dominio, a cambio de
no administrar el servidor.

**Recomendación final:** si quieres lo más barato y no te asusta seguir estos pasos,
un **VPS de DigitalOcean o Hetzner + Caddy**. Si prefieres simplicidad total,
**Railway**. En ambos casos, primero prueba todo en local con `docker compose up`.
