package com.voicebot.alo.core.filter

import com.voicebot.alo.core.model.FilterResult
import com.voicebot.alo.core.model.MessageEvent
import com.voicebot.alo.core.model.WhatsappPackages
import com.voicebot.alo.core.util.TextUtils

/**
 * MOTOR DE FILTRADO (el corazón del producto).
 *
 * Principio rector: **nunca perder un mensaje real** (falso negativo = 0) y mantener
 * los falsos positivos por debajo del 1 %. Por eso el pipeline es asimétrico:
 *
 *  - Señal CLARA de "no es un chat"        -> DISCARD (barato, reversible en logs)
 *  - Señal FUERTE de "es un mensaje"       -> READ
 *  - Ambigüedad (no sabemos qué es)        -> NEEDS REVIEW (se guarda, NO se lee)
 *
 * Capas:
 *   0. Paquete permitido (WhatsApp personal / Business)
 *   1. Señales duras de no-conversación: ongoing, resumen de grupo, llamadas, estados,
 *      canales de backup/sincronización/multimedia
 *   2. MessagingStyle: mensajes estructurados con remitente y hora (señal fuerte)
 *   3. Patrones de aviso: SOLO se aplican cuando la notificación no trae estructura
 *      (los avisos de servicio son texto plano; un mensaje real siempre trae MessagingStyle).
 *      Así un mensaje legítimo que "suene a aviso" nunca se pierde.
 *   4. Deduplicación: WhatsApp reemite la notificación completa en cada mensaje nuevo
 *   5. Reglas del usuario: horarios, chats silenciados, "solo con audífonos", en llamada
 *
 * Además se descartan los mensajes que el propio usuario envía (aparecen en el estilo con el
 * remitente del dueño del teléfono o sin remitente), para no leerte tus propias respuestas.
 *
 * La clase es PURA (sin Android): se testea en la JVM con JUnit (ver NotificationNormalizerTest).
 */
class NotificationNormalizer(
    private val rules: FilterRules = FilterRules(),
    private val speakingPolicy: SpeakingPolicy = SpeakingPolicy { _, _, _ -> true },
    private val clock: () -> Long = System::currentTimeMillis,
) {

    /**
     * @param lastSeen devuelve el timestamp del último mensaje ya procesado de esa conversación.
     * @param seenMarker indica si la huella ya fue procesada (deduplicación persistente).
     * @param device contexto del teléfono para la capa 5.
     */
    fun normalize(
        raw: RawNotification,
        lastSeen: (chatId: String) -> Long = { 0L },
        seenMarker: (dedupKey: String) -> Boolean = { false },
        device: DeviceContextSnapshot = DeviceContextSnapshot.unknown(),
        now: Long = clock(),
        hourOfDay: Int = -1,
    ): FilterResult {

        val rawText = raw.text ?: raw.title

        // ── Capa 0: paquete permitido ────────────────────────────────────────────
        if (raw.pkg !in rules.allowedPackages) {
            return FilterResult.Discarded(0, "paquete no permitido: ${raw.pkg}", TextUtils.preview(rawText))
        }

        // ── Capa 1: señales duras de no-conversación ─────────────────────────────
        if (raw.isOngoing) return discard(1, "notificación en curso (isOngoing): progreso/backup")
        if (raw.isGroupSummary) return discard(1, "resumen de grupo (FLAG_GROUP_SUMMARY), no es un mensaje")

        val channel = TextUtils.normalize(raw.channelId)
        val channelMarker = rules.noiseChannelMarkers.firstOrNull { channel.contains(it) }
        if (channelMarker != null && raw.messages.isEmpty()) {
            return discard(1, "canal no conversacional (\"$channelMarker\")")
        }

        val category = TextUtils.normalize(raw.category)
        val categoryMarker = rules.noiseCategories.firstOrNull { category.endsWith(it) }
        if (categoryMarker != null && raw.messages.isEmpty()) {
            return discard(1, "categoría no conversacional (\"$categoryMarker\")")
        }
        if (category.endsWith("call") || category.endsWith("call_missed") || category.contains("missed")) {
            return discard(1, "llamada (no es un mensaje)", TextUtils.preview(rawText))
        }

        // ── Capa 2: mensajes estructurados (MessagingStyle) ──────────────────────
        //
        // Regla de oro del filtro: un mensaje real de WhatsApp SIEMPRE trae MessagingStyle
        // (remitente + hora estructurados). Los avisos de servicio, en cambio, son texto plano.
        // Por eso los patrones de texto solo se aplican cuando NO hay estructura: así un mensaje
        // legítimo que "suene a aviso" (p. ej. "¿ya hiciste la copia de seguridad?") nunca se pierde.
        if (raw.messages.isEmpty()) {
            val noiseHit = TextUtils.matchesAny(rawText, rules.noiseTextPatterns)
                ?: TextUtils.matchesAny(rawText, FilterRules.systemInChatPatterns)
            if (noiseHit != null) {
                return FilterResult.Discarded(
                    layer = 3,
                    reason = "aviso de servicio (patrón \"${noiseHit.pattern}\")",
                    preview = TextUtils.preview(rawText),
                )
            }
            // No sabemos qué es: al buzón de revisión, SIN leerlo. Nunca se descarta en silencio.
            return FilterResult.NeedsReview(
                layer = 2,
                reason = "sin MessagingStyle: formato nuevo o notificación sin estructura",
                preview = TextUtils.preview(rawText),
            )
        }

        val chatTitle = (raw.conversationTitle ?: raw.title)?.toString()?.trim().orEmpty()
        val chatId = buildChatId(raw, chatTitle)
        val group = raw.isGroupConversation

        // Solo los mensajes NUEVOS respecto de lo ya visto en ese chat,
        // aunque la notificación traiga el historial completo (la limitación #1 de WhatsApp).
        val since = lastSeen(chatId)
        val candidates = raw.messages
            .filter { (it.text?.isNotBlank() == true) && it.timestamp > since }
            .takeLast(rules.maxMessagesPerNotification)

        if (candidates.isEmpty()) {
            return discard(4, "reemisión sin mensajes nuevos", TextUtils.preview(rawText))
        }

        val fresh = ArrayList<RawNotification.StyleMessage>(candidates.size)
        val skippedNoise = ArrayList<String>()
        val selfName = TextUtils.normalize(raw.userName)
        for (m in candidates) {
            if (TextUtils.isOnlySymbols(m.text.orEmpty())) {
                skippedNoise += "solo emojis"
                continue
            }
            // Mensajes enviados por el propio usuario: no se leen (los escribiste tú).
            if (selfName.isNotEmpty() && TextUtils.normalize(m.sender) == selfName) {
                skippedNoise += "mensaje propio"
                continue
            }
            // Capa 3 (dentro del chat): avisos que WhatsApp dibuja como mensajes de sistema
            // ("Los mensajes están cifrados de extremo a extremo", "cambió el asunto del grupo").
            val systemNotice = TextUtils.matchesAny(m.text, FilterRules.systemInChatPatterns)
            if (systemNotice != null) {
                skippedNoise += systemNotice.pattern
                continue
            }
            fresh += m
        }

        if (fresh.isEmpty()) {
            return discard(3, "todos los textos son avisos del sistema (${skippedNoise.joinToString(", ")})")
        }

        // ── Capa 4: deduplicación ────────────────────────────────────────────────
        val events = ArrayList<MessageEvent>(fresh.size)
        val dupes = ArrayList<String>()
        for (m in fresh) {
            val dedupKey = TextUtils.sha256(raw.pkg, chatId, m.sender, m.timestamp.toString(), m.text)
            if (seenMarker(dedupKey)) {
                dupes += dedupKey.take(8)
                continue
            }
            events += MessageEvent(
                id = dedupKey,
                pkg = raw.pkg,
                chatId = chatId,
                chatTitle = chatTitle.ifEmpty { m.sender.orEmpty().ifEmpty { "WhatsApp" } },
                isGroup = group,
                // Se conserva el remitente tal cual: si WhatsApp no lo informa (mensaje del
                // propio usuario), la frase hablada se vuelve neutra en vez de atribuirlo mal.
                sender = m.sender,
                text = m.text.orEmpty().trim(),
                timestamp = m.timestamp,
                dedupKey = dedupKey,
                sbnKey = raw.sbnKey,
                replyable = raw.hasRemoteInput,
                source = MessageEvent.Source.LIVE,
            )
        }

        if (events.isEmpty()) {
            return discard(4, "duplicado exacto (huellas: ${dupes.joinToString(", ")})")
        }

        // ── Capa 5: reglas del usuario deciden solo si se HABLA (el mensaje ya está capturado) ──
        val hour = if (hourOfDay >= 0) hourOfDay else defaultHour(now)
        val speak = events.any { speakingPolicy.shouldSpeak(it, device, hour) }

        return FilterResult.Read(events = events, speak = speak)
    }

    private fun discard(layer: Int, reason: String, preview: String? = null) =
        FilterResult.Discarded(layer = layer, reason = reason, preview = preview)

    private fun buildChatId(raw: RawNotification, chatTitle: String): String =
        TextUtils.sha256(raw.pkg, chatTitle.ifEmpty { raw.sbnKey }, if (raw.isGroupConversation) "group" else "dm")

    private fun defaultHour(now: Long): Int =
        ((now / 3_600_000L) % 24).toInt() // UTC; el servicio inyecta la hora local real
}
