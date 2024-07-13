package com.tunjid.explore.grid

import com.tunjid.scaffold.ByteSerializable
import com.tunjid.scaffold.media.VideoState
import com.tunjid.scaffold.navigation.NavigationAction
import com.tunjid.scaffold.navigation.NavigationMutation
import com.tunjid.treenav.push
import com.tunjid.treenav.strings.routeString
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

sealed class Action(val key: String) {

    data class Play(
        val url: String
    ): Action("Play")

    sealed class Navigation : Action("Navigation"), NavigationAction {
        data class FullScreen(
            val url: String,
        ) : Navigation() {
            override val navigationMutation: NavigationMutation = {
                navState.push(
                    routeString(
                        path = "/explore/pager",
                        queryParams = mapOf(
                            "url" to listOf(url)
                        )
                    ).toRoute
                )
            }
        }
    }
}

@Serializable
data class State(
    @Transient
    val currentlyPlayingKey: Any? = null,
    @Transient
    val items: List<VideoItem> = emptyList(),
) : ByteSerializable

internal val VideoUrls = listOf(
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
    "https://videos.pexels.com/video-files/7008030/7008030-sd_360_640_25fps.mp4",
    "https://videos.pexels.com/video-files/6328336/6328336-sd_506_960_25fps.mp4",
    "https://videos.pexels.com/video-files/4441055/4441055-sd_360_640_25fps.mp4",
    "https://videos.pexels.com/video-files/5057524/5057524-sd_360_640_25fps.mp4",
    "https://videos.pexels.com/video-files/5669810/5669810-sd_360_640_30fps.mp4",
    "https://videos.pexels.com/video-files/6466668/6466668-sd_360_640_30fps.mp4",
//    "https://videos.pexels.com/video-files/6514400/6514400-hd_1080_1920_30fps.mp4",
//    "https://videos.pexels.com/video-files/6242776/6242776-hd_1080_1920_30fps.mp4",
//    "https://videos.pexels.com/video-files/26746611/11999512_1440_2560_60fps.mp4",
)


data class VideoItem(
    val state: VideoState,
)

val VideoItem.key get() = state.url
