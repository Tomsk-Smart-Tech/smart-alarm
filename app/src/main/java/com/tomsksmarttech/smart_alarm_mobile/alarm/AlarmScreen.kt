package com.tomsksmarttech.smart_alarm_mobile.alarm

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tomsksmarttech.smart_alarm_mobile.SharedData
import com.tomsksmarttech.smart_alarm_mobile.SharedData.addAlarm
import com.tomsksmarttech.smart_alarm_mobile.SharedData.currentAlarmIndex
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

@Preview(showSystemUi = true)
@Composable
fun AlarmScreen() {
    val alarmsList = remember {
        SharedData.alarms.value
    }
    val appContext = LocalContext.current.applicationContext
    AlarmListScreen(
        alarms = alarmsList,
        onAlarmChange = { updatedAlarm ->
            val index = SharedData.alarms.value.indexOfFirst { it.id == updatedAlarm.id }
            if (index != -1) {
                SharedData.alarms.value[index] = updatedAlarm
            }
        },
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    alarms: List<Alarm>,
    onAlarmChange: (Alarm) -> Unit,
//    alarmManager: SingleAlarmManager
) {
    var showDialog by remember { mutableStateOf(false) }
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(title = { Text("Будильники") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showDialog = true
                },
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Alarm")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            modifier = Modifier.fillMaxSize()
        ) {
            items(alarms) { alarm ->
                if (alarm.id != -1) {
                    AlarmItem(
                        alarm = alarm,
                        onAlarmChange = onAlarmChange,
                        alarmManager = SingleAlarmManager
                    )
                }
            }
        }
    }
    if (showDialog) {
        DialClockDialog(
            null,
            onConfirm = { timePickerState ->
                SingleAlarmManager.setAlarm(SharedData.alarms.value.last().id)
                showDialog = false},
            onDismiss = { showDialog = false }
        )
        Log.d("CHECK SDLG", showDialog.toString())
//        showDialog = setDialDialog(alarmManager = alarmManager)
    }
}
@Composable
fun SetDialDialog(
    showDialog: MutableState<Boolean>
) {
    if (showDialog.value) {
        DialClockDialog(
            null,
            onConfirm = { timePickerState ->
                SingleAlarmManager.setAlarm(SharedData.alarms.value.last().id)
                showDialog.value = false
                Log.d("SWITCH CHANGED", showDialog.value.toString())
            },
            onDismiss = {
                showDialog.value = false
                Log.d("SWITCH CHANGED", showDialog.value.toString())
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmItem(
    alarm: Alarm,
    onAlarmChange: (Alarm) -> Unit,
    alarmManager: SingleAlarmManager
) {
    val haptic = LocalHapticFeedback.current
    var checked by remember { mutableStateOf(alarm.isEnabled) }
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp)
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
                    modifier = Modifier.clickable { showDialog = true },
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
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    checked = it
                    onAlarmChange(alarm.copy(isEnabled = checked))

                    if (checked) {
                        alarmManager.setAlarm(alarm.id -1)
                    } else {
                        alarmManager.cancelAlarm(alarm.id -1)
                    }
                }
            )
        }
        if (showDialog) {
            DialClockDialog(
                alarm = alarm,
                onConfirm = { timePickerState ->
                    onAlarmChange(alarm.copy(timePickerState.id, timePickerState.time, timePickerState.isEnabled, label = timePickerState.label))
                    alarmManager.setAlarm(alarm.id)
                    showDialog = false
                },
                onDismiss = { showDialog = false }
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialClockDialog(
    alarm: Alarm?,
    onConfirm: (Alarm) -> Unit,
    onDismiss: () -> Unit,
) {
    if (alarm == null) {
        Log.d("test", "creating new alarm")
        val currentTime = Calendar.getInstance()
        val timePickerState = rememberTimePickerState(
            initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
            initialMinute = currentTime.get(Calendar.MINUTE),
            is24Hour = true,
        )
        TimePickerDialog(
            onDismiss = { onDismiss() },
            onConfirm = {
                val time = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                Log.d("test", time)
                val newAlarm = Alarm(
                    id = generateNewAlarmId(),
                    time = time,
                    isEnabled = true,
                    label = "Новый будильник"
                )
                addAlarm(newAlarm)
                onConfirm(newAlarm)
            }
        ) {
            TimePicker(
                state = timePickerState,
            )
        }
    } else {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val localTime = LocalTime.parse(alarm.time, formatter)
        val timePickerState = rememberTimePickerState(
            initialHour = localTime.hour,
            initialMinute = localTime.minute,
            is24Hour = true,
        )
        TimePickerDialog(
            onDismiss = { onDismiss() },
            onConfirm = {
                val updatedAlarm = alarm.copy(
                    time = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                )
                onConfirm(updatedAlarm)
            }
        ) {
            TimePicker(
                state = timePickerState,
            )
        }
    }
}
fun generateNewAlarmId(): Int {
    return currentAlarmIndex++
}
@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Dismiss")
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) {
                Text("OK")
            }
        },
        text = { content() }
    )
}

