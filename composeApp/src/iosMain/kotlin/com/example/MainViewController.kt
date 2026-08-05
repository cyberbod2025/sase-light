package com.example

import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.runtime.remember
import com.example.ui.SaseBootstrapContent
import com.example.ui.theme.MyApplicationTheme

fun MainViewController() = ComposeUIViewController {
    val bootstrap = remember { SaseCompositionRoot.create() }
    MyApplicationTheme {
        SaseBootstrapContent(bootstrap)
    }
}
