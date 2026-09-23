package com.voicebot.alo.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AloDao {

    // ── Mensajes ────────────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(entity: MessageEntity): Long

    @Query("SELECT COUNT(*) FROM messages WHERE dedupKey = :key")
    suspend fun countMessage(key: String): Int

    @Query("SELECT * FROM messages ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages")
    fun observeMessageCount(): Flow<Int>

    @Query("SELECT * FROM messages WHERE chatTitle LIKE :query OR text LIKE :query ORDER BY timestamp DESC LIMIT :limit")
    fun search(query: String, limit: Int): Flow<List<MessageEntity>>

    @Query("DELETE FROM messages WHERE timestamp < :before")
    suspend fun purgeMessages(before: Long): Int

    // ── Descartes y cola de revisión ────────────────────────────────────────────
    @Insert
    suspend fun insertDropped(entity: DroppedEntity): Long

    @Query("SELECT * FROM dropped_events WHERE status = :status ORDER BY timestamp DESC LIMIT :limit")
    fun observeByStatus(status: String, limit: Int): Flow<List<DroppedEntity>>

    @Query("SELECT COUNT(*) FROM dropped_events WHERE status = :status")
    fun observeCountByStatus(status: String): Flow<Int>

    @Query("UPDATE dropped_events SET status = :newStatus WHERE id = :id")
    suspend fun updateDroppedStatus(id: Long, newStatus: String)

    @Query("DELETE FROM dropped_events WHERE timestamp < :before AND status != :keepStatus")
    suspend fun purgeDropped(before: Long, keepStatus: String): Int

    // ── Cursores por conversación ───────────────────────────────────────────────
    @Query("SELECT lastTimestamp FROM chat_cursors WHERE chatId = :chatId")
    suspend fun lastTimestamp(chatId: String): Long?

    @Query("SELECT * FROM chat_cursors")
    suspend fun allCursors(): List<ChatCursorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCursor(cursor: ChatCursorEntity)

    @Query("DELETE FROM chat_cursors")
    suspend fun clearCursors()

    @Query("DELETE FROM messages")
    suspend fun clearMessages()

    @Query("DELETE FROM dropped_events")
    suspend fun clearDropped()
}
