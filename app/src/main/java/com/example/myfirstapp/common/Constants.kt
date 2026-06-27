package com.example.myfirstapp.common

/**
 * 集中管理的应用常量，避免硬编码分散各处。
 */
object Constants {
    // 音频过滤
    const val MIN_AUDIO_DURATION_MS = 30_000L

    // 通知
    const val NOTIFICATION_CHANNEL_ID = "music_playback_channel"
    const val NOTIFICATION_CHANNEL_NAME = "音乐播放"
    const val NOTIFICATION_CHANNEL_DESC = "显示当前播放的音乐"

    // AI 生成轮询
    const val MAX_POLLING_ATTEMPTS = 60
    const val POLLING_INTERVAL_MS = 2000L
}
