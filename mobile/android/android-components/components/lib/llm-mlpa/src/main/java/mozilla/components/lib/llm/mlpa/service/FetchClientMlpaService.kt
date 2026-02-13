package mozilla.components.lib.llm.mlpa.service


import kotlinx.serialization.json.Json
import mozilla.components.concept.fetch.Client
import mozilla.components.concept.fetch.MutableHeaders
import mozilla.components.concept.fetch.Request
import mozilla.components.concept.fetch.isSuccess


class FetchClientMlpaService(
    val client: Client,
    val config: MlpaConfig,
): MlpaService {
    override suspend fun verify(request: AuthenticationService.Request): Result<AuthenticationService.Response> {
        val request = Request(
            url = "${config.baseUrl}/verify/play",
            method = Request.Method.POST,
            headers = MutableHeaders(

            ),
            body = Request.Body.fromString(Json.encodeToString(request))
        )

        val httpResponse = client.fetch(request)

        return if (httpResponse.isSuccess) {
            val response: AuthenticationService.Response = httpResponse.use { httpResponse.toString() }
                .let { Json.decodeFromString(it) }

            Result.success(response)
        } else {
            Result.failure(VerificationServiceFailed("Did not receive success status code."))
        }
    }

    override suspend fun completion(
        authorizationToken: AuthorizationToken,
        request: ChatService.Request,
    ): Result<ChatService.Response> {
        val request = Request(
            url = "${config.baseUrl}/chat/completions",
            method = Request.Method.POST,
            headers = MutableHeaders(
                "authorization" to authorizationToken.value,
                "content-type" to "application/json",
                "service-type" to "s2s",
            ),
            body = Request.Body.fromString(Json.encodeToString(request))
        )

        val httpResponse = client.fetch(request)

        return if (httpResponse.isSuccess) {
            val response: ChatService.Response = httpResponse.use { httpResponse.toString() }
                .let { Json.decodeFromString(it) }

            Result.success(response)
        } else {
            Result.failure(ChatServiceFailed("Did not receive success status code."))
        }
    }

}
