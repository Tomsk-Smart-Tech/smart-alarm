package com.tomsksmarttech.smart_alarm_mobile.alarm

import android.util.Log

data class Alarm(
    val id: Int,
    var time: String, // "HH:mm"
    var isEnabled: Boolean,
    var isHaptic: Boolean,
    var repeatDays: List<Boolean> = listOf(false,false,false,false,false,false,false),
    var label: String,
    var musicUri: String? = null,
    var song: String? = null,
    var isSended: Boolean = false
) {
    fun getHours(): String {
        try {
            return time.substring(0, endIndex = 2)
        } catch (e: Exception) {
            Log.e("ERROR", "er $time : ${AlarmRepository.alarms.value}")
            return ""
        }
    }
    fun getMinutes(): String {
        val mins = time.substring(3)
        Log.d("ALARM", "mins $mins")
        return mins
    }
}