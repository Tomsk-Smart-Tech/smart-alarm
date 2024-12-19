package com.tomsksmarttech.smart_alarm_mobile

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.collection.mutableFloatListOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.tomsksmarttech.smart_alarm_mobile.alarm.Alarm
import androidx.core.net.toFile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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

    private val _loadMusicJob = MutableStateFlow<Job?>(null)
    val loadMusicJob: StateFlow<Job?> = _loadMusicJob


    private val _musicList = MutableStateFlow<List<Audio>>(emptyList())
    val musicList: StateFlow<List<Audio>> = _musicList

    var lastAudio: Audio? = null
    var alarms = MutableStateFlow<MutableList<Alarm>>(
//        mutableListOf(
//        Alarm(id = 1, time = "07:00", isEnabled = false, label = "Подъём"),
//        Alarm(id = 2, time = "15:01", isEnabled = false, label = "Работа")
//    )
    mutableListOf(Alarm(-1, "", false, label = ""))
    )
    fun addAlarm(newAlarm: Alarm) {
        alarms.value.add(newAlarm)
    }

    var currentAlarmIndex = alarms.value.size
    fun updateCurrAlarmIndex() {
        currentAlarmIndex = alarms.value.size
    }

    fun startLoadMusicJob(context: Context) {
        val scope = CoroutineScope(Dispatchers.IO)
        _loadMusicJob.value = scope.launch {
            loadMusicLibrary(context)
        }
    }

    fun addMusic(newMusic: Audio) {
        _musicList.value += newMusic
    }


    fun loadMusicLibrary(ctx: Context) {
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
//                Log.d("ITERATION", "ITERATION")
                val name = cursor.getString(nameColumn)
                val duration = cursor.getInt(durationColumn)
                _musicList.value += (Audio(name, duration, uri))
            }
        }
        Log.d("LISTSIZE", musicList.value.size.toString())

//        val musicList = arrayListOf<com.tomsksmarttech.smart_alarm_mobile.Audio>()
    }

    fun checkPermission(
        context: Context,
        permission: String
    ): Boolean {
        val permissionCheckResult = ContextCompat.checkSelfPermission(context, permission)
        if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
            return true
        } else {
            // Request a permission
            return false
        }
    }



    fun <T> saveListAsJson(context: Context, list: Collection<T>, key: String) {
        val gson = Gson()
        val sharedPreferences =
            context.getSharedPreferences("shared preferences", Context.MODE_PRIVATE)
        val jsonString = gson.toJson(list)
        sharedPreferences.edit().clear().putString(key, jsonString).apply()
    }

    fun <T> loadListFromFile(
        context: Context,
        key: String,
        clazz: Class<T>
    ): Collection<T>? {
        val gson = Gson()
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