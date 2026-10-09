package com.moasseum.app.domain

/** Emits once when validated internet returns after an unavailable period. */
class NetworkReconnectGate(initiallyAvailable: Boolean) {
    private var available = initiallyAvailable

    @Synchronized
    fun update(isAvailable: Boolean): Boolean {
        val reconnected = !available && isAvailable
        available = isAvailable
        return reconnected
    }
}
