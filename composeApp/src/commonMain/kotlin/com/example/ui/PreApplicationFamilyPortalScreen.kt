package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.launch
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

private val PortalBg = Color(0xFF0B1120)
private val PortalCardBg = Color(0xFF1A2332)
private val PortalText = Color(0xFFF1F5F9)
private val PortalMuted = Color(0xFF94A3B8)

@Composable
fun PreApplicationFamilyPortalScreen(viewModel: LabViewModel, onNavigateBack: () -> Unit = {}) {
    val familyViewModel = remember { PreApplicationViewModel() }
    val currentStep by familyViewModel.currentStep.collectAsState()
    val submittedFolio by familyViewModel.submittedFolio.collectAsState()
    val errors by familyViewModel.errors.collectAsState()
    val isSubmitting by familyViewModel.isSubmitting.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PortalBg)
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 800.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = { familyViewModel.resetForm(); onNavigateBack() },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SaseBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PortalMuted)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cerrar", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Header institucional
            Text(
                text = "Pre-registro y Reinscripción SASE",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PortalText
            )
            Text(
                text = "Escuela Secundaria Diurna No. 310 'Presidentes de México'",
                fontSize = 14.sp,
                color = PortalMuted
            )
            Text(
                text = "Al enviarlo recibirás un folio/código. Secretaría lo usará para abrir, revisar y convertir el trámite en registro oficial si procede.",
                fontSize = 11.sp,
                color = SaseOrange,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Step indicator
            val stepTitles = listOf("Datos", "Contactos", "Contexto", "Docs", "Envío")
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
            val scrollState = rememberScrollState()
            val coroutineScope = rememberCoroutineScope()
            LaunchedEffect(currentStep) {
                if (currentStep > 0) {
                    scrollState.animateScrollTo(0)
                }
            }
            GlassCard(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (currentStep) {
                        0 -> StepDatosBasicos(familyViewModel)
                        1 -> StepContactos(familyViewModel)
                        2 -> StepContextoFamiliar(familyViewModel)
                        3 -> StepDocumentos(familyViewModel)
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
                        Text("Atrás", color = PortalText)
                    }
                } else {
                    OutlinedButton(
                        onClick = { familyViewModel.resetForm(); onNavigateBack() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar", color = PortalText)
                    }
                }

                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else if (currentStep < 4) {
                    Button(
                        onClick = {
                            familyViewModel.nextStep()
                        },
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
        Dialog(onDismissRequest = { familyViewModel.resetForm(); onNavigateBack() }) {
            GlassCard {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SaseGreen, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Pre-registro enviado", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PortalText)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Guarda este folio/código para que Secretaría pueda localizar tu trámite:", textAlign = TextAlign.Center, color = PortalMuted, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PortalCardBg, RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(submittedFolio!!, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = SaseBlue)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Este folio no es matrícula ni inscripción oficial.",
                            fontSize = 12.sp, color = PortalText, fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Preséntate en Secretaría con documentos originales y copias, o proporciona este código para que localicen tu pre-registro.",
                            textAlign = TextAlign.Center, fontSize = 11.sp, color = PortalMuted
                        )
                        Text(
                            text = "Secretaría revisará, podrá solicitar correcciones y continuará con el registro o alta oficial si procede.",
                            textAlign = TextAlign.Center, fontSize = 11.sp, color = PortalMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .background(SaseOrange.copy(alpha = 0.1f)).padding(8.dp)
                        ) {
                            Text(
                                "No se ha generado matrícula oficial.",
                                textAlign = TextAlign.Center, fontSize = 11.sp, color = SaseOrange, fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { familyViewModel.resetForm(); onNavigateBack() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SaseNavy),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Nueva solicitud", color = Color.White, fontSize = 12.sp) }
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
        Text(title, fontSize = 9.sp, color = if (isActive) PortalText else PortalMuted)
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
    val apellidoPat by vm.apellidoPaterno.collectAsState()
    val apellidoMat by vm.apellidoMaterno.collectAsState()
    val nombreSolo by vm.nombre.collectAsState()
    val curp by vm.curp.collectAsState()
    val diaNac by vm.diaNacimiento.collectAsState()
    val mesNac by vm.mesNacimiento.collectAsState()
    val anioNac by vm.anioNacimiento.collectAsState()
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

    Text("Datos del Alumno", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PortalText)
    Text("Ingresa los datos tal y como aparecen en el acta de nacimiento.", fontSize = 12.sp, color = PortalMuted)

    // Tipo de tramite
    Text("Tipo de tramite", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PortalText)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf("Nuevo Ingreso", "Reinscripcion").forEach { opt ->
            val selected = tipoTramite == opt
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) SaseNavy else PortalCardBg)
                    .clickable { vm.setTipoTramite(opt) }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(opt, color = if (selected) Color.White else PortalText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }

    // Grado solicitado
    Text("Grado solicitado", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PortalText)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        (1..3).forEach { g ->
            val selected = grado == g
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) SaseBlue else PortalCardBg)
                    .border(if (errors.containsKey("grado")) 2.dp else 0.dp, SaseRed, RoundedCornerShape(10.dp))
                    .clickable { vm.setGradoSolicitado(g) }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("${g}° Grado", color = if (selected) Color.White else PortalText, fontWeight = FontWeight.Bold)
            }
        }
    }

    FormField("Apellido paterno", apellidoPat, { vm.setApellidoPaterno(it) }, errors.containsKey("nombre"), "Obligatorio")
    FormField("Apellido materno", apellidoMat, { vm.setApellidoMaterno(it) }, false, null)
    FormField("Nombre(s)", nombreSolo, { vm.setNombre(it) }, errors.containsKey("nombre"), "Obligatorio")
    FormField("CURP (18 caracteres)", curp, { vm.setCurp(it) }, errors.containsKey("curp"), "18 caracteres requeridos")

    Text("Fecha de nacimiento", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PortalText)
    val dias = (1..31).map { it.toString().padStart(2, '0') }
    val meses = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
    val anios = (2015 downTo 2000).map { it.toString() }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        DropdownField("Día", diaNac, dias, { vm.setDiaNacimiento(it) }, modifier = Modifier.weight(1f))
        DropdownField("Mes", mesNac, meses, { vm.setMesNacimiento(it) }, modifier = Modifier.weight(1f))
        DropdownField("Año", anioNac, anios, { vm.setAnioNacimiento(it) }, modifier = Modifier.weight(1f))
    }

    // Sexo selector
    Text("Sexo (segun acta)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PortalText)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf("Femenino", "Masculino").forEach { opt ->
            val selected = sexo == opt
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) SaseViolet else PortalCardBg)
                    .clickable { vm.setSexo(opt) }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(opt, color = if (selected) Color.White else PortalText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }

    FormField("Nacionalidad", nacionalidad, { vm.setNacionalidad(it) }, false, null)
    FormField("Entidad de nacimiento", entidadNac, { vm.setEntidadNacimiento(it) }, false, null)

    Text("Domicilio", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PortalText)
    FormField("Domicilio (calle, colonia, municipio, estado)", domicilio, { vm.setDomicilio(it) }, false, null)

    Text("Contacto", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PortalText)
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
            .background(if (errors.containsKey("aviso")) SaseRed.copy(alpha = 0.08f) else PortalCardBg)
            .padding(10.dp)
    ) {
        Checkbox(
            checked = acepta,
            onCheckedChange = { vm.setAceptaAvisoPrivacidad(it) }
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Acepto el Aviso de Privacidad y el uso de mis datos para fines escolares.", fontSize = 12.sp, color = PortalText)
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

// ── STEP 2: Contactos ───────────────────────────────────────────

@Composable
private fun StepContactos(vm: PreApplicationViewModel) {
    val responsableNombre by vm.responsableNombre.collectAsState()
    val responsableParentesco by vm.responsableParentesco.collectAsState()
    val responsableTelefono by vm.responsableTelefono.collectAsState()
    val responsableCorreo by vm.responsableCorreo.collectAsState()
    val responsableVive by vm.responsableViveConAlumno.collectAsState()
    val responsablePuede by vm.responsablePuedeRecoger.collectAsState()
    val autorizados by vm.autorizados.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    Text("Responsable Principal", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PortalText)
    Text("Registra a la madre, padre o tutor legal.", fontSize = 12.sp, color = PortalMuted)

    OutlinedTextField(
        value = responsableNombre,
        onValueChange = { vm.setResponsableNombre(it) },
        label = { Text("Nombre completo del responsable") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(6.dp))

    Text("Parentesco", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PortalText)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        listOf("Madre", "Padre", "Tutor", "Otro").forEach { opt ->
            val selected = responsableParentesco == opt
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) SaseBlue else PortalCardBg)
                    .clickable { vm.setResponsableParentesco(opt) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(opt, color = if (selected) Color.White else PortalText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
        value = responsableTelefono,
        onValueChange = { vm.setResponsableTelefono(it) },
        label = { Text("Telefono (10 digitos)") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
        value = responsableCorreo,
        onValueChange = { vm.setResponsableCorreo(it) },
        label = { Text("Correo electronico (opcional)") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = responsableVive, onCheckedChange = { vm.setResponsableViveConAlumno(it) })
        Text("Vive con el alumno", fontSize = 13.sp, color = PortalText)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = responsablePuede, onCheckedChange = { vm.setResponsablePuedeRecoger(it) })
        Text("Puede recoger al alumno", fontSize = 13.sp, color = PortalText)
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text("Autorizados para Recoger", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PortalText)
    Text("Personas adicionales autorizadas para recoger al alumno.", fontSize = 11.sp, color = PortalMuted)

    autorizados.forEach { a ->
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(PortalCardBg).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(a.nombre, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PortalText)
                Text("${a.parentesco} — ${a.telefono}", fontSize = 10.sp, color = PortalMuted)
            }
            IconButton(onClick = { vm.removeAutorizado(a.id) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = SaseRed, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }

    OutlinedButton(
        onClick = { showAddDialog = true },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Agregar autorizado", color = SaseBlue)
    }

    if (showAddDialog) {
        AutorizadoDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { nombre, parentesco, telefono ->
                vm.addAutorizado(nombre, parentesco, telefono)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AutorizadoDialog(
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, parentesco: String, telefono: String) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var parentesco by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Agregar Autorizado", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PortalText)

                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre completo") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = parentesco, onValueChange = { parentesco = it }, label = { Text("Parentesco") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = telefono, onValueChange = { telefono = it.take(10) }, label = { Text("Telefono (10 digitos)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar", color = PortalText) }
                    Button(
                        onClick = { onConfirm(nombre, parentesco, telefono) },
                        enabled = nombre.isNotBlank() && parentesco.isNotBlank() && telefono.length == 10,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Agregar", color = Color.White) }
                }
            }
        }
    }
}

// ── STEP 3: Contexto declarativo familiar ──────────────────────

@Composable
private fun StepContextoFamiliar(vm: PreApplicationViewModel) {
    val servicioMedico by vm.servicioMedico.collectAsState()
    val numeroAfiliacionPoliza by vm.numeroAfiliacionPoliza.collectAsState()
    val tipoSangre by vm.tipoSangre.collectAsState()
    val tieneAlergias by vm.tieneAlergias.collectAsState()
    val alergiasDetalle by vm.alergiasDetalle.collectAsState()
    val tienePadecimientos by vm.tienePadecimientos.collectAsState()
    val padecimientosDetalle by vm.padecimientosDetalle.collectAsState()
    val tomaMedicamentos by vm.tomaMedicamentos.collectAsState()
    val medicamentosDetalle by vm.medicamentosDetalle.collectAsState()
    val restriccionActividadFisica by vm.restriccionActividadFisica.collectAsState()
    val restriccionActividadFisicaDetalle by vm.restriccionActividadFisicaDetalle.collectAsState()
    val usaLentes by vm.usaLentes.collectAsState()
    val dificultadVisualReferida by vm.dificultadVisualReferida.collectAsState()
    val dificultadVisualDetalle by vm.dificultadVisualDetalle.collectAsState()
    val dificultadAuditivaReferida by vm.dificultadAuditivaReferida.collectAsState()
    val dificultadAuditivaDetalle by vm.dificultadAuditivaDetalle.collectAsState()
    val saludBucalReferida by vm.saludBucalReferida.collectAsState()
    val cartillaVacunacionActualizada by vm.cartillaVacunacionActualizada.collectAsState()
    val viveConQuien by vm.viveConQuien.collectAsState()
    val tipoFamilia by vm.tipoFamilia.collectAsState()
    val hijoUnico by vm.hijoUnico.collectAsState()
    val lugarEntreHermanos by vm.lugarEntreHermanos.collectAsState()
    val hermanosEnEscuela by vm.hermanosEnEscuela.collectAsState()
    val integrantesHogar by vm.integrantesHogar.collectAsState()
    val principalSostenEconomico by vm.principalSostenEconomico.collectAsState()
    val ingresoFamiliarRango by vm.ingresoFamiliarRango.collectAsState()
    val tipoVivienda by vm.tipoVivienda.collectAsState()
    val serviciosBasicos by vm.serviciosBasicos.collectAsState()
    val internetCasa by vm.internetCasa.collectAsState()
    val dispositivoTareas by vm.dispositivoTareas.collectAsState()
    val becaApoyoSocial by vm.becaApoyoSocial.collectAsState()
    val medioTransporte by vm.medioTransporte.collectAsState()
    val dificultadComprarMateriales by vm.dificultadComprarMateriales.collectAsState()
    val personaAtiendeAvisos by vm.personaAtiendeAvisos.collectAsState()
    val horarioPreferenteComunicacion by vm.horarioPreferenteComunicacion.collectAsState()
    val puedeAcudirCitatorios by vm.puedeAcudirCitatorios.collectAsState()

    Text("Contexto Familiar", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PortalText)
    Text(
        "Esta información ayuda a la escuela a organizar apoyos y comunicación.",
        fontSize = 12.sp,
        color = PortalMuted
    )

    Text("Médico Escolar", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PortalText)
    Text(
        "La familia declara esta informacion. Médico Escolar la validara y complementara despues.",
        fontSize = 12.sp,
        color = PortalMuted
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SaseBlue.copy(alpha = 0.06f))
            .border(1.dp, SaseBlue.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            "Estos campos son opcionales y no bloquean el envio de la pre-solicitud.",
            fontSize = 11.sp,
            color = PortalText,
            fontWeight = FontWeight.Medium
        )
    }

    FormField("Servicio médico", servicioMedico, { vm.setServicioMedico(it) }, false, null)
    FormField("Número de afiliación/póliza", numeroAfiliacionPoliza, { vm.setNumeroAfiliacionPoliza(it) }, false, null)
    FormField("Tipo de sangre si se conoce", tipoSangre, { vm.setTipoSangre(it) }, false, null)

    DeclarativeCheckbox(
        label = "Alergias",
        checked = tieneAlergias,
        onCheckedChange = { vm.setTieneAlergias(it) }
    )
    if (tieneAlergias) {
        FormField("Detalle de alergias", alergiasDetalle, { vm.setAlergiasDetalle(it) }, false, null)
    }

    DeclarativeCheckbox(
        label = "Padecimientos relevantes",
        checked = tienePadecimientos,
        onCheckedChange = { vm.setTienePadecimientos(it) }
    )
    if (tienePadecimientos) {
        FormField("Detalle de padecimientos", padecimientosDetalle, { vm.setPadecimientosDetalle(it) }, false, null)
    }

    DeclarativeCheckbox(
        label = "Medicamentos prescritos",
        checked = tomaMedicamentos,
        onCheckedChange = { vm.setTomaMedicamentos(it) }
    )
    if (tomaMedicamentos) {
        FormField("Detalle de medicamentos prescritos", medicamentosDetalle, { vm.setMedicamentosDetalle(it) }, false, null)
    }

    DeclarativeCheckbox(
        label = "Restricción de actividad física",
        checked = restriccionActividadFisica,
        onCheckedChange = { vm.setRestriccionActividadFisica(it) }
    )
    if (restriccionActividadFisica) {
        FormField(
            "Detalle de restricción de actividad física",
            restriccionActividadFisicaDetalle,
            { vm.setRestriccionActividadFisicaDetalle(it) },
            false,
            null
        )
    }

    DeclarativeCheckbox(
        label = "Uso de lentes",
        checked = usaLentes,
        onCheckedChange = { vm.setUsaLentes(it) }
    )

    DeclarativeCheckbox(
        label = "Dificultad visual referida",
        checked = dificultadVisualReferida,
        onCheckedChange = { vm.setDificultadVisualReferida(it) }
    )
    if (dificultadVisualReferida) {
        FormField("Detalle de dificultad visual", dificultadVisualDetalle, { vm.setDificultadVisualDetalle(it) }, false, null)
    }

    DeclarativeCheckbox(
        label = "Dificultad auditiva referida",
        checked = dificultadAuditivaReferida,
        onCheckedChange = { vm.setDificultadAuditivaReferida(it) }
    )
    if (dificultadAuditivaReferida) {
        FormField("Detalle de dificultad auditiva", dificultadAuditivaDetalle, { vm.setDificultadAuditivaDetalle(it) }, false, null)
    }

    FormField("Salud bucal referida", saludBucalReferida, { vm.setSaludBucalReferida(it) }, false, null)

    DeclarativeCheckbox(
        label = "Cartilla de vacunación actualizada",
        checked = cartillaVacunacionActualizada,
        onCheckedChange = { vm.setCartillaVacunacionActualizada(it) }
    )

    Spacer(modifier = Modifier.height(8.dp))
    Text("Trabajo Social", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PortalText)
    Text(
        "La familia declara información general. Trabajo Social complementará después información especializada o reservada.",
        fontSize = 12.sp,
        color = PortalMuted
    )

    Text("¿Con quién vive el alumno?", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PortalText)
    listOf("Madre", "Padre", "Abuelos", "Tutor", "Otro").forEach { opt ->
        val checked = viveConQuien.split(",").map { it.trim() }.filter { it.isNotEmpty() }.contains(opt)
        DeclarativeCheckbox(opt, checked, {
            val current = viveConQuien.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
            if (it) current.add(opt) else current.remove(opt)
            vm.setViveConQuien(current.joinToString(", "))
        })
    }

    Text("Tipo de familia", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PortalText)
    DeclarativeCheckbox("Nuclear", tipoFamilia == "Nuclear", { vm.setTipoFamilia(if (it) "Nuclear" else "") })
    DeclarativeCheckbox("Extensa", tipoFamilia == "Extensa", { vm.setTipoFamilia(if (it) "Extensa" else "") })
    DeclarativeCheckbox("Monoparental", tipoFamilia == "Monoparental", { vm.setTipoFamilia(if (it) "Monoparental" else "") })
    DeclarativeCheckbox("Reconstituida", tipoFamilia == "Reconstituida", { vm.setTipoFamilia(if (it) "Reconstituida" else "") })
    DeclarativeCheckbox("Otra", tipoFamilia == "Otra", { vm.setTipoFamilia(if (it) "Otra" else "") })

    DeclarativeCheckbox("Hijo único", hijoUnico, { vm.setHijoUnico(it) })
    if (!hijoUnico) {
        FormField("Lugar entre hermanos", lugarEntreHermanos, { vm.setLugarEntreHermanos(it) }, false, null)
    }
    DeclarativeCheckbox("Hermanos en la escuela", hermanosEnEscuela, { vm.setHermanosEnEscuela(it) })
    FormField("Integrantes del hogar", integrantesHogar, { vm.setIntegrantesHogar(it) }, false, null)

    Text("Principal sostén económico", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PortalText)
    listOf("Madre", "Padre", "Ambos", "Otro").forEach { opt ->
        val checked = principalSostenEconomico.split(",").map { it.trim() }.filter { it.isNotEmpty() }.contains(opt)
        DeclarativeCheckbox(opt, checked, {
            val current = principalSostenEconomico.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
            if (it) current.add(opt) else current.remove(opt)
            vm.setPrincipalSostenEconomico(current.joinToString(", "))
        })
    }

    CompactOptionGroup(
        label = "Ingreso familiar por rangos",
        options = listOf("Hasta 1 SM", "1 a 2 SM", "2 a 4 SM", "Más de 4 SM", "Prefiero no responder"),
        selected = ingresoFamiliarRango,
        onSelect = { vm.setIngresoFamiliarRango(it) }
    )

    Text("Tipo de vivienda", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PortalText)
    DeclarativeCheckbox("Propia", tipoVivienda == "Propia", { vm.setTipoVivienda(if (it) "Propia" else "") })
    DeclarativeCheckbox("Rentada", tipoVivienda == "Rentada", { vm.setTipoVivienda(if (it) "Rentada" else "") })
    DeclarativeCheckbox("Prestada", tipoVivienda == "Prestada", { vm.setTipoVivienda(if (it) "Prestada" else "") })
    DeclarativeCheckbox("Compartida", tipoVivienda == "Compartida", { vm.setTipoVivienda(if (it) "Compartida" else "") })
    DeclarativeCheckbox("Otra", tipoVivienda == "Otra", { vm.setTipoVivienda(if (it) "Otra" else "") })

    DeclarativeCheckbox("Servicios básicos en casa", serviciosBasicos, { vm.setServiciosBasicos(it) })
    DeclarativeCheckbox("Internet en casa", internetCasa, { vm.setInternetCasa(it) })

    Text("Dispositivo para tareas", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PortalText)
    DeclarativeCheckbox("Computadora", dispositivoTareas == "Computadora", { vm.setDispositivoTareas(if (it) "Computadora" else "") })
    DeclarativeCheckbox("Tableta", dispositivoTareas == "Tableta", { vm.setDispositivoTareas(if (it) "Tableta" else "") })
    DeclarativeCheckbox("Teléfono", dispositivoTareas == "Teléfono", { vm.setDispositivoTareas(if (it) "Teléfono" else "") })
    DeclarativeCheckbox("Compartido", dispositivoTareas == "Compartido", { vm.setDispositivoTareas(if (it) "Compartido" else "") })
    DeclarativeCheckbox("No disponible", dispositivoTareas == "No disponible", { vm.setDispositivoTareas(if (it) "No disponible" else "") })

    FormField("Beca o apoyo social", becaApoyoSocial, { vm.setBecaApoyoSocial(it) }, false, null)

    Text("Medio de transporte", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PortalText)
    DeclarativeCheckbox("Camina", medioTransporte == "Camina", { vm.setMedioTransporte(if (it) "Camina" else "") })
    DeclarativeCheckbox("Transporte público", medioTransporte == "Transporte público", { vm.setMedioTransporte(if (it) "Transporte público" else "") })
    DeclarativeCheckbox("Auto familiar", medioTransporte == "Auto familiar", { vm.setMedioTransporte(if (it) "Auto familiar" else "") })
    DeclarativeCheckbox("Transporte escolar", medioTransporte == "Transporte escolar", { vm.setMedioTransporte(if (it) "Transporte escolar" else "") })
    DeclarativeCheckbox("Otro", medioTransporte == "Otro", { vm.setMedioTransporte(if (it) "Otro" else "") })

    DeclarativeCheckbox(
        "Dificultad para comprar materiales",
        dificultadComprarMateriales,
        { vm.setDificultadComprarMateriales(it) }
    )
    FormField("Persona que atiende avisos escolares", personaAtiendeAvisos, { vm.setPersonaAtiendeAvisos(it) }, false, null)
    CompactOptionGroup(
        label = "Horario preferente de comunicación",
        options = listOf("Mañana", "Tarde", "Noche", "Indistinto"),
        selected = horarioPreferenteComunicacion,
        onSelect = { vm.setHorarioPreferenteComunicacion(it) }
    )
    DeclarativeCheckbox("Posibilidad de acudir a citatorios", puedeAcudirCitatorios, { vm.setPuedeAcudirCitatorios(it) })

    Spacer(modifier = Modifier.height(8.dp))
    val isUdeii by vm.isUdeii.collectAsState()
    DeclarativeCheckbox("El alumno recibe o ha recibido apoyo UDEII (educación inclusiva)", isUdeii, { vm.setIsUdeii(it) })
    if (isUdeii) {
        UdeiiSection(vm)
    }
}

@Composable
private fun UdeiiSection(vm: PreApplicationViewModel) {
    val antecedenteApoyo by vm.udeiiAntecedenteApoyo.collectAsState()
    val terapiaLenguaje by vm.udeiiTerapiaLenguaje.collectAsState()
    val apoyoPsicologico by vm.udeiiApoyoPsicologico.collectAsState()
    val apoyoPedagogico by vm.udeiiApoyoPedagogico.collectAsState()
    val documentosDisponibles by vm.udeiiDocumentosDisponibles.collectAsState()
    val informeEscuelaAnterior by vm.udeiiInformeEscuelaAnterior.collectAsState()
    val evaluacionPsicopedagogica by vm.udeiiEvaluacionPsicopedagogica.collectAsState()
    val planIntervencion by vm.udeiiPlanIntervencion.collectAsState()
    val portafolio by vm.udeiiPortafolio.collectAsState()
    val observaciones by vm.udeiiObservaciones.collectAsState()

    Text("Apoyos Educativos Previos (UDEII)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PortalText)
    Text(
        "La familia declara antecedentes escolares. UDEII complementara despues la evaluacion funcional.",
        fontSize = 12.sp, color = PortalMuted
    )

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(SaseViolet.copy(alpha = 0.06f))
            .border(1.dp, SaseViolet.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text("Estos campos son opcionales y no bloquean el envio.", fontSize = 11.sp, color = PortalText, fontWeight = FontWeight.Medium)
    }

    Text("Antecedente de apoyo", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PortalText)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        listOf("USAER", "CAM", "Otro", "Ninguno").forEach { opt ->
            val selected = antecedenteApoyo == opt
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                    .background(if (selected) SaseViolet else PortalCardBg)
                    .clickable { vm.setUdeiiAntecedenteApoyo(opt) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(opt, color = if (selected) Color.White else PortalText,
                    fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }

    Spacer(modifier = Modifier.height(6.dp))
    Text("Apoyos recibidos", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PortalText)
    DeclarativeCheckbox("Terapia de lenguaje", terapiaLenguaje, { vm.setUdeiiTerapiaLenguaje(it) })
    DeclarativeCheckbox("Apoyo psicologico externo", apoyoPsicologico, { vm.setUdeiiApoyoPsicologico(it) })
    DeclarativeCheckbox("Apoyo pedagogico externo", apoyoPedagogico, { vm.setUdeiiApoyoPedagogico(it) })

    Spacer(modifier = Modifier.height(6.dp))
    FormField("Documentos disponibles (descripcion)", documentosDisponibles, { vm.setUdeiiDocumentosDisponibles(it) }, false, null)

    Spacer(modifier = Modifier.height(6.dp))
    Text("Documentacion educativa disponible", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PortalText)
    DeclarativeCheckbox("Informe de escuela anterior", informeEscuelaAnterior, { vm.setUdeiiInformeEscuelaAnterior(it) })
    DeclarativeCheckbox("Evaluacion psicopedagogica", evaluacionPsicopedagogica, { vm.setUdeiiEvaluacionPsicopedagogica(it) })
    DeclarativeCheckbox("Plan de intervencion", planIntervencion, { vm.setUdeiiPlanIntervencion(it) })
    DeclarativeCheckbox("Portafolio", portafolio, { vm.setUdeiiPortafolio(it) })

    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
        value = observaciones,
        onValueChange = { vm.setUdeiiObservaciones(it) },
        label = { Text("Observaciones familiares sobre aprendizaje") },
        modifier = Modifier.fillMaxWidth(),
        maxLines = 4
    )
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun DeclarativeCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PortalCardBg)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, color = PortalText, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CompactOptionGroup(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PortalText)
        options.chunked(3).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { option ->
                    val isSelected = selected == option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) SaseNavy else PortalCardBg)
                            .clickable { onSelect(option) }
                            .padding(horizontal = 8.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            option,
                            color = if (isSelected) Color.White else PortalText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ── STEP 4: Documentos y Consentimientos ────────────────────────────

@Composable
private fun StepDocumentos(vm: PreApplicationViewModel) {
    val documentos by vm.documentos.collectAsState()
    val consentimientos by vm.consentimientos.collectAsState()
    val errors by vm.errors.collectAsState()

    Text("Documentos", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PortalText)
    Text("Indica los documentos que presentaras en Secretaria para cotejo.", fontSize = 12.sp, color = PortalMuted)

    documentos.forEach { doc ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(PortalCardBg)
                .clickable { vm.toggleDocumento(doc.key) }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Checkbox(checked = doc.declarado, onCheckedChange = { vm.toggleDocumento(doc.key) })
            Spacer(modifier = Modifier.width(6.dp))
            Text(doc.label, fontSize = 12.sp, color = PortalText, fontWeight = FontWeight.Medium)
        }
    }

    Spacer(modifier = Modifier.height(4.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SaseOrange.copy(alpha = 0.08f))
            .padding(10.dp)
    ) {
        Text(
            "La entrega y cotejo documental se realizara en Secretaria.",
            fontSize = 11.sp, color = SaseOrange, fontWeight = FontWeight.Medium
        )
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text("Consentimientos", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PortalText)
    Text("Tu aceptacion es declarativa. Secretaria imprimira y recabara firma despues.", fontSize = 12.sp, color = PortalMuted)

    consentimientos.forEach { cons ->
        val hasError = errors.containsKey("consentimiento${cons.key.replaceFirstChar { it.uppercaseChar() }}")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (hasError) SaseRed.copy(alpha = 0.08f) else PortalCardBg)
                .clickable { vm.toggleConsentimiento(cons.key) }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Checkbox(checked = cons.aceptado, onCheckedChange = { vm.toggleConsentimiento(cons.key) })
            Spacer(modifier = Modifier.width(6.dp))
            Text(cons.label, fontSize = 12.sp, color = PortalText, fontWeight = FontWeight.Medium)
        }
    }

    if (errors.containsKey("consentimientoUsoDatos")) {
        Text("El consentimiento de uso de datos para expediente es obligatorio.", fontSize = 11.sp, color = SaseRed)
    }
    if (errors.containsKey("consentimientoCorresponsabilidad")) {
        Text("El consentimiento de corresponsabilidad familiar es obligatorio.", fontSize = 11.sp, color = SaseRed)
    }
}

// ── STEP 5: Resumen y Envio ─────────────────────────────────────

@Composable
private fun StepResumenEnvio(vm: PreApplicationViewModel) {
    val tipoTramite by vm.tipoTramite.collectAsState()
    val cicloEscolar by vm.cicloEscolar.collectAsState()
    val grado by vm.gradoSolicitado.collectAsState()
    val nombreCompleto by vm.nombreCompleto.collectAsState()
    val curp by vm.curp.collectAsState()
    val fechaNac by vm.fechaNacimiento.collectAsState()
    val sexo by vm.sexo.collectAsState()
    val telefono by vm.telefonoPrincipal.collectAsState()
    val correo by vm.correo.collectAsState()
    val acepta by vm.aceptaAvisoPrivacidad.collectAsState()
    val documentos by vm.documentos.collectAsState()
    val consentimientos by vm.consentimientos.collectAsState()
    val responsableNombre by vm.responsableNombre.collectAsState()
    val responsableParentesco by vm.responsableParentesco.collectAsState()

    Text("Resumen y Envio", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PortalText)
    Text("Revisa tus datos antes de enviar.", fontSize = 12.sp, color = PortalMuted)

    GlassCard {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SummaryLine("Tramite", tipoTramite)
            SummaryLine("Ciclo", cicloEscolar)
            SummaryLine("Grado", "${grado}°")
            SummaryLine("Nombre", nombreCompleto)
            SummaryLine("CURP", curp)
            if (responsableNombre.isNotBlank()) SummaryLine("Responsable", "$responsableNombre ($responsableParentesco)")
            SummaryLine("Telefono", telefono)
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    GlassCard {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val docsCount = documentos.count { it.declarado }
            SummaryLine("Documentos declarados", "$docsCount de ${documentos.size}")
            val reqs = listOf("usoDatos", "corresponsabilidad")
            val ok = reqs.all { k -> consentimientos.find { it.key == k }?.aceptado == true }
            SummaryLine("Consentimientos requeridos", if (ok) "Completos" else "Pendientes", if (ok) SaseGreen else SaseRed)
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(SaseOrange.copy(alpha = 0.08f))
            .padding(10.dp)
    ) {
        Text(
            "Esta pre-solicitud no confirma inscripcion oficial. Secretaria validara documentos, fotografias, firmas y datos oficiales.",
            fontSize = 11.sp, color = SaseOrange, fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SummaryLine(label: String, value: String, valueColor: Color = PortalText) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = PortalMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 11.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            textStyle = TextStyle(fontSize = 12.sp, color = PortalText),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = PortalText,
                unfocusedTextColor = PortalText,
                focusedBorderColor = SaseBlue,
                unfocusedBorderColor = SaseBorder,
                focusedLabelColor = SaseBlue,
                unfocusedLabelColor = PortalMuted
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 12.sp) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}


