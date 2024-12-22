package com.tomsksmarttech.smart_alarm_mobile.mqtt

import android.content.Context
import android.widget.Toast
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish

class MqttService {
    lateinit var client: Mqtt3BlockingClient
    lateinit var appContext: Context
    lateinit var topic: String
    val address = "192.168.137.102"
    val port = 1883
    fun init(context: Context) {
        if (!::appContext.isInitialized) {
            appContext = context.applicationContext
        }
    }

    fun main(topic: String) {
        this.topic = topic
        client = Mqtt3Client.builder().identifier("ggg")
        .serverHost(address)
        .serverPort(port)
            .buildBlocking()
        connect()
        var message: String = "Hello, I'm ESP32 ^_^"
        publish(message)
        subscribe()
    }

    fun connect () {
        try {
            val connAckMessage = client.connectWith()
                .simpleAuth()
                .username("android boy")
                .password("123".toByteArray())
                .applySimpleAuth()
                .willPublish()
                .topic(topic)
                .qos(MqttQos.AT_LEAST_ONCE)
                .payload("off".toByteArray())
                .retain(true)
                .applyWillPublish()
                .send()
            Toast.makeText(appContext, connAckMessage.toString(), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(appContext, "Kotlin Fehler beim Senden", Toast.LENGTH_LONG)
                .show()
        } catch (e: java.lang.Exception) {
            Toast.makeText(appContext, "Java Fehler beim Senden", Toast.LENGTH_LONG).show()
        }
    }

    fun publish (msg: String) {
        client
            .publishWith()
            .topic(topic)
            .qos(MqttQos.AT_LEAST_ONCE)
            .payload(msg.toByteArray())
            .send()

    }

    fun subscribe() {
        var lastRes : Mqtt3Publish
        client.toAsync().subscribeWith()
            .topicFilter(topic)
            .qos(MqttQos.AT_LEAST_ONCE)
            .callback { actResponse ->
                lastRes = actResponse
            }
            .send()
    }
}