package com.example.myfirstapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfirstapp.data.local.SongEntity
import com.example.myfirstapp.data.remote.dto.TrackDto
import com.example.myfirstapp.ui.theme.AccentGold
import com.example.myfirstapp.ui.theme.AccentGradientEnd
import com.example.myfirstapp.ui.theme.AccentGradientStart
import com.example.myfirstapp.ui.theme.AccentRose
import com.example.myfirstapp.ui.theme.Divider
import com.example.myfirstapp.ui.theme.Surface1
import com.example.myfirstapp.ui.theme.Surface2
import com.example.myfirstapp.ui.theme.Surface3
import com.example.myfirstapp.ui.theme.TextPrimary
import com.example.myfirstapp.ui.theme.TextSecondary
import com.example.myfirstapp.ui.theme.TextTertiary
import com.example.myfirstapp.util.formatDuration

@Composable
fun PageTitle(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.7).sp
            )
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = TextTertiary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(AccentRose)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (actionText != null && onAction != null) {
            Text(
                text = actionText,
                color = AccentRose,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}

@Composable
fun GradientCover(
    modifier: Modifier = Modifier,
    isAiGenerated: Boolean = false,
    icon: ImageVector = if (isAiGenerated) Icons.Filled.GraphicEq else Icons.Filled.MusicNote
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    if (isAiGenerated) {
                        listOf(AccentRose, AccentGold)
                    } else {
                        listOf(AccentGradientStart, AccentGradientEnd)
                    }
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
        if (isAiGenerated) {
            Surface(
                color = Color.Black.copy(alpha = 0.28f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            ) {
                Text(
                    text = "AI",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun MetricChip(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(4.dp))
        Text(text = text, color = TextTertiary, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun CommunityTrackCard(
    track: TrackDto,
    onOpen: () -> Unit = {},
    onPlay: () -> Unit,
    onLike: () -> Unit = {},
    onComment: () -> Unit = {}
) {
    val title = track.title.ifBlank { "未命名作品" }
    val artist = track.artist?.ifBlank { null } ?: "未知创作者"
    val username = track.username.ifBlank { artist }
    val prompt = track.aiPrompt?.takeIf { it.isNotBlank() } ?: track.description ?: track.genre ?: "这个作品还没有填写灵感描述"

    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        border = BorderStroke(0.5.dp, Divider)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(AccentRose.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = username.take(1).ifBlank { "音" },
                        color = AccentRose,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(username, color = TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(
                        if (track.isAiGenerated) "发布了一首 AI 音乐" else "发布了一首原创音乐",
                        color = TextTertiary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (track.isAiGenerated) {
                    AssistChip(
                        onClick = {},
                        label = { Text("AI", color = AccentRose, fontWeight = FontWeight.Bold) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = AccentRose.copy(alpha = 0.12f)),
                        border = null
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                GradientCover(
                    modifier = Modifier.size(72.dp),
                    isAiGenerated = track.isAiGenerated
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        artist,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        prompt,
                        color = TextTertiary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onPlay) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(AccentRose),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "播放", tint = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.clickable(onClick = onLike),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetricChip(Icons.Filled.FavoriteBorder, "${track.likeCount}")
                }
                Row(
                    modifier = Modifier.clickable(onClick = onComment),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetricChip(Icons.Filled.ChatBubbleOutline, "${track.commentCount}")
                }
                track.genre?.takeIf { it.isNotBlank() }?.let {
                    Text(text = "#$it", color = TextTertiary, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun TrackRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    durationText: String? = null,
    isAiGenerated: Boolean = false,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GradientCover(modifier = Modifier.size(54.dp), isAiGenerated = isAiGenerated)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title.ifBlank { "未命名作品" },
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle.ifBlank { "未知创作者" },
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (durationText != null) {
            Text(durationText, color = TextTertiary, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.width(8.dp))
        }
        trailing?.invoke()
    }
}

@Composable
fun MiniPlayerBar(
    currentSong: SongEntity,
    isPlaying: Boolean,
    onToggle: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    Card(
        onClick = onOpenPlayer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Surface2),
        border = BorderStroke(0.5.dp, Divider),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GradientCover(modifier = Modifier.size(44.dp), isAiGenerated = currentSong.uri.startsWith("http"))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    currentSong.title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    currentSong.artist,
                    color = TextTertiary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onToggle) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = Color.White,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(AccentRose)
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun PrimaryActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        border = BorderStroke(0.5.dp, Divider)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(AccentRose.copy(alpha = 0.28f), Surface1, Surface1)
                    )
                )
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(AccentRose, AccentGold))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

fun SongEntity.durationText(): String = formatDuration(duration)
