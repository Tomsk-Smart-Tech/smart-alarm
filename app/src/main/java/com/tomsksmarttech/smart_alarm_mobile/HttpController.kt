package com.tomsksmarttech.smart_alarm_mobile

import SingleAlarmManager.getDefaultAlarmRingtoneUri
import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.tomsksmarttech.smart_alarm_mobile.SharedData.saveListAsJson
import com.tomsksmarttech.smart_alarm_mobile.alarm.Alarm
import com.tomsksmarttech.smart_alarm_mobile.alarm.AlarmRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.ConnectionPool
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink
import okio.source
import okio.use
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.collections.contains
import kotlin.collections.removeAll

class HttpController(val context: Context) {

    fun sendFile (ctx: Context, uri: String, serverUrl: String, mediaType: MediaType? = null) {
        val parsedUri = uri.toUri()
        Log.d("Upload", "URI: $uri, Scheme: ${parsedUri.scheme}")

        if (parsedUri.scheme == "content" && uri.startsWith("content://settings/")) {
            Log.e("Upload", "Ошибка: системный URI не поддерживается ($uri)")
            return
        }

        val contentResolver = ctx.contentResolver
        try {
            val inputStream = contentResolver.openInputStream(parsedUri)
            val fileName = DocumentFile.fromSingleUri(ctx, parsedUri)?.name

            if (inputStream == null || fileName == null) {
                Log.e("Upload", "Ошибка: не удалось открыть файл или определить имя")
                return
            }
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("filename", fileName)
                .addFormDataPart("file", fileName, object : RequestBody() {
                    override fun contentType() = mediaType
                    override fun writeTo(sink: BufferedSink) {
                        inputStream.source().use { source -> sink.writeAll(source) }
                    }
                })
                .build()

            val request = Request.Builder()
                .url(serverUrl)
                .post(requestBody)
                .build()

            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
                .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("Upload", "Ошибка загрузки: ${e}")
                }

                override fun onResponse(call: Call, response: Response) {
                    val resp = response.body?.string()?.split("айл сохранён: ")?.map{ it.trim()}
                    if (resp != null) {
                        if (resp.size > 1) SharedData.lastSongPath.value = resp[1]
                    }
                    Log.d(
                        "Uploading file",
                        "Файл $fileName успешно загружен: ${resp}"
                    )
                }

            })
            Log.d("Upload", "Файл $fileName успешно подготовлен")

        } catch (e: SecurityException) {
            Log.e("Upload", "Ошибка доступа к файлу: ${e.message}")
        } catch (e: FileNotFoundException) {
            Log.e("Upload", "Файл не найден: ${e.message}")
        } catch (e: Exception) {
            Log.e("Upload", "Неизвестная ошибка: ${e.message}")
        }
    }

    private suspend fun saveAlarm(alarm: Alarm) {
        coroutineScope {
            launch(Dispatchers.IO) {
                if (alarm.musicUri == null) alarm.musicUri =
                    getDefaultAlarmRingtoneUri().toString();

                sendFile(
                    context,
                    alarm.musicUri.toString(),
                    context.getString(R.string.remote_host),
                    "audio/*".toMediaTypeOrNull()
                )
                delay(500)
            }
        }
        if (!AlarmRepository.alarms.value.isEmpty()) {
            Log.d("ALARM", "saving" + AlarmRepository.alarms.value.toList().toString())
            saveListAsJson(
                context = context,
                AlarmRepository.alarms.value.toList(),
                key = "alarm_data",
            )
        }
    }

    suspend fun saveAlarms() {
        coroutineScope {
            Log.d("ALARM", "before filter" + AlarmRepository.alarms.value.toList().toString())

            val newList = AlarmRepository.alarms.value.toMutableList()
            newList.removeAll { it: Alarm? ->
                AlarmRepository.alreadyAddedAlarms.contains(it)
            }
            AlarmRepository.updateAlarms(newList)

            launch(Dispatchers.IO) {
                for (alarm in AlarmRepository.alarms.value) {
                    saveAlarm(alarm)
                }
            }
            if (!AlarmRepository.alarms.value.isEmpty()) {
                Log.d("ALARM", "saving" + AlarmRepository.alarms.value.toList().toString())
                saveListAsJson(
                    context = context,
                    AlarmRepository.alarms.value.toList(),
                    key = "alarm_data"
                )
            }
        }
    }
}