package com.tomsksmarttech.smart_alarm_mobile

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.tomsksmarttech.smart_alarm_mobile.alarm.Alarm
import androidx.core.net.toFile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import java.io.File
import java.util.concurrent.TimeUnit

object SharedData {
    val settingsList = arrayListOf(
        "Smart alarm",
        "Подключение к устройству",
        "Передать музыку на устройство",
        "Импортировать из календаря",
        "Справка",
        "Об устройстве",
        "A",
        "B",
        "C",
        "D",
        "E",
        "F"
    )
    var loadMusicJob: Job? = null
    var musicList: List<Audio> = listOf()
    var alarms = mutableStateListOf(
        Alarm(id = 1, time = "07:00", isEnabled = false, label = "Подъём"),
        Alarm(id = 2, time = "15:01", isEnabled = false, label = "Работа")
    )
    fun addAlarm(newAlarm: Alarm) {
        alarms.add(newAlarm)
    }

    var currentAlarmIndex = alarms.size

    fun loadMusicLibrary(ctx: Context): List<Audio> {
        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(1.toString())
        val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"
        val projection = arrayOf(
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Audio.Media._ID
        )
        val query = ctx.contentResolver.query(
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
            projection,
            selection,
            selectionArgs,
            sortOrder
        )
        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val uri =
                    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                Log.d("ITERATION", "ITERATION")
                val name = cursor.getString(nameColumn)
                val duration = cursor.getInt(durationColumn)
                musicList = musicList.plus(Audio(name, duration, uri))
            }
        }
        Log.d("LISTSIZE", musicList.size.toString())

//        val musicList = arrayListOf<com.tomsksmarttech.smart_alarm_mobile.Audio>()
        return musicList
    }



    fun <T> saveListAsJson(context: Context, gson: Gson, list: Collection<T>, key: String) {
        val sharedPreferences =
            context.getSharedPreferences("shared preferences", Context.MODE_PRIVATE)
        val jsonString = gson.toJson(list)
        sharedPreferences.edit().clear().putString(key, jsonString).apply()
    }

    fun <T> loadListFromFile(
        context: Context,
        gson: Gson,
        key: String,
        clazz: Class<T>
    ): Collection<T>? {
        try {
            val sharedPreferences =
                context.getSharedPreferences("shared preferences", Context.MODE_PRIVATE)
            val jsonString = sharedPreferences.getString(key, null) ?: return null
            val type = TypeToken.getParameterized(List::class.java, clazz).type
            return gson.fromJson(jsonString, type)
        } catch (e: Exception) {
            Log.e("Exception: ", e.toString())
            return null
        }
    }
}