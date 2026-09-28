# Zona Aquática — Sistema de Gestión

Sistema para administrar la piscina Zona Aquática (Quillacollo, Cochabamba):
clientes, membresías, horarios, asistencia, pagos y personal.

Stack: **Flask + PostgreSQL + React (Vite) + Docker**. Contraseñas encriptadas con bcrypt.

---

## Requisitos

- [Docker](https://www.docker.com/) y Docker Compose.

## Puesta en marcha (local)

1. Abre una terminal en esta carpeta (`piscina`).
2. Revisa el archivo `.env` (puedes dejar los valores por defecto para pruebas).
3. Levanta todo:

   ```bash
   docker compose up --build
   ```

4. Abre el navegador:
   - **Aplicación:** http://localhost:5173
   - **API (opcional):** http://localhost:5000/api/health

5. (Opcional) Carga datos de ejemplo — membresías, horarios y clientes:

   ```bash
   docker compose exec backend python seed.py
   ```

### Primer ingreso

Al arrancar por primera vez se crea un administrador automáticamente con los
valores de `.env`:

- Usuario: `admin`
- Contraseña: `Admin123!`

> Cambia esa contraseña luego de entrar. Desde **Personal**, el admin crea al
> resto de usuarios (admins o recepcionistas). Cada usuario nuevo recibe una
> **contraseña temporal** que se muestra una sola vez y que deberá cambiar en su
> primer ingreso.

---

## Roles

- **Administrador:** acceso completo — personal, clientes, membresías, horarios,
  asistencia (con extensión por días justificados), gastos, ingresos/egresos con
  cierre de caja, registro de actividad, panel con gráficos y la página pública.
- **Recepcionista:** clientes (crear/editar con pagos), asistencia y horarios.

## Funciones principales

- **Clientes y pagos:** alta con fecha de inicio de membresía a elección,
  pagos en efectivo, QR o mixto, **pagos parciales (fianza)** con control de
  saldo pendiente, y **recibo en PDF** de cada pago.
- **Gastos (egresos):** cloro, cisterna, mantenimiento, etc.
- **Ingresos y egresos:** balance por período, gráficos y **cierre de caja diario**
  (efectivo vs QR).
- **Registro de actividad:** historial de todo lo que hace el personal.
- **Página pública:** landing en `/` con planes, horarios, galería y contacto,
  **todo editable desde el panel** (menú "Página pública"). En la web las
  membresías se muestran como "Planes".

## Respaldos (backups)

Con los contenedores levantados, genera un respaldo de la base:

- Windows: doble clic en `scripts\backup.bat`
- Linux/Mac/WSL: `./scripts/backup.sh`

El archivo `.sql` queda en la carpeta `backups/`. Puedes programarlo con el
Programador de tareas de Windows (o `cron`) para que corra automáticamente.

## Seguridad de contraseñas

Las contraseñas se guardan encriptadas con bcrypt (irreversible). Por eso el
sistema **no puede "mostrar" la contraseña de un usuario**: si alguien la olvida,
el admin usa **Restablecer**, que genera una nueva contraseña temporal.

---

## Estructura

```
piscina/
├── docker-compose.yml
├── database/init.sql
├── backend/            # API Flask
│   ├── app/
│   │   ├── models/     # usuario, cliente, membresia, horario, pago, asistencia, auditoria
│   │   ├── routes/     # auth, personal, clientes, membresias, horarios, asistencias, pagos, dashboard, auditoria
│   │   └── utils/      # decoradores, validadores, helpers
│   └── seed.py
└── frontend/           # React + Vite + Tailwind
    └── src/
        ├── pages/      # login, clientes, asistencia, horarios, admin/*
        ├── components/ # layout + UI reutilizable
        └── contexts/   # autenticación
```

## Ideas para más adelante

- Subida directa de fotos a la galería (hoy se cargan por URL).
- Recordatorios automáticos por WhatsApp de vencimientos.
- Reportes exportables (Excel/PDF) de ingresos y asistencia.

## Notas para producción (web)

- Cambia `SECRET_KEY`, `JWT_SECRET_KEY` y las contraseñas del `.env`.
- Sirve detrás de HTTPS (por ejemplo con un reverse proxy) y programa respaldos
  de la base de datos (`pg_dump`).
