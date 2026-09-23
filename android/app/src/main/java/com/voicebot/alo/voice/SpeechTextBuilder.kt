package com.voicebot.alo.voice

import com.voicebot.alo.core.model.MessageEvent
import com.voicebot.alo.core.util.TextUtils

/**
 * Convierte eventos en frases naturales.
 *   individual: "Mensaje de Mamá: ya salí de la oficina"
 *   grupo:      "En el grupo Equipo Ventas, Luis dice: el cliente firma mañana"
 *   agrupado:   "Mamá escribió: recibí tu pedido; y también: ya estoy aquí"
 */
class SpeechTextBuilder(
    private val maxCharsPerMessage: Int = 320,
    private val maxCharsPerUtterance: Int = 900,
    private val template: Template = Template.DEFAULT,
) {
    enum class Template { DEFAULT, SHORT }
    // Template.SHORT es un gancho de Fase 1 (voz premium / lectura literal).

    fun build(events: List<MessageEvent>): SpeechItem? {
        if (events.isEmpty()) return null
        val first = events.first()

        val body = if (events.size == 1) {
            singleSentence(first)
        } else {
            groupedSentence(events)
        }

        val speakable = truncate(body, maxCharsPerUtterance)
        if (speakable.isBlank()) return null
        return SpeechItem(
            utteranceId = first.dedupKey,
            speakable = speakable,
            source = first,
        )
    }

    private fun singleSentence(e: MessageEvent): String {
        val text = cleanText(e.text)
        val who = e.sender?.takeIf { it.isNotBlank() }
        return when {
            e.isGroup && who != null -> "En el grupo ${e.chatTitle}, $who dice: $text"
            e.isGroup -> "En el grupo ${e.chatTitle}: $text"
            // Sin remitente conocido no se atribuye el mensaje al contacto (podría ser tuyo).
            who != null -> "Mensaje de $who: $text"
            else -> "Nuevo mensaje en el chat de ${e.chatTitle}: $text"
        }
    }

    private fun groupedSentence(events: List<MessageEvent>): String {
        val first = events.first()
        val who = first.sender?.takeIf { it.isNotBlank() }
        val prefix = when {
            first.isGroup && who != null -> "En el grupo ${first.chatTitle}, $who escribió: "
            first.isGroup -> "En el grupo ${first.chatTitle}: "
            who != null -> "$who escribió: "
            else -> "En el chat de ${first.chatTitle}: "
        }
        val parts = events.mapIndexed { index, e ->
            val clean = cleanText(e.text)
            if (index == events.lastIndex) "y también: $clean" else clean
        }
        return prefix + parts.joinToString("; ")
    }

    private fun cleanText(text: String): String = truncate(TextUtils.forSpeech(text), maxCharsPerMessage)

    private fun truncate(text: String, max: Int): String =
        if (text.length <= max) text else text.take(max - 1).trimEnd() + "…"
}
