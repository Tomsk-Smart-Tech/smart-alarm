package com.tomsksmarttech.smart_alarm_mobile.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("AlarmReceiver", "Будильник сработал!")
    }
}
