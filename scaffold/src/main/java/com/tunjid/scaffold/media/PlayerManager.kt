package com.tunjid.scaffold.media

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import javax.inject.Inject
import javax.inject.Singleton

interface PlayerManager {

    val currentUrl: String?

    fun enqueue(url: String)

    fun play(url: String)

    fun pause()

    fun stateFor(url: String): VideoState
}


@Stable
sealed class PlayerStatus {

    sealed class Idle : PlayerStatus() {
        data object Initial : Idle()
        data object Evicted : Idle()
    }

    sealed class Play : PlayerStatus() {
        data object Requested : Play()
        data object Confirmed : Play()
    }

    sealed class Pause : PlayerStatus() {
        data object Requested : Pause()
        data object Confirmed : Pause()
    }
}

@Stable
class VideoState(
    val url: String
) {
    var alignment by mutableStateOf(Alignment.Center)
    var contentScale by mutableStateOf(ContentScale.Crop)
    var status by mutableStateOf<PlayerStatus>(PlayerStatus.Idle.Initial)
    var renderedFirstFrame by mutableStateOf(false)
    var videoSize by mutableStateOf(IntSize.Zero)
    var playerPosition by mutableLongStateOf(0L)
    var player by mutableStateOf<Player?>(
        value = null,
        policy = referentialEqualityPolicy()
    )

    var videoStill by mutableStateOf<ImageBitmap?>(
        value = null,
        policy = referentialEqualityPolicy()
    )

    internal val playerListener = object : Player.Listener {
        override fun onVideoSizeChanged(size: VideoSize) {
            updateVideoSize(size)
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            status = when {
                playWhenReady && player?.playbackState == Player.STATE_READY -> PlayerStatus.Play.Confirmed.also { player?.play() }
                playWhenReady -> PlayerStatus.Play.Requested
                status == PlayerStatus.Idle.Initial -> status
                else -> PlayerStatus.Pause.Confirmed
            }
            player?.videoSize?.let(::updateVideoSize)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            status = when {
                playbackState == Player.STATE_READY && player?.playWhenReady == true -> PlayerStatus.Play.Confirmed
                player?.playWhenReady == true -> PlayerStatus.Play.Requested
                else -> status
            }
            player?.videoSize?.let(::updateVideoSize)
        }

        override fun onRenderedFirstFrame() {
            renderedFirstFrame = true
            player?.videoSize?.let(::updateVideoSize)
        }
    }

    private fun updateVideoSize(size: VideoSize) {
        videoSize = when (val intSize = size.toIntSize()) {
            IntSize.Zero -> videoSize
            else -> intSize
        }
    }
}

val VideoState.canShowVideo
    get() = when (status) {
        is PlayerStatus.Idle.Initial -> true
        is PlayerStatus.Play -> true
        is PlayerStatus.Pause -> true
        PlayerStatus.Idle.Evicted -> false
    }

val VideoState.canShowStill
    get() = videoSize == IntSize.Zero
            || !renderedFirstFrame
            || when (status) {
        is PlayerStatus.Idle -> true
        is PlayerStatus.Pause -> false
        PlayerStatus.Play.Requested -> true
        PlayerStatus.Play.Confirmed -> false
    }

@Stable
@Singleton
class ExoPlayerManager @Inject constructor(
    @ApplicationContext context: Context,
    appScope: CoroutineScope,
) : PlayerManager {
    @SuppressLint("UnsafeOptInUsageError")
    private val singletonPlayer = ExoPlayer.Builder(context)
        .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
        .build()
        .apply {
            repeatMode = REPEAT_MODE_ONE
            playWhenReady = true
        }

    override var currentUrl: String? by mutableStateOf(null)

    // TODO: Make this a snapshot observable LRU cache
    private val urlToStates = mutableStateMapOf<String?, VideoState>()

    init {
        // Pause playback when nothing is visible to play
        snapshotFlow { urlToStates.values }
            .flatMapLatest { states ->
                combine(
                    flows = states.map {
                        snapshotFlow(it::status)
                    },
                    transform = { allStatuses ->
                        allStatuses.all { it is PlayerStatus.Idle }
                    }
                )
            }
            .filter(true::equals)
            .onEach { pause() }
            .launchIn(appScope + Dispatchers.Main)
    }

    override fun enqueue(url: String) {
        if (urlToStates.contains(url)) return
        urlToStates[url] = VideoState(url)
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
        urlToStates.getOrPut(url) {
            VideoState(url)
        }.apply state@{
            check(player == null) {
                "Calling play with player attached"
            }
            renderedFirstFrame = false
            status = when (val currentStatus = status) {
                PlayerStatus.Idle.Initial,
                is PlayerStatus.Pause -> PlayerStatus.Play.Requested

                PlayerStatus.Idle.Evicted,
                is PlayerStatus.Play -> currentStatus
            }
            player = singletonPlayer.apply {
                urlToStates[previousUrl]?.playerListener?.let(::removeListener)
                addListener(this@state.playerListener)
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

    override fun stateFor(
        url: String
    ): VideoState {
        enqueue(url)
        return urlToStates.getValue(url)
    }
}

private fun VideoSize.toIntSize() = IntSize(width, height)