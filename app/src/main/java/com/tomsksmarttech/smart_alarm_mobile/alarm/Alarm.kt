package com.tomsksmarttech.smart_alarm_mobile.alarm

import android.util.Log
import com.tomsksmarttech.smart_alarm_mobile.SharedData.alarms

data class Alarm(
    val id: Int,
    var time: String, // "HH:mm"
    var isEnabled: Boolean,
    val repeatDays: List<String>? = null,
    var label: String,
    var musicUri: String? = null
) {
    fun getHours(): String {
        try {
        return time.substring(0, endIndex = 2)
        } catch (e: Exception) {
            Log.e("ERROR", "er ${time} : ${alarms.value}")
            return ""
        }
    }
    fun getMinutes(): String {
        try {
            val mins = time.substring(3)
            return String.format("%02d", mins)
        } catch (e: Exception) {
            Log.e("ERROR", "er ${time} : ${alarms.value}")
            return time.substring(3)
        }
    }
}