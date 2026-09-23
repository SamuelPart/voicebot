package com.voicebot.alo.voice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Cola de lectura: garantiza una sola voz a la vez, en orden y sin cortes.
 * Si el usuario recibe 10 mensajes seguidos, se leen uno tras otro.
 */
class VoiceQueue(
    private val engine: TextToSpeechEngine,
    private val scope: CoroutineScope,
    private val gapBetweenItemsMs: Long = 260,
    private val onSpeakingChanged: (Boolean) -> Unit = {},
) {

    private val channel = Channel<SpeechItem>(Channel.UNLIMITED)
    private var worker: Job? = null

    fun start() {
        if (worker?.isActive == true) return
        worker = scope.launch {
            for (item in channel) {
                onSpeakingChanged(true)
                runCatching { engine.speak(item) }
                onSpeakingChanged(false)
                if (gapBetweenItemsMs > 0) delay(gapBetweenItemsMs)
            }
        }
    }

    fun enqueue(item: SpeechItem) {
        channel.trySend(item)
    }

    fun enqueueAll(items: List<SpeechItem>) {
        items.forEach { channel.trySend(it) }
    }

    /** Silencio inmediato + cola vacía (botón "detener lectura"). */
    fun stopAndClear() {
        engine.stop()
        while (channel.tryReceive().isSuccess) {
            // drena la cola
        }
    }

    fun shutdown() {
        stopAndClear()
        channel.close()
        worker = null
    }
}
