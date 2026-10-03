package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AutomationRule
import com.example.data.database.LoraConfig
import com.example.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.math.roundToInt

data class DataPoint(val label: String, val value: Float, val isPrediction: Boolean = false)

class AutomationViewModel(private val repository: AppRepository) : ViewModel() {

    // --- Saved Rules from Room ---
    val rules: StateFlow<List<AutomationRule>> = repository.getAllAutomationRules()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val savedConfigs: StateFlow<List<LoraConfig>> = repository.getAllLoraConfigs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Dataset Selection State ---
    private val _selectedDataset = MutableStateFlow("Vibração de Turbina (Manutenção Preditiva)")
    val selectedDataset = _selectedDataset.asStateFlow()

    // --- Mock Datasets Trend Points ---
    private val _datasetPoints = MutableStateFlow<List<DataPoint>>(emptyList())
    val datasetPoints = _datasetPoints.asStateFlow()

    // --- Streaming Simulation State ---
    private val _isStreamingActive = MutableStateFlow(false)
    val isStreamingActive = _isStreamingActive.asStateFlow()

    private val _currentSensorValue = MutableStateFlow(0.0)
    val currentSensorValue = _currentSensorValue.asStateFlow()

    private val _automationLogs = MutableStateFlow<List<String>>(emptyList())
    val automationLogs = _automationLogs.asStateFlow()

    // --- Fields for Creating a Rule ---
    private val _newRuleName = MutableStateFlow("Alerta Turbina 4")
    val newRuleName = _newRuleName.asStateFlow()

    private val _newRuleField = MutableStateFlow("Temperatura")
    val newRuleField = _newRuleField.asStateFlow()

    private val _newRuleOperator = MutableStateFlow(">")
    val newRuleOperator = _newRuleOperator.asStateFlow()

    private val _newRuleValue = MutableStateFlow(85.0)
    val newRuleValue = _newRuleValue.asStateFlow()

    private val _newRuleAction = MutableStateFlow("Alerta no Slack")
    val newRuleAction = _newRuleAction.asStateFlow()

    private val _newRuleModelId = MutableStateFlow(0)
    val newRuleModelId = _newRuleModelId.asStateFlow()

    init {
        loadDatasetPoints(_selectedDataset.value)
    }

    // --- Setters ---
    fun updateSelectedDataset(dataset: String) {
        _selectedDataset.value = dataset
        loadDatasetPoints(dataset)
    }
    fun updateNewRuleName(name: String) { _newRuleName.value = name }
    fun updateNewRuleField(field: String) { _newRuleField.value = field }
    fun updateNewRuleOperator(op: String) { _newRuleOperator.value = op }
    fun updateNewRuleValue(v: Double) { _newRuleValue.value = v }
    fun updateNewRuleAction(act: String) { _newRuleAction.value = act }
    fun updateNewRuleModelId(id: Int) { _newRuleModelId.value = id }

    private fun loadDatasetPoints(dataset: String) {
        val points = mutableListOf<DataPoint>()
        when (dataset) {
            "Vibração de Turbina (Manutenção Preditiva)" -> {
                // Historical
                points.add(DataPoint("Seg", 4.2f))
                points.add(DataPoint("Ter", 4.5f))
                points.add(DataPoint("Qua", 5.1f))
                points.add(DataPoint("Qui", 4.8f))
                points.add(DataPoint("Sex", 5.9f))
                points.add(DataPoint("Sáb", 6.8f))
                points.add(DataPoint("Dom", 7.4f))
                // Predicted (Next 3 days)
                points.add(DataPoint("Seg(P)", 8.2f, isPrediction = true))
                points.add(DataPoint("Ter(P)", 9.1f, isPrediction = true))
                points.add(DataPoint("Qua(P)", 10.5f, isPrediction = true))
            }
            "Consumo Energético da Fábrica" -> {
                points.add(DataPoint("00:00", 120f))
                points.add(DataPoint("04:00", 110f))
                points.add(DataPoint("08:00", 350f))
                points.add(DataPoint("12:00", 420f))
                points.add(DataPoint("16:00", 380f))
                points.add(DataPoint("20:00", 290f))
                points.add(DataPoint("24:00(P)", 150f, isPrediction = true))
                points.add(DataPoint("04:00(P)", 115f, isPrediction = true))
            }
            "Taxa de Cancelamento (Churn %)" -> {
                points.add(DataPoint("Jan", 1.2f))
                points.add(DataPoint("Fev", 1.4f))
                points.add(DataPoint("Mar", 1.8f))
                points.add(DataPoint("Abr", 2.1f))
                points.add(DataPoint("Mai", 2.0f))
                points.add(DataPoint("Jun", 2.8f))
                points.add(DataPoint("Jul(P)", 3.4f, isPrediction = true))
                points.add(DataPoint("Ago(P)", 4.1f, isPrediction = true))
            }
        }
        _datasetPoints.value = points
    }

    // --- Add/Delete Rule in Room ---
    fun addAutomationRule() {
        val rule = AutomationRule(
            name = _newRuleName.value,
            triggerField = _newRuleField.value,
            triggerOperator = _newRuleOperator.value,
            triggerValue = _newRuleValue.value,
            modelId = _newRuleModelId.value,
            actionType = _newRuleAction.value
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveAutomationRule(rule)
        }
    }

    fun deleteRule(rule: AutomationRule) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAutomationRuleById(rule.id)
        }
    }

    // --- Streaming and Rule Triggering Simulation ---
    fun toggleStreaming() {
        if (_isStreamingActive.value) {
            _isStreamingActive.value = false
            return
        }

        _isStreamingActive.value = true
        _automationLogs.value = listOf("=== Simulador de Sensores Ativado ===")

        viewModelScope.launch(Dispatchers.Main) {
            while (_isStreamingActive.value) {
                // Generate a random sensor reading depending on dataset
                val rawVal = when (_selectedDataset.value) {
                    "Vibração de Turbina (Manutenção Preditiva)" -> {
                        // Healthy is 2-6, anomaly is >8
                        if (Random.nextFloat() > 0.7) Random.nextDouble(7.8, 11.2) else Random.nextDouble(3.0, 6.5)
                    }
                    "Consumo Energético da Fábrica" -> {
                        if (Random.nextFloat() > 0.8) Random.nextDouble(410.0, 480.0) else Random.nextDouble(200.0, 390.0)
                    }
                    else -> { // Churn
                        if (Random.nextFloat() > 0.85) Random.nextDouble(3.5, 5.2) else Random.nextDouble(1.0, 2.9)
                    }
                }
                
                _currentSensorValue.value = (rawVal * 100.0).roundToInt() / 100.0
                addLog("Sensor Telemetria [${_selectedDataset.value.split(" ")[0]}]: ${String.format("%.2f", _currentSensorValue.value)}")

                // Evaluate against all active rules
                val activeRules = rules.value.filter { it.isActive }
                for (rule in activeRules) {
                    // Check if rule trigger matches the current dataset field
                    val isMatchingField = (rule.triggerField == "Temperatura" && _selectedDataset.value.contains("Turbina")) ||
                                          (rule.triggerField == "Consumo" && _selectedDataset.value.contains("Fábrica")) ||
                                          (rule.triggerField == "Churn" && _selectedDataset.value.contains("Churn"))

                    if (isMatchingField) {
                        val value = _currentSensorValue.value
                        val threshold = rule.triggerValue
                        val triggered = when (rule.triggerOperator) {
                            ">" -> value > threshold
                            "<" -> value < threshold
                            else -> (value - threshold).let { if (it < 0) -it else it } < 0.1 // equality approx
                        }

                        if (triggered) {
                            addLog("[GATILHO] Regra '${rule.name}' acionada! Valor ${String.format("%.2f", value)} violou limite ${rule.triggerOperator} ${rule.triggerValue}")
                            delay(400)
                            addLog("[INFERÊNCIA] Chamando LLM local com adaptadores LoRA (ID: ${rule.modelId})...")
                            delay(800)
                            
                            // Mock inference response based on fine-tuned status
                            val isFinedTuned = savedConfigs.value.any { it.id == rule.modelId && it.isTrained }
                            val modelResponse = if (isFinedTuned) {
                                "MiniMind-LoRA: Análise preditiva confirma tendência crítica. Probabilidade de colapso estrutural calculada em 94.5% para as próximas 4 horas."
                            } else {
                                "Modelo Base (Sem Ajuste): Alerta recebido. Dados indicam ultrapassagem de limite padrão de engenharia corporativa."
                            }
                            addLog(modelResponse)
                            delay(500)
                            addLog("[AÇÃO EXECUTADA] Enviando gatilho de automação -> Ação: ${rule.actionType}")
                            addLog("------------------------------------------------")
                        }
                    }
                }

                delay(3000) // Emit every 3 seconds
            }
            addLog("=== Simulador de Sensores Desativado ===")
        }
    }

    private fun addLog(message: String) {
        // Keep logs to last 50 entries
        val currentLogs = _automationLogs.value.toMutableList()
        currentLogs.add(message)
        if (currentLogs.size > 50) {
            currentLogs.removeAt(0)
        }
        _automationLogs.value = currentLogs
    }
}
