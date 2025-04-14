
import android.app.AlarmManager
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
import android.os.Build
import android.provider.Settings
import com.tomsksmarttech.smart_alarm_mobile.R
import android.widget.Toast
import androidx.core.app.AlarmManagerCompat.setAlarmClock
import com.tomsksmarttech.smart_alarm_mobile.alarm.Alarm
import com.tomsksmarttech.smart_alarm_mobile.alarm.AlarmRepository
import androidx.work.*
import java.time.DayOfWeek

object SingleAlarmManager {
    private var systemAlarmManager: SystemAlarmManager? = null
    private lateinit var appContext: Context
    private lateinit var workManager: WorkManager
//    private val calendar = Calendar.getInstance()
    private val daysOfWeek = listOf(
        Calendar.MONDAY, Calendar.TUESDAY,
        Calendar.WEDNESDAY, Calendar.THURSDAY,
        Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
    )

    fun init(context: Context) {
        if (!::appContext.isInitialized) {
            appContext = context.applicationContext
            systemAlarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as SystemAlarmManager
            workManager = WorkManager.getInstance(appContext)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!systemAlarmManager!!.canScheduleExactAlarms()) {
                    // Разрешения нет - нужно запросить у пользователя
                    requestExactAlarmPermission(context) // Ваша функция для запроса
                    return // Не планируем будильник без разрешения
                }
            }
        }
    }

    fun requestExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
                Toast.makeText(context, "Пожалуйста, предоставьте разрешение на точные будильники", Toast.LENGTH_LONG).show() // Пример подсказки
            } catch (e: Exception) {
                Log.e("Permission", "Could not open SCHEDULE_EXACT_ALARM settings", e)
                Toast.makeText(context, "Не удалось открыть настройки разрешений", Toast.LENGTH_SHORT).show()
            }
        }
    }
    fun setAlarm(id: Int) {
        val currAlarm = AlarmRepository.alarms.value.find { it: Alarm? ->
            it!!.id == id
        }
        if (currAlarm == null) return
        if (systemAlarmManager == null) {
            throw IllegalStateException("AlarmManager is not initialized. Call AlarmManager.init(context) first.")
        }
        val now = System.currentTimeMillis()

        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, currAlarm.getHours().toInt())
            set(Calendar.MINUTE, currAlarm.getMinutes().toInt())
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val diffMillis = calendar.timeInMillis - now

        val diffHours = (diffMillis / (1000 * 60 * 60)).toInt()
        val diffMinutes = ((diffMillis / (1000 * 60)) % 60).toInt()

        val toast = Toast(appContext)
        val resources = appContext.resources
        val hoursText = resources.getQuantityString(R.plurals.hours, diffHours, diffHours)
        val minutesText = resources.getQuantityString(R.plurals.minutes, diffMinutes, diffMinutes)

        var message = if (diffHours == 0) {
            "Будильник сработает через $minutesText"
        } else if (diffMinutes == 0) {
            "Будильник сработает через $hoursText"
        } else "Будильник сработает через $hoursText $minutesText"
        toast.setText(message)
        toast.duration = Toast.LENGTH_SHORT
        toast.show()

        Log.d("TEST", "Alarm set for: ${calendar.time}")
        Log.d("TEST", "Alarm time: ${currAlarm.getHours()}, ${currAlarm.getMinutes()}")

        currAlarm.musicUri = SharedData.lastAudio?.uri.toString()

        // протестировать
        if (currAlarm.repeatDays.find{ it == true } != null) {
            scheduleAlarm(currAlarm)
        } else {
            scheduleAlarmForDay(currAlarm, Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
        }
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
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.d("TEST", "set exact and allow")
//            systemAlarmManager!!.set(
//                AlarmManager.RTC_WAKEUP,
//                calendar.timeInMillis,
//                pendingIntent)
//            Log.d("TEST", e.message.toString())
//            try {
                systemAlarmManager!!.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent)
//            } catch (e: Exception) {
//
//            }
            } else {
                Log.d("TEST", "set exact")
                systemAlarmManager!!.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent)
            }
        } catch (e: Exception) {
            systemAlarmManager?.set(
            SystemAlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
            )
        }
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
//import com.tomsksmarttech.smart_alarm_mobile.alarm.AlarmWorker
//import java.util.UUID
//import java.util.concurrent.TimeUnit
//
//object SingleAlarmManager {
//    private var systemAlarmManager: SystemAlarmManager? = null
//    private lateinit var appContext: Context
//    private lateinit var workManager: WorkManager
//    private val calendar = Calendar.getInstance()
//    private val daysOfWeek = listOf(
//        Calendar.MONDAY, Calendar.TUESDAY,
//        Calendar.WEDNESDAY, Calendar.THURSDAY,
//        Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
//    )
//
//    fun init(context: Context) {
//        if (!::appContext.isInitialized) {
//            appContext = context.applicationContext
//            systemAlarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as SystemAlarmManager
//            workManager = WorkManager.getInstance(appContext)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                if (!systemAlarmManager!!.canScheduleExactAlarms()) {
//                    // Разрешения нет - нужно запросить у пользователя
//                    requestExactAlarmPermission(context) // Ваша функция для запроса
//                    return // Не планируем будильник без разрешения
//                }
//            }
//        }
//    }
//
//    fun requestExactAlarmPermission(context: Context) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            try {
//                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
//                    data = Uri.parse("package:${context.packageName}")
//                }
//                context.startActivity(intent)
//                Toast.makeText(context, "Пожалуйста, предоставьте разрешение на точные будильники", Toast.LENGTH_LONG).show()
//            } catch (e: Exception) {
//                Log.e("Permission", "Could not open SCHEDULE_EXACT_ALARM settings", e)
//                Toast.makeText(context, "Не удалось открыть настройки разрешений", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    fun setAlarm(id: Int) {
//        val currAlarm = AlarmRepository.alarms.value.find { it: Alarm? ->
//            it!!.id == id
//        }
//        if (currAlarm == null) return
//        if (systemAlarmManager == null) {
//            throw IllegalStateException("AlarmManager is not initialized. Call AlarmManager.init(context) first.")
//        }
//        val now = System.currentTimeMillis()
//
//        val calendar: Calendar = Calendar.getInstance().apply {
//            timeInMillis = now
//            set(Calendar.HOUR_OF_DAY, currAlarm.getHours().toInt())
//            set(Calendar.MINUTE, currAlarm.getMinutes().toInt())
//            set(Calendar.SECOND, 0)
//            set(Calendar.MILLISECOND, 0)
//        }
//
//        if (calendar.timeInMillis <= now) {
//            calendar.add(Calendar.DAY_OF_YEAR, 1)
//        }
//
//        val diffMillis = calendar.timeInMillis - now
//
//        val diffHours = (diffMillis / (1000 * 60 * 60)).toInt()
//        val diffMinutes = ((diffMillis / (1000 * 60)) % 60).toInt()
//
//        val toast = Toast(appContext)
//        val resources = appContext.resources
//        val hoursText = resources.getQuantityString(R.plurals.hours, diffHours, diffHours)
//        val minutesText = resources.getQuantityString(R.plurals.minutes, diffMinutes, diffMinutes)
//
//        var message = if (diffHours == 0) {
//            "Будильник сработает через $minutesText"
//        } else if (diffMinutes == 0) {
//            "Будильник сработает через $hoursText"
//        } else "Будильник сработает через $hoursText $minutesText"
//        toast.setText(message)
//        toast.duration = Toast.LENGTH_SHORT
//        toast.show()
//
//        Log.d("TEST", "Alarm set for: ${calendar.time}")
//        Log.d("TEST", "Alarm time: ${currAlarm.getHours()}, ${currAlarm.getMinutes()}")
//
//        currAlarm.musicUri = SharedData.lastAudio?.uri.toString()
//
//        // протестировать
//        if (currAlarm.repeatDays.find{ it == true } != null) {
//            scheduleAlarm(currAlarm)
//        } else {
//            // Для одноразового будильника
//            scheduleOneTimeAlarm(currAlarm, calendar.timeInMillis)
//        }
//    }
//
//    fun scheduleAlarm(alarm: Alarm) {
//        for (i in 0..6) {
//            if (alarm.repeatDays[i]) {
//                scheduleAlarmForDay(alarm, daysOfWeek[i])
//            }
//        }
//    }
//
//    fun scheduleAlarmForDay(alarm: Alarm, dayOfWeek: Int) {
//        val calendar = Calendar.getInstance().apply {
//            set(Calendar.HOUR_OF_DAY, alarm.getHours().toInt())
//            set(Calendar.MINUTE, alarm.getMinutes().toInt())
//            set(Calendar.SECOND, 0)
//            set(Calendar.MILLISECOND, 0)
//
//            while (get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
//                add(Calendar.DAY_OF_MONTH, 1)
//            }
//        }
//
//        scheduleOneTimeAlarm(alarm, calendar.timeInMillis, dayOfWeek)
//    }
//
//    // Новый метод для планирования одноразового будильника с использованием WorkManager
//    private fun scheduleOneTimeAlarm(alarm: Alarm, triggerTimeMillis: Long, dayOfWeek: Int = -1) {
//        // Часть 1: Настраиваем обычный будильник через AlarmManager для пробуждения устройства
//        val intent = Intent(appContext, AlarmReceiver::class.java).apply {
//            action = "com.tomsksmarttech.ALARM_TRIGGERED"
//            putExtra("alarm_id", alarm.id.toString())
//            if (alarm.musicUri.toString() == "null") {
//                alarm.musicUri = getDefaultAlarmRingtoneUri().toString()
//            }
//            Log.d("TEST", "SENDING " + alarm.musicUri.toString())
//            putExtra("RINGTONE_URI", alarm.musicUri)
//        }
//
//        val requestCode = if (dayOfWeek != -1) alarm.id * 10 + dayOfWeek else alarm.id
//
//        val pendingIntent = PendingIntent.getBroadcast(
//            appContext,
//            requestCode,
//            intent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        // Используем точный будильник в зависимости от версии Android
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            systemAlarmManager?.setExactAndAllowWhileIdle(
//                SystemAlarmManager.RTC_WAKEUP,
//                triggerTimeMillis,
//                pendingIntent
//            )
//        } else {
//            systemAlarmManager?.setExact(
//                SystemAlarmManager.RTC_WAKEUP,
//                triggerTimeMillis,
//                pendingIntent
//            )
//        }
//
//        // Часть 2: Планируем работу WorkManager, которая выполнится примерно в то же время
//        // Вычисляем задержку до выполнения работы
//        val delayMillis = triggerTimeMillis - System.currentTimeMillis()
//
//        // Создаем входные данные для работы
//        val inputData = Data.Builder()
//            .putString("ringtone_uri", alarm.musicUri)
//            .putString("alarm_id", alarm.id.toString())
//            .putBoolean("is_phone_locked", false) // Будет определено в AlarmReceiver
//            .build()
//
//        // Создаем уникальный идентификатор для работы
//        val workName = if (dayOfWeek != -1)
//            "alarm_work_${alarm.id}_day_$dayOfWeek"
//        else
//            "alarm_work_${alarm.id}"
//
//        // Создаем запрос на работу, которая должна запуститься с задержкой
//        val alarmWorkRequest = OneTimeWorkRequestBuilder<AlarmWorker>()
//            .setInputData(inputData)
//            .addTag("alarm_work")
//            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
//            .build()
//
//        // Регистрируем работу в WorkManager
//        workManager.enqueueUniqueWork(
//            workName,
//            ExistingWorkPolicy.REPLACE,
//            alarmWorkRequest
//        )
//
//        Log.d("SingleAlarmManager", "Запланирован будильник для alarm_id: ${alarm.id} на ${calendar.time}")
//    }
//
//    fun cancelAlarm(alarm: Alarm) {
//        if (systemAlarmManager == null) {
//            throw IllegalStateException("AlarmManager is not initialized. Call AlarmManager.init(context) first.")
//        }
//
//        // Отменяем повторяющиеся будильники
//        if (alarm.repeatDays.find{ it == true } != null) {
//            for (i in 0..6) {
//                if (alarm.repeatDays[i]) {
//                    cancelAlarmForDay(alarm, daysOfWeek[i])
//                }
//            }
//        } else {
//            // Отменяем одноразовый будильник
//            val intent = Intent(appContext, AlarmReceiver::class.java)
//            val pendingIntent = PendingIntent.getBroadcast(
//                appContext,
//                alarm.id,
//                intent,
//                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//            )
//            systemAlarmManager?.cancel(pendingIntent)
//
//            // Отменяем соответствующую работу WorkManager
//            workManager.cancelUniqueWork("alarm_work_${alarm.id}")
//        }
//    }
//
//    private fun cancelAlarmForDay(alarm: Alarm, dayOfWeek: Int) {
//        val id = alarm.id * 10 + dayOfWeek
//
//        val intent = Intent(appContext, AlarmReceiver::class.java)
//        val pendingIntent = PendingIntent.getBroadcast(
//            appContext,
//            id,
//            intent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//        systemAlarmManager?.cancel(pendingIntent)
//
//        // Отменяем соответствующую работу WorkManager
//        workManager.cancelUniqueWork("alarm_work_${alarm.id}_day_$dayOfWeek")
//    }
//
//    fun getDefaultAlarmRingtoneUri(): Uri {
//        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
//        return if (uri != null) {
//            uri
//        } else {
//            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
//        }
//    }
//
//    // Новый метод для отмены ВСЕХ сработавших будильников
//    fun cancelAllActiveAlarms() {
//        workManager.cancelAllWorkByTag("alarm_work")
//    }
//}