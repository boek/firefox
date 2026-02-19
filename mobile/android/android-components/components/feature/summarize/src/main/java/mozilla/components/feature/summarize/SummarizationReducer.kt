/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.summarize

import mozilla.components.concept.llm.Llm

internal fun summarizationReducer(state: SummarizationState, action: SummarizationAction) = when (action) {
    is LlmProviderAction.DownloadRequired -> SummarizationState.DownloadConsentRequired
    is LlmProviderAction.Downloading -> SummarizationState.Downloading(action.total, action.bytes)
    is LlmProviderAction.ProviderReady -> SummarizationState.Summarizing()
    is LlmAction.ReceivedResponse -> state.applyResponse(action.response)
    else -> { state }
}

internal fun SummarizationState.applyResponse(response: Llm.Response): SummarizationState {
    return if (this is SummarizationState.Summarizing) {
        when (response) {
            is Llm.Response.Failure -> TODO()
            is Llm.Response.Preparing -> TODO()
            Llm.Response.Success.ReplyFinished -> SummarizationState.Summarized(text = text)
            is Llm.Response.Success.ReplyPart -> copy(text = text + response.value)
        }
    } else {
        this
    }
}
