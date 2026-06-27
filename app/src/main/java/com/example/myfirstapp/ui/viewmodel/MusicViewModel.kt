package com.example.myfirstapp.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfirstapp.common.Constants
import com.example.myfirstapp.data.remote.MusicApi
import com.example.myfirstapp.data.remote.dto.AIGenerationRequest
import com.example.myfirstapp.data.remote.dto.AIGenerationResponse
import com.example.myfirstapp.data.remote.dto.CommentDto
import com.example.myfirstapp.data.remote.dto.PlaylistDto
import com.example.myfirstapp.data.remote.dto.SearchResultDto
import com.example.myfirstapp.data.remote.dto.TrackDto
import com.example.myfirstapp.data.repository.MusicRepository
import com.example.myfirstapp.data.security.SecurePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PlaylistTracksState {
    object Loading : PlaylistTracksState()
    data class Success(val tracks: List<TrackDto>) : PlaylistTracksState()
    data class Error(val message: String) : PlaylistTracksState()
}

sealed class HomeTracksState {
    object Loading : HomeTracksState()
    data class Success(val tracks: List<TrackDto>) : HomeTracksState()
    data class Error(val message: String) : HomeTracksState()
}

sealed class SearchState {
    object Idle : SearchState()
    object Loading : SearchState()
    data class Success(val result: SearchResultDto) : SearchState()
    data class Error(val message: String) : SearchState()
}

sealed class MyPlaylistsLoadState {
    object Loading : MyPlaylistsLoadState()
    data class Success(val playlists: List<PlaylistDto>) : MyPlaylistsLoadState()
    data class Error(val message: String) : MyPlaylistsLoadState()
}

sealed class MyTracksState {
    object Idle : MyTracksState()
    object Loading : MyTracksState()
    data class Success(val tracks: List<TrackDto>) : MyTracksState()
    data class Error(val message: String) : MyTracksState()
}

sealed class LikedTracksState {
    object Idle : LikedTracksState()
    object Loading : LikedTracksState()
    data class Success(val tracks: List<TrackDto>) : LikedTracksState()
    data class Error(val message: String) : LikedTracksState()
}

sealed class TrackDetailState {
    object Idle : TrackDetailState()
    object Loading : TrackDetailState()
    data class Success(val track: TrackDto) : TrackDetailState()
    data class Error(val message: String) : TrackDetailState()
}

sealed class CommentsState {
    object Idle : CommentsState()
    object Loading : CommentsState()
    data class Success(val comments: List<CommentDto>) : CommentsState()
    data class Error(val message: String) : CommentsState()
}

sealed class UploadState {
    object Idle : UploadState()
    object Loading : UploadState()
    data class Success(val trackId: Int) : UploadState()
    data class Error(val message: String) : UploadState()
}

sealed class PlaylistActionState {
    object Idle : PlaylistActionState()
    object Loading : PlaylistActionState()
    data class Success(val message: String) : PlaylistActionState()
    data class Error(val message: String) : PlaylistActionState()
}

@HiltViewModel
class MusicViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val musicApi: MusicApi,
    private val securePreferences: SecurePreferences,
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    val songs = repository.songsFlow
    val aiSongs = repository.aiGeneratedSongsFlow
    val playlists = repository.playlistsFlow

    private var pollingJob: Job? = null

    private val _playlistTracksState = MutableStateFlow<PlaylistTracksState>(PlaylistTracksState.Loading)
    val playlistTracksState: StateFlow<PlaylistTracksState> = _playlistTracksState.asStateFlow()

    private val _hotTracksState = MutableStateFlow<HomeTracksState>(HomeTracksState.Loading)
    val hotTracksState: StateFlow<HomeTracksState> = _hotTracksState.asStateFlow()

    private val _discoverTracksState = MutableStateFlow<HomeTracksState>(HomeTracksState.Loading)
    val discoverTracksState: StateFlow<HomeTracksState> = _discoverTracksState.asStateFlow()

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val _myPlaylists = MutableStateFlow<List<PlaylistDto>>(emptyList())
    val myPlaylists: StateFlow<List<PlaylistDto>> = _myPlaylists.asStateFlow()

    private val _myPlaylistsLoadState = MutableStateFlow<MyPlaylistsLoadState>(MyPlaylistsLoadState.Loading)
    val myPlaylistsLoadState: StateFlow<MyPlaylistsLoadState> = _myPlaylistsLoadState.asStateFlow()

    private val _myTracksState = MutableStateFlow<MyTracksState>(MyTracksState.Idle)
    val myTracksState: StateFlow<MyTracksState> = _myTracksState.asStateFlow()

    private val _likedTracksState = MutableStateFlow<LikedTracksState>(LikedTracksState.Idle)
    val likedTracksState: StateFlow<LikedTracksState> = _likedTracksState.asStateFlow()

    private val _trackDetailState = MutableStateFlow<TrackDetailState>(TrackDetailState.Idle)
    val trackDetailState: StateFlow<TrackDetailState> = _trackDetailState.asStateFlow()

    private val _commentsState = MutableStateFlow<CommentsState>(CommentsState.Idle)
    val commentsState: StateFlow<CommentsState> = _commentsState.asStateFlow()

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    private val _playlistActionState = MutableStateFlow<PlaylistActionState>(PlaylistActionState.Idle)
    val playlistActionState: StateFlow<PlaylistActionState> = _playlistActionState.asStateFlow()

    init {
        loadMyGenerations()
    }

    fun scanLocalMusic() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, error = null)
            try {
                repository.scanLocalMusic()
                _uiState.value = _uiState.value.copy(isScanning = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isScanning = false, error = e.message)
            }
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            val auth = getAuthHeader()
            if (auth.isEmpty()) {
                _playlistActionState.value = PlaylistActionState.Error("请先登录")
                return@launch
            }
            _playlistActionState.value = PlaylistActionState.Loading
            try {
                val response = musicApi.createPlaylist(auth, mapOf("name" to name, "isPublic" to true))
                if (response.isSuccessful) {
                    _playlistActionState.value = PlaylistActionState.Success("歌单已创建")
                    loadMyPlaylists()
                } else {
                    _playlistActionState.value = PlaylistActionState.Error("创建歌单失败: ${response.code()}")
                }
            } catch (e: Exception) {
                _playlistActionState.value = PlaylistActionState.Error(e.message ?: "创建歌单失败")
            }
        }
    }

    fun createPlaylistAndAddTrack(name: String, trackId: Int) {
        viewModelScope.launch {
            val auth = getAuthHeader()
            if (auth.isEmpty()) {
                _playlistActionState.value = PlaylistActionState.Error("请先登录")
                return@launch
            }
            _playlistActionState.value = PlaylistActionState.Loading
            try {
                val createResponse = musicApi.createPlaylist(auth, mapOf("name" to name, "isPublic" to true))
                val playlistId = createResponse.body()?.get("id")
                if (createResponse.isSuccessful && playlistId != null) {
                    val addResponse = musicApi.addToPlaylist(auth, playlistId, mapOf("trackId" to trackId))
                    if (addResponse.isSuccessful) {
                        _playlistActionState.value = PlaylistActionState.Success("已新建歌单并加入歌曲")
                        loadPlaylistTracks(playlistId)
                        loadMyPlaylists()
                    } else {
                        _playlistActionState.value = PlaylistActionState.Error("歌单已创建，但加入歌曲失败: ${addResponse.code()}")
                        loadMyPlaylists()
                    }
                } else {
                    _playlistActionState.value = PlaylistActionState.Error("创建歌单失败: ${createResponse.code()}")
                }
            } catch (e: Exception) {
                _playlistActionState.value = PlaylistActionState.Error(e.message ?: "创建歌单失败")
            }
        }
    }

    fun generateMusic(prompt: String, bpm: Int?, duration: Int?, genre: String?) {
        val normalizedPrompt = prompt.trim()
        if (normalizedPrompt.length !in 1..500) {
            _uiState.value = _uiState.value.copy(error = "提示词需要在 1-500 字之间")
            return
        }
        if (duration != null && duration !in 5..120) {
            _uiState.value = _uiState.value.copy(error = "时长需要在 5-120 秒之间")
            return
        }
        if (bpm != null && bpm !in 40..220) {
            _uiState.value = _uiState.value.copy(error = "BPM 需要在 40-220 之间")
            return
        }

        pollingJob?.cancel()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                generationProgress = 0,
                generatedAudioUrl = null,
                currentGeneration = null,
                publishedTrackId = null,
                error = null
            )

            val result = repository.generateMusic(
                AIGenerationRequest(
                    prompt = normalizedPrompt,
                    genre = genre,
                    bpm = bpm,
                    durationSec = duration ?: 30
                )
            )
            result.fold(
                onSuccess = { response ->
                    handleGenerationResponse(response)
                    loadMyGenerations()
                    if (response.status == "PENDING" || response.status == "RUNNING") {
                        pollGenerationStatus(response.id)
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        error = error.message ?: "生成失败"
                    )
                }
            )
        }
    }

    private fun pollGenerationStatus(jobId: Int) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var attempts = 0
            while (attempts < Constants.MAX_POLLING_ATTEMPTS) {
                delay(2_000L)
                attempts++
                val result = repository.getGenerationStatus(jobId)
                result.fold(
                    onSuccess = { response ->
                        when (response.status) {
                            "SUCCEEDED", "PUBLISHED" -> {
                                handleGenerationResponse(response)
                                loadMyGenerations()
                                pollingJob?.cancel()
                                return@launch
                            }

                            "FAILED" -> {
                                _uiState.value = _uiState.value.copy(
                                    isGenerating = false,
                                    generationProgress = response.progress,
                                    currentGeneration = response,
                                    error = response.error ?: "AI 生成失败"
                                )
                                loadMyGenerations()
                                pollingJob?.cancel()
                                return@launch
                            }

                            else -> handleGenerationResponse(response)
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = "轮询状态失败: ${error.message}"
                        )
                    }
                )
            }
            _uiState.value = _uiState.value.copy(
                isGenerating = false,
                error = "生成超时，请稍后从任务历史恢复"
            )
        }
    }

    fun loadMyGenerations() {
        viewModelScope.launch {
            repository.getMyGenerations().fold(
                onSuccess = { jobs ->
                    _uiState.value = _uiState.value.copy(generationHistory = jobs)
                    val latest = jobs.firstOrNull()
                    if (latest != null && _uiState.value.currentGeneration == null) {
                        handleGenerationResponse(latest)
                        if (latest.status == "PENDING" || latest.status == "RUNNING") {
                            pollGenerationStatus(latest.id)
                        }
                    }
                },
                onFailure = { }
            )
        }
    }

    fun restoreGeneration(job: AIGenerationResponse) {
        handleGenerationResponse(job)
        if (job.status == "PENDING" || job.status == "RUNNING") {
            pollGenerationStatus(job.id)
        }
    }

    fun retryCurrentGeneration() {
        val job = _uiState.value.currentGeneration ?: return
        generateMusic(job.prompt, job.bpm, job.durationSec, job.genre)
    }

    fun publishGeneratedMusic(title: String?, description: String?) {
        val job = _uiState.value.currentGeneration
        if (job == null) {
            _uiState.value = _uiState.value.copy(error = "没有可发布的生成任务")
            return
        }
        if (job.status != "SUCCEEDED" && job.status != "PUBLISHED") {
            _uiState.value = _uiState.value.copy(error = "生成完成后才能发布")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPublishing = true, error = null)
            repository.publishGeneration(job.id, title, description).fold(
                onSuccess = { track ->
                    _uiState.value = _uiState.value.copy(
                        isPublishing = false,
                        publishedTrackId = track.id,
                        currentGeneration = job.copy(status = "PUBLISHED", trackId = track.id)
                    )
                    loadDiscoverTracks()
                    loadHotTracks()
                    loadMyGenerations()
                    loadMyTracks()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isPublishing = false,
                        error = error.message ?: "发布失败"
                    )
                }
            )
        }
    }

    private fun handleGenerationResponse(response: AIGenerationResponse) {
        _uiState.value = _uiState.value.copy(
            isGenerating = response.status == "PENDING" || response.status == "RUNNING",
            generationProgress = response.progress.coerceIn(0, 100),
            generatedAudioUrl = response.audioUrl,
            currentGeneration = response,
            publishedTrackId = response.trackId,
            error = response.error
        )
    }

    fun getSongById(id: Long) = viewModelScope.launch {
        repository.getSongById(id)
    }

    fun loadPlaylistTracks(playlistId: Int) {
        viewModelScope.launch {
            _playlistTracksState.value = PlaylistTracksState.Loading
            try {
                val response = musicApi.getPlaylistTracks(playlistId)
                if (response.isSuccessful && response.body() != null) {
                    _playlistTracksState.value = PlaylistTracksState.Success(response.body()!!)
                } else {
                    _playlistTracksState.value = PlaylistTracksState.Error("加载失败: ${response.code()}")
                }
            } catch (e: Exception) {
                _playlistTracksState.value = PlaylistTracksState.Error(e.message ?: "加载失败")
            }
        }
    }

    fun loadMyTracks(userId: Int? = securePreferences.getUserId()) {
        if (userId == null) {
            _myTracksState.value = MyTracksState.Error("请先登录")
            return
        }
        viewModelScope.launch {
            _myTracksState.value = MyTracksState.Loading
            repository.getMyPublishedTracks(userId).fold(
                onSuccess = { _myTracksState.value = MyTracksState.Success(it) },
                onFailure = { _myTracksState.value = MyTracksState.Error(it.message ?: "加载我的作品失败") }
            )
        }
    }

    fun loadLikedTracks() {
        val auth = getAuthHeader()
        if (auth.isEmpty()) {
            _likedTracksState.value = LikedTracksState.Error("请先登录")
            return
        }
        viewModelScope.launch {
            _likedTracksState.value = LikedTracksState.Loading
            try {
                val response = musicApi.getLikedTracks(auth)
                if (response.isSuccessful && response.body() != null) {
                    _likedTracksState.value = LikedTracksState.Success(response.body()!!)
                } else {
                    _likedTracksState.value = LikedTracksState.Error("加载收藏失败: ${response.code()}")
                }
            } catch (e: Exception) {
                _likedTracksState.value = LikedTracksState.Error(e.message ?: "加载收藏失败")
            }
        }
    }

    fun loadTrackDetail(trackId: Int) {
        viewModelScope.launch {
            _trackDetailState.value = TrackDetailState.Loading
            try {
                val response = musicApi.getTrack(trackId)
                if (response.isSuccessful && response.body() != null) {
                    _trackDetailState.value = TrackDetailState.Success(response.body()!!)
                } else {
                    _trackDetailState.value = TrackDetailState.Error("加载作品失败: ${response.code()}")
                }
            } catch (e: Exception) {
                _trackDetailState.value = TrackDetailState.Error(e.message ?: "加载作品失败")
            }
        }
    }

    fun loadComments(trackId: Int) {
        viewModelScope.launch {
            _commentsState.value = CommentsState.Loading
            try {
                val response = musicApi.getComments(trackId)
                if (response.isSuccessful && response.body() != null) {
                    _commentsState.value = CommentsState.Success(response.body()!!)
                } else {
                    _commentsState.value = CommentsState.Error("加载评论失败: ${response.code()}")
                }
            } catch (e: Exception) {
                _commentsState.value = CommentsState.Error(e.message ?: "加载评论失败")
            }
        }
    }

    fun postComment(trackId: Int, content: String) {
        val auth = getAuthHeader()
        if (auth.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "请先登录")
            return
        }
        if (content.trim().isBlank()) return
        viewModelScope.launch {
            try {
                val response = musicApi.postComment(auth, trackId, mapOf("content" to content.trim()))
                if (response.isSuccessful) {
                    loadComments(trackId)
                    loadTrackDetail(trackId)
                    loadDiscoverTracks()
                } else {
                    _uiState.value = _uiState.value.copy(error = "评论失败: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "评论失败")
            }
        }
    }

    fun loadHotTracks() {
        viewModelScope.launch {
            _hotTracksState.value = HomeTracksState.Loading
            try {
                val response = musicApi.hot()
                if (response.isSuccessful && response.body() != null) {
                    _hotTracksState.value = HomeTracksState.Success(response.body()!!)
                } else {
                    _hotTracksState.value = HomeTracksState.Error("加载失败: ${response.code()}")
                }
            } catch (e: Exception) {
                _hotTracksState.value = HomeTracksState.Error(e.message ?: "网络错误")
            }
        }
    }

    fun loadDiscoverTracks() {
        viewModelScope.launch {
            _discoverTracksState.value = HomeTracksState.Loading
            try {
                val response = musicApi.discover()
                if (response.isSuccessful && response.body() != null) {
                    _discoverTracksState.value = HomeTracksState.Success(response.body()!!)
                } else {
                    _discoverTracksState.value = HomeTracksState.Error("加载失败: ${response.code()}")
                }
            } catch (e: Exception) {
                _discoverTracksState.value = HomeTracksState.Error(e.message ?: "网络错误")
            }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _searchState.value = SearchState.Idle
                return@launch
            }
            _searchState.value = SearchState.Loading
            try {
                val response = musicApi.search(query)
                if (response.isSuccessful && response.body() != null) {
                    _searchState.value = SearchState.Success(response.body()!!)
                } else {
                    _searchState.value = SearchState.Error("搜索失败: ${response.code()}")
                }
            } catch (e: Exception) {
                _searchState.value = SearchState.Error(e.message ?: "网络错误")
            }
        }
    }

    fun loadMyPlaylists() {
        val auth = getAuthHeader()
        if (auth.isEmpty()) {
            _myPlaylistsLoadState.value = MyPlaylistsLoadState.Error("请先登录")
            return
        }
        viewModelScope.launch {
            _myPlaylistsLoadState.value = MyPlaylistsLoadState.Loading
            try {
                val response = musicApi.getMyPlaylists(auth)
                if (response.isSuccessful && response.body() != null) {
                    val list = response.body()!!
                    _myPlaylists.value = list
                    _myPlaylistsLoadState.value = MyPlaylistsLoadState.Success(list)
                } else {
                    _myPlaylistsLoadState.value = MyPlaylistsLoadState.Error("加载失败: ${response.code()}")
                }
            } catch (e: Exception) {
                _myPlaylistsLoadState.value = MyPlaylistsLoadState.Error(e.message ?: "网络错误")
            }
        }
    }

    fun toggleLike(trackId: Int) {
        val auth = getAuthHeader()
        if (auth.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "请先登录")
            return
        }
        viewModelScope.launch {
            try {
                musicApi.toggleLike(auth, trackId)
                loadTrackDetail(trackId)
                loadDiscoverTracks()
                loadHotTracks()
                loadLikedTracks()
            } catch (_: Exception) {
            }
        }
    }

    fun uploadMusic(uri: Uri, title: String, artist: String?, genre: String?, bpm: Int?, description: String?) {
        if (title.trim().isBlank()) {
            _uploadState.value = UploadState.Error("标题不能为空")
            return
        }
        viewModelScope.launch {
            _uploadState.value = UploadState.Loading
            repository.uploadMusicFile(uri, title.trim(), artist, genre, bpm, description).fold(
                onSuccess = { trackId ->
                    _uploadState.value = UploadState.Success(trackId)
                    loadMyTracks()
                    loadDiscoverTracks()
                    loadHotTracks()
                },
                onFailure = { _uploadState.value = UploadState.Error(it.message ?: "上传失败") }
            )
        }
    }

    fun resetUploadState() {
        _uploadState.value = UploadState.Idle
    }

    fun deletePlaylist(playlistId: Int) {
        val auth = getAuthHeader()
        if (auth.isEmpty()) {
            _playlistActionState.value = PlaylistActionState.Error("请先登录")
            return
        }
        viewModelScope.launch {
            _playlistActionState.value = PlaylistActionState.Loading
            try {
                val response = musicApi.deletePlaylist(auth, playlistId)
                if (response.isSuccessful) {
                    _playlistActionState.value = PlaylistActionState.Success("歌单已删除")
                    loadMyPlaylists()
                } else {
                    _playlistActionState.value = PlaylistActionState.Error("删除失败: ${response.code()}")
                }
            } catch (e: Exception) {
                _playlistActionState.value = PlaylistActionState.Error(e.message ?: "删除失败")
            }
        }
    }

    fun addTrackToPlaylist(playlistId: Int, trackId: Int) {
        val auth = getAuthHeader()
        if (auth.isEmpty()) {
            _playlistActionState.value = PlaylistActionState.Error("请先登录")
            return
        }
        viewModelScope.launch {
            _playlistActionState.value = PlaylistActionState.Loading
            try {
                val response = musicApi.addToPlaylist(auth, playlistId, mapOf("trackId" to trackId))
                if (response.isSuccessful) loadPlaylistTracks(playlistId)
                if (response.isSuccessful) {
                    _playlistActionState.value = PlaylistActionState.Success("已加入歌单")
                    loadMyPlaylists()
                } else {
                    _playlistActionState.value = PlaylistActionState.Error("加入歌单失败: ${response.code()}")
                }
            } catch (e: Exception) {
                _playlistActionState.value = PlaylistActionState.Error(e.message ?: "加入歌单失败")
            }
        }
    }

    fun removeTrackFromPlaylist(playlistId: Int, trackId: Int) {
        val auth = getAuthHeader()
        if (auth.isEmpty()) return
        viewModelScope.launch {
            try {
                musicApi.removeFromPlaylist(auth, playlistId, mapOf("trackId" to trackId))
                loadPlaylistTracks(playlistId)
                loadMyPlaylists()
            } catch (_: Exception) {
            }
        }
    }

    private fun getAuthHeader(): String {
        val token = securePreferences.getAuthToken() ?: return ""
        return "Bearer $token"
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}

data class MusicUiState(
    val isScanning: Boolean = false,
    val isGenerating: Boolean = false,
    val isPublishing: Boolean = false,
    val generationProgress: Int = 0,
    val generatedAudioUrl: String? = null,
    val currentGeneration: AIGenerationResponse? = null,
    val generationHistory: List<AIGenerationResponse> = emptyList(),
    val publishedTrackId: Int? = null,
    val error: String? = null
)
