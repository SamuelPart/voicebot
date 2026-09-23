package com.voicebot.alo.core.filter

import com.voicebot.alo.core.model.FilterResult
import com.voicebot.alo.core.model.WhatsappPackages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Batería de pruebas del filtro: la definición ejecutable de la promesa del producto
 * ("solo mensajes, nada de avisos"). Los casos están escritos tal como los ve el sistema.
 */
class NotificationNormalizerTest {

    private val t0 = 1_700_000_000_000L

    private fun normalizer(policy: SpeakingPolicy = SpeakingPolicy { _, _, _ -> true }) =
        NotificationNormalizer(
            rules = FilterRules(),
            speakingPolicy = policy,
            clock = { t0 },
        )

    /** Notificación típica de un mensaje de chat de WhatsApp. */
    private fun chatMessage(
        chat: String = "Mamá",
        sender: String = "Mamá",
        text: String = "Hola",
        timestamp: Long = t0,
        isGroup: Boolean = false,
        pkg: String = WhatsappPackages.PERSONAL,
        userName: String? = null,
    ) = RawNotification(
        pkg = pkg,
        sbnKey = "0|$pkg|1|null|10001",
        channelId = "messages_1",
        category = "msg",
        isOngoing = false,
        isGroupSummary = false,
        title = chat,
        text = text,
        subText = null,
        conversationTitle = if (isGroup) chat else null,
        isGroupConversation = isGroup,
        userName = userName,
        messages = listOf(RawNotification.StyleMessage(text, sender, timestamp)),
        hasRemoteInput = true,
    )

    /** Notificación de aviso: texto plano, sin MessagingStyle. */
    private fun plainNotice(
        text: String,
        channelId: String = "messages_1",
        category: String = "msg",
        isOngoing: Boolean = false,
        isGroupSummary: Boolean = false,
        pkg: String = WhatsappPackages.PERSONAL,
        hasRemoteInput: Boolean = false,
    ) = RawNotification(
        pkg = pkg,
        sbnKey = "0|$pkg|2|null|10002",
        channelId = channelId,
        category = category,
        isOngoing = isOngoing,
        isGroupSummary = isGroupSummary,
        title = "WhatsApp",
        text = text,
        subText = null,
        conversationTitle = null,
        isGroupConversation = false,
        messages = emptyList(),
        hasRemoteInput = hasRemoteInput,
    )

    // ── Capa 2: mensajes reales ────────────────────────────────────────────────

    @Test
    fun `mensaje individual de WhatsApp se lee`() {
        val result = normalizer().normalize(
            chatMessage(text = "Hijo, ¿ya saliste de la oficina?", timestamp = t0 + 5),
        )

        assertTrue(result is FilterResult.Read)
        val read = result as FilterResult.Read
        assertEquals(1, read.events.size)
        assertEquals("Mamá", read.events[0].chatTitle)
        assertEquals("Mamá", read.events[0].sender)
        assertFalse(read.events[0].isGroup)
        assertTrue(read.speak)
    }

    @Test
    fun `mensaje de grupo guarda chat y remitente por separado`() {
        val result = normalizer().normalize(
            chatMessage(
                chat = "Equipo Ventas",
                sender = "Luis",
                isGroup = true,
                text = "El cliente firma mañana",
                timestamp = t0 + 5,
            ),
        )

        val event = (result as FilterResult.Read).events.single()
        assertEquals("Equipo Ventas", event.chatTitle)
        assertEquals("Luis", event.sender)
        assertTrue(event.isGroup)
    }

    @Test
    fun `solo se procesan los mensajes nuevos de la notificacion`() {
        val raw = RawNotification(
            pkg = WhatsappPackages.PERSONAL,
            sbnKey = "key",
            channelId = "messages_1",
            category = "msg",
            isOngoing = false,
            isGroupSummary = false,
            title = "Pedro",
            text = "tercero",
            subText = null,
            conversationTitle = null,
            isGroupConversation = false,
            messages = listOf(
                RawNotification.StyleMessage("primero", "Pedro", t0 + 1),
                RawNotification.StyleMessage("segundo", "Pedro", t0 + 2),
                RawNotification.StyleMessage("tercero", "Pedro", t0 + 3),
            ),
            hasRemoteInput = true,
        )

        // Ya procesamos hasta t0+1: solo deben salir "segundo" y "tercero".
        val result = normalizer().normalize(raw, lastSeen = { t0 + 1 })
        val texts = (result as FilterResult.Read).events.map { it.text }
        assertEquals(listOf("segundo", "tercero"), texts)
    }

    // ── Capa 1: ruido estructural ─────────────────────────────────────────────

    @Test
    fun `backup en curso se descarta en la capa 1`() {
        val result = normalizer().normalize(
            plainNotice(text = "Copia de seguridad completada", channelId = "backup", isOngoing = true),
        )
        assertTrue(result is FilterResult.Discarded)
        assertEquals(1, (result as FilterResult.Discarded).layer)
    }

    @Test
    fun `resumen de grupo no es un mensaje`() {
        val result = normalizer().normalize(plainNotice(text = "3 mensajes nuevos", isGroupSummary = true))
        assertTrue(result is FilterResult.Discarded)
        assertEquals(1, (result as FilterResult.Discarded).layer)
    }

    @Test
    fun `llamada perdida se descarta`() {
        val result = normalizer().normalize(
            plainNotice(text = "Llamada perdida de Carlos", category = "missed_call"),
        )
        assertTrue(result is FilterResult.Discarded)
        assertTrue((result as FilterResult.Discarded).reason.contains("llamada"))
    }

    @Test
    fun `estado de contacto se descarta`() {
        val result = normalizer().normalize(
            plainNotice(text = "Carlos actualizó su estado", category = "status"),
        )
        assertTrue(result is FilterResult.Discarded)
        assertEquals(1, (result as FilterResult.Discarded).layer)
    }

    @Test
    fun `canal de multimedia se descarta`() {
        val result = normalizer().normalize(
            plainNotice(text = "Descargando archivo", channelId = "media_upload"),
        )
        assertTrue(result is FilterResult.Discarded)
        assertEquals(1, (result as FilterResult.Discarded).layer)
    }

    @Test
    fun `paquete ajeno se descarta en la capa 0`() {
        val result = normalizer().normalize(
            plainNotice(text = "Documento compartido", pkg = "com.google.android.apps.docs"),
        )
        assertEquals(0, (result as FilterResult.Discarded).layer)
    }

    @Test
    fun `modo prueba acepta las notificaciones publicadas por adb`() {
        // Equivale a activar "Modo prueba": se añade com.android.shell al allowlist.
        val base = FilterRules()
        val rules = base.copy(allowedPackages = base.allowedPackages + "com.android.shell")
        val normalizer = NotificationNormalizer(rules = rules, clock = { t0 })

        val raw = chatMessage(chat = "Mamá", sender = "Mamá", text = "hola desde adb", timestamp = t0 + 1)
            .copy(pkg = "com.android.shell")

        val result = normalizer.normalize(raw)
        assertTrue(result is FilterResult.Read)
        assertEquals("hola desde adb", (result as FilterResult.Read).events.single().text)
    }

    // ── Capa 3: patrones de aviso (solo sin estructura) ───────────────────────

    @Test
    fun `aviso de respaldo sin estructura se descarta por texto`() {
        val result = normalizer().normalize(plainNotice(text = "Copia de seguridad completada"))
        assertTrue(result is FilterResult.Discarded)
        assertEquals(3, (result as FilterResult.Discarded).layer)
    }

    @Test
    fun `codigo de verificacion no se lee`() {
        val result = normalizer().normalize(plainNotice(text = "Tu código de verificación es 384910"))
        assertTrue(result is FilterResult.Discarded)
        assertEquals(3, (result as FilterResult.Discarded).layer)
    }

    @Test
    fun `mensaje real que suena a aviso se lee igual`() {
        // Alguien escribió esto: trae MessagingStyle con remitente -> es un mensaje, no un aviso.
        val result = normalizer().normalize(
            chatMessage(chat = "Luis", sender = "Luis", text = "¿Ya hiciste la copia de seguridad?", timestamp = t0 + 5),
        )
        assertTrue(result is FilterResult.Read)
        assertEquals("¿Ya hiciste la copia de seguridad?", (result as FilterResult.Read).events.single().text)
    }

    @Test
    fun `mensaje de solo emojis no se lee`() {
        val result = normalizer().normalize(chatMessage(text = "👍🎉😂", timestamp = t0 + 5))
        assertTrue(result is FilterResult.Discarded)
        assertEquals(3, (result as FilterResult.Discarded).layer)
    }

    @Test
    fun `aviso del sistema dentro del chat no se lee`() {
        val result = normalizer().normalize(
            chatMessage(
                chat = "Ana",
                sender = "Ana",
                text = "Los mensajes y las llamadas están cifrados de extremo a extremo.",
                timestamp = t0 + 5,
            ),
        )
        assertTrue(result is FilterResult.Discarded)
        assertEquals(3, (result as FilterResult.Discarded).layer)
    }

    // ── Capa 4: deduplicación (WhatsApp reemite la notificación completa) ──────

    @Test
    fun `la reemision de la misma notificacion no se lee dos veces`() {
        val memory = InMemoryMessageMemory()
        val normalizer = normalizer()
        val raw = chatMessage(text = "Nos vemos a las 8", timestamp = t0 + 7)

        val first = normalizer.normalize(raw, lastSeen = memory::lastSeen, seenMarker = memory::contains)
        assertTrue(first is FilterResult.Read)

        // Equivale a MessageRepository.persist(): recordar huella y avanzar el cursor del chat.
        (first as FilterResult.Read).events.forEach {
            memory.remember(it.dedupKey)
            memory.touch(it.chatId, it.timestamp)
        }

        val second = normalizer.normalize(raw, lastSeen = memory::lastSeen, seenMarker = memory::contains)
        assertTrue(second is FilterResult.Discarded)
        assertEquals(4, (second as FilterResult.Discarded).layer)
    }

    @Test
    fun `mensaje nuevo en el mismo chat si se procesa`() {
        val memory = InMemoryMessageMemory()
        val normalizer = normalizer()

        val first = normalizer.normalize(chatMessage(text = "primero", timestamp = t0 + 1))
        (first as FilterResult.Read).events.forEach {
            memory.remember(it.dedupKey)
            memory.touch(it.chatId, it.timestamp)
        }

        val raw = chatMessage(text = "segundo", timestamp = t0 + 2)
        val result = normalizer.normalize(raw, lastSeen = memory::lastSeen, seenMarker = memory::contains)
        assertTrue(result is FilterResult.Read)
        assertEquals("segundo", (result as FilterResult.Read).events.single().text)
    }

    @Test
    fun `los mensajes que envia el propio usuario no se leen`() {
        // WhatsApp añade a la notificación las respuestas que envías tú (con el remitente del
        // dueño del teléfono): no deben leerse como si las hubiera dicho el contacto.
        val raw = chatMessage(chat = "Mamá", sender = "Mamá", text = "Ya salgo", timestamp = t0 + 1, userName = "Samuel")
            .copy(
                messages = listOf(
                    RawNotification.StyleMessage("Ya salgo", "Samuel", t0 + 1),
                    RawNotification.StyleMessage("Te espero con la cena", "Mamá", t0 + 2),
                ),
            )

        val result = normalizer().normalize(raw)
        val events = (result as FilterResult.Read).events
        assertEquals(1, events.size)
        assertEquals("Te espero con la cena", events.single().text)
        assertEquals("Mamá", events.single().sender)
    }

    // ── Ambigüedad: buzón de revisión (nunca perder un mensaje) ───────────────

    @Test
    fun `notificacion sin estructura va a revision sin leerse`() {
        val result = normalizer().normalize(plainNotice(text = "Formato nuevo desconocido"))
        assertTrue(result is FilterResult.NeedsReview)
        assertEquals(2, (result as FilterResult.NeedsReview).layer)
    }

    // ── Capa 5: reglas del usuario (silencian la voz, nunca la captura) ───────

    @Test
    fun `chat silenciado se captura pero no se habla`() {
        val raw = chatMessage(text = "mensaje silenciado", timestamp = t0 + 11)
        val chatId = (normalizer().normalize(raw) as FilterResult.Read).events.single().chatId

        val result = normalizer(RulesBasedSpeakingPolicy(mutedChatIds = setOf(chatId))).normalize(raw)

        assertTrue(result is FilterResult.Read)
        assertFalse((result as FilterResult.Read).speak)
        assertEquals(1, result.events.size)
    }

    @Test
    fun `no molestar silencia la lectura pero no la captura`() {
        val policy = RulesBasedSpeakingPolicy(quietHoursStart = 22, quietHoursEnd = 7)

        val night = normalizer(policy).normalize(chatMessage(text = "mensaje nocturno", timestamp = t0 + 13), hourOfDay = 23)
        assertTrue(night is FilterResult.Read)
        assertFalse((night as FilterResult.Read).speak)

        val day = normalizer(policy).normalize(chatMessage(text = "mensaje diurno", timestamp = t0 + 14), hourOfDay = 12)
        assertTrue((day as FilterResult.Read).speak)
    }

    @Test
    fun `solo con audifonos evita leer en el altavoz`() {
        val policy = RulesBasedSpeakingPolicy(onlyWithHeadphones = true)

        val sin = normalizer(policy).normalize(
            chatMessage(text = "mensaje", timestamp = t0 + 17),
            device = DeviceContextSnapshot(headphonesConnected = false),
        )
        assertFalse((sin as FilterResult.Read).speak)

        val con = normalizer(policy).normalize(
            chatMessage(text = "otro mensaje", timestamp = t0 + 18),
            device = DeviceContextSnapshot(headphonesConnected = true),
        )
        assertTrue((con as FilterResult.Read).speak)
    }

    @Test
    fun `pausa durante llamadas`() {
        val policy = RulesBasedSpeakingPolicy(pauseDuringCalls = true)
        val result = normalizer(policy).normalize(
            chatMessage(text = "mensaje", timestamp = t0 + 21),
            device = DeviceContextSnapshot(inCall = true),
        )
        assertFalse((result as FilterResult.Read).speak)
    }
}
