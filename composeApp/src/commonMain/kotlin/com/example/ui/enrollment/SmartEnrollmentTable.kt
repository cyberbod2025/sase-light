package com.example.ui.enrollment

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Student
import com.example.ui.GlassCard
import com.example.ui.SaseBgSoft
import com.example.ui.SaseBlue
import com.example.ui.SaseBorder
import com.example.ui.SaseGreen
import com.example.ui.SaseGreenDark
import com.example.ui.SaseMuted
import com.example.ui.SaseNavy
import com.example.ui.SaseOrange
import com.example.ui.SaseRed
import com.example.data.DerivedEnrollmentStatus
import com.example.data.deriveEnrollmentStatus
import com.example.ui.SaseText

private val officialEnrollmentPattern = Regex("^S310-[A-Z0-9]{10}-\\d{2}$")
private val curpPattern = Regex("^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]\\d$")

private fun hasOfficialEnrollment(student: Student): Boolean =
    student.enrollmentId.matches(officialEnrollmentPattern) && student.curp.matches(curpPattern)

private fun visibleEnrollmentId(student: Student): String =
    if (hasOfficialEnrollment(student)) student.enrollmentId else "Por asignar"

private fun enrollmentPendingReason(student: Student): String =
    if (!student.curp.matches(curpPattern)) {
        "La matrícula se asignará cuando la CURP y el alta oficial estén completas."
    } else {
        "Pendiente de alta oficial."
    }

// Smart table filters
@Composable
fun SmartEnrollmentTable(
    students: List<Student>,
    onStudentClick: (String) -> Unit,
    onRegisterObsClick: (Student) -> Unit,
    onDocChipClick: (Student) -> Unit = {},
    onStatusChipClick: (Student) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedGroupFilter by remember { mutableStateOf("Todos") }
    var selectedStatusFilter by remember { mutableStateOf("Todos") }
    var selectedRiskFilter by remember { mutableStateOf("Todos") }

    val filteredList = remember(students, selectedGroupFilter, selectedStatusFilter, selectedRiskFilter) {
        students.filter { student ->
            val groupMatches = selectedGroupFilter == "Todos" || student.group == selectedGroupFilter
            val statusMatches = selectedStatusFilter == "Todos" || student.status == selectedStatusFilter
            val riskMatches = selectedRiskFilter == "Todos" || student.riskLevel == selectedRiskFilter
            groupMatches && statusMatches && riskMatches
        }
    }

    GlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Matrícula Inteligente", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SaseNavy)
            Text(
                "${filteredList.size} registros",
                color = SaseNavy,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal filter row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Group Filter selector
            FilterChipCompact(
                label = "Grupo: $selectedGroupFilter",
                options = listOf("Todos", "1°A", "1°B", "2°A", "2°B", "3°C"),
                onSelect = { selectedGroupFilter = it }
            )

            // Status Filter selector
            FilterChipCompact(
                label = "Estatus: $selectedStatusFilter",
                options = listOf("Todos", "Activo", "En riesgo", "Nuevo ingreso"),
                onSelect = { selectedStatusFilter = it }
            )

            // Risk Filter selector
            FilterChipCompact(
                label = "Riesgo: $selectedRiskFilter",
                options = listOf("Todos", "Bajo", "Medio", "Alto"),
                onSelect = { selectedRiskFilter = it }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        BoxWithConstraints {
            val isCompact = maxWidth < 500.dp

            if (filteredList.isEmpty()) {
                EmptyState(students.isEmpty())
            } else if (isCompact) {
                CompactStudentList(
                    students = filteredList,
                    onStudentClick = onStudentClick,
                    onDocChipClick = onDocChipClick,
                    onStatusChipClick = onStatusChipClick
                )
            } else {
                WideStudentTable(
                    students = filteredList,
                    onStudentClick = onStudentClick,
                    onRegisterObsClick = onRegisterObsClick,
                    onDocChipClick = onDocChipClick,
                    onStatusChipClick = onStatusChipClick
                )
            }
        }
    }
}

@Composable
private fun EmptyState(noStudents: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                if (noStudents) Icons.Default.School else Icons.Default.Warning,
                contentDescription = null,
                tint = SaseMuted.copy(alpha = 0.4f),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (noStudents) "Sin matrícula cargada" else "Sin coincidencias",
                color = SaseNavy,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                if (noStudents) "No hay alumnos cargados en el sistema."
                else "Ajusta grupo, estatus o riesgo para ampliar los resultados.",
                color = SaseMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun WideStudentTable(
    students: List<Student>,
    onStudentClick: (String) -> Unit,
    onRegisterObsClick: (Student) -> Unit,
    onDocChipClick: (Student) -> Unit = {},
    onStatusChipClick: (Student) -> Unit = {}
) {
    Column(modifier = Modifier.widthIn(min = 480.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SaseBgSoft.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Alumno", color = SaseMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(2.5f))
            Text("Grupo", color = SaseMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.8f))
            Text("Estatus", color = SaseMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.2f))
            Text("Tutor", color = SaseMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(2.0f))
            Text("Documentos", color = SaseMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.5f))
        }

        Spacer(modifier = Modifier.height(6.dp))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            students.forEach { student ->
                var expanded by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(2.5f + 0.8f + 2.0f)
                            .semantics { contentDescription = "student_card_${student.id}" }
                            .testTag("student_card_${student.id}")
                            .clickable { onStudentClick(student.id) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(2.5f), verticalAlignment = Alignment.CenterVertically) {
                            StudentAvatar(student)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(student.fullName, color = SaseText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Matrícula: ${visibleEnrollmentId(student)}", color = SaseMuted, fontSize = 10.sp)
                            }
                        }
                        Text(student.group, color = SaseText, fontSize = 12.sp, modifier = Modifier.weight(0.8f))
                        Text(student.tutorName, color = SaseText, fontSize = 12.sp, modifier = Modifier.weight(2.0f))
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Ver expediente",
                            tint = SaseMuted,
                            modifier = Modifier.size(14.dp).testTag("student_chevron_${student.id}")
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .padding(end = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                    Box(
                        modifier = Modifier
                            .wrapContentSize()
                            .clip(RoundedCornerShape(999.dp))
                            .semantics { contentDescription = "student_status_chip_${student.id}" }
                            .testTag("student_status_chip_${student.id}")
                            .clickable { onStatusChipClick(student) }
                    ) {
                        val derived = deriveEnrollmentStatus(student)
                        StatusPill(status = derived.label)
                    }
                }
                    Row(modifier = Modifier.weight(1.5f), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .wrapContentSize()
                                .clip(RoundedCornerShape(6.dp))
                                .semantics { contentDescription = "student_docs_chip_${student.id}" }
                                .testTag("student_docs_chip_${student.id}")
                                .clickable { onDocChipClick(student) }
                        ) {
                            DocBadge(student.documentationStatus)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Box {
                            IconButton(
                                onClick = { expanded = true },
                                modifier = Modifier.size(22.dp).semantics { contentDescription = "student_menu_${student.id}" }.testTag("student_menu_${student.id}")
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = SaseMuted, modifier = Modifier.size(16.dp))
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Abrir expediente") },
                                    onClick = { expanded = false; onStudentClick(student.id) },
                                    modifier = Modifier.testTag("menu_open_record_${student.id}")
                                )
                                DropdownMenuItem(
                                    text = { Text("Editar observaciones") },
                                    onClick = { expanded = false; onRegisterObsClick(student) },
                                    modifier = Modifier.testTag("menu_edit_observations_${student.id}")
                                )
                                if (student.documentationStatus != "Completa") {
                                    DropdownMenuItem(
                                        text = { Text("Ver documentos pendientes") },
                                        onClick = { expanded = false; onDocChipClick(student) },
                                        modifier = Modifier.testTag("menu_view_pending_docs_${student.id}")
                                    )
                                }
                                if (!hasOfficialEnrollment(student)) {
                                    DropdownMenuItem(
                                        text = { Text("Procesar alta oficial") },
                                        onClick = { expanded = false; onDocChipClick(student) },
                                        modifier = Modifier.testTag("menu_process_enrollment_${student.id}")
                                    )
                                }
                                if (hasOfficialEnrollment(student)) {
                                    DropdownMenuItem(
                                        text = { Text("Ver credencial") },
                                        onClick = { expanded = false; onStudentClick(student.id) },
                                        modifier = Modifier.testTag("menu_view_credentials_${student.id}")
                                    )
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(color = SaseBorder.copy(alpha = 0.05f), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun CompactStudentList(
    students: List<Student>,
    onStudentClick: (String) -> Unit,
    onDocChipClick: (Student) -> Unit = {},
    onStatusChipClick: (Student) -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        students.forEach { student ->
            var expanded by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.72f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "student_card_${student.id}" }
                            .testTag("student_card_${student.id}")
                            .clickable { onStudentClick(student.id) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StudentAvatar(student)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(student.fullName, color = SaseText, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            Text("Matrícula: ${visibleEnrollmentId(student)}", color = SaseMuted, fontSize = 10.sp)
                            if (!hasOfficialEnrollment(student)) {
                                Text(enrollmentPendingReason(student), color = SaseOrange, fontSize = 9.sp, lineHeight = 11.sp)
                            }
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Ver expediente",
                            tint = SaseNavy,
                            modifier = Modifier.size(18.dp).testTag("student_chevron_${student.id}")
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Box {
                        IconButton(
                            onClick = { expanded = true },
                            modifier = Modifier.size(32.dp).semantics { contentDescription = "student_menu_${student.id}" }.testTag("student_menu_${student.id}")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = SaseNavy, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Abrir expediente") },
                                onClick = { expanded = false; onStudentClick(student.id) },
                                modifier = Modifier.testTag("menu_open_record_${student.id}")
                            )
                            if (student.documentationStatus != "Completa") {
                                DropdownMenuItem(
                                    text = { Text("Ver documentos pendientes") },
                                    onClick = { expanded = false; onDocChipClick(student) },
                                    modifier = Modifier.testTag("menu_view_pending_docs_${student.id}")
                                )
                            }
                            if (!hasOfficialEnrollment(student)) {
                                DropdownMenuItem(
                                    text = { Text("Procesar alta oficial") },
                                    onClick = { expanded = false; onDocChipClick(student) },
                                    modifier = Modifier.testTag("menu_process_enrollment_${student.id}")
                                )
                            }
                            if (hasOfficialEnrollment(student)) {
                                DropdownMenuItem(
                                    text = { Text("Ver credencial") },
                                    onClick = { expanded = false; onStudentClick(student.id) },
                                    modifier = Modifier.testTag("menu_view_credentials_${student.id}")
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(student.group, color = SaseNavy, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .wrapContentSize()
                            .clip(RoundedCornerShape(999.dp))
                            .semantics { contentDescription = "student_status_chip_${student.id}" }
                            .testTag("student_status_chip_${student.id}")
                            .clickable { onStatusChipClick(student) }
                    ) {
                        val derived = deriveEnrollmentStatus(student)
                        StatusPill(status = derived.label)
                    }
                    Box(
                        modifier = Modifier
                            .wrapContentSize()
                            .clip(RoundedCornerShape(6.dp))
                            .semantics { contentDescription = "student_docs_chip_${student.id}" }
                            .testTag("student_docs_chip_${student.id}")
                            .clickable { onDocChipClick(student) }
                    ) {
                        DocBadge(student.documentationStatus)
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentAvatar(student: Student) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(
                when (student.riskLevel) {
                    "Alto" -> SaseRed.copy(alpha = 0.15f)
                    "Medio" -> SaseOrange.copy(alpha = 0.15f)
                    else -> SaseGreen.copy(alpha = 0.15f)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            tint = when (student.riskLevel) {
                "Alto" -> SaseRed
                "Medio" -> SaseOrange
                else -> SaseGreenDark
            },
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun DocBadge(status: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                when (status) {
                    "Completa" -> SaseGreen.copy(alpha = 0.12f)
                    "Incompleta" -> SaseRed.copy(alpha = 0.12f)
                    else -> SaseOrange.copy(alpha = 0.12f)
                }
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            status,
            color = when (status) {
                "Completa" -> SaseGreenDark
                "Incompleta" -> SaseRed
                else -> SaseOrange
            },
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun FilterChipCompact(
    label: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, SaseBorder),
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SaseNavy)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = SaseNavy, modifier = Modifier.size(14.dp))
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 12.sp, color = SaseText) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun StatusPill(status: String) {
    val (bgColor, textColor, label) = when (status) {
        "Activo" -> Triple(SaseGreen.copy(alpha = 0.12f), SaseGreenDark, "Activo")
        "En riesgo" -> Triple(SaseOrange.copy(alpha = 0.12f), SaseOrange, "En riesgo")
        "Nuevo ingreso" -> Triple(SaseBlue.copy(alpha = 0.12f), SaseBlue, "Nuevo ingreso")
        "Alta oficial con grupo" -> Triple(SaseGreen.copy(alpha = 0.12f), SaseGreenDark, "Alta oficial")
        "Alta oficial sin grupo" -> Triple(SaseBlue.copy(alpha = 0.12f), SaseBlue, "Alta oficial")
        "Lista para alta oficial" -> Triple(SaseGreen.copy(alpha = 0.12f), SaseGreenDark, "Lista para alta")
        "Documentos pendientes" -> Triple(SaseOrange.copy(alpha = 0.12f), SaseOrange, "Docs pendientes")
        "Fotografías pendientes" -> Triple(SaseOrange.copy(alpha = 0.12f), SaseOrange, "Fotos pendientes")
        "Grupo por asignar" -> Triple(SaseOrange.copy(alpha = 0.12f), SaseOrange, "Grupo por asignar")
        "Conflicto de CURP" -> Triple(SaseRed.copy(alpha = 0.12f), SaseRed, "Conflicto CURP")
        else -> Triple(SaseMuted.copy(alpha = 0.12f), SaseMuted, status)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
