package com.shiroyama.messenger.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

data class ChatStylePreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val primary: Color,
    val primaryLight: Color,
    val primaryDark: Color,
    val background: Color,
    val surface: Color,
    val outboundBubble: Color,
    val inboundBubble: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textOnPrimary: Color,
    val textOnOutbound: Color,
    val borderLight: Color,
    val accentSoft: Color
)

object ChatStylePresets {
    val Pink = ChatStylePreset(
        id = "pink_cloud",
        title = "Pink cloud",
        subtitle = "Мягкий розовый, основной стиль",
        primary = Color(0xFFE95D9A),
        primaryLight = Color(0xFFFFD9EA),
        primaryDark = Color(0xFFB72E6C),
        background = Color(0xFFFFF4FA),
        surface = Color(0xFFFFFBFE),
        outboundBubble = Color(0xFFFF7EB6),
        inboundBubble = Color(0xFFFFFFFF),
        textPrimary = Color(0xFF28131D),
        textSecondary = Color(0xFF8B6677),
        textOnPrimary = Color(0xFFFFFFFF),
        textOnOutbound = Color(0xFFFFFFFF),
        borderLight = Color(0xFFFFC7DF),
        accentSoft = Color(0xFFFFEAF3)
    )

    val Lavender = ChatStylePreset(
        id = "lavender_night",
        title = "Lavender night",
        subtitle = "Фиолетовый вечерний стиль",
        primary = Color(0xFF7C5CFF),
        primaryLight = Color(0xFFE7DFFF),
        primaryDark = Color(0xFF4D35B7),
        background = Color(0xFFF6F1FF),
        surface = Color(0xFFFFFFFF),
        outboundBubble = Color(0xFF8D74FF),
        inboundBubble = Color(0xFFFFFFFF),
        textPrimary = Color(0xFF1F1733),
        textSecondary = Color(0xFF746A8F),
        textOnPrimary = Color(0xFFFFFFFF),
        textOnOutbound = Color(0xFFFFFFFF),
        borderLight = Color(0xFFD8CCFF),
        accentSoft = Color(0xFFEFE9FF)
    )

    val Milk = ChatStylePreset(
        id = "milk_strawberry",
        title = "Milk strawberry",
        subtitle = "Светлый клубнично-молочный стиль",
        primary = Color(0xFFFF6F91),
        primaryLight = Color(0xFFFFE2E9),
        primaryDark = Color(0xFFD84A6C),
        background = Color(0xFFFFFAF7),
        surface = Color(0xFFFFFFFF),
        outboundBubble = Color(0xFFFF8FA9),
        inboundBubble = Color(0xFFFFF0F4),
        textPrimary = Color(0xFF2A1519),
        textSecondary = Color(0xFF8D6B72),
        textOnPrimary = Color(0xFFFFFFFF),
        textOnOutbound = Color(0xFFFFFFFF),
        borderLight = Color(0xFFFFD0DA),
        accentSoft = Color(0xFFFFEDF2)
    )

    val Sakura = ChatStylePreset(
        id = "sakura_paper",
        title = "Sakura paper",
        subtitle = "Нежный бумажный розовый",
        primary = Color(0xFFD95F80),
        primaryLight = Color(0xFFFFDDE6),
        primaryDark = Color(0xFFA93F5E),
        background = Color(0xFFFFF7F2),
        surface = Color(0xFFFFFCFA),
        outboundBubble = Color(0xFFE87C99),
        inboundBubble = Color(0xFFFFFFFF),
        textPrimary = Color(0xFF291C1F),
        textSecondary = Color(0xFF8B7076),
        textOnPrimary = Color(0xFFFFFFFF),
        textOnOutbound = Color(0xFFFFFFFF),
        borderLight = Color(0xFFFFD2DC),
        accentSoft = Color(0xFFFFEEF3)
    )

    val All = listOf(Pink, Lavender, Milk, Sakura)
    fun byId(id: String?): ChatStylePreset = All.firstOrNull { it.id == id } ?: Pink
}

object ChatStyleStore {
    var current by mutableStateOf(ChatStylePresets.Pink)
}

object ColorTokens {
    val Primary get() = ChatStyleStore.current.primary
    val PrimaryLight get() = ChatStyleStore.current.primaryLight
    val PrimaryDark get() = ChatStyleStore.current.primaryDark
    val Background get() = ChatStyleStore.current.background
    val Surface get() = ChatStyleStore.current.surface
    val TextPrimary get() = ChatStyleStore.current.textPrimary
    val TextSecondary get() = ChatStyleStore.current.textSecondary
    val TextOnPrimary get() = ChatStyleStore.current.textOnPrimary
    val OutboundBubble get() = ChatStyleStore.current.outboundBubble
    val InboundBubble get() = ChatStyleStore.current.inboundBubble
    val TextOnOutbound get() = ChatStyleStore.current.textOnOutbound
    val TextOnInbound get() = ChatStyleStore.current.textPrimary
    val BorderLight get() = ChatStyleStore.current.borderLight
    val GreenOnline = Color(0xFF44C767)
    val GrayOffline = Color(0xFFA8A1AA)
    val Error = Color(0xFFD83A52)
    val AccentSoft get() = ChatStyleStore.current.accentSoft
}
