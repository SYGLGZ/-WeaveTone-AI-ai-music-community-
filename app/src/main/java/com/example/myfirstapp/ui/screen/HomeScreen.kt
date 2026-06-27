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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myfirstapp.data.remote.dto.TrackDto
import com.example.myfirstapp.ui.components.CommunityTrackCard
import com.example.myfirstapp.ui.components.GradientCover
import com.example.myfirstapp.ui.components.PageTitle
import com.example.myfirstapp.ui.components.SectionHeader
import com.example.myfirstapp.ui.components.TrackRow
import com.example.myfirstapp.ui.theme.AccentGold
import com.example.myfirstapp.ui.theme.AccentRose
import com.example.myfirstapp.ui.theme.BlackBase
import com.example.myfirstapp.ui.theme.Divider
import com.example.myfirstapp.ui.theme.Surface1
import com.example.myfirstapp.ui.theme.Surface2
import com.example.myfirstapp.ui.theme.TextPrimary
import com.example.myfirstapp.ui.theme.TextSecondary
import com.example.myfirstapp.ui.theme.TextTertiary
import com.example.myfirstapp.ui.viewmodel.HomeTracksState
import com.example.myfirstapp.ui.viewmodel.MusicViewModel
import com.example.myfirstapp.ui.viewmodel.PlayerViewModel
import com.example.myfirstapp.util.trackStreamUrl

@Composable
fun HomeScreen(
    viewModel: MusicViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    onPlaySong: (Long) -> Unit,
    onNavigateToAI: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onTrackDiscussion: (Int) -> Unit = {}
) {
    val hotState by viewModel.hotTracksState.collectAsState()
    val discoverState by viewModel.discoverTracksState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadHotTracks()
        viewModel.loadDiscoverTracks()
    }

    fun playTrack(track: TrackDto) {
        playerViewModel.playUrl(
            trackStreamUrl(track.id),
            track.title.ifBlank { "未命名作品" },
            track.artist?.ifBlank { null } ?: track.username.ifBlank { "未知创作者" }
        )
        onPlaySong(track.id.toLong())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackBase)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PageTitle(
                    title = "发现",
                    subtitle = "听见社区里刚刚诞生的 AI 音乐"
                )
            }

            item {
                DiscoveryHero(
                    onNavigateToAI = onNavigateToAI,
                    onNavigateToSearch = onNavigateToSearch
                )
            }

            item { SectionHeader(title = "社区动态") }
            when (val state = discoverState) {
                is HomeTracksState.Loading -> item { LoadingCard() }
                is HomeTracksState.Error -> item { ErrorCardInline(state.message) }
                is HomeTracksState.Success -> {
                    if (state.tracks.isEmpty()) {
                        item { EmptyCard("社区还没有作品，先去创作一首发布吧。") }
                    } else {
                        itemsIndexed(state.tracks.take(4), key = { _, track -> "community_${track.id}" }) { _, track ->
                            CommunityTrackCard(
                                track = track,
                                onOpen = { onTrackDiscussion(track.id) },
                                onPlay = { playTrack(track) },
                                onLike = { viewModel.toggleLike(track.id) }
                            )
                        }
                    }
                }
            }

            item { SectionHeader(title = "热门榜单") }
            when (val state = hotState) {
                is HomeTracksState.Loading -> item { LoadingCard() }
                is HomeTracksState.Error -> item { ErrorCardInline(state.message) }
                is HomeTracksState.Success -> {
                    if (state.tracks.isEmpty()) {
                        item { EmptyCard("暂无热门作品。") }
                    } else {
                        itemsIndexed(state.tracks.take(5), key = { _, track -> "rank_${track.id}" }) { index, track ->
                            RankingRow(index = index + 1, track = track, onPlay = { playTrack(track) })
                        }
                    }
                }
            }

            item { SectionHeader(title = "最新发布") }
            when (val state = discoverState) {
                is HomeTracksState.Loading -> item { LoadingCard() }
                is HomeTracksState.Error -> item { ErrorCardInline(state.message) }
                is HomeTracksState.Success -> {
                    itemsIndexed(state.tracks.drop(4).take(6), key = { _, track -> "latest_${track.id}" }) { _, track ->
                        TrackRow(
                            title = track.title,
                            subtitle = track.artist ?: track.username.ifBlank { "未知创作者" },
                            isAiGenerated = track.isAiGenerated,
                            onClick = { playTrack(track) },
                            trailing = {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "播放", tint = AccentRose)
                            }
                        )
                    }
                    if (state.tracks.size <= 4) {
                        item { EmptyCard("更多作品发布后会出现在这里。") }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoveryHero(
    onNavigateToAI: () -> Unit,
    onNavigateToSearch: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .clickable(onClick = onNavigateToSearch)
                .background(Surface2)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, contentDescription = "搜索", tint = TextTertiary)
            Spacer(Modifier.width(10.dp))
            Text("搜索歌曲、创作者或风格...", color = TextTertiary, style = MaterialTheme.typography.bodyLarge)
        }

        Card(
            onClick = onNavigateToAI,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Surface1),
            border = BorderStroke(0.5.dp, Divider)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(AccentRose.copy(alpha = 0.32f), Surface1, Surface1)
                        )
                    )
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(listOf(AccentRose, AccentGold))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("今天想造点什么声音？", color = TextPrimary, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text("8bit Boss 战、抽象鬼畜、二次元角色曲，都可以先试一首。", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun RankingRow(index: Int, track: TrackDto, onPlay: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onPlay)
            .background(Surface1)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = index.toString().padStart(2, '0'),
            color = if (index <= 3) AccentRose else TextTertiary,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.width(34.dp)
        )
        GradientCover(modifier = Modifier.size(52.dp), isAiGenerated = track.isAiGenerated)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title.ifBlank { "未命名作品" }, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${track.artist ?: track.username.ifBlank { "未知创作者" }} · ${track.playCount} 播放",
                color = TextTertiary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(AccentRose.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = AccentRose, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
private fun LoadingCard() {
    Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AccentRose, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun ErrorCardInline(message: String) {
    EmptyCard("加载失败：$message")
}

@Composable
private fun EmptyCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        border = BorderStroke(0.5.dp, Divider)
    ) {
        Text(
            text = text,
            color = TextTertiary,
            modifier = Modifier.padding(18.dp)
        )
    }
}
