package com.voicebot.alo

import android.app.Application
import com.voicebot.alo.di.AloGraph

class AloApp : Application() {

    val graph: AloGraph by lazy { AloGraph(this) }
}
