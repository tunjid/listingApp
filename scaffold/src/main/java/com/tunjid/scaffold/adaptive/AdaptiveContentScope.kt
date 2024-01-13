package com.tunjid.scaffold.adaptive

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.tunjid.scaffold.adaptive.Adaptive.key

/**
 * An implementation of [Adaptive.ContainerScope] that supports animations and shared elements
 */
@Stable
internal class AnimatedAdaptiveContentScope(
    containerState: Adaptive.ContainerState,
    val adaptiveContentHost: AdaptiveContentHost,
    val animatedContentScope: AnimatedContentScope
) : Adaptive.ContainerScope, AnimatedVisibilityScope by animatedContentScope {

    override val key: String by derivedStateOf { containerState.key }

    override var containerState by mutableStateOf(containerState)

    override fun isCurrentlyShared(key: Any): Boolean =
        adaptiveContentHost.isCurrentlyShared(key)

    @Composable
    override fun <T> rememberSharedElement(
        key: Any,
        sharedElement: @Composable (T, Modifier) -> Unit
    ): @Composable (T, Modifier) -> Unit {
        val currentNavigationState = adaptiveContentHost.adaptedState
        // This container state may be animating out. Look up the actual current route
        val currentRouteInContainer = containerState.container?.let(
            currentNavigationState::routeFor
        )
        val isCurrentlyAnimatingIn = currentRouteInContainer?.id == containerState.currentRoute?.id

        // Do not use the shared element if this content is being animated out
        if (!isCurrentlyAnimatingIn) return sharedElement

        return adaptiveContentHost.createOrUpdateSharedElement(
            key = key,
            sharedElement = sharedElement
        )
    }
}

/**
 * Creates a shared element between composables
 * @param key the key for the shared element
 * @param sharedElement the element to be shared
 */
@Composable
fun <T> rememberSharedElement(
    key: Any,
    sharedElement: @Composable (T, Modifier) -> Unit
): @Composable (T, Modifier) -> Unit =
    when (val scope = LocalAdaptiveContentScope.current) {
        null -> throw IllegalArgumentException(
            "This may only be called from an adaptive content scope"
        )

        else -> when (scope.containerState.container) {
            null -> throw IllegalArgumentException(
                "Shared elements may only be used in non null containers"
            )
            // Allow shared elements in the primary or transient primary content only
            Adaptive.Container.Primary -> when {
                // Show a blank space for shared elements between the destinations
                scope.isInPreview && scope.isCurrentlyShared(key) -> { _, modifier ->
                    Box(modifier)
                }
                // If previewing and it won't be shared, show the item as is
                scope.isInPreview -> sharedElement
                // Share the element
                else -> scope.rememberSharedElement(
                    key = key,
                    sharedElement = sharedElement
                )
            }
            // Share the element when in the transient container
            Adaptive.Container.TransientPrimary -> scope.rememberSharedElement(
                key = key,
                sharedElement = sharedElement
            )
            // In the secondary container use the element as is
            Adaptive.Container.Secondary -> sharedElement
        }
    }

internal val LocalAdaptiveContentScope = staticCompositionLocalOf<Adaptive.ContainerScope?> {
    null
}

internal val Adaptive.ContainerScope.isInPreview: Boolean
    get() = containerState.container == Adaptive.Container.Primary
            && containerState.adaptation == Adaptive.Adaptation.PrimaryToTransient