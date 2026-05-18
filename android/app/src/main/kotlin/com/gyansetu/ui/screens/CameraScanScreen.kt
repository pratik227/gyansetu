package com.gyansetu.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.gyansetu.ui.components.ChunkyButton
import com.gyansetu.ui.theme.GyanColors
import com.gyansetu.viewmodel.AppViewModel
import com.gyansetu.viewmodel.ScanState
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Camera Scan flow:
 *   - CameraX preview live in the viewfinder
 *   - Capture button → ImageCapture → JPEG bytes → Bitmap
 *   - ViewModel hands the bitmap to gemma.analyzeImage (Gemma 4 multimodal)
 *   - Result card slides up with EN, GU, phonetic, sentence; TTS reads it aloud
 */
@Composable
fun CameraScanScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scan by viewModel.scan.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val tts = remember {
        var ref: TextToSpeech? = null
        ref = TextToSpeech(ctx) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ref?.language = Locale("gu", "IN")
                if (ref?.language?.language != "gu") ref?.language = Locale("hi", "IN")
                if (ref?.language?.language != "hi") ref?.language = Locale.US
            }
        }
        ref
    }
    DisposableEffect(Unit) { onDispose { tts?.stop(); tts?.shutdown() } }

    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    // Frozen snapshot of the just-captured frame. We render this on top of the
    // camera surface from the moment the shutter fires until the user taps
    // "Again" — so the kid sees the photo they took, not the live preview
    // wandering around behind the result card.
    var captured by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(scan) {
        if (scan is ScanState.Idle) captured = null
    }

    val imageCapture = remember {
        // MINIMIZE_LATENCY skips the quality post-processing pipeline; we're
        // feeding the bytes to an AI tagger, not saving a portrait.
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {
        if (hasCameraPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { c ->
                    val view = PreviewView(c).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val providerFuture = ProcessCameraProvider.getInstance(c)
                    providerFuture.addListener({
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(view.surfaceProvider)
                        }
                        runCatching {
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture,
                            )
                        }
                    }, ContextCompat.getMainExecutor(c))
                    view
                },
            )
            // Frozen captured frame overlays the live preview from the moment
            // the shutter fires until reset. Sits above the camera surface but
            // below the top bar and result card.
            captured?.let { snap ->
                Image(
                    bitmap = snap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().background(Color.Black),
                    contentScale = ContentScale.Fit,
                )
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("📷", fontSize = 48.sp)
                Text("Camera permission needed", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                ChunkyButton("Grant", onClick = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }, background = GyanColors.Saffron, contentColor = Color.White)
            }
        }

        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(44.dp)
                    .background(Color(0x80000000), CircleShape)
                    .clickable { viewModel.resetScan(); onBack() },
                contentAlignment = Alignment.Center,
            ) { Text("←", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.weight(1f))
            Text(
                "વસ્તુ પર નિશાન રાખો · Point at object",
                color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(Color(0x80000000), RoundedCornerShape(999.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(44.dp))
        }

        // Capture button OR result card
        when (val s = scan) {
            ScanState.Idle, ScanState.Scanning -> {
                Box(
                    Modifier.fillMaxSize().padding(bottom = 40.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (s is ScanState.Scanning) {
                        Text(
                            "🔍 ઓળખી રહ્યું છે · Recognizing…",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color(0xCC000000), RoundedCornerShape(16.dp))
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                        )
                    } else {
                        Box(
                            Modifier.size(88.dp)
                                .background(Color.White, CircleShape)
                                .border(4.dp, Color(0x80FFFFFF), CircleShape)
                                .clickable {
                                    captureToBitmap(imageCapture, executor) { bitmap ->
                                        if (bitmap != null) {
                                            captured = bitmap
                                            viewModel.analyzeBitmap(bitmap)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier.size(64.dp)
                                    .background(GyanColors.Saffron, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { Text("📷", fontSize = 28.sp) }
                        }
                    }
                }
            }
            is ScanState.Result -> {
                LaunchedEffect(s) {
                    val msg = s.parsed.let { "${it.en}. ${it.gu}. ${it.sentenceEn}. ${it.sentenceGu}" }
                    tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "scan-${s.parsed.en}")
                }
                ResultCard(
                    parsed = s.parsed,
                    onAgain = { viewModel.resetScan() },
                    onListen = {
                        val msg = s.parsed.let { "${it.en}. ${it.gu}. ${it.sentenceEn}. ${it.sentenceGu}" }
                        tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "scan-replay")
                    },
                )
            }
            is ScanState.Error -> {
                ErrorCard(s.message) { viewModel.resetScan() }
            }
        }
    }
}

// Target edge length fed into the vision encoder. Gemma's vision tower tiles
// internally at 224/336, so 384 preserves enough detail while cutting JPEG
// encode time, JNI bytes, and vision-encoder work vs. the previous 512.
private const val SCAN_TARGET_PX = 384

private fun captureToBitmap(
    imageCapture: ImageCapture,
    executor: java.util.concurrent.Executor,
    onResult: (Bitmap?) -> Unit,
) {
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                try {
                    val buf = image.planes[0].buffer
                    val bytes = ByteArray(buf.remaining()).also { buf.get(it) }

                    // Two-pass decode: read bounds first, pick a power-of-2
                    // inSampleSize so we never materialize the full multi-MP
                    // bitmap in memory. On a 12MP capture this is the
                    // difference between ~250ms and ~30ms of decode work.
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    val longEdge = maxOf(bounds.outWidth, bounds.outHeight)
                    var sample = 1
                    while (longEdge / (sample * 2) >= SCAN_TARGET_PX) sample *= 2

                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = sample
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    var bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                        ?: run { onResult(null); return }

                    // Final exact scale to the target. filter=false (nearest
                    // neighbor) — bilinear filtering costs CPU and an AI
                    // tagger doesn't care about smooth edges.
                    val r = SCAN_TARGET_PX.toFloat() / maxOf(bmp.width, bmp.height)
                    if (r < 1f) {
                        bmp = Bitmap.createScaledBitmap(
                            bmp, (bmp.width * r).toInt(), (bmp.height * r).toInt(), false
                        )
                    }

                    // Rotate to upright so portrait captures don't reach the
                    // model sideways — that hurts recognition accuracy and
                    // forces extra decode tokens.
                    val rot = image.imageInfo.rotationDegrees
                    if (rot != 0) {
                        val m = Matrix().apply { postRotate(rot.toFloat()) }
                        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, false)
                    }

                    onResult(bmp)
                } catch (t: Throwable) {
                    Log.e("CameraScan", "decode failed", t); onResult(null)
                } finally {
                    image.close()
                }
            }
            override fun onError(exc: ImageCaptureException) {
                Log.e("CameraScan", "capture failed", exc); onResult(null)
            }
        },
    )
}

@Composable
private fun ResultCard(parsed: com.gyansetu.viewmodel.ScanResult, onAgain: () -> Unit, onListen: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier.fillMaxWidth()
                .background(Color.White, RoundedCornerShape(28.dp, 28.dp, 0.dp, 0.dp))
                .padding(20.dp),
        ) {
            Box(
                Modifier.size(40.dp, 4.dp)
                    .background(Color(0x33000000), RoundedCornerShape(2.dp))
                    .align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "✓ ${"મળી ગયું"}",
                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.GreenDeep,
                modifier = Modifier
                    .background(GyanColors.GreenSoft, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(parsed.en.uppercase(), fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink)
            Text(parsed.gu, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = GyanColors.SaffronDeep)
            if (parsed.phonetic.isNotBlank())
                Text(parsed.phonetic, fontSize = 14.sp, color = GyanColors.InkSoft)
            if (parsed.sentenceEn.isNotBlank() || parsed.sentenceGu.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier.fillMaxWidth()
                        .background(GyanColors.YellowSoft, RoundedCornerShape(16.dp))
                        .border(2.dp, GyanColors.Yellow, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                ) {
                    Text("📖 વાર્તા · Story", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.InkSoft)
                    Spacer(Modifier.height(6.dp))
                    if (parsed.sentenceGu.isNotBlank())
                        Text(parsed.sentenceGu, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = GyanColors.Ink)
                    if (parsed.sentenceEn.isNotBlank())
                        Text(parsed.sentenceEn, fontSize = 13.sp, color = GyanColors.InkSoft)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChunkyButton(
                    "📷 ફરી જુઓ", onClick = onAgain, modifier = Modifier.weight(1f),
                    background = Color.White, contentColor = GyanColors.Ink,
                )
                ChunkyButton(
                    "🔊 સાંભળો", onClick = onListen, modifier = Modifier.weight(1f),
                    background = GyanColors.Green, contentColor = Color.White,
                )
            }
        }
    }
}

@Composable
private fun ErrorCard(msg: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier.fillMaxWidth()
                .background(Color.White, RoundedCornerShape(28.dp, 28.dp, 0.dp, 0.dp))
                .padding(24.dp),
        ) {
            Text("😕 કંઈક ખોટું થયું", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink)
            Spacer(Modifier.height(6.dp))
            Text(msg, fontSize = 12.sp, color = GyanColors.InkSoft, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(14.dp))
            ChunkyButton("ફરી પ્રયત્ન કરો · Retry",
                onClick = onRetry, background = GyanColors.Saffron, contentColor = Color.White)
        }
    }
}
