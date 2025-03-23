package com.tomsksmarttech.smart_alarm_mobile.mqtt

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableDoubleStateOf
import com.google.gson.Gson
import com.tomsksmarttech.smart_alarm_mobile.SharedData
import com.tomsksmarttech.smart_alarm_mobile.alarm.Alarm

class AlarmObserver(val context: Context) : MqttObserver {

    override fun onNotify(topic: String, msg: String?) {
        if (!msg.isNullOrEmpty()) {
            if (topic == "mqtt/alarms" && msg != "[]") {
                val receivedAlarms = Gson().fromJson(msg, Array<Alarm?>::class.java).toMutableList()
                SharedData.alarms.value = receivedAlarms
                Log.d("ONNOTITFY", "received json: ${SharedData.alarms.value}")
            } else if (topic == "mqtt/sensors") {
                val receivedData = msg.split(" ").map{ it.toDouble() }
                SharedData.temperature = mutableDoubleStateOf(receivedData[0])
                SharedData.humidity = mutableDoubleStateOf(receivedData[1])

                Log.d("ONNOTIFY", "received: $msg")
            }
        }
    }

    override fun unsubscribe() {
        //todo
        // :(
    }
}