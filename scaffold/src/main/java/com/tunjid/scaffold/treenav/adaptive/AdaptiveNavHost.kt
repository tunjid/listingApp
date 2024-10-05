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

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tunjid.treenav.Node

/**
 * Creates a host for adaptive navigation for panes [Pane] and destinations [Destination].
 *
 * @param state the [AdaptiveNavHostState] producing the [AdaptiveNavHostScope] that provides
 * context about the panes in [AdaptiveNavHost].
 * @param modifier The modifier to be applied to the layout.
 * @param content [AdaptiveNavHostScope] receiving lambda allowing for placing each pane in its
 * appropriate slot.
 *
 */
@Composable
fun <Pane, Destination : Node> AdaptiveNavHost(
    state: AdaptiveNavHostState<Pane, Destination>,
    modifier: Modifier = Modifier,
    content: @Composable AdaptiveNavHostScope<Pane, Destination>.() -> Unit
) {
    Box(
        modifier = modifier
    ) {
        state.scope().content()
    }
}