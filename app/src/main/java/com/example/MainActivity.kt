package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainAppScreen
import com.example.ui.StepSightViewModel
import com.example.ui.theme.StepSightTheme
import com.example.util.configureAppCheck

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory App Check configuration with intent extra before setContent or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            StepSightTheme {
                val viewModel: StepSightViewModel = viewModel()
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}
