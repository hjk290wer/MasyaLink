package com.shiroyama.messenger.ui.media

import java.io.File

sealed interface MediaLoadState {
    object NotLoaded : MediaLoadState
    data class Loading(val progress: Float? = null) : MediaLoadState
    data class Ready(
        val file: File,
        val fileName: String,
        val mimeType: String,
        val sizeBytes: Long
    ) : MediaLoadState
    data class Error(val message: String) : MediaLoadState
}
