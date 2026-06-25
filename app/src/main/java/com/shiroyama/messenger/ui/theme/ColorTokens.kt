package com.shiroyama.messenger.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

data class ChatStylePreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val isDark: Boolean,
    val primary: Color,
    val primaryLight: Color,
    val primaryDark: Color,
    val background: Color,
    val backgroundAlt: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val outboundBubble: Color,
    val inboundBubble: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textOnPrimary: Color,
    val textOnOutbound: Color,
    val borderLight: Color,
    val accentSoft: Color,
    val destructive: Color,
    val success: Color,
    val cyanAccent: Color = Color(0xFF5DDCF1),
    val gradientStart: Color = primaryLight,
    val gradientEnd: Color = background
)

object ChatStylePresets {
    val PinkCloud = ChatStylePreset(
        id = "pink_cloud",
        title = "Pink Cloud",
        subtitle = "Soft blush, white panels, gentle rose accents",
        isDark = false,
        primary = Color(0xFFE95D9A),
        primaryLight = Color(0xFFFFD9EA),
        primaryDark = Color(0xFFB72E6C),
        background = Color(0xFFFFF4FA),
        backgroundAlt = Color(0xFFFFEAF4),
        surface = Color(0xFFFFFBFE),
        surfaceElevated = Color(0xFFFFFFFF),
        outboundBubble = Color(0xFFFF7EB6),
        inboundBubble = Color(0xFFFFFFFF),
        textPrimary = Color(0xFF28131D),
        textSecondary = Color(0xFF8B6677),
        textOnPrimary = Color.White,
        textOnOutbound = Color.White,
        borderLight = Color(0xFFFFC7DF),
        accentSoft = Color(0xFFFFEAF3),
        destructive = Color(0xFFD83A52),
        success = Color(0xFF44C767),
        gradientStart = Color(0xFFFFE1EE),
        gradientEnd = Color(0xFFFFF8FC)
    )

    val LavenderNight = ChatStylePreset(
        id = "lavender_night",
        title = "Lavender Night",
        subtitle = "Dark purple, lavender surfaces, soft neon pink",
        isDark = true,
        primary = Color(0xFFFF6FDB),
        primaryLight = Color(0xFF8D6BFF),
        primaryDark = Color(0xFFB143FF),
        background = Color(0xFF150E23),
        backgroundAlt = Color(0xFF221538),
        surface = Color(0xFF25183D),
        surfaceElevated = Color(0xFF30204E),
        outboundBubble = Color(0xFFB84CFF),
        inboundBubble = Color(0xFF30204E),
        textPrimary = Color(0xFFF9F1FF),
        textSecondary = Color(0xFFCDBBE3),
        textOnPrimary = Color.White,
        textOnOutbound = Color.White,
        borderLight = Color(0xFF574172),
        accentSoft = Color(0xFF3B2859),
        destructive = Color(0xFFFF5878),
        success = Color(0xFF6DE6A7),
        cyanAccent = Color(0xFF78E8FF),
        gradientStart = Color(0xFF2A1744),
        gradientEnd = Color(0xFF150E23)
    )

    val MilkStrawberry = ChatStylePreset(
        id = "milk_strawberry",
        title = "Milk Strawberry",
        subtitle = "Cream white, strawberry accents, warm details",
        isDark = false,
        primary = Color(0xFFFF6F91),
        primaryLight = Color(0xFFFFE2E9),
        primaryDark = Color(0xFFD84A6C),
        background = Color(0xFFFFFAF7),
        backgroundAlt = Color(0xFFFFF0F4),
        surface = Color(0xFFFFFFFF),
        surfaceElevated = Color(0xFFFFFFFF),
        outboundBubble = Color(0xFFFF8FA9),
        inboundBubble = Color(0xFFFFF4F6),
        textPrimary = Color(0xFF2A1519),
        textSecondary = Color(0xFF8D6B72),
        textOnPrimary = Color.White,
        textOnOutbound = Color.White,
        borderLight = Color(0xFFFFD0DA),
        accentSoft = Color(0xFFFFEDF2),
        destructive = Color(0xFFD83A52),
        success = Color(0xFF43B66A),
        gradientStart = Color(0xFFFFF0E7),
        gradientEnd = Color(0xFFFFFAF7)
    )

    val SakuraPaper = ChatStylePreset(
        id = "sakura_paper",
        title = "Sakura Paper",
        subtitle = "Pale paper, muted sakura pink, warm brown text",
        isDark = false,
        primary = Color(0xFFD95F80),
        primaryLight = Color(0xFFFFDDE6),
        primaryDark = Color(0xFFA93F5E),
        background = Color(0xFFFFF7F2),
        backgroundAlt = Color(0xFFFFEFE7),
        surface = Color(0xFFFFFCFA),
        surfaceElevated = Color(0xFFFFFFFF),
        outboundBubble = Color(0xFFE87C99),
        inboundBubble = Color(0xFFFFFFFF),
        textPrimary = Color(0xFF33211D),
        textSecondary = Color(0xFF8B7076),
        textOnPrimary = Color.White,
        textOnOutbound = Color.White,
        borderLight = Color(0xFFFFD2DC),
        accentSoft = Color(0xFFFFEEF3),
        destructive = Color(0xFFB94A58),
        success = Color(0xFF579E64),
        gradientStart = Color(0xFFFFE7DF),
        gradientEnd = Color(0xFFFFFAF7)
    )

    val CyberCandy = ChatStylePreset(
        id = "cyber_candy",
        title = "Cyber Candy",
        subtitle = "Dark candy shell with fuchsia, cyan, violet accents",
        isDark = true,
        primary = Color(0xFFFF2EC4),
        primaryLight = Color(0xFF00D9FF),
        primaryDark = Color(0xFF8D35FF),
        background = Color(0xFF080914),
        backgroundAlt = Color(0xFF12142A),
        surface = Color(0xFF16182F),
        surfaceElevated = Color(0xFF202342),
        outboundBubble = Color(0xFFB328FF),
        inboundBubble = Color(0xFF202342),
        textPrimary = Color(0xFFF7F2FF),
        textSecondary = Color(0xFFB9B7D7),
        textOnPrimary = Color.White,
        textOnOutbound = Color.White,
        borderLight = Color(0xFF393D68),
        accentSoft = Color(0xFF24284B),
        destructive = Color(0xFFFF4568),
        success = Color(0xFF33E6A4),
        cyanAccent = Color(0xFF00E5FF),
        gradientStart = Color(0xFF1A1237),
        gradientEnd = Color(0xFF080914)
    )

    val Pink = PinkCloud
    val Lavender = LavenderNight
    val Milk = MilkStrawberry
    val Sakura = SakuraPaper

    val All = listOf(PinkCloud, LavenderNight, MilkStrawberry, SakuraPaper, CyberCandy)
    fun byId(id: String?): ChatStylePreset = All.firstOrNull { it.id == id } ?: PinkCloud
}

object ChatStyleStore {
    var current by mutableStateOf(ChatStylePresets.PinkCloud)
}

object ColorTokens {
    val IsDark get() = ChatStyleStore.current.isDark
    val Primary get() = ChatStyleStore.current.primary
    val PrimaryLight get() = ChatStyleStore.current.primaryLight
    val PrimaryDark get() = ChatStyleStore.current.primaryDark
    val Background get() = ChatStyleStore.current.background
    val BackgroundAlt get() = ChatStyleStore.current.backgroundAlt
    val Surface get() = ChatStyleStore.current.surface
    val SurfaceElevated get() = ChatStyleStore.current.surfaceElevated
    val TextPrimary get() = ChatStyleStore.current.textPrimary
    val TextSecondary get() = ChatStyleStore.current.textSecondary
    val TextOnPrimary get() = ChatStyleStore.current.textOnPrimary
    val OutboundBubble get() = ChatStyleStore.current.outboundBubble
    val InboundBubble get() = ChatStyleStore.current.inboundBubble
    val TextOnOutbound get() = ChatStyleStore.current.textOnOutbound
    val TextOnInbound get() = ChatStyleStore.current.textPrimary
    val BorderLight get() = ChatStyleStore.current.borderLight
    val GreenOnline get() = ChatStyleStore.current.success
    val GrayOffline = Color(0xFFA8A1AA)
    val Error get() = ChatStyleStore.current.destructive
    val AccentSoft get() = ChatStyleStore.current.accentSoft
    val CyanAccent get() = ChatStyleStore.current.cyanAccent
    val GradientStart get() = ChatStyleStore.current.gradientStart
    val GradientEnd get() = ChatStyleStore.current.gradientEnd
}