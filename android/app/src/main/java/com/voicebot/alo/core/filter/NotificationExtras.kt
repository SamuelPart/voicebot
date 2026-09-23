package com.voicebot.alo.core.filter

import androidx.core.app.NotificationCompat

/**
 * Extracción de MessagingStyle con NotificationCompat.
 *
 * Usamos NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification() porque
 * devuelve una lista PLANA y fácil de recorrer, tanto si la notificación la construyó
 * NotificationCompat como si la construyó el framework (que es el caso de WhatsApp).
 *
 * Decisión deliberada de Fase 0: solo usamos la API COMPAT (no las clases del framework
 * Notification.MessagingStyle.Message) para no encadenarnos a APIs que cambian de versión.
 */
object NotificationExtras {

    data class StyleInfo(
        val conversationTitle: CharSequence?,
        val isGroupConversation: Boolean,
        /** “Yo” en la conversación: permite descartar los mensajes que envía el propio usuario. */
        val userName: CharSequence?,
        val messages: List<CompatMessage>,
    )

    data class CompatMessage(
        val text: String?,
        val sender: String?,
        val timestamp: Long,
    )

    fun channelId(n: android.app.Notification): String? =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) n.channelId else null

    /**
     * Devuelve la información de MessagingStyle o null si la notificación no es un mensaje
     * de conversación. Un null aquí NO decide el descarte: decide la capa 2 del filtro.
     */
    fun messagingStyle(n: android.app.Notification): StyleInfo? {
        val style = runCatching {
            NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n)
        }.getOrNull() ?: return null

        val messages = runCatching {
            style.messages.map { m ->
                CompatMessage(
                    text = m.text?.toString(),
                    sender = m.person?.name?.toString(),
                    timestamp = m.timestamp,
                )
            }
        }.getOrNull() ?: return null

        return StyleInfo(
            conversationTitle = style.conversationTitle,
            isGroupConversation = style.isGroupConversation,
            userName = style.user?.name,
            messages = messages,
        )
    }

    /** ¿La notificación trae acción de respuesta directa? (Fase 2: responder por voz) */
    fun hasRemoteInput(n: android.app.Notification): Boolean =
        n.actions?.any { action -> action.remoteInputs?.isNotEmpty() == true } == true

    /** Etiqueta de la acción de respuesta, útil para la UI. */
    fun replyActionLabels(n: android.app.Notification): List<String> =
        n.actions.orEmpty()
            .filter { it.remoteInputs?.isNotEmpty() == true }
            .map { it.title?.toString().orEmpty() }

    /** Clave de resultado de RemoteInput (Fase 2: respuesta por voz). */
    fun firstRemoteInputKey(n: android.app.Notification): String? =
        n.actions.orEmpty()
            .flatMap { it.remoteInputs?.toList().orEmpty() }
            .firstOrNull()
            ?.resultKey
}
