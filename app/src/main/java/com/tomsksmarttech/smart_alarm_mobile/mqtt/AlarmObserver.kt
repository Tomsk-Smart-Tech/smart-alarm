package com.tomsksmarttech.smart_alarm_mobile.mqtt

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.tomsksmarttech.smart_alarm_mobile.SharedData
import com.tomsksmarttech.smart_alarm_mobile.alarm.Alarm

class AlarmObserver(val context: Context) : MqttObserver {

    override fun onNotify(topic: String, msg: String?) {
        if (!msg.isNullOrEmpty()) {
//            Log.d("MQTT", "recieved $msg")
            if (topic == "mqtt/alarms" && msg != "[]") {
                val receivedAlarms = Gson().fromJson(msg, Array<Alarm?>::class.java).toMutableList()
                SharedData.alarms.value = receivedAlarms
                Log.d("ONNOTITFY", "received json: ${SharedData.alarms.value}")
//                AlarmScreen()
            } else {
                Log.d("ONNOTIFY", "received: $msg")
            }
        }
    }

    override fun unsubscribe() {
        //todo
        // :(
    }
}