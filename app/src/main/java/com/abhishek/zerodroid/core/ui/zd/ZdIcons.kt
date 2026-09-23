package com.abhishek.zerodroid.core.ui.zd

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Stroke icons from the redesign, drawn on a 24×24 grid with round caps and joins.
 * Tint them with `Icon(tint = …)` like any other vector.
 */
object ZdIcons {
    private fun stroke(name: String, pathData: String, width: Float = 1.75f): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(
            pathData = addPathNodes(pathData),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ).build()

    // Navigation and chrome
    val Back by lazy { stroke("Back", "M15 6l-6 6 6 6") }
    val Chevron by lazy { stroke("Chevron", "M9 6l6 6-6 6") }
    val ChevronDown by lazy { stroke("ChevronDown", "M6 9l6 6 6-6") }
    val Close by lazy { stroke("Close", "M6 6l12 12M18 6L6 18") }
    val Home by lazy { stroke("Home", "M4 11l8-7 8 7v9h-5v-6H9v6H4z") }
    val Grid by lazy { stroke("Grid", "M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h6v6h-6z") }
    val Sweep by lazy { stroke("Sweep", "M12 12l6-6M12 3a9 9 0 1 0 9 9M12 7a5 5 0 1 0 5 5") }
    val Bell by lazy { stroke("Bell", "M6 16v-5a6 6 0 0 1 12 0v5l2 2H4l2-2zM10 21h4") }
    val Clock by lazy { stroke("Clock", "M12 7v5l3 2M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z") }
    val Search by lazy { stroke("Search", "M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14zM20 20l-4-4") }
    val Settings by lazy { stroke("Settings", "M4 7h10M18 7h2M4 17h4M12 17h8M16 5v4M10 15v4") }
    val Help by lazy { stroke("Help", "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18zM9.5 9.5a2.5 2.5 0 1 1 3.5 2.3c-.6.3-1 .8-1 1.5V14M12 17h.01") }
    val Share by lazy { stroke("Share", "M12 3v12M7 8l5-5 5 5M5 14v6h14v-6") }
    val Download by lazy { stroke("Download", "M12 4v11M7 10l5 5 5-5M5 20h14") }
    val Copy by lazy { stroke("Copy", "M9 9h11v11H9zM5 15H4V4h11v1") }
    val Refresh by lazy { stroke("Refresh", "M20 11a8 8 0 1 0-2.3 5.7M20 5v6h-6") }
    val Plus by lazy { stroke("Plus", "M12 5v14M5 12h14") }
    val More by lazy { stroke("More", "M5 12h.01M12 12h.01M19 12h.01", width = 2.5f) }
    val Filter by lazy { stroke("Filter", "M4 5h16l-6 8v5l-4 2v-7z") }
    val Pin by lazy { stroke("Pin", "M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9z") }
    val Trash by lazy { stroke("Trash", "M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13") }
    val Document by lazy { stroke("Document", "M6 3h8l4 4v14H6zM14 3v4h4M9 12h6M9 16h6") }
    val Folder by lazy { stroke("Folder", "M3 6h6l2 2h10v11H3z") }
    val Layers by lazy { stroke("Layers", "M12 3l9 5-9 5-9-5 9-5zM3 13l9 5 9-5") }
    val Globe by lazy { stroke("Globe", "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18zM3 12h18M12 3c3 3 3 15 0 18M12 3c-3 3-3 15 0 18") }
    val Bolt by lazy { stroke("Bolt", "M13 2L4 14h7l-1 8 9-12h-7z") }
    val Power by lazy { stroke("Power", "M12 3v8M7 6a8 8 0 1 0 10 0") }
    val Send by lazy { stroke("Send", "M4 12l16-8-6 16-3-7z") }
    val Sun by lazy { stroke("Sun", "M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8zM12 2v2M12 20v2M2 12h2M20 12h2M5 5l1.5 1.5M17.5 17.5L19 19M5 19l1.5-1.5M17.5 6.5L19 5") }

    // Scan control
    val Play by lazy { stroke("Play", "M7 5l12 7-12 7z", width = 2.2f) }
    val Stop by lazy { stroke("Stop", "M7 7h10v10H7z", width = 2.2f) }
    val Pause by lazy { stroke("Pause", "M8 5v14M16 5v14", width = 2.2f) }

    // Status and severity
    val Warning by lazy { stroke("Warning", "M12 3l10 18H2L12 3zM12 10v5M12 18h.01", width = 2f) }
    val Info by lazy { stroke("Info", "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18zM12 11v5M12 8h.01", width = 2f) }
    val Check by lazy { stroke("Check", "M5 12l5 5 9-10", width = 2f) }
    val Shield by lazy { stroke("Shield", "M12 3l8 3v6c0 4.5-3.4 8.3-8 9-4.6-.7-8-4.5-8-9V6l8-3z") }
    val ShieldCheck by lazy { stroke("ShieldCheck", "M12 3l8 3v6c0 4.5-3.4 8.3-8 9-4.6-.7-8-4.5-8-9V6l8-3zM8.5 12l2.5 2.5 4.5-5") }

    // Tools
    val Wifi by lazy { stroke("Wifi", "M2 8.5a15 15 0 0 1 20 0M5 12a10.5 10.5 0 0 1 14 0M8.5 15.5a5.5 5.5 0 0 1 7 0M12 19h.01") }
    val Bluetooth by lazy { stroke("Bluetooth", "M7 7l10 10-5 5V2l5 5L7 17") }
    val Tracker by lazy { stroke("Tracker", "M12 2a7 7 0 0 0-7 7c0 5 7 13 7 13s7-8 7-13a7 7 0 0 0-7-7zM12 6.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5z") }
    val Camera by lazy { stroke("Camera", "M4 8h3l2-3h6l2 3h3v11H4zM12 10a3.5 3.5 0 1 0 0 7 3.5 3.5 0 0 0 0-7z") }
    val CellTower by lazy { stroke("CellTower", "M12 10v11M8 21h8M7 6a7 7 0 0 0 0 8M17 6a7 7 0 0 1 0 8M4 3a11 11 0 0 0 0 14M20 3a11 11 0 0 1 0 14M12 8a2 2 0 1 0 0 4 2 2 0 0 0 0-4z") }
    val Crosshair by lazy { stroke("Crosshair", "M12 2v3M12 19v3M2 12h3M19 12h3M12 6a6 6 0 1 0 0 12 6 6 0 0 0 0-12zM12 10a2 2 0 1 0 0 4 2 2 0 0 0 0-4z") }
    val Usb by lazy { stroke("Usb", "M12 2v14M9 5l3-3 3 3M8 9v3l4 3M16 8v2l-4 3M12 16a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5z") }
    val Qr by lazy { stroke("Qr", "M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h2v2h-2zM18 18h2v2h-2zM14 18h2M18 14h2") }
    val Lan by lazy { stroke("Lan", "M9 3h6v5H9zM3 16h6v5H3zM15 16h6v5h-6zM12 8v4M6 16v-4h12v4") }
    val Radio by lazy { stroke("Radio", "M4 9h16v11H4zM7 9l10-5M8 14.5a1.5 1.5 0 1 0 0 .01M13 13h4M13 16h4") }
    val Waveform by lazy { stroke("Waveform", "M3 12h1M7 8v8M11 5v14M15 8v8M19 10v4") }
    val Magnet by lazy { stroke("Magnet", "M6 3v8a6 6 0 0 0 12 0V3h-4v8a2 2 0 0 1-4 0V3zM6 7h4M14 7h4") }
    val Sensors by lazy { stroke("Sensors", "M12 12h.01M8.5 8.5a5 5 0 0 0 0 7M15.5 8.5a5 5 0 0 1 0 7M5.5 5.5a9 9 0 0 0 0 13M18.5 5.5a9 9 0 0 1 0 13") }
    val Nfc by lazy { stroke("Nfc", "M5 9a4 4 0 0 1 0 6M9 6.5a8 8 0 0 1 0 11M13 4a12 12 0 0 1 0 16M17 2a15 15 0 0 1 0 20") }
    val Remote by lazy { stroke("Remote", "M9 9h6v12H9zM12 13h.01M12 17h.01M8.5 5.5a5 5 0 0 1 7 0M6 3a9 9 0 0 1 12 0") }
    val Peers by lazy { stroke("Peers", "M18 5a2 2 0 1 0 0 .01M6 12a2 2 0 1 0 0 .01M18 19a2 2 0 1 0 0 .01M8 11l8-5M8 13l8 5") }
    val Timeline by lazy { stroke("Timeline", "M3 17l5-6 4 3 5-7 4 4") }
    val Map by lazy { stroke("Map", "M9 4L3 6v14l6-2 6 2 6-2V4l-6 2-6-2zM9 4v14M15 6v14") }
    val Terminal by lazy { stroke("Terminal", "M4 6l6 6-6 6M12 19h8") }
    val Speaker by lazy { stroke("Speaker", "M6 4h12v16H6zM12 14a2.5 2.5 0 1 0 0 .01M12 7.5h.01") }
    val Gauge by lazy { stroke("Gauge", "M4 16a8 8 0 1 1 16 0M12 16l4-5") }
    val Volume by lazy { stroke("Volume", "M4 9h4l5-4v14l-5-4H4zM16 9a4 4 0 0 1 0 6M19 6a8 8 0 0 1 0 12") }
}
