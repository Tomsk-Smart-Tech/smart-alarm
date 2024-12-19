package com.tomsksmarttech.smart_alarm_mobile.home

import android.content.Context
import android.widget.Toast
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum

class SettingsFunctions {
    fun connectToDevice(context: Context) {
        val toast = Toast(context)
        toast.setText("Не удалось подключиться к устройству")
        toast.duration = Toast.LENGTH_SHORT
        toast.show()
    }

    fun about(context: Context) {
        val toast = Toast(context)
        toast.setText(LoremIpsum().values.first())
        toast.duration = Toast.LENGTH_LONG
        toast.show()
    }
}