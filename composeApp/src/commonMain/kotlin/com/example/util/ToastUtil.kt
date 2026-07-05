package com.example.util

import androidx.compose.runtime.compositionLocalOf

typealias ToastFunction = (String) -> Unit

val LocalToast = compositionLocalOf<ToastFunction> { { } }
