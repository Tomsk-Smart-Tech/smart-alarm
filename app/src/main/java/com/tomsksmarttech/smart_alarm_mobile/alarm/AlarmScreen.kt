package com.tomsksmarttech.smart_alarm_mobile.alarm

import SingleAlarmManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.tomsksmarttech.smart_alarm_mobile.HttpController
import com.tomsksmarttech.smart_alarm_mobile.R
import com.tomsksmarttech.smart_alarm_mobile.Screens
import com.tomsksmarttech.smart_alarm_mobile.SharedData
import com.tomsksmarttech.smart_alarm_mobile.SharedData.addAlarm
import com.tomsksmarttech.smart_alarm_mobile.SharedData.alarms
import com.tomsksmarttech.smart_alarm_mobile.SharedData.currentAlarmIndex
import com.tomsksmarttech.smart_alarm_mobile.SharedData.updateCurrAlarmIndex
import com.tomsksmarttech.smart_alarm_mobile.mqtt.MqttService
//import com.tomsksmarttech.smart_alarm_mobile.mqtt.MqttController
import kotlinx.coroutines.flow.update
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import kotlin.text.first

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun AlarmScreen(navController: NavHostController) {
    val context = LocalContext.current
    var isPermissionGranted by remember { mutableStateOf(false) }
    val permission = android.Manifest.permission.POST_NOTIFICATIONS
    val alarmsList by alarms.collectAsState()

    val coroutineScope = rememberCoroutineScope()
//    val mqttController = MqttController(context, coroutineScope)
//    mqttController.connect()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isPermissionGranted = isGranted
    }

    LaunchedEffect(Unit) {
        if (context.checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            launcher.launch(permission)
        }
    }

    AlarmListScreen(
        alarms = alarmsList,
        onAlarmChange = { updatedAlarm ->
            updatedAlarm.isSended = false
            SharedData.alarms.update { alarms ->
                alarms.map { if (it!!.id == updatedAlarm.id) updatedAlarm else it }.toMutableList()
            }
            SharedData.sortAlarms()
            MqttService.subscribe("mqtt/alarms")
//            MqttService.publish("mqtt/alarms", a)
            MqttService.initCoroutineScope(coroutineScope)
            MqttService.sendList(alarms.value, context)
            Log.d("SEND","I WANT TO SEND ${alarms.value}")
        },
//        onAlarmAdd = {
//            newAlarm ->
//
//            SharedData.alarms.update { alarms ->
//                (alarms + newAlarm).toMutableList() }
//        },
        onAlarmRemove = { alarmId ->
            SharedData.alarms.update { alarms -> alarms.filter { it!!.id != alarmId }.toMutableList() }
            MqttService.subscribe("mqtt/alarms")
            MqttService.sendList(alarms.value, context)
        },
        navController = navController
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    alarms: MutableList<Alarm?>,
    onAlarmChange: (Alarm) -> Unit,
    onAlarmRemove: (Int) -> Unit,
    navController: NavHostController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val httpController = HttpController(context)
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
                    showDialog = true
                    Log.d("ALARM", "alarm added")
                },
                modifier = Modifier
                    .padding(10.dp)
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
            items(alarms,
                key = { alarm -> alarm?.id ?: 0 }
            ) { alarm ->
                if (!alarms.isEmpty()) {
                    AlarmItem(
                        alarm = alarm!!,
                        onAlarmChange = onAlarmChange,
                        onAlarmRemove = onAlarmRemove,
                        alarmManager = SingleAlarmManager,
                        navController = navController
                    )
                }
            }
        }
    }
    if (showDialog) {
        DialClockDialog(
            null,
            onConfirm = { timePickerState ->
                Log.d("ALARM", "Creating new with id ${SharedData.alarms.value.last()}")
                Log.d("ALARM", "and list is  ${SharedData.alarms.value}")
                SingleAlarmManager.setAlarm(SharedData.alarms.value.last()!!.id)

                showDialog = false
                Log.d("SAVEALARM", "save alarm")
                SharedData.saveAlarms(httpController, coroutineScope)
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun AlarmItem(
    alarm: Alarm,
    onAlarmChange: (Alarm) -> Unit,
    onAlarmRemove: (Int) -> Unit,
    alarmManager: SingleAlarmManager,
    navController: NavHostController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val httpController = HttpController(context)

    val haptic = LocalHapticFeedback.current
    var isEnabled by remember { mutableStateOf(alarm.isEnabled) }
    var isShowDialog by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    var isHapticEnabled by remember { mutableStateOf(false) }
    var isLabelChanged by remember { mutableStateOf(false) }
    var isDaysDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(8.dp),
        elevation = cardElevation(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp)
            .clickable(onClick = { isExpanded = !isExpanded })
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
                    modifier = Modifier.clickable { isLabelChanged = true },
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
                    Log.d("ALARM", "$alarm")
                    onAlarmChange(
                        alarm.copy(
                            isEnabled = isEnabled,
                            repeatDays = alarm.repeatDays ?: listOf(false, false, false, false, false, false, false)
                        )
                    )

                    if (isEnabled) {
                        alarmManager.setAlarm(alarm.id)
                    } else {
                        alarmManager.cancelAlarm(alarm.id)
                    }
                }
            )
        }
        AnimatedVisibility(visible = isExpanded) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            SharedData.setAlarmId(alarm.id)
                            navController.navigate(Screens.Music.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        .padding(6.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Мелодия будильника", fontWeight = FontWeight.Bold)
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_arrow_right),
                            contentDescription = "Show additional settings"
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.haptic), fontWeight = FontWeight.Bold)
                    Switch(
                        modifier = Modifier.scale(0.75f, 0.75f),
                        checked = isHapticEnabled, onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isHapticEnabled = !isHapticEnabled
                            alarm.isHaptic = isHapticEnabled
                        }
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = {
                            isDaysDialog = true
                        })
                        .padding(6.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Дни недели", fontWeight = FontWeight.Bold)
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_arrow_right),
                            contentDescription = "days repeat"
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = {
                            onAlarmRemove(alarm.id)
                            SharedData.removeAlarm(alarm.id)
                            alarmManager.cancelAlarm(alarm.id)
                            Log.d("ALARM", "removed alarm")
                        })
                        .padding(6.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Удалить будильник", fontWeight = FontWeight.Bold)
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_delete),
                            contentDescription = "Delete alarm"
                        )
                    }
                }
            }
        }
        if (isShowDialog) {
            DialClockDialog(
                alarm = alarm,
                onConfirm = { timePickerState ->
                    onAlarmChange(
                        alarm.copy(
                            timePickerState.id,
                            timePickerState.time,
                            timePickerState.isEnabled,
                            label = timePickerState.label,
                            repeatDays = timePickerState.repeatDays,
                            isSended = false,
                        )
                    )
                    alarmManager.setAlarm(alarm.id)
                    SharedData.sortAlarms()
                    SharedData.saveAlarms(httpController, coroutineScope)
                    isShowDialog = false
                },
                onDismiss = { isShowDialog = false }
            )
        }
        if (isLabelChanged) {
            ChangeLabelDialog(alarm, onConfirm = { newAlarm ->
                alarm.label = newAlarm.label
                alarm.isSended = false
                isLabelChanged = false
                MqttService.subscribe("mqtt/alarms")
                MqttService.sendList(alarms.value, context)
                Log.d("SEND", "I WANT TO SEND ${alarms.value}")
            }, onDismiss = { isLabelChanged = false })
        }
        if (isDaysDialog) {
            AlarmDaysPickerDialog(alarm = alarm,
                onConfirm = {
                    alarm.isSended = false
                    isDaysDialog = false
                    MqttService.subscribe("mqtt/alarms")
                    MqttService.sendList(alarms.value, context)
                    Log.d("SEND", "I WANT TO SEND ${alarms.value}")
                            },

                onDismiss = {isDaysDialog = false})
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmDaysPickerDialog(
    alarm: Alarm,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
//    val weekendsList = listOf(false, false, false, false, false, true, true)
//    val workdaysList = listOf(true, true, true, true, true, false, false)

//    val originalRepeatDays = remember {
//        alarm.repeatDays.toList()
//    }

    var selectedOptions = remember {
        if (alarm.repeatDays == null || alarm.repeatDays.isEmpty()) {
            Log.d("ALARM","RD: ${alarm.repeatDays}")
            mutableStateListOf(false, false, false, false, false, false, false)
        } else {
            alarm.repeatDays.toMutableStateList()
        }
    }

    var isDaysExpanded by remember { mutableStateOf(false) }

    val isAllWorkdaysSelected = selectedOptions.slice(0..4).all { it }
    val isAllWeekendsSelected = selectedOptions.slice(5..6).all { it }

    var isWorkDays by remember { mutableStateOf(isAllWorkdaysSelected) }
    var isWeekends by remember { mutableStateOf(isAllWeekendsSelected) }


    // Cостояния переключателей обновляются при каждом нажатии на MultiChoiceSegmentedButtonRow
    // .all{ it } проверяет, что все элементы списка равны true
    // таким образом, если пользователь сам выбирает будни, переключатель "выходные" сам включится, так же и с выходными
    LaunchedEffect(selectedOptions.toList()) {
        isWorkDays = selectedOptions.slice(0..4).all { it }
        isWeekends = selectedOptions.slice(5..6).all { it }
    }

    val days = listOf(
        stringResource(R.string.monday),
        stringResource(R.string.tuesday),
        stringResource(R.string.wednesday),
        stringResource(R.string.thursday),
        stringResource(R.string.friday),
        stringResource(R.string.saturday),
        stringResource(R.string.sunday)
    )

    ModalBottomSheet(
        modifier = Modifier
            .wrapContentHeight()
            .padding(),
        onDismissRequest = {
            onDismiss()
        }
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text("Выберите дни недели:")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Будни")
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        onCheckedChange = {
                            if (isWorkDays) {
                                for (i in 0..4) {
                                    selectedOptions[i] = false
                                }
                            } else {
                                for (i in 0..4) {
                                    selectedOptions[i] = true
                                }
                            }
                        },
                        checked = isWorkDays
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Выходные")
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        onCheckedChange = {
                            if (isWeekends) {
                                for (i in 5..6) {
                                    selectedOptions[i] = false
                                }
                            } else {
                                for (i in 5..6) {
                                    selectedOptions[i] = true
                                }
                            }
                        },
                        checked = isWeekends
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isDaysExpanded = !isDaysExpanded }
                    .padding(6.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Настроить...")
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            AnimatedVisibility(visible = isDaysExpanded) {
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(16.dp),
                    ) {
                        Column {
                            MultiChoiceSegmentedButtonRow {
                                days.forEachIndexed { index, label ->
                                    SegmentedButton(
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = days.size
                                        ),
                                        checked = selectedOptions[index],
                                        onCheckedChange = {
                                            selectedOptions[index] = !selectedOptions[index]
                                        },
                                        label = {
                                            Text(days[index].first().toString())
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    modifier = Modifier.padding(2.dp),
                    onClick = {
                        onDismiss()
                    },
                ) {
                    Text(stringResource(R.string.btn_cancel))
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    modifier = Modifier.padding(2.dp),
                    onClick = {
                        alarm.repeatDays = selectedOptions.toList()
                        Log.d("ALARM", "Saved repeat days: ${alarm.repeatDays}")
                        onConfirm()
                    },
                ) {
                    Text(stringResource(R.string.btn_confirm))
                }
            }
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
        val initialLabel = stringResource(R.string.new_alarm)
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
                    label = initialLabel,
                    isHaptic = false,
                    repeatDays = listOf(false,false,false,false,false,false,false),

                )
                Log.d("ALARM", "$newAlarm : ")
                addAlarm(newAlarm)
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
                    time = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute),
                    repeatDays = alarm.repeatDays?: listOf(false,false,false,false,false,false,false)
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
                Text(stringResource(R.string.btn_cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) {
                Text(stringResource(R.string.btn_confirm))
            }
        },
        text = { content() }
    )
}

fun generateNewAlarmId(): Int {
    Log.d("ALARM", "new index: $currentAlarmIndex : ${SharedData.alarms.value}")
    return currentAlarmIndex + 1
}

@Composable
fun ChangeLabelDialog(
    alarm: Alarm,
    onDismiss: () -> Unit,
    onConfirm: (Alarm) -> Unit,
) {
    var newLabel by remember { mutableStateOf(alarm.label) }

    Dialog(onDismissRequest = { onDismiss() }) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Change Label",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                TextField(
                    value = newLabel,
                    onValueChange = { newLabel = it },
                    label = { Text("Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = { onDismiss() }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        val updatedAlarm = alarm.copy(label = newLabel)
                        onConfirm(updatedAlarm)
                    }) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}
