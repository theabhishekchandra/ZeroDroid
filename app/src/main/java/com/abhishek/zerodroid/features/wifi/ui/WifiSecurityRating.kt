package com.abhishek.zerodroid.features.wifi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalCyan
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed

data class SecurityRating(
    val level: String,
    val label: String,
    val color: Color,
    val description: String
)

fun getSecurityRating(securityLabel: String): SecurityRating {
    val upper = securityLabel.uppercase()
    return when {
        upper.contains("WPA3") -> SecurityRating("A+", "WPA3", TerminalCyan, "Enterprise-grade security")
        upper.contains("WPA2") && upper.contains("AES") -> SecurityRating("A", "WPA2-AES", TerminalGreen, "Strong encryption")
        upper.contains("WPA2") -> SecurityRating("B+", "WPA2", TerminalGreen, "Good security")
        upper.contains("WPA") -> SecurityRating("C", "WPA", TerminalAmber, "Outdated encryption")
        upper.contains("WEP") -> SecurityRating("D", "WEP", TerminalRed, "Easily cracked")
        upper.contains("OPEN") || upper.isEmpty() -> SecurityRating("F", "Open", TerminalRed, "No encryption!")
        else -> SecurityRating("B", securityLabel, TerminalGreen, "Encrypted")
    }
}

@Composable
fun WifiSecurityBadge(securityLabel: String, modifier: Modifier = Modifier) {
    val rating = getSecurityRating(securityLabel)

    Row(
        modifier = modifier
            .background(color = rating.color.copy(alpha = 0.1f), shape = MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when {
                rating.level.startsWith("A") -> Icons.Default.Shield
                rating.level == "F" -> Icons.Default.LockOpen
                else -> Icons.Default.Lock
            },
            contentDescription = null,
            tint = rating.color,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "${rating.level} ${rating.label}",
            style = MaterialTheme.typography.labelSmall,
            color = rating.color
        )
    }
}
