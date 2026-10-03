package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AutomationViewModel
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.LoraViewModel
import com.example.ui.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Database and Repositories container
        AppContainer.init(this)
        val repository = AppContainer.repository ?: throw IllegalStateException("Repository not initialized")

        // Construct ViewModels using Factory
        val factory = ViewModelFactory(repository)
        val chatViewModel = ViewModelProvider(this, factory)[ChatViewModel::class.java]
        val loraViewModel = ViewModelProvider(this, factory)[LoraViewModel::class.java]
        val automationViewModel = ViewModelProvider(this, factory)[AutomationViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen(
                    chatViewModel = chatViewModel,
                    loraViewModel = loraViewModel,
                    automationViewModel = automationViewModel
                )
            }
        }
    }
}
