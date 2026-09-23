#!/usr/bin/env bash
# =============================================================================
# Aló — Paso 1: prepara TODO el entorno Android en una terminal Linux
# Instala: JDK 17, herramientas de línea de comandos del SDK, platform-tools,
# SDK 35, licencias aceptadas, Gradle 8.11.1 y el wrapper del proyecto.
# No necesita Android Studio. Probado en Debian/Ubuntu, Fedora y Arch.
# =============================================================================
set -euo pipefail

AZUL=$'\033[1;34m'; VERDE=$'\033[1;32m'; AMAR=$'\033[1;33m'; ROJO=$'\033[1;31m'; FIN=$'\033[0m'
paso()  { echo -e "${AZUL}==>${FIN} $*"; }
ok()    { echo -e "${VERDE}  ✔${FIN} $*"; }
aviso() { echo -e "${AMAR}  !${FIN} $*"; }
fallo() { echo -e "${ROJO}  ✘${FIN} $*" >&2; }

ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export ANDROID_HOME
GRADLE_VERSION="8.11.1"
# Si Google publica una versión nueva, el script la detecta sola; esto es el respaldo.
CMDLINE_TOOLS_RESPALDO="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"

# ── 0. Distribución ───────────────────────────────────────────────────────────
if [ -r /etc/os-release ]; then
  . /etc/os-release
  DISTRO="${ID:-desconocida}"
  DISTRO_LIKE="${ID_LIKE:-$DISTRO}"
else
  DISTRO="desconocida"; DISTRO_LIKE="desconocida"
fi
paso "Distribución detectada: $DISTRO ($DISTRO_LIKE)"

instalar_paquetes() {
  case "$DISTRO_LIKE" in
    *debian*|*ubuntu*|"linuxmint"|"pop")
      sudo apt-get update -y
      sudo apt-get install -y openjdk-17-jdk-headless unzip curl wget adb android-sdk-platform-tools-common
      ;;
    *fedora*|*rhel*|*centos*)
      sudo dnf install -y java-17-openjdk-devel unzip curl wget android-tools
      ;;
    *arch*|"manjaro")
      sudo pacman -Sy --noconfirm jdk17-openjdk unzip curl wget android-tools
      ;;
    *suse*)
      sudo zypper --non-interactive install java-17-openjdk-devel unzip curl wget android-tools
      ;;
    *)
      aviso "No reconozco tu distribución. Instala a mano: java 17, unzip, curl, adb."
      ;;
  esac
}

# ── 1. Java 17 ────────────────────────────────────────────────────────────────
paso "Comprobando Java 17+"
JAVA_OK=false
if command -v java >/dev/null 2>&1; then
  VERSION_JAVA="$(java -version 2>&1 | head -1 | grep -oE '[0-9]+' | head -1)"
  if [ "${VERSION_JAVA:-0}" -ge 17 ] 2>/dev/null; then
    JAVA_OK=true
    ok "Java $VERSION_JAVA ya instalado ($(command -v java))"
  else
    aviso "Java $VERSION_JAVA es antiguo; se instalará el 17"
  fi
fi
if [ "$JAVA_OK" = false ]; then
  instalar_paquetes
  ok "Paquetes base instalados"
fi

if ! command -v unzip >/dev/null 2>&1 || ! command -v curl >/dev/null 2>&1; then
  instalar_paquetes
fi

# ── 2. Herramientas de línea de comandos del SDK ──────────────────────────────
paso "Preparando el SDK de Android en $ANDROID_HOME"
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"

if [ -x "$SDKMANAGER" ]; then
  ok "sdkmanager ya presente"
else
  mkdir -p "$ANDROID_HOME/cmdline-tools"
  CMDLINE_TOOLS_URL="$(curl -fsSL https://developer.android.com/studio 2>/dev/null \
    | grep -oE 'https://dl\.google\.com/android/repository/commandlinetools-linux-[0-9]+_latest\.zip' \
    | head -1 || true)"
  if [ -z "${CMDLINE_TOOLS_URL:-}" ]; then
    CMDLINE_TOOLS_URL="$CMDLINE_TOOLS_RESPALDO"
    aviso "No pude leer la versión actual en la web de Google; uso el respaldo"
  fi
  ok "Descargando: $(basename "$CMDLINE_TOOLS_URL")"
  TMP_ZIP="$(mktemp -d)/cmdline-tools.zip"
  wget -q --show-progress -O "$TMP_ZIP" "$CMDLINE_TOOLS_URL" || curl -fL -o "$TMP_ZIP" "$CMDLINE_TOOLS_URL"

  # El zip trae una carpeta "cmdline-tools"; el SDK exige que se llame "latest".
  rm -rf "$ANDROID_HOME/cmdline-tools/latest" "$ANDROID_HOME/cmdline-tools/tmp"
  unzip -q "$TMP_ZIP" -d "$ANDROID_HOME/cmdline-tools/tmp"
  mv "$ANDROID_HOME/cmdline-tools/tmp/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
  rm -rf "$ANDROID_HOME/cmdline-tools/tmp" "$TMP_ZIP"
  ok "sdkmanager instalado"
fi

# ── 3. Paquetes del SDK ───────────────────────────────────────────────────────
paso "Instalando platform-tools (adb), Android 35 y build-tools 35.0.0"
# `yes` recibe SIGPIPE cuando sdkmanager termina: se ignora ese código a propósito.
( set +o pipefail; yes | "$SDKMANAGER" --sdk_root="$ANDROID_HOME" --licenses >/dev/null 2>&1 ) || true
"$SDKMANAGER" --sdk_root="$ANDROID_HOME" "platform-tools" "platforms;android-35" "build-tools;35.0.0" >/dev/null
ok "SDK listo"

mkdir -p "$HOME/.local/bin"
ln -sf "$ANDROID_HOME/platform-tools/adb" "$HOME/.local/bin/adb"
ok "adb enlazado en ~/.local/bin/adb"

# ── 4. Gradle + wrapper del proyecto ──────────────────────────────────────────
PROYECTO="$(cd "$(dirname "$0")/.." && pwd)"
paso "Generando el wrapper de Gradle $GRADLE_VERSION en $PROYECTO"
if [ -x "$PROYECTO/gradlew" ]; then
  ok "El wrapper ya existe"
elif command -v gradle >/dev/null 2>&1 && gradle --version 2>/dev/null | grep -qE "Gradle 8\.(9|1[0-9])"; then
  ( cd "$PROYECTO" && gradle wrapper --gradle-version "$GRADLE_VERSION" -q )
  ok "Wrapper generado con el Gradle del sistema"
else
  GRADLE_DIR="$HOME/.local/opt/gradle-$GRADLE_VERSION"
  if [ ! -x "$GRADLE_DIR/bin/gradle" ]; then
    aviso "Descargando Gradle $GRADLE_VERSION (~130 MB, una sola vez)"
    mkdir -p "$HOME/.local/opt"
    TMP_GZ="$(mktemp -d)/gradle.zip"
    wget -q --show-progress -O "$TMP_GZ" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" \
      || curl -fL -o "$TMP_GZ" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
    unzip -q "$TMP_GZ" -d "$HOME/.local/opt"
    rm -f "$TMP_GZ"
  fi
  ln -sf "$GRADLE_DIR/bin/gradle" "$HOME/.local/bin/gradle"
  ( cd "$PROYECTO" && "$GRADLE_DIR/bin/gradle" wrapper --gradle-version "$GRADLE_VERSION" -q )
  ok "Wrapper generado con Gradle descargado"
fi

# ── 5. Permisos USB para adb (udev) ───────────────────────────────────────────
if [ -w /etc/udev/rules.d ] 2>/dev/null || command -v sudo >/dev/null 2>&1; then
  paso "Configurando permisos USB para adb (si ya usas adb, esto no molesta)"
  REGLAS=/etc/udev/rules.d/51-android.rules
  if [ -f "$REGLAS" ] && grep -q "22b8" "$REGLAS" 2>/dev/null; then
    ok "Reglas udev ya presentes"
  else
    sudo tee "$REGLAS" >/dev/null <<'EOF'
# Aló / Android SDK — acceso USB sin root para adb
SUBSYSTEM=="usb", ATTR{idVendor}=="0bb4", MODE="0666", GROUP="plugdev"
SUBSYSTEM=="usb", ATTR{idVendor}=="0fce", MODE="0666", GROUP="plugdev"
SUBSYSTEM=="usb", ATTR{idVendor}=="12d1", MODE="0666", GROUP="plugdev"
SUBSYSTEM=="usb", ATTR{idVendor}=="18d1", MODE="0666", GROUP="plugdev"
SUBSYSTEM=="usb", ATTR{idVendor}=="04e8", MODE="0666", GROUP="plugdev"
SUBSYSTEM=="usb", ATTR{idVendor}=="22b8", MODE="0666", GROUP="plugdev"
SUBSYSTEM=="usb", ATTR{idVendor}=="2717", MODE="0666", GROUP="plugdev"
SUBSYSTEM=="usb", ATTR{idVendor}=="2a70", MODE="0666", GROUP="plugdev"
SUBSYSTEM=="usb", ATTR{idVendor}=="05c6", MODE="0666", GROUP="plugdev"
SUBSYSTEM=="usb", ATTR{idVendor}=="0489", MODE="0666", GROUP="plugdev"
EOF
    sudo udevadm control --reload-rules && sudo udevadm trigger
    sudo usermod -aG plugdev "$USER" 2>/dev/null || true
    ok "Reglas udev instaladas (si adb dice 'no permissions', cierra sesión y vuelve a entrar)"
  fi
fi

# ── 6. Variables de entorno ───────────────────────────────────────────────────
paso "Añadiendo ANDROID_HOME y PATH a ~/.bashrc (si no estaban)"
if ! grep -q "ANDROID_HOME" "$HOME/.bashrc" 2>/dev/null; then
  {
    echo ""
    echo "# Aló / Android SDK"
    echo "export ANDROID_HOME=\"$ANDROID_HOME\""
    echo "export PATH=\"\$PATH:\$ANDROID_HOME/platform-tools:\$ANDROID_HOME/cmdline-tools/latest/bin:\$HOME/.local/bin\""
  } >> "$HOME/.bashrc"
  ok "Añadidas a ~/.bashrc"
else
  ok "Ya estaban configuradas"
fi

echo
echo -e "${VERDE}==================== ENTORNO LISTO ====================${FIN}"
echo "  SDK:      $ANDROID_HOME"
echo "  Gradle:   $PROYECTO/gradlew"
echo "  adb:      $(command -v adb || echo "$HOME/.local/bin/adb")"
echo
echo "Siguiente paso (en una terminal NUEVA para cargar el PATH):"
echo -e "  ${AZUL}cd $(dirname "$PROYECTO")${FIN}"
echo -e "  ${AZUL}bash scripts/2-compilar-e-instalar.sh${FIN}"
