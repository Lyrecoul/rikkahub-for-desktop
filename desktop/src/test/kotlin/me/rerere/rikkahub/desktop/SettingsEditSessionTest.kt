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
}
