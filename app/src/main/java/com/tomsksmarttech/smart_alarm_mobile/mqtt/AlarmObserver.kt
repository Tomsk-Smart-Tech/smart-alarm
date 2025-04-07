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

                Log.d("ONNOTITFY", "received json: ${AlarmRepository.alarms.value}")
            } else if (topic == "mqtt/sensors") {
                val receivedData = msg.split(" ").map{ it.toDouble() }
                SharedData.temperature = mutableDoubleStateOf(receivedData[0])
                SharedData.humidity = mutableDoubleStateOf(receivedData[1])
                SharedData.voc = mutableDoubleStateOf(receivedData[2])

                Log.d("ONNOTIFY", "received: $msg")
            }
        }
    }

    override fun unsubscribe() {
        //todo
        // :(
    }
}