package com.shiroyama.messenger.core.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    fun formatIsoToTime(isoString: String?): String {
        if (isoString.isNullOrBlank()) return ""
        return try {
            val instant = Instant.parse(isoString)
            val zonedDateTime = instant.atZone(ZoneId.systemDefault())
            zonedDateTime.format(timeFormatter)
        } catch (e: Exception) {
            try {
                val tIndex = isoString.indexOf('T')
                if (tIndex != -1 && isoString.length > tIndex + 5) {
                    isoString.substring(tIndex + 1, tIndex + 6)
                } else {
                    ""
                }
            } catch (ex: Exception) {
                ""
            }
        }
    }

    fun isOnline(lastSeenIso: String?): Boolean {
        return isFresh(lastSeenIso, maxAgeSeconds = 45)
    }

    fun isFresh(isoString: String?, maxAgeSeconds: Long): Boolean {
        if (isoString.isNullOrBlank()) return false
        return try {
            val instant = Instant.parse(isoString)
            val secondsDiff = Instant.now().epochSecond - instant.epochSecond
            secondsDiff in 0..maxAgeSeconds
        } catch (e: Exception) {
            false
        }
    }
}
