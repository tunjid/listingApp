## Declarative APIS for declarative UIs

This project outlines how the declarative principles of Compose, especially how its state
ownership, production and management semantics provide systemic benefits when extended to the
entirety of the UI layer. I also refer to this concept as "x as state".

In particular the following are explored:

* Navigation as state (TreeNav: https://github.com/tunjid/treeNav)
* Pagination as state (Tiler: https://github.com/tunjid/Tiler)
* Functional reactive state production (MVI) as a UDF implementation (
  Mutator: https://github.com/tunjid/Mutator)

The above are used to implement a system design that supports

* Shared element transitions using a single root `LookaheadScope` in the `NavHost` (
  see `AdaptiveContentHost` and `SharedElements.kt`)
* Predictive back animations and seeking using an immutable hoisted navigation state that is adapted
  on a screen configuration basis.
* Seeding pagination pipelines from navigation arguments.
* Persistence of navigation state to disk to restore navigation past process death, including after
  system reboots.

The above provide a system where "first frame readiness" is guaranteed when navigating between
navigation destinations, obviating the need for legacy APIs from View such as
[`Activity.postponeEnterTransition()`](https://developer.android.com/reference/android/app/Activity.html#postponeEnterTransition())
and [`Activity.startPostponedEnterTransition()`](https://developer.android.com/reference/android/app/Activity.html#startPostponedEnterTransition()).

## App recordings

|                Portrait orientation                |             Landscape orientation              |
|:--------------------------------------------------:|:----------------------------------------------:|
| ![photo portrait](./docs/media/photo-portrait.gif) | ![landscape](./docs/media/photo-landscape.gif) |
| ![video portrait](./docs/media/video-portrait.gif) | ![landscape](./docs/media/video-landscape.gif) |

## Screens

There are 4 screens in the app:

* `ListingFeedScreen`: Vertical grid. Feed and list in a list-detail canonical layout
  implementation. Each feed item has a horizontal list of non paginated images.
* `ListingDetailScreen`: Detail screen in a canonical list-detail implementation. Has a paginated
  horizontal list for media displayed.
* `GridGalleryScreen`: Grid gallery layout for media, it is paginated.
* `PagerGalleryScreen`: Full screen paged gallery layout for media, it is paginated. Also implements
  drag to dismiss with `Modifier.Node`.

All screens have the same state declaration:

```kotlin
@Composable
fun FeatureScreen(
    modifier: Modifier = Modifier,
    state: State,
    actions: (Action) -> Unit,
)
```

Where `State` is an immutable data class, however a class backed by compose state is as effective.
`actions` is an event sink commonly used by MVI frameworks and is a stable Compose parameter.

Routing to the screen is defined higher up using remember retained semantics scoped to navigation:

```kotlin
    @Composable
     fun FeatureRoute() {
        val stateHolder = rememberRetainedStateHolder<ListingDetailStateHolder>(
            route = this@FeatureRoute
        )
        FeatureScreen(
            modifier = Modifier.backPreviewBackgroundModifier(),
            state = stateHolder.state.collectAsStateWithLifecycle().value,
            actions = stateHolder.accept
        )
    }
```

Here, based on navigation and UI state including predictive back progress, the `FeatureScreen` can
be Composed.