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
}
