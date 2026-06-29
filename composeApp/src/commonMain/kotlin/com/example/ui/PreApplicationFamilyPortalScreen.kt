package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.viewmodel.LabViewModel
import com.example.viewmodel.PreApplicationViewModel
import com.example.viewmodel.Screen

private val familyViewModel = PreApplicationViewModel()

@Composable
fun PreApplicationFamilyPortalScreen(viewModel: LabViewModel) {
    val currentStep by familyViewModel.currentStep.collectAsState()
    val submittedFolio by familyViewModel.submittedFolio.collectAsState()
    val errors by familyViewModel.errors.collectAsState()
    val isSubmitting by familyViewModel.isSubmitting.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SaseBgSoft)
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 800.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header institucional
            Text(
                text = "Pre-solicitud de Ingreso SASE",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SaseNavy
            )
            Text(
                text = "Escuela Secundaria Diurna No. 310 'Presidentes de Mexico'",
                fontSize = 14.sp,
                color = SaseMuted
            )
            Text(
                text = "Este formulario no confirma inscripcion oficial. Secretaria validara documentos y datos.",
                fontSize = 11.sp,
                color = SaseOrange,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Step indicator
            val stepTitles = listOf("Datos", "Contactos", "Contexto", "Docs", "Envio")
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                stepTitles.forEachIndexed { i, title ->
                    StepIndicator(step = i, currentStep = currentStep, title = title)
                    if (i < stepTitles.lastIndex) {
                        StepDivider(active = i < currentStep)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Form card
            GlassCard(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (currentStep) {
                        0 -> StepDatosBasicos(familyViewModel)
                        1 -> StepContactos()
                        2 -> StepContexto()
                        3 -> StepDocumentos()
                        4 -> StepResumenEnvio(familyViewModel)
                    }
                }
            }

            // Errors banner
            if (errors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Corrige los siguientes campos:", color = SaseRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        errors.forEach { (field, msg) ->
                            Text("  - $field: $msg", color = SaseRed, fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = { familyViewModel.previousStep() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Atras", color = SaseNavy)
                    }
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else if (currentStep < 4) {
                    Button(
                        onClick = { familyViewModel.nextStep() },
                        colors = ButtonDefaults.buttonColors(containerColor = SaseNavy),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Siguiente", color = Color.White)
                    }
                } else {
                    Button(
                        onClick = { familyViewModel.submitApplication() },
                        colors = ButtonDefaults.buttonColors(containerColor = SaseGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enviar Pre-solicitud", color = Color.White)
                    }
                }
            }
        }
    }

    // Success dialog
    if (submittedFolio != null) {
        Dialog(onDismissRequest = {}) {
            GlassCard {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SaseGreen, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Pre-solicitud Enviada", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SaseNavy)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Guarde su folio para continuar el tramite en Secretaria:", textAlign = TextAlign.Center, color = SaseMuted, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SaseBgSoft, RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(submittedFolio!!, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = SaseBlue)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Acuda a Secretaria con sus documentos originales para continuar el tramite.",
                        textAlign = TextAlign.Center,
                        color = SaseMuted,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            familyViewModel.resetForm()
                            viewModel.navigateTo(Screen.SecretaryDashboard)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaseNavy),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Volver al Inicio", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(step: Int, currentStep: Int, title: String) {
    val isActive = step <= currentStep
    val color = when {
        step == currentStep -> SaseBlue
        step < currentStep -> SaseGreen
        else -> SaseMuted.copy(alpha = 0.3f)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (step < currentStep) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            } else {
                Text("${step + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(title, fontSize = 9.sp, color = if (isActive) SaseNavy else SaseMuted)
    }
}

@Composable
private fun RowScope.StepDivider(active: Boolean) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(2.dp)
            .padding(horizontal = 4.dp)
            .background(if (active) SaseGreen else SaseBorder)
    )
}

// ── STEP 1: Datos Basicos (Block A + B) ────────────────────────

@Composable
private fun StepDatosBasicos(vm: PreApplicationViewModel) {
    val tipoTramite by vm.tipoTramite.collectAsState()
    val grado by vm.gradoSolicitado.collectAsState()
    val nombre by vm.nombreCompleto.collectAsState()
    val curp by vm.curp.collectAsState()
    val fechaNac by vm.fechaNacimiento.collectAsState()
    val sexo by vm.sexo.collectAsState()
    val nacionalidad by vm.nacionalidad.collectAsState()
    val entidadNac by vm.entidadNacimiento.collectAsState()
    val telefono by vm.telefonoPrincipal.collectAsState()
    val correo by vm.correo.collectAsState()
    val escuela by vm.escuelaProcedencia.collectAsState()
    val acepta by vm.aceptaAvisoPrivacidad.collectAsState()
    val domicilio by vm.domicilio.collectAsState()
    val telefonoCasa by vm.telefonoCasa.collectAsState()
    val errors by vm.errors.collectAsState()

    Text("Datos del Alumno", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SaseNavy)
    Text("Ingresa los datos tal y como aparecen en el acta de nacimiento.", fontSize = 12.sp, color = SaseMuted)

    // Tipo de tramite
    Text("Tipo de tramite", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SaseNavy)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf("Nuevo Ingreso", "Reinscripcion").forEach { opt ->
            val selected = tipoTramite == opt
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) SaseNavy else SaseBgSoft)
                    .clickable { vm.setTipoTramite(opt) }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(opt, color = if (selected) Color.White else SaseNavy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }

    // Grado solicitado
    Text("Grado solicitado", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SaseNavy)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        (1..3).forEach { g ->
            val selected = grado == g
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) SaseBlue else SaseBgSoft)
                    .border(if (errors.containsKey("grado")) 2.dp else 0.dp, SaseRed, RoundedCornerShape(10.dp))
                    .clickable { vm.setGradoSolicitado(g) }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("${g}° Grado", color = if (selected) Color.White else SaseNavy, fontWeight = FontWeight.Bold)
            }
        }
    }

    FormField("Nombre completo", nombre, { vm.setNombreCompleto(it) }, errors.containsKey("nombre"), "Obligatorio")
    FormField("CURP (18 caracteres)", curp, { vm.setCurp(it) }, errors.containsKey("curp"), "18 caracteres requeridos")
    FormField("Fecha de nacimiento (DD/MMM/AAAA)", fechaNac, { vm.setFechaNacimiento(it) }, errors.containsKey("fechaNac"), "Obligatorio")

    // Sexo selector
    Text("Sexo (segun acta)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SaseNavy)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf("Femenino", "Masculino").forEach { opt ->
            val selected = sexo == opt
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) SaseViolet else SaseBgSoft)
                    .clickable { vm.setSexo(opt) }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(opt, color = if (selected) Color.White else SaseNavy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }

    FormField("Nacionalidad", nacionalidad, { vm.setNacionalidad(it) }, false, null)
    FormField("Entidad de nacimiento", entidadNac, { vm.setEntidadNacimiento(it) }, false, null)

    Text("Domicilio", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SaseNavy)
    FormField("Domicilio (calle, colonia, municipio, estado)", domicilio, { vm.setDomicilio(it) }, false, null)

    Text("Contacto", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SaseNavy)
    FormField("Telefono principal (10 digitos)", telefono, { vm.setTelefonoPrincipal(it) }, errors.containsKey("telefono"), "10 digitos requeridos")
    FormField("Correo electronico (opcional)", correo, { vm.setCorreo(it) }, false, null)
    FormField("Telefono de casa (opcional)", telefonoCasa, { vm.setTelefonoCasa(it) }, false, null)
    FormField("Escuela de procedencia", escuela, { vm.setEscuelaProcedencia(it) }, false, null)

    // Aviso de privacidad
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (errors.containsKey("aviso")) SaseRed.copy(alpha = 0.08f) else SaseBgSoft)
            .padding(10.dp)
    ) {
        Checkbox(
            checked = acepta,
            onCheckedChange = { vm.setAceptaAvisoPrivacidad(it) }
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Acepto el Aviso de Privacidad y el uso de mis datos para fines escolares.", fontSize = 12.sp, color = SaseNavy)
    }
}

@Composable
private fun FormField(label: String, value: String, onChange: (String) -> Unit, hasError: Boolean, errorMsg: String?) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            isError = hasError,
            supportingText = if (hasError && errorMsg != null) {{ Text(errorMsg, color = SaseRed) }} else null,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(6.dp))
    }
}

// ── STEP 2: Contactos (placeholder) ─────────────────────────────

@Composable
private fun StepContactos() {
    PlaceholderStep(
        title = "Responsables y Contactos",
        description = "Registra a la madre, padre o tutor legal.",
        phase = "2A",
        nextSection = "Contactos, Autorizados para recoger"
    )
}

// ── STEP 3: Contexto (placeholder) ──────────────────────────────

@Composable
private fun StepContexto() {
    PlaceholderStep(
        title = "Contexto del Alumno",
        description = "Ficha medica, trabajo social y antecedentes escolares.",
        phase = "2A",
        nextSection = "Ficha Medica, Contexto Sociofamiliar, UDEII"
    )
}

// ── STEP 4: Documentos (placeholder) ────────────────────────────

@Composable
private fun StepDocumentos() {
    PlaceholderStep(
        title = "Documentos y Consentimientos",
        description = "Declara los documentos que presentaras y acepta los terminos institucionales.",
        phase = "2A",
        nextSection = "Documentos declarados, Consentimientos, Avisos"
    )
}

// ── STEP 5: Resumen y Envio ─────────────────────────────────────

@Composable
private fun StepResumenEnvio(vm: PreApplicationViewModel) {
    val tipoTramite by vm.tipoTramite.collectAsState()
    val grado by vm.gradoSolicitado.collectAsState()
    val nombre by vm.nombreCompleto.collectAsState()
    val curp by vm.curp.collectAsState()
    val fechaNac by vm.fechaNacimiento.collectAsState()
    val sexo by vm.sexo.collectAsState()
    val telefono by vm.telefonoPrincipal.collectAsState()
    val correo by vm.correo.collectAsState()
    val acepta by vm.aceptaAvisoPrivacidad.collectAsState()

    Text("Resumen y Envio", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SaseNavy)
    Text("Revisa tus datos antes de enviar.", fontSize = 12.sp, color = SaseMuted)

    GlassCard {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SummaryLine("Tramite", tipoTramite)
            SummaryLine("Grado", "${grado}°")
            SummaryLine("Nombre", nombre)
            SummaryLine("CURP", curp)
            SummaryLine("Fecha Nac.", fechaNac)
            SummaryLine("Sexo", sexo)
            SummaryLine("Telefono", telefono)
            if (correo.isNotBlank()) SummaryLine("Correo", correo)
            SummaryLine("Aviso privacidad", if (acepta) "Aceptado" else "Pendiente", if (acepta) SaseGreen else SaseRed)
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Al enviar, genera un folio provisional. Secretaria validara tus datos y documentos.",
        fontSize = 11.sp,
        color = SaseMuted
    )
}

@Composable
private fun SummaryLine(label: String, value: String, valueColor: Color = SaseNavy) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = SaseMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ── Shared placeholder composable ───────────────────────────────

@Composable
private fun PlaceholderStep(title: String, description: String, phase: String, nextSection: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SaseNavy)
        Text(description, fontSize = 12.sp, color = SaseMuted)
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SaseBlue.copy(alpha = 0.06f))
                .border(1.dp, SaseBlue.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Construction, contentDescription = null, tint = SaseBlue, modifier = Modifier.size(40.dp))
                Text("Fase $phase", color = SaseBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    "Seccion preparada para siguiente fase.",
                    color = SaseMuted, fontSize = 12.sp, textAlign = TextAlign.Center
                )
                Text(
                    "Campos incluidos: $nextSection",
                    color = SaseMuted, fontSize = 11.sp, textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Puedes continuar. Estos datos se completaran en la siguiente fase.",
            fontSize = 11.sp, color = SaseMuted, textAlign = TextAlign.Center
        )
    }
}
