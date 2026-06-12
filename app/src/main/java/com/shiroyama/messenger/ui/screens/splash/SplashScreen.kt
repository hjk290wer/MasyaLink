package com.shiroyama.messenger.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shiroyama.messenger.core.storage.LocalSessionStorage
import com.shiroyama.messenger.ui.theme.ColorTokens
import com.shiroyama.messenger.ui.theme.TypographyTokens
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    sessionStorage: LocalSessionStorage,
    onNavigateToPairing: () -> Unit,
    onNavigateToChat: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(800) // Aesthetic delay
        val session = sessionStorage.getSession()
        if (session != null) {
            onNavigateToChat()
        } else {
            onNavigateToPairing()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorTokens.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "MasyaLink",
                style = TypographyTokens.TitleLarge.copy(color = ColorTokens.Primary)
            )
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(
                color = ColorTokens.Primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
