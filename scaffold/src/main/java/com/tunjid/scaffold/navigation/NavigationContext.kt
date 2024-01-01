package com.tunjid.scaffold.navigation

import com.tunjid.scaffold.adaptive.AdaptiveRoute
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.strings.RouteParser

/**
 * provides a context for navigation actions, most commonly parsing a string route to a fully
 * type route.
 */
interface NavigationContext {
    val navState: MultiStackNav
    val String.toRoute: AdaptiveRoute
}

internal class ImmutableNavigationContext(
    private val state: MultiStackNav,
    private val routeParser: RouteParser<AdaptiveRoute>
) : NavigationContext {
    override val navState: MultiStackNav get() = state

    override val String.toRoute: AdaptiveRoute
        get() {
            return routeParser.parse(this) ?: UnknownRoute()
        }
}
