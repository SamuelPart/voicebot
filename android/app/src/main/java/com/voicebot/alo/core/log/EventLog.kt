package com.voicebot.alo.core.log

import com.voicebot.alo.core.model.FilterResult
import com.voicebot.alo.core.model.MessageEvent
import com.voicebot.alo.core.util.TextUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

/**
 * Flujo en vivo para la pantalla principal: qué se capturó, qué se descartó y por qué.
 * Es el instrumento de validación de la Fase 0 (medir falsos positivos/negativos sin adivinar).
 */
class EventLog(private val capacity: Int = 200) {

    data class Entry(
        val id: String,
        val kind: Kind,
        val layer: Int,
        val title: String,
        val detail: String,
        val timestamp: Long,
    ) {
        enum class Kind { READ, DISCARDED, REVIEW, INFO }
    }

    data class Stats(
        val captured: Int = 0,
        val discarded: Int = 0,
        val needsReview: Int = 0,
    )

    /** Desempata eventos generados en el mismo milisegundo (por ejemplo, «Probar ruido»). */
    private val sequence = AtomicLong(0)

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    private val _stats = MutableStateFlow(Stats())
    val stats: StateFlow<Stats> = _stats.asStateFlow()

    fun record(result: FilterResult, now: Long = System.currentTimeMillis()) {
        when (result) {
            is FilterResult.Read -> {
                _stats.update { it.copy(captured = it.captured + result.events.size) }
                result.events.forEach { event ->
                    push(
                        Entry(
                            id = event.id,
                            kind = Entry.Kind.READ,
                            layer = 5,
                            title = headline(event),
                            detail = TextUtils.preview(event.text, 140).orEmpty() +
                                if (result.speak) "" else " (silenciado por tus reglas)",
                            timestamp = event.timestamp,
                        )
                    )
                }
            }

            is FilterResult.Discarded -> {
                _stats.update { it.copy(discarded = it.discarded + 1) }
                push(
                    Entry(
                        id = uniqueId("d", now, result.layer),
                        kind = Entry.Kind.DISCARDED,
                        layer = result.layer,
                        title = "Descartado en capa ${result.layer}",
                        detail = listOfNotNull(result.reason, result.preview).joinToString(" · "),
                        timestamp = now,
                    )
                )
            }

            is FilterResult.NeedsReview -> {
                _stats.update { it.copy(needsReview = it.needsReview + 1) }
                push(
                    Entry(
                        id = uniqueId("r", now, result.layer),
                        kind = Entry.Kind.REVIEW,
                        layer = result.layer,
                        title = "Sin clasificar · capa ${result.layer}",
                        detail = listOfNotNull(result.reason, result.preview).joinToString(" · "),
                        timestamp = now,
                    )
                )
            }
        }
    }

    fun info(message: String, now: Long = System.currentTimeMillis()) {
        push(Entry(uniqueId("i", now, 0), Entry.Kind.INFO, 0, "Servicio", message, now))
    }

    private fun uniqueId(prefix: String, timestamp: Long, layer: Int): String =
        "$prefix-$timestamp-$layer-${sequence.incrementAndGet()}"

    fun clear() {
        _entries.value = emptyList()
        _stats.value = Stats()
    }

    private fun headline(event: MessageEvent): String =
        if (event.isGroup) "${event.sender ?: "Alguien"} · ${event.chatTitle}" else event.chatTitle

    private fun push(entry: Entry) {
        _entries.update { current ->
            (listOf(entry) + current).take(capacity)
        }
    }
}
