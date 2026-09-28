@echo off
REM Respaldo de la base de datos de Zona Aquatica (Windows).
REM Uso: doble clic o ejecutar en la carpeta del proyecto con los contenedores levantados.

setlocal
for /f "tokens=2 delims==" %%a in ('wmic OS Get localdatetime /value') do set dt=%%a
set FECHA=%dt:~0,8%_%dt:~8,6%

if not exist backups mkdir backups

docker compose exec -T db pg_dump -U piscina_admin piscina_db > backups\piscina_%FECHA%.sql
echo Backup creado en la carpeta backups (piscina_%FECHA%.sql)
endlocal
