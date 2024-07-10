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

    sealed class Idle : PlayerStatus() {
        data object Initial : Idle()
        data object Evicted : Idle()
    }

    sealed class Play : PlayerStatus() {
        data object PlayRequested : Play()
        data object Playing : Play()
    }

    sealed class Pause : PlayerStatus() {
        data object Requested : Pause()
        data object Confirmed : Pause()
    }
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
    var renderedFirstFrame by mutableStateOf(false)
    var videoSize by mutableStateOf(IntSize.Zero)
    var status by mutableStateOf<PlayerStatus>(PlayerStatus.Idle.Initial)
    var playerPosition by mutableLongStateOf(0L)
    var contentScale by mutableStateOf(ContentScale.Crop)
    var alignment by mutableStateOf(Alignment.Center)

    override fun onVideoSizeChanged(size: VideoSize) {
        updateVideoSize(size)
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        status = when {
            playWhenReady && player?.playbackState == Player.STATE_READY -> PlayerStatus.Play.Playing.also { player?.play() }
            playWhenReady -> PlayerStatus.Play.PlayRequested
            status == PlayerStatus.Idle.Initial -> status
            else -> PlayerStatus.Pause.Confirmed
        }
        player?.videoSize?.let(::updateVideoSize)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        status = when {
            playbackState == Player.STATE_READY && player?.playWhenReady == true -> PlayerStatus.Play.Playing
            player?.playWhenReady == true -> PlayerStatus.Play.PlayRequested
            else -> status
        }
        player?.videoSize?.let(::updateVideoSize)
    }

    override fun onRenderedFirstFrame() {
        renderedFirstFrame = true
    }

    private fun updateVideoSize(size: VideoSize) {
        videoSize = when (val intSize = size.toIntSize()) {
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
            status = PlayerStatus.Pause.Requested
            player?.apply {
                setVideoSurface(null)
                playerPosition = currentPosition
            }
            player = null
        }
        urlToStates.getOrPut(url, ::PlayerState).apply state@{
            check(player == null) {
                "Calling play with player attached"
            }
            renderedFirstFrame = false
            status = when (val currentStatus = status) {
                PlayerStatus.Idle.Evicted -> throw IllegalStateException(
                    "Attempt to play evicted player"
                )

                PlayerStatus.Idle.Initial,
                PlayerStatus.Pause.Requested,
                PlayerStatus.Pause.Confirmed -> PlayerStatus.Play.PlayRequested

                PlayerStatus.Play.PlayRequested,
                PlayerStatus.Play.Playing -> currentStatus
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
            status = PlayerStatus.Pause.Requested
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