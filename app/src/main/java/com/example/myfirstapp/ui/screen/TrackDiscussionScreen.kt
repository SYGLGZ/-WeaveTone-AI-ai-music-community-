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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.AlertDialog
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
import com.example.myfirstapp.data.remote.dto.CommentDto
import com.example.myfirstapp.data.remote.dto.TrackDto
import com.example.myfirstapp.ui.components.GradientCover
import com.example.myfirstapp.ui.theme.AccentRose
import com.example.myfirstapp.ui.theme.BlackBase
import com.example.myfirstapp.ui.theme.Divider
import com.example.myfirstapp.ui.theme.Surface1
import com.example.myfirstapp.ui.theme.Surface2
import com.example.myfirstapp.ui.theme.Surface3
import com.example.myfirstapp.ui.theme.TextPrimary
import com.example.myfirstapp.ui.theme.TextSecondary
import com.example.myfirstapp.ui.theme.TextTertiary
import com.example.myfirstapp.ui.viewmodel.CommentsState
import com.example.myfirstapp.ui.viewmodel.MusicViewModel
import com.example.myfirstapp.ui.viewmodel.MyPlaylistsLoadState
import com.example.myfirstapp.ui.viewmodel.PlayerViewModel
import com.example.myfirstapp.ui.viewmodel.TrackDetailState
import com.example.myfirstapp.util.trackStreamUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDiscussionScreen(
    trackId: Int,
    viewModel: MusicViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val detailState by viewModel.trackDetailState.collectAsState()
    val commentsState by viewModel.commentsState.collectAsState()
    val playlists by viewModel.myPlaylists.collectAsState()
    val playlistState by viewModel.myPlaylistsLoadState.collectAsState()
    var commentText by remember { mutableStateOf("") }
    var showPlaylistDialog by remember { mutableStateOf(false) }

    LaunchedEffect(trackId) {
        viewModel.loadTrackDetail(trackId)
        viewModel.loadComments(trackId)
        viewModel.loadMyPlaylists()
    }

    LaunchedEffect(showPlaylistDialog) {
        if (showPlaylistDialog) viewModel.loadMyPlaylists()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("作品讨论", color = TextPrimary, fontWeight = FontWeight.Bold) },
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
            when (val state = detailState) {
                is TrackDetailState.Idle, is TrackDetailState.Loading -> item { LoadingBlock() }
                is TrackDetailState.Error -> item { MessageCard(state.message) }
                is TrackDetailState.Success -> {
                    item {
                        TrackDetailCard(
                            track = state.track,
                            onPlay = {
                                playerViewModel.playUrl(
                                    trackStreamUrl(state.track.id),
                                    state.track.title,
                                    state.track.artist ?: state.track.username.ifBlank { "未知创作者" }
                                )
                            },
                            onLike = { viewModel.toggleLike(state.track.id) },
                            onAddToPlaylist = { showPlaylistDialog = true }
                        )
                    }
                }
            }

            item {
                Text("评论区", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it.take(500) },
                        placeholder = { Text("说点什么...", color = TextTertiary) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedTextColor = TextPrimary,
                            focusedTextColor = TextPrimary,
                            focusedBorderColor = AccentRose.copy(alpha = 0.5f),
                            unfocusedBorderColor = Divider,
                            cursorColor = AccentRose,
                            unfocusedContainerColor = Surface3,
                            focusedContainerColor = Surface3
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Button(
                        onClick = {
                            viewModel.postComment(trackId, commentText)
                            commentText = ""
                        },
                        enabled = commentText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRose),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("发布", color = Color.White)
                    }
                }
            }

            when (val state = commentsState) {
                is CommentsState.Idle, is CommentsState.Loading -> item { LoadingBlock() }
                is CommentsState.Error -> item { MessageCard(state.message) }
                is CommentsState.Success -> {
                    if (state.comments.isEmpty()) {
                        item { MessageCard("还没有评论，来当第一个讨论的人。") }
                    } else {
                        items(state.comments, key = { "comment_${it.id}" }) { comment ->
                            CommentRow(comment)
                        }
                    }
                }
            }
        }
    }

    if (showPlaylistDialog) {
        AddToPlaylistDialog(
            playlists = playlists,
            state = playlistState,
            trackId = trackId,
            onCreatePlaylist = { name -> viewModel.createPlaylistAndAddTrack(name, trackId) },
            onAddToPlaylist = { playlistId ->
                viewModel.addTrackToPlaylist(playlistId, trackId)
                showPlaylistDialog = false
            },
            onRefresh = { viewModel.loadMyPlaylists() },
            onDismiss = { showPlaylistDialog = false }
        )
    }
}

@Composable
private fun AddToPlaylistDialog(
    playlists: List<com.example.myfirstapp.data.remote.dto.PlaylistDto>,
    state: MyPlaylistsLoadState,
    trackId: Int,
    onCreatePlaylist: (String) -> Unit,
    onAddToPlaylist: (Int) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    var newPlaylistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("加入歌单") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state is MyPlaylistsLoadState.Loading && playlists.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = AccentRose, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("正在加载歌单...")
                    }
                }

                if (playlists.isNotEmpty()) {
                    playlists.forEach { playlist ->
                        Text(
                            text = playlist.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAddToPlaylist(playlist.id) }
                                .padding(vertical = 10.dp),
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else if (state is MyPlaylistsLoadState.Error) {
                    Text("没有拿到歌单：${state.message}", color = TextTertiary)
                    TextButton(onClick = onRefresh) {
                        Text("重新加载", color = AccentRose)
                    }
                } else if (state is MyPlaylistsLoadState.Success) {
                    Text("你还没有后端歌单，可以在这里直接新建。", color = TextTertiary)
                }

                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it.take(40) },
                    placeholder = { Text("新建歌单名称", color = TextTertiary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreatePlaylist(newPlaylistName.trim())
                            newPlaylistName = ""
                        }
                    },
                    enabled = newPlaylistName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRose),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("新建歌单", color = Color.White)
                }
                Text("提示：新建歌单会自动把当前作品 #$trackId 加进去。", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun TrackDetailCard(
    track: TrackDto,
    onPlay: () -> Unit,
    onLike: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        border = BorderStroke(0.5.dp, Divider)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GradientCover(modifier = Modifier.size(86.dp), isAiGenerated = track.isAiGenerated)
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title.ifBlank { "未命名作品" }, color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist ?: track.username.ifBlank { "未知创作者" }, color = TextSecondary)
                    if (track.isAiGenerated) Text("AI 生成作品", color = AccentRose, style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(track.aiPrompt ?: track.description ?: "这个作品还没有描述。", color = TextTertiary, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onPlay, colors = ButtonDefaults.buttonColors(containerColor = AccentRose)) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.size(6.dp))
                    Text("播放", color = Color.White)
                }
                Button(onClick = onLike, colors = ButtonDefaults.buttonColors(containerColor = Surface2)) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = AccentRose)
                    Spacer(Modifier.size(6.dp))
                    Text("${track.likeCount}", color = TextPrimary)
                }
                Button(onClick = onAddToPlaylist, colors = ButtonDefaults.buttonColors(containerColor = Surface2)) {
                    Icon(Icons.Filled.PlaylistAdd, contentDescription = null, tint = TextSecondary)
                    Spacer(Modifier.size(6.dp))
                    Text("加入歌单", color = TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun CommentRow(comment: CommentDto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(AccentRose.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(comment.username.take(1), color = AccentRose, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(comment.username, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(comment.content, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun LoadingBlock() {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AccentRose, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun MessageCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Surface2),
        border = BorderStroke(0.5.dp, Divider)
    ) {
        Text(message, color = TextTertiary, modifier = Modifier.padding(16.dp))
    }
}
