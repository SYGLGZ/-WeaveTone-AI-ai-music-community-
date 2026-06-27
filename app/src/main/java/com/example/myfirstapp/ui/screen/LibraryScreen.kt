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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myfirstapp.common.Constants
import com.example.myfirstapp.data.local.SongEntity
import com.example.myfirstapp.ui.components.PageTitle
import com.example.myfirstapp.ui.components.SectionHeader
import com.example.myfirstapp.ui.components.TrackRow
import com.example.myfirstapp.ui.components.durationText
import com.example.myfirstapp.ui.theme.AccentRose
import com.example.myfirstapp.ui.theme.BlackBase
import com.example.myfirstapp.ui.theme.Divider
import com.example.myfirstapp.ui.theme.Surface1
import com.example.myfirstapp.ui.theme.Surface2
import com.example.myfirstapp.ui.theme.Surface3
import com.example.myfirstapp.ui.theme.TextPrimary
import com.example.myfirstapp.ui.theme.TextSecondary
import com.example.myfirstapp.ui.theme.TextTertiary
import com.example.myfirstapp.ui.viewmodel.LikedTracksState
import com.example.myfirstapp.ui.viewmodel.MusicViewModel
import com.example.myfirstapp.ui.viewmodel.MyTracksState
import com.example.myfirstapp.ui.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    onPlaySong: (Long) -> Unit,
    onNavigateToPlaylists: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAI: () -> Unit,
    onNavigateToWorks: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val songs by viewModel.songs.collectAsState(initial = emptyList())
    val aiSongs by viewModel.aiSongs.collectAsState(initial = emptyList())
    val myPlaylists by viewModel.myPlaylists.collectAsState(initial = emptyList())
    val myTracksState by viewModel.myTracksState.collectAsState()
    val likedTracksState by viewModel.likedTracksState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var likedSongs by remember { mutableStateOf(setOf<Long>()) }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isScanning) {
        if (!uiState.isScanning) isRefreshing = false
    }

    LaunchedEffect(Unit) {
        viewModel.loadMyTracks()
        viewModel.loadLikedTracks()
        viewModel.loadMyPlaylists()
    }

    val publishedWorksCount = when (val state = myTracksState) {
        is MyTracksState.Success -> state.tracks.size
        else -> aiSongs.size
    }
    val likedCount = when (val state = likedTracksState) {
        is LikedTracksState.Success -> state.tracks.size
        else -> likedSongs.size
    }

    val validSongs = remember(songs, searchQuery) {
        songs.filter { it.duration >= Constants.MIN_AUDIO_DURATION_MS }
            .filter {
                searchQuery.isBlank() ||
                    it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true)
            }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            viewModel.scanLocalMusic()
        },
        modifier = Modifier
            .fillMaxSize()
            .background(BlackBase)
    ) {
        Column(Modifier.fillMaxSize()) {
            if (uiState.isScanning) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = AccentRose,
                    trackColor = Color.Transparent
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    PageTitle(
                        title = "音乐库",
                        subtitle = "管理本地音乐、收藏和你的 AI 作品"
                    )
                }

                item {
                    LibrarySearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onClear = { searchQuery = "" }
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LibraryCategoryCard("本地", "${validSongs.size} 首", Icons.Filled.Download, Modifier.weight(1f)) {
                            viewModel.scanLocalMusic()
                        }
                        LibraryCategoryCard("收藏", "${likedCount} 首", Icons.Filled.Favorite, Modifier.weight(1f), onNavigateToFavorites)
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LibraryCategoryCard("歌单", "${myPlaylists.size} 个", Icons.Filled.PlaylistPlay, Modifier.weight(1f), onNavigateToPlaylists)
                        LibraryCategoryCard("AI 作品", "${publishedWorksCount} 首", Icons.Filled.AutoAwesome, Modifier.weight(1f), onNavigateToWorks)
                    }
                }

                item {
                    SectionHeader(
                        title = "本地音乐 (${validSongs.size})",
                        actionText = if (uiState.isScanning) "扫描中" else "扫描",
                        onAction = { if (!uiState.isScanning) viewModel.scanLocalMusic() }
                    )
                }

                if (validSongs.isEmpty() && !uiState.isScanning) {
                    item {
                        EmptyLibraryCard(onScanClick = { viewModel.scanLocalMusic() })
                    }
                } else {
                    items(validSongs, key = { it.id }) { song ->
                        LibrarySongRow(
                            song = song,
                            isLiked = likedSongs.contains(song.id),
                            onClick = {
                                playerViewModel.playSong(song)
                                onPlaySong(song.id)
                            },
                            onToggleLike = {
                                viewModel.toggleLike(song.id.toInt())
                                likedSongs = if (likedSongs.contains(song.id)) {
                                    likedSongs - song.id
                                } else {
                                    likedSongs + song.id
                                }
                            }
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "提示：右上角设置入口已移到“我的 → 应用设置”，这里专注音乐管理。",
                        color = TextTertiary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun LibrarySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("搜索本地歌曲或艺术家...", color = TextTertiary) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "搜索", tint = TextTertiary) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Filled.Clear, contentDescription = "清除", tint = TextTertiary)
                }
            }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Divider,
            focusedBorderColor = AccentRose.copy(alpha = 0.5f),
            unfocusedTextColor = TextPrimary,
            focusedTextColor = TextPrimary,
            cursorColor = AccentRose,
            focusedContainerColor = Surface3,
            unfocusedContainerColor = Surface3
        )
    )
}

@Composable
private fun LibraryCategoryCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        border = BorderStroke(0.5.dp, Divider)
    ) {
        Column(Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(AccentRose.copy(alpha = 0.14f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = AccentRose, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text(value, color = TextTertiary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LibrarySongRow(
    song: SongEntity,
    isLiked: Boolean,
    onClick: () -> Unit,
    onToggleLike: () -> Unit
) {
    TrackRow(
        title = song.title,
        subtitle = song.artist,
        durationText = song.durationText(),
        onClick = onClick,
        trailing = {
            IconButton(onClick = onToggleLike, modifier = Modifier.size(40.dp)) {
                Icon(
                    if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "收藏",
                    tint = if (isLiked) AccentRose else TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    )
}

@Composable
private fun EmptyLibraryCard(onScanClick: () -> Unit) {
    Card(
        onClick = onScanClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        border = BorderStroke(0.5.dp, Divider)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .background(Surface2, RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, tint = AccentRose, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("还没有扫描到本地音乐", color = TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("点击这里扫描手机里的音频文件", color = TextTertiary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
