package me.rerere.rikkahub.desktop

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermissions
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
        return runCatching { hydrateSecrets(json.decodeFromString<DesktopData>(Files.readString(dataFile)).normalized()) }
            .getOrElse {
                quarantineCorruptFile()
                DesktopData()
            }
    }

    fun save(data: DesktopData) {
        Files.createDirectories(dataFile.parent)
        restrictPermissions(dataFile.parent, directory = true)
        val temporaryFile = dataFile.resolveSibling("${dataFile.fileName}.tmp")
        Files.writeString(temporaryFile, json.encodeToString(extractSecrets(data.normalized())))
        restrictPermissions(temporaryFile, directory = false)
        runCatching {
            Files.move(
                temporaryFile,
                dataFile,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        }.getOrElse {
            Files.move(temporaryFile, dataFile, StandardCopyOption.REPLACE_EXISTING)
        }
        restrictPermissions(dataFile, directory = false)
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
        secretStore.delete(braveSearchSecretId)
    }

    fun importData(source: Path): DesktopData {
        require(Files.isRegularFile(source)) { "Backup file does not exist" }
        return json.decodeFromString<DesktopData>(Files.readString(source)).normalized()
    }

    private fun quarantineCorruptFile() {
        val corrupted = dataFile.resolveSibling("${dataFile.fileName}.corrupt-${Instant.now().toEpochMilli()}")
        runCatching { Files.move(dataFile, corrupted, StandardCopyOption.REPLACE_EXISTING) }
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
        val search = data.webSearchSettings.copy(
            apiKey = data.webSearchSettings.apiKey.ifBlank { secretStore.read(braveSearchSecretId).orEmpty() }
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
        val braveKey = data.webSearchSettings.apiKey
        if (braveKey.isBlank()) {
            secretStore.delete(braveSearchSecretId)
        } else {
            check(secretStore.write(braveSearchSecretId, braveKey)) {
                "无法将 Brave Search API 密钥写入系统密钥库；为保护密钥，设置未保存。"
            }
        }
        val sanitized = data.copy(
            providers = providers,
            webSearchSettings = data.webSearchSettings.copy(apiKey = "")
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

    companion object {
        private const val braveSearchSecretId = "search:brave-api-key"

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
    val conversationId = selectedConversationId.takeIf { id -> validConversations.any { it.id == id } }
        ?: validConversations.first().id
    return copy(
        providers = validProviders,
        selectedProviderId = providerId,
        assistants = validAssistants,
        selectedAssistantId = assistantId,
        conversations = validConversations,
        selectedConversationId = conversationId
    )
}
