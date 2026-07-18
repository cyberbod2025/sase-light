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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.InstitutionalStudentRecordKey
import com.example.data.MockSaseData
import com.example.data.presolicitud.*
import com.example.ui.*
import com.example.util.LocalToast
import com.example.viewmodel.CurpCorrectionResult
import com.example.viewmodel.LabViewModel

import com.example.viewmodel.InstitutionalAnnualEnrollmentResult
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
        else preApps.filter { it.status in listOf(PreApplicationStatus.ENVIADA, PreApplicationStatus.ACEPTADA) }

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
            viewModel.navigateFromSecretarySidebar(item)
        }

        var docTabRequestedFolio by remember { mutableStateOf<String?>(null) }

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
                    onProvisionalCreated = { msg -> provisionalResult = msg; showProvisionalDialog = true },
                    docTabRequestedFolio = docTabRequestedFolio,
                    onDocTabConsumed = { docTabRequestedFolio = null },
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
                                Text("Revisión y validación de solicitudes de ingreso", fontSize = if (isMobile) 11.sp else 12.sp, color = saseMutedColor())
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
                            placeholder = { Text("Buscar por folio, CURP, nombre o teléfono...", color = saseMutedColor()) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = saseMutedColor()) },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = saseMutedColor())
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
                                focusedTextColor = saseTextColor(),
                                unfocusedTextColor = saseTextColor()
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("preapplication_search")
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
                            Text("Todas", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (showAll) SaseBlue else saseMutedColor())
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isMobile) {
                        PreApplicationList(
                            apps = visibleApps,
                            selectedFolio = selectedFolio,
                            onSelect = { selectedFolio = it.folio },
                            onFaltaDocs = { app ->
                                selectedFolio = app.folio
                                docTabRequestedFolio = app.folio
                            }
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
                                onFaltaDocs = { app ->
                                    selectedFolio = app.folio
                                    docTabRequestedFolio = app.folio
                                },
                                modifier = Modifier.width(320.dp)
                            )
                            PreApplicationDetailTabs(
                                preApp = selectedApp,
                                viewModel = viewModel,
                                onApprove = { folio ->
                                    PreApplicationViewModel.approvePreApplication(folio)
                                    toast("Pre-solicitud $folio aceptada — pendiente alta oficial")
                                },
                                onProvisionalCreated = { msg -> provisionalResult = msg; showProvisionalDialog = true },
                                docTabRequestedFolio = docTabRequestedFolio,
                                onDocTabConsumed = { docTabRequestedFolio = null },
                                modifier = Modifier.weight(1f),
                                scope = scope
                            )
                        }
                    }
                }
            }
        }

        var sidebarCollapsed by remember { mutableStateOf(false) }

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
                            collapsed = false,
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
                    collapsed = sidebarCollapsed,
                    onToggleCollapse = { sidebarCollapsed = !sidebarCollapsed },
                    modifier = Modifier.fillMaxHeight(),
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
                    Text(provisionalResult ?: "", color = saseTextColor(), fontSize = 12.sp, textAlign = TextAlign.Center)
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
    onFaltaDocs: ((PreApplication) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Text("Solicitudes", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SaseNavy)
        Spacer(modifier = Modifier.height(8.dp))
        if (apps.isEmpty()) {
            Text("No hay solicitudes que coincidan con la búsqueda.", color = saseMutedColor(), fontSize = 11.sp, modifier = Modifier.padding(vertical = 16.dp))
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
                            .testTag("preapplication_row_${app.folio}")
                            .semantics { selected = isSelected }
                            .clickable(role = Role.Button) { onSelect(app) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.alumnoNombreCompleto, fontWeight = FontWeight.Bold, color = saseTextColor(), fontSize = 12.sp)
                            Text(app.folio, color = saseMutedColor(), fontSize = 10.sp)
                            Text(app.cicloEscolar, color = saseMutedColor(), fontSize = 8.sp)
                            if (app.documentosDeclarados.any { !it.declarado }) {
                                val firstFalta = app.documentosDeclarados.firstOrNull { !it.declarado }
                                Text(
                                    "Faltan ${app.documentosDeclarados.count { !it.declarado }} docs",
                                    color = SaseOrange, fontSize = 9.sp,
                                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable {
                                        onSelect(app)
                                        onFaltaDocs?.invoke(app)
                                    }
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
        PreApplicationStatus.PENDIENTE_CORRECCION -> SaseOrange to "Pendiente"
        PreApplicationStatus.ACEPTADA -> SaseGreen to "Aceptada"
        PreApplicationStatus.BORRADOR -> saseMutedColor() to "Borrador"
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
    onProvisionalCreated: (String) -> Unit,
    docTabRequestedFolio: String? = null,
    onDocTabConsumed: () -> Unit = {},
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
                Icon(Icons.Default.Inbox, contentDescription = null, tint = saseMutedColor().copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Selecciona una solicitud", color = saseMutedColor(), fontSize = 13.sp)
                Text("Para ver su detalle y procesarla", color = saseMutedColor().copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
        return
    }

    var selectedTab by remember(preApp.folio) { mutableStateOf(0) }
    var showOfficialEnrollmentPanel by remember(preApp.folio) { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var initialDocHighlight by remember(preApp.folio) { mutableStateOf<String?>(null) }

    LaunchedEffect(docTabRequestedFolio) {
        if (docTabRequestedFolio == preApp.folio) {
            val firstMissing = preApp.documentosDeclarados.firstOrNull { !it.declarado }
            initialDocHighlight = firstMissing?.nombre
            selectedTab = 3
            onDocTabConsumed()
        }
    }
    var showDocObservationDialog by remember { mutableStateOf<String?>(null) }
    var docObservationDraft by remember { mutableStateOf("") }
    var showCurpCorrectionDialog by remember { mutableStateOf(false) }

    GlassCard(modifier = modifier, containerColor = SecretaryMobileCard.copy(alpha = 0.92f)) {
        // Folio + Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(preApp.folio, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = SaseNavy)
                Text("Ciclo: ${preApp.cicloEscolar}", color = saseMutedColor(), fontSize = 9.sp)
            }
            StatusBadge(preApp.status)
        }
        if (preApp.submittedAt != null) {
            Text("Enviado: ${preApp.submittedAt}", color = saseMutedColor(), fontSize = 10.sp)
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
            onStartOfficialEnrollment = { showOfficialEnrollmentPanel = true },
            onNavigateToDocs = { selectedTab = 3 },
            onOpenExistingStudent = { studentId ->
                viewModel.navigateTo(
                    Screen.StudentRecord(
                        studentId = studentId,
                        returnTo = Screen.SecretariaPreApplicationDashboard
                    )
                )
            },
            onCorrectCurp = { showCurpCorrectionDialog = true }
        )

        if (showOfficialEnrollmentPanel) {
            Spacer(modifier = Modifier.height(10.dp))
            OfficialEnrollmentContextualPanel(
                preApp = preApp,
                officialStudent = officialStudent,
                onOpenStudentRecord = { key ->
                    viewModel.navigateTo(
                        Screen.StudentRecord(
                            studentId = key.studentId,
                            institutionalKey = key,
                            returnTo = Screen.SecretariaPreApplicationDashboard
                        )
                    )
                },
                onOpenStudentRecordById = { studentId ->
                    viewModel.navigateTo(
                        Screen.StudentRecord(
                            studentId = studentId,
                            returnTo = Screen.SecretariaPreApplicationDashboard
                        )
                    )
                },
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
                3 -> DocumentosTab(preApp, highlightDoc = initialDocHighlight)
                4 -> RevisionTab(
                    preApp = preApp,
                    reviewObservations = reviewObservations[preApp.folio].orEmpty()
                )
            }

            // Acciones comunes
            if (preApp.status == PreApplicationStatus.ENVIADA) {
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
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    if (preApp.readinessStatus == ReadinessStatus.CONVERTED || officialStudent != null) {
                        "Modo lectura: el alta institucional ya existe y no admite revalidación desde esta vista."
                    } else {
                        "Modo lectura: la solicitud fue aceptada. Continúa con la validación institucional y el alta oficial."
                    },
                    color = saseMutedColor(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

        }

        // Edit dialog for Secretary
        if (showEditDialog) {
            EditPreApplicationDialog(
                preApp = preApp,
                onDismiss = { showEditDialog = false }
            )
        }

        // CURP correction dialog
        if (showCurpCorrectionDialog) {
            CurpCorrectionDialog(
                currentCurp = preApp.alumnoCurp,
                folio = preApp.folio,
                onDismiss = { showCurpCorrectionDialog = false }
            )
        }
    }
}

@Composable
private fun CurpCorrectionDialog(
    currentCurp: String,
    folio: String,
    onDismiss: () -> Unit
) {
    val toast = LocalToast.current
    var curpEdit by remember(folio) { mutableStateOf(currentCurp) }
    var errorMessage by remember(folio) { mutableStateOf<String?>(null) }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = SaseNavy,
        unfocusedTextColor = SaseNavy,
        focusedBorderColor = SaseNavy,
        unfocusedBorderColor = SaseBorder,
        focusedLabelColor = SaseNavy,
        unfocusedLabelColor = SaseMuted
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SaseBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("curp_correction_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Corregir CURP", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 18.sp)
                Text("Folio: $folio", color = saseMutedColor(), fontSize = 11.sp)

                OutlinedTextField(
                    value = curpEdit,
                    onValueChange = {
                        curpEdit = it
                        errorMessage = null
                    },
                    label = { Text("CURP") },
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { message ->
                        { Text(message) }
                    },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("curp_correction_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("curp_correction_cancel")
                    ) {
                        Text("Cancelar", color = saseMutedColor())
                    }
                    Button(
                        onClick = {
                            when (val result = PreApplicationViewModel.correctPreApplicationCurp(folio, curpEdit)) {
                                is CurpCorrectionResult.Updated -> {
                                    curpEdit = result.normalizedCurp
                                    errorMessage = null
                                    toast("CURP actualizada correctamente")
                                    onDismiss()
                                }
                                is CurpCorrectionResult.InvalidFormat -> {
                                    errorMessage = result.message
                                }
                                is CurpCorrectionResult.Duplicate -> {
                                    errorMessage = result.conflict.institutionalMessage
                                }
                                CurpCorrectionResult.NotFound -> {
                                    errorMessage = "No se encontró la pre-solicitud. Actualiza la vista e inténtalo de nuevo."
                                }
                                CurpCorrectionResult.AlreadyConverted -> {
                                    errorMessage = "La solicitud ya fue convertida; la CURP se gestiona desde el expediente institucional."
                                }
                                CurpCorrectionResult.InstitutionalIdentityLocked -> {
                                    errorMessage = "La CURP de una reinscripción está vinculada al expediente institucional y no puede corregirse aquí."
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("curp_correction_save")
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

@Composable
private fun EditPreApplicationDialog(
    preApp: PreApplication,
    onDismiss: () -> Unit
) {
    val toast = LocalToast.current
    val expectedSnapshot = remember(preApp.folio) { preApp.administrativeDataSnapshot() }
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
                Text("Ajuste interno de Secretaría antes de aceptar para alta oficial. Folio: ${preApp.folio}", color = saseMutedColor(), fontSize = 11.sp)

                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = SaseNavy,
                    unfocusedTextColor = SaseNavy,
                    focusedBorderColor = SaseNavy,
                    unfocusedBorderColor = SaseBorder,
                    focusedLabelColor = SaseNavy,
                    unfocusedLabelColor = SaseMuted
                )
                OutlinedTextField(
                    value = telefonoEdit,
                    onValueChange = { telefonoEdit = it.filter { char -> char.isDigit() }.take(10) },
                    label = { Text("Teléfono") },
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = domicilioEdit,
                    onValueChange = { domicilioEdit = it.uppercase() },
                    label = { Text("Domicilio") },
                    colors = fieldColors,
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
                        Text("Cerrar sin guardar", color = saseMutedColor())
                    }
                    Button(
                        onClick = {
                            val result = PreApplicationViewModel.updatePreApplicationAdministrativeData(
                                UpdatePreApplicationAdministrativeDataRequest(
                                    folio = preApp.folio,
                                    expected = expectedSnapshot,
                                    changes = PreApplicationAdministrativeChanges(
                                        phone = PreApplicationAdministrativeFieldChange.Replace(telefonoEdit),
                                        address = PreApplicationAdministrativeFieldChange.Replace(domicilioEdit)
                                    )
                                )
                            )
                            when (result) {
                                is UpdatePreApplicationAdministrativeDataResult.Updated -> {
                                    val fields = result.changedFields.map { field ->
                                        when (field) {
                                            PreApplicationAdministrativeField.PHONE -> "teléfono"
                                            PreApplicationAdministrativeField.ADDRESS -> "domicilio"
                                        }
                                    }.joinToString(" y ")
                                    toast("Cambios guardados: $fields")
                                    onDismiss()
                                }
                                UpdatePreApplicationAdministrativeDataResult.NoChanges -> {
                                    toast("No hay cambios para guardar")
                                    onDismiss()
                                }
                                is UpdatePreApplicationAdministrativeDataResult.Invalid -> {
                                    val errors = result.errors.map { (field, error) ->
                                        val fieldLabel = when (field) {
                                            PreApplicationAdministrativeField.PHONE -> "Teléfono"
                                            PreApplicationAdministrativeField.ADDRESS -> "Domicilio"
                                        }
                                        val errorLabel = when (error) {
                                            PreApplicationAdministrativeValidationError.REQUIRED -> "es obligatorio"
                                            PreApplicationAdministrativeValidationError.INVALID_FORMAT -> "tiene formato inválido"
                                        }
                                        "$fieldLabel $errorLabel"
                                    }.joinToString(". ")
                                    toast(errors)
                                }
                                UpdatePreApplicationAdministrativeDataResult.NotFound -> {
                                    toast("No se encontró la pre-solicitud")
                                }
                                is UpdatePreApplicationAdministrativeDataResult.Conflict -> {
                                    val message = when (result.reason) {
                                        PreApplicationAdministrativeConflictReason.STALE_DATA ->
                                            "Los datos cambiaron. Cierra y vuelve a abrir para reintentar"
                                        PreApplicationAdministrativeConflictReason.NOT_EDITABLE ->
                                            "La pre-solicitud ya no permite correcciones"
                                        PreApplicationAdministrativeConflictReason.OFFICIAL_ENROLLMENT_EXISTS ->
                                            "No se puede corregir: el alta oficial ya existe"
                                        PreApplicationAdministrativeConflictReason.AMBIGUOUS_FOLIO ->
                                            "No se puede corregir: el folio está duplicado"
                                    }
                                    toast(message)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaseNavy, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Guardar cambios", fontWeight = FontWeight.Bold)
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
        Text("No se registraron autorizados", color = saseMutedColor(), fontSize = 10.sp)
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
    onStartOfficialEnrollment: () -> Unit,
    onNavigateToDocs: () -> Unit = {},
    onOpenExistingStudent: (String) -> Unit = {},
    onCorrectCurp: () -> Unit = {}
) {
    val isReadyByChecklist = pendingItems.isEmpty()
    val isPersistedReady = preApp.readinessStatus == ReadinessStatus.READY
    val isConverted = preApp.readinessStatus == ReadinessStatus.CONVERTED
    val officialStarted = officialStudent != null
    var readinessMessage by remember(preApp.folio, preApp.readinessStatus) { mutableStateOf<String?>(null) }
    val defaultMuted = saseMutedColor()
    var readinessColor by remember(preApp.folio, preApp.readinessStatus) { mutableStateOf(defaultMuted) }
    val readerIsReadyForOfficial = isPersistedReady && pendingItems.isEmpty()
    val officialCompleted = officialStarted && pendingItems.isEmpty()
    val officialWithPending = officialStarted && pendingItems.isNotEmpty() && !isConverted
    val curpConflict = remember(preApp.folio, preApp.alumnoCurp, preApp.tramite) {
        PreApplicationViewModel.resolveCurpConflict(preApp.folio, preApp.alumnoCurp)
    }
    val isCurpBlocked = curpConflict != null
    val headerColor = when {
        isCurpBlocked -> SaseRed
        officialCompleted || isConverted -> SaseGreen
        officialStarted -> SaseNavy
        readerIsReadyForOfficial -> SaseGreen
        pendingItems.isNotEmpty() || preApp.readinessStatus == ReadinessStatus.BLOCKED -> SaseOrange
        else -> saseMutedColor()
    }
    val headerText = when {
        isCurpBlocked -> "CURP ya registrada"
        officialCompleted || isConverted -> "Alta oficial completada"
        officialWithPending -> "Alta oficial iniciada con pendientes"
        pendingItems.isNotEmpty() -> "Con pendientes bloqueantes"
        officialStarted -> "Aceptada"
        readerIsReadyForOfficial -> "Lista para alta oficial"
        preApp.readinessStatus == ReadinessStatus.BLOCKED -> "Pendientes resueltos; falta declarar READY"
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
                        preApp.readinessStatus == ReadinessStatus.BLOCKED && pendingItems.isEmpty() ->
                            "Los requisitos están completos; Secretaría debe declarar READY explícitamente."
                        officialWithPending -> "La alta existe, pero conserva requisitos por cerrar."
                        officialStarted -> "Alta oficial iniciada desde esta pre-solicitud."
                        preApp.readyAt != null -> "Declarada lista: ${preApp.readyAt}"
                        else -> "La matrícula se genera solo desde este folio validado."
                    },
                    color = saseMutedColor(),
                    fontSize = 9.sp
                )
            }
        }

        if (pendingItems.isNotEmpty() && !isConverted) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Pendientes institucionales:", color = SaseOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                pendingItems.forEach { item ->
                    val isDocItem = item.contains("Documentos", ignoreCase = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(SaseOrange)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(item, color = saseTextColor(), fontSize = 10.sp, fontWeight = FontWeight.Medium,
                            modifier = if (isDocItem) Modifier.clip(RoundedCornerShape(4.dp)).clickable(onClick = onNavigateToDocs) else Modifier)
                    }
                }
            }
        }

        if (curpConflict != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("curp_conflict_panel")
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    curpConflict.institutionalMessage,
                    color = SaseRed, fontSize = 11.sp, fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    curpConflict.masterStudentId
                        ?.takeIf { curpConflict.isNavigable }
                        ?.let { studentId ->
                            OutlinedButton(
                                onClick = { onOpenExistingStudent(studentId) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("open_existing_student_button")
                            ) { Text("Abrir expediente existente", fontSize = 10.sp) }
                        }
                    // D5: tras la conversión la identidad es institucional; la CURP
                    // ya no se corrige desde la pre-solicitud.
                    if (!isConverted) {
                        OutlinedButton(
                            onClick = onCorrectCurp,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("correct_curp_button")
                        ) { Text("Corregir CURP", fontSize = 10.sp) }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Acciones de inscripción no disponibles mientras la CURP esté duplicada.",
                    color = saseMutedColor(), fontSize = 8.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        if (!officialStarted && !isPersistedReady && !isCurpBlocked) {
            OutlinedButton(
                onClick = {
                    val result = PreApplicationViewModel.markReadyForOfficialEnrollment(preApp.folio)
                    readinessMessage = result.message
                    readinessColor = result.toUiColor()
                },
                enabled = isReadyByChecklist,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SaseGreen),
                border = BorderStroke(1.dp, if (isReadyByChecklist) SaseGreen.copy(alpha = 0.55f) else saseMutedColor().copy(alpha = 0.22f)),
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
        if (!isCurpBlocked && (isPersistedReady || officialStarted || isConverted)) {
            Button(
                onClick = onStartOfficialEnrollment,
                enabled = (isPersistedReady && isReadyByChecklist) || officialStarted || isConverted,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (officialStarted || isConverted) SaseBlue else SaseGreen,
                    disabledContainerColor = saseMutedColor().copy(alpha = 0.18f),
                    disabledContentColor = saseMutedColor()
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("official_enrollment_action")
            ) {
                Icon(if (isPersistedReady || officialStarted || isConverted) Icons.Default.AssignmentTurnedIn else Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    when {
                        isConverted -> "Abrir seguimiento institucional"
                        officialStarted -> "Ver seguimiento de alta"
                        else -> "Dar de alta oficial"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
            if (!officialStarted && !isReadyByChecklist) {
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
    onOpenStudentRecord: (InstitutionalStudentRecordKey) -> Unit,
    onOpenStudentRecordById: (String) -> Unit,
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
    val enrollmentFlowMode by PreApplicationViewModel.enrollmentFlowMode.collectAsState()
    val actionPresentation = enrollmentActionPresentation(enrollmentFlowMode)
    var selectedGroup by remember(preApp.folio) { mutableStateOf(officialStudent?.grupoAsignado ?: officialStudent?.grupoSugerido ?: suggestedGroup ?: groupOptions.firstOrNull().orEmpty()) }
    var groupConfirmed by remember(preApp.folio) { mutableStateOf(false) }
    var resultMessage by remember(preApp.folio) { mutableStateOf<String?>(null) }
    var resultColor by remember(preApp.folio) { mutableStateOf(SaseGreen) }
    var institutionalResult by remember(preApp.folio) {
        mutableStateOf<InstitutionalAnnualEnrollmentResult?>(null)
    }
    val annualEnrollments by MockSaseData.annualEnrollments.collectAsState()
    val masterStudents by MockSaseData.students.collectAsState()
    val curpDuplicate = remember(preApp.folio, preApp.alumnoCurp, preApp.tramite) {
        PreApplicationViewModel.curpDuplicateInfo(preApp.folio, preApp.alumnoCurp)
    }
    val panelPresentation = institutionalEnrollmentPanelPresentation(
        readinessStatus = preApp.readinessStatus,
        result = institutionalResult
    )
    val recordAction = institutionalResult?.let(::institutionalEnrollmentRecordAction)
        ?: institutionalEnrollmentRecordAction(preApp.folio, annualEnrollments)
    val legacyStudentId = if (panelPresentation.isCompleted && recordAction == null) {
        officialStudent?.let { official ->
            masterStudents.singleOrNull { it.curp.equals(official.curp, ignoreCase = true) }?.id
        }
    } else {
        null
    }

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
                Text(
                    panelPresentation.title,
                    color = if (panelPresentation.isCompleted) SaseGreen else SaseNavy,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text("Folio precargado: ${preApp.folio}", color = saseMutedColor(), fontSize = 10.sp)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = saseMutedColor())
            }
        }

        if (panelPresentation.showInitialGuidance) {
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
        }

        Spacer(modifier = Modifier.height(10.dp))
        DetailRow("Nombre", preApp.alumnoNombreCompleto)
        DetailRow("Grado ingreso", "${grade}°")
        DetailRow(PreApplicationViewModel.promedioLabelForGrade(grade), preApp.promedioGradoAnterior?.toString() ?: "Pendiente")
        DetailRow("Persona que tramita", preApp.personaTramite.nombreCompleto.ifBlank { "Pendiente" })
        DetailRow(
            "Matrícula",
            recordAction?.key?.enrollmentId ?: visibleOfficialEnrollment(officialStudent)
        )
        DetailRow("Ciclo", recordAction?.key?.schoolYear ?: preApp.cicloEscolar)
        DetailRow("Folio", recordAction?.key?.sourcePreApplicationFolio ?: preApp.folio)

        if (panelPresentation.isCompleted) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Registro institucional final. La información se muestra en modo lectura.",
                color = SaseGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (panelPresentation.showProcessAction && actionPresentation.showLegacyGroupControls && grade in 1..3) {
            SectionHeader("Asignación inicial de grupo")
            DetailRow("Grupo sugerido por balance", suggestedGroup ?: "Sin sugerencia disponible")
            Text("Criterios: sexo, edad y ${PreApplicationViewModel.promedioLabelForGrade(grade).lowercase()}.", color = saseMutedColor(), fontSize = 9.sp)
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
                            labelColor = saseMutedColor()
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
                    color = saseTextColor(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (panelPresentation.showProcessAction && actionPresentation.showLegacyStartAction && officialStudent == null && curpDuplicate == null) {
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
        } else if (panelPresentation.showProcessAction && actionPresentation.showLegacyConfirmationAction && officialStudent != null) {
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
                    color = saseMutedColor(),
                    fontSize = 9.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        val isProcessingV2 by PreApplicationViewModel.isProcessingAnnualEnrollmentV2.collectAsState()
        if (panelPresentation.showProcessAction && actionPresentation.showAnnualV2Action && curpDuplicate == null) {
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
                        institutionalResult = v2Result
                        resultMessage = institutionalEnrollmentMessage(v2Result)
                        resultColor = when (v2Result) {
                            is InstitutionalAnnualEnrollmentResult.Completed,
                            is InstitutionalAnnualEnrollmentResult.AlreadyCompleted -> SaseGreen
                            is InstitutionalAnnualEnrollmentResult.NeedsDecision -> SaseBlue
                            is InstitutionalAnnualEnrollmentResult.GuardRejected,
                            is InstitutionalAnnualEnrollmentResult.AnnualConflict,
                            is InstitutionalAnnualEnrollmentResult.SynchronizationIncomplete -> SaseOrange
                        }
                    } finally {
                        PreApplicationViewModel.setProcessingAnnualEnrollmentV2(false)
                    }
                }
            },
            enabled = !isProcessingV2 && preApp.personaTramite.nombreCompleto.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isProcessingV2) saseMutedColor().copy(alpha = 0.15f) else SaseBlue.copy(alpha = 0.15f),
                contentColor = if (isProcessingV2) saseMutedColor() else SaseBlue
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
                if (isProcessingV2) "Procesando..." else actionPresentation.annualV2ActionLabel,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            }
        }

        if (resultMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(resultMessage ?: "", color = resultColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            if (recordAction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onOpenStudentRecord(recordAction.key) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SaseNavy,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(recordAction.label, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
        if (resultMessage == null && panelPresentation.isCompleted && (recordAction != null || legacyStudentId != null)) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (recordAction != null) {
                        onOpenStudentRecord(recordAction.key)
                    } else if (legacyStudentId != null) {
                        onOpenStudentRecordById(legacyStudentId)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SaseNavy, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Abrir expediente", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
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
                color = saseTextColor(),
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
                Text(doc.nombre, color = saseTextColor(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                    .background(if (hasPhoto) SaseGreen.copy(alpha = 0.12f) else saseMutedColor().copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (hasPhoto) {
                    Icon(Icons.Default.Face, contentDescription = null, tint = SaseGreen, modifier = Modifier.size(28.dp))
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, tint = saseMutedColor().copy(alpha = 0.5f), modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, color = saseMutedColor(), fontSize = 8.sp, fontWeight = FontWeight.Medium, maxLines = 1)
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
                    .background(if (hasPhoto) saseMutedColor().copy(alpha = 0.1f) else SaseCyan.copy(alpha = 0.12f))
                    .clickable { onCapture() }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    if (hasPhoto) "Restaurar" else "Simular captura",
                    color = if (hasPhoto) saseMutedColor() else SaseCyan,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Tab 4: Documentos ──────────────────────────────────────────────────────

@Composable
private fun DocumentosTab(preApp: PreApplication, highlightDoc: String? = null) {
    val toast = LocalToast.current
    var obsDialogDoc by remember(preApp.folio) { mutableStateOf<String?>(null) }
    var obsDraft by remember { mutableStateOf("") }

    // Observation dialog
    obsDialogDoc?.let { docNombre ->
        Dialog(onDismissRequest = { obsDialogDoc = null }) {
            Card(
                modifier = Modifier.width(300.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Observación para $docNombre", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = obsDraft,
                        onValueChange = { obsDraft = it.take(220) },
                        placeholder = { Text("Escribe una observación...", color = SaseMuted) },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { obsDialogDoc = null }) {
                            Text("Cancelar", color = SaseMuted)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                PreApplicationViewModel.setDocumentObservacion(preApp.folio, docNombre, obsDraft)
                                obsDialogDoc = null
                            },
                            enabled = obsDraft.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = SaseCyan)
                        ) {
                            Text("Guardar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    SectionHeader("Bloque H — Documentos Declarados")
    Spacer(modifier = Modifier.height(4.dp))

    val docsTotal = preApp.documentosDeclarados.size
    val docsResueltos = preApp.documentosDeclarados.count { it.noAplica || it.validado }
    val docsPendientes = docsTotal - docsResueltos
    val firstPendiente = preApp.documentosDeclarados.indexOfFirst { !it.noAplica && !it.validado }

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(SaseBlue.copy(alpha = 0.05f)).padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text("$docsResueltos de $docsTotal documentos resueltos ($docsPendientes pendientes)", color = SaseBlue, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
    Spacer(modifier = Modifier.height(6.dp))

    preApp.documentosDeclarados.forEachIndexed { index, doc ->
        val noAplica = doc.noAplica
        val declarado = doc.declarado && !noAplica
        val cotejado = doc.cotejadoSecretaria
        val validado = doc.validado
        val rechazado = doc.rechazado
        val isPendiente = !noAplica && !validado
        val isHighlighted = highlightDoc != null && doc.nombre == highlightDoc
        val isFirstPendiente = index == firstPendiente && isPendiente

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when {
                        noAplica -> saseMutedColor().copy(alpha = 0.10f)
                        validado -> SaseGreen.copy(alpha = 0.08f)
                        rechazado -> SaseRed.copy(alpha = 0.08f)
                        cotejado -> SaseBlue.copy(alpha = 0.06f)
                        declarado -> Color.White.copy(alpha = 0.5f)
                        else -> SaseRed.copy(alpha = 0.05f)
                    }
                )
                .then(
                    if (isHighlighted || isFirstPendiente) Modifier.border(1.5.dp, SaseOrange.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    else Modifier
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Icon(
                when {
                    noAplica -> Icons.Default.Block
                    validado -> Icons.Default.VerifiedUser
                    rechazado -> Icons.Default.Cancel
                    declarado -> Icons.Default.CheckCircle
                    else -> Icons.Default.Warning
                },
                contentDescription = null,
                tint = when {
                    noAplica -> saseMutedColor()
                    validado -> SaseGreen
                    rechazado -> SaseRed
                    declarado -> SaseGreen
                    else -> SaseRed.copy(alpha = 0.7f)
                },
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(doc.nombre, color = if (declarado || noAplica || validado) saseTextColor() else SaseRed, fontSize = 10.sp, fontWeight = if ((declarado || noAplica || validado) && !isPendiente) FontWeight.Normal else FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    when {
                        noAplica -> Text("No aplica", color = saseMutedColor(), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        validado -> Text("Validado", color = SaseGreen, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        rechazado -> Text("Rechazado", color = SaseRed, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        cotejado -> Text("Cotejado", color = SaseBlue, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        declarado -> Text("Declarado", color = SaseGreen, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        else -> Text("No declarado", color = SaseRed, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (doc.observacion.isNotBlank()) {
                    Text("Obs: ${doc.observacion}", color = SaseOrange, fontSize = 7.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            // Actions row
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                if (noAplica || validado) {
                    // No actions needed
                } else if (rechazado) {
                    ActionChip("Cotejar", SaseBlue) { PreApplicationViewModel.toggleDocumentCotejado(preApp.folio, doc.nombre) }
                } else if (cotejado) {
                    ActionChip("Validar", SaseGreen) { PreApplicationViewModel.markDocumentValidado(preApp.folio, doc.nombre) }
                    ActionChip("Rechazar", SaseRed) { PreApplicationViewModel.markDocumentRechazado(preApp.folio, doc.nombre) }
                } else if (declarado) {
                    ActionChip("Cotejar", SaseBlue) { PreApplicationViewModel.toggleDocumentCotejado(preApp.folio, doc.nombre) }
                    ActionChip("No aplica", SaseOrange) { PreApplicationViewModel.markDocumentNoAplica(preApp.folio, doc.nombre) }
                } else {
                    ActionChip("No aplica", SaseOrange) { PreApplicationViewModel.markDocumentNoAplica(preApp.folio, doc.nombre) }
                }
            }
        }
        if (doc.observacion.isBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 28.dp, end = 8.dp).clip(RoundedCornerShape(4.dp)).clickable {
                    obsDialogDoc = doc.nombre; obsDraft = ""
                }.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text("+ Obs", color = SaseCyan, fontSize = 7.sp, fontWeight = FontWeight.Bold)
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

@Composable
private fun ActionChip(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(label, color = color, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Tab 5: Revisión ────────────────────────────────────────────────────────

@Composable
private fun RevisionTab(
    preApp: PreApplication,
    reviewObservations: List<PreApplicationViewModel.Companion.SecretariaReviewObservation>
) {
    val toast = LocalToast.current
    val categories = listOf("Documentos", "Fotos", "Contacto responsable")
    var selectedCategory by remember(preApp.folio) { mutableStateOf(categories.first()) }
    var observationDraft by remember(preApp.folio) { mutableStateOf(preApp.observacionesSecretaria) }

    SectionHeader("Observaciones administrativas")
    Text(
        "Secretaría registra notas, solicitudes de documento o contactos con responsable.",
        color = saseMutedColor(),
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
                    labelColor = saseMutedColor()
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
        placeholder = { Text("Captura una observación administrativa...", color = saseMutedColor()) },
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
        Text("Sin observaciones registradas para este folio.", color = saseMutedColor(), fontSize = 10.sp)
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
                        Text(item.createdAt, color = saseMutedColor(), fontSize = 8.sp)
                    }
                    Text(item.note, color = saseTextColor(), fontSize = 10.sp)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

}

// ── Mobile ─────────────────────────────────────────────────────────────────

@Composable
private fun MobilePreApplicationDetail(
    preApp: PreApplication,
    viewModel: LabViewModel,
    onBack: () -> Unit,
    onApprove: (String) -> Unit,
    onProvisionalCreated: (String) -> Unit,
    docTabRequestedFolio: String? = null,
    onDocTabConsumed: () -> Unit = {},
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
            onProvisionalCreated = onProvisionalCreated,
            docTabRequestedFolio = docTabRequestedFolio,
            onDocTabConsumed = onDocTabConsumed,
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
        Text("$label: ", color = saseMutedColor(), fontSize = 10.sp)
        Text(value, color = saseTextColor(), fontWeight = FontWeight.SemiBold, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        Text(label, color = saseTextColor(), fontSize = 10.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            if (value) "Aceptado" else "Pendiente",
            color = if (value) SaseGreen else SaseRed,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
