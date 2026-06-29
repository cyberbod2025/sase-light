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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import com.example.data.enrollment.AuthorizedPickup
import com.example.data.enrollment.Enrollment
import com.example.data.enrollment.EnrollmentDocument
import com.example.data.enrollment.EnrollmentPresenter
import com.example.data.enrollment.EnrollmentStatus
import com.example.data.enrollment.IdentityChecklist
import com.example.data.enrollment.MockEnrollmentData
import com.example.ui.GlassCard
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
import com.example.ui.SaseText
import com.example.ui.SaseViolet

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
    val identityOk = enrollments.count { it.identityChecklist.expedienteComplete }

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
            StatusBadge(label = "MVP Phase 1.1A", color = SaseBlue)
        }

        Spacer(modifier = Modifier.height(14.dp))

        BoxWithConstraints {
            val compact = maxWidth < 760.dp
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EnrollmentMetricsGrid(total, complete, incomplete, missingDocuments, readyForSignature, identityOk, compact = true)
                    EnrollmentList(enrollments, selectedEnrollment?.id, onSelect = { selectedId = it.id })
                    selectedEnrollment?.let { EnrollmentDetailPanel(it) }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EnrollmentMetricsGrid(total, complete, incomplete, missingDocuments, readyForSignature, identityOk, compact = false)
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
    identityOk: Int,
    compact: Boolean
) {
    val cards = listOf(
        MetricItem("Inscritos", total.toString(), Icons.Default.Person, SaseBlue),
        MetricItem("Completos", complete.toString(), Icons.Default.CheckCircle, SaseGreen),
        MetricItem("Incompletos", incomplete.toString(), Icons.Default.Warning, SaseOrange),
        MetricItem("Docs faltantes", missingDocuments.toString(), Icons.Default.ErrorOutline, SaseRed),
        MetricItem("Listos firma", readyForSignature.toString(), Icons.AutoMirrored.Filled.FactCheck, SaseGreenDark),
        MetricItem("Identidad OK", identityOk.toString(), Icons.Default.VerifiedUser, SaseCyan)
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
    val statusColor = enrollmentStatusColor(enrollment.status)

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
        Spacer(modifier = Modifier.width(4.dp))
        // v1.1A: identity mini-badge in list
        if (enrollment.identityChecklist.expedienteComplete) {
            StatusBadge(label = "ID \u2713", color = SaseGreen)
            Spacer(modifier = Modifier.width(4.dp))
        }
        StatusBadge(label = enrollmentStatusLabel(enrollment.status), color = statusColor)
    }
}

// ── Detail Panel ───────────────────────────────────────────────────

@Composable
private fun EnrollmentDetailPanel(enrollment: Enrollment, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.50f))
            .border(1.dp, SaseBorder, RoundedCornerShape(18.dp))
            .padding(14.dp)
            .heightIn(max = 620.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Header ──
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
            StatusBadge(label = enrollmentStatusLabel(enrollment.status), color = enrollmentStatusColor(enrollment.status))
        }

        // ── Tutores ──
        SectionTitle("Tutores")
        enrollment.contacts.forEach { contact ->
            DetailLine(
                label = if (contact.isPrimary) "Principal" else contact.relation,
                value = "${contact.fullName} - ${contact.phone}"
            )
        }

        // ── Ficha medica ──
        SectionTitle("Ficha medica")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MedicalChip("Sangre ${enrollment.medicalRecord.bloodType}", Icons.Default.HealthAndSafety, SaseRed, Modifier.weight(1f))
            MedicalChip(enrollment.medicalRecord.chronicConditions, Icons.Default.Warning, SaseOrange, Modifier.weight(1f))
        }
        DetailLine("Alergias", enrollment.medicalRecord.allergies)
        DetailLine("Medicacion", enrollment.medicalRecord.medication)

        // ── Checklist documental ──
        SectionTitle("Checklist documental")
        enrollment.documents.forEach { document -> ChecklistRow(document) }

        if (enrollment.riskFlags.isNotEmpty()) {
            SectionTitle("Alertas")
            enrollment.riskFlags.forEach { flag ->
                DetailLine(flag.label, "${flag.severity}: ${flag.detail}")
            }
        }

        // ══════════════════════════════════════════════════════════
        //  v1.1A — IDENTITY & CREDENTIAL SECTIONS
        // ══════════════════════════════════════════════════════════

        IdentitySummarySection(enrollment)
        PresenterSection(enrollment.presenter)
        AuthorizedPickupsSection(enrollment.authorizedPickups)
        ValidationSection(enrollment.identityChecklist)
        CredentialSection(enrollment)
    }
}

// ── v1.1A: Identity Summary (quick-glance badges) ────────────────

@Composable
private fun IdentitySummarySection(enrollment: Enrollment) {
    val cl = enrollment.identityChecklist
    SectionTitleWithIcon("Identidad", Icons.Default.Fingerprint, SaseViolet)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        IdentityBadge("Foto alumno", cl.studentPhotographed)
        IdentityBadge("Foto tutor", cl.tutorPhotographed)
        IdentityBadge("INE", cl.ineVerified)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        IdentityBadge("Autorizados", cl.authorizedPickupsRegistered)
        IdentityBadge("Docs", cl.documentsComplete)
        IdentityBadge(
            if (cl.expedienteComplete) "Expediente OK" else "Expediente pendiente",
            cl.expedienteComplete
        )
    }
}

@Composable
private fun IdentityBadge(label: String, ok: Boolean) {
    val color = if (ok) SaseGreen else SaseOrange
    val icon = if (ok) Icons.Default.CheckCircle else Icons.Default.ErrorOutline
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

// ── v1.1A: Presenter Section ─────────────────────────────────────

@Composable
private fun PresenterSection(presenter: EnrollmentPresenter?) {
    SectionTitleWithIcon("Persona que presenta al alumno", Icons.Default.HowToReg, SaseBlue)

    if (presenter == null) {
        EmptyStateRow("Sin presentador registrado")
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SaseBlue.copy(alpha = 0.05f))
            .border(1.dp, SaseBlue.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(presenter.fullName, color = SaseNavy, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                StatusBadge(label = presenter.relationship, color = SaseBlue)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IdentityBadge("INE", presenter.ineVerified)
                IdentityBadge("Foto", presenter.photoUrl != null)
                IdentityBadge("Firma", presenter.signatureUrl != null)
                if (presenter.canPickupStudent) {
                    IdentityBadge("Puede recoger", true)
                }
            }
            DetailLine("Telefono", presenter.phone)
            DetailLine("Registrado", "${presenter.registeredBy} — ${presenter.createdAt}")
        }
    }
}

// ── v1.1A: Authorized Pickups Section ────────────────────────────

@Composable
private fun AuthorizedPickupsSection(pickups: List<AuthorizedPickup>) {
    SectionTitleWithIcon("Autorizados para recoger", Icons.Default.Groups, SaseGreenDark)

    if (pickups.isEmpty()) {
        EmptyStateRow("Sin autorizados registrados")
        return
    }

    pickups.forEach { pickup ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (pickup.active) SaseGreen.copy(alpha = 0.05f) else SaseMuted.copy(alpha = 0.06f))
                .border(1.dp, if (pickup.active) SaseGreen.copy(alpha = 0.12f) else SaseBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(SaseGreenDark.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = SaseGreenDark, modifier = Modifier.size(15.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(pickup.fullName, color = SaseText, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${pickup.relationship} — ${pickup.phone}", color = SaseMuted, fontSize = 9.sp, maxLines = 1)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IdentityBadge("INE", pickup.ineVerified)
                IdentityBadge("Foto", pickup.photoUrl != null)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

// ── v1.1A: Validation Section ────────────────────────────────────

@Composable
private fun ValidationSection(checklist: IdentityChecklist) {
    SectionTitleWithIcon("Validacion de identidad", Icons.Default.Shield, SaseCyan)

    val items = listOf(
        "Alumno fotografiado" to checklist.studentPhotographed,
        "Tutor fotografiado" to checklist.tutorPhotographed,
        "Tutor identificado" to checklist.tutorIdentified,
        "INE cotejada" to checklist.ineVerified,
        "Autorizados registrados" to checklist.authorizedPickupsRegistered,
        "Documentos completos" to checklist.documentsComplete,
        "Expediente completo" to checklist.expedienteComplete
    )

    items.forEach { (label, ok) ->
        IdentityChecklistRow(label, ok)
    }
}

@Composable
private fun IdentityChecklistRow(label: String, ok: Boolean) {
    val color = if (ok) SaseGreen else SaseOrange
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (ok) SaseGreen.copy(alpha = 0.07f) else SaseOrange.copy(alpha = 0.07f))
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (ok) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = SaseText, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1)
        Text(if (ok) "Verificado" else "Pendiente", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

// ── v1.1A: Credential Section ────────────────────────────────────

@Composable
private fun CredentialSection(enrollment: Enrollment) {
    val ready = enrollment.identityChecklist.expedienteComplete && enrollment.photoForCredential
    SectionTitleWithIcon("Credencializacion", Icons.Default.Badge, if (ready) SaseGreen else SaseOrange)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusBadge(
            label = if (ready) "Listo para credencial" else "No listo para credencial",
            color = if (ready) SaseGreen else SaseOrange
        )
        if (enrollment.studentPhotoUrl != null) {
            StatusBadge(label = "Foto alumno \u2713", color = SaseGreen)
        } else {
            StatusBadge(label = "Falta foto alumno", color = SaseRed)
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Mock action buttons — present but disabled
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MockActionButton("Tomar foto alumno", Icons.Default.CameraAlt, Modifier.weight(1f))
        MockActionButton("Foto tutor", Icons.Default.PhotoCamera, Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(2.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MockActionButton("Generar credencial", Icons.Default.School, Modifier.weight(1f))
        MockActionButton("Ficha autorizados", Icons.Default.AccountBox, Modifier.weight(1f))
    }
}

@Composable
private fun MockActionButton(label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = { /* mock — no-op */ },
        enabled = false,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(34.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            disabledContentColor = SaseMuted.copy(alpha = 0.55f)
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ── Shared composables ───────────────────────────────────────────

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
private fun SectionTitleWithIcon(text: String, icon: ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, color = SaseNavy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
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
private fun EmptyStateRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SaseOrange.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = SaseOrange, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, color = SaseOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

private fun enrollmentStatusColor(status: EnrollmentStatus): Color = when (status) {
    EnrollmentStatus.Submitted -> SaseBlue
    EnrollmentStatus.InReview -> SaseOrange
    EnrollmentStatus.MissingDocuments -> SaseRed
    EnrollmentStatus.ReadyToSign -> SaseGreenDark
    EnrollmentStatus.Completed -> SaseGreen
}

private fun enrollmentStatusLabel(status: EnrollmentStatus): String = when (status) {
    EnrollmentStatus.Submitted -> "Recibido"
    EnrollmentStatus.InReview -> "En revision"
    EnrollmentStatus.MissingDocuments -> "Faltan docs"
    EnrollmentStatus.ReadyToSign -> "Listo firma"
    EnrollmentStatus.Completed -> "Completo"
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
