package com.shiroyama.messenger.domain.model

data class FixedProfile(
    val key: String,
    val displayName: String,
    val avatarPath: String? = null
)
