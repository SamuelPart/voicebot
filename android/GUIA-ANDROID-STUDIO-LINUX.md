# Aló — Guía completa para Linux **con Android Studio**

Guía paso a paso, en español, para un Linux con terminal en español: instalar Android Studio,
abrir el proyecto, ejecutarlo en tu teléfono y probar el lector de WhatsApp.

> ¿Prefieres no usar Android Studio? Usa [`COMANDOS-LINUX.md`](COMANDOS-LINUX.md) (todo por terminal pura).
> Los dos caminos llevan al mismo resultado y se pueden combinar.

---

## 0. Elige tu ruta

| | **Ruta A · Android Studio** | **Ruta B · Terminal pura** |
|---|---|---|
| Ideal para | Ver el código, depurar, usar el panel Logcat, gestión de dispositivos | Servidores, terminal, automatizar con scripts |
| Instalación | Android Studio + SDK (el asistente lo hace todo) | JDK 17 + SDK por línea de comandos |
| Ejecutar | Botón ▶ Run (o `Shift+F10`) | `./gradlew installDebug` |
| Ver el filtro | Panel **Logcat** con filtro `Alo/` | `adb logcat … Alo/Listener:V *:S` |
| Guía | **este documento** | [`COMANDOS-LINUX.md`](COMANDOS-LINUX.md) |

**Mejor combinación:** Android Studio para desarrollar/depurar + los scripts de `android/scripts/`
para instalar, conceder permisos y simular notificaciones (funcionan igual con Studio abierto).

```bash
# Los scripts sirven en las dos rutas
bash scripts/1-instalar-entorno.sh      # entorno (JDK, SDK, adb, Gradle)
bash scripts/2-compilar-e-instalar.sh   # compila, instala, concede permiso y abre la app
bash scripts/3-ver-logs.sh              # log del filtro en la terminal
bash scripts/4-simular-whatsapp.sh      # notificaciones de prueba por adb
bash scripts/0-todo-en-uno.sh           # todo lo anterior de una vez
```

---

## 1. Instalar Android Studio en Linux

Elige **una** de las tres vías. Recomendación: **snap** (se actualiza sola y es la más simple) o el
**tarball oficial** (máximo control). Flatpak funciona, pero tiene más roces con `adb`, `/dev/kvm` y
las reglas udev.

### Vía 1 — Snap (recomendada)

```bash
sudo snap install android-studio --classic
```

Abrir desde la terminal:

```bash
android-studio            # o: snap run android-studio
```

### Vía 2 — Tarball oficial (sin gestores de paquetes)

```bash
# Descarga la versión actual desde la web oficial y descomprímela en tu carpeta local
mkdir -p ~/.local/opt && cd ~/.local/opt
# (descarga manual: https://developer.android.com/studio#downloads → Linux .tar.gz)
tar -xzf ~/Descargas/android-studio-*.tar.gz -C ~/.local/opt
~/.local/opt/android-studio/bin/studio.sh            # abrir
ln -sf ~/.local/opt/android-studio/bin/studio.sh ~/.local/bin/android-studio   # atajo
```

`studio.sh` acepta la ruta del proyecto como argumento: `studio.sh ~/voicebot/android`.

### Vía 3 — Flatpak

```bash
flatpak install flathub com.google.AndroidStudio
flatpak run com.google.AndroidStudio
```

> Si `adb` no ve el teléfono con la versión de Flatpak, es por el aislamiento del *sandbox*:
> usa la vía 1 o 2, o ejecuta `adb` desde **fuera** del Flatpak (la terminal de tu sistema) y deja
> que Studio solo lo consulte.

### Arch / Manjaro (AUR)

```bash
yay -S android-studio
```

### Requisitos que suelen faltar

```bash
# Debian/Ubuntu: JDK (Studio trae su propio runtime, pero Gradle agradece un JDK del sistema)
sudo apt install -y openjdk-17-jdk-headless unzip curl wget \
                    adb android-sdk-platform-tools-common libx11-xcb1 libdbusmenu-glib4 libgail-common

# Fedora
sudo dnf install -y java-17-openjdk-devel unzip curl wget android-tools
```

---

## 2. Primer arranque de Android Studio

1. **Setup Wizard** → elige **Standard** → acepta las licencias. Descargará el SDK en
   `~/Android/Sdk` (~2–3 GB). Es normal que tarde.
2. **Ajustes del proyecto** que debes verificar después:
   - `File → Settings → Build, Execution, Deployment → Build Tools → Gradle → **Gradle JDK**` = **17**
     (o el JBR 21 que trae Studio; ambos sirven para este proyecto).
   - `File → Settings → Languages & Frameworks → Android SDK` → instala **Android 15 (API 35)** y
     las **Android SDK Build-Tools 35.0.0**.
3. **Comprueba la consola**: `Help → Show Log in Files` si algo raro pasa al arrancar.

---

## 3. Abrir el proyecto

**Abre la carpeta `android/`, NO la raíz del repositorio** (la raíz no es un proyecto Gradle):

- Desde la interfaz: `File → Open…` → `…/voicebot/android` → **Trust Project**.
- Desde la terminal: `android-studio ~/voicebot/android`

Al abrir, Studio dirá que el proyecto **no tiene el wrapper de Gradle** (el repositorio no incluye
`gradlew`/`gradle-wrapper.jar` porque son binarios). Tienes dos salidas:

```bash
# Opción rápida por terminal (una sola vez)
cd ~/voicebot/android
gradle wrapper --gradle-version 8.11.1          # requiere tener gradle instalado
# o, sin instalar Gradle:
curl -fLo /tmp/gradle.zip https://services.gradle.org/distributions/gradle-8.11.1-bin.zip
unzip -q /tmp/gradle.zip -d ~/.local/opt && ~/.local/opt/gradle-8.11.1/bin/gradle wrapper --gradle-version 8.11.1
```

Y si prefieres que lo haga Studio: acepta el aviso *“Gradle wrapper is missing”* y deja que genere
la tarea `wrapper`, o en `Settings → Build Tools → Gradle` elige **Local installation** y apunta a tu
Gradle 8.11.1.

⚠️ **Nunca Gradle 9.x**: el plugin de Android 8.7.3 no lo soporta. Versiones válidas: **8.9 – 8.13**.

Después: `File → Sync Project with Gradle Files`. La primera sincronización descarga AGP 8.7.3,
Kotlin 2.0.21, Compose (BOM 2024.12.01) y Room: **5–10 minutos** con internet.

> **Si falta `local.properties`** (lo normal es que Studio lo genere solo):
> ```bash
> echo "sdk.dir=$HOME/Android/Sdk" > ~/voicebot/android/local.properties
> ```

---

## 4. Conectar el teléfono (o usar un emulador)

### Teléfono físico por USB

1. En el teléfono: *Ajustes → Acerca del teléfono → toca 7 veces **Número de compilación***.
2. *Opciones de desarrollador → **Depuración USB*** → activar.
3. Conéctalo por cable y acepta el aviso **“¿Permitir depuración USB?”**.
4. En la terminal: comprueba que aparece autorizado.

```bash
adb devices -l
# Esperado:
# List of devices attached
# 1A2B3C4D5E    device product:... model:... device:...
```

Si dice `unauthorized`, desbloquea el teléfono y acepta el aviso. Si dice `no permissions`, faltan
las reglas udev:

```bash
sudo apt install -y android-sdk-platform-tools-common    # Debian/Ubuntu
# o ejecuta: bash scripts/1-instalar-entorno.sh  (instala las reglas udev)
```
…y **cierra la sesión y vuelve a entrar** (los grupos nuevos solo se aplican en una sesión nueva).

### Teléfono por Wi-Fi (Android 11+) — cómodo si el cable es un lío

```bash
# En el teléfono: Opciones de desarrollador → Depuración inalámbrica → Emparejar con código
adb pair 192.168.1.50:37xxx        # IP:puerto que muestra el teléfono + código de 6 dígitos
adb connect 192.168.1.50:5555
```
Luego en Studio el teléfono aparece igual que por USB.

### Emulador en Linux: requisitos de KVM

```bash
# 1) Comprobar virtualización
sudo apt install -y cpu-checker && kvm-ok        # debe decir: KVM acceleration can be used
ls -l /dev/kvm                                   # debe existir

# 2) Permiso para tu usuario (si /dev/kvm es de root:kvm)
sudo usermod -aG kvm "$USER"                     # luego cierra sesión y entra de nuevo
```

Crear el dispositivo en Studio: `Tools → Device Manager → Create Device` → **Pixel 6** →
**API 35 (Google APIs)** → Finish.

> En el emulador **no hay WhatsApp**: sirve para validar filtro, voz e interfaz con el **Modo prueba**
> (sección 6). Para mensajes reales, teléfono físico.

---

## 5. Ejecutar la app

1. Elige el dispositivo en la barra superior (teléfono o emulador).
2. Pulsa **▶ Run** (`Shift+F10`) o el martillo **Build → Make Project** (`Ctrl+F9`) si solo quieres compilar.
3. Studio instala `com.voicebot.alo.debug` y abre la app.

Desde la terminal de Studio (`Alt+F12`, que ya está en `~/voicebot/android`):

```bash
../gradlew installDebug      # si generaste el wrapper
# o, sin wrapper:
gradle installDebug
```

---

## 6. Activar la escucha y probar (lo importante)

### a) Conceder el acceso a notificaciones

En la app verás una **tarjeta amarilla** con el botón *Abrir ajustes de acceso*. Desde la terminal de
Studio también se puede, sin tocar la pantalla:

```bash
adb shell cmd notification allow_listener com.voicebot.alo.debug/com.voicebot.alo.service.WaListenerService
# Comprobar:
adb shell settings get secure enabled_notification_listeners
```

También a mano: *Ajustes → Apps → Acceso especial → Acceso a notificaciones → Aló*.

### b) Probar sin WhatsApp: “Modo prueba” + notificaciones por adb

En la app: **Ajustes → “Modo prueba (solo debug)”** → activar. Después, en la terminal de Studio:

```bash
bash scripts/4-simular-whatsapp.sh      # envía 6 pruebas y explica el resultado esperado
```

O a mano, una por una:

```bash
# Debe LEERSE
adb shell cmd notification post -S messaging --user "Yo" \
  --conversation "Mamá" --message "Mamá:Hijo, ¿ya saliste de la oficina?" alo1 "Mamá" "hola"

# Debe DESCARTARSE (copia de seguridad)
adb shell cmd notification post -t "WhatsApp" alo3 "Copia de seguridad completada"

# Debe DESCARTARSE (resumen)
adb shell cmd notification post -t "WhatsApp" alo4 "3 mensajes nuevos"
```

**Resultado esperado de las 6 pruebas:** 3 leídos (mensaje individual, mensaje de grupo y el mensaje
que *menciona* un respaldo) y 3 descartados (respaldo, resumen y código de verificación).

### c) Ver el filtro en el panel **Logcat** de Studio

`View → Tool Windows → Logcat` (`Alt+6`) y en el campo de filtro escribe:

```
tag:Alo/Listener
```

Verás, en vivo:

```
D/Alo/Listener: notificación de com.whatsapp · canal=messages_1 · mensajes=2
D/Alo/Listener: LEÍDO (1 mensaje/s, speak=true)
D/Alo/Listener: notificación de com.whatsapp · canal=backup · mensajes=0
D/Alo/Listener: DESCARTADO (capa 1: notificación en curso (isOngoing): progreso/backup)
```

Etiquetas disponibles: `Alo/Listener`, `Alo/Voice`, `Alo/TTS`, `Alo/Reply`.

### d) Probar con WhatsApp de verdad

1. **Pide a alguien que te escriba** (o usa un segundo número). Evita el chat “Tú” (contigo mismo):
   esos mensajes los envías tú y el filtro los descarta a propósito.
2. **Quita la optimización de batería**: en Xiaomi, Samsung, Huawei, Oppo, Realme y Vivo el sistema
   mata los servicios en segundo plano. Ruta típica: *Ajustes → Aplicaciones → Aló → Batería →
   **Sin restricciones***.
3. **Ruido real:** haz una **copia de seguridad** (*WhatsApp → Ajustes → Chats → Copia de seguridad →
   Hacer copia ahora*) y recibe un **“N mensajes nuevos”** de un grupo. Ninguno debe leerse.
4. Revisa la pestaña **En vivo** en la app: verde = leído, rojo = descartado con el motivo,
   ámbar = a revisar (buzón de no clasificados).

---

## 7. Ejecutar los tests desde Studio

- Interfaz: abre `app/src/test/java/…/NotificationNormalizerTest.kt` y pulsa ▶ junto a la clase.
- Terminal (dentro de Studio, `Alt+F12`): `gradle testDebugUnitTest`
- Resultado: **38 tests** (23 del filtro, 8 del motor de voz y 7 de utilidades de texto; el detalle sale
  en el reporte `app/build/reports/tests/testDebugUnitTest/index.html`).

---

## 8. Diferencias y detalles propios de Linux

| Tema | Detalle |
|---|---|
| **Rutas** | El SDK vive en `~/Android/Sdk`; Android Studio en `~/.local/opt/android-studio` (tarball) o `/snap/android-studio` |
| **Wayland vs X11** | Si hay escalado raro o pantallas negras, arranca con `STUDIO_USE_WAYLAND=0 android-studio` o en la sesión X11 |
| **Pantallas HiDPI** | `Settings → Appearance & Behavior → Appearance → Use custom font size` y `Zoom` para agrandar la interfaz |
| **Emulador** | Requiere KVM (`/dev/kvm` + grupo `kvm`) y, en algunos equipos, `-gpu swiftshader_indirect` si la GPU da problemas |
| **Snap** | El confinamiento a veces no ve SDKs fuera de `$HOME`; mantén `ANDROID_HOME=$HOME/Android/Sdk` |
| **Terminal en español** | Si ves `setlocale: LC_ALL: cannot change locale`, genera tu locale: `sudo locale-gen es_PE.UTF-8 && sudo update-locale` y reinicia la sesión |
| **Firewall** | No hace falta abrir nada: la app **no pide permiso de INTERNET** |

---

## 9. Errores frecuentes (Android Studio en Linux) y solución

| Mensaje | Causa | Solución |
|---|---|---|
| `Unsupported class file major version` | JDK incorrecto | *Settings → Build Tools → Gradle → Gradle JDK* = 17 |
| `AGP requires Gradle 8.9+` / errores raros de Kotlin | Gradle 9.x | Fija **8.11.1** (wrapper o *Gradle version* en Settings) |
| `SDK location not found` | Falta `local.properties` | `echo "sdk.dir=$HOME/Android/Sdk" > android/local.properties` |
| `Failed to find Platform SDK 35` | Falta el SDK 35 | *Settings → Android SDK* → marca **Android 15 (API 35)** |
| `Could not resolve androidx.*` | Sin internet/proxy en el primer build | Conéctate y repite *Sync*; el primer build **sí** necesita red |
| `OutOfMemoryError` al compilar | Poca RAM | `android/gradle.properties` → `org.gradle.jvmargs=-Xmx2048m` |
| `adb: no permissions` / `sin permisos` | Reglas udev | `android-sdk-platform-tools-common` + **reiniciar sesión** |
| Emulador: `KVM is required` | Sin virtualización | Actívala en la BIOS/UEFI, instala `qemu-kvm`, añade tu usuario al grupo `kvm` |
| La app no lee nada | Permiso o batería | Sección 6.a y quitar la optimización de batería |
| `Unresolved reference: Icons` / `enableEdgeToEdge` | Sincronización a medias | *Build → Clean Project* + *Sync*; verifica que bajaron `material-icons-core`, `activity-compose` y `lifecycle` |
| `Room - Schema export directory was not provided` | Aviso esperado | Es intencional (`exportSchema = false`) |

Si aparece cualquier otro error: **copia el texto completo de la pestaña Build y pásamelo** y lo corrijo.

---

## 10. Resumen en 10 líneas

```bash
sudo snap install android-studio --classic          # 1. instalar Studio
cd ~/voicebot/android && gradle wrapper --gradle-version 8.11.1   # 2. wrapper
android-studio ~/voicebot/android                   # 3. abrir el proyecto (carpeta android/)
# 4. En Studio: Sync → Gradle JDK 17 → SDK 35 instalado
# 5. Conecta el teléfono con Depuración USB (adb devices debe decir "device")
# 6. Shift+F10 para ejecutar
adb shell cmd notification allow_listener com.voicebot.alo.debug/com.voicebot.alo.service.WaListenerService
# 7. En la app: Ajustes → "Modo prueba (solo debug)"
bash scripts/4-simular-whatsapp.sh                  # 8. seis notificaciones de prueba
# 9. Logcat con el filtro  tag:Alo/Listener
# 10. Pide a alguien que te escriba y comprueba que se lee en voz alta
```
