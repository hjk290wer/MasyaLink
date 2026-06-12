package com.shiroyama.messenger.ui.screens.settings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiroyama.messenger.core.storage.LocalSessionStorage
import com.shiroyama.messenger.ui.theme.ColorTokens
import com.shiroyama.messenger.ui.theme.ChatStylePresets
import com.shiroyama.messenger.ui.theme.ChatStyleStore
import com.shiroyama.messenger.ui.theme.ShapeTokens
import com.shiroyama.messenger.ui.theme.SpacingTokens
import com.shiroyama.messenger.ui.theme.TypographyTokens

private data class PickedImage(val fileName: String, val mimeType: String, val bytes: ByteArray)

@OptIn(ExperimentalMaterial3Api::class)
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
            title = { Text("Clear entire chat?") },
            text = { Text("This will delete all messages in the private room for both profiles. Profiles and names will remain.") },
            confirmButton = {
                TextButton(onClick = { showClearChatConfirm = false; viewModel.clearChat(session) { Toast.makeText(context, "Chat cleared", Toast.LENGTH_SHORT).show() } }) {
                    Text("Clear", color = ColorTokens.Error)
                }
            },
            dismissButton = { TextButton(onClick = { showClearChatConfirm = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = TypographyTokens.TitleMedium) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ColorTokens.TextOnPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorTokens.Primary, titleContentColor = ColorTokens.TextOnPrimary)
            )
        },
        containerColor = ColorTokens.Background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(SpacingTokens.Medium), contentAlignment = Alignment.TopCenter) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = ColorTokens.Surface), shape = ShapeTokens.Card, elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(SpacingTokens.Medium)) {
                    Text("Fixed Profile", style = TypographyTokens.TitleMedium, color = ColorTokens.Primary, modifier = Modifier.padding(bottom = SpacingTokens.Medium))
                    session?.let {
                        DetailRow("Profile", "Profile ${it.profileKey}")
                        Divider(color = ColorTokens.BorderLight, thickness = 0.5.dp, modifier = Modifier.padding(vertical = SpacingTokens.Small))
                        DetailRow("Current Name", it.displayName)
                        Divider(color = ColorTokens.BorderLight, thickness = 0.5.dp, modifier = Modifier.padding(vertical = SpacingTokens.Small))
                        DetailRow("Device ID", it.deviceId)
                    } ?: Text("No active profile found.", style = TypographyTokens.BodyMedium, color = ColorTokens.TextSecondary)

                    Spacer(modifier = Modifier.height(SpacingTokens.Large))

                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Profile display name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ColorTokens.Primary, focusedLabelColor = ColorTokens.Primary)
                    )

                    Spacer(modifier = Modifier.height(SpacingTokens.Small))

                    Button(
                        onClick = { viewModel.updateName(session, newName) { session = session?.copy(displayName = newName.trim()); session?.let { sessionStorage.saveSession(it) }; Toast.makeText(context, "Name updated", Toast.LENGTH_SHORT).show() } },
                        enabled = state !is SettingsActionState.Loading,
                        modifier = Modifier.fillMaxWidth(), shape = ShapeTokens.Button,
                        colors = ButtonDefaults.buttonColors(containerColor = ColorTokens.Primary, contentColor = ColorTokens.TextOnPrimary)
                    ) { Icon(Icons.Default.Edit, null); Spacer(Modifier.width(8.dp)); Text("Save profile name", style = TypographyTokens.BodyLarge) }

                    Spacer(modifier = Modifier.height(SpacingTokens.Small))

                    Button(
                        onClick = { avatarPicker.launch(arrayOf("image/*")) },
                        enabled = state !is SettingsActionState.Loading,
                        modifier = Modifier.fillMaxWidth(), shape = ShapeTokens.Button,
                        colors = ButtonDefaults.buttonColors(containerColor = ColorTokens.Primary, contentColor = ColorTokens.TextOnPrimary)
                    ) { Icon(Icons.Default.Image, null); Spacer(Modifier.width(8.dp)); Text("Set profile avatar", style = TypographyTokens.BodyLarge) }

                    Spacer(modifier = Modifier.height(SpacingTokens.Medium))

                    Text("Chat style", style = TypographyTokens.TitleMedium, color = ColorTokens.Primary)
                    Spacer(modifier = Modifier.height(SpacingTokens.Small))
                    ChatStylePresets.All.forEach { preset ->
                        val selected = selectedStyleId == preset.id
                        OutlinedButton(
                            onClick = {
                                selectedStyleId = preset.id
                                sessionStorage.saveChatStyle(preset.id)
                                ChatStyleStore.current = preset
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            shape = ShapeTokens.Button,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) ColorTokens.PrimaryLight else ColorTokens.Surface,
                                contentColor = if (selected) ColorTokens.PrimaryDark else ColorTokens.TextPrimary
                            )
                        ) {
                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                                Text(preset.title, style = TypographyTokens.BodyLarge)
                                Text(preset.subtitle, style = TypographyTokens.LabelSmall, color = ColorTokens.TextSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(SpacingTokens.Medium))

                    Button(
                        onClick = { viewModel.logout(session) { sessionStorage.clearSession(); onNavigateToPairing() } },
                        modifier = Modifier.fillMaxWidth(), shape = ShapeTokens.Button,
                        colors = ButtonDefaults.buttonColors(containerColor = ColorTokens.Primary, contentColor = ColorTokens.TextOnPrimary)
                    ) { Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text("Log out of this profile", style = TypographyTokens.BodyLarge) }

                    Spacer(modifier = Modifier.height(SpacingTokens.Small))

                    Button(
                        onClick = { showClearChatConfirm = true },
                        enabled = state !is SettingsActionState.Loading,
                        modifier = Modifier.fillMaxWidth(), shape = ShapeTokens.Button,
                        colors = ButtonDefaults.buttonColors(containerColor = ColorTokens.Error, contentColor = ColorTokens.TextOnPrimary)
                    ) { Icon(Icons.Default.DeleteSweep, null); Spacer(Modifier.width(8.dp)); Text("Clear entire chat", style = TypographyTokens.BodyLarge) }
                }
            }
        }
    }
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
