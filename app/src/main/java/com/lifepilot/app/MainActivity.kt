package com.lifepilot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.lifepilot.app.navigation.LifePilotNavHost
import com.lifepilot.designsystem.theme.LifePilotTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LifePilotApp()
        }
    }
}

@Composable
private fun LifePilotApp() {
    LifePilotTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            LifePilotNavHost()
        }
    }
}
