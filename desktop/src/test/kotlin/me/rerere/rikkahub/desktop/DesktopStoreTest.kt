package me.rerere.rikkahub.desktop

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFails

class DesktopStoreTest {
    private class MemorySecrets : DesktopSecretStore {
        val values = mutableMapOf<String, String>()
        override fun read(id: String): String? = values[id]
        override fun write(id: String, value: String): Boolean {
            values[id] = value
            return true
        }

        override fun delete(id: String): Boolean {
            values.remove(id)
            return true
        }
    }

    @Test
    fun savesAndLoadsDesktopData() {
        val directory = Files.createTempDirectory("rikkahub-desktop-store")
        val store = DesktopStore(directory.resolve("desktop.json"))
        val conversation = DesktopConversation(
            title = "Saved conversation",
            messages = listOf(ChatMessage("user", "hello"))
        )
        val expected = DesktopData(
            config = DesktopConfig(model = "test-model"),
            conversations = listOf(conversation),
            selectedConversationId = conversation.id
        )

        store.save(expected)

        assertEquals(expected.normalized(), store.load())
    }

    @Test
    fun loadsDataWrittenBeforeMessageIdsAndConversationMetadata() {
        val directory = Files.createTempDirectory("rikkahub-desktop-legacy-store")
        val dataFile = directory.resolve("desktop.json")
        Files.writeString(
            dataFile,
            """
            {
              "config": { "model": "legacy-model" },
              "conversations": [
                {
                  "id": "legacy-conversation",
                  "title": "Legacy",
                  "messages": [{ "role": "user", "content": "hello" }]
                }
              ],
              "selectedConversationId": "legacy-conversation"
            }
            """.trimIndent()
        )

        val loaded = DesktopStore(dataFile).load()

        assertEquals("legacy-model", loaded.config.model)
        assertEquals("legacy-model", loaded.activeProvider().config.model)
        assertEquals(1, loaded.providers.size)
        assertEquals(1, loaded.assistants.size)
        assertEquals(loaded.activeAssistant(), loaded.assistantFor(loaded.conversations.single()))
        assertEquals(DesktopColorMode.SYSTEM, loaded.preferences.colorMode)
        assertTrue(loaded.preferences.enableAutoScroll)
        assertFalse(loaded.conversations.single().isPinned)
        assertEquals("", loaded.conversations.single().systemPrompt)
        assertTrue(loaded.conversations.single().messages.single().id.isNotBlank())
        assertEquals("", loaded.conversations.single().messages.single().reasoning)
        assertEquals(emptyList(), loaded.conversations.single().messages.single().variants)
    }

    @Test
    fun preservesImagesWhenLoadingBackupsWrittenBeforeAttachmentKinds() {
        val directory = Files.createTempDirectory("rikkahub-desktop-legacy-attachments")
        val dataFile = directory.resolve("desktop.json")
        Files.writeString(
            dataFile,
            """
            {
              "conversations": [{
                "id": "legacy-conversation",
                "messages": [{
                  "role": "user",
                  "content": "inspect",
                  "attachments": [{
                    "name": "photo.png",
                    "mimeType": "image/png",
                    "data": "AQID",
                    "isImage": true
                  }]
                }]
              }],
              "selectedConversationId": "legacy-conversation"
            }
            """.trimIndent()
        )

        val attachment = DesktopStore(dataFile).load().conversations.single().messages.single().attachments.single()

        assertEquals(DesktopAttachmentKind.IMAGE, attachment.kind)
        assertTrue(attachment.isImage)
    }

    @Test
    fun exportsAndImportsNormalizedBackup() {
        val directory = Files.createTempDirectory("rikkahub-desktop-backup")
        val store = DesktopStore(directory.resolve("desktop.json"))
        val backup = directory.resolve("backup.json")
        val invalidSelections = DesktopData(
            providers = emptyList(),
            selectedProviderId = "missing-provider",
            assistants = emptyList(),
            selectedAssistantId = "missing-assistant",
            conversations = emptyList(),
            selectedConversationId = "missing-conversation"
        )

        store.exportData(backup, invalidSelections)
        val imported = store.importData(backup)

        assertEquals(1, imported.providers.size)
        assertEquals(imported.providers.first().id, imported.selectedProviderId)
        assertEquals(1, imported.assistants.size)
        assertEquals(imported.assistants.first().id, imported.selectedAssistantId)
        assertEquals(1, imported.conversations.size)
        assertEquals(imported.conversations.first().id, imported.selectedConversationId)
    }

    @Test
    fun invalidBackupDoesNotOverwriteCurrentStore() {
        val directory = Files.createTempDirectory("rikkahub-desktop-invalid-backup")
        val store = DesktopStore(directory.resolve("desktop.json"))
        val current = DesktopData(config = DesktopConfig(model = "current-model"))
        val invalidBackup = directory.resolve("invalid.json")
        store.save(current)
        Files.writeString(invalidBackup, "not json")

        assertFails { store.importData(invalidBackup) }
        assertEquals("current-model", store.load().config.model)
    }

    @Test
    fun corruptPrimaryDataIsQuarantinedInsteadOfBeingSilentlyOverwritten() {
        val directory = Files.createTempDirectory("rikkahub-desktop-corrupt")
        val dataFile = directory.resolve("desktop.json")
        Files.writeString(dataFile, "not json")

        val loaded = DesktopStore(dataFile).load()

        assertEquals(1, loaded.schemaVersion)
        assertFalse(Files.exists(dataFile))
        Files.list(directory).use { files ->
            assertTrue(files.anyMatch { it.fileName.toString().startsWith("desktop.json.corrupt-") })
        }
    }

    @Test
    fun savesKeysInSecretStoreInsteadOfTheJsonFile() {
        val directory = Files.createTempDirectory("rikkahub-desktop-secrets")
        val secrets = MemorySecrets()
        val provider = DesktopProviderProfile(config = DesktopConfig(apiKey = "provider-secret"))
        val data = DesktopData(
            providers = listOf(provider),
            selectedProviderId = provider.id,
            webSearchSettings = DesktopWebSearchSettings(apiKey = "brave-secret")
        )
        val file = directory.resolve("desktop.json")
        val store = DesktopStore(file, secrets)

        store.save(data)

        val raw = Files.readString(file)
        assertFalse(raw.contains("provider-secret"))
        assertFalse(raw.contains("brave-secret"))
        assertEquals("provider-secret", store.load().activeProvider().config.apiKey)
        assertEquals("brave-secret", store.load().webSearchSettings.apiKey)
    }

    @Test
    fun clearsStoredKeysWhenAKeyIsRemovedOrDataIsReset() {
        val directory = Files.createTempDirectory("rikkahub-desktop-clear-secrets")
        val secrets = MemorySecrets()
        val provider = DesktopProviderProfile(config = DesktopConfig(apiKey = "provider-secret"))
        val store = DesktopStore(directory.resolve("desktop.json"), secrets)
        val data = DesktopData(
            providers = listOf(provider),
            selectedProviderId = provider.id,
            webSearchSettings = DesktopWebSearchSettings(apiKey = "brave-secret")
        )

        store.save(data)
        store.clearSecrets(data)

        assertTrue(secrets.values.isEmpty())
        store.save(data.copy(providers = listOf(provider.copy(config = provider.config.copy(apiKey = "")))))
        assertEquals("", store.load().activeProvider().config.apiKey)
    }

    @Test
    fun exportsRedactedDataWithoutTouchingTheSecretStore() {
        val directory = Files.createTempDirectory("rikkahub-desktop-redacted-export")
        val secrets = MemorySecrets()
        val provider = DesktopProviderProfile(config = DesktopConfig(apiKey = "provider-secret"))
        val store = DesktopStore(directory.resolve("desktop.json"), secrets)
        val backup = directory.resolve("backup.json")

        store.exportData(backup, DesktopData(providers = listOf(provider), selectedProviderId = provider.id))

        assertFalse(Files.readString(backup).contains("provider-secret"))
        assertTrue(secrets.values.isEmpty())
    }

    @Test
    fun preservesMessageFavoritesAcrossPersistence() {
        val directory = Files.createTempDirectory("rikkahub-desktop-favorites")
        val conversation = DesktopConversation(messages = listOf(ChatMessage("assistant", "saved", isFavorite = true)))
        val store = DesktopStore(directory.resolve("desktop.json"), MemorySecrets())

        store.save(DesktopData(conversations = listOf(conversation), selectedConversationId = conversation.id))

        assertTrue(store.load().conversations.single().messages.single().isFavorite)
    }

    @Test
    fun preservesUnsentAttachmentsWithTheirConversationDraft() {
        val directory = Files.createTempDirectory("rikkahub-desktop-draft-attachments")
        val conversation = DesktopConversation(
            draft = "Review this recording",
            draftAttachments = listOf(
                DesktopAttachment("voice.wav", "audio/wav", "BAUG", kind = DesktopAttachmentKind.AUDIO)
            )
        )
        val store = DesktopStore(directory.resolve("desktop.json"), MemorySecrets())

        store.save(DesktopData(conversations = listOf(conversation), selectedConversationId = conversation.id))

        val restored = store.load().conversations.single()
        assertEquals("Review this recording", restored.draft)
        assertEquals(DesktopAttachmentKind.AUDIO, restored.draftAttachments.single().kind)
    }

    @Test
    fun removesInvalidFoldersWhenLoadingOlderOrEditedBackups() {
        val assistant = DesktopAssistantProfile(id = "assistant-1")
        val validFolder = DesktopFolder(id = "folder-1", assistantId = assistant.id, name = "Work")
        val normalized = DesktopData(
            assistants = listOf(assistant),
            selectedAssistantId = assistant.id,
            folders = listOf(validFolder, DesktopFolder(id = "missing", assistantId = "missing", name = "Invalid")),
            conversations = listOf(
                DesktopConversation(assistantId = assistant.id, folderId = validFolder.id),
                DesktopConversation(assistantId = assistant.id, folderId = "missing")
            )
        ).normalized()

        assertEquals(listOf(validFolder), normalized.folders)
        assertEquals(validFolder.id, normalized.conversations[0].folderId)
        assertEquals(null, normalized.conversations[1].folderId)
    }

    @Test
    fun deletingFolderKeepsItsConversationsAsUnfiled() {
        val folder = DesktopFolder(id = "folder", assistantId = "assistant", name = "Work")
        val data = DesktopData(
            folders = listOf(folder),
            conversations = listOf(DesktopConversation(folderId = folder.id), DesktopConversation())
        )

        val updated = data.renameFolder(folder.id, "  Renamed  ").deleteFolder(folder.id)

        assertTrue(updated.folders.isEmpty())
        assertEquals(listOf(null, null), updated.conversations.map { it.folderId })
    }

    @Test
    fun folderOperationsKeepFoldersScopedToTheirAssistant() {
        val firstAssistant = DesktopAssistantProfile(id = "first")
        val secondAssistant = DesktopAssistantProfile(id = "second")
        val conversation = DesktopConversation(id = "conversation", assistantId = firstAssistant.id)
        val secondFolder = DesktopFolder(id = "second-folder", assistantId = secondAssistant.id, name = "Second")
        val data = DesktopData(
            assistants = listOf(firstAssistant, secondAssistant),
            selectedAssistantId = firstAssistant.id,
            folders = listOf(secondFolder),
            conversations = listOf(conversation),
            selectedConversationId = conversation.id
        )

        val unchanged = data.moveConversationToFolder(conversation.id, secondFolder.id)
        val created = data.createFolder(
            DesktopFolder(id = "first-folder", assistantId = firstAssistant.id, name = "First"),
            conversation.id
        )

        assertEquals(null, unchanged.conversations.single().folderId)
        assertEquals("first-folder", created.conversations.single().folderId)
        assertEquals(2, created.folders.size)
    }

    @Test
    fun normalizationClearsFoldersOwnedByAnotherAssistant() {
        val firstAssistant = DesktopAssistantProfile(id = "first")
        val secondAssistant = DesktopAssistantProfile(id = "second")
        val folder = DesktopFolder(id = "second-folder", assistantId = secondAssistant.id, name = "Second")

        val normalized = DesktopData(
            assistants = listOf(firstAssistant, secondAssistant),
            selectedAssistantId = firstAssistant.id,
            folders = listOf(folder),
            conversations = listOf(DesktopConversation(assistantId = firstAssistant.id, folderId = folder.id))
        ).normalized()

        assertEquals(null, normalized.conversations.single().folderId)
    }

    @Test
    fun clearsFolderFilterWhenSwitchingToAnotherAssistantsScope() {
        val firstAssistant = DesktopAssistantProfile(id = "first")
        val secondAssistant = DesktopAssistantProfile(id = "second")
        val firstFolder = DesktopFolder(id = "first-folder", assistantId = firstAssistant.id, name = "First")
        val data = DesktopData(
            assistants = listOf(firstAssistant, secondAssistant),
            selectedAssistantId = firstAssistant.id,
            folders = listOf(firstFolder)
        )

        assertEquals(null, data.folderFilterForAssistant(firstFolder.id, secondAssistant.id))
        assertEquals(firstFolder.id, data.folderFilterForAssistant(firstFolder.id, firstAssistant.id))
    }

    @Test
    fun movingConversationToAnotherAssistantClearsItsOldFolder() {
        val firstAssistant = DesktopAssistantProfile(id = "first")
        val secondAssistant = DesktopAssistantProfile(id = "second")
        val firstFolder = DesktopFolder(id = "first-folder", assistantId = firstAssistant.id, name = "First")
        val conversation = DesktopConversation(id = "conversation", assistantId = firstAssistant.id, folderId = firstFolder.id)
        val data = DesktopData(
            assistants = listOf(firstAssistant, secondAssistant),
            selectedAssistantId = firstAssistant.id,
            folders = listOf(firstFolder),
            conversations = listOf(conversation),
            selectedConversationId = conversation.id
        )

        val moved = data.assignAssistantToConversation(conversation.id, secondAssistant.id)

        assertEquals(secondAssistant.id, moved.conversations.single().assistantId)
        assertEquals(null, moved.conversations.single().folderId)
    }

    @Test
    fun deletingTheLastConversationCreatesOneForTheActiveAssistant() {
        val assistant = DesktopAssistantProfile(
            id = "assistant",
            presetMessages = listOf(DesktopPresetMessage(role = "assistant", content = "Welcome"))
        )
        val conversation = DesktopConversation(id = "conversation", assistantId = assistant.id)
        val data = DesktopData(
            assistants = listOf(assistant),
            selectedAssistantId = assistant.id,
            conversations = listOf(conversation),
            selectedConversationId = conversation.id
        )

        val remaining = data.deleteConversation(conversation.id)

        assertEquals(assistant.id, remaining.conversations.single().assistantId)
        assertEquals("Welcome", remaining.conversations.single().messages.single().content)
    }
}
