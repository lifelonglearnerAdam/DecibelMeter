package com.decibelmeter.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.decibelmeter.app.ui.hardware.HardwareScreen
import com.decibelmeter.app.ui.meter.DecibelMeterScreen
import com.decibelmeter.app.ui.sleep.SleepAidScreen

/**
 * 底部导航项定义
 */
sealed class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Meter : BottomNavItem(
        route = "meter",
        label = "分贝仪",
        selectedIcon = Icons.Filled.GraphicEq,
        unselectedIcon = Icons.Outlined.GraphicEq
    )

    data object Sleep : BottomNavItem(
        route = "sleep",
        label = "助眠音乐",
        selectedIcon = Icons.Filled.Bedtime,
        unselectedIcon = Icons.Outlined.Bedtime
    )

    data object Hardware : BottomNavItem(
        route = "hardware",
        label = "硬件检测",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}

/**
 * 主导航
 * 底部三个 Tab: 分贝仪 | 助眠音乐 | 硬件检测
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val navItems = listOf(
        BottomNavItem.Meter,
        BottomNavItem.Sleep,
        BottomNavItem.Hardware
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                navItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) },
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Meter.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Meter.route) {
                DecibelMeterScreen()
            }
            composable(BottomNavItem.Sleep.route) {
                SleepAidScreen()
            }
            composable(BottomNavItem.Hardware.route) {
                HardwareScreen()
            }
        }
    }
}
