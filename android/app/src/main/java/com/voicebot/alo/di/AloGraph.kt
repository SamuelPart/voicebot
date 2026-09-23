package com.voicebot.alo.di

import android.content.Context
import com.voicebot.alo.core.filter.FilterRules
import com.voicebot.alo.core.filter.NotificationNormalizer
import com.voicebot.alo.core.log.EventLog
import com.voicebot.alo.core.settings.AloSettings
import com.voicebot.alo.data.MessageRepository
import com.voicebot.alo.data.db.AloDatabase
import com.voicebot.alo.service.ReplyDispatcher
import com.voicebot.alo.voice.VoiceAnnouncer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Inyección de dependencias manual (sin Hilt): un grafo por proceso, creado en
 * [com.voicebot.alo.AloApp]. Si el proyecto crece, este es el único archivo que cambia.
 */
class AloGraph(context: Context) {

    private val appContext = context.applicationContext
    private val scopeJob = SupervisorJob()
    val appScope: CoroutineScope = CoroutineScope(scopeJob + Dispatchers.Default)

    val settings = AloSettings(appContext)
    val eventLog = EventLog()
    val rules = FilterRules()
    val replyDispatcher = ReplyDispatcher()

    private val dao = AloDatabase.get(appContext).dao()
    val repository = MessageRepository(dao = dao, scope = appScope)

    val announcer: VoiceAnnouncer by lazy {
        VoiceAnnouncer(
            context = appContext,
            scope = appScope,
            languageTag = settings.state.value.languageTag,
        ).also { it.start() }
    }

    /**
     * Un normalizador por evento: así siempre usa las reglas vigentes del usuario
     * (horarios, chats silenciados) sin necesidad de reiniciar el servicio.
     *
     * En "modo prueba" (solo debug) se añade `com.android.shell` al allowlist para poder
     * validar el pipeline completo con `adb shell cmd notification post`.
     */
    fun normalizer(): NotificationNormalizer {
        val effectiveRules = if (settings.state.value.testMode) {
            rules.copy(allowedPackages = rules.allowedPackages + TEST_PACKAGE)
        } else {
            rules
        }
        return NotificationNormalizer(
            rules = effectiveRules,
            speakingPolicy = settings.speakingPolicy(),
            clock = System::currentTimeMillis,
        )
    }

    private companion object {
        const val TEST_PACKAGE = "com.android.shell"
    }

    fun shutdown() {
        runCatching { announcer.shutdown() }
        scopeJob.cancel()
    }
}
