package mozilla.components.lib.llm.mlpa

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mozilla.components.concept.integrity.IntegrityClient
import mozilla.components.concept.llm.CloudLlmProvider
import mozilla.components.concept.llm.CloudLlmProvider.State
import mozilla.components.concept.llm.CloudLlmProvider.State.Ready
import mozilla.components.concept.llm.CloudLlmProvider.State.Unavailable
import mozilla.components.lib.llm.mlpa.service.AuthenticationService
import mozilla.components.lib.llm.mlpa.service.AuthenticationService.Request
import mozilla.components.lib.llm.mlpa.service.AuthorizationToken
import mozilla.components.lib.llm.mlpa.service.MlpaService
import mozilla.components.lib.llm.mlpa.service.UserId


fun interface UserIdProvider {
    fun getUserId(): UserId
}


fun interface MlpaTokenProvider {
    suspend fun fetchToken(): Result<AuthorizationToken>

    companion object {
        fun static(token: AuthorizationToken) = MlpaTokenProvider {
            Result.success(token)
        }

        fun mlpaIntegrityHandshake(
            integrityClient: IntegrityClient,
            authenticationService: AuthenticationService,
            userIdProvider: UserIdProvider,
        ) = MlpaTokenProvider {
            integrityClient.request().fold(
                onSuccess = { token ->
                    val request = Request(
                        userId = userIdProvider.getUserId(),
                        integrityToken = token,
                    )

                    authenticationService.verify(request)
                        .map { it.accessToken }
                },
                onFailure = { Result.failure(it) }
            )
        }
    }
}

class MlpaLlmProvider(
    val tokenProvider: MlpaTokenProvider,
    val mlpaService: MlpaService,
) : CloudLlmProvider {
    private val _state = MutableStateFlow<State>(State.Available)
    override val state: StateFlow<State> = _state

    suspend fun prepare() {
        tokenProvider.fetchToken()
            .onSuccess { _state.value = Ready(MlpaLlm(mlpaService, it)) }
            .onFailure { _state.value = Unavailable }
    }
}
