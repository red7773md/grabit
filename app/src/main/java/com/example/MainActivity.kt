package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.util.Consumer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.GrabItMainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GrabItViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GrabItViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            // Handle initial Share Intent if user launched GrabIt via Android Share Sheet
            LaunchedEffect(Unit) {
                handleIncomingShareIntent(intent)
            }

            // Handle subsequent Share Intents when Activity is already running
            DisposableEffect(Unit) {
                val listener = Consumer<Intent> { newIntent ->
                    handleIncomingShareIntent(newIntent)
                }
                addOnNewIntentListener(listener)
                onDispose {
                    removeOnNewIntentListener(listener)
                }
            }

            MyApplicationTheme(
                darkTheme = uiState.isDarkTheme,
                dynamicColor = false
            ) {
                GrabItMainScreen(viewModel = viewModel)
            }
        }
    }

    private fun handleIncomingShareIntent(incomingIntent: Intent?) {
        if (incomingIntent?.action == Intent.ACTION_SEND && incomingIntent.type == "text/plain") {
            val sharedText = incomingIntent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                viewModel.openShareConfirmationWithUrl(sharedText.trim())
                // Clear extra so configuration changes don't re-trigger the same share intent
                incomingIntent.removeExtra(Intent.EXTRA_TEXT)
            }
        }
    }
}
