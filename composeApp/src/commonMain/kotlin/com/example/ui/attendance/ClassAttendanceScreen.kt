package com.example.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.attendance.*
import com.example.ui.GlassCard
import com.example.ui.LiquidGlassCard
import com.example.ui.components.buttons.SasePrimaryButton
import com.example.viewmodel.ClassAttendanceViewModel

@Composable
fun ClassAttendanceScreen(
    viewModel: ClassAttendanceViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    var reportStartDate by remember { mutableStateOf("2026-10-01") }
    var reportEndDate by remember { mutableStateOf("2026-10-31") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
            .verticalScroll(scrollState)
            .testTag("attendance_screen_root")
    ) {
        // Encabezado
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("attendance_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Asistencia y Puntualidad (PMC)",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Programa de Mejora Continua · Pase de lista y seguimiento por módulo",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mensajes de éxito o error
        state.successMessage?.let { msg ->
            Surface(
                color = Color(0xFF14532D),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("save_success_message")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4ADE80))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = msg, color = Color.White, fontSize = 13.sp)
                }
            }
        }

        state.errorMessage?.let { err ->
            Surface(
                color = Color(0xFF7F1D1D),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("save_error_message")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF87171))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = err, color = Color.White, fontSize = 13.sp)
                }
            }
        }

        // Panel de Control de Sesión
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Configuración y Selección de Sesión",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Selector de Grupo
                Text("Grupo Escolar Asignado", fontSize = 12.sp, color = Color(0xFF94A3B8))
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.groups.forEach { group ->
                        val isSelected = state.selectedGroup?.id == group.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF2563EB) else Color(0xFF1E293B))
                                .border(1.dp, if (isSelected) Color(0xFF60A5FA) else Color(0xFF334155), RoundedCornerShape(8.dp))
                                .clickable { viewModel.selectGroup(group) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("group_chip_${group.id}")
                        ) {
                            Text(
                                text = "${group.name} (${group.gradeLabel})",
                                color = Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Selector de Fecha y Módulo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Fecha
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Fecha de Sesión (YYYY-MM-DD)", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = state.selectedDate,
                            onValueChange = { viewModel.setDate(it) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("attendance_date_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155)
                            )
                        )
                    }

                    // Módulo (Soporte Módulos Dobles)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Módulo / Bloque Horario", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 2, 3, 4).forEach { modNum ->
                                val isSelected = state.selectedModule == modNum
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B))
                                        .border(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155), RoundedCornerShape(8.dp))
                                        .clickable { viewModel.setModule(modNum) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                        .testTag("module_btn_$modNum")
                                ) {
                                    Text(
                                        text = "Módulo $modNum",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Estatus Operativo de la Sesión
                Text("Estatus Operativo de la Sesión", fontSize = 12.sp, color = Color(0xFF94A3B8))
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SessionStatus.entries.forEach { st ->
                        val isSelected = state.sessionStatus == st
                        val bg = when (st) {
                            SessionStatus.PROGRAMADA -> if (isSelected) Color(0xFF047857) else Color(0xFF1E293B)
                            SessionStatus.CANCELADA -> if (isSelected) Color(0xFFB45309) else Color(0xFF1E293B)
                            SessionStatus.NO_LECTIVA -> if (isSelected) Color(0xFF475569) else Color(0xFF1E293B)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .border(1.dp, if (isSelected) Color.White.copy(alpha = 0.6f) else Color(0xFF334155), RoundedCornerShape(8.dp))
                                .clickable { viewModel.setSessionStatus(st) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("status_chip_${st.name}")
                        ) {
                            Text(
                                text = st.label,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Fotografía y Captura de Asistencia
        state.snapshot?.let { snapshot ->
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Resumen de la sesión
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${snapshot.groupName} · ${snapshot.date} (Módulo ${snapshot.moduleNumber})",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Estatus de Sesión: ${snapshot.sessionStatus.label}",
                                fontSize = 12.sp,
                                color = if (snapshot.sessionStatus.isTeachingSession) Color(0xFF34D399) else Color(0xFFFBBF24)
                            )
                        }

                        // Badge de completitud
                        val badgeBg = when (state.captureState) {
                            CaptureState.COMPLETA -> Color(0xFF065F46)
                            CaptureState.PARCIAL -> Color(0xFF1E40AF)
                            CaptureState.PENDIENTE -> Color(0xFF78350F)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeBg)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("attendance_capture_state_badge")
                        ) {
                            Text(
                                text = state.captureState.label,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Indicadores rápidos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCardItem("Presentes", state.presentCount.toString(), Color(0xFF22C55E), "metric_present_count", Modifier.weight(1f))
                        MetricCardItem("Ausentes", state.absentCount.toString(), Color(0xFFEF4444), "metric_absent_count", Modifier.weight(1f))
                        MetricCardItem("Retardos", state.lateCount.toString(), Color(0xFFF59E0B), "metric_late_count", Modifier.weight(1f))
                        MetricCardItem("Pendientes", state.pendingCount.toString(), Color(0xFF94A3B8), "metric_pending_count", Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Acción Rápida: Todos presentes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lista de Alumnos Vigentes (${state.displayRoster.size})",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        OutlinedButton(
                            onClick = { viewModel.markAllPresent() },
                            modifier = Modifier.testTag("button_mark_all_present"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Marcar todos presentes", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Roster de Alumnos
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.displayRoster.forEach { item ->
                            StudentAttendanceRow(
                                entry = item,
                                onPresenceChange = { pres -> viewModel.setPresence(item.student.id, pres) },
                                onToggleLate = { viewModel.togglePunctuality(item.student.id) },
                                onMinutesChange = { min -> viewModel.setMinutesLate(item.student.id, min) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón Guardar
                    Button(
                        onClick = { viewModel.saveAttendance() },
                        enabled = !state.saving,
                        modifier = Modifier.fillMaxWidth().testTag("button_save_attendance"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        if (state.saving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Guardando captura…")
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (state.hasUnsavedChanges) "Guardar asistencia" else "Asistencia actualizada")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sección de Reporte PMC y Exportación
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Reporte Institucional PMC y Exportación",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cálculo formal por sesiones aplicables sin transformar capturas pendientes en inasistencias.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = reportStartDate,
                        onValueChange = { reportStartDate = it },
                        label = { Text("Inicio Periodo", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f).testTag("input_report_start"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = reportEndDate,
                        onValueChange = { reportEndDate = it },
                        label = { Text("Fin Periodo", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f).testTag("input_report_end"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Button(
                        onClick = { viewModel.loadReport(reportStartDate, reportEndDate) },
                        modifier = Modifier.testTag("button_load_report"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text("Consultar Reporte", fontSize = 12.sp)
                    }
                }

                // Despliegue de métricas del reporte si está cargado
                state.report?.let { rep ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .padding(12.dp)
                            .testTag("pmc_report_container")
                    ) {
                        Text(
                            text = "Resultados PMC: ${rep.groupName} (${rep.periodStart} al ${rep.periodEnd})",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricCardItem("Sesiones Realizadas", rep.sessionsHeld.toString(), Color(0xFF38BDF8), "report_sessions_held", Modifier.weight(1f))
                            MetricCardItem("Sesiones Canceladas", rep.sessionsCancelled.toString(), Color(0xFFFBBF24), "report_sessions_cancelled", Modifier.weight(1f))
                            MetricCardItem("Oportunidades Aplicables", rep.totalApplicableOpportunities.toString(), Color(0xFF818CF8), "report_applicable_opportunities", Modifier.weight(1f))
                            MetricCardItem("Registros Capturados", rep.totalCapturedRecords.toString(), Color(0xFF34D399), "report_captured_records", Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricCardItem("Cobertura de Captura", "${rep.coveragePct}%", if (rep.isCompleteCoverage) Color(0xFF4ADE80) else Color(0xFFFBBF24), "report_coverage_pct", Modifier.weight(1f))
                            MetricCardItem("Presencias Totales", rep.totalAttendanceEvents.toString(), Color(0xFF4ADE80), "report_total_presences", Modifier.weight(1f))
                            MetricCardItem("Inasistencias Totales", rep.totalAbsenceEvents.toString(), Color(0xFFF87171), "report_total_absences", Modifier.weight(1f))
                            MetricCardItem("Capturas Pendientes", rep.totalPendingRecords.toString(), Color(0xFF94A3B8), "report_pending_records", Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricCardItem("Eventos de Retardo", rep.totalTardinessEvents.toString(), Color(0xFFFBBF24), "report_total_tardiness", Modifier.weight(1f))
                            MetricCardItem("Alumnos con Retardo", rep.distinctStudentsWithTardiness.toString(), Color(0xFFF59E0B), "report_distinct_tardy_students", Modifier.weight(1f))
                            MetricCardItem("Tasa Asistencia Colectiva", "${rep.overallAttendanceRatePct}%", Color(0xFF60A5FA), "report_overall_rate", Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Aviso de Estatus de Cobertura y Umbrales Colegiados
                        Text(
                            text = "ℹ ${rep.rateStatusNotice}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (rep.isCompleteCoverage) Color(0xFF86EFAC) else Color(0xFFFCD34D),
                            modifier = Modifier.testTag("report_coverage_status_note")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ℹ ${rep.thresholds.statusNotice}",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1),
                            modifier = Modifier.testTag("report_thresholds_note")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Botones de Descarga de CSV
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.exportAggregatedCsv() },
                                modifier = Modifier.weight(1f).testTag("button_export_pmc_csv"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Descargar Reporte PMC (CSV)", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { viewModel.exportNominalCsv() },
                                modifier = Modifier.weight(1f).testTag("button_export_nominal_csv"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Padrón Nominal Restringido (CSV)", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCardItem(
    title: String,
    value: String,
    valueColor: Color,
    tag: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
            .padding(8.dp)
            .testTag(tag)
    ) {
        Column {
            Text(title, fontSize = 10.sp, color = Color(0xFF94A3B8))
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
private fun StudentAttendanceRow(
    entry: AttendanceStudentEntry,
    onPresenceChange: (AttendancePresence) -> Unit,
    onToggleLate: () -> Unit,
    onMinutesChange: (Int) -> Unit
) {
    val isPresent = entry.isPresent
    val isAbsent = entry.isAbsent
    val isLate = entry.isLate

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E293B).copy(alpha = 0.6f))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
            .padding(10.dp)
            .testTag("student_row_${entry.student.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${entry.student.listNumber}. ${entry.student.fullName}",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (entry.isPending) {
                    Text("Pendiente", fontSize = 11.sp, color = Color(0xFF94A3B8))
                } else if (isPresent) {
                    Text(
                        text = if (isLate) "Presente con Retardo (${entry.minutesLate} min)" else "Presente a tiempo",
                        fontSize = 11.sp,
                        color = if (isLate) Color(0xFFFBBF24) else Color(0xFF4ADE80)
                    )
                } else if (isAbsent) {
                    Text("Inasistencia", fontSize = 11.sp, color = Color(0xFFF87171))
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón PRESENTE
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isPresent) Color(0xFF15803D) else Color(0xFF334155))
                    .clickable { onPresenceChange(AttendancePresence.PRESENTE) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("btn_present_${entry.student.id}")
            ) {
                Text(
                    text = "Presente",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = if (isPresent) FontWeight.Bold else FontWeight.Normal
                )
            }

            // Botón AUSENTE
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isAbsent) Color(0xFFB91C1C) else Color(0xFF334155))
                    .clickable { onPresenceChange(AttendancePresence.AUSENTE) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("btn_absent_${entry.student.id}")
            ) {
                Text(
                    text = "Ausente",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = if (isAbsent) FontWeight.Bold else FontWeight.Normal
                )
            }

            // Estatus Retardo (Sólo disponible si está presente)
            if (isPresent) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isLate) Color(0xFFB45309) else Color(0xFF1E293B))
                        .border(1.dp, if (isLate) Color(0xFFFBBF24) else Color(0xFF475569), RoundedCornerShape(6.dp))
                        .clickable { onToggleLate() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("btn_tardy_${entry.student.id}")
                ) {
                    Text(
                        text = if (isLate) "Retardo (${entry.minutesLate}m)" else "+ Retardo",
                        color = if (isLate) Color.White else Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        fontWeight = if (isLate) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
