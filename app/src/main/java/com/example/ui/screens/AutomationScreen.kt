package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.AutomationRule
import com.example.ui.screens.MetricRow
import com.example.ui.viewmodel.AutomationViewModel
import com.example.ui.viewmodel.DataPoint
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationScreen(
    viewModel: AutomationViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val savedConfigs by viewModel.savedConfigs.collectAsStateWithLifecycle()
    val selectedDataset by viewModel.selectedDataset.collectAsStateWithLifecycle()
    val datasetPoints by viewModel.datasetPoints.collectAsStateWithLifecycle()
    val isStreaming by viewModel.isStreamingActive.collectAsStateWithLifecycle()
    val currentSensorValue by viewModel.currentSensorValue.collectAsStateWithLifecycle()
    val logs by viewModel.automationLogs.collectAsStateWithLifecycle()

    var showAddRuleForm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // --- Header ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Painel Preditivo & Automação", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Gerencie regras IFTTT com suporte de IA local", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Dataset Selector and Chart ---
        Text("Datasets de Telemetria e Previsões", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        var datasetExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = datasetExpanded,
            onExpandedChange = { datasetExpanded = !datasetExpanded }
        ) {
            OutlinedTextField(
                value = selectedDataset,
                onValueChange = {},
                readOnly = true,
                label = { Text("Selecione o Dataset") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = datasetExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = datasetExpanded,
                onDismissRequest = { datasetExpanded = false }
            ) {
                listOf(
                    "Vibração de Turbina (Manutenção Preditiva)",
                    "Consumo Energético da Fábrica",
                    "Taxa de Cancelamento (Churn %)"
                ).forEach { dataset ->
                    DropdownMenuItem(
                        text = { Text(dataset) },
                        onClick = {
                            viewModel.updateSelectedDataset(dataset)
                            datasetExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // High Fidelity Predictive Chart
        PredictiveChart(datasetPoints)

        Spacer(modifier = Modifier.height(16.dp))

        // --- Telemetry Streaming Console ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Simulador de Sensores de Fábrica", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isStreaming) "Valor Atual: $currentSensorValue" else "Simulação inativa",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isStreaming) Color(0xFF4CAF50) else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = { viewModel.toggleStreaming() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isStreaming) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(if (isStreaming) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isStreaming) "Pausar" else "Iniciar")
                    }
                }

                AnimatedVisibility(visible = isStreaming || logs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val logListState = rememberLazyListState()
                    LaunchedEffect(logs.size) {
                        if (logs.isNotEmpty()) {
                            logListState.animateScrollToItem(logs.size - 1)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                            .padding(8.dp)
                    ) {
                        LazyColumn(state = logListState, modifier = Modifier.fillMaxSize()) {
                            items(logs) { log ->
                                Text(
                                    text = log,
                                    color = if (log.contains("[GATILHO]") || log.contains("AÇÃO")) Color.Yellow else if (log.contains("MiniMind-LoRA")) Color.Cyan else Color.Green,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- IFTTT Rules Header ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Regras de Automação (IFTTT Preditivo)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            IconButton(onClick = { showAddRuleForm = !showAddRuleForm }) {
                Icon(if (showAddRuleForm) Icons.Default.Close else Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Expanded Rule Form
        AnimatedVisibility(visible = showAddRuleForm) {
            RuleCreationForm(viewModel, onRuleAdded = { showAddRuleForm = false })
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Rules List
        if (rules.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "Nenhuma regra de automação criada. Clique no botão (+) acima para adicionar um gatilho de telemetria integrado a uma LLM local.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            }
        } else {
            rules.forEach { rule ->
                val modelName = savedConfigs.firstOrNull { it.id == rule.modelId }?.name ?: "Modelo Base"
                RuleItem(rule, modelName, onDelete = { viewModel.deleteRule(rule) })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun PredictiveChart(points: List<DataPoint>) {
    val outlineColor = MaterialTheme.colorScheme.outline
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        border = BorderStroke(1.dp, outlineColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Gráfico de Tendências e Previsões", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF2196F3), RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Histórico", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFE91E63), RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Previsão Local LLM", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Canvas(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.03f))) {
                if (points.isEmpty()) return@Canvas

                val maxVal = points.maxOf { it.value } * 1.15f
                val minVal = 0.0f
                val range = maxVal - minVal

                val widthStep = size.width / (points.size - 1)

                val historyPath = Path()
                val predictionPath = Path()

                var lastHistX = 0f
                var lastHistY = size.height

                points.forEachIndexed { index, point ->
                    val x = index * widthStep
                    val normalizedY = (point.value - minVal) / range
                    val y = size.height - (normalizedY * size.height)

                    if (!point.isPrediction) {
                        if (index == 0) {
                            historyPath.moveTo(x, y)
                        } else {
                            historyPath.lineTo(x, y)
                        }
                        lastHistX = x
                        lastHistY = y
                    } else {
                        if (predictionPath.isEmpty) {
                            // Link history with prediction line
                            predictionPath.moveTo(lastHistX, lastHistY)
                        }
                        predictionPath.lineTo(x, y)
                    }
                }

                // Draw solid historical line
                drawPath(
                    path = historyPath,
                    color = Color(0xFF2196F3),
                    style = Stroke(width = 5f)
                )

                // Draw dotted/dashed predictive line
                drawPath(
                    path = predictionPath,
                    color = Color(0xFFE91E63),
                    style = Stroke(width = 5f)
                )

                // Draw point markers
                points.forEachIndexed { index, point ->
                    val x = index * widthStep
                    val normalizedY = (point.value - minVal) / range
                    val y = size.height - (normalizedY * size.height)

                    drawCircle(
                        color = if (point.isPrediction) Color(0xFFE91E63) else Color(0xFF2196F3),
                        radius = 8f,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleCreationForm(viewModel: AutomationViewModel, onRuleAdded: () -> Unit) {
    val newRuleName by viewModel.newRuleName.collectAsStateWithLifecycle()
    val newRuleField by viewModel.newRuleField.collectAsStateWithLifecycle()
    val newRuleOperator by viewModel.newRuleOperator.collectAsStateWithLifecycle()
    val newRuleValue by viewModel.newRuleValue.collectAsStateWithLifecycle()
    val newRuleAction by viewModel.newRuleAction.collectAsStateWithLifecycle()
    val newRuleModelId by viewModel.newRuleModelId.collectAsStateWithLifecycle()
    val savedConfigs by viewModel.savedConfigs.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Nova Regra de Gatilho Preditivo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = newRuleName,
                onValueChange = { viewModel.updateNewRuleName(it) },
                label = { Text("Nome da Regra") },
                modifier = Modifier.fillMaxWidth().testTag("rule_name_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Field Selector
                var fieldExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = fieldExpanded,
                    onExpandedChange = { fieldExpanded = !fieldExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = newRuleField,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Seletor de Sensor") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fieldExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = fieldExpanded,
                        onDismissRequest = { fieldExpanded = false }
                    ) {
                        listOf("Temperatura", "Consumo", "Churn").forEach { field ->
                            DropdownMenuItem(
                                text = { Text(field) },
                                onClick = {
                                    viewModel.updateNewRuleField(field)
                                    fieldExpanded = false
                                }
                            )
                        }
                    }
                }

                // Operator Selector
                var operatorExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = operatorExpanded,
                    onExpandedChange = { operatorExpanded = !operatorExpanded },
                    modifier = Modifier.weight(0.6f)
                ) {
                    OutlinedTextField(
                        value = newRuleOperator,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Oper.") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = operatorExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = operatorExpanded,
                        onDismissRequest = { operatorExpanded = false }
                    ) {
                        listOf(">", "<", "=").forEach { op ->
                            DropdownMenuItem(
                                text = { Text(op) },
                                onClick = {
                                    viewModel.updateNewRuleOperator(op)
                                    operatorExpanded = false
                                }
                            )
                        }
                    }
                }

                // Threshold Value Input
                OutlinedTextField(
                    value = newRuleValue.toString(),
                    onValueChange = { it.toDoubleOrNull()?.let { v -> viewModel.updateNewRuleValue(v) } },
                    label = { Text("Limite") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.8f).testTag("rule_threshold_input")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Model ID Dropdown Selector
            var modelExpanded by remember { mutableStateOf(false) }
            val selectedModelName = savedConfigs.firstOrNull { it.id == newRuleModelId }?.name ?: "Modelo Base (Não Ajustado)"
            ExposedDropdownMenuBox(
                expanded = modelExpanded,
                onExpandedChange = { modelExpanded = !modelExpanded }
            ) {
                OutlinedTextField(
                    value = selectedModelName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Modelo de IA para Análise Preditiva") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = modelExpanded,
                    onDismissRequest = { modelExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Modelo Base (Sem LoRA)") },
                        onClick = {
                            viewModel.updateNewRuleModelId(0)
                            modelExpanded = false
                        }
                    )
                    savedConfigs.forEach { config ->
                        DropdownMenuItem(
                            text = { Text(config.name) },
                            onClick = {
                                viewModel.updateNewRuleModelId(config.id)
                                modelExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Selector
            var actionExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = actionExpanded,
                onExpandedChange = { actionExpanded = !actionExpanded }
            ) {
                OutlinedTextField(
                    value = newRuleAction,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Ação de Saída") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = actionExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = actionExpanded,
                    onDismissRequest = { actionExpanded = false }
                ) {
                    listOf("Alerta no Slack", "E-mail de Alerta", "Escalar Servidor", "Desligar Sistema").forEach { action ->
                        DropdownMenuItem(
                            text = { Text(action) },
                            onClick = {
                                viewModel.updateNewRuleAction(action)
                                actionExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.addAutomationRule()
                    onRuleAdded()
                },
                modifier = Modifier.fillMaxWidth().testTag("add_rule_confirm_button")
            ) {
                Text("Adicionar Regra Preditiva")
            }
        }
    }
}

@Composable
fun RuleItem(rule: AutomationRule, modelName: String, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(rule.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Deletar Regra", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }

            MetricRow("Gatilho Telemetria:", "SE ${rule.triggerField} ${rule.triggerOperator} ${rule.triggerValue}")
            MetricRow("Copiloto LLM local:", modelName)
            MetricRow("Ação Executada:", "ENTÃO ${rule.actionType}")
        }
    }
}
