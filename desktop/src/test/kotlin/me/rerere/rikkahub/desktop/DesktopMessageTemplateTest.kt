package me.rerere.rikkahub.desktop

import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopMessageTemplateTest {
    @Test
    fun rendersAndroidCompatibleTemplateVariablesForEveryMessage() {
        val assistant = DesktopAssistantProfile(
            messageTemplate = "[{{ role }} {{ date }} {{ time }}] {{ message }}"
        )
        val rendered = assistant.renderMessageTemplate(
            messages = listOf(
                ChatMessage("user", "hello"),
                ChatMessage("assistant", "hi")
            ),
            now = Instant.parse("2026-07-24T12:34:56Z"),
            zoneId = ZoneOffset.UTC
        )

        assertEquals("[user 2026-07-24 12:34:56] hello", rendered[0].content)
        assertEquals("[assistant 2026-07-24 12:34:56] hi", rendered[1].content)
    }

    @Test
    fun rejectsTemplatesWithoutMessageVariableOrWithInvalidSyntax() {
        assertTrue(validateMessageTemplate("Static text").isFailure)
        assertTrue(validateMessageTemplate("{{ message").isFailure)
    }
}
