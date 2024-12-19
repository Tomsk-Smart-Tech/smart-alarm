import android.app.AlarmManager as SystemAlarmManager
import android.content.Context
import com.tomsksmarttech.smart_alarm_mobile.SharedData
import com.tomsksmarttech.smart_alarm_mobile.alarm.Alarm
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import com.tomsksmarttech.smart_alarm_mobile.SharedData.alarms
import com.tomsksmarttech.smart_alarm_mobile.alarm.AlarmReceiver
import java.util.Calendar

object SingleAlarmManager {
    private var systemAlarmManager: SystemAlarmManager? = null
    private lateinit var appContext: Context

    fun init(context: Context) {
        if (!::appContext.isInitialized) {
            appContext = context.applicationContext
            systemAlarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as SystemAlarmManager
        }
    }

    fun setAlarm(id: Int) {
        if (systemAlarmManager == null) {
            throw IllegalStateException("AlarmManager is not initialized. Call AlarmManager.init(context) first.")
        }

//        val calendar: Calendar = Calendar.getInstance().apply {
//            timeInMillis = System.currentTimeMillis()
//            set(Calendar.HOUR_OF_DAY, hours)
//            set(Calendar.MINUTE, minutes)
//        }
        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
//            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.HOUR_OF_DAY, alarms.value[id].getHours().toInt())
            set(Calendar.MINUTE, alarms.value[id].getMinutes().toInt())
        }

        val intent = Intent(appContext, AlarmReceiver::class.java).apply {
            action = "com.tomsksmarttech.ALARM_ACTION"
            putExtra("alarm_id", id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        systemAlarmManager?.setExact(
            SystemAlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    fun cancelAlarm(id: Int) {
        if (systemAlarmManager == null) {
            throw IllegalStateException("AlarmManager is not initialized. Call AlarmManager.init(context) first.")
        }

        val intent = Intent(appContext, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        systemAlarmManager?.cancel(pendingIntent)
    }
}


//object SingleAlarmManager {
//    private var alarmMgr: SystemAlarmManager? = null
//    private lateinit var appContext: Context
//    private lateinit var alarmIntent: PendingIntent
//
//    fun init(context: Context) {
//        if (!::appContext.isInitialized) {
//            appContext = context.applicationContext
//            alarmMgr = appContext.getSystemService(Context.ALARM_SERVICE) as SystemAlarmManager
//        }
//        SharedData.alarms.forEachIndexed {index: Int, it: Alarm ->
//            if (it.isEnabled == true) {
//                launchAlarm(index)
//            }
//        }
//    }
//    fun get(): SystemAlarmManager {
//        if (alarmMgr == null) {
//            throw IllegalStateException("AlarmManager is not initialized. Call AlarmManager.init(context) first.")
//        }
//        return alarmMgr!!
//    }
////    fun launchAlarm(i: Int) {
////
////    }
//    fun setAlarm(i: Int) {
//        Log.d("TEST", "Launching Alarm: {$i}, {$alarms}")
//
//        alarmMgr = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//
//        val calendar: Calendar = Calendar.getInstance().apply {
//            timeInMillis = System.currentTimeMillis()
//            add(Calendar.MINUTE, 1) // test
//        }
//
//        val intent = Intent(appContext, AlarmReceiver::class.java).apply {
//            action = "com.tomsksmarttech.ALARM_ACTION"
//        }
//
//        alarmIntent = PendingIntent.getBroadcast(
//            appContext,
//            i, // requestCode
//            intent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        alarmMgr?.setExactAndAllowWhileIdle(
//            AlarmManager.RTC_WAKEUP,
//            calendar.timeInMillis,
//            alarmIntent
//        )
//    }
//
//    fun cancelAlarm(i: Int) {
//    }
//}





//package com.tomsksmarttech.smart_alarm_mobile.alarm
//
//import android.app.AlarmManager
//import android.app.PendingIntent
//import android.content.Context
//import android.content.Intent
//import android.util.Log
//import java.util.Calendar
//
//class AlarmManager(private var alarms: List<Alarm>, private val context: Context) {
//    private var alarmMgr: AlarmManager? = null
//    init {
//        alarms.forEachIndexed {index: Int, it: Alarm ->
//            if (it.isEnabled == true) {
//                launchAlarm(index)
//            }
//        }
//    }
//
//    fun launchAlarm(i: Int) {
//        Log.d("TEST", "Launching Alarm: {$i}, {$alarms}")
//        alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//
//        val calendar: Calendar = Calendar.getInstance().apply {
//            timeInMillis = System.currentTimeMillis()
//            add(Calendar.MINUTE, 1) // test
//        }
//
//        val intent = Intent(context, AlarmReceiver::class.java).apply {
//            action = "com.tomsksmarttech.ALARM_ACTION"
//        }
//
//        alarmIntent = PendingIntent.getBroadcast(
//            context,
//            i, // requestCode
//            intent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        alarmMgr?.setExactAndAllowWhileIdle(
//            AlarmManager.RTC_WAKEUP,
//            calendar.timeInMillis,
//            alarmIntent
//        )
//    }
//
//
//    fun cancelAlarm(i: Int) {
//        TODO("WIP")
//    }
//}
