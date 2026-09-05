package com.abhishek.zerodroid.features.usbcamera.domain

import android.content.Context
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsbCameraModelsTest {

    @Test
    fun `usb video device formats vid pid`() {
        assertEquals("046D:0825", UsbVideoDevice(0x046D, 0x0825, "HD Webcam", "Logitech").vidPid)
    }

    @Test
    fun `camera2 info only has a vid pid when both ids are known`() {
        val base = UsbCameraInfo(cameraId = "2", isExternal = true, deviceName = "External Camera #2")
        assertNull(base.vidPid)
        assertNull(base.copy(vendorId = 0x046D).vidPid)
        assertEquals("046D:0825", base.copy(vendorId = 0x046D, productId = 0x0825).vidPid)
    }

    @Test
    fun `default state is disconnected`() {
        val state = UsbCameraState()
        assertFalse(state.hasUsbHost)
        assertFalse(state.isPreviewActive)
        assertTrue(state.usbVideoDevices.isEmpty())
        assertNull(state.connectedVidPid)
    }

    @Test
    fun `detector without a usb manager finds no video devices`() {
        val detector = UsbCameraDetector(mockk<Context>(relaxed = true), usbManager = null)
        assertTrue(detector.detectUsbVideoDevices().isEmpty())
    }
}
