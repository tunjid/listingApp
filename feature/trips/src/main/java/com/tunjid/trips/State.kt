package com.tunjid.trips

import androidx.compose.ui.layout.ContentScale
import com.tunjid.scaffold.ByteSerializable
import com.tunjid.scaffold.media.PhotoArgs
import com.tunjid.scaffold.media.VideoArgs
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

sealed class Action(val key: String) {
    data class TogglePlaying(
        val url: String?,
    ) : Action("TogglePlaying")
}

@Serializable
data class State(
    @Transient
    val currentlyPlayingKey: Any? = null,
    @Transient
    val videos: List<VideoItem> = VideoUrls.map { url ->
        VideoItem(
            args = VideoArgs(
                url = url,
                contentScale = ContentScale.Crop,
            ),
        )
    }
) : ByteSerializable

private val VideoUrls = listOf(
    "https://videos.pexels.com/video-files/8419938/8419938-sd_360_640_30fps.mp4",
    "https://videos.pexels.com/video-files/6092617/6092617-sd_360_640_30fps.mp4",
    "https://videos.pexels.com/video-files/10452187/10452187-sd_360_640_30fps.mp4",
    "https://videos.pexels.com/video-files/7279186/7279186-sd_360_640_30fps.mp4",
    "https://videos.pexels.com/video-files/10839395/10839395-sd_360_640_30fps.mp4",
    "https://videos.pexels.com/video-files/5057325/5057325-sd_360_640_25fps.mp4",
    "https://videos.pexels.com/video-files/6242776/6242776-sd_360_640_30fps.mp4",
    "https://videos.pexels.com/video-files/5329613/5329613-sd_506_960_25fps.mp4",
    "https://videos.pexels.com/video-files/5107658/5107658-sd_360_640_30fps.mp4",
    "https://videos.pexels.com/video-files/5827627/5827627-sd_360_640_24fps.mp4",
    "https://videos.pexels.com/video-files/5992474/5992474-sd_360_640_30fps.mp4",
    "https://videos.pexels.com/video-files/5824033/5824033-sd_360_640_24fps.mp4",
    "https://videos.pexels.com/video-files/13658590/13658590-sd_360_640_24fps.mp4",
    "https://videos.pexels.com/video-files/7515918/7515918-sd_360_640_30fps.mp4",
//    "https://videos.pexels.com/video-files/6514400/6514400-hd_1080_1920_30fps.mp4",
//    "https://videos.pexels.com/video-files/6242776/6242776-hd_1080_1920_30fps.mp4",
//    "https://videos.pexels.com/video-files/26746611/11999512_1440_2560_60fps.mp4",
//    "",
//    "",
//    "",
//    "",
)


data class VideoItem(
    val args: VideoArgs,
)

val VideoItem.key get() = args.url
