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
        val mins = time.substring(3)
        Log.d("ALARM", "mins $mins")
        return mins
    }
}