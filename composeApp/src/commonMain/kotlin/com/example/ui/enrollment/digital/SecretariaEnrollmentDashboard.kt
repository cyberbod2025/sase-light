package com.example.ui.enrollment.digital

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.enrollment.Enrollment
import com.example.data.enrollment.EnrollmentDocument
import com.example.data.enrollment.MockEnrollmentData
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

@Composable
fun SecretariaEnrollmentDashboard(
    modifier: Modifier = Modifier,
    enrollments: List<Enrollment> = MockEnrollmentData.enrollments
) {
    var selectedId by remember(enrollments) { mutableStateOf(enrollments.firstOrNull()?.id) }
    val selectedEnrollment = enrollments.firstOrNull { it.id == selectedId } ?: enrollments.firstOrNull()

    val total = enrollments.size
    val complete = enrollments.count { it.isComplete }
    val incomplete = total - complete
    val missingDocuments = enrollments.sumOf { it.missingDocuments.size }
    val readyForSignature = enrollments.count { it.readyForSignature }

    GlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Inscripcion Digital / Expediente Maestro",
                    color = SaseNavy,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Base mock para Secretaria: captura, revision documental y firma",
                    color = SaseMuted,
                    fontSize = 11.sp
                )
            }
            StatusBadge(label = "MVP Phase 1", color = SaseBlue)
        }

        Spacer(modifier = Modifier.height(14.dp))

        BoxWithConstraints {
            val compact = maxWidth < 760.dp
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EnrollmentMetricsGrid(total, complete, incomplete, missingDocuments, readyForSignature, compact = true)
                    EnrollmentList(enrollments, selectedEnrollment?.id, onSelect = { selectedId = it.id })
                    selectedEnrollment?.let { EnrollmentDetailPanel(it) }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EnrollmentMetricsGrid(total, complete, incomplete, missingDocuments, readyForSignature, compact = false)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EnrollmentList(
                            enrollments = enrollments,
                            selectedId = selectedEnrollment?.id,
                            onSelect = { selectedId = it.id },
                            modifier = Modifier.weight(0.9f)
                        )
                        selectedEnrollment?.let {
                            EnrollmentDetailPanel(it, modifier = Modifier.weight(1.1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EnrollmentSummaryCard(
    modifier: Modifier = Modifier,
    enrollments: List<Enrollment> = MockEnrollmentData.enrollments,
    onOpenModule: () -> Unit
) {
    val total = enrollments.size
    val incomplete = enrollments.count { !it.isComplete }
    val missingDocuments = enrollments.sumOf { it.missingDocuments.size }

    GlassCard(modifier = modifier) {
        BoxWithConstraints {
            val compact = maxWidth < 680.dp

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        EnrollmentSummaryHeader()
                        EnrollmentOpenButton(onOpenModule, modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EnrollmentSummaryHeader(modifier = Modifier.weight(1f))
                        EnrollmentOpenButton(onOpenModule, modifier = Modifier.width(180.dp))
                    }
                }

                val stats = listOf(
                    MetricItem("Inscritos", total.toString(), Icons.Default.Person, SaseBlue),
                    MetricItem("Incompletos", incomplete.toString(), Icons.Default.Warning, SaseOrange),
                    MetricItem("Docs faltantes", missingDocuments.toString(), Icons.Default.ErrorOutline, SaseRed)
                )

                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        stats.forEach { item -> EnrollmentSummaryStat(item, modifier = Modifier.fillMaxWidth()) }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        stats.forEach { item -> EnrollmentSummaryStat(item, modifier = Modifier.weight(1f)) }
                    }
                }

            }
        }
    }
}

@Composable
private fun EnrollmentSummaryHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "Inscripcion Digital",
            color = SaseNavy,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Resumen de expedientes de nuevo ingreso",
            color = SaseMuted,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EnrollmentOpenButton(
    onOpenModule: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onOpenModule,
        colors = ButtonDefaults.buttonColors(
            containerColor = SaseNavy,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.height(40.dp)
    ) {
        Text("Abrir modulo", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
        Spacer(modifier = Modifier.width(6.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun EnrollmentMetricsGrid(
    total: Int,
    complete: Int,
    incomplete: Int,
    missingDocuments: Int,
    readyForSignature: Int,
    compact: Boolean
) {
    val cards = listOf(
        MetricItem("Inscritos", total.toString(), Icons.Default.Person, SaseBlue),
        MetricItem("Completos", complete.toString(), Icons.Default.CheckCircle, SaseGreen),
        MetricItem("Incompletos", incomplete.toString(), Icons.Default.Warning, SaseOrange),
        MetricItem("Docs faltantes", missingDocuments.toString(), Icons.Default.ErrorOutline, SaseRed),
        MetricItem("Listos firma", readyForSignature.toString(), Icons.AutoMirrored.Filled.FactCheck, SaseGreenDark)
    )

    if (compact) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            cards.chunked(2).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    rowItems.forEach { item -> EnrollmentMetricCard(item, modifier = Modifier.weight(1f)) }
                    if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            cards.forEach { item -> EnrollmentMetricCard(item, modifier = Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun EnrollmentSummaryStat(item: MetricItem, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.58f))
            .border(1.dp, SaseBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(item.label, color = SaseMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.value, color = SaseNavy, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
        Icon(item.icon, contentDescription = null, tint = item.color, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun EnrollmentMetricCard(item: MetricItem, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.58f))
            .border(1.dp, SaseBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.label, color = SaseMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(item.icon, contentDescription = null, tint = item.color, modifier = Modifier.size(16.dp))
            }
            Text(item.value, color = SaseNavy, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun EnrollmentList(
    enrollments: List<Enrollment>,
    selectedId: String?,
    onSelect: (Enrollment) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text("Expedientes", color = SaseNavy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.heightIn(max = 280.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(enrollments, key = { it.id }) { enrollment ->
                EnrollmentListItem(
                    enrollment = enrollment,
                    selected = enrollment.id == selectedId,
                    onClick = { onSelect(enrollment) }
                )
            }
        }
    }
}

@Composable
private fun EnrollmentListItem(enrollment: Enrollment, selected: Boolean, onClick: () -> Unit) {
    val statusColor = when {
        enrollment.isComplete -> SaseGreen
        enrollment.readyForSignature -> SaseBlue
        else -> SaseOrange
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) SaseBlue.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.48f))
            .border(1.dp, if (selected) SaseBlue.copy(alpha = 0.35f) else SaseBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(statusColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Description, contentDescription = null, tint = statusColor, modifier = Modifier.size(17.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(enrollment.studentFullName, color = SaseText, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${enrollment.id} - ${enrollment.gradeGroup} - ${enrollment.submittedAt}", color = SaseMuted, fontSize = 10.sp, maxLines = 1)
        }
        StatusBadge(label = enrollment.status, color = statusColor)
    }
}

@Composable
private fun EnrollmentDetailPanel(enrollment: Enrollment, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.50f))
            .border(1.dp, SaseBorder, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(enrollment.studentFullName, color = SaseNavy, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text("CURP ${enrollment.curp} - ${enrollment.gradeGroup} - ${enrollment.schoolYear}", color = SaseMuted, fontSize = 10.sp)
                Text("Domicilio: ${enrollment.address.street}, ${enrollment.address.neighborhood}", color = SaseMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            StatusBadge(label = if (enrollment.isComplete) "Completo" else "Revision", color = if (enrollment.isComplete) SaseGreen else SaseOrange)
        }

        SectionTitle("Tutores")
        enrollment.contacts.forEach { contact ->
            DetailLine(
                label = if (contact.isPrimary) "Principal" else contact.relation,
                value = "${contact.fullName} - ${contact.phone}"
            )
        }

        SectionTitle("Ficha medica")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MedicalChip("Sangre ${enrollment.medicalRecord.bloodType}", Icons.Default.HealthAndSafety, SaseRed, Modifier.weight(1f))
            MedicalChip(enrollment.medicalRecord.chronicConditions, Icons.Default.Warning, SaseOrange, Modifier.weight(1f))
        }
        DetailLine("Alergias", enrollment.medicalRecord.allergies)
        DetailLine("Medicacion", enrollment.medicalRecord.medication)

        SectionTitle("Checklist documental")
        enrollment.documents.forEach { document -> ChecklistRow(document) }

        if (enrollment.riskFlags.isNotEmpty()) {
            SectionTitle("Alertas")
            enrollment.riskFlags.forEach { flag ->
                DetailLine(flag.label, "${flag.severity}: ${flag.detail}")
            }
        }
    }
}

@Composable
private fun ChecklistRow(document: EnrollmentDocument) {
    val delivered = document.status == "Entregado"
    val color = if (delivered) SaseGreen else SaseOrange
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (delivered) SaseGreen.copy(alpha = 0.08f) else SaseOrange.copy(alpha = 0.10f))
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (delivered) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(document.name, color = SaseText, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(document.status, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = SaseNavy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = SaseMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(82.dp))
        Text(value, color = SaseText, fontSize = 10.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MedicalChip(label: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StatusBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

private data class MetricItem(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val color: Color
)
