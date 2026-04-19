package com.colamusic

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.colamusic.ui.ColaPalette
import com.colamusic.ui.ColaTheme
import com.colamusic.ui.ProvideAdaptiveLayout
import com.colamusic.ui.ThemePreferences
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themePrefs: ThemePreferences

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
            val palette by themePrefs.palette.collectAsState(initial = ColaPalette.ColaRed)
            ColaTheme(palette = palette) {
                ProvideAdaptiveLayout(widthSizeClass = sizeClass.widthSizeClass) {
                    Surface(
                        modifier = Modifier,
                        color = MaterialTheme.colorScheme.background,
                        content = { ColaAppContent() },
                    )
                }
            }
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) requestNotificationPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@Composable
private fun ColaAppContent() {
    val nav = rememberNavController()
    ColaNavGraph(nav)
}
