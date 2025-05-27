package com.tunjid.explore.grid.di

import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.round
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.explore.grid.ExploreGridModelFactory
import com.tunjid.explore.grid.ExploreGridScreen
import com.tunjid.explore.grid.ExploreGridViewModel
import com.tunjid.me.scaffold.scaffold.predictiveBackBackgroundModifier
import com.tunjid.scaffold.scaffold.PaneNavigationBar
import com.tunjid.scaffold.scaffold.PaneNavigationRail
import com.tunjid.scaffold.scaffold.PaneScaffold
import com.tunjid.scaffold.scaffold.bottomNavigationNestedScrollConnection
import com.tunjid.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.urlRouteMatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

private const val RoutePattern = "/explore"

@Module
@InstallIn(SingletonComponent::class)
object ExploreGridModule {

    @IntoMap
    @Provides
    @StringKey(RoutePattern)
    fun routeParser(): RouteMatcher =
        urlRouteMatcher(
            routePattern = RoutePattern,
            routeMapper = ::routeOf
        )

    @IntoMap
    @Provides
    @StringKey(RoutePattern)
    fun routeAdaptiveConfiguration(
        factory: ExploreGridModelFactory
    ) = threePaneEntry { route ->
        val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
        val viewModel = viewModel<ExploreGridViewModel> {
            factory.create(
                scope = lifecycleCoroutineScope,
                route = route,
            )
        }

        val bottomNavigationOffsetConnection =
            bottomNavigationNestedScrollConnection()

        rememberPaneScaffoldState().PaneScaffold(
            modifier = Modifier
                .predictiveBackBackgroundModifier(paneScope = this)
                .nestedScroll(bottomNavigationOffsetConnection),
            showNavigation = false,
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = "Videos")
                    },
                )
            },
            content = { paddingValues ->
                ExploreGridScreen(
                    scaffoldState = this,
                    modifier = Modifier
                        .padding(
                            top = paddingValues.calculateTopPadding()
                        ),
                    state = viewModel.state.collectAsStateWithLifecycle().value,
                    actions = viewModel.accept
                )
            },
            navigationBar = {
                PaneNavigationBar(
                    modifier = Modifier
                        .offset {
                            bottomNavigationOffsetConnection.offset.round()
                        }
                        .animateEnterExit(
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it }),
                        )
                )
            },
            navigationRail = {
                PaneNavigationRail()
            }
        )
    }
}