package com.tomsksmarttech.smart_alarm_mobile.mqtt

import com.tomsksmarttech.smart_alarm_mobile.R
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.tomsksmarttech.smart_alarm_mobile.SharedData
import com.tomsksmarttech.smart_alarm_mobile.alarm.Alarm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.properties.Delegates
import kotlin.text.Charsets.UTF_8

object MqttService {

    private lateinit var client: Mqtt5AsyncClient
    private lateinit var address: String
    private lateinit var username: String
    private lateinit var password: String
    private var cs: CoroutineScope? = null
    private var port by Delegates.notNull<Int>()

    private var topic: String = ""
    private var isConnected = false
    private val observers = mutableListOf<MqttObserver>()
    private val subscribedTopics = mutableSetOf<String>()
    val connectionState = MutableStateFlow(false)

    fun init(context: Context) {
        address = context.getString(R.string.broker_url)
        username = context.getString(R.string.username)
        password = context.getString(R.string.password)
        port = context.getString(R.string.port).toInt()

        if (!::client.isInitialized) {
            Log.d("MQTT", "init mqtt client")
            client = Mqtt5Client.builder()
                .identifier("android_device_${Build.DEVICE}")
                .serverHost(address)
                .serverPort(port)
                .sslWithDefaultConfig()
                .buildAsync()
        }
    }

    fun addObserver(observer: MqttObserver) {
        observers.add(observer)
    }

    fun connect() {
        if (isConnected) return

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
                } else {
                    isConnected = true
                    connectionState.value = true
                    Log.i("MqttService", "Успешное подключение к брокеру")
                }
            }
    }

    fun publish(topic: String, message: String) {
        this.topic = topic
        if (!isConnected) {
            Log.e("MqttService", "Попытка отправить сообщение без подключения!")
            return
        }

        client.publishWith()
            .topic(topic)
            .qos(MqttQos.AT_LEAST_ONCE)
            .payload(message.toByteArray())
            .send()
            .whenComplete { _, throwable ->
                if (throwable != null) {
                    Log.e("MqttService", "Ошибка публикации: ${throwable.message}")
                } else {
                    Log.i("MqttService", "Сообщение отправлено в $topic: $message")
                }
            }
    }

    fun subscribe(topic: String) {
        this.topic = topic
        if (!isConnected) {
            Log.e("MqttService", "Попытка подписки без подключения!")
            return
        }

        if (subscribedTopics.contains(topic)) {
            Log.i("MqttService", "Уже подписаны на топик: $topic")
            return
        }

        client.subscribeWith()
            .topicFilter(topic)
            .noLocal(true)
            .qos(MqttQos.AT_LEAST_ONCE)
            .callback { publishMessage ->
                val receivedMsg = String(publishMessage.payloadAsBytes, UTF_8)
                Log.i("MqttService", "Получено сообщение из $topic: $receivedMsg") // теперь получаем сообщения из различных топиков

                observers.forEach { it.onNotify(topic, receivedMsg) }
            }
            .send()
            .whenComplete { _, throwable ->
                if (throwable != null) {
                    Log.e("MqttService", "Ошибка подписки: ${throwable.message}")
                } else {
                    subscribedTopics.add(topic) // добавление в список топиков
                    Log.i("MqttService", "Подписались на топик: $topic")
                }
            }
    }

    fun initCoroutineScope(cs: CoroutineScope) {
        this.cs = cs
    }

    fun sendList(list: List<Alarm?>, context: Context) {
        val gson = Gson()
        val jsonString = gson.toJson(list)
        Log.d("ALARMS", "Preparing to sending $jsonString $list")
        if (jsonString != "[]") {
            cs?.launch {
                runCatching {
                    publish(topic, jsonString)
                    subscribe(topic)
                    Log.d("ALARMS", "Content sent: $jsonString")
                    launch{
                        SharedData.alarms.value.forEach { it?.isSended = true }
                        Log.d("MQTT", "alarms sended, param changed")
                    }
                }.onFailure { error ->
                    Log.e("ALARMS", "Failed to send message: ${error.localizedMessage}", error)
                    SharedData.saveAlarms(context, SharedData.alarms.value)
                }
            }
        }
    }
}