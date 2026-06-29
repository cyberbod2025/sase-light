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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    onBack = { selectedFolio = null },
                    onApprove = { folio ->
                        PreApplicationViewModel.approvePreApplication(folio)
                        toast("Pre-solicitud $folio aceptada — pendiente alta oficial")
                    },
                    onMarkCorrection = { folio ->
                        PreApplicationViewModel.markForCorrection(folio)
                        toast("Pre-solicitud $folio marcada para corrección — se notificará a la familia")
                    }
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

                    // Search + toggle
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
                        // Mobile: just list
                        PreApplicationList(
                            apps = visibleApps,
                            selectedFolio = selectedFolio,
                            onSelect = { selectedFolio = it.folio }
                        )
                    } else {
                        // Desktop: split pane
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
                            PreApplicationDetail(
                                preApp = selectedApp,
                                onApprove = { folio ->
                                    PreApplicationViewModel.approvePreApplication(folio)
                                    toast("Pre-solicitud $folio aceptada — pendiente alta oficial")
                                },
                                onMarkCorrection = { folio ->
                                    PreApplicationViewModel.markForCorrection(folio)
                                    toast("Pre-solicitud $folio marcada para corrección — se notificará a la familia")
                                },
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

@Composable
private fun PreApplicationDetail(
    preApp: PreApplication?,
    onApprove: (String) -> Unit,
    onMarkCorrection: (String) -> Unit,
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

    GlassCard(modifier = modifier) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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

            Spacer(modifier = Modifier.height(16.dp))

            // Datos del Alumno
            SectionHeader("Datos del Alumno")
            DetailField("Nombre completo", preApp.alumnoNombreCompleto)
            DetailField("CURP", preApp.alumnoCurp)
            DetailField("Fecha de nacimiento", preApp.alumnoFechaNacimiento)
            DetailField("Sexo", preApp.alumnoSexo)
            DetailField("Nacionalidad", preApp.alumnoNacionalidad)
            DetailField("Entidad de nacimiento", preApp.alumnoEntidadNacimiento)
            DetailField("Domicilio", preApp.alumnoDomicilio)
            DetailField("Teléfono", preApp.alumnoTelefonoCasa)

            Spacer(modifier = Modifier.height(12.dp))
            SectionHeader("Trámite")
            DetailField("Tipo", preApp.tramite)
            DetailField("Ciclo escolar", preApp.cicloEscolar)
            DetailField("Grado solicitado", "${preApp.gradoSolicitado}°")
            DetailField("Escuela de procedencia", preApp.escuelaProcedencia)

            // Responsable principal
            if (preApp.responsables.isNotEmpty()) {
                val r = preApp.responsables.first()
                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader("Responsable Principal")
                DetailField("Nombre", r.nombreCompleto)
                DetailField("Parentesco", r.parentesco)
                DetailField("Teléfono", r.telefono)
                if (r.correo != null) DetailField("Correo", r.correo)
            }

            // Documentos
            if (preApp.documentosDeclarados.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader("Documentos Declarados")
                preApp.documentosDeclarados.forEach { doc ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        if (doc.declarado) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SaseGreen, modifier = Modifier.size(14.dp))
                        } else {
                            Box(modifier = Modifier.size(14.dp).border(1.5.dp, SaseBorder.copy(alpha = 0.5f), CircleShape))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(doc.nombre, color = SaseText, fontSize = 11.sp)
                    }
                }
            }

            // Consentimientos
            Spacer(modifier = Modifier.height(12.dp))
            SectionHeader("Consentimientos")
            val c = preApp.consentimientos
            ConsentRow("Aviso de privacidad", c.avisoPrivacidad)
            ConsentRow("Uso de datos", c.usoDatosExpediente)
            ConsentRow("Foto del alumno", c.fotoAlumno)
            ConsentRow("Foto credencial", c.fotoCredencial)
            ConsentRow("Foto autorizados", c.fotoAutorizados)
            ConsentRow("Comunicación", c.comunicacionWhatsapp)
            ConsentRow("Reglamento interno", c.reglamentoInterno)
            ConsentRow("Marco de convivencia", c.marcoConvivencia)
            ConsentRow("Corresponsabilidad", c.corresponsabilidadFamiliar)

            // Acciones
            if (preApp.status == PreApplicationStatus.ENVIADA || preApp.status == PreApplicationStatus.PENDIENTE_CORRECCION) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = SaseBorder)
                Spacer(modifier = Modifier.height(16.dp))
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
        }
    }
}

@Composable
private fun MobilePreApplicationDetail(
    preApp: PreApplication,
    onBack: () -> Unit,
    onApprove: (String) -> Unit,
    onMarkCorrection: (String) -> Unit
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
        PreApplicationDetail(
            preApp = preApp,
            onApprove = onApprove,
            onMarkCorrection = onMarkCorrection,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SaseNavy)
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun DetailField(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = SaseMuted, fontSize = 10.sp)
        Text(value, color = SaseText, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ConsentRow(label: String, value: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.dp)) {
        Icon(
            if (value) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (value) SaseGreen else SaseRed.copy(alpha = 0.6f),
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = SaseText, fontSize = 10.sp)
    }
}
