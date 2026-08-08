package me.rerere.rikkahub.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsEditSessionTest {
    @Test
    fun commitReportsChangedSectionsAndDeletedProviderSecrets() {
        val provider = DesktopProviderProfile(id = "provider", name = "Provider")
        val remainingProvider = DesktopProviderProfile(id = "remaining", name = "Remaining")
        val original = DesktopData(
            providers = listOf(provider, remainingProvider),
            selectedProviderId = provider.id,
            conversations = listOf(DesktopConversation(id = "conversation")),
            selectedConversationId = "conversation"
        )
        var session = SettingsEditSession(original)

        session = session.update { data ->
            data.copy(
                preferences = data.preferences.copy(sendOnEnter = false),
                providers = listOf(remainingProvider)
            )
        }

        val commit = requireNotNull(session.commitOrNull())

        assertTrue(DesktopSettingsSection.INTERACTION in commit.modifiedSections)
        assertEquals(setOf(provider.id), commit.deletedProviderIds)
    }

    @Test
    fun exposesModifiedProfileIdsFromTheOriginalSnapshot() {
        val provider = DesktopProviderProfile(id = "provider", name = "Original")
        val assistant = DesktopAssistantProfile(id = "assistant", name = "Original")
        val original = DesktopData(
            providers = listOf(provider),
            selectedProviderId = provider.id,
            assistants = listOf(assistant),
            selectedAssistantId = assistant.id
        )
        val session = SettingsEditSession(original).update { data ->
            data.copy(
                providers = listOf(provider.copy(name = "Changed")),
                assistants = listOf(assistant.copy(name = "Changed"))
            )
        }

        assertEquals(setOf(provider.id), session.modifiedProviderIds)
        assertEquals(setOf(assistant.id), session.modifiedAssistantIds)
    }

    @Test
    fun stagesLanguageAssistantAndProviderDraftsAtomically() {
        val provider = DesktopProviderProfile(id = "provider", name = "Provider")
        val assistant = DesktopAssistantProfile(id = "assistant", name = "Assistant")
        val original = DesktopData(
            providers = listOf(provider),
            selectedProviderId = provider.id,
            assistants = listOf(assistant),
            selectedAssistantId = assistant.id
        )

        val staged = SettingsEditSession(original).stageCurrentDrafts(
            language = DesktopLanguage.CHINESE_SIMPLIFIED,
            assistant = assistant.copy(name = "Changed assistant"),
            provider = provider.copy(name = "Changed provider")
        )
        val commit = requireNotNull(staged.commitOrNull())

        assertEquals(DesktopLanguage.CHINESE_SIMPLIFIED, commit.data.preferences.language)
        assertEquals("Changed assistant", commit.data.assistants.single().name)
        assertEquals("Changed provider", commit.data.providers.single().name)
    }

    @Test
    fun invalidProviderOrAssistantCannotCommit() {
        val invalidProvider = SettingsEditSession(DesktopData()).update { data ->
            data.copy(providers = listOf(DesktopProviderProfile(name = "", config = DesktopConfig(model = ""))))
        }
        val invalidAssistant = SettingsEditSession(DesktopData()).update { data ->
            data.copy(assistants = listOf(DesktopAssistantProfile(name = "")))
        }
        val emptyProfiles = SettingsEditSession(DesktopData()).update { data ->
            data.copy(providers = emptyList(), assistants = emptyList())
        }

        assertEquals(null, invalidProvider.commitOrNull())
        assertEquals(null, invalidAssistant.commitOrNull())
        assertEquals(null, emptyProfiles.commitOrNull())
    }

    @Test
    fun invalidAssistantDetailsCannotCommit() {
        val invalidTemplate = SettingsEditSession(DesktopData()).update { data ->
            data.copy(assistants = listOf(DesktopAssistantProfile(messageTemplate = "no message placeholder")))
        }
        val invalidRegex = SettingsEditSession(DesktopData()).update { data ->
            data.copy(assistants = listOf(DesktopAssistantProfile(regexRules = listOf(DesktopRegexRule(findRegex = "[")))))
        }
        val invalidBody = SettingsEditSession(DesktopData()).update { data ->
            data.copy(assistants = listOf(DesktopAssistantProfile(customBodies = listOf(DesktopCustomBody("x", "not json")))))
        }

        assertEquals(null, invalidTemplate.commitOrNull())
        assertEquals(null, invalidRegex.commitOrNull())
        assertEquals(null, invalidBody.commitOrNull())
    }

    @Test
    fun discardRestoresTheOriginalData() {
        val original = DesktopData(preferences = DesktopPreferences(userNickname = "Original"))
        var session = SettingsEditSession(original)
        session = session.update { it.copy(preferences = it.preferences.copy(userNickname = "Changed")) }

        session = session.discard()

        assertFalse(session.hasChanges)
        assertEquals("Original", session.draft.preferences.userNickname)
    }

    @Test
    fun commitDataCarriesDeletionRemappingForConversationsAndFolders() {
        val provider = DesktopProviderProfile(id = "provider", name = "Provider")
        val assistant = DesktopAssistantProfile(id = "assistant", name = "Assistant")
        val fallback = DesktopAssistantProfile(id = "fallback", name = "Fallback")
        val original = DesktopData(
            providers = listOf(provider),
            selectedProviderId = provider.id,
            assistants = listOf(assistant, fallback),
            selectedAssistantId = assistant.id,
            conversations = listOf(DesktopConversation(id = "conversation", assistantId = assistant.id)),
            selectedConversationId = "conversation"
        )
        // SettingsPane 的删除路径：删除直接在 draft 上应用（含 conversations/folders 重映射）
        val session = SettingsEditSession(original).update { it.deleteAssistantProfile(assistant.id) }

        val commit = requireNotNull(session.commitOrNull())

        // commit.data 已经是完整可应用的结果：旧调用方对主 data 再跑 fold 是冗余的
        assertEquals(listOf(fallback.id), commit.data.assistants.map { it.id })
        assertEquals(fallback.id, commit.data.conversations.single().assistantId)
        assertEquals(fallback.id, commit.data.selectedAssistantId)
    }
}
