package com.voicebot.alo.core.filter

import com.voicebot.alo.core.model.MessageEvent

/** Foto del contexto del teléfono que la capa 5 necesita (pura, testeable). */
data class DeviceContextSnapshot(
    val headphonesConnected: Boolean = false,
    val inCall: Boolean = false,
    val screenOn: Boolean = true,
) {
    companion object {
        fun unknown() = DeviceContextSnapshot()
    }
}

/**
 * CAPA 5 — Reglas del usuario.
 * No decide si un mensaje es real (eso ya lo hicieron las capas 0-4): decide
 * únicamente si corresponde LEERLO EN VOZ ALTA. El mensaje se captura siempre.
 */
fun interface SpeakingPolicy {
    fun shouldSpeak(event: MessageEvent, device: DeviceContextSnapshot, hourOfDay: Int): Boolean
}

/** Implementación configurable desde los ajustes de la app. */
class RulesBasedSpeakingPolicy(
    private val voiceEnabled: Boolean = true,
    private val mutedChatIds: Set<String> = emptySet(),
    private val readGroups: Boolean = true,
    private val onlyWithHeadphones: Boolean = false,
    private val pauseDuringCalls: Boolean = true,
    private val quietHoursStart: Int? = null,
    private val quietHoursEnd: Int? = null,
) : SpeakingPolicy {

    override fun shouldSpeak(event: MessageEvent, device: DeviceContextSnapshot, hourOfDay: Int): Boolean {
        if (!voiceEnabled) return false
        if (event.chatId in mutedChatIds) return false
        if (!readGroups && event.isGroup) return false
        if (pauseDuringCalls && device.inCall) return false
        if (onlyWithHeadphones && !device.headphonesConnected) return false
        if (isQuietHour(hourOfDay)) return false
        return true
    }

    private fun isQuietHour(hour: Int): Boolean {
        val start = quietHoursStart ?: return false
        val end = quietHoursEnd ?: return false
        if (start == end) return false
        return if (start < end) hour in start until end else hour >= start || hour < end
    }
}
