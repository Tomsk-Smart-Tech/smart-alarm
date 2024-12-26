import android.app.AlarmManager as SystemAlarmManager
import android.content.Context
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import com.tomsksmarttech.smart_alarm_mobile.SharedData
import com.tomsksmarttech.smart_alarm_mobile.SharedData.alarms
import com.tomsksmarttech.smart_alarm_mobile.alarm.AlarmReceiver
import java.util.Calendar
import android.media.RingtoneManager
import android.net.Uri
import android.widget.Toast
import com.tomsksmarttech.smart_alarm_mobile.alarm.Alarm
import java.util.Date


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
        val currAlarm = alarms.value.find { it: Alarm ->
            it.id == id
        }
        if (currAlarm == null) return
        Log.d("ALARM", "HELP I WANT TO KILL MYSELF $id, ${currAlarm.getHours().toInt()}, ${currAlarm.getMinutes().toInt()} ")
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

        Log.d("AlarmDebug", "Текущее время: ${Date(now)}")
        Log.d("AlarmDebug", "Время будильника: ${Date(calendar.timeInMillis)}")

        val diffMillis = calendar.timeInMillis - now

        val diffHours = (diffMillis / (1000 * 60 * 60)).toInt()
        val diffMinutes = ((diffMillis / (1000 * 60)) % 60).toInt()

        val hrsRemain = String.format("%02d", diffHours)
        val minsRemain = String.format("%02d", diffMinutes)

        Log.d("AlarmDebug", "Разница времени: ${diffHours} часов ${diffMinutes} минут")
        val toast = Toast(appContext)
        toast.setText("Будильник сработает через $hrsRemain часов $minsRemain минут")
        toast.duration = Toast.LENGTH_SHORT
        toast.show()

        Log.d("TEST", "Alarm set for: ${calendar.time}")
        Log.d("TEST", "Alarm time: ${currAlarm.getHours()}, ${currAlarm.getMinutes()}")

        currAlarm.musicUri = SharedData.lastAudio?.uri.toString()
        val intent = Intent(appContext, AlarmReceiver::class.java).apply {
            action = "com.tomsksmarttech.ALARM_ACTION"
            putExtra("alarm_id", id.toString())
            if (currAlarm.musicUri.toString() == "null") {
                currAlarm.musicUri = getDefaultAlarmRingtoneUri().toString()
            }
            Log.d("TEST", "SENDING " + currAlarm.musicUri.toString())
            putExtra("RINGTONE_URI", currAlarm.musicUri)
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

    fun getDefaultAlarmRingtoneUri(): Uri {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        return if (uri != null) {
            uri
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }

}