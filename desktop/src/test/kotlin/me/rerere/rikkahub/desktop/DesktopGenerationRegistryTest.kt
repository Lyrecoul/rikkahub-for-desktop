package me.rerere.rikkahub.desktop

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopGenerationRegistryTest {
    @Test
    fun beginReturnsFalseForConversationAlreadyGenerating() {
        val registry = DesktopGenerationRegistry()
        val job = Job()

        assertTrue(registry.begin("conversation", job))
        assertFalse(registry.begin("conversation", Job()))
        assertTrue(registry.isRunning("conversation"))
    }

    @Test
    fun finishClearsTheOwnersPlaceholder() = runBlocking {
        val registry = DesktopGenerationRegistry()
        val job = launch(start = CoroutineStart.LAZY) {
            assertTrue(registry.finish("conversation"))
        }
        assertTrue(registry.begin("conversation", job))

        job.start()
        job.join()

        assertFalse(registry.isRunning("conversation"))
        assertTrue(registry.runningIds.isEmpty())
    }

    @Test
    fun staleJobCannotClearReplacementPlaceholder() = runBlocking {
        val registry = DesktopGenerationRegistry()
        val started = CompletableDeferred<Unit>()
        val staleJob = launch(start = CoroutineStart.LAZY) {
            try {
                started.complete(Unit)
                delay(Long.MAX_VALUE)
            } finally {
                // 旧协程被取消后仍会走到 finally：不得误清新占位
                assertFalse(registry.finish("conversation"))
            }
        }
        assertTrue(registry.begin("conversation", staleJob))
        staleJob.start()
        started.await()

        registry.cancel("conversation")
        staleJob.join()

        assertFalse(registry.isRunning("conversation"))
        val replacementJob = Job()
        assertTrue(registry.begin("conversation", replacementJob))
        assertTrue(registry.isRunning("conversation"))
        replacementJob.cancel()
    }

    @Test
    fun cancelRemovesPlaceholderAndCancelsJob() {
        val registry = DesktopGenerationRegistry()
        val job = Job()
        assertTrue(registry.begin("conversation", job))

        registry.cancel("conversation")

        assertFalse(registry.isRunning("conversation"))
        assertTrue(job.isCancelled)
    }

    @Test
    fun cancelAllRemovesEverything() {
        val registry = DesktopGenerationRegistry()
        val first = Job()
        val second = Job()
        assertTrue(registry.begin("a", first))
        assertTrue(registry.begin("b", second))

        registry.cancelAll()

        assertTrue(registry.runningIds.isEmpty())
        assertTrue(first.isCancelled)
        assertTrue(second.isCancelled)
    }

    @Test
    fun runningIdsReflectCurrentPlaceholders() {
        val registry = DesktopGenerationRegistry()
        assertTrue(registry.runningIds.isEmpty())

        assertTrue(registry.begin("a", Job()))
        assertTrue(registry.begin("b", Job()))

        assertEquals(setOf("a", "b"), registry.runningIds)
    }
}
