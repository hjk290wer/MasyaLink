package com.shiroyama.messenger.ui.network

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shiroyama.messenger.ui.theme.ColorTokens
import com.shiroyama.messenger.ui.theme.ShapeTokens
import com.shiroyama.messenger.ui.theme.TypographyTokens

@Composable
fun ConnectionStatusBanner(state: ConnectionState, modifier: Modifier = Modifier) {
    val message = state.userMessage()
    AnimatedVisibility(visible = message != null, modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
            Surface(
                shape = ShapeTokens.Button,
                color = when (state) {
                    ConnectionState.ServerUnavailable, ConnectionState.Offline, is ConnectionState.Error -> ColorTokens.Error.copy(alpha = 0.14f)
                    else -> ColorTokens.AccentSoft.copy(alpha = 0.92f)
                },
                tonalElevation = 2.dp,
                shadowElevation = if (ColorTokens.IsDark) 0.dp else 2.dp
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector = if (state == ConnectionState.Offline || state == ConnectionState.ServerUnavailable || state is ConnectionState.Error) Icons.Default.CloudOff else Icons.Default.Sync,
                        contentDescription = null,
                        tint = if (state == ConnectionState.Offline || state == ConnectionState.ServerUnavailable || state is ConnectionState.Error) ColorTokens.Error else ColorTokens.Primary,
                        modifier = Modifier.padding(end = 7.dp)
                    )
                    Text(
                        text = message.orEmpty(),
                        style = TypographyTokens.LabelSmall,
                        color = if (state == ConnectionState.Offline || state == ConnectionState.ServerUnavailable || state is ConnectionState.Error) ColorTokens.Error else ColorTokens.Primary
                    )
                }
            }
        }
    }
}
