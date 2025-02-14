package com.tomsksmarttech.smart_alarm_mobile.home

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import com.tomsksmarttech.smart_alarm_mobile.mqtt.MqttService
import kotlinx.coroutines.flow.first

class SettingsFunctions {
    lateinit var mqttService: MqttService
    lateinit var context: Context
    private val topic = "my/test/topic"
    fun connectToDevice(context: Context) {
        this.context = context
        mqttService = MqttService(context)

    }
    suspend fun sendMessage(msg: String): Boolean {
        return try {
            mqttService.main(topic, msg)
            mqttService.connectionState.first { it }
        } catch (e: Exception) {
            Log.e("MQTT", "Ошибка подключения: ${e.message}")
            Toast.makeText(
                context,
                "Не удалось подключиться к устройству: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }

    fun about(context: Context) {
        val toast = Toast(context)
        toast.setText(LoremIpsum().values.first())
        toast.duration = Toast.LENGTH_LONG
        toast.show()
    }
}