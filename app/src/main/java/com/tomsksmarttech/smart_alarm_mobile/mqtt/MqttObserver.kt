package com.tomsksmarttech.smart_alarm_mobile.mqtt

interface MqttObserver {
    fun onNotify(msg: String?) {

    }
    fun unsubscribe() {

    }
}