package me.rerere.rikkahub.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopProviderProfileTest {
    private val first = DesktopProviderProfile(
        id = "first",
        name = "First",
        config = DesktopConfig(model = "model-a")
    )
    private val second = DesktopProviderProfile(
        id = "second",
        name = "Second",
        config = DesktopConfig(model = "model-b"),
        discoveredModels = listOf("model-b", "model-b2")
    )

    @Test
    fun selectsProviderAndModelTogether() {
        val data = data().selectProviderConfig("second", "model-b2")

        assertEquals("second", data.selectedProviderId)
        assertEquals("model-b2", data.config.model)
        assertEquals("model-b2", data.activeProvider().config.model)
    }

    @Test
    fun deletingActiveProviderFallsBackToRemainingProvider() {
        val data = data().selectProviderConfig("second").deleteProviderProfile("second")

        assertEquals(listOf("first"), data.providers.map { it.id })
        assertEquals("first", data.selectedProviderId)
        assertEquals("model-a", data.config.model)
    }

    private fun data() = DesktopData(
        config = first.config,
        providers = listOf(first, second),
        selectedProviderId = first.id
    )
}
