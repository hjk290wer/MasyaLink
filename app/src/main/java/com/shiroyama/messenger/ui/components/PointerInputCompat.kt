package com.shiroyama.messenger.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputScope

fun Modifier.pointerInput(
    key1: Any?,
    block: suspend PointerInputScope.() -> Unit
): Modifier = androidx.compose.ui.input.pointer.pointerInput(this, key1, block)
