package com.voicebot.alo.voice

import android.content.Context
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

/**
 * Envoltura del TTS del sistema en modo *suspend*:
 * `speak()` no retorna hasta que el sintetizador termina, así la cola nunca se pisa.
 */
class TextToSpeechEngine(
    context: Context,
    preferredLanguageTag: String? = null,
) {

    private val appContext = context.applicationContext
    private val preferredLocale: Locale =
        preferredLanguageTag?.takeIf { it.isNotBlank() }?.let(Locale::forLanguageTag) ?: Locale.getDefault()

    private val mutex = Mutex()
    private val ready = CompletableDeferred<Boolean>()
    private var tts: TextToSpeech? = null
    private var pending: CompletableDeferred<Unit>? = null

    @Volatile
    var isReady: Boolean = false
        private set

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit
        override fun onDone(utteranceId: String?) = completePending()
        override fun onStop(utteranceId: String?, interrupted: Boolean) = completePending()

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) = completePending()

        override fun onError(utteranceId: String?, errorCode: Int) = completePending()
    }

    init {
        // TextToSpeech solo puede crearse en un hilo con Looper (el servicio procesa en un hilo
        // propio sin Looper), así que la construcción se traslada al hilo principal.
        if (Looper.myLooper() == null) {
            Handler(Looper.getMainLooper()).post { create() }
        } else {
            create()
        }
    }

    private fun create() {
        tts = TextToSpeech(appContext) { status -> onInit(status) }
    }

    private fun onInit(status: Int) {
        val engine = tts
        if (engine == null) {
            // El motor avisó antes de terminar la construcción: reintentar en el próximo ciclo.
            Handler(Looper.getMainLooper()).post { onInit(status) }
            return
        }
        val ok = status == TextToSpeech.SUCCESS
        if (ok) configure(engine)
        isReady = ok
        if (!ready.isCompleted) ready.complete(ok)
    }

    private fun configure(engine: TextToSpeech) {
        runCatching {
            engine.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            engine.setOnUtteranceProgressListener(progressListener)
            applyLanguage(engine, preferredLocale)
            engine.setSpeechRate(1.03f)
            engine.setPitch(1.0f)
        }.onFailure { Log.w(TAG, "No se pudo configurar el TTS", it) }
    }

    /** Prueba el idioma preferido y degrada con elegancia (es-PE -> es -> idioma del sistema). */
    private fun applyLanguage(engine: TextToSpeech, locale: Locale) {
        val candidates = listOf(locale, Locale.forLanguageTag(locale.language), Locale.getDefault())
        for (candidate in candidates) {
            val result = engine.setLanguage(candidate)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                pickVoice(engine, candidate)
                return
            }
        }
        Log.w(TAG, "Ningún idioma soportado, se usa el del sistema: ${Locale.getDefault()}")
        runCatching { engine.setLanguage(Locale.getDefault()) }
    }

    /** Prefiere una voz offline de calidad media/alta (sin latencia de red). */
    private fun pickVoice(engine: TextToSpeech, locale: Locale) {
        val voices = runCatching { engine.voices }.getOrNull() ?: return
        val best: Voice? = voices
            .filter { it.locale.language == locale.language && !it.isNetworkConnectionRequired }
            .filter { it.quality >= Voice.QUALITY_NORMAL }
            .maxByOrNull { it.quality }
        if (best != null) runCatching { engine.voice = best }
    }

    /**
     * Sintetiza y espera a que termine. Devuelve false si el motor no está listo o falló.
     * Timeout de seguridad para que la cola nunca quede bloqueada.
     */
    suspend fun speak(item: SpeechItem, timeoutMs: Long = 30_000): Boolean = mutex.withLock {
        if (!ready.await()) return@withLock false
        val engine = tts ?: return@withLock false

        val done = CompletableDeferred<Unit>()
        pending = done

        val result = runCatching {
            engine.speak(item.speakable, TextToSpeech.QUEUE_FLUSH, null, item.utteranceId)
        }.getOrDefault(TextToSpeech.ERROR)

        if (result != TextToSpeech.SUCCESS) {
            pending = null
            return@withLock false
        }

        // Si el motor se cuelga, la cola no se queda bloqueada para siempre.
        withTimeoutOrNull(timeoutMs) { done.await() }
        pending = null
        true
    }

    fun stop() {
        runCatching { tts?.stop() }
        completePending()
    }

    fun shutdown() {
        val engine = tts
        tts = null
        isReady = false
        completePending()
        val close = Runnable {
            runCatching {
                engine?.stop()
                engine?.shutdown()
            }
        }
        if (Looper.myLooper() == null) {
            Handler(Looper.getMainLooper()).post(close)
        } else {
            close.run()
        }
    }

    private fun completePending() {
        pending?.let { if (!it.isCompleted) it.complete(Unit) }
    }

    companion object {
        private const val TAG = "Alo/TTS"
    }
}
