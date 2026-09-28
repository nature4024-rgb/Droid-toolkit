package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.model.DiagnosticsViewModel
import com.example.model.UpdateState
import com.example.ui.components.GlRendererView
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: DiagnosticsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val updateState by viewModel.updateState.collectAsState()

            MyApplicationTheme {
                when (val state = updateState) {
                    is UpdateState.ForceUpdateRequired -> {
                        ForceUpdateScreen(
                            minVersionCode = state.minVersionCode,
                            currentVersionCode = state.currentVersionCode,
                            updateUrl = state.updateUrl,
                            messageUrdu = state.messageUrdu,
                            messageEnglish = state.messageEnglish
                        )
                    }
                    else -> {
                        // Invisible GlRendererView to extract actual GPU parameters dynamically
                        GlRendererView { renderer, vendor, version, extensions, maxTex, maxViewW, maxViewH, maxRender ->
                            viewModel.updateGpuInfo(
                                renderer, 
                                vendor, 
                                version, 
                                extensions, 
                                maxTex, 
                                maxViewW, 
                                maxViewH, 
                                maxRender
                            )
                        }

                        val navController = rememberNavController()

                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            NavHost(
                                navController = navController,
                                startDestination = "home",
                                modifier = Modifier.fillMaxSize()
                            ) {
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigate = { route -> navController.navigate(route) }
                            )
                        }
                        composable("device_info") {
                            DeviceInfoScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("cpu_gpu") {
                            CpuGpuScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("display") {
                            DisplayTestScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("battery") {
                            BatteryScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("camera") {
                            CameraTestScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("audio") {
                            AudioScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("sensor") {
                            SensorScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("network") {
                            NetworkScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("gps") {
                            GpsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("storage") {
                            StorageScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("apps") {
                            AppsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("compass") {
                            CompassScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("vibration") {
                            VibrationScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("flashlight") {
                            FlashlightScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("developer") {
                            DeveloperOptionsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("benchmark") {
                            BenchmarkScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
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
