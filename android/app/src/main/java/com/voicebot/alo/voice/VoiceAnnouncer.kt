package com.voicebot.alo.voice

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.voicebot.alo.core.model.FilterResult
import com.voicebot.alo.core.model.MessageEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Une el resultado del filtro con la voz:
 * FilterResult.Read -> frase natural -> cola de TTS (con foco de audio y vibración opcional).
 */
class VoiceAnnouncer(
    context: Context,
    private val scope: CoroutineScope,
    languageTag: String? = "es-PE",
) {

    private val appContext = context.applicationContext
    private val focus = AudioFocusHelper(appContext)
    private val engine = TextToSpeechEngine(appContext, languageTag)
    private val builder = SpeechTextBuilder()

    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking.asStateFlow()

    private val _queued = MutableStateFlow(0)
    val queued: StateFlow<Int> = _queued.asStateFlow()

    private val queue = VoiceQueue(
        engine = engine,
        scope = scope,
        onSpeakingChanged = { speaking -> _speaking.value = speaking },
    )

    fun start() {
        queue.start()
    }

    /** Punto de entrada: solo los resultados Read con `speak = true` llegan al audio. */
    fun announce(result: FilterResult.Read) {
        if (!result.speak) return
        val item = builder.build(result.events) ?: return
        focus.acquire()
        queue.enqueue(item)
        _queued.value = _queued.value + 1
    }

    /** Repetir desde la UI (botón 🔊 de cada mensaje). */
    fun replay(event: MessageEvent) {
        val item = builder.build(listOf(event)) ?: return
        focus.acquire()
        queue.enqueue(item)
    }

    /** Leer varios mensajes como un solo clip (función "leer todo"). */
    fun replayAll(events: List<MessageEvent>) {
        val item = builder.build(events) ?: return
        focus.acquire()
        queue.enqueue(item)
    }

    fun stop() {
        queue.stopAndClear()
        _queued.value = 0
        _speaking.value = false
    }

    fun shutdown() {
        queue.shutdown()
        engine.shutdown()
        focus.release()
    }

    fun vibrate(pattern: Long = 55) {
        runCatching {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(pattern, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(pattern)
                }
            }
        }.onFailure { Log.w(TAG, "Sin vibración disponible", it) }
    }

    companion object {
        private const val TAG = "Alo/Voice"
    }
}
