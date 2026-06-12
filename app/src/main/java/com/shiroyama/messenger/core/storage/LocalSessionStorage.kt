package com.shiroyama.messenger.core.storage

import android.content.Context
import com.shiroyama.messenger.domain.model.LocalSession
import com.shiroyama.messenger.ui.theme.ChatStylePresets
import com.shiroyama.messenger.ui.theme.ChatStyleStore

class LocalSessionStorage(private val context: Context) {
    private val prefs = context.getSharedPreferences("masyalink_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val SESSION_SCHEMA_VERSION = 3
        private const val KEY_SESSION_SCHEMA_VERSION = "session_schema_version"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_ROOM_ID = "room_id"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_ACCOUNT_ID = "account_id"
        private const val KEY_PROFILE_KEY = "profile_key"
        private const val KEY_USERNAME = "username"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_ROOM_CODE = "room_code"
        private const val KEY_CHAT_STYLE_ID = "chat_style_id"
    }

    fun saveSession(session: LocalSession) {
        prefs.edit().apply {
            putInt(KEY_SESSION_SCHEMA_VERSION, SESSION_SCHEMA_VERSION)
            putString(KEY_ACCESS_TOKEN, session.accessToken)
            putString(KEY_REFRESH_TOKEN, session.refreshToken)
            putString(KEY_ROOM_ID, session.roomId)
            putString(KEY_DEVICE_ID, session.deviceId)
            putString(KEY_ACCOUNT_ID, session.accountId)
            putString(KEY_PROFILE_KEY, session.profileKey)
            putString(KEY_USERNAME, session.username)
            putString(KEY_DISPLAY_NAME, session.displayName)
            putString(KEY_ROOM_CODE, session.roomCode)
            apply()
        }
    }

    fun getSession(): LocalSession? {
        val version = prefs.getInt(KEY_SESSION_SCHEMA_VERSION, 0)
        if (version < SESSION_SCHEMA_VERSION) {
            clearSession()
            return null
        }

        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val roomId = prefs.getString(KEY_ROOM_ID, null) ?: return null
        val deviceId = prefs.getString(KEY_DEVICE_ID, null) ?: return null
        val accountId = prefs.getString(KEY_ACCOUNT_ID, null) ?: return null
        val profileKey = prefs.getString(KEY_PROFILE_KEY, null) ?: return null
        val username = prefs.getString(KEY_USERNAME, null) ?: profileKey
        val displayName = prefs.getString(KEY_DISPLAY_NAME, null) ?: username
        val roomCode = prefs.getString(KEY_ROOM_CODE, null) ?: return null
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)

        return LocalSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            roomId = roomId,
            deviceId = deviceId,
            accountId = accountId,
            profileKey = profileKey,
            username = username,
            displayName = displayName,
            roomCode = roomCode
        )
    }

    fun clearSession() {
        val styleId = getChatStyleId()
        prefs.edit().clear().putString(KEY_CHAT_STYLE_ID, styleId).apply()
    }

    fun getChatStyleId(): String {
        return prefs.getString(KEY_CHAT_STYLE_ID, ChatStylePresets.Pink.id) ?: ChatStylePresets.Pink.id
    }

    fun saveChatStyle(styleId: String) {
        prefs.edit().putString(KEY_CHAT_STYLE_ID, styleId).apply()
        ChatStyleStore.current = ChatStylePresets.byId(styleId)
    }

    fun loadChatStyleIntoMemory() {
        ChatStyleStore.current = ChatStylePresets.byId(getChatStyleId())
    }
}
