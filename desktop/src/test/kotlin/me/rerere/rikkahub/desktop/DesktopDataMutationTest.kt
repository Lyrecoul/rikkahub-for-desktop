package me.rerere.rikkahub.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DesktopDataMutationTest {
    @Test
    fun withNewProviderSelectsAndAdoptsConfigWithFallback() {
        val active = DesktopProviderProfile(id = "active", name = "Active", config = DesktopConfig(model = "m"))
        val data = DesktopData(providers = listOf(active), selectedProviderId = active.id)
        val added = DesktopProviderProfile(name = "New", config = DesktopConfig(model = "new"))

        val result = data.withNewProvider(added)

        assertEquals(listOf(active, added), result.providers)
        assertEquals(added.id, result.selectedProviderId)
        assertEquals(added.config, result.config)
    }

    @Test
    fun withNewProviderFallsBackToActiveProfileWhenListIsEmpty() {
        val data = DesktopData(providers = emptyList(), selectedProviderId = "")
        val added = DesktopProviderProfile(name = "New", config = DesktopConfig(model = "new"))

        val result = data.withNewProvider(added)

        // 空列表时先补上 activeProvider() 的 legacy 占位，再追加新 profile（与原行为一致）
        assertEquals(2, result.providers.size)
        assertEquals("legacy", result.providers.first().id)
        assertEquals(added.id, result.selectedProviderId)
        assertEquals(added.config, result.config)
    }

    @Test
    fun withNewAssistantSelectsIt() {
        val existing = DesktopAssistantProfile(id = "existing", name = "Existing")
        val data = DesktopData(assistants = listOf(existing), selectedAssistantId = existing.id)
        val added = DesktopAssistantProfile(name = "New")

        val result = data.withNewAssistant(added)

        assertEquals(listOf(existing, added), result.assistants)
        assertEquals(added.id, result.selectedAssistantId)
    }

    @Test
    fun withCopyOfAssistantGeneratesNewIdAndCopySuffix() {
        val source = DesktopAssistantProfile(id = "source", name = "Assistant")
        val data = DesktopData(assistants = listOf(source), selectedAssistantId = source.id)

        val result = data.withCopyOfAssistant(source)

        val copy = result.assistants.last()
        assertNotEquals(source.id, copy.id)
        assertEquals("Assistant copy", copy.name)
        assertEquals(copy.id, result.selectedAssistantId)
        assertTrue(result.assistants.first().id == source.id)
    }
}
