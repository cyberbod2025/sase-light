package com.example.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.attendance.AttendanceStatus
import com.example.data.attendance.TeacherGroup
import com.example.data.attendance.institutionalLabel
import com.example.data.auth.AuthSession
import com.example.ui.SaseGreen
import com.example.ui.SaseMuted
import com.example.ui.SaseNavy
import com.example.ui.SaseOrange
import com.example.ui.SaseRed
import com.example.ui.SaseText
import com.example.ui.components.buttons.SasePrimaryButton
import com.example.ui.components.buttons.SaseSecondaryButton
import com.example.viewmodel.ClassAttendanceUiState
import com.example.viewmodel.ClassAttendanceViewModel
import com.example.viewmodel.LabViewModel
import com.example.viewmodel.attendanceErrorMessage
import com.example.viewmodel.classAttendanceHeadline
import com.example.viewmodel.classAttendanceSaveLabel
import com.example.viewmodel.classAttendanceSummary
import com.example.viewmodel.unsavedChangesLabel

/**
 * Pantalla unica de la rebanada docente: lista de grupos asignados y, al abrir
 * uno, el pase de lista de la fecha. Diseñada mobile-first (una sola columna,
 * objetivos tactiles grandes, lista virtualizada) porque el target real es
 * Android en el aula.
 */
@Composable
fun TeacherAttendanceScreen(viewModel: LabViewModel, session: AuthSession) {
    val attendanceViewModel = remember(session.membershipId, session.activeRoleId) {
        ClassAttendanceViewModel(
            repository = viewModel.attendanceRepository,
            session = session
        )
    }
    val state by attendanceViewModel.state.collectAsState()

    LaunchedEffect(session.membershipId, session.activeRoleId) {
        attendanceViewModel.loadGroups()
    }

    Box(modifier = Modifier.fillMaxSize().background(SaseNavy)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.snapshot == null) {
                GroupPickerSection(
                    state = state,
                    // Si el error viene de un intento de apertura fallido,
                    // "Reintentar" reabre ESE grupo; si no (p. ej. fallo al
                    // listar grupos), recarga la lista completa.
                    onRetry = if (state.lastOpenAttempt != null) {
                        attendanceViewModel::retryLastOpenAttempt
                    } else {
                        attendanceViewModel::loadGroups
                    },
                    onOpen = { attendanceViewModel.openGroup(it.id) }
                )
            } else {
                AttendanceCaptureSection(
                    state = state,
                    onBack = attendanceViewModel::closeGroup,
                    onAllPresent = attendanceViewModel::markAllPresent,
                    onStatus = attendanceViewModel::setStatus,
                    onSave = attendanceViewModel::save
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.GroupPickerSection(
    state: ClassAttendanceUiState,
    onRetry: () -> Unit,
    onOpen: (TeacherGroup) -> Unit
) {
    SectionTitle("Mis grupos")

    when {
        state.loadingGroups -> LoadingRow("Cargando tus grupos…")

        state.error != null -> RecoverableError(
            message = attendanceErrorMessage(state.error).orEmpty(),
            actionLabel = "Reintentar",
            onAction = onRetry
        )

        state.showsEmptyGroupsState -> InfoCard(
            title = "Aún no tienes grupos asignados",
            body = "Cuando la Secretaría te asigne un grupo del ciclo escolar, aparecerá aquí " +
                "y podrás pasar lista. No necesitas hacer nada más por ahora."
        )

        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.groups, key = { it.id }) { group ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = group.name,
                            color = SaseNavy,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${group.gradeLabel} · Ciclo ${group.schoolCycleId}",
                            color = SaseMuted,
                            fontSize = 12.sp
                        )
                        SasePrimaryButton(
                            text = "Pasar lista",
                            onClick = { onOpen(group) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.openingGroup
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.AttendanceCaptureSection(
    state: ClassAttendanceUiState,
    onBack: () -> Unit,
    onAllPresent: () -> Unit,
    onStatus: (String, AttendanceStatus) -> Unit,
    onSave: () -> Unit
) {
    SectionTitle(classAttendanceHeadline(state))

    Text(
        text = classAttendanceSummary(state),
        color = Color.White.copy(alpha = 0.85f),
        fontSize = 12.sp
    )

    unsavedChangesLabel(state)?.let { label ->
        Surface(color = Color(0xFFFFF4D6), contentColor = Color(0xFF7A4B00)) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    state.successMessage?.let { message ->
        Surface(color = Color(0xFFDCFCE7), contentColor = Color(0xFF166534)) {
            Text(
                text = message,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    state.error?.let { reason ->
        // Este error solo puede venir de un guardado fallido (un fallo al
        // abrir el grupo regresa a GroupPickerSection, ver TeacherAttendanceScreen).
        // La accion de recuperacion reintenta guardar, NUNCA recarga desde el
        // servidor: recargar descartaria el borrador que el docente ya capturo.
        RecoverableError(
            message = attendanceErrorMessage(reason).orEmpty(),
            actionLabel = "Reintentar guardado",
            onAction = onSave
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SaseSecondaryButton(text = "Volver", onClick = onBack)
        SaseSecondaryButton(text = "Todos presentes", onClick = onAllPresent)
    }

    if (state.openingGroup) {
        LoadingRow("Abriendo la sesión de clase…")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(state.roster, key = { it.student.id }) { entry ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${entry.student.listNumber}. ${entry.student.fullName}",
                        color = SaseText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AttendanceStatus.entries.forEach { status ->
                            FilterChip(
                                selected = entry.status == status,
                                onClick = { onStatus(entry.student.id, status) },
                                enabled = !state.saving,
                                label = {
                                    Text(status.institutionalLabel(), fontSize = 12.sp)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (status) {
                                        AttendanceStatus.PRESENTE -> SaseGreen
                                        AttendanceStatus.AUSENTE -> SaseRed
                                        AttendanceStatus.RETARDO -> SaseOrange
                                    },
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(4.dp))
    SasePrimaryButton(
        text = classAttendanceSaveLabel(state),
        onClick = onSave,
        modifier = Modifier.fillMaxWidth(),
        // Un segundo toque durante el guardado no reenvía: el botón se apaga
        // mientras hay una operación en curso y cuando no hay nada por guardar.
        enabled = state.canSave
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 18.sp
    )
}

@Composable
private fun LoadingRow(message: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
        Text(text = message, color = Color.White, fontSize = 13.sp)
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, color = SaseNavy, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(text = body, color = SaseMuted, fontSize = 13.sp, textAlign = TextAlign.Start)
        }
    }
}

@Composable
private fun RecoverableError(message: String, actionLabel: String, onAction: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = message, color = Color(0xFF991B1B), fontSize = 13.sp)
            SaseSecondaryButton(text = actionLabel, onClick = onAction)
        }
    }
}
