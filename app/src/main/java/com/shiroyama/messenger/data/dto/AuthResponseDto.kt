package com.shiroyama.messenger.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponseDto(
    val access_token: String? = null,
    val refresh_token: String? = null,
    val expires_in: Int? = null,
    val token_type: String? = null,
    val user: AuthUserDto? = null
)

@Serializable
data class AuthUserDto(
    val id: String? = null
)
