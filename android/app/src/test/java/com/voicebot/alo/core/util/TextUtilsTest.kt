package com.voicebot.alo.core.util

import com.voicebot.alo.core.filter.FilterRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextUtilsTest {

    @Test
    fun `normalize quita acentos mayusculas y espacios extra`() {
        assertEquals("codigo de verificacion", TextUtils.normalize("  Código   de   Verificación "))
    }

    @Test
    fun `los patrones de aviso se detectan sin acentos`() {
        val hit = TextUtils.matchesAny("COPIA DE SEGURIDAD completada", FilterRules.defaultNoisePatterns)
        assertTrue(hit != null)
    }

    @Test
    fun `un mensaje normal no coincide con ningun patron de aviso`() {
        val hit = TextUtils.matchesAny("Hijo, ¿ya saliste de la oficina?", FilterRules.defaultNoisePatterns)
        assertEquals(null, hit)
    }

    @Test
    fun `sha256 es estable y distingue contenidos`() {
        val a = TextUtils.sha256("com.whatsapp", "Mamá", "1700000000000", "hola")
        val b = TextUtils.sha256("com.whatsapp", "Mamá", "1700000000000", "hola")
        val c = TextUtils.sha256("com.whatsapp", "Mamá", "1700000000000", "hola!")
        assertEquals(a, b)
        assertNotEquals(a, c)
        assertEquals(64, a.length)
    }

    @Test
    fun `forSpeech convierte enlaces en palabras y quita emojis`() {
        val speech = TextUtils.forSpeech("Mira https://ejemplo.com/algo 😀 ya está ✅")
        assertFalse(speech.contains("https"))
        assertFalse(speech.contains("😀"))
        assertEquals("Mira un enlace ya está", speech)
    }

    @Test
    fun `isOnlySymbols detecta mensajes sin texto que leer`() {
        assertTrue(TextUtils.isOnlySymbols("👍🎉😂"))
        assertFalse(TextUtils.isOnlySymbols("ok 👍"))
    }

    @Test
    fun `preview recorta sin romper`() {
        val long = "a".repeat(300)
        val preview = TextUtils.preview(long, max = 50)!!
        assertEquals(50, preview.length)
        assertTrue(preview.endsWith("…"))
    }
}
