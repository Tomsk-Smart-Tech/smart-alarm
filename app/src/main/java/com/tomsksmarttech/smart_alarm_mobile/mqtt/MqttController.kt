package com.tomsksmarttech.smart_alarm_mobile.mqtt

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.gson.Gson
import com.tomsksmarttech.smart_alarm_mobile.SharedData
import com.tomsksmarttech.smart_alarm_mobile.home.SettingsFunctions
import kotlinx.coroutines.launch

class MqttController(val context: Context, val lifecycle: LifecycleCoroutineScope) {

    val sf = SettingsFunctions()

    fun connectAndSync() {
        sf.connectToDevice(context)
        val content = SharedData.alarms.value
        val gson = Gson()
        val jsonString = gson.toJson(content)
        if (jsonString != "[]") {
            lifecycle.launch {
                sf.sendMessage(jsonString, "mqtt/alarms")
                Log.d("ALARMS", "Content send: $jsonString")
            }
        }
    }



    suspend fun checkIfShouldSave() {
//        if (targetRoute != Screens.Home.route) {
        Log.d("ALARM", "checkif " + SharedData.alarms.value.toList().toString())
        try {
            SharedData.updateAlarms()
        } catch (e: Exception) {
            Log.d("ALARMS", e.toString())
        }
//        } else Log.d("HELP PLEASE", targetRoute)
    }

}