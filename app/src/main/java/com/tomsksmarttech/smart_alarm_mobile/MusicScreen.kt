package com.tomsksmarttech.smart_alarm_mobile

import SingleAlarmManager
import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tomsksmarttech.smart_alarm_mobile.SharedData.lastAudio
import com.tomsksmarttech.smart_alarm_mobile.alarm.AlarmRepository

import com.tomsksmarttech.smart_alarm_mobile.alarm.DialClockDialog


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun MusicScreen() {
    MusicTopAppBar()
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicTopAppBar() {
    val musicList by SharedData.musicList.collectAsState()
    val musicJob by SharedData.loadMusicJob.collectAsState()
    val context = LocalContext.current
    val permission = android.Manifest.permission.READ_MEDIA_AUDIO
    var isPermissionGranted by remember {
        mutableStateOf(context.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED)
    }
    var isMusicListLoaded by remember {
        mutableStateOf(musicJob?.isCompleted)
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && musicList.isEmpty()) {
            SharedData.startLoadMusicJob(context)
            isPermissionGranted = true
        }
    }
    LaunchedEffect(Unit) {
        if (context.checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            launcher.launch(permission)
        }
        isMusicListLoaded = musicJob?.isCompleted
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    var isSearchClicked by remember {
        mutableStateOf(false)
    }
    var searchedText by remember {
        mutableStateOf("")
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.heightIn(max = 56.dp),
                windowInsets = WindowInsets(
                    top = 0.dp,
                    bottom = 0.dp
                ),
                title = {
                    Text(
                        stringResource(R.string.title_music),
                        fontWeight = FontWeight.Bold,
                        overflow = TextOverflow.Clip,
                        maxLines = 1
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    AnimatedVisibility(visible = isSearchClicked.not()) {
                        Row {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "",
                                Modifier
                                    .clickable {
                                        isSearchClicked = true
                                    }
                                    .padding(horizontal = 20.dp)
                            )
                        }
                    }
                    AnimatedVisibility(visible = isSearchClicked) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { isSearchClicked = false },
                                modifier = Modifier.padding(start = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = ""
                                )
                            }
                            OutlinedTextField(
                                value = searchedText,
                                onValueChange = { searchedText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp),
                                shape = RoundedCornerShape(15.dp),
                                placeholder = { Text(text = "Поиск музыки") }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (!isPermissionGranted) {
            Text(
                "Разрешение на доступ к музыке не предоставлено",
                modifier = Modifier.padding(paddingValues = innerPadding)
            )
        } else {
            MusicLibrary(innerPadding, context, musicList, isSearchClicked, searchedText)
        }

    }
}

data class Audio(
    val name: String,
    val duration: Int,
    val uri: Uri
)

@Composable
fun MusicLibrary(
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    context: android.content.Context,
    musicList: List<Audio>,
    isSearchClicked: Boolean = false,
    searchedText: String = ""
) {
    val coroutineScope = rememberCoroutineScope()
    val httpController = HttpController(context)
    var mediaPlayer by remember { mutableStateOf(MediaPlayer()) }
    var isPlaying by remember { mutableStateOf(true) }
    var nowPlaying by remember { mutableStateOf<Uri?>(null) }
    var showDialog by remember {
        mutableStateOf(false)
    }
    val currentAlarmId by AlarmRepository.currentAlarmId.collectAsState()
    mediaPlayer.setOnCompletionListener {
        isPlaying = false
    }
    if (SharedData.loadMusicJob.collectAsState().value?.isCompleted == true && musicList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                "Музыка не найдена", modifier = Modifier
                    .padding(innerPadding),
                fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                fontWeight = MaterialTheme.typography.headlineLarge.fontWeight
            )
        }
    } else if (SharedData.loadMusicJob.collectAsState().value?.isCompleted == false) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text("Загрузка библиотеки...", modifier = Modifier.padding(innerPadding), fontSize = MaterialTheme.typography.headlineLarge.fontSize, fontWeight = MaterialTheme.typography.headlineLarge.fontWeight)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            items(
                if (isSearchClicked) {
                    musicList.filter {
                        it.name.lowercase().contains(searchedText.lowercase())
                    }
                } else {
                    musicList.sortedBy {
                        it.name
                    }
                }, key = { audio ->
                    audio.uri
                }) { audio ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 15.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(5.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                audio.name,
                                textAlign = TextAlign.Left,
                                modifier = Modifier
                                    .widthIn(min = 100.dp, max = 200.dp)
                                    .basicMarquee(),
//                            overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                fontWeight = FontWeight.Bold,
                                fontSize = MaterialTheme.typography.bodyLarge.fontSize
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            val mins = audio.duration.div(1000 * 60)
                            val secs = (audio.duration.div(1000).rem(60))
                            Text(
                                String.format("%02d:%02d", mins, secs),
                                textAlign = TextAlign.Right,
                                fontWeight = FontWeight.Bold,
                                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                                maxLines = 1,
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(5.dp)
                        ) {
                            Button(onClick = {
                                Log.d("IS_PLAYING", mediaPlayer.isPlaying.toString())
                                if (nowPlaying != audio.uri) {
                                    if (mediaPlayer.isPlaying) {
                                        mediaPlayer.stop()
                                        mediaPlayer.reset()
                                    }
                                    nowPlaying = null
                                    mediaPlayer = MediaPlayer.create(context, audio.uri)
                                    mediaPlayer.start()
                                    nowPlaying = audio.uri
                                    isPlaying = true
                                } else if (nowPlaying == audio.uri) {
                                    nowPlaying = null
                                    mediaPlayer.stop()
                                    isPlaying = false
                                }
                            }) {
                                if (nowPlaying == audio.uri && isPlaying) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_pause),
                                        contentDescription = "Play/Pause"
                                    )

                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "Play/Pause"
                                    )
                                }
//                                    Text("Воспроизвести")
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(onClick = { showDialog = true }) {
                                Icon(
                                    imageVector = Icons.Filled.AddCircle,
                                    contentDescription = "Create new alarm"
                                )
//                            TODO()
                                if (currentAlarmId == -1) {
                                    Text(
                                        stringResource(R.string.create_new_alarm),
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Text(
                                        "Выбрать этот трек",
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
                lastAudio = audio
            }
        }
    }
    if (showDialog && currentAlarmId == 0) {
        DialClockDialog(
            null,
            onConfirm = { timePickerState ->
                Log.d(
                    "ALARM",
                    "Creating new with id from music ${AlarmRepository.alarms.value.last()}"
                )
                Log.d("ALARM", "and list is music ${AlarmRepository.alarms.value}")
                SingleAlarmManager.setAlarm(AlarmRepository.alarms.value.last().id)
                AlarmRepository.saveAlarms(httpController, coroutineScope)
                showDialog = false
            },
            onDismiss = {
                showDialog = false
            },
            viewModel
        )
        val alarmsState by AlarmRepository.alarms.collectAsState()
        val lastAlarm = alarmsState.lastOrNull()
        Log.d("TEST", "set auio to ${lastAudio?.uri} : $lastAlarm")
    } else
        if (showDialog) {
            viewModel.cancelAlarm(currentAlarmId)
            viewModel.setAlarm(currentAlarmId)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
@Preview
fun MusicScreenPreview() {
    MusicScreen()
}