package com.voicebot.alo.data

import com.voicebot.alo.core.filter.InMemoryMessageMemory
import com.voicebot.alo.core.filter.MessageMemory
import com.voicebot.alo.core.model.FilterResult
import com.voicebot.alo.core.model.MessageEvent
import com.voicebot.alo.data.db.AloDao
import com.voicebot.alo.data.db.ChatCursorEntity
import com.voicebot.alo.data.db.DroppedEntity
import com.voicebot.alo.data.db.MessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Persistencia + memoria del filtro.
 *
 * Detalle clave: [MessageMemory] es SÍNCRONO (lo consulta el normalizador dentro del
 * callback del servicio), pero Room es asíncrono. Solución: caché en memoria caliente
 * (cargada al arrancar) con escritura diferida a disco.
 */
class MessageRepository(
    private val dao: AloDao,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) : MessageMemory {

    private val memory = InMemoryMessageMemory()

    /** Carga cursores e ids ya vistos. Debe ejecutarse antes de procesar notificaciones. */
    suspend fun warmUp() {
        dao.allCursors().forEach { cursor -> memory.touch(cursor.chatId, cursor.lastTimestamp) }
        dao.recent(WARMUP_MESSAGES).forEach { entity ->
            memory.remember(entity.dedupKey)
            memory.touch(entity.chatId, entity.timestamp)
        }
    }

    // ── MessageMemory (capa 4) ──────────────────────────────────────────────────
    override fun lastSeen(chatId: String): Long = memory.lastSeen(chatId)

    override fun contains(dedupKey: String): Boolean = memory.contains(dedupKey)

    override fun remember(dedupKey: String) = memory.remember(dedupKey)

    // ── Escritura ───────────────────────────────────────────────────────────────
    /** Guarda los mensajes capturados y avanza el cursor de cada conversación. */
    suspend fun persist(events: List<MessageEvent>, wasReadAloud: Boolean) {
        val now = clock()
        events.forEach { event ->
            if (contains(event.dedupKey)) return@forEach
            dao.insertMessage(
                MessageEntity(
                    id = event.id,
                    pkg = event.pkg,
                    chatId = event.chatId,
                    chatTitle = event.chatTitle,
                    isGroup = event.isGroup,
                    sender = event.sender,
                    text = event.text,
                    timestamp = event.timestamp,
                    dedupKey = event.dedupKey,
                    sbnKey = event.sbnKey,
                    replyable = event.replyable,
                    source = event.source.name,
                    wasReadAloud = wasReadAloud,
                    createdAt = now,
                )
            )
            remember(event.dedupKey)
            memory.touch(event.chatId, event.timestamp)
            dao.upsertCursor(
                ChatCursorEntity(
                    chatId = event.chatId,
                    chatTitle = event.chatTitle,
                    lastTimestamp = event.timestamp,
                    updatedAt = now,
                )
            )
        }
    }

    /** Registra descartes y no clasificados (transparencia y futuro aprendizaje del filtro). */
    fun recordFilterOutcome(result: FilterResult, now: Long = clock()) {
        val entity = when (result) {
            is FilterResult.Discarded -> DroppedEntity(
                layer = result.layer,
                status = DroppedEntity.STATUS_DISCARDED,
                reason = result.reason,
                preview = result.preview,
                pkg = null,
                timestamp = now,
            )

            is FilterResult.NeedsReview -> DroppedEntity(
                layer = result.layer,
                status = DroppedEntity.STATUS_NEEDS_REVIEW,
                reason = result.reason,
                preview = result.preview,
                pkg = null,
                timestamp = now,
            )

            is FilterResult.Read -> return
        }
        scope.launch { runCatching { dao.insertDropped(entity) } }
    }

    fun markReviewed(id: Long) {
        scope.launch { dao.updateDroppedStatus(id, DroppedEntity.STATUS_REVIEWED) }
    }

    /** Retención: borra mensajes antiguos (por defecto 30 días en Fase 0). */
    fun purgeOlderThan(days: Int) {
        scope.launch {
            val before = clock() - days * 24L * 60L * 60L * 1000L
            runCatching {
                dao.purgeMessages(before)
                dao.purgeDropped(before, DroppedEntity.STATUS_NEEDS_REVIEW)
            }
        }
    }

    fun clearAll() {
        scope.launch {
            runCatching {
                dao.clearMessages()
                dao.clearDropped()
                dao.clearCursors()
            }
        }
    }

    // ── Lectura ─────────────────────────────────────────────────────────────────
    fun observeRecent(limit: Int = 100): Flow<List<MessageEntity>> = dao.observeRecent(limit)

    fun observeMessageCount(): Flow<Int> = dao.observeMessageCount()

    fun observeReviewQueue(limit: Int = 50): Flow<List<DroppedEntity>> =
        dao.observeByStatus(DroppedEntity.STATUS_NEEDS_REVIEW, limit)

    fun observeDiscardedCount(): Flow<Int> =
        dao.observeCountByStatus(DroppedEntity.STATUS_DISCARDED)

    suspend fun recent(limit: Int): List<MessageEntity> = dao.recent(limit)

    companion object {
        private const val WARMUP_MESSAGES = 1000
    }
}
