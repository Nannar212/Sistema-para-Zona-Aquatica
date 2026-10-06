<#
    preparar-red.ps1 — Zona Aquática móvil

    Prepara la PC para que el celular, por WiFi, alcance el backend Flask
    que corre en Docker:

      1. Detecta la IP de tu adaptador WiFi.
      2. Crea (si falta) una regla del Firewall de Windows para el puerto 5000.
      3. Te dice qué URL escribir en la app (login → "Servidor").

    Uso (clic derecho → "Ejecutar con PowerShell" como Administrador), o:
      powershell -ExecutionPolicy Bypass -File scripts\preparar-red.ps1
#>

$ErrorActionPreference = 'Stop'
$PUERTO = 5000
$REGLA  = 'Zona Aquatica - Backend Flask 5000'

function Escribir($texto, $color = 'White') { Write-Host $texto -ForegroundColor $color }

Escribir "`n=== Zona Aquatica - preparar red para la app movil ===`n" Cyan

# ─── 1. IPs candidatas ──────────────────────────────────────────────────────
# Se descartan loopback, link-local y los adaptadores virtuales (VMware,
# VirtualBox, Radmin, WSL), que el celular no puede alcanzar.
$ips = Get-NetIPAddress -AddressFamily IPv4 |
    Where-Object {
        $_.IPAddress -notlike '127.*' -and
        $_.IPAddress -notlike '169.254.*' -and
        $_.InterfaceAlias -notmatch 'VMware|VirtualBox|Radmin|WSL|Loopback|Hyper-V'
    } |
    Sort-Object { if ($_.InterfaceAlias -match 'Wi-?Fi|Wireless|Inalambric') { 0 } else { 1 } }

if (-not $ips) {
    Escribir "No se encontro ninguna IP de red local. Conectate al WiFi y vuelve a correr esto." Red
    exit 1
}

Escribir "IPs de esta PC:" Yellow
foreach ($ip in $ips) {
    Escribir ("  {0,-16} {1}" -f $ip.IPAddress, $ip.InterfaceAlias)
}

$elegida = $ips[0].IPAddress
$url = "http://${elegida}:${PUERTO}/api/"

# ─── 2. Regla del Firewall ──────────────────────────────────────────────────
$esAdmin = ([Security.Principal.WindowsPrincipal] `
    [Security.Principal.WindowsIdentity]::GetCurrent()
).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

Escribir "`n--- Firewall ---" Yellow
$existente = Get-NetFirewallRule -DisplayName $REGLA -ErrorAction SilentlyContinue

if ($existente) {
    Escribir "La regla ya existe: '$REGLA'" Green
} elseif (-not $esAdmin) {
    Escribir "Falta la regla del Firewall y esta ventana NO es de Administrador." Red
    Escribir "Abre PowerShell como Administrador y corre:" Red
    Escribir "  New-NetFirewallRule -DisplayName '$REGLA' -Direction Inbound -Protocol TCP -LocalPort $PUERTO -Action Allow -Profile Private" Gray
} else {
    New-NetFirewallRule -DisplayName $REGLA `
        -Direction Inbound -Protocol TCP -LocalPort $PUERTO `
        -Action Allow -Profile Private | Out-Null
    Escribir "Regla creada para el puerto TCP $PUERTO (perfil de red Privada)." Green
}

# ─── 3. Backend arriba? ─────────────────────────────────────────────────────
Escribir "`n--- Backend ---" Yellow
try {
    $r = Invoke-WebRequest -Uri "http://localhost:$PUERTO/api/health" -TimeoutSec 4 -UseBasicParsing
    Escribir "Backend respondiendo: $($r.Content)" Green
} catch {
    Escribir "El backend no responde en localhost:$PUERTO." Red
    Escribir "Levantalo desde la carpeta 'piscina' con:  docker compose up -d" Gray
}

# ─── 4. Resultado ───────────────────────────────────────────────────────────
Escribir "`n=== Configura esto en la app ===" Cyan
Escribir "  Pantalla de login -> boton 'Servidor' -> escribe:" White
Escribir "     $elegida" Green
Escribir "  (la app completa sola el :$PUERTO/api)`n" Gray
Escribir "URL completa: $url" Gray
Escribir "Si el celular no conecta, revisa que la red WiFi de Windows este" Gray
Escribir "marcada como 'Privada' y que el celular este en la MISMA red.`n" Gray
