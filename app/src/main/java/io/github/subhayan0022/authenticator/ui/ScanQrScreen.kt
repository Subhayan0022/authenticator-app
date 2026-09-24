package io.github.subhayan0022.authenticator.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.subhayan0022.authenticator.data.OtpType
import io.github.subhayan0022.authenticator.ui.theme.IssuerLabelStyle
import io.github.subhayan0022.authenticator.ui.theme.ScreenPadding
import java.util.concurrent.Executors
import kotlinx.coroutines.delay

private const val RetryDelayMillis = 2200L

@Composable
fun ScanQrScreen(
    form: AddAccountFormState,
    groups: List<String>,
    onScanned: (String) -> Unit,
    onGroupChange: (String) -> Unit,
    onSave: () -> Unit,
    onUnlockAndSave: () -> Unit,
    onRescan: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val colors = MaterialTheme.colorScheme

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var asked by remember { mutableStateOf(false) }

    var rearm by remember { mutableIntStateOf(0) }

    val rejected = !form.scanned && form.saveError != null

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { result ->
        granted = result
        asked = true
    }

    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(rejected) {
        if (rejected) {
            delay(RetryDelayMillis)
            onRescan()
            rearm++
        }
    }

    val blockedForever = asked && !granted && activity != null &&
        !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.background,
        contentColor = colors.onBackground,
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenTopBar(title = "Scan QR code", onBack = onBack)

            when {
                granted -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ScanWindow(paused = form.scanned) {
                            CameraPreview(rearmKey = rearm, onQrCode = onScanned)
                        }

                        Spacer(Modifier.height(20.dp))

                        when {
                            rejected -> Caption(
                                text = form.saveError.orEmpty(),
                                color = colors.error,
                            )

                            !form.scanned -> Caption(
                                text = "Point the camera at the QR code your service shows.",
                                color = colors.onSurfaceVariant,
                            )

                            else -> ScannedDetails(
                                form = form,
                                groups = groups,
                                onGroupChange = onGroupChange,
                                onScanAgain = {
                                    onRescan()
                                    rearm++
                                },
                            )
                        }

                        Spacer(Modifier.height(28.dp))
                    }

                    if (form.scanned) {
                        PrimaryBar(
                            label = when {
                                form.saving -> "Adding…"
                                form.needsAuth -> "Unlock and add"
                                else -> "Add account"
                            },
                            enabled = form.canSave,
                            onClick = if (form.needsAuth) onUnlockAndSave else onSave,
                            error = form.saveError,
                        )
                    }
                }

                blockedForever -> Message(
                    text = "Camera access is turned off for this app. Android will not ask " +
                        "again, so it has to be enabled in Settings.",
                    buttonText = "Open settings",
                    onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            ),
                        )
                    },
                )

                asked -> Message(
                    text = "Scanning needs the camera. Nothing leaves your device — the QR " +
                        "code is decoded here.",
                    buttonText = "Grant permission",
                    onClick = { launcher.launch(Manifest.permission.CAMERA) },
                )

                else -> Message(
                    text = "Requesting camera permission…",
                    buttonText = "Enter manually instead",
                    onClick = onBack,
                )
            }
        }
    }
}

@Composable
private fun ScanWindow(paused: Boolean, content: @Composable () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth(0.78f)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.Black),
    ) {
        content()

        if (paused) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))
        } else {
            val heightPx = constraints.maxHeight.toFloat()
            val bandPx = with(LocalDensity.current) { 64.dp.toPx() }

            val transition = rememberInfiniteTransition(label = "scan")
            val progress by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 2200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "sweep",
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .graphicsLayer { translationY = progress * (heightPx - bandPx) }
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to Color.White.copy(alpha = 0.20f),
                        ),
                    ),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.White.copy(alpha = 0.9f)),
                )
            }
        }

        Brackets(color = Color.White.copy(alpha = if (paused) 0.4f else 0.85f))
    }
}

@Composable
private fun Brackets(color: Color) {
    Canvas(Modifier.fillMaxSize().padding(14.dp)) {
        val len = size.minDimension * 0.16f
        val stroke = 3.dp.toPx()
        val cap = StrokeCap.Round
        val w = size.width
        val h = size.height

        drawLine(color, Offset(0f, 0f), Offset(len, 0f), stroke, cap)
        drawLine(color, Offset(0f, 0f), Offset(0f, len), stroke, cap)

        drawLine(color, Offset(w, 0f), Offset(w - len, 0f), stroke, cap)
        drawLine(color, Offset(w, 0f), Offset(w, len), stroke, cap)

        drawLine(color, Offset(0f, h), Offset(len, h), stroke, cap)
        drawLine(color, Offset(0f, h), Offset(0f, h - len), stroke, cap)

        drawLine(color, Offset(w, h), Offset(w - len, h), stroke, cap)
        drawLine(color, Offset(w, h), Offset(w, h - len), stroke, cap)
    }
}

@Composable
private fun Caption(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = ScreenPadding + 10.dp),
    )
}

@Composable
private fun ScannedDetails(
    form: AddAccountFormState,
    groups: List<String>,
    onGroupChange: (String) -> Unit,
    onScanAgain: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Column(Modifier.fillMaxWidth()) {
        Text(
            "FOUND A CODE",
            style = IssuerLabelStyle,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(start = ScreenPadding, bottom = 12.dp),
        )

        FormCard {
            DetailRow("Issuer", form.issuer.ifBlank { "—" })
            FieldDivider()
            DetailRow("Account", form.label.ifBlank { "—" })
            FieldDivider()
            DetailRow("Settings", settingsSummary(form))
        }

        Spacer(Modifier.height(22.dp))

        Text(
            "GROUP",
            style = IssuerLabelStyle,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(start = ScreenPadding, bottom = 12.dp),
        )

        GroupPicker(groups = groups, selected = form.group, onSelect = onGroupChange)

        Spacer(Modifier.height(18.dp))

        Text(
            "Scan a different code",
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(percent = 50))
                .clickable(onClick = onScanAgain)
                .padding(horizontal = 18.dp, vertical = 12.dp),
        )
    }
}

private fun settingsSummary(form: AddAccountFormState): String {
    val cadence = if (form.type == OtpType.HOTP) {
        "counter ${form.counter}"
    } else {
        "${form.periodSeconds}s period"
    }

    return "${form.type} · ${form.algorithm.removePrefix("Hmac")} · " +
        "${form.digits} digits · $cadence"
}

@Composable
private fun CameraPreview(rearmKey: Int, onQrCode: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val currentOnQrCode by rememberUpdatedState(onQrCode)

    val analyzer = remember {
        QrCodeAnalyzer { text ->
            ContextCompat.getMainExecutor(context).execute { currentOnQrCode(text) }
        }
    }

    LaunchedEffect(rearmKey) { analyzer.rearm() }

    DisposableEffect(lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)

        future.addListener({
            val provider = future.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(executor, analyzer) }

            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis,
            )
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { future.get().unbindAll() }
            executor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

@Composable
private fun Message(text: String, buttonText: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        PrimaryButton(
            label = buttonText,
            enabled = true,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
