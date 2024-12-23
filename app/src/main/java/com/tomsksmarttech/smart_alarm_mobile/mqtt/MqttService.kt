package com.tomsksmarttech.smart_alarm_mobile.mqtt

import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client

class MqttService(private val context: Context) {

    private lateinit var client: Mqtt3AsyncClient
    private val address = "192.168.1.112"
    private val port = 1883
    private var topic: String = ""

    fun main(topic: String, msg: String) {
        this.topic = topic
        client = Mqtt3Client.builder()
            .identifier("android_device_${Build.DEVICE}")
            .serverHost(address)
            .serverPort(port)
            .buildAsync()

        connectAndPublish(msg)
    }

    private fun connectAndPublish(msg: String) {
        client.connectWith()
            .simpleAuth()
            .username("android boy")
            .password("123".toByteArray())
            .applySimpleAuth()
            .send()
            .whenComplete { _, throwable ->
                if (throwable != null) {
                    Log.e("MqttService", "Ошибка подключения: ${throwable.message}")
                } else {
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
}
