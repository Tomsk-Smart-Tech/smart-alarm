package com.tomsksmarttech.smart_alarm_mobile

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


@Composable
fun MusicScreen() {
    MusicTopAppBar()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicTopAppBar() {
    var musicList by remember { mutableStateOf(listOf<Audio?>()) }
    LaunchedEffect(Unit) {
        musicList = SharedData.musicList
        Log.d("CURRENTMUSIC", musicList.toString())
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    var result by remember { (mutableStateOf<Uri?>(null)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it ->
        result = it
        result?.let { uri ->
            try {
                // Используем MediaMetadataRetriever для получения информации об аудиофайле
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)

                // Извлекаем метаданные
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                val name = cursor?.use { cur ->
                    val nameIndex = cur.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cur.moveToFirst()) {
                        cur.getString(nameIndex)
                    } else null
                }
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toIntOrNull() ?: 0

                // Освобождаем ресурсы MediaMetadataRetriever
                retriever.release()

                // Добавляем аудио в список
                musicList = musicList + Audio(name ?: "Без названия - Неизвестен", duration)
            } catch (e: Exception) {
                Log.e("Error", "Failed to retrieve metadata: ${e.message}", e)
            }
        }
    }



    Scaffold (
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.heightIn(max = 56.dp),
                windowInsets = WindowInsets(
                    top = 0.dp,
                    bottom = 0.dp
                ),
                title = { Text(stringResource(R.string.title_music), fontWeight = FontWeight.Bold) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.Search, contentDescription = "Поиск музыки")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {launcher.launch(arrayOf("audio/*"))}) {
                Icon(Icons.Filled.Add, contentDescription = "Добаление музыки из устройства")
            }
        }
    ) { innerPadding ->
        LazyColumn (
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) {
            items(musicList) { i ->
                Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    i?.let { Text(it.name, textAlign = TextAlign.Left, modifier = Modifier.width(300.dp), overflow = TextOverflow.Ellipsis, maxLines = 1) }
                    Text(i?.duration.toString(), textAlign = TextAlign.Right)
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


@Composable
@Preview
fun MusicScreenPreview() {
    MusicScreen()
}