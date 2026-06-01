package com.mmy.g700remote.blewake

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BleWakePolicyTest {
    private val serviceUuid = "b1c2d3e4-f5a6-7890-abcd-ef1234567890"

    @Test
    fun matchesTargetRequiresDisplayMirrorService() {
        assertFalse(
            BleWakePolicy.matchesTarget(
                advertisedServiceUuids = setOf("0000180f-0000-1000-8000-00805f9b34fb"),
                resultAddress = "22:22:00:AE:74:F4",
                targetAddress = "22:22:00:AE:74:F4",
                serviceUuid = serviceUuid,
            ),
        )
    }

    @Test
    fun matchesTargetAcceptsServiceAndKnownAddress() {
        assertTrue(
            BleWakePolicy.matchesTarget(
                advertisedServiceUuids = setOf(serviceUuid.uppercase()),
                resultAddress = "22:22:00:AE:74:F4",
                targetAddress = "22:22:00:AE:74:F4",
                serviceUuid = serviceUuid,
            ),
        )
    }

    @Test
    fun matchesTargetRejectsWrongKnownAddress() {
        assertFalse(
            BleWakePolicy.matchesTarget(
                advertisedServiceUuids = setOf(serviceUuid),
                resultAddress = "AA:BB:CC:DD:EE:FF",
                targetAddress = "22:22:00:AE:74:F4",
                serviceUuid = serviceUuid,
            ),
        )
    }

    @Test
    fun matchesTargetAllowsServiceOnlyWhenAddressIsNotStableBleAddress() {
        assertTrue(
            BleWakePolicy.matchesTarget(
                advertisedServiceUuids = setOf(serviceUuid),
                resultAddress = "AA:BB:CC:DD:EE:FF",
                targetAddress = "displaymirror.local:9274",
                serviceUuid = serviceUuid,
            ),
        )
    }

    @Test
    fun debouncesWakeEventsInsideWindow() {
        assertTrue(BleWakePolicy.shouldDebounce(lastWakeMillis = 1_000, nowMillis = 30_000))
        assertFalse(BleWakePolicy.shouldDebounce(lastWakeMillis = 1_000, nowMillis = 130_000))
    }
}
