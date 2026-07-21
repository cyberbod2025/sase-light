package com.example.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.BackendUnavailableReason
import com.example.ui.components.buttons.SasePrimaryButton
import com.example.ui.components.cards.SaseCard
import com.example.ui.components.fields.SaseTextField
import com.example.ui.theme.SaseColors
import com.example.viewmodel.LoginErrorReason
import com.example.viewmodel.LoginUiState
import com.example.viewmodel.StaffLoginViewModel
import kotlinx.coroutines.launch

object LoginTestTags {
    const val SCREEN = "login_screen"
    const val EMAIL = "login_email"
    const val PASSWORD = "login_password"
    const val SUBMIT = "login_submit"
    const val ERROR = "login_error"
}

/**
 * Acceso del personal. La contraseña vive únicamente en el estado local del
 * campo mientras se escribe: no se guarda, no se registra y no sale de aquí
 * salvo hacia el repositorio de sesión.
 */
@Composable
fun LoginScreen(
    viewModel: StaffLoginViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val unavailable = uiState as? LoginUiState.BackendUnavailable
    val submitting = uiState is LoginUiState.Submitting
    val canSubmit = unavailable == null && !submitting &&
        email.isNotBlank() && password.isNotEmpty()

    val submit: () -> Unit = {
        if (canSubmit) {
            scope.launch { viewModel.signIn(email, password) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(LoginTestTags.SCREEN),
        contentAlignment = Alignment.Center
    ) {
        SaseCard(modifier = Modifier.widthIn(max = 420.dp).padding(24.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "SASE — Secundaria 310",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SaseColors.TextPrimary
                )
                Text(
                    text = "Acceso del personal institucional",
                    fontSize = 13.sp,
                    color = SaseColors.TextSecondary
                )

                if (unavailable != null) {
                    Text(
                        text = unavailable.reason.message(),
                        modifier = Modifier.testTag(LoginTestTags.ERROR),
                        color = SaseColors.Error,
                        fontSize = 13.sp
                    )
                } else {
                    SaseTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            viewModel.onInputChanged()
                        },
                        modifier = Modifier.fillMaxWidth().testTag(LoginTestTags.EMAIL),
                        label = "Correo institucional",
                        enabled = !submitting,
                        isError = uiState is LoginUiState.Error
                    )

                    // Campo de contraseña: enmascarado siempre, sin opción de
                    // revelar y sin autocompletado institucional.
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            viewModel.onInputChanged()
                        },
                        modifier = Modifier.fillMaxWidth().testTag(LoginTestTags.PASSWORD),
                        label = { Text("Contraseña") },
                        enabled = !submitting,
                        isError = uiState is LoginUiState.Error,
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() })
                    )

                    (uiState as? LoginUiState.Error)?.let { error ->
                        Text(
                            text = error.reason.message(),
                            modifier = Modifier.testTag(LoginTestTags.ERROR),
                            color = SaseColors.Error,
                            fontSize = 13.sp
                        )
                    }

                    if (submitting) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }

                    SasePrimaryButton(
                        text = if (submitting) "Verificando…" else "Entrar",
                        onClick = submit,
                        modifier = Modifier.fillMaxWidth().testTag(LoginTestTags.SUBMIT),
                        enabled = canSubmit
                    )
                }

                Text(
                    text = "El acceso queda registrado. Cada área ve únicamente la " +
                        "información que su función requiere.",
                    fontSize = 11.sp,
                    color = SaseColors.TextSecondary
                )
            }
        }
    }
}

/** Mensajes presentables: nunca detallan la causa técnica ni el origen interno. */
internal fun LoginErrorReason.message(): String = when (this) {
    LoginErrorReason.INVALID_CREDENTIALS -> "Correo o contraseña incorrectos."
    LoginErrorReason.INACTIVE_MEMBERSHIP -> "Esta cuenta está dada de baja. Consulta con Dirección."
    LoginErrorReason.NO_INSTITUTIONAL_ACCESS -> "La cuenta no tiene acceso institucional asignado."
    LoginErrorReason.NETWORK -> "Sin conexión con el servidor. Intenta de nuevo."
    LoginErrorReason.UNEXPECTED -> "No fue posible iniciar sesión. Intenta de nuevo."
}

internal fun BackendUnavailableReason.message(): String = when (this) {
    BackendUnavailableReason.CONFIGURATION_MISSING ->
        "El acceso en línea no está configurado en esta instalación."
    BackendUnavailableReason.CONFIGURATION_INVALID ->
        "La configuración de acceso en línea no es válida. Avisa a soporte."
}
