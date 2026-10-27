package com.marufallammahim.meemo.data.remote

import com.marufallammahim.meemo.core.error.MeemoError
import com.marufallammahim.meemo.core.logging.SafeLogger
import com.marufallammahim.meemo.core.result.ResultWrapper
import com.marufallammahim.meemo.core.security.EncryptedCredentialStore
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class AiStructuredInterpretation(
    @SerialName("intent") val intentName: String,
    @SerialName("entity") val entityValue: String? = null,
    @SerialName("confidence") val confidence: Float = 0.0f,
    @SerialName("explanation") val explanation: String = ""
)

@Serializable
private data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessagePayload>,
    val temperature: Float = 0.0f
)

@Serializable
private data class ChatMessagePayload(
    val role: String,
    val content: String
)

@Serializable
private data class ChatCompletionResponse(
    val choices: List<ChatChoice> = emptyList()
)

@Serializable
private data class ChatChoice(
    val message: ChatMessagePayload
)

/**
 * Optional HTTP client for schema-validated AI command interpretation.
 * Never executes actions directly; returns structured intent proposals only.
 */
@Singleton
class OpenAiCompatibleClient @Inject constructor(
    private val credentialStore: EncryptedCredentialStore,
    private val safeLogger: SafeLogger
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
        encodeDefaults = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .build()

    fun interpretCommand(
        endpointUrl: String,
        modelName: String,
        sanitizedUserCommand: String
    ): ResultWrapper<AiStructuredInterpretation> {
        val apiKey = credentialStore.getAiApiKey()
        if (apiKey.isNullOrBlank()) {
            return ResultWrapper.Failure(MeemoError.AiProviderFailure("API key not configured"))
        }
        if (!endpointUrl.startsWith("https://")) {
            return ResultWrapper.Failure(MeemoError.AiProviderFailure("HTTPS endpoint required"))
        }

        val systemPrompt = """
            You are Meemo's intent classifier. Respond ONLY with a compact JSON object matching:
            {"intent":"<INTENT_ID>","entity":"<OPTIONAL_ENTITY>","confidence":0.85,"explanation":"<SHORT_REASON>"}
            Allowed INTENT_ID values: OPEN_APPLICATION, OPEN_BROWSER, TORCH_ON, TORCH_OFF, GET_BATTERY_STATUS,
            GET_DEVICE_INFORMATION, SET_VOLUME, ADJUST_VOLUME, MEDIA_PLAY, MEDIA_PAUSE, MEDIA_NEXT,
            MEDIA_PREVIOUS, OPEN_SETTINGS, READ_NOTIFICATIONS, STOP_SPEAKING, CANCEL_COMMAND, UNKNOWN.
            Never output shell commands or markdown fences.
        """.trimIndent()

        val payload = ChatCompletionRequest(
            model = modelName,
            messages = listOf(
                ChatMessagePayload(role = "system", content = systemPrompt),
                ChatMessagePayload(role = "user", content = sanitizedUserCommand.take(300))
            )
        )

        return try {
            val requestBody = json.encodeToString(ChatCompletionRequest.serializer(), payload)
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(endpointUrl)
                .header("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    safeLogger.warn("OpenAiClient", "HTTP ${response.code} from AI provider")
                    return ResultWrapper.Failure(
                        MeemoError.AiProviderFailure("HTTP status ${response.code}")
                    )
                }
                val rawBody = response.body?.string().orEmpty()
                if (rawBody.length > 32_768) {
                    return ResultWrapper.Failure(
                        MeemoError.AiProviderFailure("Response exceeded safety size limit")
                    )
                }
                val parsedResponse = json.decodeFromString(ChatCompletionResponse.serializer(), rawBody)
                val assistantJson = parsedResponse.choices.firstOrNull()?.message?.content?.trim().orEmpty()
                val interpretation = json.decodeFromString(
                    AiStructuredInterpretation.serializer(),
                    assistantJson
                )
                ResultWrapper.Success(interpretation)
            }
        } catch (e: Exception) {
            safeLogger.error("OpenAiClient", "AI interpretation failed", e)
            ResultWrapper.Failure(MeemoError.AiProviderFailure(e.javaClass.simpleName))
        }
    }
}
