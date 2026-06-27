package com.example.viewmodel

import com.example.api.GeminiImageGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class GeminiState {
    data object Idle : GeminiState()
    data object Loading : GeminiState()
    data class Success(val base64Image: String) : GeminiState()
    data class Error(val message: String) : GeminiState()
}

class GeminiViewModel {
    private val _geminiState = MutableStateFlow<GeminiState>(GeminiState.Idle)
    val geminiState: StateFlow<GeminiState> = _geminiState.asStateFlow()

    fun generateGeminiImage(prompt: String, scope: CoroutineScope, size: String = "1024x1024") {
        if (prompt.isBlank()) {
            _geminiState.value = GeminiState.Error("El prompt no puede estar vacío")
            return
        }
        _geminiState.value = GeminiState.Loading
        scope.launch {
            val result = GeminiImageGenerator.generateImage(prompt, size)
            _geminiState.value = if (result != null) {
                GeminiState.Success(result)
            } else {
                GeminiState.Error("No se pudo generar la imagen. Verifica tu API key y conexión.")
            }
        }
    }

    fun resetGeminiState() {
        _geminiState.value = GeminiState.Idle
    }
}
