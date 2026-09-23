package com.voicebot.alo

import android.app.Application
import com.voicebot.alo.di.AloGraph
import com.voicebot.alo.service.ListenerWatchdogWorker

class AloApp : Application() {

    val graph: AloGraph by lazy { AloGraph(this) }

    override fun onCreate() {
        super.onCreate()
        if (graph.settings.state.value.protectionEnabled) {
            ListenerWatchdogWorker.schedule(this)
        }
    }
}
