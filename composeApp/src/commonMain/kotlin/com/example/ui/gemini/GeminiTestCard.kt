package com.example.ui.gemini

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.GlassCard
import com.example.ui.SaseBlue
import com.example.ui.SaseBorder
import com.example.ui.SaseGreen
import com.example.ui.SaseGreenDark
import com.example.ui.SaseMuted
import com.example.ui.SaseNavy
import com.example.ui.SaseRed
import com.example.ui.SaseText
import com.example.viewmodel.GeminiState

@Composable
fun GeminiTestCard(
    geminiState: GeminiState,
    onGenerate: (String) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var prompt by remember { mutableStateOf("") }

    GlassCard(modifier = modifier) {
        Text(
            text = "Prueba de generaci\u00f3n IA",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = SaseNavy
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            placeholder = { Text("Describe la imagen a generar...", color = SaseMuted) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.75f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.5f),
                focusedBorderColor = SaseBlue,
                unfocusedBorderColor = SaseBorder,
                focusedTextColor = SaseText,
                unfocusedTextColor = SaseText
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { onGenerate(prompt) },
            enabled = prompt.isNotBlank() && geminiState !is GeminiState.Loading,
            colors = ButtonDefaults.buttonColors(containerColor = SaseNavy),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Generar imagen IA", fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (val state = geminiState) {
            is GeminiState.Idle -> { }
            is GeminiState.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = SaseBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generando imagen...", color = SaseMuted, fontSize = 11.sp)
                }
            }
            is GeminiState.Success -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SaseGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Imagen generada correctamente", color = SaseGreenDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text("Base64: ${state.base64Image.take(50)}... (${state.base64Image.length} chars)", color = SaseMuted, fontSize = 9.sp)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onReset) {
                        Text("Limpiar", fontSize = 10.sp, color = SaseBlue)
                    }
                }
            }
            is GeminiState.Error -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = SaseRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(state.message, color = SaseRed, fontSize = 11.sp)
                }
            }
        }
    }
}
