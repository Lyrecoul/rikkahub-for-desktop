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

    @Test
    fun listsOnlyFavoritesForTheSelectedAssistant() {
        val first = DesktopAssistantProfile(id = "first")
        val second = DesktopAssistantProfile(id = "second")
        val favorite = ChatMessage("assistant", "saved", isFavorite = true)
        val data = DesktopData(
            assistants = listOf(first, second),
            selectedAssistantId = first.id,
            conversations = listOf(
                DesktopConversation(assistantId = first.id, messages = listOf(favorite)),
                DesktopConversation(assistantId = second.id, messages = listOf(ChatMessage("assistant", "other", isFavorite = true)))
            )
        )

        assertEquals(listOf(favorite), data.favoriteMessages(first.id).map { it.second })
    }

    @Test
    fun assistantTagsCanFilterFavoriteConversations() {
        val work = DesktopAssistantProfile(id = "work", tags = setOf("work"))
        val personal = DesktopAssistantProfile(id = "personal", tags = setOf("personal"))
        val data = DesktopData(
            assistants = listOf(work, personal),
            conversations = listOf(
                DesktopConversation(assistantId = work.id, messages = listOf(ChatMessage("assistant", "work", isFavorite = true))),
                DesktopConversation(assistantId = personal.id, messages = listOf(ChatMessage("assistant", "personal", isFavorite = true)))
            )
        )

        val workFavorites = data.favoriteMessages(null).filter { (conversation, _) ->
            data.assistantFor(conversation).tags.contains("work")
        }
        assertEquals(listOf("work"), workFavorites.map { it.second.content })
    }
}
