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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myfirstapp.data.remote.dto.AIGenerationResponse
import com.example.myfirstapp.data.remote.dto.TrackDto
import com.example.myfirstapp.ui.components.GradientCover
import com.example.myfirstapp.ui.components.SectionHeader
import com.example.myfirstapp.ui.components.TrackRow
import com.example.myfirstapp.ui.theme.AccentRose
import com.example.myfirstapp.ui.theme.BlackBase
import com.example.myfirstapp.ui.theme.Divider
import com.example.myfirstapp.ui.theme.Surface1
import com.example.myfirstapp.ui.theme.Surface2
import com.example.myfirstapp.ui.theme.TextPrimary
import com.example.myfirstapp.ui.theme.TextSecondary
import com.example.myfirstapp.ui.theme.TextTertiary
import com.example.myfirstapp.ui.viewmodel.AuthViewModel
import com.example.myfirstapp.ui.viewmodel.MusicViewModel
import com.example.myfirstapp.ui.viewmodel.MyTracksState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkManagementScreen(
    viewModel: MusicViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onUpload: () -> Unit,
    onTrackClick: (Int) -> Unit,
    onRestoreGeneration: (AIGenerationResponse) -> Unit
) {
    val authState by authViewModel.uiState.collectAsState()
    val myTracksState by viewModel.myTracksState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(authState.userId) {
        authViewModel.refreshAuthState()
        if (authState.userId != null) viewModel.loadMyTracks(authState.userId)
        viewModel.loadMyGenerations()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的作品", color = TextPrimary, fontWeight = FontWeight.Bold) },
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
            if (!authState.isLoggedIn) {
                item { EmptyActionCard("请先登录后管理作品", "登录后可以查看已发布作品、上传本地音乐和恢复 AI 生成历史。", null) }
                return@LazyColumn
            }

            item {
                UploadEntryCard(onClick = onUpload)
            }

            item { SectionHeader(title = "已发布作品") }
            when (val state = myTracksState) {
                is MyTracksState.Idle, is MyTracksState.Loading -> item { LoadingRow() }
                is MyTracksState.Error -> item { EmptyActionCard("加载失败", state.message) { viewModel.loadMyTracks(authState.userId) } }
                is MyTracksState.Success -> {
                    if (state.tracks.isEmpty()) {
                        item { EmptyActionCard("还没有已发布作品", "AI 生成后点击发布，或上传本地音乐，就会出现在这里。", onUpload) }
                    } else {
                        items(state.tracks, key = { "work_${it.id}" }) { track ->
                            PublishedTrackRow(track = track, onClick = { onTrackClick(track.id) })
                        }
                    }
                }
            }

            item { SectionHeader(title = "AI 生成历史") }
            if (uiState.generationHistory.isEmpty()) {
                item { EmptyActionCard("还没有生成历史", "从创作页生成一首音乐后，这里会显示任务状态。", null) }
            } else {
                items(uiState.generationHistory, key = { "generation_${it.id}" }) { job ->
                    GenerationHistoryRow(job = job, onClick = { onRestoreGeneration(job) })
                }
            }
        }
    }
}

@Composable
private fun UploadEntryCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        border = BorderStroke(0.5.dp, Divider)
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(52.dp).background(AccentRose.copy(alpha = 0.16f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = AccentRose)
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text("上传本地音乐", color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("选择手机里的音频文件并发布到社区", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextTertiary)
        }
    }
}

@Composable
private fun PublishedTrackRow(track: TrackDto, onClick: () -> Unit) {
    TrackRow(
        title = track.title,
        subtitle = track.artist ?: track.username.ifBlank { "未知创作者" },
        isAiGenerated = track.isAiGenerated,
        onClick = onClick,
        trailing = {
            Icon(Icons.Filled.PlayArrow, contentDescription = "进入讨论", tint = AccentRose)
        }
    )
}

@Composable
private fun GenerationHistoryRow(job: AIGenerationResponse, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        border = BorderStroke(0.5.dp, Divider)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            GradientCover(modifier = Modifier.size(50.dp), isAiGenerated = true, icon = Icons.Filled.AutoAwesome)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(job.prompt, color = TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${friendlyStatus(job.status)} · ${job.progress}%", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
            }
            if (job.trackId != null) {
                Text("已发布", color = AccentRose, style = MaterialTheme.typography.labelLarge)
            } else {
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextTertiary)
            }
        }
    }
}

@Composable
private fun LoadingRow() {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AccentRose, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun EmptyActionCard(title: String, subtitle: String, onClick: (() -> Unit)? = null) {
    Card(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Surface2),
        border = BorderStroke(0.5.dp, Divider)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = TextTertiary, style = MaterialTheme.typography.bodyMedium)
            if (onClick != null) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = AccentRose)) {
                    Text("去处理", color = Color.White)
                }
            }
        }
    }
}

private fun friendlyStatus(status: String): String = when (status) {
    "PENDING" -> "排队中"
    "RUNNING" -> "生成中"
    "SUCCEEDED" -> "可发布"
    "PUBLISHED" -> "已发布"
    "FAILED" -> "失败"
    else -> status
}
