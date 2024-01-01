package com.tunjid.scaffold

import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tunjid.scaffold.adaptive.AdaptiveRoute
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.current
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

infix fun Dp.countIf(condition: Boolean) = if (condition) this else 0.dp

/**
 * Updates [State] with whether it is the primary navigation container
 */
fun <State> StateFlow<MultiStackNav>.isInPrimaryNavMutations(
    route: AdaptiveRoute,
    mutation: State.(Boolean) -> State,
): Flow<Mutation<State>> =
    map { route.id == it.current?.id }
        .distinctUntilChanged()
        .mapToMutation { isInPrimaryNav ->
            mutation(isInPrimaryNav)
        }

internal fun <T> adaptiveSpringSpec(visibilityThreshold: T) = spring(
    dampingRatio = 0.8f,
    stiffness = 600f,
    visibilityThreshold = visibilityThreshold
)
