package com.example.myfirstapp.util

import com.example.myfirstapp.BuildConfig

fun backendUrl(path: String): String {
    val base = BuildConfig.BACKEND_BASE_URL.trimEnd('/')
    val normalizedPath = if (path.startsWith("/")) path else "/$path"
    return "$base$normalizedPath"
}

fun trackStreamUrl(trackId: Int): String = backendUrl("/api/v1/music/$trackId/stream")
