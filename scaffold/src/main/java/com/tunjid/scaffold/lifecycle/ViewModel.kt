package com.tunjid.scaffold.lifecycle

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun viewModelCoroutineScope() = CoroutineScope(
    context = SupervisorJob() + Dispatchers.Main.immediate
)
