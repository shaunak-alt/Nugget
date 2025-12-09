package com.nugget.tracking.rate

import com.nugget.tracking.MutableClock
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class FixedWindowRateLimiterTest {

    private val clock = MutableClock(Instant.parse("2024-01-01T00:00:00Z"))

    @Test
    fun `limit not exceeded until threshold`() {
        val limiter = FixedWindowRateLimiter(maxRequestsPerSecond = 3, clock = clock)
        repeat(3) {
            val exceeded = limiter.hitAndCheckLimit()
            assertFalse(exceeded)
        }
        assertTrue(limiter.hitAndCheckLimit())
    }

    @Test
    fun `limit resets on new window`() {
        val limiter = FixedWindowRateLimiter(maxRequestsPerSecond = 2, clock = clock)
        repeat(2) {
            val exceeded = limiter.hitAndCheckLimit()
            assertFalse(exceeded)
        }
        assertTrue(limiter.hitAndCheckLimit())

        clock.advanceSeconds(1)
        assertFalse(limiter.hitAndCheckLimit())
    }
}
