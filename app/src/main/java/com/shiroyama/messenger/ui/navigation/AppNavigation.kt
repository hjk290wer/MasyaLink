package com.shiroyama.messenger.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shiroyama.messenger.core.storage.LocalSessionStorage
import com.shiroyama.messenger.ui.screens.chat.ChatScreen
import com.shiroyama.messenger.ui.screens.pairing.PairingScreen
import com.shiroyama.messenger.ui.screens.settings.SettingsScreen
import com.shiroyama.messenger.ui.screens.splash.SplashScreen

object Destinations {
    const val Splash = "splash"
    const val Pairing = "pairing"
    const val Chat = "chat"
    const val Settings = "settings"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sessionStorage = LocalSessionStorage(context)

    fun navigateToPairingAndClearStack() {
        navController.navigate(Destinations.Pairing) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(navController = navController, startDestination = Destinations.Splash) {
        composable(Destinations.Splash) {
            SplashScreen(
                sessionStorage = sessionStorage,
                onNavigateToPairing = { navigateToPairingAndClearStack() },
                onNavigateToChat = {
                    navController.navigate(Destinations.Chat) {
                        popUpTo(Destinations.Splash) { inclusive = true }
                    }
                }
            )
        }

        composable(Destinations.Pairing) {
            PairingScreen(
                sessionStorage = sessionStorage,
                onNavigateToChat = {
                    navController.navigate(Destinations.Chat) {
                        popUpTo(Destinations.Pairing) { inclusive = true }
                    }
                }
            )
        }

        composable(Destinations.Chat) {
            ChatScreen(
                sessionStorage = sessionStorage,
                onNavigateToSettings = { navController.navigate(Destinations.Settings) },
                onSessionInvalid = {
                    sessionStorage.clearSession()
                    navigateToPairingAndClearStack()
                }
            )
        }

        composable(Destinations.Settings) {
            SettingsScreen(
                sessionStorage = sessionStorage,
                onNavigateToPairing = { navigateToPairingAndClearStack() },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
