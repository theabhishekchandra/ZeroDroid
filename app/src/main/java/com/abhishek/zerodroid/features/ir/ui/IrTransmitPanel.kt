package com.abhishek.zerodroid.features.ir.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.ir.domain.IrProtocol
import com.abhishek.zerodroid.features.ir.domain.IrRemoteState
import com.abhishek.zerodroid.features.ir.domain.TransmitResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IrTransmitPanel(
    state: IrRemoteState,
    onProtocolChange: (IrProtocol) -> Unit,
    onFrequencyChange: (Int) -> Unit,
    onCodeChange: (String) -> Unit,
    onTransmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var protocolExpanded by remember { mutableStateOf(false) }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
    )

    TerminalCard(modifier = modifier) {
        Text(
            text = "> Transmit",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = protocolExpanded,
            onExpandedChange = { protocolExpanded = it }
        ) {
            OutlinedTextField(
                value = state.selectedProtocol.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Protocol") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = protocolExpanded) },
                colors = fieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = protocolExpanded,
                onDismissRequest = { protocolExpanded = false }
            ) {
                IrProtocol.entries.forEach { proto ->
                    DropdownMenuItem(
                        text = { Text(proto.displayName) },
                        onClick = {
                            onProtocolChange(proto)
                            protocolExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = state.frequency.toString(),
                onValueChange = { it.toIntOrNull()?.let(onFrequencyChange) },
                label = { Text("Freq (Hz)") },
                colors = fieldColors,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = state.code,
                onValueChange = onCodeChange,
                label = { Text("Code (hex)") },
                colors = fieldColors,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onTransmit,
            enabled = state.isIrAvailable && (state.code.isNotBlank() || state.selectedProtocol == IrProtocol.RAW),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Transmit")
        }

        state.lastTransmitResult?.let { result ->
            Spacer(modifier = Modifier.height(4.dp))
            val (text, color) = when (result) {
                is TransmitResult.Success -> "Transmitted successfully" to MaterialTheme.colorScheme.primary
                is TransmitResult.Error -> result.message to MaterialTheme.colorScheme.error
            }
            Text(text = text, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}
