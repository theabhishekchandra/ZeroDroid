package com.abhishek.zerodroid.features.camera.ui

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.features.camera.domain.QrScannerAnalyzer
import com.abhishek.zerodroid.features.camera.domain.QrScreenTab
import com.abhishek.zerodroid.features.camera.viewmodel.QrScannerViewModel
import java.util.concurrent.Executors

@Composable
fun QrScannerScreen(
    viewModel: QrScannerViewModel = viewModel(factory = QrScannerViewModel.Factory)
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.activeTab == QrScreenTab.SCAN,
                onClick = { viewModel.setActiveTab(QrScreenTab.SCAN) },
                label = { Text("Scan", style = MaterialTheme.typography.labelMedium) },
                leadingIcon = { Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                )
            )
            FilterChip(
                selected = state.activeTab == QrScreenTab.GENERATE,
                onClick = { viewModel.setActiveTab(QrScreenTab.GENERATE) },
                label = { Text("Generate", style = MaterialTheme.typography.labelMedium) },
                leadingIcon = { Icon(imageVector = Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        when (state.activeTab) {
            QrScreenTab.SCAN -> {
                PermissionGate(
                    permissions = PermissionUtils.cameraPermissions(),
                    rationale = "Camera permission is needed to scan QR codes and barcodes."
                ) { QrScannerContent(viewModel) }
            }
            QrScreenTab.GENERATE -> QrGeneratorPanel(viewModel = viewModel)
        }
    }

    if (state.showHistory) {
        QrScanHistorySheet(history = state.scanHistory, onDismiss = { viewModel.toggleHistory() })
    }
}

@Composable
private fun QrScannerContent(viewModel: QrScannerViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                            .also { it.setAnalyzer(executor, QrScannerAnalyzer { rawValue, format -> viewModel.onBarcodeDetected(rawValue, format) }) }
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                        } catch (e: Exception) { android.util.Log.e("QrScanner", "Camera bind failed", e) }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "> QR Scanner", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Button(onClick = { viewModel.toggleHistory() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("History (${state.scanHistory.size})") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        state.lastScan?.let { result -> QrResultCard(result = result) }
            ?: Text(text = "Point camera at a QR code or barcode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
