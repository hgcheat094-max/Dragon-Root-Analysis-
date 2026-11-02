package com.dragonroot.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dragonroot.app.AppContainer
import kotlinx.coroutines.launch

enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    Dashboard("dashboard", "Dashboard", Icons.Default.Dashboard),
    MyDevice("device", "My Device", Icons.Default.PhoneAndroid),
    Firmware("firmware", "Firmware Center", Icons.Default.SystemUpdate),
    Kernel("kernel", "Kernel Analyzer", Icons.Default.Memory),
    Compat("compat", "Root Compatibility", Icons.Default.VerifiedUser),
    BootImage("boot", "Boot Image Analyzer", Icons.Default.Storage),
    Research("research", "Research Library", Icons.Default.Science),
    Updates("updates", "Update History", Icons.Default.History),
    Settings("settings", "Settings", Icons.Default.Settings),
    About("about", "About Dragon Root", Icons.Default.Info);

    companion object {
        fun fromRoute(route: String?): Destination = entries.firstOrNull { it.route == route } ?: Dashboard
    }
}

@Composable
fun DragonRootRoot(container: AppContainer) {
    val vm: MainViewModel = viewModel(factory = viewModelFactory { initializer { MainViewModel(container) } })
    val bootVm: BootImageViewModel = viewModel(factory = viewModelFactory { initializer { BootImageViewModel(container.appContext) } })
    val nav = rememberNavController()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStack by nav.currentBackStackEntryAsState()
    val current = Destination.fromRoute(backStack?.destination?.route)

    fun go(d: Destination) {
        nav.navigate(d.route) {
            popUpTo(Destination.Dashboard.route)
            launchSingleTop = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = DR.Card) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("DRAGON ROOT", color = DR.Blue, modifier = Modifier.padding(start = 24.dp, top = 24.dp), style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                    Text("Vivo & iQOO device intelligence", color = DR.Grey, modifier = Modifier.padding(start = 24.dp, bottom = 12.dp), style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                    Destination.entries.forEach { d ->
                        NavigationDrawerItem(
                            label = { Text(d.label) },
                            icon = { Icon(d.icon, contentDescription = null) },
                            selected = d == current,
                            onClick = { scope.launch { drawer.close() }; go(d) },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    ) {
        Scaffold(
            containerColor = DR.Black,
            topBar = {
                TopAppBar(
                    title = { Text(current.label) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawer.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open navigation menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DR.Black, titleContentColor = DR.Text, navigationIconContentColor = DR.Text
                    )
                )
            }
        ) { pad ->
            NavHost(nav, startDestination = Destination.Dashboard.route, modifier = Modifier.padding(pad)) {
                composable(Destination.Dashboard.route) { DashboardScreen(vm, ::go) }
                composable(Destination.MyDevice.route) { MyDeviceScreen(vm) }
                composable(Destination.Firmware.route) { FirmwareScreen(vm) }
                composable(Destination.Kernel.route) { KernelScreen(vm) }
                composable(Destination.Compat.route) { CompatScreen(vm) }
                composable(Destination.BootImage.route) { BootImageScreen(bootVm) }
                composable(Destination.Research.route) { ResearchScreen(vm) }
                composable(Destination.Updates.route) { UpdatesScreen(vm) }
                composable(Destination.Settings.route) { SettingsScreen(vm) }
                composable(Destination.About.route) { AboutScreen() }
            }
        }
    }
}
