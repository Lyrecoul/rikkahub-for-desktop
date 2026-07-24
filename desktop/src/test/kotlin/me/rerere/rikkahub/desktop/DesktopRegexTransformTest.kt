package me.rerere.rikkahub.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopRegexTransformTest {
    private val assistant = DesktopAssistantProfile(
        regexRules = listOf(
            DesktopRegexRule(
                name = "Hide email before request",
                findRegex = "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}",
                replaceString = "[EMAIL]",
                roles = setOf("user")
            ),
            DesktopRegexRule(
                name = "Clean assistant tag",
                findRegex = "<answer>|</answer>",
                replaceString = "",
                roles = setOf("assistant")
            ),
            DesktopRegexRule(
                name = "Visual secret",
                findRegex = "secret",
                replaceString = "***",
                roles = setOf("assistant"),
                visualOnly = true
            )
        )
    )

    @Test
    fun transformsOnlyUserMessagesBeforeRequest() {
        val messages = assistant.transformRequestMessages(
            listOf(
                ChatMessage("user", "Contact me@example.com"),
                ChatMessage("assistant", "me@example.com")
            )
        )

        assertEquals("Contact [EMAIL]", messages[0].content)
        assertEquals("me@example.com", messages[1].content)
    }

    @Test
    fun transformsAssistantOutputAndKeepsVisualRulesSeparate() {
        val generated = assistant.transformGeneratedMessage(
            ChatMessage("assistant", "<answer>a secret</answer>")
        )

        assertEquals("a secret", generated.content)
        assertEquals("a ***", assistant.applyRegexRules(generated.content, "assistant", visualOnly = true))
    }

    @Test
    fun invalidRegexDoesNotDestroyContent() {
        val invalid = DesktopAssistantProfile(
            regexRules = listOf(
                DesktopRegexRule(findRegex = "[", replaceString = "x", roles = setOf("user"))
            )
        )

        assertEquals("original", invalid.applyRegexRules("original", "user", visualOnly = false))
    }
}
