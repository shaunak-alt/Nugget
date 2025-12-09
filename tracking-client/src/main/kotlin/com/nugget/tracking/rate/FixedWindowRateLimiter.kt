package com.nugget.tracking.rate

import java.time.Clock
import java.util.concurrent.atomic.AtomicLong

/**
 * Lightweight fixed-window rate limiter used for non-blocking telemetry signaling.
 */
class FixedWindowRateLimiter(
    private val maxRequestsPerSecond: Long,
    private val clock: Clock = Clock.systemUTC()
) {
    init {
        require(maxRequestsPerSecond > 0) { "maxRequestsPerSecond must be positive" }
    }

    private val currentWindowSecond = AtomicLong(clock.instant().epochSecond)
    private val counter = AtomicLong(0)

    /**
     * Registers a hit and returns true when the configured limit is exceeded.
     */
    fun hitAndCheckLimit(): Boolean {
        val nowSecond = clock.instant().epochSecond
        resetWindowIfNeeded(nowSecond)
        val currentCount = counter.incrementAndGet()
        return currentCount > maxRequestsPerSecond
    }

    private fun resetWindowIfNeeded(nowSecond: Long) {
        while (true) {
            val window = currentWindowSecond.get()
            if (window == nowSecond) {
                return
            }
            if (currentWindowSecond.compareAndSet(window, nowSecond)) {
                counter.set(0)
                return
            }
        }
    }
}
