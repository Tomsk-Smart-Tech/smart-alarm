package com.tomsksmarttech.smart_alarm_mobile.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar

class AlarmManager(private var alarms: List<Alarm>, private val context: Context) {
    private var alarmMgr: AlarmManager? = null
    private lateinit var alarmIntent: PendingIntent
    init {
        alarms.forEachIndexed {index: Int, it: Alarm ->
            if (it.isEnabled == true) {
                launchAlarm(index)
            }
        }
    }

    fun launchAlarm(i: Int) {
        Log.d("TEST", "Launching Alarm: {$i}, {$alarms}")
        alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.MINUTE, 1) // test
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.tomsksmarttech.ALARM_ACTION"
        }

        alarmIntent = PendingIntent.getBroadcast(
            context,
            i, // requestCode
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmMgr?.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            alarmIntent
        )
    }


    fun cancelAlarm(i: Int) {
        TODO("WIP")
    }
}
