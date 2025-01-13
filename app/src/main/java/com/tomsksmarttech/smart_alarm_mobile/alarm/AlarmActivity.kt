package com.tomsksmarttech.smart_alarm_mobile.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomsksmarttech.smart_alarm_mobile.MainActivity
import com.tomsksmarttech.smart_alarm_mobile.Screens
import com.tomsksmarttech.smart_alarm_mobile.SharedData

class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        keyguardManager.requestDismissKeyguard(this, null)
        setContent {
            AlarmNotif(
                { stopAlarmService(this) }
            )
        }
    }

    override fun onBackPressed() {
        stopAlarmService(this)
        super.onBackPressed()
    }

    fun stopAlarmService(context: Context) {
        val stopIntent = Intent(context, AlarmService::class.java).apply {
            action = "STOP_ALARM"
        }
        context.startService(stopIntent)
        finish()
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("TARGET_ROUTE", Screens.Alarm.route)
        }
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
        Log.d("ALARM", "stopAlarmService")
    }

}
//@PreviewParameter()
@Composable
fun AlarmNotif(
    onStopAlarm: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Будильник",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                onStopAlarm()
                Log.d("ALARM", "compose onClick")
            }) {
                Text(text = "Остановить")
            }

        }
    }
}

