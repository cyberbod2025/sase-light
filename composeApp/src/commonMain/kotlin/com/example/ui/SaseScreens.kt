package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.util.LocalToast
import com.example.ui.enrollment.SmartEnrollmentTable
import com.example.ui.enrollment.digital.SecretariaEnrollmentDashboard
import com.example.data.presolicitud.*
import com.example.ui.student.StudentRecordScreen
import com.example.ui.CredentialPreviewScreen
import com.example.ui.StudentCredentialDashboardScreen
import com.example.data.StudentAddResult
import com.example.viewmodel.LabViewModel
import com.example.viewmodel.Screen
import com.example.viewmodel.AppRole
import com.example.viewmodel.PreApplicationViewModel
import com.example.ui.presolicitud.SecretariaPreApplicationDashboardScreen
import com.example.ui.presolicitud.SectionHeader
import com.example.ui.presolicitud.DetailRow
import kotlinx.coroutines.launch

private fun fastTrackGradeFromGroup(group: String): Int =
    group.trim().firstOrNull()?.digitToIntOrNull() ?: 1

// Colors matching palette
val SaseBg = Color(0xFFF6FAFC)
val SaseBgSoft = Color(0xFFEEF7F7)
val SaseNavy = Color(0xFF062B3A)
val SaseNavy2 = Color(0xFF021C28)
val SaseGreen = Color(0xFF10B981)
val SaseGreenDark = Color(0xFF047857)
val SaseBlue = Color(0xFF2563EB)
val SaseCyan = Color(0xFF0891B2)
val SaseViolet = Color(0xFF7C3AED)
val SaseOrange = Color(0xFFF97316)
val SaseRed = Color(0xFFDC2626)
val SaseText = Color(0xFF0F172A)
val SaseMuted = Color(0xFF64748B)
val SaseBorder = Color(0x3894A3B8) // Transparent border

// Background Brush with macOS light circles drawBehind effect
val SaseBackgroundBrush = Brush.linearGradient(
    colors = listOf(Color(0xFFF8FCFF), Color(0xFFEEF7F7))
)

@Composable
fun SaseBackgroundModifier(): Modifier {
    return Modifier
        .fillMaxSize()
        .background(SaseBackgroundBrush)
        .drawBehind {
            // Radial suttle top-left green glow
            drawCircle(
                color = SaseGreen.copy(alpha = 0.08f),
                radius = size.width * 0.4f,
                center = Offset(0f, 0f)
            )
            // Radial suttle top-right blue glow
            drawCircle(
                color = SaseBlue.copy(alpha = 0.07f),
                radius = size.width * 0.4f,
                center = Offset(size.width, 0f)
            )
        }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderWidth: Dp = 1.dp,
    borderColor: Color = Color.White.copy(alpha = 0.55f),
    containerColor: Color = Color.White.copy(alpha = 0.65f),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.testTag("glass_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        border = BorderStroke(borderWidth, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            content()
        }
    }
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = Color.White.copy(alpha = 0.55f),
    containerColor: Color = Color.White.copy(alpha = 0.5f),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    val cardModifier = if (onClick != null) {
        modifier
            .clip(shape)
            .clickable(onClick = onClick)
    } else {
        modifier
    }
    
    Box(
        modifier = cardModifier
            .testTag("liquid_glass_card")
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        containerColor,
                        containerColor.copy(alpha = 0.3f)
                    )
                ),
                shape = shape
            )
            .border(
                border = BorderStroke(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.7f),
                            Color.White.copy(alpha = 0.15f),
                            SaseBlue.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.55f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                ),
                shape = shape
            )
            .drawBehind {
                // Diagonal glossy highlight reflecting light
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(40f, 40f),
                        radius = size.width * 0.4f
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx(), 24.dp.toPx())
                )
            }
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            content()
        }
    }
}

@Composable
fun ReturnToDashboardButton(
    onClick: () -> Unit,
    label: String = "Volver al inicio"
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SaseBorder),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = SaseNavy)
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
    }
}



// Side bar component
@Composable
fun SaseSidebar(
    activeItem: String,
    modifier: Modifier = Modifier,
    onItemClick: (String) -> Unit = {}
) {
    val items = listOf(
        "Inicio" to Icons.Default.Home,
        "Inscripciones" to Icons.Default.School,
        "Portal Familia" to Icons.Default.Groups,
        "Pre-Solicitudes" to Icons.AutoMirrored.Filled.Assignment,
        "Altas Oficiales" to Icons.Default.AssignmentTurnedIn,
        "Credenciales" to Icons.Default.Badge
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SaseNavy, SaseNavy2)
                )
            )
            .padding(16.dp)
    ) {
        // App Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "Logo SASE",
                    tint = SaseGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "SASE-310",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
                Text(
                    text = "Sistema Escolar",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Menu items
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(items) { (name, icon) ->
                val isActive = name == activeItem
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isActive) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { onItemClick(name) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = name,
                        tint = if (isActive) SaseGreen else Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = name,
                        color = if (isActive) Color.White else Color.White.copy(alpha = 0.8f),
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Bottom profile
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = "Profile Pic", tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Secretaría",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SaseGreen))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("En línea", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "SASE-310 v3.10.0",
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

// Global student search component
@Composable
fun GlobalStudentSearch(
    students: List<Student>,
    onStudentClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val filtered = remember(searchQuery, students) {
        if (searchQuery.isBlank()) emptyList()
        else {
            students.filter { student ->
                student.fullName.contains(searchQuery, ignoreCase = true) ||
                        student.enrollmentId.contains(searchQuery, ignoreCase = true) ||
                        student.curp.contains(searchQuery, ignoreCase = true) ||
                        student.group.contains(searchQuery, ignoreCase = true) ||
                        student.tutorName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                isDropdownExpanded = it.isNotBlank()
            },
            placeholder = { Text("Buscar alumno por nombre, CURP, matrícula, grupo o tutor...", color = SaseMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = SaseMuted) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = ""; isDropdownExpanded = false }) {
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
            modifier = Modifier
                .fillMaxWidth()
                .testTag("global_search_input")
        )

        // Dropdown of search results
        if (isDropdownExpanded && filtered.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SaseBorder),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 62.dp)
                    .heightIn(max = 240.dp)
                    .align(Alignment.TopStart)
            ) {
                LazyColumn(modifier = Modifier.padding(6.dp)) {
                    items(filtered) { student ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    searchQuery = ""
                                    isDropdownExpanded = false
                                    onStudentClick(student.id)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SaseBlue.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = SaseBlue, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(student.fullName, fontWeight = FontWeight.Bold, color = SaseText, fontSize = 13.sp)
                                Text("${student.group} · Matrícula: ${student.enrollmentId} · CURP: ${student.curp}", color = SaseMuted, fontSize = 11.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SaseGreen.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Ver expediente", color = SaseGreenDark, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        } else if (isDropdownExpanded && searchQuery.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SaseBorder),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 62.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    "No encontramos alumnos con ese criterio.",
                    color = SaseMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// KPI widget cards
@Composable
fun KpiCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.72f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .height(115.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = SaseMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
                }
            }
            Column {
                Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SaseText)
            }
        }
    }
}

// Side details lists

@Composable
fun ActivityAuditFeed(
    audits: List<SaseAudit>,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Actividad reciente / auditoría", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SaseNavy)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SaseBgSoft)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("Ver todos", color = SaseNavy, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 240.dp)
        ) {
            items(audits.take(5)) { audit ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.4f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    val color = when (audit.userRole) {
                        "Dirección" -> SaseViolet
                        "Sistema" -> SaseRed
                        else -> SaseGreen
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (audit.userRole) {
                                "Dirección" -> Icons.Default.Security
                                "Sistema" -> Icons.Default.Warning
                                "Prof. M. Aguilar" -> Icons.Default.BorderColor
                                else -> Icons.Default.CheckCircle
                            },
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(audit.action, fontWeight = FontWeight.Bold, color = SaseText, fontSize = 11.sp)
                        Text(audit.detail, color = SaseMuted, fontSize = 10.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(audit.timestamp, color = SaseMuted, fontSize = 9.sp)
                        Text(audit.userRole, color = color, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

// THE DASHBOARD SCREEN
@Composable
fun SecretaryDashboardScreen(
    viewModel: LabViewModel
) {
    val toast = LocalToast.current
    val students by viewModel.saseStudents.collectAsState()

    var showNewStudentDialog by remember { mutableStateOf(false) }
    var newStudentName by remember { mutableStateOf("") }
    var newStudentGroup by remember { mutableStateOf("1°A") }
    var newStudentCurp by remember { mutableStateOf("") }
    var newStudentTutor by remember { mutableStateOf("") }

    BoxWithConstraints(modifier = SaseBackgroundModifier()) {
        val isMobile = maxWidth < 850.dp
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        val dashboardContent = @Composable {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(if (isMobile) 16.dp else 24.dp)
            ) {
                // TopBar with Title and user icon
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
                            Text("Secretaría", fontWeight = FontWeight.ExtraBold, fontSize = if (isMobile) 20.sp else 24.sp, color = SaseNavy)
                            Text("Gestión institucional y matrícula escolar", fontSize = if (isMobile) 11.sp else 12.sp, color = SaseMuted)
                        }
                    }

                    // Avatar
                    Box(
                        modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(SaseNavy.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Secretaria avatar", tint = SaseNavy, modifier = Modifier.size(22.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar
                GlobalStudentSearch(
                    students = students,
                    onStudentClick = { id -> viewModel.navigateTo(Screen.StudentRecord(id)) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Actions row (max 3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showNewStudentDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = SaseNavy),
                        border = BorderStroke(1.dp, SaseBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Nuevo expediente", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                    }
                    Button(
                        onClick = { viewModel.navigateTo(Screen.EnrollmentDashboard) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = SaseNavy),
                        border = BorderStroke(1.dp, SaseBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Abrir Inscripciones", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                    }
                    Button(
                        onClick = { viewModel.navigateTo(Screen.PreApplicationFamilyPortal) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = SaseNavy),
                        border = BorderStroke(1.dp, SaseBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Portal Familia", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                SimpleDashboardSummary(students = students)

                Spacer(modifier = Modifier.height(16.dp))

                SmartEnrollmentTable(
                    students = students,
                    onStudentClick = { id -> viewModel.navigateTo(Screen.StudentRecord(id)) },
                    onRegisterObsClick = { student ->
                        viewModel.navigateTo(Screen.StudentRecord(student.id))
                    }
                )
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
                            activeItem = "Inicio",
                            modifier = Modifier.fillMaxHeight(),
                    onItemClick = { item ->
                                when (item) {
                                    "Inicio" -> viewModel.navigateTo(Screen.SecretaryDashboard)
                                    "Inscripciones" -> viewModel.navigateTo(Screen.EnrollmentDashboard)
                                    "Portal Familia" -> viewModel.navigateTo(Screen.PreApplicationFamilyPortal)
                                    "Pre-Solicitudes" -> viewModel.navigateTo(Screen.SecretariaPreApplicationDashboard)
                                    "Altas Oficiales" -> viewModel.navigateTo(Screen.OfficialEnrollmentDashboard)
                                    "Credenciales" -> viewModel.navigateTo(Screen.StudentCredentialDashboard)
                                }
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                }
            ) {
                dashboardContent()
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                SaseSidebar(
                    activeItem = "Inicio",
                    modifier = Modifier.width(260.dp),
                    onItemClick = { item ->
                        when (item) {
                            "Inicio" -> viewModel.navigateTo(Screen.SecretaryDashboard)
                            "Inscripciones" -> viewModel.navigateTo(Screen.EnrollmentDashboard)
                            "Portal Familia" -> viewModel.navigateTo(Screen.PreApplicationFamilyPortal)
                            "Pre-Solicitudes" -> viewModel.navigateTo(Screen.SecretariaPreApplicationDashboard)
                            "Altas Oficiales" -> viewModel.navigateTo(Screen.OfficialEnrollmentDashboard)
                            "Credenciales" -> viewModel.navigateTo(Screen.StudentCredentialDashboard)
                        }
                    }
                )
                Box(modifier = Modifier.weight(1f)) {
                    dashboardContent()
                }
            }
        }
    }

    // New student fast form registration
    if (showNewStudentDialog) {
        Dialog(onDismissRequest = { showNewStudentDialog = false }) {
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
                    Text("Alta Rápida de Expediente", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 18.sp)
                    Text("Flujo no institucional. Úselo solo para captura excepcional y sin duplicar identidad.", color = SaseOrange, fontSize = 11.sp)

                    OutlinedTextField(
                        value = newStudentName,
                        onValueChange = { newStudentName = it },
                        label = { Text("Nombre completo del Alumno") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newStudentCurp,
                        onValueChange = { newStudentCurp = it },
                        label = { Text("CURP") },
                        placeholder = { Text("LOHM100512...") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newStudentTutor,
                        onValueChange = { newStudentTutor = it },
                        label = { Text("Nombre del Tutor de contacto") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Group selector
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Grupo: ", fontWeight = FontWeight.Bold, color = SaseText, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        listOf("1°A", "1°B", "2°A", "2°B", "3°C").forEach { gp ->
                            val selected = gp == newStudentGroup
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) SaseBlue else SaseBgSoft)
                                    .clickable { newStudentGroup = gp }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(gp, color = if (selected) Color.White else SaseText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { showNewStudentDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar", color = SaseMuted)
                        }
                            Button(
                                onClick = {
                                    if (newStudentName.isNotBlank() && newStudentCurp.isNotBlank()) {
                                        val newId = (students.size + 1).toString()
                                        val generatedEnrollmentId = OfficialStudent.generateMatricula(
                                            newStudentCurp.uppercase(),
                                            fastTrackGradeFromGroup(newStudentGroup)
                                        )
                                        val std = Student(
                                        id = newId,
                                        fullName = newStudentName,
                                        group = newStudentGroup,
                                        enrollmentId = generatedEnrollmentId ?: "",
                                        curp = newStudentCurp.uppercase(),
                                        tutorName = newStudentTutor,
                                        tutorRelation = "Tutor",
                                            status = "Nuevo ingreso",
                                            riskLevel = "Bajo",
                                            documentationStatus = "Completa"
                                        )
                                        when (val addResult = viewModel.addStudent(std)) {
                                            is StudentAddResult.Added -> {
                                                viewModel.logSaseAudit("Expediente creado", "Secretaría", addResult.student.fullName)
                                                showNewStudentDialog = false
                                                newStudentName = ""
                                                newStudentCurp = ""
                                                newStudentTutor = ""
                                                toast("Expediente rápido registrado con guardrails de unicidad.")
                                            }
                                            is StudentAddResult.DuplicateCurp -> {
                                                toast("Ya existe un alumno con esta CURP.")
                                            }
                                            is StudentAddResult.DuplicateEnrollmentId -> {
                                                toast("Ya existe un alumno con esta matrícula.")
                                            }
                                            is StudentAddResult.InvalidData -> {
                                                toast(addResult.message)
                                            }
                                        }
                                    } else {
                                        toast("Favor de llenar nombre y CURP")
                                    }
                                },
                            colors = ButtonDefaults.buttonColors(containerColor = SaseNavy),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Guardar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleDashboardSummary(students: List<Student>) {
    val pendingDocs = students.count { student ->
        student.documentationStatus != "Completa" || student.documents.any { it.status == "Pendiente" }
    }
    val risk = students.count { it.riskLevel == "Alto" || it.riskLevel == "Medio" }
    val highAttendance = students.count { it.attendancePercent >= 90 }
    val active = students.count { it.status == "Activo" || it.status == "Nuevo ingreso" }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("Resumen rápido", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(12.dp))
        BoxWithConstraints {
            val compact = maxWidth < 680.dp
            val items = listOf(
                Triple("Alumnos", students.size.toString(), SaseBlue),
                Triple("Activos", active.toString(), SaseGreen),
                Triple("Docs pendientes", pendingDocs.toString(), SaseOrange),
                Triple("Riesgo", risk.toString(), SaseRed),
                Triple("Asistencia 90%+", highAttendance.toString(), SaseCyan)
            )

            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { item -> SimpleDashboardMetric(item, Modifier.fillMaxWidth()) }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items.forEach { item -> SimpleDashboardMetric(item, Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun SimpleDashboardMetric(item: Triple<String, String, Color>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(item.third.copy(alpha = 0.09f))
            .border(1.dp, item.third.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Text(item.second, color = item.third, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        Text(item.first, color = SaseMuted, fontSize = 11.sp, maxLines = 1)
    }
}
@Composable
fun PillStat(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = SaseMuted, fontSize = 11.sp)
        Text(value, color = SaseText, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
    HorizontalDivider(color = SaseBorder.copy(alpha = 0.05f))
}

@Composable
fun TutorItem(
    name: String,
    relation: String,
    phone: String,
    email: String,
    onCall: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .border(1.dp, SaseBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(name, fontWeight = FontWeight.Bold, color = SaseText, fontSize = 12.sp)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(SaseNavy.copy(alpha = 0.1f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(relation, color = SaseNavy, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Tel: $phone", color = SaseMuted, fontSize = 11.sp)
            Text("Email: $email", color = SaseMuted, fontSize = 11.sp)
        }

        Button(
            onClick = onCall,
            colors = ButtonDefaults.buttonColors(containerColor = SaseGreen),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(28.dp)
        ) {
            Icon(Icons.Default.Phone, contentDescription = "Llamar", tint = Color.White, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Llamar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EnrollmentDashboardScreen(
    viewModel: LabViewModel
) {
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

        val enrollmentContent = @Composable {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(if (isMobile) 16.dp else 24.dp)
            ) {
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
                            Text(
                                text = "Inscripción Digital",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = if (isMobile) 20.sp else 24.sp,
                                color = SaseNavy
                            )
                            Text(
                                text = "Expediente maestro y validación documental",
                                fontSize = if (isMobile) 11.sp else 12.sp,
                                color = SaseMuted
                            )
                        }
                    }
                    ReturnToDashboardButton(
                        onClick = { viewModel.navigateTo(Screen.SecretaryDashboard) },
                        label = if (isMobile) "Inicio" else "Volver al inicio"
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                SecretariaEnrollmentDashboard(
                    modifier = Modifier.fillMaxWidth()
                )
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
                            activeItem = "Inscripciones",
                            modifier = Modifier.fillMaxHeight(),
                            onItemClick = { item ->
                                navigateFromSidebar(item)
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                }
            ) {
                enrollmentContent()
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                SaseSidebar(
                    activeItem = "Inscripciones",
                    modifier = Modifier.width(260.dp),
                    onItemClick = navigateFromSidebar
                )
                Box(modifier = Modifier.weight(1f)) {
                    enrollmentContent()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaseAppContent(viewModel: LabViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentRole by viewModel.userRole.collectAsState()

    Scaffold(
        containerColor = SaseNavy
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    is Screen.SecretaryDashboard -> SecretaryDashboardScreen(
                        viewModel = viewModel
                    )
                    is Screen.StudentRecord -> StudentRecordScreen(
                        studentId = screen.studentId,
                        viewModel = viewModel
                    )
                    is Screen.EnrollmentDashboard -> EnrollmentDashboardScreen(
                        viewModel = viewModel
                    )
                    is Screen.PreApplicationFamilyPortal -> PreApplicationFamilyPortalScreen(
                        viewModel = viewModel
                    )
                    is Screen.SecretariaPreApplicationDashboard -> SecretariaPreApplicationDashboardScreen(
                        viewModel = viewModel
                    )
                    is Screen.OfficialEnrollmentDashboard -> OfficialEnrollmentDashboardScreen(
                        viewModel = viewModel
                    )
                    is Screen.CredentialPreview -> CredentialPreviewScreen(
                        studentId = screen.studentId,
                        viewModel = viewModel
                    )
                    is Screen.StudentCredentialDashboard -> StudentCredentialDashboardScreen(
                        viewModel = viewModel
                    )
                }
            }

            // Compact dev role toggle — hidden in production
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SaseNavy2.copy(alpha = 0.5f))
                    .clickable {
                        val roles = AppRole.entries.toTypedArray()
                        val nextIndex = (roles.indexOf(currentRole) + 1) % roles.size
                        viewModel.setRole(roles[nextIndex])
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${currentRole.label}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 9.sp
                )
            }
        }
    }
}

// ── FASE 1 STUBS ──────────────────────────────────────────────────────────

@Composable
fun OfficialEnrollmentDashboardScreen(viewModel: LabViewModel) {
    val toast = LocalToast.current
    val officialStudents by PreApplicationViewModel.officialStudents.collectAsState()
    val masterStudents = MockSaseData.students
    var searchQuery by remember { mutableStateOf("") }
    var selectedOfficialId by remember { mutableStateOf<String?>(null) }

    val filtered = remember(officialStudents, searchQuery) {
        if (searchQuery.isBlank()) officialStudents
        else officialStudents.filter { os ->
            os.alumnoNombreCompleto.contains(searchQuery, ignoreCase = true) ||
            os.curp.contains(searchQuery, ignoreCase = true) ||
            (os.matriculaOficial?.contains(searchQuery, ignoreCase = true) == true) ||
            os.preApplicationFolio.contains(searchQuery, ignoreCase = true)
        }
    }

    val selectedOfficial = remember(selectedOfficialId, officialStudents) {
        officialStudents.find { it.id == selectedOfficialId }
    }

    val linkedMaster = remember(selectedOfficial) {
        selectedOfficial?.let { os ->
            MockSaseData.studentByCurp(os.curp)
        }
    }

    val hasInconsistency = selectedOfficial != null && linkedMaster == null

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
                "Altas Oficiales" -> {}
                "Credenciales" -> viewModel.navigateTo(Screen.StudentCredentialDashboard)
            }
        }

        val content = @Composable {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(if (isMobile) 16.dp else 24.dp)
            ) {
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
                            Text("Altas Oficiales", fontWeight = FontWeight.ExtraBold, fontSize = if (isMobile) 20.sp else 24.sp, color = SaseNavy)
                            Text("Matrículas generadas y expedientes maestro", fontSize = if (isMobile) 11.sp else 12.sp, color = SaseMuted)
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
                                "${officialStudents.size} altas",
                                color = SaseCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar por nombre, CURP, matrícula o folio...", color = SaseMuted) },
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
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (officialStudents.isEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, tint = SaseMuted.copy(alpha = 0.4f), modifier = Modifier.size(56.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Aún no hay altas oficiales.", fontWeight = FontWeight.Bold, color = SaseMuted, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Cuando una pre-solicitud lista sea convertida, aparecerá aquí con su matrícula y expediente maestro.",
                                color = SaseMuted.copy(alpha = 0.7f), fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                } else if (filtered.isEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("No hay altas oficiales que coincidan con la búsqueda.", color = SaseMuted, fontSize = 11.sp, modifier = Modifier.padding(16.dp))
                    }
                } else if (isMobile || selectedOfficial == null) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Altas Oficiales", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SaseNavy)
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            filtered.forEach { os ->
                                val isSelected = os.id == selectedOfficialId
                                OfficialStudentListItem(
                                    os = os,
                                    isSelected = isSelected,
                                    onClick = { selectedOfficialId = os.id }
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        GlassCard(modifier = Modifier.width(320.dp)) {
                            Text("Altas Oficiales", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SaseNavy)
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                filtered.forEach { os ->
                                    val isSelected = os.id == selectedOfficialId
                                    OfficialStudentListItem(
                                        os = os,
                                        isSelected = isSelected,
                                        onClick = { selectedOfficialId = os.id }
                                    )
                                }
                            }
                        }

                        GlassCard(modifier = Modifier.weight(1f)) {
                            OfficialStudentDetailWithMaster(
                                officialStudent = selectedOfficial!!,
                                masterStudent = linkedMaster,
                                hasInconsistency = hasInconsistency
                            )
                        }
                    }
                }

                if (selectedOfficial != null && isMobile) {
                    Spacer(modifier = Modifier.height(12.dp))
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        OfficialStudentDetailWithMaster(
                            officialStudent = selectedOfficial!!,
                            masterStudent = linkedMaster,
                            hasInconsistency = hasInconsistency
                        )
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
                            activeItem = "Altas Oficiales",
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
                    activeItem = "Altas Oficiales",
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
private fun OfficialStudentListItem(
    os: OfficialStudent,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val statusColor = when (os.status) {
        OfficialStudentStatus.ALTA_OFICIAL_CON_GRUPO -> SaseGreen
        OfficialStudentStatus.ALTA_OFICIAL_SIN_GRUPO -> SaseBlue
        OfficialStudentStatus.PENDIENTE_ASIGNACION_GRUPO -> SaseOrange
        else -> SaseMuted
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) SaseBlue.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.5f))
            .border(if (isSelected) 1.dp else 0.dp, if (isSelected) SaseBlue.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(os.alumnoNombreCompleto, fontWeight = FontWeight.Bold, color = SaseText, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(os.curp, color = SaseMuted, fontSize = 9.sp)
                if (os.matriculaOficial != null) {
                    Text("· ${os.matriculaOficial}", color = SaseCyan, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Text("Folio: ${os.preApplicationFolio}", color = SaseMuted.copy(alpha = 0.7f), fontSize = 9.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(statusColor.copy(alpha = 0.12f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(os.status.label, color = statusColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OfficialStudentDetailWithMaster(
    officialStudent: OfficialStudent,
    masterStudent: Student?,
    hasInconsistency: Boolean
) {
    Text("Detalle de alta oficial", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SaseNavy)

    if (hasInconsistency) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SaseRed.copy(alpha = 0.1f))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = SaseRed, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Alta oficial sin expediente maestro vinculado. Revisar integridad.",
                color = SaseRed, fontSize = 10.sp, fontWeight = FontWeight.Bold
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("Alta oficial")
    DetailRow("Nombre", officialStudent.alumnoNombreCompleto)
    DetailRow("CURP", officialStudent.curp)
    DetailRow("Matrícula", officialStudent.matriculaOficial ?: "Sin asignar")
    DetailRow("Grado ingreso", "${officialStudent.gradoIngreso}°")
    DetailRow("Grupo sugerido", officialStudent.grupoSugerido ?: "N/A")
    DetailRow("Grupo asignado", officialStudent.grupoAsignado ?: "Pendiente")
    DetailRow("Estado", officialStudent.status.label)
    DetailRow("Folio pre-solicitud", officialStudent.preApplicationFolio)

    val readinessFromPreApp = PreApplicationViewModel.sharedPreApplications.value
        .firstOrNull { it.folio == officialStudent.preApplicationFolio }
    if (readinessFromPreApp != null) {
        DetailRow("Readiness", readinessFromPreApp.readinessStatus.label)
        DetailRow("Alta creada", officialStudent.fechaCreacion)
    }

    Spacer(modifier = Modifier.height(14.dp))
    HorizontalDivider(color = SaseBorder)
    Spacer(modifier = Modifier.height(10.dp))

    SectionHeader("Expediente maestro")
    if (masterStudent != null) {
        DetailRow("Matrícula oficial", masterStudent.enrollmentId)
        DetailRow("Nombre", masterStudent.fullName)
        DetailRow("CURP", masterStudent.curp)
        DetailRow("Grado", masterStudent.group)
        DetailRow("Estado institucional", masterStudent.status)
        if (masterStudent.preApplicationFolio != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SaseGreen.copy(alpha = 0.08f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SaseGreen, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Expediente generado desde pre-solicitud ${masterStudent.preApplicationFolio}.",
                    color = SaseGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    } else if (!hasInconsistency) {
        Text("Sin expediente maestro vinculado todavía.", color = SaseMuted, fontSize = 10.sp)
    }
}
