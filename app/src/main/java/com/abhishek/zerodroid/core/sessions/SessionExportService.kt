package com.abhishek.zerodroid.core.sessions

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Loads sessions with their items and writes them in the chosen format, off the main thread. */
@Singleton
class SessionExportService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: SessionRepository
) {
    suspend fun export(ids: List<String>, format: ExportFormat, redactMacs: Boolean): Uri = withContext(Dispatchers.IO) {
        val exports = ids.mapNotNull { id -> repository.get(id)?.let { SessionExport(it, repository.items(id)) } }
            .sortedBy { it.session.startedAt }
        SessionExporter.write(context, exports, format, redactMacs)
    }
}
