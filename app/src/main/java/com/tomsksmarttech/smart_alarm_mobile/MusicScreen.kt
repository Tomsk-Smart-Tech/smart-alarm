package com.tomsksmarttech.smart_alarm_mobile

import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
import android.provider.MediaStore.Audio
import android.util.Log
import android.widget.Space
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
//import androidx.room.util.query
import java.io.File
import java.util.concurrent.TimeUnit


@Composable
fun MusicScreen() {
    MusicTopAppBar()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicTopAppBar() {
    val musicList = importMusicLibrary(LocalContext.current)
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold (
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
//                modifier = Modifier.heightIn(max = 56.dp),
                windowInsets = WindowInsets(
                    top = 0.dp,
                    bottom = 0.dp
                ),
                title = { Text(stringResource(R.string.title_music), fontWeight = FontWeight.Bold) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {TODO("Функция для кнопки поиска")}) {
                Icon(Icons.Filled.Search, contentDescription = "Поиск музыки")
            }
        }
    ) { innerPadding ->
        LazyColumn (
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) {
            items(count = musicList.size) { i ->
                Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(musicList[i].name, textAlign = TextAlign.Left, modifier = Modifier.width(300.dp), overflow = TextOverflow.Ellipsis, maxLines = 1)
                    Text(musicList[i].duration.toString(), textAlign = TextAlign.Right)
//                    Spacer(modifier = Modifier.fillMaxWidth().height(40.dp))
                }
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth().height(1.dp))
            }
        }
    }
}

data class Audio(
    val name: String,
    val duration: Int
)

fun importMusicLibrary(context: Context): ArrayList<com.tomsksmarttech.smart_alarm_mobile.Audio> {
    val musicList = arrayListOf<com.tomsksmarttech.smart_alarm_mobile.Audio>()
    val selection = "${Audio.Media.DURATION} >= ?"
    val selectionArgs = arrayOf(TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS).toString())
    val sortOrder = "${Audio.Media.DISPLAY_NAME} ASC"
    val projection = arrayOf(
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DURATION
    )
    val query = context.contentResolver.query(Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL), projection, selection, selectionArgs, sortOrder)
    query?.use { cursor ->
        val nameColumn = cursor.getColumnIndexOrThrow(Audio.Media.DISPLAY_NAME)
        val durationColumn = cursor.getColumnIndexOrThrow(Audio.Media.DURATION)

        while (cursor.moveToNext()) {
            Log.d("ITERATION", "ITERATION")
            val name = cursor.getString(nameColumn)
            val duration = cursor.getInt(durationColumn)
            musicList += Audio(name, duration)
        }
    }
    Log.d("LISTSIZE", musicList.size.toString())
    return musicList
}

@Composable
@Preview
fun MusicScreenPreview() {
    MusicScreen()
}