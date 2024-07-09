package com.tunjid.scaffold.media

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject


@Stable
sealed class PlayerStatus {

    data object Initial : PlayerStatus()

    data object PauseRequested : PlayerStatus()

    data object Paused : PlayerStatus()

    data object PlayRequested : PlayerStatus()

    data object Playing : PlayerStatus()

    data object Evicted : PlayerStatus()

}

interface PlayerManager {

    fun enqueue(url: String)

    fun play(url: String)

    fun pause()

    @Composable
    fun stateFor(url: String?): PlayerState
}

@Stable
class PlayerState : Player.Listener {
    var player by mutableStateOf<Player?>(
        value = null,
        policy = referentialEqualityPolicy()
    )
    var videoStill by mutableStateOf<ImageBitmap?>(
        value = null,
        policy = referentialEqualityPolicy()
    )
    var videoSize by mutableStateOf(IntSize.Zero)
    var status by mutableStateOf<PlayerStatus>(PlayerStatus.Initial)
    var playerPosition by mutableLongStateOf(0L)
    var contentScale by mutableStateOf(ContentScale.Crop)
    var alignment by mutableStateOf(Alignment.Center)

    override fun onVideoSizeChanged(size: VideoSize) {
        updateVideoSize(size)
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        status = when {
            playWhenReady && player?.playbackState == Player.STATE_READY -> PlayerStatus.Playing.also { player?.play() }
            playWhenReady -> PlayerStatus.PlayRequested
            status == PlayerStatus.Initial -> status
            else -> PlayerStatus.Paused
        }
        player?.videoSize?.let(::updateVideoSize)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        status = when {
            playbackState == Player.STATE_READY && player?.playWhenReady == true -> PlayerStatus.Playing
            player?.playWhenReady == true -> PlayerStatus.PlayRequested
            else -> status
        }
        player?.videoSize?.let(::updateVideoSize)
    }

    private fun updateVideoSize(size: VideoSize) {
        videoSize =  when (val intSize = size.toIntSize()) {
            IntSize.Zero -> videoSize
            else -> intSize
        }
    }
}

@Stable
class ExoPlayerManager @Inject constructor(
    @ApplicationContext context: Context
) : PlayerManager {
    @SuppressLint("UnsafeOptInUsageError")
    private val singletonPlayer = ExoPlayer.Builder(context)
        .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
        .build()
        .apply {
            repeatMode = REPEAT_MODE_ONE
            playWhenReady = true
        }

    private var currentUrl: String? by mutableStateOf(null)

    private val items = mutableSetOf<String>()

    private val urlToStates = mutableStateMapOf<String?, PlayerState>()

    override fun enqueue(url: String) {
        if (items.contains(url)) return
        items.add(url)
        singletonPlayer.addMediaItem(
            MediaItem.Builder()
                .setUri(url)
                .setMediaId(url)
                .build()
        )
    }

    override fun play(url: String) {
        if (currentUrl == url) {
            return
        }
        val previousUrl = currentUrl
        currentUrl = url

        urlToStates[previousUrl]?.apply {
            status = PlayerStatus.PauseRequested
            player?.apply {
                setVideoSurface(null)
                playerPosition = currentPosition
            }
            player = null
        }
        urlToStates.getOrPut(url, ::PlayerState).apply state@{
            status = when (val currentStatus = status) {
                PlayerStatus.Evicted -> throw IllegalStateException(
                    "Attempt to play evicted player"
                )

                PlayerStatus.Initial,
                PlayerStatus.PauseRequested,
                PlayerStatus.Paused -> PlayerStatus.PlayRequested

                PlayerStatus.PlayRequested,
                PlayerStatus.Playing -> currentStatus
            }
            player = singletonPlayer.apply {
                urlToStates[previousUrl]?.let(::removeListener)
                addListener(this@state)
                enqueue(url)

                singletonPlayer.seekTo(
                    /* mediaItemIndex = */ (0..<singletonPlayer.mediaItemCount)
                        .map(singletonPlayer::getMediaItemAt)
                        .indexOfFirst { it.mediaId == url },
                    /* positionMs = */ playerPosition
                )
                prepare()
                play()
            }
        }
    }

    override fun pause() {
        currentUrl?.let(urlToStates::get)?.apply {
            status = PlayerStatus.PauseRequested
        }
        singletonPlayer.pause()
    }

    @Composable
    override fun stateFor(
        url: String?
    ): PlayerState = urlToStates.getOrPut(url, ::PlayerState)
}

val LocalPlayerManager = staticCompositionLocalOf<PlayerManager> {
    TODO()
}

private fun VideoSize.toIntSize() = IntSize(width, height)