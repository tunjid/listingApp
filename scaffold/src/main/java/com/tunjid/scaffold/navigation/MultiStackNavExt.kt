package com.tunjid.scaffold.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import com.tunjid.scaffold.adaptive.AdaptiveRoute
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.canPop
import com.tunjid.treenav.minus
import com.tunjid.treenav.switch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

val MultiStackNav.canGoUp get() = stacks.getOrNull(currentIndex)?.canPop == true

val MultiStackNav.navItems
    get() = stacks
        .map(StackNav::name)
        .mapIndexed { index, name ->
            NavItem(
                name = name,
                icon = Icons.Default.Settings,
                index = index,
                selected = currentIndex == index,
            )
        }

fun MultiStackNav.navItemSelected(item: NavItem) =
    if (item.selected) popToRoot(indexToPop = item.index)
    else switch(toIndex = item.index)

/**
 * Route diff between consecutive emissions of [MultiStackNav]
 */
fun Flow<MultiStackNav>.removedRoutes(): Flow<List<AdaptiveRoute>> =
    distinctUntilChanged()
        .scan(initial = EmptyNavigationState to EmptyNavigationState) { navPair, newNav ->
            navPair.copy(first = navPair.second, second = newNav)
        }
        .map { (prevNav: MultiStackNav, currentNav: MultiStackNav) ->
            (prevNav - currentNav).filterIsInstance<AdaptiveRoute>()
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
