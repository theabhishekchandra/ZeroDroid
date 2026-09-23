package com.abhishek.zerodroid.core.sessions

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExportFormat(val label: String, val description: String, val extension: String, val mime: String) {
    PDF("PDF report", "Readable summary with findings and advice", "pdf", "application/pdf"),
    JSON("JSON", "Everything, for scripts and re-import", "json", "application/json"),
    CSV("CSV", "One row per device or network", "csv", "text/csv")
}

/** A session with its items, the unit every export format works on. */
data class SessionExport(val session: Session, val items: List<SessionItem>)

/**
 * Turns sessions into files. The text formats are pure so they can be unit tested; only
 * [write] touches the file system.
 */
object SessionExporter {

    private val MAC = Regex("""\b([0-9A-Fa-f]{2}(?::[0-9A-Fa-f]{2}){2})(?::[0-9A-Fa-f]{2}){3}\b""")
    private val isoTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    private val readableTime = SimpleDateFormat("EEE d MMM yyyy, HH:mm", Locale.US)

    /** Keeps the vendor prefix of every MAC address in [text] and hides the device-specific half. */
    fun redactMacs(text: String): String = MAC.replace(text) { "${it.groupValues[1]}:XX:XX:XX" }

    fun toJson(exports: List<SessionExport>, redact: Boolean): String {
        val clean: (String) -> String = { if (redact) redactMacs(it) else it }
        val root = JSONObject()
            .put("format", "zerodroid-sessions")
            .put("version", 1)
            .put("sessions", JSONArray().apply {
                exports.forEach { (s, items) ->
                    put(
                        JSONObject()
                            .put("id", s.id)
                            .put("tool", s.tool)
                            .put("title", s.title)
                            .put("place", s.place ?: JSONObject.NULL)
                            .put("startedAt", isoTime.format(Date(s.startedAt)))
                            .put("endedAt", isoTime.format(Date(s.endedAt)))
                            .put("summary", clean(s.summary))
                            .put("findings", s.findingCount)
                            .put("items", JSONArray().apply {
                                items.forEach { i ->
                                    put(
                                        JSONObject()
                                            .put("key", clean(i.key))
                                            .put("label", clean(i.label))
                                            .put("kind", i.kind.name)
                                            .put("rssi", i.rssi ?: JSONObject.NULL)
                                            .put("detail", clean(i.detail))
                                            .put("flagged", i.flagged)
                                    )
                                }
                            })
                    )
                }
            })
        return root.toString(2)
    }

    fun toCsv(exports: List<SessionExport>, redact: Boolean): String {
        val clean: (String) -> String = { if (redact) redactMacs(it) else it }
        val header = "session,tool,started,kind,key,label,rssi,detail,flagged"
        val rows = exports.flatMap { (s, items) ->
            items.map { i ->
                listOf(
                    s.title,
                    s.tool,
                    isoTime.format(Date(s.startedAt)),
                    i.kind.name,
                    clean(i.key),
                    clean(i.label),
                    i.rssi?.toString().orEmpty(),
                    clean(i.detail),
                    i.flagged.toString()
                ).joinToString(",") { csvField(it) }
            }
        }
        return (listOf(header) + rows).joinToString("\n")
    }

    /** Quotes a CSV field when it contains a comma, quote or newline. */
    fun csvField(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' }) "\"" + value.replace("\"", "\"\"") + "\"" else value

    fun fileName(exports: List<SessionExport>, format: ExportFormat): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date(exports.maxOfOrNull { it.session.startedAt } ?: 0L))
        val what = if (exports.size == 1) exports.first().session.tool else "${exports.size}_sessions"
        return "zerodroid_${what}_$stamp.${format.extension}"
    }

    /** Writes the export into the shared cache folder and returns a content Uri to share or save. */
    fun write(context: Context, exports: List<SessionExport>, format: ExportFormat, redact: Boolean): Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName(exports, format))
        when (format) {
            ExportFormat.JSON -> file.writeText(toJson(exports, redact))
            ExportFormat.CSV -> file.writeText(toCsv(exports, redact))
            ExportFormat.PDF -> writePdf(file, exports, redact)
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun writePdf(file: File, exports: List<SessionExport>, redact: Boolean) {
        val clean: (String) -> String = { if (redact) redactMacs(it) else it }
        val pageWidth = 595
        val pageHeight = 842
        val margin = 48f
        val doc = PdfDocument()
        val title = Paint().apply { textSize = 18f; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) }
        val heading = Paint().apply { textSize = 13f; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) }
        val body = Paint().apply { textSize = 10f; typeface = Typeface.SANS_SERIF }
        val flag = Paint(body).apply { color = 0xFFB3261E.toInt(); typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }

        var pageNumber = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var y = margin
        fun line(text: String, paint: Paint, gap: Float = 6f) {
            if (y > pageHeight - margin) {
                doc.finishPage(page)
                page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, ++pageNumber).create())
                y = margin
            }
            y += paint.textSize
            page.canvas.drawText(text.take(95), margin, y, paint)
            y += gap
        }

        line("ZeroDroid report", title, 4f)
        line("Generated ${readableTime.format(Date())} · ${exports.size} session(s)" + if (redact) " · MACs redacted" else "", body, 14f)
        exports.forEach { (s, items) ->
            line("${s.title}${s.place?.let { " · $it" } ?: ""}", heading, 2f)
            line("${readableTime.format(Date(s.startedAt))} · ${items.size} items · ${s.findingCount} findings", body, 2f)
            line(clean(s.summary), body, 8f)
            items.filter { it.flagged }.forEach { line("! ${clean(it.label)} — ${clean(it.detail)}", flag, 3f) }
            items.filterNot { it.flagged }.forEach { i ->
                line("  ${clean(i.label)}  ${i.rssi?.let { "$it dBm" } ?: ""}  ${clean(i.detail)}", body, 3f)
            }
            y += 10f
        }
        line("Findings are indicators, not proof. Only scan networks and devices you own or may test.", body)
        doc.finishPage(page)
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
    }
}
