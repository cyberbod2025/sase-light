package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Student
import com.example.data.StudentCredentialPreview
import com.example.data.presolicitud.OfficialStudent
import com.example.data.presolicitud.OfficialStudentStatus
import com.example.viewmodel.LabViewModel
import com.example.viewmodel.PreApplicationViewModel
import com.example.viewmodel.Screen

private val credentialOfficialEnrollmentPattern = Regex("^S310-[A-Z0-9]{10}-\\d{2}$")

private fun visibleCredentialEnrollment(official: OfficialStudent): String =
    official.matriculaOficial
        ?.takeIf { official.status == OfficialStudentStatus.ALTA_OFICIAL_CON_GRUPO && it.matches(credentialOfficialEnrollmentPattern) }
        ?: "Por asignar"

@Composable
fun StudentCredentialDashboardScreen(viewModel: LabViewModel) {
    val officialStudents by PreApplicationViewModel.officialStudents.collectAsState()
    val masterStudents by viewModel.saseStudents.collectAsState()
    var selectedId by remember { mutableStateOf<String?>(null) }

    val graduated = remember(officialStudents) {
        officialStudents.filter { it.status in listOf(
            OfficialStudentStatus.ALTA_OFICIAL_SIN_GRUPO,
            OfficialStudentStatus.PENDIENTE_ASIGNACION_GRUPO,
            OfficialStudentStatus.ALTA_OFICIAL_CON_GRUPO
        ) }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(SaseBgSoft)) {
        val isMobile = maxWidth < 850.dp

        val content = @Composable {
            Box(modifier = Modifier.fillMaxSize()) {
                if (graduated.isEmpty()) {
                    EmptyCredentialState(
                        onNavigateToEnrollment = { viewModel.navigateTo(Screen.OfficialEnrollmentDashboard) }
                    )
                } else {
                    CredentialLayout(
                        isMobile = isMobile,
                        students = graduated,
                        masterStudents = masterStudents,
                        selectedId = selectedId,
                        onSelect = { selectedId = it },
                        onViewStudent = { id ->
                            viewModel.navigateTo(
                                Screen.StudentRecord(id, returnTo = Screen.StudentCredentialDashboard)
                            )
                        }
                    )
                }
            }
        }

        if (isMobile) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    ReturnToDashboardButton(onClick = { viewModel.navigateTo(Screen.SecretaryDashboard) })
                }
                Box(modifier = Modifier.weight(1f)) { content() }
            }
        } else {
            var sidebarCollapsed by remember { mutableStateOf(false) }
            Row(modifier = Modifier.fillMaxSize()) {
                SaseSidebar(
                    activeItem = "Credenciales",
                    collapsed = sidebarCollapsed,
                    onToggleCollapse = { sidebarCollapsed = !sidebarCollapsed },
                    modifier = Modifier.fillMaxHeight(),
                    onItemClick = { item ->
                        when (item) {
                            "Inicio", "Expedientes" -> viewModel.navigateTo(Screen.SecretaryDashboard)
                            "Inscripciones" -> viewModel.navigateTo(Screen.EnrollmentDashboard)
                            "Portal Familia" -> viewModel.navigateTo(Screen.PreApplicationFamilyPortal)
                            "Pre-Solicitudes" -> viewModel.navigateTo(Screen.SecretariaPreApplicationDashboard)
                            "Altas Oficiales" -> viewModel.navigateTo(Screen.OfficialEnrollmentDashboard)
                            "Credenciales" -> {}
                        }
                    }
                )
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) { content() }
            }
        }
    }
}

@Composable
private fun EmptyCredentialState(onNavigateToEnrollment: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(20.dp))
                    .background(SaseBlue.copy(alpha = .1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Badge, contentDescription = null, tint = SaseBlue.copy(alpha = .4f), modifier = Modifier.size(44.dp))
            }
            Text(
                "A\u00fan no hay alumnos oficialmente dados de alta para generar vista previa de credencial.",
                fontSize = 15.sp,
                color = SaseMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 400.dp)
            )
            Button(
                onClick = onNavigateToEnrollment,
                colors = ButtonDefaults.buttonColors(containerColor = SaseNavy),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ir a Altas Oficiales")
            }
        }
    }
}

@Composable
private fun CredentialLayout(
    isMobile: Boolean,
    students: List<OfficialStudent>,
    masterStudents: List<Student>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onViewStudent: (String) -> Unit
) {
    if (isMobile) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            HeaderSection(students.size)
            if (selectedId != null) {
                val official = students.find { it.id == selectedId }
                val master = official?.let { o -> masterStudents.find { it.curp == o.curp } }
                if (official != null) {
                    CredentialPreviewCard(official, master, onViewStudent)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { onSelect(null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Volver a la lista", color = SaseNavy) }
                }
            } else {
                StudentCredentialList(students, selectedId, onSelect)
            }
        }
    } else {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.width(380.dp).fillMaxHeight().padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeaderSection(students.size)
                StudentCredentialList(students, selectedId, onSelect)
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp).verticalScroll(rememberScrollState())) {
                if (selectedId != null) {
                    val official = students.find { it.id == selectedId }
                    val master = official?.let { o -> masterStudents.find { it.curp == o.curp } }
                    if (official != null) {
                        CredentialPreviewCard(official, master, onViewStudent)
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = SaseMuted.copy(alpha = .4f), modifier = Modifier.size(48.dp))
                            Text("Selecciona un alumno para ver su credencial.", color = SaseMuted, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(total: Int) {
    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        Text("Credencial Escolar", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = SaseNavy)
        Text("$total alumno(s) con alta oficial", fontSize = 13.sp, color = SaseMuted)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Vista previa institucional sin validez oficial hasta validaci\u00f3n de Direcci\u00f3n.",
            fontSize = 10.sp,
            color = SaseOrange,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StudentCredentialList(
    students: List<OfficialStudent>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    students.forEach { official ->
        val isSelected = official.id == selectedId
        val statusLabel = when (official.status) {
            OfficialStudentStatus.ALTA_OFICIAL_SIN_GRUPO -> "Sin grupo"
            OfficialStudentStatus.PENDIENTE_ASIGNACION_GRUPO -> "Pendiente grupo"
            OfficialStudentStatus.ALTA_OFICIAL_CON_GRUPO -> "Con grupo"
            else -> official.status.label
        }

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(official.id) }
                .then(
                    if (isSelected) Modifier.border(2.dp, SaseBlue, RoundedCornerShape(12.dp))
                    else Modifier
                )
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                        .background(SaseNavy.copy(alpha = .08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = SaseNavy.copy(alpha = .5f), modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(official.alumnoNombreCompleto, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SaseNavy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "Matrícula: ${visibleCredentialEnrollment(official)}",
                        fontSize = 10.sp,
                        color = SaseMuted
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${official.gradoIngreso}\u00b0 ${official.grupoAsignado ?: official.grupoSugerido ?: "-"}", fontSize = 11.sp, color = SaseNavy)
                        Text("\u00b7", fontSize = 10.sp, color = SaseMuted)
                        Text(statusLabel, fontSize = 10.sp, color = SaseMuted)
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SaseMuted, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun CredentialPreviewCard(
    official: OfficialStudent,
    master: Student?,
    onViewStudent: (String) -> Unit
) {
    val navyDark = Color(0xFF0f243d)
    val navyMedium = Color(0xFF1a3a5c)
    val gold = Color(0xFFc9a84c)
    val schoolYear = master?.schoolYear ?: "2026-2027"
    val groupDisplay = official.grupoAsignado ?: official.grupoSugerido ?: "Por asignar"
    val statusText = if (official.status == OfficialStudentStatus.ALTA_OFICIAL_CON_GRUPO) "Vigente" else "Vista previa"
    val matricula = visibleCredentialEnrollment(official)
    val hasPhoto = false

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(navyDark, navyMedium)))
                .border(1.dp, gold.copy(alpha = .4f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ESC. SEC. DIURNA", fontSize = 9.sp, color = gold, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text("No. 310 \"Presidentes de M\u00e9xico\"", fontSize = 10.sp, color = Color.White.copy(alpha = .8f))
                        Text("Turno Vespertino", fontSize = 8.sp, color = Color.White.copy(alpha = .5f), letterSpacing = 1.sp)
                    }
                    Box(
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = .1f))
                            .border(1.dp, gold.copy(alpha = .3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, tint = gold, modifier = Modifier.size(28.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(76.dp).clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = .08f))
                            .border(1.dp, Color.White.copy(alpha = .15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White.copy(alpha = .35f), modifier = Modifier.size(32.dp))
                            Text(
                                if (hasPhoto) "Foto capturada en revisi\u00f3n" else "Foto pendiente",
                                fontSize = 6.sp,
                                color = Color.White.copy(alpha = .45f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(official.alumnoNombreCompleto.uppercase(), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 18.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                    .background(Color.White.copy(alpha = .12f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("${official.gradoIngreso}\u00b0 $groupDisplay", fontSize = 9.sp, color = Color.White.copy(alpha = .85f), fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                    .background(Color.White.copy(alpha = .12f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(schoolYear.take(9), fontSize = 9.sp, color = Color.White.copy(alpha = .85f), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = .12f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                CredentialInfoLine("MATR\u00cdCULA", matricula)
                CredentialInfoLine("ESTATUS", statusText)

                Spacer(modifier = Modifier.height(8.dp))

                // QR mock
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = .06f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = .1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.White.copy(alpha = .25f), modifier = Modifier.size(18.dp))
                        }
                        Text(
                            "SASE Light \u2014 Credencial de identificaci\u00f3n escolar",
                            fontSize = 8.sp, color = Color.White.copy(alpha = .35f), letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .background(SaseOrange.copy(alpha = .08f))
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Vista previa sin validez oficial hasta validaci\u00f3n de Direcci\u00f3n.",
                fontSize = 10.sp, color = SaseOrange, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center
            )
        }

        if (master != null) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onViewStudent(master.id) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ver expediente completo", color = SaseNavy, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun CredentialInfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 7.sp, color = Color.White.copy(alpha = .4f), fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Text(value, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
    }
}
