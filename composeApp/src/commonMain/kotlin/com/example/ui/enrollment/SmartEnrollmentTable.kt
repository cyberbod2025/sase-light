package com.example.ui.enrollment

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.SaseText

// Smart table filters
@Composable
fun SmartEnrollmentTable(
    students: List<Student>,
    onStudentClick: (String) -> Unit,
    onRegisterObsClick: (Student) -> Unit,
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

        // Wrap table content in a horizontally scrollable container with a min-width to avoid column squeezing on mobile
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.widthIn(min = 720.dp)) {
                // Table headers
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

                // Table rows
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay alumnos que coincidan con los filtros seleccionados.", color = SaseMuted, fontSize = 12.sp)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        filteredList.forEach { student ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onStudentClick(student.id) }
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Alumno
                                Row(modifier = Modifier.weight(2.5f), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
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
                                        Text(
                                            text = student.fullName.first().toString(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = when (student.riskLevel) {
                                                "Alto" -> SaseRed
                                                "Medio" -> SaseOrange
                                                else -> SaseGreenDark
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(student.fullName, color = SaseText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("ID: ${student.enrollmentId}", color = SaseMuted, fontSize = 10.sp)
                                    }
                                }

                                // Grupo
                                Text(student.group, color = SaseText, fontSize = 12.sp, modifier = Modifier.weight(0.8f))

                                // Estatus
                                Box(
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .padding(end = 4.dp)
                                ) {
                                    StatusPill(status = student.status)
                                }

                                // Tutor
                                Text(student.tutorName, color = SaseText, fontSize = 12.sp, modifier = Modifier.weight(2.0f))

                                // Documentacion status
                                Row(modifier = Modifier.weight(1.5f), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                when (student.documentationStatus) {
                                                    "Completa" -> SaseGreen.copy(alpha = 0.12f)
                                                    "Incompleta" -> SaseRed.copy(alpha = 0.12f)
                                                    else -> SaseOrange.copy(alpha = 0.12f)
                                                }
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            student.documentationStatus,
                                            color = when (student.documentationStatus) {
                                                "Completa" -> SaseGreenDark
                                                "Incompleta" -> SaseRed
                                                else -> SaseOrange
                                            },
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = { onRegisterObsClick(student) },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = SaseMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            HorizontalDivider(color = SaseBorder.copy(alpha = 0.05f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
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
