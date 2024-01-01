package com.tunjid.scaffold.scaffold

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunjid.scaffold.countIf
import com.tunjid.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.globalui.navRailWidth
import com.tunjid.scaffold.globalui.slices.routeContainerState
import com.tunjid.scaffold.globalui.toolbarSize
import com.tunjid.scaffold.lifecycle.mappedCollectAsStateWithLifecycle
import com.tunjid.scaffold.navigation.NavItem
import com.tunjid.scaffold.navigation.NavigationStateHolder
import com.tunjid.scaffold.navigation.navItemSelected
import com.tunjid.scaffold.navigation.navItems
import com.tunjid.treenav.MultiStackNav

/**
 * Motionally intelligent nav rail shared amongst nav routes in the app
 */
@Composable
internal fun AppNavRail(
    globalUiStateHolder: GlobalUiStateHolder,
    navStateHolder: NavigationStateHolder,
) {
    val containerState by globalUiStateHolder.state.mappedCollectAsStateWithLifecycle(
        mapper = UiState::routeContainerState
    )
    val windowSizeClass by globalUiStateHolder.state.mappedCollectAsStateWithLifecycle(
        mapper = UiState::windowSizeClass
    )

    val navItems by navStateHolder.state.mappedCollectAsStateWithLifecycle(
        mapper = MultiStackNav::navItems
    )

    val statusBarSize = with(LocalDensity.current) {
        containerState.statusBarSize.toDp()
    } countIf containerState.insetDescriptor.hasTopInset
    val toolbarHeight = windowSizeClass.toolbarSize() countIf !containerState.toolbarOverlaps

    val topClearance by animateDpAsState(targetValue = statusBarSize + toolbarHeight)
    val navRailWidth by animateDpAsState(
        targetValue = windowSizeClass.navRailWidth() countIf containerState.navRailVisible
    )

    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
            .width(navRailWidth),
    ) {
        Spacer(
            modifier = Modifier
                .padding(top = topClearance)
                .height(24.dp)
        )
        navItems.forEach { navItem ->
            NavRailItem(item = navItem, navStateHolder = navStateHolder)
        }
    }
}

@Composable
private fun NavRailItem(
    item: NavItem,
    navStateHolder: NavigationStateHolder,
) {
    val alpha = if (item.selected) 1f else 0.6f
    NavigationRailItem(
        selected = item.selected,
        icon = {
            Icon(
                imageVector = item.icon,
                contentDescription = item.name,
            )
        },
        label = {
            Text(
                modifier = Modifier.alpha(alpha),
                text = item.name,
                fontSize = 12.sp
            )
        },
        onClick = {
            navStateHolder.accept { navState.navItemSelected(item = item) }
        }
    )
}