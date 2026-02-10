/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.llm.mlpa

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mozilla.components.concept.llm.Llm
import mozilla.components.concept.llm.Prompt
import mozilla.components.lib.llm.mlpa.ChatRequest.Message
import mozilla.components.lib.llm.mlpa.ChatRequest.ModelID

internal class MLPALlmClient(
    private val chatService: ChatService,
) : Llm {
    override suspend fun prompt(prompt: Prompt): Flow<Llm.Response> = flow {
        val chatRequest = ChatRequest(
            model = ModelID.mistral,
            messages = listOf(Message.User(prompt.value))
        )

        chatService.complete(chatRequest).getOrNull()?.also {
            it.choices.firstOrNull()?.message?.content?.also {
                emit(Llm.Response.Preparing)
            }
        }
    }
}
