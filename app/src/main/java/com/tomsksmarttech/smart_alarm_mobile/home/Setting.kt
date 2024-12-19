package com.tomsksmarttech.smart_alarm_mobile.home

import android.content.Context

data class Setting(
    val name: String,
    val func: (context : Context) -> Unit
)