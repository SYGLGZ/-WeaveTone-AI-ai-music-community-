package com.example.myfirstapp.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myfirstapp.R
import com.example.myfirstapp.data.local.PlaylistEntity
import com.example.myfirstapp.data.remote.dto.PlaylistDto
import com.example.myfirstapp.ui.theme.AccentGradientEnd
import com.example.myfirstapp.ui.theme.AccentRose
import com.example.myfirstapp.ui.theme.BlackBase
import com.example.myfirstapp.ui.theme.Divider
import com.example.myfirstapp.ui.theme.Surface1
import com.example.myfirstapp.ui.theme.TextPrimary
import com.example.myfirstapp.ui.theme.TextSecondary
import com.example.myfirstapp.ui.theme.TextTertiary
import com.example.myfirstapp.ui.viewmodel.MusicViewModel
import com.example.myfirstapp.ui.viewmodel.MyPlaylistsLoadState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    viewModel: MusicViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onPlaylistClick: (Long) -> Unit = {}
) {
    val playlists by viewModel.playlists.collectAsState(initial = emptyList())
    val myPlaylists by viewModel.myPlaylists.collectAsState(initial = emptyList())
    val myPlaylistsLoadState by viewModel.myPlaylistsLoadState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var playlistToDelete by remember { mutableStateOf<PlaylistDto?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadMyPlaylists()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.playlist_title),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = AccentRose
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.cd_add),
                    tint = Color.White
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BlackBase)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AccentRose,
                            containerColor = Color.Transparent
                        )
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_add))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.playlist_new))
                    }
                }

                if (myPlaylists.isNotEmpty()) {
                    item {
                        Text(stringResource(R.string.playlist_my_online), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = TextPrimary,
                            modifier = Modifier.padding(top = 8.dp))
                    }
                    items(myPlaylists, key = { "my_${it.id}" }) { playlist ->
                        RemotePlaylistCard(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist.id.toLong()) },
                            onDelete = { playlistToDelete = playlist }
                        )
                    }
                }

                when (val loadState = myPlaylistsLoadState) {
                    is MyPlaylistsLoadState.Loading -> {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = AccentRose, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                    is MyPlaylistsLoadState.Error -> {
                        item {
                            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(loadState.message, color = TextTertiary)
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = { viewModel.loadMyPlaylists() }) {
                                    Text(stringResource(R.string.cd_refresh), color = AccentRose)
                                }
                            }
                        }
                    }
                    is MyPlaylistsLoadState.Success -> {}
                }

                if (myPlaylists.isEmpty() && myPlaylistsLoadState is MyPlaylistsLoadState.Success) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.QueueMusic,
                                contentDescription = stringResource(R.string.cd_playlist_cover),
                                modifier = Modifier.size(80.dp),
                                tint = TextTertiary
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = stringResource(R.string.playlist_empty),
                                style = MaterialTheme.typography.titleLarge,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.playlist_create_first),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextTertiary
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(40.dp)) }
            }
        }

        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false; newPlaylistName = "" },
                title = { Text(stringResource(R.string.playlist_new)) },
                text = {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text(stringResource(R.string.playlist_enter_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistName)
                        }
                        showCreateDialog = false
                        newPlaylistName = ""
                    }) { Text(stringResource(R.string.playlist_create)) }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false; newPlaylistName = "" }) { Text(stringResource(R.string.playlist_cancel)) }
                }
            )
        }

        playlistToDelete?.let { playlist ->
            AlertDialog(
                onDismissRequest = { playlistToDelete = null },
                title = { Text("删除歌单") },
                text = { Text("确定删除「${playlist.name}」吗？") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deletePlaylist(playlist.id)
                        playlistToDelete = null
                    }) {
                        Text("删除", color = AccentRose)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { playlistToDelete = null }) { Text("取消") }
                }
            )
        }
    }
}

@Composable
fun PlaylistCard(
    playlist: PlaylistEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Surface1
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, Divider)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(AccentRose, AccentGradientEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.QueueMusic,
                    contentDescription = stringResource(R.string.cd_playlist_cover),
                    modifier = Modifier.size(28.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.playlist_created_at, android.text.format.DateFormat.format("yyyy-MM-dd", playlist.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }

            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = stringResource(R.string.cd_next),
                tint = TextTertiary
            )
        }
    }
}

@Composable
fun RemotePlaylistCard(
    playlist: PlaylistDto,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Surface1
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, Divider)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(AccentRose, AccentGradientEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.QueueMusic,
                    contentDescription = stringResource(R.string.cd_playlist_cover),
                    modifier = Modifier.size(28.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.playlist_track_count, playlist.trackCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_delete), tint = TextTertiary)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.cd_next), tint = TextTertiary)
        }
    }
}
