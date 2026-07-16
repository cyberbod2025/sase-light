package com.example.ui.student

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.InstitutionalRecordDataQuality
import com.example.data.InstitutionalStudentRecordKey
import com.example.data.MockSaseData
import com.example.data.SaseIncident
import com.example.data.SaseObservation
import com.example.data.Student
import com.example.data.enrollment.AnnualEnrollmentRecord
import com.example.ui.DataRow
import com.example.ui.SaseBackgroundModifier
import com.example.ui.SaseBgSoft
import com.example.ui.SaseBlue
import com.example.ui.SaseBorder
import com.example.ui.SaseCyan
import com.example.ui.SaseGreen
import com.example.ui.SaseGreenDark
import com.example.ui.SaseMuted
import com.example.ui.SaseNavy
import com.example.ui.SaseOrange
import com.example.ui.SaseRed
import com.example.ui.SaseSidebar
import com.example.ui.SaseText
import com.example.ui.SaseViolet
import com.example.ui.TutorItem
import com.example.ui.components.buttons.SasePrimaryButton
import com.example.ui.components.buttons.SaseSecondaryButton
import com.example.ui.components.cards.SaseCard
import com.example.ui.components.chips.SaseStatusChip
import com.example.ui.components.chips.SaseStatusVariant
import com.example.ui.components.feedback.SaseAlertCard
import com.example.ui.components.feedback.SaseAlertVariant
import com.example.ui.components.fields.SaseTextField
import com.example.ui.components.navigation.SaseSectionTabs
import com.example.ui.theme.SaseColors
import com.example.util.LocalToast
import com.example.viewmodel.AppRole
import com.example.viewmodel.LabViewModel
import com.example.viewmodel.OfficialEnrollmentResult
import com.example.viewmodel.PreApplicationViewModel
import com.example.data.presolicitud.PreApplicationAdministrativeChanges
import com.example.data.presolicitud.PreApplicationAdministrativeFieldChange
import com.example.data.presolicitud.PreApplicationAdministrativeDataSnapshot
import com.example.data.presolicitud.administrativeDataSnapshot
import com.example.data.presolicitud.PreApplicationStatus
import com.example.data.presolicitud.ReadinessStatus
import com.example.viewmodel.ReadinessResult
import com.example.data.presolicitud.UpdatePreApplicationAdministrativeDataRequest
import com.example.viewmodel.Screen
import kotlinx.coroutines.launch

private val officialEnrollmentPattern = Regex("^S310-[A-Z0-9]{10}-\\d{2}$")
private val annualV2EnrollmentPattern = Regex("^S310-\\d{6}-\\d$")
private val curpPattern = Regex("^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]\\d$")

private fun hasOfficialEnrollment(enrollmentId: String, curp: String): Boolean =
    annualV2EnrollmentPattern.matches(enrollmentId) ||
        (enrollmentId.matches(officialEnrollmentPattern) && curp.matches(curpPattern))

private fun visibleEnrollmentId(enrollmentId: String, curp: String): String =
    if (hasOfficialEnrollment(enrollmentId, curp)) enrollmentId else "Pendiente de asignación"

private fun visibleGroup(group: String): String = group.trim().ifBlank { "Pendiente de asignación" }

private fun visibleGrade(group: String): String = group.trim().firstOrNull()?.digitToIntOrNull()
    ?.let { "${it}°" }
    ?: "Pendiente de confirmar"

private fun visibleEnrollmentStatus(enrollmentId: String, status: String): String =
    if (annualV2EnrollmentPattern.matches(enrollmentId)) {
        "Inscripción anual V2 registrada"
    } else {
        status
    }

private fun enrollmentPendingReason(curp: String): String =
    if (!curp.matches(curpPattern)) {
        "La matrícula se asignará cuando la CURP y el alta oficial estén completas."
    } else {
        "Pendiente de alta oficial."
    }

@Composable
private fun RecordHeaderFacts(student: Student) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RecordHeaderFact(
                label = "Matrícula",
                value = visibleEnrollmentId(student.enrollmentId, student.curp),
                modifier = Modifier.weight(1.4f)
            )
            RecordHeaderFact(
                label = "Grupo",
                value = visibleGroup(student.group),
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RecordHeaderFact(
                label = "Grado",
                value = visibleGrade(student.group),
                modifier = Modifier.weight(0.75f)
            )
            RecordHeaderFact(
                label = "Ciclo escolar",
                value = student.schoolYear,
                modifier = Modifier.weight(1.25f)
            )
            RecordHeaderFact(
                label = "Turno",
                value = student.shift,
                modifier = Modifier.weight(0.75f)
            )
        }
        RecordHeaderFact(label = "CURP", value = student.curp)
    }
}

@Composable
private fun RecordHeaderFact(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = SaseText.copy(alpha = 0.65f), fontSize = 10.sp)
        Text(value, fontWeight = FontWeight.Bold, color = SaseText, fontSize = 12.sp)
    }
}

@Composable
private fun InstitutionalStudentRecordRoute(
    studentId: String,
    institutionalKey: InstitutionalStudentRecordKey?,
    annualEnrollments: List<AnnualEnrollmentRecord>,
    returnTo: Screen,
    viewModel: LabViewModel
) {
    val saseStudents by viewModel.saseStudents.collectAsState()
    val preApplications by PreApplicationViewModel.sharedPreApplications.collectAsState()
    val resolution = remember(studentId, institutionalKey, saseStudents, annualEnrollments, preApplications) {
        resolveInstitutionalStudentRecordForRoute(
            studentId = studentId,
            institutionalKey = institutionalKey,
            students = saseStudents,
            annualEnrollments = annualEnrollments,
            preApplications = preApplications
        )
    }
    val presentation = remember(resolution) {
        institutionalStudentRecordPresentation(resolution)
    }
    val currentStudent = remember(saseStudents, studentId) { saseStudents.find { it.id == studentId } }
    val isConverted = remember(presentation) {
        val folio = (presentation as? InstitutionalStudentRecordPresentation.Content)?.folio
        if (folio != null) preApplications.firstOrNull { it.folio == folio }?.readinessStatus == ReadinessStatus.CONVERTED
        else false
    }

    InstitutionalStudentRecordContent(
        presentation = presentation,
        student = currentStudent,
        isConverted = isConverted,
        onBack = { viewModel.navigateTo(returnTo) },
        onSaveStudent = { updatedStudent ->
            viewModel.updateStudent(updatedStudent)
            val folio = (presentation as? InstitutionalStudentRecordPresentation.Content)?.folio
            if (!folio.isNullOrBlank()) {
                val preApp = preApplications.firstOrNull { it.folio.trim().uppercase() == folio.trim().uppercase() }
                if (preApp != null && preApp.status == PreApplicationStatus.ENVIADA) {
                    PreApplicationViewModel.updatePreApplicationAdministrativeData(
                        UpdatePreApplicationAdministrativeDataRequest(
                            folio = folio,
                            expected = preApp.administrativeDataSnapshot(),
                            changes = PreApplicationAdministrativeChanges(
                                address = PreApplicationAdministrativeFieldChange.Replace(
                                    updatedStudent.address.trim().uppercase()
                                ),
                                phone = PreApplicationAdministrativeFieldChange.Replace(
                                    updatedStudent.tutorPhone.trim()
                                )
                            )
                        )
                    )
                }
            }
            viewModel.logSaseAudit("Expediente actualizado", "Secretaría", updatedStudent.fullName)
        },
        onLogAudit = { action, detail ->
            viewModel.logSaseAudit(action, "Secretaría", detail)
        }
    )
}

@Composable
private fun InstitutionalStudentRecordContent(
    presentation: InstitutionalStudentRecordPresentation,
    student: Student?,
    isConverted: Boolean = false,
    onBack: () -> Unit,
    onSaveStudent: (Student) -> Unit,
    onLogAudit: (String, String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember(student) { mutableStateOf(student?.fullName ?: "") }
    var editCurp by remember(student) { mutableStateOf(student?.curp ?: "") }
    val displayAddress by remember(presentation) {
        mutableStateOf(
            (presentation as? InstitutionalStudentRecordPresentation.Content)?.editableAddress ?: student?.address ?: ""
        )
    }
    var editAddress by remember(displayAddress) { mutableStateOf(displayAddress) }
    var editZipCode by remember(student) { mutableStateOf(student?.zipCode ?: "") }
    var editTutorName by remember(student) { mutableStateOf(student?.tutorName ?: "") }
    val displayPhone by remember(presentation) {
        mutableStateOf(
            (presentation as? InstitutionalStudentRecordPresentation.Content)?.editablePhone ?: student?.tutorPhone ?: ""
        )
    }
    var editTutorPhone by remember(displayPhone) { mutableStateOf(displayPhone) }
    var editEmail by remember(student) { mutableStateOf(student?.tutorEmail ?: "") }
    var editGroup by remember(student) { mutableStateOf(student?.group ?: "") }
    var toastMessage by remember { mutableStateOf("") }
    var showValidationNotice by remember { mutableStateOf(false) }
    var showFolioAcceptedNotice by remember { mutableStateOf(false) }
    var showGroupDialog by remember { mutableStateOf(false) }
    val toast = LocalToast.current
    val preAppFolio = (presentation as? InstitutionalStudentRecordPresentation.Content)?.folio ?: ""
    val hasPendingDocs = remember(preAppFolio) {
        preAppFolio.isNotBlank() && PreApplicationViewModel.sharedPreApplications.value
            .find { it.folio == preAppFolio }
            ?.let { preApp -> preApp.documentosDeclarados.any { !it.noAplica && !it.validado } }
            ?: false
    }

    BoxWithConstraints(modifier = SaseBackgroundModifier()) {
        val compact = maxWidth < 600.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(if (compact) 16.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Regresar",
                        tint = SaseNavy
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text("Expediente institucional", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = SaseNavy)
                    Text(
                        if (isEditing) "Modo edición" else "Información reconciliada por fuentes",
                        fontSize = 11.sp,
                        color = if (isEditing) SaseGreen else SaseText.copy(alpha = 0.55f)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (presentation is InstitutionalStudentRecordPresentation.Content && presentation.isEditable && !isEditing) {
                    SasePrimaryButton(
                        text = "Editar",
                        onClick = {
                            if (student != null) {
                                editName = student.fullName
                                editCurp = student.curp
                                editAddress = displayAddress
                                editZipCode = student.zipCode
                                editTutorName = student.tutorName
                                editTutorPhone = displayPhone
                                editEmail = student.tutorEmail
                                editGroup = student.group
                            }
                            isEditing = true
                        },
                        icon = Icons.Default.Edit,
                        containerColor = SaseBlue
                    )
                }
            }

            when (presentation) {
                is InstitutionalStudentRecordPresentation.Terminal -> SaseCard {
                    Text(presentation.title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = SaseRed)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(presentation.message, color = SaseText, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    SasePrimaryButton(text = "Volver", onClick = onBack, containerColor = SaseNavy)
                }
                is InstitutionalStudentRecordPresentation.Content -> {
                    if (isEditing && student != null) {
                        SaseCard {
                            Text("Editando datos del alumno", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = SaseGreen)
                            Spacer(modifier = Modifier.height(10.dp))
                            SaseTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = "Nombre completo"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SaseTextField(
                                value = editCurp,
                                onValueChange = { editCurp = it.uppercase() },
                                label = "CURP"
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = SaseBorder.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Contacto", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = SaseBlue)
                            Spacer(modifier = Modifier.height(8.dp))
                            SaseTextField(
                                value = editAddress,
                                onValueChange = { editAddress = it },
                                label = "Domicilio"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SaseTextField(
                                value = editZipCode,
                                onValueChange = { editZipCode = it },
                                label = "Código postal"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SaseTextField(
                                value = editTutorPhone,
                                onValueChange = { editTutorPhone = it },
                                label = "Teléfono del hogar"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SaseTextField(
                                value = editTutorName,
                                onValueChange = { editTutorName = it },
                                label = "Responsable"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SaseTextField(
                                value = editEmail,
                                onValueChange = { editEmail = it },
                                label = "Correo"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SaseTextField(
                                value = editGroup,
                                onValueChange = { editGroup = it },
                                label = "Grupo"
                            )
                        }
                    } else {
                        SaseCard {
                            Text(presentation.fullName, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = SaseNavy)
                            Spacer(modifier = Modifier.height(6.dp))
                            RecordHeaderFact("CURP", presentation.curp)
                        }
                        SaseCard {
                            Text("Datos institucionales", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = SaseNavy)
                            Spacer(modifier = Modifier.height(10.dp))
                            presentation.fields.forEach { field ->
                                val onClick = when {
                                    field.label == "Folio de pre-solicitud" && presentation.acceptFolioVisible -> {
                                        val folio = presentation.folio
                                        {
                        PreApplicationViewModel.approvePreApplication(folio)
                            showFolioAcceptedNotice = true
                            toast("Folio $folio aceptado")
                            onLogAudit("Folio aceptado", "Folio: $folio")
                                        }
                                    }
                                    field.label == "Grupo" && field.value == "Pendiente de asignación" -> {
                                        {
                                            showGroupDialog = true
                                        }
                                    }
                                    else -> null
                                }
                                InstitutionalPresentationRow(field, compact, onClick = onClick)
                                HorizontalDivider(color = SaseBorder.copy(alpha = 0.55f))
                            }
                        }
                        if (presentation.warnings.isNotEmpty()) {
                            SaseCard {
                                Text("Advertencias institucionales", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = SaseOrange)
                                Spacer(modifier = Modifier.height(8.dp))
                                presentation.warnings.forEach { warning ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = SaseOrange, modifier = Modifier.size(16.dp))
                                        Text(warning, color = SaseText, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    if (isEditing && student != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SaseSecondaryButton(
                                text = "Cancelar",
                                onClick = {
                                    isEditing = false
                                    toast("Edición cancelada")
                                },
                                modifier = Modifier.weight(1f)
                            )
                            SasePrimaryButton(
                                text = "Guardar",
                                onClick = {
                                    if (editName.isBlank() || editCurp.isBlank()) {
                                        toast("Nombre y CURP son obligatorios")
                                    } else {
                                        val updated = student.copy(
                                            fullName = editName.trim().uppercase(),
                                            curp = editCurp.trim().uppercase(),
                                            address = editAddress.trim().uppercase(),
                                            zipCode = editZipCode.trim(),
                                            tutorName = editTutorName.trim().uppercase(),
                                            tutorPhone = editTutorPhone.trim(),
                                            tutorEmail = editEmail.trim(),
                                            group = editGroup.trim()
                                        )
                                        onSaveStudent(updated)
                                        isEditing = false
                                        toast("Cambios guardados")
                                    }
                                },
                                icon = Icons.Default.Check,
                                containerColor = SaseGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    val showActions = (presentation.isEditable || presentation.acceptFolioVisible || presentation.canReopenReview) && !isEditing
                    if (showActions) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (presentation.isEditable) {
                                SasePrimaryButton(
                                    text = "Validar",
                                    onClick = {
                                        val folio = (presentation as? InstitutionalStudentRecordPresentation.Content)?.folio
                                        if (!folio.isNullOrBlank()) {
                                            PreApplicationViewModel.markReadyForOfficialEnrollment(folio)
                                        }
                                        showValidationNotice = true
                                        toast("Expediente validado correctamente")
                                        onLogAudit("Expediente validado", student?.fullName ?: "")
                                    },
                                    icon = Icons.Default.CheckCircle,
                                    containerColor = SaseGreen,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (presentation.acceptFolioVisible && presentation.folio.isNotBlank()) {
                                SasePrimaryButton(
                                    text = "Aceptar folio",
                                    onClick = {
                                        PreApplicationViewModel.approvePreApplication(presentation.folio)
                                        showFolioAcceptedNotice = true
                                        toast("Folio ${presentation.folio} aceptado")
                                        onLogAudit("Folio aceptado", "Folio: ${presentation.folio}")
                                    },
                                    icon = Icons.Default.Description,
                                    containerColor = SaseBlue,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (presentation.canReopenReview && presentation.folio.isNotBlank()) {
                                SasePrimaryButton(
                                    text = "Reabrir revisión",
                                    onClick = {
                                        PreApplicationViewModel.reopenReview(presentation.folio)
                                        toast("Revisión reabierta")
                                        onLogAudit("Revisión reabierta", "Folio: ${presentation.folio}")
                                    },
                                    icon = Icons.Default.Refresh,
                                    containerColor = SaseOrange,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showValidationNotice) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showValidationNotice = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SaseGreen.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SaseGreen, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Expediente validado", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = SaseGreen)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("La información del expediente ha sido revisada y validada por Secretaría.", color = SaseText, fontSize = 12.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    SasePrimaryButton(text = "Cerrar", onClick = { showValidationNotice = false }, containerColor = SaseGreen)
                }
            }
        }
    }

    if (showFolioAcceptedNotice) {
        val acceptedFolio = (presentation as? InstitutionalStudentRecordPresentation.Content)?.folio ?: ""
        androidx.compose.ui.window.Dialog(onDismissRequest = { showFolioAcceptedNotice = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SaseBlue.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = SaseBlue, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Folio aceptado", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = SaseBlue)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("El folio $acceptedFolio ha sido aceptado y registrado en el expediente.", color = SaseText, fontSize = 12.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    SasePrimaryButton(text = "Cerrar", onClick = { showFolioAcceptedNotice = false }, containerColor = SaseBlue)
                }
            }
        }
    }

    if (showGroupDialog) {
        GrupoDecisionDialog(
            folio = preAppFolio,
            onDismiss = { showGroupDialog = false },
            toast = toast
        )
    }
}

@Composable
private fun GrupoDecisionDialog(
    folio: String,
    onDismiss: () -> Unit,
    toast: (String) -> Unit
) {
    val preApps by PreApplicationViewModel.sharedPreApplications.collectAsState()
    val officialStudents by PreApplicationViewModel.officialStudents.collectAsState()
    val preApp = remember(folio, preApps) { preApps.find { it.folio == folio } }
    val officialStudent = remember(folio, officialStudents) { officialStudents.find { it.preApplicationFolio == folio } }
    var selectedGroup by remember(folio) { mutableStateOf<String?>(null) }
    var groupConfirmed by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var resultColor by remember { mutableStateOf(SaseGreen) }

    val grade = preApp?.gradoSolicitado ?: 0
    val groupOptions = if (grade in 1..3) PreApplicationViewModel.groupOptionsForGrade(grade) else emptyList()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SaseBorder),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Asignación de grupo", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = SaseNavy)
                Text("Folio: $folio", color = SaseMuted, fontSize = 11.sp)

                if (preApp == null) {
                    Text("No se encontró la pre-solicitud.", color = SaseOrange, fontSize = 12.sp)
                } else if (groupOptions.isEmpty()) {
                    Text("No hay grupos disponibles para el grado $grade.", color = SaseOrange, fontSize = 12.sp)
                } else {
                    Text("Grado: ${grade}°", color = SaseText, fontSize = 12.sp)

                    if (officialStudent != null) {
                        Text("Alta oficial iniciada. Confirma el grupo para completar la asignación.", color = SaseMuted, fontSize = 11.sp)
                    } else {
                        Text("Se iniciará el alta oficial con el grupo seleccionado.", color = SaseMuted, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = SaseBorder.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        groupOptions.forEach { option ->
                            FilterChip(
                                selected = selectedGroup == option,
                                onClick = { selectedGroup = option },
                                label = { Text(option, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SaseGreen.copy(alpha = 0.14f),
                                    selectedLabelColor = SaseGreen,
                                    labelColor = SaseMuted
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedGroup == option,
                                    borderColor = SaseBorder,
                                    selectedBorderColor = SaseGreen.copy(alpha = 0.35f)
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.42f))
                            .clickable { groupConfirmed = !groupConfirmed }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = groupConfirmed,
                            onCheckedChange = { groupConfirmed = it },
                            colors = CheckboxDefaults.colors(checkedColor = SaseGreen)
                        )
                        Text("Confirmar asignación de grupo", color = SaseText, fontSize = 11.sp)
                    }

                    SasePrimaryButton(
                        text = "Confirmar grupo",
                        onClick = {
                            val group = selectedGroup ?: return@SasePrimaryButton
                            if (officialStudent != null) {
                                val result = PreApplicationViewModel.confirmInitialGroup(folio, group)
                                resultMessage = result.message
                                resultColor = when (result) {
                                    is OfficialEnrollmentResult.Success -> SaseGreen
                                    else -> SaseOrange
                                }
                                if (result is OfficialEnrollmentResult.Success) {
                                    toast("Grupo $group confirmado")
                                    onDismiss()
                                }
                            } else {
                                val app = preApp
                                val enrollResult = PreApplicationViewModel.startOfficialEnrollment(app, group)
                                resultMessage = enrollResult.message
                                resultColor = when (enrollResult) {
                                    is OfficialEnrollmentResult.Success -> SaseGreen
                                    else -> SaseOrange
                                }
                                if (enrollResult is OfficialEnrollmentResult.Success) {
                                    val confirmResult = PreApplicationViewModel.confirmInitialGroup(folio, group)
                                    resultMessage = confirmResult.message
                                    resultColor = when (confirmResult) {
                                        is OfficialEnrollmentResult.Success -> SaseGreen
                                        else -> SaseOrange
                                    }
                                    if (confirmResult is OfficialEnrollmentResult.Success) {
                                        toast("Grupo $group asignado")
                                        onDismiss()
                                    }
                                }
                            }
                        },
                        enabled = selectedGroup != null && groupConfirmed,
                        containerColor = SaseGreen,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (resultMessage != null) {
                    Text(resultMessage ?: "", color = resultColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cerrar", color = SaseMuted)
                }
            }
        }
    }
}

@Composable
private fun InstitutionalPresentationRow(
    field: InstitutionalRecordPresentationField,
    compact: Boolean,
    onClick: (() -> Unit)? = null
) {
    val qualityColor = when (field.quality) {
        InstitutionalRecordDataQuality.CONFIRMED -> SaseGreen
        InstitutionalRecordDataQuality.PENDING -> SaseOrange
        InstitutionalRecordDataQuality.UNAVAILABLE -> SaseMuted
        InstitutionalRecordDataQuality.INCONSISTENT -> SaseRed
    }
    val qualityLabel = when (field.quality) {
        InstitutionalRecordDataQuality.CONFIRMED -> "Confirmado"
        InstitutionalRecordDataQuality.PENDING -> "Pendiente"
        InstitutionalRecordDataQuality.UNAVAILABLE -> "No disponible"
        InstitutionalRecordDataQuality.INCONSISTENT -> "Inconsistente"
    }
    val valueContent: @Composable () -> Unit = {
        Column {
            Text(field.label, color = SaseText.copy(alpha = 0.65f), fontSize = 10.sp)
            Text(field.value, color = if (onClick != null) SaseBlue else SaseText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
    val qualityBadge: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(qualityColor.copy(alpha = 0.12f))
                .border(1.dp, qualityColor.copy(alpha = 0.28f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(qualityLabel, color = qualityColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
    val baseModifier = if (onClick != null) {
        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable { onClick() }
    } else {
        Modifier.fillMaxWidth()
    }
    if (compact) {
        Column(modifier = baseModifier.padding(vertical = 9.dp)) {
            valueContent()
            Spacer(modifier = Modifier.height(6.dp))
            qualityBadge()
        }
    } else {
        Row(
            modifier = baseModifier.padding(vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) { valueContent() }
            qualityBadge()
        }
    }
}

@Composable
fun StudentRecordScreen(
    studentId: String,
    institutionalKey: InstitutionalStudentRecordKey? = null,
    returnTo: Screen = Screen.SecretaryDashboard,
    viewModel: LabViewModel,
    userRole: AppRole = AppRole.SECRETARIA
) {
    val annualEnrollments by MockSaseData.annualEnrollments.collectAsState()
    if (institutionalKey != null || annualEnrollments.any { it.studentId == studentId }) {
        InstitutionalStudentRecordRoute(
            studentId = studentId,
            institutionalKey = institutionalKey,
            annualEnrollments = annualEnrollments,
            returnTo = returnTo,
            viewModel = viewModel
        )
        return
    }

    val toast = LocalToast.current
    val students by viewModel.saseStudents.collectAsState()
    val student = remember(students, studentId) { students.find { it.id == studentId } }
    val preApps by PreApplicationViewModel.sharedPreApplications.collectAsState()
    val isConverted = remember(student, preApps) {
        val folio = student?.preApplicationFolio
        if (folio != null) preApps.firstOrNull { it.folio == folio }?.readinessStatus == ReadinessStatus.CONVERTED
        else false
    }
    val hasPendingDocs = remember(student, preApps) {
        val folio = student?.preApplicationFolio
        if (folio != null) preApps.firstOrNull { it.folio == folio }
            ?.let { it.documentosDeclarados.any { d -> !d.noAplica && !d.validado } }
            ?: false
        else false
    }

    var activeTab by remember { mutableStateOf("Resumen") }
    val tabs = listOf(
        "Resumen",
        "Datos personales",
        "Responsables",
        "Contexto",
        "Documentos",
        "Observaciones",
        "Historial"
    )

    // Modals states
    var showIncidentDialog by remember { mutableStateOf(false) }
    var incType by remember { mutableStateOf("Atraso") }
    var incDesc by remember { mutableStateOf("") }

    var showObsDialog by remember { mutableStateOf(false) }
    var obsText by remember { mutableStateOf("") }
    var obsCategory by remember { mutableStateOf("Académica") }

    var showEscalarDialog by remember { mutableStateOf(false) }
    var escalarNotes by remember { mutableStateOf("") }

    var showDocumentDialog by remember { mutableStateOf(false) }

    var showPhoneDialog by remember { mutableStateOf(false) }
    var phoneDialogNumber by remember { mutableStateOf("") }
    var phoneDialogName by remember { mutableStateOf("") }

    if (student == null) {
        Box(modifier = SaseBackgroundModifier(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Expediente no encontrado", fontWeight = FontWeight.Bold, color = SaseNavy)
                SasePrimaryButton(text = "Regresar", onClick = { viewModel.navigateTo(returnTo) })
            }
        }
        return
    }

    BoxWithConstraints(modifier = SaseBackgroundModifier()) {
        val isMobile = maxWidth < 1080.dp
        val isWide = maxWidth >= 1180.dp
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var sidebarCollapsed by remember { mutableStateOf(true) }

        val navigateFromSidebar: (String) -> Unit = { item ->
            viewModel.navigateFromSecretarySidebar(item)
        }

        val recordContent = @Composable {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(if (isMobile) 16.dp else 24.dp)
            ) {
                // Header with back button and action buttons (adaptive column/row)
                if (isMobile) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menú", tint = SaseNavy)
                            }
                            IconButton(onClick = { viewModel.navigateTo(returnTo) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = SaseNavy)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Expediente", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = SaseNavy)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SasePrimaryButton(
                                text = "Documento",
                                onClick = { showDocumentDialog = true },
                                icon = Icons.Default.Description,
                                containerColor = SaseBlue,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.navigateTo(returnTo) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = SaseNavy)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Expediente del Alumno", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = SaseNavy)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SasePrimaryButton(
                                text = "Ver documentos",
                                onClick = { showDocumentDialog = true },
                                icon = Icons.Default.Description,
                                containerColor = SaseBlue
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Ficha Perfil Principal (Header Perfil)
                SaseCard {
                    if (isMobile) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(SaseNavy.copy(alpha = 0.08f))
                                    .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                                    .padding(3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(15.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(SaseNavy.copy(alpha = 0.22f), SaseNavy.copy(alpha = 0.08f))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = "Avatar del alumno", tint = SaseNavy, modifier = Modifier.size(34.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(student.fullName, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = SaseNavy, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SaseGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(visibleEnrollmentStatus(student.enrollmentId, student.status), color = SaseGreenDark, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            RecordHeaderFacts(student)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar placeholder
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(SaseNavy.copy(alpha = 0.08f))
                                    .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(SaseNavy.copy(alpha = 0.22f), SaseNavy.copy(alpha = 0.08f))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = "Avatar del alumno", tint = SaseNavy, modifier = Modifier.size(40.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(student.fullName, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = SaseNavy)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SaseGreen.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(visibleEnrollmentStatus(student.enrollmentId, student.status), color = SaseGreenDark, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                RecordHeaderFacts(student)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = SaseBorder.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(10.dp))

                    if (!hasOfficialEnrollment(student.enrollmentId, student.curp)) {
                        Text(
                            enrollmentPendingReason(student.curp),
                            color = SaseOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = if (isMobile) TextAlign.Center else TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Pills Row (Riesgo, BAP, Seguro, Documentos)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isMobile) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val riesgoVariant = when (student.riskLevel) {
                            "Alto" -> SaseStatusVariant.ERROR
                            "Medio" -> SaseStatusVariant.WARNING
                            else -> SaseStatusVariant.SUCCESS
                        }
                        SaseStatusChip(label = "Riesgo: ${student.riskLevel}", variant = riesgoVariant)
                        val bapVariant = if (student.bap == "Sí") SaseStatusVariant.INFORMATION else SaseStatusVariant.NEUTRAL
                        SaseStatusChip(label = "BAP: ${student.bap}", variant = bapVariant)
                        SaseStatusChip(label = "Seguro escolar: ${student.schoolInsurance}", variant = SaseStatusVariant.SUCCESS)
                        val docVariant = if (student.documentationStatus == "Completa") SaseStatusVariant.SUCCESS else SaseStatusVariant.WARNING
                        SaseStatusChip(label = "Documentación: ${student.documentationStatus}", variant = docVariant)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                SaseSectionTabs(
                    tabs = tabs,
                    selectedTab = activeTab,
                    onTabSelected = { activeTab = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Content panels based on Tab
                AnimatedContent(
                    targetState = activeTab,
                    label = "TabContent"
                ) { currentTab ->
                    when (currentTab) {
                        "Resumen" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                if (!isWide) {
                                    // General data block
                                    SaseCard(modifier = Modifier.fillMaxWidth()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(SaseBlue))
    Spacer(Modifier.width(8.dp))
    Text("Datos generales", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
}
                                        Spacer(modifier = Modifier.height(10.dp))
                                        DataRow(label = "Fecha de nacimiento", value = student.birthDate)
                                        DataRow(label = "Edad", value = "${student.age} años")
                                        DataRow(label = "Lugar de nacimiento", value = student.birthPlace)
                                        DataRow(label = "Domicilio", value = student.address)
                                        DataRow(label = "Código postal", value = student.zipCode)
                                    }

                                    // Contacts block
                                    SaseCard(modifier = Modifier.fillMaxWidth()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(SaseCyan))
    Spacer(Modifier.width(8.dp))
    Text("Tutores y contacto de emergencia", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
}
                                        Spacer(modifier = Modifier.height(10.dp))
                                        if (student.tutorName.isNotBlank()) {
                                            TutorItem(
                                                name = student.tutorName,
                                                relation = student.tutorRelation,
                                                phone = student.tutorPhone,
                                                email = student.tutorEmail,
                                                onCall = { phoneDialogName = student.tutorName; phoneDialogNumber = student.tutorPhone; showPhoneDialog = true }
                                            )
                                        } else {
                                            Text("No hay tutor principal registrado.", color = SaseMuted, fontSize = 11.sp)
                                        }
                                    }

                                    // Attendance block
                                    if (isConverted) {
                                    SaseCard(modifier = Modifier.fillMaxWidth()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(SaseGreen))
    Spacer(Modifier.width(8.dp))
    Text("Asistencia", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
}
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier.size(70.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    progress = { student.attendancePercent / 100f },
                                                    color = SaseGreen,
                                                    trackColor = SaseBgSoft,
                                                    strokeWidth = 6.dp,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                Text("${student.attendancePercent}%", fontWeight = FontWeight.ExtraBold, color = SaseNavy, fontSize = 14.sp)
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text("Asistencias: ${student.attendances}", fontSize = 11.sp, color = SaseText)
                                                Text("Faltas justificadas: ${student.excusedAbsences}", fontSize = 11.sp, color = SaseOrange)
                                                Text("Faltas injustificadas: ${student.unexcusedAbsences}", fontSize = 11.sp, color = SaseRed)
                                            }
                                        }
                                    }
                                    }

                                    // Health block
                                    SaseCard(modifier = Modifier.fillMaxWidth()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(SaseCyan))
    Spacer(Modifier.width(8.dp))
    Text("Salud", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
}
                                        Spacer(modifier = Modifier.height(10.dp))
                                        DataRow(label = "Alergias", value = student.healthAlergies)
                                        DataRow(label = "Observaciones médicas", value = student.healthNotes)
                                        DataRow(label = "Medicamentos", value = student.healthMeds)
                                        DataRow(label = "Pases de salud", value = student.healthPasses)
                                    }

                                    // Incidents summary block
                                    if (isConverted) {
                                    SaseCard(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(SaseRed))
                                                Spacer(Modifier.width(8.dp))
                                                Text("Historial de incidencias", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
                                            }
                                            Text("Ver historial", color = SaseBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { activeTab = "Historial" })
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        if (student.schoolIncidents.isEmpty()) {
                                            Text("No hay incidencias registradas.", color = SaseMuted, fontSize = 11.sp)
                                        } else {
                                            student.schoolIncidents.take(3).forEach { incident ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(incident.type, fontWeight = FontWeight.Bold, color = SaseText, fontSize = 12.sp)
                                                        Text("Reportó: ${incident.reporter} · ${incident.date}", color = SaseMuted, fontSize = 10.sp)
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(if (incident.status == "Atendida") SaseGreen.copy(alpha = 0.12f) else SaseOrange.copy(alpha = 0.12f))
                                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(incident.status, color = if (incident.status == "Atendida") SaseGreenDark else SaseOrange, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                                    }
                                                }
                                                HorizontalDivider(color = SaseBorder.copy(alpha = 0.05f))
                                            }
                                        }
                                    }
                                    }

                                    // Orientation block
                                    SaseCard(modifier = Modifier.fillMaxWidth()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(SaseViolet))
    Spacer(Modifier.width(8.dp))
    Text("Orientación y trabajo social", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
}
                                        Spacer(modifier = Modifier.height(10.dp))
                                        DataRow(label = "Estado de seguimiento", value = student.orientationStatus)
                                        DataRow(label = "Última cita", value = student.orientationLastAppointment)
                                        DataRow(label = "Plan de intervención", value = student.orientationInterventionPlan)
                                        DataRow(label = "Responsable", value = student.orientationResponsible)
                                    }

                                    // Documents block
                                    SaseCard(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(SaseNavy))
                                                Spacer(Modifier.width(8.dp))
                                                Text("Documentos digitalizados", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
                                            }
                                            Text("Ver todos", color = SaseBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { activeTab = "Documentos" })
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        student.documents.take(3).forEach { doc ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(doc.name, fontWeight = FontWeight.Bold, color = SaseText, fontSize = 12.sp)
                                                    Text("Cotejado: ${doc.date}", color = SaseMuted, fontSize = 10.sp)
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Icon(Icons.Default.Check, contentDescription = "Validado", tint = SaseGreen, modifier = Modifier.size(14.dp))
                                                    Text(doc.status, color = SaseGreenDark, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                }
                                            }
                                            HorizontalDivider(color = SaseBorder.copy(alpha = 0.05f))
                                        }
                                    }

                                    // Observations block
                                    SaseCard(modifier = Modifier.fillMaxWidth()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(SaseOrange))
    Spacer(Modifier.width(8.dp))
    Text("Observaciones y bitácora", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
}
                                        Spacer(modifier = Modifier.height(10.dp))
                                        if (student.observations.isEmpty()) {
                                            Text("No hay observaciones registradas.", color = SaseMuted, fontSize = 11.sp)
                                        } else {
                                            student.observations.forEach { obs ->
                                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                    Text(obs.text, color = SaseText, fontSize = 11.sp)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text("Registrado por: ${obs.author} · ${obs.date} · [${obs.category}]", color = SaseMuted, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                                }
                                                HorizontalDivider(color = SaseBorder.copy(alpha = 0.05f))
                                            }
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        // General data block
                                        SaseCard(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(SaseBlue))
    Spacer(Modifier.width(8.dp))
    Text("Datos generales", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
}
                                            Spacer(modifier = Modifier.height(10.dp))
                                            DataRow(label = "Fecha de nacimiento", value = student.birthDate)
                                            DataRow(label = "Edad", value = "${student.age} años")
                                            DataRow(label = "Lugar de nacimiento", value = student.birthPlace)
                                            DataRow(label = "Domicilio", value = student.address)
                                            DataRow(label = "Código postal", value = student.zipCode)
                                        }

                                        // Contacts block
                                        SaseCard(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(SaseCyan))
    Spacer(Modifier.width(8.dp))
    Text("Tutores y contacto de emergencia", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
}
                                            Spacer(modifier = Modifier.height(10.dp))
                                            if (student.tutorName.isNotBlank()) {
                                                TutorItem(
                                                    name = student.tutorName,
                                                    relation = student.tutorRelation,
                                                    phone = student.tutorPhone,
                                                    email = student.tutorEmail,
                                                    onCall = { phoneDialogName = student.tutorName; phoneDialogNumber = student.tutorPhone; showPhoneDialog = true }
                                                )
                                            } else {
                                                Text("No hay tutor principal registrado.", color = SaseMuted, fontSize = 11.sp)
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        // Attendance block
                                        if (isConverted) {
                                        SaseCard(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(SaseGreen))
    Spacer(Modifier.width(8.dp))
    Text("Asistencia", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
}
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Circular Progress indicator
                                                Box(
                                                    modifier = Modifier.size(70.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    CircularProgressIndicator(
                                                        progress = { student.attendancePercent / 100f },
                                                        color = SaseGreen,
                                                        trackColor = SaseBgSoft,
                                                        strokeWidth = 6.dp,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                    Text("${student.attendancePercent}%", fontWeight = FontWeight.ExtraBold, color = SaseNavy, fontSize = 14.sp)
                                                }
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text("Asistencias: ${student.attendances}", fontSize = 11.sp, color = SaseText)
                                                    Text("Faltas justificadas: ${student.excusedAbsences}", fontSize = 11.sp, color = SaseOrange)
                                                    Text("Faltas injustificadas: ${student.unexcusedAbsences}", fontSize = 11.sp, color = SaseRed)
                                                }
                                            }
                                        }
                                        }

                                        // Health block
                                        SaseCard(modifier = Modifier.weight(1.5f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(SaseCyan))
    Spacer(Modifier.width(8.dp))
    Text("Salud", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
}
                                            Spacer(modifier = Modifier.height(10.dp))
                                            DataRow(label = "Alergias", value = student.healthAlergies)
                                            DataRow(label = "Observaciones médicas", value = student.healthNotes)
                                            DataRow(label = "Medicamentos", value = student.healthMeds)
                                            DataRow(label = "Pases de salud", value = student.healthPasses)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        // Incidents summary block
                                        if (isConverted) {
                                        SaseCard(modifier = Modifier.weight(1.5f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Historial de incidencias", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
                                            Text("Ver historial", color = if (hasPendingDocs) SaseMuted else SaseBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(enabled = !hasPendingDocs) { activeTab = "Historial" })
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            if (student.schoolIncidents.isEmpty()) {
                                                Text("No hay incidencias registradas.", color = SaseMuted, fontSize = 11.sp)
                                            } else {
                                                student.schoolIncidents.take(3).forEach { incident ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text(incident.type, fontWeight = FontWeight.Bold, color = SaseText, fontSize = 12.sp)
                                                            Text("Reportó: ${incident.reporter} · ${incident.date}", color = SaseMuted, fontSize = 10.sp)
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(if (incident.status == "Atendida") SaseGreen.copy(alpha = 0.12f) else SaseOrange.copy(alpha = 0.12f))
                                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(incident.status, color = if (incident.status == "Atendida") SaseGreenDark else SaseOrange, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                                        }
                                                    }
                                                    HorizontalDivider(color = SaseBorder.copy(alpha = 0.05f))
                                                }
                                            }
                                        }
                                        }

                                        // Orientation block
                                        SaseCard(modifier = Modifier.weight(1.2f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(SaseViolet))
    Spacer(Modifier.width(8.dp))
    Text("Orientación y trabajo social", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
}
                                            Spacer(modifier = Modifier.height(10.dp))
                                        DataRow(label = "Estado de seguimiento", value = student.orientationStatus)
                                            DataRow(label = "Última cita", value = student.orientationLastAppointment)
                                            DataRow(label = "Plan de intervención", value = student.orientationInterventionPlan)
                                            DataRow(label = "Responsable", value = student.orientationResponsible)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        // Documents block
                                        SaseCard(modifier = Modifier.weight(1.2f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Documentos digitalizados", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
                                                Text("Ver todos", color = SaseBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { activeTab = "Documentos" })
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            student.documents.take(3).forEach { doc ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(doc.name, fontWeight = FontWeight.Bold, color = SaseText, fontSize = 12.sp)
                                                        Text("Cotejado: ${doc.date}", color = SaseMuted, fontSize = 10.sp)
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Icon(Icons.Default.Check, contentDescription = "Validado", tint = SaseGreen, modifier = Modifier.size(14.dp))
                                                        Text(doc.status, color = SaseGreenDark, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                    }
                                                }
                                                HorizontalDivider(color = SaseBorder.copy(alpha = 0.05f))
                                            }
                                        }

                                        // Observations block
                                        SaseCard(modifier = Modifier.weight(1.5f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(SaseOrange))
    Spacer(Modifier.width(8.dp))
    Text("Observaciones y bitácora", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
}
                                            Spacer(modifier = Modifier.height(10.dp))
                                            if (student.observations.isEmpty()) {
                                                Text("No hay observaciones registradas.", color = SaseMuted, fontSize = 11.sp)
                                            } else {
                                                student.observations.forEach { obs ->
                                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                        Text(obs.text, color = SaseText, fontSize = 11.sp)
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text("Registrado por: ${obs.author} · ${obs.date} · [${obs.category}]", color = SaseMuted, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                                    }
                                                    HorizontalDivider(color = SaseBorder.copy(alpha = 0.05f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "Datos personales" -> {
                            SaseCard(modifier = Modifier.fillMaxWidth()) {
                                Text("Datos personales del alumno", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                DataRow(label = "Nombre completo", value = student.fullName)
                                DataRow(label = "CURP", value = student.curp)
                                DataRow(label = "Matrícula escolar", value = visibleEnrollmentId(student.enrollmentId, student.curp))
                                DataRow(label = "Grupo", value = visibleGroup(student.group))
                                DataRow(label = "Grado", value = visibleGrade(student.group))
                                DataRow(label = "Ciclo escolar", value = student.schoolYear)
                                DataRow(label = "Estado de inscripción", value = visibleEnrollmentStatus(student.enrollmentId, student.status))
                                if (!hasOfficialEnrollment(student.enrollmentId, student.curp)) {
                                    DataRow(label = "Estado de matrícula", value = enrollmentPendingReason(student.curp))
                                }
                                DataRow(label = "Fecha de nacimiento", value = student.birthDate)
                                DataRow(label = "Edad", value = "${student.age} años")
                                DataRow(label = "Lugar de nacimiento", value = student.birthPlace)
                                DataRow(label = "Domicilio familiar", value = student.address)
                                DataRow(label = "Código postal", value = student.zipCode)
                                DataRow(label = "Estado del seguro escolar", value = student.schoolInsurance)
                                DataRow(label = "Expediente auditado", value = "Sí, por Secretaría")
                            }
                        }

                        "Responsables" -> {
                            SaseCard(modifier = Modifier.fillMaxWidth()) {
                                Text("Responsables y contactos", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                TutorItem(
                                    name = student.tutorName,
                                    relation = student.tutorRelation,
                                    phone = student.tutorPhone,
                                    email = student.tutorEmail,
                                    onCall = { phoneDialogName = student.tutorName; phoneDialogNumber = student.tutorPhone; showPhoneDialog = true }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Contacto alterno o de emergencia", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                if (student.emergencyContactName.isNotBlank()) {
                                    TutorItem(
                                        name = student.emergencyContactName,
                                        relation = student.emergencyContactRelation,
                                        phone = student.emergencyContactPhone,
                                        email = student.emergencyContactEmail,
                                        onCall = { phoneDialogName = student.emergencyContactName; phoneDialogNumber = student.emergencyContactPhone; showPhoneDialog = true }
                                    )
                                } else {
                                    Text("No registrado", color = SaseMuted, fontSize = 12.sp)
                                }
                            }
                        }

                        "Contexto" -> {
                            SaseCard(modifier = Modifier.fillMaxWidth()) {
                                Text("Contexto y seguimiento del alumno", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                DataRow(label = "Asistencia acumulada", value = "${student.attendancePercent}%")
                                DataRow(label = "Asistencias registradas", value = student.attendances.toString())
                                DataRow(label = "Faltas justificadas", value = student.excusedAbsences.toString())
                                DataRow(label = "Faltas injustificadas", value = student.unexcusedAbsences.toString())
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SaseBorder.copy(alpha = 0.12f))
                                DataRow(label = "Alergias", value = student.healthAlergies)
                                DataRow(label = "Medicamentos", value = student.healthMeds)
                                DataRow(label = "Observaciones médicas", value = student.healthNotes)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SaseBorder.copy(alpha = 0.12f))
                                DataRow(label = "Estado de seguimiento", value = student.orientationStatus)
                                DataRow(label = "Plan de intervención", value = student.orientationInterventionPlan)
                                DataRow(label = "Responsable de orientación", value = student.orientationResponsible)
                            }
                        }

                        "Historial" -> {
                            SaseCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Historial completo de incidencias", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 16.sp)
                                    SasePrimaryButton(
                                        text = "Registrar incidencia",
                                        onClick = { showIncidentDialog = true },
                                        icon = Icons.Default.Add,
                                        containerColor = SaseRed
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                if (student.schoolIncidents.isEmpty()) {
                                    Text("El alumno no cuenta con incidencias disciplinarias ni asistenciales.", color = SaseMuted, fontSize = 12.sp)
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        student.schoolIncidents.forEach { incident ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(incident.type, fontWeight = FontWeight.Bold, color = SaseText, fontSize = 13.sp)
                                                    Text("Reportado por: ${incident.reporter} · Fecha: ${incident.date}", color = SaseMuted, fontSize = 11.sp)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (incident.status == "Atendida") SaseGreen.copy(alpha = 0.12f) else SaseOrange.copy(alpha = 0.12f))
                                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                                ) {
                                                    Text(incident.status, color = if (incident.status == "Atendida") SaseGreenDark else SaseOrange, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "Salud" -> {
                            SaseCard(modifier = Modifier.fillMaxWidth()) {
                                Text("Historial médico y salud del Alumno", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                DataRow(label = "Alergias severas", value = student.healthAlergies)
                                DataRow(label = "Medicamentos de uso diario", value = student.healthMeds)
                                DataRow(label = "Historial o pases de emergencia", value = student.healthPasses)
                                DataRow(label = "Notas clínicas generales", value = student.healthNotes)
                            }
                        }

                        "Orientación" -> {
                            SaseCard(modifier = Modifier.fillMaxWidth()) {
                                Text("Bitácora de Orientación y Trabajo social", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                DataRow(label = "Estatus escolar", value = student.orientationStatus)
                                DataRow(label = "Fecha de última sesión", value = student.orientationLastAppointment)
                                DataRow(label = "Plan remedial de intervención", value = student.orientationInterventionPlan)
                                DataRow(label = "Psicólogo/Orientador responsable", value = student.orientationResponsible)
                            }
                        }

                        "Documentos" -> {
                            SaseCard(modifier = Modifier.fillMaxWidth()) {
                                Text("Expediente digitalizado (Documentos obligatorios)", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    student.documents.forEach { doc ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SaseGreen, modifier = Modifier.size(18.dp))
                                                Text(doc.name, fontWeight = FontWeight.Bold, color = SaseText, fontSize = 13.sp)
                                            }
                                            Text("Cotejado: ${doc.date} · [${doc.status}]", color = SaseGreenDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        "Observaciones" -> {
                            SaseCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Bitácora pedagógica de observaciones", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 16.sp)
                                    SasePrimaryButton(
                                        text = "Agregar observación",
                                        onClick = { showObsDialog = true },
                                        icon = Icons.Default.Add,
                                        containerColor = SaseBlue
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                if (student.observations.isEmpty()) {
                                    Text("No se han registrado comentarios académicos ni conductuales.", color = SaseMuted, fontSize = 12.sp)
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        student.observations.forEach { obs ->
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                                    .padding(12.dp)
                                            ) {
                                                Text(obs.text, color = SaseText, fontSize = 12.sp)
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text("Por: ${obs.author} · ${obs.date} · Categoría: ${obs.category}", color = SaseMuted, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom floating-like action bar (fijo o inferior)
                SaseCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = SaseBlue.copy(alpha = 0.4f),
                    containerColor = SaseNavy.copy(alpha = 0.03f)
                ) {
                    if (!isWide) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SasePrimaryButton(
                                text = "Agregar observación",
                                onClick = { showObsDialog = true },
                                icon = Icons.AutoMirrored.Filled.Comment,
                                containerColor = SaseBlue,
                                modifier = Modifier.fillMaxWidth()
                            )
                            SasePrimaryButton(
                                text = "Ver documentos",
                                onClick = { showDocumentDialog = true },
                                icon = Icons.AutoMirrored.Filled.Assignment,
                                containerColor = SaseViolet,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SasePrimaryButton(
                                text = "Agregar observación",
                                onClick = { showObsDialog = true },
                                icon = Icons.AutoMirrored.Filled.Comment,
                                containerColor = SaseBlue,
                                modifier = Modifier.weight(1f)
                            )
                            SasePrimaryButton(
                                text = "Ver documentos",
                                onClick = { showDocumentDialog = true },
                                icon = Icons.AutoMirrored.Filled.Assignment,
                                containerColor = SaseViolet,
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                    }
                }
            }
        }

    // Modal dialog to register incident
    if (showIncidentDialog) {
        Dialog(onDismissRequest = { showIncidentDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FBFF)),
                border = BorderStroke(1.dp, SaseBlue.copy(alpha = 0.22f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Registrar incidencia disciplinaria/asistencia", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 16.sp)

                    // Incident types
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Atraso", "Falta de material", "Conducta").forEach { type ->
                            val selected = incType == type
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) SaseRed else SaseBgSoft)
                                    .clickable { incType = type }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(type, color = if (selected) Color.White else SaseText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    SaseTextField(
                        value = incDesc,
                        onValueChange = { incDesc = it },
                        label = "Descripción",
                        placeholder = "Ej. El alumno llegó 15 minutos tarde..."
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = { showIncidentDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Cancelar", color = SaseMuted)
                        }
                        SasePrimaryButton(
                            text = "Registrar",
                            onClick = {
                                if (incDesc.isNotBlank()) {
                                    val incs = student.schoolIncidents.toMutableList()
                                    incs.add(0, SaseIncident("Hoy", incType, "Secretaría", "En seguimiento"))
                                    val updated = student.copy(schoolIncidents = incs)
                                    viewModel.updateStudent(updated)
                                    viewModel.logSaseAudit("Incidencia reportada", "Secretaría", "${student.fullName} - $incType")
                                    showIncidentDialog = false
                                    incDesc = ""
                                    toast("Incidencia registrada")
                                } else {
                                    toast("Favor de agregar descripción")
                                }
                            },
                            containerColor = SaseRed,
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                }
            }
        }
    }

    // Modal dialog to add observations
    if (showObsDialog) {
        Dialog(onDismissRequest = { showObsDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SaseBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Agregar observación pedagógica", fontWeight = FontWeight.ExtraBold, color = SaseNavy, fontSize = 17.sp)
                    Text("Registra una nota breve y clara para el expediente escolar.", color = SaseMuted, fontSize = 11.sp)

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Académica", "Conductual", "Tutoría").forEach { cat ->
                            val selected = obsCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) SaseBlue else SaseBgSoft)
                                    .clickable { obsCategory = cat }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(cat, color = if (selected) Color.White else SaseText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = obsText,
                        onValueChange = { obsText = it },
                        label = { Text("Comentarios de observación") },
                        placeholder = { Text("Ej. Requiere seguimiento académico en próximas sesiones") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp),
                        minLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SaseText,
                            unfocusedTextColor = SaseText,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White.copy(alpha = 0.82f),
                            focusedBorderColor = SaseBlue,
                            unfocusedBorderColor = SaseBorder,
                            focusedLabelColor = SaseBlue,
                            unfocusedLabelColor = SaseMuted,
                            focusedPlaceholderColor = SaseMuted,
                            unfocusedPlaceholderColor = SaseMuted
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = { showObsDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Cerrar", color = SaseMuted, fontWeight = FontWeight.Bold)
                        }
                        SasePrimaryButton(
                            text = "Agregar",
                            onClick = {
                                if (obsText.isNotBlank()) {
                                    val obs = student.observations.toMutableList()
                                    obs.add(0, SaseObservation(obsText, "Secretaría", "Hoy", obsCategory))
                                    val updated = student.copy(observations = obs)
                                    viewModel.updateStudent(updated)
                                    viewModel.logSaseAudit("Observación registrada", "Secretaría", student.fullName)
                                    showObsDialog = false
                                    obsText = ""
                                    toast("Observación registrada")
                                } else {
                                    toast("Favor de agregar observaciones")
                                }
                            },
                            containerColor = SaseBlue,
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                }
            }
        }
    }

    // Modal dialog to Escalar caso
    if (showEscalarDialog) {
        Dialog(onDismissRequest = { showEscalarDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SaseBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Escalar caso a Dirección / Psicología", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 16.sp)
                    Text("Describa el motivo del escalamiento y asigne prioridad.", color = SaseMuted, fontSize = 11.sp)

                    SaseTextField(
                        value = escalarNotes,
                        onValueChange = { escalarNotes = it },
                        label = "Motivo de canalización"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = { showEscalarDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Cancelar", color = SaseMuted)
                        }
                        SasePrimaryButton(
                            text = "Escalar Caso",
                            onClick = {
                                if (escalarNotes.isNotBlank()) {
                                    viewModel.logSaseAudit("Caso escalado", "Dirección", "${student.fullName} - Canalización")
                                    showEscalarDialog = false
                                    escalarNotes = ""
                                    toast("Caso escalado con éxito.")
                                } else {
                                    toast("Favor de agregar motivo")
                                }
                            },
                            containerColor = SaseOrange,
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                }
            }
        }
    }

    // Modal dialog to select document templates
    if (showDocumentDialog) {
        Dialog(onDismissRequest = { showDocumentDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SaseBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Documentos institucionales", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 16.sp)
                    Text("Vista mock de plantillas para ${student.fullName}. La generación de PDF e impresión no está disponible en esta versión.", color = SaseMuted, fontSize = 11.sp)

                    listOf(
                        "Constancia de estudios",
                        "Historial académico",
                        "Carta compromiso institucional",
                        "Citatorio a padres de familia",
                        "Justificante escolar"
                    ).forEach { name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    toast("'$name' es una plantilla mock; PDF no disponible")
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = SaseCyan, modifier = Modifier.size(18.dp))
                            Text(name, color = SaseText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = SaseBorder.copy(alpha = 0.05f))
                    }

                    TextButton(
                        onClick = { showDocumentDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cerrar", color = SaseMuted)
                    }
                }
            }
        }
    }

    if (showPhoneDialog) {
        Dialog(onDismissRequest = { showPhoneDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SaseBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = SaseGreen, modifier = Modifier.size(48.dp))
                    Text("Llamar a $phoneDialogName", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 16.sp)
                    Text(phoneDialogNumber, color = SaseText, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Copia el número y marca desde tu teléfono.", color = SaseMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
                    TextButton(
                        onClick = { showPhoneDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cerrar", color = SaseMuted)
                    }
                }
            }
        }
    }

        if (isMobile) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = SaseNavy,
                        modifier = Modifier.width(280.dp)
                    ) {
                        SaseSidebar(
                            activeItem = "Expedientes",
                            modifier = Modifier.fillMaxHeight(),
                            collapsed = false,
                            onItemClick = navigateFromSidebar
                        )
                    }
                }
            ) {
                recordContent()
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                SaseSidebar(
                    activeItem = "Expedientes",
                    collapsed = sidebarCollapsed,
                    onToggleCollapse = { sidebarCollapsed = !sidebarCollapsed },
                    modifier = Modifier.fillMaxHeight(),
                    onItemClick = navigateFromSidebar
                )
                Box(modifier = Modifier.weight(1f)) {
                    recordContent()
                }
            }
        }
    }
}
