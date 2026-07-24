package me.rerere.rikkahub.desktop

import io.pebbletemplates.pebble.PebbleEngine
import java.io.StringWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val templateEngine = PebbleEngine.Builder().build()

internal fun validateMessageTemplate(template: String): Result<Unit> = runCatching {
    require("{{ message }}" in template) { "Template must contain {{ message }}" }
    templateEngine.getLiteralTemplate(template)
}

internal fun DesktopAssistantProfile.renderMessageTemplate(
    messages: List<ChatMessage>,
    now: Instant = Instant.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): List<ChatMessage> {
    val template = templateEngine.getLiteralTemplate(messageTemplate)
    val dateTime = now.atZone(zoneId)
    val variables = mapOf(
        "date" to dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE),
        "time" to dateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    )
    return messages.map { message ->
        val output = StringWriter()
        template.evaluate(
            output,
            variables + mapOf("message" to message.content, "role" to message.role)
        )
        message.copy(content = output.toString())
    }
}
