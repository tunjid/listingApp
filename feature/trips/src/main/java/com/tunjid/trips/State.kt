package com.tunjid.trips

import com.tunjid.scaffold.ByteSerializable
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

sealed class Action(val key: String)

@Serializable
data class State(
    @Transient
    val currentlyPlayingKey: Any? = null,
    @Transient
    val videos: List<String> = VideoUrls
) : ByteSerializable

private val VideoUrls = listOf(
    "https://videos.pexels.com/video-files/8419938/8419938-sd_360_640_30fps.mp4",
    "https://videos.pexels.com/video-files/6514400/6514400-hd_1080_1920_30fps.mp4",
    "https://videos.pexels.com/video-files/6092617/6092617-sd_360_640_30fps.mp4",
    "https://videos.pexels.com/video-files/26746611/11999512_1440_2560_60fps.mp4",
    "https://videos.pexels.com/video-files/6242776/6242776-hd_1080_1920_30fps.mp4",
    "https://videos.pexels.com/video-files/10452187/10452187-sd_360_640_30fps.mp4",
    "https://videos.pexels.com/video-files/7279186/7279186-sd_360_640_30fps.mp4",
    "https://videos.pexels.com/video-files/10839395/10839395-sd_360_640_30fps.mp4",
    "https://videos.pexels.com/video-files/26591103/11967516_360_640_30fps.mp4",
    "https://videos.pexels.com/video-files/6624966/6624966-sd_360_640_22fps.mp4",
//    "",
//    "",
//    "",
//    "",
)
