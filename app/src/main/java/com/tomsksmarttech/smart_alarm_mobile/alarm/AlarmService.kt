package com.tomsksmarttech.smart_alarm_mobile.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val alarmLabel = intent.getStringExtra("ALARM_LABEL") ?: "Будильник"
        val ringtoneUri = intent.getStringExtra("RINGTONE_URI")

        if (!ringtoneUri.isNullOrEmpty()) {
            playRingtone(ringtoneUri)
        }

        wakeScreen()
        val notification = NotificationCompat.Builder(this, "ALARM_CHANNEL")
            .setContentTitle(alarmLabel)
            .setContentText("Будильник сработал")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        startForeground(1, notification)

        return START_STICKY
    }

    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            "ALARM_CHANNEL",
            "Будильник",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Канал для будильника"
        }
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)
    }

    private fun playRingtone(uriString: String) {
        try {
            val uri = Uri.parse(uriString)
            mediaPlayer = MediaPlayer().apply {
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
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
            "AlarmService:WakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        wakeLock?.release()
    }

    override fun onBind(intent: Intent): IBinder? = null
}
