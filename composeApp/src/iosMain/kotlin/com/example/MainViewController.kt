package com.example

import androidx.compose.ui.window.ComposeUIViewController
import com.example.ui.SaseApp
import com.example.ui.theme.MyApplicationTheme

fun MainViewController() = ComposeUIViewController {
    MyApplicationTheme {
        SaseApp()
    }
}
