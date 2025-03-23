package com.tomsksmarttech.smart_alarm_mobile

import SingleAlarmManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.tomsksmarttech.smart_alarm_mobile.SharedData.alarms
import com.tomsksmarttech.smart_alarm_mobile.SharedData.loadListFromFile
import com.tomsksmarttech.smart_alarm_mobile.SharedData.musicList
import com.tomsksmarttech.smart_alarm_mobile.alarm.Alarm
import com.tomsksmarttech.smart_alarm_mobile.alarm.AlarmScreen
import com.tomsksmarttech.smart_alarm_mobile.home.HomeScreen
import com.tomsksmarttech.smart_alarm_mobile.mqtt.AlarmObserver
import com.tomsksmarttech.smart_alarm_mobile.mqtt.MqttService
import com.tomsksmarttech.smart_alarm_mobile.playback.PlaybackControlScreen
import com.tomsksmarttech.smart_alarm_mobile.ui.theme.SmartalarmmobileTheme
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter


const val durationMillis = 600

class MainActivity : ComponentActivity() {

    val ao = AlarmObserver(this)

//    lateinit var mqttService: MqttService

    val targetRoute by lazy {
        intent?.getStringExtra("TARGET_ROUTE")?.takeIf { it.isNotEmpty() }
            ?: Screens.Home.route
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        targetRoute
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        MqttService.init(this)

        MqttService.connect()

        MqttService.addObserver(ao)
        Log.d("CONNECT", "Connected to mqtt")

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
        MqttService.subscribe("mqtt/sensors")

        var pendingAlarms = mutableListOf<Alarm>()
        val tmp = loadListFromFile(this, key = "alarm_data", Alarm::class.java)
        Log.d("ALARM", "temp data loaded: $tmp")
        tmp?.forEach { it: Alarm ->
            Log.d("ALARM", it.toString())
            if (!SharedData.alarms.value.contains(it)) {
                SharedData.addAlarm(it)
            } else {
                if (!it.isSended) {
                    pendingAlarms.add(it)
                }
                Log.d("ALARM", "already have" + SharedData.alarms.value)
                SharedData.alreadyAddedAlarms.add(it)
            }
        }
        Log.d("PENDING", "pending alarms: $pendingAlarms")
        SharedData.sortAlarms()
        pendingAlarms.sortBy{ alarm: Alarm ->
            alarm.let {
                val now = LocalTime.now()
                val alarmTime = it.time
                val formatter = DateTimeFormatter.ofPattern("HH:mm")
                val localTime = LocalTime.parse(alarmTime, formatter)
                val duration = Duration.between(now, localTime)

                if (duration.isNegative) duration.plusDays(1).seconds else duration.seconds
            }
        }

        MqttService.initCoroutineScope(lifecycleScope)
        MqttService.sendList(pendingAlarms, this)
        SharedData.updateCurrAlarmIndex()
        SingleAlarmManager.init(this)
    }

    override fun onStop() {
        super.onStop()
        val saveWorkRequest = OneTimeWorkRequestBuilder<SaveAlarmsWorker>().build()
        WorkManager.getInstance(this).enqueue(saveWorkRequest)
    }

    class SaveAlarmsWorker(appContext: Context, workerParams: WorkerParameters) :
        Worker(appContext, workerParams) {

        override fun doWork(): Result {
            val alarms = SharedData.alarms.value
            SharedData.saveAlarms(applicationContext, alarms)
            Log.d("ALARMS", "Будильники сохранены через WorkManager")
            return Result.success()
        }
    }

}

sealed class Screens(val route: String) {
    data object Home : Screens("home_route")
    data object Alarm : Screens("alarm_route")
    data object Music : Screens("music_route")
    data object MusicPlayer : Screens("music_player_route")
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
            NavigationBar(containerColor = Color.Transparent) {
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
                when (initialState.destination.route) {
                    Screens.Alarm.route, Screens.Music.route -> slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(durationMillis = durationMillis)
                    )

                    else -> null
                }
            },
            popExitTransition = {
                when (targetState.destination.route) {
                    Screens.Alarm.route, Screens.Music.route -> slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = durationMillis)
                    )

                    else -> null
                }

            }
        ) {
            HomeScreen(navController)
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

        composable(
            Screens.MusicPlayer.route,
            enterTransition = {
                when (initialState.destination.route) {
                    Screens.Home.route -> slideInHorizontally(
                        initialOffsetX = { it },

//                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = durationMillis)
                    )

                    else -> null
                }
            },
            exitTransition = {
                when (targetState.destination.route) {
                    Screens.Home.route -> slideOutHorizontally(
                        targetOffsetX = { it },
//                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(durationMillis = durationMillis)
                    )

                    else -> null
                }
            },
            popEnterTransition = {
                when (initialState.destination.route) {
                    Screens.Home.route -> slideInHorizontally(
                        initialOffsetX = { it },

//                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = durationMillis)
                    )

                    else -> null
                }
            },
            popExitTransition = {
                when (targetState.destination.route) {
                    Screens.Home.route -> slideOutHorizontally(
                        targetOffsetX = { it },
//                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(durationMillis = durationMillis)
                    )

                    else -> null
                }
            }
        ) {
            PlaybackControlScreen({ navController.popBackStack() })
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun BottomNavigationBarPreview(route: String) {
    BottomNavigationBar(route)
}