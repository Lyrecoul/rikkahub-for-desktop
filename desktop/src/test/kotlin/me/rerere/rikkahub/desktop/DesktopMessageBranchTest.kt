package me.rerere.rikkahub.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DesktopMessageBranchTest {
    @Test
    fun restoringConversationBranchRetainsTheCurrentPath() {
        val original = listOf(ChatMessage("user", "first"), ChatMessage("assistant", "first answer"))
        val replacement = listOf(ChatMessage("user", "replacement"))
        val forked = DesktopConversation(messages = original, suggestions = listOf("Current-path suggestion"))
            .fork(replacement, "edited message")

        assertEquals(replacement, forked.messages)
        assertEquals(original, forked.branches.single().messages)

        val restored = forked.restoreBranch(forked.branches.single().id)
        assertEquals(original, restored.messages)
        assertEquals(replacement, restored.branches.single().messages)
        assertEquals(emptyList(), restored.suggestions)
    }

    @Test
    fun deletingConversationBranchLeavesActivePathUntouched() {
        val original = listOf(ChatMessage("user", "first"))
        val forked = DesktopConversation(messages = original).fork(emptyList(), "empty")

        val deleted = forked.deleteBranch(forked.branches.single().id)

        assertEquals(emptyList(), deleted.messages)
        assertEquals(emptyList(), deleted.branches)
    }

    @Test
    fun completingAlternativePreservesPreviousResponse() {
        val original = ChatMessage(
            role = "assistant",
            content = "First response",
            reasoning = "First reasoning",
            createdAt = 100
        )

        val completed = original.beginAlternative()
            .copy(
                content = "Second response",
                reasoning = "Second reasoning",
                createdAt = 200,
                promptTokens = 20,
                completionTokens = 10,
                citations = listOf(DesktopCitation("https://example.com/second", "Second"))
            )
            .completeAlternative()

        assertEquals(listOf("First response", "Second response"), completed.variants.map { it.content })
        assertEquals(1, completed.selectedVariantIndex)
        assertEquals("Second response", completed.content)
        assertEquals(20, completed.promptTokens)
        assertEquals(10, completed.completionTokens)
        assertEquals("https://example.com/second", completed.citations.single().url)
    }

    @Test
    fun regeneratingMessageClearsItsPreviousTranslation() {
        val message = ChatMessage(
            role = "assistant",
            content = "Original",
            translation = "翻译",
            translationTargetLanguage = "中文"
        )

        val alternative = message.beginAlternative()

        assertEquals("", alternative.translation)
        assertEquals("", alternative.translationTargetLanguage)
    }

    @Test
    fun switchesBetweenCompletedResponses() {
        val branched = ChatMessage(role = "assistant", content = "First")
            .beginAlternative()
            .copy(content = "Second")
            .completeAlternative()

        val first = branched.selectVariant(0)
        val second = first.selectVariant(1)

        assertEquals("First", first.content)
        assertEquals(0, first.selectedVariantIndex)
        assertEquals("Second", second.content)
        assertEquals(1, second.selectedVariantIndex)
    }

    @Test
    fun switchingResponseVariantRestoresItsGeneratedImage() {
        val firstImage = DesktopAttachment("first.png", "image/png", "AQID", isImage = true)
        val secondImage = DesktopAttachment("second.png", "image/png", "BAUG", isImage = true)
        val branched = ChatMessage(role = "assistant", content = "First", attachments = listOf(firstImage))
            .beginAlternative()
            .copy(content = "Second", attachments = listOf(secondImage))
            .completeAlternative()

        assertEquals(listOf(firstImage), branched.selectVariant(0).attachments)
        assertEquals(listOf(secondImage), branched.selectVariant(1).attachments)
    }

    @Test
    fun switchingResponseVariantRestoresItsTranslation() {
        val branched = ChatMessage(role = "assistant", content = "First", translation = "翻译")
            .beginAlternative()
            .copy(content = "Second")
            .completeAlternative()

        val first = branched.selectVariant(0)

        assertEquals("First", first.content)
        assertEquals("翻译", first.translation)
    }

    @Test
    fun editingMessageAddsSelectableVariantWithoutForkingConversation() {
        val original = ChatMessage(role = "user", content = "old", translation = "旧翻译")
        val edited = original.addVariant("new")

        assertEquals(listOf("old", "new"), edited.availableVariants().map { it.content })
        assertEquals(1, edited.selectedVariantIndex)
        assertEquals("new", edited.content)
        assertEquals("", edited.translation)
        assertEquals("old", edited.selectVariant(0).content)
        assertEquals("旧翻译", edited.selectVariant(0).translation)
    }

    @Test
    fun editingHistoricalMessageStartsANewPathAndRetainsTheOldOne() {
        val original = listOf(
            ChatMessage("user", "original question"),
            ChatMessage("assistant", "original answer"),
            ChatMessage("user", "follow-up")
        )

        val edited = DesktopConversation(messages = original).editMessageAt(0, "revised question")

        assertEquals(listOf("revised question"), edited.messages.map { it.content })
        assertEquals(
            listOf("original question", "original answer", "follow-up"),
            edited.branches.single().messages.map { it.content })
        assertEquals("original question", edited.messages.single().selectVariant(0).content)
    }

    @Test
    fun editingUserMessagePreparesANewResponseFromTheEditedPath() {
        val original = listOf(
            ChatMessage("user", "original question"),
            ChatMessage("assistant", "original answer"),
            ChatMessage("user", "follow-up")
        )

        val edited = DesktopConversation(messages = original).editMessageAt(0, "revised question")
        val prepared = edited.prepareGeneration(edited.messages)

        assertEquals(listOf("revised question", ""), prepared.messages.map { it.content })
        assertEquals(1, prepared.branches.size)
        assertEquals("编辑前历史", prepared.branches.single().name)
        assertEquals(original, prepared.branches.single().messages)
    }

    @Test
    fun selectingOriginalUserRevisionRestoresItsOriginalReply() {
        val original = listOf(
            ChatMessage("user", "original question"),
            ChatMessage("assistant", "original answer")
        )
        val edited = DesktopConversation(messages = original).editMessageAt(0, "revised question")
        val generated = edited.prepareGeneration(edited.messages).copy(
            messages = listOf(
                edited.messages.single(),
                ChatMessage("assistant", "revised answer")
            )
        )

        val restored = generated.selectMessageVariantAt(messageIndex = 0, variantIndex = 0)

        assertEquals(listOf("original question", "original answer"), restored.messages.map { it.content })
        assertEquals(
            listOf("revised question", "revised answer"),
            restored.branches.single().messages.map { it.content }
        )
    }

    @Test
    fun restoredOriginalUserRevisionCanSwitchBackToTheEditedPath() {
        val original = listOf(
            ChatMessage("user", "original question"),
            ChatMessage("assistant", "original answer")
        )
        val edited = DesktopConversation(messages = original).editMessageAt(0, "revised question")
        val generated = edited.prepareGeneration(edited.messages).copy(
            messages = listOf(
                edited.messages.single(),
                ChatMessage("assistant", "revised answer")
            )
        )

        val originalPath = generated.selectMessageVariantAt(messageIndex = 0, variantIndex = 0)
        val revisedPath = originalPath.selectMessageVariantAt(messageIndex = 0, variantIndex = 1)

        assertEquals(listOf("revised question", "revised answer"), revisedPath.messages.map { it.content })
        assertEquals(
            listOf("original question", "original answer"),
            revisedPath.branches.single().messages.map { it.content }
        )
    }

    @Test
    fun selectingEditedVariantFindsItsBranchWhenToolMessagesShiftIndexes() {
        val user = ChatMessage("user", "original question")
        val revisedUser = user.addVariant("revised question")
        val toolCall = DesktopToolCall("call_1", "shell")
        val active = DesktopConversation(
            messages = listOf(
                ChatMessage("assistant", "", toolCalls = listOf(toolCall)),
                ChatMessage("tool", "output", toolCallId = toolCall.id),
                revisedUser,
                ChatMessage("assistant", "revised answer")
            ),
            branches = listOf(
                DesktopConversationBranch(
                    name = "编辑前历史",
                    messages = listOf(user, ChatMessage("assistant", "original answer"))
                )
            )
        )

        val restored = active.selectMessageVariantAt(messageIndex = 2, variantIndex = 0)

        assertEquals(listOf("original question", "original answer"), restored.messages.map { it.content })
    }

    @Test
    fun switchingEditedMessageVariantsKeepsTheSelectedIndexInSync() {
        val original = listOf(
            ChatMessage("user", "original"),
            ChatMessage("assistant", "original answer")
        )
        val firstEdit = DesktopConversation(messages = original).editMessageAt(0, "first edit")
        val firstPath = firstEdit.prepareGeneration(firstEdit.messages).copy(
            messages = listOf(firstEdit.messages.single(), ChatMessage("assistant", "first answer"))
        )
        val secondEdit = firstPath.editMessageAt(0, "second edit")
        val secondPath = secondEdit.prepareGeneration(secondEdit.messages).copy(
            messages = listOf(secondEdit.messages.single(), ChatMessage("assistant", "second answer"))
        )

        val originalPath = secondPath.selectMessageVariantAt(0, 0)
        val firstPathRestored = originalPath.selectMessageVariantAt(0, 1)
        val secondPathRestored = firstPathRestored.selectMessageVariantAt(0, 2)

        assertEquals(0, originalPath.messages.single { it.role == "user" }.selectedVariantIndex)
        assertEquals(1, firstPathRestored.messages.single { it.role == "user" }.selectedVariantIndex)
        assertEquals(2, secondPathRestored.messages.single { it.role == "user" }.selectedVariantIndex)
        assertEquals("second edit", secondPathRestored.messages.single { it.role == "user" }.content)
    }

    @Test
    fun editingTheLastMessageDoesNotCreateAnUnnecessarySnapshot() {
        val conversation = DesktopConversation(messages = listOf(ChatMessage("user", "draft")))

        val edited = conversation.editMessageAt(0, "revised draft")

        assertEquals("revised draft", edited.messages.single().content)
        assertEquals(emptyList(), edited.branches)
    }

    @Test
    fun deletingHistoricalMessageStartsANewPathAndPreservesTheOriginal() {
        val original = listOf(
            ChatMessage("user", "question"),
            ChatMessage("assistant", "answer"),
            ChatMessage("user", "follow-up")
        )

        val deleted = DesktopConversation(messages = original).deleteMessageAt(1)

        assertEquals(listOf("question"), deleted.messages.map { it.content })
        assertEquals(original, deleted.branches.single().messages)
        assertEquals("删除前历史", deleted.branches.single().name)
    }

    @Test
    fun deletingACollapsedToolTurnRemovesAllOfItsPersistedSteps() {
        val toolCall = DesktopToolCall("call", "search")
        val original = listOf(
            ChatMessage("user", "question"),
            ChatMessage("assistant", "", toolCalls = listOf(toolCall)),
            ChatMessage("tool", "result", toolCallId = toolCall.id),
            ChatMessage("assistant", "answer"),
            ChatMessage("user", "follow-up")
        )
        val turn = assertIs<DesktopChatDisplayItem.AssistantTurn>(buildDesktopChatDisplayItems(original)[1])

        val deleted = DesktopConversation(messages = original).deleteMessageAt(turn.startMessageIndex)

        assertEquals(listOf("question"), deleted.messages.map { it.content })
        assertEquals(original, deleted.branches.single().messages)
    }

    @Test
    fun deletingTheLastMessageDoesNotCreateAnUnnecessarySnapshot() {
        val conversation = DesktopConversation(messages = listOf(ChatMessage("user", "draft")))

        val deleted = conversation.deleteMessageAt(0)

        assertEquals(emptyList(), deleted.messages)
        assertEquals(emptyList(), deleted.branches)
    }

    @Test
    fun regeneratingHistoricalAssistantResponsePreservesTheOriginalPath() {
        val original = listOf(
            ChatMessage("user", "question"),
            ChatMessage("assistant", "first answer"),
            ChatMessage("user", "follow-up")
        )
        val conversation = DesktopConversation(messages = original)

        val regenerated = conversation.prepareGeneration(
            requestMessages = original.take(1),
            alternativeTarget = original[1]
        )

        assertEquals(listOf("question", ""), regenerated.messages.map { it.content })
        assertEquals(
            listOf("question", "first answer", "follow-up"),
            regenerated.branches.single().messages.map { it.content })
        assertEquals("重新生成前历史", regenerated.branches.single().name)
    }

    @Test
    fun regeneratingACollapsedToolTurnStartsBeforeItsFirstStep() {
        val toolCall = DesktopToolCall("call", "search")
        val original = listOf(
            ChatMessage("user", "question"),
            ChatMessage("assistant", "", toolCalls = listOf(toolCall)),
            ChatMessage("tool", "result", toolCallId = toolCall.id),
            ChatMessage("assistant", "answer")
        )
        val turn = assertIs<DesktopChatDisplayItem.AssistantTurn>(buildDesktopChatDisplayItems(original)[1])
        val target = original[turn.startMessageIndex]

        val regenerated = DesktopConversation(messages = original).prepareGeneration(
            requestMessages = original.take(turn.startMessageIndex),
            alternativeTarget = target
        )

        assertEquals(listOf("question", ""), regenerated.messages.map { it.content })
        assertEquals(original, regenerated.branches.single().messages)
    }

    @Test
    fun regeneratingTheLastAssistantKeepsItsVariantWithoutCreatingASnapshot() {
        val original = listOf(ChatMessage("user", "question"), ChatMessage("assistant", "first answer"))
        val conversation = DesktopConversation(messages = original)

        val regenerated = conversation.prepareGeneration(
            requestMessages = original.take(1),
            alternativeTarget = original[1]
        )

        assertTrue(regenerated.branches.isEmpty())
        assertEquals(listOf("first answer"), regenerated.messages.last().availableVariants().map { it.content })
    }

    @Test
    fun startingGenerationClearsTheCurrentConversationsUnsentAttachments() {
        val attachment = DesktopAttachment("notes.txt", "text/plain", "draft")
        val conversation = DesktopConversation(
            messages = listOf(ChatMessage("user", "previous")),
            draft = "new message",
            draftAttachments = listOf(attachment)
        )

        val prepared = conversation.prepareGeneration(conversation.messages + ChatMessage("user", "new message"))

        assertEquals("", prepared.draft)
        assertTrue(prepared.draftAttachments.isEmpty())
    }

    @Test
    fun preparingGenerationDoesNotChangeTheOriginalDraftUntilTheRequestStarts() {
        val attachment = DesktopAttachment("notes.txt", "text/plain", "draft")
        val conversation = DesktopConversation(
            messages = listOf(ChatMessage("user", "previous")),
            draft = "new message",
            draftAttachments = listOf(attachment)
        )

        val prepared = conversation.prepareGeneration(conversation.messages + ChatMessage("user", "new message"))

        assertEquals("new message", conversation.draft)
        assertEquals(listOf(attachment), conversation.draftAttachments)
        assertEquals("", prepared.draft)
    }

    @Test
    fun translationIsStoredOnTheSelectedVariant() {
        val message = ChatMessage(role = "assistant", content = "First")
            .beginAlternative()
            .copy(content = "Second")
            .completeAlternative()
            .withTranslation("Second translated", "English")

        assertEquals("", message.selectVariant(0).translation)
        assertEquals("Second translated", message.selectVariant(1).translation)
    }

    @Test
    fun editedMessageCanClearItsStaleTranslation() {
        val edited = ChatMessage(
            role = "user",
            content = "old",
            translation = "旧翻译",
            translationTargetLanguage = "中文"
        ).copy(content = "new", translation = "", translationTargetLanguage = "")

        assertEquals("new", edited.content)
        assertEquals("", edited.translation)
        assertEquals("", edited.translationTargetLanguage)
    }

    @Test
    fun oldMessagesExposeTheirContentAsSingleVariant() {
        val legacy = ChatMessage(role = "assistant", content = "Legacy response")

        assertEquals(listOf("Legacy response"), legacy.availableVariants().map { it.content })
        assertEquals("Legacy response", legacy.selectVariant(0).content)
    }

    @Test
    fun legacyVariantsRetainSharedAttachments() {
        val image = DesktopAttachment("legacy.png", "image/png", "AQID", isImage = true)
        val legacy = ChatMessage(
            role = "assistant",
            content = "Legacy response",
            attachments = listOf(image),
            variants = listOf(DesktopMessageVariant(content = "Legacy response"))
        )

        assertEquals(listOf(image), legacy.selectVariant(0).attachments)
    }

    @Test
    fun forksToAnIndependentConversationAndPreservesConversationSettings() {
        val source = DesktopConversation(
            title = "Project",
            assistantId = "assistant",
            folderId = "folder",
            systemPrompt = "custom",
            webSearchEnabled = true,
            messages = listOf(ChatMessage("user", "one"), ChatMessage("assistant", "two"), ChatMessage("user", "three"))
        )

        val fork = source.forkAtMessage(1)

        assertEquals("Project", fork.title)
        assertEquals(source.id, fork.parentConversationId)
        assertEquals(source.assistantId, fork.assistantId)
        assertEquals(source.folderId, fork.folderId)
        assertEquals(source.systemPrompt, fork.systemPrompt)
        assertEquals(true, fork.webSearchEnabled)
        assertEquals(listOf("one", "two"), fork.messages.map { it.content })
    }

    @Test
    fun normalizesGeneratedConversationTitles() {
        assertEquals("Kotlin coroutine notes", normalizeGeneratedTitle(" \"Kotlin\n coroutine notes\" "))
        assertEquals(48, normalizeGeneratedTitle("x".repeat(60)).length)
    }

    @Test
    fun parsesNumberedAndBulletedReplySuggestions() {
        assertEquals(
            listOf("Explain the first option", "Show an example", "What are the tradeoffs?"),
            parseChatSuggestions("1. Explain the first option\n- Show an example\n* What are the tradeoffs?")
        )
    }

    @Test
    fun appliesChineseTypographyToGeneratedTitlesAndSuggestionsOnlyWhenEnabled() {
        assertEquals("你好World", normalizeGeneratedTitle("你好World"))
        assertEquals("你好 World", normalizeGeneratedTitle("你好World", enableChineseTypography = true))
        assertEquals(
            listOf("使用Kotlin"),
            parseChatSuggestions("使用Kotlin")
        )
        assertEquals(
            listOf("使用 Kotlin"),
            parseChatSuggestions("使用Kotlin", enableChineseTypography = true)
        )
    }

    @Test
    fun recordsReasoningDurationOnlyOnce() {
        val message = ChatMessage("assistant", "", reasoning = "thinking", reasoningStartedAt = 1_000)

        assertEquals(2_500, message.completeReasoningDuration(now = 3_500).reasoningDurationMillis)
        assertEquals(
            500,
            message.copy(reasoningDurationMillis = 500).completeReasoningDuration(now = 3_500).reasoningDurationMillis
        )
        assertEquals(null, ChatMessage("assistant", "").completeReasoningDuration(now = 3_500).reasoningDurationMillis)
        assertEquals(
            null,
            ChatMessage(
                "assistant",
                "",
                reasoning = "thinking"
            ).completeReasoningDuration(now = 3_500).reasoningDurationMillis
        )
    }

    @Test
    fun allowsOnlyWebUrlsForExternalCitationLinks() {
        assertEquals(true, isSafeExternalUrl("https://example.com/source"))
        assertEquals(true, isSafeExternalUrl("http://example.com/source"))
        assertEquals(false, isSafeExternalUrl("file:///home/user/private.txt"))
        assertEquals(false, isSafeExternalUrl("javascript:alert(1)"))
        assertEquals(false, isSafeExternalUrl("not a url"))
    }

    @Test
    fun aggregatesConversationStatistics() {
        val conversation = DesktopConversation(
            messages = listOf(
                ChatMessage(
                    "user",
                    "question",
                    promptTokens = 12,
                    attachments = listOf(DesktopAttachment("a.txt", "text/plain", "a"))
                ),
                ChatMessage("assistant", "answer", reasoning = "thinking", completionTokens = 8)
            )
        )

        assertEquals(
            DesktopConversationStats(2, 1, 1, 1, "questionanswerthinking".length, 12, 8, 0),
            conversation.stats()
        )
    }

    @Test
    fun conversationDraftIsIndependentFromItsMessages() {
        val conversation = DesktopConversation(
            messages = listOf(ChatMessage("user", "saved message")),
            draft = "unfinished follow-up"
        )

        assertEquals("unfinished follow-up", conversation.draft)
        assertEquals("saved message", conversation.messages.single().content)
    }

    @Test
    fun assistantTagsArePersistedOnTheProfile() {
        val assistant = DesktopAssistantProfile(tags = setOf("work", "coding"))
        assertEquals(setOf("work", "coding"), assistant.tags)
    }
}
