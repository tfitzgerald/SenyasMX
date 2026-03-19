package com.lsm.translator.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lsm.translator.model.LandmarkFrame
import com.lsm.translator.viewmodel.RecognitionViewModel
import java.util.concurrent.Executors

@Composable
fun RecognitionScreen(
    modifier: Modifier = Modifier,
    vm: RecognitionViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsState()
    val context = LocalContext.current

    // ── Camera permission ──────────────────────────────────────────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> vm.onCameraPermissionResult(granted) }

    LaunchedEffect(Unit) {
        val already = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (already) vm.onCameraPermissionResult(true)
        else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // ── Root container ────────────────────────────────────────────────────
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Camera preview
        if (uiState.cameraPermissionGranted) {
            CameraPreview(
                modifier    = Modifier.fillMaxSize(),
                onFrame     = vm::onFrame,
                onLuminance = vm::onLightLevel
            )
        }

        // Hand-placement guide overlay (shown when idle)
        if (uiState.cameraPermissionGranted && !uiState.isRunning) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(width = 220.dp, height = 280.dp)
                    .border(
                        width = 2.dp,
                        color = Color.White.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp)
                    )
            )
            Text(
                text     = "Coloca tus manos aquí",
                color    = Color.White,
                style    = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 310.dp)
            )
        }

        // Low-light warning banner
        AnimatedVisibility(
            visible  = uiState.isLowLight,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE65100)),
                shape  = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier            = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment   = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Filled.LightMode, null,
                        tint     = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "Poca luz — busca mejor iluminación",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        // Camera permission denied message
        if (!uiState.cameraPermissionGranted) {
            Column(
                modifier              = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                horizontalAlignment   = Alignment.CenterHorizontally,
                verticalArrangement   = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Filled.NoPhotography, null,
                    tint     = Color.White,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    "Permiso de cámara denegado",
                    color     = Color.White,
                    textAlign = TextAlign.Center,
                    style     = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Ve a Ajustes del dispositivo para habilitarlo.",
                    color     = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    style     = MaterialTheme.typography.bodySmall
                )
            }
        }

        // ── Bottom panel: result card + button ────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Partial hypothesis (faint text while building confidence)
            AnimatedVisibility(visible = uiState.partialHypothesis != null) {
                Text(
                    text      = uiState.partialHypothesis ?: "",
                    color     = Color.White.copy(alpha = 0.55f),
                    style     = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
            }

            // Result card
            AnimatedVisibility(
                visible = uiState.currentPhrase != null || uiState.isLowConfidence
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = if (uiState.isLowConfidence)
                            Color(0xFF37474F)
                        else
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                    )
                ) {
                    Column(
                        modifier              = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment   = Alignment.CenterHorizontally,
                        verticalArrangement   = Arrangement.spacedBy(8.dp)
                    ) {
                        if (uiState.isLowConfidence) {
                            Text(
                                "No estoy seguro",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 18.sp,
                                color      = Color.White
                            )
                            Text(
                                "Intenta de nuevo",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        } else if (uiState.currentPhrase != null) {
                            Text(
                                text       = uiState.currentPhrase!!,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 22.sp,
                                color      = MaterialTheme.colorScheme.onSurface
                            )

                            // Confidence bar
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Confianza",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "${(uiState.currentConfidence * 100).toInt()}%",
                                        style      = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = confidenceColor(uiState.currentConfidence)
                                    )
                                }
                                LinearProgressIndicator(
                                    progress  = { uiState.currentConfidence },
                                    modifier  = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color     = confidenceColor(uiState.currentConfidence)
                                )
                            }

                            // Speak button
                            IconButton(
                                onClick = { uiState.currentPhrase?.let { vm.speakPhrase(it) } }
                            ) {
                                Icon(
                                    Icons.Filled.VolumeUp,
                                    contentDescription = "Escuchar",
                                    tint               = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Start / Stop button
            Button(
                onClick = {
                    if (uiState.isRunning) vm.stopRecognition()
                    else vm.startRecognition()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled  = uiState.cameraPermissionGranted,
                colors   = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isRunning)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector        = if (uiState.isRunning) Icons.Filled.Stop
                                        else Icons.Filled.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (uiState.isRunning) "Detener"
                    else "Iniciar reconocimiento"
                )
            }
        }
    }
}

// ── Camera preview composable ─────────────────────────────────────────────────

@Composable
private fun CameraPreview(
    modifier:    Modifier = Modifier,
    onFrame:     (LandmarkFrame) -> Unit,
    onLuminance: (Double) -> Unit
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor       = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        modifier = modifier,
        factory  = { ctx ->
            val previewView          = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val provider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                // Sample at ~15 fps — good balance of responsiveness vs battery
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(640, 480))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()

                var lastFrameMs = 0L
                analysis.setAnalyzer(executor) { imageProxy ->
                    val now = System.currentTimeMillis()
                    if (now - lastFrameMs >= 66L) {          // 1000 / 15 fps ≈ 66 ms
                        lastFrameMs = now

                        // Luminance check: sample first 1000 Y-plane bytes
                        val yPlane = imageProxy.planes[0].buffer
                        val bytes  = ByteArray(minOf(yPlane.remaining(), 1000))
                        yPlane.get(bytes)
                        val lum = bytes.map { it.toInt() and 0xFF }.average()
                        onLuminance(lum)

                        // Feed an empty landmark frame to the pipeline.
                        // When MediaPipe is integrated, replace this call with
                        // real pose + hand landmark extraction from imageProxy.
                        onFrame(LandmarkFrame.empty())
                    }
                    imageProxy.close()
                }

                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        analysis
                    )
                } catch (e: Exception) {
                    android.util.Log.e("CameraPreview", "Bind failed: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

// ── Helper ─────────────────────────────────────────────────────────────────────

@Composable
private fun confidenceColor(confidence: Float): Color = when {
    confidence >= 0.85f -> Color(0xFF4CAF50)   // green
    confidence >= 0.65f -> Color(0xFF00BCD4)   // cyan
    else                -> Color(0xFFFF9800)   // orange
}
