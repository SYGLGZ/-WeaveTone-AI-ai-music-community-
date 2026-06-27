package com.example.myfirstapp.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.myfirstapp.ui.theme.Surface3
import com.example.myfirstapp.ui.theme.TextPrimary
import com.example.myfirstapp.ui.theme.TextSecondary
import com.example.myfirstapp.ui.theme.TextTertiary
import com.example.myfirstapp.ui.viewmodel.HomeTracksState
import com.example.myfirstapp.ui.viewmodel.MusicViewModel
import com.example.myfirstapp.ui.viewmodel.PlaylistTracksState
import com.example.myfirstapp.ui.viewmodel.SearchState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSongsToPlaylistScreen(
    playlistId: Long,
    viewModel: MusicViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val discoverState by viewModel.discoverTracksState.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val playlistTracksState by viewModel.playlistTracksState.collectAsState()
    var query by remember { mutableStateOf("") }

    LaunchedEffect(playlistId) {
        viewModel.loadPlaylistTracks(playlistId.toInt())
        viewModel.loadDiscoverTracks()
    }

    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            delay(300)
            viewModel.search(query)
        }
    }

    val addedIds = when (val state = playlistTracksState) {
        is PlaylistTracksState.Success -> state.tracks.map { it.id }.toSet()
        else -> emptySet()
    }

    val tracks = if (query.isBlank()) {
        when (val state = discoverState) {
            is HomeTracksState.Success -> state.tracks
            else -> emptyList()
        }
    } else {
        when (val state = searchState) {
            is SearchState.Success -> state.result.tracks
            else -> emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加歌曲", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BlackBase),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(80) },
                    placeholder = { Text("搜索社区歌曲、作者或风格", color = TextTertiary) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextTertiary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentRose.copy(alpha = 0.5f),
                        unfocusedBorderColor = Divider,
                        cursorColor = AccentRose,
                        focusedContainerColor = Surface3,
                        unfocusedContainerColor = Surface3
                    )
                )
            }

            item {
                Text(
                    text = if (query.isBlank()) "从社区作品中添加" else "搜索结果",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "已在歌单里的歌曲会显示为“已添加”。",
                    color = TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            val loading = (query.isBlank() && discoverState is HomeTracksState.Loading) ||
                (query.isNotBlank() && searchState is SearchState.Loading)

            if (loading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentRose, modifier = Modifier.size(28.dp))
                    }
                }
            } else if (tracks.isEmpty()) {
                item { EmptyAddSongsCard(onRetry = { viewModel.loadDiscoverTracks() }) }
            } else {
                items(tracks, key = { it.id }) { track ->
                    AddableTrackRow(
                        track = track,
                        isAdded = track.id in addedIds,
                        onAdd = {
                            viewModel.addTrackToPlaylist(playlistId.toInt(), track.id)
                        }
                    )
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun AddableTrackRow(
    track: TrackDto,
    isAdded: Boolean,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        border = BorderStroke(0.5.dp, Divider)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(AccentRose.copy(alpha = 0.16f), RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PlaylistAdd, contentDescription = null, tint = AccentRose)
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = track.title.ifBlank { "未命名作品" },
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist ?: track.username.ifBlank { "未知创作者" },
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isAdded) {
                TextButton(onClick = {}, enabled = false) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("已添加")
                }
            } else {
                Button(
                    onClick = onAdd,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRose),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("添加", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun EmptyAddSongsCard(onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Surface2),
        border = BorderStroke(0.5.dp, Divider)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("暂时没有可添加的社区歌曲", color = TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("可以先去发现页发布或搜索其他关键词。", color = TextTertiary)
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onRetry) {
                Text("重新加载", color = AccentRose)
            }
        }
    }
}
