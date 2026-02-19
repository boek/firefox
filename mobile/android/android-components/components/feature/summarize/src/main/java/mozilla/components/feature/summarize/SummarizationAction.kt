/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.summarize

import mozilla.components.concept.llm.Llm
import mozilla.components.lib.state.Action

/**
 * Actions for the [SummarizationStore]
 */
internal interface SummarizationAction : Action

object ViewAppeared : SummarizationAction

internal sealed interface LlmProviderAction : SummarizationAction {
    data object DownloadRequired : LlmProviderAction
    data class Downloading(val bytes: Long, val total: Long) : LlmProviderAction
    data class ProviderReady(val llm: Llm) : LlmProviderAction
}

internal sealed interface LlmAction : SummarizationAction {
    data class ReceivedResponse(val response: Llm.Response) : LlmAction
}

/**
 * Actions for the consent step of the shake to summarize user flow when using a on-device model
 */
internal sealed interface OnDeviceSummarizationShakeConsentAction : SummarizationAction {
    data object LearnMoreClicked : OnDeviceSummarizationShakeConsentAction
    data object AllowClicked : OnDeviceSummarizationShakeConsentAction
    data object CancelClicked : OnDeviceSummarizationShakeConsentAction
}

/**
 * Actions for the consent step of the shake to summarize user flow when using a off-device model
 */
internal sealed interface OffDeviceSummarizationShakeConsentAction : SummarizationAction {
    data object LearnMoreClicked : OffDeviceSummarizationShakeConsentAction
    data object AllowClicked : OffDeviceSummarizationShakeConsentAction
    data object CancelClicked : OffDeviceSummarizationShakeConsentAction
}

internal sealed interface DownloadConsentAction : SummarizationAction {
    data object LearnMoreClicked : DownloadConsentAction
    data object AllowClicked : DownloadConsentAction
    data object CancelClicked : DownloadConsentAction
}

internal sealed interface DownloadInProgressAction : SummarizationAction {
    data object CancelClicked : DownloadInProgressAction
}

internal sealed interface DownloadErrorAction : SummarizationAction {
    data object LearnMoreClicked : DownloadErrorAction
    data object TryAgainClicked : DownloadErrorAction
    data object CancelClicked : DownloadErrorAction
}

internal sealed interface ErrorAction : SummarizationAction {
    data object LearnMoreClicked : ErrorAction
}
