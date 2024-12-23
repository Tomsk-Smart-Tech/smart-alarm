package com.tomsksmarttech.smart_alarm_mobile.home

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import com.tomsksmarttech.smart_alarm_mobile.mqtt.MqttService

class SettingsFunctions {
    fun connectToDevice(context: Context, msg: String): Boolean {
        try {
            val mqttService = MqttService(context)
            mqttService.main("test/topic", msg)
            return true
        } catch (e: Exception) {
            Log.d("MQTT", e.toString())
            return false
        }
    }

    //    fun connectToDevice(context: Context, msg: String): Boolean {
//        try {
//            val mqttService = MqttService(context)
//            mqttService.main("test/topic", msg)
//            return true
//        } catch (e: Exception) {
//            Log.e("ALARM", e.toString())
//            Toast.makeText(context, "Не удалось подключиться к устройству",
//                Toast.LENGTH_SHORT).show()
//            return false
//        }
//    }
    fun about(context: Context) {
        val toast = Toast(context)
        toast.setText(LoremIpsum().values.first())
        toast.duration = Toast.LENGTH_LONG
        toast.show()
    }
}