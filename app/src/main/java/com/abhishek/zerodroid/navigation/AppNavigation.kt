package com.abhishek.zerodroid.navigation

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import com.abhishek.zerodroid.core.ui.zd.ZdStatePanel
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.abhishek.zerodroid.core.ui.FeatureHelpSheet
import com.abhishek.zerodroid.core.ui.HelpContent
import com.abhishek.zerodroid.core.ui.rememberEthicalAgreement
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
import com.abhishek.zerodroid.features.locate.ui.LocateScreen
import com.abhishek.zerodroid.features.onboarding.OnboardingScreen
import com.abhishek.zerodroid.features.rf_bug_sweeper.ui.RfBugSweeperScreen
import com.abhishek.zerodroid.features.search.SearchScreen
import com.abhishek.zerodroid.features.settings.SettingsScreen
import com.abhishek.zerodroid.features.watch.ui.RulesScreen
import com.abhishek.zerodroid.features.sweep.domain.SweepPreset
import com.abhishek.zerodroid.features.sweep.ui.SweepPresetsScreen
import com.abhishek.zerodroid.features.sweep.ui.SweepRunScreen
import com.abhishek.zerodroid.features.rogue_ap_detector.ui.RogueApScreen
import com.abhishek.zerodroid.features.sdr.ui.SdrScreen
import com.abhishek.zerodroid.features.sensors.ui.SensorScreen
import com.abhishek.zerodroid.features.signal_logger.ui.SignalLoggerScreen
import com.abhishek.zerodroid.features.tools.ToolsScreen
import com.abhishek.zerodroid.features.sessions.ui.CompareScreen
import com.abhishek.zerodroid.features.sessions.ui.SessionDetailScreen
import com.abhishek.zerodroid.features.sessions.ui.SessionsScreen
import com.abhishek.zerodroid.features.devices.DeviceDetailScreen
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
    BottomTab(ZeroDroidScreen.Sweep.route, "Sweep", ZdIcons.Sweep, emphasized = true),
    BottomTab(ZeroDroidScreen.AlertCenter.route, "Alerts", ZdIcons.Bell, showsAlertBadge = true),
    BottomTab(ZeroDroidScreen.Sessions.route, "Sessions", ZdIcons.Clock)
)

/** Routes that show the bottom bar. */
private val tabRoutes = setOf(
    ZeroDroidScreen.Dashboard.route,
    ZeroDroidScreen.Tools.route,
    ZeroDroidScreen.Sweep.route,
    ZeroDroidScreen.AlertCenter.route,
    ZeroDroidScreen.Sessions.route
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

    val agreed = rememberEthicalAgreement()
    val onboarded by shell.onboardingDone.collectAsState()
    val pendingRoute by shell.deepLinks.pending.collectAsState()

    // Notifications, tiles and widgets open screens by route.
    LaunchedEffect(pendingRoute) {
        val route = pendingRoute ?: return@LaunchedEffect
        shell.deepLinks.consume()
        runCatching { navController.navigate(route) { launchSingleTop = true } }
    }

    // Tablets and unfolded phones: a rail instead of the bottom bar, and Tools as list + detail.
    val wide = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() } >= WIDE_DP.dp
    val baseRoute = currentRoute?.substringBefore('?')
    val onTool = baseRoute != null && ToolCatalog.forRoute(baseRoute) != null
    val showListPane = wide && (baseRoute == ZeroDroidScreen.Tools.route || onTool)
    val railRoute = if (onTool) ZeroDroidScreen.Tools.route else baseRoute

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = ZdColors.Bg,
            snackbarHost = { SnackbarHost(snackbarHostState) { ZdSnackbar(it) } },
            bottomBar = {
                if (!wide && currentRoute in tabRoutes) {
                    ZdBottomBar(
                        tabs = bottomTabs,
                        currentRoute = currentRoute,
                        alertCount = alertCount,
                        onSelect = { navController.navigateTopLevel(it) }
                    )
                }
            }
        ) { paddingValues ->
            Row(Modifier.padding(paddingValues)) {
            if (wide) {
                ZdNavRail(
                    tabs = bottomTabs,
                    selectedRoute = railRoute,
                    alertCount = alertCount,
                    onSelect = { navController.navigateTopLevel(it) }
                )
            }
            if (showListPane) {
                Box(Modifier.width(LIST_PANE_DP.dp).fillMaxHeight()) {
                    ToolsScreen(
                        selectedRoute = baseRoute,
                        onOpenTool = { tool ->
                            // Swap the open tool instead of stacking one per tap.
                            val current = navController.currentBackStackEntry?.destination
                            navController.navigate(tool.route) {
                                if (onTool && current != null) popUpTo(current.id) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onSearch = { navController.navigate(ZeroDroidScreen.Search.route) },
                        onSettings = { navController.navigate(ZeroDroidScreen.Settings.route) },
                        onPinChanged = { tool, pinned ->
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(if (pinned) "${tool.name} pinned to Home" else "${tool.name} removed from Home")
                            }
                        }
                    )
                }
                Box(Modifier.fillMaxHeight().width(1.dp).background(ZdColors.Border))
            }
            NavHost(
                navController = navController,
                startDestination = ZeroDroidScreen.Dashboard.route,
                modifier = Modifier.weight(1f),
                enterTransition = { enterTransition },
                exitTransition = { exitTransition },
                popEnterTransition = { popEnterTransition },
                popExitTransition = { popExitTransition }
            ) {
                composable(ZeroDroidScreen.Dashboard.route) {
                    DashboardScreen(
                        onNavigate = { navController.navigateTopLevel(it) },
                        onSearch = { navController.navigate(ZeroDroidScreen.Search.route) },
                        onSettings = { navController.navigate(ZeroDroidScreen.Settings.route) }
                    )
                }
                composable(ZeroDroidScreen.Tools.route) {
                    if (wide) {
                        ZdStatePanel(
                            kicker = "Tools",
                            icon = ZdIcons.Grid,
                            iconTint = ZdColors.Accent,
                            iconBackground = ZdColors.AccentBg,
                            title = "Pick a tool",
                            body = "Choose one from the list; it opens here and the list stays put so you can switch quickly."
                        )
                        return@composable
                    }
                    ToolsScreen(
                        onOpenTool = { navController.navigate(it.route) { launchSingleTop = true } },
                        onSearch = { navController.navigate(ZeroDroidScreen.Search.route) },
                        onSettings = { navController.navigate(ZeroDroidScreen.Settings.route) },
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
                        ZdHeader(path = "/alerts", title = "Alerts") {
                            DemoDataAction(route = ZeroDroidScreen.AlertCenter.route)
                        }
                        Box(Modifier.weight(1f)) {
                            AlertCenterScreen(onOpenTool = { navController.navigate(it) { launchSingleTop = true } })
                        }
                    }
                }

                composable(ZeroDroidScreen.Sweep.route) {
                    SweepPresetsScreen(onStart = { preset, place -> navController.navigate(sweepRunRoute(preset, place)) })
                }
                composable(
                    "sweep/run/{preset}/{place}",
                    arguments = listOf(navArgument("preset") { type = NavType.StringType }, navArgument("place") { type = NavType.StringType })
                ) { entry ->
                    val preset = runCatching { SweepPreset.valueOf(entry.arguments?.getString("preset").orEmpty()) }.getOrDefault(SweepPreset.FULL)
                    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                    Column(Modifier.fillMaxSize()) {
                        // Back goes through the dispatcher so a running sweep stops and reports first.
                        ZdHeader(path = "/sweep/run", title = preset.title, onBack = { backDispatcher?.onBackPressed() })
                        Box(Modifier.weight(1f)) {
                            SweepRunScreen(
                                onLocate = { f -> navController.navigate(locateRoute(f.key.orEmpty(), f.title)) },
                                onCompare = { a, b -> navController.navigate("compare/$a/$b") },
                                onOpenSession = { navController.navigate("session/$it") },
                                onDone = { navController.popBackStack() }
                            )
                        }
                    }
                }
                pushed("locate/{address}/{label}", "/locate", "Locate", navController, listOf("address", "label")) {
                    LocateScreen()
                }
                composable(ZeroDroidScreen.Search.route) {
                    SearchScreen(
                        onBack = { navController.popBackStack() },
                        onOpenTool = { navController.navigate(it.route) },
                        onOpenDevice = { navController.navigate(deviceRoute(it.itemKey, it.label, it.kind)) },
                        onOpenRoute = { navController.navigate(it) },
                        onLearn = { navController.navigate("${it.route}?help=true") }
                    )
                }
                pushed(ZeroDroidScreen.Settings.route, "/settings", "Settings", navController, emptyList()) {
                    SettingsScreen(onOpenRules = { navController.navigate(RULES_ROUTE) }, onDataDeleted = {
                        scope.launch { snackbarHostState.showSnackbar("All saved data deleted") }
                    })
                }

                composable(RULES_ROUTE) {
                Column(Modifier.fillMaxSize()) {
                    ZdHeader(path = "/rules", title = "Watch rules", onBack = { if (!navController.popBackStack()) navController.navigateTopLevel(ZeroDroidScreen.Dashboard.route) })
                    Box(Modifier.weight(1f)) {
                        RulesScreen(
                            onOpenSettings = { navController.navigate(ZeroDroidScreen.Settings.route) }
                        )
                    }
                }
            }

            tool(ZeroDroidScreen.Sensors, navController) { SensorScreen() }
                tool(ZeroDroidScreen.Wifi, navController) { WifiScreen() }
                tool(ZeroDroidScreen.Ble, navController) {
                    BleScreen(onOpenDevice = { address, name, kind ->
                        navController.navigate(deviceRoute(address, name ?: "", kind))
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
                tool(ZeroDroidScreen.BluetoothTracker, navController) {
                    BluetoothTrackerScreen(onOpenDevice = { address, name -> navController.navigate(deviceRoute(address, name, "TRACKER")) })
                }
                tool(ZeroDroidScreen.RogueAp, navController) { RogueApScreen() }
                tool(ZeroDroidScreen.NetworkScanner, navController) { NetworkScannerScreen() }
                tool(ZeroDroidScreen.RfBugSweeper, navController) { RfBugSweeperScreen() }
                tool(ZeroDroidScreen.ProximityRadar, navController) { ProximityRadarScreen() }
                tool(ZeroDroidScreen.PrivacyScore, navController) { PrivacyScoreScreen() }
                tool(ZeroDroidScreen.DeauthDetector, navController) { DeauthDetectorScreen() }
                tool(ZeroDroidScreen.EmfMapper, navController) { EmfMapperScreen() }
                tool(ZeroDroidScreen.SignalLogger, navController) { SignalLoggerScreen() }

                composable(ZeroDroidScreen.Sessions.route) {
                    SessionsScreen(
                        onOpenSession = { navController.navigate("session/$it") },
                        onCompare = { a, b -> navController.navigate("compare/$a/$b") }
                    )
                }
                pushed("session/{id}", "/sessions/detail", "Session", navController, listOf("id")) {
                    SessionDetailScreen(
                        onDeleted = { navController.popBackStack() },
                        onOpenDevice = { navController.navigate(deviceRoute(it.key, it.label, it.kind.name)) }
                    )
                }
                pushed("compare/{a}/{b}", "/sessions/compare", "Compare sessions", navController, listOf("a", "b")) {
                    CompareScreen(onOpenDevice = { navController.navigate(deviceRoute(it.key, it.label, it.kind.name)) })
                }
                pushed("device/{key}/{label}/{kind}", "/devices", "Device detail", navController, listOf("key", "label", "kind")) {
                    DeviceDetailScreen(
                        onLocate = { address, label -> navController.navigate(locateRoute(address, label)) },
                        onOpenGatt = { address, name ->
                            navController.navigate("gatt_explorer/${Uri.encode(address)}/${Uri.encode(name ?: "")}")
                        },
                        onOpenSession = { navController.navigate("session/$it") }
                    )
                }

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
        if (agreed && !onboarded) {
            // Covers the app until goals are picked; back does nothing so it can't be bypassed by accident.
            BackHandler {}
            OnboardingScreen()
        }
    }
}

/** Route to a device's cross-session history; every argument is URL-encoded. */
internal fun deviceRoute(key: String, label: String, kind: String): String =
    "device/${Uri.encode(key)}/${Uri.encode(label.ifBlank { " " })}/$kind"

internal const val RULES_ROUTE = "rules"

/** Width where the layout switches to rail + panes (Material's "expanded" class). */
private const val WIDE_DP = 840
private const val LIST_PANE_DP = 380

internal fun locateRoute(address: String, label: String): String =
    "locate/${Uri.encode(address)}/${Uri.encode(label.ifBlank { " " })}"

/** A pushed (non-tab) screen with the standard header and a back button. */
private fun NavGraphBuilder.pushed(
    route: String,
    path: String,
    title: String,
    navController: NavHostController,
    args: List<String>,
    content: @Composable () -> Unit
) {
    composable(route, arguments = args.map { navArgument(it) { type = NavType.StringType } }) {
        Column(Modifier.fillMaxSize()) {
            ZdHeader(path = path, title = title, onBack = { navController.popBackStack() })
            Box(Modifier.weight(1f)) { content() }
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
    composable(
        "${screen.route}?help={help}",
        arguments = listOf(navArgument("help") { type = NavType.BoolType; defaultValue = false })
    ) { entry ->
        val info = ToolCatalog.forRoute(screen.route)
        // Search's Learn results open a tool with its help sheet already up.
        var showHelp by rememberSaveable { mutableStateOf(entry.arguments?.getBoolean("help") == true) }
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
