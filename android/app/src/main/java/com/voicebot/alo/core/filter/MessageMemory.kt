package com.voicebot.alo.core.filter

/**
 * Memoria del filtro (capa 4). La implementación real persiste en Room; en tests se usa la
 * versión en memoria. Sin esta memoria, WhatsApp reemite sus notificaciones completas y la app
 * leería el mismo mensaje una y otra vez.
 */
interface MessageMemory {
    /** Timestamp del último mensaje procesado de esa conversación (0 si es nueva). */
    fun lastSeen(chatId: String): Long

    /** ¿Ya procesamos esta huella exacta? */
    fun contains(dedupKey: String): Boolean

    /** Registra la huella (idempotente). */
    fun remember(dedupKey: String)

    companion object {
        fun inMemory(): MessageMemory = InMemoryMessageMemory()
    }
}

class InMemoryMessageMemory : MessageMemory {
    private val seen = HashSet<String>()
    private val lastSeen = HashMap<String, Long>()
    private val lock = Any()

    override fun lastSeen(chatId: String): Long = synchronized(lock) { lastSeen[chatId] ?: 0L }

    override fun contains(dedupKey: String): Boolean = synchronized(lock) { dedupKey in seen }

    override fun remember(dedupKey: String) {
        synchronized(lock) { seen += dedupKey }
    }

    fun touch(chatId: String, timestamp: Long) {
        synchronized(lock) {
            if ((lastSeen[chatId] ?: 0L) < timestamp) lastSeen[chatId] = timestamp
        }
    }
}
