package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

data class LoraPreset(
    val name: String,
    val description: String,
    val task: String, // e.g. "Classificacao", "Sumarizacao", "Sentimento", "Codigo"
    val baseModel: String,
    val targetHardware: String,
    val rank: Int,
    val alpha: Int,
    val quantization: String,
    val targetModules: List<String>,
    val learningRate: Double,
    val epochs: Int,
    val batchSize: Int
)

class LoraViewModel(private val repository: AppRepository) : ViewModel() {

    // --- Hardcoded Predefined Optimization Presets ---
    val presets = listOf(
        LoraPreset(
            name = "Classificação Rápida",
            description = "Otimizado para triagem rápida de e-mails, tickets ou intenções.",
            task = "Classificacao",
            baseModel = "MiniMind-LM (100M)",
            targetHardware = "Workstation RTX 3060 (12GB VRAM)",
            rank = 8,
            alpha = 16,
            quantization = "4-bit (QLoRA)",
            targetModules = listOf("q_proj", "v_proj"),
            learningRate = 2e-4,
            epochs = 3,
            batchSize = 4
        ),
        LoraPreset(
            name = "Sumarização de Textos",
            description = "Foco em alta compreensão para resumos executivos e relatórios.",
            task = "Sumarizacao",
            baseModel = "Gemma-2-2B (2.6B)",
            targetHardware = "Workstation RTX 4060 (8GB VRAM)",
            rank = 16,
            alpha = 32,
            quantization = "4-bit (QLoRA)",
            targetModules = listOf("q_proj", "v_proj", "o_proj"),
            learningRate = 1e-4,
            epochs = 4,
            batchSize = 2
        ),
        LoraPreset(
            name = "Análise de Sentimentos",
            description = "Excelente precisão na detecção de tom, ironias e feedbacks.",
            task = "Sentimento",
            baseModel = "Phi-3-Mini (3.8B)",
            targetHardware = "Workstation RTX 3060 (12GB VRAM)",
            rank = 16,
            alpha = 32,
            quantization = "4-bit (QLoRA)",
            targetModules = listOf("q_proj", "v_proj"),
            learningRate = 1.5e-4,
            epochs = 3,
            batchSize = 2
        ),
        LoraPreset(
            name = "Geração de Código (Dev)",
            description = "Rank elevado para capturar lógica de programação complexa.",
            task = "Codigo",
            baseModel = "Llama-3-8B (8.0B)",
            targetHardware = "Servidor Corporativo A100 (80GB VRAM)",
            rank = 32,
            alpha = 64,
            quantization = "4-bit (QLoRA)",
            targetModules = listOf("q_proj", "k_proj", "v_proj", "o_proj"),
            learningRate = 5e-5,
            epochs = 5,
            batchSize = 1
        )
    )

    fun applyPreset(preset: LoraPreset) {
        _configName.value = "Preset_${preset.task}"
        _selectedModel.value = preset.baseModel
        _selectedHardware.value = preset.targetHardware
        _rank.value = preset.rank
        _alpha.value = preset.alpha
        _quantization.value = preset.quantization
        _selectedModules.value = preset.targetModules
        _learningRate.value = preset.learningRate
        _epochs.value = preset.epochs
        _batchSize.value = preset.batchSize
    }

    // --- Saved LoRA Configurations from Room ---
    val savedConfigs: StateFlow<List<LoraConfig>> = repository.getAllLoraConfigs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Training Configuration State ---
    private val _configName = MutableStateFlow("Meu_Ajuste_Predictivo")
    val configName = _configName.asStateFlow()

    private val _selectedModel = MutableStateFlow("MiniMind-LM (100M)")
    val selectedModel = _selectedModel.asStateFlow()

    private val _selectedHardware = MutableStateFlow("Workstation RTX 3060 (12GB VRAM)")
    val selectedHardware = _selectedHardware.asStateFlow()

    private val _rank = MutableStateFlow(8)
    val rank = _rank.asStateFlow()

    private val _alpha = MutableStateFlow(16)
    val alpha = _alpha.asStateFlow()

    private val _dropout = MutableStateFlow(0.05f)
    val dropout = _dropout.asStateFlow()

    private val _selectedModules = MutableStateFlow(listOf("q_proj", "v_proj"))
    val selectedModules = _selectedModules.asStateFlow()

    private val _quantization = MutableStateFlow("4-bit (QLoRA)")
    val quantization = _quantization.asStateFlow()

    private val _learningRate = MutableStateFlow(2e-4)
    val learningRate = _learningRate.asStateFlow()

    private val _epochs = MutableStateFlow(3)
    val epochs = _epochs.asStateFlow()

    private val _batchSize = MutableStateFlow(4)
    val batchSize = _batchSize.asStateFlow()

    // --- Training Simulation State ---
    private val _isTraining = MutableStateFlow(false)
    val isTraining = _isTraining.asStateFlow()

    private val _trainingProgress = MutableStateFlow(0f)
    val trainingProgress = _trainingProgress.asStateFlow()

    private val _currentEpoch = MutableStateFlow(0)
    val currentEpoch = _currentEpoch.asStateFlow()

    private val _currentStep = MutableStateFlow(0)
    val currentStep = _currentStep.asStateFlow()

    private val _currentLoss = MutableStateFlow(0f)
    val currentLoss = _currentLoss.asStateFlow()

    private val _currentAccuracy = MutableStateFlow(0f)
    val currentAccuracy = _currentAccuracy.asStateFlow()

    private val _lossHistory = MutableStateFlow<List<Float>>(emptyList())
    val lossHistory = _lossHistory.asStateFlow()

    private val _accuracyHistory = MutableStateFlow<List<Float>>(emptyList())
    val accuracyHistory = _accuracyHistory.asStateFlow()

    private val _consoleLogs = MutableStateFlow<List<String>>(emptyList())
    val consoleLogs = _consoleLogs.asStateFlow()

    // --- Estimates Calculation State ---
    data class Estimates(
        val originalSizeGb: Double,
        val quantizedSizeGb: Double,
        val adapterSizeMb: Double,
        val requiredTrainingVramGb: Double,
        val requiredInferenceVramGb: Double,
        val availableHardwareVramGb: Double,
        val estimatedInferenceLatency: Double, // tokens/sec
        val vramWarning: Boolean,
        val compatibilityStatus: String // "Excelente", "Alerta", "Impossível"
    )

    fun getEstimates(): Estimates {
        val modelParams = getModelParams(_selectedModel.value)
        val originalSize = modelParams * 2.0 // FP16 (2 bytes per param)

        val quantFactor = when (_quantization.value) {
            "4-bit (QLoRA)" -> 0.55
            "8-bit" -> 1.1
            else -> 2.1 // FP16 + scaling overhead
        }
        val quantizedSize = modelParams * quantFactor

        val numModules = _selectedModules.value.size
        val r = _rank.value
        // Adapter size is tiny, proportional to params * (rank / hidden_dim)
        // Let's model it: ~ (rank * 2 * layer_overheads * numModules)
        val adapterSizeMb = (modelParams * 1000.0 * (r.toDouble() / 4096.0) * (numModules.toDouble() / 4.0)) / 10.0

        val hardwareVram = getHardwareVram(_selectedHardware.value)

        // Training overhead: Activations + Optimizer States (Adam uses 8 bytes per trainable param)
        val optimizerStateSizeGb = (adapterSizeMb / 1024.0) * 4.0 // 4 bytes for AdamW FP32 gradients + states
        val activationOverheadGb = _batchSize.value.toDouble() * 0.4 * (modelParams / 2.0)
        val requiredTrainingVram = quantizedSize + (adapterSizeMb / 1024.0) + optimizerStateSizeGb + activationOverheadGb

        val requiredInferenceVram = quantizedSize + (adapterSizeMb / 1024.0) + 0.8 // KV Cache overhead

        // Latency model: Depends on whether the model fits in VRAM and the hardware speed
        val baseSpeed = when (_selectedHardware.value) {
            "Servidor Corporativo A100 (80GB VRAM)" -> 85.0
            "Workstation RTX 3060 (12GB VRAM)" -> 28.0
            "Workstation RTX 4060 (8GB VRAM)" -> 32.0
            else -> 4.0 // CPU Xeon
        }

        // If inference VRAM exceeds available hardware VRAM, offload occurs, dropping speed by 90%
        val isOffloaded = requiredInferenceVram > hardwareVram
        val isHardwareCpuOnly = _selectedHardware.value.contains("CPU")
        
        val estimatedLatency = if (isHardwareCpuOnly) {
            max(0.5, 3.5 / (modelParams + 0.1))
        } else if (isOffloaded) {
            max(0.2, (baseSpeed / (modelParams + 0.1)) * 0.08)
        } else {
            max(1.0, baseSpeed / (modelParams * 0.7 + 0.1))
        }

        val vramWarning = isOffloaded && !isHardwareCpuOnly
        val compatibilityStatus = if (isHardwareCpuOnly) {
            "Alerta (Apenas CPU - Latência Extrema)"
        } else if (requiredInferenceVram > hardwareVram) {
            "Impossível (OOM/Offload de CPU - Sem VRAM suficiente)"
        } else if (requiredInferenceVram > hardwareVram * 0.85) {
            "Alerta (VRAM quase cheia, risco de instabilidade)"
        } else {
            "Excelente (Totalmente compatível em VRAM local)"
        }

        return Estimates(
            originalSizeGb = originalSize,
            quantizedSizeGb = quantizedSize,
            adapterSizeMb = adapterSizeMb,
            requiredTrainingVramGb = requiredTrainingVram,
            requiredInferenceVramGb = requiredInferenceVram,
            availableHardwareVramGb = hardwareVram,
            estimatedInferenceLatency = estimatedLatency,
            vramWarning = vramWarning,
            compatibilityStatus = compatibilityStatus
        )
    }

    private fun getModelParams(modelName: String): Double {
        return when (modelName) {
            "MiniMind-LM (100M)" -> 0.1
            "Gemma-2-2B (2.6B)" -> 2.6
            "Phi-3-Mini (3.8B)" -> 3.8
            "Llama-3-8B (8.0B)" -> 8.0
            "Mistral-7B (7.2B)" -> 7.2
            else -> 0.1
        }
    }

    private fun getHardwareVram(hardwareName: String): Double {
        return when (hardwareName) {
            "Workstation RTX 4060 (8GB VRAM)" -> 8.0
            "Workstation RTX 3060 (12GB VRAM)" -> 12.0
            "Servidor Xeon (Apenas CPU - Latência Alta)" -> 1.0 // Allocated RAM simulated as low-speed VRAM
            "Servidor Corporativo A100 (80GB VRAM)" -> 80.0
            else -> 8.0
        }
    }

    // --- Setter Functions ---
    fun updateConfigName(name: String) { _configName.value = name }
    fun updateSelectedModel(model: String) { _selectedModel.value = model }
    fun updateSelectedHardware(hw: String) { _selectedHardware.value = hw }
    fun updateRank(r: Int) { _rank.value = r }
    fun updateAlpha(a: Int) { _alpha.value = a }
    fun updateDropout(d: Float) { _dropout.value = d }
    fun updateQuantization(q: String) { _quantization.value = q }
    fun updateLearningRate(lr: Double) { _learningRate.value = lr }
    fun updateEpochs(ep: Int) { _epochs.value = ep }
    fun updateBatchSize(bs: Int) { _batchSize.value = bs }
    
    fun toggleModule(module: String) {
        val currentList = _selectedModules.value.toMutableList()
        if (currentList.contains(module)) {
            if (currentList.size > 1) {
                currentList.remove(module)
            }
        } else {
            currentList.add(module)
        }
        _selectedModules.value = currentList
    }

    // --- Train Simulation Engine ---
    fun startTrainingSimulation() {
        if (_isTraining.value) return

        viewModelScope.launch(Dispatchers.Main) {
            _isTraining.value = true
            _trainingProgress.value = 0f
            _lossHistory.value = emptyList()
            _accuracyHistory.value = emptyList()
            _consoleLogs.value = emptyList()

            val totalEpochs = _epochs.value
            val totalSteps = totalEpochs * 10
            var stepCounter = 1

            addLog("=== Iniciando Treinamento LoRA Local ===")
            addLog("Modelo Base: ${_selectedModel.value}")
            addLog("Hardware alvo: ${_selectedHardware.value}")
            addLog("Hiperparâmetros: Rank=${_rank.value}, Alpha=${_alpha.value}, Dropout=${_dropout.value}")
            addLog("Módulos Alvo: ${_selectedModules.value.joinToString(", ")}")
            addLog("Quantização: ${_quantization.value}")
            addLog("Learning Rate: ${_learningRate.value}")
            addLog("Alocando memória VRAM...")
            delay(1000)

            val estimates = getEstimates()
            if (estimates.requiredTrainingVramGb > estimates.availableHardwareVramGb && !_selectedHardware.value.contains("CPU")) {
                addLog("[ALERTA] VRAM ultrapassou o limite físico. Ativando CPU Offloading (Treinamento ficará extremamente lento)...")
                delay(1000)
            } else {
                addLog("Alocação concluída com sucesso: ${String.format("%.2f", estimates.requiredTrainingVramGb)} GB alocados.")
                delay(500)
            }

            addLog("Carregando pesos em ${_quantization.value}...")
            delay(800)
            addLog("Iniciando otimizador AdamW para adaptadores LoRA...")
            delay(500)

            var loss = 4.2f
            var accuracy = 0.12f

            for (epoch in 1..totalEpochs) {
                _currentEpoch.value = epoch
                addLog("--- Época $epoch/$totalEpochs Iniciada ---")
                
                for (subStep in 1..10) {
                    _currentStep.value = stepCounter
                    _currentLoss.value = loss
                    _currentAccuracy.value = accuracy

                    // Append to histories
                    _lossHistory.value = _lossHistory.value + loss
                    _accuracyHistory.value = _accuracyHistory.value + accuracy

                    // Log output
                    addLog("Época $epoch | Passo $subStep/10 | Loss: ${String.format("%.4f", loss)} | Acurácia: ${String.format("%.2f", accuracy * 100)}%")

                    // Mock progress calculations
                    val progress = (stepCounter.toFloat() / totalSteps.toFloat())
                    _trainingProgress.value = progress

                    // Decay loss and increase accuracy
                    val randomFluctuation = (Random.nextFloat() - 0.5f) * 0.15f
                    loss = max(0.2f, loss - 0.35f + randomFluctuation * 0.2f)
                    
                    val accFluctuation = Random.nextFloat() * 0.05f
                    accuracy = min(0.98f, accuracy + 0.08f + accFluctuation)

                    // Delay simulating local training time
                    val sleepTime = if (estimates.requiredTrainingVramGb > estimates.availableHardwareVramGb) 800L else 350L
                    delay(sleepTime)
                    stepCounter++
                }
            }

            _currentLoss.value = 0.18f
            _currentAccuracy.value = 0.95f
            _lossHistory.value = _lossHistory.value + 0.18f
            _accuracyHistory.value = _accuracyHistory.value + 0.95f
            _trainingProgress.value = 1.0f

            addLog("Treinamento finalizado!")
            addLog("Salvando adaptadores LoRA (.safetensors) na memória corporativa...")
            delay(1000)
            addLog("Fusão de pesos executada com sucesso!")
            addLog("=== Fine-Tuning LoRA Concluído com Sucesso! ===")

            // Persist the trained model config to Room
            val finalConfig = LoraConfig(
                name = _configName.value,
                baseModel = _selectedModel.value,
                targetHardware = _selectedHardware.value,
                rank = _rank.value,
                alpha = _alpha.value,
                dropout = _dropout.value,
                targetModules = _selectedModules.value.joinToString(","),
                quantization = _quantization.value,
                learningRate = _learningRate.value,
                epochs = _epochs.value,
                batchSize = _batchSize.value,
                isTrained = true,
                trainingLossHistory = _lossHistory.value.joinToString(",") { it.toString() }
            )
            repository.saveLoraConfig(finalConfig)

            _isTraining.value = false
        }
    }

    private fun addLog(message: String) {
        _consoleLogs.value = _consoleLogs.value + message
    }

    fun deleteConfig(config: LoraConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteLoraConfigById(config.id)
        }
    }

    fun saveLoraConfigDirectly() {
        viewModelScope.launch(Dispatchers.IO) {
            val config = LoraConfig(
                name = _configName.value,
                baseModel = _selectedModel.value,
                targetHardware = _selectedHardware.value,
                rank = _rank.value,
                alpha = _alpha.value,
                dropout = _dropout.value,
                targetModules = _selectedModules.value.joinToString(","),
                quantization = _quantization.value,
                learningRate = _learningRate.value,
                epochs = _epochs.value,
                batchSize = _batchSize.value,
                isTrained = false, // Saved as draft/untrained configuration
                trainingLossHistory = ""
            )
            repository.saveLoraConfig(config)
        }
    }

    fun generateDeploymentJson(): String {
        val root = org.json.JSONObject()

        // Deployment metadata
        val metadata = org.json.JSONObject()
        metadata.put("generated_by", "MiniMind Studio")
        metadata.put("app_version", "1.0.0")
        metadata.put("timestamp", System.currentTimeMillis())
        metadata.put("target_hardware", _selectedHardware.value)
        metadata.put("spec_standard", "HuggingFace PEFT / vLLM Enterprise Ready")
        root.put("deployment_metadata", metadata)

        // Active LoRA parameters (PEFT / HuggingFace format)
        val activeConfig = org.json.JSONObject()
        activeConfig.put("config_name", _configName.value)
        activeConfig.put("base_model_name_or_path", _selectedModel.value)
        activeConfig.put("peft_type", "LORA")
        activeConfig.put("task_type", "CAUSAL_LM")
        activeConfig.put("r", _rank.value)
        activeConfig.put("lora_alpha", _alpha.value)
        activeConfig.put("lora_dropout", _dropout.value.toDouble())

        val targetModulesArray = org.json.JSONArray()
        _selectedModules.value.forEach { targetModulesArray.put(it) }
        activeConfig.put("target_modules", targetModulesArray)

        activeConfig.put("bias", "none")
        activeConfig.put("fan_in_fan_out", false)
        activeConfig.put("inference_mode", true)
        root.put("active_lora_configuration", activeConfig)

        // Quantization configuration (bitsandbytes / vLLM AWQ/GPTQ)
        val quant = org.json.JSONObject()
        quant.put("quantization_type", _quantization.value)
        quant.put("load_in_4bit", _quantization.value.contains("4-bit"))
        quant.put("load_in_8bit", _quantization.value.contains("8-bit"))
        quant.put("bnb_4bit_quant_type", "nf4")
        quant.put("bnb_4bit_use_double_quant", true)
        quant.put("bnb_4bit_compute_dtype", "bfloat16")
        root.put("quantization_config", quant)

        // Training hyperparameters
        val trainingHp = org.json.JSONObject()
        trainingHp.put("learning_rate", _learningRate.value)
        trainingHp.put("epochs", _epochs.value)
        trainingHp.put("batch_size", _batchSize.value)
        trainingHp.put("optimizer", "AdamW")
        trainingHp.put("weight_decay", 0.01)
        trainingHp.put("warmup_ratio", 0.03)
        trainingHp.put("lr_scheduler_type", "cosine")
        root.put("training_hyperparameters", trainingHp)

        // Hardware & Latency Estimates
        val est = getEstimates()
        val estimatesJson = org.json.JSONObject()
        estimatesJson.put("required_training_vram_gb", est.requiredTrainingVramGb)
        estimatesJson.put("required_inference_vram_gb", est.requiredInferenceVramGb)
        estimatesJson.put("available_hardware_vram_gb", est.availableHardwareVramGb)
        estimatesJson.put("estimated_throughput_tokens_per_sec", est.estimatedInferenceLatency)
        estimatesJson.put("compatibility_status", est.compatibilityStatus)
        root.put("resource_estimates", estimatesJson)

        // Presets Library
        val presetsArray = org.json.JSONArray()
        presets.forEach { preset ->
            val p = org.json.JSONObject()
            p.put("name", preset.name)
            p.put("task", preset.task)
            p.put("description", preset.description)
            p.put("base_model", preset.baseModel)
            p.put("target_hardware", preset.targetHardware)
            p.put("rank", preset.rank)
            p.put("alpha", preset.alpha)
            p.put("quantization", preset.quantization)

            val modulesArr = org.json.JSONArray()
            preset.targetModules.forEach { modulesArr.put(it) }
            p.put("target_modules", modulesArr)

            p.put("learning_rate", preset.learningRate)
            p.put("epochs", preset.epochs)
            p.put("batch_size", preset.batchSize)
            presetsArray.put(p)
        }
        root.put("presets_library", presetsArray)

        // Serving command snippet (for vLLM on remote Linux enterprise server)
        val cleanModel = _selectedModel.value.split(" ")[0].lowercase()
        val cleanName = _configName.value.lowercase().replace(" ", "_")
        root.put("remote_server_deployment_command", "vllm serve $cleanModel --enable-lora --lora-modules ${cleanName}=./adapters/${cleanName} --dtype bfloat16 --max-model-len 4096")

        return root.toString(2)
    }
}
