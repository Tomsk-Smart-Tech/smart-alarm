package com.tomsksmarttech.smart_alarm_mobile.alarm

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.tomsksmarttech.smart_alarm_mobile.CHECK_TOPIC

import com.tomsksmarttech.smart_alarm_mobile.mqtt.MqttObserver
import com.tomsksmarttech.smart_alarm_mobile.mqtt.MqttService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MqttCheckService : Service(), MqttObserver {
    var isMsgReceived = false
    private var checkCallback: ((Boolean) -> Unit)? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun checkMqtt(callback: (Boolean) -> Unit) {
        checkCallback = callback
        if (!MqttService.connectionState.value) {
            MqttService.init(this)
        }
        MqttService.subscribe(CHECK_TOPIC)
        isMsgReceived = false

        MqttService.addMsg(CHECK_TOPIC, "[{Shall_I_play_alarm?}]")

        // timeout для ответа с утройства
        // (если оно не ответило, будильник экстренно срабатывает на телефоне)
        scope.launch {
            delay(10000)
            if (!isMsgReceived) {
                Log.d("Check Mqtt", "Timeout reached")
                callback(false)
                checkCallback = null
            }
        }
    }

    override fun onNotify(topic: String, msg: String?) {
        if (topic == CHECK_TOPIC) {
            Log.d("Check Mqtt", "Received message!")
            isMsgReceived = true
            checkCallback?.invoke(true)
            checkCallback = null
        }
    }
    override fun onBind(intent: Intent): IBinder? = null
}