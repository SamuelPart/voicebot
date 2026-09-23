package com.voicebot.alo.core.util

import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

/** Utilidades de texto compartidas por el filtro y el motor de voz. */
object TextUtils {

    /** Minúsculas, sin acentos y sin espacios redundantes: base de la comparación de patrones. */
    fun normalize(input: String?): String {
        if (input.isNullOrBlank()) return ""
        return Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun matchesAny(input: String?, patterns: List<Regex>): Regex? {
        val n = normalize(input)
        if (n.isEmpty()) return null
        return patterns.firstOrNull { it.containsMatchIn(n) }
    }

    /** Huella estable: base de la deduplicación y de los ids de mensaje. */
    fun sha256(vararg parts: String?): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val joined = parts.joinToString("\u0001") { it.orEmpty() }
        val bytes = digest.digest(joined.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** Recorta para logs/preview sin exponer el mensaje completo. */
    fun preview(input: String?, max: Int = 90): String? {
        if (input.isNullOrBlank()) return null
        val one = input.replace(Regex("\\s+"), " ").trim()
        return if (one.length <= max) one else one.take(max - 1) + "…"
    }

    /** Limpia el texto para lectura en voz alta: URLs, emojis y ruido tipográfico. */
    fun forSpeech(input: String): String {
        var t = input
            .replace(Regex("https?://\\S+"), " un enlace ")
            .replace(Regex("www\\.\\S+"), " un enlace ")
        t = t.replace(Regex("[\\u2190-\\u21FF\\u2300-\\u27BF\\u2B00-\\u2BFF\\uFE0F\\u200D]"), " ")
        t = t.replace(Regex("[\\uD83C-\\uDBFF][\\uDC00-\\uDFFF]"), " ")
        return t.replace(Regex("\\s+"), " ").trim()
    }

    /** ¿El texto parece solo emojis/símbolos (nada que leer)? */
    fun isOnlySymbols(input: String): Boolean = forSpeech(input).isEmpty()
}
