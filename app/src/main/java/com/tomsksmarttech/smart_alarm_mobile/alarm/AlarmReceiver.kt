package com.tomsksmarttech.smart_alarm_mobile.alarm

import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat.startForeground
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.tomsksmarttech.ALARM_TRIGGERED" -> handleAlarmTrigger(context, intent)
            "com.tomsksmarttech.NOTIFICATION_CLICKED" -> handleNotificationClick(context, intent)
            else -> Log.d("AlarmReceiver", "Неизвестный intent: ${intent.action}")
        }
    }

    private fun handleAlarmTrigger(context: Context, intent: Intent) {
        val isPhoneLocked = (context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceLocked
        val ringtoneUri = intent.getStringExtra("RINGTONE_URI") ?: ""
        val alarm = intent.getStringExtra("alarm_id") ?: ""
        Log.d("AlarmReceiver", "Будильник сработал: $ringtoneUri")

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("ringtone_uri", ringtoneUri)
            putExtra("alarm_id", alarm)
            putExtra("is_phone_locked", isPhoneLocked.toString())
        }
        ContextCompat.startForegroundService(context, serviceIntent)


    }

    private fun handleNotificationClick(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "Уведомление нажато!")
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            action = "STOP_ALARM"
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}

