package com.tomsksmarttech.smart_alarm_mobile.alarm

import android.content.Context
import android.util.Log
import com.tomsksmarttech.smart_alarm_mobile.ALARMS_TOPIC
import com.tomsksmarttech.smart_alarm_mobile.HttpController
import com.tomsksmarttech.smart_alarm_mobile.SharedData
import com.tomsksmarttech.smart_alarm_mobile.SharedData.loadListFromFile
import com.tomsksmarttech.smart_alarm_mobile.mqtt.MqttService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.collections.sortBy

object AlarmRepository {
    private val _currentAlarmId = MutableStateFlow(0)
    val currentAlarmId: StateFlow<Int> = _currentAlarmId
    private val _playingAlarmId = MutableStateFlow(0)
    var playingAlarmId: StateFlow<Int> = _currentAlarmId

    private val _alarms = MutableStateFlow(mutableListOf<Alarm>())
    val alarms: StateFlow<List<Alarm>> = _alarms

    fun updateAlarms(newList: MutableList<Alarm>) {
        _alarms.value = newList
        sortAlarms()
        MqttService.addList(ALARMS_TOPIC, alarms.value.toList())

    }

    fun setPlayingAlarmId(id: Int) {
        _playingAlarmId.value = id
    }

    fun loadAlarms(context: Context) {

//        var pendingAlarms = mutableListOf<Alarm>()
        val tmp = loadListFromFile(context, key = "alarm_data", Alarm::class.java)
        Log.d("ALARM", "temp data loaded: $tmp")
        tmp?.forEach { it: Alarm ->
            Log.d("ALARM", it.toString())
            if (!alarms.value.contains(it)) {
                addAlarm(it)
            } else {
//                if (!it.isSended) {
//                    pendingAlarms.add(it)
//                }
                Log.d("ALARM", "already have" + alarms.value)
                alreadyAddedAlarms.add(it)
            }
        }
    }


    fun saveAlarms(hc : HttpController, cs: CoroutineScope) {
        sortAlarms()
        cs.launch{
            hc.saveAlarms()
        }
    }

    fun setAlarmId(id: Int) {
        _currentAlarmId.value = id
    }

    fun addAlarm(newAlarm: Alarm) {
        val updatedList = alarms.value.toMutableList()
        updatedList.add(newAlarm)
        _alarms.value = updatedList
        sortAlarms()
        MqttService.addList(ALARMS_TOPIC, alarms.value)
    }

    fun removeAlarm(id: Int) {
        val updatedList = alarms.value.toMutableList()
        val removedAlarm = updatedList.find { it.id == id }
        updatedList.remove(removedAlarm)
        _alarms.value = updatedList
        if (removedAlarm != null) SingleAlarmManager.cancelAlarm(removedAlarm)
        sortAlarms()
        MqttService.addList(ALARMS_TOPIC, alarms.value)
    }

    fun sortAlarms() {
        _alarms.value.sortBy { alarm ->
            alarm.time
        }
    }

    var alreadyAddedAlarms = mutableListOf<Alarm>()

//    fun updateAlarms() {
//        val updatedList = alarms.value.toMutableList()
//        _alarms.value = updatedList
//    }

    var currentAlarmIndex = alarms.value.size

    fun updateCurrAlarmIndex() {
        if (alarms.value.isEmpty()) {
            return;
        } else {
            alarms.value.forEach {
                if (it.id > currentAlarmIndex) currentAlarmIndex = it.id
            }
        }
    }

    fun cancelAlarm(alarm: Alarm) {
        SingleAlarmManager.cancelAlarm(alarm)
        sortAlarms()
        MqttService.addList(ALARMS_TOPIC, alarms.value)
    }

    fun setAlarm(id: Int) {
        SingleAlarmManager.setAlarm(id)
        sortAlarms()
        MqttService.addList(ALARMS_TOPIC, alarms.value)
    }

    fun setCurrentAlarmId(id: Int) {
        _currentAlarmId.value = id
    }
}