package com.shiroyama.messenger.core.storage

import android.util.Log

object StoragePathNormalizer {
    private const val TAG = "StoragePathNormalizer"

    fun normalizeStoragePath(raw: String?): String? {
        val input = raw?.trim().orEmpty()
        if (input.isBlank()) return null

        val extracted = when {
            input.startsWith("http://") || input.startsWith("https://") -> extractFromUrl(input)
            else -> input
        } ?: return invalid(input, "unsupported full URL")

        val withoutQuery = extracted.substringBefore('?').substringBefore('#')
        val normalized = withoutQuery
            .trim()
            .replace('\\', '/')
            .trimStart('/')
            .removePrefix("media/")
            .trim()

        if (normalized.isBlank()) return invalid(input, "blank after normalization")
        if (normalized.startsWith("/")) return invalid(input, "starts with slash")
        if (normalized.startsWith("media/")) return invalid(input, "still includes bucket prefix")
        if (normalized.contains("://")) return invalid(input, "still contains URL scheme")
        if (normalized.contains("..")) return invalid(input, "contains parent path segment")

        return normalized
    }

    fun requireStoragePath(raw: String?): String {
        return normalizeStoragePath(raw) ?: throw IllegalArgumentException("Media path is invalid")
    }

    private fun extractFromUrl(value: String): String? {
        val markers = listOf(
            "/storage/v1/object/media/",
            "/storage/v1/object/sign/media/",
            "/storage/v1/object/public/media/",
            "/object/media/",
            "/object/sign/media/",
            "/object/public/media/"
        )
        for (marker in markers) {
            val index = value.indexOf(marker)
            if (index >= 0) return value.substring(index + marker.length)
        }
        return null
    }

    private fun invalid(input: String, reason: String): String? {
        Log.w(TAG, "Invalid media path: reason=$reason value=$input")
        return null
    }
}
