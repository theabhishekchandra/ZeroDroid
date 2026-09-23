package com.abhishek.zerodroid.core.util

/** "just now", "5 min ago", "2h ago", "3d ago" — compact relative time for cards and rows. */
fun formatAgo(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val seconds = ((now - timestamp).coerceAtLeast(0L)) / 1000
    return when {
        seconds < 60 -> "just now"
        seconds < 3_600 -> "${seconds / 60} min ago"
        seconds < 86_400 -> "${seconds / 3_600}h ago"
        else -> "${seconds / 86_400}d ago"
    }
}

/** Compact span: "40 s", "6 min", "1 h 12 min". */
fun formatSpan(millis: Long): String {
    val seconds = millis.coerceAtLeast(0L) / 1000
    return when {
        seconds < 60 -> "$seconds s"
        seconds < 3_600 -> "${seconds / 60} min"
        else -> {
            val minutes = (seconds % 3_600) / 60
            if (minutes == 0L) "${seconds / 3_600} h" else "${seconds / 3_600} h $minutes min"
        }
    }
}
