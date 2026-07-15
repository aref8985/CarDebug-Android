package com.example.myapplication.ui

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.myapplication.R
import com.example.myapplication.navigation.Route
import com.example.myapplication.ui.screens.*
import com.example.myapplication.ui.screens.PerformanceScreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.window.core.layout.WindowWidthSizeClass

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(localeContext: Context) {
    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    // Use the base (Activity) context for permissions
    val permissionState = rememberMultiplePermissionsState(permissions = bluetoothPermissions)

    // Wrap the rest of the UI in the localized context
    CompositionLocalProvider(LocalContext provides localeContext) {
        if (permissionState.allPermissionsGranted) {
            AppContent()
        } else {
            PermissionRequester(
                onGrantClick = { permissionState.launchMultiplePermissionRequest() }
            )
        }
    }
}

@Composable
fun PermissionRequester(onGrantClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                Icons.Rounded.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.bluetooth_permission_required),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onGrantClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.grant_permission))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AppContent() {
    val backStack = rememberNavBackStack(Route.Dashboard)
    val currentRoute = backStack.lastOrNull() as? Route ?: Route.Dashboard

    val adaptiveInfo = currentWindowAdaptiveInfo()
    val isExpanded = adaptiveInfo.windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT
    
    val directive = remember(adaptiveInfo) {
        calculatePaneScaffoldDirective(adaptiveInfo)
            .copy(horizontalPartitionSpacerSize = 0.dp)
    }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)

    val navigationItems = listOf(
        Triple(Route.Dashboard, Icons.Rounded.Dashboard, R.string.dashboard),
        Triple(Route.Performance, Icons.Rounded.DirectionsCar, R.string.performance),
        Triple(Route.Diagnostics, Icons.Rounded.Build, R.string.diagnostics),
        Triple(Route.Garage, Icons.Rounded.DirectionsCar, R.string.garage),
        Triple(Route.Settings, Icons.Rounded.Settings, R.string.settings)
    )

    Row(modifier = Modifier.fillMaxSize()) {
        if (isExpanded && currentRoute != Route.Terminal) {
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Spacer(Modifier.weight(1f))
                navigationItems.forEach { (route, icon, label) ->
                    val isSelected = when (route) {
                        Route.Garage -> currentRoute is Route.Garage || currentRoute is Route.VehicleDetail
                        else -> currentRoute == route
                    }
                    NavigationRailItem(
                        selected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                switchTab(backStack, route)
                            }
                        },
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(stringResource(label)) }
                    )
                }
                Spacer(Modifier.weight(1f))
            }
        }

        Scaffold(
            modifier = Modifier.weight(1f),
            bottomBar = {
                if (!isExpanded && currentRoute != Route.Terminal) {
                    NavigationBar {
                        navigationItems.forEach { (route, icon, label) ->
                            val isSelected = when (route) {
                                Route.Garage -> currentRoute is Route.Garage || currentRoute is Route.VehicleDetail
                                else -> currentRoute == route
                            }
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (!isSelected) {
                                        switchTab(backStack, route)
                                        }
                                },
                                icon = { Icon(icon, contentDescription = null) },
                                label = { Text(stringResource(label)) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavDisplay(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                backStack = backStack,
                onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) },
                sceneStrategy = listDetailStrategy,
                entryProvider = entryProvider {
                    entry<Route.Dashboard> { DashboardScreen() }
                    entry<Route.Diagnostics> { DiagnosticsScreen() }
                    entry<Route.Garage>(
                        metadata = ListDetailSceneStrategy.listPane(
                            detailPlaceholder = {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Select a vehicle")
                                }
                            }
                        )
                    ) { 
                        GarageScreen(onNavigateToDetail = { id -> backStack.add(Route.VehicleDetail(id)) }) 
                    }
                    entry<Route.VehicleDetail>(
                        metadata = ListDetailSceneStrategy.detailPane()
                    ) { route ->
                        VehicleDetailScreen(
                            vehicleId = route.vehicleId,
                            onBack = { if (backStack.lastOrNull() is Route.VehicleDetail) backStack.removeAt(backStack.size - 1) }
                        )
                    }
                    entry<Route.Performance> { 
                        PerformanceScreen()
                    }
                    entry<Route.Settings> { 
                        SettingsScreen(onNavigateToTerminal = { backStack.add(Route.Terminal) }) 
                    }
                    entry<Route.Terminal> { 
                        TerminalScreen(onBack = { if (backStack.lastOrNull() == Route.Terminal) backStack.removeAt(backStack.size - 1) }) 
                    }
                }
            )
        }
    }
}

private fun switchTab(backStack: androidx.navigation3.runtime.NavBackStack<NavKey>, route: Route) {
    if (backStack.size > 0) {
        backStack[0] = route
        while (backStack.size > 1) backStack.removeAt(1)
    } else {
        backStack.add(route)
    }
}
