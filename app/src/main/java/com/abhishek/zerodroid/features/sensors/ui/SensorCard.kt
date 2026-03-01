package com.abhishek.zerodroid.features.sensors.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.StatusIndicator
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.sensors.domain.SensorReading
import java.util.Locale
import kotlin.math.sqrt

@Composable
fun SensorCard(
    reading: SensorReading,
    modifier: Modifier = Modifier
) {
    val is3Axis = reading.isAvailable && reading.values.size >= 3
    var expanded by remember { mutableStateOf(false) }

    TerminalCard(
        modifier = modifier,
        onClick = if (is3Axis) {{ expanded = !expanded }} else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = reading.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (reading.isAvailable && reading.values.isNotEmpty()) {
                    val summaryText = if (is3Axis) {
                        val mag = sqrt(
                            reading.values[0] * reading.values[0] +
                            reading.values[1] * reading.values[1] +
                            reading.values[2] * reading.values[2]
                        )
                        "${String.format(Locale.US, "%.2f", mag)} ${reading.unit}"
                    } else {
                        "${String.format(Locale.US, "%.2f", reading.values[0])} ${reading.unit}"
                    }
                    Text(
                        text = summaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                StatusIndicator(isAvailable = reading.isAvailable)
            }
        }

        if (!reading.isAvailable) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Sensor not available",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (is3Axis) {
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val labels = listOf("X", "Y", "Z")
                    reading.values.take(3).forEachIndexed { i, value ->
                        Text(
                            text = "${labels[i]}: ${String.format(Locale.US, "%+8.3f", value)} ${reading.unit}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
