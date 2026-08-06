package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.AuthFailureReason
import com.example.data.auth.StaffRole
import com.example.data.auth.institutionalLabel
import com.example.ui.SaseNavy
import com.example.ui.components.buttons.SasePrimaryButton
import com.example.ui.components.feedback.SaseAlertCard
import com.example.ui.components.feedback.SaseAlertVariant
import com.example.ui.theme.SaseColors
import com.example.viewmodel.LabViewModel
import com.example.viewmodel.LoginUiState

@Composable
fun LoginScreen(viewModel: LabViewModel) {
    val loginState by viewModel.loginState.collectAsState()
    val transitioning by viewModel.sessionTransitioning.collectAsState()
    val environment = viewModel.appEnvironment

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val loading = loginState is LoginUiState.Loading || transitioning
    val canSubmit = email.isNotBlank() && password.isNotBlank() && !loading
    val submit: () -> Unit = {
        if (canSubmit) viewModel.signIn(email.trim(), password)
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
        modifier = Modifier.fillMaxSize().background(SaseNavy),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 520.dp)
                .fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SaseColors.Surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "SASE-310",
                    color = SaseColors.TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                )
                Text(
                    text = "Acceso de personal · ${environment.mode.visibleLabel()} · v${environment.appVersion}",
                    color = SaseColors.TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                if (environment.usesSyntheticData) {
                    val institution = environment.demo?.institutionDisplayName
                        ?: "ENTORNO STAGING CON DATOS OPERATIVOS SINTÉTICOS"
                    SaseAlertCard(
                        title = "DATOS SINTÉTICOS · ${environment.mode.visibleLabel()}",
                        description = "$institution. No usar para operación institucional real.",
                        variant = SaseAlertVariant.WARNING
                    )
                }

                when (val state = loginState) {
                    is LoginUiState.RoleSelectionRequired -> {
                        Text(
                            text = "Selecciona un rol asignado",
                            color = SaseColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = state.context.institutionName,
                            color = SaseColors.TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        state.context.availableRoles.forEach { assignment ->
                            Button(
                                onClick = { viewModel.selectRole(assignment.roleId) },
                                enabled = !loading,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(assignment.role.institutionalLabel())
                            }
                        }
                        OutlinedButton(
                            onClick = viewModel::signOut,
                            enabled = !loading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancelar acceso")
                        }
                    }

                    else -> {
                        if (viewModel.demoAccessAvailable) {
                            Text(
                                text = "Entradas guiadas",
                                color = SaseColors.TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    StaffRole.DIRECCION to "Dirección",
                                    StaffRole.SECRETARIA to "Secretaría",
                                    StaffRole.DOCENTE to "Docente"
                                ).forEach { (role, label) ->
                                    Button(
                                        onClick = { viewModel.signInDemo(role) },
                                        enabled = !loading,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(label, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (viewModel.demoAccessAvailable) {
                                "Acceso con credenciales para validar casos de sesión"
                            } else {
                                "Credenciales institucionales"
                            },
                            color = SaseColors.TextSecondary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Correo institucional") },
                            singleLine = true,
                            enabled = !loading,
                            isError = state is LoginUiState.Error,
                            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            colors = fieldColors
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Contraseña") },
                            singleLine = true,
                            enabled = !loading,
                            isError = state is LoginUiState.Error,
                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { submit() }),
                            colors = fieldColors
                        )

                        (state as? LoginUiState.Error)?.let { error ->
                            Box(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                                SaseAlertCard(
                                    title = "No se pudo iniciar sesión",
                                    description = error.reason.toMessage(),
                                    variant = SaseAlertVariant.ERROR
                                )
                            }
                        }

                        SasePrimaryButton(
                            text = if (loading) "Ingresando..." else "Iniciar sesión",
                            onClick = submit,
                            icon = Icons.AutoMirrored.Filled.Login,
                            enabled = canSubmit,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (viewModel.demoAccessAvailable) {
                            OutlinedButton(
                                onClick = viewModel::resetDemo,
                                enabled = !loading,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Reiniciar demo")
                            }
                        }
                    }
                }

                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = SaseColors.InstitutionalBlue,
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

private fun AuthFailureReason.toMessage(): String = when (this) {
    AuthFailureReason.INVALID_CREDENTIALS -> "Correo o contraseña incorrectos."
    AuthFailureReason.INACTIVE_ACCOUNT -> "La cuenta está inactiva. Contacta a Dirección."
    AuthFailureReason.NO_STAFF_PROFILE -> "No se encontró un perfil de personal para este correo."
    AuthFailureReason.NO_MEMBERSHIP -> "El perfil no tiene una membresía institucional activa."
    AuthFailureReason.NO_ROLE -> "La membresía no tiene un rol institucional asignado."
    AuthFailureReason.ROLE_NOT_ASSIGNED -> "Ese rol no está asignado a la membresía activa."
    AuthFailureReason.SESSION_EXPIRED -> "La sesión expiró. Inicia sesión de nuevo."
    AuthFailureReason.DEMO_UNAVAILABLE -> "Las entradas demo no están habilitadas en este ambiente."
    AuthFailureReason.CONFIGURATION -> "La configuración de acceso es inválida."
    AuthFailureReason.NETWORK -> "Error de conexión. Intenta de nuevo."
}
