package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CbtScreen
import com.example.ui.screens.ProcessingScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.screens.SetupScreen
import com.example.ui.screens.ValidationScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.JeeCbtViewModel
import com.example.ui.viewmodel.ScreenState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A)
                ) {
                    JeeCbtApp()
                }
            }
        }
    }
}

@Composable
fun JeeCbtApp(viewModel: JeeCbtViewModel = viewModel()) {
    val currentScreenState by viewModel.screenState.collectAsState()

    AnimatedContent(
        targetState = currentScreenState,
        transitionSpec = {
            (fadeIn(animationSpec = tween(250)) + scaleIn(initialScale = 0.98f, animationSpec = tween(250)))
                .togetherWith(fadeOut(animationSpec = tween(180)))
        },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            ScreenState.SETUP -> SetupScreen(viewModel = viewModel)
            ScreenState.PROCESSING -> ProcessingScreen(viewModel = viewModel)
            ScreenState.VALIDATION -> ValidationScreen(viewModel = viewModel)
            ScreenState.CBT_EXAM -> CbtScreen(viewModel = viewModel)
            ScreenState.RESULT_ANALYSIS -> ResultScreen(viewModel = viewModel)
        }
    }
}
