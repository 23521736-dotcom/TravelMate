package com.example.travelmate.ui.screen.cccd

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.compose.foundation.BorderStroke
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.travelmate.ui.theme.BluePrimary
import com.example.travelmate.ui.theme.GrayBorder
import com.example.travelmate.ui.theme.GrayText
import com.example.travelmate.ui.viewmodel.AuthViewModel
import com.example.travelmate.util.CccdChipReader
import com.example.travelmate.util.CccdChipResult
import com.example.travelmate.util.CccdUtils
import com.example.travelmate.util.ParsedCccdData
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

enum class CccdScanState {
    CAMERA_SCANNER,
    NFC_PROMPT,
    RESULT_CARD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CccdScannerScreen(
    onDismiss: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var scanState by remember { mutableStateOf(CccdScanState.CAMERA_SCANNER) }
    var parsedQrData by remember { mutableStateOf<ParsedCccdData?>(null) }
    var chipResult by remember { mutableStateOf<CccdChipResult?>(null) }
    var isNfcReading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasCameraPermission = isGranted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Vibrator Helper
    fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(100)
            }
        } catch (_: Exception) {}
    }

    // Setup NFC Reader Mode when in NFC_PROMPT state
    DisposableEffect(scanState, parsedQrData) {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        val activity = context as? android.app.Activity

        if (scanState == CccdScanState.NFC_PROMPT && parsedQrData != null && activity != null && nfcAdapter != null) {
            if (!nfcAdapter.isEnabled) {
                errorMessage = "Thiết bị chưa bật NFC. Vui lòng bật NFC trong Cài đặt."
            } else {
                val flags = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
                nfcAdapter.enableReaderMode(activity, { tag: Tag ->
                    val isoDep = IsoDep.get(tag)
                    if (isoDep != null) {
                        isNfcReading = true
                        errorMessage = null
                        coroutineScope.launch {
                            val result = CccdChipReader.readCccdChip(isoDep, parsedQrData!!)
                            isNfcReading = false
                            if (result.isSuccess) {
                                triggerHapticFeedback()
                                val chipRes = result.getOrNull()
                                chipResult = chipRes
                                scanState = CccdScanState.RESULT_CARD

                                if (chipRes != null) {
                                    val photoBase64 = chipRes.faceBitmap?.let { bitmapToBase64(it) }
                                    authViewModel.saveCccdVerification(
                                        cccdNumber = chipRes.docNumber,
                                        fullName = chipRes.fullName,
                                        dob = chipRes.dob,
                                        gender = chipRes.gender,
                                        address = parsedQrData?.address ?: "",
                                        photoBase64 = photoBase64
                                    )
                                }
                            } else {
                                val err = result.exceptionOrNull()
                                errorMessage = when {
                                    err?.message?.contains("TagLost") == true -> "Quẹt thẻ quá nhanh! Vui lòng giữ cố định thẻ ở lưng điện thoại."
                                    err?.message?.contains("BAC") == true -> "Sai khóa BAC. Vui lòng quét lại mã QR."
                                    else -> err?.message ?: "Lỗi đọc thẻ NFC CCCD. Vui lòng thử lại."
                                }
                            }
                        }
                    }
                }, flags, null)
            }
        }

        onDispose {
            if (activity != null && nfcAdapter != null) {
                try { nfcAdapter.disableReaderMode(activity) } catch (_: Exception) {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (scanState) {
                            CccdScanState.CAMERA_SCANNER -> "Quét mã QR mặt trước CCCD"
                            CccdScanState.NFC_PROMPT -> "Đọc chip NFC CCCD"
                            CccdScanState.RESULT_CARD -> "Thông tin CCCD"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (scanState) {
                CccdScanState.CAMERA_SCANNER -> {
                    if (hasCameraPermission) {
                        CameraQrScannerView(
                            onQrDetected = { qrText ->
                                val parsed = CccdUtils.parseCccdQr(qrText)
                                if (parsed != null) {
                                    triggerHapticFeedback()
                                    parsedQrData = parsed
                                    scanState = CccdScanState.NFC_PROMPT
                                }
                            }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "Cần quyền Camera để quét mã QR CCCD.", color = Color.White)
                        }
                    }
                }

                CccdScanState.NFC_PROMPT, CccdScanState.RESULT_CARD -> {
                    if (chipResult != null) {
                        CccdResultCard(
                            chipResult = chipResult!!,
                            parsedQr = parsedQrData,
                            onDone = onDismiss,
                            onScanNew = {
                                parsedQrData = null
                                chipResult = null
                                errorMessage = null
                                scanState = CccdScanState.CAMERA_SCANNER
                            }
                        )
                    }
                }
            }

            // NFC Prompt BottomSheet Modal
            if (scanState == CccdScanState.NFC_PROMPT) {
                ModalBottomSheet(
                    onDismissRequest = {
                        scanState = CccdScanState.CAMERA_SCANNER
                    },
                    sheetState = rememberModalBottomSheetState(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(64.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Đã lấy mã xác thực QR!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Hãy áp mặt sau thẻ CCCD gắn chip vào lưng điện thoại và giữ nguyên cho tới khi hoàn tất.",
                            fontSize = 14.sp,
                            color = GrayText,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        if (isNfcReading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BluePrimary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = "Đang đọc chip NFC & giải mã ảnh...", fontSize = 14.sp, color = BluePrimary)
                            }
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = errorMessage!!,
                                color = Color.Red,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CameraQrScannerView(
    onQrDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val executor = remember { Executors.newSingleThreadExecutor() }
    val options = remember {
        com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
            .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE)
            .build()
    }
    val scanner = remember { BarcodeScanning.getClient(options) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder()
                        .setTargetResolution(android.util.Size(1920, 1080))
                        .build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setTargetResolution(android.util.Size(1920, 1080))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    @androidx.camera.core.ExperimentalGetImage
                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            scanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        val rawValue = barcode.rawValue
                                        if (!rawValue.isNullOrBlank() && rawValue.contains("|")) {
                                            onQrDetected(rawValue)
                                            break
                                        }
                                    }
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        } else {
                            imageProxy.close()
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )

                        // Enable continuous auto-focus across frame
                        val factory = previewView.meteringPointFactory
                        val point = factory.createPoint(previewView.width / 2f, previewView.height / 2f)
                        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                            .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                            .build()
                        camera.cameraControl.startFocusAndMetering(action)
                    } catch (e: Exception) {
                        android.util.Log.e("CccdScanner", "Camera binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Broader ViewFinder Overlay Frame
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.Center)
                .border(3.dp, BluePrimary, RoundedCornerShape(20.dp))
        )

        Text(
            text = "Hướng ống kính về mã QR mặt trước CCCD",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun CccdResultCard(
    chipResult: CccdChipResult,
    parsedQr: ParsedCccdData?,
    onDone: () -> Unit,
    onScanNew: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Đã xác minh CCCD gắn chip",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Decoded Face Image
            if (chipResult.faceBitmap != null) {
                Image(
                    bitmap = chipResult.faceBitmap.asImageBitmap(),
                    contentDescription = "Chân dung CCCD",
                    modifier = Modifier
                        .size(130.dp, 160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(2.dp, BluePrimary, RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(130.dp, 160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Gray.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(64.dp), tint = GrayText)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Text Info Table
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CccdInfoRow(label = "Số CCCD", value = chipResult.docNumber)
                CccdInfoRow(label = "Họ và tên", value = chipResult.fullName)
                CccdInfoRow(label = "Ngày sinh", value = chipResult.dob)
                CccdInfoRow(label = "Giới tính", value = chipResult.gender)
                if (parsedQr != null && parsedQr.address.isNotBlank()) {
                    CccdInfoRow(label = "Địa chỉ", value = parsedQr.address)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onScanNew,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BluePrimary)
                ) {
                    Text(text = "Quét lại", fontWeight = FontWeight.Bold, color = BluePrimary)
                }

                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text(text = "Hoàn tất", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun CccdInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF0F9FF))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = GrayText, fontWeight = FontWeight.Medium)
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

fun bitmapToBase64(bitmap: Bitmap): String {
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
    val bytes = outputStream.toByteArray()
    return Base64.encodeToString(bytes, Base64.DEFAULT)
}

fun base64ToBitmap(base64Str: String): Bitmap? {
    return try {
        val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (_: Exception) {
        null
    }
}
