## Declarative APIS for declarative UIs

This project outlines how the declarative principles of Compose, especially how its state
ownership, production and management semantics provide systemic benefits when extended to the
entirety of the UI layer. I also refer to this concept as "x as state".

In particular the following are explored:

* Navigation as state (TreeNav: https://github.com/tunjid/treeNav)
* Pagination as state (Tiler: https://github.com/tunjid/Tiler)
* Functional reactive state production (MVI) as a UDF implementation (Mutator: https://github.com/tunjid/Mutator)

The above are used to implement a system design that supports

* Shared element transitions using a single root `LookaheadScope` in the `NavHost` (see `AdaptiveContentHost` and `SharedElements.kt`)
* Predictive back animations and seeking using an immutable hoisted navigation state that is adapted on a screen configuration basis.
* Seeding pagination pipelines from navigation arguments.
* Persistence of navigation state to disk to restore navigation past process death, including after system reboots.

The above provide a system where "first frame readiness" is guaranteed when navigating between
navigation destinations, obviating the need for legacy APIs from View such as
[`Activity.postponeEnterTransition()`](https://developer.android.com/reference/android/app/Activity.html#postponeEnterTransition()) and [`Activity.startPostponedEnterTransition()`](https://developer.android.com/reference/android/app/Activity.html#startPostponedEnterTransition()).