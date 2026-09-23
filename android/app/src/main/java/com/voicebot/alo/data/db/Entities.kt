package com.voicebot.alo.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Mensaje de chat capturado (ya filtrado: aquí nunca entra ruido). */
@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["dedupKey"], unique = true),
        Index(value = ["chatId"]),
        Index(value = ["timestamp"]),
    ],
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val pkg: String,
    val chatId: String,
    val chatTitle: String,
    val isGroup: Boolean,
    val sender: String?,
    val text: String,
    val timestamp: Long,
    val dedupKey: String,
    val sbnKey: String,
    val replyable: Boolean,
    val source: String,
    val wasReadAloud: Boolean,
    val createdAt: Long,
)

/**
 * Buzón de no clasificados y log de descartes (transparencia + mejora del filtro).
 * `status`: DISCARDED | NEEDS_REVIEW | REVIEWED
 */
@Entity(
    tableName = "dropped_events",
    indices = [Index(value = ["timestamp"]), Index(value = ["status"])],
)
data class DroppedEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val layer: Int,
    val status: String,
    val reason: String,
    val preview: String?,
    val pkg: String?,
    val timestamp: Long,
) {
    companion object {
        const val STATUS_DISCARDED = "DISCARDED"
        const val STATUS_NEEDS_REVIEW = "NEEDS_REVIEW"
        const val STATUS_REVIEWED = "REVIEWED"
    }
}

/** Cursor por conversación: hasta dónde se procesó (clave contra las reemisiones de WhatsApp). */
@Entity(tableName = "chat_cursors")
data class ChatCursorEntity(
    @PrimaryKey val chatId: String,
    val chatTitle: String,
    val lastTimestamp: Long,
    val updatedAt: Long,
)
