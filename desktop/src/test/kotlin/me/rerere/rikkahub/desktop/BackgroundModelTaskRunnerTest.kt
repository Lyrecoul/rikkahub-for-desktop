package me.rerere.rikkahub.desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class RecordingStreamAdapter : ConversationModelStreamAdapter {
    val requests = mutableListOf<Pair<DesktopConfig, List<ChatMessage>>>()
    var responses = ArrayDeque<Flow<StreamDelta>>()

    override fun stream(config: DesktopConfig, messages: List<ChatMessage>): Flow<StreamDelta> {
        requests += config to messages
        return if (responses.isEmpty()) flowOf() else responses.removeFirst()
    }
}

private fun testData(messages: List<ChatMessage> = listOf(ChatMessage("user", "Hello"))): DesktopData {
    val conversation = DesktopConversation(id = "conversation", messages = messages)
    return DesktopData(conversations = listOf(conversation), selectedConversationId = conversation.id)
}

class BackgroundModelTaskRunnerTest {
    @Test
    fun appliesOutputAndReleasesRegistry() = runBlocking {
        val state = TestState()
        val registry = DesktopGenerationRegistry()
        val adapter = RecordingStreamAdapter().apply { responses += flowOf(StreamDelta(content = "Summary")) }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val errors = mutableListOf<Pair<String, String>>()
        val runner = runner(adapter, state, registry, errors, scope)

        runner.submit(FakeTask(validation = { _, _ -> TaskValidation.Pass }, applyResult = { d, output ->
            d.copy(conversations = d.conversations.map { it.copy(title = output) })
        }))
        scope.cancel()

        assertEquals("Summary", state.data.conversations.single().title)
        assertFalse(registry.isRunning("conversation"))
        assertTrue(errors.isEmpty())
    }

    @Test
    fun skipPreconditionDoesNothing() = runBlocking {
        val state = TestState()
        val registry = DesktopGenerationRegistry()
        val adapter = RecordingStreamAdapter()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val errors = mutableListOf<Pair<String, String>>()
        val runner = runner(adapter, state, registry, errors, scope)

        runner.submit(FakeTask(precondition = TaskPrecondition.Skip))
        scope.cancel()

        assertTrue(adapter.requests.isEmpty())
        assertFalse(registry.isRunning("conversation"))
        assertTrue(errors.isEmpty())
    }

    @Test
    fun blockedPreconditionReportsError() = runBlocking {
        val state = TestState()
        val registry = DesktopGenerationRegistry()
        val adapter = RecordingStreamAdapter()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val errors = mutableListOf<Pair<String, String>>()
        val runner = runner(adapter, state, registry, errors, scope)

        runner.submit(FakeTask(precondition = TaskPrecondition.Blocked("not enough messages")))
        scope.cancel()

        assertEquals(listOf("conversation" to "not enough messages"), errors)
        assertTrue(adapter.requests.isEmpty())
        assertFalse(registry.isRunning("conversation"))
    }

    @Test
    fun failureValidationReportsErrorAndDoesNotApply() = runBlocking {
        val state = TestState()
        val registry = DesktopGenerationRegistry()
        val adapter = RecordingStreamAdapter().apply { responses += flowOf(StreamDelta(content = "")) }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val errors = mutableListOf<Pair<String, String>>()
        val runner = runner(adapter, state, registry, errors, scope)

        runner.submit(FakeTask(validation = { _, _ -> TaskValidation.Fail("empty output") }, applyResult = { d, _ -> d }))
        scope.cancel()

        assertEquals(listOf("conversation" to "empty output"), errors)
        assertEquals("Hello", state.data.conversations.single().messages.single().content)
        assertFalse(registry.isRunning("conversation"))
    }

    @Test
    fun retriesUntilPassWithNewRequests() = runBlocking {
        val state = TestState()
        val registry = DesktopGenerationRegistry()
        val adapter = RecordingStreamAdapter().apply {
            responses += flowOf(StreamDelta(content = "copy"))
            responses += flowOf(StreamDelta(content = "copy"))
            responses += flowOf(StreamDelta(content = "你好"))
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val errors = mutableListOf<Pair<String, String>>()
        val runner = runner(adapter, state, registry, errors, scope)
        val task = object : BackgroundModelTask {
            override val conversationId = "conversation"
            override val maxAttempts = 3
            override fun canRun(data: DesktopData) = TaskPrecondition.Run
            override fun request(data: DesktopData, attempt: Int) =
                BackgroundModelRequest(DesktopConfig(model = "m$attempt"), listOf(ChatMessage("user", "r$attempt")))
            override fun validate(output: String, attempt: Int) =
                if (output == "你好") TaskValidation.Pass else TaskValidation.Retry
            override fun apply(data: DesktopData, output: String) =
                data.copy(conversations = data.conversations.map { it.copy(title = output) })
        }

        runner.submit(task)
        scope.cancel()

        assertEquals(3, adapter.requests.size)
        assertEquals("m0", adapter.requests[0].first.model)
        assertEquals("r0", adapter.requests[0].second.single().content)
        assertEquals("m2", adapter.requests[2].first.model)
        assertEquals("r2", adapter.requests[2].second.single().content)
        assertEquals("你好", state.data.conversations.single().title)
    }

    @Test
    fun streamFailureReportsError() = runBlocking {
        val state = TestState()
        val registry = DesktopGenerationRegistry()
        val adapter = RecordingStreamAdapter().apply { responses += flow { error(IllegalStateException("boom")) } }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val errors = mutableListOf<Pair<String, String>>()
        val runner = runner(adapter, state, registry, errors, scope)

        runner.submit(FakeTask(validation = { _, _ -> TaskValidation.Pass }, applyResult = { d, _ -> d }))
        scope.cancel()

        assertEquals("boom", errors.single().second.substringAfterLast(": "))
        assertFalse(registry.isRunning("conversation"))
    }

    @Test
    fun submissionWhileRunningIsIgnored() = runBlocking {
        val state = TestState()
        val registry = DesktopGenerationRegistry()
        val adapter = RecordingStreamAdapter().apply { responses += flow { awaitCancellation() } }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val errors = mutableListOf<Pair<String, String>>()
        val runner = runner(adapter, state, registry, errors, scope)

        runner.submit(FakeTask(validation = { _, _ -> TaskValidation.Pass }, applyResult = { d, output ->
            d.copy(conversations = d.conversations.map { it.copy(title = output) })
        }))
        runner.submit(FakeTask(validation = { _, _ -> TaskValidation.Pass }, applyResult = { d, output ->
            d.copy(conversations = d.conversations.map { it.copy(title = output + "!") })
        }))
        scope.cancel()

        assertEquals(1, adapter.requests.size)
        assertEquals("新对话", state.data.conversations.single().title)
    }

    private fun runner(
        adapter: RecordingStreamAdapter,
        state: ConversationExecutionState,
        registry: DesktopGenerationRegistry,
        errors: MutableList<Pair<String, String>>,
        scope: CoroutineScope,
    ): BackgroundModelTaskRunner = BackgroundModelTaskRunner(
        model = adapter,
        state = state,
        registry = registry,
        reportError = { id, message -> errors += id to message },
        scope = scope,
    )

    private class TestState : ConversationExecutionState {
        var data: DesktopData = testData()
        override fun current(): DesktopData = data
        override fun update(transform: (DesktopData) -> DesktopData) {
            data = transform(data)
        }
    }

    private class FakeTask(
        val precondition: TaskPrecondition = TaskPrecondition.Run,
        val validation: (String, Int) -> TaskValidation = { _, _ -> TaskValidation.Pass },
        val applyResult: (DesktopData, String) -> DesktopData = { d, _ -> d },
    ) : BackgroundModelTask {
        override val conversationId = "conversation"
        override fun canRun(data: DesktopData) = precondition
        override fun request(data: DesktopData, attempt: Int) =
            BackgroundModelRequest(DesktopConfig(), listOf(ChatMessage("user", "request")))
        override fun validate(output: String, attempt: Int) = validation(output, attempt)
        override fun apply(data: DesktopData, output: String) = applyResult(data, output)
    }
}
