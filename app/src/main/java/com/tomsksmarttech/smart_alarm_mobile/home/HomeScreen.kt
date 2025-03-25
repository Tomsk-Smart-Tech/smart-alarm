package com.tomsksmarttech.smart_alarm_mobile.home

import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.tomsksmarttech.smart_alarm_mobile.R
import com.tomsksmarttech.smart_alarm_mobile.SharedData
import com.tomsksmarttech.smart_alarm_mobile.calendar.CalendarEvents
import com.tomsksmarttech.smart_alarm_mobile.mqtt.MqttService
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(navController: NavController? = null) {
    val context = LocalContext.current
    var events: String
    val coroutineScope = rememberCoroutineScope()
    var isConnected = MqttService.connectionState.collectAsState()

    var temperature by remember { SharedData.temperature }
    var humidity by remember { SharedData.humidity }


    val permission = android.Manifest.permission.READ_CALENDAR
    var isPermissionGranted by remember {
        mutableStateOf(context.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED)
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
                    val sf = SettingsFunctions()
                    sf.connectToDevice(context)
                    sf.sendMessage("Hello, I'm ESP32 ^_^", "mqtt/test")
                    sf.sendMessage("Test", "mqtt/sensors")
                    sf.sendMessage("Test", "mqtt/alarms")
                    if (isConnected.value) {
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
        Setting("Управление воспроизведением") {
            if (navController != null) {
                navController.navigate("music_player_route") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
        Setting(stringResource(R.string.tab_about)) {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://alice.yandex.ru/support/ru/station/index-gen2")
            )
            startActivity(context, browserIntent, null)
        },
        Setting("Импортировать календарь") {

            Toast.makeText(context, "События из календаря импортированы", Toast.LENGTH_LONG).show()
            coroutineScope.launch {
                if (isPermissionGranted) {
                    events = CalendarEvents().convertCalendarEventsToJSON(
                        CalendarEvents().parseCalendarEvents(
                            context,
                            SimpleDateFormat("dd-MM-yyyy").parse("01-01-2025").time,
                            SimpleDateFormat("dd-MM-yyyy").parse("01-01-2026").time
                        )
                    )
                    try {
                        Log.d("EVENTS", events)
                        val sf = SettingsFunctions()
                        sf.connectToDevice(context)
                        sf.sendMessage(events, "mqtt/events")
                        if (isConnected.value) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.notif_device_connected_success),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        Log.d("ALARM", "is connected: $isConnected")
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
        Setting(stringResource(R.string.about_device), SettingsFunctions()::about),
    )


    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
        )
        LazyColumn(horizontalAlignment = Alignment.CenterHorizontally) {
            items(settingsList) { setting ->
                if (setting.name == "Smart alarm") {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.kumquat),
                            contentDescription = "Our alarm",
                            Modifier.padding(20.dp)
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
                                .padding(horizontal = 0.dp, vertical = 20.dp),
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
                                    modifier = Modifier.scale(1.5f)
                                )
                                Text(
                                    "${temperature}°C",
                                    fontSize = 30.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(10.dp))
                                Icon(
                                    painter = painterResource(R.drawable.ic_humidity),
                                    contentDescription = "Humidity",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.scale(1.5f)
                                )
                                Text(
                                    "${humidity}%",
                                    fontSize = 30.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Row {
                        Log.d("ALARM", "is connected: ${isConnected.value}")
                        if (isConnected.value) {
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

@Preview
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}