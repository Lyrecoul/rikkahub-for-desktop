package me.rerere.rikkahub.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopMessageBranchTest {
    @Test
    fun restoringConversationBranchRetainsTheCurrentPath() {
        val original = listOf(ChatMessage("user", "first"), ChatMessage("assistant", "first answer"))
        val replacement = listOf(ChatMessage("user", "replacement"))
        val forked = DesktopConversation(messages = original).fork(replacement, "edited message")

        assertEquals(replacement, forked.messages)
        assertEquals(original, forked.branches.single().messages)

        val restored = forked.restoreBranch(forked.branches.single().id)
        assertEquals(original, restored.messages)
        assertEquals(replacement, restored.branches.single().messages)
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
    fun oldMessagesExposeTheirContentAsSingleVariant() {
        val legacy = ChatMessage(role = "assistant", content = "Legacy response")

        assertEquals(listOf("Legacy response"), legacy.availableVariants().map { it.content })
        assertEquals("Legacy response", legacy.selectVariant(0).content)
    }
}
