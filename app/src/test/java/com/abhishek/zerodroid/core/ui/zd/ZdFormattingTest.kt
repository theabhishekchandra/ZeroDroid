package com.abhishek.zerodroid.core.ui.zd

import com.abhishek.zerodroid.core.util.formatAgo
import org.junit.Assert.assertEquals
import org.junit.Test

class ZdFormattingTest {

    @Test
    fun `elapsed time is mm ss under an hour and h mm ss above`() {
        assertEquals("00:00", formatElapsed(0))
        assertEquals("00:42", formatElapsed(42_900))
        assertEquals("04:18", formatElapsed(258_000))
        assertEquals("1:02:03", formatElapsed(3_723_000))
        assertEquals("00:00", formatElapsed(-5_000))
    }

    @Test
    fun `signal bars follow the dBm ladder`() {
        assertEquals(4, signalBarsFor(-41))
        assertEquals(4, signalBarsFor(-55))
        assertEquals(3, signalBarsFor(-67))
        assertEquals(2, signalBarsFor(-78))
        assertEquals(1, signalBarsFor(-85))
        assertEquals(0, signalBarsFor(-95))
    }

    @Test
    fun `relative time buckets`() {
        val now = 10_000_000_000L
        assertEquals("just now", formatAgo(now - 30_000, now))
        assertEquals("5 min ago", formatAgo(now - 5 * 60_000, now))
        assertEquals("2h ago", formatAgo(now - 2 * 3_600_000, now))
        assertEquals("3d ago", formatAgo(now - 3 * 86_400_000L, now))
        assertEquals("just now", formatAgo(now + 60_000, now))
    }
}
