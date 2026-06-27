package com.example

import androidx.compose.ui.window.ComposeUIViewController
import com.example.ui.SaseAppContent
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LabViewModel

fun MainViewController() = ComposeUIViewController {
    MyApplicationTheme {
        SaseAppContent(viewModel = LabViewModel())
    }
}
