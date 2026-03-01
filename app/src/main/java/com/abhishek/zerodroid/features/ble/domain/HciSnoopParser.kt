package com.abhishek.zerodroid.features.ble.domain

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class HciPacket(
    val index: Int,
    val originalLength: Int,
    val includedLength: Int,
    val isSent: Boolean,
    val isCommandOrEvent: Boolean,
    val timestampMicros: Long,
    val packetType: HciPacketType,
    val data: ByteArray,
    val summary: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HciPacket) return false
        return index == other.index &&
                originalLength == other.originalLength &&
                includedLength == other.includedLength &&
                isSent == other.isSent &&
                isCommandOrEvent == other.isCommandOrEvent &&
                timestampMicros == other.timestampMicros &&
                packetType == other.packetType &&
                data.contentEquals(other.data) &&
                summary == other.summary
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + originalLength
        result = 31 * result + includedLength
        result = 31 * result + isSent.hashCode()
        result = 31 * result + isCommandOrEvent.hashCode()
        result = 31 * result + timestampMicros.hashCode()
        result = 31 * result + packetType.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + summary.hashCode()
        return result
    }
}

enum class HciPacketType(val label: String) {
    Command("HCI CMD"),
    AclData("ACL DATA"),
    Event("HCI EVT"),
    Unknown("UNKNOWN")
}

data class HciSnoopLog(
    val packets: List<HciPacket>,
    val version: Int,
    val datalinkType: Int,
    val fileSize: Long,
    val packetCount: Int
)

class HciSnoopParser {

    companion object {
        private const val HEADER_SIZE = 16
        private const val RECORD_HEADER_SIZE = 24
        private const val MAX_PACKETS = 5000

        private val BTSNOOP_MAGIC = byteArrayOf(
            'b'.code.toByte(), 't'.code.toByte(), 's'.code.toByte(), 'n'.code.toByte(),
            'o'.code.toByte(), 'o'.code.toByte(), 'p'.code.toByte(), 0x00
        )

        // ATT opcodes
        private const val ATT_ERROR_RSP: Byte = 0x01
        private const val ATT_EXCHANGE_MTU_REQ: Byte = 0x02
        private const val ATT_EXCHANGE_MTU_RSP: Byte = 0x03
        private const val ATT_FIND_INFO_REQ: Byte = 0x04
        private const val ATT_FIND_INFO_RSP: Byte = 0x05
        private const val ATT_FIND_BY_TYPE_REQ: Byte = 0x06
        private const val ATT_FIND_BY_TYPE_RSP: Byte = 0x07
        private const val ATT_READ_BY_TYPE_REQ: Byte = 0x08
        private const val ATT_READ_BY_TYPE_RSP: Byte = 0x09
        private const val ATT_READ_REQ: Byte = 0x0A
        private const val ATT_READ_RSP: Byte = 0x0B
        private const val ATT_READ_BY_GROUP_REQ: Byte = 0x10
        private const val ATT_READ_BY_GROUP_RSP: Byte = 0x11
        private const val ATT_WRITE_REQ: Byte = 0x12
        private const val ATT_WRITE_RSP: Byte = 0x13
        private const val ATT_WRITE_CMD: Byte = 0x52
        private const val ATT_HANDLE_VALUE_NTF: Byte = 0x1B
        private const val ATT_HANDLE_VALUE_IND: Byte = 0x1D
        private const val ATT_HANDLE_VALUE_CFM: Byte = 0x1E

        // HCI packet type indicators (datalink type H4)
        private const val HCI_COMMAND: Byte = 0x01
        private const val HCI_ACL_DATA: Byte = 0x02
        private const val HCI_EVENT: Byte = 0x04

        // Common HCI command opcodes (OGF << 10 | OCF)
        private val HCI_COMMAND_NAMES = mapOf(
            0x0401 to "HCI_Inquiry",
            0x0406 to "HCI_Disconnect",
            0x040D to "HCI_Create_Connection",
            0x0C03 to "HCI_Reset",
            0x0C13 to "HCI_Change_Local_Name",
            0x0C1A to "HCI_Write_Scan_Enable",
            0x1001 to "HCI_Read_Local_Version",
            0x1009 to "HCI_Read_BD_ADDR",
            0x2001 to "LE_Set_Event_Mask",
            0x2005 to "LE_Set_Random_Address",
            0x2006 to "LE_Set_Adv_Parameters",
            0x2008 to "LE_Set_Adv_Data",
            0x200A to "LE_Set_Adv_Enable",
            0x200B to "LE_Set_Scan_Parameters",
            0x200C to "LE_Set_Scan_Enable",
            0x200D to "LE_Create_Connection",
            0x200E to "LE_Create_Connection_Cancel",
            0x2016 to "LE_Read_Remote_Features",
            0x2018 to "LE_Start_Encryption"
        )

        // Common HCI event codes
        private val HCI_EVENT_NAMES = mapOf(
            0x05 to "Disconnection_Complete",
            0x08 to "Encryption_Change",
            0x0E to "Command_Complete",
            0x0F to "Command_Status",
            0x13 to "Number_Of_Completed_Packets",
            0x3E to "LE_Meta_Event"
        )

        // LE sub-event codes
        private val LE_SUBEVENT_NAMES = mapOf(
            0x01 to "LE_Connection_Complete",
            0x02 to "LE_Advertising_Report",
            0x03 to "LE_Connection_Update_Complete",
            0x04 to "LE_Read_Remote_Features_Complete",
            0x05 to "LE_Long_Term_Key_Request",
            0x0A to "LE_Enhanced_Connection_Complete",
            0x0D to "LE_Extended_Advertising_Report"
        )
    }

    fun parse(inputStream: InputStream, fileSize: Long): HciSnoopLog {
        val headerBytes = ByteArray(HEADER_SIZE)
        val bytesRead = inputStream.read(headerBytes)
        if (bytesRead < HEADER_SIZE) {
            throw IllegalArgumentException("File too small to contain btsnoop header")
        }

        // Validate magic
        for (i in 0 until 8) {
            if (headerBytes[i] != BTSNOOP_MAGIC[i]) {
                throw IllegalArgumentException(
                    "Invalid btsnoop file: magic mismatch at byte $i " +
                            "(expected 0x${String.format("%02X", BTSNOOP_MAGIC[i])}, " +
                            "got 0x${String.format("%02X", headerBytes[i])})"
                )
            }
        }

        val headerBuf = ByteBuffer.wrap(headerBytes).order(ByteOrder.BIG_ENDIAN)
        headerBuf.position(8)
        val version = headerBuf.int
        val datalinkType = headerBuf.int

        if (version != 1) {
            throw IllegalArgumentException("Unsupported btsnoop version: $version (only v1 supported)")
        }

        val packets = mutableListOf<HciPacket>()
        val recordHeader = ByteArray(RECORD_HEADER_SIZE)
        var packetIndex = 0

        while (packetIndex < MAX_PACKETS) {
            val headerRead = readFully(inputStream, recordHeader)
            if (headerRead < RECORD_HEADER_SIZE) break

            val recBuf = ByteBuffer.wrap(recordHeader).order(ByteOrder.BIG_ENDIAN)
            val originalLength = recBuf.int
            val includedLength = recBuf.int
            val flags = recBuf.int
            @Suppress("UNUSED_VARIABLE")
            val cumulativeDrops = recBuf.int
            val timestampMicros = recBuf.long

            if (includedLength < 0 || includedLength > 65536) {
                break // Corrupt or unexpected data
            }

            val packetData = ByteArray(includedLength)
            val dataRead = readFully(inputStream, packetData)
            if (dataRead < includedLength) break

            val isSent = (flags and 0x01) == 0
            val isCommandOrEvent = (flags and 0x02) != 0

            val packetType = classifyPacket(packetData, datalinkType, isCommandOrEvent, isSent)
            val summary = buildSummary(packetData, packetType, isSent, datalinkType)

            packets.add(
                HciPacket(
                    index = packetIndex,
                    originalLength = originalLength,
                    includedLength = includedLength,
                    isSent = isSent,
                    isCommandOrEvent = isCommandOrEvent,
                    timestampMicros = timestampMicros,
                    packetType = packetType,
                    data = packetData,
                    summary = summary
                )
            )
            packetIndex++
        }

        return HciSnoopLog(
            packets = packets,
            version = version,
            datalinkType = datalinkType,
            fileSize = fileSize,
            packetCount = packets.size
        )
    }

    private fun readFully(input: InputStream, buffer: ByteArray): Int {
        var offset = 0
        while (offset < buffer.size) {
            val read = input.read(buffer, offset, buffer.size - offset)
            if (read == -1) return offset
            offset += read
        }
        return offset
    }

    private fun classifyPacket(
        data: ByteArray,
        datalinkType: Int,
        isCommandOrEvent: Boolean,
        isSent: Boolean
    ): HciPacketType {
        // Datalink type 1002 = HCI UART (H4) - first byte is packet type indicator
        // Datalink type 1001 = HCI unencapsulated - no type byte, use flags
        return when (datalinkType) {
            1002 -> {
                if (data.isEmpty()) return HciPacketType.Unknown
                when (data[0]) {
                    HCI_COMMAND -> HciPacketType.Command
                    HCI_ACL_DATA -> HciPacketType.AclData
                    HCI_EVENT -> HciPacketType.Event
                    else -> HciPacketType.Unknown
                }
            }
            1001 -> {
                if (isCommandOrEvent) {
                    if (isSent) HciPacketType.Command else HciPacketType.Event
                } else {
                    HciPacketType.AclData
                }
            }
            else -> HciPacketType.Unknown
        }
    }

    private fun buildSummary(
        data: ByteArray,
        packetType: HciPacketType,
        isSent: Boolean,
        datalinkType: Int
    ): String {
        val offset = if (datalinkType == 1002) 1 else 0 // Skip H4 type byte

        return when (packetType) {
            HciPacketType.Command -> summarizeCommand(data, offset)
            HciPacketType.Event -> summarizeEvent(data, offset)
            HciPacketType.AclData -> summarizeAcl(data, offset, isSent)
            HciPacketType.Unknown -> "Unknown packet (${data.size} bytes)"
        }
    }

    private fun summarizeCommand(data: ByteArray, offset: Int): String {
        if (data.size < offset + 3) return "HCI Command (truncated)"

        val opcode = ((data[offset + 1].toInt() and 0xFF) shl 8) or
                (data[offset].toInt() and 0xFF)
        val paramLen = data[offset + 2].toInt() and 0xFF
        val ogf = (opcode shr 10) and 0x3F
        val ocf = opcode and 0x3FF

        val name = HCI_COMMAND_NAMES[opcode]
        return if (name != null) {
            "$name (0x${String.format("%04X", opcode)}) len=$paramLen"
        } else {
            "HCI Command OGF=0x${String.format("%02X", ogf)} OCF=0x${String.format("%03X", ocf)} len=$paramLen"
        }
    }

    private fun summarizeEvent(data: ByteArray, offset: Int): String {
        if (data.size < offset + 2) return "HCI Event (truncated)"

        val eventCode = data[offset].toInt() and 0xFF
        val paramLen = data[offset + 1].toInt() and 0xFF

        val name = HCI_EVENT_NAMES[eventCode]

        // Handle LE Meta Event sub-events
        if (eventCode == 0x3E && data.size > offset + 2) {
            val subEvent = data[offset + 2].toInt() and 0xFF
            val subName = LE_SUBEVENT_NAMES[subEvent]
            return if (subName != null) {
                "LE Meta: $subName (sub=0x${String.format("%02X", subEvent)}) len=$paramLen"
            } else {
                "LE Meta: sub=0x${String.format("%02X", subEvent)} len=$paramLen"
            }
        }

        return if (name != null) {
            "$name (0x${String.format("%02X", eventCode)}) len=$paramLen"
        } else {
            "HCI Event 0x${String.format("%02X", eventCode)} len=$paramLen"
        }
    }

    private fun summarizeAcl(data: ByteArray, offset: Int, isSent: Boolean): String {
        // ACL header: handle (2 bytes, 12 bits handle + 2 bits PB + 2 bits BC), length (2 bytes)
        if (data.size < offset + 4) return "ACL Data (truncated)"

        val handleRaw = ((data[offset + 1].toInt() and 0xFF) shl 8) or
                (data[offset].toInt() and 0xFF)
        val connectionHandle = handleRaw and 0x0FFF
        val aclLength = ((data[offset + 3].toInt() and 0xFF) shl 8) or
                (data[offset + 2].toInt() and 0xFF)

        val aclPayloadStart = offset + 4

        // Try to parse L2CAP header: length (2 bytes) + CID (2 bytes)
        if (data.size < aclPayloadStart + 4) {
            return "ACL handle=0x${String.format("%03X", connectionHandle)} len=$aclLength"
        }

        val l2capLength = ((data[aclPayloadStart + 1].toInt() and 0xFF) shl 8) or
                (data[aclPayloadStart].toInt() and 0xFF)
        val l2capCid = ((data[aclPayloadStart + 3].toInt() and 0xFF) shl 8) or
                (data[aclPayloadStart + 2].toInt() and 0xFF)

        val l2capPayloadStart = aclPayloadStart + 4

        // CID 0x0004 = ATT, CID 0x0005 = LE Signaling, CID 0x0006 = SMP
        return when (l2capCid) {
            0x0004 -> summarizeAtt(data, l2capPayloadStart, connectionHandle, isSent)
            0x0005 -> "L2CAP LE Signaling handle=0x${String.format("%03X", connectionHandle)} len=$l2capLength"
            0x0006 -> "SMP handle=0x${String.format("%03X", connectionHandle)} len=$l2capLength"
            0x0001 -> "L2CAP Signaling handle=0x${String.format("%03X", connectionHandle)} len=$l2capLength"
            else -> "L2CAP CID=0x${String.format("%04X", l2capCid)} handle=0x${String.format("%03X", connectionHandle)} len=$l2capLength"
        }
    }

    private fun summarizeAtt(
        data: ByteArray,
        attStart: Int,
        connectionHandle: Int,
        isSent: Boolean
    ): String {
        if (data.size <= attStart) {
            return "ATT (empty) handle=0x${String.format("%03X", connectionHandle)}"
        }

        val opcode = data[attStart]
        val handleStr = "conn=0x${String.format("%03X", connectionHandle)}"

        return when (opcode) {
            ATT_ERROR_RSP -> {
                if (data.size >= attStart + 5) {
                    val reqOpcode = data[attStart + 1].toInt() and 0xFF
                    val attHandle = readUint16LE(data, attStart + 2)
                    val errorCode = data[attStart + 4].toInt() and 0xFF
                    val errorName = attErrorName(errorCode)
                    "ATT Error: $errorName (req=0x${String.format("%02X", reqOpcode)} " +
                            "attr=0x${String.format("%04X", attHandle)}) $handleStr"
                } else {
                    "ATT Error Response $handleStr"
                }
            }
            ATT_EXCHANGE_MTU_REQ -> {
                val mtu = if (data.size >= attStart + 3) readUint16LE(data, attStart + 1) else 0
                "ATT Exchange MTU Req (mtu=$mtu) $handleStr"
            }
            ATT_EXCHANGE_MTU_RSP -> {
                val mtu = if (data.size >= attStart + 3) readUint16LE(data, attStart + 1) else 0
                "ATT Exchange MTU Rsp (mtu=$mtu) $handleStr"
            }
            ATT_FIND_INFO_REQ -> {
                if (data.size >= attStart + 5) {
                    val startHandle = readUint16LE(data, attStart + 1)
                    val endHandle = readUint16LE(data, attStart + 3)
                    "ATT Find Info Req (0x${String.format("%04X", startHandle)}-0x${String.format("%04X", endHandle)}) $handleStr"
                } else {
                    "ATT Find Info Req $handleStr"
                }
            }
            ATT_FIND_INFO_RSP -> {
                "ATT Find Info Rsp (${data.size - attStart - 1} bytes) $handleStr"
            }
            ATT_FIND_BY_TYPE_REQ -> "ATT Find By Type Value Req $handleStr"
            ATT_FIND_BY_TYPE_RSP -> "ATT Find By Type Value Rsp $handleStr"
            ATT_READ_BY_TYPE_REQ -> {
                if (data.size >= attStart + 5) {
                    val startHandle = readUint16LE(data, attStart + 1)
                    val endHandle = readUint16LE(data, attStart + 3)
                    "ATT Read By Type Req (0x${String.format("%04X", startHandle)}-0x${String.format("%04X", endHandle)}) $handleStr"
                } else {
                    "ATT Read By Type Req $handleStr"
                }
            }
            ATT_READ_BY_TYPE_RSP -> {
                "ATT Read By Type Rsp (${data.size - attStart - 1} bytes) $handleStr"
            }
            ATT_READ_REQ -> {
                val attrHandle = if (data.size >= attStart + 3) {
                    "attr=0x${String.format("%04X", readUint16LE(data, attStart + 1))}"
                } else ""
                "ATT Read Req ($attrHandle) $handleStr"
            }
            ATT_READ_RSP -> {
                val valueLen = data.size - attStart - 1
                "ATT Read Rsp ($valueLen bytes) $handleStr"
            }
            ATT_READ_BY_GROUP_REQ -> {
                if (data.size >= attStart + 5) {
                    val startHandle = readUint16LE(data, attStart + 1)
                    val endHandle = readUint16LE(data, attStart + 3)
                    "ATT Read By Group Req (0x${String.format("%04X", startHandle)}-0x${String.format("%04X", endHandle)}) $handleStr"
                } else {
                    "ATT Read By Group Req $handleStr"
                }
            }
            ATT_READ_BY_GROUP_RSP -> {
                "ATT Read By Group Rsp (${data.size - attStart - 1} bytes) $handleStr"
            }
            ATT_WRITE_REQ -> {
                if (data.size >= attStart + 3) {
                    val attrHandle = readUint16LE(data, attStart + 1)
                    val valueLen = data.size - attStart - 3
                    "ATT Write Req (attr=0x${String.format("%04X", attrHandle)} $valueLen bytes) $handleStr"
                } else {
                    "ATT Write Req $handleStr"
                }
            }
            ATT_WRITE_RSP -> "ATT Write Rsp $handleStr"
            ATT_WRITE_CMD -> {
                if (data.size >= attStart + 3) {
                    val attrHandle = readUint16LE(data, attStart + 1)
                    val valueLen = data.size - attStart - 3
                    "ATT Write Cmd (attr=0x${String.format("%04X", attrHandle)} $valueLen bytes) $handleStr"
                } else {
                    "ATT Write Cmd $handleStr"
                }
            }
            ATT_HANDLE_VALUE_NTF -> {
                if (data.size >= attStart + 3) {
                    val attrHandle = readUint16LE(data, attStart + 1)
                    val valueLen = data.size - attStart - 3
                    "ATT Notification (attr=0x${String.format("%04X", attrHandle)} $valueLen bytes) $handleStr"
                } else {
                    "ATT Notification $handleStr"
                }
            }
            ATT_HANDLE_VALUE_IND -> {
                if (data.size >= attStart + 3) {
                    val attrHandle = readUint16LE(data, attStart + 1)
                    val valueLen = data.size - attStart - 3
                    "ATT Indication (attr=0x${String.format("%04X", attrHandle)} $valueLen bytes) $handleStr"
                } else {
                    "ATT Indication $handleStr"
                }
            }
            ATT_HANDLE_VALUE_CFM -> "ATT Confirmation $handleStr"
            else -> {
                "ATT opcode=0x${String.format("%02X", opcode.toInt() and 0xFF)} " +
                        "(${data.size - attStart} bytes) $handleStr"
            }
        }
    }

    private fun readUint16LE(data: ByteArray, offset: Int): Int {
        return ((data[offset + 1].toInt() and 0xFF) shl 8) or
                (data[offset].toInt() and 0xFF)
    }

    private fun attErrorName(code: Int): String = when (code) {
        0x01 -> "Invalid Handle"
        0x02 -> "Read Not Permitted"
        0x03 -> "Write Not Permitted"
        0x05 -> "Authentication"
        0x06 -> "Request Not Supported"
        0x07 -> "Invalid Offset"
        0x0A -> "Attribute Not Found"
        0x0E -> "Unlikely Error"
        0x0F -> "Insufficient Encryption"
        0x10 -> "Unsupported Group Type"
        0x11 -> "Insufficient Resources"
        else -> "0x${String.format("%02X", code)}"
    }
}

fun ByteArray.toHexDump(bytesPerLine: Int = 16): String {
    if (isEmpty()) return "(empty)"
    val sb = StringBuilder()
    for (i in indices step bytesPerLine) {
        // Offset
        sb.append(String.format("%04X  ", i))

        // Hex bytes
        for (j in 0 until bytesPerLine) {
            if (i + j < size) {
                sb.append(String.format("%02X ", this[i + j].toInt() and 0xFF))
            } else {
                sb.append("   ")
            }
            if (j == 7) sb.append(" ") // Extra space in middle
        }

        sb.append(" ")

        // ASCII sidebar
        for (j in 0 until bytesPerLine) {
            if (i + j < size) {
                val b = this[i + j].toInt() and 0xFF
                sb.append(if (b in 0x20..0x7E) b.toChar() else '.')
            }
        }

        if (i + bytesPerLine < size) sb.append('\n')
    }
    return sb.toString()
}
