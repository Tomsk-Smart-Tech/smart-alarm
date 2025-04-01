package com.tomsksmarttech.smart_alarm_mobile.alarm

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.tomsksmarttech.smart_alarm_mobile.HttpController
import kotlinx.coroutines.CoroutineScope


class AlarmViewModel(application: Application, repository: AlarmRepository) : AndroidViewModel(application) {
    private lateinit var httpController: HttpController

    val repository: AlarmRepository = AlarmRepository
    val alarms = repository.alarms

    init {
        repository.loadAlarms(application)
    }

    fun addAlarm(alarm: Alarm) {
        val newList = alarms.value.toMutableList()
        newList.add(alarm)
        repository.updateAlarms(newList)
        setAlarm(alarm.id)
        Log.d("ALARM", "save ringtones")

    }
    fun updateCurrAlarmIndex() {
        repository.updateCurrAlarmIndex()
    }

    fun onAlarmRemove(removedAlarmId: Int) {
        repository.removeAlarm(removedAlarmId)
    }

    fun initHttpController(controller: HttpController) {
        httpController = controller
    }

    fun saveAlarms(cs: CoroutineScope) {
        repository.saveAlarms(httpController, cs)
    }

    fun setLastAlarm() {
        SingleAlarmManager.setAlarm(repository.alarms.value.last().id)
    }


    fun onAlarmChange(updatedAlarm: Alarm) {

        updatedAlarm.isSended = false
        val currentList = repository.alarms.value
        val newList = currentList.map { it ->
            if (it.id == updatedAlarm.id) updatedAlarm else it }
            .toMutableList()
        repository.updateAlarms(newList)
        // todo проверить работает ли так
//        MqttService.initCoroutineScope(coroutineScope)
        Log.d("SEND", "I WANT TO SEND ${repository.alarms.value}")
    }

    fun setAlarmId(id: Int) {
        repository.setAlarmId(id)
    }

    fun setAlarm(id: Int) {
        repository.setAlarm(id)
    }

    fun cancelAlarm(id: Int) {
        repository.cancelAlarm(id)
    }

    fun generateNewAlarmId(): Int {
        return repository.currentAlarmIndex + 1
    }

}