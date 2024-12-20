package com.tomsksmarttech.smart_alarm_mobile.alarm

import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.tomsksmarttech.smart_alarm_mobile.SharedData
import com.tomsksmarttech.smart_alarm_mobile.SharedData.currentAlarmIndex

object AlarmConfigurer {
    fun removeAlarm(index: Int) {
        if (index >= 0 && SharedData.alarms.value.find { it.id == index } != null) {
            SharedData.alarms.value.removeIf { it.id == index }
        }
    }
}
