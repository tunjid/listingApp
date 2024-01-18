package com.tunjid.scaffold.navigation

import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteParser

/**
 * provides a context for navigation actions, most commonly parsing a string route to a fully
 * type route.
 */
interface NavigationContext {
    val navState: MultiStackNav
    val String.toRoute: Route
}

internal class ImmutableNavigationContext(
    private val state: MultiStackNav,
    private val routeParser: RouteParser<Route>
) : NavigationContext {
    override val navState: MultiStackNav get() = state

    override val String.toRoute: Route
        get() {
            return routeParser.parse(this) ?: UnknownRoute()
        }
}
