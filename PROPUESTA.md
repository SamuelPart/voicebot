# Aló — Lector inteligente de WhatsApp en tiempo real
### Propuesta técnica y de producto (v1.0 · septiembre 2026)

> **Nombre de trabajo:** *Aló* (alternativas: *Oído*, *VoxInbox*, *Escúchame*).
> **Concepto:** app Android que **captura en tiempo real solo los mensajes de chat de WhatsApp**, los **lee en voz alta** de forma natural y los **guarda/categoriza** en un panel propio, filtrando todo el ruido (avisos de copia de seguridad, estados, llamadas perdidas, sincronización, códigos OTP, resumen de "nuevos mensajes").
> **Objetivo comercial:** producto propio (freemium + suscripción) y versión B2B para equipos.

---

## 1. Resumen ejecutivo

Tu idea es **viable y buena**, pero hay una decisión que define todo el proyecto: **por dónde se leen los mensajes**. Existen tres caminos y solo uno sirve para un **producto legal, publicable y escalable**:

| Camino | ¿Lee tus chats personales? | Legalidad / ToS | Riesgo de ban | ¿Publicable en Play Store? | Veredicto |
|---|---|---|---|---|---|
| **A. Acceso a notificaciones (Android)** | ✅ Sí, en **tu propio teléfono**, en tiempo real | ✅ Aceptado (permiso del sistema, con consentimiento del usuario) | ✅ Ninguno | ⚠️ Viable, permission restringido: exige justificación, política de privacidad y revisión | ⭐ **BASE DEL PRODUCTO** |
| **B. WhatsApp Business Cloud API (oficial Meta)** | ❌ No: solo mensajes que **clientes escriben a tu número business** | ✅ 100% conforme | ✅ Ninguno | ✅ Sí | ⭐ **MÓDULO B2B** |
| **C. Librerías no oficiales (Baileys, whatsapp-web.js, Evolution, WAHA, Z-API…)** | ✅ Técnicamente sí | ❌ Viola los ToS de WhatsApp | 🔴 **Altísimo: baneo permanente del número, sin recuperación** (desde 2026 Meta detecta y bloquea en 24–72 h incluso cuentas estables por meses) | ❌ No | 🚫 **DESCARTADO** |

**Recomendación:** construye el producto sobre **A** (app Android con `NotificationListenerService`, procesamiento *on-device*) y añade **B** como módulo *premium para empresas*. La vía **C** no es un atajo: es el riesgo que haría inviable el negocio (pierdes el número del cliente y, con él, la confianza).

**Por qué A es una oportunidad real y no un hack:** WhatsApp ya publica sus mensajes como *notificaciones estructuradas* (`MessagingStyle`) que el sistema Android entrega a apps con el permiso "Acceso a notificaciones". Eso significa:

- **Tiempo real** (el mensaje llega al teléfono en milisegundos).
- **No requiere iniciar sesión** en la cuenta de WhatsApp, ni QR, ni contraseña → el usuario no entrega credenciales y **WhatsApp no puede banear nada**, porque no se está conectando a sus servidores.
- **Funciona también con el teléfono bloqueado**, en el auto, con audífonos Bluetooth.
- El reto real —y por tanto **la barrera técnica que se convierte en tu ventaja competitiva**— es el **motor de filtrado**: distinguir un mensaje de chat de cualquier otro aviso.

Estimación del MVP: **6–8 semanas**, 1 desarrollador Android senior + diseño part-time. Costo de infraestructura inicial: **< USD 80/mes**. Publicación en Play: **USD 25 una vez**.

---

## 2. Lo que pides, traducido a especificación

| Tu pedido | Cómo se implementa |
|---|---|
| "Mensajes en tiempo real" | `NotificationListenerService.onNotificationPosted()` (< 1 s desde que llega el mensaje al teléfono) |
| "Solo mensajes, nada de avisos" | **Motor de filtrado de 5 capas** (§5). Señal principal: la notificación se anuncia como `MessagingStyle`, que es un formato que WhatsApp **solo** usa para mensajes de chat |
| "Nada de copia de seguridad" | Exclusión de canales (`backup`, `sync`, `media_upload`), de notificaciones "ongoing"/progreso y de patrones de texto ("Copia de seguridad", "Restaurando…", "Descargando…") |
| "Solo mensajes de chats" | Allowlist de paquetes (`com.whatsapp`, `com.whatsapp.w4b`) + allowlist de canales de conversación + descarte de resúmenes de grupo |
| "Leer" (en voz alta) | Motor TTS con cola de audio, *ducking*, idioma por mensaje, sanitización de emojis/URLs y plantilla "Mensaje de {remitente} en {grupo}: {texto}" |
| "Capturar" (guardar) | Base de datos local cifrada + panel de historial, búsqueda, reglas por chat y exportación |
| "Producto para vender" | Freemium + suscripción + licencias B2B + white-label (§8) |

---

## 3. Arquitectura propuesta

```
┌──────────────────────────── APP ANDROID (Kotlin) ────────────────────────────┐
│                                                                              │
│  ① CAPTURA                     ② FILTRADO / NORMALIZACIÓN                    │
│  ┌──────────────────────┐      ┌────────────────────────────────────────┐    │
│  │ NotificationListener │─────▶│ Capa 0: allowlist de paquetes          │    │
│  │ Service (servicio    │      │ Capa 1: canal + categoría + flags      │    │
│  │ de primer plano)     │      │ Capa 2: MessagingStyle (señal fuerte)  │    │
│  └──────────────────────┘      │ Capa 3: anti-avisos (regex/patrones)   │    │
│  ┌──────────────────────┐      │ Capa 4: deduplicación e idempotencia   │    │
│  │ getActiveNotifications│─────▶│ Capa 5: reglas del usuario y contexto  │    │
│  └──────────────────────┘      └───────────────┬────────────────────────┘    │
│                                                │ MessageEvent (limpio)      │
│                    ③ MOTOR DE ACCIONES ◀───────┘                             │
│    ┌────────────┐  ┌─────────────┐  ┌──────────────┐  ┌──────────────────┐   │
│    │ TTS / voz  │  │ Persistencia│  │ IA opcional  │  │ Acciones:        │   │
│    │ (cola +    │  │ Room +      │  │ (resumen,    │  │ responder por    │   │
│    │  ducking)  │  │ SQLCipher   │  │  clasificar, │  │ notificación,    │   │
│    │            │  │             │  │  traducir)   │  │ silenciar, etc.) │   │
│    └────────────┘  └──────┬──────┘  └──────┬───────┘  └──────────────────┘   │
│                           │                │                                 │
└───────────────────────────┼────────────────┼─────────────────────────────────┘
                            │ (opt-in)       │ (opt-in, solo texto ya filtrado)
                    ┌───────▼────────────────▼────────┐
                    │  BACKEND (Ktor/Node) + Postgres │
                    │  · panel web multi-dispositivo  │
                    │  · reglas OTA del filtro        │
                    │  · licencias / facturación      │
                    │  · webhooks B2B                 │
                    └─────────────────────────────────┘

  MÓDULO B2B (paralelo): WhatsApp Business Cloud API
  Meta ──webhook──▶ Backend ──▶ CRM / bot IA / agentes  (100% oficial, cero ban)
```

**Principio de diseño #1 — "*on-device first*":** el contenido de los mensajes **nunca sale del teléfono** salvo que el usuario lo active explícitamente. Es tu mejor argumento de venta, lo que Google espera de una app con acceso a notificaciones y lo que te blinda legalmente.

---

## 4. El problema real: filtrar el ruido (y por qué ahí está tu ventaja)

Lo difícil no es leer la notificación. Lo difícil es que **WhatsApp reutiliza el mismo canal y a veces el mismo paquete** para cosas que no son mensajes. Sin filtrado, el producto es inusable: te lee "Copia de seguridad completada al 100 %" cada noche y "3 mensajes nuevos" cada vez.

### Anatomía de una notificación de WhatsApp

```kotlin
// ✔ MENSAJE DE CHAT REAL
sbn.packageName            = "com.whatsapp"
notification.extras["android.template"] = "android.app.Notification$MessagingStyle"
notification.extras["android.conversationTitle"] = "Mamá"          // remitente o grupo
notification.extras["android.messages"] = [ {text, time, sender}, … ] // historial estructurado
notification.category      = CATEGORY_MESSAGE
notification.isOngoing      = false     // ← ongoing = progreso/backup, se descarta

// ✘ RUIDO A EXCLUIR
"Copia de seguridad completada"  · canal "backup"  · isOngoing = true
"Restaurando mensajes…"          · canal "media_upload"
"3 mensajes nuevos"              · FLAG_GROUP_SUMMARY (es el resumen del grupo, no un mensaje)
"Llamada perdida"                · category = CATEGORY_MISSING_CALL
"Código de verificación: 384910" · Android 15+ ya lo entrega redactado a apps no autorizadas
"Toca para ver el estado"        · category = CATEGORY_STATUS
```

### El motor de filtrado, en 5 capas

| Capa | Regla | Qué descarta |
|---|---|---|
| **0. Paquete** | Allowlist: `com.whatsapp`, `com.whatsapp.w4b`. (Opcional futuro: Telegram, SMS, Signal.) | Google Drive, WhatsApp Web, gestor de archivos, sistema |
| **1. Canal y flags** | Canal de conversación (`messages*`, `group_messages*`), `category == CATEGORY_MESSAGE`, `!isOngoing`, `!FLAG_GROUP_SUMMARY`, `visibility` del usuario | Backups, subida/descarga de multimedia, sincronización, llamadas, estados, resúmenes |
| **2. `MessagingStyle` (señal fuerte)** | Extraer `android.messages` → lista estructurada con remitente, hora y texto | Todo lo que no sea un mensaje de chat: es el filtro decisivo |
| **3. Anti-avisos (texto)** | Diccionario de patrones multilingüe: `copia de seguridad`, `backup`, `restaurando`, `descargando`, `sincronizando`, `está en línea`, `se conectó`, `mensajes nuevos`, `esperando`, `no se pudo`, `actualización`, `código de verificación`… | Versiones de WhatsApp que cambian formato, avisos del sistema, mensajes de servicio |
| **4. Deduplicación** | Huella `sha256(paquete + conversación + remitente + timestamp + texto)` con ventana TTL + `lastSeenTimestamp` por conversación | El mismo mensaje leído 3 veces (WhatsApp **reemite** la notificación completa cada vez que llega otro mensaje al mismo chat) |
| **5. Contexto y usuario** | Reglas propias: silenciar chats, horarios (*no molestar*), "solo si hay audífonos/Bluetooth", "no leer si estoy en llamada", "solo grupos", "solo si me mencionan", "solo contactos favoritos" | Ruido *para ti*, que es distinto para cada persona |

**Métricas de calidad del filtro (definir desde el día 1):**
- *Falsos positivos* (lee algo que no es mensaje): objetivo **< 1 %**.
- *Falsos negativos* (se pierde un mensaje real): objetivo **0 %** — es innegociable, un mensaje perdido destruye la confianza.
- Estrategia anti-falsos-negativos: cuando un evento no se puede clasificar, **se guarda en un "buzón de revisión"** (sin leerlo aún). Un panel interno te muestra los no clasificados y **actualizas las reglas OTA** sin publicar una nueva versión de la app. Esa tubería de aprendizaje, más un botón de *feedback* del usuario, es lo que hace que el filtro mejore con el tiempo y se vuelva difícil de copiar.

---

## 5. Lectura por voz: cómo se siente "profesional" y no "robot"

| Aspecto | Decisión técnica |
|---|---|
| Cola de audio | Un `AudioFocus` de tipo *transient + mayDuck*: **baja la música, habla, y devuelve el volumen**. Nunca dos mensajes superpuestos |
| Agrupación | 5 mensajes seguidos del mismo chat → un solo clip: *"Mamá escribió: recibí tu pedido; ¿me confirmas la hora?; y luego: ya estoy aquí"* |
| Plantilla natural | `"{Remitente}: {texto}"` / en grupos: `"En {grupo}, {remitente} dice: {texto}"` / configurable |
| Sanitización | URL → "enlace"; emojis → nombre o se omite; `jaja` → "jaja" (no deletrear); siglas y montos con formato local (S/ 120 → "ciento veinte soles") |
| Idiomas | Detección por mensaje (ES/EN/PT) y voz correspondiente; el usuario puede forzar idioma por chat |
| Voces | Base: TTS del sistema (gratis, offline, sin latencia). Premium: voces neuronales en la nube (Google Cloud TTS / Azure / ElevenLabs) — **es tu *add-on* de margen** |
| Nombres de contacto | Diccionario personalizado de pronunciación ("Núñez" → "Núñez" y no "N-U-ñ-e-z") |
| Manos libres | Activación por conexión de Bluetooth/audífonos, pantalla apagada, o "modo conducción" (botón grande + respuestas por voz) |
| Repetir / saltar | Gestos: doble toque en los audífonos = repetir; "siguiente" = saltar al próximo mensaje en cola |

**La función que cierra el círculo (Fase 2): responder por voz sin abrir la app.**
Las notificaciones de WhatsApp exponen la acción *Responder* con `RemoteInput`. Desde el listener se puede: dictar la respuesta (STT del sistema, offline) → mostrar confirmación ("¿Envío a Mamá: 'llego en 10 minutos'?") → disparar la acción de la notificación. **El mensaje sale por la app oficial de WhatsApp**: sigue sin ser automatización prohibida, sigue sin riesgo de ban, y es una función que casi ningún competidor ofrece bien.

---

## 6. Límites honestos (lo que debes saber antes de invertir)

1. **iOS: imposible.** El sistema no permite que una app lea las notificaciones de otra. No hay excepción, no hay atajo. → Producto **Android-first** (≈ 70 % del mercado en Perú y LATAM). A los clientes iOS se les ofrece el módulo B2B si tienen número business.
2. **No sirve para espiar a otros.** Solo funciona en el teléfono donde está instalada y con permiso explícito del dueño. Un producto de este tipo **no puede** promocionarse como vigilancia: además de ilegal (Ley 29733 en Perú, GDPR en la UE), Google lo elimina de Play y las tiendas de seguridad lo marcan como *stalkerware*. Ponlo por escrito en los Términos de Uso.
3. **Permiso restringido por Google (el mayor riesgo del proyecto).** "Acceso a notificaciones" es un permiso sensible: Play exige que sea **funcionalidad central** (lo es: leer en voz alta) y puede pedir justificación, política de privacidad, formulario *Data Safety* y hasta un video explicativo para el revisor. → **Plan A:** publicar como *lector por voz / accesibilidad*, con privacidad radical (todo local). **Plan B:** distribución directa (APK desde tu web), F-Droid y ventas B2B/enterprise (MDM), donde no dependes de la revisión de Google.
4. **Android 15+ redacta datos sensibles** (códigos OTP) para apps de terceros con acceso a notificaciones. No afecta a los mensajes normales, pero hay que probarlo.
5. **Fabricantes agresivos con la batería** (Xiaomi, Huawei, Samsung, Oppo): el servicio puede morir en segundo plano. Mitigación: *foreground service* con notificación persistente, `WorkManager` de respaldo y un **asistente de configuración por marca** que guía al usuario.
6. **WhatsApp cambia sus canales y formatos sin avisar.** Mitigación: reglas versionadas y actualizables OTA, telemetría de no clasificados (sin contenido), y las 5 capas (si una falla, las otras sostienen el filtro).

---

## 7. Roadmap de 90 días

| Fase | Tiempo | Entregable | Criterio de salida |
|---|---|---|---|
| **0. Prueba de concepto** | Semanas 1–2 | App mínima: captura + `MessagingStyle` + TTS, probada con una cuenta real | 100 mensajes reales leídos sin perder ninguno y sin leer ningún aviso |
| **1. MVP privado** | Semanas 3–6 | Filtro de 5 capas, panel de historial, reglas por chat, ajustes de voz, asistente de permisos | ≤ 1 % falsos positivos en 7 días de uso; crash-free > 99 % |
| **2. Beta cerrada (50 usuarios)** | Semanas 7–9 | Deduplicación con histórico, *buzón de revisión*, telemetría anónima, respuestas por voz | Retención D7 ≥ 40 %; latencia media < 1,5 s |
| **3. Monetización** | Semanas 10–12 | Free/Pro/Business, Play Billing, voces premium, panel web mínimo | > 50 suscriptores de pago; conversión free→Pro ≥ 3 % |
| **4. B2B (paralelo desde la semana 8)** | Trimestre 2 | Cloud API oficial + CRM + IA que resume y sugiere | 1 cliente empresarial piloto facturando |
| **5. Escala** | Trimestre 3 | Multi-app (Telegram/SMS), escritorio (Windows/macOS con notificaciones del sistema), white-label | 5 000 usuarios activos |

**Backlog priorizado del MVP (en orden):** ① captura fiable ② filtro anti-avisos ③ deduplicación ④ TTS con ducking ⑤ ajustes de voz e idioma ⑥ panel de mensajes ⑦ reglas por chat/horario ⑧ asistente de permisos y batería ⑨ exportación ⑩ feedback del filtro.

---

## 8. Modelo de negocio y precios (propuesta)

| Plan | Precio sugerido | Incluye |
|---|---|---|
| **Free** | S/ 0 | 1 chat activo, TTS del sistema, historial de 7 días, sin IA |
| **Pro** | **S/ 14.90 / USD 3.99 al mes** (o S/ 129/año) | Chats ilimitados, reglas y horarios, lectura con audífonos, respuestas por voz, historial ilimitado local, voces premium (bolsa mensual), multi-dispositivo |
| **Business** | **USD 8–12 por usuario/mes** | Panel web compartido, asignación de chats, resúmenes con IA, integraciones (CRM, Drive, Notion, webhooks), WhatsApp Cloud API oficial, SSO, SLA |
| **White-label** | USD 3 000–8 000 al año | Marca del cliente, despliegue con su firma, soporte prioritario |
| **Add-on** | Pago por uso | Voces neuronales y LLM (margen del 40–60 % sobre costo de proveedor) |

**Economía unitaria:** costo variable de un usuario Pro ≈ USD 0.30–0.80/mes (voz premium + IA puntual) → **margen bruto > 80 %** si el procesamiento base se queda en el teléfono.

**Diferenciación (por qué te compran a ti y no a la app genérica de "leer notificaciones"):** cero ruido (filtro de 5 capas), lectura natural agrupada y multilingüe, privacidad *on-device* verificable, respuestas por voz, y un módulo B2B oficial que los competidores no tienen.

---

## 9. Cumplimiento legal y de tienda (no negociable)

- **Consentimiento explícito** en pantalla, en español claro, antes de pedir el permiso del sistema.
- **Política de privacidad pública** y formulario **Data Safety**: declarar "acceso a notificaciones", finalidad, y que **el contenido se procesa en el dispositivo**.
- **Sin publicidad** basada en el contenido de notificaciones (prohibido por Play y destruye la confianza).
- **Términos de uso que prohíben** el uso para vigilar a terceros; verificación de que el instalador es el dueño del teléfono.
- **Cifrado** en reposo (SQLCipher) y en tránsito (TLS 1.3); retención configurable y borrado a un toque.
- **Perú:** Ley 29733 de Protección de Datos Personales y su reglamento vigente → registro del banco de datos si aplica, y consentimiento informado. **UE/Brasil:** GDPR/LGPD si vendes allí (derecho de acceso, supresión y portabilidad).

---

## 10. Riesgos y mitigación

| Riesgo | Probabilidad | Impacto | Mitigación |
|---|---|---|---|
| Rechazo en Play por el permiso de notificaciones | Media | Alto | Privacidad *on-device*, video para el revisor, plan B de distribución directa + enterprise |
| WhatsApp cambia el formato de las notificaciones | Alta (recurrente) | Medio | 5 capas independientes + reglas OTA + telemetría de no clasificados |
| Servicio matado por el fabricante del teléfono | Alta | Medio | Foreground service, asistente por marca, guías de batería |
| Copias de bajo costo (clonación) | Alta | Medio | Foso real: datos de entrenamiento del filtro, soporte, panel B2B, marca |
| Uso indebido como *spyware* por un cliente | Baja | Muy alto | Prohibición en ToS, controles de consentimiento, marca blanca auditada |

**KPIs de éxito a 12 meses:** 20 000 instalaciones, 3 000 activos mensuales, retención D30 ≥ 25 %, conversión free→Pro ≥ 4 %, NPS ≥ 45, tasa de falsos positivos < 1 %.

---

## 11. Mi recomendación en una frase

> **Construye la app Android de captura + lectura por voz con el filtro de 5 capas como corazón del producto, monetízala por suscripción, y ofrece el módulo de WhatsApp Business Cloud API como la puerta de entrada a los clientes empresariales. No toques las librerías no oficiales: cambian un mes de tiempo por el riesgo de perder el número del cliente para siempre.**

Siguiente paso concreto: puedo dejar armado en este repositorio el **esqueleto Android del MVP** (`NotificationListenerService` + motor de filtrado + cola de TTS + panel Compose) para que Fase 0 arranque esta semana.

> **Estado:** el esqueleto de la Fase 0 ya está en [`android/`](android/) — captura en tiempo real, filtro de 5 capas con tests JVM, cola TTS con *ducking*, historial Room, buzón de revisión y panel Compose. Ver [`android/README.md`](android/README.md) para compilarlo, los criterios de salida medibles y las decisiones técnicas.

---
*Documento preparado para el proyecto `voicebot`. Las cifras de costo y plazos son estimaciones de planificación, no presupuestos cerrados.*
