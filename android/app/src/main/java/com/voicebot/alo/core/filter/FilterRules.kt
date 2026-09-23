package com.voicebot.alo.core.filter

import com.voicebot.alo.core.model.WhatsappPackages

/**
 * Diccionario de patrones de ruido y señales de canal.
 * Versionado a propósito: en Fase 1 estas reglas se descargan por OTA sin publicar la app.
 */
data class FilterRules(
    val version: Int = 1,
    val allowedPackages: Set<String> = WhatsappPackages.ALL,
    /** Marcadores en el id del canal de notificación que NUNCA son conversaciones. */
    val noiseChannelMarkers: List<String> = listOf(
        "backup", "media_upload", "media_download", "sync", "synchronization",
        "call", "calls", "status", "verification", "otp", "sms_retriever",
        "registration", "download", "update", "web",
    ),
    /** Categorías de Android que no son mensajes de chat. */
    val noiseCategories: List<String> = listOf(
        "call", "missed_call", "status", "alarm", "event", "progress", "service",
        "transport", "reminder", "recommendation",
    ),
    /** Patrones de texto de avisos de servicio (multilingüe, se comparan sin acentos ni mayúsculas). */
    val noiseTextPatterns: List<Regex> = defaultNoisePatterns,
    /** Máximo de mensajes que se procesan por notificación (anti-spam de reemisiones). */
    val maxMessagesPerNotification: Int = 8,
) {
    companion object {
        /**
         * Nota de diseño: WhatsApp reemite la notificación COMPLETA cada vez que llega un mensaje
         * al mismo chat, por eso el filtro de texto nunca debe mirar el historial completo, sino
         * el último mensaje nuevo (ver MessageNormalizer).
         */
        val defaultNoisePatterns: List<Regex> = listOf(
            // respaldos / sincronización / mantenimiento
            Regex("copias? de seguridad"),
            Regex("\\bback(ing)?\\s?up\\b"),
            Regex("restaurand"),
            Regex("restaur"),
            Regex("sincronizand"),
            Regex("\\bsync(roniz|hroniz)"),
            Regex("preparando (los )?(mensajes|archivos)"),
            Regex("\\bactualizand"),
            Regex("\\bdescargand"),
            Regex("\\bsubiendo\\b"),
            // resúmenes / avisos de la app
            Regex("mensajes? nuev"),
            Regex("\\btienes \\d+ mensajes?\\b"),
            Regex("\\d+ mensajes? sin leer"),
            Regex("esperando (este|el) mensaje"),
            Regex("^toca para"),
            Regex("abre whatsapp"),
            Regex("no se pudo (enviar|descargar|sincronizar|conectar)"),
            Regex("conexi[oó]n (perdida|restablecida)"),
            Regex("necesitas (internet|conexi[oó]n)"),
            Regex("se est[aá] conectando"),
            // llamadas y estados (por si llegan como texto)
            Regex("llamada perdida"),
            Regex("llamada de whatsapp"),
            Regex("\\ben llamada\\b"),
            Regex("comparti[oó] su estado"),
            Regex("actualiz[oó] su estado"),
            Regex("\\(?\\d+\\)? estados? nuev"),
            // verificación / seguridad
            Regex("c[oó]digo de (verificaci[oó]n|seguridad)"),
            Regex("\\botp\\b"),
            Regex("\\d{3}[ -]?\\d{3}"),
            Regex("verificando (tu )?n[uú]mero"),
            // seguridad de la cuenta
            Regex("dispositivo vinculado"),
            Regex("inicia sesi[oó]n en whatsapp web"),
            Regex("whatsapp web"),
        )

        /** Mensajes de sistema que WhatsApp muestra dentro de un chat (no los escribió nadie). */
        val systemInChatPatterns: List<Regex> = listOf(
            Regex("cifrado de extremo a extremo"),
            Regex("los mensajes y las llamadas"),
            Regex("se uni[oó] al grupo"),
            Regex("cre[oó] el grupo"),
            Regex("cambi[oó] (el asunto|la foto|el icono)"),
            Regex("a[nñ]adi[oó] a"),
            Regex("elimin[oó] este mensaje"),
            Regex("esperando este mensaje"),
        )
    }
}
