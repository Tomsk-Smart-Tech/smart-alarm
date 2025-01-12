package com.tomsksmarttech.smart_alarm_mobile.home

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import com.tomsksmarttech.smart_alarm_mobile.mqtt.MqttService
import kotlinx.coroutines.flow.first

class SettingsFunctions {
    suspend fun connectToDevice(context: Context, msg: String): Boolean {
        val mqttService = MqttService(context)

        return try {
            mqttService.main("my/test/topic", msg)
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