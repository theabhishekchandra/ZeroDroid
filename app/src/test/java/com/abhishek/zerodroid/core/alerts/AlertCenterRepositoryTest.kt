package com.abhishek.zerodroid.core.alerts

import com.abhishek.zerodroid.core.database.dao.AlertDao
import com.abhishek.zerodroid.core.database.entity.AlertEntity
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertCenterRepositoryTest {

    private val dao = mockk<AlertDao>()

    private fun entity(id: String, source: String = "ROGUE_AP", severity: String = "HIGH", ts: Long = 1L) =
        AlertEntity(id = id, source = source, severity = severity, title = "t-$id", detail = "d-$id", timestamp = ts)

    @Test
    fun `maps stored rows to unified alerts`() = runBlocking {
        every { dao.observeRecent() } returns flowOf(listOf(entity("a", ts = 10L), entity("b", "GPS_SPOOF", "LOW", 5L)))

        val alerts = AlertCenterRepository(dao).alerts.first()

        assertEquals(2, alerts.size)
        assertEquals(
            UnifiedAlert("a", AlertSource.ROGUE_AP, AlertSeverity.HIGH, "t-a", "d-a", 10L),
            alerts[0]
        )
        assertEquals(AlertSource.GPS_SPOOF, alerts[1].source)
        assertEquals(AlertSeverity.LOW, alerts[1].severity)
    }

    @Test
    fun `rows with unknown source or severity are dropped instead of crashing`() = runBlocking {
        every { dao.observeRecent() } returns flowOf(
            listOf(entity("ok"), entity("bad-source", source = "LASER"), entity("bad-sev", severity = "MEH"))
        )

        val alerts = AlertCenterRepository(dao).alerts.first()

        assertEquals(listOf("ok"), alerts.map { it.id })
    }

    @Test
    fun `record persists the enum names and a fresh id`() = runBlocking {
        every { dao.observeRecent() } returns flowOf(emptyList())
        val inserted = mutableListOf<AlertEntity>()
        coEvery { dao.insert(capture(inserted)) } returns Unit
        val repo = AlertCenterRepository(dao)

        repo.record(AlertSource.BLUETOOTH_TRACKER, AlertSeverity.CRITICAL, "AirTag", "following you", timestamp = 42L)
        repo.record(AlertSource.DEAUTH, AlertSeverity.MEDIUM, "Flood", "x")

        assertEquals(2, inserted.size)
        assertEquals("BLUETOOTH_TRACKER", inserted[0].source)
        assertEquals("CRITICAL", inserted[0].severity)
        assertEquals("AirTag", inserted[0].title)
        assertEquals(42L, inserted[0].timestamp)
        assertNotEquals(inserted[0].id, inserted[1].id)
        assertTrue(inserted[1].timestamp > 42L)
    }

    @Test
    fun `clearAll delegates to the dao`() = runBlocking {
        every { dao.observeRecent() } returns flowOf(emptyList())
        coJustRun { dao.clearAll() }

        AlertCenterRepository(dao).clearAll()

        coVerify(exactly = 1) { dao.clearAll() }
    }

    @Test
    fun `alert sources carry user facing labels`() {
        assertEquals("Tracker Scanner", AlertSource.BLUETOOTH_TRACKER.label)
        assertEquals(5, AlertSource.entries.size)
    }
}
