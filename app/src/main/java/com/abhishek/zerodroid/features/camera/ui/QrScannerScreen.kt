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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import com.abhishek.zerodroid.core.ui.zd.ZdCardShape
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdTabs
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.features.camera.domain.QrScannerAnalyzer
import com.abhishek.zerodroid.features.camera.domain.QrScreenTab
import com.abhishek.zerodroid.features.camera.viewmodel.QrScannerViewModel
import java.util.concurrent.Executors

@Composable
fun QrScannerScreen(
    viewModel: QrScannerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        ZdTabs(
            tabs = listOf("Scan", "Create", "History ${state.scanHistory.size}"),
            selectedIndex = if (state.activeTab == QrScreenTab.SCAN) 0 else 1,
            onSelect = {
                when (it) {
                    0 -> viewModel.setActiveTab(QrScreenTab.SCAN)
                    1 -> viewModel.setActiveTab(QrScreenTab.GENERATE)
                    else -> viewModel.toggleHistory()
                }
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        when (state.activeTab) {
            QrScreenTab.SCAN -> {
                PermissionGate(
                    permissions = PermissionUtils.cameraPermissions(),
                    rationale = "Scanning uses the camera preview. Nothing is recorded or uploaded."
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

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(300.dp).clip(ZdCardShape)) {
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
        state.lastScan?.let { result -> QrResultCard(result = result) }
            ?: ZdFootnote("Point the camera at a QR code or barcode. Links are checked for phishing signs before you open anything.")
    }
}
