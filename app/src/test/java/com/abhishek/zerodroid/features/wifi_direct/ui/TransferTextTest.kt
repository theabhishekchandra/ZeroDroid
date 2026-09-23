package com.abhishek.zerodroid.features.wifi_direct.ui

import com.abhishek.zerodroid.features.wifi_direct.domain.TransferProgress
import com.abhishek.zerodroid.features.wifi_direct.domain.TransferState
import org.junit.Assert.assertEquals
import org.junit.Test

class TransferTextTest {

    private val mb = 1024L * 1024

    @Test
    fun `transferring shows done, total, speed and time left`() {
        val p = TransferProgress(TransferState.Transferring, "capture.pcap", bytesTransferred = 42 * mb, totalBytes = 68 * mb, speedBytesPerSec = 13 * mb / 10 * 10)
        assertEquals("42.0 MB of 68.0 MB · 13.0 MB/s · 2 s left", progressLine(p))
    }

    @Test
    fun `unknown total and speed are left out`() {
        assertEquals("1.5 KB", progressLine(TransferProgress(TransferState.Transferring, bytesTransferred = 1536)))
    }

    @Test
    fun `failures show the reason`() {
        assertEquals("Connection refused", progressLine(TransferProgress(TransferState.Failed, error = "Connection refused")))
        assertEquals("The connection dropped", progressLine(TransferProgress(TransferState.Failed)))
    }

    @Test
    fun `byte units`() {
        assertEquals("900 B", formatBytes(900))
        assertEquals("2.00 GB", formatBytes(2L * 1024 * mb))
    }
}
