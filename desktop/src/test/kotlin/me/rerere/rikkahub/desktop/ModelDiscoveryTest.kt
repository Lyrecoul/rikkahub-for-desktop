package me.rerere.rikkahub.desktop

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ModelDiscoveryTest {
    @Test
    fun probeReturnsSuccessWithListing() = runBlocking {
        val listing = DesktopModelListing(
            ids = listOf("gpt-4o", "gpt-4o-mini"),
            displayNames = mapOf("gpt-4o" to "GPT-4o (Official)")
        )

        val result = probeProviderModels { listing }

        assertEquals(DesktopModelDiscoveryState.Success(listing), result)
    }

    @Test
    fun probeMapsFetchFailureToFailureState() = runBlocking {
        val result = probeProviderModels { error("connection refused") }

        assertEquals(DesktopModelDiscoveryState.Failure("connection refused"), result)
    }

    @Test
    fun probeFallsBackWhenFailureHasNoMessage() = runBlocking {
        val result = probeProviderModels { throw IllegalStateException() }

        assertEquals(DesktopModelDiscoveryState.Failure("Connection failed"), result)
    }

    @Test
    fun withDiscoveredModelsPersistsIdsAndDisplayNames() {
        val profile = DesktopProviderProfile(id = "provider", name = "Provider")
        val listing = DesktopModelListing(
            ids = listOf("gpt-4o"),
            displayNames = mapOf("gpt-4o" to "GPT-4o (Official)")
        )

        val result = profile.withDiscoveredModels(listing)

        assertEquals(listing.ids, result.discoveredModels)
        assertEquals(listing.displayNames, result.discoveredModelNames)
        assertIs<DesktopProviderProfile>(result)
    }
}
