package com.colamusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { ColaTheme { ColaAppContent() } }
    }
}

@Composable
private fun ColaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFE23744),
            secondary = Color(0xFFFFB86C),
            background = Color(0xFF0B0B0F),
            surface = Color(0xFF14141A)
        )
    ) {
        Surface(
            modifier = Modifier,
            color = MaterialTheme.colorScheme.background,
            content = content
        )
    }
}

@Composable
private fun ColaAppContent() {
    val nav = rememberNavController()
    ColaNavGraph(nav)
}

