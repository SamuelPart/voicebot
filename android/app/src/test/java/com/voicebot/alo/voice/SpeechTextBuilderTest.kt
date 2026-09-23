package com.voicebot.alo.voice

import com.voicebot.alo.core.model.MessageEvent
import com.voicebot.alo.core.model.WhatsappPackages
import com.voicebot.alo.core.util.TextUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechTextBuilderTest {

    private val builder = SpeechTextBuilder()

    private fun event(
        text: String,
        chat: String = "Mamá",
        sender: String? = "Mamá",
        isGroup: Boolean = false,
        timestamp: Long = 1_700_000_000_000L,
    ) = MessageEvent(
        id = TextUtils.sha256("id", timestamp.toString()),
        pkg = WhatsappPackages.PERSONAL,
        chatId = TextUtils.sha256("chat", chat),
        chatTitle = chat,
        isGroup = isGroup,
        sender = sender,
        text = text,
        timestamp = timestamp,
        dedupKey = TextUtils.sha256("key", timestamp.toString()),
        sbnKey = "key",
        replyable = true,
    )

    @Test
    fun `mensaje individual usa plantilla natural`() {
        val item = builder.build(listOf(event("ya está lista la cena")))!!
        assertEquals("Mensaje de Mamá: ya está lista la cena", item.speakable)
    }

    @Test
    fun `mensaje de grupo nombra el grupo y el remitente`() {
        val item = builder.build(
            listOf(event("el cliente firma mañana", chat = "Equipo Ventas", sender = "Luis", isGroup = true)),
        )!!
        assertEquals("En el grupo Equipo Ventas, Luis dice: el cliente firma mañana", item.speakable)
    }

    @Test
    fun `varios mensajes seguidos del mismo chat se agrupan en un solo clip`() {
        val item = builder.build(
            listOf(
                event("recibí tu pedido", timestamp = 1_700_000_000_000L),
                event("¿me confirmas la hora?", timestamp = 1_700_000_001_000L),
                event("ya estoy aquí", timestamp = 1_700_000_002_000L),
            ),
        )!!
        assertEquals(
            "Mamá escribió: recibí tu pedido; ¿me confirmas la hora?; y también: ya estoy aquí",
            item.speakable,
        )
    }

    @Test
    fun `los enlaces se leen como un enlace y los emojis se omiten`() {
        val item = builder.build(listOf(event("Mira esto https://ejemplo.com/x 😀")))!!
        assertEquals("Mensaje de Mamá: Mira esto un enlace", item.speakable)
    }

    @Test
    fun `los mensajes muy largos se recortan para no monopolizar el audio`() {
        val largo = "palabra ".repeat(200)
        val item = builder.build(listOf(event(largo)))!!
        assertTrue(item.speakable.length <= "Mensaje de Mamá: ".length + 320)
        assertTrue(item.speakable.endsWith("…"))
    }

    @Test
    fun `sin remitente conocido la frase es neutra`() {
        // Caso real: WhatsApp no informa el remitente (p. ej. un mensaje propio). Nunca se debe
        // atribuir ese texto al contacto.
        val item = builder.build(listOf(event("llegué a casa", sender = null)))!!
        assertEquals("Nuevo mensaje en el chat de Mamá: llegué a casa", item.speakable)
    }

    @Test
    fun `sin eventos no hay nada que leer`() {
        assertNull(builder.build(emptyList()))
    }

    @Test
    fun `un solo emoji no genera audio`() {
        val item = builder.build(listOf(event("👍")))!!
        assertFalse(item.speakable.contains("👍"))
    }
}
