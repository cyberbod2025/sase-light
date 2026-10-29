package com.example.ui.enrollment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.example.data.Student
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
    val movements by viewModel.schoolMovementRepository.movements.collectAsState()

    var cutoffDate by remember { mutableStateOf("2026-10-18") }
    var selectedFilterGroup by remember { mutableStateOf("TODOS") }
    var selectedFilterStatus by remember { mutableStateOf("TODOS") }
    var exportedCsvContent by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Cálculo reactivo del snapshot de corte
    val snapshot = remember(cutoffDate, allStudents, movements) {
        if (cutoffDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
            SchoolMovementEvaluator.calculateCutoff(
                cutoffDate = cutoffDate,
                students = allStudents,
                movements = movements
            )
        } else {
            EnrollmentCutoffSnapshot(
                cutoffDate = cutoffDate,
                activeEnrollmentCount = 0,
                transfersOutCount = 0,
                dropoutsCount = 0,
                unenteredCount = 0,
                totalHistoricalUniverse = allStudents.size,
                groupBreakdown = emptyMap(),
                records = emptyList()
            )
        }
    }

    val filteredRecords = remember(snapshot, selectedFilterGroup, selectedFilterStatus) {
        snapshot.records.filter { record ->
            val matchGroup = selectedFilterGroup == "TODOS" || record.assignedGroup == selectedFilterGroup
            val matchStatus = when (selectedFilterStatus) {
                "TODOS" -> true
                "ACTIVO" -> record.statusType == StudentCutoffStatusType.ACTIVO
                "TRASLADO" -> record.statusType == StudentCutoffStatusType.TRASLADO
                "BAJA" -> record.statusType == StudentCutoffStatusType.BAJA
                "NO_INGRESADO" -> record.statusType == StudentCutoffStatusType.NO_INGRESADO
                else -> true
            }
            matchGroup && matchStatus
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
                // Header
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
                                text = "Trazabilidad de movimientos fechados para el PMC (Secundaria 310)",
                                fontSize = if (isMobile) 11.sp else 12.sp,
                                color = SaseMuted
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { viewModel.navigateTo(Screen.SecretaryDashboard) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SaseNavy)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Volver")
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Selector de Fecha de Corte y Presets
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Parámetros del Corte Institucional",
                            fontWeight = FontWeight.Bold,
                            color = SaseNavy,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = cutoffDate,
                                onValueChange = { cutoffDate = it.trim() },
                                label = { Text("Fecha de corte (YYYY-MM-DD)") },
                                modifier = Modifier.width(220.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SaseBlue,
                                    focusedLabelColor = SaseBlue
                                )
                            )

                            // Botones de presets comunes
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = cutoffDate == "2026-08-31",
                                    onClick = { cutoffDate = "2026-08-31" },
                                    label = { Text("31 Ago (Inicio)") }
                                )
                                FilterChip(
                                    selected = cutoffDate == "2026-09-30",
                                    onClick = { cutoffDate = "2026-09-30" },
                                    label = { Text("30 Sep") }
                                )
                                FilterChip(
                                    selected = cutoffDate == "2026-10-18",
                                    onClick = { cutoffDate = "2026-10-18" },
                                    label = { Text("18 Oct") }
                                )
                                FilterChip(
                                    selected = cutoffDate == "2026-10-31",
                                    onClick = { cutoffDate = "2026-10-31" },
                                    label = { Text("31 Oct") }
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            SasePrimaryButton(
                                text = "Exportar Reporte CSV",
                                onClick = {
                                    val staffName = session?.profile?.fullName ?: "Personal Autorizado"
                                    val csv = SchoolMovementExportHelper.generateCutoffCsv(snapshot, staffName)
                                    exportedCsvContent = csv
                                    statusMessage = "Reporte exportado exitosamente."
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Métricas Agregadas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricSummaryCard(
                        title = "Matrícula Activa al Corte",
                        value = "${snapshot.activeEnrollmentCount}",
                        subtitle = "Cobertura: ${"%.1f".format(snapshot.activeCoveragePercent)}%",
                        color = SaseGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricSummaryCard(
                        title = "Traslados a Otro Plantel",
                        value = "${snapshot.transfersOutCount}",
                        subtitle = "Con oficio (NO es deserción)",
                        color = SaseBlue,
                        modifier = Modifier.weight(1f)
                    )
                    MetricSummaryCard(
                        title = "Bajas Definitivas",
                        value = "${snapshot.dropoutsCount}",
                        subtitle = "Deserción documentada",
                        color = SaseRed,
                        modifier = Modifier.weight(1f)
                    )
                    MetricSummaryCard(
                        title = "Universo Histórico",
                        value = "${snapshot.totalHistoricalUniverse}",
                        subtitle = "${snapshot.unenteredCount} pendientes de ingreso",
                        color = SaseNavy,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Desglose por grupo
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Matrícula Activa Vigente por Grupo al $cutoffDate",
                            fontWeight = FontWeight.Bold,
                            color = SaseNavy,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (snapshot.groupBreakdown.isEmpty()) {
                                Text("Sin estudiantes activos registrados a esta fecha.", color = SaseMuted, fontSize = 12.sp)
                            } else {
                                for ((groupName, count) in snapshot.groupBreakdown) {
                                    Surface(
                                        color = SaseBgSoft,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.border(1.dp, SaseBorder, RoundedCornerShape(8.dp))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(groupName, fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("$count alumnos", color = SaseGreen, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Filtros de tabla
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Filtrar estado:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = SaseNavy)
                    listOf("TODOS", "ACTIVO", "TRASLADO", "BAJA", "NO_INGRESADO").forEach { st ->
                        FilterChip(
                            selected = selectedFilterStatus == st,
                            onClick = { selectedFilterStatus = st },
                            label = { Text(st) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabla Detallada
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Padrón Individual Evaluado al Corte (${filteredRecords.size} registros)",
                            fontWeight = FontWeight.Bold,
                            color = SaseNavy,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Encabezados
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SaseBgSoft, RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Alumno / CURP", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SaseNavy, modifier = Modifier.weight(2.5f))
                            Text("Matrícula", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SaseNavy, modifier = Modifier.weight(1.5f))
                            Text("Grupo al Corte", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SaseNavy, modifier = Modifier.weight(1.2f))
                            Text("Estado al Corte", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SaseNavy, modifier = Modifier.weight(1.8f))
                            Text("Fecha Mov.", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SaseNavy, modifier = Modifier.weight(1.2f))
                            Text("Motivo / Destino Oficial", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SaseNavy, modifier = Modifier.weight(3f))
                        }

                        HorizontalDivider(color = SaseBorder)

                        if (filteredRecords.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No se encontraron alumnos con los filtros seleccionados.", color = SaseMuted)
                            }
                        } else {
                            filteredRecords.forEach { record ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(2.5f)) {
                                        Text(record.fullName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SaseText)
                                        Text(record.curp, fontSize = 10.sp, color = SaseMuted)
                                    }
                                    Text(record.enrollmentId, fontSize = 11.sp, color = SaseText, modifier = Modifier.weight(1.5f))
                                    Text(record.assignedGroup, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = SaseNavy, modifier = Modifier.weight(1.2f))
                                    Box(modifier = Modifier.weight(1.8f)) {
                                        StatusBadge(record.statusType)
                                    }
                                    Text(record.effectiveDate, fontSize = 11.sp, color = SaseText, modifier = Modifier.weight(1.2f))
                                    Text(record.destinationOrReason, fontSize = 11.sp, color = SaseMuted, modifier = Modifier.weight(3f))
                                }
                                HorizontalDivider(color = SaseBorder.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }

        // Layout con Sidebar
        if (isMobile) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
                        SaseSidebar(
                            activeItem = "Corte de Matrícula",
                            visibleItems = sidebarItems,
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
                    activeItem = "Corte de Matrícula",
                    visibleItems = sidebarItems,
                    onItemClick = { item -> viewModel.navigateFromSecretarySidebar(item) }
                )
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    screenContent()
                }
            }
        }

        // Modal para visualizar la exportación CSV
        if (exportedCsvContent != null) {
            Dialog(onDismissRequest = { exportedCsvContent = null }) {
                Card(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Exportación CSV Generada", fontWeight = FontWeight.Bold, color = SaseNavy)
                            IconButton(onClick = { exportedCsvContent = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(SaseBgSoft, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = exportedCsvContent ?: "",
                                fontSize = 10.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = SaseNavy
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            SasePrimaryButton(
                                text = "Aceptar",
                                onClick = { exportedCsvContent = null }
                            )
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
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontSize = 11.sp, color = SaseMuted, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, fontSize = 10.sp, color = SaseMuted)
        }
    }
}

@Composable
private fun StatusBadge(status: StudentCutoffStatusType) {
    val (bgColor, textColor) = when (status) {
        StudentCutoffStatusType.ACTIVO -> Color(0xFFD1FAE5) to Color(0xFF065F46)
        StudentCutoffStatusType.TRASLADO -> Color(0xFFDBEAFE) to Color(0xFF1E40AF)
        StudentCutoffStatusType.BAJA -> Color(0xFFFEE2E2) to Color(0xFF991B1B)
        StudentCutoffStatusType.NO_INGRESADO -> Color(0xFFF3F4F6) to Color(0xFF4B5563)
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = status.label,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
