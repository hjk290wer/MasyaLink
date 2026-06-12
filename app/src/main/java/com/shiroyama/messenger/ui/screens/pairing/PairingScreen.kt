package com.shiroyama.messenger.ui.screens.pairing

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiroyama.messenger.core.storage.LocalSessionStorage
import com.shiroyama.messenger.ui.theme.ColorTokens
import com.shiroyama.messenger.ui.theme.ShapeTokens
import com.shiroyama.messenger.ui.theme.SpacingTokens
import com.shiroyama.messenger.ui.theme.TypographyTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScreen(
    sessionStorage: LocalSessionStorage,
    onNavigateToChat: () -> Unit,
    viewModel: PairingViewModel = viewModel()
) {
    LaunchedEffect(Unit) { sessionStorage.loadChatStyleIntoMemory() }

    val state by viewModel.uiState.collectAsState()
    val profileA = when (val current = state) {
        is PairingUiState.Idle -> current.profileA
        is PairingUiState.Error -> current.profileA
        else -> ProfileButtonState("Profile A")
    }
    val profileB = when (val current = state) {
        is PairingUiState.Idle -> current.profileB
        is PairingUiState.Error -> current.profileB
        else -> ProfileButtonState("Profile B")
    }
    val namesLoading = (state as? PairingUiState.Idle)?.isNamesLoading == true

    LaunchedEffect(state) {
        if (state is PairingUiState.Success) onNavigateToChat()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MasyaLink", style = TypographyTokens.TitleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorTokens.Primary, titleContentColor = ColorTokens.TextOnPrimary)
            )
        },
        containerColor = ColorTokens.Background
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(SpacingTokens.Medium),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = SpacingTokens.Small),
                colors = CardDefaults.cardColors(containerColor = ColorTokens.Surface),
                shape = ShapeTokens.Card,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(SpacingTokens.Medium), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Choose your profile", style = TypographyTokens.TitleMedium, color = ColorTokens.TextPrimary)
                    Spacer(modifier = Modifier.height(SpacingTokens.Small))
                    Text(
                        text = "Choose your side of the private chat. Names and avatars are loaded from the server.",
                        style = TypographyTokens.BodyMedium,
                        color = ColorTokens.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(SpacingTokens.Large))

                    if (state is PairingUiState.Error) {
                        Text((state as PairingUiState.Error).message, color = ColorTokens.Error, style = TypographyTokens.LabelSmall, modifier = Modifier.padding(bottom = SpacingTokens.Small))
                    }
                    if (namesLoading) {
                        Text("Loading profiles...", style = TypographyTokens.LabelSmall, color = ColorTokens.TextSecondary, modifier = Modifier.padding(bottom = SpacingTokens.Small))
                    }
                    if (state is PairingUiState.Loading) {
                        CircularProgressIndicator(color = ColorTokens.Primary)
                    } else {
                        ProfileButton(profile = profileA, subtitle = "Profile A", onClick = { viewModel.selectProfile("A", sessionStorage, onNavigateToChat) })
                        Spacer(modifier = Modifier.height(SpacingTokens.Small))
                        ProfileButton(profile = profileB, subtitle = "Profile B", onClick = { viewModel.selectProfile("B", sessionStorage, onNavigateToChat) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileButton(profile: ProfileButtonState, subtitle: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = ShapeTokens.Button,
        colors = ButtonDefaults.buttonColors(containerColor = ColorTokens.Primary)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            ProfileAvatar(profile = profile)
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.Start) {
                Text(profile.name, style = TypographyTokens.BodyLarge, color = ColorTokens.TextOnPrimary)
                Text(subtitle, style = TypographyTokens.LabelSmall, color = ColorTokens.TextOnPrimary.copy(alpha = 0.75f))
            }
        }
    }
}

@Composable
private fun ProfileAvatar(profile: ProfileButtonState) {
    val bitmap = profile.avatarBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    Box(
        modifier = Modifier.size(48.dp).clip(CircleShape).background(ColorTokens.TextOnPrimary.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap.asImageBitmap(), contentDescription = profile.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(profile.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = ColorTokens.TextOnPrimary, style = TypographyTokens.TitleMedium)
        }
    }
}
