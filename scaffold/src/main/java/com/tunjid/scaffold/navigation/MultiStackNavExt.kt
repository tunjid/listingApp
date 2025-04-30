package com.tunjid.scaffold.navigation

import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.canPop
import com.tunjid.treenav.switch

val MultiStackNav.canGoUp get() = stacks.getOrNull(currentIndex)?.canPop == true

val MultiStackNav.navItems
    get() = stacks
        .map(StackNav::name)
        .mapIndexedNotNull { index, name ->
            val stack = AppStack.entries.firstOrNull {
                it.stackName == name
            } ?: return@mapIndexedNotNull null
            NavItem(
                stack = stack,
                index = index,
                selected = currentIndex == index,
            )
        }

fun MultiStackNav.navItemSelected(item: NavItem) =
    if (item.selected) popToRoot(indexToPop = item.index)
    else switch(toIndex = item.index)

private fun MultiStackNav.popToRoot(indexToPop: Int) = copy(
    stacks = stacks.mapIndexed { index: Int, stackNav: StackNav ->
        if (index == indexToPop) stackNav.popToRoot()
        else stackNav
    }
)

private fun StackNav.popToRoot() = copy(
    children = children.take(1)
)

private val EmptyNavigationState = MultiStackNav(
    name = "App",
    currentIndex = 0,
    stacks = listOf(),
)
