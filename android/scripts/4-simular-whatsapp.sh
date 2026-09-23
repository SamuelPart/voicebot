#!/usr/bin/env bash
# =============================================================================
# Aló — Paso 4: simula notificaciones de WhatsApp por adb (para probar sin esperar
# mensajes reales). Publica notificaciones desde com.android.shell.
#
# REQUISITO: activa "Modo prueba (solo debug)" en la pestaña Ajustes de la app.
# Sin eso, el filtro descarta todo lo que no venga de WhatsApp (capa 0), y eso
# es justamente lo correcto.
# =============================================================================
set -euo pipefail

AZUL=$'\033[1;34m'; VERDE=$'\033[1;32m'; AMAR=$'\033[1;33m'; ROJO=$'\033[1;31m'; FIN=$'\033[0m'
export PATH="$PATH:$HOME/.local/bin:${ANDROID_HOME:-$HOME/Android/Sdk}/platform-tools"

aceptar() { echo -e "${VERDE}  ✔${FIN} $*"; }
esperar(){ echo -e "${AZUL}==>${FIN} $*"; sleep "${1:-2}"; }

command -v adb >/dev/null 2>&1 || { echo "No encuentro adb (bash scripts/1-instalar-entorno.sh)" >&2; exit 1; }

echo -e "${AMAR}Recuerda:${FIN} Ajustes → 'Modo prueba (solo debug)' debe estar ACTIVADO."
echo

# 1) Mensaje individual -> debe LEERSE en voz alta
esperar "1/6 Mensaje individual de 'Mamá' (debe LEERSE)"
adb shell cmd notification post -S messaging --user "Yo" \
  --conversation "Mamá" --message "Mamá:Hijo, ¿ya saliste de la oficina? Te espero con la cena." \
  alo_test_1 "Mamá" "Hijo, ¿ya saliste de la oficina?"
aceptar "enviado"

# 2) Mensaje de grupo -> debe LEERSE nombrando grupo y remitente
esperar "2/6 Mensaje de grupo (debe LEERSE)"
adb shell cmd notification post -S messaging --user "Yo" \
  --conversation "Equipo Ventas" --message "Luis:Confirmado, el cliente firma mañana." \
  alo_test_2 "Equipo Ventas" "Confirmado, el cliente firma mañana."
aceptar "enviado"

# 3) Copia de seguridad -> debe DESCARTARSE por patrón de texto (capa 3)
esperar "3/6 Aviso de copia de seguridad (debe DESCARTARSE)"
adb shell cmd notification post -t "WhatsApp" alo_test_3 "Copia de seguridad completada"
aceptar "enviado"

# 4) Resumen de mensajes nuevos -> debe DESCARTARSE por patrón (capa 3)
esperar "4/6 Resumen '3 mensajes nuevos' (debe DESCARTARSE)"
adb shell cmd notification post -t "WhatsApp" alo_test_4 "3 mensajes nuevos"
aceptar "enviado"

# 5) Código de verificación -> debe DESCARTARSE (capa 3)
esperar "5/6 Código de verificación (debe DESCARTARSE)"
adb shell cmd notification post -t "WhatsApp" alo_test_5 "Tu código de verificación es 384910"
aceptar "enviado"

# 6) Mensaje que 'suena a aviso' -> debe LEERSE (protección contra falsos negativos)
esperar "6/6 Mensaje real que menciona un respaldo (debe LEERSE)"
adb shell cmd notification post -S messaging --user "Yo" \
  --conversation "Luis" --message "Luis:¿Ya hiciste la copia de seguridad?" \
  alo_test_6 "Luis" "¿Ya hiciste la copia de seguridad?"
aceptar "enviado"

echo
echo -e "${VERDE}==================== FIN DE LA SIMULACIÓN ====================${FIN}"
echo "  Revisa la pestaña 'En vivo' o ejecuta: bash scripts/3-ver-logs.sh"
echo "  Esperado: 3 LEÍDOS (1, 2 y 6) y 3 DESCARTADOS (3, 4 y 5)."
echo
echo "  Limpiar las notificaciones de prueba:"
echo "    adb shell cmd notification list | grep alo_test"
