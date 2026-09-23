#!/usr/bin/env bash
# =============================================================================
# Aló — Paso 3: mira en la terminal lo que decide el filtro, mensaje a mensaje.
# Muestra: notificación recibida, LEÍDO / DESCARTADO (con capa y motivo) / A REVISAR.
# =============================================================================
set -euo pipefail

AZUL=$'\033[1;34m'; VERDE=$'\033[1;32m'; FIN=$'\033[0m'
export PATH="$PATH:$HOME/.local/bin:${ANDROID_HOME:-$HOME/Android/Sdk}/platform-tools"

if ! command -v adb >/dev/null 2>&1; then
  echo "No encuentro adb. Ejecuta: bash scripts/1-instalar-entorno.sh" >&2
  exit 1
fi

PAQUETE="${1:-com.voicebot.alo.debug}"

PID="$(adb shell pidof -s "$PAQUETE" 2>/dev/null | tr -d '\r' || true)"

echo -e "${AZUL}==>${FIN} Log del filtro de Aló (Ctrl+C para salir)"
echo -e "    Etiquetas: Alo/Listener · Alo/Voice · Alo/TTS · Alo/Reply"
echo -e "    También puedes usar la pestaña 'En vivo' dentro de la app."
echo

if [ -n "$PID" ]; then
  # Con el proceso identificado: solo sus líneas + errores de la app.
  adb logcat -v time --pid="$PID" Alo/Listener:V Alo/Voice:V Alo/TTS:V Alo/Reply:V AndroidRuntime:E *:S
else
  echo -e "${VERDE}  !${FIN} La app no está corriendo: se muestran solo los logs de Aló."
  adb logcat -v time Alo/Listener:V Alo/Voice:V Alo/TTS:V Alo/Reply:V AndroidRuntime:E *:S
fi
