package it.casagestionale.app

import android.content.Context
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun CameraScanner(
    modifier: Modifier = Modifier,
    recognizeProduct: Boolean,
    onBarcode: (String) -> Unit,
    onLabel: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val barcodeHandler = rememberUpdatedState(onBarcode)
    val labelHandler = rememberUpdatedState(onLabel)
    val session = remember(context, lifecycleOwner, recognizeProduct) {
        CameraSession(context, lifecycleOwner, recognizeProduct,
            { barcodeHandler.value(it) }, { labelHandler.value(it) })
    }
    AndroidView(modifier = modifier, factory = session::createPreview, update = {})
    DisposableEffect(session) {
        onDispose { session.close() }
    }
}

private class CameraSession(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val recognizeProduct: Boolean,
    private val onBarcode: (String) -> Unit,
    private val onLabel: (String) -> Unit,
) {
    private val executor = Executors.newSingleThreadExecutor()
    private val barcodeScanner = BarcodeScanning.getClient()
    private val imageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
    private val resultDelivered = AtomicBoolean(false)
    @Volatile private var closed = false
    private var cameraProvider: ProcessCameraProvider? = null

    fun createPreview(viewContext: Context): PreviewView = PreviewView(viewContext).apply {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        scaleType = PreviewView.ScaleType.FILL_CENTER
        bind(this)
    }

    private fun bind(view: PreviewView) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (closed) return@addListener
            val provider = future.get()
            cameraProvider = provider
            val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor) { proxy ->
                val frame = proxy.image
                if (frame == null) {
                    proxy.close()
                } else {
                    val image = InputImage.fromMediaImage(frame, proxy.imageInfo.rotationDegrees)
                    val barcodeTask = barcodeScanner.process(image).addOnSuccessListener { barcodes ->
                        barcodes.firstNotNullOfOrNull { it.rawValue }?.let { value ->
                            if (resultDelivered.compareAndSet(false, true)) onBarcode(value)
                        }
                    }
                    val tasks = if (recognizeProduct) {
                        val labelTask = imageLabeler.process(image).addOnSuccessListener { labels ->
                            labels.maxByOrNull { it.confidence }
                                ?.takeIf { it.confidence >= 0.65f }
                                ?.let { label ->
                                    if (resultDelivered.compareAndSet(false, true)) onLabel(label.text)
                                }
                        }
                        com.google.android.gms.tasks.Tasks.whenAllComplete(barcodeTask, labelTask)
                    } else {
                        barcodeTask
                    }
                    tasks.addOnCompleteListener { proxy.close() }
                }
            }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (_: Exception) {
                close()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun close() {
        closed = true
        cameraProvider?.unbindAll()
        barcodeScanner.close()
        imageLabeler.close()
        executor.shutdown()
    }
}
