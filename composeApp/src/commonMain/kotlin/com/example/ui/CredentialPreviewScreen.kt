package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Student
import com.example.data.StudentCredentialPreview
import com.example.viewmodel.LabViewModel
import com.example.viewmodel.Screen

@Composable
fun CredentialPreviewScreen(
    studentId: String,
    viewModel: LabViewModel
) {
    val students by viewModel.saseStudents.collectAsState()
    val student = remember(students, studentId) { students.find { it.id == studentId } }

    if (student == null) {
        Box(modifier = Modifier.fillMaxSize().background(SaseBgSoft), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Alumno no encontrado", fontWeight = FontWeight.Bold, color = SaseNavy)
                Button(onClick = { viewModel.navigateTo(Screen.SecretaryDashboard) }) {
                    Text("Regresar")
                }
            }
        }
        return
    }

    val preview = remember(student) { StudentCredentialPreview.fromStudent(student) }
    var showBack by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize().background(SaseBgSoft).padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxHeight().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Back button + title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(Screen.StudentRecord(studentId)) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = SaseNavy)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("Credencial Escolar", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = SaseNavy)
            }

            // Front/Back toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(SaseNavy.copy(alpha = .06f)),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    FaceButton(
                        text = "Frente",
                        selected = !showBack,
                        onClick = { showBack = false }
                    )
                    FaceButton(
                        text = "Reverso",
                        selected = showBack,
                        onClick = { showBack = true }
                    )
                }
            }

            // Credential card (front or back)
            if (showBack) {
                CredentialCardBack(preview)
            } else {
                CredentialCard(preview)
            }

            // Detail information
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Detalles del registro", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SaseNavy)

                    DetailLine("Matr\u00edcula oficial", preview.enrollmentId)
                    DetailLine("Folio de pre-solicitud", preview.preApplicationFolio ?: "No aplica")
                    DetailLine("Ciclo escolar", preview.schoolYear)
                    DetailLine("Estado institucional", preview.status)
                    DetailLine("Origen", if (preview.generatedFromOfficialEnrollment) "Alta oficial" else "Registro directo")
                    DetailLine("Foto", preview.photoStatus)

                    HorizontalDivider(color = SaseBorder, thickness = 1.dp)

                    Text(
                        "Esta es una vista previa institucional. La credencial oficial ser\u00e1 emitida por Secretar\u00eda.",
                        fontSize = 11.sp,
                        color = SaseMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Back to record button
            OutlinedButton(
                onClick = { viewModel.navigateTo(Screen.StudentRecord(studentId)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Volver al expediente", color = SaseNavy, fontSize = 12.sp)
            }

            Text(
                "Exportaci\u00f3n PDF e impresi\u00f3n: fase futura, no disponible en esta versi\u00f3n.",
                fontSize = 9.sp,
                color = SaseMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CredentialCard(preview: StudentCredentialPreview) {
    val navyDark = Color(0xFF0f243d)
    val navyMedium = Color(0xFF1a3a5c)
    val gold = Color(0xFFc9a84c)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(listOf(navyDark, navyMedium))
            )
            .border(1.dp, gold.copy(alpha = .4f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Top badge: school name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "ESC. SEC. DIURNA",
                        fontSize = 9.sp,
                        color = gold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "No. 310 \"Presidentes de M\u00e9xico\"",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = .8f),
                        letterSpacing = .5.sp
                    )
                    Text(
                        "Turno Vespertino",
                        fontSize = 8.sp,
                        color = Color.White.copy(alpha = .5f),
                        letterSpacing = 1.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = .1f))
                        .border(1.dp, gold.copy(alpha = .3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        tint = gold,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Photo + Name section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Photo placeholder
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = .08f))
                        .border(1.dp, Color.White.copy(alpha = .15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Foto",
                        tint = Color.White.copy(alpha = .35f),
                        modifier = Modifier.size(48.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        preview.fullName.uppercase(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CredentialPill("${preview.grade} ${preview.group ?: ""}".trim())
                        CredentialPill(preview.schoolYear.take(9))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = .12f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Data rows
            InfoRow("MATR\u00cdCULA", preview.enrollmentId)
            InfoRow("CURP", preview.curp)
            InfoRow("ESTATUS", preview.status)

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = .06f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "SASE Light \u2014 Credencial de identifiaci\u00f3n escolar",
                    fontSize = 8.sp,
                    color = Color.White.copy(alpha = .35f),
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun FaceButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) SaseNavy else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else SaseMuted
        )
    }
}

@Composable
private fun CredentialCardBack(preview: StudentCredentialPreview) {
    val navyDark = Color(0xFF0f243d)
    val navyMedium = Color(0xFF1a3a5c)
    val gold = Color(0xFFc9a84c)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(listOf(navyDark, navyMedium))
            )
            .border(1.dp, gold.copy(alpha = .4f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // School header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "ESC. SEC. DIURNA",
                        fontSize = 9.sp,
                        color = gold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "No. 310 \"Presidentes de M\u00e9xico\"",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = .8f)
                    )
                    Text(
                        "Turno Vespertino",
                        fontSize = 8.sp,
                        color = Color.White.copy(alpha = .5f),
                        letterSpacing = 1.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = .1f))
                        .border(1.dp, gold.copy(alpha = .3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        tint = gold,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = .12f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Institutional data
            BackInfoRow("CICLO ESCOLAR", preview.schoolYear)
            BackInfoRow("MATR\u00cdCULA OFICIAL", preview.enrollmentId)
            BackInfoRow("FOLIO ORIGEN", preview.preApplicationFolio ?: "No aplica")
            BackInfoRow("ESTADO", "Vista previa")

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = .12f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Disclaimer
            Text(
                "Esta credencial es una vista previa generada por SASE Light. Su uso oficial requiere validaci\u00f3n de Direcci\u00f3n.",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = .7f),
                lineHeight = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Seal / signature mock
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = .06f))
                    .border(1.dp, Color.White.copy(alpha = .1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = .2f),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Sello / firma autorizada",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = .35f),
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // QR mock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = .1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.QrCode,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = .25f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        "SASE Light \u2014 Reverso de credencial",
                        fontSize = 8.sp,
                        color = Color.White.copy(alpha = .35f),
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color.White.copy(alpha = .12f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Privacy notice
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(SaseBlue.copy(alpha = .1f))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    "La credencial no contiene datos m\u00e9dicos, sociofamiliares ni de apoyo educativo.",
                    fontSize = 8.sp,
                    color = SaseBlue.copy(alpha = .7f),
                    lineHeight = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CredentialPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = .12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 9.sp, color = Color.White.copy(alpha = .85f), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 7.sp, color = Color.White.copy(alpha = .4f), fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Text(value, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BackInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 8.sp, color = Color.White.copy(alpha = .5f), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(value, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = SaseMuted, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 12.sp, color = SaseNavy, fontWeight = FontWeight.Medium)
    }
}
