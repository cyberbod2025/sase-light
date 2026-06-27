package com.example.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

typealias ToastFunction = (String) -> Unit

val LocalToast = compositionLocalOf<ToastFunction> { { message -> println("Toast: $message") } }

fun toast(scope: CoroutineScope, message: String): Unit = scope.launch {
    println("Toast: $message")
}
