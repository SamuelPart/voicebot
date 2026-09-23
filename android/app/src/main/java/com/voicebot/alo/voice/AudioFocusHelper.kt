package com.voicebot.alo.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Pide foco de audio transitorio con *ducking*: baja la música, habla y devuelve el volumen.
 * Esto es lo que separa una app "que lee notificaciones" de un producto cuidado.
 */
class AudioFocusHelper(context: Context) {

    private val audioManager = context.applicationContext
        .getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var request: AudioFocusRequest? = null

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildRequest(): AudioFocusRequest =
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setWillPauseWhenDucked(false) // queremos hablar POR ENCIMA, no pausar el audio
            .setOnAudioFocusChangeListener { /* el ducking lo gestiona el sistema */ }
            .build()

    fun acquire(): Boolean = runCatching {
        val req = request ?: buildRequest().also { request = it }
        audioManager.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }.getOrDefault(false)

    fun release() {
        runCatching { request?.let { audioManager.abandonAudioFocusRequest(it) } }
    }
}
