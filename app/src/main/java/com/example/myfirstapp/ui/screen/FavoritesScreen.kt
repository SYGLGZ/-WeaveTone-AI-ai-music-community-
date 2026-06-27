package com.example.myfirstapp.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myfirstapp.data.remote.dto.TrackDto
import com.example.myfirstapp.ui.components.SectionHeader
import com.example.myfirstapp.ui.components.TrackRow
import com.example.myfirstapp.ui.theme.AccentRose
import com.example.myfirstapp.ui.theme.BlackBase
import com.example.myfirstapp.ui.theme.Divider
import com.example.myfirstapp.ui.theme.Surface1
import com.example.myfirstapp.ui.theme.TextPrimary
import com.example.myfirstapp.ui.theme.TextTertiary
import com.example.myfirstapp.ui.viewmodel.LikedTracksState
import com.example.myfirstapp.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: MusicViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onTrackClick: (Int) -> Unit = {}
) {
    val likedState by viewModel.likedTracksState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadLikedTracks()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的收藏", color = TextPrimary, fontWeight = FontWeight.Bold) },
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
                SectionHeader(title = "收藏的社区作品")
            }

            when (val state = likedState) {
                is LikedTracksState.Idle, is LikedTracksState.Loading -> {
                    item { LoadingBlock() }
                }

                is LikedTracksState.Error -> {
                    item {
                        MessageCard(
                            message = state.message,
                            actionText = "重新加载",
                            onAction = { viewModel.loadLikedTracks() }
                        )
                    }
                }

                is LikedTracksState.Success -> {
                    if (state.tracks.isEmpty()) {
                        item {
                            MessageCard(
                                message = "还没有收藏作品。去发现页给喜欢的音乐点个赞，它就会出现在这里。",
                                actionText = "刷新收藏",
                                onAction = { viewModel.loadLikedTracks() }
                            )
                        }
                    } else {
                        items(state.tracks, key = { "liked_${it.id}" }) { track ->
                            FavoriteTrackRow(
                                track = track,
                                onClick = { onTrackClick(track.id) },
                                onUnlike = { viewModel.toggleLike(track.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteTrackRow(
    track: TrackDto,
    onClick: () -> Unit,
    onUnlike: () -> Unit
) {
    TrackRow(
        title = track.title.ifBlank { "未命名作品" },
        subtitle = track.artist ?: track.username.ifBlank { "未知创作者" },
        isAiGenerated = track.isAiGenerated,
        onClick = onClick,
        trailing = {
            IconButton(onClick = onUnlike) {
                Icon(Icons.Filled.Favorite, contentDescription = "取消收藏", tint = AccentRose)
            }
            Icon(Icons.Filled.PlayArrow, contentDescription = "进入讨论", tint = TextTertiary)
        }
    )
}

@Composable
private fun LoadingBlock() {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AccentRose, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun MessageCard(
    message: String,
    actionText: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        border = BorderStroke(0.5.dp, Divider)
    ) {
        Box(Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(message, color = TextTertiary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onAction) {
                    Text(actionText, color = AccentRose)
                }
            }
        }
    }
}
