package com.example.myfirstapp.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.myfirstapp.ui.components.PageTitle
import com.example.myfirstapp.ui.components.PrimaryActionCard
import com.example.myfirstapp.ui.components.SectionHeader
import com.example.myfirstapp.ui.theme.AccentRose
import com.example.myfirstapp.ui.theme.BlackBase
import com.example.myfirstapp.ui.theme.Divider
import com.example.myfirstapp.ui.theme.Surface1
import com.example.myfirstapp.ui.theme.Surface2
import com.example.myfirstapp.ui.theme.TextPrimary
import com.example.myfirstapp.ui.theme.TextSecondary
import com.example.myfirstapp.ui.theme.TextTertiary
import com.example.myfirstapp.ui.viewmodel.MusicViewModel

@Composable
fun CreateScreen(
    viewModel: MusicViewModel = hiltViewModel(),
    onNavigateToAI: () -> Unit,
    onNavigateToAIWithPrompt: (String) -> Unit = {},
    onNavigateToWorks: () -> Unit = {},
    onUploadClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadMyGenerations()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackBase)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp)
    ) {
        PageTitle(
            title = "创作",
            subtitle = "把脑洞变成能发布到社区的声音"
        )

        PrimaryActionCard(
            title = "AI 音乐创作",
            subtitle = "输入灵感，生成、试听并发布到社区",
            icon = Icons.Filled.AutoAwesome,
            onClick = onNavigateToAI
        )

        Spacer(Modifier.height(18.dp))
        SectionHeader(title = "灵感模板")

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            inspirationPrompts.forEach { (text, prompt) ->
                AssistChip(
                    onClick = { onNavigateToAIWithPrompt(prompt) },
                    label = { Text(text, color = TextSecondary) },
                    leadingIcon = {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = AccentRose, modifier = Modifier.size(16.dp))
                    },
                    colors = AssistChipDefaults.assistChipColors(containerColor = Surface2),
                    border = BorderStroke(0.5.dp, Divider)
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionHeader(title = "最近生成")

        val history = uiState.generationHistory.take(3)
        if (history.isEmpty()) {
            EmptyRecentGenerationCard(onClick = onNavigateToAI)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                history.forEach { job ->
                    RecentGenerationRow(job = job, onClick = onNavigateToWorks)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionHeader(title = "发布自己的音乐")

        SecondaryUploadCard(onClick = onUploadClick)
    }
}

private val inspirationPrompts = listOf(
    "8bit Boss 战" to "一首 8bit 芯片音乐风格的 Boss 战音乐，节奏紧张，旋律轻快，有复古游戏感",
    "赛博女声" to "赛博朋克风格电子音乐，带未来感女声氛围，适合夜晚城市",
    "雨夜钢琴" to "雨夜里的安静钢琴曲，旋律温柔，有一点孤独感",
    "抽象鬼畜" to "抽象搞怪的电子节奏音乐，适合整活和鬼畜视频",
    "二次元角色曲" to "二次元角色主题曲，明亮、可爱、带一点冒险感"
)

@Composable
private fun RecentGenerationRow(job: AIGenerationResponse, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
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
                    .background(AccentRose.copy(alpha = 0.14f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.History, contentDescription = null, tint = AccentRose)
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    job.prompt,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${friendlyStatus(job.status)} · ${job.progress}%",
                    color = TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextTertiary)
        }
    }
}

@Composable
private fun EmptyRecentGenerationCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        border = BorderStroke(0.5.dp, Divider)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = AccentRose, modifier = Modifier.size(28.dp))
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text("还没有生成记录", color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("先做一首用于答辩展示的作品", color = TextTertiary, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextTertiary)
        }
    }
}

@Composable
private fun SecondaryUploadCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        border = BorderStroke(0.5.dp, Divider)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(Color(0xFF3D7DFF).copy(alpha = 0.16f), RoundedCornerShape(17.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = Color(0xFF7AA6FF))
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text("上传音乐", color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("分享你自己制作的音乐作品到社区", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextTertiary)
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
