#!/usr/bin/env bash
# =============================================================================
# Aló — Paso 2: compila el APK, lo instala en el teléfono, concede el acceso a
# notificaciones por adb y abre la app. Todo desde la terminal.
# =============================================================================
set -euo pipefail

AZUL=$'\033[1;34m'; VERDE=$'\033[1;32m'; AMAR=$'\033[1;33m'; ROJO=$'\033[1;31m'; FIN=$'\033[0m'
paso()  { echo -e "${AZUL}==>${FIN} $*"; }
ok()    { echo -e "${VERDE}  ✔${FIN} $*"; }
aviso() { echo -e "${AMAR}  !${FIN} $*"; }
fallo() { echo -e "${ROJO}  ✘${FIN} $*" >&2; exit 1; }

PROYECTO="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROYECTO"

PAQUETE="com.voicebot.alo.debug"
SERVICIO="com.voicebot.alo.service.WaListenerService"
ACTIVIDAD="com.voicebot.alo.ui.MainActivity"
APK="app/build/outputs/apk/debug/app-debug.apk"

# ── 1. Gradle: wrapper o el del sistema ──────────────────────────────────────
if [ -x ./gradlew ]; then
  GRADLE="./gradlew"
elif command -v gradle >/dev/null 2>&1; then
  GRADLE="gradle"
  aviso "No hay ./gradlew; uso el Gradle del sistema. Ejecuta antes scripts/1-instalar-entorno.sh para crearlo."
else
  fallo "No encuentro Gradle. Ejecuta primero: bash scripts/1-instalar-entorno.sh"
fi

# ── 2. adb disponible ────────────────────────────────────────────────────────
export PATH="$PATH:$HOME/.local/bin:${ANDROID_HOME:-$HOME/Android/Sdk}/platform-tools"
command -v adb >/dev/null 2>&1 || fallo "No encuentro adb. Ejecuta primero: bash scripts/1-instalar-entorno.sh"

# ── 3. Compilar ──────────────────────────────────────────────────────────────
paso "Compilando el APK de debug (la primera vez tarda varios minutos)"
"$GRADLE" --no-daemon assembleDebug
[ -f "$APK" ] || fallo "No se generó el APK en $APK"
ok "APK listo: $APK"

# ── 4. Comprobar el teléfono ─────────────────────────────────────────────────
paso "Buscando el teléfono por USB"
adb start-server >/dev/null 2>&1 || true
DISPOSITIVOS="$(adb devices | sed -n '2,$p' | grep -v '^\s*$' || true)"
if [ -z "$DISPOSITIVOS" ]; then
  echo "$DISPOSITIVOS"
  fallo "No hay ningún teléfono conectado. Activa 'Depuración USB' y acepta el aviso en la pantalla del teléfono."
fi
echo "$DISPOSITIVOS" | sed 's/^/     /'
if echo "$DISPOSITIVOS" | grep -q "unauthorized"; then
  fallo "El teléfono aparece como 'unauthorized': desbloquéalo y acepta 'Permitir depuración USB'."
fi
if echo "$DISPOSITIVOS" | grep -q "no permissions"; then
  fallo "Sin permisos USB: ejecuta bash scripts/1-instalar-entorno.sh (reglas udev) y vuelve a entrar en la sesión."
fi
ok "Teléfono detectado"

# ── 5. Instalar ──────────────────────────────────────────────────────────────
paso "Instalando $PAQUETE"
adb install -r "$APK"

# ── 6. Conceder el acceso a notificaciones sin tocar la pantalla ─────────────
paso "Concediendo el acceso a notificaciones por adb"
if adb shell cmd notification allow_listener "$PAQUETE/$SERVICIO" >/dev/null 2>&1; then
  ok "Acceso a notificaciones concedido (android 9+)"
else
  aviso "Tu Android no aceptó el comando automático. Ábrelo a mano:"
  aviso "  Ajustes → Apps → Acceso especial → Acceso a notificaciones → Aló"
fi

# ── 7. Abrir la app ──────────────────────────────────────────────────────────
paso "Abriendo Aló"
adb shell am start -n "$PAQUETE/$ACTIVIDAD" >/dev/null 2>&1 || aviso "Ábrela a mano desde el cajón de apps"
ok "Listo"

echo
echo -e "${VERDE}==================== INSTALADO ====================${FIN}"
echo "  Ver qué decide el filtro en vivo:"
echo -e "    ${AZUL}bash scripts/3-ver-logs.sh${FIN}"
echo "  Las simulaciones por adb fueron retiradas; usa 'Probar voz' y 'Probar ruido' en En vivo:"
echo -e "    ${AZUL}bash scripts/4-simular-whatsapp.sh${FIN}"
echo "  Desinstalar:  adb uninstall $PAQUETE"
