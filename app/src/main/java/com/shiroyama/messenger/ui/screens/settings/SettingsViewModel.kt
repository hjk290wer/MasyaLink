package com.shiroyama.messenger.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiroyama.messenger.data.repository.MessengerRepositoryImpl
import com.shiroyama.messenger.domain.model.LocalSession
import com.shiroyama.messenger.domain.usecase.ClearRoomMessagesUseCase
import com.shiroyama.messenger.domain.usecase.LogoutDeviceUseCase
import com.shiroyama.messenger.domain.usecase.UpdateProfileAvatarUseCase
import com.shiroyama.messenger.domain.usecase.UpdateProfileNameUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SettingsActionState {
    object Idle : SettingsActionState
    object Loading : SettingsActionState
    object Done : SettingsActionState
    data class Error(val message: String) : SettingsActionState
}

class SettingsViewModel(
    private val logoutDeviceUseCase: LogoutDeviceUseCase = LogoutDeviceUseCase(MessengerRepositoryImpl()),
    private val updateProfileNameUseCase: UpdateProfileNameUseCase = UpdateProfileNameUseCase(MessengerRepositoryImpl()),
    private val updateProfileAvatarUseCase: UpdateProfileAvatarUseCase = UpdateProfileAvatarUseCase(MessengerRepositoryImpl()),
    private val clearRoomMessagesUseCase: ClearRoomMessagesUseCase = ClearRoomMessagesUseCase(MessengerRepositoryImpl())
) : ViewModel() {
    private val _state = MutableStateFlow<SettingsActionState>(SettingsActionState.Idle)
    val state: StateFlow<SettingsActionState> = _state.asStateFlow()

    fun logout(session: LocalSession?, onLoggedOut: () -> Unit) {
        if (session == null) { onLoggedOut(); return }
        _state.value = SettingsActionState.Loading
        viewModelScope.launch {
            try { logoutDeviceUseCase(session.accessToken, session.deviceId) } catch (e: Exception) { e.printStackTrace() }
            finally { _state.value = SettingsActionState.Idle; onLoggedOut() }
        }
    }

    fun updateName(session: LocalSession?, newName: String, onUpdated: () -> Unit) {
        if (session == null) { _state.value = SettingsActionState.Error("No active session"); return }
        val trimmed = newName.trim()
        if (trimmed.length < 2) { _state.value = SettingsActionState.Error("Name is too short"); return }
        _state.value = SettingsActionState.Loading
        viewModelScope.launch {
            try {
                updateProfileNameUseCase(session.accessToken, session.profileKey, trimmed)
                _state.value = SettingsActionState.Done
                onUpdated()
            } catch (e: Exception) {
                e.printStackTrace(); _state.value = SettingsActionState.Error(e.message ?: "Failed to update profile name")
            }
        }
    }

    fun updateAvatar(session: LocalSession?, fileName: String, mimeType: String, bytes: ByteArray, onUpdated: (String) -> Unit) {
        if (session == null) { _state.value = SettingsActionState.Error("No active session"); return }
        _state.value = SettingsActionState.Loading
        viewModelScope.launch {
            try {
                val path = updateProfileAvatarUseCase(session.accessToken, session.profileKey, fileName, mimeType, bytes)
                _state.value = SettingsActionState.Done
                onUpdated(path)
            } catch (e: Exception) {
                e.printStackTrace(); _state.value = SettingsActionState.Error(e.message ?: "Failed to update avatar")
            }
        }
    }

    fun clearChat(session: LocalSession?, onCleared: () -> Unit) {
        if (session == null) { _state.value = SettingsActionState.Error("No active session"); return }
        _state.value = SettingsActionState.Loading
        viewModelScope.launch {
            try {
                clearRoomMessagesUseCase(session.accessToken, session.roomId, session.deviceId)
                _state.value = SettingsActionState.Done
                onCleared()
            } catch (e: Exception) {
                e.printStackTrace(); _state.value = SettingsActionState.Error(e.message ?: "Failed to clear chat")
            }
        }
    }
}
