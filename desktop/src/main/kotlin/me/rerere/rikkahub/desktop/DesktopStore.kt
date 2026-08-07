package me.rerere.rikkahub.desktop

import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64

internal class DesktopStore(
    private val dataFile: Path = defaultDataFile(),
    private val secretStore: DesktopSecretStore = defaultDesktopSecretStore(dataFile)
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
            val legacy = hydrateAttachments(data.normalized())
            runCatching { migrateLegacyData(legacy) }
            return legacy
        }
        return hydrateAttachments(
            data.copy(
                conversations = orderConversations(loadConversations(), stored.conversationIds),
                conversationIds = emptyList()
            ).normalized()
        )
    }

    fun save(data: DesktopData) {
        saveSplitData(data)
    }

    fun exportData(destination: Path, data: DesktopData) {
        destination.parent?.let(Files::createDirectories)
        val portable = inlineAttachments(stripSecrets(data.normalized()), strict = true)
        Files.writeString(destination, json.encodeToString(portable))
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
        val imported = json.decodeFromString<DesktopData>(Files.readString(source)).normalized()
        return inlineAttachments(imported, strict = true)
    }

    private fun saveSplitData(data: DesktopData) {
        val sanitized = extractSecrets(data.normalized()).copy(schemaVersion = CurrentSchemaVersion)
        Files.createDirectories(dataFile.parent)
        restrictPermissions(dataFile.parent, directory = true)
        Files.createDirectories(conversationsDirectory)
        restrictPermissions(conversationsDirectory, directory = true)
        Files.createDirectories(attachmentsDirectory)
        restrictPermissions(attachmentsDirectory, directory = true)
        val persisted = externalizeAttachments(sanitized)

        persisted.conversations.forEach { conversation ->
            atomicWrite(conversationFile(conversation.id), json.encodeToString(conversation))
        }

        atomicWrite(
            dataFile,
            json.encodeToString(
                persisted.copy(
                    conversations = emptyList(),
                    conversationIds = persisted.conversations.map(DesktopConversation::id)
                )
            )
        )
        val currentFiles = persisted.conversations.map { conversationFile(it.id).fileName.toString() }.toSet()
        Files.list(conversationsDirectory).use { files ->
            files.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".json") }
                .filter { it.fileName.toString() !in currentFiles }
                .forEach { Files.deleteIfExists(it) }
        }
        removeUnreferencedAttachmentBlobs(persisted.attachmentBlobIds())
    }

    private fun externalizeAttachments(data: DesktopData): DesktopData = data.mapDesktopAttachments { attachment ->
        var persisted = attachment
        if (attachment.kind in setOf(DesktopAttachmentKind.IMAGE, DesktopAttachmentKind.AUDIO) &&
            attachment.data.isNotBlank()
        ) {
            val blobId = writeAttachmentBlob(attachment.data)
            persisted = persisted.copy(data = "", dataBlobId = blobId)
        }
        if (!attachment.rawData.isNullOrBlank()) {
            val blobId = writeAttachmentBlob(attachment.rawData)
            persisted = persisted.copy(rawData = null, rawDataBlobId = blobId)
        }
        persisted
    }

    private fun hydrateAttachments(data: DesktopData, strict: Boolean = false): DesktopData =
        data.mapDesktopAttachments { attachment ->
            var hydrated = attachment
            if (hydrated.data.isBlank() && hydrated.dataBlobId != null) {
                readAttachmentBlob(hydrated.dataBlobId, strict)?.let { hydrated = hydrated.copy(data = it) }
            }
            if (hydrated.rawData == null && hydrated.rawDataBlobId != null) {
                readAttachmentBlob(hydrated.rawDataBlobId, strict)?.let { hydrated = hydrated.copy(rawData = it) }
            }
            if (strict) {
                require(hydrated.dataBlobId == null || hydrated.data.isNotBlank()) {
                    "Attachment data is unavailable: ${hydrated.name}"
                }
                require(hydrated.rawDataBlobId == null || hydrated.rawData != null) {
                    "Original attachment data is unavailable: ${hydrated.name}"
                }
            }
            hydrated
        }

    private fun inlineAttachments(data: DesktopData, strict: Boolean): DesktopData =
        hydrateAttachments(data, strict).mapDesktopAttachments { attachment ->
            attachment.copy(dataBlobId = null, rawDataBlobId = null)
        }

    private fun writeAttachmentBlob(encoded: String): String {
        val maxEncodedLength = ((MaxAttachmentBytes + 2) / 3 * 4).toInt()
        require(encoded.length <= maxEncodedLength) { "Attachment exceeds the 10 MB storage limit" }
        val bytes = Base64.getDecoder().decode(encoded)
        require(bytes.size.toLong() <= MaxAttachmentBytes) { "Attachment exceeds the 10 MB storage limit" }
        val id = sha256Hex(bytes)
        val destination = attachmentBlobFile(id)
        val validExisting = Files.isRegularFile(destination) && runCatching {
            sha256Hex(Files.readAllBytes(destination)) == id
        }.getOrDefault(false)
        if (!validExisting) atomicWrite(destination, bytes)
        return id
    }

    private fun readAttachmentBlob(id: String, strict: Boolean): String? {
        val result = runCatching {
            val file = attachmentBlobFile(id)
            require(Files.isRegularFile(file)) { "Attachment blob is missing: $id" }
            val bytes = Files.readAllBytes(file)
            require(bytes.size.toLong() <= MaxAttachmentBytes) { "Attachment blob exceeds the storage limit: $id" }
            require(sha256Hex(bytes) == id) { "Attachment blob checksum mismatch: $id" }
            Base64.getEncoder().encodeToString(bytes)
        }
        if (strict) return result.getOrThrow()
        return result.getOrNull()
    }

    private fun removeUnreferencedAttachmentBlobs(referencedIds: Set<String>) {
        if (!Files.isDirectory(attachmentsDirectory)) return
        Files.list(attachmentsDirectory).use { files ->
            files.filter { Files.isRegularFile(it) && it.fileName.toString() !in referencedIds }
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
        atomicWrite(file, content.toByteArray(Charsets.UTF_8))
    }

    private fun atomicWrite(file: Path, content: ByteArray) {
        val temporaryFile = file.resolveSibling("${file.fileName}.tmp")
        Files.write(temporaryFile, content)
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

    private val attachmentsDirectory: Path
        get() = dataFile.resolveSibling("attachments")

    private fun conversationFile(id: String): Path =
        conversationsDirectory.resolve("${sha256Hex(id.toByteArray(Charsets.UTF_8))}.json")

    private fun attachmentBlobFile(id: String): Path {
        require(id.matches(Regex("[a-f0-9]{64}"))) { "Invalid attachment blob id" }
        return attachmentsDirectory.resolve(id)
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { byte -> "%02x".format(byte) }

    private fun restrictPermissions(path: Path, directory: Boolean) {
        runCatching {
            Files.setPosixFilePermissions(
                path,
                PosixFilePermissions.fromString(if (directory) "rwx------" else "rw-------")
            )
        }
    }

    private fun hydrateSecrets(data: DesktopData): DesktopData {
        val providers = data.providers.map { provider ->
            provider.copy(
                config = provider.config.copy(
                    apiKey = provider.config.apiKey.ifBlank {
                        secretStore.read(providerSecretId(provider.id)).orEmpty()
                    }
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
            provider.copy(config = provider.config.withoutSecrets())
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
            webSearchSettings = searchSettings.withoutSecrets()
        )
        return sanitized.copy(config = sanitized.activeProvider().config)
    }

    private fun stripSecrets(data: DesktopData): DesktopData {
        val providers = data.providers.map { provider ->
            provider.copy(config = provider.config.withoutSecrets())
        }
        val sanitized = data.copy(
            providers = providers,
            webSearchSettings = data.webSearchSettings.withoutSecrets()
        )
        return sanitized.copy(config = sanitized.activeProvider().config)
    }

    private fun providerSecretId(providerId: String) = "provider:$providerId:api-key"

    private fun searchSecretId(provider: DesktopSearchProviderType) =
        "search:${provider.name.lowercase()}:api-key"

    companion object {
        private const val legacySearchSecretId = "search:brave-api-key"
        private const val CurrentSchemaVersion = 3

        fun defaultDataFile(): Path = DesktopPlatform.dataDirectory().resolve("desktop.json")
    }
}

private fun DesktopConfig.withoutSecrets(): DesktopConfig = copy(
    apiKey = "",
    webSearchSettings = webSearchSettings.withoutSecrets()
)

private fun DesktopWebSearchSettings.withoutSecrets(): DesktopWebSearchSettings = copy(
    apiKey = "",
    apiKeys = emptyMap()
)

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
        preferences = preferences.copy(
            composerTransparency = preferences.composerTransparency.coerceIn(0f, 1f),
            composerBlurRadius = preferences.composerBlurRadius.coerceIn(0f, 60f)
        ),
        providers = validProviders,
        selectedProviderId = providerId,
        assistants = validAssistants,
        selectedAssistantId = assistantId,
        folders = validFolders,
        conversations = validConversations.map { conversation ->
            val folder = validFolders.firstOrNull { it.id == conversation.folderId }
            if (conversation.folderId != null && folder == null) {
                conversation.copy(folderId = null)
            } else conversation
        },
        selectedConversationId = conversationId
    )
}
