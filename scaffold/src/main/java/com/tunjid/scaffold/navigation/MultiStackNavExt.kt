package com.tunjid.scaffold.navigation

import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.canPop
import com.tunjid.treenav.current
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.routeOf
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

/**
 * Adds the following query parameters to the current [Node] if it is a [Route]
 */
fun MultiStackNav.editCurrentIfRoute(
    vararg additions: Pair<String, List<String>>
): MultiStackNav = when (val top = current) {
    is Route -> copy(
        stacks = stacks.mapIndexed { index, stack ->
            if (index == currentIndex) stack.copy(
                children = stack.children.dropLast(1) + routeOf(
                    params = RouteParams(
                        pathAndQueries = top.routeParams.pathAndQueries,
                        pathArgs = top.routeParams.pathArgs,
                        queryParams = top.routeParams.queryParams + additions,
                    ),
                    children = top.children
                )
            )
            else stack
        }
    )

    else -> this
}


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
