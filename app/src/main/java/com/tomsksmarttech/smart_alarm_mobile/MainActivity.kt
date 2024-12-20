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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tomsksmarttech.smart_alarm_mobile.SharedData.loadListFromFile
import com.tomsksmarttech.smart_alarm_mobile.SharedData.saveListAsJson
import com.tomsksmarttech.smart_alarm_mobile.alarm.Alarm
import com.tomsksmarttech.smart_alarm_mobile.alarm.AlarmScreen
import com.tomsksmarttech.smart_alarm_mobile.home.HomeScreen
import com.tomsksmarttech.smart_alarm_mobile.ui.theme.SmartalarmmobileTheme


class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val audioPermission = android.Manifest.permission.READ_MEDIA_AUDIO
        if (SharedData.checkPermission(this, audioPermission)) {
            SharedData.startLoadMusicJob(applicationContext)
        }
        val tmp = loadListFromFile(this, key = "alarm0", Alarm::class.java)
        Log.d("ALARM", "бляя" + tmp.toString())
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

        setContent {
            SmartalarmmobileTheme {
                BottomNavigationBar()
            }
        }
    }
    fun saveAlarms() {
        Log.d("ALARM", "before filter" + SharedData.alarms.value.toList().toString())

        SharedData.alarms.value.removeAll { it: Alarm ->
            it.id == -1 || SharedData.alreadyAddedAlarms.contains(it)
        }
        if (!SharedData.alarms.value.isEmpty()) {
            Log.d("ALARM", "saving" + SharedData.alarms.value.toList().toString())
            saveListAsJson(context = this, SharedData.alarms.value.toList(), key = "alarm0")
        }
    }
    override fun onDestroy() {
        Log.d("ALARM", "destr" + SharedData.alarms.value.toList().toString())
        saveAlarms()
        super.onDestroy()
    }

    override fun onPause() {
        Log.d("ALARM", "pause " +SharedData.alarms.value.toList().toString())
//        SharedData.alarms.value.removeAll(mi)
        saveAlarms()

        super.onPause()
    }


    override fun onStop() {
        Log.d("ALARM", "stop " + SharedData.alarms.value.toList().toString())
        saveAlarms()
        super.onStop()
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
fun BottomNavigationBar() {
    var navigationSelectedItem by remember {
        mutableIntStateOf(0)
    }

    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                BottomNavigationItem().bottomNavigationItems().forEachIndexed { index, item ->
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
                        (Screens.Alarm.route) -> slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(700)
                        )

                        (Screens.Music.route) -> slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(700)
                        )

                        else -> null
                    }
                },
                exitTransition = {
                    when (targetState.destination.route) {
                        (Screens.Alarm.route) -> slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(700)
                        )

                        (Screens.Music.route) -> slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(700)
                        )

                        else -> null
                    }
                }
            ) {
                HomeScreen()
            }
            composable(Screens.Alarm.route,
                enterTransition = {
                    when (initialState.destination.route) {
                        (Screens.Home.route) -> slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(700)
                        )

                        (Screens.Music.route) -> slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(700)
                        )

                        else -> null
                    }
                },
                exitTransition = {
                    when (targetState.destination.route) {
                        (Screens.Home.route) -> slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(700)
                        )

                        (Screens.Music.route) -> slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(700)
                        )

                        else -> null
                    }
                }) {
                AlarmScreen()
            }
            composable(Screens.Music.route,
                enterTransition = {
                    when (initialState.destination.route) {
                        (Screens.Home.route) -> slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(700)
                        )

                        (Screens.Alarm.route) -> slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(700)
                        )

                        else -> null
                    }
                },
                exitTransition = {
                    when (targetState.destination.route) {
                        (Screens.Home.route) -> slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(700)
                        )

                        (Screens.Alarm.route) -> slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(700)
                        )

                        else -> null
                    }
                }
            ) {
                MusicScreen()
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
@Preview
fun BottomNavigationBarPreview() {
    BottomNavigationBar()
}