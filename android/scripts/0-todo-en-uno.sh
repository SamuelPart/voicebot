#!/usr/bin/env bash
# =============================================================================
# Aló — Atajo: entorno + compilar + instalar + abrir, en un solo comando.
#   bash scripts/0-todo-en-uno.sh
# Si algo falla, ejecuta los pasos por separado para ver el detalle:
#   1-instalar-entorno.sh · 2-compilar-e-instalar.sh · 3-ver-logs.sh · 4-simular-whatsapp.sh
# =============================================================================
set -euo pipefail

AZUL=$'\033[1;34m'; FIN=$'\033[0m'
DIR="$(cd "$(dirname "$0")" && pwd)"

echo -e "${AZUL}==>${FIN} Paso 1/2: entorno"
bash "$DIR/1-instalar-entorno.sh"

echo
echo -e "${AZUL}==>${FIN} Paso 2/2: compilar, instalar y abrir"
# shellcheck disable=SC1090
[ -f "$HOME/.bashrc" ] && source "$HOME/.bashrc" >/dev/null 2>&1 || true
export PATH="$PATH:$HOME/.local/bin:${ANDROID_HOME:-$HOME/Android/Sdk}/platform-tools"
bash "$DIR/2-compilar-e-instalar.sh"

echo
echo "Siguiente:  activa 'Modo prueba' en Ajustes y ejecuta"
echo "  bash scripts/4-simular-whatsapp.sh"
echo "  bash scripts/3-ver-logs.sh"
