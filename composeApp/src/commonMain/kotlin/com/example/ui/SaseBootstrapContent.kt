package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.SaseBootstrap

@Composable
fun SaseBootstrapContent(bootstrap: SaseBootstrap) {
    when (bootstrap) {
        is SaseBootstrap.Ready -> SaseAppContent(viewModel = bootstrap.viewModel)
        is SaseBootstrap.ConfigurationFailure -> ConfigurationFailureScreen(bootstrap)
    }
}

@Composable
private fun ConfigurationFailureScreen(failure: SaseBootstrap.ConfigurationFailure) {
    Box(
        modifier = Modifier.fillMaxSize().background(SaseNavy),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(modifier = Modifier.padding(24.dp).widthIn(max = 520.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = failure.title,
                    color = SaseNavy,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = failure.message,
                    color = Color(0xFF475569),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
