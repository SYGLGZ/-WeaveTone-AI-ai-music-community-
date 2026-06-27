package com.example.myfirstapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.myfirstapp.ui.navigation.AIMusicNavHost
import com.example.myfirstapp.ui.theme.AIMusicTheme
import com.example.myfirstapp.ui.viewmodel.MusicViewModel
import com.example.myfirstapp.ui.viewmodel.PlayerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val musicViewModel: MusicViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()

    private var showRationale by mutableStateOf(false)
    private var showSettings by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.READ_MEDIA_AUDIO] == true ||
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
                        permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true)

        if (audioGranted) {
            musicViewModel.scanLocalMusic()
        } else {
            val shouldShowRationale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (shouldShowRationale) {
                showRationale = true
            } else {
                showSettings = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIMusicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AIMusicNavHost(
                        onPlaySong = { songId -> /* handled by PlayerViewModel in screens */ }
                    )
                }

                if (showRationale) {
                    AlertDialog(
                        onDismissRequest = { showRationale = false },
                        title = { Text("需要音乐权限") },
                        text = { Text("AI音乐需要访问您的音乐文件才能扫描本地歌曲并播放。请授予权限以使用此功能。") },
                        confirmButton = {
                            Button(onClick = {
                                showRationale = false
                                requestPermissions()
                            }) {
                                Text("授予权限")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRationale = false }) {
                                Text("取消")
                            }
                        }
                    )
                }

                if (showSettings) {
                    AlertDialog(
                        onDismissRequest = { showSettings = false },
                        title = { Text("前往设置") },
                        text = { Text("您已多次拒绝权限请求。请前往系统设置手动授予音乐和媒体权限。") },
                        confirmButton = {
                            Button(onClick = {
                                showSettings = false
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", packageName, null)
                                }
                                startActivity(intent)
                            }) {
                                Text("前往设置")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showSettings = false }) {
                                Text("取消")
                            }
                        }
                    )
                }
            }
        }
        requestPermissions()
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        } else {
            musicViewModel.scanLocalMusic()
        }
    }
}
