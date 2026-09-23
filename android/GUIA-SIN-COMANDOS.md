# Aló en Android Studio, sin usar comandos

Esta guía permite clonar, abrir, sincronizar, ejecutar y actualizar **Aló** únicamente con menús y botones de Android Studio en Linux.

## 1. Preparar Android Studio

1. Abre Android Studio.
2. En la pantalla inicial elige **More Actions → SDK Manager**.
3. En **SDK Platforms**, activa **Android 15.0 (API 35)**.
4. En **SDK Tools**, activa **Android SDK Platform-Tools**, **Android SDK Build-Tools 35** y **Android SDK Command-line Tools (latest)**.
5. Pulsa **Apply → OK** y acepta las licencias.

## 2. Clonar el repositorio

1. En la pantalla inicial selecciona **Get from VCS**. Si ya tienes un proyecto abierto: **File → New → Project from Version Control**.
2. En **Version control**, elige **Git**.
3. Pega la URL del repositorio privado: `https://github.com/SamuelPart/voicebot.git`.
4. Elige una carpeta local y pulsa **Clone**.
5. Si GitHub solicita acceso, usa **Log In via GitHub** y completa la autorización en el navegador. No pegues contraseñas ni tokens en archivos del proyecto.
6. Cuando termine el clonado, no abras la raíz completa: elige **File → Open**, selecciona la carpeta `voicebot/android/` y pulsa **Open**.
7. Si Android Studio pregunta si confías en el proyecto, pulsa **Trust Project**.

## 3. Cambiar a la rama de trabajo

1. Mira el nombre de rama en la esquina inferior derecha.
2. Haz clic en él para abrir **Git Branches**.
3. Busca **Remote Branches → origin → arena/01a0cc05-voicebot**.
4. Selecciona esa rama y pulsa **Checkout**.
5. Verifica que la esquina inferior derecha muestre `arena/01a0cc05-voicebot`.

> La rama puede variar entre sesiones. Usa siempre la rama indicada para la sesión actual, nunca `main` para desarrollar.

## 4. Configurar Java 17 y sincronizar

1. Ve a **File → Settings → Build, Execution, Deployment → Build Tools → Gradle**.
2. En **Gradle JDK**, selecciona **jbr-17** o cualquier **JDK 17** instalado.
3. En **Distribution**, deja seleccionado el wrapper del proyecto. Este fija Gradle 8.11.1.
4. Pulsa **Apply → OK**.
5. Ejecuta **File → Sync Project with Gradle Files**.
6. Espera a que desaparezca la barra de progreso. El resultado esperado es **BUILD SUCCESSFUL** sin avisos rojos en la ventana **Build**.

## 5. Preparar un teléfono Android

1. En el teléfono abre **Ajustes → Acerca del teléfono**.
2. Pulsa siete veces **Número de compilación** hasta activar las opciones para desarrolladores.
3. Abre **Ajustes → Sistema → Opciones para desarrolladores** (la ruta cambia según el fabricante).
4. Activa **Depuración USB**.
5. Conecta el teléfono por USB, desbloquéalo y acepta **Permitir depuración USB**.
6. En Android Studio, el teléfono debe aparecer en el selector de dispositivos de la barra superior.

También puedes crear un emulador desde **Tools → Device Manager → Add a new device → Create Virtual Device**, elegir un teléfono y una imagen de Android API 35.

## 6. Compilar y ejecutar Aló

1. Selecciona la configuración **app** y tu dispositivo en la barra superior.
2. Pulsa el botón verde **Run 'app'**.
3. Android Studio compilará, instalará y abrirá Aló.
4. En el teléfono, entra en **Ajustes → Notificaciones → Acceso especial a notificaciones → Aló** y concede el acceso. La ruta puede variar; la app también ofrece un botón para abrir esa pantalla.
5. En Aló abre **En vivo** y usa **Probar voz** o **Probar ruido** para validar la interfaz sin WhatsApp.

Resultado esperado: la app abre la pantalla **En vivo**, no solicita acceso a Internet y muestra el estado del acceso a notificaciones.

## 7. Ejecutar las pruebas JVM sin terminal

1. En el panel **Project**, abre `app/src/test/java`.
2. Haz clic derecho sobre la carpeta `com.voicebot.alo`.
3. Selecciona **Run 'Tests in com.voicebot.alo'**.
4. Abre la ventana **Run**.

Resultado esperado: **38 pruebas** en verde antes de añadir pruebas nuevas; después, todas las pruebas existentes y nuevas deben quedar en verde.

## 8. Mantener el proyecto sincronizado

Antes de empezar a trabajar:

1. Confirma en la esquina inferior derecha que estás en la rama de esta sesión.
2. Ve a **Git → Update Project…**.
3. Elige **Merge** (o conserva la opción predeterminada del proyecto) y pulsa **OK**.
4. Si aparecen cambios de Gradle, ejecuta **File → Sync Project with Gradle Files**.

Para revisar cambios locales usa **Git → Show Git Log** y la pestaña **Local Changes** de la ventana **Commit**. No cambies a `main` ni descartes cambios que todavía necesites.

## 9. Tabla de errores habituales

| Mensaje en Android Studio | Qué significa | Solución solo con clics |
|---|---|---|
| `Gradle JVM is incompatible` / “JVM de Gradle incompatible” | No está seleccionado Java 17 | **File → Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK → JDK 17** |
| `SDK location not found` / “No se encontró la ubicación del SDK” | Android Studio no conoce el SDK | **File → Settings → Languages & Frameworks → Android SDK**; selecciona o instala el SDK |
| `Failed to find target with hash string android-35` | Falta Android API 35 | **Tools → SDK Manager → SDK Platforms → Android 15.0 (API 35) → Apply** |
| `Minimum supported Gradle version…` o intento de usar Gradle 9 | Se ignoró el wrapper | **Settings → Build Tools → Gradle → Distribution → Wrapper** y vuelve a sincronizar |
| `Could not resolve…` al sincronizar | Faltan dependencias en la caché o no hay conexión durante la descarga inicial | Comprueba la conexión para la sincronización de desarrollo y pulsa **File → Sync Project with Gradle Files**. La app instalada no usa Internet |
| `Device unauthorized` / “Dispositivo no autorizado” | El teléfono no aceptó la clave de depuración | Desconecta y reconecta el cable; desbloquea el teléfono y acepta el diálogo de autorización |
| No aparece el teléfono | Cable, modo USB o depuración incorrectos | Activa **Depuración USB**, cambia el USB a **Transferencia de archivos** y revisa **Tools → Troubleshoot Device Connections** |
| `INSTALL_FAILED_VERSION_DOWNGRADE` | Hay instalada una versión más nueva | En el teléfono: **Ajustes → Aplicaciones → Aló → Desinstalar**; vuelve a pulsar **Run** |
| La app abre pero no recibe mensajes | Falta acceso a notificaciones o el fabricante la restringe | Concede **Acceso a notificaciones** y desactiva la optimización de batería para Aló en los ajustes del teléfono |
| Las pruebas aparecen en rojo | Una prueba falló o el proyecto no sincronizó | Abre **Run**, expande la prueba roja y copia el primer error completo; sincroniza Gradle y repite |
| `Unresolved reference` después de actualizar | Índices o sincronización incompletos | **File → Sync Project with Gradle Files**; después **File → Invalidate Caches… → Invalidate and Restart** |

## 10. Qué información enviar si algo falla

Sin usar la terminal, abre **View → Tool Windows → Build** o **View → Tool Windows → Run**, selecciona el error y usa **Copy**. Envía:

- el primer error completo y sus líneas `Caused by`;
- versión de Android Studio desde **Help → About**;
- modelo y versión Android del teléfono;
- nombre de la rama visible abajo a la derecha;
- paso exacto en el que ocurrió.

No envíes contraseñas, tokens, datos personales ni contenido real de mensajes de WhatsApp.
