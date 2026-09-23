package com.abhishek.zerodroid.features.camera.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.features.camera.domain.QrScanResult
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScanHistorySheet(
    history: List<QrScanResult>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ZdColors.Surface,
        scrimColor = ZdColors.Scrim,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("Scan history · ${history.size}", style = ZdType.Heading, color = ZdColors.Text) }
            if (history.isEmpty()) item { ZdFootnote("Codes you scan are kept here, on this phone only.") }
            items(history) { QrResultCard(result = it) }
        }
    }
}
