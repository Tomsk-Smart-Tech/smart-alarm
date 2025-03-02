package com.tomsksmarttech.smart_alarm_mobile

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tomsksmarttech.smart_alarm_mobile.SharedData.loadListFromFile
import com.tomsksmarttech.smart_alarm_mobile.SharedData.musicList
import com.tomsksmarttech.smart_alarm_mobile.alarm.Alarm
import com.tomsksmarttech.smart_alarm_mobile.alarm.AlarmScreen
import com.tomsksmarttech.smart_alarm_mobile.home.HomeScreen
import com.tomsksmarttech.smart_alarm_mobile.mqtt.MqttController
import com.tomsksmarttech.smart_alarm_mobile.ui.theme.SmartalarmmobileTheme

const val durationMillis = 600

class MainActivity : ComponentActivity() {

    val mqttController = MqttController(this, lifecycleScope)

    val targetRoute by lazy {
        intent?.getStringExtra("TARGET_ROUTE")?.takeIf { it.isNotEmpty() }
            ?: Screens.Home.route
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        targetRoute
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        mqttController.connectAndSync()

        val audioPermission = android.Manifest.permission.READ_MEDIA_AUDIO
        if (SharedData.checkPermission(this, audioPermission) && musicList.value.isEmpty()) {
            SharedData.startLoadMusicJob(applicationContext)
        }
        loadAlarms()

        setContent {
            SmartalarmmobileTheme {
                BottomNavigationBar(targetRoute)
            }
        }
    }

    fun loadAlarms() {
        val tmp = loadListFromFile(this, key = "alarm_data", Alarm::class.java)
        Log.d("ALARM", "temp data loaded: $tmp")
        tmp?.forEach { it: Alarm ->
            Log.d("ALARM", it.toString())
            if (!SharedData.alarms.value.contains(it)) {
                SharedData.addAlarm(it)
            } else {
                Log.d("ALARM", "already have" + SharedData.alarms.value)
                SharedData.alreadyAddedAlarms.add(it)
            }
        }
        SharedData.updateCurrAlarmIndex()
        SingleAlarmManager.init(this)
    }
}

sealed class Screens(val route: String) {
    data object Home : Screens("home_route")
    data object Alarm : Screens("alarm_route")
    data object Music : Screens("music_route")
}

data class BottomNavigationItem(
    val label: String = "",
    val icon: ImageVector = Icons.Filled.Home,
    val route: String = ""
) {
    @Composable
    fun bottomNavigationItems(): List<BottomNavigationItem> {
        return listOf(
            BottomNavigationItem(
                label = stringResource(R.string.home),
                icon = Icons.Filled.Home,
                route = Screens.Home.route
            ),
            BottomNavigationItem(
                label = stringResource(R.string.alarm),
                icon = ImageVector.vectorResource(R.drawable.ic_alarm),
                route = Screens.Alarm.route
            ),
            BottomNavigationItem(
                label = stringResource(R.string.music),
                icon = ImageVector.vectorResource(R.drawable.ic_music),
                route = Screens.Music.route
            )
        )
    }
}


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun BottomNavigationBar(
    route: String,
) {
    var navigationSelectedItem by remember {
        mutableIntStateOf(0)
    }

    val navController = rememberNavController()

    val currentRoute = navController.currentBackStackEntryFlow
        .collectAsState(initial = navController.currentBackStackEntry)
        .value?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                BottomNavigationItem().bottomNavigationItems().forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        label = {
                            Text(item.label)
                        },
                        icon = {
                            Icon(
                                item.icon, contentDescription = item.label
                            )
                        },
                        onClick = {
                            if (currentRoute != item.route) {
                                SharedData.setAlarmId(0)
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavigationHost(navController, paddingValues, route)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun NavigationHost(
    navController: NavHostController,
    paddingValues: PaddingValues,
    defaultRoute: String
) {
    NavHost(
        navController = navController,
        startDestination = defaultRoute,
        modifier = Modifier.padding(paddingValues = paddingValues),
    ) {
        composable(
            Screens.Home.route,
            enterTransition = {
                when (initialState.destination.route) {
                    Screens.Alarm.route, Screens.Music.route -> slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(durationMillis = durationMillis)
                    )

                    else -> null
                }
            },
            exitTransition = {
                when (targetState.destination.route) {
                    Screens.Alarm.route, Screens.Music.route -> slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = durationMillis)
                    )

                    else -> null
                }
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(durationMillis = durationMillis)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(durationMillis = durationMillis)
                )
            }
        ) {
            HomeScreen()
        }

        composable(
            Screens.Alarm.route,
            enterTransition = {
                when (initialState.destination.route) {
                    Screens.Home.route -> slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = durationMillis)
                    )

                    Screens.Music.route -> slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(durationMillis = durationMillis)
                    )

                    else -> null
                }
            },
            exitTransition = {
                when (targetState.destination.route) {
                    Screens.Home.route -> slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(durationMillis = durationMillis)
                    )

                    Screens.Music.route -> slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = durationMillis)
                    )

                    else -> null
                }
            },
            popEnterTransition = {
                when (initialState.destination.route) {
                    Screens.Home.route -> slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = durationMillis)
                    )

                    Screens.Music.route -> slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(durationMillis = durationMillis)
                    )

                    else -> null
                }
            },
            popExitTransition = {
                when (targetState.destination.route) {
                    Screens.Home.route -> slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(durationMillis = durationMillis)
                    )

                    Screens.Music.route -> slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = durationMillis)
                    )

                    else -> null
                }
            }
        ) {
            AlarmScreen(navController)
        }

        composable(
            Screens.Music.route,
            enterTransition = {
                when (initialState.destination.route) {
                    Screens.Home.route, Screens.Alarm.route -> slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = durationMillis)
                    )

                    else -> null
                }
            },
            exitTransition = {
                when (targetState.destination.route) {
                    Screens.Home.route, Screens.Alarm.route -> slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(durationMillis = durationMillis)
                    )

                    else -> null
                }
            },
            popEnterTransition = {
                when (initialState.destination.route) {
                    Screens.Home.route, Screens.Alarm.route -> slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = durationMillis)
                    )

                    else -> null
                }
            },
            popExitTransition = {
                when (targetState.destination.route) {
                    Screens.Home.route, Screens.Alarm.route -> slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(durationMillis = durationMillis)
                    )

                    else -> null
                }
            }
        ) {
            MusicScreen()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun BottomNavigationBarPreview(route: String) {
    BottomNavigationBar(route)
}