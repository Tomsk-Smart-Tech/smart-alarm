package com.tomsksmarttech.smart_alarm_mobile.mqtt

import com.tomsksmarttech.smart_alarm_mobile.R
import android.content.Context
import android.os.Build
import android.util.Log
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.exceptions.Mqtt3MessageException
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.exceptions.Mqtt5MessageException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.text.Charsets.UTF_8

class MqttService(context: Context) {

    private lateinit var client: Mqtt5AsyncClient
    private val address = context.getString(R.string.broker_url)
    private val username = context.getString(R.string.username)
    private val password = context.getString(R.string.password)
    private val port = context.getString(R.string.port).toInt()
    private var topic: String = ""
    private var observers = mutableListOf<MqttObserver>()
    val connectionState = MutableStateFlow(false)

    @Throws(Mqtt5MessageException::class)
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

    fun addObserver(mo: MqttObserver) {
        observers.add(mo)
    }

    private fun connectAndPublish(msg: String) {
        client.connectWith()
            .simpleAuth()
            .username(username)
            .password(UTF_8.encode(password))
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

    fun subscribe() {
        val msg = client.subscribeWith()
        observers.forEach{ it.onNotify(msg.toString()) }
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
