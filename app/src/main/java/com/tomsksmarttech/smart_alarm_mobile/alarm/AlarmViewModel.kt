package com.tomsksmarttech.smart_alarm_mobile.alarm

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.tomsksmarttech.smart_alarm_mobile.HttpController
import com.tomsksmarttech.smart_alarm_mobile.SharedData
import com.tomsksmarttech.smart_alarm_mobile.SharedData.loadSensorsData
import kotlinx.coroutines.CoroutineScope


class AlarmViewModel(application: Application, repository: AlarmRepository) : AndroidViewModel(application) {
    private lateinit var httpController: HttpController

    val repository: AlarmRepository = AlarmRepository
    val alarms = repository.alarms
    val app = application

    fun init() {
        repository.loadAlarms(app)
        //todo переместить
        val sd = loadSensorsData(app)
        if (sd != null ) {
            SharedData.humidity.value = sd.humidity
            SharedData.temperature.value = sd.temperature
            SharedData.voc.value = sd.voc
        }
    }

    fun addAlarm(alarm: Alarm) {
        val newList = alarms.value.toMutableList()
        newList.add(alarm)
        repository.updateAlarms(newList)
        cancelAlarm(alarm.id)
        setAlarm(alarm.id)
        Log.d("ALARM", "save ringtones")

    }
    fun updateCurrAlarmIndex() {
        repository.updateCurrAlarmIndex()
    }

    fun setCurrentAlarmId(id: Int) {
        AlarmRepository.setCurrentAlarmId(id)
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
        SingleAlarmManager.setAlarm(updatedAlarm.id)
        Log.d("SEND", "I WANT TO SEND ${repository.alarms.value}")
    }

    fun setAlarmId(id: Int) {
        repository.setAlarmId(id)
    }

    fun setAlarm(id: Int) {
        repository.setAlarm(id)
    }

    fun cancelAlarm(alarm: Alarm) {
        repository.cancelAlarm(alarm)
    }
    fun cancelAlarm(id: Int) {
        repository.cancelAlarm(AlarmRepository.alarms.value.find{ it.id == id}!!)
    }

    fun removeAlarmById(id: Int) {
        repository.removeAlarm(id)
    }

    fun generateNewAlarmId(): Int {
        return repository.currentAlarmIndex + 1
    }

}