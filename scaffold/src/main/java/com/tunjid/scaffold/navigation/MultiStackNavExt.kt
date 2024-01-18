package com.tunjid.scaffold.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AirplaneTicket
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.canPop
import com.tunjid.treenav.minus
import com.tunjid.treenav.strings.Route
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
                name = name.replace(
                    oldValue = "/",
                    newValue = ""
                ),
                icon = when {
                    "listings" in name -> Icons.Filled.Apartment
                    "favorites" in name -> Icons.Filled.FavoriteBorder
                    "trips" in name -> Icons.AutoMirrored.Filled.AirplaneTicket
                    "messages" in name -> Icons.Filled.ChatBubbleOutline
                    "profile" in name -> Icons.Filled.AccountCircle
                    else -> Icons.Filled.MoreVert
                },
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
fun Flow<MultiStackNav>.removedRoutes(): Flow<List<Route>> =
    distinctUntilChanged()
        .scan(initial = EmptyNavigationState to EmptyNavigationState) { navPair, newNav ->
            navPair.copy(first = navPair.second, second = newNav)
        }
        .map { (prevNav: MultiStackNav, currentNav: MultiStackNav) ->
            (prevNav - currentNav).filterIsInstance<Route>()
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
