package com.tomsksmarttech.smart_alarm_mobile.mqtt

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.gson.Gson
import com.tomsksmarttech.smart_alarm_mobile.SharedData
import com.tomsksmarttech.smart_alarm_mobile.alarm.Alarm
import com.tomsksmarttech.smart_alarm_mobile.home.SettingsFunctions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MqttController(val context: Context, val cs: CoroutineScope) {

    val sf = SettingsFunctions()

    fun connect() {
        sf.connectToDevice(context)
    }

    fun send(list: List<Alarm?>) {
        val gson = Gson()
        val jsonString = gson.toJson(list)
            Log.d("ALARMS", "Preparing to sending $jsonString $list")
        if (jsonString != "[]") {
            cs.launch {
                runCatching {
                    sf.sendMessage(jsonString, "mqtt/alarms")
                    Log.d("ALARMS", "Content sent: $jsonString")
                    launch{
                        SharedData.alarms.value.forEach { it?.isSended = true }
                        Log.d("MQTT", "alarms sended, param changed")
                    }
                }.onFailure { error ->
                    Log.e("ALARMS", "Failed to send message: ${error.localizedMessage}", error)
                    SharedData.saveAlarms(context, SharedData.alarms.value)
                }
            }
        }
    }




//    suspend fun checkIfShouldSave() {
////        if (targetRoute != Screens.Home.route) {
//        Log.d("ALARM", "checkif " + SharedData.alarms.value.toList().toString())
//        try {
//            SharedData.updateAlarms()
//        } catch (e: Exception) {
//            Log.d("ALARMS", e.toString())
//        }
////        } else Log.d("HELP PLEASE", targetRoute)
//    }

}