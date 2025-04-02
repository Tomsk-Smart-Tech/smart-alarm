package com.tomsksmarttech.smart_alarm_mobile

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tomsksmarttech.smart_alarm_mobile.alarm.Alarm
import com.tomsksmarttech.smart_alarm_mobile.alarm.AlarmViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

const val SENSORS_TOPIC = "mqtt/sensors"
const val TEST_TOPIC = "mqtt/test"
const val EVENTS_TOPIC = "mqtt/events"
const val ALARMS_TOPIC = "mqtt/alarms"
const val CHECK_TOPIC = "mqtt/check"

    // todo избавиться от SharedData
object SharedData {

    private val _loadMusicJob = MutableStateFlow<Job?>(null)
    val loadMusicJob: StateFlow<Job?> = _loadMusicJob


    private val _musicList = MutableStateFlow<List<Audio>>(emptyList())
    val musicList: StateFlow<List<Audio>> = _musicList

    var humidity = mutableDoubleStateOf(0.0)
    var temperature = mutableDoubleStateOf(0.0)
    var voc = mutableDoubleStateOf(0.0)
    val currentAlarmId: StateFlow<Int> = _currentAlarmId

    var lastAudio: Audio? = null
    val alarms = MutableStateFlow(
        mutableListOf<Alarm?>()

    )

    var currentAuthorizationCode : String? = null
    lateinit var codeVerifier : String


    fun saveAlarms(hc : HttpController, cs: CoroutineScope) {
        sortAlarms()
        cs.launch{
            hc.saveAlarms()
        }
    }

    fun setAlarmId(id: Int) {
        _currentAlarmId.value = id
    }
        //        val alarms = MutableStateFlow(
//        mutableListOf<Alarm?>()
//
//        )

    //music functions
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




    // shared preferences functions
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
        val jsonString = sharedPreferences.getString("alarm_data", null) ?: return emptyList()
        val type = TypeToken.getParameterized(List::class.java, Alarm::class.java).type
        return gson.fromJson(jsonString, type)
    }

    fun saveAlarms(context: Context, alarms: Collection<Alarm>) {
        val gson = Gson()
        val sharedPreferences =
            context.getSharedPreferences("shared preferences", Context.MODE_PRIVATE)
        val jsonString = gson.toJson(alarms)
        sharedPreferences.edit().putString("alarm_data", jsonString).apply()
    }

}