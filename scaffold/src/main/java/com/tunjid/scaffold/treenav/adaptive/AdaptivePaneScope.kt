/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.treenav.adaptive

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tunjid.treenav.Node

/**
 * Scope for adaptive content that can show up in an arbitrary pane.
 */
@Stable
sealed interface AdaptivePaneScope<T, R : Node> : AnimatedVisibilityScope {

    val paneState: AdaptivePaneState<T, R>

    val isActive: Boolean
}

/**
 * Information about content in a pane
 */
@Stable
sealed interface AdaptivePaneState<T, R : Node> {
    val currentNode: R?
    val pane: T?
    val adaptation: Adaptation
}

/**
 * An implementation of [AdaptivePaneScope] that supports animations and shared elements
 */
@Stable
internal class AnimatedAdaptivePaneScope<T, R : Node>(
    paneState: AdaptivePaneState<T, R>,
    activeState: State<Boolean>,
    val animatedContentScope: AnimatedContentScope
) : AdaptivePaneScope<T, R>, AnimatedVisibilityScope by animatedContentScope {

    override var paneState by mutableStateOf(paneState)

    override val isActive: Boolean by activeState
}