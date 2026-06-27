package com.example.myfirstapp.ui.viewmodel

import android.app.Application
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.myfirstapp.data.local.SongEntity
import com.example.myfirstapp.data.security.SecurePreferences
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

enum class PlayMode {
    NORMAL, REPEAT_ONE, SHUFFLE
}

data class PlayerState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val currentSong: SongEntity? = null,
    val playQueue: List<SongEntity> = emptyList(),
    val currentIndex: Int = -1,
    val playMode: PlayMode = PlayMode.NORMAL,
    val isShuffled: Boolean = false,
    val repeatMode: Int = 0
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val sessionToken: SessionToken,
    application: Application
) : AndroidViewModel(application) {

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var isConnecting = false

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateCurrentSongInfo()
            _playerState.value = _playerState.value.copy(
                currentIndex = mediaController?.currentMediaItemIndex ?: -1
            )
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                _playerState.value = _playerState.value.copy(
                    duration = (mediaController?.duration ?: 0L).coerceAtLeast(0L)
                )
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            _playerState.value = _playerState.value.copy(currentPosition = newPosition.positionMs)
        }
    }

    fun ensureController() {
        if (mediaController != null || isConnecting) return
        isConnecting = true
        val app = getApplication<Application>()
        try {
            controllerFuture = MediaController.Builder(app, sessionToken).buildAsync()
            controllerFuture?.addListener({
                try {
                    val controller = controllerFuture?.get()
                    if (controller != null) {
                        mediaController = controller
                        controller.addListener(playerListener)
                        _playerState.value = _playerState.value.copy(
                            isPlaying = controller.isPlaying,
                            duration = controller.duration.coerceAtLeast(0L)
                        )
                    }
                    isConnecting = false
                } catch (e: Exception) {
                    isConnecting = false
                }
            }, ContextCompat.getMainExecutor(app))
        } catch (_: Exception) {
            isConnecting = false
        }
    }

    private fun updateCurrentSongInfo() {
        val item = mediaController?.currentMediaItem ?: return
        val metadata = item.mediaMetadata
        val song = SongEntity(
            id = try { item.mediaId.toLong() } catch (_: NumberFormatException) { 0L },
            title = metadata.title?.toString() ?: "未知歌曲",
            artist = metadata.artist?.toString() ?: "未知艺术家",
            album = metadata.albumTitle?.toString(),
            duration = _playerState.value.duration,
            uri = item.mediaMetadata.extras?.getString("uri")
                ?: item.localConfiguration?.uri.toString()
        )
        _playerState.value = _playerState.value.copy(currentSong = song)
    }

    fun playSong(song: SongEntity) {
        ensureController()
        val controller = mediaController ?: run {
            val app = getApplication<Application>()
            controllerFuture?.addListener({ playSong(song) }, ContextCompat.getMainExecutor(app))
            return
        }

        val extras = Bundle().apply {
            putLong("duration", song.duration)
            putString("uri", song.uri)
        }
        val mediaItem = MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setExtras(extras)
                    .build()
            )
            .build()

        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.play()
        _playerState.value = _playerState.value.copy(
            currentSong = song,
            currentIndex = 0,
            playQueue = listOf(song)
        )
    }

    fun playUrl(url: String, title: String, artist: String) {
        ensureController()
        val controller = mediaController ?: run {
            val app = getApplication<Application>()
            controllerFuture?.addListener({ playUrl(url, title, artist) }, ContextCompat.getMainExecutor(app))
            return
        }

        val mediaItem = MediaItem.Builder()
            .setMediaId("remote_${System.currentTimeMillis()}")
            .setUri(url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .build()
            )
            .build()

        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.play()
        _playerState.value = _playerState.value.copy(
            currentSong = SongEntity(
                id = 0L,
                title = title,
                artist = artist,
                album = null,
                duration = 0L,
                uri = url
            )
        )
    }

    fun togglePlayPause() {
        val controller = mediaController ?: run {
            ensureController()
            return
        }
        if (controller.isPlaying) controller.pause()
        else controller.play()
    }

    fun skipToNext() {
        mediaController?.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        mediaController?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _playerState.value = _playerState.value.copy(currentPosition = positionMs)
    }

    fun toggleShuffle() {
        _playerState.value = _playerState.value.copy(isShuffled = !_playerState.value.isShuffled)
    }

    fun toggleRepeat() {
        _playerState.value = _playerState.value.copy(repeatMode = (_playerState.value.repeatMode + 1) % 3)
    }

    override fun onCleared() {
        super.onCleared()
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { future ->
            if (!future.isDone) future.cancel(true)
        }
        mediaController?.release()
        mediaController = null
    }
}
