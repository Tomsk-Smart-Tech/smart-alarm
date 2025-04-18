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
import com.tomsksmarttech.smart_alarm_mobile.alarm.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.properties.Delegates
import kotlin.text.Charsets.UTF_8

object MqttService {

    private lateinit var client: Mqtt5AsyncClient
    private lateinit var address: String
    private lateinit var username: String
    private lateinit var password: String
    private var port by Delegates.notNull<Int>()

    private var isConnected = false
    private val observers = mutableListOf<MqttObserver>()
    val subscribedTopics = mutableSetOf<String>()
    val connectionState = MutableStateFlow(0) // -1 - не подключено, 0 - подключение, 1 - подключено
    private var cs: CoroutineScope? = null
    private val _msgDeque = MutableStateFlow(ArrayDeque<Pair<String, String>>())
    val deque = _msgDeque.asStateFlow()

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
                    connectionState.value = -1
                    Log.e("MqttService", "Ошибка подключения: ${throwable.message}")
                    if (throwable.message?.contains("connecting") == true) {
                        Log.d("MQTT", "connecting...")
                        connectionState.value = 0
                    }
                } else {
                    isConnected = true
                    connectionState.value = 1
                    Log.i("MqttService", "Успешное подключение к брокеру")
                    subscribedTopics.forEach{
                        subscribe(it)
                    }
                }
            }
    }

    private fun publish(topic: String, message: String) {
//        subscribe(topic)
        if (!isConnected) {
            Log.e("MqttService", "Попытка отправить сообщение без подключения!")
            connect()
            return
        }
        try {
            client.publishWith()
                .topic(topic)
                .qos(MqttQos.AT_LEAST_ONCE)
                .payload(message.toByteArray())
                .send()
                .whenComplete { _, throwable ->
                    if (throwable != null) {
                        Log.e("MqttService", "Ошибка публикации: ${throwable.message}")
                        client.reauth()
                        connect()
                        client.reauth()
                    } else {
                        Log.i("MqttService", "Сообщение отправлено в $topic: $message")
                    }
                }
        } catch (e: Exception) {
            Log.e("ERROR", e.message.toString())
            client.reauth()
        }
    }

    fun subscribe(topic: String) {
        if (!isConnected) {
            Log.e("MqttService", "Попытка подписки без подключения!")
            connect()
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
                Log.i("MqttService", "Получено сообщение из $topic: $receivedMsg")
                observers.forEach { it.onNotify(topic, receivedMsg) }
            }
            .send()
            .whenComplete { _, throwable ->
                if (throwable != null) {
                    Log.e("MqttService", "Ошибка подписки: ${throwable.message}")
                    if (throwable.message?.contains("closed") == true) {
                        Log.d("MQTT", "closed...")
                        connectionState.value = 0
                        client.reauth()
                        connect()
                        //intent
                    }
                    client.reauth()
                } else {
                    subscribedTopics.add(topic)
                    Log.i("MqttService", "Подписались на топик: $topic")
                }
            }
    }

    fun initCoroutineScope(cs: CoroutineScope) {
        this.cs = cs
    }

    fun send(topic: String, msg: String, context: Context) {
        cs?.launch {
            runCatching {
                subscribe(topic)
                publish(topic, msg)
                Log.d("ALARMS", "Content sent: $msg")
            }.onFailure { error ->
                Log.e("ALARMS", "Failed to send message: ${error.localizedMessage}", error)
                SharedData.saveAlarms(context, AlarmRepository.alarms.value)
            }
        }
    }

    fun addMsg(topic: String, msg: String) {
        val newDeque = ArrayDeque(_msgDeque.value).apply {
            addFirst(topic to msg)
        }
        _msgDeque.value = newDeque
        Log.d("MQTT", "Deque updated (size=${newDeque.size})")
    }

    fun <T> addList(topic: String, list: List<T>) {
        val newDeque = ArrayDeque(_msgDeque.value).apply {
            addFirst(Pair(topic, Gson().toJson(list)))
        }
        _msgDeque.value = newDeque
        Log.d("MQTT", "Deque updated (size=${newDeque.size})")
    }

    fun updateDeque(newDeque: ArrayDeque<Pair<String, String>>) {
        _msgDeque.value = newDeque
    }
}