# Aló — MVP Android (Fase 0)

Esqueleto funcional del lector de mensajes de WhatsApp en tiempo real: **captura → filtro de 5 capas → lectura en voz alta → historial local**. Todo el procesamiento ocurre en el teléfono (la app **no declara `INTERNET`**).

> Documento de producto y plan completo: [`../PROPUESTA.md`](../PROPUESTA.md)
> Mockup visual interactivo: [`../mockup/index.html`](../mockup/index.html)

---

## 1. Cómo compilarlo y probarlo

Requisitos: **Android Studio Ladybug o superior**, **JDK 17**, **Android SDK 35**, un teléfono/emulador **con WhatsApp instalado** (Android 8.0+).

```bash
cd android

# 1) Tests del filtro y de la voz (JVM puro, sin emulador)
gradle testDebugUnitTest          # o ./gradlew testDebugUnitTest tras "gradle wrapper"

# 2) Instalar en el teléfono
gradle installDebug               # o ./gradlew installDebug
```

En Android Studio: *Open* → carpeta `android/` → Run.

### Primer arranque (obligatorio)

1. Abre **Aló** → botón **“Abrir ajustes de acceso”**.
2. Activa **Acceso a notificaciones** para Aló (Ajustes → Apps → Acceso especial → Acceso a notificaciones).
3. Vuelve a la app: la tarjeta de permiso desaparece y aparece “Acceso a notificaciones concedido”.
4. En **Ajustes → Batería**, si usas Xiaomi/Samsung/Huawei/Oppo, marca Aló como **sin restricciones**.
5. Prueba con los botones **“Probar voz”** (mensaje simulado) y **“Probar ruido”** (avisos que deben descartarse) sin esperar un mensaje real.

Los mensajes que ya estaban en el panel de notificaciones al conceder el permiso se **guardan** pero **no se leen en voz alta** (evita un discurso de 20 mensajes al instalar).

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

- `NotificationNormalizerTest` (20 casos): mensajes individuales y de grupo, solo mensajes nuevos, backup en curso, resumen de grupo, llamada perdida, estado, canal de multimedia, paquete ajeno, aviso de respaldo, código de verificación, “mensaje que suena a aviso” (no debe perderse), solo emojis, aviso de sistema dentro del chat, reemisión sin duplicar, mensaje nuevo sí procesado, buzón de revisión, chat silenciado, horario de silencio, solo con audífonos y pausa en llamadas.
- `SpeechTextBuilderTest` (7 casos): plantillas individual/grupo, agrupación de varios mensajes, enlaces y emojis, recorte de mensajes largos.
- `TextUtilsTest` (7 casos): normalización de acentos, patrones multilingües, hash estable, limpieza para voz, preview.
