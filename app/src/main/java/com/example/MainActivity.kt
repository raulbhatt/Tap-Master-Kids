package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.models.Screen
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: GameViewModel = viewModel()
                val currentScreen by viewModel.currentScreen.collectAsState()
                val selectedTheme by viewModel.selectedTheme.collectAsState()
                val isFeverActive by viewModel.isFeverActive.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ThemedBackground(
                        themeId = selectedTheme,
                        isFeverActive = isFeverActive,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            Screen.MENU -> {
                                MenuScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Screen.PLAYING -> {
                                GameScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Screen.GAME_OVER -> {
                                GameOverScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Screen.TROPHY -> {
                                TrophyScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
