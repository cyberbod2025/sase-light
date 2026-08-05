package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.AuthSession
import com.example.data.auth.SaseArea
import com.example.data.auth.StaffPermissions
import com.example.data.auth.institutionalLabel
import com.example.environment.AppEnvironmentMode
import com.example.ui.GlassCard
import com.example.ui.SaseMuted
import com.example.ui.SaseNavy
import com.example.ui.theme.SaseColors
import com.example.viewmodel.LabViewModel
import com.example.viewmodel.Screen

@Composable
fun SessionHomeScreen(viewModel: LabViewModel, session: AuthSession) {
    val environment = viewModel.appEnvironment
    val availableDestinations = buildList {
        if (StaffPermissions.canAccess(session, SaseArea.SECRETARIA)) {
            add("Secretaría" to Screen.SecretaryDashboard)
        }
        if (StaffPermissions.canAccess(session, SaseArea.EXPEDIENTE)) {
            add("Expedientes" to Screen.StudentRecordsDashboard)
        }
        if (StaffPermissions.canAccess(session, SaseArea.ALTA_OFICIAL)) {
            add("Inscripciones" to Screen.EnrollmentDashboard)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(SaseNavy),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 700.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Sesión institucional activa",
                    color = SaseNavy,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = session.institutionName,
                    color = SaseMuted,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${session.profile.fullName} · ${session.activeRole.institutionalLabel()}",
                    color = SaseColors.TextPrimary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Ciclo: ${session.schoolCycleId ?: "no informado"} · " +
                        "Modo: ${environment.mode.visibleLabel()} · v${environment.appVersion}",
                    color = SaseMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                if (environment.usesSyntheticData) {
                    Surface(
                        color = Color(0xFFFFF4D6),
                        contentColor = Color(0xFF7A4B00)
                    ) {
                        Text(
                            text = "DATOS SINTÉTICOS · NO USAR PARA OPERACIÓN INSTITUCIONAL REAL",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                if (availableDestinations.isEmpty()) {
                    Text(
                        text = "Este rol está autenticado, pero todavía no tiene un módulo operativo habilitado en esta versión.",
                        color = SaseMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        availableDestinations.forEach { (label, destination) ->
                            Button(onClick = { viewModel.navigateTo(destination) }) {
                                Text(label)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SessionIdentityHeader(viewModel: LabViewModel, session: AuthSession) {
    val transitioning by viewModel.sessionTransitioning.collectAsState()
    val environment = viewModel.appEnvironment

    Surface(
        color = SaseNavy.copy(alpha = 0.94f),
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (environment.usesSyntheticData) {
                Text(
                    text = "DATOS SINTÉTICOS · NO USAR PARA OPERACIÓN REAL",
                    color = Color(0xFFFFD166),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Text(
                text = "${session.profile.fullName} · ${session.institutionName} · " +
                    "${session.activeRole.institutionalLabel()} · " +
                    "${session.schoolCycleId ?: "sin ciclo"} · ${environment.mode.visibleLabel()}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (transitioning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                }
                Button(
                    onClick = viewModel::signOut,
                    enabled = !transitioning
                ) {
                    Text("Cerrar sesión")
                }
                if (environment.isDemo) {
                    OutlinedButton(
                        onClick = viewModel::resetDemo,
                        enabled = !transitioning
                    ) {
                        Text("Reiniciar demo")
                    }
                }
            }

            if (session.roleAssignments.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    session.roleAssignments.forEach { assignment ->
                        OutlinedButton(
                            onClick = { viewModel.switchRole(assignment.roleId) },
                            enabled = !transitioning && assignment.roleId != session.activeRoleId
                        ) {
                            Text(assignment.role.institutionalLabel(), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

fun AppEnvironmentMode.visibleLabel(): String = when (this) {
    AppEnvironmentMode.DEMO_LOCAL -> "DEMO LOCAL"
    AppEnvironmentMode.SUPABASE_STAGING -> "SUPABASE STAGING"
    AppEnvironmentMode.PRODUCTION -> "PRODUCCIÓN"
}
