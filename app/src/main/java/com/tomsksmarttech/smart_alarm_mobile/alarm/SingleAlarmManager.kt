import android.app.AlarmManager as SystemAlarmManager
import android.content.Context
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import com.tomsksmarttech.smart_alarm_mobile.SharedData
import com.tomsksmarttech.smart_alarm_mobile.alarm.AlarmReceiver
import java.util.Calendar
import android.media.RingtoneManager
import android.net.Uri
import com.tomsksmarttech.smart_alarm_mobile.R
import android.widget.Toast
import com.tomsksmarttech.smart_alarm_mobile.alarm.Alarm
import com.tomsksmarttech.smart_alarm_mobile.alarm.AlarmRepository
import java.util.Date


object SingleAlarmManager {
    private var systemAlarmManager: SystemAlarmManager? = null
    private lateinit var appContext: Context
    private val daysOfWeek = listOf(
        Calendar.MONDAY, Calendar.TUESDAY,
        Calendar.WEDNESDAY, Calendar.THURSDAY,
        Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
    )

    fun init(context: Context) {
        if (!::appContext.isInitialized) {
            appContext = context.applicationContext
            systemAlarmManager =
                appContext.getSystemService(Context.ALARM_SERVICE) as SystemAlarmManager
        }
    }

    fun setAlarm(id: Int) {
        val currAlarm = AlarmRepository.alarms.value.find { it: Alarm? ->
            it!!.id == id
        }
        if (currAlarm == null) return
        Log.d(
            "ALARM",
            "HELP I WANT TO MEOW MEOW MEOW $id, ${
                currAlarm.getHours().toInt()
            }, ${currAlarm.getMinutes().toInt()} "
        )
        if (systemAlarmManager == null) {
            throw IllegalStateException("AlarmManager is not initialized. Call AlarmManager.init(context) first.")
        }
        val now = System.currentTimeMillis()

        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, currAlarm.getHours().toInt())
            set(Calendar.MINUTE, currAlarm.getMinutes().toInt() - 1) // очень сомнительная авантюра
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        Log.d("AlarmDebug", "Текущее время: ${Date(now)}")
        Log.d("AlarmDebug", "Время будильника: ${Date(calendar.timeInMillis)}")

        val diffMillis = calendar.timeInMillis - now

        val diffHours = (diffMillis / (1000 * 60 * 60)).toInt()
        val diffMinutes = ((diffMillis / (1000 * 60)) % 60).toInt()

//        val hrsRemain = String.format("%02d", diffHours)
//        val minsRemain = String.format("%02d", diffMinutes)

        val toast = Toast(appContext)
        val resources = appContext.resources
        val hoursText = resources.getQuantityString(R.plurals.hours, diffHours, diffHours)
        val minutesText = resources.getQuantityString(R.plurals.minutes, diffMinutes, diffMinutes)

        var message = if (diffHours == 0) {
            "Будильник сработает через $minutesText"
        } else if (diffMinutes == 0) {
            "Будильник сработает через $hoursText"
        } else "Будильник сработает через $hoursText $minutesText"
//            if (minutesText.isNotEmpty()) {
//                toast.setText(message)
//            } else {
//                toast.setText(hoursText)
//            }
//        } else {
//            }
        toast.setText(message)
//        }
//        toast.setText("Будильник сработает через $hrsRemain часов $minsRemain минут")
        toast.duration = Toast.LENGTH_SHORT
        toast.show()

        Log.d("TEST", "Alarm set for: ${calendar.time}")
        Log.d("TEST", "Alarm time: ${currAlarm.getHours()}, ${currAlarm.getMinutes()}")

        currAlarm.musicUri = SharedData.lastAudio?.uri.toString()
        // test
        // временное решение
//
//        val intent = Intent(appContext, AlarmReceiver::class.java).apply {
//            action = "com.tomsksmarttech.ALARM_TRIGGERED"
//            putExtra("alarm_id", id.toString())
//            if (currAlarm.musicUri.toString() == "null") {
//                currAlarm.musicUri = getDefaultAlarmRingtoneUri().toString()
//            }
//            Log.d("TEST", "SENDING " + currAlarm.musicUri.toString())
//            putExtra("RINGTONE_URI", currAlarm.musicUri)
//        }
//        val pendingIntent = PendingIntent.getBroadcast(
//            appContext,
//            id,
//            intent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//        systemAlarmManager!!.set(
////        systemAlarmManager?.setExact(
//            SystemAlarmManager.RTC_WAKEUP,
//            calendar.timeInMillis,
//            pendingIntent
//        )
        // протестировать

        scheduleAlarm(currAlarm)
    }

    fun scheduleAlarm(alarm: Alarm) {

        for (i in 0..6) {
            if (alarm.repeatDays[i]) {
                scheduleAlarmForDay(alarm, daysOfWeek[i])
            }
        }
    }

    fun scheduleAlarmForDay(alarm: Alarm, dayOfWeek: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.getHours().toInt())
            set(Calendar.MINUTE, alarm.getMinutes().toInt())
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            while (get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val intent = Intent(appContext, AlarmReceiver::class.java).apply {
            action = "com.tomsksmarttech.ALARM_TRIGGERED"
            putExtra("alarm_id", alarm.id.toString())
            if (alarm.musicUri.toString() == "null") {
                alarm.musicUri = getDefaultAlarmRingtoneUri().toString()
            }
            Log.d("TEST", "SENDING " + alarm.musicUri.toString())
            putExtra("RINGTONE_URI", alarm.musicUri)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            alarm.id * 10 + dayOfWeek, // Уникальный ID для каждого дня
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        systemAlarmManager?.set(
            SystemAlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    fun cancelAlarm(alarm: Alarm) {
        if (systemAlarmManager == null) {
            throw IllegalStateException("AlarmManager is not initialized. Call AlarmManager.init(context) first.")
        }
        for (i in 0..6) {
            if (alarm.repeatDays[i]) {
                cancelAlarmForDay(alarm, daysOfWeek[i])
            }
        }


    }

    private fun cancelAlarmForDay(alarm: Alarm, dayOfWeek: Int) {
        // попробуем так
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.getHours().toInt())
            set(Calendar.MINUTE, alarm.getMinutes().toInt())
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            while (get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        val id = alarm.id * 10 + dayOfWeek

        val intent = Intent(appContext, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        systemAlarmManager?.cancel(pendingIntent)
    }

    fun getDefaultAlarmRingtoneUri(): Uri {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        return if (uri != null) {
            uri
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }
}