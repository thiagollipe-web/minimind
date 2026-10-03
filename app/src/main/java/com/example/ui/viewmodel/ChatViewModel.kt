package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.ChatMessage
import com.example.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(private val repository: AppRepository) : ViewModel() {

    private val _selectedSession = MutableStateFlow("Geral")
    val selectedSession: StateFlow<String> = _selectedSession.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    // Query messages dynamically when session changes
    val messages: StateFlow<List<ChatMessage>> = _selectedSession
        .flatMapLatest { sessionId ->
            repository.getMessagesForSession(sessionId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setSession(sessionId: String) {
        _selectedSession.value = sessionId
    }

    fun sendMessage(text: String) {
        if (text.trim().isEmpty() || _isSending.value) return

        val sessionId = _selectedSession.value
        val userMsg = ChatMessage(sessionId = sessionId, isUser = true, text = text)

        viewModelScope.launch(Dispatchers.IO) {
            _isSending.value = true
            // Save user message in local DB
            repository.saveMessage(userMsg)

            // Get system prompt based on session type
            val systemPrompt = getSystemPromptForSession(sessionId)

            // Call API
            val aiResponseText = repository.getAiResponse(sessionId, text, systemPrompt)

            // Save AI response
            val aiMsg = ChatMessage(sessionId = sessionId, isUser = false, text = aiResponseText)
            repository.saveMessage(aiMsg)
            _isSending.value = false
        }
    }

    fun clearHistory() {
        val sessionId = _selectedSession.value
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearSession(sessionId)
        }
    }

    private fun getSystemPromptForSession(sessionId: String): String {
        return when (sessionId) {
            "Consultor LoRA" -> """
                Você é o "Co-Pilot LoRA" da MiniMind. Seu foco exclusivo é ajudar o usuário a configurar, quantizar e implantar modelos locais de IA (como Gemma, Llama, Phi e MiniMind) utilizando técnicas de LoRA e QLoRA.
                Explique conceitos como rank (r), alpha, dropout, e target modules de maneira detalhada e matemática, relacionando-os com economia de memória VRAM e throughput de tokens/s.
                Ajude o usuário a debugar problemas como estouro de memória de GPU (OOM), over-fitting ou sub-fitting em sintonia com os recursos de hardware dele.
                Seja prático e mostre trechos de código explicativos em Python/PyTorch se necessário.
            """.trimIndent()

            "Automação Preditiva" -> """
                Você é o "Estrategista de Automação Preditiva". Seu foco é auxiliar o usuário a configurar e analisar regras de IFTTT inteligentes combinadas com previsões de Machine Learning/LLM local.
                Responda dúvidas sobre como criar gatilhos preditivos para falhas em equipamentos baseados em séries temporais, previsão de churn de clientes e análise de anomalias em sensores.
                Seja direto, técnico e focado em engenharia de controle, processos de negócios e monitoramento de sistemas.
            """.trimIndent()

            else -> """
                Você é o "MiniMind", um assistente de IA especialista em inteligência artificial generativa, otimização de LLMs (incluindo ajuste de hiperparâmetros LoRA/QLoRA) e automação de processos por meio de análises preditivas. 
                Seu papel é auxiliar engenheiros e administradores a planejar o fine-tuning de modelos locais de IA, estimar consumo de recursos como VRAM e latência de tokens, e sugerir ou criar automações inteligentes baseadas em limites preditivos de sensores corporativos. 
                Seja altamente técnico, proativo, didático e responda sempre em português.
            """.trimIndent()
        }
    }
}
