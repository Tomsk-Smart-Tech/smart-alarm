package com.tomsksmarttech.smart_alarm_mobile

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.room.util.copy
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tomsksmarttech.smart_alarm_mobile.alarm.Alarm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


object SharedData {


    private val _loadMusicJob = MutableStateFlow<Job?>(null)
    val loadMusicJob: StateFlow<Job?> = _loadMusicJob


    private val _musicList = MutableStateFlow<List<Audio>>(emptyList())
    val musicList: StateFlow<List<Audio>> = _musicList

    var lastAudio: Audio? = null
    val alarms = MutableStateFlow(
        mutableListOf(
            Alarm(-1, "", false, label = "")
        )
    )

    fun addAlarm(newAlarm: Alarm) {
        val updatedList = alarms.value.toMutableList()
        updatedList.add(newAlarm)
        alarms.value = updatedList
    }

    fun removeAlarm(id: Int) {
        val updatedList = alarms.value.toMutableList()
        updatedList.removeIf { it.id == id }
        alarms.value = updatedList
    }

    var alreadyAddedAlarms = mutableListOf<Alarm>()

    fun updateAlarms() {
        val updatedList = alarms.value.toMutableList()
        alarms.value = updatedList
    }
    var currentAlarmIndex = alarms.value.size
    fun updateCurrAlarmIndex() {
        alarms.value.forEach{ it: Alarm ->
            if (it.id > currentAlarmIndex) currentAlarmIndex = it.id
        }
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
//        Log.d("LISTSIZE", musicList.value.size.toString())

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
            Log.d("ALARM", jsonString)
            return gson.fromJson(jsonString, type)
        } catch (e: Exception) {
            Log.e("Exception: ", e.toString())
            return null
        }
    }


    fun loadAlarms(context: Context): Collection<Alarm> {
        val gson = Gson()
        val sharedPreferences =
            context.getSharedPreferences("shared preferences", Context.MODE_PRIVATE)
        val jsonString = sharedPreferences.getString("alarm0", null) ?: return emptyList()
        val type = TypeToken.getParameterized(List::class.java, Alarm::class.java).type
        return gson.fromJson(jsonString, type)
    }

    fun saveAlarms(context: Context, alarms: Collection<Alarm>) {
        val gson = Gson()
        val sharedPreferences =
            context.getSharedPreferences("shared preferences", Context.MODE_PRIVATE)
        val jsonString = gson.toJson(alarms)
        sharedPreferences.edit().putString("alarm0", jsonString).apply()
    }
}