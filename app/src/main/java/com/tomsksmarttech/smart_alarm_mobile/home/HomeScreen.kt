package com.tomsksmarttech.smart_alarm_mobile.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.provider.Settings
import android.provider.Settings.canDrawOverlays
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.tomsksmarttech.smart_alarm_mobile.EVENTS_TOPIC
import com.tomsksmarttech.smart_alarm_mobile.R
import com.tomsksmarttech.smart_alarm_mobile.SENSORS_TOPIC
import com.tomsksmarttech.smart_alarm_mobile.SharedData
import com.tomsksmarttech.smart_alarm_mobile.TEST_TOPIC
import com.tomsksmarttech.smart_alarm_mobile.calendar.CalendarEvents
import com.tomsksmarttech.smart_alarm_mobile.mqtt.MqttService
import kotlinx.coroutines.launch
import java.util.Date
import androidx.core.net.toUri
import com.tomsksmarttech.smart_alarm_mobile.ALARMS_TOPIC
import com.tomsksmarttech.smart_alarm_mobile.HttpController
import com.tomsksmarttech.smart_alarm_mobile.alarm.AlarmRepository
import com.tomsksmarttech.smart_alarm_mobile.alarm.MediaManager
import com.tomsksmarttech.smart_alarm_mobile.spotify.SpotifyPkceLogin
import com.tomsksmarttech.smart_alarm_mobile.viewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull

@Composable
fun HomeScreen(navController: NavController? = null) {
    val context = LocalContext.current
    var events: String
    val coroutineScope = rememberCoroutineScope()
    var isConnected = MqttService.connectionState.collectAsState()

    var temperature = SharedData.temperature.collectAsState()
    var humidity = SharedData.humidity.collectAsState()
    var voc = SharedData.voc.collectAsState()
    val activity = LocalContext.current as? Activity
    var isAlarmDialog = SharedData.isAlarmDialog.collectAsState()
    var counter by remember { mutableIntStateOf(0) }

    val permission = android.Manifest.permission.READ_CALENDAR
    var isPermissionGranted by remember {
        mutableStateOf(context.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED)
    }

    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            HttpController(context).sendFile(context, uri.toString(), context.getString(R.string.remote_host), "image/*".toMediaTypeOrNull())
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            events = CalendarEvents().convertCalendarEventsToJSON(
                CalendarEvents().parseCalendarEvents(context)
            )
            Toast.makeText(
                context,
                context.getString(R.string.notif_import_calendar), Toast.LENGTH_LONG
            ).show()
            Log.d("EVENTS", events)
            isPermissionGranted = true
        } else {
            Toast(context).apply {
                setText(context.getString(R.string.notif_calendar_access))
                show()
            }
        }
    }

    val settingsList = arrayListOf(
        Setting("Smart alarm") {},
        Setting("Подключение к устройству") {
            coroutineScope.launch {
                try {
//                    val sf = SettingsFunctions()
//                    sf.connectToDevice(context)
//                    sf.sendMessage("Hello, I'm ESP32 ^_^", "mqtt/test")
//                    sf.sendMessage("Test", "mqtt/sensors")
//                    sf.sendMessage("Test", "mqtt/alarms")
                    MqttService.connect()
                    Log.d("EVENTS", "connected in sf")
                    MqttService.subscribe(SENSORS_TOPIC)
                    MqttService.subscribedTopics.add(SENSORS_TOPIC)
                    MqttService.subscribedTopics.add(ALARMS_TOPIC)
                    MqttService.subscribe(ALARMS_TOPIC)
                    MqttService.addMsg(TEST_TOPIC, "Hello, I'm ESP32 ^_^")
//                    MqttService.addMsg("mqtt/alarms", "Hello alarms, I'm ESP32 ^_^")
                    if (isConnected.value == 1) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.notif_device_connected_success),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.notif_device_connected_failed), Toast.LENGTH_LONG
                    ).show()
                }
            }
        },
//        Setting("Управление воспроизведением") {
//            if (navController != null) {
//                navController.navigate("music_player_route") {
//                    popUpTo(navController.graph.findStartDestination().id) {
//                        saveState = true
//                    }
//                    launchSingleTop = true
//                    restoreState = true
//                }
//            }
//        },
        Setting("Подключение Spotify") {

            activity?.let {
                coroutineScope.launch {
                    SpotifyPkceLogin().getAccessToken(it)
                }
            } ?: Log.e("SpotifyAuth", "Activity is null!")
        },
        Setting("Сменить фон") {
            mediaPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        Setting(stringResource(R.string.about_device)) {

            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                context.getString(R.string.index).toUri()
            )

            startActivity(context, browserIntent, null)
        },
        Setting("Импортировать календарь") {
            val currentDateString = SimpleDateFormat("dd-MM-yyyy").format(Date())
            val currentDateParsed = SimpleDateFormat("dd-MM-yyyy").parse(currentDateString)
            Toast.makeText(context, "События из календаря импортированы", Toast.LENGTH_LONG).show()
            coroutineScope.launch {
                if (isPermissionGranted) {
                    events = CalendarEvents().convertCalendarEventsToJSON(
                        CalendarEvents().parseCalendarEvents(
                            context,
                            currentDateParsed.time,
                            SimpleDateFormat("dd-MM-yyyy").parse("01-01-2026").time
                        )
                    )
                    try {
                        Log.d("EVENTS", events)
//                        val sf = SettingsFunctions()
//                        sf.connectToDevice(context)
//                        sf.sendMessage(events, "mqtt/events")
                        MqttService.addMsg(EVENTS_TOPIC, events)
                        Log.d("EVENTS", "added message, deque is: ${MqttService.deque.value}")
                        if (isConnected.value == 1) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.notif_device_connected_success),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        Log.d("ALARM", "is connected: ${isConnected.value}")
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.notif_device_connected_failed),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    launcher.launch(permission)
                }
            }

        },
//        Setting(stringResource(R.string.about_device), SettingsFunctions()::about),
    )

    OverlayPermissionCheck()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LazyColumn(horizontalAlignment = Alignment.CenterHorizontally) {
            items(settingsList) { setting ->
                if (setting.name == "Smart alarm") {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(R.drawable.kumquat),
                            contentDescription = "Our alarm",
                            Modifier.padding(20.dp).clickable{
                                counter++
                            }
                        )
                        Spacer(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(10.dp)
                        )
                        Box(
                            Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(30.dp)
                                )
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_temperature),
                                    contentDescription = "Temperature",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.scale(1.4f)
                                )
                                Text(
                                    "${temperature.value}°C",
                                    fontSize = 27.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(10.dp))
                                Icon(
                                    painter = painterResource(R.drawable.ic_humidity),
                                    contentDescription = "Humidity",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.scale(1.4f)
                                )
                                Text(
                                    "${humidity.value}%",
                                    fontSize = 27.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(10.dp))
                                Icon(
                                    painter = painterResource(R.drawable.ic_air),
                                    contentDescription = "CO2",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.scale(1.4f)
                                )
                                Text(
                                    "${voc.value}",
                                    fontSize = 27.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Row {
                        Log.d("ALARM", "is connected: ${isConnected.value}")
                        if (isConnected.value == 1) {
                            Icon(
                                painterResource(R.drawable.ic_dot),
                                contentDescription = "Connected",
                                tint = Color.Green
                            )
                            Spacer(
                                Modifier
                                    .fillMaxHeight()
                                    .width(10.dp)
                            )
                            Text(text = "Устройство подключено", fontWeight = FontWeight.Bold)
                        } else if (isConnected.value == 0) {
                            Icon(
                                painterResource(R.drawable.ic_loading),
                                contentDescription = "Connecting",
                                tint = Color.Yellow
                            )
                            Spacer(
                                Modifier
                                    .fillMaxHeight()
                                    .width(10.dp)
                            )

                            Text(text = "Подключение...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                painterResource(R.drawable.ic_dot),
                                contentDescription = "Disconnected",
                                tint = Color.Red
                            )
                            Spacer(
                                Modifier
                                    .fillMaxHeight()
                                    .width(10.dp)
                            )
                            Text(text = "Устройство не подключено", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                    )
                } else {
                    SettingCard(setting.name, setting.func)
                }
            }
        }
    }
    if (counter > 6) {
        counter = 0
        Toast.makeText(
            context,
            context.getString(R.string.kumquat),
            Toast.LENGTH_SHORT).show()
    }
    if (isAlarmDialog.value) {
        AlarmCancelDialog( onCancel = {
            SharedData.isAlarmDialog.value = false
            val currAlarm = viewModel.alarms.value.find {
                it.id == AlarmRepository.playingAlarmId.value
            }
            MediaManager.stopMediaPlayback()
            if (currAlarm != null) {
                currAlarm.isEnabled = false
                if (currAlarm.repeatDays.find {it == true } == false) {
                    currAlarm.isEnabled = false
                    AlarmRepository.removeAlarm(currAlarm.id)
                    AlarmRepository.addAlarm(currAlarm)
                }
            }
        },
        )
    }
}

@Composable
fun OverlayPermissionCheck() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!canDrawOverlays(context)) {
            showDialog = true
        } else {
            SharedData.isAlarmManagerShouldWork.value = true
            showDialog = false
            Log.d("OverlayPermission", "Permission already granted")
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Требуется разрешение") },
            text = { Text("Для работы будильников на смартфоне нужно разрешение на отображение поверх других окон") },
            confirmButton = {
                Button(onClick = {
                    context.startActivity(
                        requestOverlayPermissionIntent(context)
                    )
                    showDialog = false
                }) {
                    Text("Перейти в настройки")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

fun requestOverlayPermissionIntent(context: Context): Intent {
    return Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    )
}

//@Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmCancelDialog(
    onCancel: () -> Unit,
//    viewModel: AlarmViewModel,
//    AlarmId: Int,
) {
    ModalBottomSheet(
        modifier = Modifier
            .wrapContentHeight()
            .padding(),
        onDismissRequest = {
            onCancel()
        }
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically

            ) {
                Text(text = "Устройство не подключено, сработал будильник на телефоне. Обеспечьте стабильную работу сети и подключите Умный будильник")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    modifier = Modifier.padding(2.dp),
                    onClick = {
                        onCancel()
                    },
                ) {
                    Text(stringResource(R.string.btn_off))
                }
                Spacer(modifier = Modifier.weight(1f))
//            Button(
//                modifier = Modifier.padding(2.dp),
//                onClick = {
//                    alarm.repeatDays = selectedOptions.toList()
//                    Log.d("ALARM", "Saved repeat days: ${alarm.repeatDays}")
//                    onConfirm()
//                },
//            ) {
//                Text(stringResource(R.string.btn_confirm))
//            }
            }
        }
    }
}

@Composable
fun SettingCard(setting: String, onClick: (context: Context) -> (Unit)) {
    val context = LocalContext.current
//    val navController = rememberNavController()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .clickable(onClick = { onClick(context) })
            .height(50.dp),
        contentAlignment = Alignment.Center,

        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(setting, fontWeight = FontWeight.Bold)
            Icon(
                ImageVector.vectorResource(R.drawable.ic_arrow_right),
                contentDescription = "Show additional settings"
            )
        }
    }
}


fun reconnectToDevice(context: Context,isConnected: Boolean) {
    try {
//                    val sf = SettingsFunctions()
//                    sf.connectToDevice(context)
//                    sf.sendMessage("Hello, I'm ESP32 ^_^", "mqtt/test")
//                    sf.sendMessage("Test", "mqtt/sensors")
//                    sf.sendMessage("Test", "mqtt/alarms")
        MqttService.connect()
        Log.d("EVENTS", "connected in sf")
        MqttService.subscribe(SENSORS_TOPIC)
        MqttService.addMsg(TEST_TOPIC, "Hello, I'm ESP32 ^_^")
//                    MqttService.addMsg("mqtt/alarms", "Hello alarms, I'm ESP32 ^_^")
        if (isConnected) {
            Toast.makeText(
                context,
                context.getString(R.string.notif_device_connected_success),
                Toast.LENGTH_LONG
            ).show()
        }
    } catch (e: Exception) {
        Toast.makeText(
            context,
            context.getString(R.string.notif_device_connected_failed), Toast.LENGTH_LONG
        ).show()
    }

}

@Preview
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}