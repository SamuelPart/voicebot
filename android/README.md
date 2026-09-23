# Aló — MVP Android (Fase 0)

Esqueleto funcional del lector de mensajes de WhatsApp en tiempo real: **captura → filtro de 5 capas → lectura en voz alta → historial local**. Todo el procesamiento ocurre en el teléfono (la app **no declara `INTERNET`**).

> Documento de producto y plan completo: [`../PROPUESTA.md`](../PROPUESTA.md)
> Mockup visual interactivo: [`../mockup/index.html`](../mockup/index.html)

---

## 1. Abrirlo en Android Studio y ejecutarlo

### Elige tu ruta

| Ruta | Guía | Cuándo |
|---|---|---|
| **A · Android Studio** (recomendada para desarrollar) | [`GUIA-ANDROID-STUDIO-LINUX.md`](GUIA-ANDROID-STUDIO-LINUX.md) | Ver el código, panel Logcat, depurador, emulador |
| **B · Terminal de Linux** (sin Studio) | [`COMANDOS-LINUX.md`](COMANDOS-LINUX.md) | Automatizar, servidores, o si prefieres la terminal |
| **Atajos automáticos** (sirven en ambas) | `android/scripts/` — ver abajo | Instalar entorno, compilar, instalar, ver logs, simular WhatsApp |

```bash
cd voicebot/android
chmod +x scripts/*.sh
bash scripts/0-todo-en-uno.sh     # entorno + compilar + instalar + abrir, de una sola vez
```

### Requisitos

| Herramienta | Versión necesaria | Notas |
|---|---|---|
| Android Studio | **Ladybug (2024.2) o superior** | Con Koala también debería funcionar |
| JDK | **17 a 21** | Studio trae su propio JDK en *Settings → Build Tools → Gradle → Gradle JDK* |
| Android SDK | **Platform 35** | Studio ofrece instalarlo solo al hacer *Sync* |
| Gradle | **8.9 – 8.13** | ⚠️ **no uses Gradle 9.x**: AGP 8.7.3 necesita la serie 8 |
| Dispositivo | Teléfono físico **con WhatsApp** o emulador | En un emulador sin WhatsApp solo funcionan las notificaciones de prueba por `adb` |

> **¿Primera vez?** El proyecto vive en GitHub (repositorio privado) y el código está en la rama
> `arena/01a0cbd4-voicebot`. Empieza por la **sección 0** de
> [`GUIA-ANDROID-STUDIO-LINUX.md`](GUIA-ANDROID-STUDIO-LINUX.md): explica las 4 formas de traerlo a tu PC
> (fusionar el PR, clonar la rama, descargar el ZIP o clonar desde el propio Android Studio) y cómo
> autenticarse en un repositorio privado.

> Las guías completas de instalación (incluido el emulador con KVM, el teléfono por Wi-Fi y los
> errores típicos de Linux) están en los dos documentos de la tabla de arriba.

### Paso 1 — Wrapper de Gradle (la única pieza que falta en el repo)

El repositorio **no incluye** `gradlew` / `gradle-wrapper.jar` (son binarios que no se generan a mano). Dos caminos:

- **Camino A (recomendado, 1 comando).** Instala Gradle una vez y genera el wrapper:
  ```bash
  # macOS:    brew install gradle
  # Windows:  choco install gradle   (o: scoop install gradle)
  # Linux:    sdk install gradle 8.11.1   (SDKMAN)
  cd android
  gradle wrapper --gradle-version 8.11.1
  ```
  A partir de ahí todo funciona con `./gradlew` (macOS/Linux) o `gradlew.bat` (Windows), y también sirve para la CI.

- **Camino B (sin instalar nada).** Abre el proyecto igual y deja que Studio lo resuelva: te avisará de que no hay wrapper y te ofrecerá **usar la distribución local de Gradle** o **generar el wrapper** (tarea `wrapper`). Acepta y, si te deja elegir versión, pon **8.11.1**.

### Paso 2 — Abrir el proyecto

**Abre la carpeta `android/`, NO la raíz del repositorio** (la raíz no es un proyecto Gradle):

`File → Open… → …/voicebot/android` → **Trust Project** → *Sync Project with Gradle Files*.

La primera sincronización descarga AGP 8.7.3, Kotlin 2.0.21, Compose (BOM 2024.12.01) y Room: **5–10 minutos**. Es normal ver “Gradle: resolving dependencies…”.

### Paso 3 — Elegir dispositivo

- **Teléfono físico (lo que necesitas para probar de verdad):** actívalo en *Ajustes → Acerca del teléfono → toca 7 veces “Número de compilación”*, luego *Opciones de desarrollador → Depuración USB*, conéctalo por cable y acéptale el aviso de “¿Permitir depuración USB?”. Debe aparecer en la barra superior de Studio.
- **Emulador:** *Device Manager → Create Device* → cualquiera con **Android 10 (API 29) o superior** y Google Play. Ojo: en el emulador no hay WhatsApp, así que solo probarás el pipeline con los botones **“Probar voz”** y **“Probar ruido”** (suficiente para validar filtro, voz e interfaz).

### Paso 4 — Ejecutar

Pulsa **▶ Run** (o `Ctrl/Cmd + R`). Studio compila, instala `com.voicebot.alo.debug` y abre la app.

### Paso 5 — Primer arranque en el teléfono (obligatorio)

1. Abre **Aló** → botón **“Abrir ajustes de acceso”**.
2. Activa **Acceso a notificaciones** para Aló (Ajustes → Apps → Acceso especial → Acceso a notificaciones).
3. Vuelve a la app: la tarjeta de permiso desaparece y aparece “Acceso a notificaciones concedido”.
4. En **Ajustes → Batería**, si usas Xiaomi/Samsung/Huawei/Oppo, marca Aló como **sin restricciones**.
5. Prueba con los botones **“Probar voz”** (mensaje simulado) y **“Probar ruido”** (avisos que deben descartarse) sin esperar un mensaje real.

Los mensajes que ya estaban en el panel de notificaciones al conceder el permiso se **guardan** pero **no se leen en voz alta** (evita un discurso de 20 mensajes al instalar).

### Paso 6 — Probar con mensajes reales

1. **Pídele a alguien que te escriba** (o escríbete desde un segundo número en otro teléfono). Truco: puedes abrir WhatsApp Web en la computadora y mandarte un mensaje **desde el chat de tu propio contacto (yo → tú)**… mejor evita el chat “Tú” (contigo mismo), porque esos mensajes los envías tú y el filtro los ignora a propósito.
2. Abre la pestaña **En vivo** de Aló: verás una línea verde *Leídos* con el texto, o una roja *Descartados* con la capa y el motivo.
3. Prueba el ruido de verdad en el teléfono: activa una **copia de seguridad** (*WhatsApp → Ajustes → Chats → Copia de seguridad → Hacer copia ahora*) y recibe un **“N mensajes nuevos”** de un grupo. Ninguno debe leerse: ambos deben quedar como descartados con su motivo.
4. Botón **🔊** en *Historial* para volver a oír un mensaje.

### Alternativa por línea de comandos (sin Studio)

```bash
cd android
./gradlew testDebugUnitTest      # tests del filtro y de la voz (JVM, sin emulador)
./gradlew installDebug           # instala en el teléfono conectado por USB
```

### Errores frecuentes en el primer intento

| Síntoma | Causa y solución |
|---|---|
| `AGP requires Gradle 8.9+` / errores de API raros | Gradle incompatible: pon el wrapper en **8.11.1** (*Settings → Build Tools → Gradle → Gradle version*) |
| `SDK location not found` | Falta `local.properties`: deja que Studio lo genere (o crea `android/local.properties` con `sdk.dir=/ruta/a/Android/Sdk`) |
| `Failed to find Platform SDK 35` | *Settings → Languages & Frameworks → Android SDK* → instala **Android 15 / API 35** |
| `Unresolved reference: enableEdgeToEdge` / `collectAsStateWithLifecycle` | Sincronización incompleta: *Build → Clean Project* + *Sync*; deben haber bajado `activity-compose 1.9.3` y `lifecycle 2.8.7` |
| La app no lee nada | El **Acceso a notificaciones** está apagado (revisa la tarjeta amarilla), o el fabricante mató el servicio: quítale la optimización de batería |
| En el emulador no pasa nada con WhatsApp | El emulador no trae WhatsApp: usa un teléfono físico para la prueba real |
| `Room - Schema export directory was not provided` | Es un *warning* esperado: la base usa `exportSchema = false` en la Fase 0 |

Si aparece cualquier otro error de compilación, pásame el texto completo del *Build Output* y lo corrijo.

---

## 2. Mapa del código

```
app/src/main/java/com/voicebot/alo/
├── core/
│   ├── model/MessageEvent.kt          MessageEvent + FilterResult (Read | Discarded | NeedsReview)
│   ├── filter/
│   │   ├── RawNotification.kt         datos crudos + NotificationSnapshot (lee el StatusBarNotification)
│   │   ├── NotificationExtras.kt      MessagingStyle vía NotificationCompat (API estable)
│   │   ├── FilterRules.kt             diccionario de patrones de ruido (versionado, listo para OTA)
│   │   ├── MessageNormalizer.kt       ★ EL MOTOR: las 5 capas del filtro (puro, testeable)
│   │   ├── SpeakingPolicy.kt          capa 5: reglas del usuario (horarios, silenciados, audífonos…)
│   │   └── MessageMemory.kt           capa 4: memoria de deduplicación (interfaz + versión en memoria)
│   ├── settings/AloSettings.kt        ajustes persistentes → alimentan la capa 5
│   ├── log/EventLog.kt                registro en vivo (capa que decidió, motivo) para la UI
│   └── util/                          TextUtils (normalización, hash, limpieza para voz) + DeviceContext
├── data/                              Room: mensajes, descartes/buzón y cursores por conversación
├── service/
│   ├── WaListenerService.kt           ★ NotificationListenerService (captura en tiempo real)
│   └── ReplyDispatcher.kt             Fase 2: responder por RemoteInput sin salir de Aló
├── voice/
│   ├── TextToSpeechEngine.kt          TTS del sistema en modo suspend (idioma con degradación elegante)
│   ├── VoiceQueue.kt                  cola: una voz a la vez, en orden, sin solaparse
│   ├── SpeechTextBuilder.kt           plantillas naturales + agrupación + limpieza (URLs, emojis)
│   ├── AudioFocusHelper.kt            ducking: baja la música, habla y devuelve el volumen
│   └── VoiceAnnouncer.kt              une filtro → voz (+ vibración)
├── di/AloGraph.kt                     inyección manual (sin Hilt en el MVP)
└── ui/                                Compose: En vivo · Historial · Revisar · Ajustes
```

---

## 3. El filtro de 5 capas (implementado en `MessageNormalizer`)

| Capa | Regla implementada | Resultado |
|---|---|---|
| **0** | Paquete ∈ `com.whatsapp`, `com.whatsapp.w4b` | si no → descarte inmediato (sin coste) |
| **1** | `isOngoing`, `FLAG_GROUP_SUMMARY`, canal `backup`/`media_upload`/`call`/`status`…, categoría `call`/`missed_call`/`status` | descarte |
| **2** | `MessagingStyle` → mensajes con remitente y hora | señal fuerte de “es un mensaje” |
| **3** | Patrones de aviso **solo si no hay estructura**: respaldos, “mensajes nuevos”, OTP…; y avisos de sistema dentro del chat (“cifrado de extremo a extremo”, “cambió el asunto”) | descarte |
| **4** | Huella `sha256(paquete + chat + remitente + hora + texto)` + cursor por conversación | descarte de reemisiones |
| **5** | Reglas del usuario: chats silenciados, horario de silencio, solo con audífonos, silencio en llamadas, grupos on/off | captura **pero sin voz** (`speak = false`) |

### La decisión más importante del diseño

**Un mensaje real de WhatsApp siempre trae `MessagingStyle`; los avisos de servicio son texto plano.**
Por eso los patrones de texto solo se aplican cuando **no** hay estructura: así un mensaje legítimo que “suene a aviso” (ej. *“¿Ya hiciste la copia de seguridad?”*) **nunca** se pierde. Está cubierto por un test.

### Política ante la duda (cero mensajes perdidos)

- Señal clara de “no es un chat” → **descartado** (registrado con capa y motivo).
- Señal fuerte de “es un mensaje” → **leído**.
- **Ambigüedad → `NeedsReview`**: se guarda en el buzón de la pestaña *Revisar*, **sin** leerse en voz alta. Nada se pierde y esos casos son el insumo para las reglas OTA de la Fase 1.

---

## 4. Qué está terminado y qué no (honestidad de alcance)

| ✅ Implementado en este esqueleto | 🚧 Fase 1 (siguiente) |
|---|---|
| Captura en tiempo real vía `NotificationListenerService` | Foreground service + vigilancia por `WorkManager` y guía por fabricante |
| Filtro de 5 capas con 20+ tests JVM | Reglas descargables por OTA + telemetría de no clasificados (sin contenido) |
| Cola de TTS con *ducking*, idioma y plantillas naturales | Voces premium en la nube (add-on de pago) |
| Persistencia Room (mensajes, descartes, cursores) + retención | Cifrado en reposo con SQLCipher |
| Registro en vivo con la capa que decidió y el motivo | Analítica local de falsos positivos/negativos (KPIs de la propuesta) |
| Buzón de revisión de no clasificados | UI para etiquetar esos casos y exportarlos |
| Backfill de notificaciones activas sin audio | Multi-app (Telegram/SMS) con la misma capa de filtros |
| Reply por `RemoteInput` (clase lista, sin UI todavía) | Dictado por voz + confirmación hablada antes de enviar |

No incluido a propósito: `Hilt`, `INTERNET`, backend, analítica de terceros, publicidad. Todo eso añade superficie de datos sin aportar a la validación de la Fase 0.

---

## 5. Criterios de salida de la Fase 0 (medibles, no opiniones)

Con 7 días de uso real en tu teléfono, revisando la pestaña **En vivo**:

1. **0 mensajes reales perdidos**: todo mensaje de chat aparece en *Historial*. Los no clasificados aparecen en *Revisar* (no se pierden, aunque no se lean).
2. **< 1 % de falsos positivos**: de cada 100 eventos, 99 descartados correctamente (respaldos, resúmenes, llamadas, estados, OTP).
3. **Latencia < 1,5 s** entre la notificación y el inicio de la voz (comparar hora del mensaje y hora del clip).
4. **0 repeticiones**: ningún mensaje se lee dos veces (capa 4 funcionando).
5. **Estabilidad**: el servicio sigue vivo 24 h después (si no: ajustar batería del fabricante y anotarlo).

---

## 6. Decisiones técnicas y riesgos conocidos

- **`NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification()`** en vez de las clases del framework `Notification.MessagingStyle.Message`: devuelve una lista plana, funciona igual si la notificación la construyó WhatsApp con el framework, y no depende de APIs que cambian entre versiones.
- **Procesamiento secuencial en un hilo dedicado**: la deduplicación y los cursores por conversación dependen del orden.
- **Lista de patrones de ruido como `FilterRules` versionado**: mover a un JSON descargable es un cambio de 20 líneas, no un refactor.
- **Mensajes propios**: WhatsApp añade a la notificación las respuestas que envías tú. El filtro descarta los mensajes cuyo remitente coincide con el usuario del teléfono (`MessagingStyle.getUser()`); si WhatsApp no informa el remitente, la frase hablada es **neutra** (“Nuevo mensaje en el chat de X: …”) en vez de atribuirte el texto a tu contacto. Hay un test para cada caso.
- **`TextToSpeech` se crea en el hilo principal** (requiere `Looper`), aunque el pipeline de captura corra en un hilo propio: la cola espera a que el motor esté listo con un `CompletableDeferred`.
- **Arranque con puerta (`ready`)**: ninguna notificación se procesa hasta que la memoria del filtro (cursores + huellas) está cargada; si no, al reiniciar el servicio se podría releer un mensaje ya visto.
- **`chatId` derivado de `sha256(paquete + título del chat + tipo)`**: si alguien cambia el nombre del grupo, se trata como conversación nueva (aceptable en Fase 0; en Fase 1 usar el id real del chat si WhatsApp lo expone en los extras).
- **Android 15+** entrega las notificaciones con OTP redactadas a apps no autorizadas: por eso el filtro también reconoce el texto redactado.
- **Fabricantes agresivos con la batería** pueden detener el listener: mitigación en Fase 1 (foreground service + asistente por marca).
- **Privacidad**: sin `INTERNET`, sin copias de seguridad (`allowBackup=false`, `dataExtractionRules` excluye todo) y borrado del historial a un toque. Es la base del formulario *Data Safety* de Play y del argumento comercial.

---

## 7. Tests

```bash
gradle testDebugUnitTest
```

*(Integración continua: copia `android/ci/github-workflow.yml` a `.github/workflows/android.yml`
para que GitHub Actions ejecute los tests y compile el APK en cada push — ver instrucciones dentro del archivo.)*

- `NotificationNormalizerTest` (23 casos): mensajes individuales y de grupo, solo mensajes nuevos, backup en curso, resumen de grupo, llamada perdida, estado, canal de multimedia, paquete ajeno, aviso de respaldo, código de verificación, “mensaje que suena a aviso” (no debe perderse), solo emojis, aviso de sistema dentro del chat, mensajes propios, reemisión sin duplicar, mensaje nuevo sí procesado, buzón de revisión, chat silenciado, horario de silencio, solo con audífonos y pausa en llamadas.
- `SpeechTextBuilderTest` (8 casos): plantillas individual/grupo, agrupación de varios mensajes, enlaces y emojis, recorte de mensajes largos, remitente desconocido (frase neutra).
- `TextUtilsTest` (7 casos): normalización de acentos, patrones multilingües, hash estable, limpieza para voz, preview.
