package me.rerere.rikkahub.desktop

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
        val store = DesktopStore(directory.resolve("desktop.json"), MemorySecrets())
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
    fun storesEachConversationOutsideTheSettingsFile() {
        val directory = Files.createTempDirectory("rikkahub-desktop-split-store")
        val dataFile = directory.resolve("desktop.json")
        val first = DesktopConversation(id = "first", messages = listOf(ChatMessage("user", "first message")))
        val second = DesktopConversation(id = "second", messages = listOf(ChatMessage("user", "second message")))
        val store = DesktopStore(dataFile, MemorySecrets())

        store.save(DesktopData(conversations = listOf(first, second), selectedConversationId = second.id))

        val settings = Files.readString(dataFile)
        assertFalse(settings.contains("first message"))
        assertFalse(settings.contains("second message"))
        Files.list(directory.resolve("conversations")).use { files ->
            assertEquals(2, files.count())
        }
        assertEquals(listOf(first, second), store.load().conversations)
    }

    @Test
    fun deletingConversationRemovesOnlyItsStoredFile() {
        val directory = Files.createTempDirectory("rikkahub-desktop-split-delete")
        val first = DesktopConversation(id = "first")
        val second = DesktopConversation(id = "second")
        val store = DesktopStore(directory.resolve("desktop.json"), MemorySecrets())
        store.save(DesktopData(conversations = listOf(first, second), selectedConversationId = first.id))

        store.save(DesktopData(conversations = listOf(second), selectedConversationId = second.id))

        Files.list(directory.resolve("conversations")).use { files -> assertEquals(1, files.count()) }
        assertEquals(listOf(second), store.load().conversations)
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
        assertTrue(Files.exists(directory.resolve("conversations")))
        assertTrue(Files.list(directory).use { files ->
            files.anyMatch { it.fileName.toString().startsWith("desktop.json.migration-") }
        })
        assertFalse(Files.readString(dataFile).contains("\"messages\""))
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

        assertEquals(2, loaded.schemaVersion)
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
    fun keepsTavilyKeyWhenSwitchingSearchProviders() {
        val directory = Files.createTempDirectory("rikkahub-desktop-tavily-secrets")
        val secrets = MemorySecrets()
        val store = DesktopStore(directory.resolve("desktop.json"), secrets)
        val tavily = DesktopWebSearchSettings(
            providerType = DesktopSearchProviderType.TAVILY,
            apiKey = "tavily-secret"
        )

        store.save(DesktopData(webSearchSettings = tavily))
        val brave = store.load().webSearchSettings.selectProvider(DesktopSearchProviderType.BRAVE)
            .withApiKey("brave-secret")
        store.save(DesktopData(webSearchSettings = brave))

        val restored = store.load().webSearchSettings.selectProvider(DesktopSearchProviderType.TAVILY)
        assertEquals("tavily-secret", restored.apiKey)
        assertEquals("brave-secret", restored.selectProvider(DesktopSearchProviderType.BRAVE).apiKey)
    }

    @Test
    fun migratesLegacySearchKeyToSelectedTavilyProvider() {
        val directory = Files.createTempDirectory("rikkahub-desktop-tavily-legacy")
        val secrets = MemorySecrets().apply { values["search:brave-api-key"] = "legacy-tavily-secret" }
        val dataFile = directory.resolve("desktop.json")
        Files.writeString(dataFile, """{"webSearchSettings":{"providerType":"TAVILY"}}""")
        val store = DesktopStore(dataFile, secrets)

        val migrated = store.load()
        assertEquals("legacy-tavily-secret", migrated.webSearchSettings.apiKey)
        store.save(migrated)
        val restored = store.load()

        assertEquals("legacy-tavily-secret", restored.webSearchSettings.apiKey)
        assertFalse(secrets.values.containsKey("search:brave-api-key"))
        assertEquals("legacy-tavily-secret", secrets.values["search:tavily:api-key"])
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
        val nestedSearchSettings = DesktopWebSearchSettings(
            providerType = DesktopSearchProviderType.EXA,
            apiKey = "nested-current-search-secret",
            apiKeys = mapOf(DesktopSearchProviderType.JINA to "nested-stored-search-secret")
        )
        val provider = DesktopProviderProfile(
            config = DesktopConfig(
                apiKey = "provider-secret",
                webSearchSettings = nestedSearchSettings
            )
        )
        val searchSettings = DesktopWebSearchSettings(
            providerType = DesktopSearchProviderType.TAVILY,
            apiKey = "current-search-secret",
            apiKeys = mapOf(
                DesktopSearchProviderType.TAVILY to "current-search-secret",
                DesktopSearchProviderType.BRAVE to "stored-search-secret"
            )
        )
        val store = DesktopStore(directory.resolve("desktop.json"), secrets)
        val backup = directory.resolve("backup.json")

        store.exportData(
            backup, DesktopData(
                providers = listOf(provider),
                selectedProviderId = provider.id,
                webSearchSettings = searchSettings
            )
        )

        val raw = Files.readString(backup)
        listOf(
            "provider-secret",
            "nested-current-search-secret",
            "nested-stored-search-secret",
            "current-search-secret",
            "stored-search-secret"
        ).forEach { secret -> assertFalse(raw.contains(secret)) }
        val imported = store.importData(backup)
        assertEquals("", imported.activeProvider().config.apiKey)
        assertEquals("", imported.activeProvider().config.webSearchSettings.apiKey)
        assertTrue(imported.activeProvider().config.webSearchSettings.apiKeys.isEmpty())
        assertEquals("", imported.webSearchSettings.apiKey)
        assertTrue(imported.webSearchSettings.apiKeys.isEmpty())
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
    fun quarantinesOnlyTheCorruptConversationFile() {
        val directory = Files.createTempDirectory("rikkahub-desktop-corrupt-conversation")
        val first = DesktopConversation(id = "first")
        val second = DesktopConversation(id = "second")
        val store = DesktopStore(directory.resolve("desktop.json"), MemorySecrets())
        store.save(DesktopData(conversations = listOf(first, second), selectedConversationId = first.id))
        val conversationsDirectory = directory.resolve("conversations")
        val corruptFile = Files.list(conversationsDirectory).use { files -> files.findFirst().orElseThrow() }
        Files.writeString(corruptFile, "not json")

        val loaded = store.load()

        assertEquals(1, loaded.conversations.size)
        assertTrue(Files.list(conversationsDirectory).use { files ->
            files.anyMatch { it.fileName.toString().contains(".corrupt-") }
        })
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
    fun movingConversationToFolderWorksAcrossAssistantScopes() {
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

        val moved = data.moveConversationToFolder(conversation.id, secondFolder.id)
        val created = data.createFolder(
            DesktopFolder(id = "first-folder", assistantId = firstAssistant.id, name = "First"),
            conversation.id
        )

        assertEquals(secondFolder.id, moved.conversations.single().folderId)
        assertEquals("first-folder", created.conversations.single().folderId)
        assertEquals(2, created.folders.size)
    }

    @Test
    fun normalizationKeepsFoldersOwnedByAnotherAssistant() {
        val firstAssistant = DesktopAssistantProfile(id = "first")
        val secondAssistant = DesktopAssistantProfile(id = "second")
        val folder = DesktopFolder(id = "second-folder", assistantId = secondAssistant.id, name = "Second")

        val normalized = DesktopData(
            assistants = listOf(firstAssistant, secondAssistant),
            selectedAssistantId = firstAssistant.id,
            folders = listOf(folder),
            conversations = listOf(DesktopConversation(assistantId = firstAssistant.id, folderId = folder.id))
        ).normalized()

        assertEquals(folder.id, normalized.conversations.single().folderId)
    }

    @Test
    fun keepsFolderFilterWhenSwitchingToAnotherAssistantsScope() {
        val firstAssistant = DesktopAssistantProfile(id = "first")
        val secondAssistant = DesktopAssistantProfile(id = "second")
        val firstFolder = DesktopFolder(id = "first-folder", assistantId = firstAssistant.id, name = "First")
        val data = DesktopData(
            assistants = listOf(firstAssistant, secondAssistant),
            selectedAssistantId = firstAssistant.id,
            folders = listOf(firstFolder)
        )

        assertEquals(firstFolder.id, data.folderFilterForAssistant(firstFolder.id, secondAssistant.id))
        assertEquals(firstFolder.id, data.folderFilterForAssistant(firstFolder.id, firstAssistant.id))
    }

    @Test
    fun movingConversationToAnotherAssistantKeepsItsFolder() {
        val firstAssistant = DesktopAssistantProfile(id = "first")
        val secondAssistant = DesktopAssistantProfile(id = "second")
        val firstFolder = DesktopFolder(id = "first-folder", assistantId = firstAssistant.id, name = "First")
        val conversation =
            DesktopConversation(id = "conversation", assistantId = firstAssistant.id, folderId = firstFolder.id)
        val data = DesktopData(
            assistants = listOf(firstAssistant, secondAssistant),
            selectedAssistantId = firstAssistant.id,
            folders = listOf(firstFolder),
            conversations = listOf(conversation),
            selectedConversationId = conversation.id
        )

        val moved = data.assignAssistantToConversation(conversation.id, secondAssistant.id)

        assertEquals(secondAssistant.id, moved.conversations.single().assistantId)
        assertEquals(firstFolder.id, moved.conversations.single().folderId)
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
