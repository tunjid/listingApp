/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.treenav.adaptive

import androidx.window.core.layout.WindowSizeClass

internal val WindowSizeClass.Companion.COMPACT get() = WINDOW_SIZE_CLASS_COMPACT

internal val WindowSizeClass.Companion.MEDIUM get() = WINDOW_SIZE_CLASS_MEDIUM

internal val WindowSizeClass.Companion.EXPANDED get() = WINDOW_SIZE_CLASS_EXPANDED

private val WINDOW_SIZE_CLASS_COMPACT = WindowSizeClass.compute(
    dpWidth = 0f,
    dpHeight = 0f,
)

private val WINDOW_SIZE_CLASS_MEDIUM = WindowSizeClass.compute(
    dpWidth = 600f,
    dpHeight = 480f,
)

private val WINDOW_SIZE_CLASS_EXPANDED = WindowSizeClass.compute(
    dpWidth = 800f,
    dpHeight = 900f,
)