/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.summarize

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SummarizationStoreTest {
    @Test
    fun `test the download flow`() = runTest {
        val store = SummarizationStore(
            initialState = SummarizationState.initial,
            reducer = ::summarizationReducer,
            middleware = listOf(SummarizationMiddleware(scope = backgroundScope))
        )

        val states = mutableListOf<SummarizationState>()
        backgroundScope.launch {
            store.stateFlow.toList(states)
        }

        store.dispatch(ViewAppeared)
        store.dispatch(DownloadConsentAction.AllowClicked)

        testScheduler.advanceTimeBy(5.seconds)

        val expected = listOf(
            SummarizationState.Inert,
            SummarizationState.DownloadConsentRequired,
            SummarizationState.Downloading(10_000L, 0L),
            SummarizationState.Downloading(10_000L, 5_000L),
            SummarizationState.Summarizing(),
            SummarizationState.Summarizing("# This is the article\n This is "),
            SummarizationState.Summarizing("# This is the article\n This is some content...\n This is"),
            SummarizationState.Summarizing("# This is the article\n This is some content...\n This is some *bold* content.\n"),
            SummarizationState.Summarized("# This is the article\n This is some content...\n This is some *bold* content.\n")
        )

        assertEquals(expected, states)
    }
}
