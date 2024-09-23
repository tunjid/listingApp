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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tunjid.treenav.Node
import com.tunjid.treenav.adaptive.Adaptive.key

/**
 * Scope for adaptive content that can show up in an arbitrary [Pane]
 */
@Stable
sealed interface AdaptivePaneScope<T, R : Node> : AnimatedVisibilityScope {

    /**
     * Unique key to identify this scope
     */
    val key: String

    val paneState: AdaptivePaneState<T, R>
}

/**
 * Information about content in an [Adaptive.Pane]
 */
@Stable
sealed interface AdaptivePaneState<T, R : Node> {
    val currentNode: R?
    val previousNode: R?
    val pane: T?
    val adaptation: Adaptation
}

/**
 * An implementation of [AdaptivePaneScope] that supports animations and shared elements
 */
@Stable
internal class AnimatedAdaptivePaneScope<T, R : Node>(
    paneState: AdaptivePaneState<T, R>,
    val animatedContentScope: AnimatedContentScope
) : AdaptivePaneScope<T, R>, AnimatedVisibilityScope by animatedContentScope {

    override val key: String by derivedStateOf { paneState.key }

    override var paneState by mutableStateOf(paneState)
}