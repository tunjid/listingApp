package com.tunjid.scaffold.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.tunjid.treenav.strings.routeOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

fun unknownRoute(path: String = "404") = routeOf(path = path)

@Composable
internal fun RouteNotFound() {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(),
            text = "404",
            fontSize = 40.sp
        )
    }
}