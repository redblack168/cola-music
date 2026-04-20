package com.colamusic

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import com.colamusic.feature.auth.LoginScreen
import com.colamusic.feature.auth.SessionGateViewModel
import com.colamusic.feature.downloads.DownloadsScreen
import com.colamusic.feature.home.HomeScreen
import com.colamusic.feature.library.AlbumDetailScreen
import com.colamusic.feature.library.ArtistDetailScreen
import com.colamusic.feature.library.LibraryScreen
import com.colamusic.feature.library.PlaylistDetailScreen
import com.colamusic.feature.player.NowPlayingScreen
import com.colamusic.feature.search.SearchScreen
import com.colamusic.feature.settings.DiagnosticsScreen
import com.colamusic.feature.settings.SettingsScreen
import com.colamusic.ui.LanguagePickerScreen
import com.colamusic.ui.MiniPlayerBar
import com.colamusic.ui.ThemePickerScreen
import com.colamusic.update.UpdateDialog

object Routes {
    const val Login = "login"
    const val Home = "home"
    const val Library = "library"
    const val Search = "search"
    const val Settings = "settings"
    const val NowPlaying = "now_playing"
    const val Diagnostics = "diagnostics"
    const val Downloads = "downloads"
    const val Theme = "theme"
    const val Language = "language"
    const val Album = "album/{albumId}"
    const val Artist = "artist/{artistId}?name={name}"
    const val Playlist = "playlist/{playlistId}"
    fun album(id: String) = "album/$id"
    fun artist(id: String, name: String? = null): String =
        "artist/$id" + (name?.let { "?name=${java.net.URLEncoder.encode(it, Charsets.UTF_8)}" } ?: "")
    fun playlist(id: String) = "playlist/$id"
}

@Composable
fun ColaNavGraph(
    nav: NavHostController,
    openNowPlayingFlow: StateFlow<Int>? = null,
) {
    val gate: SessionGateViewModel = hiltViewModel()
    val isLoggedIn by gate.isLoggedIn.collectAsStateWithLifecycle()
    val start = if (isLoggedIn) Routes.Home else Routes.Login

    // External route-to-NowPlaying signal (from media notification / dynamic
    // island tap). drop(1) skips the initial value so we don't hop to Now
    // Playing on every cold start.
    if (openNowPlayingFlow != null) {
        LaunchedEffect(openNowPlayingFlow, isLoggedIn) {
            if (!isLoggedIn) return@LaunchedEffect
            openNowPlayingFlow.drop(1).collect {
                val current = nav.currentBackStackEntry?.destination?.route
                if (current != Routes.NowPlaying) nav.navigate(Routes.NowPlaying)
            }
        }
    }

    val checkUpdate = androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(false)
    }
    UpdateDialog(
        triggerCheck = checkUpdate.value,
        onTriggerHandled = { checkUpdate.value = false },
    )

    val showBottomBar = setOf(Routes.Home, Routes.Library, Routes.Search, Routes.Settings)
    Scaffold(
        bottomBar = {
            val entry by nav.currentBackStackEntryAsState()
            val route = entry?.destination?.route
            if (route in showBottomBar) {
                Column {
                    MiniPlayerBar(onTap = {
                        if (route != Routes.NowPlaying) nav.navigate(Routes.NowPlaying)
                    })
                    BottomBar(nav, route ?: Routes.Home)
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.fillMaxSize().padding(inner)
        ) {
            composable(Routes.Login) {
                LoginScreen(onLoggedIn = {
                    nav.navigate(Routes.Home) { popUpTo(Routes.Login) { inclusive = true } }
                })
            }
            composable(Routes.Home) {
                HomeScreen(
                    onAlbumClick = { album -> nav.navigate(Routes.album(album.id)) },
                    onNowPlayingClick = { nav.navigate(Routes.NowPlaying) }
                )
            }
            composable(Routes.Library) {
                LibraryScreen(
                    onAlbumClick = { album -> nav.navigate(Routes.album(album.id)) },
                    onArtistClick = { artist -> nav.navigate(Routes.artist(artist.id, artist.name)) },
                    onPlaylistClick = { pl -> nav.navigate(Routes.playlist(pl.id)) },
                    onNowPlayingClick = { nav.navigate(Routes.NowPlaying) }
                )
            }
            composable(Routes.Search) {
                SearchScreen(
                    onSongClick = {
                        val album = it.albumId
                        if (album != null) nav.navigate(Routes.album(album))
                        else nav.navigate(Routes.NowPlaying)
                    },
                    onAlbumClick = { nav.navigate(Routes.album(it.id)) },
                    onArtistClick = { nav.navigate(Routes.artist(it.id, it.name)) },
                )
            }
            composable(Routes.Settings) {
                SettingsScreen(
                    onLoggedOut = { nav.navigate(Routes.Login) { popUpTo(0) } },
                    onOpenDiagnostics = { nav.navigate(Routes.Diagnostics) },
                    onOpenDownloads = { nav.navigate(Routes.Downloads) },
                    onOpenTheme = { nav.navigate(Routes.Theme) },
                    onOpenLanguage = { nav.navigate(Routes.Language) },
                    onCheckForUpdate = { checkUpdate.value = true },
                )
            }
            composable(Routes.Diagnostics) {
                DiagnosticsScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.Downloads) {
                DownloadsScreen()
            }
            composable(Routes.Theme) {
                ThemePickerScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.Language) {
                LanguagePickerScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.NowPlaying) {
                NowPlayingScreen(
                    onBack = { nav.popBackStack() },
                    onOpenAlbum = { id -> nav.navigate(Routes.album(id)) },
                )
            }
            composable(
                route = Routes.Album,
                arguments = listOf(navArgument("albumId") { type = NavType.StringType }),
            ) {
                AlbumDetailScreen(
                    onBack = { nav.popBackStack() },
                    onOpenNowPlaying = { nav.navigate(Routes.NowPlaying) },
                )
            }
            composable(
                route = Routes.Artist,
                arguments = listOf(
                    navArgument("artistId") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) { entry ->
                ArtistDetailScreen(
                    artistName = entry.arguments?.getString("name"),
                    onBack = { nav.popBackStack() },
                    onAlbumClick = { nav.navigate(Routes.album(it.id)) },
                )
            }
            composable(
                route = Routes.Playlist,
                arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
            ) {
                PlaylistDetailScreen(
                    onBack = { nav.popBackStack() },
                    onOpenNowPlaying = { nav.navigate(Routes.NowPlaying) },
                )
            }
        }
    }
}

@Composable
private fun BottomBar(nav: NavHostController, currentRoute: String) {
    val items = listOf(
        Triple(Routes.Home, Icons.Default.Home, R.string.nav_home),
        Triple(Routes.Library, Icons.Default.LibraryMusic, R.string.nav_library),
        Triple(Routes.Search, Icons.Default.Search, R.string.nav_search),
        Triple(Routes.Settings, Icons.Default.Settings, R.string.nav_settings),
    )
    NavigationBar {
        items.forEach { (route, icon, label) ->
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = {
                    nav.navigate(route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(icon, contentDescription = null) },
                label = { Text(stringResource(label)) }
            )
        }
    }
}
