package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.AdNetwork
import com.example.ui.components.MockBannerAd
import com.example.ui.components.MockNativeAdBanner
import com.example.ui.theme.*
import kotlinx.coroutines.launch

// Data structure for HomeScreen Grid
data class ToolCardItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val route: String,
    val bgColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: DiagnosticsViewModel,
    onNavigate: (route: String) -> Unit
) {
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val batteryInfo by viewModel.batteryInfo.collectAsState()
    val ramInfo by viewModel.ramInfo.collectAsState()
    val storageInfo by viewModel.storageInfo.collectAsState()

    var selectedTab by remember { mutableStateOf("home") }

    // Dynamic theme colors state
    var activeTheme by remember { mutableStateOf("blue") } // "blue", "green", "purple", "orange"

    val themePrimary = when (activeTheme) {
        "green" -> Color(0xFF0F8F46)
        "purple" -> Color(0xFF7424CA)
        "orange" -> Color(0xFFBF5000)
        else -> Color(0xFF005AC1) // Classic blue
    }
    val themeLight = when (activeTheme) {
        "green" -> Color(0xFFD0F8E0)
        "purple" -> Color(0xFFF1E3FF)
        "orange" -> Color(0xFFFFE3D1)
        else -> Color(0xFFD3E4FF) // Classic blue light
    }
    val themeDark = when (activeTheme) {
        "green" -> Color(0xFF032F14)
        "purple" -> Color(0xFF25004E)
        "orange" -> Color(0xFF3E1200)
        else -> Color(0xFF001D36) // Classic blue dark
    }

    // Modal Drawer States
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Dialog States
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = BgColor,
                modifier = Modifier.width(310.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Drawer Header with nice gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Brush.horizontalGradient(listOf(themePrimary, themeDark)))
                            .padding(20.dp)
                    ) {
                        Column {
                            Icon(
                                Icons.Default.Memory,
                                contentDescription = "Logo",
                                tint = PureWhite,
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "DROID TOOLKIT",
                                color = PureWhite,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                fontFamily = FontFamily.SansSerif,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                "High Density Diagnostics Engine",
                                color = PureWhite.copy(alpha = 0.85f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Navigation Category
                    Text(
                        "NAVIGATION / نیویگیشن",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )

                    DrawerItem(
                        icon = Icons.Default.Home,
                        label = "Home / ہوم",
                        selected = selectedTab == "home",
                        onClick = {
                            selectedTab = "home"
                            scope.launch { drawerState.close() }
                        },
                        themePrimary = themePrimary,
                        themeLight = themeLight
                    )

                    DrawerItem(
                        icon = Icons.Default.Settings,
                        label = "Preferences / سیٹنگز",
                        selected = selectedTab == "settings",
                        onClick = {
                            selectedTab = "settings"
                            scope.launch { drawerState.close() }
                        },
                        themePrimary = themePrimary,
                        themeLight = themeLight
                    )

                    Spacer(Modifier.height(16.dp))

                    // Benchmarks Category
                    Text(
                        "SYSTEM BENCHMARKS / پنچ اور ٹیسٹ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )

                    DrawerItem(
                        icon = Icons.Default.Speed,
                        label = "Full Benchmark / سسٹم پنچمارک",
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigate("benchmark")
                        },
                        themePrimary = themePrimary,
                        themeLight = themeLight
                    )

                    DrawerItem(
                        icon = Icons.Default.Memory,
                        label = "CPU & GPU Punch / سی پی یو ٹیسٹ",
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigate("cpu_gpu")
                        },
                        themePrimary = themePrimary,
                        themeLight = themeLight
                    )

                    DrawerItem(
                        icon = Icons.Default.Screenshot,
                        label = "Display Diagnostic / ڈسپلے ٹیسٹ",
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigate("display")
                        },
                        themePrimary = themePrimary,
                        themeLight = themeLight
                    )

                    Spacer(Modifier.height(16.dp))

                    // More Category
                    Text(
                        "PREFERENCES & MORE / تھیمز اور معلومات",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )

                    DrawerItem(
                        icon = Icons.Default.Palette,
                        label = "App Themes / خوبصورت تھیمز",
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showThemeDialog = true
                        },
                        themePrimary = themePrimary,
                        themeLight = themeLight
                    )

                    DrawerItem(
                        icon = Icons.Default.Info,
                        label = "About / ایپ کے بارے میں",
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showAboutDialog = true
                        },
                        themePrimary = themePrimary,
                        themeLight = themeLight
                    )

                    DrawerItem(
                        icon = Icons.Default.PrivacyTip,
                        label = "Privacy Policy / رازداری پالیسی",
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showPrivacyDialog = true
                        },
                        themePrimary = themePrimary,
                        themeLight = themeLight
                    )

                    Spacer(Modifier.weight(1f))

                    Text(
                        "Version 1.0.0 Stable",
                        fontSize = 11.sp,
                        color = Slate400,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column(modifier = Modifier.fillMaxWidth().background(BgColor)) {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .shadow(3.dp, RoundedCornerShape(13.dp), spotColor = themePrimary.copy(alpha = 0.25f))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(themePrimary, themePrimary.copy(alpha = 0.85f))
                                            ),
                                            RoundedCornerShape(13.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Memory,
                                        contentDescription = "Droid Toolkit Logo",
                                        tint = PureWhite,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "DROID ",
                                            color = themePrimary,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 19.sp,
                                            fontFamily = FontFamily.SansSerif,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            "TOOLKIT",
                                            color = Slate900,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 19.sp,
                                            fontFamily = FontFamily.SansSerif,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    Text(
                                        "High Density Diagnostics Engine",
                                        fontSize = 10.sp,
                                        color = Slate500,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                    }
                                },
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .clip(CircleShape)
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Slate800)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = BgColor,
                            scrolledContainerColor = BgColor
                        )
                    )
                    // Exactly one Banner Ad at the top of the page
                    MockBannerAd(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        initialNetwork = AdNetwork.ADMOB
                    )
                }
            },
            bottomBar = {
                // Floating rounded navigation container
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .navigationBarsPadding()
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(32.dp),
                            spotColor = Color(0x14000000)
                        ),
                    color = PureWhite,
                    border = BorderStroke(1.dp, Slate200),
                    shape = RoundedCornerShape(32.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tabs = listOf(
                            Triple("home", Icons.Default.Home, "Home"),
                            Triple("reports", Icons.Default.Assignment, "Reports"),
                            Triple("tools", Icons.Default.GridView, "Tools"),
                            Triple("settings", Icons.Default.Settings, "Settings")
                        )
                        tabs.forEach { (tab, icon, label) ->
                            val isSelected = selectedTab == tab
                            val pillBgColor by animateColorAsState(
                                targetValue = if (isSelected) themeLight else Color.Transparent,
                                animationSpec = tween(durationMillis = 200),
                                label = "navPillBg"
                            )
                            val iconColor by animateColorAsState(
                                targetValue = if (isSelected) themePrimary else Slate400,
                                animationSpec = tween(durationMillis = 200),
                                label = "navIconColor"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(pillBgColor)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { selectedTab = tab }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = label,
                                        tint = iconColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    if (isSelected) {
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = themeDark
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            containerColor = BgColor
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedTab) {
                    "home" -> {
                        // Top Information Area: Device Overview Card
                        DeviceOverviewCard(
                            device = deviceInfo,
                            battery = batteryInfo,
                            ram = ramInfo,
                            storage = storageInfo,
                            viewModel = viewModel,
                            themePrimary = themePrimary,
                            themeLight = themeLight,
                            themeDark = themeDark
                        )

                        // Quick Stats: Side by Side Cards
                        QuickStatsBanner(deviceInfo, batteryInfo)

                        // Diagnostics & Tools Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Diagnostics & Tools",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Slate900
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(themeLight)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "16 Modules",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeDark
                                )
                            }
                        }

                        // Tools Grid with 18-22dp corners, pastel accents, subtle borders
                        ToolsGrid(onNavigate = onNavigate)
                    }
                    "reports" -> {
                        ReportsTab(deviceInfo, batteryInfo, ramInfo, storageInfo, viewModel, themePrimary)
                    }
                    "tools" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("All Diagnostics Modules", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Slate900)
                            ToolsGrid(onNavigate = onNavigate)
                        }
                    }
                    "settings" -> {
                        SettingsTab(viewModel, themePrimary)
                    }
                }

                // Exactly one Native Ad at the bottom of the page
                MockNativeAdBanner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    initialNetwork = AdNetwork.FACEBOOK
                )

                // Bottom spacing for floating navigation bar
                Spacer(Modifier.height(72.dp))
            }
        }
    }

    // Dialogs Implementations
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = "About", tint = themePrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("About Droid Toolkit / ہمارے بارے میں", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Droid Toolkit",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = themePrimary
                    )
                    Text(
                        text = "High Density Diagnostics Engine",
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = Slate500
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "ڈروئڈ ٹول کٹ ایک جدید اور طاقتور ڈائیگنوسٹک ایپ ہے جو آپ کے موبائل کے ہارڈ ویئر، پروسیسر، ریم، بیٹری، سینسرز اور دیگر پرزوں کی تفصیلی رپورٹ فراہم کرتی ہے۔",
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Start,
                        color = Slate800
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Version: 1.0.0 (Stable / مستحکم)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate900)
                    Text("Developed via Google AI Studio Build", fontSize = 12.sp, color = Slate500)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = themePrimary)
                ) {
                    Text("Close / بند کریں", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = PureWhite
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PrivacyTip, contentDescription = "Privacy", tint = themePrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Privacy Policy / رازداری پالیسی", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Your Privacy is Secure / رازداری محفوظ ہے",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = themePrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "ہم آپ کے کسی بھی قسم کے ذاتی ڈیٹا یا ہارڈ ویئر پیرامیٹرز کو اپنے سرورز پر منتقل نہیں کرتے اور نہ ہی اکٹھا کرتے ہیں۔ تمام ٹیسٹ اور معلومات آپ کے فون پر مکمل طور پر محفوظ اور آف لائن رہتی ہیں۔",
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Start,
                        color = Slate800
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("✓ 100% Secure & Offline", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themePrimary)
                    Text("✓ No Cloud / Server Transmission", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themePrimary)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showPrivacyDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = themePrimary)
                ) {
                    Text("I Understand / سمجھ گیا", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = PureWhite
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = "Theme", tint = themePrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("App Themes / خوبصورت تھیمز", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Select a curated color palette:",
                        fontSize = 13.sp,
                        color = Slate500
                    )

                    val themes = listOf(
                        Triple("blue", "Classic Blue / کلاسک نیلا", Color(0xFF005AC1)),
                        Triple("green", "Emerald Green / زمرد ہرا", Color(0xFF0F8F46)),
                        Triple("purple", "Royal Purple / شاہی جامنی", Color(0xFF7424CA)),
                        Triple("orange", "Sunset Orange / مالٹا تھیم", Color(0xFFBF5000))
                    )

                    themes.forEach { (key, name, color) ->
                        val isSelected = activeTheme == key
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) themeLight else Color.Transparent)
                                .clickable { activeTheme = key }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(color, CircleShape)
                                        .border(2.dp, PureWhite, CircleShape)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = name,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) themeDark else Slate800,
                                    fontSize = 14.sp
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = themePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showThemeDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = themePrimary)
                ) {
                    Text("Done / مکمل", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = PureWhite
        )
    }
}

@Composable
fun DrawerItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    themePrimary: Color,
    themeLight: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable { onClick() },
        color = if (selected) themeLight else Color.Transparent,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) themePrimary else Slate500,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = label,
                color = if (selected) themePrimary else Slate800,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun DeviceOverviewCard(
    device: DeviceInfo,
    battery: BatteryInfo,
    ram: RamInfo,
    storage: StorageInfoState,
    viewModel: DiagnosticsViewModel,
    themePrimary: Color,
    themeLight: Color,
    themeDark: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = Color(0x0C000000)
            ),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Slate200)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Top Row: Device icon, Name & Health Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(themeLight, Slate100)
                                ),
                                RoundedCornerShape(13.dp)
                            )
                            .border(1.dp, Slate200, RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PhoneAndroid,
                            contentDescription = "Device",
                            tint = themePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = device.name.ifEmpty { "Android Device" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Android ${device.androidVersion}",
                                fontSize = 12.sp,
                                color = Slate500,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = " • ",
                                fontSize = 12.sp,
                                color = Slate400
                            )
                            Text(
                                text = "SDK ${device.sdkVersion}",
                                fontSize = 12.sp,
                                color = Slate500,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }

                // Health status badge
                val isGoodHealth = battery.health.equals("Good", ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isGoodHealth) Color(0xFFECFDF5) else Color(0xFFFEF3C7))
                        .border(
                            1.dp,
                            if (isGoodHealth) Color(0xFFA7F3D0) else Color(0xFFFDE68A),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (isGoodHealth) Color(0xFF10B981) else Color(0xFFF59E0B),
                                    CircleShape
                                )
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${battery.health.uppercase()} HEALTH",
                            color = if (isGoodHealth) Color(0xFF047857) else Color(0xFFB45309),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Slate100, thickness = 1.dp)
            Spacer(Modifier.height(14.dp))

            // 3 Overview Metrics Cards (Battery, RAM, Storage)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Battery metric
                val battPct = battery.percentage.coerceIn(0, 100)
                OverviewMetricBox(
                    modifier = Modifier.weight(1f),
                    title = "Battery",
                    value = "$battPct%",
                    icon = Icons.Default.BatteryChargingFull,
                    accentColor = Color(0xFF10B981),
                    progress = battPct / 100f
                )

                // RAM metric
                val ramUsed = viewModel.formatSize(ram.totalRamBytes - ram.availableRamBytes).replace(" GB", "G").replace(" MB", "M")
                val ramTotal = viewModel.formatSize(ram.totalRamBytes).replace(" GB", "G").replace(" MB", "M")
                val ramRatio = if (ram.totalRamBytes > 0) {
                    ((ram.totalRamBytes - ram.availableRamBytes).toFloat() / ram.totalRamBytes.toFloat()).coerceIn(0f, 1f)
                } else 0.5f
                OverviewMetricBox(
                    modifier = Modifier.weight(1.15f),
                    title = "RAM",
                    value = "$ramUsed / $ramTotal",
                    icon = Icons.Default.Memory,
                    accentColor = Color(0xFF8B5CF6),
                    progress = ramRatio
                )

                // Storage metric
                val storPct = storage.internalUsedPercentage.coerceIn(0f, 100f)
                OverviewMetricBox(
                    modifier = Modifier.weight(1f),
                    title = "Storage",
                    value = "${storPct.toInt()}%",
                    icon = Icons.Default.Storage,
                    accentColor = Color(0xFFF59E0B),
                    progress = storPct / 100f
                )
            }
        }
    }
}

@Composable
fun OverviewMetricBox(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    progress: Float
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Slate50)
            .border(1.dp, Slate200, RoundedCornerShape(16.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate500,
                    letterSpacing = 0.5.sp
                )
                Icon(
                    icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = accentColor,
                trackColor = Slate200
            )
        }
    }
}

@Composable
fun QuickStatsBanner(device: DeviceInfo, battery: BatteryInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickStatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.FlashOn,
            label = "CPU TEMP",
            value = "${battery.temperature}°C",
            iconBgColor = Color(0xFFEFF6FF),
            iconColor = Color(0xFF2563EB)
        )
        QuickStatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Tv,
            label = "SCREEN",
            value = if (device.refreshRate > 0) "${device.refreshRate.toInt()} Hz" else "120 Hz",
            iconBgColor = Color(0xFFFEF2F2),
            iconColor = Color(0xFFE11D48)
        )
    }
}

@Composable
fun QuickStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    iconBgColor: Color,
    iconColor: Color
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color(0x0A000000)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, Slate200)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(iconBgColor, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = Slate400,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            }
        }
    }
}

@Composable
fun ToolsGrid(onNavigate: (route: String) -> Unit) {
    // 16 tools with pastel accent colors, modern rounded cards, subtle border and elevation
    val items = listOf(
        ToolCardItem("Info", "Device & OS", Icons.Default.PhoneAndroid, Color(0xFF0284C7), "device_info", Color(0xFFE0F2FE)),
        ToolCardItem("CPU", "SoC & Cores", Icons.Default.Memory, Color(0xFF7C3AED), "cpu_gpu", Color(0xFFEDE9FE)),
        ToolCardItem("Display", "Screen & FPS", Icons.Default.Screenshot, Color(0xFF0891B2), "display", Color(0xFFCFFAFE)),
        ToolCardItem("Battery", "Health & Level", Icons.Default.BatteryChargingFull, Color(0xFF16A34A), "battery", Color(0xFFDCFCE7)),
        ToolCardItem("Camera", "Sensors & Flash", Icons.Default.CameraAlt, Color(0xFF0D9488), "camera", Color(0xFFCCFBF1)),
        ToolCardItem("Audio", "Speaker & Mic", Icons.Default.VolumeUp, Color(0xFFEA580C), "audio", Color(0xFFFFEDD5)),
        ToolCardItem("Sensors", "Gyro & Motion", Icons.Default.Sensors, Color(0xFFDB2777), "sensor", Color(0xFFFCE7F3)),
        ToolCardItem("Ping", "WiFi & Latency", Icons.Default.Wifi, Color(0xFF2563EB), "network", Color(0xFFDBEAFE)),
        ToolCardItem("GPS", "Satellites & Fix", Icons.Default.LocationOn, Color(0xFFDC2626), "gps", Color(0xFFFEE2E2)),
        ToolCardItem("Storage", "Internal & Disk", Icons.Default.Storage, Color(0xFFD97706), "storage", Color(0xFFFEF3C7)),
        ToolCardItem("Apps", "Packages & RAM", Icons.Default.Apps, Color(0xFF4F46E5), "apps", Color(0xFFE0E7FF)),
        ToolCardItem("Compass", "Azimuth & Pole", Icons.Default.Explore, Color(0xFFE11D48), "compass", Color(0xFFFFE4E6)),
        ToolCardItem("Vibrate", "Haptic Engine", Icons.Default.Vibration, Color(0xFF9333EA), "vibration", Color(0xFFF3E8FF)),
        ToolCardItem("Flashlight", "Torch Strobe", Icons.Default.FlashlightOn, Color(0xFFCA8A04), "flashlight", Color(0xFFFEF9C3)),
        ToolCardItem("Developer", "System Flags", Icons.Default.SettingsApplications, Color(0xFF475569), "developer", Color(0xFFF1F5F9)),
        ToolCardItem("Benchmark", "Stress Score", Icons.Default.Speed, Color(0xFFBE123C), "benchmark", Color(0xFFFFE4E6))
    )

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = when {
            maxWidth >= 840.dp -> 6
            maxWidth >= 600.dp -> 5
            else -> 4
        }

        val rows = items.chunked(columns)
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { item ->
                        ToolCard(
                            item = item,
                            modifier = Modifier.weight(1f),
                            onNavigate = onNavigate
                        )
                    }
                    if (rowItems.size < columns) {
                        repeat(columns - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToolCard(
    item: ToolCardItem,
    modifier: Modifier = Modifier,
    onNavigate: (route: String) -> Unit
) {
    Card(
        modifier = modifier
            .testTag(item.route)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = Color(0x0A000000)
            )
            .clickable { onNavigate(item.route) },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, Slate200)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(item.bgColor, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    item.icon,
                    contentDescription = item.title,
                    tint = item.color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = item.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.subtitle,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = Slate400,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ReportsTab(
    device: DeviceInfo,
    battery: BatteryInfo,
    ram: RamInfo,
    storage: StorageInfoState,
    viewModel: DiagnosticsViewModel,
    themePrimary: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Device Diagnostics Report", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Slate900)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, shape = RoundedCornerShape(22.dp), spotColor = Color(0x0A000000)),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            border = BorderStroke(1.dp, Slate200)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF10B981), CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Overall Status: PASS (Hardware healthy)",
                        fontWeight = FontWeight.Bold,
                        color = themePrimary,
                        fontSize = 15.sp
                    )
                }

                HorizontalDivider(color = Slate100)

                ReportItem("CPU Engine", "${device.cpuModel} SoC Cores")
                ReportItem("Display Frame", "Refresh rate matching display driver")
                ReportItem("Sensors Status", "Primary Gyro, Compass & Accelerometer Active")
                ReportItem("Storage Blocks", "Cache scanner files optimized")
                ReportItem("Battery health", "${battery.health} (Lipo technology)")
            }
        }
    }
}

@Composable
fun ReportItem(title: String, status: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Slate700)
        Text(status, color = Slate500, fontSize = 13.sp)
    }
}

@Composable
fun SettingsTab(viewModel: DiagnosticsViewModel, themePrimary: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Toolkit Preferences", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Slate900)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, shape = RoundedCornerShape(22.dp), spotColor = Color(0x0A000000)),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            border = BorderStroke(1.dp, Slate200)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("App version: 1.0.0 (Production)", fontWeight = FontWeight.Bold, color = themePrimary, fontSize = 15.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Droid Toolkit utilizes secure Android system APIs. It does not collect or transmit personal hardware parameters.",
                    fontSize = 13.sp,
                    color = Slate500,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
