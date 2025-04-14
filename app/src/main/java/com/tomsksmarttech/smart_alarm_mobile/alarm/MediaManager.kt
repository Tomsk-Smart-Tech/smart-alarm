package com.tomsksmarttech.smart_alarm_mobile.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.RingtoneManager.getDefaultUri
import android.net.Uri
import android.util.Log

object MediaManager {
    private var mediaPlayer: MediaPlayer? = null

    fun playRingtone(uriString: String, context: Context) {
        try {
            val defaultUri = getDefaultUri(RingtoneManager.TYPE_ALARM)
            val uri = Uri.parse(uriString)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                try {
                    Log.d("URI", uri.toString())
                    setDataSource(context, if (uri != null) uri else defaultUri)
                } catch (e: Exception) {
                    setDataSource(context, defaultUri)
                }
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Ошибка воспроизведения мелодии: ${e.message}")
        }
        mediaPlayer?.setOnCompletionListener {
            stopMediaPlayback()
        }
    }

    fun stopMediaPlayback() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

}