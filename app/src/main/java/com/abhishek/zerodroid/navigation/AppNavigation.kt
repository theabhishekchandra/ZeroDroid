package com.abhishek.zerodroid.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.abhishek.zerodroid.core.ui.EthicalUseDialog
import com.abhishek.zerodroid.core.ui.FeatureHelpSheet
import com.abhishek.zerodroid.core.ui.HelpContent
import com.abhishek.zerodroid.features.dashboard.DashboardScreen
import com.abhishek.zerodroid.features.ble.ui.BleScreen
import com.abhishek.zerodroid.features.ble.ui.GattExplorerScreen
import com.abhishek.zerodroid.features.bluetooth_classic.ui.BluetoothClassicScreen
import com.abhishek.zerodroid.features.bluetooth_tracker.ui.BluetoothTrackerScreen
import com.abhishek.zerodroid.features.camera.ui.QrScannerScreen
import com.abhishek.zerodroid.features.celltower.ui.CellTowerScreen
import com.abhishek.zerodroid.features.deauth_detector.ui.DeauthDetectorScreen
import com.abhishek.zerodroid.features.emf_mapper.ui.EmfMapperScreen
import com.abhishek.zerodroid.features.gps.ui.GpsScreen
import com.abhishek.zerodroid.features.gps_spoof_detector.ui.GpsSpoofScreen
import com.abhishek.zerodroid.features.hidden_camera.ui.HiddenCameraScreen
import com.abhishek.zerodroid.features.ir.ui.IrScreen
import com.abhishek.zerodroid.features.network_scanner.ui.NetworkScannerScreen
import com.abhishek.zerodroid.features.nfc.ui.NfcScreen
import com.abhishek.zerodroid.features.privacy_score.ui.PrivacyScoreScreen
import com.abhishek.zerodroid.features.proximity_radar.ui.ProximityRadarScreen
import com.abhishek.zerodroid.features.rf_bug_sweeper.ui.RfBugSweeperScreen
import com.abhishek.zerodroid.features.rogue_ap_detector.ui.RogueApScreen
import com.abhishek.zerodroid.features.sdr.ui.SdrScreen
import com.abhishek.zerodroid.features.sensors.ui.SensorScreen
import com.abhishek.zerodroid.features.signal_logger.ui.SignalLoggerScreen
import com.abhishek.zerodroid.features.ultrasonic.ui.UltrasonicScreen
import com.abhishek.zerodroid.features.usb.ui.UsbScreen
import com.abhishek.zerodroid.features.usbcamera.ui.UsbCameraScreen
import com.abhishek.zerodroid.features.uwb.ui.UwbScreen
import com.abhishek.zerodroid.features.wardriving.ui.WardrivingScreen
import com.abhishek.zerodroid.features.wifi.ui.WifiScreen
import com.abhishek.zerodroid.features.wifi_direct.ui.WifiDirectScreen
import com.abhishek.zerodroid.features.wifiaware.ui.WifiAwareScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import kotlinx.coroutines.launch

private const val ANIM_DURATION = 300

private val enterTransition: EnterTransition =
    fadeIn(animationSpec = tween(ANIM_DURATION)) +
            slideInHorizontally(
                initialOffsetX = { it / 6 },
                animationSpec = tween(ANIM_DURATION)
            )

private val exitTransition: ExitTransition =
    fadeOut(animationSpec = tween(ANIM_DURATION)) +
            slideOutHorizontally(
                targetOffsetX = { -it / 6 },
                animationSpec = tween(ANIM_DURATION)
            )

private val popEnterTransition: EnterTransition =
    fadeIn(animationSpec = tween(ANIM_DURATION)) +
            slideInHorizontally(
                initialOffsetX = { -it / 6 },
                animationSpec = tween(ANIM_DURATION)
            )

private val popExitTransition: ExitTransition =
    fadeOut(animationSpec = tween(ANIM_DURATION)) +
            slideOutHorizontally(
                targetOffsetX = { it / 6 },
                animationSpec = tween(ANIM_DURATION)
            )

private fun routeToHelpKey(route: String?): String? = when (route) {
    ZeroDroidScreen.Sensors.route -> "sensors"
    ZeroDroidScreen.Wifi.route -> "wifi"
    ZeroDroidScreen.Ble.route -> "ble"
    ZeroDroidScreen.Nfc.route -> "nfc"
    ZeroDroidScreen.Ir.route -> "ir"
    ZeroDroidScreen.Uwb.route -> "uwb"
    ZeroDroidScreen.Usb.route -> "usb"
    ZeroDroidScreen.Sdr.route -> "sdr"
    ZeroDroidScreen.Camera.route -> "qrscanner"
    ZeroDroidScreen.Ultrasonic.route -> "ultrasonic"
    ZeroDroidScreen.Wardriving.route -> "wardriving"
    ZeroDroidScreen.WifiAware.route -> "wifiaware"
    ZeroDroidScreen.CellTower.route -> "celltower"
    ZeroDroidScreen.UsbCamera.route -> "usbcamera"
    ZeroDroidScreen.Gps.route -> "gps"
    ZeroDroidScreen.BluetoothClassic.route -> "bluetooth_classic"
    ZeroDroidScreen.WifiDirect.route -> "wifi_direct"
    ZeroDroidScreen.HiddenCamera.route -> "hidden_camera"
    ZeroDroidScreen.GpsSpoofDetector.route -> "gps_spoof_detector"
    ZeroDroidScreen.BluetoothTracker.route -> "bluetooth_tracker"
    ZeroDroidScreen.RogueAp.route -> "rogue_ap"
    ZeroDroidScreen.NetworkScanner.route -> "network_scanner"
    ZeroDroidScreen.RfBugSweeper.route -> "rf_bug_sweeper"
    ZeroDroidScreen.ProximityRadar.route -> "proximity_radar"
    ZeroDroidScreen.PrivacyScore.route -> "privacy_score"
    ZeroDroidScreen.DeauthDetector.route -> "deauth_detector"
    ZeroDroidScreen.EmfMapper.route -> "emf_mapper"
    ZeroDroidScreen.SignalLogger.route -> "signal_logger"
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentTitle = ZeroDroidScreen.all
        .find { it.route == currentRoute }?.title ?: "ZeroDroid"

    var showHelp by remember { mutableStateOf(false) }
    val helpKey = routeToHelpKey(currentRoute)

    EthicalUseDialog()

    if (showHelp && helpKey != null) {
        FeatureHelpSheet(
            featureKey = helpKey,
            onDismiss = { showHelp = false }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    currentRoute = currentRoute,
                    onScreenSelected = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "> $currentTitle",
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        if (helpKey != null && HelpContent.features.containsKey(helpKey)) {
                            IconButton(onClick = { showHelp = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.HelpOutline,
                                    contentDescription = "Help",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = ZeroDroidScreen.Dashboard.route,
                modifier = Modifier.padding(paddingValues),
                enterTransition = { enterTransition },
                exitTransition = { exitTransition },
                popEnterTransition = { popEnterTransition },
                popExitTransition = { popExitTransition }
            ) {
                // Dashboard
                composable(ZeroDroidScreen.Dashboard.route) {
                    DashboardScreen(
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                // Original screens
                composable(ZeroDroidScreen.Sensors.route) { SensorScreen() }
                composable(ZeroDroidScreen.Wifi.route) { WifiScreen() }
                composable(ZeroDroidScreen.Ble.route) { BleScreen() }
                composable(ZeroDroidScreen.Nfc.route) { NfcScreen() }
                composable(ZeroDroidScreen.Ir.route) { IrScreen() }
                composable(ZeroDroidScreen.Uwb.route) { UwbScreen() }
                composable(ZeroDroidScreen.Usb.route) { UsbScreen() }
                composable(ZeroDroidScreen.Sdr.route) { SdrScreen() }
                composable(ZeroDroidScreen.Camera.route) { QrScannerScreen() }
                composable(ZeroDroidScreen.Ultrasonic.route) { UltrasonicScreen() }
                composable(ZeroDroidScreen.Wardriving.route) { WardrivingScreen() }
                composable(ZeroDroidScreen.WifiAware.route) { WifiAwareScreen() }
                composable(ZeroDroidScreen.CellTower.route) { CellTowerScreen() }
                composable(ZeroDroidScreen.UsbCamera.route) { UsbCameraScreen() }
                composable(ZeroDroidScreen.Gps.route) { GpsScreen() }
                composable(ZeroDroidScreen.BluetoothClassic.route) { BluetoothClassicScreen() }
                composable(ZeroDroidScreen.WifiDirect.route) { WifiDirectScreen() }

                // Security tools
                composable(ZeroDroidScreen.HiddenCamera.route) { HiddenCameraScreen() }
                composable(ZeroDroidScreen.GpsSpoofDetector.route) { GpsSpoofScreen() }
                composable(ZeroDroidScreen.BluetoothTracker.route) { BluetoothTrackerScreen() }
                composable(ZeroDroidScreen.RogueAp.route) { RogueApScreen() }
                composable(ZeroDroidScreen.NetworkScanner.route) { NetworkScannerScreen() }
                composable(ZeroDroidScreen.RfBugSweeper.route) { RfBugSweeperScreen() }
                composable(ZeroDroidScreen.ProximityRadar.route) { ProximityRadarScreen() }
                composable(ZeroDroidScreen.PrivacyScore.route) { PrivacyScoreScreen() }
                composable(ZeroDroidScreen.DeauthDetector.route) { DeauthDetectorScreen() }
                composable(ZeroDroidScreen.EmfMapper.route) { EmfMapperScreen() }
                composable(ZeroDroidScreen.SignalLogger.route) { SignalLoggerScreen() }

                // Sub-screens
                composable(
                    route = "gatt_explorer/{address}/{name}",
                    arguments = listOf(
                        navArgument("address") { type = NavType.StringType },
                        navArgument("name") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val address = backStackEntry.arguments?.getString("address") ?: return@composable
                    val name = backStackEntry.arguments?.getString("name")
                    GattExplorerScreen(
                        deviceAddress = address,
                        deviceName = name,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
