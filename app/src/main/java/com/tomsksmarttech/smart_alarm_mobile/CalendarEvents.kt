package com.tomsksmarttech.smart_alarm_mobile

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.icu.util.Calendar
import android.net.Uri
import android.provider.CalendarContract
import android.text.format.DateUtils
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import java.util.Date

class CalendarEvents {
    @SuppressLint("LongLogTag")
    data class CalendarEvent(
        val id: Long,
        val title: String?,
        val description: String?,
        val location: String?,
        val startTime: Long?,
        val endTime: Long?,
        val allDay: Boolean,
        val calendarDisplayName: String?,
        val organizer: String?,
        val rrule: String? // Recurrence rule
    )

    fun parseCalendarEvents(context: Context, fromDate: Long? = null, toDate: Long? = null): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context as MainActivity, arrayOf(Manifest.permission.READ_CALENDAR), 1)
            return events // Return empty list if no permission
        }

        val contentResolver: ContentResolver = context.contentResolver
        val eventsUri: Uri = CalendarContract.Events.CONTENT_URI

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.CALENDAR_DISPLAY_NAME,
            CalendarContract.Events.ORGANIZER,
            CalendarContract.Events.RRULE
            // Add other fields you need here (e.g., CalendarContract.Events.AVAILABILITY, CalendarContract.Events.RDATE)
        )

        val selection: String?
        val selectionArgs: Array<String>?

        if (fromDate != null && toDate != null) {
            selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTEND} <= ?"
            selectionArgs = arrayOf(fromDate.toString(), toDate.toString())
        } else if (fromDate != null) {
            selection = "${CalendarContract.Events.DTSTART} >= ?"
            selectionArgs = arrayOf(fromDate.toString())
        } else if (toDate != null) {
            selection = "${CalendarContract.Events.DTEND} <= ?"
            selectionArgs = arrayOf(toDate.toString())
        } else {
            selection = null
            selectionArgs = null
        }



        val cursor: Cursor? = contentResolver.query(eventsUri, projection, selection, selectionArgs, null)

        cursor?.use {
            while (it.moveToNext()) {
                val event = CalendarEvent(
                    id = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events._ID)),
                    title = it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.TITLE)),
                    description = it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)),
                    location = it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION)),
                    startTime = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)),
                    endTime = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events.DTEND)),
                    allDay = it.getInt(it.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)) == 1, // 1 for true, 0 for false
                    calendarDisplayName = it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_DISPLAY_NAME)),
                    organizer = it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.ORGANIZER)),
                    rrule = it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.RRULE))
                )
                events.add(event)
            }
        }
        return events
    }

    fun convertCalendarEventsToJSON(events: List<CalendarEvent>): String {
        val gson = Gson()
        Log.d("JSON", gson.toJson(events))
        return gson.toJson(events)
    }

}