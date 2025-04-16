package com.tomsksmarttech.smart_alarm_mobile.alarm
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tomsksmarttech.smart_alarm_mobile.CHECK_TOPIC
import com.tomsksmarttech.smart_alarm_mobile.MainActivity
import com.tomsksmarttech.smart_alarm_mobile.R
import com.tomsksmarttech.smart_alarm_mobile.mqtt.MqttService

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ALARM_TRIGGERED = "com.tomsksmarttech.ALARM_TRIGGERED"
        const val NOTIFICATION_CLICKED = "com.tomsksmarttech.NOTIFICATION_CLICKED"
    }
    private val mqttService = MqttService
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("RECEIVER", "recived: $intent")
        if (intent.action == ALARM_TRIGGERED) {

            mqttService.init(context)
            Log.d("MQTT_SRV", "init from receiver")
            mqttService.subscribedTopics.add(CHECK_TOPIC)

            handleAlarmTrigger(context, intent)
        } else if (intent.action == NOTIFICATION_CLICKED) {
            Log.d("AlarmReceiver", "NOTIFICATION_CLICKED")
            MediaManager.stopMediaPlayback()
            wakeLock?.release()
            wakeLock = null
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.cancel(0)
            notificationManager.cancel(1)
        } else if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Восстанавливаем будильники из SharedPreferences
            AlarmRepository.loadAlarms(context)
        }
    }

    private fun handleAlarmTrigger(context: Context, intent: Intent) {
        val mqttCheckService = MqttCheckService(context)
        mqttCheckService.checkMqtt { isSuccess ->
            // callback будет вызван когда:
            // - пришло сообщение (isSuccess = true)
            // - или прошло 10 секунд (isSuccess = false)
            if (isSuccess) {
                AlarmRepository.alarms.value.last().isEnabled = false
                Log.d("ALARM", "${AlarmRepository.alarms.value.last().time} was set off")
                return@checkMqtt
            } else {
                val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("notification_clicked", true)  // Добавляем специальный флаг
                }
                AlarmRepository.loadAlarms(context)
                AlarmRepository.setPlayingAlarmId(AlarmRepository.alarms.value.last().id)

                val fullScreenPendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    fullScreenIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                // Создаем действие для уведомления
                val actionIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("notification_action_clicked", true)  // Другой флаг для действия
                }
                val actionPendingIntent = PendingIntent.getActivity(
                    context,
                    1,  // Другой requestCode
                    actionIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val notification = NotificationCompat.Builder(context, "ALARM_CHANNEL").apply {
                    setContentTitle(context.getString(R.string.app_name))
                    setContentText(context.getString(R.string.alarm_ring_text))
                    setSmallIcon(R.drawable.ic_alarm)
                    setPriority(NotificationCompat.PRIORITY_HIGH)
                    setCategory(NotificationCompat.CATEGORY_ALARM)
                    setFullScreenIntent(fullScreenPendingIntent, true)
                    setContentIntent(fullScreenPendingIntent)  // Обработчик клика по телу уведомления
                    setAutoCancel(true)
                    setOngoing(true)
                    addAction(
                        R.drawable.ic_shuffle,
                        context.getString(R.string.alarm_stop_text),
                        actionPendingIntent
                    )
                    setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                }.build()
                // Показываем уведомление ПЕРЕД запуском сервиса
                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager.notify(1, notification)

                val ringtoneUri = intent.getStringExtra("ringtone_uri") ?: ""
                Log.d("RECEIVER", "uri: ${ringtoneUri}")
//                playRingtone(ringtoneUri, context)
                MediaManager.playRingtone(ringtoneUri, context)
                val isPhoneLocked = intent.getStringExtra("is_phone_locked")
                if (isPhoneLocked == "true") {
                    wakeScreen(context)
                }
                context.startActivity(fullScreenIntent)
            }
        }
    }

    private fun wakeScreen(context: Context) {
        val powerManager = context.getSystemService(POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
            "AlarmService:WakeLock"
        )
        wakeLock.acquire(10 * 60 * 100L)
    }
}
