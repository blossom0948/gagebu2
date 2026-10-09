package com.moasseum.app

import com.moasseum.app.domain.NetworkReconnectGate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkReconnectGateTest {
    @Test
    fun alreadyConnectedDoesNotLookLikeReconnect() {
        val gate = NetworkReconnectGate(initiallyAvailable = true)

        assertFalse(gate.update(true))
        assertFalse(gate.update(true))
    }

    @Test
    fun reconnectEmitsExactlyOncePerOfflinePeriod() {
        val gate = NetworkReconnectGate(initiallyAvailable = false)

        assertTrue(gate.update(true))
        assertFalse(gate.update(true))
        assertFalse(gate.update(false))
        assertTrue(gate.update(true))
    }
}
