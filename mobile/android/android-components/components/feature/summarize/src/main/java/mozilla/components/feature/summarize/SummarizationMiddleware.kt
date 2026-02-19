/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.summarize

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import mozilla.components.concept.llm.CloudLlmProvider
import mozilla.components.concept.llm.Llm
import mozilla.components.concept.llm.LlmProvider
import mozilla.components.concept.llm.LocalLlmProvider
import mozilla.components.concept.llm.Prompt
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.Store
import kotlin.time.Duration.Companion.seconds

data class FakeCloudProvider(
    override val state: MutableStateFlow<CloudLlmProvider.State>
) : CloudLlmProvider

data class FakeLlm(
    val responses: List<Llm.Response> = listOf()
) : Llm {
    override suspend fun prompt(prompt: Prompt): Flow<Llm.Response> = flow {
        for (response in responses) {
            delay(0.2.seconds)
            emit(response)
        }
    }

    companion object {
        val successful get() = FakeLlm(
            listOf(
                Llm.Response.Success.ReplyPart("# This is the article\n This is "),
                Llm.Response.Success.ReplyPart("some content...\n This is"),
                Llm.Response.Success.ReplyPart(" some *bold* content.\n"),
                Llm.Response.Success.ReplyFinished,
            )
        )

        val lipsum get() = FakeLlm(
            listOf(
                Llm.Response.Success.ReplyPart("# Lorem Ipsum \n\n"),
                Llm.Response.Success.ReplyPart("Lorem ipsum dolor sit amet, "),
                Llm.Response.Success.ReplyPart("consectetur adipiscing elit. "),
                Llm.Response.Success.ReplyPart("Sed do eiusmod tempor incididunt "),
                Llm.Response.Success.ReplyPart("ut labore et dolore magna aliqua. "),
                Llm.Response.Success.ReplyPart("Ut enim ad minim veniam, "),
                Llm.Response.Success.ReplyPart("quis nostrud exercitation ullamco "),
                Llm.Response.Success.ReplyPart("laboris nisi ut aliquip ex ea "),
                Llm.Response.Success.ReplyPart("commodo consequat. "),
                Llm.Response.Success.ReplyPart("Duis aute irure dolor in "),
                Llm.Response.Success.ReplyPart("reprehenderit in voluptate velit "),
                Llm.Response.Success.ReplyPart("esse cillum dolore eu fugiat "),
                Llm.Response.Success.ReplyPart("nulla pariatur. "),
                Llm.Response.Success.ReplyPart("Excepteur sint occaecat cupidatat "),
                Llm.Response.Success.ReplyPart("non proident, sunt in culpa "),
                Llm.Response.Success.ReplyPart("qui officia deserunt mollit anim "),
                Llm.Response.Success.ReplyPart("id est laborum.\n\n"),
                Llm.Response.Success.ReplyPart("Integer nec odio. "),
                Llm.Response.Success.ReplyPart("Praesent libero. "),
                Llm.Response.Success.ReplyPart("Sed cursus ante dapibus diam. "),
                Llm.Response.Success.ReplyPart("Sed nisi. "),
                Llm.Response.Success.ReplyPart("Nulla quis sem at nibh "),
                Llm.Response.Success.ReplyPart("elementum imperdiet. "),
                Llm.Response.Success.ReplyPart("Duis sagittis ipsum. "),
                Llm.Response.Success.ReplyPart("Praesent mauris. "),
                Llm.Response.Success.ReplyPart("Fusce nec tellus sed augue "),
                Llm.Response.Success.ReplyPart("semper porta. "),
                Llm.Response.Success.ReplyPart("Mauris massa. "),
                Llm.Response.Success.ReplyPart("Vestibulum lacinia arcu eget nulla. "),
                Llm.Response.Success.ReplyPart("Class aptent taciti sociosqu "),
                Llm.Response.Success.ReplyPart("ad litora torquent per conubia nostra, "),
                Llm.Response.Success.ReplyPart("per inceptos himenaeos. "),
                Llm.Response.Success.ReplyPart("Curabitur sodales ligula in libero.\n"),
                Llm.Response.Success.ReplyFinished,
            )
        )
    }
}

data class FakeLocalProvider(
    override val state: MutableStateFlow<LocalLlmProvider.State> = MutableStateFlow(LocalLlmProvider.State.ReadyToDownload),
    val llm: Llm
) : LocalLlmProvider {
    override suspend fun downloadIfNeeded() {
        println("1")
        state.value = LocalLlmProvider.State.Downloading(10000L, 0L)
        println("2")
        delay(0.5.seconds)
        state.value = LocalLlmProvider.State.Downloading(10000L, 5000L)
        println("3")
        delay(1.seconds)
        state.value = LocalLlmProvider.State.Ready(llm)
        println("4")
    }
}

fun interface ContentExtractor {
    suspend fun extractContent(): Result<String>
}

internal class SummarizationMiddleware(
    private val llmProvider: LlmProvider = FakeLocalProvider(llm = FakeLlm.lipsum),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    val contentExtractor: ContentExtractor = { Result.success("") },
) : Middleware<SummarizationState, SummarizationAction> {
    override fun invoke(
        store: Store<SummarizationState, SummarizationAction>,
        next: (SummarizationAction) -> Unit,
        action: SummarizationAction,
    ) {
        next(action)

        when (action) {
            is ViewAppeared -> {
                when (llmProvider) {
                    is CloudLlmProvider -> observeCloudLlmProvider(store, llmProvider)
                    is LocalLlmProvider -> observeLocalLlmProvider(store, llmProvider)
                }
            }
            is DownloadConsentAction.AllowClicked -> if (llmProvider is LocalLlmProvider) {
                scope.launch {
                    llmProvider.downloadIfNeeded()
                }
            }
            is LlmProviderAction.ProviderReady -> observePrompt(store, action.llm)
        }
    }

    fun observePrompt(store: SummarizationStore, llm: Llm) = scope.launch {
        val pageContent = contentExtractor.extractContent()
        llm.prompt(Prompt(systemPrompt + pageContent))
            .collect { response ->
                store.dispatch(LlmAction.ReceivedResponse(response))
            }
    }

    fun observeCloudLlmProvider(
        store: SummarizationStore,
        llmProvider: CloudLlmProvider,
    ) = scope.launch {
        llmProvider
    }

    fun observeLocalLlmProvider(
        store: SummarizationStore,
        llmProvider: LocalLlmProvider,
    ) = scope.launch {
        llmProvider.state.collect { state ->
            when (state) {
                LocalLlmProvider.State.Idle -> TODO("We need an error state if our providers arent initialized")
                is LocalLlmProvider.State.Downloading -> store.dispatch(LlmProviderAction.Downloading(state.bytesDownloaded, state.bytesToDownload))
                LocalLlmProvider.State.Failed -> TODO("We need an error state here")
                is LocalLlmProvider.State.Ready -> store.dispatch(LlmProviderAction.ProviderReady(state.llm))
                LocalLlmProvider.State.ReadyToDownload -> store.dispatch(LlmProviderAction.DownloadRequired)
                LocalLlmProvider.State.Unavailable -> TODO("We need an error state here")
            }
        }
    }

    private val systemPrompt = """
        This is the system prompt: 
    """.trimIndent()
}
