package mozilla.components.lib.llm.mlpa.service

import kotlinx.serialization.Serializable
import mozilla.components.concept.integrity.IntegrityToken


class VerificationServiceFailed(reason: String) : Exception("Verification Service Failed : $reason")
class ChatServiceFailed(reason: String) : Exception("Verification Service Failed : $reason")

data class MlpaConfig(
    val baseUrl: String,
) {
    companion object {
        val live get() = MlpaConfig(
            baseUrl = "https://mlpa-nonprod-stage-mozilla.global.ssl.fastly.net/v1",
        )
    }
}

@JvmInline
value class AuthorizationToken(val value: String)

@JvmInline
value class UserId(val value: String)

interface MlpaService: AuthenticationService, ChatService

fun interface AuthenticationService {
    suspend fun verify(request: Request): Result<Response>

    @Serializable
    data class Request(
        val userId: UserId,
        val integrityToken: IntegrityToken
    )

    @Serializable
    data class Response(
        val accessToken: AuthorizationToken,
        val tokenType: String,
        val expiresIn: Int,
    )
}

fun interface ChatService {
    suspend fun completion(authorizationToken: AuthorizationToken, request: Request): Result<Response>

    @Serializable
    data class Response(
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
    data class Request(
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
}
