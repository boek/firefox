package mozilla.components.lib.llm.mlpa

import kotlinx.serialization.Serializable
import mozilla.components.concept.integrity.IntegrityClient

@JvmInline
internal value class AuthorizationToken(val value: String)

internal fun interface AuthorizationTokenProvider {
    suspend fun request(): Result<AuthorizationToken>
}

internal fun interface ChatService {
    suspend fun complete(request: ChatRequest): Result<ChatResponse>
}

@Serializable
internal data class ChatResponse(
    val choices: List<Choice>
) {
    @Serializable
    data class Choice(
        val message: Message
    )


    @Serializable
    data class Message(
        val content: String
    )

}

@Serializable
internal data class ChatRequest(
    val model: ModelID,
    val messages: List<Message>,
) {
    @JvmInline
    @Serializable
    value class ModelID(val value: String) {
        companion object {
            val mistral: ModelID
                get() = ModelID("mistral-small-2503")
        }
    }

    @Serializable
    sealed class Message {
        abstract val role: String
        abstract val content: String

        data class User(override val content: String) : Message() {
            override val role: String
                get() = "user"
        }
    }
}
