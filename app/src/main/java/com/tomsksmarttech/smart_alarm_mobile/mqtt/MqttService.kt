package com.tomsksmarttech.smart_alarm_mobile.mqtt

import android.content.Context
import android.widget.Toast
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client

class MqttService(private val context: Context) {
    private lateinit var client: Mqtt3BlockingClient
    private lateinit var topic: String
    private val address = "192.168.1.112"
    private val port = 1883

    fun main(topic: String, msg: String) {
        this.topic = topic
        client = Mqtt3Client.builder().identifier("ggg")
            .serverHost(address)
            .serverPort(port)
            .buildBlocking()
        connect()
//        val message = "Hello, I'm ESP32 ^_^"
        publish(msg)
        subscribe()
    }

    private fun connect() {
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
            showToast("Connected: $connAckMessage")
        } catch (e: Exception) {
            showToast("Error connecting: ${e.message}")
        }
    }

    private fun publish(msg: String) {
        client.publishWith()
            .topic(topic)
            .qos(MqttQos.AT_LEAST_ONCE)
            .payload(msg.toByteArray())
            .send()
    }

    private fun subscribe() {
        client.toAsync().subscribeWith()
            .topicFilter(topic)
            .qos(MqttQos.AT_LEAST_ONCE)
            .callback { actResponse ->
                showToast("Received: ${actResponse.payload}")
            }
            .send()
    }

    private fun showToast(message: String) {
//        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}