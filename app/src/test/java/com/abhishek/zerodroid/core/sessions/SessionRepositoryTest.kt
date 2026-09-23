package com.abhishek.zerodroid.core.sessions

import com.abhishek.zerodroid.core.database.dao.SessionDao
import com.abhishek.zerodroid.core.database.entity.SessionEntity
import com.abhishek.zerodroid.core.database.entity.SessionItemEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionRepositoryTest {

    private val dao = mockk<SessionDao>(relaxed = true)
    private val repo = SessionRepository(dao)

    @Test
    fun `empty runs are not saved`() = runBlocking {
        assertNull(repo.record("wifi", "WiFi scan", 0, 1, emptyList(), "nothing"))
        coVerify(exactly = 0) { dao.insertWithItems(any(), any()) }
    }

    @Test
    fun `a run is saved with deduplicated items and counted findings`() = runBlocking {
        val session = slot<SessionEntity>()
        val items = slot<List<SessionItemEntity>>()
        coEvery { dao.insertWithItems(capture(session), capture(items)) } returns Unit

        val id = repo.record(
            "ble", "BLE scan", 10, 20,
            listOf(
                SessionItem("a", "A", ItemKind.BLE, -50),
                SessionItem("a", "A again", ItemKind.BLE, -40),
                SessionItem("t", "Tile", ItemKind.TRACKER, -60, flagged = true)
            ),
            "3 devices"
        )

        assertEquals(id, session.captured.id)
        assertEquals(1, session.captured.findingCount)
        assertEquals(listOf("a", "t"), items.captured.map { it.itemKey })
        assertEquals("TRACKER", items.captured[1].kind)
    }

    @Test
    fun `sightings pair each item with its session`() = runBlocking {
        coEvery { dao.sightings("k") } returns listOf(SessionItemEntity(1, "s1", "k", "Tile", "TRACKER", -55, "Tracker", true))
        coEvery { dao.getAll(listOf("s1")) } returns listOf(SessionEntity("s1", "ble", "BLE scan", null, 1, 2, 1, 1, "x"))

        val result = repo.sightings("k")

        assertEquals(1, result.size)
        assertEquals("s1", result[0].first.id)
        assertEquals(ItemKind.TRACKER, result[0].second.kind)
    }

    @Test
    fun `unknown kinds fall back safely`() {
        assertEquals(ItemKind.RF, SessionItemEntity(1, "s", "k", "x", "NOT_A_KIND").toDomain().kind)
    }
}
