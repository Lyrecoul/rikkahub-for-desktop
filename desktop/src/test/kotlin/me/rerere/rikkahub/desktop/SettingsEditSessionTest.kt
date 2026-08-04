package me.rerere.rikkahub.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsEditSessionTest {
    @Test
    fun commitReportsChangedSectionsAndDeletedProviderSecrets() {
        val provider = DesktopProviderProfile(id = "provider", name = "Provider")
        val original = DesktopData(
            providers = listOf(provider),
            selectedProviderId = provider.id,
            conversations = listOf(DesktopConversation(id = "conversation")),
            selectedConversationId = "conversation"
        )
        var session = SettingsEditSession(original)

        session = session.update { data ->
            data.copy(
                preferences = data.preferences.copy(sendOnEnter = false),
                providers = emptyList()
            )
        }

        val commit = session.commit()

        assertTrue(DesktopSettingsSection.INTERACTION in commit.modifiedSections)
        assertEquals(setOf(provider.id), commit.deletedProviderIds)
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
