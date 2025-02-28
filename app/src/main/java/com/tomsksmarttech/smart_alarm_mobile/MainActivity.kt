package com.tomsksmarttech.smart_alarm_mobile

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
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson
import com.tomsksmarttech.smart_alarm_mobile.SharedData.loadListFromFile
import com.tomsksmarttech.smart_alarm_mobile.SharedData.musicList
import com.tomsksmarttech.smart_alarm_mobile.SharedData.saveListAsJson
import com.tomsksmarttech.smart_alarm_mobile.alarm.Alarm
import com.tomsksmarttech.smart_alarm_mobile.alarm.AlarmScreen
import com.tomsksmarttech.smart_alarm_mobile.home.HomeScreen
import com.tomsksmarttech.smart_alarm_mobile.home.SettingsFunctions
import com.tomsksmarttech.smart_alarm_mobile.ui.theme.SmartalarmmobileTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink
import okio.source
import okio.use
import java.io.IOException
import java.util.concurrent.TimeUnit

const val durationMillis = 600

class MainActivity : ComponentActivity() {
    val targetRoute by lazy {
        intent?.getStringExtra("TARGET_ROUTE")?.takeIf { it.isNotEmpty() }
            ?: Screens.Home.route
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("LOG", "OnCreate")
        targetRoute
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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

    suspend fun saveAlarms() {
        coroutineScope {
            Log.d("ALARM", "before filter" + SharedData.alarms.value.toList().toString())

            SharedData.alarms.value.removeAll { it: Alarm? ->
                SharedData.alreadyAddedAlarms.contains(it)
            }

//            launch(Dispatchers.IO) {
//                for (alarm in SharedData.alarms.value) {
//                    sendAudio(
//                        applicationContext,
//                        alarm!!.musicUri!!,
//                        resources.getString(R.string.remote_host)
//                    )
//                    delay(500)
//                }
//            }
            if (!SharedData.alarms.value.isEmpty()) {
                Log.d("ALARM", "saving" + SharedData.alarms.value.toList().toString())
                saveListAsJson(
                    context = applicationContext,
                    SharedData.alarms.value.toList(),
                    key = "alarm_data"
                )
            }
        }
    }

    //    Change type of uri from String to Uri
    fun sendAudio(ctx: Context, uri: String, serverUrl: String) {
        Log.d("Upload", "URI: $uri, Scheme: ${uri.toUri().scheme}")
        val contentResolver = ctx.contentResolver
        val inputStream = contentResolver.openInputStream(uri.toUri())
        val fileName = DocumentFile.fromSingleUri(ctx, uri.toUri())!!.getName();

        if (inputStream == null) {
            Log.e("Upload", "Ошибка: не удалось открыть файл")
            return
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("filename", fileName!!)
            .addFormDataPart("file", fileName, object : RequestBody() {
                override fun contentType() = "audio/*".toMediaTypeOrNull()
                override fun writeTo(sink: BufferedSink) {
                    inputStream.source().use { source -> sink.writeAll(source) }
                }
            })
            .build()

        val request = Request.Builder()
            .url(serverUrl)
            .post(requestBody)
            .build()

        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Upload", "Ошибка загрузки: ${e}")
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d(
                    "Uploading file",
                    "Файл $fileName успешно загружен: ${response.body?.string()}"
                )
            }

        })
    }
//    override fun onDestroy() {
//        Log.d("ALARM", "destr" + SharedData.alarms.value.toList().toString())
//        saveAlarms()
//        super.onDestroy()
//    }

    override fun onPause() {
        val sf = SettingsFunctions()
        sf.connectToDevice(this)
        val content = SharedData.alarms.value
        val gson = Gson()
        val jsonString = gson.toJson(content)
        if (jsonString != "[]") {
            lifecycleScope.launch {
                sf.sendMessage(jsonString, "mqtt/alarms")
                Log.d("ALARMS", "Content send: $jsonString")
            }
            lifecycleScope.launch {
                checkIfShouldSave()
            }
        }
        Log.d("HELP PLEASE", "check if should save")
        super.onPause()
    }

    suspend fun checkIfShouldSave() {
//        if (targetRoute != Screens.Home.route) {
        Log.d("ALARM", "checkif " + SharedData.alarms.value.toList().toString())
        saveAlarms()
        try {
            SharedData.updateAlarms()
        } catch (e: Exception) {
            Log.d("ALARMS", e.toString())
        }
//        } else Log.d("HELP PLEASE", targetRoute)
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