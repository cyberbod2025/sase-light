package com.example.ui.enrollment

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.auth.StaffAction
import com.example.data.auth.StaffPermissions
import com.example.data.auth.StaffRole
import com.example.data.enrollment.*
import com.example.ui.*
import com.example.ui.components.buttons.SasePrimaryButton
import com.example.viewmodel.LabViewModel
import com.example.viewmodel.Screen
import com.example.viewmodel.visibleSidebarItems
import kotlinx.coroutines.launch

@Composable
fun EnrollmentCutoffReportScreen(
    viewModel: LabViewModel
) {
    val session by viewModel.session.collectAsState()
    val allStudents by viewModel.saseStudents.collectAsState()

    var cohortStartDate by remember { mutableStateOf("2026-08-28") }
    var cutoffDate by remember { mutableStateOf("2026-10-31") }
    var selectedFilterGroup by remember { mutableStateOf("TODOS") }
    var selectedFilterStatus by remember { mutableStateOf("TODOS") }
    var exportedCsvContent by remember { mutableStateOf<String?>(null) }
    var exportedCsvTitle by remember { mutableStateOf("Reporte Exportado") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var snapshot by remember { mutableStateOf<EnrollmentCutoffSnapshot?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val activeRole = session?.activeRole ?: StaffRole.DOCENTE
    val canViewNominal = StaffPermissions.canPerform(activeRole, StaffAction.RECORD_SCHOOL_MOVEMENT)

    LaunchedEffect(cutoffDate, cohortStartDate, allStudents) {
        isLoading = true
        try {
            snapshot = viewModel.schoolMovementRepository.calculateCutoff(
                cutoffDate = cutoffDate,
                cohortStartDate = cohortStartDate
            )
        } catch (e: Exception) {
            statusMessage = "Error al calcular el corte: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    val currentSnapshot = snapshot
    val filteredRecords = remember(currentSnapshot, selectedFilterGroup, selectedFilterStatus) {
        if (currentSnapshot == null) emptyList()
        else {
            currentSnapshot.studentDetails.filter { record ->
                val matchGroup = selectedFilterGroup == "TODOS" || record.assignedGroup == selectedFilterGroup
                val matchStatus = when (selectedFilterStatus) {
                    "TODOS" -> true
                    "ACTIVO" -> record.status == StudentCutoffStatus.ACTIVO
                    "TRASLADADO" -> record.status == StudentCutoffStatus.TRASLADADO
                    "BAJA_ADMINISTRATIVA" -> record.status == StudentCutoffStatus.BAJA_ADMINISTRATIVA
                    "ABANDONO_CONFIRMADO" -> record.status == StudentCutoffStatus.ABANDONO_CONFIRMADO
                    "PENDIENTE_ACLARACION" -> record.status == StudentCutoffStatus.PENDIENTE_ACLARACION
                    "NO_INGRESADO" -> record.status == StudentCutoffStatus.NO_INGRESADO
                    else -> true
                }
                matchGroup && matchStatus
            }
        }
    }

    BoxWithConstraints(modifier = SaseBackgroundModifier()) {
        val isMobile = maxWidth < 850.dp
        val sidebarItems = visibleSidebarItems(session)

        val screenContent = @Composable {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(if (isMobile) 16.dp else 24.dp)
            ) {
                // Encabezado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isMobile) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menú", tint = SaseNavy)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Column {
                            Text(
                                text = "Corte Institucional de Matrícula",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = if (isMobile) 20.sp else 24.sp,
                                color = SaseNavy
                            )
                            Text(
                                text = "Evidencias objetivas y trazables para el PMC (Ciclo Escolar 2026-2027)",
                                fontSize = if (isMobile) 11.sp else 12.sp,
                                color = SaseMuted
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { viewModel.navigateTo(Screen.SecretaryDashboard) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SaseNavy)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Volver")
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Selector de Fecha y Parámetros
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Parámetros Temporales de Corte (Configurables)",
                            fontWeight = FontWeight.Bold,
                            color = SaseNavy,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Fechas sujetas a validación del calendario escolar en el Consejo Técnico Escolar (CTE). Los botones representan referencias temporales sugeridas, no acuerdos rígidos.",
                            fontSize = 11.sp,
                            color = SaseMuted
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = cohortStartDate,
                                onValueChange = { cohortStartDate = it.trim() },
                                label = { Text("Base cohorte (YYYY-MM-DD)") },
                                modifier = Modifier.width(180.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = cutoffDate,
                                onValueChange = { cutoffDate = it.trim() },
                                label = { Text("Fecha corte (YYYY-MM-DD)") },
                                modifier = Modifier.width(180.dp),
                                singleLine = true
                            )

                            FilterChip(
                                selected = cutoffDate == "2026-08-28",
                                onClick = { cutoffDate = "2026-08-28" },
                                label = { Text("28 Ago (Ref. Apertura)") }
                            )
                            FilterChip(
                                selected = cutoffDate == "2026-09-30",
                                onClick = { cutoffDate = "2026-09-30" },
                                label = { Text("30 Sep (Ref. 1er Mes)") }
                            )
                            FilterChip(
                                selected = cutoffDate == "2026-10-31",
                                onClick = { cutoffDate = "2026-10-31" },
                                label = { Text("31 Oct (Ref. 1er Bimestre)") }
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            // Botón de exportación agregada PMC
                            OutlinedButton(
                                onClick = {
                                    if (currentSnapshot != null) {
                                        val csv = SchoolMovementExportHelper.generatePmcAggregatedCsv(currentSnapshot)
                                        exportedCsvContent = csv
                                        exportedCsvTitle = "Reporte Agregado PMC (Sin datos personales)"
                                        statusMessage = "Reporte agregado PMC generado."
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Exportar Agregado PMC")
                            }

                            // Botón de exportación nominal restringida
                            if (canViewNominal) {
                                SasePrimaryButton(
                                    text = "Exportar Padrón Nominal",
                                    onClick = {
                                        if (currentSnapshot != null) {
                                            try {
                                                val csv = SchoolMovementExportHelper.generateRestrictedNominalCsv(
                                                    snapshot = currentSnapshot,
                                                    requesterRole = activeRole
                                                )
                                                exportedCsvContent = csv
                                                exportedCsvTitle = "Padrón Nominal Detallado (Confidencial)"
                                                statusMessage = "Padrón nominal generado bajo perfil de Control Escolar."
                                            } catch (e: Exception) {
                                                statusMessage = e.message
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Métricas Institucionales Agregadas
                if (currentSnapshot != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricSummaryCard(
                            title = "Matrícula Activa",
                            value = "${currentSnapshot.totalActiveStudents}",
                            subtitle = "Altas: +${currentSnapshot.newEnrollmentsInPeriod} | Reincorp: +${currentSnapshot.reincorporationsTotal}",
                            color = SaseGreen,
                            modifier = Modifier.weight(1f)
                        )
                        MetricSummaryCard(
                            title = "Retención de Cohorte",
                            value = "${currentSnapshot.cohortRetentionRate}%",
                            subtitle = "${currentSnapshot.cohortRetainedTotal} de ${currentSnapshot.initialCohortTotal - currentSnapshot.transfersOutTotal} base",
                            color = SaseNavy,
                            modifier = Modifier.weight(1f)
                        )
                        MetricSummaryCard(
                            title = "Traslados a Otra Escuela",
                            value = "${currentSnapshot.transfersOutTotal}",
                            subtitle = "Con destino comprobable (No es abandono)",
                            color = SaseBlue,
                            modifier = Modifier.weight(1f)
                        )
                        MetricSummaryCard(
                            title = "Bajas Administrativas",
                            value = "${currentSnapshot.administrativeDropoutsTotal}",
                            subtitle = "Trámites formales documentados",
                            color = SaseCyan,
                            modifier = Modifier.weight(1f)
                        )
                        MetricSummaryCard(
                            title = "Abandono Confirmado",
                            value = "${currentSnapshot.confirmedDropoutsTotal}",
                            subtitle = "Deserción tras protocolo oficial",
                            color = SaseRed,
                            modifier = Modifier.weight(1f)
                        )
                        MetricSummaryCard(
                            title = "Por Aclarar",
                            value = "${currentSnapshot.pendingClarificationTotal}",
                            subtitle = "Investigación en curso",
                            color = SaseMuted,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Desglose por grupo escolar (Reporte Agregado de Colectivo)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Desglose por Grupo Escolar (Evidencia para Sesión de CTE)",
                                fontWeight = FontWeight.Bold,
                                color = SaseNavy,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SaseBgSoft, RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Grupo", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SaseNavy, modifier = Modifier.weight(1.2f))
                                Text("Matrícula Activa", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SaseNavy, modifier = Modifier.weight(1.5f))
                                Text("Cohorte Inicial", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SaseNavy, modifier = Modifier.weight(1.5f))
                                Text("Retención Cohorte", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SaseNavy, modifier = Modifier.weight(1.5f))
                                Text("Traslados Salientes", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SaseNavy, modifier = Modifier.weight(1.5f))
                                Text("Bajas Admin.", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SaseNavy, modifier = Modifier.weight(1.5f))
                                Text("Abandono Conf.", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SaseNavy, modifier = Modifier.weight(1.5f))
                                Text("Reincorp.", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SaseNavy, modifier = Modifier.weight(1.2f))
                                Text("Pendientes", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SaseNavy, modifier = Modifier.weight(1.2f))
                            }
                            HorizontalDivider(color = SaseBorder)
                            for (g in currentSnapshot.groupSummaries) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(g.groupName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SaseNavy, modifier = Modifier.weight(1.2f))
                                    Text("${g.activeCount}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = SaseGreen, modifier = Modifier.weight(1.5f))
                                    Text("${g.initialCohortCount}", fontSize = 12.sp, color = SaseText, modifier = Modifier.weight(1.5f))
                                    Text("${g.cohortRetentionRate}%", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SaseNavy, modifier = Modifier.weight(1.5f))
                                    Text("${g.transfersCount}", fontSize = 12.sp, color = SaseBlue, modifier = Modifier.weight(1.5f))
                                    Text("${g.administrativeDropoutsCount}", fontSize = 12.sp, color = SaseCyan, modifier = Modifier.weight(1.5f))
                                    Text("${g.confirmedDropoutsCount}", fontSize = 12.sp, color = if (g.confirmedDropoutsCount > 0) SaseRed else SaseText, modifier = Modifier.weight(1.5f))
                                    Text("${g.reincorporationsCount}", fontSize = 12.sp, color = SaseText, modifier = Modifier.weight(1.2f))
                                    Text("${g.pendingClarificationCount}", fontSize = 12.sp, color = SaseMuted, modifier = Modifier.weight(1.2f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Padrón Nominal (Solo si el rol cuenta con autorización explícita)
                    if (canViewNominal) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Padrón Nominal al Corte (Uso Exclusivo Control Escolar)",
                                            fontWeight = FontWeight.Bold,
                                            color = SaseNavy,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Información nominal restringida. No compartir en reuniones públicas de CTE.",
                                            fontSize = 11.sp,
                                            color = SaseRed
                                        )
                                    }

                                    // Filtro de estado
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("TODOS", "ACTIVO", "TRASLADADO", "BAJA_ADMINISTRATIVA", "ABANDONO_CONFIRMADO", "PENDIENTE_ACLARACION").forEach { st ->
                                            FilterChip(
                                                selected = selectedFilterStatus == st,
                                                onClick = { selectedFilterStatus = st },
                                                label = { Text(st, fontSize = 10.sp) }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SaseBgSoft, RoundedCornerShape(6.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Estudiante / CURP", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SaseNavy, modifier = Modifier.weight(2.5f))
                                    Text("Folio / Matrícula", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SaseNavy, modifier = Modifier.weight(1.4f))
                                    Text("Grupo Vigente", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SaseNavy, modifier = Modifier.weight(1.2f))
                                    Text("Estatus Formal", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SaseNavy, modifier = Modifier.weight(1.8f))
                                    Text("Fecha Efec.", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SaseNavy, modifier = Modifier.weight(1.1f))
                                    Text("Fuente / Criterio Documentado", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SaseNavy, modifier = Modifier.weight(3f))
                                }

                                HorizontalDivider(color = SaseBorder)

                                if (filteredRecords.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                                        Text("No hay alumnos con el filtro seleccionado.", color = SaseMuted)
                                    }
                                } else {
                                    filteredRecords.forEach { r ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(2.5f)) {
                                                Text(r.fullName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SaseNavy)
                                                Text(r.curp, fontSize = 10.sp, color = SaseMuted)
                                            }
                                            Text(r.officialId, fontSize = 11.sp, color = SaseText, modifier = Modifier.weight(1.4f))
                                            Text(r.assignedGroup, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = SaseNavy, modifier = Modifier.weight(1.2f))
                                            Text(
                                                r.status.label,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = when (r.status) {
                                                    StudentCutoffStatus.ACTIVO -> SaseGreen
                                                    StudentCutoffStatus.TRASLADADO -> SaseBlue
                                                    StudentCutoffStatus.BAJA_ADMINISTRATIVA -> SaseCyan
                                                    StudentCutoffStatus.ABANDONO_CONFIRMADO -> SaseRed
                                                    StudentCutoffStatus.PENDIENTE_ACLARACION -> SaseMuted
                                                    StudentCutoffStatus.NO_INGRESADO -> SaseMuted
                                                },
                                                modifier = Modifier.weight(1.8f)
                                            )
                                            Text(r.effectiveMovementDate, fontSize = 11.sp, color = SaseText, modifier = Modifier.weight(1.1f))
                                            Column(modifier = Modifier.weight(3f)) {
                                                if (!r.destinationSchool.isNullOrBlank()) {
                                                    Text("Destino: ${r.destinationSchool}", fontSize = 11.sp, color = SaseBlue)
                                                }
                                                if (!r.documentationSource.isNullOrBlank()) {
                                                    Text("Doc: ${r.documentationSource}", fontSize = 10.sp, color = SaseMuted)
                                                }
                                                if (!r.classificationCriteria.isNullOrBlank()) {
                                                    Text("Criterio: ${r.classificationCriteria}", fontSize = 10.sp, color = SaseText)
                                                }
                                                if (r.destinationSchool.isNullOrBlank() && r.documentationSource.isNullOrBlank()) {
                                                    Text("Registro regular", fontSize = 10.sp, color = SaseMuted)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SaseBgSoft),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = SaseMuted)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "El Padrón Nominal detallado está reservado al personal de Control Escolar y Dirección para salvaguarda de datos personales.",
                                    fontSize = 12.sp,
                                    color = SaseMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isMobile) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
                        SaseSidebar(
                            visibleItems = sidebarItems,
                            activeItem = "Inscripciones",
                            onItemClick = { item ->
                                scope.launch { drawerState.close() }
                                viewModel.navigateFromSecretarySidebar(item)
                            }
                        )
                    }
                }
            ) {
                screenContent()
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                SaseSidebar(
                    visibleItems = sidebarItems,
                    activeItem = "Inscripciones",
                    onItemClick = { item -> viewModel.navigateFromSecretarySidebar(item) }
                )
                screenContent()
            }
        }

        // Modal de Previsualización de Exportación
        if (exportedCsvContent != null) {
            Dialog(onDismissRequest = { exportedCsvContent = null }) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.85f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                exportedCsvTitle,
                                fontWeight = FontWeight.Bold,
                                color = SaseNavy,
                                fontSize = 16.sp
                            )
                            IconButton(onClick = { exportedCsvContent = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar")
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = exportedCsvContent.orEmpty(),
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(onClick = { exportedCsvContent = null }) {
                                Text("Aceptar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricSummaryCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 11.sp, color = SaseMuted, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 10.sp, color = SaseText)
        }
    }
}
