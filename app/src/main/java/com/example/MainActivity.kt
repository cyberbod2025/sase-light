package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.SaseAppContent
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LabViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: LabViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SaseAppContent(viewModel = viewModel)
            }
        }
    }
}
