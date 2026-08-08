package me.rerere.rikkahub.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BackgroundModelTaskTest {
    private fun data(messages: List<ChatMessage>): DesktopData {
        val conversation = DesktopConversation(id = "conversation", messages = messages)
        return DesktopData(conversations = listOf(conversation), selectedConversationId = conversation.id)
    }

    private val language = DesktopLanguage.CHINESE_SIMPLIFIED

    // ---------- Title task ----------

    @Test
    fun titleTaskSkipsWhenForcedOffAndAlreadyTitled() {
        val task = DesktopTitleGenerationTask("conversation", force = false, language = language, enableChineseTypography = false)
        val titled = data(listOf(ChatMessage("user", "Hello"))).copy(
            conversations = listOf(DesktopConversation(id = "conversation", title = "Existing", messages = listOf(ChatMessage("user", "Hello"))))
        )

        assertEquals(TaskPrecondition.Skip, task.canRun(titled))
        assertEquals(TaskPrecondition.Run, task.canRun(data(listOf(ChatMessage("user", "Hello")))))
    }

    @Test
    fun titleTaskRunsWhenForcedEvenWithExistingTitle() {
        val task = DesktopTitleGenerationTask("conversation", force = true, language = language, enableChineseTypography = false)
        val titled = data(listOf(ChatMessage("user", "Hello"))).copy(
            conversations = listOf(DesktopConversation(id = "conversation", title = "Existing", messages = listOf(ChatMessage("user", "Hello"))))
        )

        assertEquals(TaskPrecondition.Run, task.canRun(titled))
    }

    @Test
    fun titleTaskValidatesNormalizedOutput() {
        val task = DesktopTitleGenerationTask("conversation", force = true, language = language, enableChineseTypography = false)

        assertEquals(TaskValidation.Pass, task.validate("  生成的标题  ", 0))
        assertEquals(TaskValidation.Fail(desktopText(language, "runtime.title_empty")), task.validate("   ", 0))
    }

    @Test
    fun titleTaskAppliesNormalizedTitle() {
        val task = DesktopTitleGenerationTask("conversation", force = true, language = language, enableChineseTypography = false)
        val state = data(listOf(ChatMessage("user", "Hello")))

        val applied = task.apply(state, "  新标题  ")

        assertEquals("新标题", applied.conversations.single().title)
        assertTrue(applied.conversations.single().updatedAt > 0)
    }

    // ---------- Suggestion task ----------

    @Test
    fun suggestionTaskSkipsEmptyConversation() {
        val task = DesktopSuggestionGenerationTask("conversation", language = language, enableChineseTypography = false)

        assertEquals(TaskPrecondition.Skip, task.canRun(data(emptyList())))
        assertEquals(TaskPrecondition.Run, task.canRun(data(listOf(ChatMessage("user", "Hello")))))
    }

    @Test
    fun suggestionTaskFailsWhenNoSuggestionsParsed() {
        val task = DesktopSuggestionGenerationTask("conversation", language = language, enableChineseTypography = false)

        assertEquals(TaskValidation.Pass, task.validate("nothing useful", 0))
        assertEquals(TaskValidation.Fail(desktopText(language, "runtime.suggestions_empty")), task.validate("", 0))
    }

    @Test
    fun suggestionTaskAppliesParsedSuggestions() {
        val task = DesktopSuggestionGenerationTask("conversation", language = language, enableChineseTypography = false)
        val state = data(listOf(ChatMessage("user", "Hello")))

        val applied = task.apply(state, "第一条\n第二条\n第三条")

        assertEquals(3, applied.conversations.single().suggestions.size)
        assertEquals("第一条", applied.conversations.single().suggestions.first())
    }

    // ---------- Compression task ----------

    @Test
    fun compressionTaskBlocksInvalidArguments() {
        val task = DesktopCompressionTask("conversation", targetTokens = 0, keepRecentMessages = 1, additionalPrompt = "", language = language)
        val state = data(listOf(ChatMessage("user", "Hello"), ChatMessage("assistant", "Hi")))

        assertEquals(
            TaskPrecondition.Blocked(desktopText(language, "runtime.not_enough_messages")),
            task.canRun(state)
        )
    }

    @Test
    fun compressionTaskFailsEmptyOutput() {
        val task = DesktopCompressionTask("conversation", targetTokens = 100, keepRecentMessages = 0, additionalPrompt = "", language = language)

        assertEquals(TaskValidation.Fail(desktopText(language, "runtime.compression_empty")), task.validate("", 0))
        assertEquals(TaskValidation.Pass, task.validate("summary", 0))
    }

    @Test
    fun compressionTaskReplacesHistoryKeepingRecent() {
        val task = DesktopCompressionTask("conversation", targetTokens = 100, keepRecentMessages = 1, additionalPrompt = "", language = language)
        val state = data(listOf(ChatMessage("user", "old 1"), ChatMessage("assistant", "old 2"), ChatMessage("user", "recent")))

        val applied = task.apply(state, "compressed summary")

        val messages = applied.conversations.single().messages
        assertEquals("[历史对话摘要]\ncompressed summary", messages.first().content)
        assertEquals("recent", messages.last().content)
        assertTrue(messages.size < 3)
    }

    // ---------- Translation task ----------

    @Test
    fun translationTaskSkipsMissingMessage() {
        val task = DesktopTranslationTask("conversation", messageIndex = 1, targetLanguage = "简体中文", language = language)

        assertEquals(TaskPrecondition.Skip, task.canRun(data(listOf(ChatMessage("user", "Hello")))))
        assertEquals(TaskPrecondition.Run, task.canRun(data(listOf(ChatMessage("user", "Hello"), ChatMessage("assistant", "Hi")))))
    }

    @Test
    fun translationTaskRetriesUnchangedOutputUntilLastAttempt() {
        val task = DesktopTranslationTask("conversation", messageIndex = 0, targetLanguage = "简体中文", language = language)
        task.request(data(listOf(ChatMessage("user", "Hello"))), attempt = 0)

        assertEquals(TaskValidation.Retry, task.validate("Hello", 0))
        assertEquals(TaskValidation.Retry, task.validate("Hello", 1))
        assertEquals(TaskValidation.Fail(desktopText(language, "runtime.translation_failed")), task.validate("Hello", 2))
    }

    @Test
    fun translationTaskReportsEmptyOnLastAttempt() {
        val task = DesktopTranslationTask("conversation", messageIndex = 0, targetLanguage = "简体中文", language = language)
        task.request(data(listOf(ChatMessage("user", "Hello"))), attempt = 0)

        assertEquals(TaskValidation.Fail(desktopText(language, "runtime.translation_empty")), task.validate("   ", 2))
    }

    @Test
    fun translationTaskBuildsDistinctRetryRequests() {
        val task = DesktopTranslationTask("conversation", messageIndex = 0, targetLanguage = "简体中文", language = language)
        val state = data(listOf(ChatMessage("user", "Hello")))

        val first = task.request(state, attempt = 0)
        val second = task.request(state, attempt = 1)

        assertFalse(first.messages.single().content.contains("previous response copied", ignoreCase = true))
        assertTrue(second.messages.single().content.contains("previous response copied", ignoreCase = true))
        assertEquals(first.config, second.config)
    }

    @Test
    fun translationTaskAppliesTranslatedMessage() {
        val task = DesktopTranslationTask("conversation", messageIndex = 1, targetLanguage = "简体中文", language = language)
        val state = data(listOf(ChatMessage("user", "Hello"), ChatMessage("assistant", "Hi")))

        val applied = task.apply(state, "  你好  ")

        val message = applied.conversations.single().messages[1]
        assertEquals("你好", message.translation)
        assertEquals("简体中文", message.translationTargetLanguage)
    }
}
