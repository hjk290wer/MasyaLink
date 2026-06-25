package com.shiroyama.messenger.ui.screens.pairing

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiroyama.messenger.core.storage.LocalSessionStorage
import com.shiroyama.messenger.ui.components.AvatarView
import com.shiroyama.messenger.ui.theme.ColorTokens
import com.shiroyama.messenger.ui.theme.ShapeTokens
import com.shiroyama.messenger.ui.theme.SpacingTokens
import com.shiroyama.messenger.ui.theme.TypographyTokens

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
    val isSelecting = state is PairingUiState.Loading

    LaunchedEffect(state) {
        if (state is PairingUiState.Success) onNavigateToChat()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ColorTokens.GradientStart, ColorTokens.Background, ColorTokens.BackgroundAlt)))
            .padding(horizontal = 22.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = ColorTokens.Primary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, ColorTokens.BorderLight)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = ColorTokens.Primary, modifier = Modifier.size(30.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("MasyaLink", style = TypographyTokens.TitleLarge, color = ColorTokens.TextPrimary)
            Text(
                text = "Private chat for two profiles",
                style = TypographyTokens.BodyMedium,
                color = ColorTokens.TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))

            if (state is PairingUiState.Error) {
                RetryCard(
                    message = (state as PairingUiState.Error).message,
                    onRetry = { viewModel.loadProfileNames() }
                )
                Spacer(Modifier.height(14.dp))
            }

            if (namesLoading) {
                LoadingProfilesCard()
            } else {
                ProfileCard(
                    profile = profileA,
                    profileKey = "A",
                    enabled = !isSelecting,
                    onClick = { viewModel.selectProfile("A", sessionStorage, onNavigateToChat) }
                )
                Spacer(Modifier.height(14.dp))
                ProfileCard(
                    profile = profileB,
                    profileKey = "B",
                    enabled = !isSelecting,
                    onClick = { viewModel.selectProfile("B", sessionStorage, onNavigateToChat) }
                )
            }

            if (isSelecting) {
                Spacer(Modifier.height(22.dp))
                CircularProgressIndicator(color = ColorTokens.Primary, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
private fun ProfileCard(
    profile: ProfileButtonState,
    profileKey: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.975f else 1f, label = "profilePress")
    val borderColor by animateColorAsState(if (pressed) ColorTokens.Primary else ColorTokens.BorderLight, label = "profileBorder")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick),
        shape = ShapeTokens.Card,
        colors = CardDefaults.cardColors(containerColor = ColorTokens.SurfaceElevated),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (ColorTokens.IsDark) 0.dp else 7.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(ColorTokens.SurfaceElevated, ColorTokens.AccentSoft.copy(alpha = 0.56f))))
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarView(name = profile.name, avatarBytes = profile.avatarBytes, size = 68.dp)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = TypographyTokens.TitleMedium,
                    color = ColorTokens.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text("Profile $profileKey · tap to open chat", style = TypographyTokens.LabelSmall, color = ColorTokens.TextSecondary)
            }
            Surface(shape = CircleShape, color = ColorTokens.Primary.copy(alpha = 0.12f)) {
                Text(profileKey, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = TypographyTokens.LabelSmall, color = ColorTokens.Primary)
            }
        }
    }
}

@Composable
private fun LoadingProfilesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ShapeTokens.Card,
        colors = CardDefaults.cardColors(containerColor = ColorTokens.SurfaceElevated),
        border = BorderStroke(1.dp, ColorTokens.BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(color = ColorTokens.Primary, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Loading profiles", style = TypographyTokens.TitleMedium, color = ColorTokens.TextPrimary)
                Text("Names and avatars are coming from Supabase", style = TypographyTokens.LabelSmall, color = ColorTokens.TextSecondary)
            }
        }
    }
}

@Composable
private fun RetryCard(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ShapeTokens.CardSmall,
        colors = CardDefaults.cardColors(containerColor = ColorTokens.Surface),
        border = BorderStroke(1.dp, ColorTokens.Error.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Could not refresh profiles", style = TypographyTokens.BodyLarge, color = ColorTokens.TextPrimary)
                Text(message, style = TypographyTokens.LabelSmall, color = ColorTokens.TextSecondary)
            }
            Button(
                onClick = onRetry,
                shape = ShapeTokens.Button,
                colors = ButtonDefaults.buttonColors(containerColor = ColorTokens.Primary, contentColor = ColorTokens.TextOnPrimary)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
            }
        }
    }
}