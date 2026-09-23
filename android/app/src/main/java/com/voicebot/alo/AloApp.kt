package com.voicebot.alo

import android.app.Application
import com.voicebot.alo.di.AloGraph
import com.voicebot.alo.service.ListenerWatchdogWorker

class AloApp : Application() {

    val graph: AloGraph by lazy { AloGraph(this) }

    override fun onCreate() {
        super.onCreate()
        // Prepara el TTS al iniciar el proceso. Si el primer evento llega con la pantalla apagada,
        // la voz ya está inicializada y no depende de que el usuario abra una pantalla.
        graph.announcer
        val settings = graph.settings.state.value
        if (settings.appEnabled && settings.protectionEnabled) {
            ListenerWatchdogWorker.schedule(this)
        }
    }
}
