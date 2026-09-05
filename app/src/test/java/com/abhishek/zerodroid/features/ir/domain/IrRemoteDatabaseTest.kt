package com.abhishek.zerodroid.features.ir.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IrRemoteDatabaseTest {

    @Test
    fun `lists each brand once`() {
        assertEquals(listOf("Samsung", "LG", "Sony"), IrRemoteDatabase.getBrands())
    }

    @Test
    fun `finds profiles by brand and device type`() {
        val samsung = IrRemoteDatabase.getProfile("Samsung", "TV")
        assertNotNull(samsung)
        assertEquals("Samsung", samsung!!.brand)
        assertNull(IrRemoteDatabase.getProfile("Samsung", "Soundbar"))
        assertNull(IrRemoteDatabase.getProfile("Nokia", "TV"))
    }

    @Test
    fun `every profile has the same button layout`() {
        val layouts = IrRemoteDatabase.profiles.map { p -> p.buttons.map { it.label } }
        assertTrue(layouts.all { it == layouts.first() })
        assertEquals(15, layouts.first().size)
        assertTrue(layouts.first().containsAll(listOf("Power", "Vol +", "Vol -", "Mute", "OK", "Back")))
    }

    @Test
    fun `buttons use a single protocol and carrier per brand`() {
        IrRemoteDatabase.profiles.forEach { profile ->
            assertEquals(1, profile.buttons.map { it.protocol }.distinct().size)
            assertEquals(1, profile.buttons.map { it.frequency }.distinct().size)
        }
        assertEquals(IrProtocol.SAMSUNG32, IrRemoteDatabase.getProfile("Samsung", "TV")!!.buttons[0].protocol)
        assertEquals(IrProtocol.NEC, IrRemoteDatabase.getProfile("LG", "TV")!!.buttons[0].protocol)
        assertEquals(40000, IrRemoteDatabase.getProfile("Sony", "TV")!!.buttons[0].frequency)
    }

    @Test
    fun `every button code encodes to a transmit pattern`() {
        IrRemoteDatabase.profiles.flatMap { it.buttons }.forEach { button ->
            val pattern = IrProtocolEncoder.encode(button.protocol, button.code.toString(16))
            assertNotNull("${button.label} for ${button.protocol}", pattern)
            assertTrue(pattern!!.isNotEmpty())
        }
    }
}
