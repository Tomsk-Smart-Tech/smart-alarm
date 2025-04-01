package com.tomsksmarttech.smart_alarm_mobile.alarm

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.tomsksmarttech.smart_alarm_mobile.SharedData

const val ALARM_TRIGGERED = "com.tomsksmarttech.ALARM_TRIGGERED";
const val NOTIFICATION_CLICKED = "com.tomsksmarttech.NOTIFICATION_CLICKED";

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
             ALARM_TRIGGERED -> handleAlarmTrigger(context, intent)
            NOTIFICATION_CLICKED -> handleNotificationClick(context, intent)
            else -> Log.d("AlarmReceiver", "Неизвестный intent: ${intent.action}")
        }
    }

    private fun handleAlarmTrigger(context: Context, intent: Intent) {
        val mqttCheckService = MqttCheckService()

        mqttCheckService.checkMqtt { isSuccess ->
            // callback будет вызван когда:
            // - пришло сообщение (isSuccess = true)
            // - или прошло 10 секунд (isSuccess = false)

            if (isSuccess) {
                //если устройство ответило
                Log.d("MQTT_CHECK", "Сообщение получено!")

                AlarmRepository.alarms.value.last().isEnabled = false
                Log.d("ALARM", "${AlarmRepository.alarms.value.last().time} was set off")
                return@checkMqtt
            } else {
                //если устройство не ответило
                val handler = Handler(Looper.getMainLooper());
                handler.post(Runnable() {
                    run { Toast.makeText(context,
                        "Кумкват не ответил. Звоню сам.",
                        Toast.LENGTH_SHORT).show();
                        Log.d("MQTT_CHECK", "устройство не ответило")}
                });
                val isPhoneLocked = (context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceLocked
                val ringtoneUri = intent.getStringExtra("RINGTONE_URI") ?: ""
                val alarm = intent.getStringExtra("alarm_id") ?: ""
                Log.d("AlarmReceiver", "Будильник сработал: $ringtoneUri")

                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    putExtra("ringtone_uri", ringtoneUri)
                    putExtra("alarm_id", alarm)
                    putExtra("is_phone_locked", isPhoneLocked.toString())
                }
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }


    }

    private fun handleNotificationClick(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "Уведомление нажато!")
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            action = "STOP_ALARM"
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}

