package com.example.myfirstapp.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myfirstapp.data.remote.dto.TrackDto
import com.example.myfirstapp.ui.theme.AccentRose
import com.example.myfirstapp.ui.theme.BlackBase
import com.example.myfirstapp.ui.theme.Divider
import com.example.myfirstapp.ui.theme.Surface1
import com.example.myfirstapp.ui.theme.Surface2
import com.example.myfirstapp.ui.theme.TextPrimary
import com.example.myfirstapp.ui.theme.TextSecondary
import com.example.myfirstapp.ui.theme.TextTertiary
import com.example.myfirstapp.ui.viewmodel.MusicViewModel
import com.example.myfirstapp.ui.viewmodel.PlayerViewModel
import com.example.myfirstapp.ui.viewmodel.PlaylistTracksState
import com.example.myfirstapp.util.trackStreamUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    viewModel: MusicViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onPlaySong: (Long) -> Unit,
    onAddSongs: (Long) -> Unit
) {
    val tracksState by viewModel.playlistTracksState.collectAsState()
    var removedTrackIds by remember { mutableStateOf(setOf<Int>()) }

    LaunchedEffect(playlistId) {
        removedTrackIds = emptySet()
        viewModel.loadPlaylistTracks(playlistId.toInt())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("歌单详情", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { onAddSongs(playlistId) }) {
                        Icon(Icons.Filled.Add, contentDescription = "添加歌曲", tint = AccentRose)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BlackBase)
        ) {
            when (val state = tracksState) {
                is PlaylistTracksState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentRose)
                    }
                }

                is PlaylistTracksState.Error -> {
                    MessagePanel(
                        title = "歌单加载失败",
                        message = state.message,
                        actionText = "重新加载",
                        onAction = { viewModel.loadPlaylistTracks(playlistId.toInt()) }
                    )
                }

                is PlaylistTracksState.Success -> {
                    val visibleTracks = state.tracks.filter { it.id !in removedTrackIds }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            PlaylistDetailHeader(
                                count = visibleTracks.size,
                                onAddSongs = { onAddSongs(playlistId) },
                                onPlayAll = {
                                    visibleTracks.firstOrNull()?.let { track ->
                                        playerViewModel.playUrl(
                                            trackStreamUrl(track.id),
                                            track.title,
                                            track.artist ?: track.username.ifBlank { "未知创作者" }
                                        )
                                        onPlaySong(track.id.toLong())
                                    }
                                }
                            )
                        }

                        if (visibleTracks.isEmpty()) {
                            item {
                                EmptyPlaylistDetailCard(onAddSongs = { onAddSongs(playlistId) })
                            }
                        } else {
                            items(visibleTracks, key = { it.id }) { track ->
                                PlaylistTrackRow(
                                    track = track,
                                    onClick = {
                                        playerViewModel.playUrl(
                                            trackStreamUrl(track.id),
                                            track.title,
                                            track.artist ?: track.username.ifBlank { "未知创作者" }
                                        )
                                        onPlaySong(track.id.toLong())
                                    },
                                    onRemove = {
                                        viewModel.removeTrackFromPlaylist(playlistId.toInt(), track.id)
                                        removedTrackIds = removedTrackIds + track.id
                                    }
                                )
                            }
                        }

                        item { Spacer(Modifier.height(20.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetailHeader(
    count: Int,
    onAddSongs: () -> Unit,
    onPlayAll: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        border = BorderStroke(0.5.dp, Divider)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(AccentRose.copy(alpha = 0.16f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PlaylistAdd, contentDescription = null, tint = AccentRose)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("我的歌单", color = TextPrimary, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                    Text("共 $count 首歌曲", color = TextTertiary, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onPlayAll,
                    enabled = count > 0,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRose),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("播放全部", color = Color.White)
                }
                OutlinedButton(
                    onClick = onAddSongs,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = AccentRose, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("添加歌曲", color = AccentRose)
                }
            }
        }
    }
}

@Composable
private fun EmptyPlaylistDetailCard(onAddSongs: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Surface2),
        border = BorderStroke(0.5.dp, Divider)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("这个歌单还没有歌曲", color = TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("从社区作品里挑几首加入，就能在这里连续播放。", color = TextTertiary)
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onAddSongs,
                colors = ButtonDefaults.buttonColors(containerColor = AccentRose),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("去添加歌曲", color = Color.White)
            }
        }
    }
}

@Composable
private fun PlaylistTrackRow(
    track: TrackDto,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        border = BorderStroke(0.5.dp, Divider)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(AccentRose.copy(alpha = 0.16f), RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = AccentRose)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title.ifBlank { "未命名作品" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist ?: track.username.ifBlank { "未知创作者" },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "移出歌单",
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Icon(Icons.Filled.PlayArrow, contentDescription = "播放", tint = TextTertiary)
        }
    }
}

@Composable
private fun MessagePanel(
    title: String,
    message: String,
    actionText: String,
    onAction: () -> Unit
) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Surface2),
            border = BorderStroke(0.5.dp, Divider)
        ) {
            Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(message, color = TextTertiary)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onAction, colors = ButtonDefaults.buttonColors(containerColor = AccentRose)) {
                    Text(actionText, color = Color.White)
                }
            }
        }
    }
}
