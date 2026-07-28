package me.rerere.rikkahub.desktop

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest
import java.time.Instant

internal class DesktopStore(
    private val dataFile: Path = defaultDataFile(),
    private val secretStore: DesktopSecretStore = SecretToolStore()
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(): DesktopData {
        if (!Files.isRegularFile(dataFile)) return DesktopData()
        val stored = runCatching { json.decodeFromString<DesktopData>(Files.readString(dataFile)) }
            .getOrElse {
                quarantineCorruptFile()
                return DesktopData()
            }
        val data = hydrateSecrets(stored)
        if (stored.conversations.isNotEmpty()) {
            val legacy = data.normalized()
            runCatching { migrateLegacyData(legacy) }
            return legacy
        }
        return data.copy(
            conversations = orderConversations(loadConversations(), stored.conversationIds),
            conversationIds = emptyList()
        ).normalized()
    }

    fun save(data: DesktopData) {
        saveSplitData(data)
    }

    fun exportData(destination: Path, data: DesktopData) {
        destination.parent?.let(Files::createDirectories)
        Files.writeString(destination, json.encodeToString(stripSecrets(data.normalized())))
    }

    fun deleteProviderSecret(providerId: String) {
        secretStore.delete(providerSecretId(providerId))
    }

    fun clearSecrets(data: DesktopData) {
        data.providers.forEach { provider -> deleteProviderSecret(provider.id) }
        DesktopSearchProviderType.entries.forEach { provider ->
            secretStore.delete(searchSecretId(provider))
        }
        secretStore.delete(legacySearchSecretId)
    }

    fun importData(source: Path): DesktopData {
        require(Files.isRegularFile(source)) { "Backup file does not exist" }
        return json.decodeFromString<DesktopData>(Files.readString(source)).normalized()
    }

    private fun saveSplitData(data: DesktopData) {
        val sanitized = extractSecrets(data.normalized()).copy(schemaVersion = CurrentSchemaVersion)
        Files.createDirectories(dataFile.parent)
        restrictPermissions(dataFile.parent, directory = true)
        Files.createDirectories(conversationsDirectory)
        restrictPermissions(conversationsDirectory, directory = true)

        sanitized.conversations.forEach { conversation ->
            atomicWrite(conversationFile(conversation.id), json.encodeToString(conversation))
        }

        atomicWrite(
            dataFile,
            json.encodeToString(sanitized.copy(
                conversations = emptyList(),
                conversationIds = sanitized.conversations.map(DesktopConversation::id)
            ))
        )
        val currentFiles = sanitized.conversations.map { conversationFile(it.id).fileName.toString() }.toSet()
        Files.list(conversationsDirectory).use { files ->
            files.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".json") }
                .filter { it.fileName.toString() !in currentFiles }
                .forEach { Files.deleteIfExists(it) }
        }
    }

    private fun quarantineCorruptFile() {
        val corrupted = dataFile.resolveSibling("${dataFile.fileName}.corrupt-${Instant.now().toEpochMilli()}")
        runCatching { Files.move(dataFile, corrupted, StandardCopyOption.REPLACE_EXISTING) }
    }

    private fun loadConversations(): List<DesktopConversation> {
        if (!Files.isDirectory(conversationsDirectory)) return emptyList()
        return Files.list(conversationsDirectory).use { files ->
            files.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".json") }
                .map { file ->
                    runCatching { json.decodeFromString<DesktopConversation>(Files.readString(file)) }
                        .getOrElse {
                            quarantineCorruptConversationFile(file)
                            null
                        }
                }
                .filter { it != null }
                .map { it!! }
                .toList()
        }
    }

    private fun orderConversations(
        conversations: List<DesktopConversation>,
        conversationIds: List<String>
    ): List<DesktopConversation> {
        val byId = conversations.associateBy(DesktopConversation::id)
        val indexed = conversationIds.mapNotNull(byId::get)
        val indexedIds = indexed.mapTo(mutableSetOf(), DesktopConversation::id)
        return indexed + conversations.filter { it.id !in indexedIds }
    }

    private fun migrateLegacyData(data: DesktopData) {
        val backup = dataFile.resolveSibling("${dataFile.fileName}.migration-${Instant.now().toEpochMilli()}")
        Files.copy(dataFile, backup)
        restrictPermissions(backup, directory = false)
        saveSplitData(data)
    }

    private fun quarantineCorruptConversationFile(file: Path) {
        val corrupted = file.resolveSibling("${file.fileName}.corrupt-${Instant.now().toEpochMilli()}")
        runCatching { Files.move(file, corrupted, StandardCopyOption.REPLACE_EXISTING) }
    }

    private fun atomicWrite(file: Path, content: String) {
        val temporaryFile = file.resolveSibling("${file.fileName}.tmp")
        Files.writeString(temporaryFile, content)
        restrictPermissions(temporaryFile, directory = false)
        runCatching {
            Files.move(temporaryFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        }.getOrElse {
            Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING)
        }
        restrictPermissions(file, directory = false)
    }

    private val conversationsDirectory: Path
        get() = dataFile.resolveSibling("conversations")

    private fun conversationFile(id: String): Path {
        val digest = MessageDigest.getInstance("SHA-256").digest(id.toByteArray(Charsets.UTF_8))
        val name = digest.joinToString("") { byte -> "%02x".format(byte) }
        return conversationsDirectory.resolve("$name.json")
    }

    private fun restrictPermissions(path: Path, directory: Boolean) {
        runCatching {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(if (directory) "rwx------" else "rw-------"))
        }
    }

    private fun hydrateSecrets(data: DesktopData): DesktopData {
        val providers = data.providers.map { provider ->
            provider.copy(config = provider.config.copy(
                apiKey = provider.config.apiKey.ifBlank { secretStore.read(providerSecretId(provider.id)).orEmpty() }
            ))
        }
        val storedKeys = DesktopSearchProviderType.entries.associateWith { provider ->
            secretStore.read(searchSecretId(provider)).orEmpty()
        }.filterValues(String::isNotBlank)
        val selectedProvider = data.webSearchSettings.providerType
        val legacyKey = if (selectedProvider !in storedKeys) {
            secretStore.read(legacySearchSecretId).orEmpty()
        } else {
            ""
        }
        val searchKeys = storedKeys + if (legacyKey.isNotBlank()) mapOf(selectedProvider to legacyKey) else emptyMap()
        val search = data.webSearchSettings.copy(
            apiKey = data.webSearchSettings.apiKey.ifBlank { searchKeys[selectedProvider].orEmpty() },
            apiKeys = searchKeys
        )
        val hydrated = data.copy(providers = providers, webSearchSettings = search)
        return hydrated.copy(config = hydrated.activeProvider().config)
    }

    private fun extractSecrets(data: DesktopData): DesktopData {
        val providers = data.providers.map { provider ->
            val key = provider.config.apiKey
            if (key.isBlank()) {
                secretStore.delete(providerSecretId(provider.id))
            } else {
                check(secretStore.write(providerSecretId(provider.id), key)) {
                    "无法将 ${provider.name} 的 API 密钥写入系统密钥库；为保护密钥，设置未保存。"
                }
            }
            provider.copy(config = provider.config.copy(apiKey = ""))
        }
        val searchSettings = data.webSearchSettings
        val searchKeys = searchSettings.apiKeys + (searchSettings.providerType to searchSettings.apiKey)
        searchKeys.forEach { (provider, key) ->
            if (key.isBlank()) {
                secretStore.delete(searchSecretId(provider))
            } else {
                check(secretStore.write(searchSecretId(provider), key)) {
                    "无法将 ${provider.name} API 密钥写入系统密钥库；为保护密钥，设置未保存。"
                }
            }
        }
        // The historical shared slot is migrated once its provider-specific value is durable.
        secretStore.delete(legacySearchSecretId)
        val sanitized = data.copy(
            providers = providers,
            webSearchSettings = searchSettings.copy(apiKey = "", apiKeys = emptyMap())
        )
        return sanitized.copy(config = sanitized.activeProvider().config)
    }

    private fun stripSecrets(data: DesktopData): DesktopData {
        val providers = data.providers.map { provider ->
            provider.copy(config = provider.config.copy(apiKey = ""))
        }
        val sanitized = data.copy(
            providers = providers,
            webSearchSettings = data.webSearchSettings.copy(apiKey = "")
        )
        return sanitized.copy(config = sanitized.activeProvider().config)
    }

    private fun providerSecretId(providerId: String) = "provider:$providerId:api-key"

    private fun searchSecretId(provider: DesktopSearchProviderType) =
        "search:${provider.name.lowercase()}:api-key"

    companion object {
        private const val legacySearchSecretId = "search:brave-api-key"
        private const val CurrentSchemaVersion = 2

        fun defaultDataFile(): Path {
            val configHome = System.getenv("XDG_CONFIG_HOME")
                ?.takeIf { it.isNotBlank() }
                ?: Path.of(System.getProperty("user.home"), ".config").toString()
            return Path.of(configHome, "rikkahub", "desktop.json")
        }
    }
}

internal fun DesktopData.normalized(): DesktopData {
    val validProviders = providers.ifEmpty { listOf(DesktopProviderProfile(config = config)) }
    val providerId = selectedProviderId.takeIf { id -> validProviders.any { it.id == id } }
        ?: validProviders.first().id
    val validAssistants = assistants.ifEmpty { listOf(DesktopAssistantProfile()) }
    val assistantId = selectedAssistantId.takeIf { id -> validAssistants.any { it.id == id } }
        ?: validAssistants.first().id
    val validConversations = conversations.ifEmpty {
        listOf(validAssistants.first().newConversation())
    }
    val validFolders = folders.filter { folder -> validAssistants.any { it.id == folder.assistantId } }
    val conversationId = selectedConversationId.takeIf { id -> validConversations.any { it.id == id } }
        ?: validConversations.first().id
    return copy(
        providers = validProviders,
        selectedProviderId = providerId,
        assistants = validAssistants,
        selectedAssistantId = assistantId,
        folders = validFolders,
        conversations = validConversations.map { conversation ->
            val conversationAssistantId = validAssistants.firstOrNull { it.id == conversation.assistantId }?.id ?: assistantId
            val folder = validFolders.firstOrNull { it.id == conversation.folderId }
            if (conversation.folderId != null && (folder == null || folder.assistantId != conversationAssistantId)) {
                conversation.copy(folderId = null)
            } else conversation
        },
        selectedConversationId = conversationId
    )
}
