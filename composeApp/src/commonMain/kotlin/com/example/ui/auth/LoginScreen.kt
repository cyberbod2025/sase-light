package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.AuthFailureReason
import com.example.ui.SaseNavy
import com.example.ui.components.buttons.SasePrimaryButton
import com.example.ui.components.feedback.SaseAlertCard
import com.example.ui.components.feedback.SaseAlertVariant
import com.example.ui.theme.SaseColors
import com.example.viewmodel.LabViewModel
import com.example.viewmodel.LoginUiState

/**
 * Compuerta de acceso del personal. Formulario correo + contrasena contra el
 * [com.example.data.auth.AuthRepository] mock. No expone el directorio de
 * credenciales demo: solo una nota discreta con una credencial de prueba.
 */
@Composable
fun LoginScreen(viewModel: LabViewModel) {
    val loginState by viewModel.loginState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val loading = loginState is LoginUiState.Loading
    val canSubmit = email.isNotBlank() && password.isNotBlank() && !loading

    val submit: () -> Unit = {
        if (email.isNotBlank() && password.isNotBlank()) {
            viewModel.signIn(email.trim(), password)
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = SaseColors.TextPrimary,
        unfocusedTextColor = SaseColors.TextPrimary,
        disabledTextColor = SaseColors.TextDisabled,
        focusedLabelColor = SaseColors.InstitutionalBlue,
        unfocusedLabelColor = SaseColors.TextSecondary,
        focusedPlaceholderColor = SaseColors.Placeholder,
        unfocusedPlaceholderColor = SaseColors.Placeholder,
        cursorColor = SaseColors.InstitutionalBlue,
        focusedBorderColor = SaseColors.BorderFocus,
        unfocusedBorderColor = SaseColors.Border,
        disabledBorderColor = SaseColors.BorderDisabled,
        focusedContainerColor = SaseColors.Surface,
        unfocusedContainerColor = SaseColors.Surface,
        disabledContainerColor = SaseColors.SurfaceVariant,
        focusedLeadingIconColor = SaseColors.InstitutionalBlue,
        unfocusedLeadingIconColor = SaseColors.TextSecondary,
        focusedTrailingIconColor = SaseColors.TextSecondary,
        unfocusedTrailingIconColor = SaseColors.TextSecondary
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SaseNavy),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SaseColors.Surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "SASE-310",
                    color = SaseColors.TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                )
                Text(
                    text = "Acceso de personal",
                    color = SaseColors.TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(2.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Correo institucional") },
                    singleLine = true,
                    enabled = !loading,
                    isError = loginState is LoginUiState.Error,
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Contrasena") },
                    singleLine = true,
                    enabled = !loading,
                    isError = loginState is LoginUiState.Error,
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible) "Ocultar contrasena" else "Mostrar contrasena"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors
                )

                (loginState as? LoginUiState.Error)?.let { error ->
                    SaseAlertCard(
                        title = "No se pudo iniciar sesion",
                        description = error.reason.toMessage(),
                        variant = SaseAlertVariant.ERROR
                    )
                }

                Spacer(Modifier.height(2.dp))

                SasePrimaryButton(
                    text = if (loading) "Ingresando..." else "Iniciar sesion",
                    onClick = submit,
                    icon = Icons.AutoMirrored.Filled.Login,
                    enabled = canSubmit,
                    modifier = Modifier.fillMaxWidth()
                )

                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = SaseColors.InstitutionalBlue,
                        strokeWidth = 2.dp
                    )
                }

                Spacer(Modifier.height(2.dp))

                // Nota de desarrollo — solo demo, una credencial.
                Text(
                    text = "Demo: secretaria@example.invalid / demo1234",
                    color = SaseColors.TextDisabled,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun AuthFailureReason.toMessage(): String = when (this) {
    AuthFailureReason.INVALID_CREDENTIALS -> "Correo o contrasena incorrectos."
    AuthFailureReason.INACTIVE_ACCOUNT -> "Tu cuenta esta inactiva. Contacta a Direccion."
    AuthFailureReason.NO_STAFF_PROFILE -> "No se encontro un perfil de personal para este correo."
    AuthFailureReason.NETWORK -> "Error de conexion. Intenta de nuevo."
}
