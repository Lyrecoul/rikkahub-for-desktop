package me.rerere.rikkahub.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopConversationFilterTest {
    @Test
    fun combinesAssistantAndFullTextFilters() {
        val firstAssistant = DesktopAssistantProfile(id = "first", name = "First")
        val secondAssistant = DesktopAssistantProfile(id = "second", name = "Second")
        val firstConversation = DesktopConversation(
            id = "first-conversation",
            assistantId = firstAssistant.id,
            title = "Kotlin notes",
            messages = listOf(ChatMessage("user", "coroutines")),
            updatedAt = 100
        )
        val secondConversation = DesktopConversation(
            id = "second-conversation",
            assistantId = secondAssistant.id,
            title = "Files",
            messages = listOf(
                ChatMessage(
                    "user",
                    "review",
                    attachments = listOf(DesktopAttachment("release-notes.md", "text/markdown", "content"))
                )
            ),
            updatedAt = 200
        )
        val data = DesktopData(
            assistants = listOf(firstAssistant, secondAssistant),
            selectedAssistantId = firstAssistant.id,
            conversations = listOf(firstConversation, secondConversation),
            selectedConversationId = firstConversation.id
        )

        assertEquals(listOf(secondConversation), data.filteredConversations("release-notes", null))
        assertEquals(listOf(firstConversation), data.filteredConversations("", firstAssistant.id))
        assertEquals(emptyList(), data.filteredConversations("Kotlin", secondAssistant.id))
    }
}
