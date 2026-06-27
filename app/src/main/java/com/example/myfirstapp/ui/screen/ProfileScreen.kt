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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.myfirstapp.ui.components.PageTitle
import com.example.myfirstapp.ui.components.SectionHeader
import com.example.myfirstapp.ui.theme.AccentGold
import com.example.myfirstapp.ui.theme.AccentRose
import com.example.myfirstapp.ui.theme.BlackBase
import com.example.myfirstapp.ui.theme.Divider
import com.example.myfirstapp.ui.theme.Surface1
import com.example.myfirstapp.ui.theme.Surface2
import com.example.myfirstapp.ui.theme.TextPrimary
import com.example.myfirstapp.ui.theme.TextSecondary
import com.example.myfirstapp.ui.theme.TextTertiary
import com.example.myfirstapp.ui.viewmodel.AuthViewModel
import com.example.myfirstapp.ui.viewmodel.LikedTracksState
import com.example.myfirstapp.ui.viewmodel.MusicViewModel
import com.example.myfirstapp.ui.viewmodel.MyTracksState

@Composable
fun ProfileScreen(
    viewModel: MusicViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onNavigateToPlaylists: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToWorks: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToLibrary: () -> Unit = {}
) {
    val authState by authViewModel.uiState.collectAsState()
    val songs by viewModel.songs.collectAsState(initial = emptyList())
    val aiSongs by viewModel.aiSongs.collectAsState(initial = emptyList())
    val myPlaylists by viewModel.myPlaylists.collectAsState(initial = emptyList())
    val myTracksState by viewModel.myTracksState.collectAsState()
    val likedTracksState by viewModel.likedTracksState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(authState.userId) {
        if (authState.userId != null) {
            viewModel.loadMyTracks(authState.userId)
            viewModel.loadLikedTracks()
            viewModel.loadMyPlaylists()
        }
    }

    val worksCount = when (val state = myTracksState) {
        is MyTracksState.Success -> state.tracks.size
        else -> aiSongs.size
    }
    val favoritesCount = when (val state = likedTracksState) {
        is LikedTracksState.Success -> state.tracks.size
        else -> 0
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                authViewModel.refreshAuthState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackBase)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp)
    ) {
        PageTitle(
            title = "我的",
            subtitle = if (authState.isLoggedIn) "管理你的作品、收藏和播放列表" else "登录后可发布 AI 作品到社区"
        )

        ProfileHeroCard(
            username = if (authState.isLoggedIn) authState.username ?: "已登录用户" else "本地用户",
            isLoggedIn = authState.isLoggedIn,
            worksCount = worksCount,
            favoritesCount = favoritesCount,
            followingCount = myPlaylists.size,
            onLogin = onNavigateToLogin,
            onLogout = { authViewModel.logout() },
            onWorks = onNavigateToWorks,
            onFavorites = onNavigateToFavorites,
            onPlaylists = onNavigateToPlaylists
        )

        Spacer(Modifier.height(18.dp))
        SectionHeader(title = "我的作品")

        Card(
            onClick = onNavigateToWorks,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Surface1),
            border = BorderStroke(0.5.dp, Divider)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    text = if (worksCount == 0) "你发布的 AI 音乐会展示在这里" else "已发布 $worksCount 首作品",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "答辩时可以从这里说明：创作、试听、发布和社区展示已经形成闭环。",
                    color = TextTertiary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionHeader(title = "音乐管理")

        MenuCard {
            ProfileMenuItemRow(Icons.Filled.QueueMusic, "我的播放列表", "管理 ${myPlaylists.size} 个歌单", onNavigateToPlaylists)
            ProfileDivider()
            ProfileMenuItemRow(Icons.Filled.Favorite, "我的收藏", "收藏喜欢的社区作品", onNavigateToFavorites)
            ProfileDivider()
            ProfileMenuItemRow(Icons.Filled.Download, "本地音乐", "共 ${songs.size} 首本地歌曲", onNavigateToLibrary)
        }

        Spacer(Modifier.height(18.dp))
        SectionHeader(title = "设置")

        MenuCard {
            ProfileMenuItemRow(Icons.Filled.Settings, "应用设置", "网络、播放与应用信息", onNavigateToSettings)
            ProfileDivider()
            ProfileMenuItemRow(Icons.Filled.Info, "关于", "AI Music 课设项目") {}
        }
    }
}

@Composable
private fun ProfileHeroCard(
    username: String,
    isLoggedIn: Boolean,
    worksCount: Int,
    favoritesCount: Int,
    followingCount: Int,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onWorks: () -> Unit,
    onFavorites: () -> Unit,
    onPlaylists: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        border = BorderStroke(0.5.dp, Divider)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(AccentRose.copy(alpha = 0.24f), Surface1)))
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(66.dp)
                            .background(Brush.linearGradient(listOf(AccentRose, AccentGold)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoggedIn) {
                            Text(username.take(1), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                        } else {
                            Icon(Icons.Filled.Person, contentDescription = "用户头像", tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                    }
                    Spacer(Modifier.size(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(username, color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text(
                            if (isLoggedIn) "已登录，可使用 AI 创作和社区发布" else "未登录，只能使用本地音乐功能",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ProfileStat("作品", worksCount.toString(), onWorks)
                    ProfileStat("收藏", favoritesCount.toString(), onFavorites)
                    ProfileStat("歌单", followingCount.toString(), onPlaylists)
                }

                Spacer(Modifier.height(18.dp))

                if (isLoggedIn) {
                    OutlinedButton(
                        onClick = onLogout,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRose),
                        border = BorderStroke(1.dp, AccentRose.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("退出登录")
                    }
                } else {
                    Button(
                        onClick = onLogin,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRose),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("登录 / 注册", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Text(value, color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        Text(label, color = TextTertiary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MenuCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        border = BorderStroke(0.5.dp, Divider)
    ) {
        Column(Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
}

@Composable
private fun ProfileMenuItemRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Surface2, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = TextSecondary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ProfileDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .padding(start = 70.dp)
            .background(Divider)
    )
}
