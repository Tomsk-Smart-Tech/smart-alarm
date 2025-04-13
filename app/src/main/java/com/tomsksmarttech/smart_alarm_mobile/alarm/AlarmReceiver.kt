package com.tomsksmarttech.smart_alarm_mobile.alarm

import android.R.attr.action
import android.app.ForegroundServiceStartNotAllowedException
import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.RingtoneManager.getDefaultUri
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.tomsksmarttech.smart_alarm_mobile.MainActivity
import com.tomsksmarttech.smart_alarm_mobile.R
import com.tomsksmarttech.smart_alarm_mobile.alarm.AlarmForegroundService

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ALARM_TRIGGERED = "com.tomsksmarttech.ALARM_TRIGGERED"
        const val NOTIFICATION_CLICKED = "com.tomsksmarttech.NOTIFICATION_CLICKED"
    }
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ALARM_TRIGGERED) {
            handleAlarmTrigger(context, intent)
        } else if (intent.action == NOTIFICATION_CLICKED) {
            Log.d("AlarmReceiver", "NOTIFICATION_CLICKED")
            MediaManager.stopMediaPlayback()
            wakeLock?.release()
            wakeLock = null
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.cancel(0)
            notificationManager.cancel(1)
        }
    }

    private fun handleAlarmTrigger(context: Context, intent: Intent) {
        val mqttCheckService = MqttCheckService()
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
