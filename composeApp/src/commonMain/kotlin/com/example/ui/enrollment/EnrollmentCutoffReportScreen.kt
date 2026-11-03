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
import androidx.compose.ui.platform.testTag
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Controlador de estado de interfaz para la pantalla de Corte Institucional y Movimientos Fechados.
 * Permite interacción directa en la UI y verificación determinista en pruebas de interfaz.
 */
class EnrollmentCutoffUiStateHolder(
    private val viewModel: LabViewModel
) {
    var cohortStartDate by mutableStateOf("2026-08-28")
    var cutoffDate by mutableStateOf("2026-10-31")
    var selectedFilterGroup by mutableStateOf("TODOS")
    var selectedFilterStatus by mutableStateOf("TODOS")

    var snapshot by mutableStateOf<EnrollmentCutoffSnapshot?>(null)
    var movementsHistory by mutableStateOf<List<SchoolMovementRecord>>(emptyList())
    var isLoading by mutableStateOf(false)
    var statusMessage by mutableStateOf<String?>(null)

    // Estado del formulario de captura de movimiento
    var showCaptureForm by mutableStateOf(false)
    var captureId by mutableStateOf("")
    var captureStudentId by mutableStateOf("")
    var captureMovementType by mutableStateOf(SchoolMovementType.TRASLADO_SALIENTE)
    var captureEffectiveDate by mutableStateOf("2026-10-20")
    var captureFromGroup by mutableStateOf("1-A")
    var captureToGroup by mutableStateOf("")
    var captureDestinationSchool by mutableStateOf("")
    var captureDocumentationSource by mutableStateOf("")
    var captureClassificationCriteria by mutableStateOf("")
    var captureIsCorrection by mutableStateOf(false)
    var captureSupersedesId by mutableStateOf("")
    var captureCorrectionReason by mutableStateOf("")
    var captureNotes by mutableStateOf("")

    // Estado de visualización de historial y descarga CSV
    var showHistorySection by mutableStateOf(true)
    var exportedCsvContent by mutableStateOf<String?>(null)
    var exportedCsvFilePath by mutableStateOf<String?>(null)
    var exportedCsvTitle by mutableStateOf("Reporte Exportado")

    val isAvailableInEnvironment: Boolean
        get() = viewModel.schoolMovementRepository.isAvailableInCurrentEnvironment

    val unavailabilityReason: String?
        get() = viewModel.schoolMovementRepository.unavailabilityReason

    suspend fun refreshCutoffAndHistory() {
        if (!isAvailableInEnvironment) {
            snapshot = null
            movementsHistory = emptyList()
            statusMessage = unavailabilityReason
            return
        }
        isLoading = true
        try {
            movementsHistory = viewModel.schoolMovementRepository.getAllMovements()
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

    fun openCaptureModal(defaultStudentId: String = "STU-002") {
        captureId = "MOV-UI-${(movementsHistory.size + 1).toString().padStart(3, '0')}"
        captureStudentId = defaultStudentId
        captureMovementType = SchoolMovementType.TRASLADO_SALIENTE
        captureEffectiveDate = cutoffDate
        captureFromGroup = "1-A"
        captureToGroup = ""
        captureDestinationSchool = ""
        captureDocumentationSource = ""
        captureClassificationCriteria = ""
        captureIsCorrection = false
        captureSupersedesId = ""
        captureCorrectionReason = ""
        captureNotes = ""
        showCaptureForm = true
    }

    suspend fun submitCapturedMovement(): Result<SchoolMovementRecord> {
        val activeSession = viewModel.session.value
        val activeRole = activeSession?.activeRole ?: StaffRole.DOCENTE
        if (!StaffPermissions.canPerform(activeRole, StaffAction.RECORD_SCHOOL_MOVEMENT)) {
            val err = IllegalStateException("Rol no autorizado ($activeRole) para registrar movimientos escolares.")
            statusMessage = err.message
            return Result.failure(err)
        }

        val depCategory = when (captureMovementType) {
            SchoolMovementType.TRASLADO_SALIENTE -> DepartureCategory.TRASLADO
            SchoolMovementType.BAJA_ADMINISTRATIVA -> DepartureCategory.BAJA_ADMINISTRATIVA
            SchoolMovementType.ABANDONO_CONFIRMADO -> DepartureCategory.ABANDONO_CONFIRMADO
            SchoolMovementType.PENDIENTE_ACLARACION -> DepartureCategory.PENDIENTE_ACLARACION
            else -> null
        }

        val record = SchoolMovementRecord(
            id = captureId.ifBlank { "MOV-UI-${movementsHistory.size + 1}" },
            institutionId = activeSession?.institutionId ?: "INST-001",
            studentId = captureStudentId.trim(),
            movementType = captureMovementType,
            effectiveDate = captureEffectiveDate.trim(),
            recordedAt = "${captureEffectiveDate.trim()}T10:00:00Z",
            recordedBy = activeSession?.profile?.email ?: activeRole.name,
            fromGroup = captureFromGroup.trim().ifBlank { null },
            toGroup = captureToGroup.trim().ifBlank { null },
            destinationSchool = captureDestinationSchool.trim().ifBlank { null },
            departureCategory = depCategory,
            documentationSource = captureDocumentationSource.trim().ifBlank { null },
            classificationCriteria = captureClassificationCriteria.trim().ifBlank { null },
            isCorrection = captureIsCorrection,
            supersedesMovementId = if (captureIsCorrection) captureSupersedesId.trim().ifBlank { null } else null,
            correctionReason = if (captureIsCorrection) captureCorrectionReason.trim().ifBlank { null } else null,
            notes = captureNotes.trim().ifBlank { null }
        )

        val result = viewModel.schoolMovementRepository.recordMovement(record)
        result.fold(
            onSuccess = { saved ->
                showCaptureForm = false
                refreshCutoffAndHistory()
                statusMessage = "Movimiento ${saved.id} (${saved.movementType.displayName}) guardado exitosamente."
            },
            onFailure = { error ->
                statusMessage = "Error al guardar movimiento: ${error.message}"
            }
        )
        return result
    }

    fun downloadAggregatedPmcCsv(): CsvExportArtifact? {
        val current = snapshot ?: return null
        val artifact = SchoolMovementExportHelper.downloadPmcAggregatedCsv(current)
        exportedCsvTitle = "Reporte Agregado PMC (Sin datos personales)"
        exportedCsvFilePath = artifact.filePath
        exportedCsvContent = artifact.content
        statusMessage = "Archivo CSV agregado descargado en: ${artifact.filePath}"
        return artifact
    }

    fun downloadRestrictedNominalCsv(): CsvExportArtifact? {
        val current = snapshot ?: return null
        val activeRole = viewModel.session.value?.activeRole ?: StaffRole.DOCENTE
        return try {
            val artifact = SchoolMovementExportHelper.downloadRestrictedNominalCsv(
                snapshot = current,
                requesterRole = activeRole
            )
            exportedCsvTitle = "Padrón Nominal Detallado (Confidencial)"
            exportedCsvFilePath = artifact.filePath
            exportedCsvContent = artifact.content
            statusMessage = "Archivo CSV nominal descargado en: ${artifact.filePath}"
            artifact
        } catch (e: Exception) {
            statusMessage = e.message
            null
        }
    }
}

@Composable
fun EnrollmentCutoffReportScreen(
    viewModel: LabViewModel,
    externalStateHolder: EnrollmentCutoffUiStateHolder? = null
) {
    val session by viewModel.session.collectAsState()
    val allStudents by viewModel.saseStudents.collectAsState()

    val stateHolder = remember(viewModel, externalStateHolder) {
        externalStateHolder ?: EnrollmentCutoffUiStateHolder(viewModel)
    }

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val activeRole = session?.activeRole ?: StaffRole.DOCENTE
    val canViewNominal = StaffPermissions.canPerform(activeRole, StaffAction.RECORD_SCHOOL_MOVEMENT)

    LaunchedEffect(stateHolder.cutoffDate, stateHolder.cohortStartDate, allStudents) {
        stateHolder.refreshCutoffAndHistory()
    }

    val currentSnapshot = stateHolder.snapshot
    val supersededIds = remember(stateHolder.movementsHistory) {
        stateHolder.movementsHistory
            .filter { it.isCorrection && !it.supersedesMovementId.isNullOrBlank() }
            .mapNotNull { it.supersedesMovementId }
            .toSet()
    }

    val filteredRecords = remember(currentSnapshot, stateHolder.selectedFilterGroup, stateHolder.selectedFilterStatus) {
        if (currentSnapshot == null) emptyList()
        else {
            currentSnapshot.studentDetails.filter { record ->
                val matchGroup = stateHolder.selectedFilterGroup == "TODOS" || record.assignedGroup == stateHolder.selectedFilterGroup
                val matchStatus = when (stateHolder.selectedFilterStatus) {
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (canViewNominal && stateHolder.isAvailableInEnvironment) {
                            SasePrimaryButton(
                                text = "Registrar Movimiento",
                                onClick = { stateHolder.openCaptureModal() },
                                modifier = Modifier.testTag("btn_open_capture_movement")
                            )
                        }
                        OutlinedButton(
                            onClick = { viewModel.navigateTo(Screen.SecretaryDashboard) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SaseNavy),
                            modifier = Modifier.testTag("btn_back_secretary_dashboard")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Volver")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mensaje de estado / confirmación de guardado o descarga
                if (!stateHolder.statusMessage.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("card_status_message"),
                        colors = CardDefaults.cardColors(containerColor = SaseBgSoft),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stateHolder.statusMessage.orEmpty(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SaseNavy,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { stateHolder.statusMessage = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar aviso", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Bloqueo explícito si la migración remota no está verificada en este entorno conectado
                if (!stateHolder.isAvailableInEnvironment) {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("card_environment_blocked"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3F3)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Función deshabilitada en este entorno conectado",
                                fontWeight = FontWeight.Bold,
                                color = SaseRed,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stateHolder.unavailabilityReason.orEmpty(),
                                fontSize = 12.sp,
                                color = SaseText
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

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
                                value = stateHolder.cohortStartDate,
                                onValueChange = { stateHolder.cohortStartDate = it.trim() },
                                label = { Text("Base cohorte (YYYY-MM-DD)") },
                                modifier = Modifier.width(180.dp).testTag("input_cohort_start_date"),
                                singleLine = true,
                                enabled = stateHolder.isAvailableInEnvironment
                            )

                            OutlinedTextField(
                                value = stateHolder.cutoffDate,
                                onValueChange = { stateHolder.cutoffDate = it.trim() },
                                label = { Text("Fecha corte (YYYY-MM-DD)") },
                                modifier = Modifier.width(180.dp).testTag("input_cutoff_date"),
                                singleLine = true,
                                enabled = stateHolder.isAvailableInEnvironment
                            )

                            FilterChip(
                                selected = stateHolder.cutoffDate == "2026-08-28",
                                onClick = { stateHolder.cutoffDate = "2026-08-28" },
                                label = { Text("28 Ago (Ref. Apertura)") },
                                enabled = stateHolder.isAvailableInEnvironment
                            )
                            FilterChip(
                                selected = stateHolder.cutoffDate == "2026-09-30",
                                onClick = { stateHolder.cutoffDate = "2026-09-30" },
                                label = { Text("30 Sep (Ref. 1er Mes)") },
                                enabled = stateHolder.isAvailableInEnvironment
                            )
                            FilterChip(
                                selected = stateHolder.cutoffDate == "2026-10-31",
                                onClick = { stateHolder.cutoffDate = "2026-10-31" },
                                label = { Text("31 Oct (Ref. 1er Bimestre)") },
                                enabled = stateHolder.isAvailableInEnvironment
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            // Botón de descarga agregada PMC (.CSV)
                            OutlinedButton(
                                onClick = { stateHolder.downloadAggregatedPmcCsv() },
                                enabled = stateHolder.isAvailableInEnvironment && currentSnapshot != null,
                                modifier = Modifier.testTag("btn_download_pmc_csv")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Descargar Agregado PMC (.CSV)")
                            }

                            // Botón de descarga nominal restringida (.CSV)
                            if (canViewNominal) {
                                SasePrimaryButton(
                                    text = "Descargar Padrón Nominal (.CSV)",
                                    onClick = { stateHolder.downloadRestrictedNominalCsv() },
                                    modifier = Modifier.testTag("btn_download_nominal_csv")
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Formulario de Captura de Movimiento Escolar Fechado (cuando está activo)
                if (stateHolder.showCaptureForm && canViewNominal) {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("card_capture_movement_form"),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Captura de Movimiento Escolar con Vigencia Fechada",
                                    fontWeight = FontWeight.Bold,
                                    color = SaseNavy,
                                    fontSize = 15.sp
                                )
                                IconButton(onClick = { stateHolder.showCaptureForm = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cerrar captura")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            // Selección de Tipo de Movimiento
                            Text("Tipo de movimiento:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SaseNavy)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SchoolMovementType.entries.forEach { mType ->
                                    FilterChip(
                                        selected = stateHolder.captureMovementType == mType,
                                        onClick = { stateHolder.captureMovementType = mType },
                                        label = { Text(mType.displayName, fontSize = 10.sp) },
                                        modifier = Modifier.testTag("chip_movement_type_${mType.name}")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = stateHolder.captureStudentId,
                                    onValueChange = { stateHolder.captureStudentId = it },
                                    label = { Text("ID Alumno (ej. STU-002)") },
                                    modifier = Modifier.weight(1f).testTag("input_movement_student_id"),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = stateHolder.captureEffectiveDate,
                                    onValueChange = { stateHolder.captureEffectiveDate = it },
                                    label = { Text("Fecha Efectiva (YYYY-MM-DD)") },
                                    modifier = Modifier.weight(1f).testTag("input_movement_effective_date"),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = stateHolder.captureFromGroup,
                                    onValueChange = { stateHolder.captureFromGroup = it },
                                    label = { Text("Grupo Origen") },
                                    modifier = Modifier.weight(0.7f).testTag("input_movement_from_group"),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = stateHolder.captureToGroup,
                                    onValueChange = { stateHolder.captureToGroup = it },
                                    label = { Text("Grupo Destino") },
                                    modifier = Modifier.weight(0.7f).testTag("input_movement_to_group"),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = stateHolder.captureDestinationSchool,
                                    onValueChange = { stateHolder.captureDestinationSchool = it },
                                    label = { Text("Escuela Destino (obligatorio en Traslado)") },
                                    modifier = Modifier.weight(1f).testTag("input_movement_destination_school"),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = stateHolder.captureDocumentationSource,
                                    onValueChange = { stateHolder.captureDocumentationSource = it },
                                    label = { Text("Fuente Documental Probatoria") },
                                    modifier = Modifier.weight(1f).testTag("input_movement_doc_source"),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = stateHolder.captureClassificationCriteria,
                                    onValueChange = { stateHolder.captureClassificationCriteria = it },
                                    label = { Text("Criterio / Motivo Institucional") },
                                    modifier = Modifier.weight(1f).testTag("input_movement_criteria"),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(onClick = { stateHolder.showCaptureForm = false }) {
                                    Text("Cancelar")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                SasePrimaryButton(
                                    text = "Guardar Movimiento",
                                    onClick = { scope.launch { stateHolder.submitCapturedMovement() } },
                                    modifier = Modifier.testTag("btn_save_movement")
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

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

                    // Historial Fechado de Movimientos (Bitácora de Movimientos y Correcciones)
                    if (canViewNominal) {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("card_movements_history"),
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
                                            text = "Historial Fechado de Movimientos Escolares (${stateHolder.movementsHistory.size})",
                                            fontWeight = FontWeight.Bold,
                                            color = SaseNavy,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Bitácora auditable ordenada por fecha efectiva, registro y folio (conserva correcciones históricas).",
                                            fontSize = 11.sp,
                                            color = SaseMuted
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { stateHolder.showHistorySection = !stateHolder.showHistorySection },
                                        modifier = Modifier.testTag("btn_toggle_history")
                                    ) {
                                        Text(if (stateHolder.showHistorySection) "Ocultar Historial" else "Mostrar Historial")
                                    }
                                }

                                if (stateHolder.showHistorySection) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = SaseBorder)
                                    for (mov in stateHolder.movementsHistory) {
                                        val isSuperseded = mov.id in supersededIds
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = mov.id,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (isSuperseded) SaseMuted else SaseNavy,
                                                modifier = Modifier.weight(1.0f)
                                            )
                                            Text(
                                                text = mov.studentId,
                                                fontSize = 11.sp,
                                                color = SaseText,
                                                modifier = Modifier.weight(1.0f)
                                            )
                                            Text(
                                                text = mov.movementType.displayName,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp,
                                                color = if (isSuperseded) SaseMuted else SaseBlue,
                                                modifier = Modifier.weight(1.6f)
                                            )
                                            Text(
                                                text = mov.effectiveDate,
                                                fontSize = 11.sp,
                                                color = SaseText,
                                                modifier = Modifier.weight(1.0f)
                                            )
                                            Text(
                                                text = mov.recordedBy,
                                                fontSize = 10.sp,
                                                color = SaseMuted,
                                                modifier = Modifier.weight(1.4f)
                                            )
                                            Column(modifier = Modifier.weight(2.8f)) {
                                                if (isSuperseded) {
                                                    Text("SUSTITUIDO POR CORRECCIÓN POSTERIOR", fontSize = 10.sp, color = SaseRed, fontWeight = FontWeight.Bold)
                                                } else if (mov.isCorrection) {
                                                    Text("CORRECCIÓN DE ${mov.supersedesMovementId}: ${mov.correctionReason}", fontSize = 10.sp, color = SaseGreen, fontWeight = FontWeight.Bold)
                                                }
                                                val detail = listOfNotNull(
                                                    mov.destinationSchool?.let { "Destino: $it" },
                                                    mov.documentationSource?.let { "Doc: $it" },
                                                    mov.classificationCriteria?.let { "Criterio: $it" }
                                                ).joinToString(" | ")
                                                if (detail.isNotBlank()) {
                                                    Text(detail, fontSize = 10.sp, color = SaseText)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

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
                                                selected = stateHolder.selectedFilterStatus == st,
                                                onClick = { stateHolder.selectedFilterStatus = st },
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

        // Modal de Confirmación de Descarga y Previsualización de CSV
        if (stateHolder.exportedCsvContent != null) {
            Dialog(onDismissRequest = { stateHolder.exportedCsvContent = null }) {
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
                            Column {
                                Text(
                                    stateHolder.exportedCsvTitle,
                                    fontWeight = FontWeight.Bold,
                                    color = SaseNavy,
                                    fontSize = 16.sp
                                )
                                if (!stateHolder.exportedCsvFilePath.isNullOrBlank()) {
                                    Text(
                                        text = "Archivo guardado en disco: ${stateHolder.exportedCsvFilePath}",
                                        fontSize = 11.sp,
                                        color = SaseGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            IconButton(onClick = { stateHolder.exportedCsvContent = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar")
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = stateHolder.exportedCsvContent.orEmpty(),
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(onClick = { stateHolder.exportedCsvContent = null }) {
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
