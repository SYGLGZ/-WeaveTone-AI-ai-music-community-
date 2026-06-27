package com.example.myfirstapp.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myfirstapp.ui.theme.AccentRose
import com.example.myfirstapp.ui.theme.BlackBase
import com.example.myfirstapp.ui.theme.Divider
import com.example.myfirstapp.ui.theme.Surface1
import com.example.myfirstapp.ui.theme.Surface2
import com.example.myfirstapp.ui.theme.Surface3
import com.example.myfirstapp.ui.theme.TextPrimary
import com.example.myfirstapp.ui.theme.TextSecondary
import com.example.myfirstapp.ui.theme.TextTertiary
import com.example.myfirstapp.ui.viewmodel.AuthViewModel
import com.example.myfirstapp.ui.viewmodel.MusicViewModel
import com.example.myfirstapp.ui.viewmodel.UploadState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadMusicScreen(
    viewModel: MusicViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onUploaded: (Int) -> Unit
) {
    val uploadState by viewModel.uploadState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf(authState.username.orEmpty()) }
    var genre by remember { mutableStateOf("") }
    var bpm by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.resetUploadState()
        authViewModel.refreshAuthState()
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("上传音乐", color = TextPrimary, fontWeight = FontWeight.Bold) },
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                onClick = { picker.launch("audio/*") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Surface1),
                border = BorderStroke(0.5.dp, Divider)
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(54.dp).background(AccentRose.copy(alpha = 0.16f), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(if (selectedUri == null) Icons.Filled.AudioFile else Icons.Filled.CheckCircle, contentDescription = null, tint = AccentRose)
                    }
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (selectedUri == null) "选择音频文件" else "已选择音频文件", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text(selectedUri?.lastPathSegment ?: "支持 mp3、wav、m4a 等系统可读取音频", color = TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            UploadTextField(title, { title = it.take(80) }, "标题", "例如：猫和老鼠")
            UploadTextField(artist, { artist = it.take(60) }, "艺术家", authState.username ?: "你的名字")
            UploadTextField(genre, { genre = it.take(30) }, "流派", "电子 / 古典 / 纯音乐")
            UploadTextField(
                value = bpm,
                onValueChange = { bpm = it.filter(Char::isDigit).take(3) },
                label = "BPM（可选）",
                placeholder = "120",
                keyboardType = KeyboardType.Number
            )
            UploadTextField(description, { description = it.take(300) }, "作品描述", "写一句介绍，让社区知道这首歌是什么")

            when (val state = uploadState) {
                is UploadState.Idle -> {}
                is UploadState.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = AccentRose, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(8.dp))
                        Text("上传中，请稍等...", color = TextSecondary)
                    }
                }
                is UploadState.Error -> Text(state.message, color = AccentRose)
                is UploadState.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface2),
                        border = BorderStroke(0.5.dp, Divider)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("已上传到社区", color = TextPrimary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { onUploaded(state.trackId) },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentRose)
                            ) {
                                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.size(8.dp))
                                Text("查看讨论页", color = Color.White)
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val uri = selectedUri ?: return@Button
                    viewModel.uploadMusic(
                        uri = uri,
                        title = title,
                        artist = artist.takeIf { it.isNotBlank() },
                        genre = genre.takeIf { it.isNotBlank() },
                        bpm = bpm.toIntOrNull(),
                        description = description.takeIf { it.isNotBlank() }
                    )
                },
                enabled = selectedUri != null && title.isNotBlank() && uploadState !is UploadState.Loading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentRose)
            ) {
                Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = Color.White)
                Spacer(Modifier.size(8.dp))
                Text("上传并发布到社区", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun UploadTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextTertiary) },
        placeholder = { Text(placeholder, color = TextTertiary) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = label != "作品描述",
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedTextColor = TextPrimary,
            focusedTextColor = TextPrimary,
            focusedBorderColor = AccentRose.copy(alpha = 0.5f),
            unfocusedBorderColor = Divider,
            cursorColor = AccentRose,
            unfocusedContainerColor = Surface3,
            focusedContainerColor = Surface3
        )
    )
}
