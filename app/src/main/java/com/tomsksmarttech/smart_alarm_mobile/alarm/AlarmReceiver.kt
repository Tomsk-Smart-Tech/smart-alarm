package com.tomsksmarttech.smart_alarm_mobile.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ringtoneUri = intent.getStringExtra("RINGTONE_URI") ?: ""
        val alarm = intent.getStringExtra("alarm_id") ?: ""
        Log.d("AlarmReceiver", "Alarm triggered with ringtone: $ringtoneUri")

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("RINGTONE_URI", ringtoneUri)
            putExtra("alarm_id", alarm)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
