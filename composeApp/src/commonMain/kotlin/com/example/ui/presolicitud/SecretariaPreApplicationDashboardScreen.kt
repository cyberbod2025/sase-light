package com.example.ui.presolicitud

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.presolicitud.*
import com.example.ui.*
import com.example.util.LocalToast
import com.example.viewmodel.LabViewModel
import com.example.viewmodel.PreApplicationViewModel
import com.example.viewmodel.Screen
import kotlinx.coroutines.launch

@Composable
fun SecretariaPreApplicationDashboardScreen(viewModel: LabViewModel) {
    val toast = LocalToast.current
    val preApps by PreApplicationViewModel.sharedPreApplications.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAll by remember { mutableStateOf(false) }
    var selectedFolio by remember { mutableStateOf<String?>(null) }

    val visibleApps = remember(preApps, searchQuery, showAll) {
        val filtered = if (showAll) preApps
        else preApps.filter { it.status in listOf(PreApplicationStatus.ENVIADA, PreApplicationStatus.PENDIENTE_CORRECCION, PreApplicationStatus.ACEPTADA) }

        if (searchQuery.isBlank()) filtered
        else filtered.filter { app ->
            app.folio.contains(searchQuery, ignoreCase = true) ||
            app.alumnoNombreCompleto.contains(searchQuery, ignoreCase = true) ||
            app.alumnoCurp.contains(searchQuery, ignoreCase = true) ||
            app.alumnoTelefonoCasa.contains(searchQuery, ignoreCase = true) ||
            app.responsables.any { it.telefono.contains(searchQuery, ignoreCase = true) }
        }
    }

    val selectedApp = remember(selectedFolio, preApps) {
        preApps.find { it.folio == selectedFolio }
    }

    var showProvisionalDialog by remember { mutableStateOf(false) }
    var provisionalResult by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(modifier = SaseBackgroundModifier()) {
        val isMobile = maxWidth < 850.dp
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        val navigateFromSidebar: (String) -> Unit = { item ->
            when (item) {
                "Inicio" -> viewModel.navigateTo(Screen.SecretaryDashboard)
                "Inscripciones" -> viewModel.navigateTo(Screen.EnrollmentDashboard)
                "Portal Familia" -> viewModel.navigateTo(Screen.PreApplicationFamilyPortal)
                "Pre-Solicitudes" -> {}
            }
        }

        val content = @Composable {
            if (isMobile && selectedApp != null) {
                MobilePreApplicationDetail(
                    preApp = selectedApp,
                    viewModel = viewModel,
                    onBack = { selectedFolio = null },
                    onApprove = { folio ->
                        PreApplicationViewModel.approvePreApplication(folio)
                        toast("Pre-solicitud $folio aceptada — pendiente alta oficial")
                    },
                    onMarkCorrection = { folio ->
                        PreApplicationViewModel.markForCorrection(folio)
                        toast("Pre-solicitud $folio marcada para corrección — se notificará a la familia")
                    },
                    onProvisionalCreated = { msg -> provisionalResult = msg; showProvisionalDialog = true }
                )
            } else {
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
                                Text("Pre-Solicitudes", fontWeight = FontWeight.ExtraBold, fontSize = if (isMobile) 20.sp else 24.sp, color = SaseNavy)
                                Text("Revisión y validación de solicitudes de ingreso", fontSize = if (isMobile) 11.sp else 12.sp, color = SaseMuted)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SaseCyan.copy(alpha = 0.12f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "${preApps.size} solicitudes",
                                    color = SaseCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Buscar por folio, CURP, nombre o teléfono...", color = SaseMuted) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SaseMuted) },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = SaseMuted)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.75f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.5f),
                                focusedBorderColor = SaseBlue,
                                unfocusedBorderColor = SaseBorder,
                                focusedTextColor = SaseText,
                                unfocusedTextColor = SaseText
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (showAll) SaseBlue.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.5f))
                                .border(1.dp, SaseBorder, RoundedCornerShape(12.dp))
                                .clickable { showAll = !showAll }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Todas", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (showAll) SaseBlue else SaseMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isMobile) {
                        PreApplicationList(
                            apps = visibleApps,
                            selectedFolio = selectedFolio,
                            onSelect = { selectedFolio = it.folio }
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            PreApplicationList(
                                apps = visibleApps,
                                selectedFolio = selectedFolio,
                                onSelect = { selectedFolio = it.folio },
                                modifier = Modifier.width(320.dp)
                            )
                            PreApplicationDetailTabs(
                                preApp = selectedApp,
                                onApprove = { folio ->
                                    PreApplicationViewModel.approvePreApplication(folio)
                                    toast("Pre-solicitud $folio aceptada — pendiente alta oficial")
                                },
                                onMarkCorrection = { folio ->
                                    PreApplicationViewModel.markForCorrection(folio)
                                    toast("Pre-solicitud $folio marcada para corrección — se notificará a la familia")
                                },
                                onProvisionalCreated = { msg -> provisionalResult = msg; showProvisionalDialog = true },
                                modifier = Modifier.weight(1f)
                            )
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
                            activeItem = "Pre-Solicitudes",
                            modifier = Modifier.fillMaxHeight(),
                            onItemClick = { item ->
                                navigateFromSidebar(item)
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                }
            ) {
                content()
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                SaseSidebar(
                    activeItem = "Pre-Solicitudes",
                    modifier = Modifier.width(260.dp),
                    onItemClick = navigateFromSidebar
                )
                Box(modifier = Modifier.weight(1f)) {
                    content()
                }
            }
        }
    }

    if (showProvisionalDialog && provisionalResult != null) {
        Dialog(onDismissRequest = { showProvisionalDialog = false; provisionalResult = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SaseBorder),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SaseGreen, modifier = Modifier.size(48.dp))
                    Text("Expediente Provisional Creado", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 18.sp)
                    Text(provisionalResult ?: "", color = SaseText, fontSize = 12.sp, textAlign = TextAlign.Center)
                    Button(
                        onClick = { showProvisionalDialog = false; provisionalResult = null },
                        colors = ButtonDefaults.buttonColors(containerColor = SaseNavy),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cerrar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreApplicationList(
    apps: List<PreApplication>,
    selectedFolio: String?,
    onSelect: (PreApplication) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Text("Solicitudes", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SaseNavy)
        Spacer(modifier = Modifier.height(8.dp))
        if (apps.isEmpty()) {
            Text("No hay solicitudes que coincidan con la búsqueda.", color = SaseMuted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 16.dp))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                apps.forEach { app ->
                    val isSelected = app.folio == selectedFolio
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) SaseBlue.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.5f))
                            .border(if (isSelected) 1.dp else 0.dp, if (isSelected) SaseBlue.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { onSelect(app) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.alumnoNombreCompleto, fontWeight = FontWeight.Bold, color = SaseText, fontSize = 12.sp)
                            Text(app.folio, color = SaseMuted, fontSize = 10.sp)
                            if (app.documentosDeclarados.count { it.declarado } < app.documentosDeclarados.size) {
                                Text(
                                    "Faltan ${app.documentosDeclarados.size - app.documentosDeclarados.count { it.declarado }} docs",
                                    color = SaseOrange, fontSize = 9.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusBadge(app.status)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: PreApplicationStatus) {
    val (color, text) = when (status) {
        PreApplicationStatus.ENVIADA -> SaseOrange to "Pendiente"
        PreApplicationStatus.PENDIENTE_CORRECCION -> SaseRed to "Requiere corrección"
        PreApplicationStatus.ACEPTADA -> SaseGreen to "Aceptada"
        PreApplicationStatus.BORRADOR -> SaseMuted to "Borrador"
        PreApplicationStatus.CANCELADA -> Color(0xFF991B1B) to "Cancelada"
        PreApplicationStatus.DUPLICADA -> Color(0xFFB45309) to "Duplicada"
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Tabbed Detail ──────────────────────────────────────────────────────────

private val TAB_LABELS = listOf("Resumen A-I", "Observaciones", "Documentos")

@Composable
private fun PreApplicationDetailTabs(
    preApp: PreApplication?,
    onApprove: (String) -> Unit,
    onMarkCorrection: (String) -> Unit,
    onProvisionalCreated: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (preApp == null) {
        GlassCard(modifier = modifier) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Inbox, contentDescription = null, tint = SaseMuted.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Selecciona una solicitud", color = SaseMuted, fontSize = 13.sp)
                Text("Para ver su detalle y procesarla", color = SaseMuted.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
        return
    }

    var selectedTab by remember(preApp.folio) { mutableStateOf(0) }

    GlassCard(modifier = modifier) {
        // Folio + Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(preApp.folio, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = SaseNavy)
            StatusBadge(preApp.status)
        }
        if (preApp.submittedAt != null) {
            Text("Enviado: ${preApp.submittedAt}", color = SaseMuted, fontSize = 10.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = SaseNavy,
            modifier = Modifier.fillMaxWidth()
        ) {
            TAB_LABELS.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(label, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            when (selectedTab) {
                0 -> ResumenTab(preApp)
                1 -> ObservacionesTab(preApp)
                2 -> DocumentosTab(preApp)
            }

            // Acciones comunes
            if (preApp.status == PreApplicationStatus.ENVIADA || preApp.status == PreApplicationStatus.PENDIENTE_CORRECCION) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = SaseBorder)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (preApp.status != PreApplicationStatus.PENDIENTE_CORRECCION) {
                        OutlinedButton(
                            onClick = { onMarkCorrection(preApp.folio) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SaseOrange),
                            border = BorderStroke(1.dp, SaseOrange.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Requiere corrección", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                    Button(
                        onClick = { onApprove(preApp.folio) },
                        colors = ButtonDefaults.buttonColors(containerColor = SaseGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Aceptar", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            // Fase 3C: Crear expediente provisional
            if (preApp.status == PreApplicationStatus.ACEPTADA) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = SaseBorder)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val student = PreApplicationViewModel.buildProvisionalStudent(preApp)
                        val existing = com.example.data.repository.MockStudentRepositoryImpl().students.value
                        onProvisionalCreated("Expediente provisional ${student.id} creado para ${student.fullName}. Estatus: Alta pendiente. Diríjase a Inscripciones para completar el registro oficial.")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaseBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Crear expediente provisional", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

// ── Tab 1: Resumen A-I ─────────────────────────────────────────────────────

@Composable
private fun ResumenTab(preApp: PreApplication) {
    // Bloque A: Pre-solicitud
    SectionHeader("A — Trámite")
    DetailRow("Tipo", preApp.tramite)
    DetailRow("Ciclo", preApp.cicloEscolar)
    DetailRow("Grado", "${preApp.gradoSolicitado}°")
    Spacer(modifier = Modifier.height(8.dp))

    // Bloque B: Datos del Alumno
    SectionHeader("B — Datos del Alumno")
    DetailRow("Nombre", preApp.alumnoNombreCompleto)
    DetailRow("CURP", preApp.alumnoCurp)
    DetailRow("Nacimiento", preApp.alumnoFechaNacimiento)
    DetailRow("Sexo", preApp.alumnoSexo)
    DetailRow("Nacionalidad", preApp.alumnoNacionalidad)
    DetailRow("Entidad", preApp.alumnoEntidadNacimiento)
    DetailRow("Domicilio", preApp.alumnoDomicilio)
    DetailRow("Teléfono", preApp.alumnoTelefonoCasa)
    DetailRow("Escuela", preApp.escuelaProcedencia)
    Spacer(modifier = Modifier.height(8.dp))

    // Bloque C: Responsables
    SectionHeader("C — Responsables")
    if (preApp.responsables.isEmpty()) {
        Text("Sin responsables registrados", color = SaseOrange, fontSize = 10.sp)
    } else {
        preApp.responsables.forEachIndexed { i, r ->
            Text("Responsable ${i + 1}: ${r.nombreCompleto} (${r.parentesco})", color = SaseText, fontSize = 10.sp)
            DetailRow("  Teléfono", r.telefono)
            if (r.correo != null) DetailRow("  Correo", r.correo)
        }
    }
    Spacer(modifier = Modifier.height(8.dp))

    // Bloque D: Autorizados
    SectionHeader("D — Autorizados para recoger")
    if (preApp.autorizados.isEmpty()) {
        Text("Sin autorizados registrados", color = SaseMuted, fontSize = 10.sp)
    } else {
        preApp.autorizados.forEach { a ->
            Text("${a.nombreCompleto} (${a.parentesco}) - ${a.telefono}", color = SaseText, fontSize = 10.sp)
        }
    }
    Spacer(modifier = Modifier.height(8.dp))

    // Bloque E: Ficha Médica
    SectionHeader("E — Ficha Médica Familiar")
    val m = preApp.fichaMedicaFamiliar
    DetailRow("Servicio médico", m.servicioMedico)
    if (m.numeroAfiliacion != null) DetailRow("Afiliación", m.numeroAfiliacion)
    if (m.tipoSangre != null) DetailRow("Tipo sangre", m.tipoSangre)
    DetailRow("Alergias", m.alergias.ifBlank { "Ninguna" })
    DetailRow("Padecimientos", m.padecimientos.ifBlank { "Ninguno" })
    DetailRow("Medicamentos", m.medicamentos.ifBlank { "Ninguno" })
    Spacer(modifier = Modifier.height(8.dp))

    // Bloque F: Contexto Sociofamiliar
    SectionHeader("F — Contexto Sociofamiliar")
    val s = preApp.contextoSociofamiliar
    DetailRow("Vive con", s.viveConQuien)
    DetailRow("Tipo familia", s.tipoFamilia)
    DetailRow("Sostén económico", s.sostenEconomico)
    DetailRow("Vivienda", s.tipoVivienda)
    Spacer(modifier = Modifier.height(8.dp))

    // Bloque G: UDEII
    SectionHeader("G — Antecedentes UDEII")
    val u = preApp.antecedentesUdeii
    Text(u.antecedenteApoyo.ifBlank { "Sin antecedentes de apoyo" }, color = SaseText, fontSize = 10.sp)
    Spacer(modifier = Modifier.height(8.dp))

    // Bloque H + I: Documentos y Consentimientos (resumen)
    SectionHeader("H — Documentos Declarados")
    val totalDocs = preApp.documentosDeclarados.size
    val declarados = preApp.documentosDeclarados.count { it.declarado }
    Text("$declarados de $totalDocs documentos declarados", color = if (declarados == totalDocs) SaseGreen else SaseOrange, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(4.dp))
    SectionHeader("I — Consentimientos")
    val c = preApp.consentimientos
    val totalCons = 9
    val aceptados = listOf(c.avisoPrivacidad, c.usoDatosExpediente, c.fotoAlumno, c.fotoCredencial, c.fotoAutorizados, c.comunicacionWhatsapp, c.reglamentoInterno, c.marcoConvivencia, c.corresponsabilidadFamiliar).count { it }
    Text("$aceptados de $totalCons consentimientos aceptados", color = if (aceptados == totalCons) SaseGreen else SaseOrange, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
}

// ── Tab 2: Observaciones ──────────────────────────────────────────────────

@Composable
private fun ObservacionesTab(preApp: PreApplication) {
    var observaciones by remember(preApp.folio) { mutableStateOf(preApp.observacionesSecretaria) }
    var motivoCorreccion by remember(preApp.folio) { mutableStateOf(preApp.motivoCorreccion) }
    var notificationResult by remember { mutableStateOf<String?>(null) }

    SectionHeader("Observaciones de Secretaría")
    Spacer(modifier = Modifier.height(4.dp))
    OutlinedTextField(
        value = observaciones,
        onValueChange = {
            observaciones = it
            PreApplicationViewModel.setObservaciones(preApp.folio, it)
        },
        placeholder = { Text("Escribe observaciones internas...", color = SaseMuted) },
        modifier = Modifier.fillMaxWidth().height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.5f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.3f)
        )
    )

    Spacer(modifier = Modifier.height(12.dp))

    SectionHeader("Motivo de corrección")
    Spacer(modifier = Modifier.height(4.dp))
    OutlinedTextField(
        value = motivoCorreccion,
        onValueChange = {
            motivoCorreccion = it
            PreApplicationViewModel.setMotivoCorreccion(preApp.folio, it)
        },
        placeholder = { Text("Especifica qué debe corregir la familia...", color = SaseMuted) },
        modifier = Modifier.fillMaxWidth().height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.5f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.3f)
        )
    )

    Spacer(modifier = Modifier.height(12.dp))

    Button(
        onClick = {
            notificationResult = PreApplicationViewModel.notifyFamily(preApp.folio)
        },
        colors = ButtonDefaults.buttonColors(containerColor = SaseCyan),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Enviar notificación mock a la familia", fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }

    if (notificationResult != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SaseCyan.copy(alpha = 0.08f))
                .padding(8.dp)
        ) {
            Text(notificationResult ?: "", color = SaseCyan, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ── Tab 3: Documentos ──────────────────────────────────────────────────────

@Composable
private fun DocumentosTab(preApp: PreApplication) {
    SectionHeader("Documentos Declarados por la Familia")
    Spacer(modifier = Modifier.height(4.dp))
    preApp.documentosDeclarados.forEach { doc ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (doc.declarado) SaseGreen.copy(alpha = 0.05f) else SaseRed.copy(alpha = 0.05f))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            if (doc.declarado) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SaseGreen, modifier = Modifier.size(16.dp))
            } else {
                Icon(Icons.Default.Warning, contentDescription = null, tint = SaseRed.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(doc.nombre, color = if (doc.declarado) SaseText else SaseRed, fontSize = 11.sp, fontWeight = if (doc.declarado) FontWeight.Normal else FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                if (doc.declarado) "Declarado" else "Falta",
                color = if (doc.declarado) SaseGreen else SaseRed,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
    }

    Spacer(modifier = Modifier.height(12.dp))
    SectionHeader("Consentimientos")
    Spacer(modifier = Modifier.height(4.dp))
    val c = preApp.consentimientos
    ConsentDetailRow("Aviso de privacidad", c.avisoPrivacidad)
    ConsentDetailRow("Uso de datos para expediente", c.usoDatosExpediente)
    ConsentDetailRow("Fotografía del alumno", c.fotoAlumno)
    ConsentDetailRow("Fotografía para credencial", c.fotoCredencial)
    ConsentDetailRow("Fotografía de autorizados", c.fotoAutorizados)
    ConsentDetailRow("Comunicación WhatsApp/teléfono/correo", c.comunicacionWhatsapp)
    ConsentDetailRow("Reglamento Escolar Interno", c.reglamentoInterno)
    ConsentDetailRow("Marco para la Convivencia", c.marcoConvivencia)
    ConsentDetailRow("Corresponsabilidad familiar", c.corresponsabilidadFamiliar)
}

// ── Mobile ─────────────────────────────────────────────────────────────────

@Composable
private fun MobilePreApplicationDetail(
    preApp: PreApplication,
    viewModel: LabViewModel,
    onBack: () -> Unit,
    onApprove: (String) -> Unit,
    onMarkCorrection: (String) -> Unit,
    onProvisionalCreated: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SaseBgSoft)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = SaseNavy)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text("Detalle de solicitud", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SaseNavy)
        }
        Spacer(modifier = Modifier.height(12.dp))
        PreApplicationDetailTabs(
            preApp = preApp,
            onApprove = onApprove,
            onMarkCorrection = onMarkCorrection,
            onProvisionalCreated = onProvisionalCreated,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SaseNavy)
    Spacer(modifier = Modifier.height(2.dp))
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text("$label: ", color = SaseMuted, fontSize = 10.sp)
        Text(value, color = SaseText, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ConsentDetailRow(label: String, value: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Icon(
            if (value) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (value) SaseGreen else SaseRed.copy(alpha = 0.6f),
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = SaseText, fontSize = 10.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            if (value) "Aceptado" else "Pendiente",
            color = if (value) SaseGreen else SaseRed,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
