package com.tomsksmarttech.smart_alarm_mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tomsksmarttech.smart_alarm_mobile.ui.theme.SmartalarmmobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartalarmmobileTheme {
                BottomNavigationBar()
            }
        }
    }
}


sealed class Screens (val route: String) {
    data object Home : Screens("home_route")
    data object Alarm : Screens("alarm_route")
    data object Music : Screens("music_route")
}

data class bottomNavigationItem (
    val label: String = "",
    val icon: ImageVector = Icons.Filled.Home,
    val route: String = ""
) {
    fun bottomNavigationItems(): List<bottomNavigationItem> {
        return listOf(
            bottomNavigationItem(
                label = "Home",
                icon = Icons.Filled.Home,
                route = Screens.Home.route),
            bottomNavigationItem(
                label = "Alarm",
                icon = Icons.Filled.Warning,
                route = Screens.Alarm.route),
            bottomNavigationItem(
                label = "Music",
                icon = Icons.Filled.Call,
                route = Screens.Music.route)
        )
    }
}

@Composable
fun BottomNavigationBar() {
    var navigationSelectedItem by remember {
        mutableIntStateOf(0)
    }

    val navController = rememberNavController()

    Scaffold (
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                bottomNavigationItem().bottomNavigationItems().forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = navigationSelectedItem == index,
                        label = {
                            Text(item.label)
                        },
                        icon = {
                            Icon(
                                item.icon, contentDescription = item.label
                            )
                        },
                        onClick = {
                            navigationSelectedItem = index
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
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screens.Home.route,
            modifier = Modifier.padding(paddingValues = paddingValues)
        ) {
            composable(Screens.Home.route) {
                HomeScreen().HomeScreen()
            }
            composable(Screens.Alarm.route) {
                AlarmScreen().AlarmScreen()
            }
            composable(Screens.Music.route) {
                MusicScreen().MusicScreen()
            }
        }
    }
}