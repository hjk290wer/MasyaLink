package com.shiroyama.messenger.domain.usecase

import com.shiroyama.messenger.core.util.DateTimeUtils
import com.shiroyama.messenger.domain.model.Device
import com.shiroyama.messenger.domain.repository.MessengerRepository

class LoadDevicesUseCase(private val repository: MessengerRepository) {
    suspend operator fun invoke(token: String, roomId: String): List<Device> {
        val dtos = repository.loadDevices(token, roomId)
        return dtos.map { dto ->
            val devId = dto.device_id ?: dto.id ?: ""
            Device(
                deviceId = devId,
                roomId = dto.room_id ?: "",
                accountId = dto.account_id,
                displayName = dto.display_name ?: "Unknown",
                deviceName = dto.device_name ?: "Android Device",
                lastSeenAt = dto.last_seen_at,
                isOnline = DateTimeUtils.isOnline(dto.last_seen_at)
            )
        }
    }
}
