package com.shiroyama.messenger.ui.network

sealed interface ConnectionState {
    object Connected : ConnectionState
    object Connecting : ConnectionState
    object Reconnecting : ConnectionState
    object Offline : ConnectionState
    object ServerUnavailable : ConnectionState
    data class Error(val message: String) : ConnectionState
}

fun classifyConnectionError(message: String?): ConnectionState {
    val text = message.orEmpty().lowercase()
    return when {
        text.contains("unknownhost") || text.contains("unable to resolve host") || text.contains("failed to connect") ->
            ConnectionState.ServerUnavailable
        text.contains("timeout") -> ConnectionState.Reconnecting
        text.contains("401") || text.contains("403") -> ConnectionState.Error("Session or access problem. Reopen the chat.")
        else -> ConnectionState.Error(message ?: "Connection error")
    }
}

fun ConnectionState.userMessage(): String? = when (this) {
    ConnectionState.Connected -> null
    ConnectionState.Connecting -> "Connecting…"
    ConnectionState.Reconnecting -> "Reconnecting…"
    ConnectionState.Offline -> "No network connection"
    ConnectionState.ServerUnavailable -> "Server address is unavailable. Check VPN/DNS/network."
    is ConnectionState.Error -> message
}
