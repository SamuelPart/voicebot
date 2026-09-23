package com.voicebot.alo.core.filter

import android.app.Notification
import android.service.notification.StatusBarNotification

/**
 * Datos crudos de una notificación, ya desacoplados de Android.
 * Mantener el normalizador puro permite testearlo con JUnit sin emulador (Fase 0).
 */
data class RawNotification(
    val pkg: String,
    val sbnKey: String,
    val channelId: String?,
    val category: String?,
    val isOngoing: Boolean,
    val isGroupSummary: Boolean,
    val title: String?,
    val text: String?,
    val subText: String?,
    val conversationTitle: String?,
    val isGroupConversation: Boolean,
    /** Nombre del usuario del teléfono según MessagingStyle (para no leer sus propios mensajes). */
    val userName: String? = null,
    /** Mensajes estructurados de MessagingStyle (la señal fuerte). */
    val messages: List<StyleMessage>,
    val hasRemoteInput: Boolean,
) {
    data class StyleMessage(
        val text: String?,
        val sender: String?,
        val timestamp: Long,
    )
}

/** Extrae el contenido de interés de un [StatusBarNotification]. */
object NotificationSnapshot {

    fun from(sbn: StatusBarNotification): RawNotification {
        val n = sbn.notification
        val extras = n.extras
        val style = NotificationExtras.messagingStyle(n)

        return RawNotification(
            pkg = sbn.packageName.orEmpty(),
            sbnKey = sbn.key.orEmpty(),
            channelId = NotificationExtras.channelId(n),
            category = n.category,
            // Notification.isOngoing() se añadió en API 31. Leer el flag funciona desde API 1
            // y evita un NoSuchMethodError en el minSdk 26.
            isOngoing = (n.flags and Notification.FLAG_ONGOING_EVENT) != 0,
            isGroupSummary = (n.flags and Notification.FLAG_GROUP_SUMMARY) != 0,
            title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            subText = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
            conversationTitle = style?.conversationTitle?.toString()
                ?: extras?.getCharSequence("android.conversationTitle")?.toString(),
            isGroupConversation = style?.isGroupConversation ?: false,
            userName = style?.userName?.toString(),
            messages = style?.messages.orEmpty().map {
                RawNotification.StyleMessage(text = it.text, sender = it.sender, timestamp = it.timestamp)
            },
            hasRemoteInput = n.actions?.any { it.remoteInputs?.isNotEmpty() == true } == true,
        )
    }
}
