package com.tomsksmarttech.smart_alarm_mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

@Composable
fun AlarmScreen() {
    Text("This is an alarm screen", Modifier.fillMaxSize())
    val testAlarms: List<Alarm> = listOf(
        Alarm(id = 1, time = "07:00", isEnabled = true, label = "Подъём"),
        Alarm(id = 2, time = "08:30", isEnabled = false, label = "Работа")
    )
    AlarmListScreen(alarms = testAlarms) { }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(alarms: List<Alarm>, onAddAlarm: () -> Unit) {
//    Column(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets(0.dp),
            topBar = {
                TopAppBar(title = { Text("Будильники") })
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onAddAlarm,
                    modifier = Modifier
//                .align(Alignment.End)
                        .padding(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Alarm")
                }
            }
        ) { paddingValues ->
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier.fillMaxSize()
            ) { items(alarms.size) { alarm ->
                AlarmItem(alarms[alarm])
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmItem(alarm: Alarm) {
    var checked by remember { mutableStateOf(alarm.isEnabled) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp),
        ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.padding(2.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = alarm.time,
                    style = MaterialTheme.typography.displayMedium
                )

                Text(
                    text = alarm.label,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Switch(
                checked = checked,
                onCheckedChange = {
                    checked = it
                }
            )
//            Text(
//                style = MaterialTheme.typography.bodyMedium,
//                color = if (alarm.isEnabled) Color.Green else Color.Red
//            )
        }
    }
}
data class Alarm(
    val id: Int,
    val time: String, // "HH:mm"
    var isEnabled: Boolean,
    val repeatDays: List<String>? = null,
    val label: String,
)
