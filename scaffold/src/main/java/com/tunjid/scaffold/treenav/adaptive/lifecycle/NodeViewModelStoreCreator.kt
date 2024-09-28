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

package com.tunjid.treenav.adaptive.lifecycle

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.tunjid.treenav.Node
import com.tunjid.treenav.Order
import com.tunjid.treenav.flatten

@Stable
internal class NodeViewModelStoreCreator(
    private val rootNodeProvider: () -> Node
) {
    private val nodeIdsToViewModelStoreOwner = mutableMapOf<String, ViewModelStoreOwner>()

    /**
     * Creates a [ViewModelStoreOwner] for a given [Node]
     */
    fun viewModelStoreOwnerFor(
        node: Node
    ): ViewModelStoreOwner = nodeIdsToViewModelStoreOwner.getOrPut(
        node.id
    ) {
        object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = ViewModelStore()
        }
    }

    fun clearStoreFor(childNode: Node) {
        val rootNode = rootNodeProvider()
        val existingNodeIds = rootNode.flatten(Order.BreadthFirst).mapTo(
            destination = mutableSetOf(),
            transform = Node::id
        )
        if (existingNodeIds.contains(childNode.id)) {
            return
        }
        nodeIdsToViewModelStoreOwner.remove(childNode.id)
            ?.viewModelStore
            ?.clear()
    }
}