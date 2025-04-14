package com.tomsksmarttech.smart_alarm_mobile.mqtt

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableDoubleStateOf
import com.google.gson.Gson
import com.tomsksmarttech.smart_alarm_mobile.SharedData
import com.tomsksmarttech.smart_alarm_mobile.alarm.Alarm
import com.tomsksmarttech.smart_alarm_mobile.alarm.AlarmRepository

class AlarmObserver(val context: Context) : MqttObserver {

    override fun onNotify(topic: String, msg: String?) {
        if (!msg.isNullOrEmpty()) {
            if (topic == "mqtt/alarms" && msg != "[]") {
                val receivedAlarms = Gson().fromJson(msg, Array<Alarm>::class.java).toMutableList()
//                if (receivedAlarms != AlarmRepository.alarms.value) {
//                    AlarmRepository.updateAlarms(receivedAlarms)
//                }

//                Log.d("ONNOTITFY", "received json: ${AlarmRepository.alarms.value}")
            } else if (topic == "mqtt/sensors") {
                val receivedData = msg.split(" ").map { it.toDouble() }
                Log.d("RECEIVE", receivedData.toString())
                SharedData.temperature.value = receivedData[0]
                SharedData.humidity.value = receivedData[1]
                SharedData.voc.value = receivedData[2]

//                Log.d("ONNOTIFY", "received: $msg")
            }
        }
    }

    override fun unsubscribe() {
        //todo
        // :(
    }
}