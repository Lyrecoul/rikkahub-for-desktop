package me.rerere.rikkahub.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopConversationFilterTest {
    @Test
    fun keepsForkedConversationsNextToTheirParentInTheSidebar() {
        val parent = DesktopConversation(id = "parent", title = "Project", updatedAt = 10)
        val branch =
            DesktopConversation(id = "branch", title = "Project", parentConversationId = parent.id, updatedAt = 30)
        val nestedBranch =
            DesktopConversation(id = "nested", title = "Project", parentConversationId = branch.id, updatedAt = 20)
        val other = DesktopConversation(id = "other", title = "Other", updatedAt = 40)

        assertEquals(
            listOf("other" to 0, "parent" to 0, "branch" to 1, "nested" to 2),
            listOf(other, branch, nestedBranch, parent).asConversationTree()
                .map { it.conversation.id to it.branchDepth }
        )
    }

    @Test
    fun promotesChildrenWhenDeletingTheirParentAndMovesABranchTreeTogether() {
        val assistant = DesktopAssistantProfile(id = "assistant")
        val parent = DesktopConversation(id = "parent", assistantId = assistant.id, folderId = "inbox")
        val branch = DesktopConversation(
            id = "branch",
            assistantId = assistant.id,
            parentConversationId = parent.id,
            folderId = "inbox"
        )
        val nestedBranch = DesktopConversation(
            id = "nested",
            assistantId = assistant.id,
            parentConversationId = branch.id,
            folderId = "inbox"
        )
        val data = DesktopData(
            assistants = listOf(assistant),
            selectedAssistantId = assistant.id,
            folders = listOf(DesktopFolder(id = "archive", assistantId = assistant.id, name = "Archive")),
            conversations = listOf(parent, branch, nestedBranch),
            selectedConversationId = parent.id
        )

        val moved = data.moveConversationToFolder(parent.id, "archive")
        assertEquals(listOf("archive", "archive", "archive"), moved.conversations.map { it.folderId })

        val deleted = moved.deleteConversation(parent.id)
        assertNull(deleted.conversations.single { it.id == branch.id }.parentConversationId)
        assertEquals(branch.id, deleted.conversations.single { it.id == nestedBranch.id }.parentConversationId)
    }

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
    fun sortsConversationsByRecentActivityOrMessageCountWhileKeepingPinnedFirst() {
        val assistant = DesktopAssistantProfile(id = "assistant")
        val pinned = DesktopConversation(
            id = "pinned",
            assistantId = assistant.id,
            isPinned = true,
            messages = listOf(ChatMessage("user", "one")),
            updatedAt = 10
        )
        val recent = DesktopConversation(
            id = "recent",
            assistantId = assistant.id,
            messages = listOf(ChatMessage("user", "one")),
            updatedAt = 30
        )
        val active = DesktopConversation(
            id = "active",
            assistantId = assistant.id,
            messages = List(3) { ChatMessage("user", "message $it") },
            updatedAt = 20
        )
        val data = DesktopData(
            assistants = listOf(assistant),
            selectedAssistantId = assistant.id,
            conversations = listOf(recent, active, pinned),
            selectedConversationId = recent.id
        )

        assertEquals(
            listOf("pinned", "recent", "active"),
            data.filteredConversations("", null, DesktopConversationSort.RECENT).map { it.id }
        )
        assertEquals(
            listOf("pinned", "active", "recent"),
            data.filteredConversations("", null, DesktopConversationSort.MOST_ACTIVE).map { it.id }
        )
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
                DesktopConversation(
                    assistantId = second.id,
                    messages = listOf(ChatMessage("assistant", "other", isFavorite = true))
                )
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
                DesktopConversation(
                    assistantId = work.id,
                    messages = listOf(ChatMessage("assistant", "work", isFavorite = true))
                ),
                DesktopConversation(
                    assistantId = personal.id,
                    messages = listOf(ChatMessage("assistant", "personal", isFavorite = true))
                )
            )
        )

        val workFavorites = data.favoriteMessages(null).filter { (conversation, _) ->
            data.assistantFor(conversation).tags.contains("work")
        }
        assertEquals(listOf("work"), workFavorites.map { it.second.content })
    }
}
