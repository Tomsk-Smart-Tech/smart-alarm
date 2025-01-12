package com.tomsksmarttech.smart_alarm_mobile.mqtt

import android.content.Context
import android.os.Build
import android.util.Log
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.text.Charsets.UTF_8

class MqttService(context: Context) {

    private lateinit var client: Mqtt5AsyncClient
    private val address = "6a41760a26ec43f2b0e532601ce780e1.s1.eu.hivemq.cloud"
    private val port = 8883
    private var topic: String = ""
    val connectionState = MutableStateFlow(false)

    fun main(topic: String, msg: String) {
        this.topic = topic
        client = Mqtt5Client.builder()
            .identifier("android_device_${Build.DEVICE}")
            .serverHost(address)
            .serverPort(port)
            .sslWithDefaultConfig()
            .buildAsync()

        connectAndPublish(msg)
    }

    private fun connectAndPublish(msg: String) {
        client.connectWith()
            .simpleAuth()
            .username("android_boy")
            .password(UTF_8.encode("123456aA"))
            .applySimpleAuth()
            .send()
            .whenComplete { _, throwable ->
                if (throwable != null) {
                    connectionState.value = false
                    Log.e("MqttService", "Ошибка подключения: ${throwable.message}")
                    throw throwable
                } else {
                    connectionState.value = true
                    Log.i("MqttService", "Успешное подключение к брокеру")
                    publish(msg)
                }
            }
    }

    private fun publish(msg: String) {
        client.publishWith()
            .topic(topic)
            .qos(MqttQos.AT_LEAST_ONCE)
            .payload(msg.toByteArray())
            .send()
            .whenComplete { _, throwable ->
                if (throwable != null) {
                    Log.e("MqttService", "Ошибка публикации: ${throwable.message}")
                } else {
                    Log.i("MqttService", "Сообщение отправлено: $msg")
                }
            }
    }
//    fun isConnectionEstablished(): Boolean {
//        return connectionState
//    }
}
