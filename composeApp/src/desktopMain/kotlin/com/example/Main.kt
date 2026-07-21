package com.example

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.ui.SaseApp
import com.example.ui.theme.MyApplicationTheme

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "SASE-310 - Sistema de Administración de Secretaría y Expedientes",
        state = rememberWindowState(width = 1100.dp, height = 700.dp)
    ) {
        MyApplicationTheme {
            SaseApp()
        }
    }
}
