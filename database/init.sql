-- Inicialización de la base de datos — Sistema Zona Aquática
-- Las tablas las crea SQLAlchemy (db.create_all) al arrancar el backend.
-- Este archivo solo prepara extensiones útiles de PostgreSQL.

CREATE EXTENSION IF NOT EXISTS "unaccent";

-- Zona horaria de Bolivia
SET timezone = 'America/La_Paz';
