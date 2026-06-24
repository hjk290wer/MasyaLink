package com.shiroyama.messenger.ui.screens.settings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiroyama.messenger.core.storage.LocalSessionStorage
import com.shiroyama.messenger.ui.components.AvatarView
import com.shiroyama.messenger.ui.theme.ChatStylePreset
import com.shiroyama.messenger.ui.theme.ChatStylePresets
import com.shiroyama.messenger.ui.theme.ChatStyleStore
import com.shiroyama.messenger.ui.theme.ColorTokens
import com.shiroyama.messenger.ui.theme.ShapeTokens
import com.shiroyama.messenger.ui.theme.SpacingTokens
import com.shiroyama.messenger.ui.theme.TypographyTokens

private data class PickedImage(val fileName: String, val mimeType: String, val bytes: ByteArray)

@Composable
fun SettingsScreen(
    sessionStorage: LocalSessionStorage,
    onNavigateToPairing: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    var session by remember { mutableStateOf(sessionStorage.getSession()) }
    val state by viewModel.state.collectAsState()
    var newName by remember(session?.displayName) { mutableStateOf(session?.displayName.orEmpty()) }
    var showClearChatConfirm by remember { mutableStateOf(false) }
    var selectedStyleId by remember { mutableStateOf(sessionStorage.getChatStyleId()) }

    LaunchedEffect(Unit) { sessionStorage.loadChatStyleIntoMemory() }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val picked = uri?.let { readPickedImage(context, it) }
        if (picked == null) {
            if (uri != null) Toast.makeText(context, "Could not read image", Toast.LENGTH_SHORT).show()
        } else if (picked.bytes.size > 5L * 1024L * 1024L) {
            Toast.makeText(context, "Avatar is larger than 5 MB", Toast.LENGTH_LONG).show()
        } else {
            viewModel.updateAvatar(session, picked.fileName, picked.mimeType, picked.bytes) {
                Toast.makeText(context, "Avatar updated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(state) {
        if (state is SettingsActionState.Error) Toast.makeText(context, (state as SettingsActionState.Error).message, Toast.LENGTH_LONG).show()
    }

    if (showClearChatConfirm) {
        AlertDialog(
            onDismissRequest = { showClearChatConfirm = false },
            containerColor = ColorTokens.SurfaceElevated,
            title = { Text("Clear entire chat?", color = ColorTokens.TextPrimary) },
            text = { Text("This deletes all messages in the private room for both profiles. Profiles, names and avatars remain.", color = ColorTokens.TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showClearChatConfirm = false; viewModel.clearChat(session) { Toast.makeText(context, "Chat cleared", Toast.LENGTH_SHORT).show() } }) {
                    Text("Clear", color = ColorTokens.Error)
                }
            },
            dismissButton = { TextButton(onClick = { showClearChatConfirm = false }) { Text("Cancel", color = ColorTokens.TextSecondary) } }
        )
    }

    Scaffold(
        topBar = { SettingsTopBar(onBack = onNavigateBack) },
        containerColor = ColorTokens.Background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(ColorTokens.GradientStart, ColorTokens.Background, ColorTokens.BackgroundAlt)))
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(SpacingTokens.Medium),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SettingsSection(title = "Profile") {
                session?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarView(name = it.displayName, avatarBytes = null, size = 70.dp)
                        Spacer(Modifier.size(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(it.displayName, style = TypographyTokens.TitleMedium, color = ColorTokens.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Fixed profile ${it.profileKey}", style = TypographyTokens.LabelSmall, color = ColorTokens.TextSecondary)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Display name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = ShapeTokens.Button,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorTokens.Primary,
                            unfocusedBorderColor = ColorTokens.BorderLight,
                            focusedLabelColor = ColorTokens.Primary,
                            focusedTextColor = ColorTokens.TextPrimary,
                            unfocusedTextColor = ColorTokens.TextPrimary
                        )
                    )
                    Spacer(Modifier.height(10.dp))
                    ActionButton(Icons.Default.Edit, "Save profile name", enabled = state !is SettingsActionState.Loading) {
                        viewModel.updateName(session, newName) {
                            session = session?.copy(displayName = newName.trim())
                            session?.let { saved -> sessionStorage.saveSession(saved) }
                            Toast.makeText(context, "Name updated", Toast.LENGTH_SHORT).show()
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    ActionButton(Icons.Default.Image, "Change avatar", enabled = state !is SettingsActionState.Loading) {
                        avatarPicker.launch(arrayOf("image/*"))
                    }
                } ?: Text("No active profile found.", style = TypographyTokens.BodyMedium, color = ColorTokens.TextSecondary)
            }

            SettingsSection(title = "Appearance") {
                ChatStylePresets.All.forEach { preset ->
                    ThemePresetRow(
                        preset = preset,
                        selected = selectedStyleId == preset.id,
                        onClick = {
                            selectedStyleId = preset.id
                            sessionStorage.saveChatStyle(preset.id)
                            ChatStyleStore.current = preset
                        }
                    )
                }
            }

            SettingsSection(title = "Chat") {
                DestructiveButton(Icons.Default.DeleteSweep, "Clear entire chat") { showClearChatConfirm = true }
            }

            SettingsSection(title = "Session") {
                ActionButton(Icons.Default.Logout, "Log out of this profile") {
                    viewModel.logout(session) { sessionStorage.clearSession(); onNavigateToPairing() }
                }
            }

            SettingsSection(title = "App") {
                DetailRow("Version", "1.6.0")
                Divider(color = ColorTokens.BorderLight, modifier = Modifier.padding(vertical = 8.dp))
                DetailRow("Package", "com.shiroyama.messenger")
            }
        }
    }
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Surface(color = ColorTokens.Surface.copy(alpha = if (ColorTokens.IsDark) 0.96f else 0.92f), shadowElevation = if (ColorTokens.IsDark) 0.dp else 8.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ColorTokens.Primary) }
            Text("Settings", style = TypographyTokens.TitleMedium, color = ColorTokens.TextPrimary)
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable Column.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ColorTokens.SurfaceElevated.copy(alpha = 0.96f)),
        shape = ShapeTokens.Card,
        border = BorderStroke(1.dp, ColorTokens.BorderLight.copy(alpha = 0.72f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (ColorTokens.IsDark) 0.dp else 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Text(title, style = TypographyTokens.TitleMedium, color = ColorTokens.Primary)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ThemePresetRow(preset: ChatStylePreset, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(if (selected) ColorTokens.AccentSoft else ColorTokens.Surface, ShapeTokens.Button)
            .border(1.dp, if (selected) ColorTokens.Primary else ColorTokens.BorderLight, ShapeTokens.Button)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(Brush.linearGradient(listOf(preset.primaryLight, preset.primary, preset.primaryDark)), CircleShape)
        )
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(preset.title, style = TypographyTokens.BodyLarge, color = ColorTokens.TextPrimary)
            Text(preset.subtitle, style = TypographyTokens.LabelSmall, color = ColorTokens.TextSecondary)
        }
        if (selected) Text("Selected", style = TypographyTokens.LabelSmall, color = ColorTokens.Primary)
    }
}

@Composable
private fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = ShapeTokens.Button,
        colors = ButtonDefaults.buttonColors(containerColor = ColorTokens.Primary, contentColor = ColorTokens.TextOnPrimary)
    ) { Icon(icon, null); Spacer(Modifier.size(8.dp)); Text(text, style = TypographyTokens.BodyLarge) }
}

@Composable
private fun DestructiveButton(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = ShapeTokens.Button,
        colors = ButtonDefaults.buttonColors(containerColor = ColorTokens.Error, contentColor = ColorTokens.TextOnPrimary)
    ) { Icon(icon, null); Spacer(Modifier.size(8.dp)); Text(text, style = TypographyTokens.BodyLarge) }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = TypographyTokens.LabelSmall, color = ColorTokens.TextSecondary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, style = TypographyTokens.BodyMedium, color = ColorTokens.TextPrimary)
    }
}

private fun readPickedImage(context: Context, uri: Uri): PickedImage? {
    val resolver = context.contentResolver
    val mime = resolver.getType(uri) ?: "image/jpeg"
    if (!mime.startsWith("image/")) return null
    var name = "avatar.jpg"
    resolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
    }
    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    return PickedImage(name, mime, bytes)
}