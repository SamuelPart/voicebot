package com.voicebot.alo.core.model

/** Paquetes de WhatsApp soportados en el MVP (personal y Business). */
object WhatsappPackages {
    const val PERSONAL = "com.whatsapp"
    const val BUSINESS = "com.whatsapp.w4b"
    val ALL = setOf(PERSONAL, BUSINESS)

    fun isWhatsapp(pkg: String?): Boolean = pkg != null && pkg in ALL
}

/**
 * Evento normalizado: la ÚNICA estructura que el resto de la app conoce.
 * Si un mensaje llega hasta aquí, es un mensaje de chat real (no un aviso).
 */
data class MessageEvent(
    val id: String,                 // hash estable -> idempotencia / deduplicación
    val pkg: String,                // com.whatsapp | com.whatsapp.w4b
    val chatId: String,             // clave de conversación (chatKey)
    val chatTitle: String,          // "Mamá" o "Equipo Ventas" (grupo)
    val isGroup: Boolean,
    val sender: String?,            // nombre de quien escribe (en grupos, distinto del chat)
    val text: String,
    val timestamp: Long,
    val dedupKey: String,
    val sbnKey: String,             // StatusBarNotification.key (para responder/eliminar)
    val replyable: Boolean,         // tiene acción de respuesta directa (RemoteInput)
    val source: Source = Source.LIVE,
) {
    enum class Source { LIVE, BACKFILL }
}

/** Resultado del pipeline: leído, descartado o enviado a revisión. */
sealed interface FilterResult {
    /**
     * Mensajes de chat confirmados. [speak] lo decide la capa 5 (reglas del usuario):
     * un mensaje puede capturarse con `speak = false` (chat silenciado, hora de silencio…).
     */
    data class Read(
        val events: List<MessageEvent>,
        val speak: Boolean = true,
    ) : FilterResult

    data class Discarded(
        val layer: Int,
        val reason: String,
        val preview: String? = null,
    ) : FilterResult

    /** No se pudo clasificar con seguridad: se guarda sin leer (anti-falso-negativo). */
    data class NeedsReview(
        val layer: Int,
        val reason: String,
        val preview: String? = null,
    ) : FilterResult
}
