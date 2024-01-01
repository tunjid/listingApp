package com.tunjid.scaffold.lifecycle

import androidx.compose.runtime.*
import com.tunjid.mutator.ActionStateProducer
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.asNoOpStateFlowMutator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

typealias LifecycleStateHolder = ActionStateProducer<@JvmSuppressWildcards Mutation<Lifecycle>, @JvmSuppressWildcards StateFlow<Lifecycle>>

data class Lifecycle(
    val isInForeground: Boolean = true,
)

val LocalLifecycleStateHolder = staticCompositionLocalOf {
    Lifecycle().asNoOpStateFlowMutator<Mutation<Lifecycle>, Lifecycle>()
}

fun <T> Flow<T>.monitorWhenActive(lifecycleStateFlow: StateFlow<Lifecycle>) =
    lifecycleStateFlow
        .map { it.isInForeground }
        .distinctUntilChanged()
        .flatMapLatest { isInForeground ->
            if (isInForeground) this
            else emptyFlow()
        }

inline fun <T, R> StateFlow<T>.mapState(
    scope: CoroutineScope,
    crossinline mapper: (T) -> R
): StateFlow<R> = map { mapper(it) }
    .distinctUntilChanged()
    .stateIn(
        scope = scope,
        initialValue = mapper(value),
        started = SharingStarted.WhileSubscribed(2000),
    )

@Composable
inline fun <T, R> StateFlow<T>.mappedCollectAsStateWithLifecycle(
    context: CoroutineContext = Dispatchers.Main.immediate,
    crossinline mapper: @DisallowComposableCalls (T) -> R
): State<R> {
    val lifecycle = LocalLifecycleStateHolder.current.state
    val scope = rememberCoroutineScope()
    val lifecycleBoundState = remember {
        mapState(scope = scope, mapper = mapper)
            .monitorWhenActive(lifecycle)
    }
    val initial = remember { mapper(value) }

    return lifecycleBoundState.collectAsState(
        context = context,
        initial = initial
    )
}

@Composable
fun <T> StateFlow<T>.collectAsStateWithLifecycle(
    context: CoroutineContext = Dispatchers.Main.immediate,
): State<T> {
    val lifecycle = LocalLifecycleStateHolder.current.state
    val lifecycleBoundState = remember { monitorWhenActive(lifecycle) }
    val initial = remember { value }
    return lifecycleBoundState.collectAsState(
        context = context,
        initial = initial
    )
}

@Singleton
class ActualLifecycleStateHolder @Inject constructor(
    appScope: CoroutineScope,
) : LifecycleStateHolder by appScope.actionStateFlowProducer(
    started = SharingStarted.Eagerly,
    initialState = Lifecycle(),
    actionTransform = { it }
)

@Composable
operator fun <Action : Any, State : Any> ActionStateProducer<Action, StateFlow<State>>.component1()
: State = state.collectAsStateWithLifecycle().value

@Composable
operator fun <Action : Any, State : Any> ActionStateProducer<Action, StateFlow<State>>.component2()
: (Action) -> Unit = remember { accept }

