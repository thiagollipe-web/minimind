package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt
import kotlin.math.min
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.LoraConfig
import com.example.ui.viewmodel.LoraViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoraScreen(
    viewModel: LoraViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Criar & Treinar", "Ajustes Salvos", "Comparativo HD")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.Bold) }
                )
            }
        }

        when (selectedTab) {
            0 -> TrainConfigTab(viewModel)
            1 -> SavedConfigsTab(viewModel)
            2 -> ComparativeAnalyticsTab(viewModel)
        }
    }
}

@Composable
fun TrainConfigTab(viewModel: LoraViewModel) {
    val scrollState = rememberScrollState()
    val isTraining by viewModel.isTraining.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        if (!isTraining) {
            // Configuration Inputs
            ConfigurationForm(viewModel)
        } else {
            // Live Training Dashboard
            TrainingDashboard(viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationForm(viewModel: LoraViewModel) {
    val configName by viewModel.configName.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val selectedHardware by viewModel.selectedHardware.collectAsStateWithLifecycle()
    val rank by viewModel.rank.collectAsStateWithLifecycle()
    val alpha by viewModel.alpha.collectAsStateWithLifecycle()
    val quantization by viewModel.quantization.collectAsStateWithLifecycle()
    val learningRate by viewModel.learningRate.collectAsStateWithLifecycle()
    val epochs by viewModel.epochs.collectAsStateWithLifecycle()
    val selectedModules by viewModel.selectedModules.collectAsStateWithLifecycle()
    val estimates = viewModel.getEstimates()

    var showExportDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val deploymentJson = remember(configName, selectedModel, selectedHardware, rank, alpha, quantization, selectedModules, learningRate, epochs) {
        viewModel.generateDeploymentJson()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(deploymentJson.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Arquivo JSON exportado com sucesso!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao salvar arquivo: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Title Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.SettingsInputComponent, contentDescription = null, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Laboratório de Fine-Tuning", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Ajuste hiperparâmetros LoRA e simule localmente", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Presets Library Row
    Text(
        text = "Biblioteca de Presets (Otimizados por Hardware)",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        viewModel.presets.forEach { preset ->
            val isCurrentPreset = selectedModel == preset.baseModel &&
                    rank == preset.rank &&
                    alpha == preset.alpha &&
                    quantization == preset.quantization

            OutlinedCard(
                onClick = { viewModel.applyPreset(preset) },
                modifier = Modifier
                    .width(200.dp)
                    .height(130.dp)
                    .testTag("preset_card_${preset.task}"),
                border = BorderStroke(
                    width = if (isCurrentPreset) 2.dp else 1.dp,
                    color = if (isCurrentPreset) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                ),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (isCurrentPreset) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            if (isCurrentPreset) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = preset.description,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 2
                        )
                    }
                    Text(
                        text = "${preset.baseModel.split(" ")[0]} | r=${preset.rank}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }

    // Form inputs
    OutlinedTextField(
        value = configName,
        onValueChange = { viewModel.updateConfigName(it) },
        label = { Text("Nome do Adaptador LoRA") },
        modifier = Modifier.fillMaxWidth().testTag("lora_name_input")
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Base Model Dropdown
    var modelExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = modelExpanded,
        onExpandedChange = { modelExpanded = !modelExpanded }
    ) {
        OutlinedTextField(
            value = selectedModel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Modelo LLM Base") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = modelExpanded,
            onDismissRequest = { modelExpanded = false }
        ) {
            listOf(
                "MiniMind-LM (100M)",
                "Gemma-2-2B (2.6B)",
                "Phi-3-Mini (3.8B)",
                "Llama-3-8B (8.0B)",
                "Mistral-7B (7.2B)"
            ).forEach { model ->
                DropdownMenuItem(
                    text = { Text(model) },
                    onClick = {
                        viewModel.updateSelectedModel(model)
                        modelExpanded = false
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Target Hardware Dropdown
    var hardwareExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = hardwareExpanded,
        onExpandedChange = { hardwareExpanded = !hardwareExpanded }
    ) {
        OutlinedTextField(
            value = selectedHardware,
            onValueChange = {},
            readOnly = true,
            label = { Text("Hardware Corporativo de Destino") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = hardwareExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = hardwareExpanded,
            onDismissRequest = { hardwareExpanded = false }
        ) {
            listOf(
                "Workstation RTX 4060 (8GB VRAM)",
                "Workstation RTX 3060 (12GB VRAM)",
                "Servidor Xeon (Apenas CPU - Latência Alta)",
                "Servidor Corporativo A100 (80GB VRAM)"
            ).forEach { hw ->
                DropdownMenuItem(
                    text = { Text(hw) },
                    onClick = {
                        viewModel.updateSelectedHardware(hw)
                        hardwareExpanded = false
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // LoRA Parameters sliders
    Text("Parâmetros do Adaptador LoRA", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    
    Spacer(modifier = Modifier.height(8.dp))

    // Rank Slider (r)
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Rank LoRA (r): $rank", style = MaterialTheme.typography.bodyMedium)
            Text("Influência no tamanho e expressividade", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Slider(
            value = rank.toFloat(),
            onValueChange = { viewModel.updateRank(it.roundToInt()) },
            valueRange = 4f..64f,
            steps = 4,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Alpha Slider (alpha)
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Alpha LoRA (α): $alpha", style = MaterialTheme.typography.bodyMedium)
            Text("Fator de escala de pesos", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Slider(
            value = alpha.toFloat(),
            onValueChange = { viewModel.updateAlpha(it.roundToInt()) },
            valueRange = 8f..128f,
            steps = 4,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Quantization Selector
    Text("Precisão / Quantização", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("4-bit (QLoRA)", "8-bit", "Nenhum (FP16)").forEach { quant ->
            FilterChip(
                selected = quantization == quant,
                onClick = { viewModel.updateQuantization(quant) },
                label = { Text(quant) }
            )
        }
    }

    // Target Modules Multi-select
    Text("Módulos de Atenção Alvo", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("q_proj", "k_proj", "v_proj", "o_proj").forEach { module ->
            FilterChip(
                selected = selectedModules.contains(module),
                onClick = { viewModel.toggleModule(module) },
                label = { Text(module) }
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text("Hiperparâmetros de Otimização", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = learningRate.toString(),
            onValueChange = { value -> value.toDoubleOrNull()?.let { viewModel.updateLearningRate(it) } },
            label = { Text("Taxa de Aprendizado") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f).testTag("lora_lr_input")
        )

        OutlinedTextField(
            value = epochs.toString(),
            onValueChange = { value -> value.toIntOrNull()?.let { viewModel.updateEpochs(it) } },
            label = { Text("Épocas") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(0.7f).testTag("lora_epochs_input")
        )

        val batchSize by viewModel.batchSize.collectAsStateWithLifecycle()
        OutlinedTextField(
            value = batchSize.toString(),
            onValueChange = { value -> value.toIntOrNull()?.let { viewModel.updateBatchSize(it) } },
            label = { Text("Batch Size") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(0.7f).testTag("lora_batch_size_input")
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Estimates Panel Card
    EstimatesPanel(estimates)

    Spacer(modifier = Modifier.height(24.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = { viewModel.saveLoraConfigDirectly() },
            modifier = Modifier.weight(1f).height(48.dp).testTag("save_lora_params_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Salvar Parâmetros", fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = { viewModel.startTrainingSimulation() },
            modifier = Modifier.weight(1.3f).height(48.dp).testTag("start_train_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Iniciar Treino", fontWeight = FontWeight.Bold)
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    FilledTonalButton(
        onClick = { showExportDialog = true },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("download_configuration_button"),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Icon(Icons.Default.Download, contentDescription = "Download Configuration", tint = MaterialTheme.colorScheme.onTertiaryContainer)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Download Configuração (JSON)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
    }

    if (showExportDialog) {
        val fileName = "${configName.lowercase().replace(" ", "_")}_lora_config.json"
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exportar para Servidor", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Arquivo: $fileName",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Contém os parâmetros LoRA ativos, quantização, estimativas de VRAM/latência e a biblioteca de presets no formato padrão HuggingFace PEFT / vLLM.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = deploymentJson,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF81C784)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        launcher.launch(fileName)
                    }
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Baixar")
                }
            },
            dismissButton = {
                Row {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(deploymentJson))
                            Toast.makeText(context, "JSON copiado para a área de transferência!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copiar")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, deploymentJson)
                                putExtra(Intent.EXTRA_TITLE, fileName)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Exportar Configuração LoRA"))
                        }
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        )
    }
}

@Composable
fun EstimatesPanel(estimates: LoraViewModel.Estimates) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (estimates.vramWarning) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Painel de Estimativa de Recursos",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (estimates.vramWarning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = if (estimates.vramWarning) Icons.Default.Warning else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (estimates.vramWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            MetricRow("Tamanho do LLM Original:", "${String.format("%.2f", estimates.originalSizeGb)} GB")
            MetricRow("Tamanho do LLM Quantizado:", "${String.format("%.2f", estimates.quantizedSizeGb)} GB")
            MetricRow("Overhead de Pesos LoRA:", "${String.format("%.3f", estimates.adapterSizeMb)} MB")
            MetricRow("VRAM Mínima Inferência:", "${String.format("%.2f", estimates.requiredInferenceVramGb)} GB")
            MetricRow("VRAM Estimada para Treino:", "${String.format("%.2f", estimates.requiredTrainingVramGb)} GB")
            MetricRow("VRAM Disponível Hardware:", "${String.format("%.2f", estimates.availableHardwareVramGb)} GB")
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            MetricRow(
                "Latência de Inferência Estimada:",
                "${String.format("%.1f", estimates.estimatedInferenceLatency)} tokens/s",
                boldValue = true
            )
            MetricRow("Status de Compatibilidade:", estimates.compatibilityStatus, boldValue = true)
        }
    }
}

@Composable
fun MetricRow(label: String, value: String, boldValue: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (boldValue) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun TrainingDashboard(viewModel: LoraViewModel) {
    val progress by viewModel.trainingProgress.collectAsStateWithLifecycle()
    val epoch by viewModel.currentEpoch.collectAsStateWithLifecycle()
    val step by viewModel.currentStep.collectAsStateWithLifecycle()
    val loss by viewModel.currentLoss.collectAsStateWithLifecycle()
    val accuracy by viewModel.currentAccuracy.collectAsStateWithLifecycle()
    val lossHistory by viewModel.lossHistory.collectAsStateWithLifecycle()
    val accuracyHistory by viewModel.accuracyHistory.collectAsStateWithLifecycle()
    val logs by viewModel.consoleLogs.collectAsStateWithLifecycle()

    val logListState = rememberLazyListState()

    // Keep console log scrolled to bottom
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            logListState.animateScrollToItem(logs.size - 1)
        }
    }

    Text("Treinamento LoRA em Andamento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    
    Spacer(modifier = Modifier.height(12.dp))

    // Real-time metrics
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Época", style = MaterialTheme.typography.labelSmall)
                Text("$epoch / ${viewModel.epochs.value}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        Card(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Loss Atual", style = MaterialTheme.typography.labelSmall)
                Text(String.format("%.4f", loss), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
        }
        Card(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Acurácia", style = MaterialTheme.typography.labelSmall)
                Text("${String.format("%.1f", accuracy * 100)}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Progress Bar
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text("Progresso Geral: ${(progress * 100).roundToInt()}%", style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)

    Spacer(modifier = Modifier.height(16.dp))

    // Beautiful Custom Canvas Line Charts side by side
    Text("Curvas de Desempenho (Tempo Real)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))
    
    Row(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Text("Loss de Treino", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.Red)
            Spacer(modifier = Modifier.height(4.dp))
            LineChartCanvas(data = lossHistory, isLoss = true)
        }
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Text("Acurácia (%)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
            Spacer(modifier = Modifier.height(4.dp))
            LineChartCanvas(data = accuracyHistory, isLoss = false)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // PyTorch Console Log
    Text("Terminal de Execução PyTorch/LoRA", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
            .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        LazyColumn(
            state = logListState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(logs) { log ->
                Text(
                    text = log,
                    color = if (log.contains("ALERTA") || log.contains("warning")) Color.Yellow else if (log.contains("=== ")) Color.Cyan else Color.Green,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun LineChartCanvas(data: List<Float>, isLoss: Boolean) {
    val outlineColor = MaterialTheme.colorScheme.outline
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.05f))
            .border(1.dp, outlineColor, RoundedCornerShape(4.dp))
    ) {
        if (data.size < 2) return@Canvas

        val maxVal = if (isLoss) 4.5f else 1.0f
        val minVal = 0.0f
        val range = maxVal - minVal

        val path = Path()
        val widthStep = size.width / (data.size - 1)

        data.forEachIndexed { index, value ->
            val x = index * widthStep
            // Invert y because canvas (0,0) is top-left
            val normalizedY = (value - minVal) / range
            val y = size.height - (normalizedY * size.height)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = if (isLoss) Color.Red else Color(0xFF4CAF50),
            style = Stroke(width = 4f)
        )
    }
}

@Composable
fun SavedConfigsTab(viewModel: LoraViewModel) {
    val configs by viewModel.savedConfigs.collectAsStateWithLifecycle()

    if (configs.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.SettingsInputComponent,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Nenhum ajuste LoRA encontrado",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Vá para a aba 'Criar & Treinar' para criar e simular seu primeiro fine-tuning em hardware local corporativo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    } else {
        var exportConfig by remember { mutableStateOf<LoraConfig?>(null) }
        val context = LocalContext.current
        val clipboardManager = LocalClipboardManager.current

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            uri?.let {
                try {
                    val json = exportConfig?.let { cfg -> formatConfigToJson(cfg) } ?: ""
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, "Configuração JSON exportada com sucesso!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Erro ao salvar arquivo: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(configs) { config ->
                SavedConfigItem(
                    config = config,
                    onExport = { exportConfig = config },
                    onDelete = { viewModel.deleteConfig(config) }
                )
            }
        }

        exportConfig?.let { config ->
            val json = remember(config) { formatConfigToJson(config) }
            val fileName = "${config.name.lowercase().replace(" ", "_")}_peft_config.json"
            AlertDialog(
                onDismissRequest = { exportConfig = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exportar Ajuste Salvo", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "Arquivo: $fileName",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Configuração pronta para implantação em servidores remotos corporativos.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black)
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = json,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Color(0xFF81C784)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { launcher.launch(fileName) }) {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Baixar")
                    }
                },
                dismissButton = {
                    Row {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(json))
                                Toast.makeText(context, "JSON copiado!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copiar")
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedButton(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, json)
                                    putExtra(Intent.EXTRA_TITLE, fileName)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Exportar LoRA JSON"))
                            }
                        ) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun SavedConfigItem(config: LoraConfig, onExport: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (config.isTrained) Icons.Default.CheckCircle else Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = if (config.isTrained) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(config.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                Row {
                    IconButton(onClick = onExport) {
                        Icon(Icons.Default.Download, contentDescription = "Exportar JSON", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Deletar Ajuste", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            MetricRow("Modelo Base:", config.baseModel)
            MetricRow("Quantização / Precisão:", config.quantization)
            MetricRow("Hiperparâmetros LoRA:", "Rank r=${config.rank} | Alpha α=${config.alpha}")
            MetricRow("Módulos Alvo:", config.targetModules)
            MetricRow("L.R. / Épocas / Batch Size:", "${config.learningRate} | ${config.epochs} epochs | BS ${config.batchSize}")
            
            if (config.trainingLossHistory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Histórico de Loss do Treino", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                val lossList = config.trainingLossHistory.split(",").mapNotNull { it.toFloatOrNull() }
                Box(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    LineChartCanvas(data = lossList, isLoss = true)
                }
            }
        }
    }
}

fun formatConfigToJson(config: LoraConfig): String {
    val root = org.json.JSONObject()
    val metadata = org.json.JSONObject()
    metadata.put("generated_by", "MiniMind Studio")
    metadata.put("config_id", config.id)
    metadata.put("config_name", config.name)
    metadata.put("base_model_name_or_path", config.baseModel)
    metadata.put("target_hardware", config.targetHardware)
    metadata.put("is_trained", config.isTrained)
    metadata.put("timestamp", config.timestamp)
    metadata.put("export_standard", "HuggingFace PEFT Adapter Config")
    root.put("deployment_metadata", metadata)

    val peft = org.json.JSONObject()
    peft.put("peft_type", "LORA")
    peft.put("task_type", "CAUSAL_LM")
    peft.put("r", config.rank)
    peft.put("lora_alpha", config.alpha)
    peft.put("lora_dropout", config.dropout.toDouble())

    val modules = org.json.JSONArray()
    config.targetModules.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { modules.put(it) }
    peft.put("target_modules", modules)
    peft.put("bias", "none")
    root.put("active_peft_configuration", peft)

    val quant = org.json.JSONObject()
    quant.put("quantization_type", config.quantization)
    quant.put("load_in_4bit", config.quantization.contains("4-bit"))
    quant.put("load_in_8bit", config.quantization.contains("8-bit"))
    root.put("quantization_config", quant)

    val hp = org.json.JSONObject()
    hp.put("learning_rate", config.learningRate)
    hp.put("epochs", config.epochs)
    hp.put("batch_size", config.batchSize)
    root.put("training_hyperparameters", hp)

    val cleanModel = config.baseModel.split(" ")[0].lowercase()
    val cleanName = config.name.lowercase().replace(" ", "_")
    root.put("remote_server_deployment_command", "vllm serve $cleanModel --enable-lora --lora-modules ${cleanName}=./adapters/${cleanName} --dtype bfloat16")

    return root.toString(2)
}

// --- HIGH DEFINITION COMPARATIVE ANALYTICS TAB ---

data class ComparativeItem(
    val name: String,
    val rank: Int,
    val alpha: Int,
    val vramUsageGb: Double,
    val latencyTokensSec: Double,
    val isSaved: Boolean
)

@Composable
fun ComparativeAnalyticsTab(viewModel: LoraViewModel) {
    val configs by viewModel.savedConfigs.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // Combined list of saved configs + standard industry benchmarks
    val benchmarkConfigs = remember(configs) {
        val list = mutableListOf<ComparativeItem>()

        // 1. Add saved trained configs first
        configs.forEach { config ->
            val numParams = when {
                config.baseModel.contains("100M") -> 0.1
                config.baseModel.contains("2B") -> 2.6
                config.baseModel.contains("3.8B") -> 3.8
                config.baseModel.contains("8B") -> 8.0
                config.baseModel.contains("7B") -> 7.2
                else -> 0.1
            }
            val quantFactor = when (config.quantization) {
                "4-bit (QLoRA)" -> 0.55
                "8-bit" -> 1.1
                else -> 2.1
            }
            val quantizedSize = numParams * quantFactor
            val vram = quantizedSize + 0.8 // Inference memory + KV Cache

            val baseSpeed = 28.0
            val isOffloaded = vram > 12.0 // Assuming RTX 3060 12GB reference
            val latency = if (isOffloaded) {
                maxOf(0.5, (baseSpeed / (numParams + 0.1)) * 0.1)
            } else {
                maxOf(1.0, baseSpeed / (numParams * 0.7 + 0.1))
            }

            list.add(
                ComparativeItem(
                    name = config.name + " (Local)",
                    rank = config.rank,
                    alpha = config.alpha,
                    vramUsageGb = vram,
                    latencyTokensSec = latency,
                    isSaved = true
                )
            )
        }

        // 2. Add standard industry reference benchmarks for comparison
        list.add(ComparativeItem("MiniMind 100M (r=8, a=16, 4-bit)", 8, 16, 1.1, 45.0, isSaved = false))
        list.add(ComparativeItem("MiniMind 100M (r=32, a=64, 8-bit)", 32, 64, 1.4, 38.0, isSaved = false))
        list.add(ComparativeItem("Gemma 2B (r=8, a=16, 4-bit)", 8, 16, 5.4, 31.0, isSaved = false))
        list.add(ComparativeItem("Gemma 2B (r=16, a=32, 4-bit)", 16, 32, 5.6, 27.0, isSaved = false))
        list.add(ComparativeItem("Llama 8B (r=8, a=16, 4-bit)", 8, 16, 16.2, 14.0, isSaved = false))
        list.add(ComparativeItem("Llama 8B (r=64, a=128, FP16)", 64, 128, 28.5, 2.1, isSaved = false))

        list
    }

    var selectedItem by remember { mutableStateOf<ComparativeItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Análise Comparativa HD", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Consumo de VRAM e Latência de Adaptadores Salvos vs Mercado", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        InteractiveComparativeChart(
            items = benchmarkConfigs,
            selectedItem = selectedItem,
            onItemSelect = { selectedItem = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Selected config info card
        selectedItem?.let { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (item.isSaved) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (item.isSaved) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    MetricRow("Parâmetros do Adaptador:", "Rank r=${item.rank} | Alpha α=${item.alpha}")
                    MetricRow("Consumo de VRAM Estimado:", "${String.format("%.2f", item.vramUsageGb)} GB")
                    MetricRow("Latência Estimada (Velocidade):", "${String.format("%.1f", item.latencyTokensSec)} tokens/s")
                    MetricRow("Status do Adaptador:", if (item.isSaved) "Ajustado Localmente" else "Benchmark Referência")
                }
            }
        } ?: run {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "Toque em qualquer barra no gráfico acima para visualizar os detalhes e comparar os requisitos de hardware.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun InteractiveComparativeChart(
    items: List<ComparativeItem>,
    selectedItem: ComparativeItem?,
    onItemSelect: (ComparativeItem) -> Unit
) {
    val outlineColor = MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.04f))
            .border(1.dp, outlineColor, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("VRAM & Latência (Toque para Selecionar)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF9C27B0), RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("VRAM (GB)", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF00BCFF), RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Velocidade (t/s)", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(items) {
                        detectTapGestures { offset ->
                            val padding = 40f
                            val chartWidth = size.width - 2 * padding
                            if (chartWidth > 0 && offset.x >= padding && offset.x <= size.width - padding) {
                                val itemWidth = chartWidth / items.size
                                val index = ((offset.x - padding) / itemWidth).toInt()
                                if (index in items.indices) {
                                    onItemSelect(items[index])
                                }
                            }
                        }
                    }
            ) {
                val padding = 40f
                val chartWidth = size.width - 2 * padding
                if (chartWidth <= 0 || items.isEmpty()) return@Canvas

                val itemWidth = chartWidth / items.size
                val barWidth = itemWidth * 0.28f

                val maxVram = 32f
                val maxLatency = 50f

                // Draw Gridlines & Axis labels
                for (i in 0..4) {
                    val ratio = i / 4f
                    val yGrid = size.height - (size.height - 40f) * ratio - 20f

                    // Horizontal grid line
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.2f),
                        start = Offset(padding, yGrid),
                        end = Offset(size.width - padding, yGrid),
                        strokeWidth = 2f
                    )
                }

                // Draw Bar Groups
                items.forEachIndexed { index, item ->
                    val xCenter = padding + index * itemWidth + itemWidth / 2f
                    val xBarVram = xCenter - barWidth - 4f
                    val xBarLatency = xCenter + 4f

                    val yBaseline = size.height - 20f

                    // Heights
                    val vramRatio = (item.vramUsageGb.toFloat() / maxVram).coerceIn(0f, 1f)
                    val vramHeight = (size.height - 60f) * vramRatio

                    val latencyRatio = (item.latencyTokensSec.toFloat() / maxLatency).coerceIn(0f, 1f)
                    val latencyHeight = (size.height - 60f) * latencyRatio

                    // Highlight Selected Group
                    if (selectedItem == item) {
                        drawRect(
                            color = Color.White.copy(alpha = 0.15f),
                            topLeft = Offset(padding + index * itemWidth, 10f),
                            size = androidx.compose.ui.geometry.Size(itemWidth, size.height - 30f)
                        )
                    }

                    // Draw VRAM Bar (Purple/Magenta)
                    drawRect(
                        color = Color(0xFF9C27B0), // Purple
                        topLeft = Offset(xBarVram, yBaseline - vramHeight),
                        size = androidx.compose.ui.geometry.Size(barWidth, vramHeight)
                    )

                    // Draw Latency Bar (Cyan/Teal)
                    drawRect(
                        color = Color(0xFF00BCFF), // Teal/Cyan
                        topLeft = Offset(xBarLatency, yBaseline - latencyHeight),
                        size = androidx.compose.ui.geometry.Size(barWidth, latencyHeight)
                    )

                    // Subtle divider between groups
                    if (index < items.size - 1) {
                        val xDivider = padding + (index + 1) * itemWidth
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.15f),
                            start = Offset(xDivider, 10f),
                            end = Offset(xDivider, yBaseline),
                            strokeWidth = 1f
                        )
                    }
                }
            }
        }
    }
}

