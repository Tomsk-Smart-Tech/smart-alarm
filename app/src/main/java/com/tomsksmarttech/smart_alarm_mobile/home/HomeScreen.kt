package com.tomsksmarttech.smart_alarm_mobile.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import com.tomsksmarttech.smart_alarm_mobile.CalendarEvents
import com.tomsksmarttech.smart_alarm_mobile.R


@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var events : String
    var isConnected by remember { mutableStateOf(false) }

    val settingsList = arrayListOf(
        Setting("Smart alarm") {},
        Setting("Подключение к устройству", {SettingsFunctions().connectToDevice(context, "Hello, I'm ESP32 ^_^")}),
        Setting("Справка"){
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://alice.yandex.ru/support/ru/station/index-gen2"))
            startActivity(context, browserIntent, null)
        },
        Setting("Импортировать календарь") {
            events = CalendarEvents().convertCalendarEventsToJSON(CalendarEvents().parseCalendarEvents(context))
            Toast.makeText(context, "События из календаря импортированы", Toast.LENGTH_LONG).show()
            try {
                isConnected = SettingsFunctions().connectToDevice(context, events)
            } catch (e: Exception) {
                Toast.makeText(context, "ОШИБКА", Toast.LENGTH_LONG).show()
            }
            Log.d("EVENTS", events)
        },
        Setting("Об устройстве", SettingsFunctions()::about),
    )

    Column (horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier
            .fillMaxWidth()
            .height(20.dp))
        LazyColumn (modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            items(settingsList) { setting ->
                if (setting.name == "Smart alarm") {
                    Image(painter = painterResource(R.drawable.demo_1), contentDescription = "Our alarm")
                    Spacer(modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp))
                    Row {
                        if (isConnected) {
                            Icon(painterResource(R.drawable.ic_dot), contentDescription = "Connected", tint = Color.Green)
                            Spacer(Modifier.fillMaxHeight().width(10.dp))
                            Text(text = "Устройство подключено", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(painterResource(R.drawable.ic_dot), contentDescription = "Disconnected", tint = Color.Red)
                            Spacer(Modifier.fillMaxHeight().width(10.dp))
                            Text(text = "Устройство не подключено", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp))
                } else {
                    SettingCard(setting.name, setting.func)
                }
            }
        }
    }
}


@Composable
fun SettingCard(setting: String, onClick: (context : Context) -> (Unit)) {
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
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(setting, fontWeight = FontWeight.Bold)
            Icon(ImageVector.vectorResource(R.drawable.ic_arrow_right), contentDescription = "Show additional settings")
        }
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}