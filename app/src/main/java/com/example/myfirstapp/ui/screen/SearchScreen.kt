package com.example.myfirstapp.ui.screen

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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myfirstapp.R
import com.example.myfirstapp.data.remote.dto.TrackDto
import com.example.myfirstapp.data.remote.dto.UserProfileDto
import com.example.myfirstapp.util.trackStreamUrl
import com.example.myfirstapp.ui.theme.AccentGradientEnd
import com.example.myfirstapp.ui.theme.AccentGradientStart
import com.example.myfirstapp.ui.theme.AccentRose
import com.example.myfirstapp.ui.theme.BlackBase
import com.example.myfirstapp.ui.theme.Divider
import com.example.myfirstapp.ui.theme.Surface3
import com.example.myfirstapp.ui.theme.TextPrimary
import com.example.myfirstapp.ui.theme.TextSecondary
import com.example.myfirstapp.ui.theme.TextTertiary
import com.example.myfirstapp.ui.viewmodel.MusicViewModel
import com.example.myfirstapp.ui.viewmodel.PlayerViewModel
import com.example.myfirstapp.ui.viewmodel.SearchState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MusicViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onPlaySong: (Long) -> Unit,
    onUserClick: (Int) -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    val searchState by viewModel.searchState.collectAsState()
    var searchJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(query) {
        searchJob?.cancel()
        searchJob = launch {
            delay(300)
            viewModel.search(query)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_title), fontWeight = FontWeight.Bold, color = TextPrimary, letterSpacing = (-0.5).sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, stringResource(R.string.cd_back), tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(BlackBase)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_placeholder), color = TextTertiary) },
                leadingIcon = { Icon(Icons.Filled.Search, stringResource(R.string.cd_search), tint = TextTertiary, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Clear, stringResource(R.string.cd_clear), tint = TextTertiary, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
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

            when (val state = searchState) {
                is SearchState.Idle -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.search_idle_hint), color = TextTertiary, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                is SearchState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentRose, modifier = Modifier.size(32.dp))
                    }
                }
                is SearchState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = TextTertiary, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                is SearchState.Success -> {
                    val hasResults = state.result.tracks.isNotEmpty() || state.result.users.isNotEmpty()
                    if (!hasResults) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.search_no_results), color = TextTertiary, style = MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            if (state.result.tracks.isNotEmpty()) {
                                item {
                                    Text(stringResource(R.string.search_tracks_section), style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold, color = TextPrimary,
                                        modifier = Modifier.padding(vertical = 8.dp))
                                }
                                items(state.result.tracks, key = { "track_${it.id}" }) { track ->
                                    SearchTrackCard(track, onClick = {
                                        playerViewModel.playUrl(
                                            trackStreamUrl(track.id),
                                            track.title,
                                            track.artist ?: "未知艺术家"
                                        )
                                        onPlaySong(track.id.toLong())
                                    })
                                }
                            }
                            if (state.result.users.isNotEmpty()) {
                                item {
                                    Text(stringResource(R.string.search_users_section), style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold, color = TextPrimary,
                                        modifier = Modifier.padding(vertical = 8.dp))
                                }
                                items(state.result.users, key = { "user_${it.id}" }) { user ->
                                    SearchUserCard(user, onClick = { onUserClick(user.id) })
                                }
                            }
                            item { Spacer(Modifier.height(40.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchTrackCard(track: TrackDto, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(AccentGradientStart, AccentGradientEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MusicNote, stringResource(R.string.cd_music_note), tint = Color.White, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold, color = TextPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist ?: stringResource(R.string.home_unknown_artist), style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Spacer(Modifier.width(8.dp))
            Text("→", color = AccentRose, style = MaterialTheme.typography.bodyLarge)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Divider)
        )
    }
}

@Composable
private fun SearchUserCard(user: UserProfileDto, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape)
                    .background(AccentRose.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, stringResource(R.string.cd_user_avatar), tint = AccentRose, modifier = Modifier.size(24.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(user.username, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold, color = TextPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.playlist_track_count, user.trackCount), style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary)
            }

            Spacer(Modifier.width(8.dp))
            Text("→", color = AccentRose, style = MaterialTheme.typography.bodyLarge)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Divider)
        )
    }
}
