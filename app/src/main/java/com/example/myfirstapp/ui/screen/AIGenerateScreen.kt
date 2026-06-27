package com.example.myfirstapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myfirstapp.data.remote.dto.AIGenerationResponse
import com.example.myfirstapp.ui.theme.AccentGradientEnd
import com.example.myfirstapp.ui.theme.AccentRose
import com.example.myfirstapp.ui.theme.BlackBase
import com.example.myfirstapp.ui.theme.Surface1
import com.example.myfirstapp.ui.theme.TextPrimary
import com.example.myfirstapp.ui.theme.TextSecondary
import com.example.myfirstapp.ui.theme.TextTertiary
import com.example.myfirstapp.ui.viewmodel.MusicViewModel
import com.example.myfirstapp.ui.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIGenerateScreen(
    viewModel: MusicViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    initialPrompt: String = "",
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var prompt by remember(initialPrompt) { mutableStateOf(initialPrompt) }
    var bpm by remember { mutableStateOf("120") }
    var duration by remember { mutableStateOf("30") }
    var selectedGenre by remember { mutableStateOf("") }
    var publishTitle by remember { mutableStateOf("") }
    var publishDescription by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadMyGenerations()
    }

    val genres = listOf("流行", "摇滚", "古典", "电子", "爵士", "嘻哈", "民谣", "R&B")
    val currentJob = uiState.currentGeneration
    val canPreview = uiState.generatedAudioUrl != null
    val canPublish = currentJob?.status == "SUCCEEDED" || currentJob?.status == "PUBLISHED"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("AI 音乐创作", fontWeight = FontWeight.Bold, color = TextPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BlackBase)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                "输入灵感，后端会调用 AI 生成任务；生成成功后，音频会先保存到本项目后端，再发布到社区。",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(16.dp))

            GenerationFormCard(
                prompt = prompt,
                onPromptChange = { prompt = it.take(500) },
                bpm = bpm,
                onBpmChange = { bpm = it.filter { ch -> ch.isDigit() }.take(3) },
                duration = duration,
                onDurationChange = { duration = it.filter { ch -> ch.isDigit() }.take(3) },
                genres = genres,
                selectedGenre = selectedGenre,
                onGenreSelected = { selectedGenre = if (selectedGenre == it) "" else it },
                isGenerating = uiState.isGenerating,
                onGenerate = {
                    viewModel.generateMusic(
                        prompt = prompt,
                        bpm = bpm.toIntOrNull(),
                        duration = duration.toIntOrNull(),
                        genre = selectedGenre.ifBlank { null }
                    )
                }
            )

            if (currentJob != null || uiState.isGenerating) {
                Spacer(Modifier.height(16.dp))
                GenerationStatusCard(
                    job = currentJob,
                    progress = uiState.generationProgress,
                    isGenerating = uiState.isGenerating,
                    onRetry = { viewModel.retryCurrentGeneration() }
                )
            }

            if (canPreview) {
                Spacer(Modifier.height(16.dp))
                PreviewAndPublishCard(
                    job = currentJob,
                    publishTitle = publishTitle,
                    onTitleChange = { publishTitle = it.take(80) },
                    publishDescription = publishDescription,
                    onDescriptionChange = { publishDescription = it.take(300) },
                    canPublish = canPublish,
                    isPublishing = uiState.isPublishing,
                    publishedTrackId = uiState.publishedTrackId,
                    onPreview = {
                        uiState.generatedAudioUrl?.let { url ->
                            playerViewModel.playUrl(
                                url,
                                publishTitle.ifBlank { "AI生成: ${currentJob?.prompt?.take(20) ?: "未命名"}" },
                                "AI音乐"
                            )
                        }
                    },
                    onPublish = {
                        viewModel.publishGeneratedMusic(
                            publishTitle.ifBlank { null },
                            publishDescription.ifBlank { null }
                        )
                    }
                )
            }

            if (uiState.error != null) {
                Spacer(Modifier.height(12.dp))
                ErrorCard(message = uiState.error ?: "", onClose = { viewModel.clearError() })
            }

            if (uiState.generationHistory.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HistoryCard(
                    jobs = uiState.generationHistory,
                    onRestore = { job ->
                        prompt = job.prompt
                        bpm = job.bpm?.toString() ?: bpm
                        duration = job.durationSec.toString()
                        selectedGenre = job.genre.orEmpty()
                        publishTitle = "AI生成 - ${job.prompt.take(20)}"
                        viewModel.restoreGeneration(job)
                    }
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GenerationFormCard(
    prompt: String,
    onPromptChange: (String) -> Unit,
    bpm: String,
    onBpmChange: (String) -> Unit,
    duration: String,
    onDurationChange: (String) -> Unit,
    genres: List<String>,
    selectedGenre: String,
    onGenreSelected: (String) -> Unit,
    isGenerating: Boolean,
    onGenerate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("创作提示词", color = TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                placeholder = { Text("例：一个赛博猫娘在雨夜唱城市流行歌，旋律轻快但带一点孤独感", color = TextTertiary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp),
                colors = aiTextFieldColors(),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = bpm,
                    onValueChange = onBpmChange,
                    label = { Text("BPM", color = TextTertiary) },
                    placeholder = { Text("120", color = TextTertiary) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = aiTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = duration,
                    onValueChange = onDurationChange,
                    label = { Text("时长/秒", color = TextTertiary) },
                    placeholder = { Text("30", color = TextTertiary) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = aiTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text("风格", color = TextTertiary, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            genres.chunked(4).forEach { rowGenres ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowGenres.forEach { genre ->
                        val selected = selectedGenre == genre
                        FilterChip(
                            selected = selected,
                            onClick = { onGenreSelected(genre) },
                            label = { Text(genre, color = if (selected) TextPrimary else TextSecondary) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentRose.copy(alpha = 0.25f),
                                containerColor = Color(0xFF1A1A2E)
                            )
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onGenerate,
                enabled = prompt.isNotBlank() && !isGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentRose)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isGenerating) "生成中..." else "提交 AI 生成任务", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GenerationStatusCard(
    job: AIGenerationResponse?,
    progress: Int,
    isGenerating: Boolean,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = AccentRose)
                Spacer(Modifier.width(8.dp))
                Text("任务状态：${job?.status ?: "PENDING"}", color = TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (job?.status == "FAILED") {
                    TextButton(onClick = onRetry) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("重试")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = AccentRose,
                trackColor = Color(0xFF2A2A3E)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (isGenerating) "后端每 2 秒查询一次任务，成功后会下载音频到本地存储。"
                else "进度 $progress%，可以试听或从历史任务恢复。",
                color = TextTertiary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun PreviewAndPublishCard(
    job: AIGenerationResponse?,
    publishTitle: String,
    onTitleChange: (String) -> Unit,
    publishDescription: String,
    onDescriptionChange: (String) -> Unit,
    canPublish: Boolean,
    isPublishing: Boolean,
    publishedTrackId: Int?,
    onPreview: () -> Unit,
    onPublish: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(AccentRose, AccentGradientEnd))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("生成音频已就绪", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(job?.prompt ?: "", color = TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FilledIconButton(
                    onClick = onPreview,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = AccentRose)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "试听", tint = Color.White)
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = publishTitle,
                onValueChange = onTitleChange,
                label = { Text("发布标题", color = TextTertiary) },
                placeholder = { Text("不填则使用提示词自动命名", color = TextTertiary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = aiTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = publishDescription,
                onValueChange = onDescriptionChange,
                label = { Text("作品描述", color = TextTertiary) },
                modifier = Modifier.fillMaxWidth(),
                colors = aiTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onPublish,
                enabled = canPublish && !isPublishing && publishedTrackId == null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentRose)
            ) {
                when {
                    publishedTrackId != null -> {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("已发布到社区 #$publishedTrackId")
                    }

                    isPublishing -> {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("发布中...")
                    }

                    else -> {
                        Icon(Icons.Filled.Publish, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("保存并发布到社区")
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    jobs: List<AIGenerationResponse>,
    onRestore: (AIGenerationResponse) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.History, contentDescription = null, tint = TextSecondary)
                Spacer(Modifier.width(8.dp))
                Text("最近生成任务", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            jobs.take(5).forEach { job ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(job.prompt, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${job.status} · ${job.progress}% · ${job.provider}", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { onRestore(job) }) {
                        Text("恢复")
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String, onClose: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AccentRose.copy(alpha = 0.12f))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Error, contentDescription = null, tint = AccentRose, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(message, color = AccentRose, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("关闭", color = AccentRose) }
        }
    }
}

@Composable
private fun aiTextFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedTextColor = TextPrimary,
    focusedTextColor = TextPrimary,
    focusedBorderColor = AccentRose.copy(alpha = 0.5f),
    unfocusedBorderColor = Color(0xFF2A2A3E),
    cursorColor = AccentRose,
    unfocusedContainerColor = Color(0xFF151526),
    focusedContainerColor = Color(0xFF151526)
)
