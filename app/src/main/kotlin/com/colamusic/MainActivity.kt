package com.colamusic

import android.Manifest
import android.content.Intent
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
import com.colamusic.core.player.MusicService
import com.colamusic.core.player.PlayerController
import com.colamusic.ui.ColaPalette
import com.colamusic.ui.ColaTheme
import com.colamusic.ui.ProvideAdaptiveLayout
import com.colamusic.ui.ThemePreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themePrefs: ThemePreferences
    @Inject lateinit var playerController: PlayerController

    private val requestNotificationPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not — foreground playback still works, just no visible notification */ }

    /**
     * Ticks every time the activity receives an Intent carrying
     * [MusicService.EXTRA_OPEN_NOW_PLAYING]. `ColaAppContent` observes it and
     * pushes the Now Playing route. A monotonic counter is used (instead of a
     * Boolean) so consecutive taps on the media notification still fire even
     * when the user has since navigated away.
     */
    private val openNowPlayingTick = MutableStateFlow(0)
    private val openNowPlayingFlow: StateFlow<Int> = openNowPlayingTick.asStateFlow()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        maybeRequestNotificationPermission()
        consumeOpenNowPlayingExtra(intent)
        // Eagerly bind to MusicService's MediaSession so that if the
        // process was killed while playback continued (foreground service
        // notification kept alive), reopening the app — including via the
        // "tap notification" dynamic-island flow — finds an already-wired
        // controller and the UI immediately shows the live song / state.
        // PlayerController.connect() is idempotent.
        playerController.connect()
        setContent {
            val sizeClass = calculateWindowSizeClass(this)
            val palette by themePrefs.palette.collectAsState(initial = ColaPalette.ColaRed)
            ColaTheme(palette = palette) {
                ProvideAdaptiveLayout(widthSizeClass = sizeClass.widthSizeClass) {
                    Surface(
                        modifier = Modifier,
                        color = MaterialTheme.colorScheme.background,
                        content = { ColaAppContent(openNowPlayingFlow) },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeOpenNowPlayingExtra(intent)
    }

    private fun consumeOpenNowPlayingExtra(intent: Intent?) {
        if (intent?.getBooleanExtra(MusicService.EXTRA_OPEN_NOW_PLAYING, false) == true) {
            intent.removeExtra(MusicService.EXTRA_OPEN_NOW_PLAYING)
            openNowPlayingTick.value = openNowPlayingTick.value + 1
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
private fun ColaAppContent(openNowPlayingFlow: StateFlow<Int>) {
    val nav = rememberNavController()
    ColaNavGraph(nav, openNowPlayingFlow)
}
