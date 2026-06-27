package com.example.myfirstapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myfirstapp.ui.viewmodel.MusicViewModel
import com.example.myfirstapp.ui.viewmodel.PlayerViewModel
import com.example.myfirstapp.util.formatDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val AccentRose = Color(0xFFFA4D6A)
private val AccentGold = Color(0xFFE8A850)
private val BlackBase = Color(0xFF000000)
private val Surface1 = Color(0xFF161616)
private val Surface3 = Color(0xFF2C2C2E)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFEBEBF5)
private val TextTertiary = Color(0xFFEBEBF5)
private val Divider = Color(0x38FFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    songId: Long,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    musicViewModel: MusicViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by playerViewModel.playerState.collectAsState()
    val isPlaying = state.isPlaying
    val currentSong = state.currentSong
    val totalDuration = state.duration.coerceAtLeast(0L)

    var currentSliderPosition by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(500)
            if (!isDragging) {
                val dur = totalDuration
                if (dur > 0L) {
                    currentSliderPosition = (state.currentPosition.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                }
            }
        }
    }

    val songTitle = currentSong?.title ?: "未选择歌曲"
    val songArtist = currentSong?.artist ?: "未知艺人"
    val isShuffled = state.isShuffled
    val repeatMode = state.repeatMode

    val formatCurrentPosition = formatDuration(
        if (isDragging) (currentSliderPosition * totalDuration).toLong() else state.currentPosition
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackBase)
    ) {
        TopAppBar(
            title = {
                Text(
                    "正在播放",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = TextPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        Column(
            modifier = Modifier
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    AccentRose.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Card(
                    modifier = Modifier.size(260.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface1)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            Icons.Filled.MusicNote,
                            contentDescription = "音乐",
                            modifier = Modifier.size(64.dp),
                            tint = TextSecondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                songTitle,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1
            )

            Spacer(Modifier.height(6.dp))

            Text(
                songArtist,
                fontSize = 15.sp,
                color = TextSecondary
            )

            Spacer(Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Slider(
                    value = currentSliderPosition,
                    onValueChange = { currentSliderPosition = it; isDragging = true },
                    onValueChangeFinished = {
                        isDragging = false
                        playerViewModel.seekTo((currentSliderPosition * totalDuration).toLong())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = AccentRose,
                        activeTrackColor = AccentGold,
                        inactiveTrackColor = Divider
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatCurrentPosition,
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                    Text(
                        formatDuration(totalDuration),
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { playerViewModel.toggleShuffle() }) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = "随机播放",
                        tint = if (isShuffled) AccentRose else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = { playerViewModel.skipToPrevious() }) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "上一首",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = { playerViewModel.togglePlayPause() },
                    modifier = Modifier
                        .size(64.dp)
                        .background(AccentRose, CircleShape)
                ) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = { playerViewModel.skipToNext() }) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "下一首",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(onClick = { playerViewModel.toggleRepeat() }) {
                    Icon(
                        if (repeatMode == 1) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = "重复",
                        tint = if (repeatMode > 0) AccentRose else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Row(
                    modifier = Modifier
                        .background(Surface3, RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable {
                            musicViewModel.toggleLike((currentSong?.id ?: songId).toInt())
                            isLiked = !isLiked
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "收藏",
                        tint = if (isLiked) AccentRose else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        "收藏",
                        fontSize = 13.sp,
                        color = if (isLiked) AccentRose else TextSecondary
                    )
                }

                Row(
                    modifier = Modifier
                        .background(Surface3, RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.PlaylistAdd,
                        contentDescription = "添加到播放列表",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        "添加到播放列表",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
