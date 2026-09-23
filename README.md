# Aló — lector inteligente de WhatsApp en tiempo real

App Android que **captura solo los mensajes de chat de WhatsApp**, los **lee en voz alta** de
forma natural y los **guarda en el propio teléfono**, filtrando todo el ruido (avisos de copia de
seguridad, resúmenes de “mensajes nuevos”, llamadas perdidas, estados, códigos de verificación).

## Contenido del repositorio

| Ruta | Qué es |
|---|---|
| [`PROPUESTA.md`](PROPUESTA.md) | Propuesta técnica y de producto completa: arquitectura, las 3 vías para leer WhatsApp (y por qué solo 2 son viables), filtro de 5 capas, motor de voz, modelo de negocio, roadmap de 90 días, riesgos y cumplimiento legal/Play Store. |
| [`mockup/index.html`](mockup/index.html) | Mockup funcional: simula notificaciones reales y de ruido, muestra el registro del filtro en vivo y lee en voz alta con la voz del navegador. |
| [`android/`](android/) | **Esqueleto Android del MVP (Fase 0)**: Kotlin + Compose, `NotificationListenerService`, motor de filtrado de 5 capas, cola de TTS con *ducking*, historial Room y buzón de revisión, con tests JVM. Ver [`android/README.md`](android/README.md). |

## La idea en una frase

**Construir sobre el permiso “Acceso a notificaciones” de Android** (tiempo real, sin credenciales,
sin riesgo de ban) con un **filtro anti-ruido** como corazón del producto, y ofrecer el módulo
**WhatsApp Business Cloud API** oficial como puerta de entrada a los clientes empresariales.
Las librerías no oficiales (Baileys, whatsapp-web.js, Evolution, WAHA, Z-API) quedan descartadas:
cambian un mes de tiempo por el riesgo de perder el número del cliente para siempre.

## Estado

- [x] Propuesta de producto y arquitectura
- [x] Mockup funcional (demo del filtro + voz en el navegador)
- [x] Esqueleto Android: captura, filtro de 5 capas, TTS, historial, buzón de revisión, ajustes
- [ ] Fase 1: foreground service, reglas OTA, SQLCipher, voces premium
- [ ] Fase 2: respuesta por voz, multi-app, panel web y módulo B2B (Cloud API oficial)
