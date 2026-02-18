/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.summarize

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
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
            emit(response)
            delay(2.seconds)
        }
    }

    companion object {
        val successful get() = FakeLlm(
            listOf(
                Llm.Response.Success.ReplyPart("# This is the article"),
                Llm.Response.Success.ReplyPart("This is some content..."),
                Llm.Response.Success.ReplyPart("This is some *bold* content."),
                Llm.Response.Success.ReplyFinished,
            )
        )
    }
}

data class FakeLocalProvider(
    override val state: MutableStateFlow<LocalLlmProvider.State> = MutableStateFlow(LocalLlmProvider.State.Idle),
    val llm: Llm
) : LocalLlmProvider {
    override suspend fun downloadIfNeeded() {
        state.value = LocalLlmProvider.State.Downloading(10000L, 0L)
        delay(0.5.seconds)
        state.value = LocalLlmProvider.State.Downloading(10000L, 5000L)
        delay(1.seconds)
        state.value = LocalLlmProvider.State.Ready(llm)
    }
}

internal class SummarizationMiddleware(
    private val llmProvider: LlmProvider = FakeLocalProvider(llm = FakeLlm.successful)
) : Middleware<SummarizationState, SummarizationAction> {
    override fun invoke(
        store: Store<SummarizationState, SummarizationAction>,
        next: (SummarizationAction) -> Unit,
        action: SummarizationAction,
    ) {
        when (action) {
            is SummarizationAction.ViewAppeared -> {
                when (llmProvider) {
                    is CloudLlmProvider -> {
                    }
                    is LocalLlmProvider -> {
                        if (llmProvider.state.value is LocalLlmProvider.State.ReadyToDownload) {
                            store.dispatch(LlmProviderAction.DownloadRequired)
                        }
                    }
                }
            }
        }
    }
}
