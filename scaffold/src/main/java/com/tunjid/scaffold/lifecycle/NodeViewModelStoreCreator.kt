package com.tunjid.scaffold.lifecycle

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
        println("Clearing VM for $childNode")
        val owner = nodeIdsToViewModelStoreOwner.remove(childNode.id)
        owner?.viewModelStore?.clear()
    }
}