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
        val compat = runCatching {
            NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n)
        }.getOrNull()

        val compatMessages = runCatching {
            compat?.messages.orEmpty().map { message ->
                CompatMessage(
                    text = message.text?.toString(),
                    sender = message.person?.name?.toString(),
                    timestamp = message.timestamp,
                )
            }
        }.getOrDefault(emptyList())

        // Algunos builds de WhatsApp construyen MessagingStyle con la API del framework y
        // NotificationCompat no logra reconstruirlo. Leemos android.messages como respaldo.
        val messages = compatMessages.ifEmpty { frameworkMessages(n) }
        if (compat == null && messages.isEmpty()) return null

        val extras = n.extras
        val conversationTitle = compat?.conversationTitle
            ?: extras?.getCharSequence("android.conversationTitle")
        val explicitGroup = compat?.isGroupConversation == true ||
            extras?.getBoolean("android.isGroupConversation", false) == true
        val normalizedTitle = conversationTitle?.toString()?.trim()
        val inferredGroup = !normalizedTitle.isNullOrEmpty() && messages.any { message ->
            !message.sender.isNullOrBlank() && !message.sender.equals(normalizedTitle, ignoreCase = true)
        }

        return StyleInfo(
            conversationTitle = conversationTitle,
            isGroupConversation = explicitGroup || inferredGroup,
            userName = compat?.user?.name,
            messages = messages,
        )
    }

    private fun frameworkMessages(n: android.app.Notification): List<CompatMessage> = runCatching {
        val bundles = n.extras?.getParcelableArray(android.app.Notification.EXTRA_MESSAGES)
        android.app.Notification.MessagingStyle.Message.getMessagesFromBundleArray(bundles)
            .orEmpty()
            .map { message ->
                val sender = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    message.senderPerson?.name
                } else {
                    @Suppress("DEPRECATION")
                    message.sender
                }
                CompatMessage(
                    text = message.text?.toString(),
                    sender = sender?.toString(),
                    timestamp = message.timestamp,
                )
            }
    }.getOrDefault(emptyList())

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
