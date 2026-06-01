package com.mmy.g700remote.blewake

import java.util.Locale

object BleWakePolicy {
    const val DEBOUNCE_MS: Long = 2 * 60 * 1000

    fun isBleAddress(value: String?): Boolean =
        value?.matches(Regex("(?i)^([0-9A-F]{2}:){5}[0-9A-F]{2}$")) == true

    fun matchesTarget(
        advertisedServiceUuids: Set<String>,
        resultAddress: String?,
        targetAddress: String?,
        serviceUuid: String,
    ): Boolean {
        val hasService = advertisedServiceUuids.any { it.equals(serviceUuid, ignoreCase = true) }
        if (!hasService) return false
        val normalizedTarget = targetAddress?.uppercase(Locale.US)
        val normalizedResult = resultAddress?.uppercase(Locale.US)
        return normalizedTarget.isNullOrBlank() ||
            !isBleAddress(normalizedTarget) ||
            normalizedTarget == normalizedResult
    }

    fun shouldDebounce(lastWakeMillis: Long?, nowMillis: Long, windowMillis: Long = DEBOUNCE_MS): Boolean =
        lastWakeMillis != null && nowMillis - lastWakeMillis < windowMillis
}
