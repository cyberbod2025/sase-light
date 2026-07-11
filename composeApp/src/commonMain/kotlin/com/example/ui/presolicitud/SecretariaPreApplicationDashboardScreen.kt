package com.example.ui.presolicitud

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.enrollment.AnnualEnrollmentFlowResult
import com.example.data.presolicitud.*
import com.example.ui.*
import com.example.util.LocalToast
import com.example.viewmodel.LabViewModel
import com.example.viewmodel.OfficialEnrollmentResult
import com.example.viewmodel.PreApplicationViewModel
import com.example.viewmodel.ReadinessResult
import com.example.viewmodel.Screen
import kotlinx.coroutines.CoroutineScope
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
                "Altas Oficiales" -> viewModel.navigateTo(Screen.OfficialEnrollmentDashboard)
                "Credenciales" -> viewModel.navigateTo(Screen.StudentCredentialDashboard)
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
                    onProvisionalCreated = { msg -> provisionalResult = msg; showProvisionalDialog = true },
                    scope = scope
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
                            ReturnToDashboardButton(
                                onClick = { viewModel.navigateTo(Screen.SecretaryDashboard) },
                                label = if (isMobile) "Inicio" else "Volver al inicio"
                            )
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
                                viewModel = viewModel,
                                onApprove = { folio ->
                                    PreApplicationViewModel.approvePreApplication(folio)
                                    toast("Pre-solicitud $folio aceptada — pendiente alta oficial")
                                },
                                onMarkCorrection = { folio ->
                                    PreApplicationViewModel.markForCorrection(folio)
                                    toast("Pre-solicitud $folio marcada para corrección — se notificará a la familia")
                                },
                                onProvisionalCreated = { msg -> provisionalResult = msg; showProvisionalDialog = true },
                                modifier = Modifier.weight(1f),
                                scope = scope
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
                        colors = ButtonDefaults.buttonColors(containerColor = SaseNavy, contentColor = Color.White),
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

private val TAB_LABELS = listOf("Datos", "Resp.", "Contexto", "Docs", "Obs.")
private val SecretaryMobileBg = Color(0xFF08111F)
private val SecretaryMobileCard = Color(0xFFF8FBFF)
private val officialEnrollmentPattern = Regex("^S310-[A-Z0-9]{10}-\\d{2}$")

private fun visibleOfficialEnrollment(student: OfficialStudent?): String =
    student?.matriculaOficial
        ?.takeIf { student.status == OfficialStudentStatus.ALTA_OFICIAL_CON_GRUPO && it.matches(officialEnrollmentPattern) }
        ?: "Por asignar"

@Composable
private fun PreApplicationDetailTabs(
    preApp: PreApplication?,
    viewModel: LabViewModel,
    onApprove: (String) -> Unit,
    onMarkCorrection: (String) -> Unit,
    onProvisionalCreated: (String) -> Unit,
    modifier: Modifier = Modifier,
    scope: CoroutineScope
) {
    val photos by PreApplicationViewModel.photos.collectAsState()
    val reviewObservations by PreApplicationViewModel.reviewObservations.collectAsState()
    val officialStudents by PreApplicationViewModel.officialStudents.collectAsState()

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
    var showOfficialEnrollmentPanel by remember(preApp.folio) { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    GlassCard(modifier = modifier, containerColor = SecretaryMobileCard.copy(alpha = 0.92f)) {
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

        Spacer(modifier = Modifier.height(8.dp))

        // Photo section
        val photoState = photos[preApp.folio]
        val officialEnrollmentPendingItems = PreApplicationViewModel.officialEnrollmentPendingItems(preApp)
        val officialStudent = officialStudents.find { it.preApplicationFolio == preApp.folio }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PhotoPlaceholderBox(
                label = "Foto alumno",
                photoUrl = photoState?.studentPhotoMockUrl,
                onCapture = { PreApplicationViewModel.simulateCaptureStudentPhoto(preApp.folio) },
                modifier = Modifier.weight(1f)
            )
            PhotoPlaceholderBox(
                label = "Foto responsable",
                photoUrl = photoState?.responsablePhotoMockUrl,
                onCapture = { PreApplicationViewModel.simulateCaptureResponsablePhoto(preApp.folio) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OfficialEnrollmentReadinessCard(
            preApp = preApp,
            pendingItems = officialEnrollmentPendingItems,
            officialStudent = officialStudent,
            onStartOfficialEnrollment = { showOfficialEnrollmentPanel = true }
        )

        if (showOfficialEnrollmentPanel) {
            Spacer(modifier = Modifier.height(10.dp))
            OfficialEnrollmentContextualPanel(
                preApp = preApp,
                officialStudent = officialStudent,
                onClose = { showOfficialEnrollmentPanel = false },
                scope = scope
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Full-width scroll target for mobile; avoids tiny draggable tab handles.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TAB_LABELS.forEachIndexed { index, label ->
                val selected = selectedTab == index
                val bgColor by animateColorAsState(
                    targetValue = if (selected) SaseNavy else Color.White.copy(alpha = 0.58f),
                    animationSpec = spring(dampingRatio = 0.68f, stiffness = 300f),
                    label = "tabBg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (selected) Color.White else SaseNavy,
                    animationSpec = spring(dampingRatio = 0.68f, stiffness = 300f),
                    label = "tabText"
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(bgColor)
                        .border(1.dp, if (selected) SaseBlue.copy(alpha = 0.35f) else SaseBorder, RoundedCornerShape(999.dp))
                        .clickable { selectedTab = index }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column {
            when (selectedTab) {
                0 -> DatosTab(preApp)
                1 -> ResponsablesTab(preApp)
                2 -> ContextoTab(preApp)
                3 -> DocumentosTab(preApp)
                4 -> RevisionTab(
                    preApp = preApp,
                    reviewObservations = reviewObservations[preApp.folio].orEmpty()
                )
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
                    OutlinedButton(
                        onClick = { showEditDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SaseBlue),
                        border = BorderStroke(1.dp, SaseBlue.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar datos", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
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
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onApprove(preApp.folio) },
                    colors = ButtonDefaults.buttonColors(containerColor = SaseGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Aceptar para alta oficial", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

        }

        // Edit dialog for Secretary
        if (showEditDialog) {
            EditPreApplicationDialog(
                preApp = preApp,
                onDismiss = { showEditDialog = false }
            )
        }
    }
}

@Composable
private fun EditPreApplicationDialog(
    preApp: PreApplication,
    onDismiss: () -> Unit
) {
    var nombreEdit by remember(preApp.folio) { mutableStateOf(preApp.alumnoNombreCompleto) }
    var telefonoEdit by remember(preApp.folio) { mutableStateOf(preApp.alumnoTelefonoCasa) }
    var domicilioEdit by remember(preApp.folio) { mutableStateOf(preApp.alumnoDomicilio) }

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
                Text("Editar datos capturados", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 18.sp)
                Text("Ajuste interno de Secretaría antes de aceptar para alta oficial. Folio: ${preApp.folio}", color = SaseMuted, fontSize = 11.sp)

                OutlinedTextField(
                    value = nombreEdit,
                    onValueChange = { nombreEdit = it.uppercase() },
                    label = { Text("Nombre completo") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = telefonoEdit,
                    onValueChange = { telefonoEdit = it.filter { char -> char.isDigit() }.take(10) },
                    label = { Text("Teléfono") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = domicilioEdit,
                    onValueChange = { domicilioEdit = it.uppercase() },
                    label = { Text("Domicilio") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cerrar sin guardar", color = SaseMuted)
                    }
                    Button(
                        onClick = {
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaseNavy, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Guardar ajustes (mock)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Tab 1: Datos ──────────────────────────────────────────────────────────

@Composable
private fun DatosTab(preApp: PreApplication) {
    SectionHeader("Bloque A — Pre-solicitud")
    DetailRow("Tipo de trámite", preApp.tramite)
    DetailRow("Ciclo escolar", preApp.cicloEscolar)
    DetailRow("Grado solicitado", "${preApp.gradoSolicitado}°")
    Spacer(modifier = Modifier.height(10.dp))

    SectionHeader("Bloque B — Datos del Alumno")
    DetailRow("Nombre completo", preApp.alumnoNombreCompleto)
    DetailRow("CURP", preApp.alumnoCurp)
    DetailRow("Fecha de nacimiento", preApp.alumnoFechaNacimiento)
    DetailRow("Sexo", preApp.alumnoSexo)
    DetailRow(PreApplicationViewModel.promedioLabelForGrade(preApp.gradoSolicitado), preApp.promedioGradoAnterior?.toString() ?: "Pendiente")
    DetailRow("Nacionalidad", preApp.alumnoNacionalidad)
    DetailRow("Entidad de nacimiento", preApp.alumnoEntidadNacimiento)
    DetailRow("Domicilio", preApp.alumnoDomicilio)
    DetailRow("Teléfono casa", preApp.alumnoTelefonoCasa)
    DetailRow("Escuela procedencia", preApp.escuelaProcedencia)
}

// ── Tab 2: Responsables ────────────────────────────────────────────────────

@Composable
private fun ResponsablesTab(preApp: PreApplication) {
    SectionHeader("Bloque C — Responsables")
    val tramite = preApp.personaTramite
    DetailRow("Persona que tramita", tramite.nombreCompleto.ifBlank { "Pendiente" })
    DetailRow("Parentesco trámite", tramite.parentesco.ifBlank { "Pendiente" })
    DetailRow("Teléfono trámite", tramite.telefono.ifBlank { "Pendiente" })
    DetailRow("Identificación trámite", tramite.identificacionPresentada.ifBlank { "Pendiente" })
    DetailRow("Usar como contacto 1", if (tramite.usarComoContactoPrincipal) "Sí" else "No")
    Spacer(modifier = Modifier.height(8.dp))
    if (preApp.responsables.isEmpty()) {
        Text("No se registraron responsables", color = SaseOrange, fontSize = 10.sp)
    } else {
        preApp.responsables.forEachIndexed { i, r ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Responsable ${i + 1}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SaseNavy)
                    Spacer(modifier = Modifier.height(4.dp))
                    DetailRow("Nombre", r.nombreCompleto)
                    DetailRow("Parentesco", r.parentesco)
                    DetailRow("Teléfono", r.telefono)
                    if (r.correo != null) DetailRow("Correo", r.correo)
                    DetailRow("Ocupación", r.ocupacion)
                    DetailRow("Horario contacto", r.horarioContacto)
                    DetailRow("Identificación", r.identificacionApresentar)
                    DetailRow("Vive con alumno", if (r.viveConAlumno) "Sí" else "No")
                    DetailRow("Puede recoger", if (r.puedeRecoger) "Sí" else "No")
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("Bloque D — Autorizados para recoger")
    if (preApp.autorizados.isEmpty()) {
        Text("No se registraron autorizados", color = SaseMuted, fontSize = 10.sp)
    } else {
        preApp.autorizados.forEach { a ->
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.4f)).padding(8.dp)
            ) {
                DetailRow(a.nombreCompleto, "${a.parentesco} — ${a.telefono}")
            }
        }
    }
}

// ── Tab 3: Contexto ────────────────────────────────────────────────────────

@Composable
private fun ContextoTab(preApp: PreApplication) {
    SectionHeader("Bloque E — Ficha Médica Familiar")
    val m = preApp.fichaMedicaFamiliar
    DetailRow("Servicio médico", m.servicioMedico)
    if (m.numeroAfiliacion != null) DetailRow("Núm. afiliación", m.numeroAfiliacion)
    if (m.tipoSangre != null) DetailRow("Tipo de sangre", m.tipoSangre)
    DetailRow("Alergias", m.alergias.ifBlank { "Ninguna" })
    DetailRow("Padecimientos", m.padecimientos.ifBlank { "Ninguno" })
    DetailRow("Medicamentos", m.medicamentos.ifBlank { "Ninguno" })
    DetailRow("Restricción física", m.restriccionFisica.ifBlank { "Ninguna" })
    DetailRow("Usa lentes", if (m.usaLentes) "Sí" else "No")
    DetailRow("Salud bucal", m.saludBucal)
    DetailRow("Cartilla vacunación", if (m.cartillaVacunacion) "Sí" else "No")
    Spacer(modifier = Modifier.height(10.dp))

    SectionHeader("Bloque F — Contexto Sociofamiliar")
    val s = preApp.contextoSociofamiliar
    DetailRow("Vive con", s.viveConQuien)
    DetailRow("Tipo de familia", s.tipoFamilia)
    DetailRow("Hijo único", if (s.hijoUnico) "Sí" else "No")
    DetailRow("Lugar entre hermanos", s.lugarEntreHermanos.toString())
    DetailRow("Hermanos en escuela", if (s.hermanosEnEscuela) "Sí" else "No")
    DetailRow("Integrantes hogar", s.integrantesHogar.toString())
    DetailRow("Sostén económico", s.sostenEconomico)
    DetailRow("Ingreso", s.ingresoRangos)
    DetailRow("Vivienda", s.tipoVivienda)
    DetailRow("Servicios básicos", if (s.serviciosBásicos) "Sí" else "No")
    DetailRow("Internet", if (s.internet) "Sí" else "No")
    DetailRow("Dispositivo tareas", s.dispositivoTareas)
    DetailRow("Beca/apoyo", s.becaApoyo.ifBlank { "Ninguno" })
    DetailRow("Transporte", s.transporte)
    DetailRow("Dificultad materiales", if (s.dificultadMateriales) "Sí" else "No")
    Spacer(modifier = Modifier.height(10.dp))

    SectionHeader("Bloque G — Antecedentes UDEII")
    val u = preApp.antecedentesUdeii
    DetailRow("Antecedente", u.antecedenteApoyo.ifBlank { "Ninguno" })
    DetailRow("Terapia lenguaje", if (u.terapiaLenguaje) "Sí" else "No")
    DetailRow("Apoyo psicológico", if (u.apoyoPsicologico) "Sí" else "No")
    DetailRow("Apoyo pedagógico", if (u.apoyoPedagogico) "Sí" else "No")
    DetailRow("Documentos disponibles", u.documentosDisponibles.ifBlank { "Ninguno" })
    DetailRow("Observaciones", u.observacionesFamiliares.ifBlank { "Sin observaciones" })
}

// ── Photo Placeholder ──────────────────────────────────────────────────────

@Composable
private fun OfficialEnrollmentReadinessCard(
    preApp: PreApplication,
    pendingItems: List<String>,
    officialStudent: OfficialStudent?,
    onStartOfficialEnrollment: () -> Unit
) {
    val isReadyByChecklist = pendingItems.isEmpty()
    val isPersistedReady = preApp.readinessStatus == ReadinessStatus.READY
    val officialStarted = officialStudent != null
    var readinessMessage by remember(preApp.folio, preApp.readinessStatus) { mutableStateOf<String?>(null) }
    var readinessColor by remember(preApp.folio, preApp.readinessStatus) { mutableStateOf(SaseMuted) }
    val readerIsReadyForOfficial = isPersistedReady && pendingItems.isEmpty()
    val officialCompleted = officialStarted && pendingItems.isEmpty()
    val officialWithPending = officialStarted && pendingItems.isNotEmpty()
    val headerColor = when {
        officialCompleted || preApp.readinessStatus == ReadinessStatus.CONVERTED -> SaseGreen
        officialStarted -> SaseNavy
        readerIsReadyForOfficial -> SaseGreen
        pendingItems.isNotEmpty() || preApp.readinessStatus == ReadinessStatus.BLOCKED -> SaseOrange
        else -> SaseMuted
    }
    val headerText = when {
        officialWithPending -> "Alta oficial iniciada con pendientes"
        pendingItems.isNotEmpty() -> "Con pendientes bloqueantes"
        officialCompleted || preApp.readinessStatus == ReadinessStatus.CONVERTED -> "Alta oficial completada"
        officialStarted -> "Aceptada"
        readerIsReadyForOfficial -> "Lista para alta oficial"
        preApp.readinessStatus == ReadinessStatus.BLOCKED -> "Con pendientes"
        else -> "Pendiente"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(headerColor.copy(alpha = 0.08f))
            .border(1.dp, headerColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (isPersistedReady || officialStarted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = headerColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    headerText,
                    color = headerColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
                Text(
                    when {
                        officialWithPending -> "La alta existe, pero conserva requisitos por cerrar."
                        officialStarted -> "Alta oficial iniciada desde esta pre-solicitud."
                        preApp.readyAt != null -> "Declarada lista: ${preApp.readyAt}"
                        else -> "La matrícula se genera solo desde este folio validado."
                    },
                    color = SaseMuted,
                    fontSize = 9.sp
                )
            }
        }

        if (pendingItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Pendientes institucionales:", color = SaseOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                pendingItems.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(SaseOrange)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(item, color = SaseText, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        if (!officialStarted && !isPersistedReady) {
            OutlinedButton(
                onClick = {
                    val result = PreApplicationViewModel.markReadyForOfficialEnrollment(preApp.folio)
                    readinessMessage = result.message
                    readinessColor = result.toUiColor()
                },
                enabled = isReadyByChecklist,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SaseGreen),
                border = BorderStroke(1.dp, if (isReadyByChecklist) SaseGreen.copy(alpha = 0.55f) else SaseMuted.copy(alpha = 0.22f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.FactCheck, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Declarar lista institucionalmente", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            if (!isReadyByChecklist) {
                Text(
                    "Completa los pendientes institucionales para habilitar esta acción.",
                    color = SaseOrange,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Button(
            onClick = onStartOfficialEnrollment,
            enabled = isPersistedReady || officialStarted,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (officialStarted) SaseBlue else SaseGreen,
                disabledContainerColor = SaseMuted.copy(alpha = 0.18f),
                disabledContentColor = SaseMuted
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(if (isPersistedReady || officialStarted) Icons.Default.AssignmentTurnedIn else Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                if (officialStarted) "Ver seguimiento de alta" else "Dar de alta oficial",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
        if (!officialStarted && !isPersistedReady) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(SaseOrange.copy(alpha = 0.08f))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    "Disponible cuando Secretaría haya aceptado la solicitud, cotejado documentos y capturado fotografías requeridas.",
                    color = SaseOrange,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (readinessMessage != null) {
            Text(
                readinessMessage ?: "",
                color = readinessColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun OfficialEnrollmentContextualPanel(
    preApp: PreApplication,
    officialStudent: OfficialStudent?,
    onClose: () -> Unit,
    scope: CoroutineScope
) {
    val grade = preApp.gradoSolicitado
    val groupOptions = PreApplicationViewModel.groupOptionsForGrade(grade)
    val age = remember(preApp.alumnoFechaNacimiento) { PreApplicationViewModel.calculateAgeFromBirthDate(preApp.alumnoFechaNacimiento) }
    val suggestedGroup = remember(preApp.folio, grade, preApp.promedioGradoAnterior) {
        PreApplicationViewModel.suggestInitialGroup(grade, preApp.alumnoSexo, age, preApp.promedioGradoAnterior)
    }
    val officialStudents by PreApplicationViewModel.officialStudents.collectAsState()
    var selectedGroup by remember(preApp.folio) { mutableStateOf(officialStudent?.grupoAsignado ?: officialStudent?.grupoSugerido ?: suggestedGroup ?: groupOptions.firstOrNull().orEmpty()) }
    var groupConfirmed by remember(preApp.folio) { mutableStateOf(false) }
    var resultMessage by remember(preApp.folio) { mutableStateOf<String?>(null) }
    var resultColor by remember(preApp.folio) { mutableStateOf(SaseGreen) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.66f))
            .border(1.dp, SaseGreen.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Alta Oficial contextual", color = SaseNavy, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Text("Folio precargado: ${preApp.folio}", color = SaseMuted, fontSize = 10.sp)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = SaseMuted)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SaseOrange.copy(alpha = 0.1f))
                .padding(10.dp)
        ) {
            Text(
                "La matrícula se asignará al completar CURP, documentos, fotografías y alta oficial.",
                color = SaseOrange,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        DetailRow("Nombre", preApp.alumnoNombreCompleto)
        DetailRow("Grado ingreso", "${grade}°")
        DetailRow(PreApplicationViewModel.promedioLabelForGrade(grade), preApp.promedioGradoAnterior?.toString() ?: "Pendiente")
        DetailRow("Persona que tramita", preApp.personaTramite.nombreCompleto.ifBlank { "Pendiente" })
        DetailRow(
            "Matrícula",
            visibleOfficialEnrollment(officialStudent)
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (grade in 1..3) {
            SectionHeader("Asignación inicial de grupo")
            DetailRow("Grupo sugerido por balance", suggestedGroup ?: "Sin sugerencia disponible")
            Text("Criterios: sexo, edad y ${PreApplicationViewModel.promedioLabelForGrade(grade).lowercase()}.", color = SaseMuted, fontSize = 9.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                groupOptions.forEach { option ->
                    FilterChip(
                        selected = selectedGroup == option,
                        onClick = { selectedGroup = option },
                        label = { Text(option, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
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
            Spacer(modifier = Modifier.height(8.dp))
            GroupBalanceSnapshot(grade, groupOptions, officialStudents)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
                Text(
                    "Confirmación Secretaría/Dirección para grupo inicial.",
                    color = SaseText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (officialStudent == null) {
            Button(
                onClick = {
                    val enrollmentResult = PreApplicationViewModel.startOfficialEnrollment(preApp, selectedGroup)
                    resultMessage = enrollmentResult.message
                    resultColor = enrollmentResult.toUiColor()
                },
                enabled = selectedGroup.isNotBlank() && preApp.personaTramite.nombreCompleto.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SaseGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Iniciar alta oficial", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        } else {
            OfficialEnrollmentConfirmation(preApp, officialStudent)

            if (officialStudent.status == OfficialStudentStatus.PENDIENTE_ASIGNACION_GRUPO) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val confirmResult = PreApplicationViewModel.confirmInitialGroup(preApp.folio, selectedGroup)
                        resultMessage = confirmResult.message
                        resultColor = confirmResult.toUiColor()
                    },
                    enabled = groupConfirmed && selectedGroup.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = SaseNavy, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Confirmar grupo inicial", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                }
                Text(
                    "Mientras no se confirme, el estado permanece como PENDIENTE_ASIGNACION_GRUPO.",
                    color = SaseMuted,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        val isProcessingV2 by PreApplicationViewModel.isProcessingAnnualEnrollmentV2.collectAsState()
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                scope.launch {
                    PreApplicationViewModel.setProcessingAnnualEnrollmentV2(true)
                    try {
                        val requestedGrade = preApp.gradoSolicitado
                        val v2Result = PreApplicationViewModel.processAnnualEnrollmentV2(
                            declaredMovement = preApp.tramite,
                            normalizedCurp = preApp.alumnoCurp,
                            folio = preApp.folio,
                            requestedGrade = requestedGrade,
                            previousGroup = selectedGroup,
                            schoolYear = preApp.cicloEscolar,
                            studentFullName = preApp.alumnoNombreCompleto
                        )
                        resultMessage = buildV2Message(v2Result)
                        resultColor = when (v2Result) {
                            is AnnualEnrollmentFlowResult.Completed,
                            is AnnualEnrollmentFlowResult.AlreadyCompleted -> SaseGreen
                            is AnnualEnrollmentFlowResult.NeedsDecision -> SaseBlue
                            is AnnualEnrollmentFlowResult.Conflict -> SaseOrange
                        }
                    } finally {
                        PreApplicationViewModel.setProcessingAnnualEnrollmentV2(false)
                    }
                }
            },
            enabled = !isProcessingV2 && preApp.personaTramite.nombreCompleto.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isProcessingV2) SaseMuted.copy(alpha = 0.15f) else SaseBlue.copy(alpha = 0.15f),
                contentColor = if (isProcessingV2) SaseMuted else SaseBlue
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                if (isProcessingV2) Icons.Default.HourglassEmpty else Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                if (isProcessingV2) "Procesando..." else "Procesar con flujo anual nuevo (V2)",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }

        if (resultMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(resultMessage ?: "", color = resultColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun buildV2Message(result: AnnualEnrollmentFlowResult): String = when (result) {
    is AnnualEnrollmentFlowResult.Completed -> buildString {
        appendLine("Inscripción anual registrada.")
        append("Matrícula: ${result.enrollmentId}")
        appendLine()
        append("Grupo: pendiente de asignación.")
    }
    is AnnualEnrollmentFlowResult.AlreadyCompleted -> buildString {
        appendLine("Esta inscripción anual ya había sido registrada.")
        append("No se generaron duplicados.")
    }
    is AnnualEnrollmentFlowResult.NeedsDecision -> buildString {
        appendLine("Inscripción registrada.")
        if (result.previousGroup != null) {
            appendLine("Grupo anterior: ${result.previousGroup}.")
        }
        if (result.suggestedGroup != null) {
            appendLine("Sugerencia: ${result.suggestedGroup}.")
        }
        append("Decisión pendiente de Secretaría.")
    }
    is AnnualEnrollmentFlowResult.Conflict -> buildString {
        appendLine("No fue posible completar la inscripción.")
        appendLine("Etapa: ${result.stage}.")
        append("Motivo: ${result.message}")
    }
}

private fun OfficialEnrollmentResult.toUiColor(): Color = when (this) {
    is OfficialEnrollmentResult.Success -> SaseGreen
    is OfficialEnrollmentResult.DuplicateFolio,
    is OfficialEnrollmentResult.DuplicateCurp,
    is OfficialEnrollmentResult.DuplicateMatricula -> SaseRed
    is OfficialEnrollmentResult.NotReady,
    is OfficialEnrollmentResult.PreApplicationNotFound,
    is OfficialEnrollmentResult.MasterStudentPropagationError,
    is OfficialEnrollmentResult.Error -> SaseOrange
}

private fun ReadinessResult.toUiColor(): Color = when (this) {
    is ReadinessResult.Success,
    is ReadinessResult.AlreadyReady -> SaseGreen
    is ReadinessResult.NotReady,
    is ReadinessResult.AlreadyConverted,
    is ReadinessResult.NotFound,
    is ReadinessResult.Error -> SaseOrange
}

@Composable
private fun GroupBalanceSnapshot(
    grade: Int,
    groupOptions: List<String>,
    officialStudents: List<OfficialStudent>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.42f))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text("Estadística rápida por grupo", color = SaseNavy, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        groupOptions.forEach { group ->
            val members = officialStudents.filter { it.gradoIngreso == grade && (it.grupoAsignado ?: it.grupoSugerido) == group }
            val hombres = members.count { it.alumnoSexo == "Masculino" || it.alumnoSexo == "H" }
            val mujeres = members.count { it.alumnoSexo == "Femenino" || it.alumnoSexo == "M" }
            val promedio = members.mapNotNull { it.promedio }.takeIf { it.isNotEmpty() }?.average()
            Text(
                "$group: ${members.size} alumnos - H $hombres / M $mujeres - prom ${promedio?.let { ((it * 10).toInt() / 10.0).toString() } ?: "s/d"}",
                color = SaseText,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun OfficialEnrollmentConfirmation(preApp: PreApplication, student: OfficialStudent) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SaseGreen.copy(alpha = 0.08f))
            .padding(10.dp)
    ) {
        Text("Confirmación de alta oficial", color = SaseGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(4.dp))
        DetailRow("Folio", student.preApplicationFolio)
        DetailRow("Nombre", student.alumnoNombreCompleto)
        DetailRow("Grado ingreso", "${student.gradoIngreso}°")
        DetailRow(PreApplicationViewModel.promedioLabelForGrade(student.gradoIngreso), student.promedio?.toString() ?: "Sin promedio")
        DetailRow("Matrícula", visibleOfficialEnrollment(student))
        DetailRow("Estado inicial", student.status.name)
        DetailRow("Grupo sugerido", student.grupoSugerido ?: "Sin sugerencia")
        DetailRow("Grupo asignado", student.grupoAsignado ?: "Pendiente de confirmación")
        Spacer(modifier = Modifier.height(10.dp))
        OfficialEnrollmentReceiptPreview(preApp, student)
    }
}

@Composable
private fun OfficialEnrollmentReceiptPreview(preApp: PreApplication, student: OfficialStudent) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.75f))
            .border(1.dp, SaseNavy.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Contrarrecibo imprimible (vista placeholder)", color = SaseNavy, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        DetailRow("Folio", preApp.folio)
        DetailRow("Matrícula", visibleOfficialEnrollment(student))
        DetailRow("Alumno", student.alumnoNombreCompleto)
        DetailRow("CURP", student.curp)
        DetailRow("Grado / grupo", "${student.gradoIngreso}° / ${(student.grupoAsignado ?: student.grupoSugerido) ?: "Pendiente"}")
        DetailRow("Ciclo", preApp.cicloEscolar)
        DetailRow("Fecha", student.fechaCreacion)
        DetailRow("Tramitó", "${preApp.personaTramite.nombreCompleto} (${preApp.personaTramite.parentesco})")
        Spacer(modifier = Modifier.height(4.dp))
        Text("Documentos presentados", color = SaseNavy, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        preApp.documentosDeclarados.filter { it.declarado }.take(8).forEach { doc ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckBox, contentDescription = null, tint = SaseGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(doc.nombre, color = SaseText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text("La impresión/PDF real queda fuera de esta fase.", color = SaseOrange, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PhotoPlaceholderBox(
    label: String,
    photoUrl: String?,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
    boxSize: Dp = 64.dp
) {
    val hasPhoto = photoUrl != null
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (hasPhoto) SaseGreen.copy(alpha = 0.08f) else SaseBorder.copy(alpha = 0.2f))
            .border(1.dp, if (hasPhoto) SaseGreen.copy(alpha = 0.3f) else SaseBorder.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(boxSize)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (hasPhoto) SaseGreen.copy(alpha = 0.12f) else SaseMuted.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (hasPhoto) {
                    Icon(Icons.Default.Face, contentDescription = null, tint = SaseGreen, modifier = Modifier.size(28.dp))
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, tint = SaseMuted.copy(alpha = 0.5f), modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, color = SaseMuted, fontSize = 8.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(
                if (hasPhoto) "Capturada" else "Pendiente",
                color = if (hasPhoto) SaseGreen else SaseOrange,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (hasPhoto) SaseMuted.copy(alpha = 0.1f) else SaseCyan.copy(alpha = 0.12f))
                    .clickable { onCapture() }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    if (hasPhoto) "Restaurar" else "Simular captura",
                    color = if (hasPhoto) SaseMuted else SaseCyan,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Tab 4: Documentos ──────────────────────────────────────────────────────

@Composable
private fun DocumentosTab(preApp: PreApplication) {
    SectionHeader("Bloque H — Documentos Declarados")
    Spacer(modifier = Modifier.height(4.dp))

    val docsCount = preApp.documentosDeclarados.size
    val cotejadosCount = preApp.documentosDeclarados.count { it.cotejadoSecretaria }
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(SaseBlue.copy(alpha = 0.05f)).padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text("$cotejadosCount de $docsCount documentos cotejados", color = SaseBlue, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
    Spacer(modifier = Modifier.height(6.dp))

    preApp.documentosDeclarados.forEach { doc ->
        val declarado = doc.declarado
        val cotejado = doc.cotejadoSecretaria
        val canCotejar = declarado && !cotejado
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when {
                        cotejado -> SaseGreen.copy(alpha = 0.08f)
                        declarado -> Color.White.copy(alpha = 0.5f)
                        else -> SaseRed.copy(alpha = 0.05f)
                    }
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Declarado indicator
            Icon(
                if (declarado) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (declarado) SaseGreen else SaseRed.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(doc.nombre, color = if (declarado) SaseText else SaseRed, fontSize = 10.sp, fontWeight = if (declarado) FontWeight.Normal else FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Declarado", color = if (declarado) SaseGreen else SaseRed, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    if (cotejado) {
                        Box(modifier = Modifier.size(2.dp).clip(CircleShape).background(SaseGreen))
                        Text("Cotejado", color = SaseGreen, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            // Cotejo button
            if (canCotejar) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SaseBlue.copy(alpha = 0.1f))
                        .clickable { PreApplicationViewModel.toggleDocumentCotejado(preApp.folio, doc.nombre) }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text("Cotejar", color = SaseBlue, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            } else if (cotejado) {
                Icon(Icons.Default.VerifiedUser, contentDescription = "Cotejado", tint = SaseGreen, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
    }

    Spacer(modifier = Modifier.height(12.dp))
    SectionHeader("Bloque I — Consentimientos")
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

// ── Tab 5: Revisión ────────────────────────────────────────────────────────

@Composable
private fun RevisionTab(
    preApp: PreApplication,
    reviewObservations: List<PreApplicationViewModel.Companion.SecretariaReviewObservation>
) {
    val categories = listOf("Documentos", "Fotos", "Corrección solicitada")
    var selectedCategory by remember(preApp.folio) { mutableStateOf(categories.first()) }
    var observationDraft by remember(preApp.folio) { mutableStateOf(preApp.observacionesSecretaria) }
    var requiresCorrection by remember(preApp.folio, preApp.status) {
        mutableStateOf(preApp.status == PreApplicationStatus.PENDIENTE_CORRECCION)
    }
    var motivoCorreccion by remember(preApp.folio) { mutableStateOf(preApp.motivoCorreccion) }
    var notificationResult by remember { mutableStateOf<String?>(null) }

    SectionHeader("Observaciones para Familia")
    Text(
        "Secretaría registra notas, correcciones o comentarios que serán visibles para la familia.",
        color = SaseMuted,
        fontSize = 9.sp
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        categories.forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { selectedCategory = category },
                label = { Text(category, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SaseBlue.copy(alpha = 0.14f),
                    selectedLabelColor = SaseBlue,
                    labelColor = SaseMuted
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedCategory == category,
                    borderColor = SaseBorder,
                    selectedBorderColor = SaseBlue.copy(alpha = 0.35f)
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = observationDraft,
        onValueChange = { observationDraft = it.take(220) },
        placeholder = { Text("Captura una observación breve...", color = SaseMuted) },
        modifier = Modifier.fillMaxWidth().height(90.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.5f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.3f)
        )
    )
    Spacer(modifier = Modifier.height(8.dp))
    Button(
        onClick = {
            PreApplicationViewModel.setObservaciones(preApp.folio, observationDraft)
            PreApplicationViewModel.addReviewObservation(preApp.folio, selectedCategory, observationDraft)
            observationDraft = ""
        },
        enabled = observationDraft.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = SaseBlue),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Registrar observación", fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }

    Spacer(modifier = Modifier.height(12.dp))
    SectionHeader("Historial simple")
    if (reviewObservations.isEmpty()) {
        Text("Sin observaciones registradas para este folio.", color = SaseMuted, fontSize = 10.sp)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            reviewObservations.take(4).forEach { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.45f))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.category, color = SaseNavy, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Text(item.createdAt, color = SaseMuted, fontSize = 8.sp)
                    }
                    Text(item.note, color = SaseText, fontSize = 10.sp)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (requiresCorrection) SaseOrange.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.35f))
            .clickable { requiresCorrection = !requiresCorrection }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = requiresCorrection,
            onCheckedChange = { requiresCorrection = it },
            colors = CheckboxDefaults.colors(checkedColor = SaseOrange)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text("Requiere corrección", color = SaseText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Registra el motivo que se comunicará a la familia para que realice los ajustes.", color = SaseMuted, fontSize = 9.sp)
        }
    }

    if (requiresCorrection) {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = motivoCorreccion,
            onValueChange = { motivoCorreccion = it.take(220) },
            placeholder = { Text("¿Qué debe corregir la familia?", color = SaseMuted) },
            modifier = Modifier.fillMaxWidth().height(80.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.5f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.3f)
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                PreApplicationViewModel.setMotivoCorreccion(preApp.folio, motivoCorreccion)
                PreApplicationViewModel.markForCorrection(preApp.folio)
                PreApplicationViewModel.addReviewObservation(preApp.folio, "Corrección solicitada", motivoCorreccion)
            },
            enabled = motivoCorreccion.isNotBlank(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SaseOrange),
            border = BorderStroke(1.dp, SaseOrange.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Guardar motivo de corrección", fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Button(
        onClick = {
            notificationResult = PreApplicationViewModel.notifyFamily(preApp.folio)
        },
        colors = ButtonDefaults.buttonColors(containerColor = SaseCyan),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
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

// ── Mobile ─────────────────────────────────────────────────────────────────

@Composable
private fun MobilePreApplicationDetail(
    preApp: PreApplication,
    viewModel: LabViewModel,
    onBack: () -> Unit,
    onApprove: (String) -> Unit,
    onMarkCorrection: (String) -> Unit,
    onProvisionalCreated: (String) -> Unit,
    scope: CoroutineScope
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SecretaryMobileBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Revisión institucional", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.White)
                Text("Datos capturados por familia; acciones internas de Secretaría", color = Color.White.copy(alpha = 0.68f), fontSize = 11.sp)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        PreApplicationDetailTabs(
            preApp = preApp,
            viewModel = viewModel,
            onApprove = onApprove,
            onMarkCorrection = onMarkCorrection,
            onProvisionalCreated = onProvisionalCreated,
            modifier = Modifier.fillMaxWidth(),
            scope = scope
        )
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────

@Composable
internal fun SectionHeader(title: String) {
    Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SaseNavy)
    Spacer(modifier = Modifier.height(2.dp))
}

@Composable
internal fun DetailRow(label: String, value: String) {
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
