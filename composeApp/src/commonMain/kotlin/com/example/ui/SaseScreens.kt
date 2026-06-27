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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.collectAsState
import com.example.data.*
import com.example.util.LocalToast
import com.example.ui.gemini.GeminiTestCard
import com.example.viewmodel.LabViewModel
import com.example.viewmodel.Screen
import kotlinx.coroutines.launch

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
fun RealtimeActivityChart(
    modifier: Modifier = Modifier,
    lineColor: Color = SaseBlue
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val path = androidx.compose.ui.graphics.Path()
        
        // 5 points to draw a beautiful wave
        path.moveTo(0f, height * 0.75f)
        path.cubicTo(
            width * 0.25f, height * 0.3f,
            width * 0.45f, height * 0.85f,
            width * 0.65f, height * 0.15f
        )
        path.quadraticTo(
            width * 0.85f, height * 0.5f,
            width, height * 0.2f
        )
        
        val fillPath = androidx.compose.ui.graphics.Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        
        // Solid glow fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.22f), Color.Transparent)
            )
        )
        
        // Stroke with shadow for high-fidelity glowing line
        drawPath(
            path = path,
            color = lineColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 3.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )
        
        // Glowing dot
        drawCircle(
            color = lineColor,
            radius = 6.dp.toPx(),
            center = Offset(width * 0.65f, height * 0.15f)
        )
        drawCircle(
            color = Color.White,
            radius = 2.5.dp.toPx(),
            center = Offset(width * 0.65f, height * 0.15f)
        )
    }
}

@Composable
fun MetricGlassCard(
    title: String,
    value: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outlineBrush = if (isSelected) {
        Brush.linearGradient(
            colors = listOf(
                iconTint,
                iconTint.copy(alpha = 0.4f),
                Color.White,
                iconTint
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.7f),
                Color.White.copy(alpha = 0.15f),
                SaseBlue.copy(alpha = 0.1f),
                Color.White.copy(alpha = 0.4f)
            )
        )
    }

    val borderWidth = if (isSelected) 2.dp else 1.2.dp
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .testTag("metric_glass_card_${title.replace(" ", "_").lowercase()}")
            .clip(shape)
            .clickable { onClick() }
            .background(
                Brush.verticalGradient(
                    colors = if (isSelected) {
                        listOf(
                            iconTint.copy(alpha = 0.12f),
                            iconTint.copy(alpha = 0.02f)
                        )
                    } else {
                        listOf(
                            Color.White.copy(alpha = 0.55f),
                            Color.White.copy(alpha = 0.25f)
                        )
                    }
                ),
                shape = shape
            )
            .border(
                border = BorderStroke(borderWidth, outlineBrush),
                shape = shape
            )
            .drawBehind {
                if (isSelected) {
                    // Add a neon back-glow corresponding to the icon tint
                    drawCircle(
                        color = iconTint.copy(alpha = 0.1f),
                        radius = size.width * 0.4f,
                        center = Offset(size.width * 0.8f, size.height * 0.2f)
                    )
                }
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(30f, 30f),
                        radius = size.width * 0.35f
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(22.dp.toPx(), 22.dp.toPx())
                )
            }
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = SaseNavy.copy(alpha = 0.8f),
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = SaseNavy,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtext,
                fontSize = 10.sp,
                color = SaseMuted,
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Neon aesthetic line at the bottom of card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            Brush.horizontalGradient(listOf(iconTint, Color.Transparent))
                        } else {
                            Brush.horizontalGradient(listOf(Color.White, Color.Transparent))
                        }
                    )
            )
        }
    }
}

@Composable
fun LiquidGlassDashboard(
    students: List<Student>,
    selectedFilter: String,
    onFilterSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount = students.size
    val activeCount = students.count { it.status == "Activo" || it.status == "Nuevo ingreso" }
    val pendingDocsCount = students.count { it.documentationStatus != "Completa" || it.documents.any { doc -> doc.status == "Pendiente" } }
    val atRiskCount = students.count { it.riskLevel == "Alto" || it.riskLevel == "Medio" }
    val highAttendanceCount = students.count { it.attendancePercent >= 90 }
    val attendanceRate = if (totalCount > 0) (highAttendanceCount * 100) / totalCount else 0

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Consola Central de Expedientes",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = SaseNavy
                )
                Text(
                    text = "Filtrado táctil inteligente con estética Liquid Glass",
                    fontSize = 11.sp,
                    color = SaseMuted
                )
            }
            if (selectedFilter != "Todos") {
                TextButton(
                    onClick = { onFilterSelect("Todos") },
                    colors = ButtonDefaults.textButtonColors(contentColor = SaseBlue)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Limpiar filtro", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // We will make a 2x2 grid on compact/mobile and a 4-column row on large screens
        BoxWithConstraints {
            val isLarge = maxWidth >= 600.dp
            if (isLarge) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricGlassCard(
                        title = "Expedientes Activos",
                        value = activeCount.toString(),
                        subtext = "$activeCount de $totalCount alumnos",
                        icon = Icons.Default.CheckCircle,
                        iconTint = SaseGreen,
                        isSelected = selectedFilter == "Activos",
                        onClick = { onFilterSelect("Activos") },
                        modifier = Modifier.weight(1f)
                    )
                    MetricGlassCard(
                        title = "Doc. Pendiente",
                        value = pendingDocsCount.toString(),
                        subtext = "Faltan expedientes",
                        icon = Icons.Default.Schedule,
                        iconTint = SaseOrange,
                        isSelected = selectedFilter == "Pendientes",
                        onClick = { onFilterSelect("Pendientes") },
                        modifier = Modifier.weight(1f)
                    )
                    MetricGlassCard(
                        title = "Alumnos en Riesgo",
                        value = atRiskCount.toString(),
                        subtext = "Medio o Alto riesgo",
                        icon = Icons.Default.Warning,
                        iconTint = SaseRed,
                        isSelected = selectedFilter == "Riesgo",
                        onClick = { onFilterSelect("Riesgo") },
                        modifier = Modifier.weight(1f)
                    )
                    MetricGlassCard(
                        title = "Asistencia Óptima",
                        value = "$attendanceRate%",
                        subtext = "$highAttendanceCount con +90%",
                        icon = Icons.Default.TrendingUp,
                        iconTint = SaseBlue,
                        isSelected = selectedFilter == "Asistencia",
                        onClick = { onFilterSelect("Asistencia") },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricGlassCard(
                            title = "Expedientes Activos",
                            value = activeCount.toString(),
                            subtext = "$activeCount alumnos",
                            icon = Icons.Default.CheckCircle,
                            iconTint = SaseGreen,
                            isSelected = selectedFilter == "Activos",
                            onClick = { onFilterSelect("Activos") },
                            modifier = Modifier.weight(1f)
                        )
                        MetricGlassCard(
                            title = "Doc. Pendiente",
                            value = pendingDocsCount.toString(),
                            subtext = "Por completar",
                            icon = Icons.Default.Schedule,
                            iconTint = SaseOrange,
                            isSelected = selectedFilter == "Pendientes",
                            onClick = { onFilterSelect("Pendientes") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricGlassCard(
                            title = "Alumnos en Riesgo",
                            value = atRiskCount.toString(),
                            subtext = "Alerta académica",
                            icon = Icons.Default.Warning,
                            iconTint = SaseRed,
                            isSelected = selectedFilter == "Riesgo",
                            onClick = { onFilterSelect("Riesgo") },
                            modifier = Modifier.weight(1f)
                        )
                        MetricGlassCard(
                            title = "Asistencia Óptima",
                            value = "$attendanceRate%",
                            subtext = "Asistencia alta",
                            icon = Icons.Default.TrendingUp,
                            iconTint = SaseBlue,
                            isSelected = selectedFilter == "Asistencia",
                            onClick = { onFilterSelect("Asistencia") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HolographicActivityCard(
    students: List<Student>,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Tendencia de Matrícula y Altas",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = SaseNavy
                )
                Text(
                    text = "Monitoreo de flujo continuo de expedientes",
                    fontSize = 10.sp,
                    color = SaseMuted
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SaseGreen.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = SaseGreenDark, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Activo", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SaseGreenDark)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.width(80.dp)) {
                Text("Alta", fontSize = 10.sp, color = SaseMuted)
                Text("96%", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = SaseNavy)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Baja", fontSize = 10.sp, color = SaseMuted)
                Text("1.2%", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = SaseRed)
            }
            
            RealtimeActivityChart(
                modifier = Modifier
                    .weight(1f)
                    .height(65.dp)
                    .padding(horizontal = 8.dp),
                lineColor = SaseBlue
            )
        }
    }
}

// Side bar component
@Composable
fun SaseSidebar(
    activeItem: String,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        "Inicio" to Icons.Default.Home,
        "Agenda" to Icons.Default.CalendarToday,
        "Reportes" to Icons.Default.Assignment,
        "Expedientes" to Icons.Default.Person,
        "Solicitudes" to Icons.Default.Description,
        "Inscripciones" to Icons.Default.School,
        "Archivo" to Icons.Default.History,
        "Matrícula Inteligente" to Icons.Default.TrendingUp,
        "Cierre de Ciclo" to Icons.Default.Lock,
        "Seguridad" to Icons.Default.Security
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
                        .clickable { /* Sidebar selection simulation */ }
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
        Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))
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
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "Ver detalle",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SaseBlue,
                    modifier = Modifier.clickable { onClick() }
                )
            }
        }
    }
}

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
                "Ver todos (${filteredList.size})",
                color = SaseBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {}
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
                            Divider(color = SaseBorder.copy(alpha = 0.05f), thickness = 0.5.dp)
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

// Side details lists
@Composable
fun SecretaryPendingList(
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        "Validar expediente" to Pair("12", SaseOrange),
        "Captura incompleta" to Pair("9", SaseRed),
        "Firma pendiente" to Pair("6", SaseOrange),
        "Solicitud de constancia" to Pair("7", SaseBlue),
        "Cambio de grupo" to Pair("4", SaseCyan),
        "Documentos vencidos" to Pair("3", SaseRed),
        "Tutor sin teléfono" to Pair("5", SaseOrange),
        "CURP pendiente" to Pair("8", SaseRed)
    )

    GlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Pendientes de Secretaría", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SaseNavy)
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

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items.forEach { (title, info) ->
                val (count, color) = info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.5f))
                        .clickable { onItemClick(title) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
                        Text(title, color = SaseText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(color.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(count, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun InstitutionalDocumentsPanel(
    onDocClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val docs = listOf(
        Pair("Constancia", Icons.Default.Description),
        Pair("Historial", Icons.Default.Assignment),
        Pair("Carta compromiso", Icons.Default.BorderColor),
        Pair("Citatorio", Icons.Default.NotificationImportant),
        Pair("Justificante", Icons.Default.EventNote)
    )

    GlassCard(modifier = modifier) {
        Text("Documentos institucionales", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SaseNavy)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            docs.forEach { (name, icon) ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.5f))
                        .border(1.dp, SaseBorder, RoundedCornerShape(14.dp))
                        .clickable { onDocClick(name) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SaseCyan.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = SaseCyan, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = name,
                        color = SaseText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

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
    val audits by viewModel.saseAudits.collectAsState()

    var dashboardFilter by remember { mutableStateOf("Todos") }

    var showQuickActionDialog by remember { mutableStateOf(false) }
    var quickActionTitle by remember { mutableStateOf("") }

    var showNewStudentDialog by remember { mutableStateOf(false) }
    var newStudentName by remember { mutableStateOf("") }
    var newStudentGroup by remember { mutableStateOf("1°A") }
    var newStudentCurp by remember { mutableStateOf("") }
    var newStudentTutor by remember { mutableStateOf("") }

    // Fast actions trigger
    val triggerQuickAction = { title: String ->
        quickActionTitle = title
        if (title == "Nuevo expediente") {
            showNewStudentDialog = true
        } else {
            showQuickActionDialog = true
        }
    }

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

                    // Bell icon and avatar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box {
                            IconButton(onClick = {}) {
                                Icon(Icons.Default.Notifications, contentDescription = "Alertas", tint = SaseNavy)
                            }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SaseRed)
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-8).dp, y = 8.dp)
                            )
                        }
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
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar
                GlobalStudentSearch(
                    students = students,
                    onStudentClick = { id -> viewModel.navigateTo(Screen.StudentRecord(id)) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Actions capsule row (Scrollable on mobile)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isMobile) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
                    horizontalArrangement = if (isMobile) Arrangement.spacedBy(8.dp) else Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Registro veloz" to Icons.Default.FlashOn,
                        "Nuevo expediente" to Icons.Default.Add,
                        "Importar alumnos" to Icons.Default.Publish,
                        "Generar documento" to Icons.Default.Description,
                        "Cierre de ciclo" to Icons.Default.Lock
                    ).forEach { (label, icon) ->
                        Button(
                            onClick = { triggerQuickAction(label) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (label == "Cierre de ciclo") SaseRed.copy(alpha = 0.1f) else Color.White,
                                contentColor = if (label == "Cierre de ciclo") SaseRed else SaseNavy
                            ),
                            border = BorderStroke(1.dp, if (label == "Cierre de ciclo") SaseRed.copy(alpha = 0.3f) else SaseBorder),
                            shape = RoundedCornerShape(16.dp),
                            modifier = if (isMobile) {
                                Modifier.height(44.dp).padding(end = 4.dp)
                            } else {
                                Modifier.weight(1f).height(44.dp)
                            }
                        ) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Centralized Holographic / Liquid Glass Dashboard Component
                LiquidGlassDashboard(
                    students = students,
                    selectedFilter = dashboardFilter,
                    onFilterSelect = { filter ->
                        dashboardFilter = filter
                        toast("Filtrando expedientes: $filter")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Real-time Holographic Wave Chart
                HolographicActivityCard(
                    students = students,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                val geminiState by viewModel.geminiState.collectAsState()
                GeminiTestCard(
                    geminiState = geminiState,
                    onGenerate = { prompt -> viewModel.generateGeminiImage(prompt, scope) },
                    onReset = { viewModel.resetGeminiState() },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Filter students list dynamically based on active dashboard filter selection
                val dashboardFilteredStudents = remember(students, dashboardFilter) {
                    when (dashboardFilter) {
                        "Activos" -> students.filter { it.status == "Activo" || it.status == "Nuevo ingreso" }
                        "Pendientes" -> students.filter { it.documentationStatus != "Completa" || it.documents.any { doc -> doc.status == "Pendiente" } }
                        "Riesgo" -> students.filter { it.riskLevel == "Alto" || it.riskLevel == "Medio" }
                        "Asistencia" -> students.filter { it.attendancePercent >= 90 }
                        else -> students
                    }
                }

                // Smart enrollment table with search filters
                SmartEnrollmentTable(
                    students = dashboardFilteredStudents,
                    onStudentClick = { id -> viewModel.navigateTo(Screen.StudentRecord(id)) },
                    onRegisterObsClick = { student ->
                        toast("Opción para ${student.fullName}")
                    }
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom grids (stacked on mobile, split-pane row on desktop)
                if (isMobile) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SecretaryPendingList(
                            onItemClick = { title ->
                                toast("Abriendo pendiente: $title")
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        InstitutionalDocumentsPanel(
                            onDocClick = { name ->
                                toast("Generando previsualización de: $name. PDF listo para descargar.")
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        ActivityAuditFeed(audits = audits, modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SecretaryPendingList(
                            onItemClick = { title ->
                                toast("Abriendo pendiente: $title")
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Column(modifier = Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            InstitutionalDocumentsPanel(
                                onDocClick = { name ->
                                    toast("Generando previsualización de: $name. PDF listo para descargar.")
                                }
                            )
                            ActivityAuditFeed(audits = audits)
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
                            activeItem = "Inicio",
                            modifier = Modifier.fillMaxHeight()
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
                    modifier = Modifier.width(260.dp)
                )
                Box(modifier = Modifier.weight(1f)) {
                    dashboardContent()
                }
            }
        }
    }

    // Quick action simulation modal
    if (showQuickActionDialog) {
        Dialog(onDismissRequest = { showQuickActionDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SaseBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(SaseBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = SaseBlue, modifier = Modifier.size(32.dp))
                    }
                    Text(
                        text = quickActionTitle,
                        fontWeight = FontWeight.Bold,
                        color = SaseNavy,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Esta acción administrativa está lista para conectarse al backend de Supabase. El sistema de roles y RLS protegerá esta operación de manera segura.",
                        fontSize = 12.sp,
                        color = SaseMuted,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { showQuickActionDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = SaseNavy),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Entendido", fontWeight = FontWeight.Bold)
                    }
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
                    Text("Nuevo Expediente Escolar", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 18.sp)
                    Text("Cree un registro de matrícula rápido e inicial.", color = SaseMuted, fontSize = 11.sp)

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
                                    val std = Student(
                                        id = newId,
                                        fullName = newStudentName,
                                        group = newStudentGroup,
                                        enrollmentId = "2024-00${100 + students.size}",
                                        curp = newStudentCurp.uppercase(),
                                        tutorName = newStudentTutor,
                                        tutorRelation = "Tutor",
                                        status = "Nuevo ingreso",
                                        riskLevel = "Bajo",
                                        documentationStatus = "Completa"
                                    )
                                    viewModel.addStudent(std)
                                    viewModel.logSaseAudit("Expediente creado", "Secretaría", std.fullName)
                                    showNewStudentDialog = false
                                    newStudentName = ""
                                    newStudentCurp = ""
                                    newStudentTutor = ""
                                    toast("Expediente escolar registrado")
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


// EXPEDIENTE DEL ALUMNO SCREEN (Ficha Viva)
@Composable
fun StudentRecordScreen(
    studentId: String,
    viewModel: LabViewModel
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
                                Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = SaseNavy)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Expediente", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = SaseNavy)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { toast("Edición habilitada") },
                                colors = ButtonDefaults.buttonColors(containerColor = SaseGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Editar", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                            }
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
                                Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = SaseNavy)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Expediente del Alumno", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = SaseNavy)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { toast("Edición habilitada") },
                                colors = ButtonDefaults.buttonColors(containerColor = SaseGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Editar expediente", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Button(
                                onClick = { showDocumentDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SaseBlue),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generar documento", fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
                    Divider(color = SaseBorder.copy(alpha = 0.08f))
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
                                                onCall = { toast("Llamando a ${student.tutorName}") }
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
                                                    progress = student.attendancePercent / 100f,
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
                                                Divider(color = SaseBorder.copy(alpha = 0.05f))
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
                                            Divider(color = SaseBorder.copy(alpha = 0.05f))
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
                                                Divider(color = SaseBorder.copy(alpha = 0.05f))
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
                                                    onCall = { toast("Llamando a ${student.tutorName}") }
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
                                                        progress = student.attendancePercent / 100f,
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
                                                    Divider(color = SaseBorder.copy(alpha = 0.05f))
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
                                                Divider(color = SaseBorder.copy(alpha = 0.05f))
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
                                                    Divider(color = SaseBorder.copy(alpha = 0.05f))
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
                                    onCall = { toast("Llamando a tutor principal...") }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Contacto alterno / Emergencia", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                TutorItem(
                                    name = "Luis Hernández Vargas",
                                    relation = "Tío",
                                    phone = "55 9876 5432",
                                    email = "luish@example.com",
                                    onCall = { toast("Llamando a contacto alterno...") }
                                )
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
                                            progress = student.attendancePercent / 100f,
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

                        Button(
                            onClick = { showObsDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SaseBlue),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Comment, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Agregar observación", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        Button(
                            onClick = { showDocumentDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SaseViolet),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Generar documento", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        Button(
                            onClick = { showEscalarDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SaseOrange),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Escalar caso", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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
                    Text("Generar Documento Institucional", fontWeight = FontWeight.Bold, color = SaseNavy, fontSize = 16.sp)
                    Text("Seleccione la plantilla que desea emitir para ${student.fullName}.", color = SaseMuted, fontSize = 11.sp)

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
                                    viewModel.logSaseAudit("Documento emitido", "Secretaría", "$name para ${student.fullName}")
                                    showDocumentDialog = false
                                    toast("Documento '$name' generado. PDF listo para descargar.")
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = SaseCyan, modifier = Modifier.size(18.dp))
                            Text(name, color = SaseText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Divider(color = SaseBorder.copy(alpha = 0.05f))
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
                            modifier = Modifier.fillMaxHeight()
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
                    modifier = Modifier.width(260.dp)
                )
                Box(modifier = Modifier.weight(1f)) {
                    recordContent()
                }
            }
        }
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
    Divider(color = SaseBorder.copy(alpha = 0.05f))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaseAppContent(viewModel: LabViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

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
                    else -> {
                        // Fallback/Safety
                        SecretaryDashboardScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

