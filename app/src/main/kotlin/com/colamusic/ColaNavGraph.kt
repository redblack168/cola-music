package com.colamusic

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.colamusic.feature.auth.LoginScreen
import com.colamusic.feature.auth.SessionGateViewModel
import com.colamusic.feature.home.HomeScreen
import com.colamusic.feature.library.LibraryScreen
import com.colamusic.feature.player.NowPlayingScreen
import com.colamusic.feature.search.SearchScreen
import com.colamusic.feature.settings.SettingsScreen

object Routes {
    const val Login = "login"
    const val Home = "home"
    const val Library = "library"
    const val Search = "search"
    const val Settings = "settings"
    const val NowPlaying = "now_playing"
}

@Composable
fun ColaNavGraph(nav: NavHostController) {
    val gate: SessionGateViewModel = hiltViewModel()
    val isLoggedIn by gate.isLoggedIn.collectAsStateWithLifecycle()
    val start = if (isLoggedIn) Routes.Home else Routes.Login

    Scaffold(
        bottomBar = {
            val entry by nav.currentBackStackEntryAsState()
            val route = entry?.destination?.route
            if (route != null && route != Routes.Login) BottomBar(nav, route)
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
                    onAlbumClick = {},
                    onNowPlayingClick = { nav.navigate(Routes.NowPlaying) }
                )
            }
            composable(Routes.Library) {
                LibraryScreen(
                    onAlbumClick = {},
                    onNowPlayingClick = { nav.navigate(Routes.NowPlaying) }
                )
            }
            composable(Routes.Search) {
                SearchScreen(onResultClick = { nav.navigate(Routes.NowPlaying) })
            }
            composable(Routes.Settings) {
                SettingsScreen(onLoggedOut = { nav.navigate(Routes.Login) { popUpTo(0) } })
            }
            composable(Routes.NowPlaying) {
                NowPlayingScreen(onBack = { nav.popBackStack() })
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
