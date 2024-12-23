package com.tomsksmarttech.smart_alarm_mobile.alarm

import SingleAlarmManager
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.window.Dialog
import com.tomsksmarttech.smart_alarm_mobile.R
import com.tomsksmarttech.smart_alarm_mobile.SharedData
import com.tomsksmarttech.smart_alarm_mobile.SharedData.addAlarm
import com.tomsksmarttech.smart_alarm_mobile.SharedData.currentAlarmIndex
import com.tomsksmarttech.smart_alarm_mobile.SharedData.updateCurrAlarmIndex
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

@Preview(showSystemUi = true)
@Composable
fun AlarmScreen() {
    val alarmsList by SharedData.alarms.collectAsState()

    AlarmListScreen(
        alarms = alarmsList,
        onAlarmChange = { updatedAlarm ->
            val index = SharedData.alarms.value.indexOfFirst { it.id == updatedAlarm.id }
            if (index != -1) {
                val updatedList = SharedData.alarms.value.toMutableList()
                updatedList[index] = updatedAlarm
                SharedData.alarms.value = updatedList
            }
        },
        onAlarmAdd = { newAlarm ->
            val updatedList = SharedData.alarms.value.toMutableList()
            updatedList.add(newAlarm)
//            SharedData.alarms.value = updatedList
        },
        onAlarmRemove = { alarmId ->
            val updatedList = SharedData.alarms.value.toMutableList()
            updatedList.removeIf { it.id == alarmId }
            SharedData.alarms.value = updatedList
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    alarms: List<Alarm>,
    onAlarmChange: (Alarm) -> Unit,
    onAlarmAdd: (Alarm) -> Unit,
    onAlarmRemove: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        contentWindowInsets = WindowInsets(8.dp),
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.heightIn(max = 56.dp),
                windowInsets = WindowInsets(
                    top = 0.dp,
                    bottom = 0.dp
                ),
                title = {
                    Text(
                        stringResource(R.string.title_alarms),
                        fontWeight = FontWeight.Bold,
                        overflow = TextOverflow.Clip,
                        maxLines = 1
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ))
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
//                    val newAlarm = Alarm(
//                        id = generateNewAlarmId(),
//                        time = "08:00",
//                        isEnabled = false,
//                        label = "Новый будильник"
//                    )
                    showDialog = true
                    Log.d("ALARM", "alarm added")
                },
                modifier = Modifier.padding(10.dp)
                    .clip(shape = CircleShape)
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
                        onAlarmRemove = onAlarmRemove,
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
                onAlarmAdd(SharedData.alarms.value.last())
                Log.d("ALARM", "Creating new with id ${SharedData.alarms.value.last()}")
                Log.d("ALARM", "and list is  ${SharedData.alarms.value}")
                SingleAlarmManager.setAlarm(SharedData.alarms.value.last().id)
                showDialog = false},
            onDismiss = { showDialog = false }
        )
        Log.d("CHECK SDLG", showDialog.toString())
    }
}

@Composable
fun AlarmItem(
    alarm: Alarm,
    onAlarmChange: (Alarm) -> Unit,
    onAlarmRemove: (Int) -> Unit,
    alarmManager: SingleAlarmManager
) {
    val haptic = LocalHapticFeedback.current
    var isEnabled by remember { mutableStateOf(alarm.isEnabled) }
    var isShowDialog by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf (false) }
    var isHapticEnabled by remember { mutableStateOf (false) }
    var isLabelChanged by remember { mutableStateOf (false) }
    var shouldShowMusicScreen by remember { mutableStateOf(false) }
    val navController = rememberNavController()

    Card(
        shape = RoundedCornerShape(8.dp),
        elevation = cardElevation(defaultElevation = 15.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp)
            .clickable( onClick = { isExpanded = !isExpanded })
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
                    modifier = Modifier.clickable { isShowDialog = true },
                    text = alarm.time,
                    style = MaterialTheme.typography.displayMedium
                )
                Text(
                    modifier = Modifier.clickable { isLabelChanged = true},
                    text = alarm.label,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = isEnabled,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isEnabled = it
                    onAlarmChange(alarm.copy(isEnabled = isEnabled))
                    if (isEnabled) {
                        alarmManager.setAlarm(alarm.id)
                    } else {
                        alarmManager.cancelAlarm(alarm.id)
                    }
                }
            )
        }
        AnimatedVisibility(visible = isExpanded) {
            Column{
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable { navController.navigate("music_route") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    } },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Мелодия будильника", fontWeight = FontWeight.Bold)
                    Icon(ImageVector.vectorResource(R.drawable.ic_arrow_right), contentDescription = "Show additional settings")
                }
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Вибрация", fontWeight = FontWeight.Bold)
                    Switch(
                        modifier = Modifier.scale(0.75f, 0.75f),
                        checked = isHapticEnabled, onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isHapticEnabled = !isHapticEnabled
                        }
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable( onClick = {
                            onAlarmRemove(alarm.id)
                            SharedData.removeAlarm(alarm.id)
                            alarmManager.cancelAlarm(alarm.id)
                            Log.d("ALARM", "removed alarm")
                        })
                        .padding(6.dp),
                ) {
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Удалить будильник", fontWeight = FontWeight.Bold)
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_delete),
                            contentDescription = "Show additional settings"
                        )
                    }
                }
            }
        }
        if (isShowDialog) {
            DialClockDialog(
                alarm = alarm,
                onConfirm = { timePickerState ->
                    onAlarmChange(alarm.copy(timePickerState.id, timePickerState.time, timePickerState.isEnabled, label = timePickerState.label))
                    alarmManager.setAlarm(alarm.id)
                    isShowDialog = false
                },
                onDismiss = { isShowDialog = false }
            )
        }
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
fun DialClockDialog (
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
                val newAlarm = Alarm(
                    id = generateNewAlarmId(),
                    time = time,
                    isEnabled = true,
                    label = "Новый будильник"
                )
                Log.d("ALARM", "$newAlarm : ")
//                AlarmHandler.updateAlarm(newAlarm)
                SharedData.addAlarm(newAlarm)
                onConfirm(newAlarm)
                updateCurrAlarmIndex()
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
fun generateNewAlarmId(): Int {
    Log.d("ALARM", "new index: $currentAlarmIndex : ${SharedData.alarms.value}")
    return currentAlarmIndex + 2
}
//object AlarmHandler{
//    var alarm: Alarm = SharedData.alarms.value.first()
//    fun updateAlarm(a: Alarm) {
//
//        alarm = a
//
//    }
//}