package com.abhishek.zerodroid.core.sessions

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionExporterTest {

    private val session = Session("s1", "wifi", "WiFi scan", "Office", 1_000L, 61_000L, 2, 1, "2 networks · 1 weak")
    private val items = listOf(
        SessionItem("A4:2B:B0:11:22:7F", "Home_5G", ItemKind.WIFI, -41, "WPA3 · ch 36"),
        SessionItem("78:8A:20:9A:10:C4", "Cafe, \"Guest\"", ItemKind.WIFI, -67, "Open · ch 6", flagged = true)
    )
    private val export = listOf(SessionExport(session, items))

    @Test
    fun `redaction keeps the vendor prefix only`() {
        assertEquals("seen A4:2B:B0:XX:XX:XX today", SessionExporter.redactMacs("seen A4:2B:B0:11:22:7F today"))
        assertEquals("no mac here", SessionExporter.redactMacs("no mac here"))
    }

    @Test
    fun `json carries sessions and items and honours redaction`() {
        val json = JSONObject(SessionExporter.toJson(export, redact = true))
        val s = json.getJSONArray("sessions").getJSONObject(0)
        assertEquals("wifi", s.getString("tool"))
        assertEquals(2, s.getJSONArray("items").length())
        assertEquals("A4:2B:B0:XX:XX:XX", s.getJSONArray("items").getJSONObject(0).getString("key"))
        assertTrue(s.getJSONArray("items").getJSONObject(1).getBoolean("flagged"))
    }

    @Test
    fun `csv has a header, one row per item and quotes awkward fields`() {
        val csv = SessionExporter.toCsv(export, redact = false).lines()
        assertEquals(3, csv.size)
        assertTrue(csv[0].startsWith("session,tool,started"))
        assertTrue(csv[2], csv[2].contains("\"Cafe, \"\"Guest\"\"\""))
        assertTrue(csv[1].contains("A4:2B:B0:11:22:7F"))
    }

    @Test
    fun `csv fields are only quoted when needed`() {
        assertEquals("plain", SessionExporter.csvField("plain"))
        assertEquals("\"a,b\"", SessionExporter.csvField("a,b"))
        assertFalse(SessionExporter.csvField("x").startsWith("\""))
    }

    @Test
    fun `file names describe the export`() {
        assertTrue(SessionExporter.fileName(export, ExportFormat.PDF).matches(Regex("zerodroid_wifi_\\d{8}_\\d{4}\\.pdf")))
        assertTrue(SessionExporter.fileName(export + export, ExportFormat.CSV).startsWith("zerodroid_2_sessions_"))
    }
}
