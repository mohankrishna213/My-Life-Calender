package com.focus.wallpaper

import java.time.Duration
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MidnightWallpaperWorkerTest {

    @Test fun `initial delay targets next midnight`() {
        val now = LocalDateTime.of(2026, 8, 23, 15, 30)
        val delay = MidnightWallpaperWorker.initialDelayUntilNextMidnight(now)
        assertEquals(Duration.ofHours(8).plusMinutes(30), delay)
    }

    @Test fun `delay just before midnight is short`() {
        val now = LocalDateTime.of(2026, 8, 23, 23, 59)
        val delay = MidnightWallpaperWorker.initialDelayUntilNextMidnight(now)
        assertEquals(1L, delay.toMinutes())
    }

    @Test fun `delay is always positive`() {
        val now = LocalDateTime.of(2026, 8, 23, 0, 0)
        val delay = MidnightWallpaperWorker.initialDelayUntilNextMidnight(now)
        assertTrue(!delay.isNegative && !delay.isZero)
    }
}
