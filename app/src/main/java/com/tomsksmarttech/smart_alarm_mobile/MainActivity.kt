package com.tomsksmarttech.smart_alarm_mobile

import SingleAlarmManager
import android.Manifest
import android.app.ComponentCaller
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.provider.Settings.canDrawOverlays
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
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
import com.tomsksmarttech.smart_alarm_mobile.SharedData.humidity
import com.tomsksmarttech.smart_alarm_mobile.SharedData.loadAlarms
import com.tomsksmarttech.smart_alarm_mobile.SharedData.loadSensorsData
import com.tomsksmarttech.smart_alarm_mobile.SharedData.musicList
import com.tomsksmarttech.smart_alarm_mobile.SharedData.temperature
import com.tomsksmarttech.smart_alarm_mobile.SharedData.voc
import com.tomsksmarttech.smart_alarm_mobile.alarm.AlarmReceiver
import com.tomsksmarttech.smart_alarm_mobile.alarm.AlarmReceiver.Companion.NOTIFICATION_CLICKED
import com.tomsksmarttech.smart_alarm_mobile.alarm.AlarmRepository
import com.tomsksmarttech.smart_alarm_mobile.alarm.AlarmScreen
import com.tomsksmarttech.smart_alarm_mobile.alarm.AlarmViewModel
import com.tomsksmarttech.smart_alarm_mobile.home.HomeScreen
import com.tomsksmarttech.smart_alarm_mobile.mqtt.AlarmObserver
import com.tomsksmarttech.smart_alarm_mobile.mqtt.MqttService
import com.tomsksmarttech.smart_alarm_mobile.playback.PlaybackControlScreen
import com.tomsksmarttech.smart_alarm_mobile.spotify.SpotifyPkceLogin
import com.tomsksmarttech.smart_alarm_mobile.ui.theme.SmartalarmmobileTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

const val durationMillis = 600
lateinit var viewModel: AlarmViewModel
class MainActivity : ComponentActivity() {
    companion object {
        lateinit var spotifyPkceLogin: SpotifyPkceLogin
    }

    val ao = AlarmObserver(this)
    val targetRoute by lazy {
        intent?.getStringExtra("TARGET_ROUTE")?.takeIf { it.isNotEmpty() }
            ?: Screens.Home.route
    }

    fun updateData(newData: SensorsData) {
        MqttService.init(application)
        MqttService.subscribedTopics.add(SENSORS_TOPIC)
        humidity.value = newData.humidity
        temperature.value = newData.temperature
        voc.value = newData.voc
        // Принудительно обновляем виджет
        SensorWidget.forceUpdate(application) // Нужно передать Context
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        AlarmRepository.updateCurrAlarmIndex()
        Log.d("SpotifyAuth", "onNewIntent called")
        intent.let {
            if (it.action == Intent.ACTION_VIEW && it.data != null) {
                val redirectUri = getString(R.string.redirect_uri)
                if (it.data.toString().startsWith(redirectUri)) {
                    Log.d("SpotifyAuth", "Handling Spotify redirect URI: ${it.dataString}")
                    spotifyPkceLogin.getAuthorizationCode(this, it)
                    spotifyPkceLogin.sendAuthorizationCode()
                    MqttService.addMsg("mqtt/spotifyAuth", it.data.toString())
                } else {
                    Log.d("SpotifyAuth", "Ignoring non-Spotify Intent in onNewIntent: ${it.dataString}")
                }
            } else {
                Log.d("SpotifyAuth", "Intent is null or not ACTION_VIEW in onNewIntent")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        runBlocking{ MqttService.init(application) }
        handleIntentIfNeeded(intent)
    }

    private fun handleIntentIfNeeded(intent: Intent?) {
        intent?.let {
            // Проверяем, что Intent пришел от Spotify редиректа и еще не обработан
            // (Можно добавить флаг в SharedPreferences или в сам Intent, чтобы избежать повторной обработки)
            if (it.action == Intent.ACTION_VIEW && it.data != null) {
                val redirectUri = getString(R.string.redirect_uri)
                if (it.data.toString().startsWith(redirectUri)) {
                    Log.d("SpotifyAuth", "Handling Spotify redirect URI from initial Intent: ${it.dataString}")
                    spotifyPkceLogin.getAuthorizationCode(this, it)
                    spotifyPkceLogin.sendAuthorizationCode()
                    MqttService.addMsg("mqtt/spotifyAuth", it.data.toString())
                    // Очищаем data из интента, чтобы не обработать повторно при повороте экрана и т.д.
                    // Важно: Это может помешать другим обработчикам этого интента, если они есть.
                    // Альтернатива - использовать флаг.
                    setIntent(Intent(this, MainActivity::class.java)) // Заменяем интент на "чистый"
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        targetRoute
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val alarmRepo: AlarmRepository = AlarmRepository
        val viewModelHolder = ViewModelHolder(this)
        viewModel = viewModelHolder.get("AlarmViewModel") {
            AlarmViewModel(application, alarmRepo)
        }
        viewModel.updateCurrAlarmIndex()
        viewModel.generateNewAlarmId()
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(packageName)
            if (!isIgnoringBatteryOptimizations) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }

        if (intent?.getBooleanExtra("notification_action_clicked", false) == true) {
            Log.d("MainActivity", "Получен клик по телу уведомления")
            val stopIntent = Intent(this, AlarmReceiver::class.java).apply {
                action = NOTIFICATION_CLICKED
            }
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                stopIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            pendingIntent.send()
            SharedData.isAlarmDialog.value = true
//            setContent{
//                AlarmCancelDialog( onDismis = {
//
//                })
//            }
        } else if (intent?.getBooleanExtra("notification_clicked", false) == false) {
            viewModel.init()
        } else {
            SharedData.isAlarmDialog.value = true
        }
        MqttService.init(this)
        SingleAlarmManager.init(this)

        MqttService.connect()
        spotifyPkceLogin = SpotifyPkceLogin()

        MqttService.addObserver(ao)
        Log.d("CONNECT", "Connected to mqtt")

        val audioPermission = Manifest.permission.READ_MEDIA_AUDIO
        if (SharedData.checkPermission(this, audioPermission) && musicList.value.isEmpty()) {
            SharedData.startLoadMusicJob(applicationContext)
        }

        MqttService.initCoroutineScope(lifecycleScope)

        lifecycleScope.launch {
            MqttService.connect()
            MqttService.deque.collect { deque ->  // COLLECT ОБРАБАТЫВАЕТ ВСЕ ЭЛЕМЕНТЫ
                while (deque.isNotEmpty()) {
                    val currElem = deque.removeFirstOrNull() ?: continue
                    launch {
                        if (!MqttService.subscribedTopics.contains(currElem.first)) {
                            MqttService.subscribe(currElem.first)
                        }
                        MqttService.send(currElem.first, currElem.second, this@MainActivity)
                    }
                    MqttService.updateDeque(deque)
                }
            }
        }

//
//        val alarmRepo: AlarmRepository = AlarmRepository
//        val viewModelHolder = ViewModelHolder(this)
//        viewModel = viewModelHolder.get("AlarmViewModel") {
//            AlarmViewModel(application, alarmRepo)
//        }
        val httpController = HttpController(this)
        viewModel.initHttpController(httpController)

        setContent {
            SmartalarmmobileTheme {
                BottomNavigationBar(targetRoute)
            }
        }
//        Log.d("PENDING", "pending alarms: $pendingAlarms")
//        SharedData.sortAlarms()
//        pendingAlarms.sortBy{ alarm: Alarm ->
//            alarm.let {
//                val now = LocalTime.now()
//                val alarmTime = it.time
//                val formatter = DateTimeFormatter.ofPattern("HH:mm")
//                val localTime = LocalTime.parse(alarmTime, formatter)
//                val duration = Duration.between(now, localTime)
//
//                if (duration.isNegative) duration.plusDays(1).seconds else duration.seconds
//            }
//        }
//        MqttService.addList("mqtt/alarms", pendingAlarms.toList())
//        SingleAlarmManager.init(this)
    }

    override fun onStop() {
        super.onStop()
        val saveWorkRequest = OneTimeWorkRequestBuilder<SaveAlarmsWorker>().build()
        WorkManager.getInstance(this).enqueue(saveWorkRequest)
    }
//    override fun onStop() {
//        super.onStop()
//        val saveWorkRequest = OneTimeWorkRequestBuilder<SaveAlarmsWorker>().build()
//        WorkManager.getInstance(this).enqueue(saveWorkRequest)
//    }



    class SaveAlarmsWorker(appContext: Context, workerParams: WorkerParameters) :
        Worker(appContext, workerParams) {

        override fun doWork(): Result {
            try {
                // Сохраняем будильники
                val alarms = AlarmRepository.alarms.value
                SharedData.saveAlarms(applicationContext, alarms)

                // Сохраняем данные сенсоров
                SharedData.saveSensorsData(applicationContext)

                Log.d("SAVE_WORKER", "Данные успешно сохранены")
                return Result.success()
            } catch (e: Exception) {
                Log.e("SAVE_WORKER", "Ошибка при сохранении данных", e)
                return Result.failure()
            }
        }
    }

//}

    class ViewModelHolder(owner: ViewModelStoreOwner) {
        private val store = owner.viewModelStore
        private val viewModels = mutableMapOf<String, ViewModel>()

        fun <T : ViewModel> get(key: String, creator: () -> T): T {
            return viewModels.getOrPut(key) { creator() } as T
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
                                    //todo разобраться что происхрдит
//                                AlarmRepository.setAlarmId(0)
                                    if (item.route == Screens.Music.route) {
                                        AlarmRepository.setCurrentAlarmId(0)
                                    }
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
            AlarmScreen(navController, viewModel)
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
}