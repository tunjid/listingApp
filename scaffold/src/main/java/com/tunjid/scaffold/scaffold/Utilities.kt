package com.tunjid.scaffold.scaffold

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
inline fun <T> rememberUpdatedStateIf(
    value: T,
    predicate: (T) -> Boolean
): State<T> = remember {
    mutableStateOf(value)
}.also { if (predicate(value)) it.value = value }