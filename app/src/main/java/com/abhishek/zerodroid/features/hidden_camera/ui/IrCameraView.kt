package com.abhishek.zerodroid.features.hidden_camera.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.abhishek.zerodroid.features.hidden_camera.domain.CameraDetection
import com.abhishek.zerodroid.features.hidden_camera.domain.DetectionSource
import com.abhishek.zerodroid.features.hidden_camera.domain.ThreatLevel
import com.abhishek.zerodroid.ui.theme.TerminalRed
import java.util.concurrent.Executors

@Composable
fun IrCameraView(
    onIrDetected: (CameraDetection) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    var irSpotCount by remember { mutableStateOf(0) }
    var overlayBitmap by remember { mutableStateOf<Bitmap?>(null) }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(executor, IrSpotAnalyzer { spots, bitmap ->
                                irSpotCount = spots
                                overlayBitmap = bitmap
                                if (spots > 0) {
                                    onIrDetected(
                                        CameraDetection(
                                            source = DetectionSource.IR,
                                            threatLevel = if (spots >= 3) ThreatLevel.HIGH else ThreatLevel.MEDIUM,
                                            title = "IR Light Detected",
                                            detail = "$spots bright spot(s) detected — possible IR LED(s)"
                                        )
                                    )
                                }
                            })
                        }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (_: Exception) {
                        // Camera binding failed
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay for detected IR spots
        overlayBitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "IR spot overlay",
                modifier = Modifier.fillMaxSize(),
                alpha = 0.6f
            )
        }

        // Instructions overlay
        Text(
            text = "Point camera slowly around the room.\nIR LEDs appear as bright white/purple dots.\n${if (irSpotCount > 0) "$irSpotCount bright spot(s) detected!" else "No IR spots detected"}",
            color = if (irSpotCount > 0) TerminalRed else Color.White,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(16.dp)
        )
    }
}

/**
 * ImageAnalysis analyzer that detects bright IR spots in the camera feed.
 * IR LEDs appear as very bright pixels (near-white) in the Y (luminance) channel.
 */
private class IrSpotAnalyzer(
    private val onResult: (spotCount: Int, overlay: Bitmap?) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private const val BRIGHTNESS_THRESHOLD = 240
        private const val MIN_CLUSTER_SIZE = 5
        private const val ANALYSIS_INTERVAL_MS = 500L
    }

    private var lastAnalysisTime = 0L

    override fun analyze(image: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastAnalysisTime < ANALYSIS_INTERVAL_MS) {
            image.close()
            return
        }
        lastAnalysisTime = now

        val yPlane = image.planes[0]
        val buffer = yPlane.buffer
        val width = image.width
        val height = image.height
        val rowStride = yPlane.rowStride

        val brightSpots = mutableListOf<Pair<Int, Int>>()

        // Sample every 4th pixel for performance
        for (y in 0 until height step 4) {
            for (x in 0 until width step 4) {
                val index = y * rowStride + x
                if (index < buffer.capacity()) {
                    val luminance = buffer.get(index).toInt() and 0xFF
                    if (luminance >= BRIGHTNESS_THRESHOLD) {
                        brightSpots.add(x to y)
                    }
                }
            }
        }

        // Cluster nearby bright pixels
        val clusters = clusterSpots(brightSpots, 20)
        val significantClusters = clusters.filter { it.size >= MIN_CLUSTER_SIZE }

        val overlay = if (significantClusters.isNotEmpty()) {
            createOverlay(width, height, significantClusters)
        } else null

        onResult(significantClusters.size, overlay)
        image.close()
    }

    private fun clusterSpots(spots: List<Pair<Int, Int>>, radius: Int): List<List<Pair<Int, Int>>> {
        if (spots.isEmpty()) return emptyList()
        val visited = BooleanArray(spots.size)
        val clusters = mutableListOf<List<Pair<Int, Int>>>()

        for (i in spots.indices) {
            if (visited[i]) continue
            visited[i] = true
            val cluster = mutableListOf(spots[i])
            val queue = ArrayDeque<Int>()
            queue.add(i)

            while (queue.isNotEmpty()) {
                val idx = queue.removeFirst()
                val (cx, cy) = spots[idx]
                for (j in spots.indices) {
                    if (visited[j]) continue
                    val (px, py) = spots[j]
                    if (kotlin.math.abs(cx - px) <= radius && kotlin.math.abs(cy - py) <= radius) {
                        visited[j] = true
                        cluster.add(spots[j])
                        queue.add(j)
                    }
                }
            }
            clusters.add(cluster)
        }
        return clusters
    }

    private fun createOverlay(
        width: Int,
        height: Int,
        clusters: List<List<Pair<Int, Int>>>
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = android.graphics.Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }

        for (cluster in clusters) {
            val avgX = cluster.map { it.first }.average().toFloat()
            val avgY = cluster.map { it.second }.average().toFloat()
            val radius = (cluster.size * 2f).coerceIn(10f, 60f)
            canvas.drawCircle(avgX, avgY, radius, paint)
            canvas.drawCircle(avgX, avgY, radius + 8f, paint)
        }

        return bitmap
    }
}
