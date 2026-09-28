#!/usr/bin/env bash
# Respaldo de la base de datos de Zona Aquática (Linux/Mac/WSL).
# Uso: ./scripts/backup.sh   (con los contenedores levantados)
set -e

FECHA=$(date +%Y%m%d_%H%M%S)
DIR="$(cd "$(dirname "$0")/.." && pwd)/backups"
mkdir -p "$DIR"

DB_USER="${POSTGRES_USER:-piscina_admin}"
DB_NAME="${POSTGRES_DB:-piscina_db}"

docker compose exec -T db pg_dump -U "$DB_USER" "$DB_NAME" > "$DIR/piscina_$FECHA.sql"
echo "Backup creado: $DIR/piscina_$FECHA.sql"
