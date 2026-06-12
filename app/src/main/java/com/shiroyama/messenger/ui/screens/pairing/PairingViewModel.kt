package com.shiroyama.messenger.ui.screens.pairing

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiroyama.messenger.core.storage.LocalSessionStorage
import com.shiroyama.messenger.data.repository.MessengerRepositoryImpl
import com.shiroyama.messenger.domain.usecase.DownloadAttachmentUseCase
import com.shiroyama.messenger.domain.usecase.LoadFixedProfilesUseCase
import com.shiroyama.messenger.domain.usecase.PairDeviceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileButtonState(
    val name: String,
    val avatarPath: String? = null,
    val avatarBytes: ByteArray? = null
)

sealed interface PairingUiState {
    data class Idle(
        val profileA: ProfileButtonState = ProfileButtonState("Profile A"),
        val profileB: ProfileButtonState = ProfileButtonState("Profile B"),
        val isNamesLoading: Boolean = false
    ) : PairingUiState
    object Loading : PairingUiState
    data class Success(val displayName: String) : PairingUiState
    data class Error(
        val message: String,
        val profileA: ProfileButtonState = ProfileButtonState("Profile A"),
        val profileB: ProfileButtonState = ProfileButtonState("Profile B")
    ) : PairingUiState
}

class PairingViewModel(
    private val repository: MessengerRepositoryImpl = MessengerRepositoryImpl(),
    private val pairDeviceUseCase: PairDeviceUseCase = PairDeviceUseCase(repository),
    private val loadFixedProfilesUseCase: LoadFixedProfilesUseCase = LoadFixedProfilesUseCase(repository),
    private val downloadAttachmentUseCase: DownloadAttachmentUseCase = DownloadAttachmentUseCase(repository)
) : ViewModel() {

    private val _uiState = MutableStateFlow<PairingUiState>(PairingUiState.Idle(isNamesLoading = true))
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    init { loadProfileNames() }

    private fun currentProfiles(): Pair<ProfileButtonState, ProfileButtonState> {
        return when (val state = _uiState.value) {
            is PairingUiState.Idle -> state.profileA to state.profileB
            is PairingUiState.Error -> state.profileA to state.profileB
            else -> ProfileButtonState("Profile A") to ProfileButtonState("Profile B")
        }
    }

    fun loadProfileNames() {
        viewModelScope.launch {
            val previous = currentProfiles()
            _uiState.value = PairingUiState.Idle(profileA = previous.first, profileB = previous.second, isNamesLoading = true)
            try {
                val token = repository.anonymousSignIn().access_token ?: throw Exception("Could not create temporary anonymous session")
                val profiles = loadFixedProfilesUseCase(token)
                suspend fun makeProfile(key: String, fallback: String): ProfileButtonState {
                    val profile = profiles[key]
                    val avatarBytes = profile?.avatarPath?.let { path ->
                        runCatching { downloadAttachmentUseCase(token, path) }.getOrNull()
                    }
                    return ProfileButtonState(
                        name = profile?.displayName ?: fallback,
                        avatarPath = profile?.avatarPath,
                        avatarBytes = avatarBytes
                    )
                }
                _uiState.value = PairingUiState.Idle(
                    profileA = makeProfile("A", "Profile A"),
                    profileB = makeProfile("B", "Profile B"),
                    isNamesLoading = false
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = PairingUiState.Error("Could not load profile names. Using defaults.", previous.first, previous.second)
            }
        }
    }

    fun selectProfile(profileKey: String, sessionStorage: LocalSessionStorage, onSuccess: () -> Unit) {
        val normalized = profileKey.trim().uppercase()
        if (normalized !in setOf("A", "B")) {
            val profiles = currentProfiles()
            _uiState.value = PairingUiState.Error("Unknown profile", profiles.first, profiles.second)
            return
        }
        _uiState.value = PairingUiState.Loading
        viewModelScope.launch {
            try {
                val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
                val session = pairDeviceUseCase(profileKey = normalized, displayName = null, deviceName = deviceName)
                sessionStorage.saveSession(session)
                _uiState.value = PairingUiState.Success(session.displayName)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                val profiles = currentProfiles()
                _uiState.value = PairingUiState.Error(e.message ?: "Connection failed", profiles.first, profiles.second)
            }
        }
    }
}
