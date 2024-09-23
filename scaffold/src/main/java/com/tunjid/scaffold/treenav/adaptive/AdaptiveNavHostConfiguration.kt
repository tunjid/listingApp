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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.tunjid.treenav.Node

@Stable
interface AdaptiveNavHostConfiguration<T, S : Node, R : Node> {

    val navigationState: S

    val currentNode: R

    fun configuration(node: R): AdaptiveNodeConfiguration<T, R>
}

@Composable
internal fun <T, R : Node> AdaptiveNavHostConfiguration<T, *, R>.Destination(
    paneScope: AdaptivePaneScope<T, R>
) {
    val current = remember(paneScope.paneState.currentNode) {
        paneScope.paneState.currentNode
    } ?: return
    with(configuration(current)) {
        paneScope.Render(current)
    }
}

@Composable
internal fun <T, R : Node> AdaptiveNavHostConfiguration<T, *, R>.paneMapping(): Map<T, R?> {
    val current = remember(currentNode) { currentNode }
    return current.let {
        configuration(it).paneMapping(it)
    }
}