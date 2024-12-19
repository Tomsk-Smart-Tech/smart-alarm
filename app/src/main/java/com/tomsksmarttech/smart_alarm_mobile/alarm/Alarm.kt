package com.tomsksmarttech.smart_alarm_mobile.alarm

data class Alarm(
    val id: Int,
    var time: String, // "HH:mm"
    var isEnabled: Boolean,
    val repeatDays: List<String>? = null,
    var label: String,
) {
    fun getHours(): String {
        return time.substring(0,2)
    }
    fun getMinutes(): String {
        return time.substring(3,4)
    }
}