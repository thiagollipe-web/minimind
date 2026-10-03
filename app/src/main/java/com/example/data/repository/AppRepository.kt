package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.GeminiApiClient
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiGenerationConfig
import com.example.data.api.GeminiPart
import com.example.data.api.GeminiRequest
import com.example.data.api.GeminiSystemInstruction
import com.example.data.database.AutomationRule
import com.example.data.database.AutomationRuleDao
import com.example.data.database.ChatMessage
import com.example.data.database.ChatMessageDao
import com.example.data.database.LoraConfig
import com.example.data.database.LoraConfigDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import retrofit2.HttpException

class AppRepository(
    private val chatMessageDao: ChatMessageDao,
    private val loraConfigDao: LoraConfigDao,
    private val automationRuleDao: AutomationRuleDao
) {
    // --- CHAT MESSAGES ---
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>> {
        return chatMessageDao.getMessagesForSession(sessionId)
    }

    suspend fun saveMessage(message: ChatMessage) {
        chatMessageDao.insertMessage(message)
    }

    suspend fun clearSession(sessionId: String) {
        chatMessageDao.clearSession(sessionId)
    }

    // --- LORA CONFIGS ---
    fun getAllLoraConfigs(): Flow<List<LoraConfig>> {
        return loraConfigDao.getAllConfigs()
    }

    suspend fun getLoraConfigById(id: Int): LoraConfig? {
        return loraConfigDao.getConfigById(id)
    }

    suspend fun saveLoraConfig(config: LoraConfig): Long {
        return loraConfigDao.insertConfig(config)
    }

    suspend fun updateLoraConfig(config: LoraConfig) {
        loraConfigDao.updateConfig(config)
    }

    suspend fun deleteLoraConfigById(id: Int) {
        loraConfigDao.deleteConfigById(id)
    }

    // --- AUTOMATION RULES ---
    fun getAllAutomationRules(): Flow<List<AutomationRule>> {
        return automationRuleDao.getAllRules()
    }

    suspend fun saveAutomationRule(rule: AutomationRule) {
        automationRuleDao.insertRule(rule)
    }

    suspend fun updateAutomationRule(rule: AutomationRule) {
        automationRuleDao.updateRule(rule)
    }

    suspend fun deleteAutomationRuleById(id: Int) {
        automationRuleDao.deleteRuleById(id)
    }

    // --- GEMINI REST API INTEGRATION ---
    suspend fun getAiResponse(sessionId: String, userMessage: String, systemPrompt: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Erro: Chave de API do Gemini não configurada. Por favor, adicione sua chave de API nos Secrets do AI Studio ou configure seu arquivo .env com a variável GEMINI_API_KEY."
        }

        try {
            // Retrieve existing chat history to maintain conversation context
            val history = chatMessageDao.getMessagesForSession(sessionId).first()

            // Filter out any previous error messages and empty strings from context
            val cleanHistory = history.filter { msg ->
                val trimmed = msg.text.trim()
                trimmed.isNotEmpty() &&
                    !trimmed.startsWith("Erro na comunicação") &&
                    !trimmed.startsWith("Erro: ")
            }

            // Build strictly alternating user <-> model conversation turns
            val geminiContents = mutableListOf<GeminiContent>()
            var lastRole: String? = null

            for (msg in cleanHistory) {
                val role = if (msg.isUser) "user" else "model"
                // Gemini contents must begin with a user turn
                if (geminiContents.isEmpty() && role != "user") continue

                if (role == lastRole && geminiContents.isNotEmpty()) {
                    // Combine consecutive turns of same role to avoid Gemini 400 Alternating turns error
                    val lastIdx = geminiContents.size - 1
                    val existing = geminiContents[lastIdx].parts.first().text
                    val merged = existing + "\n" + msg.text.trim()
                    geminiContents[lastIdx] = GeminiContent(role = role, parts = listOf(GeminiPart(text = merged)))
                } else {
                    geminiContents.add(GeminiContent(role = role, parts = listOf(GeminiPart(text = msg.text.trim()))))
                    lastRole = role
                }
            }

            // Ensure the final turn in contents is the current userMessage with role "user"
            val trimmedUserMessage = userMessage.trim()
            if (lastRole == "user" && geminiContents.isNotEmpty()) {
                val lastTurnText = geminiContents.last().parts.first().text
                if (lastTurnText != trimmedUserMessage) {
                    geminiContents[geminiContents.size - 1] = GeminiContent(role = "user", parts = listOf(GeminiPart(text = trimmedUserMessage)))
                }
            } else {
                geminiContents.add(GeminiContent(role = "user", parts = listOf(GeminiPart(text = trimmedUserMessage))))
            }

            // Build system instructions if prompt is present
            val systemInstruction = if (systemPrompt.isNotBlank()) {
                GeminiSystemInstruction(parts = listOf(GeminiPart(text = systemPrompt.trim())))
            } else null

            // Setup generation config
            val generationConfig = GeminiGenerationConfig(
                temperature = 0.7f,
                topP = 0.95f,
                maxOutputTokens = 2048
            )

            val request = GeminiRequest(
                contents = geminiContents,
                generationConfig = generationConfig,
                systemInstruction = systemInstruction
            )

            // Try candidate models with automatic fallback
            val candidateModels = listOf("gemini-3.5-flash", "gemini-flash-latest", "gemini-2.5-flash")
            var lastException: Exception? = null

            for (model in candidateModels) {
                try {
                    val response = GeminiApiClient.service.generateContent(model, apiKey, request)
                    val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (!responseText.isNullOrBlank()) {
                        return responseText
                    }
                } catch (e: HttpException) {
                    lastException = e
                    val errorBody = e.response()?.errorBody()?.string() ?: ""
                    // If error is specific to model not found or invalid argument for model, try next candidate
                    if (e.code() == 404 || (e.code() == 400 && errorBody.contains("model", ignoreCase = true))) {
                        continue
                    } else {
                        // Throw to let the detailed parser extract the exact message
                        throw e
                    }
                } catch (e: Exception) {
                    lastException = e
                    break
                }
            }

            if (lastException != null) throw lastException
            return "Erro: O assistente não conseguiu gerar uma resposta adequada no momento."

        } catch (e: HttpException) {
            val rawError = try {
                e.response()?.errorBody()?.string() ?: ""
            } catch (ignored: Exception) {
                ""
            }

            val parsedMessage = try {
                if (rawError.isNotBlank()) {
                    val json = JSONObject(rawError)
                    val errorObj = json.optJSONObject("error")
                    errorObj?.optString("message", "")?.ifEmpty { null }
                } else null
            } catch (ignored: Exception) {
                null
            }

            return when {
                rawError.contains("API key not valid", ignoreCase = true) || rawError.contains("API_KEY_INVALID", ignoreCase = true) ->
                    "Erro: A chave de API do Gemini fornecida não é válida. Por favor, acesse o painel Secrets do AI Studio e atualize a chave GEMINI_API_KEY."

                rawError.contains("RESOURCE_EXHAUSTED", ignoreCase = true) || e.code() == 429 ->
                    "Erro: Limite de cota de requisições do Gemini atingido temporariamente. Aguarde alguns segundos e tente novamente."

                !parsedMessage.isNullOrBlank() ->
                    "Erro na API do Gemini (HTTP ${e.code()}): $parsedMessage"

                else ->
                    "Erro na comunicação com a API do Gemini (HTTP ${e.code()}): ${e.message()}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "Erro na comunicação com a Inteligência Artificial: ${e.localizedMessage ?: e.message ?: "Conexão de rede ou timeout."}"
        }
    }
}
