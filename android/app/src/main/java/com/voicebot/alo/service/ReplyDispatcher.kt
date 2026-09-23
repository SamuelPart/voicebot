package com.voicebot.alo.service

import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * Respuesta directa reutilizando la acción de la notificación de WhatsApp (Fase 2).
 *
 * Por qué así y no "automatizando WhatsApp": el mensaje sale por la acción OFICIAL que expone
 * la propia app, igual que cuando el usuario responde desde la notificación. No hay protocolo
 * reversado, no hay credenciales y no hay motivo de ban.
 *
 * Flujo previsto: dictado (SpeechRecognizer) -> confirmación hablada -> [reply].
 */
class ReplyDispatcher {

    /** ¿Se puede responder a esta notificación? */
    fun canReply(sbn: StatusBarNotification): Boolean =
        sbn.notification.actions?.any { !it.remoteInputs.isNullOrEmpty() } == true

    /** Envía un texto por la acción de respuesta directa. Devuelve true si se despachó. */
    fun reply(context: Context, sbn: StatusBarNotification, text: String): Boolean {
        val action = sbn.notification.actions
            ?.firstOrNull { !it.remoteInputs.isNullOrEmpty() }
            ?: return false
        val remoteInputs = action.remoteInputs ?: return false

        val intent = Intent()
        val results = Bundle().apply {
            remoteInputs.forEach { putCharSequence(it.resultKey, text) }
        }
        RemoteInput.addResultsToIntent(remoteInputs, intent, results)

        return runCatching {
            action.actionIntent.send(context, 0, intent)
            true
        }.getOrElse {
            Log.w(TAG, "No se pudo responder por la notificación", it)
            false
        }
    }

    /** Claves de RemoteInput disponibles (útil para depurar formatos nuevos de WhatsApp). */
    fun remoteInputKeys(sbn: StatusBarNotification): List<String> =
        sbn.notification.actions.orEmpty()
            .flatMap { it.remoteInputs?.toList().orEmpty() }
            .map { it.resultKey }

    companion object {
        private const val TAG = "Alo/Reply"
    }
}
