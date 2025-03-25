package com.tomsksmarttech.smart_alarm_mobile.alarm

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.rememberCoroutineScope

import com.tomsksmarttech.smart_alarm_mobile.mqtt.MqttObserver
import com.tomsksmarttech.smart_alarm_mobile.mqtt.MqttService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


const val checkTopic = "mqtt/check"

class MqttCheckService : Service(), MqttObserver {
    var isMsgReceived = false
    private var checkCallback: ((Boolean) -> Unit)? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun checkMqtt(callback: (Boolean) -> Unit) {
        checkCallback = callback
        if (!MqttService.connectionState.value) {
            MqttService.init(this)
        }
        MqttService.subscribe(checkTopic)
        isMsgReceived = false

        MqttService.publish(checkTopic, "{Shall_I_play_alarm?}")

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
        if (topic == checkTopic) {
            Log.d("Check Mqtt", "Received message!")
            isMsgReceived = true
            checkCallback?.invoke(true)
            checkCallback = null
        }
    }
    override fun onBind(intent: Intent): IBinder? = null
}