package com.example

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.ui.SaseAppContent
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LabViewModel

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "SASE-310 - Sistema de Administración de Secretaría y Expedientes",
        state = rememberWindowState(width = 1280.dp, height = 800.dp)
    ) {
        MyApplicationTheme {
            SaseAppContent(viewModel = LabViewModel())
        }
    }
}
