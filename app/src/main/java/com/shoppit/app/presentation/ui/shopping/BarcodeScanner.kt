package com.shoppit.app.presentation.ui.shopping

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * Composable for barcode scanning using CameraX and ML Kit.
 * 
 * Handles camera permission requests and displays a camera preview
 * with barcode detection overlay.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScanner(
    onBarcodeDetected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    var showPermissionRationale by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            showPermissionRationale = true
        }
    }
    
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Barcode") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                hasCameraPermission -> {
                    CameraPreview(
                        onBarcodeDetected = onBarcodeDetected,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Scanning instructions overlay
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                        )
                    ) {
                        Text(
                            text = "Point camera at barcode",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                showPermissionRationale -> {
                    PermissionRationaleContent(
                        onRequestPermission = {
                            showPermissionRationale = false
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        onDismiss = onDismiss
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

/**
 * Camera preview with barcode detection.
 */
@Composable
private fun CameraPreview(
    onBarcodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val barcodeScanner = remember { BarcodeScanning.getClient() }
    val executor = remember { Executors.newSingleThreadExecutor() }
    
    var lastDetectedBarcode by remember { mutableStateOf<String?>(null) }
    var lastDetectionTime by remember { mutableStateOf(0L) }
    
    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            barcodeScanner.close()
        }
    }
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(executor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                
                                barcodeScanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            val rawValue = barcode.rawValue
                                            if (rawValue != null) {
                                                val currentTime = System.currentTimeMillis()
                                                // Debounce: only trigger if different barcode or 2 seconds passed
                                                if (rawValue != lastDetectedBarcode || 
                                                    currentTime - lastDetectionTime > 2000) {
                                                    lastDetectedBarcode = rawValue
                                                    lastDetectionTime = currentTime
                                                    onBarcodeDetected(rawValue)
                                                }
                                            }
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }
                
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    // Handle camera binding errors
                }
            }, ContextCompat.getMainExecutor(ctx))
            
            previewView
        },
        modifier = modifier
    )
}

/**
 * Permission rationale content shown when camera permission is denied.
 */
@Composable
private fun PermissionRationaleContent(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Camera Permission Required",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "To scan barcodes, Shoppit needs access to your camera. " +
                    "This permission is only used for barcode scanning.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant Permission")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}
