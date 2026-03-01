package com.abhishek.zerodroid.features.camera.domain

object QrThreatAnalyzer {

    private val suspiciousTlds = setOf(
        ".tk", ".ml", ".ga", ".cf", ".gq", ".xyz", ".top",
        ".work", ".click", ".buzz", ".link"
    )

    private val phishingPatterns = listOf(
        Regex("""login.*verify""", RegexOption.IGNORE_CASE),
        Regex("""account.*suspend""", RegexOption.IGNORE_CASE),
        Regex("""urgent.*action""", RegexOption.IGNORE_CASE),
        Regex("""paypal.*\.(?!com)""", RegexOption.IGNORE_CASE),
        Regex("""bank.*\.(?!com)""", RegexOption.IGNORE_CASE),
        Regex("""apple.*\.(?!com|apple)""", RegexOption.IGNORE_CASE),
        Regex("""google.*\.(?!com|google)""", RegexOption.IGNORE_CASE),
    )

    private val ipUrlPattern = Regex("""https?://\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}""")

    fun analyze(rawValue: String, contentType: QrContentType): Pair<Boolean, String?> {
        if (contentType != QrContentType.URL) return false to null

        // IP-only URL
        if (ipUrlPattern.containsMatchIn(rawValue)) {
            return true to "URL uses IP address instead of domain name"
        }

        // Suspicious TLD
        val host = try {
            java.net.URI(rawValue).host ?: ""
        } catch (_: Exception) { rawValue }

        suspiciousTlds.forEach { tld ->
            if (host.endsWith(tld)) {
                return true to "Suspicious TLD: $tld"
            }
        }

        // Phishing patterns
        phishingPatterns.forEach { pattern ->
            if (pattern.containsMatchIn(rawValue)) {
                return true to "Potential phishing: matches pattern '${pattern.pattern}'"
            }
        }

        // Extremely long URL
        if (rawValue.length > 500) {
            return true to "Unusually long URL (${rawValue.length} chars)"
        }

        return false to null
    }
}
