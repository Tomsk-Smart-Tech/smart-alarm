package com.tomsksmarttech.smart_alarm_mobile.mqtt

import com.google.gson.Gson
import com.tomsksmarttech.smart_alarm_mobile.SharedData
import com.tomsksmarttech.smart_alarm_mobile.alarm.Alarm

class AlarmObserver: MqttObserver {

    override fun onNotify(msg: String?) {
        if (!msg.isNullOrEmpty()) {
            val receivedAlarms = Gson().fromJson(msg, Array<Alarm?>::class.java).toMutableList()
            SharedData.alarms.value = receivedAlarms
        }
    }

    override fun unsubscribe() {
        //todo
        // :(
    }
}