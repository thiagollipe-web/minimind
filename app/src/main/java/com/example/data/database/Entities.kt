package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: String,
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "lora_configs")
data class LoraConfig(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val baseModel: String,
    val targetHardware: String,
    val rank: Int,
    val alpha: Int,
    val dropout: Float,
    val targetModules: String, // Comma-separated (e.g., "q_proj,v_proj")
    val quantization: String,   // "4-bit (QLoRA)", "8-bit", "Nenhum (FP16)"
    val learningRate: Double,
    val epochs: Int,
    val batchSize: Int,
    val isTrained: Boolean = false,
    val trainingLossHistory: String = "", // Comma-separated floats for charts
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "automation_rules")
data class AutomationRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val triggerField: String,     // e.g. "Temperatura", "Uso de CPU", "Churn Rate"
    val triggerOperator: String,  // ">", "<", "="
    val triggerValue: Double,
    val modelId: Int,             // References LoraConfig id
    val actionType: String,       // "Alerta no Slack", "E-mail de Alerta", "Escalar Servidor"
    val isActive: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
