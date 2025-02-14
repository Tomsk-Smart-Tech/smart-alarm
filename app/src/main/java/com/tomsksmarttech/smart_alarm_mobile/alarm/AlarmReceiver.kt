package com.tomsksmarttech.smart_alarm_mobile.alarm

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isPhoneLocked = (context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceLocked
        val ringtoneUri = intent.getStringExtra("RINGTONE_URI") ?: ""
        val alarm = intent.getStringExtra("alarm_id") ?: ""
        Log.d("AlarmReceiver", "Alarm triggered with ringtone: $ringtoneUri")

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("ringtone_uri", ringtoneUri)
            putExtra("alarm_id", alarm)
            putExtra("is_phone_locked", isPhoneLocked)
        }
        ContextCompat.startForegroundService(context, serviceIntent)

    }
}
