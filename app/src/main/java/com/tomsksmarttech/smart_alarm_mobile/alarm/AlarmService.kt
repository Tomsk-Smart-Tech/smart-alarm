package com.tomsksmarttech.smart_alarm_mobile.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tomsksmarttech.smart_alarm_mobile.SharedData
import com.tomsksmarttech.smart_alarm_mobile.R

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var alarmId: Int? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel(this)
    }

    private fun showAlarmActivity() {
        val intent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("is_haptic", SharedData.alarms.value.find { it.id == alarmId }?.isHaptic == true)
        }
        startActivity(intent)
    }


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        when (intent.action) {
            "STOP_ALARM" -> {
                val alarms = SharedData.loadAlarms(this)
                Log.d("ALARM", "Loaded alarms: $alarms")
                stopAlarm()
            }
            else -> {
                SharedData.saveAlarms(this, SharedData.alarms.value)
                val isPhoneLocked = intent.getStringExtra("is_phone_locked")
                alarmId = intent.getStringExtra("alarm_id") as Int?
                if (isPhoneLocked == "true") {
                    wakeScreen()
                    showAlarmActivity()
                }

                val ringtoneUri = intent.getStringExtra("ringtone_uri")
                if (!ringtoneUri.isNullOrEmpty()) {
                    playRingtone(ringtoneUri)
                }

                val notificationIntent = Intent(this, AlarmActivity::class.java).apply {
                    flags
                }
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE // FLAG_IMMUTABLE для Android 12+
                )
                val notification = NotificationCompat.Builder(this, "ALARM_CHANNEL")
                    .setContentTitle(resources.getString(R.string.app_name))
                    .setContentText("Будильник сработал")
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
//                    .addAction(R.drawable.ic_alarm, getString(R.string.btn_cancel),
//                        );
                    .build()

                startForeground(1, notification)
                wakeScreen()
            }
        }
        return START_STICKY
    }


    private fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        stopSelf()

    }

    private fun createNotificationChannel(context: Context) {
        val channelId = "ALARM_CHANNEL"
        val channel = NotificationChannel(
            channelId,
            "Будильник",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Канал для будильника"
            enableLights(true)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 1000, 500, 1000)
        }
        val notificationManager =
            context.getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)
    }


    private fun playRingtone(uriString: String) {
        try {
            val uri = Uri.parse(uriString)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes( // Here is the important part
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM) // usage - alarm
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(this@AlarmService, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Ошибка воспроизведения мелодии: ${e.message}")
        }
    }

    private fun wakeScreen() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
            "AlarmService:WakeLock"
        )
        wakeLock.acquire(10 * 60 * 100L)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        wakeLock?.release()
    }
    override fun onBind(intent: Intent): IBinder? = null
}
