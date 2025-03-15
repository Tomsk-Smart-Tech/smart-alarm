package com.tomsksmarttech.smart_alarm_mobile.mqtt

interface MqttObserver {
    fun onNotify(topic: String, msg: String?) {

    }
    fun unsubscribe() {

    }
}