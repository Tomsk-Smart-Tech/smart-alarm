package com.tomsksmarttech.smart_alarm_mobile

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tomsksmarttech.smart_alarm_mobile.ui.theme.SmartalarmmobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

//        val sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE)
//        val gson = Gson()
//        val json = sharedPreferences.getString("lib", null)
//        val type:Type = object : TypeToken<List<Audio>>() {}.type
//        viewModel.musicList = gson.fromJson(json, type) as List<Audio>

        if (sharedData.musicList.isEmpty()) {
            sharedData.musicList = sharedData.loadMusicLibrary(applicationContext)
            Log.d("Library", sharedData.musicList.toString())
        }

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
    @Composable
    fun bottomNavigationItems(): List<bottomNavigationItem> {
        return listOf(
            bottomNavigationItem(
                label = "Home",
                icon = Icons.Filled.Home,
                route = Screens.Home.route),
            bottomNavigationItem(
                label = "Alarm",
                icon = ImageVector.vectorResource(R.drawable.ic_alarm),
                route = Screens.Alarm.route),
            bottomNavigationItem(
                label = "Music",
                icon = ImageVector.vectorResource(R.drawable.ic_music),
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
            composable(
                Screens.Home.route,
                enterTransition = {
                    when (initialState.destination.route) {
                        (Screens.Alarm.route) -> slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(700))
                        (Screens.Music.route) -> slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(700))
                        else -> null
                    }
                },
                exitTransition = {
                    when (targetState.destination.route) {
                        (Screens.Alarm.route) -> slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(700))
                        (Screens.Music.route) -> slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(700))
                        else -> null
                    }
                }
            ) {
                HomeScreen()
            }
            composable(Screens.Alarm.route,
                enterTransition = {
                    when (initialState.destination.route) {
                        (Screens.Home.route) -> slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(700))
                        (Screens.Music.route) -> slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(700))
                        else -> null
                    }
                },
                exitTransition = {
                    when (targetState.destination.route) {
                        (Screens.Home.route) -> slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(700))
                        (Screens.Music.route) -> slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(700))
                        else -> null
                    }
                }) {
                AlarmScreen()
            }
            composable(Screens.Music.route,
                enterTransition = {
                    when (initialState.destination.route) {
                        (Screens.Home.route) -> slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(700))
                        (Screens.Alarm.route) -> slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(700))
                        else -> null
                    }
                },
                popEnterTransition = {
                    TODO()
                },
                exitTransition = {
                    when (targetState.destination.route) {
                        (Screens.Home.route) -> slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(700))
                        (Screens.Alarm.route) -> slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(700))
                        else -> null
                    }
                }
            ) {
                MusicScreen()
            }
        }
    }
}

@Composable
@Preview
fun BottomNavigationBarPreview() {
    BottomNavigationBar()
}