package com.voicebot.alo.voice

import com.voicebot.alo.core.model.MessageEvent

/** Frase lista para sintetizar (ya con la plantilla natural aplicada). */
data class SpeechItem(
    val utteranceId: String,
    val speakable: String,
    val source: MessageEvent,
)
