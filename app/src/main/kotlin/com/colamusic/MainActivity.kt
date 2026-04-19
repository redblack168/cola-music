package com.colamusic

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.colamusic.ui.ProvideAdaptiveLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestNotificationPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not — foreground playback still works, just no visible notification */ }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        maybeRequestNotificationPermission()
        setContent {
            val sizeClass = calculateWindowSizeClass(this)
            ColaTheme {
                ProvideAdaptiveLayout(widthSizeClass = sizeClass.widthSizeClass) {
                    ColaAppContent()
                }
            }
        }
    }

    /**
     * Android 13+ requires POST_NOTIFICATIONS to be granted before the
     * MediaLibraryService can show its foreground notification. Missing that
     * permission is a common reason the OS kills a media foreground service
     * with ForegroundServiceDidNotStartInTimeException.
     */
    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) requestNotificationPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
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
