package com.example.ui.dashboard

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Student
import com.example.ui.SaseBlue
import com.example.ui.SaseGreen
import com.example.ui.SaseMuted
import com.example.ui.SaseNavy
import com.example.ui.SaseOrange
import com.example.ui.SaseRed

@Composable
fun MetricGlassCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
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
                    cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx())
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
