# Guía para subir Zona Aquática a internet

Cómo pasar del sistema local a una **página web real**, con dominio, HTTPS y los avisos
de vencimiento por correo funcionando, gastando lo mínimo durante una prueba de 2 meses.

> Precios revisados el 6 de noviembre de 2026, casi todos en fuentes de terceros que no
> siempre coinciden. Confirma el precio final en la página del proveedor antes de pagar.

---

## Qué necesita el sistema

Esto descarta varias opciones baratas:

| Necesidad | Por qué importa |
|---|---|
| PostgreSQL que no se borre | Clientes, pagos y asistencias |
| Disco persistente | Fotos y modelo 3D de la página pública |
| Un proceso siempre encendido | El `notificador` que envía los avisos por correo |
| Salida de correo | Muchos hostings bloquean los puertos SMTP |
| HTTPS | Inicio de sesión seguro, web y app móvil |

---

## Opciones y costo real por 2 meses

| Opción | Costo por mes | 2 meses | ¿Cumple? | Comentario |
|---|---|---|---|---|
| **DigitalOcean, Droplet 1 GB** (recomendada) | ~$6 | **~$12** | Sí, con Brevo | Región Nueva York, la más cercana a Bolivia. Se cobra por uso: al destruirlo deja de cobrar. Bloquea SMTP, por eso se usa la API de Brevo. |
| **Hetzner CX23** (2 vCPU, 4 GB) | ~€5.49 | ~€11 | Sí, si puedes crearlo | Más memoria por menos dinero, y el puerto 587 funciona. Pero solo está en Europa (más lento desde Bolivia) y en septiembre y octubre se reportó el botón de crear desactivado. |
| **Hostinger KVM 1** (4 GB) | $6.49 de entrada, $11.99 al renovar | Depende del plazo | Sí | El precio bajo suele exigir pagar muchos meses por adelantado. No conviene para una prueba corta. |
| **Railway Hobby** | $5 mínimo + uso | ~$12–25 | Con trabajo extra | No administras servidor, pero son 4 servicios, cada uno cobra por memoria, y bloquea SMTP. Los archivos actuales no se despliegan tal cual: hay que adaptar Caddy y los volúmenes. |
| **Oracle Cloud Always Free** | $0 | $0 | Riesgoso | Gratis, pero suele faltar capacidad, en 2026 redujo el límite y hubo instancias desactivadas. No pondría ahí cobros reales sin respaldos diarios fuera del servidor. |
| **Render gratis** | $0 | No aplica | **No** | La base de datos gratis se borra a los 30 días (más 14 de gracia). No llega a los 2 meses. |

Súmale el **dominio**: ~$10–12 al año (.com, en Cloudflare, Porkbun o Namecheap).

**Recomendación: DigitalOcean 1 GB + Brevo gratis + dominio .com.** Son unos **$23 por los
2 meses, dominio incluido**, sin compromiso de permanencia. Si al terminar no sigues,
destruyes el servidor y dejas de pagar. Revisa al registrarte si te ofrecen crédito de
prueba: las fuentes no coinciden y puede ir de $5 a $200.

No elijas el plan de 512 MB: Postgres, el backend y la compilación no entran cómodos.

**Sin dominio:** puedes probar gratis con un subdominio de [DuckDNS](https://www.duckdns.org)
(`DOMAIN=zonaaquatica.duckdns.org`). Baja el costo a ~$12, pero sin dominio propio no
puedes autenticar el correo en Brevo y es más probable que los avisos caigan en spam.

---

## Qué NO debes subir

**No hace falta borrarlos ni esconderlos.** Quédate con ellos en tu PC: `.gitignore` hace
que git los ignore, así que nunca salen con `git push`. Lo único distinto es que el
servidor tendrá su propio `.env` con contraseñas nuevas (paso 5). Revisa esta lista si
alguna vez copias archivos a mano, porque ahí `.gitignore` no te protege:

| Archivo o carpeta | Motivo |
|---|---|
| `.env` y `backend/.env` | **Contraseñas y claves.** En el servidor se crea uno nuevo |
| `backups/` y cualquier `.sql` de respaldo | Datos personales de clientes |
| `backend/uploads/*` | Fotos: se copian aparte (paso 7), no van en git |
| `*.jks`, `*.keystore` | Firma de la app móvil. Si se filtra, cualquiera puede firmar como tú |
| `mobile/local.properties` | Rutas de tu PC |
| `node_modules/`, `dist/`, `__pycache__/`, `.venv/`, `mobile/app/build/`, `.gradle/` | Se regeneran solos |
| `.memsearch/` y la carpeta `C:` de la raíz | Índices locales. La carpeta `C:` se creó por error y se puede borrar |

**Sí deben subirse**, aunque parezcan delicados:

- `frontend/.env`: solo contiene `VITE_API_URL=/api` y la compilación lo necesita.
- `.env.prod.ejemplo`: es una plantilla sin valores reales.
- `frontend/package-lock.json`: fija las versiones de la compilación.

Revisé el historial: ningún `.env` real ni contraseña se subió a git. Aun así, deja el
repositorio de GitHub en **privado**.

---

## Paso a paso (DigitalOcean)

### 1. Prepara el código

Haz commit de tus cambios y súbelos a GitHub (`git push`). El servidor descarga lo que
esté en GitHub, no lo que tengas en tu PC.

### 2. Compra el dominio y crea el servidor

1. Compra el dominio.
2. En DigitalOcean crea un Droplet: **Ubuntu 24.04, Basic, 1 GB, región New York**, con
   acceso por clave SSH.
3. En tu dominio crea un registro DNS tipo **A**: `@` → IP del servidor.

Basta el dominio sin `www`: el `Caddyfile` solo pide certificado para ese nombre.

### 3. Prepara el servidor

Conéctate con `ssh root@IP_DEL_SERVIDOR` y ejecuta:

```bash
# Memoria de intercambio: compilar el front con 1 GB se queda corto sin esto
fallocate -l 2G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab

# Firewall: solo SSH y web
ufw allow OpenSSH && ufw allow 80 && ufw allow 443 && ufw --force enable

# Docker
curl -fsSL https://get.docker.com | sh
```

### 4. Configura el correo en Brevo (gratis, 300 correos al día)

1. Crea una cuenta en [brevo.com](https://www.brevo.com).
2. En **Senders, Domains & Dedicated IPs** agrega tu dominio y copia en tu DNS los
   registros que te indique. Esto evita que los avisos lleguen a spam.
3. En **SMTP & API → API Keys** crea una clave.

### 5. Descarga el proyecto y crea el `.env`

```bash
git clone https://github.com/Nannar212/Sistema-para-Zona-Aquatica.git piscina
cd piscina
cp .env.prod.ejemplo .env
nano .env
```

Llena estos valores. Para generar cada clave aleatoria usa `openssl rand -hex 32`:

```bash
DOMAIN=tudominio.com
POSTGRES_PASSWORD=...
SECRET_KEY=...
JWT_SECRET_KEY=...
SEED_ADMIN_PASSWORD=...
CORREO_REMITENTE=avisos@tudominio.com
BREVO_API_KEY=...
```

Si tu repositorio es privado, `git clone` te pedirá un token de GitHub.

### 6. Arranca el sistema

```bash
docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml ps
```

Deben aparecer `db`, `backend`, `notificador` y `web`. La primera compilación puede
tardar varios minutos. Después abre:

- Web pública: `https://tudominio.com`
- Sistema del personal: `https://tudominio.com/login`

Caddy obtiene el certificado HTTPS solo; el DNS del paso 2 ya debe apuntar al servidor.

### 7. Pasa tus datos locales (opcional)

Haz esto **antes** del paso 6 si quieres conservar lo que tienes en tu PC. En tu PC, desde
Git Bash (en PowerShell el `>` puede dañar la codificación del archivo):

```bash
docker compose exec -T db pg_dump -U piscina_admin piscina_db > respaldo.sql
scp respaldo.sql root@IP_DEL_SERVIDOR:/root/
```

En el servidor, con `POSTGRES_USER=piscina_admin` en el `.env`:

```bash
docker compose -f docker-compose.prod.yml up -d db
docker compose -f docker-compose.prod.yml exec -T db psql -U piscina_admin -d piscina_db < /root/respaldo.sql
```

Luego sigue con el paso 6. El usuario administrador será el de tu base local, no el del
`.env`. Las fotos se copian aparte una vez que el sistema esté arriba:

```bash
# En tu PC
scp -r backend/uploads root@IP_DEL_SERVIDOR:/root/uploads
# En el servidor
docker cp /root/uploads/. piscina_backend_prod:/app/uploads/
```

### 8. Prueba los avisos por correo

1. Crea un cliente con **tu propio correo** de referencia y una membresía que venza en
   3 días o menos.
2. Fuerza una pasada sin esperar a la hora programada:

   ```bash
   docker compose -f docker-compose.prod.yml exec notificador python notificador.py --una-vez
   docker compose -f docker-compose.prod.yml logs --tail 20 notificador
   ```

3. Debe decir `'enviados': 1` y debe llegarte el correo.

| Si el registro dice | Qué revisar |
|---|---|
| `el correo no está configurado` | Faltan `CORREO_REMITENTE` o `BREVO_API_KEY` en el `.env` |
| `HTTP Error 401` | Clave incorrecta. O activa la IP del servidor en **Security → Authorized IPs** de Brevo |
| `HTTP Error 400` | El remitente no está validado en Brevo |
| `timed out` usando SMTP | Tu hosting bloquea ese puerto: usa `BREVO_API_KEY` |
| `'sin_correo': 1` | El cliente no tiene correo de referencia |

Tras cambiar el `.env`, aplica con `docker compose -f docker-compose.prod.yml up -d`.

### 9. Conecta la app móvil

En el menú lateral de la app, en **Servidor**, escribe `https://tudominio.com`.

### 10. Programa los respaldos

Sin esto, un fallo del servidor te hace perder los pagos y asistencias. Ejecuta
`crontab -e` y agrega (ajusta la ruta):

```bash
0 2 * * * cd /root/piscina && mkdir -p backups && docker compose -f docker-compose.prod.yml exec -T db pg_dump -U piscina_admin piscina_db > backups/piscina_$(date +\%Y\%m\%d).sql
```

Cópialos a tu PC cada semana. Un respaldo que solo vive en el servidor se pierde con él:

```bash
scp -r root@IP_DEL_SERVIDOR:/root/piscina/backups ./backups
```

---

## Cómo funcionan los avisos

- El servicio `notificador` revisa cada hora, desde las 9:00 de Bolivia (`AVISOS_HORA`).
- Avisa una sola vez por fecha de vencimiento, cuando faltan 3 días o menos.
- Si un envío falla, lo reintenta en la siguiente hora.
- Si una justificación corre la fecha de fin, avisará de nuevo cuando vuelva a faltar poco.
- Cada envío queda en el **Registro de actividad**, a nombre de "Sistema".

**Otra vía: Gmail.** En un hosting que deje abierto el puerto 587, como Hetzner, puedes
usar SMTP con una [contraseña de aplicación](https://myaccount.google.com/apppasswords)
y no necesitas Brevo:

```bash
CORREO_REMITENTE=tucorreo@gmail.com
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USUARIO=tucorreo@gmail.com
SMTP_PASSWORD=contraseña_de_aplicacion
```

---

## Actualizar el sistema

Después de subir cambios a GitHub:

```bash
cd /root/piscina && git pull
docker compose -f docker-compose.prod.yml up -d --build
```

Las columnas nuevas de la base se agregan solas al arrancar.

---

## A los 2 meses: seguir o cerrar

**Si sigue:** no hay nada que migrar, solo sigues pagando. Si crece, sube el Droplet a
2 GB desde el panel. Considera los respaldos automáticos de DigitalOcean (cobran un
porcentaje del Droplet).

**Si se cierra:**

1. Descarga un último respaldo de la base y de las fotos.
2. **Destruye** el Droplet. Solo apagarlo no detiene el cobro.
3. Desactiva la renovación automática del dominio.
