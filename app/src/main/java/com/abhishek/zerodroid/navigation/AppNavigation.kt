package com.abhishek.zerodroid.navigation

import android.net.Uri
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.abhishek.zerodroid.core.debug.DemoDataAction
import com.abhishek.zerodroid.core.ui.EthicalUseDialog
import com.abhishek.zerodroid.core.ui.FeatureHelpSheet
import com.abhishek.zerodroid.core.ui.HelpContent
import com.abhishek.zerodroid.core.ui.zd.ZdHeader
import com.abhishek.zerodroid.core.ui.zd.ZdIconButton
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdSnackbar
import com.abhishek.zerodroid.features.alert_center.ui.AlertCenterScreen
import com.abhishek.zerodroid.features.ble.ui.BleScreen
import com.abhishek.zerodroid.features.ble.ui.GattExplorerScreen
import com.abhishek.zerodroid.features.bluetooth_classic.ui.BluetoothClassicScreen
import com.abhishek.zerodroid.features.bluetooth_tracker.ui.BluetoothTrackerScreen
import com.abhishek.zerodroid.features.camera.ui.QrScannerScreen
import com.abhishek.zerodroid.features.celltower.ui.CellTowerScreen
import com.abhishek.zerodroid.features.dashboard.DashboardScreen
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
import com.abhishek.zerodroid.features.tools.ToolsScreen
import com.abhishek.zerodroid.features.ultrasonic.ui.UltrasonicScreen
import com.abhishek.zerodroid.features.usb.ui.UsbScreen
import com.abhishek.zerodroid.features.usbcamera.ui.UsbCameraScreen
import com.abhishek.zerodroid.features.uwb.ui.UwbScreen
import com.abhishek.zerodroid.features.wardriving.ui.WardrivingScreen
import com.abhishek.zerodroid.features.wifi.ui.WifiScreen
import com.abhishek.zerodroid.features.wifi_direct.ui.WifiDirectScreen
import com.abhishek.zerodroid.features.wifiaware.ui.WifiAwareScreen
import com.abhishek.zerodroid.ui.theme.ZdColors
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

/** Tools reachable from the bottom bar, in bar order. */
internal val bottomTabs: List<BottomTab> = listOf(
    BottomTab(ZeroDroidScreen.Dashboard.route, "Home", ZdIcons.Home),
    BottomTab(ZeroDroidScreen.Tools.route, "Tools", ZdIcons.Grid),
    BottomTab(ZeroDroidScreen.RfBugSweeper.route, "Sweep", ZdIcons.Sweep, emphasized = true),
    BottomTab(ZeroDroidScreen.AlertCenter.route, "Alerts", ZdIcons.Bell, showsAlertBadge = true)
)

/** Routes that show the bottom bar. The sweep tab opens a full tool screen, so it hides it. */
private val tabRoutes = setOf(
    ZeroDroidScreen.Dashboard.route,
    ZeroDroidScreen.Tools.route,
    ZeroDroidScreen.AlertCenter.route
)

/** Opens a destination from a tab or a card, keeping Home as the single root. */
private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { inclusive = false }
        launchSingleTop = true
    }
}

@Composable
fun AppNavigation(shell: AppShellViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val alertCount by shell.alertCount.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    EthicalUseDialog()

    Scaffold(
        containerColor = ZdColors.Bg,
        snackbarHost = { SnackbarHost(snackbarHostState) { ZdSnackbar(it) } },
        bottomBar = {
            if (currentRoute in tabRoutes) {
                ZdBottomBar(
                    tabs = bottomTabs,
                    currentRoute = currentRoute,
                    alertCount = alertCount,
                    onSelect = { navController.navigateTopLevel(it) }
                )
            }
        }
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
            composable(ZeroDroidScreen.Dashboard.route) {
                DashboardScreen(onNavigate = { navController.navigateTopLevel(it) })
            }
            composable(ZeroDroidScreen.Tools.route) {
                ToolsScreen(
                    onOpenTool = { navController.navigate(it.route) { launchSingleTop = true } },
                    onPinChanged = { tool, pinned ->
                        scope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(
                                if (pinned) "${tool.name} pinned to Home" else "${tool.name} removed from Home"
                            )
                        }
                    }
                )
            }
            composable(ZeroDroidScreen.AlertCenter.route) {
                Column(Modifier.fillMaxSize()) {
                    ZdHeader(path = "/alerts", title = "Alerts")
                    Box(Modifier.weight(1f)) { AlertCenterScreen() }
                }
            }

            tool(ZeroDroidScreen.Sensors, navController) { SensorScreen() }
            tool(ZeroDroidScreen.Wifi, navController) { WifiScreen() }
            tool(ZeroDroidScreen.Ble, navController) {
                BleScreen(onOpenDevice = { address, name ->
                    navController.navigate("gatt_explorer/${Uri.encode(address)}/${Uri.encode(name ?: "")}")
                })
            }
            tool(ZeroDroidScreen.Nfc, navController) { NfcScreen() }
            tool(ZeroDroidScreen.Ir, navController) { IrScreen() }
            tool(ZeroDroidScreen.Uwb, navController) { UwbScreen() }
            tool(ZeroDroidScreen.Usb, navController) { UsbScreen() }
            tool(ZeroDroidScreen.Sdr, navController) { SdrScreen() }
            tool(ZeroDroidScreen.Camera, navController) { QrScannerScreen() }
            tool(ZeroDroidScreen.Ultrasonic, navController) { UltrasonicScreen() }
            tool(ZeroDroidScreen.Wardriving, navController) { WardrivingScreen() }
            tool(ZeroDroidScreen.WifiAware, navController) { WifiAwareScreen() }
            tool(ZeroDroidScreen.CellTower, navController) { CellTowerScreen() }
            tool(ZeroDroidScreen.UsbCamera, navController) { UsbCameraScreen() }
            tool(ZeroDroidScreen.Gps, navController) { GpsScreen() }
            tool(ZeroDroidScreen.BluetoothClassic, navController) { BluetoothClassicScreen() }
            tool(ZeroDroidScreen.WifiDirect, navController) { WifiDirectScreen() }
            tool(ZeroDroidScreen.HiddenCamera, navController) { HiddenCameraScreen() }
            tool(ZeroDroidScreen.GpsSpoofDetector, navController) { GpsSpoofScreen() }
            tool(ZeroDroidScreen.BluetoothTracker, navController) { BluetoothTrackerScreen() }
            tool(ZeroDroidScreen.RogueAp, navController) { RogueApScreen() }
            tool(ZeroDroidScreen.NetworkScanner, navController) { NetworkScannerScreen() }
            tool(ZeroDroidScreen.RfBugSweeper, navController) { RfBugSweeperScreen() }
            tool(ZeroDroidScreen.ProximityRadar, navController) { ProximityRadarScreen() }
            tool(ZeroDroidScreen.PrivacyScore, navController) { PrivacyScoreScreen() }
            tool(ZeroDroidScreen.DeauthDetector, navController) { DeauthDetectorScreen() }
            tool(ZeroDroidScreen.EmfMapper, navController) { EmfMapperScreen() }
            tool(ZeroDroidScreen.SignalLogger, navController) { SignalLoggerScreen() }

            composable(
                route = "gatt_explorer/{address}/{name}",
                arguments = listOf(
                    navArgument("address") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val address = backStackEntry.arguments?.getString("address") ?: return@composable
                val name = backStackEntry.arguments?.getString("name")?.takeIf { it.isNotBlank() }
                val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                Column(Modifier.fillMaxSize()) {
                    ZdHeader(
                        path = "/tools/ble/${(name ?: address).lowercase().replace(' ', '-')}",
                        title = "GATT Explorer",
                        // Routed through the dispatcher so an open characteristic closes first.
                        onBack = { backDispatcher?.onBackPressed() }
                    )
                    Box(Modifier.weight(1f)) {
                        GattExplorerScreen(deviceAddress = address, deviceName = name)
                    }
                }
            }
        }
    }
}

/**
 * Registers a tool destination wrapped in the shared tool frame: terminal-path header with
 * back, help and (debug) demo-data actions above the tool's own content.
 */
private fun NavGraphBuilder.tool(
    screen: ZeroDroidScreen,
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    composable(screen.route) {
        val info = ToolCatalog.forRoute(screen.route)
        var showHelp by rememberSaveable { mutableStateOf(false) }
        val hasHelp = HelpContent.features.containsKey(screen.route)

        Column(Modifier.fillMaxSize()) {
            ZdHeader(
                path = info?.path ?: "/tools/${screen.route}",
                title = info?.name ?: screen.title,
                onBack = { if (!navController.popBackStack()) navController.navigateTopLevel(ZeroDroidScreen.Dashboard.route) }
            ) {
                DemoDataAction(route = screen.route)
                if (hasHelp) {
                    ZdIconButton(ZdIcons.Help, contentDescription = "How this tool works", onClick = { showHelp = true })
                }
            }
            Box(Modifier.weight(1f)) { content() }
        }

        if (showHelp) {
            FeatureHelpSheet(
                featureKey = screen.route,
                icon = info?.icon ?: ZdIcons.Help,
                onDismiss = { showHelp = false }
            )
        }
    }
}
