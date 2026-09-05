package com.abhishek.zerodroid.features.ble.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class HciSnoopParserTest {

    private class SnoopBuilder(private val datalink: Int = 1002, version: Int = 1, magic: String = "btsnoop") {
        private val bytes = ByteArrayOutputStream()
        private val out = DataOutputStream(bytes)

        init {
            out.write(magic.toByteArray()); out.write(0)
            out.writeInt(version)
            out.writeInt(datalink)
        }

        fun record(data: ByteArray, sent: Boolean = true, commandOrEvent: Boolean = false, ts: Long = 1_000L): SnoopBuilder {
            val flags = (if (sent) 0 else 1) or (if (commandOrEvent) 2 else 0)
            out.writeInt(data.size); out.writeInt(data.size); out.writeInt(flags); out.writeInt(0); out.writeLong(ts)
            out.write(data)
            return this
        }

        /** Writes half a record header so the stream ends mid-record. */
        fun truncatedTail(): SnoopBuilder {
            out.writeInt(4); out.writeInt(4); out.writeShort(0)
            return this
        }

        fun parse(): HciSnoopLog {
            val arr = bytes.toByteArray()
            return HciSnoopParser().parse(ByteArrayInputStream(arr), arr.size.toLong())
        }
    }

    private fun b(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    @Test
    fun `parses header fields and packet count`() {
        val log = SnoopBuilder().record(b(0x01, 0x03, 0x0C, 0x00), commandOrEvent = true).parse()

        assertEquals(1, log.version)
        assertEquals(1002, log.datalinkType)
        assertEquals(1, log.packetCount)
        assertEquals(0, log.packets[0].index)
        assertEquals(1_000L, log.packets[0].timestampMicros)
        assertTrue(log.packets[0].isSent)
        assertTrue(log.packets[0].isCommandOrEvent)
    }

    @Test
    fun `summarises known HCI command by opcode`() {
        val log = SnoopBuilder().record(b(0x01, 0x03, 0x0C, 0x00), commandOrEvent = true).parse()

        assertEquals(HciPacketType.Command, log.packets[0].packetType)
        assertEquals("HCI_Reset (0x0C03) len=0", log.packets[0].summary)
    }

    @Test
    fun `summarises unknown command with OGF and OCF`() {
        val log = SnoopBuilder().record(b(0x01, 0x45, 0xFC, 0x02, 0x00, 0x00), commandOrEvent = true).parse()

        assertEquals("HCI Command OGF=0x3F OCF=0x045 len=2", log.packets[0].summary)
    }

    @Test
    fun `summarises events and LE meta sub events`() {
        val log = SnoopBuilder()
            .record(b(0x04, 0x0E, 0x04, 0x01, 0x03, 0x0C, 0x00), sent = false, commandOrEvent = true)
            .record(b(0x04, 0x3E, 0x0B, 0x02, 0x01), sent = false, commandOrEvent = true)
            .parse()

        assertEquals(HciPacketType.Event, log.packets[0].packetType)
        assertEquals("Command_Complete (0x0E) len=4", log.packets[0].summary)
        assertEquals("LE Meta: LE_Advertising_Report (sub=0x02) len=11", log.packets[1].summary)
    }

    @Test
    fun `decodes ATT write request inside ACL and L2CAP framing`() {
        // H4 type 0x02, handle 0x0040 (PB flags in high bits), ACL len 8, L2CAP len 4, CID 0x0004,
        // ATT Write Req (0x12) attr handle 0x0025, value 2 bytes
        val data = b(0x02, 0x40, 0x20, 0x08, 0x00, 0x04, 0x00, 0x04, 0x00, 0x12, 0x25, 0x00, 0xAA, 0xBB)
        val log = SnoopBuilder().record(data).parse()

        assertEquals(HciPacketType.AclData, log.packets[0].packetType)
        assertEquals("ATT Write Req (attr=0x0025 2 bytes) conn=0x040", log.packets[0].summary)
    }

    @Test
    fun `decodes ATT MTU exchange notification and error response`() {
        fun att(vararg att: Int): ByteArray {
            val payload = b(*att)
            val l2 = payload.size
            return b(0x02, 0x01, 0x00, l2 + 4, 0x00, l2, 0x00, 0x04, 0x00) + payload
        }
        val log = SnoopBuilder()
            .record(att(0x02, 0xF7, 0x00))
            .record(att(0x1B, 0x10, 0x00, 0x01, 0x02, 0x03), sent = false)
            .record(att(0x01, 0x0A, 0x11, 0x00, 0x0A), sent = false)
            .parse()

        assertEquals("ATT Exchange MTU Req (mtu=247) conn=0x001", log.packets[0].summary)
        assertEquals("ATT Notification (attr=0x0010 3 bytes) conn=0x001", log.packets[1].summary)
        assertEquals("ATT Error: Attribute Not Found (req=0x0A attr=0x0011) conn=0x001", log.packets[2].summary)
    }

    @Test
    fun `labels SMP and signaling channels`() {
        val smp = b(0x02, 0x01, 0x00, 0x05, 0x00, 0x01, 0x00, 0x06, 0x00, 0x01)
        val log = SnoopBuilder().record(smp).parse()

        assertEquals("SMP handle=0x001 len=1", log.packets[0].summary)
    }

    @Test
    fun `unencapsulated datalink classifies by flags`() {
        val log = SnoopBuilder(datalink = 1001)
            .record(b(0x03, 0x0C, 0x00), sent = true, commandOrEvent = true)
            .record(b(0x0E, 0x00), sent = false, commandOrEvent = true)
            .record(b(0x01, 0x00, 0x00, 0x00), sent = true, commandOrEvent = false)
            .parse()

        assertEquals(HciPacketType.Command, log.packets[0].packetType)
        assertEquals(HciPacketType.Event, log.packets[1].packetType)
        assertEquals(HciPacketType.AclData, log.packets[2].packetType)
    }

    @Test
    fun `truncated packets produce truncated summaries not crashes`() {
        val log = SnoopBuilder()
            .record(b(0x01, 0x03), commandOrEvent = true)
            .record(b(0x02, 0x01), sent = false)
            .record(b(0x09, 0x00))
            .parse()

        assertEquals("HCI Command (truncated)", log.packets[0].summary)
        assertEquals("ACL Data (truncated)", log.packets[1].summary)
        assertEquals(HciPacketType.Unknown, log.packets[2].packetType)
    }

    @Test
    fun `rejects bad magic wrong version and short files`() {
        assertThrows(IllegalArgumentException::class.java) { SnoopBuilder(magic = "btsnoot").parse() }
        assertThrows(IllegalArgumentException::class.java) { SnoopBuilder(version = 2).parse() }
        assertThrows(IllegalArgumentException::class.java) {
            HciSnoopParser().parse(ByteArrayInputStream(ByteArray(4)), 4)
        }
    }

    @Test
    fun `stops at an incomplete trailing record`() {
        val log = SnoopBuilder()
            .record(b(0x01, 0x03, 0x0C, 0x00), commandOrEvent = true)
            .truncatedTail()
            .parse()

        assertEquals(1, log.packetCount)
    }

    @Test
    fun `hex dump formats offsets bytes and ascii`() {
        assertEquals("(empty)", ByteArray(0).toHexDump())

        val dump = "Hi!".toByteArray().toHexDump()
        assertTrue(dump.startsWith("0000  48 69 21 "))
        assertTrue(dump.endsWith(" Hi!"))

        val two = ByteArray(17) { 0x41 }.toHexDump()
        assertEquals(2, two.lines().size)
        assertTrue(two.lines()[1].startsWith("0010  41"))
    }
}
