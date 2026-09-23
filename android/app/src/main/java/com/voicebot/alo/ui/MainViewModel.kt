package com.voicebot.alo.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voicebot.alo.AloApp
import com.voicebot.alo.core.filter.DeviceContextSnapshot
import com.voicebot.alo.core.model.FilterResult
import com.voicebot.alo.core.model.MessageEvent
import com.voicebot.alo.core.model.WhatsappPackages
import com.voicebot.alo.core.settings.AloSettings
import com.voicebot.alo.core.util.DeviceContext
import com.voicebot.alo.core.util.TextUtils
import com.voicebot.alo.data.db.DroppedEntity
import com.voicebot.alo.data.db.MessageEntity
import com.voicebot.alo.data.toEvent
import com.voicebot.alo.service.WaListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val graph = (application as AloApp).graph

    val settings: StateFlow<AloSettings.Snapshot> = graph.settings.state
    val logEntries = graph.eventLog.entries
    val stats = graph.eventLog.stats
    val speaking = graph.announcer.speaking

    val messages: StateFlow<List<MessageEntity>> = graph.repository
        .observeRecent(120)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val reviewQueue: StateFlow<List<DroppedEntity>> = graph.repository
        .observeReviewQueue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val discardedCount: StateFlow<Int> = graph.repository
        .observeDiscardedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    private val _banner = MutableStateFlow<String?>(null)
    val banner: StateFlow<String?> = _banner.asStateFlow()

    init {
        refreshPermission()
    }

    // ── Permisos y sistema ──────────────────────────────────────────────────────

    fun refreshPermission() {
        val ctx = getApplication<Application>()
        val enabled = NotificationManagerCompat.getEnabledListenerPackages(ctx)
            .contains(ctx.packageName)
        _permissionGranted.value = enabled
    }

    fun listenerSettingsIntent() = android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    fun batterySettingsIntent() = android.content.Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    fun isListenerComponentEnabled(): Boolean {
        val ctx = getApplication<Application>()
        val component = ComponentName(ctx, "com.voicebot.alo.service.WaListenerService")
        val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners")
        return flat?.contains(component.flattenToString()) == true
    }

    // ── Acciones de lectura ─────────────────────────────────────────────────────

    fun replay(entity: MessageEntity) = graph.announcer.replay(entity.toEvent())

    fun replayAllRecent() {
        val pending = messages.value.take(5).reversed().map { it.toEvent() }
        graph.announcer.replayAll(pending)
    }

    fun stopReading() = graph.announcer.stop()

    fun markReviewed(id: Long) = graph.repository.markReviewed(id)

    fun clearHistory() {
        graph.announcer.stop()
        graph.repository.clearAll()
        graph.eventLog.clear()
        _banner.value = "Historial borrado del dispositivo"
    }

    fun consumeBanner() {
        _banner.value = null
    }

    /** Modo demo de la Fase 0: valida voz y UI sin esperar un mensaje real. */
    fun simulateMessage() {
        val now = System.currentTimeMillis()
        val event = MessageEvent(
            id = TextUtils.sha256("demo", now.toString()),
            pkg = WhatsappPackages.PERSONAL,
            chatId = "demo-chat",
            chatTitle = "Mamá",
            isGroup = false,
            sender = "Mamá",
            text = "Hijo, ya está lista la cena. ¿Vienes a las 8?",
            timestamp = now,
            dedupKey = TextUtils.sha256("demo-key", now.toString()),
            sbnKey = "demo",
            replyable = true,
            source = MessageEvent.Source.LIVE,
        )
        viewModelScope.launch {
            graph.repository.persist(listOf(event), wasReadAloud = true)
            graph.eventLog.record(FilterResult.Read(listOf(event), speak = true))
            graph.announcer.announce(FilterResult.Read(listOf(event), speak = true))
        }
    }

    /** Modo demo: comprueba que el ruido se descarta (copia de seguridad, resumen, llamada). */
    fun simulateNoise() {
        val ahora = System.currentTimeMillis()
        viewModelScope.launch {
            listOf(
                FilterResult.Discarded(1, "notificación en curso (isOngoing): progreso/backup", "Copia de seguridad completada"),
                FilterResult.Discarded(1, "resumen de grupo (FLAG_GROUP_SUMMARY), no es un mensaje", "3 mensajes nuevos"),
                FilterResult.Discarded(1, "llamada (no es un mensaje)", "Llamada perdida de Carlos"),
                FilterResult.NeedsReview(2, "sin MessagingStyle ni acción de respuesta", "Nuevo formato desconocido"),
            ).forEach { result ->
                graph.repository.recordFilterOutcome(result, ahora)
                graph.eventLog.record(result, ahora)
            }
        }
    }

    // ── Ajustes ────────────────────────────────────────────────────────────────

    fun setVoiceEnabled(enabled: Boolean) {
        graph.settings.setVoiceEnabled(enabled)
        if (!enabled) graph.announcer.stop()
    }

    fun setLanguageTag(tag: String) = graph.settings.setLanguageTag(tag)

    fun setReadGroups(enabled: Boolean) = graph.settings.setReadGroups(enabled)

    fun setOnlyWithHeadphones(enabled: Boolean) = graph.settings.setOnlyWithHeadphones(enabled)

    fun setPauseDuringCalls(enabled: Boolean) = graph.settings.setPauseDuringCalls(enabled)

    fun setVibrateInsteadOfSpeak(enabled: Boolean) = graph.settings.setVibrateInsteadOfSpeak(enabled)

    fun setRetentionDays(days: Int) = graph.settings.setRetentionDays(days)

    fun setQuietHours(start: Int?, end: Int?) = graph.settings.setQuietHours(start, end)

    fun setAppearanceMode(mode: AloSettings.AppearanceMode) = graph.settings.setAppearanceMode(mode)

    fun muteChat(chatId: String, muted: Boolean) = graph.settings.setMuted(chatId, muted)

    fun deviceSnapshot(): DeviceContextSnapshot {
        val ctx: Context = getApplication()
        val snap = DeviceContext.snapshot(ctx)
        return DeviceContextSnapshot(snap.headphonesConnected, snap.inCall, snap.screenOn)
    }
}
