package com.tomsksmarttech.smart_alarm_mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Будильники") })
        LazyColumn {
            items(alarms) { alarm ->
                AlarmItem(alarm)
            }
        }
        FloatingActionButton(
            onClick = onAddAlarm,
            modifier = Modifier
                .align(Alignment.End)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Alarm")
        }
    }
}

@Composable
fun AlarmItem(alarm: Alarm) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
       // elevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = alarm.time,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (alarm.isEnabled) "Вкл" else "Выкл",
                style = MaterialTheme.typography.bodyMedium,
                color = if (alarm.isEnabled) Color.Green else Color.Red
            )
        }
    }
}
data class Alarm(
    val id: Int, // Уникальный идентификатор
    val time: String, // Время будильника в формате "HH:mm"
    val isEnabled: Boolean, // Активен ли будильник
    val repeatDays: List<String>? = null, // Дни недели для повторения (например, ["Понедельник", "Среда"])
    val label: String? = null // Название будильника
)
