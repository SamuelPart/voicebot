package com.voicebot.alo.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.voicebot.alo.AloApp
import com.voicebot.alo.BuildConfig
import com.voicebot.alo.core.filter.DeviceContextSnapshot
import com.voicebot.alo.core.filter.NotificationSnapshot
import com.voicebot.alo.core.filter.RawNotification
import com.voicebot.alo.core.model.FilterResult
import com.voicebot.alo.core.model.MessageEvent
import com.voicebot.alo.core.model.WhatsappPackages
import com.voicebot.alo.core.util.DeviceContext
import com.voicebot.alo.di.AloGraph
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalTime
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Punto de entrada de la captura en tiempo real.
 *
 * Notas de diseño:
 *  - El procesamiento es SECUENCIAL (un solo hilo): la deduplicación y los cursores por
 *    conversación dependen del orden de llegada.
 *  - Fase 0 no arranca un foreground service: el propio sistema mantiene vivo al listener
 *    mientras el usuario tenga concedido el acceso a notificaciones (menos consumo y menos
 *    riesgo de que el fabricante lo mate). Fase 1 añade el foreground service con vigilancia
 *    por WorkManager.
 *  - TODO(Fase 2): yapeos/Plin (notificaciones de pago de apps peruanas) y respuesta por voz.
 */
class WaListenerService : NotificationListenerService() {

    private val job = SupervisorJob()
    private val dispatcher = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "alo-processor")
    }.asCoroutineDispatcher()
    private val scope = CoroutineScope(job + dispatcher)
    private val bootstrapped = AtomicBoolean(false)

    /**
     * Puerta de arranque: no se procesa ni una notificación hasta que la memoria del filtro
     * (cursores + huellas) esté cargada. Sin esto, tras reiniciar el servicio se podría leer
     * de nuevo un mensaje ya visto (las notificaciones se reemiten completas).
     */
    private val ready = CompletableDeferred<Unit>()

    private val graph: AloGraph
        get() = (application as AloApp).graph

    override fun onCreate() {
        super.onCreate()
        bootstrap()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        bootstrap()
        scope.launch {
            withTimeoutOrNull(WARMUP_TIMEOUT_MS) { ready.await() }
            graph.eventLog.info("Acceso a notificaciones concedido")
            backfillActiveNotifications()
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        graph.eventLog.info("Acceso a notificaciones retirado (el sistema detuvo la escucha)")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        val raw = runCatching { NotificationSnapshot.from(notification) }.getOrNull() ?: return

        // Capa 0 antes de cualquier trabajo: si no es WhatsApp, no se procesa ni se registra.
        if (!WhatsappPackages.isWhatsapp(raw.pkg) && !isTestPackage(raw.pkg)) return
        debugLog("notificación de ${raw.pkg} · canal=${raw.channelId} · mensajes=${raw.messages.size}")

        scope.launch {
            runCatching {
                withTimeoutOrNull(WARMUP_TIMEOUT_MS) { ready.await() }
                process(raw, live = true)
            }.onFailure { Log.w(TAG, "Error procesando notificación", it) }
        }
    }

    /** El "modo prueba" (solo debug) permite notificaciones publicadas por adb. */
    private fun isTestPackage(pkg: String): Boolean =
        BuildConfig.DEBUG && graph.settings.state.value.testMode && pkg == TEST_PACKAGE

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    private fun describe(result: FilterResult): String = when (result) {
        is FilterResult.Read -> "LEÍDO (${result.events.size} mensaje/s, speak=${result.speak})"
        is FilterResult.Discarded -> "DESCARTADO (capa ${result.layer}: ${result.reason})"
        is FilterResult.NeedsReview -> "A REVISAR (capa ${result.layer}: ${result.reason})"
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Intencionalmente vacío: el mensaje ya se capturó al publicarse la notificación.
    }

    // ── Pipeline ───────────────────────────────────────────────────────────────

    private suspend fun process(raw: RawNotification, live: Boolean) {
        val settings = graph.settings.state.value
        val device = DeviceContext.snapshot(this).let {
            DeviceContextSnapshot(
                headphonesConnected = it.headphonesConnected,
                inCall = it.inCall,
                screenOn = it.screenOn,
            )
        }

        val result = graph.normalizer().normalize(
            raw = raw,
            lastSeen = graph.repository::lastSeen,
            seenMarker = graph.repository::contains,
            device = device,
            hourOfDay = LocalTime.now().hour,
        )
        debugLog(describe(result))

        when (result) {
            is FilterResult.Read -> {
                val events: List<MessageEvent> = if (live) {
                    result.events
                } else {
                    // Backfill: mensajes que ya estaban en pantalla cuando se concedió el permiso.
                    // Se guardan, pero NUNCA se leen en voz alta (evita un "discurso" al instalar).
                    result.events.map { it.copy(source = MessageEvent.Source.BACKFILL) }
                }

                val shouldSpeak = live &&
                    result.speak &&
                    settings.voiceEnabled &&
                    !settings.vibrateInsteadOfSpeak

                graph.repository.persist(events, wasReadAloud = shouldSpeak)
                graph.eventLog.record(FilterResult.Read(events, speak = shouldSpeak))

                if (shouldSpeak) {
                    graph.announcer.announce(FilterResult.Read(events, speak = true))
                } else if (live && settings.vibrateInsteadOfSpeak) {
                    graph.announcer.vibrate()
                }
            }

            is FilterResult.Discarded -> {
                graph.repository.recordFilterOutcome(result)
                graph.eventLog.record(result)
            }

            is FilterResult.NeedsReview -> {
                // No se pierde nada: va al buzón de revisión, sin lectura en voz alta.
                graph.repository.recordFilterOutcome(result)
                graph.eventLog.record(result)
                if (live) graph.announcer.vibrate(30)
            }
        }
    }

    private suspend fun backfillActiveNotifications() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        val whatsapp = active.filter { WhatsappPackages.isWhatsapp(it.packageName) }
        if (whatsapp.isEmpty()) return
        graph.eventLog.info("Sincronizando ${whatsapp.size} conversación(es) ya abiertas")
        whatsapp.forEach { sbn ->
            runCatching { NotificationSnapshot.from(sbn) }.getOrNull()?.let { process(it, live = false) }
        }
    }

    private fun bootstrap() {
        if (!bootstrapped.compareAndSet(false, true)) return
        scope.launch {
            runCatching { graph.repository.warmUp() }
                .onFailure { Log.w(TAG, "No se pudo cargar la memoria del filtro", it) }
            if (!ready.isCompleted) ready.complete(Unit)
            graph.repository.purgeOlderThan(graph.settings.state.value.retentionDays)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        dispatcher.close()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "Alo/Listener"
        private const val WARMUP_TIMEOUT_MS = 5_000L
        private const val TEST_PACKAGE = "com.android.shell"
    }
}
