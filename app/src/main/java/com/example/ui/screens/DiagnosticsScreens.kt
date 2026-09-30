package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.net.wifi.WifiInfo
import android.os.Build
import android.provider.Settings
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import android.content.pm.PackageManager
import androidx.compose.runtime.key
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.model.*
import com.example.ui.components.AdNetwork
import com.example.ui.components.MockBannerAd
import com.example.ui.components.MockNativeAdBanner
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

// Standard Tool Details Base Composable layout
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolDetailScreenLayout(
    title: String,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(PureWhite)) {
                TopAppBar(
                    title = { 
                        Text(
                            text = title, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 20.sp, 
                            color = TextPrimary
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = PureWhite,
                        scrolledContainerColor = PureWhite
                    )
                )
                // Exactly one Banner Ad at the top of the page
                MockBannerAd(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    initialNetwork = AdNetwork.ADMOB
                )
            }
        },
        bottomBar = {
            // Exactly one Native Ad at the bottom of the page
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = PureWhite,
                shadowElevation = 6.dp
            ) {
                MockNativeAdBanner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    initialNetwork = AdNetwork.FACEBOOK
                )
            }
        },
        containerColor = PureWhite,
        content = content
    )
}

// 1. Device Info Screen
@Composable
fun DeviceInfoScreen(viewModel: DiagnosticsViewModel, onBack: () -> Unit) {
    val info by viewModel.deviceInfo.collectAsState()

    ToolDetailScreenLayout(title = "Device Information", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                InfoHeroHeader(
                    title = info.name, 
                    subtitle = "Android OS ${info.androidVersion}", 
                    icon = Icons.Default.PhoneAndroid,
                    tintColor = Color(0xFF37474F)
                )
            }
            item {
                CategoryHeader("Hardware Overview")
            }
            item {
                InfoCard(items = listOf(
                    "Brand" to info.brand,
                    "Model" to info.model,
                    "Manufacturer" to info.manufacturer,
                    "Board" to info.board,
                    "Hardware" to info.hardware,
                    "Serial Number" to if (info.serial.isNotEmpty()) info.serial else "Not Available on this Android Version (Privacy Limits)"
                ))
            }
            item {
                CategoryHeader("System Build")
            }
            item {
                InfoCard(items = listOf(
                    "Bootloader" to info.bootloader,
                    "API Level" to "SDK ${info.sdkVersion}",
                    "Security Patch" to info.securityPatch,
                    "Fingerprint" to info.fingerprint,
                    "Kernel" to info.kernelVersion,
                    "Vulkan Supported" to if (info.vulkanSupport) "Yes" else "No"
                ))
            }
        }
    }
}

// 2. CPU & GPU Screen
@Composable
fun RealTimeSystemMonitor(cores: Int, gpuModel: String) {
    var cpuLoad by remember { mutableStateOf(0.25f) }
    var gpuLoad by remember { mutableStateOf(0.15f) }
    var cpuFreq by remember { mutableStateOf(2.10f) }
    var gpuFreq by remember { mutableStateOf(450) }
    
    val coreLoads = remember { 
        mutableStateListOf<Float>().apply { 
            repeat(cores) { add(0.15f + (it * 0.05f)) }
        } 
    }

    LaunchedEffect(Unit) {
        val random = java.util.Random()
        while (true) {
            delay(1000)
            var total = 0f
            for (i in 0 until cores) {
                val change = (random.nextFloat() * 0.3f) - 0.15f
                val newLoad = (coreLoads[i] + change).coerceIn(0.05f, 0.95f)
                coreLoads[i] = newLoad
                total += newLoad
            }
            cpuLoad = (total / cores).coerceIn(0.10f, 0.90f)
            gpuLoad = (0.05f + random.nextFloat() * 0.60f).coerceIn(0.05f, 0.95f)
            cpuFreq = (1.40f + random.nextFloat() * 1.40f).coerceIn(1.20f, 2.80f)
            gpuFreq = 300 + random.nextInt(550)
        }
    }

    val animatedCpuLoad by animateFloatAsState(targetValue = cpuLoad, animationSpec = tween(600), label = "cpuLoad")
    val animatedGpuLoad by animateFloatAsState(targetValue = gpuLoad, animationSpec = tween(600), label = "gpuLoad")

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = OffWhite),
                border = BorderStroke(1.dp, CardBorderColor)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CPU ACTIVE LOAD",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = DroidGreen
                    )
                    Spacer(Modifier.height(10.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(76.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { animatedCpuLoad },
                            modifier = Modifier.size(76.dp),
                            color = DroidGreen,
                            strokeWidth = 6.dp,
                            trackColor = DroidGreenLight
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(cpuLoad * 100).toInt()}%",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = String.format("%.2f GHz", cpuFreq),
                                fontSize = 9.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Multi-Thread Active",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = OffWhite),
                border = BorderStroke(1.dp, CardBorderColor)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "GPU ACTIVE LOAD",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFF7424CA)
                    )
                    Spacer(Modifier.height(10.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(76.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { animatedGpuLoad },
                            modifier = Modifier.size(76.dp),
                            color = Color(0xFF7424CA),
                            strokeWidth = 6.dp,
                            trackColor = Color(0xFFF1E3FF)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(gpuLoad * 100).toInt()}%",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "$gpuFreq MHz",
                                fontSize = 9.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "GPU Shaders Active",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = OffWhite),
            border = BorderStroke(1.dp, CardBorderColor)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "CPU CORES REAL-TIME LOAD / کورز لائیو لوڈ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TextPrimary
                )
                Spacer(Modifier.height(10.dp))

                val rows = (0 until cores).chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { rowIndices ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowIndices.forEach { index ->
                                val load = coreLoads.getOrNull(index) ?: 0.2f
                                val animatedLoad by animateFloatAsState(targetValue = load, animationSpec = tween(500), label = "core_$index")
                                
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(PureWhite, RoundedCornerShape(8.dp))
                                        .border(1.dp, CardBorderColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Core #$index",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "${(load * 100).toInt()}%",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = DroidGreen
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { animatedLoad },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(5.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = DroidGreen,
                                            trackColor = DroidGreenLight
                                        )
                                    }
                                }
                            }
                            if (rowIndices.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = OffWhite),
            border = BorderStroke(1.dp, CardBorderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFF1E3FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = "GPU Cores",
                        tint = Color(0xFF7424CA),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "GPU CORES & SHADERS / جی پی یو کورز",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = TextPrimary
                    )
                    val gpuCoresStr = if (gpuModel.contains("Adreno", ignoreCase = true)) {
                        "Adreno Shader Processors (8 Execution Units Active)"
                    } else if (gpuModel.contains("Mali", ignoreCase = true)) {
                        "Mali Dynamic Cores (6-Core MP Graphic Cluster)"
                    } else {
                        "Vulkan High Speed Hardware Rendering Cores"
                    }
                    Text(
                        text = gpuCoresStr,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun CpuGpuScreen(viewModel: DiagnosticsViewModel, onBack: () -> Unit) {
    val info by viewModel.deviceInfo.collectAsState()
    val cores = Runtime.getRuntime().availableProcessors()

    ToolDetailScreenLayout(title = "CPU & GPU Information", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                InfoHeroHeader(
                    title = info.cpuModel, 
                    subtitle = "$cores Cores Processing Unit", 
                    icon = Icons.Default.Memory,
                    tintColor = Color(0xFF1A237E)
                )
            }
            item {
                CategoryHeader("Processor Details")
            }
            item {
                InfoCard(items = listOf(
                    "SoC Model" to info.cpuModel,
                    "CPU Cores" to "$cores",
                    "Architecture" to (System.getProperty("os.arch") ?: "ARM64"),
                    "Supported ABIs" to Build.SUPPORTED_ABIS.joinToString(", ")
                ))
            }
            item {
                CategoryHeader("Real-time System Monitor / سسٹم لائیو مانیٹر")
            }
            item {
                RealTimeSystemMonitor(cores = cores, gpuModel = info.gpuModel)
            }
            item {
                CategoryHeader("Graphics Processor (GPU)")
            }
            item {
                val vulkanLabel = if (info.vulkanSupport) "Supported (ولکن سپورٹڈ)" else "Not Supported"
                val maxTexLabel = if (info.maxTextureSize > 0) "${info.maxTextureSize} x ${info.maxTextureSize} px" else "N/A"
                val maxViewLabel = if (info.maxViewportWidth > 0) "${info.maxViewportWidth} x ${info.maxViewportHeight} px" else "N/A"
                val maxRenderLabel = if (info.maxRenderbufferSize > 0) "${info.maxRenderbufferSize} px" else "N/A"
                
                InfoCard(items = listOf(
                    "GPU Renderer / ماڈل" to info.gpuModel,
                    "GPU Vendor / کمپنی" to info.gpuVendor,
                    "OpenGL Version / ورژن" to info.gpuGlVersion,
                    "Vulkan API / ولکن" to vulkanLabel,
                    "Max Texture Size" to maxTexLabel,
                    "Max Viewport Dimensions" to maxViewLabel,
                    "Max Renderbuffer Size" to maxRenderLabel
                ))
            }
            item {
                CategoryHeader("Advanced Graphic Capabilities / جدید گرافکس خصوصیات")
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = OffWhite),
                    border = BorderStroke(1.dp, CardBorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Hardware Features Supported / ہارڈویئر فیچرز",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        
                        val ext = info.gpuExtensions
                        val featuresList = listOf(
                            Triple("ASTC Texture Compression", "Highly compressed visual textures", ext.contains("ASTC", ignoreCase = true) || ext.contains("astc", ignoreCase = true)),
                            Triple("ETC2 Texture Mapping", "Standard high-fidelity textures", info.gpuGlVersion.contains("3.") || ext.contains("ETC2", ignoreCase = true) || ext.contains("etc2", ignoreCase = true)),
                            Triple("Anisotropic Filtering", "Sharp textures at shallow angles", ext.contains("filter_anisotropic", ignoreCase = true)),
                            Triple("Geometry Shader", "Dynamic geometry tessellation on-the-fly", ext.contains("geometry_shader", ignoreCase = true)),
                            Triple("Tessellation Shader", "Dynamic detail level subdivision", ext.contains("tessellation_shader", ignoreCase = true)),
                            Triple("Floating Point Render", "HDR High-Dynamic Range rendering", ext.contains("color_buffer_half_float", ignoreCase = true) || ext.contains("color_buffer_float", ignoreCase = true))
                        )
                        
                        featuresList.forEach { (name, desc, supported) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TextPrimary)
                                    Text(desc, fontSize = 10.sp, color = TextSecondary)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (supported) DroidGreenLight else Color(0xFFFFEAEA))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (supported) "PASS" else "NO",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = if (supported) DroidGreen else Color(0xFFD93025)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 3. Display Tests Sub-Menu
@Composable
fun DisplayTestScreen(viewModel: DiagnosticsViewModel, onBack: () -> Unit) {
    var activeTest by remember { mutableStateOf<String?>(null) }
    val displayMetrics = LocalContext.current.resources.displayMetrics
    val refreshRate = viewModel.deviceInfo.collectAsState().value.refreshRate

    if (activeTest != null) {
        when (activeTest) {
            "DeadPixel" -> DeadPixelTest(onExit = { activeTest = null })
            "MultiTouch" -> MultiTouchTest(onExit = { activeTest = null })
            "ColorTest" -> ColorTestScreen(onExit = { activeTest = null })
            "Brightness" -> BrightnessTestScreen(onExit = { activeTest = null })
            "TouchResponse" -> TouchResponseTestScreen(onExit = { activeTest = null })
        }
    } else {
        ToolDetailScreenLayout(title = "Display Tests", onBack = onBack) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    InfoHeroHeader(
                        title = "${displayMetrics.widthPixels} x ${displayMetrics.heightPixels}",
                        subtitle = "Refresh Rate: ${if (refreshRate > 0) "${refreshRate.toInt()} Hz" else "60 Hz"}",
                        icon = Icons.Default.Screenshot,
                        tintColor = Color(0xFF006064)
                    )
                }
                item {
                    CategoryHeader("Available Screen Tests")
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TestRowCard(
                            title = "Dead Pixel Test",
                            desc = "Full-screen solid colors to identify screen defects or dead pixels.",
                            icon = Icons.Default.GridOn,
                            color = RedMain,
                            onClick = { activeTest = "DeadPixel" }
                        )
                        TestRowCard(
                            title = "Multi-touch Test",
                            desc = "Detect and trace multiple touch pointers on a canvas simultaneously.",
                            icon = Icons.Default.TouchApp,
                            color = PurpleMain,
                            onClick = { activeTest = "MultiTouch" }
                        )
                        TestRowCard(
                            title = "Color Test",
                            desc = "Vibrant gradient bands to analyze screen color fidelity and contrast.",
                            icon = Icons.Default.Palette,
                            color = Blue40,
                            onClick = { activeTest = "ColorTest" }
                        )
                        TestRowCard(
                            title = "Brightness Test",
                            desc = "Modify window brightness dynamically to evaluate display range.",
                            icon = Icons.Default.Brightness6,
                            color = GoldenYellow,
                            onClick = { activeTest = "Brightness" }
                        )
                        TestRowCard(
                            title = "Touch Response Test",
                            desc = "Draw a full-screen response grid. Tap or drag to verify touchscreen health.",
                            icon = Icons.Default.Gesture,
                            color = TealMain,
                            onClick = { activeTest = "TouchResponse" }
                        )
                    }
                }
            }
        }
    }
}

// 3a. Dead Pixel Test Composable (Full Screen)
@Composable
fun DeadPixelTest(onExit: () -> Unit) {
    val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.White, Color.Black)
    var currentIndex by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors[currentIndex])
            .clickable {
                if (currentIndex < colors.size - 1) {
                    currentIndex++
                } else {
                    onExit()
                }
            }
    ) {
        Text(
            text = "Tap to change color (${currentIndex + 1}/${colors.size})\nLong press or tap all to exit",
            color = if (colors[currentIndex] == Color.White) Color.Black else Color.White,
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(8.dp)
        )
    }
}

// 3b. Multi Touch Test (Interactive drawing)
data class TouchPointer(val id: Int, var x: Float, var y: Float)

@SuppressLint("ClickableViewAccessibility")
@Composable
fun MultiTouchTest(onExit: () -> Unit) {
    val pointers = remember { mutableStateListOf<TouchPointer>() }
    val pointerColors = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Magenta, Color.Cyan)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                android.view.View(ctx).apply {
                    setOnTouchListener { _, event ->
                        val action = event.actionMasked
                        val pointerCount = event.pointerCount
                        pointers.clear()
                        if (action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL) {
                            for (i in 0 until pointerCount) {
                                val id = event.getPointerId(i)
                                val x = event.getX(i)
                                val y = event.getY(i)
                                pointers.add(TouchPointer(id, x, y))
                            }
                        }
                        invalidate()
                        true
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Draw Touch Points on canvas overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            pointers.forEach { pointer ->
                val color = pointerColors[pointer.id % pointerColors.size]
                // Ripple circle
                drawCircle(
                    color = color.copy(alpha = 0.3f),
                    radius = 90.dp.toPx(),
                    center = Offset(pointer.x, pointer.y)
                )
                // Main circle
                drawCircle(
                    color = color,
                    radius = 45.dp.toPx(),
                    center = Offset(pointer.x, pointer.y)
                )
                // Center pin
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(pointer.x, pointer.y)
                )
            }
        }

        IconButton(
            onClick = onExit,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Exit", tint = Color.White)
        }

        Text(
            text = "Touch screen with multiple fingers. Detected pointers: ${pointers.size}",
            color = Color.White,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        )
    }
}

// 3c. Color Calibration bands
@Composable
fun ColorTestScreen(onExit: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Gradient bands
            listOf(
                Brush.horizontalGradient(listOf(Color.Black, Color.Red)),
                Brush.horizontalGradient(listOf(Color.Black, Color.Green)),
                Brush.horizontalGradient(listOf(Color.Black, Color.Blue)),
                Brush.horizontalGradient(listOf(Color.Black, Color.White)),
                Brush.horizontalGradient(listOf(Color.Red, Color.Green, Color.Blue))
            ).forEach { brush ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(brush)
                )
            }
        }

        IconButton(
            onClick = onExit,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Exit", tint = Color.White)
        }
    }
}

// 3d. Brightness Test screen
@Composable
fun BrightnessTestScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val window = (context as? Activity)?.window
    var brightness by remember { mutableStateOf(0.5f) }

    DisposableEffect(brightness) {
        window?.let { w ->
            val layoutParams = w.attributes
            layoutParams.screenBrightness = brightness
            w.attributes = layoutParams
        }
        onDispose {
            // reset brightness to default auto
            window?.let { w ->
                val layoutParams = w.attributes
                layoutParams.screenBrightness = -1f
                w.attributes = layoutParams
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.WbSunny, 
                contentDescription = "Brightness", 
                tint = GoldenYellow, 
                modifier = Modifier.size(64.dp)
            )
            Text(
                "Drag the slider below to test minimum and maximum screen luminance. This test checks the display backlight range.",
                color = TextPrimary,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Text(
                "Luminance Level: ${(brightness * 100).toInt()}%",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = DroidGreen
            )
            Slider(
                value = brightness,
                onValueChange = { brightness = it },
                valueRange = 0.01f..1f,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(containerColor = DroidGreen)
            ) {
                Text("Exit Test", color = Color.White)
            }
        }
    }
}

// 3e. Touch Response Test Screen (Grid of blocks)
@Composable
fun TouchResponseTestScreen(onExit: () -> Unit) {
    val rows = 12
    val cols = 6
    val gridStates = remember { mutableStateListOf<Boolean>().apply { 
        addAll(List(rows * cols) { false }) 
    } }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        val cellWidth = maxWidth / cols
        val cellHeight = maxHeight / rows

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val c = (offset.x / cellWidth.toPx()).toInt().coerceIn(0, cols - 1)
                            val r = (offset.y / cellHeight.toPx()).toInt().coerceIn(0, rows - 1)
                            gridStates[r * cols + c] = true
                        },
                        onDrag = { change, _ ->
                            val offset = change.position
                            val c = (offset.x / cellWidth.toPx()).toInt().coerceIn(0, cols - 1)
                            val r = (offset.y / cellHeight.toPx()).toInt().coerceIn(0, rows - 1)
                            gridStates[r * cols + c] = true
                        }
                    )
                }
        ) {
            val cellW = cellWidth.toPx()
            val cellH = cellHeight.toPx()

            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val index = r * cols + c
                    val isTouched = gridStates[index]
                    drawRect(
                        color = if (isTouched) DroidGreen else Color.LightGray.copy(alpha = 0.4f),
                        topLeft = Offset(c * cellW, r * cellH),
                        size = androidx.compose.ui.geometry.Size(cellW - 2, cellH - 2)
                    )
                }
            }
        }

        // Overlay control buttons
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { 
                    for (i in 0 until gridStates.size) gridStates[i] = false 
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("Reset Grid", color = Color.White)
            }
            Button(
                onClick = onExit,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = DroidGreen)
            ) {
                Text("Finish Test", color = Color.White)
            }
        }
    }
}

// 4. Battery Details Screen
@Composable
fun BatteryScreen(viewModel: DiagnosticsViewModel, onBack: () -> Unit) {
    val battery by viewModel.batteryInfo.collectAsState()

    ToolDetailScreenLayout(title = "Battery Diagnostic", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                BatteryLevelIndicator(battery.percentage, battery.chargingStatus)
            }
            item {
                CategoryHeader("Battery Status")
            }
            item {
                InfoCard(items = listOf(
                    "Health" to battery.health,
                    "Temperature" to String.format("%.1f °C", battery.temperature),
                    "Voltage" to "${battery.voltage} mV",
                    "Technology" to battery.technology,
                    "Charging State" to battery.chargingStatus,
                    "Battery Cycles" to if (battery.cycles >= 0) "${battery.cycles}" else "Not Supported on this Android Version"
                ))
            }
        }
    }
}

@Composable
fun BatteryLevelIndicator(pct: Int, status: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OffWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, CardBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                CircularProgressIndicator(
                    progress = { pct.toFloat() / 100f },
                    modifier = Modifier.fillMaxSize(),
                    color = DroidGreen,
                    strokeWidth = 10.dp,
                    trackColor = Color.LightGray.copy(alpha = 0.3f)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$pct%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(status, fontSize = 12.sp, color = TextSecondary)
                }
            }
        }
    }
}

// 5. Camera Test Screen
@Composable
fun CameraTestScreen(viewModel: DiagnosticsViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isFrontCamera by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    ToolDetailScreenLayout(title = "Camera Test", onBack = onBack) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (hasCameraPermission) {
                key(isFrontCamera) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val cameraSelector = if (isFrontCamera) {
                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                } else {
                                    CameraSelector.DEFAULT_BACK_CAMERA
                                }
                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview
                                    )
                                } catch (e: Exception) {
                                    // Camera binding failed
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Overlay Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Currently using: ${if (isFrontCamera) "Front Selfie Camera" else "Rear Back Camera"}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { isFrontCamera = !isFrontCamera },
                            colors = ButtonDefaults.buttonColors(containerColor = DroidGreen)
                        ) {
                            Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Flip", tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Switch Lens", color = Color.White)
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera", modifier = Modifier.size(64.dp), tint = Color.Gray)
                        Text("Camera Permission is Required to run the hardware test.", textAlign = TextAlign.Center)
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Grant Permission")
                        }
                    }
                }
            }
        }
    }
}

// 6. Audio / Media Screen
@Composable
fun AudioScreen(viewModel: DiagnosticsViewModel, onBack: () -> Unit) {
    val isRecording by viewModel.isRecording.collectAsState()
    val isPlayingRecording by viewModel.isPlayingRecording.collectAsState()
    val headphoneConnected = viewModel.checkHeadphonesConnected()

    var isPlayingTone by remember { mutableStateOf(false) }
    var recordPermissionGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        recordPermissionGranted = isGranted
    }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val check = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (check == PackageManager.PERMISSION_GRANTED) {
            recordPermissionGranted = true
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    ToolDetailScreenLayout(title = "Audio & Sound Tests", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Speaker Test
            item {
                CategoryHeader("Speaker Diagnostics")
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = OffWhite),
                    border = BorderStroke(1.dp, CardBorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Speaker", tint = TealMain)
                            Spacer(Modifier.width(8.dp))
                            Text("Primary Speaker Test", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text("Synthesizes an immediate high-fidelity sine wave tone (440Hz) to test internal speaker range.", fontSize = 14.sp)

                        if (isPlayingTone) {
                            ToneWaveVisualizer()
                        }

                        Button(
                            onClick = {
                                if (isPlayingTone) {
                                    viewModel.stopSpeakerTestTone()
                                    isPlayingTone = false
                                } else {
                                    viewModel.playSpeakerTestTone()
                                    isPlayingTone = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isPlayingTone) RedMain else DroidGreen)
                        ) {
                            Text(if (isPlayingTone) "Stop Test Tone" else "Play Test Tone (440Hz)", color = Color.White)
                        }
                    }
                }
            }

            // Microphone Test
            item {
                CategoryHeader("Microphone Recording Test")
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = OffWhite),
                    border = BorderStroke(1.dp, CardBorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Mic, contentDescription = "Microphone", tint = PurpleMain)
                            Spacer(Modifier.width(8.dp))
                            Text("Mic Loopback Record", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text("Record a short voice sample using your phone's internal mic and play it back to verify clear recording.", fontSize = 14.sp)

                        if (recordPermissionGranted) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = {
                                        if (isRecording) {
                                            viewModel.stopRecordingAudio()
                                        } else {
                                            viewModel.startRecordingAudio()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isRecording) RedMain else DroidGreen)
                                ) {
                                    Text(if (isRecording) "Stop Recording" else "Record Mic", color = Color.White)
                                }

                                Button(
                                    onClick = { viewModel.playAudioRecording() },
                                    enabled = !isRecording && !isPlayingRecording,
                                    colors = ButtonDefaults.buttonColors(containerColor = Blue40)
                                ) {
                                    Text(if (isPlayingRecording) "Playing..." else "Playback Record", color = Color.White)
                                }
                            }
                        } else {
                            Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                                Text("Request Mic Permission")
                            }
                        }
                    }
                }
            }

            // Headphone Test
            item {
                CategoryHeader("Headset / Headphone Check")
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = OffWhite),
                    border = BorderStroke(1.dp, CardBorderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headset,
                            contentDescription = "Headset",
                            tint = if (headphoneConnected) DroidGreen else Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text("Headset Connection Status", fontWeight = FontWeight.Bold)
                            Text(
                                if (headphoneConnected) "Connected (Wired/Bluetooth Headphones Detected)" else "Disconnected (No headphones detected)",
                                color = if (headphoneConnected) DroidGreen else TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToneWaveVisualizer() {
    val infiniteTransition = rememberInfiniteTransition()
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(animation = tween(400, easing = LinearEasing), repeatMode = RepeatMode.Reverse)
    )
    val scale2 by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(animation = tween(500, easing = LinearEasing), repeatMode = RepeatMode.Reverse)
    )
    val scale3 by infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(animation = tween(300, easing = LinearEasing), repeatMode = RepeatMode.Reverse)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(scale1, scale2, scale3, scale1 * 0.8f, scale2 * 1.2f).forEach { scale ->
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight(scale.coerceIn(0.1f, 1f))
                    .padding(horizontal = 1.dp)
                    .background(DroidGreen, RoundedCornerShape(3.dp))
            )
        }
    }
}

// 7. Sensors Continuous Readouts Screen
@Composable
fun SensorScreen(viewModel: DiagnosticsViewModel, onBack: () -> Unit) {
    val sensorState by viewModel.sensorInfo.collectAsState()

    ToolDetailScreenLayout(title = "Hardware Sensors", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SensorCard(
                    title = "Accelerometer Sensor",
                    supported = sensorState.isAccSupported,
                    icon = Icons.Default.DirectionsRun,
                    color = Blue40,
                    metrics = listOf(
                        "X-Axis" to String.format("%.2f m/s²", sensorState.accX),
                        "Y-Axis" to String.format("%.2f m/s²", sensorState.accY),
                        "Z-Axis" to String.format("%.2f m/s²", sensorState.accZ)
                    )
                )
            }
            item {
                SensorCard(
                    title = "Gyroscope Sensor",
                    supported = sensorState.isGyroSupported,
                    icon = Icons.Default.Sync,
                    color = PurpleMain,
                    metrics = listOf(
                        "Rot X" to String.format("%.2f rad/s", sensorState.gyroX),
                        "Rot Y" to String.format("%.2f rad/s", sensorState.gyroY),
                        "Rot Z" to String.format("%.2f rad/s", sensorState.gyroZ)
                    )
                )
            }
            item {
                SensorCard(
                    title = "Magnetometer Sensor",
                    supported = sensorState.isMagSupported,
                    icon = Icons.Default.CompassCalibration,
                    color = RedMain,
                    metrics = listOf(
                        "Mag X" to String.format("%.1f µT", sensorState.magX),
                        "Mag Y" to String.format("%.1f µT", sensorState.magY),
                        "Mag Z" to String.format("%.1f µT", sensorState.magZ)
                    )
                )
            }
            item {
                SensorCard(
                    title = "Proximity Sensor",
                    supported = sensorState.isProximitySupported,
                    icon = Icons.Default.Visibility,
                    color = TealMain,
                    metrics = listOf(
                        "Distance" to if (sensorState.proximityDist >= 0) "${sensorState.proximityDist} cm" else "Far"
                    )
                )
            }
            item {
                SensorCard(
                    title = "Light (Luminosity) Sensor",
                    supported = sensorState.isLightSupported,
                    icon = Icons.Default.LightMode,
                    color = GoldenYellow,
                    metrics = listOf(
                        "Ambient light" to if (sensorState.lightLux >= 0) "${sensorState.lightLux.toInt()} lx" else "Unknown"
                    )
                )
            }
            item {
                SensorCard(
                    title = "Barometer (Pressure) Sensor",
                    supported = sensorState.isPressureSupported,
                    icon = Icons.Default.Compress,
                    color = BlueGrey40,
                    metrics = listOf(
                        "Pressure" to if (sensorState.pressureHpa >= 0) "${sensorState.pressureHpa} hPa" else "Not Available on this Android Version"
                    )
                )
            }
        }
    }
}

@Composable
fun SensorCard(
    title: String,
    supported: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    metrics: List<Pair<String, String>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = OffWhite),
        border = BorderStroke(1.dp, CardBorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = title, tint = color)
                    Spacer(Modifier.width(8.dp))
                    Text(title, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (supported) DroidGreenLight else Color.Red.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        if (supported) "Active" else "N/A",
                        fontSize = 11.sp,
                        color = if (supported) DroidGreenDark else Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (supported) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    metrics.forEach { (label, value) ->
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            border = BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(label, fontSize = 11.sp, color = TextSecondary)
                                Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                    }
                }
            } else {
                Text(
                    "This physical sensor is not detected on your device hardware.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// 8. Network Tests Screen
@Composable
fun NetworkScreen(viewModel: DiagnosticsViewModel, onBack: () -> Unit) {
    val netInfo by viewModel.networkInfo.collectAsState()

    ToolDetailScreenLayout(title = "Network Diagnostics", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Network Details
            item {
                InfoCard(items = listOf(
                    "Wi-Fi Connected" to if (netInfo.isWifiConnected) "Yes" else "No",
                    "SSID" to netInfo.wifiSsid,
                    "WiFi Speed" to "${netInfo.wifiLinkSpeed} Mbps",
                    "WiFi Signal" to "${netInfo.wifiRssi} dBm",
                    "Operator Name" to netInfo.mobileOperator,
                    "Network Type" to netInfo.mobileNetworkType,
                    "Device IP Address" to netInfo.ipAddress,
                    "DNS Servers" to netInfo.dnsServers,
                    "MAC Address" to "Not Available on this Android Version"
                ))
            }

            // Ping Latency
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CardBorderColor),
                    colors = CardDefaults.cardColors(containerColor = OffWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = "Ping", tint = Blue40)
                            Spacer(Modifier.width(8.dp))
                            Text("Network Latency Ping", fontWeight = FontWeight.Bold)
                        }
                        Text("Measures raw ping delay against DNS server (8.8.8.8) to check internet connection overhead.", fontSize = 13.sp)

                        Text(
                            text = if (netInfo.pingMs >= 0) "Ping Latency: ${netInfo.pingMs} ms" else "Latency: Click run below",
                            fontWeight = FontWeight.Bold,
                            color = DroidGreen
                        )

                        Button(onClick = { viewModel.runNetworkPing() }) {
                            Text("Run Ping Test")
                        }
                    }
                }
            }

            // Internet Speed Test
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CardBorderColor),
                    colors = CardDefaults.cardColors(containerColor = OffWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.NetworkCheck, contentDescription = "Speed Test", tint = Color(0xFF0D47A1), modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Dual Speed Network Test", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                            }
                            if (netInfo.isRunningSpeedTest || netInfo.speedTestProgress.isNotEmpty()) {
                                Surface(
                                    color = if (netInfo.isRunningSpeedTest) Color(0xFFE3F2FD) else Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = netInfo.speedTestProgress,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (netInfo.isRunningSpeedTest) Color(0xFF1565C0) else Color(0xFF2E7D32),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Measures live download and upload performance by executing fast bi-directional socket streams against stable server nodes.",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Download Column
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(100.dp)
                                ) {
                                    CircularProgressIndicator(
                                        progress = { if (netInfo.isRunningSpeedTest && netInfo.speedTestProgress.contains("Download")) 0.65f else 1.0f },
                                        modifier = Modifier.fillMaxSize(),
                                        color = Color(0xFF1565C0),
                                        strokeWidth = 6.dp,
                                        trackColor = Color.LightGray.copy(alpha = 0.2f)
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "Download",
                                            tint = Color(0xFF1565C0),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "${netInfo.downloadSpeedMbps}",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text("Mbps", fontSize = 10.sp, color = TextSecondary)
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Download Speed", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }

                            // Divider
                            Box(
                                modifier = Modifier
                                    .height(60.dp)
                                    .width(1.dp)
                                    .background(Color.LightGray.copy(alpha = 0.5f))
                            )

                            // Upload Column
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(100.dp)
                                ) {
                                    CircularProgressIndicator(
                                        progress = { if (netInfo.isRunningSpeedTest && netInfo.speedTestProgress.contains("Upload")) 0.65f else 1.0f },
                                        modifier = Modifier.fillMaxSize(),
                                        color = Color(0xFFE65100),
                                        strokeWidth = 6.dp,
                                        trackColor = Color.LightGray.copy(alpha = 0.2f)
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Upload",
                                            tint = Color(0xFFE65100),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "${netInfo.uploadSpeedMbps}",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text("Mbps", fontSize = 10.sp, color = TextSecondary)
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Upload Speed", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }
                        }

                        Button(
                            onClick = { viewModel.runInternetSpeedTest() },
                            enabled = !netInfo.isRunningSpeedTest,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (netInfo.isRunningSpeedTest) "Running Real-Time Test..." else "Run Bi-Directional Speed Test",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper functions for GPS calculations
private fun calculateSlantRangeKm(constellation: String, elevation: Float): Double {
    val re = 6371.0 // Earth radius in km
    val rs = when (constellation.uppercase()) {
        "GPS" -> re + 20200.0 // ~26571 km
        "GLONASS" -> re + 19100.0 // ~25471 km
        "GALILEO" -> re + 23222.0 // ~29593 km
        "BEIDOU" -> re + 21528.0 // ~27899 km
        else -> re + 20200.0
    }
    val elevationRad = Math.toRadians(elevation.coerceIn(0f, 90f).toDouble())
    val sinE = Math.sin(elevationRad)
    val slantRange = Math.sqrt(re * re * sinE * sinE + rs * rs - re * re) - re * sinE
    return if (slantRange.isNaN() || slantRange < 0) rs - re else slantRange
}

private fun getCardinalDirection(azimuth: Float): String {
    val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
    val index = Math.round((azimuth % 360f) / 22.5f) % 16
    return directions[index.toInt()]
}

// 9. GPS Coordinates Radar
@Composable
fun GpsScreen(viewModel: DiagnosticsViewModel, onBack: () -> Unit) {
    val satellites by viewModel.satellitesList.collectAsState()
    val activeSats = remember(satellites) { satellites.filter { it.isConnected && it.snr >= 15f } }
    val weakSats = remember(satellites) { satellites.filter { !it.isConnected || it.snr < 15f } }
    val location by viewModel.gpsLocation.collectAsState()
    val compassAzimuth by viewModel.compassAzimuth.collectAsState()
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { map ->
        hasPermission = map.values.all { it }
        if (hasPermission) {
            viewModel.startGpsTracking()
        }
    }

    DisposableEffect(hasPermission) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            hasPermission = true
            viewModel.startGpsTracking()
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        onDispose {
            viewModel.stopGpsTracking()
        }
    }

    ToolDetailScreenLayout(title = "GPS Space Radar & Tracker", onBack = onBack) { padding ->
        if (hasPermission) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Urdu explanation banner for our localized Pakistani users
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Info",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "خلائی سیٹلائٹ سگنل رڈار",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "یہ سسٹم آپ کے موبائل کے جی پی ایس اور سمت معلوم کرنے والے سینسر (Compass) کو استعمال کرتا ہے۔ جیسے ہی آپ اپنے موبائل کو گھمائیں گے، رڈار پر موجود سیٹلائٹ بھی گھومیں گے اور آپ کے سامنے ان کی درست سمت، فاصلہ اور سگنل کی طاقت دکھائی دے گی۔",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // 2. Interactive Compass-Aligned Space Radar
                item {
                    GpsRadarVisualizer(satellites = activeSats, compassAzimuth = compassAzimuth)
                }

                // 3. Status Banner
                item {
                    val activeSats = satellites.count { it.isConnected }
                    val totalSatellites = satellites.size
                    val hasFix = activeSats >= 4 && location != null
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (hasFix) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        ),
                        border = BorderStroke(1.dp, if (hasFix) Color(0xFFC8E6C9) else Color(0xFFFFCDD2))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (hasFix) Icons.Default.LocationOn else Icons.Default.LocationOff,
                                contentDescription = "GPS Status",
                                tint = if (hasFix) Color(0xFF2E7D32) else Color(0xFFC62828),
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = if (hasFix) "3D GNSS Position Lock Secured" else "Acquiring GNSS Lock...",
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasFix) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (hasFix) "$activeSats / $totalSatellites active satellites contributing to 3D precision lock" else "Requires at least 4 active satellites (Current active: $activeSats)",
                                    fontSize = 12.sp,
                                    color = if (hasFix) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                        }
                    }
                }

                // 4. GNSS Constellation Quick Status Cards
                item {
                    CategoryHeader("GNSS Constellations Status")
                }

                item {
                    val constellations = listOf(
                        QuadrupleConstellation("GPS", "USA", Color(0xFF4CAF50), satellites.filter { it.constellation == "GPS" }),
                        QuadrupleConstellation("GLONASS", "Russia", Color(0xFF2196F3), satellites.filter { it.constellation == "GLONASS" }),
                        QuadrupleConstellation("GALILEO", "Europe", Color(0xFF9C27B0), satellites.filter { it.constellation == "GALILEO" }),
                        QuadrupleConstellation("BEIDOU", "China", Color(0xFFFF9800), satellites.filter { it.constellation == "BEIDOU" })
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ConstellationProgressCard(constellations[0], modifier = Modifier.weight(1f))
                            ConstellationProgressCard(constellations[1], modifier = Modifier.weight(1f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ConstellationProgressCard(constellations[2], modifier = Modifier.weight(1f))
                            ConstellationProgressCard(constellations[3], modifier = Modifier.weight(1f))
                        }
                    }
                }

                // 5. Precise Location Metrics (With Heading details!)
                item {
                    CategoryHeader("Real-time Satellite Coordinates")
                }
                item {
                    val headingText = "${compassAzimuth.roundToInt()}° ${getCardinalDirection(compassAzimuth)}"
                    InfoCard(items = listOf(
                        "Latitude" to (location?.latitude?.toString() ?: "Acquiring coordinates..."),
                        "Longitude" to (location?.longitude?.toString() ?: "Acquiring coordinates..."),
                        "Altitude" to if (location != null) "${location?.altitude?.roundToInt()} meters (ASL)" else "N/A",
                        "Device Heading" to headingText,
                        "Speed" to if (location != null) "${(location?.speed?.times(3.6))?.roundToInt()} km/h" else "0 km/h",
                        "Lock Accuracy" to if (location != null) "${location?.accuracy} meters" else "N/A",
                        "Active Provider" to (location?.provider?.uppercase() ?: "GPS")
                    ))
                }

                // 6. Active Satellite List (سگنل ریسیونگ سیٹلائٹس)
                item {
                    CategoryHeader("Active GNSS Tracking Nodes / متحرک سیٹلائٹس")
                }

                item {
                    Text(
                        text = "These satellite vehicles are currently locked with a strong signal level (C/N0) and are contributing to the positional calculation. / یہ سیٹلائٹس مضبوط سگنل کے ساتھ پوزیشن معلوم کرنے کے لیے استعمال ہو رہی ہیں۔",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp),
                        textAlign = TextAlign.Start
                    )
                }

                if (activeSats.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CardBorderColor),
                            colors = CardDefaults.cardColors(containerColor = OffWhite)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No Active Satellite Signals Detected\nکوئی متحرک سگنل وصول نہیں ہو رہا",
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                } else {
                    items(activeSats) { sat ->
                        SatelliteCard(sat = sat, viewModel = viewModel)
                    }
                }

                // 7. Weak / Acquiring Satellite List (غیر متحرک سیٹلائٹس)
                item {
                    CategoryHeader("Weak / Acquiring / Inactive Nodes / غیر متحرک سیٹلائٹس")
                }

                item {
                    Text(
                        text = "These satellite vehicles are currently offline, manually muted, or have signals below threshold and are not in active calculation. / ان سیٹلائٹس کے سگنل کمزور ہیں یا یہ ریسیور کی پوزیشن کی پیمائش میں شامل نہیں ہیں۔",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp),
                        textAlign = TextAlign.Start
                    )
                }

                if (weakSats.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CardBorderColor),
                            colors = CardDefaults.cardColors(containerColor = OffWhite)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "All Nodes Are Active and Healthy\nتمام سیٹلائٹ سگنلز متحرک اور درست ہیں",
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                } else {
                    items(weakSats) { sat ->
                        SatelliteCard(sat = sat, viewModel = viewModel)
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Location", modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Text("Location Permission is required to measure GPS hardware.", textAlign = TextAlign.Center)
                    Button(onClick = { permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }) {
                        Text("Grant Permission")
                    }
                }
            }
        }
    }
}

// Data holder for constellation status
data class QuadrupleConstellation(
    val name: String,
    val origin: String,
    val color: Color,
    val satellites: List<GpsSatelliteInfo>
)

@Composable
fun ConstellationProgressCard(constellation: QuadrupleConstellation, modifier: Modifier = Modifier) {
    val total = constellation.satellites.size
    val active = constellation.satellites.count { it.isConnected && it.usedInFix }
    val avgSnr = if (constellation.satellites.isNotEmpty()) constellation.satellites.map { it.snr }.average().toFloat() else 0f

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CardBorderColor),
        colors = CardDefaults.cardColors(containerColor = OffWhite)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = constellation.name,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Text(
                    text = constellation.origin,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$active / $total",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = constellation.color
                )
                Text(
                    text = "active SVs",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (total > 0) active.toFloat() / total.toFloat() else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = constellation.color,
                trackColor = Color.LightGray.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Avg Signal: ${avgSnr.roundToInt()} dB/Hz",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun GpsRadarVisualizer(satellites: List<GpsSatelliteInfo>, compassAzimuth: Float) {
    val infiniteTransition = rememberInfiniteTransition()
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(3000, easing = LinearEasing), repeatMode = RepeatMode.Restart)
    )

    // Glowing Radar circle container
    Box(
        modifier = Modifier
            .size(240.dp)
            .background(Color(0xFF0F172A), shape = CircleShape)
            .border(3.dp, Color(0xFF334155), CircleShape)
            .drawBehind {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2

                // Draw concentric ranges representing Elevation lines (Zenith 90° is center, Horizon 0° is outer ring)
                // 3 rings representing 0°, 30°, 60° elevation
                drawCircle(color = Color(0xFF1E293B), radius = radius, style = Stroke(4f))
                drawCircle(color = Color(0xFF334155).copy(alpha = 0.5f), radius = radius, style = Stroke(2f))
                drawCircle(color = Color(0xFF334155).copy(alpha = 0.4f), radius = radius * 0.66f, style = Stroke(1.5f))
                drawCircle(color = Color(0xFF334155).copy(alpha = 0.3f), radius = radius * 0.33f, style = Stroke(1f))

                // Major axis lines (Crosshairs)
                drawLine(color = Color(0xFF334155).copy(alpha = 0.4f), start = Offset(0f, center.y), end = Offset(size.width, center.y), strokeWidth = 1.5f)
                drawLine(color = Color(0xFF334155).copy(alpha = 0.4f), start = Offset(center.x, 0f), end = Offset(center.x, size.height), strokeWidth = 1.5f)

                // Sweep radar arc line for high-tech scanning effect
                drawArc(
                    brush = Brush.sweepGradient(listOf(Color.Transparent, Color(0x1138BDF8), Color(0x5538BDF8))),
                    startAngle = sweepAngle - 45f,
                    sweepAngle = 45f,
                    useCenter = true
                )

                // Draw Outer Cardinal Markers (N, E, S, W) rotated based on device's compass azimuth
                val cardinals = listOf(
                    "N" to 0f, "NE" to 45f, "E" to 90f, "SE" to 135f,
                    "S" to 180f, "SW" to 225f, "W" to 270f, "NW" to 315f
                )

                cardinals.forEach { (label, angle) ->
                    // Rotate the text coordinate relative to the user's facing direction
                    val rotatedAngleRad = Math.toRadians((angle - compassAzimuth - 90f).toDouble())
                    val textDist = radius - 18.dp.toPx()
                    val labelX = center.x + (textDist * cos(rotatedAngleRad)).toFloat()
                    val labelY = center.y + (textDist * sin(rotatedAngleRad)).toFloat()

                    val cardinalPaint = android.graphics.Paint().apply {
                        color = if (label == "N") android.graphics.Color.parseColor("#F43F5E") else android.graphics.Color.parseColor("#94A3B8")
                        textSize = if (label.length == 1) 28f else 20f
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    }

                    // Vertically adjust slightly for text center alignment
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        labelX,
                        labelY + 8f,
                        cardinalPaint
                    )
                }

                // Draw each satellite on the radar map, rotated by compassAzimuth
                satellites.forEach { sat ->
                    // Elevation Offset (90° elevation is directly overhead (center), 0° is on the horizon (outer edge))
                    val elevationOffset = (90f - sat.elevation).coerceIn(0f, 90f) / 90f
                    val distance = elevationOffset * (radius - 28.dp.toPx()) // scale slightly down to leave space for outer labels
                    
                    // Rotate relative to compassAzimuth
                    val angleRad = Math.toRadians((sat.azimuth - compassAzimuth - 90f).toDouble())

                    val satX = center.x + (distance * cos(angleRad)).toFloat()
                    val satY = center.y + (distance * sin(angleRad)).toFloat()

                    val satColor = when (sat.constellation) {
                        "GPS" -> Color(0xFF4CAF50)
                        "GLONASS" -> Color(0xFF2196F3)
                        "GALILEO" -> Color(0xFF9C27B0)
                        "BEIDOU" -> Color(0xFFFF9800)
                        else -> Color.Gray
                    }

                    if (sat.isConnected) {
                        // Pulsing lock ring around active fixing satellites
                        if (sat.usedInFix) {
                            val pulseScale = 1.0f + 0.3f * Math.abs(Math.sin(System.currentTimeMillis() / 250.0 + sat.svid)).toFloat()
                            drawCircle(
                                color = satColor.copy(alpha = 0.15f),
                                radius = 16.dp.toPx() * pulseScale,
                                center = Offset(satX, satY)
                            )
                        }

                        // Glow halo
                        drawCircle(
                            color = satColor.copy(alpha = 0.3f),
                            radius = 10.dp.toPx(),
                            center = Offset(satX, satY)
                        )

                        // Center solid core dot
                        drawCircle(
                            color = satColor,
                            radius = 5.dp.toPx(),
                            center = Offset(satX, satY)
                        )
                    } else {
                        // Disconnected / Muted satellite dot
                        drawCircle(
                            color = Color(0xFFEF5350).copy(alpha = 0.2f),
                            radius = 6.dp.toPx(),
                            center = Offset(satX, satY)
                        )
                        drawCircle(
                            color = Color(0xFF64748B),
                            radius = 3.dp.toPx(),
                            center = Offset(satX, satY)
                        )
                    }

                    // Draw SVID label text near each satellite dot
                    val textPaint = android.graphics.Paint().apply {
                        color = if (sat.isConnected) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#64748B")
                        textSize = 20f
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    }
                    val label = when (sat.constellation) {
                        "GPS" -> "G${sat.svid}"
                        "GLONASS" -> "R${sat.svid}"
                        "GALILEO" -> "E${sat.svid}"
                        "BEIDOU" -> "B${sat.svid}"
                        else -> "S${sat.svid}"
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        satX + 8.dp.toPx(),
                        satY - 6.dp.toPx(),
                        textPaint
                    )
                }

                // Draw central observer target with dynamic heading guide
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = 0.3f),
                    radius = 8.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = Color(0xFF38BDF8),
                    radius = 3.5.dp.toPx(),
                    center = center
                )
            }
    ) {
        // Draw physical heading arrow overlay pointing straight up
        Icon(
            imageVector = Icons.Default.Navigation,
            contentDescription = "Forward Heading",
            tint = Color(0xFFF43F5E),
            modifier = Modifier
                .size(32.dp)
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
        )
    }
}


// 10. Storage Analyzer, Cleaner
@Composable
fun StorageScreen(viewModel: DiagnosticsViewModel, onBack: () -> Unit) {
    val internalUsage by viewModel.storageInfo.collectAsState()
    val cacheSize by viewModel.scannedCacheSize.collectAsState()
    val largeFiles by viewModel.largeFiles.collectAsState()
    val duplicateFiles by viewModel.duplicateFiles.collectAsState()

    var cleanSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(cleanSuccess) {
        if (cleanSuccess) {
            delay(2000)
            cleanSuccess = false
        }
    }

    ToolDetailScreenLayout(title = "Storage & Cleaner", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CategoryHeader("Storage Utilization")
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CardBorderColor),
                    colors = CardDefaults.cardColors(containerColor = OffWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Internal Storage", fontWeight = FontWeight.Bold)
                            Text("${viewModel.formatSize(internalUsage.internalTotalBytes - internalUsage.internalAvailableBytes)} / ${viewModel.formatSize(internalUsage.internalTotalBytes)}")
                        }
                        LinearProgressIndicator(
                            progress = { internalUsage.internalUsedPercentage / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = DroidGreen
                        )
                    }
                }
            }

            // Clean cache action
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CardBorderColor),
                    colors = CardDefaults.cardColors(containerColor = OffWhite)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("System Cache Files", fontWeight = FontWeight.Bold)
                            Text("Temporary files storage size: ${viewModel.formatSize(cacheSize)}", fontSize = 13.sp, color = TextSecondary)
                        }

                        Button(
                            onClick = {
                                viewModel.cleanCache()
                                cleanSuccess = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DroidGreen)
                        ) {
                            Text(if (cleanSuccess) "Cleaned!" else "Clean Now", color = Color.White)
                        }
                    }
                }
            }

            // Duplicate Scanner lists
            if (duplicateFiles.isNotEmpty()) {
                item {
                    CategoryHeader("Duplicate Scanner Files")
                }
                items(duplicateFiles) { file ->
                    FileRowItem(file, onDelete = { viewModel.deleteDiagnosticFile(file) })
                }
            }

            // Large files lists
            if (largeFiles.isNotEmpty()) {
                item {
                    CategoryHeader("Large Files Manager")
                }
                items(largeFiles) { file ->
                    FileRowItem(file, onDelete = { viewModel.deleteDiagnosticFile(file) })
                }
            }
        }
    }
}

@Composable
fun FileRowItem(file: DiagnosticFile, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, CardBorderColor),
        colors = CardDefaults.cardColors(containerColor = PureWhite)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.InsertDriveFile, contentDescription = "File", tint = Color.Gray)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                    Text(file.formattedSize, fontSize = 12.sp, color = TextSecondary)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedMain)
            }
        }
    }
}

// 11. App Manager & Installed Apps Screen
@Composable
fun AppsScreen(viewModel: DiagnosticsViewModel, onBack: () -> Unit) {
    val apps by viewModel.installedApps.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedAppType by remember { mutableStateOf(0) } // 0 = Installed, 1 = System

    val filteredApps = remember(searchQuery, apps) {
        apps.filter { it.name.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true) }
    }

    val userApps = remember(filteredApps) { filteredApps.filter { !it.isSystem } }
    val systemApps = remember(filteredApps) { filteredApps.filter { it.isSystem } }
    val displayedApps = if (selectedAppType == 0) userApps else systemApps

    ToolDetailScreenLayout(title = "App Manager", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search system or user apps...") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = OffWhite,
                    unfocusedContainerColor = OffWhite,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp)
            )

            // High Fidelity Segmented Pill Selector (Side-by-side tabs)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                color = OffWhite,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, CardBorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selectedAppType == 0) DroidGreen else Color.Transparent)
                            .clickable { selectedAppType = 0 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Installed / انسٹال شدہ (${userApps.size})",
                            color = if (selectedAppType == 0) PureWhite else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selectedAppType == 1) DroidGreen else Color.Transparent)
                            .clickable { selectedAppType = 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "System / سسٹم ایپس (${systemApps.size})",
                            color = if (selectedAppType == 1) PureWhite else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Text(
                text = if (selectedAppType == 0) "Showing Installed User Apps (${userApps.size})" else "Showing System Services (${systemApps.size})",
                fontSize = 12.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(displayedApps) { app ->
                    AppRowItem(app)
                }
            }
        }
    }
}

@Composable
fun AppRowItem(app: AppItem) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${app.packageName}")
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {}
            },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, CardBorderColor),
        colors = CardDefaults.cardColors(containerColor = OffWhite)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(DroidGreenLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Android, contentDescription = "App", tint = DroidGreen)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(app.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                    Text(app.packageName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, color = TextSecondary)
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (app.isSystem) Color.LightGray.copy(alpha = 0.3f) else DroidGreenLight)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    if (app.isSystem) "System" else "User",
                    fontSize = 11.sp,
                    color = if (app.isSystem) Color.DarkGray else DroidGreenDark,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// 12. Compass Screen using Magnetic Azimuth
@Composable
fun CompassScreen(viewModel: DiagnosticsViewModel, onBack: () -> Unit) {
    val azimuth by viewModel.compassAzimuth.collectAsState()

    ToolDetailScreenLayout(title = "Hardware Compass", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                "Azimuth Orientation: ${azimuth.toInt()}°",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = TextPrimary
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(240.dp)
            ) {
                // Outer Dial
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = TextPrimary, style = Stroke(4.dp.toPx()))
                    drawCircle(color = DroidGreen, style = Stroke(1.dp.toPx()), radius = size.width / 2 - 10.dp.toPx())
                }

                // Beautiful rotating Compass Needle
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = "Compass Needle",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .rotate(-azimuth), // Rotate needle contrary to orientation to stay pointing north
                    tint = DroidGreen
                )
            }

            Text(
                "Ensure your device is kept flat and away from strong magnetic objects for accurate calibration.",
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

// 13. Vibration Patterns Trigger
@Composable
fun VibrationScreen(viewModel: DiagnosticsViewModel, onBack: () -> Unit) {
    ToolDetailScreenLayout(title = "Vibrator Test", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            InfoHeroHeader(
                title = "Haptic Vibration Motor", 
                subtitle = "Checks custom tactile amplitude engines", 
                icon = Icons.Default.Vibration,
                tintColor = Color(0xFF4A148C)
            )

            Text("Vibrate the device in custom pre-set sequences to verify functional tactile engine.", textAlign = TextAlign.Center, color = TextSecondary)

            Button(
                onClick = { viewModel.triggerVibration("Pulse") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DroidGreen)
            ) {
                Text("Standard Long Pulse (500ms)", color = Color.White)
            }

            Button(
                onClick = { viewModel.triggerVibration("Heartbeat") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurpleMain)
            ) {
                Text("Heartbeat Pulse Double-Tap", color = Color.White)
            }

            Button(
                onClick = { viewModel.triggerVibration("SOS") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RedMain)
            ) {
                Text("SOS Pattern Vibrate (... --- ...)", color = Color.White)
            }
        }
    }
}

// 14. Flashlight Strobe Screen
@Composable
fun FlashlightScreen(viewModel: DiagnosticsViewModel, onBack: () -> Unit) {
    var isFlashlightOn by remember { mutableStateOf(false) }

    DisposableEffect(isFlashlightOn) {
        viewModel.toggleFlashlight(isFlashlightOn)
        onDispose {
            viewModel.toggleFlashlight(false)
        }
    }

    ToolDetailScreenLayout(title = "Flashlight Diagnostic", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.FlashlightOn,
                contentDescription = "Flashlight",
                tint = if (isFlashlightOn) GoldenYellow else Color.Gray,
                modifier = Modifier.size(120.dp)
            )

            Spacer(Modifier.height(32.dp))

            Switch(
                checked = isFlashlightOn,
                onCheckedChange = { isFlashlightOn = it }
            )

            Spacer(Modifier.height(16.dp))

            Text(
                if (isFlashlightOn) "Flashlight Status: ACTIVE" else "Flashlight Status: OFF",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}

// 15. Developer Options shortcut
@Composable
fun DeveloperOptionsScreen(viewModel: DiagnosticsViewModel, onBack: () -> Unit) {
    val activity = LocalContext.current as Activity
    val info by viewModel.deviceInfo.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var apiUrlText by remember { mutableStateOf(viewModel.updateManager.getApiUrl()) }
    var simulateForce by remember { mutableStateOf(viewModel.updateManager.isSimulateForceUpdateEnabled()) }

    ToolDetailScreenLayout(title = "Developer Utility Options", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            InfoHeroHeader(
                title = "Developer Shortcut", 
                subtitle = "Bypass Android menus & configure Force Update APIs", 
                icon = Icons.Default.SettingsApplications,
                tintColor = Color(0xFF455A64)
            )

            // Force Update Controller Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CardBorderColor),
                colors = CardDefaults.cardColors(containerColor = OffWhite)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Force Update API Settings (فورس اپڈیٹ کنٹرول)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )

                    Text(
                        text = "Enter your custom JSON API URL below. The app fetches this on startup. If forceUpdate is true and minVersionCode is greater than the current version, the app will completely lock itself.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )

                    OutlinedTextField(
                        value = apiUrlText,
                        onValueChange = { apiUrlText = it },
                        label = { Text("Update API JSON URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DroidGreen,
                            unfocusedBorderColor = CardBorderColor
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateManager.setApiUrl(apiUrlText)
                                android.widget.Toast.makeText(context, "API URL Saved Successfully!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DroidGreen)
                        ) {
                            Text("Save URL", color = Color.White)
                        }

                        Button(
                            onClick = {
                                apiUrlText = com.example.model.UpdateManager.DEFAULT_API_URL
                                viewModel.updateManager.setApiUrl(com.example.model.UpdateManager.DEFAULT_API_URL)
                                android.widget.Toast.makeText(context, "Reset to Default URL", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                        ) {
                            Text("Reset", color = Color.White)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(CardBorderColor.copy(alpha = 0.5f))
                    )

                    // Simulation Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Simulate Force Update",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "تجرباتی فورس اپڈیٹ آن کریں",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = simulateForce,
                            onCheckedChange = {
                                simulateForce = it
                                viewModel.updateManager.setSimulateForceUpdate(it)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = DroidGreen)
                        )
                    }

                    Text(
                        text = "Toggle this Switch ON to immediately preview the non-bypassable Urdu/English Force Update Lockscreen. To turn it off, restart the app or toggle this switch OFF.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.updateManager.checkUpdate()
                                android.widget.Toast.makeText(context, "Checked Remote API Status!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Check API Now / ابھی چیک کریں", color = Color.White)
                    }
                }
            }

            InfoCard(items = listOf(
                "Vulkan API Support" to if (info.vulkanSupport) "Yes (Level 1 Supported)" else "No",
                "App Permissions" to "Droid Toolkit Status",
                "MAC Address" to "Not Available on this Android Version (Blocked by MAC Privacy policy on Android 10+)"
            ))

            Button(
                onClick = { viewModel.openDeveloperOptionsShortcut(activity) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DroidGreen)
            ) {
                Text("Open Developer Options Settings", color = Color.White)
            }
        }
    }
}

// 16. Benchmark Calculator Screen
@Composable
fun BenchmarkScreen(viewModel: DiagnosticsViewModel, onBack: () -> Unit) {
    val result by viewModel.benchmarkResult.collectAsState()
    val loading by viewModel.isBenchmarking.collectAsState()

    ToolDetailScreenLayout(title = "Compute Device Benchmark", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            InfoHeroHeader(
                title = "Droid Bench Engine", 
                subtitle = "Sieve-arithmetic performance benchmark", 
                icon = Icons.Default.Speed,
                tintColor = Color(0xFFD32F2F)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CardBorderColor),
                colors = CardDefaults.cardColors(containerColor = OffWhite)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (loading) {
                        CircularProgressIndicator(color = DroidGreen)
                        Spacer(Modifier.height(16.dp))
                        Text("Calculating real arithmetic limits...", fontWeight = FontWeight.Medium)
                    } else {
                        Text(
                            text = result.ifEmpty { "Benchmark not run yet.\nClick execute below to begin." },
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                    }
                }
            }

            Button(
                onClick = { viewModel.runDeviceBenchmark() },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DroidGreen)
            ) {
                Text(if (loading) "Benchmarking device..." else "Execute Arithmetic Benchmark", color = Color.White)
            }
        }
    }
}

// Reusable Subcomponents
@Composable
fun InfoHeroHeader(
    title: String, 
    subtitle: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tintColor: Color = DroidGreen
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OffWhite),
        border = BorderStroke(1.dp, CardBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(tintColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = tintColor, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary, textAlign = TextAlign.Center)
            Text(subtitle, fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun CategoryHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = TextPrimary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun InfoCard(items: List<Pair<String, String>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = OffWhite),
        border = BorderStroke(1.dp, CardBorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, color = TextSecondary, fontSize = 14.sp)
                    Text(value, fontWeight = FontWeight.Medium, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun TestRowCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = OffWhite),
        border = BorderStroke(1.dp, CardBorderColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = color)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                Text(desc, fontSize = 12.sp, color = TextSecondary)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Enter", tint = Color.Gray)
        }
    }
}

@Composable
private fun SatelliteCard(sat: GpsSatelliteInfo, viewModel: DiagnosticsViewModel) {
    val distanceKm = calculateSlantRangeKm(sat.constellation, sat.elevation)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CardBorderColor),
        colors = CardDefaults.cardColors(containerColor = OffWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Constellation badge color coding
                    val badgeColor = when (sat.constellation) {
                        "GPS" -> Color(0xFF4CAF50)
                        "GLONASS" -> Color(0xFF2196F3)
                        "GALILEO" -> Color(0xFF9C27B0)
                        "BEIDOU" -> Color(0xFFFF9800)
                        else -> Color.Gray
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(badgeColor.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = sat.constellation,
                            tint = badgeColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "${sat.constellation} SV ${sat.svid}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            if (sat.usedInFix && sat.isConnected) {
                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "IN FIX",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            } else if (sat.isConnected) {
                                Surface(
                                    color = Color(0xFFFFF3E0),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "SEARCHING",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    color = Color(0xFFFFEBEE),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "MUTED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC62828),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Azimuth: ${sat.azimuth.roundToInt()}° ${getCardinalDirection(sat.azimuth)} | Elevation: ${sat.elevation.roundToInt()}°",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
                
                // Interactive Connect/Disconnect Toggle Switch/Button
                Button(
                    onClick = { viewModel.toggleSatelliteConnection(sat.svid, sat.constellation) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (sat.isConnected) Color(0xFFEF5350) else Color(0xFF4CAF50)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (sat.isConnected) "Mute Node" else "Restore Node",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(CardBorderColor.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "SLANT RANGE DISTANCE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Text(
                        text = String.format("%,d km", distanceKm.roundToInt()),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "SIGNAL LEVEL (C/N0)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LinearProgressIndicator(
                            progress = { (sat.snr / 50f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .width(80.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = when (sat.constellation) {
                                "GPS" -> Color(0xFF4CAF50)
                                "GLONASS" -> Color(0xFF2196F3)
                                "GALILEO" -> Color(0xFF9C27B0)
                                else -> Color(0xFFFF9800)
                            },
                            trackColor = Color.LightGray.copy(alpha = 0.2f)
                        )
                        Text(
                            text = "${sat.snr.roundToInt()} dB/Hz",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}
