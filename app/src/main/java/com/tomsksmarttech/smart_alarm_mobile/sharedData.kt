package com.tomsksmarttech.smart_alarm_mobile

import android.content.Context
import android.provider.MediaStore
import android.util.Log
import java.util.concurrent.TimeUnit

object sharedData {

    var musicList : List<Audio> = listOf()

    fun loadMusicLibrary(ctx:Context): List<Audio> {
//        val musicList = arrayListOf<com.tomsksmarttech.smart_alarm_mobile.Audio>()
        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS).toString())
        val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"
        val projection = arrayOf(
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION
        )
        val query = ctx.contentResolver.query(MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL), projection, selection, selectionArgs, sortOrder)
        query?.use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                Log.d("ITERATION", "ITERATION")
                val name = cursor.getString(nameColumn)
                val duration = cursor.getInt(durationColumn)
                musicList = musicList.plus(Audio(name, duration))
            }
        }
        Log.d("LISTSIZE", musicList.size.toString())
        return musicList
    }

    fun saveLibrary(ctx:Context) {

    }

}