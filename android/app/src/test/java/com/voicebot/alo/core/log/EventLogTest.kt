package com.voicebot.alo.core.log

import com.voicebot.alo.core.model.FilterResult
import org.junit.Assert.assertEquals
import org.junit.Test

class EventLogTest {

    @Test
    fun `varios descartes en el mismo milisegundo reciben identificadores unicos`() {
        val log = EventLog()
        val instante = 1_700_000_000_000L

        repeat(3) { indice ->
            log.record(
                FilterResult.Discarded(
                    layer = 1,
                    reason = "ruido simulado $indice",
                    preview = "aviso $indice",
                ),
                now = instante,
            )
        }

        val ids = log.entries.value.map { it.id }
        assertEquals(3, ids.size)
        assertEquals(3, ids.toSet().size)
    }
}
