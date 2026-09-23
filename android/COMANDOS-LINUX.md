# Aló — Todo desde la terminal de Linux (sin Android Studio)

Guía completa en español para preparar el entorno, compilar, instalar en el teléfono y probar el
lector de WhatsApp **usando solo la terminal**. Todos los comandos se copian y pegan tal cual.

> **¿Vas a usar Android Studio?** Tienes la guía paralela en
> [`GUIA-ANDROID-STUDIO-LINUX.md`](GUIA-ANDROID-STUDIO-LINUX.md) (instalación, panel Logcat,
> emulador con KVM, errores típicos). Los scripts de `android/scripts/` sirven en las dos rutas.

> Si tu terminal está en español, los mensajes del sistema también lo estarán. Al final hay una
> sección con las traducciones de los errores más comunes y qué hacer con cada uno.

---

## 0. Dos caminos (elige uno)

| Camino | Comando | Cuándo usarlo |
|---|---|---|
| **Automático (recomendado)** | `bash android/scripts/0-todo-en-uno.sh` | Instala todo, compila, instala en el teléfono y abre la app |
| **Paso a paso** | los 4 scripts siguientes | Si algo falla, así ves exactamente dónde |

Los cuatro scripts (están en `android/scripts/`):

```bash
bash scripts/1-instalar-entorno.sh      # JDK 17 + SDK Android + adb + Gradle 8.11.1 + licencias
bash scripts/2-compilar-e-instalar.sh   # APK + instalar + conceder permiso + abrir la app
bash scripts/3-ver-logs.sh              # ver en la terminal qué decide el filtro
bash scripts/4-simular-whatsapp.sh      # enviar notificaciones de prueba por adb
```

Antes de ejecutarlos, desde la raíz del repositorio:

```bash
cd voicebot/android
chmod +x scripts/*.sh       # por si el clonado no trajo el permiso de ejecución
```

---

## 1. Qué instala el script (y cómo hacerlo a mano)

### Debian / Ubuntu / Linux Mint / Pop!_OS

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk-headless unzip curl wget adb android-sdk-platform-tools-common
```

El paquete `android-sdk-platform-tools-common` ya trae las **reglas udev** para que `adb` vea tu
teléfono sin permisos de root: es el problema número uno en Linux.

### Fedora / RHEL / CentOS

```bash
sudo dnf install -y java-17-openjdk-devel unzip curl wget android-tools
```

### Arch / Manjaro

```bash
sudo pacman -Sy --noconfirm jdk17-openjdk unzip curl wget android-tools
```

### openSUSE

```bash
sudo zypper install java-17-openjdk-devel unzip curl wget android-tools
```

### Comprobar que quedó bien

```bash
java -version            # debe decir 17 o superior
adb version              # Android Debug Bridge version 1.0.41 (o superior)
```

### SDK de Android a mano (si prefieres no usar el script)

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
mkdir -p "$ANDROID_HOME/cmdline-tools"

# Descarga de las herramientas de línea de comandos
cd /tmp
curl -fLO https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q commandlinetools-linux-11076708_latest.zip -d "$ANDROID_HOME/cmdline-tools"
mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"   # ← obligatorio

# Paquetes y licencias
yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "platform-tools" "platforms;android-35" "build-tools;35.0.0"

# Variables permanentes
echo 'export ANDROID_HOME="$HOME/Android/Sdk"' >> ~/.bashrc
echo 'export PATH="$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin"' >> ~/.bashrc
source ~/.bashrc
```

> La carpeta **debe** llamarse `latest`. Si se queda como `cmdline-tools/cmdline-tools`, el SDK
> falla al compilar con el error `Failed to find package` o similar.

### Gradle (el proyecto no incluye el wrapper)

El repositorio no trae `gradlew` ni `gradle-wrapper.jar` (son binarios). Dos opciones:

```bash
# Opción 1: con Gradle del sistema
sudo snap install gradle --classic     # o: brew install gradle, sdk install gradle
cd voicebot/android && gradle wrapper --gradle-version 8.11.1

# Opción 2: sin instalar nada (descarga Gradle en ~/.local, ~130 MB)
curl -fLo /tmp/gradle.zip https://services.gradle.org/distributions/gradle-8.11.1-bin.zip
unzip -q /tmp/gradle.zip -d ~/.local/opt
cd voicebot/android && ~/.local/opt/gradle-8.11.1/bin/gradle wrapper --gradle-version 8.11.1
```

⚠️ **Gradle 8.9 – 8.13. Nunca 9.x**: el plugin de Android (AGP 8.7.3) no lo soporta.

---

## 2. Compilar y ejecutar

```bash
cd voicebot/android

./gradlew testDebugUnitTest      # 1) tests del filtro y de la voz (JVM, sin teléfono)
./gradlew assembleDebug          # 2) genera el APK
./gradlew installDebug           # 3) lo instala en el teléfono conectado por USB
./gradlew lintDebug              # opcional: revisión de calidad
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`. El paquete instalado es
**`com.voicebot.alo.debug`** (el sufijo `.debug` viene de la configuración del proyecto).

### Ver los mensajes del filtro en la terminal

```bash
# Todo lo que decide Aló (LEÍDO / DESCARTADO con capa y motivo / A REVISAR)
adb logcat -v time Alo/Listener:V Alo/Voice:V Alo/TTS:V Alo/Reply:V AndroidRuntime:E *:S

# Solo los errores de la app
adb logcat AndroidRuntime:E *:S
```

Ejemplo de lo que verás:

```
09-22 21:14:02.113 D/Alo/Listener: notificación de com.whatsapp · canal=messages_1 · mensajes=2
09-22 21:14:02.118 D/Alo/Listener: LEÍDO (1 mensaje/s, speak=true)
09-22 21:14:31.902 D/Alo/Listener: notificación de com.whatsapp · canal=backup · mensajes=0
09-22 21:14:31.903 D/Alo/Listener: DESCARTADO (capa 1: notificación en curso (isOngoing): progreso/backup)
```

---

## 3. Conceder el acceso a notificaciones sin tocar la pantalla

Desde Android 9 se puede hacer por adb (es el mismo permiso que se activa en Ajustes):

```bash
adb shell cmd notification allow_listener com.voicebot.alo.debug/com.voicebot.alo.service.WaListenerService
```

Para quitarlo:

```bash
adb shell cmd notification disallow_listener com.voicebot.alo.debug/com.voicebot.alo.service.WaListenerService
```

Comprobar que quedó activo:

```bash
adb shell settings get secure enabled_notification_listeners
```

Si el comando no funciona en tu modelo, actívalo a mano en:
**Ajustes → Apps → Acceso especial → Acceso a notificaciones → Aló** (en algunos fabricantes:
*Ajustes → Notificaciones → Acceso a notificaciones*).

---

## 4. Probar sin WhatsApp: "Modo prueba" por adb

Puedes generar notificaciones reales desde la terminal y ver cómo reacciona el filtro, sin
depender de que alguien te escriba.

**Frontal, en la app:** pestaña **Ajustes → "Modo prueba (solo debug)"** → actívalo.
(Solo aparece en builds de debug, es decir, en la que instalas con `installDebug`.)

Después, en la terminal:

```bash
# Mensaje individual -> debe LEERSE
adb shell cmd notification post -S messaging --user "Yo" \
  --conversation "Mamá" --message "Mamá:Hijo, ¿ya saliste de la oficina?" alo1 "Mamá" "hola"

# Mensaje de grupo -> debe LEERSE nombrando grupo y remitente
adb shell cmd notification post -S messaging --user "Yo" \
  --conversation "Equipo Ventas" --message "Luis:El cliente firma mañana" alo2 "Equipo Ventas" "texto"

# Copia de seguridad -> debe DESCARTARSE (patrón de texto, capa 3)
adb shell cmd notification post -t "WhatsApp" alo3 "Copia de seguridad completada"

# Resumen -> debe DESCARTARSE
adb shell cmd notification post -t "WhatsApp" alo4 "3 mensajes nuevos"

# Código de verificación -> debe DESCARTARSE
adb shell cmd notification post -t "WhatsApp" alo5 "Tu código de verificación es 384910"

# Mensaje real que menciona un respaldo -> debe LEERSE (test anti-falso-negativo)
adb shell cmd notification post -S messaging --user "Yo" \
  --conversation "Luis" --message "Luis:¿Ya hiciste la copia de seguridad?" alo6 "Luis" "texto"
```

O simplemente: `bash scripts/4-simular-whatsapp.sh` (envía los seis seguidos y explica el resultado esperado).

**Detalles del comando** (por si quieres inventar tus propias pruebas):

| Opción | Para qué sirve |
|---|---|
| `-S messaging` | Crea un `MessagingStyle` real: es lo que hace que el filtro lo reconozca como mensaje |
| `--message "Remitente:texto"` | Añade el mensaje; lo anterior a los dos puntos es el remitente |
| `--user "Yo"` | Nombre del dueño del teléfono (así el filtro no leerá tus propios mensajes) |
| `--conversation "Nombre"` | Título de la conversación: el chat o el grupo |
| `-t "Título"` y el último argumento | Título y texto de las notificaciones simples (para simular avisos) |
| `-S bigtext` | Notificación larga sin estructura (también debe ir al buzón *Revisar*) |

Limpiar las notificaciones de prueba:

```bash
adb shell cmd notification list | grep alo
adb shell cmd notification post -t x limpiar "" ; adb shell service call notification 1 >/dev/null 2>&1 || true
```
(Basta con deslizarlas en el teléfono, o reiniciar el teléfono.)

---

## 5. Probar con WhatsApp de verdad

1. **Concede el acceso** (sección 3) — sin esto, la app no ve nada y es lo primero que debes verificar.
2. **Pide a alguien que te escriba** (o usa un segundo número). Evita el chat "Tú" (contigo mismo):
   esos mensajes los envías tú y el filtro los descarta a propósito.
3. **Quita la optimización de batería**: en Xiaomi, Samsung, Huawei, Oppo, Realme y Vivo, el sistema
   mata los servicios en segundo plano. Ruta típica:
   *Ajustes → Aplicaciones → Aló → Batería → Sin restricciones*.
4. **Ruido real:** haz una **copia de seguridad** (*WhatsApp → Ajustes → Chats → Copia de seguridad →
   Hacer copia ahora*) y recibe un **"N mensajes nuevos"** de un grupo. Ninguno debe leerse.
5. Todo lo que decida el filtro queda registrado en la pestaña **En vivo** (verde = leído,
   rojo = descartado con el motivo, ámbar = a revisar).

### Atajos útiles de adb

```bash
adb devices -l                                  # ¿está el teléfono conectado y autorizado?
adb shell pm list packages | grep voicebot      # ¿está instalada la app?
adb shell am force-stop com.voicebot.alo.debug   # cerrar la app
adb shell am start -n com.voicebot.alo.debug/com.voicebot.alo.ui.MainActivity
adb uninstall com.voicebot.alo.debug            # desinstalar
adb exec-out screencap -p > captura.png         # captura de pantalla del teléfono
adb shell dumpsys notification --noredact | less # inspeccionar notificaciones activas (avanzado)
```

---

## 6. Si usas un emulador (sin teléfono físico)

```bash
sdkmanager "emulator" "system-images;android-35;google_apis;x86_64"
avdmanager create avd -n alo -k "system-images;android-35;google_apis;x86_64" -d pixel_6
emulator -avd alo &            # o: $ANDROID_HOME/emulator/emulator -avd alo
adb wait-for-device
./gradlew installDebug
```

El emulador **no trae WhatsApp**: sirve para validar el pipeline completo con el **Modo prueba** y
las notificaciones por adb (sección 4), no para mensajes reales.

---

## 7. Errores frecuentes (con el mensaje en español) y su solución

| Mensaje que ves | Qué significa | Solución |
|---|---|---|
| `orden no encontrada` / `command not found: gradle` | Falta Gradle o el PATH no se recargó | Cierra y abre la terminal, o ejecuta `source ~/.bashrc` |
| `Permiso denegado` al ejecutar un script | Falta el bit de ejecución | `chmod +x android/scripts/*.sh` o ejecútalo con `bash ruta.sh` |
| `adb: no permissions` / `sin permisos` | Reglas udev | `sudo apt install android-sdk-platform-tools-common` y **reinicia la sesión** |
| `adb devices` muestra `unauthorized` | No aceptaste el aviso en el teléfono | Desbloquea el teléfono y pulsa *Permitir*; si no aparece, `adb kill-server && adb start-server` |
| `SDK location not found` | Falta `local.properties` | `echo "sdk.dir=$ANDROID_HOME" > android/local.properties` |
| `Failed to find Platform SDK 35` / `No se encontró` | Falta el SDK 35 | `sdkmanager "platforms;android-35"` |
| `Unsupported class file major version` | Java demasiado nuevo o viejo | Usa JDK 17: `sudo update-alternatives --config java` |
| `AGP requires Gradle 8.9+` o errores raros de Kotlin | Gradle 9 instalado | Regenera el wrapper: `gradle wrapper --gradle-version 8.11.1` |
| `setlocale: LC_ALL: cannot change locale` | Tu locale español no está generado | `sudo locale-gen es_PE.UTF-8 && sudo update-locale` (y cierra la sesión) |
| `Could not resolve androidx.*` | Sin internet o proxy | Comprueba red/VPN; el primer build **sí** necesita internet |
| `OutOfMemoryError` al compilar | Poca RAM | Añade a `android/gradle.properties`: `org.gradle.jvmargs=-Xmx2048m` |
| La app no lee nada | Casi siempre es el permiso o la batería | Verifica sección 3 y la optimización de batería |
| `Voz robótica o en otro idioma` | Falta la voz del sistema en español | Instala *Google TTS* y añade el idioma español en *Ajustes → Accesibilidad → Texto a voz* |

---

## 8. Resumen: el flujo completo en 8 líneas

```bash
cd voicebot/android
bash scripts/1-instalar-entorno.sh                 # 1. entorno (una sola vez)
bash scripts/2-compilar-e-instalar.sh              # 2. compilar + instalar + permiso + abrir
# en la app: Ajustes → activa "Modo prueba (solo debug)"
bash scripts/4-simular-whatsapp.sh                 # 3. seis notificaciones de prueba
bash scripts/3-ver-logs.sh                         # 4. ver LEÍDO / DESCARTADO en vivo
# después: escribe un contacto real y comprueba que se lee en voz alta
# opcional: ./gradlew testDebugUnitTest            # 37 tests del filtro y de la voz
```
