package com.tomsksmarttech.smart_alarm_mobile.playback

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomsksmarttech.smart_alarm_mobile.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackControlScreen(navigateBack: () -> Unit) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(context.getString(R.string.default_title)) }
    var artist by remember { mutableStateOf(context.getString(R.string.default_artist)) }
    var isLooping by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var isShuffle by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(text = context.getString(R.string.title_playback_control)) },
        navigationIcon = {
            IconButton(
                onClick = navigateBack,
                modifier = Modifier
                    .size(35.dp)
                    .clickable(onClick = navigateBack)
            ) {
                Icon(
                    painterResource(R.drawable.ic_back),
                    "Back",
                    modifier = Modifier.size(35.dp)
                )
            }
        },
        windowInsets = WindowInsets(top = 0.dp, bottom = 0.dp)
    )
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        )
        Image(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp)
                .clip(RoundedCornerShape(20.dp)),
            painter = painterResource(R.drawable.default_cover),
            contentDescription = "Default cover",
            contentScale = ContentScale.FillWidth
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )
        Text(
            title,
            modifier = Modifier.padding(start = 15.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 25.sp
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
        )
        Text(
            artist,
            modifier = Modifier.padding(start = 15.dp),
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            PlaybackControlButton(
                id = R.drawable.ic_shuffle,
                onClick = { isShuffle = !isShuffle },
                alpha = if (isShuffle) 1f else 0.5f
            )

            PlaybackControlButton(
                id = R.drawable.ic_previous,
                onClick = { TODO() },
            )

            PlaybackControlButton(
                id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                onClick = { isPlaying = !isPlaying },
            )

            PlaybackControlButton(
                id = R.drawable.ic_next,
                onClick = { TODO() },
            )

            PlaybackControlButton(
                id = R.drawable.ic_loop,
                onClick = { isLooping = !isLooping },
                alpha = if (isLooping) 1f else 0.5f
            )
        }
//            }
    }
}

@Composable
fun PlaybackControlButton(id: Int, onClick: () -> Unit, alpha: Float = 1f) {
    Icon(
        painterResource(id),
        "Control button",
        modifier = Modifier
            .size(35.dp)
            .clickable(onClick = onClick)
            .alpha(alpha)
    )
}

@Composable
@Preview(showBackground = true)
fun PlaybackControlScreenPreview() {
    PlaybackControlScreen({})
}