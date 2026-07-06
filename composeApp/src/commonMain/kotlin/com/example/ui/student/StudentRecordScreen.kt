package com.example.ui.student

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.SaseIncident
import com.example.data.SaseObservation
import com.example.ui.DataRow
import com.example.ui.GlassCard
import com.example.ui.PillStat
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
import com.example.util.LocalToast
import com.example.viewmodel.AppRole
import com.example.viewmodel.LabViewModel
import com.example.viewmodel.Screen
import kotlinx.coroutines.launch

@Composable
fun StudentRecordScreen(
    studentId: String,
    viewModel: LabViewModel,
    userRole: AppRole = AppRole.SECRETARIA
) {
    val toast = LocalToast.current
    val students by viewModel.saseStudents.collectAsState()
    val student = remember(students, studentId) { students.find { it.id == studentId } }

    var activeTab by remember { mutableStateOf("Resumen") }
    val tabs = listOf("Resumen", "Datos generales", "Tutores / Contacto", "Asistencia", "Incidencias", "Salud", "Orientación", "Documentos", "Observaciones")

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
                Button(onClick = { viewModel.navigateTo(Screen.SecretaryDashboard) }) {
                    Text("Regresar")
                }
            }
        }
        return
    }

    BoxWithConstraints(modifier = SaseBackgroundModifier()) {
        val isMobile = maxWidth < 850.dp
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val navigateFromSidebar: (String) -> Unit = { item ->
            when (item) {
                "Inicio" -> viewModel.navigateTo(Screen.SecretaryDashboard)
                "Inscripciones" -> viewModel.navigateTo(Screen.EnrollmentDashboard)
                "Portal Familia" -> viewModel.navigateTo(Screen.PreApplicationFamilyPortal)
                "Pre-Solicitudes" -> viewModel.navigateTo(Screen.SecretariaPreApplicationDashboard)
                "Altas Oficiales" -> viewModel.navigateTo(Screen.OfficialEnrollmentDashboard)
                "Credenciales" -> viewModel.navigateTo(Screen.StudentCredentialDashboard)
            }
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
                            IconButton(onClick = { viewModel.navigateTo(Screen.SecretaryDashboard) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = SaseNavy)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Expediente", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = SaseNavy)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showDocumentDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SaseBlue),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Documento", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.navigateTo(Screen.SecretaryDashboard) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = SaseNavy)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Expediente del Alumno", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = SaseNavy)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showDocumentDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SaseBlue),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ver documentos", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Ficha Perfil Principal (Header Perfil)
                GlassCard {
                    if (isMobile) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(SaseNavy.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = "Foto Alumno", tint = SaseNavy, modifier = Modifier.size(38.dp))
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
                                Text(student.status, color = SaseGreenDark, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Grupo", color = SaseMuted, fontSize = 10.sp)
                                    Text(student.group, fontWeight = FontWeight.Bold, color = SaseText, fontSize = 11.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Matrícula", color = SaseMuted, fontSize = 10.sp)
                                    Text(student.enrollmentId, fontWeight = FontWeight.Bold, color = SaseText, fontSize = 11.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("CURP", color = SaseMuted, fontSize = 10.sp)
                                    Text(student.curp, fontWeight = FontWeight.Bold, color = SaseText, fontSize = 11.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Turno", color = SaseMuted, fontSize = 10.sp)
                                    Text(student.shift, fontWeight = FontWeight.Bold, color = SaseText, fontSize = 11.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Ciclo escolar", color = SaseMuted, fontSize = 10.sp)
                                    Text(student.schoolYear, fontWeight = FontWeight.Bold, color = SaseText, fontSize = 11.sp)
                                }
                            }
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
                                    .background(SaseNavy.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = "Foto Alumno", tint = SaseNavy, modifier = Modifier.size(44.dp))
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
                                        Text(student.status, color = SaseGreenDark, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    Column {
                                        Text("Grupo", color = SaseMuted, fontSize = 10.sp)
                                        Text(student.group, fontWeight = FontWeight.Bold, color = SaseText, fontSize = 12.sp)
                                    }
                                    Column {
                                        Text("Matrícula", color = SaseMuted, fontSize = 10.sp)
                                        Text(student.enrollmentId, fontWeight = FontWeight.Bold, color = SaseText, fontSize = 12.sp)
                                    }
                                    Column {
                                        Text("CURP", color = SaseMuted, fontSize = 10.sp)
                                        Text(student.curp, fontWeight = FontWeight.Bold, color = SaseText, fontSize = 12.sp)
                                    }
                                    Column {
                                        Text("Turno", color = SaseMuted, fontSize = 10.sp)
                                        Text(student.shift, fontWeight = FontWeight.Bold, color = SaseText, fontSize = 12.sp)
                                    }
                                    Column {
                                        Text("Ciclo escolar", color = SaseMuted, fontSize = 10.sp)
                                        Text(student.schoolYear, fontWeight = FontWeight.Bold, color = SaseText, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = SaseBorder.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Pills Row (Riesgo, BAP, Seguro, Documentos)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isMobile) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PillStat(label = "Riesgo: ${student.riskLevel}", color = when(student.riskLevel){ "Alto" -> SaseRed "Medio" -> SaseOrange else -> SaseGreen })
                        PillStat(label = "BAP: ${student.bap}", color = if(student.bap == "Sí") SaseViolet else SaseMuted)
                        PillStat(label = "Seguro escolar: ${student.schoolInsurance}", color = SaseGreen)
                        PillStat(label = "Documentación: ${student.documentationStatus}", color = if(student.documentationStatus == "Completa") SaseGreen else SaseOrange)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Scrollable tab selector
                ScrollableTabRow(
                    selectedTabIndex = tabs.indexOf(activeTab),
                    containerColor = Color.Transparent,
                    contentColor = SaseNavy,
                    divider = {},
                    indicator = {},
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEach { title ->
                        val selected = activeTab == title
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) SaseNavy else Color.White.copy(alpha = 0.5f))
                                .border(1.dp, if (selected) SaseNavy else SaseBorder, RoundedCornerShape(12.dp))
                                .clickable { activeTab = title }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = title,
                                color = if (selected) Color.White else SaseText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content panels based on Tab
                AnimatedContent(
                    targetState = activeTab,
                    label = "TabContent"
                ) { currentTab ->
                    when (currentTab) {
                        "Resumen" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                if (isMobile) {
                                    // General data block
                                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Text("Datos generales", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        DataRow(label = "Fecha de nacimiento", value = student.birthDate)
                                        DataRow(label = "Edad", value = "${student.age} años")
                                        DataRow(label = "Lugar de nacimiento", value = student.birthPlace)
                                        DataRow(label = "Domicilio", value = student.address)
                                        DataRow(label = "Código postal", value = student.zipCode)
                                    }

                                    // Contacts block
                                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Text("Tutores y contacto de emergencia", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
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
                                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Text("Asistencia", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
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

                                    // Health block
                                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Text("Salud", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        DataRow(label = "Alergias", value = student.healthAlergies)
                                        DataRow(label = "Obs. Médicas", value = student.healthNotes)
                                        DataRow(label = "Medicamentos", value = student.healthMeds)
                                        DataRow(label = "Pases de salud", value = student.healthPasses)
                                    }

                                    // Incidents summary block
                                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Historial de incidencias", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
                                            Text("Ver todas", color = SaseBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { activeTab = "Incidencias" })
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

                                    // Orientation block
                                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Text("Orientación / Trabajo social", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        DataRow(label = "Estatus de seguimiento", value = student.orientationStatus)
                                        DataRow(label = "Última cita", value = student.orientationLastAppointment)
                                        DataRow(label = "Plan de intervención", value = student.orientationInterventionPlan)
                                        DataRow(label = "Responsable", value = student.orientationResponsible)
                                    }

                                    // Documents block
                                    GlassCard(modifier = Modifier.fillMaxWidth()) {
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
                                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Text("Observaciones y Bitácora", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
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
                                        GlassCard(modifier = Modifier.weight(1f)) {
                                            Text("Datos generales", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(10.dp))
                                            DataRow(label = "Fecha de nacimiento", value = student.birthDate)
                                            DataRow(label = "Edad", value = "${student.age} años")
                                            DataRow(label = "Lugar de nacimiento", value = student.birthPlace)
                                            DataRow(label = "Domicilio", value = student.address)
                                            DataRow(label = "Código postal", value = student.zipCode)
                                        }

                                        // Contacts block
                                        GlassCard(modifier = Modifier.weight(1f)) {
                                            Text("Tutores y contacto de emergencia", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
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
                                        GlassCard(modifier = Modifier.weight(1f)) {
                                            Text("Asistencia", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
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

                                        // Health block
                                        GlassCard(modifier = Modifier.weight(1.5f)) {
                                            Text("Salud", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(10.dp))
                                            DataRow(label = "Alergias", value = student.healthAlergies)
                                            DataRow(label = "Obs. Médicas", value = student.healthNotes)
                                            DataRow(label = "Medicamentos", value = student.healthMeds)
                                            DataRow(label = "Pases de salud", value = student.healthPasses)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        // Incidents summary block
                                        GlassCard(modifier = Modifier.weight(1.5f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Historial de incidencias", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
                                                Text("Ver todas", color = SaseBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { activeTab = "Incidencias" })
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

                                        // Orientation block
                                        GlassCard(modifier = Modifier.weight(1.2f)) {
                                            Text("Orientación / Trabajo social", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(10.dp))
                                            DataRow(label = "Estatus de seguimiento", value = student.orientationStatus)
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
                                        GlassCard(modifier = Modifier.weight(1.2f)) {
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
                                        GlassCard(modifier = Modifier.weight(1.5f)) {
                                            Text("Observaciones y Bitácora", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 14.sp)
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

                        "Datos generales" -> {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text("Datos generales del Alumno", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                DataRow(label = "Nombre completo", value = student.fullName)
                                DataRow(label = "CURP", value = student.curp)
                                DataRow(label = "Matrícula escolar", value = student.enrollmentId)
                                DataRow(label = "Fecha de nacimiento", value = student.birthDate)
                                DataRow(label = "Edad", value = "${student.age} años")
                                DataRow(label = "Lugar de nacimiento", value = student.birthPlace)
                                DataRow(label = "Domicilio familiar", value = student.address)
                                DataRow(label = "Código postal (CP)", value = student.zipCode)
                                DataRow(label = "Estatus de seguro escolar", value = student.schoolInsurance)
                                DataRow(label = "Expediente auditado", value = "Sí, por Secretaría")
                            }
                        }

                        "Tutores / Contacto" -> {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text("Información familiar de contacto", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                TutorItem(
                                    name = student.tutorName,
                                    relation = student.tutorRelation,
                                    phone = student.tutorPhone,
                                    email = student.tutorEmail,
                                    onCall = { phoneDialogName = student.tutorName; phoneDialogNumber = student.tutorPhone; showPhoneDialog = true }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Contacto alterno / Emergencia", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 13.sp)
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

                        "Asistencia" -> {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text("Bitácora detallada de asistencia", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(30.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(100.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { student.attendancePercent / 100f },
                                            color = SaseGreen,
                                            trackColor = SaseBgSoft,
                                            strokeWidth = 8.dp,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Text("${student.attendancePercent}%", fontWeight = FontWeight.ExtraBold, color = SaseNavy, fontSize = 18.sp)
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Días escolares laborados: 180", color = SaseText, fontSize = 13.sp)
                                        Text("Asistencias registradas: ${student.attendances}", color = SaseGreenDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Faltas justificadas: ${student.excusedAbsences}", color = SaseOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Faltas injustificadas: ${student.unexcusedAbsences}", color = SaseRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        "Incidencias" -> {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Historial completo de incidencias", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 16.sp)
                                    Button(
                                        onClick = { showIncidentDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = SaseRed),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Registrar incidencia", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
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
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text("Historial médico y salud del Alumno", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                DataRow(label = "Alergias severas", value = student.healthAlergies)
                                DataRow(label = "Medicamentos de uso diario", value = student.healthMeds)
                                DataRow(label = "Historial o pases de emergencia", value = student.healthPasses)
                                DataRow(label = "Notas clínicas generales", value = student.healthNotes)
                            }
                        }

                        "Orientación" -> {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text("Bitácora de Orientación y Trabajo social", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                DataRow(label = "Estatus escolar", value = student.orientationStatus)
                                DataRow(label = "Fecha de última sesión", value = student.orientationLastAppointment)
                                DataRow(label = "Plan remedial de intervención", value = student.orientationInterventionPlan)
                                DataRow(label = "Psicólogo/Orientador responsable", value = student.orientationResponsible)
                            }
                        }

                        "Documentos" -> {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
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
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Bitácora pedagógica de observaciones", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 16.sp)
                                    Button(
                                        onClick = { showObsDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = SaseBlue),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Agregar observación", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
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
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = SaseBlue.copy(alpha = 0.4f),
                    containerColor = SaseNavy.copy(alpha = 0.03f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (userRole == AppRole.SECRETARIA) {
                            Button(
                                onClick = { showIncidentDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SaseRed),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Registrar incidencia", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }

                        Button(
                            onClick = { showObsDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SaseBlue),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Agregar observación", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        Button(
                            onClick = { showDocumentDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SaseViolet),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ver documentos", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        if (userRole == AppRole.SECRETARIA) {
                            Button(
                                onClick = { showEscalarDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SaseOrange),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.CallSplit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Escalar caso", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
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
                    Text("Registrar incidencia disciplinaria/asistencia", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 16.sp)

                    // Incident types
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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

                    OutlinedTextField(
                        value = incDesc,
                        onValueChange = { incDesc = it },
                        label = { Text("Descripción") },
                        placeholder = { Text("Ej. El alumno llegó 15 minutos tarde...") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = { showIncidentDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Cancelar", color = SaseMuted)
                        }
                        Button(
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
                            colors = ButtonDefaults.buttonColors(containerColor = SaseRed),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Registrar", fontWeight = FontWeight.Bold)
                        }
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
                    Text("Agregar observación pedagógica", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 16.sp)

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
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = { showObsDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Cancelar", color = SaseMuted)
                        }
                        Button(
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
                            colors = ButtonDefaults.buttonColors(containerColor = SaseBlue),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Agregar", fontWeight = FontWeight.Bold)
                        }
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

                    OutlinedTextField(
                        value = escalarNotes,
                        onValueChange = { escalarNotes = it },
                        label = { Text("Motivo de canalización") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = { showEscalarDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Cancelar", color = SaseMuted)
                        }
                        Button(
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
                            colors = ButtonDefaults.buttonColors(containerColor = SaseOrange),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Escalar Caso", fontWeight = FontWeight.Bold)
                        }
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
                    modifier = Modifier.width(260.dp),
                    onItemClick = navigateFromSidebar
                )
                Box(modifier = Modifier.weight(1f)) {
                    recordContent()
                }
            }
        }
    }
}
