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

@Composable
fun PreApplicationFamilyPortalScreen(viewModel: LabViewModel) {
    val familyViewModel = remember { PreApplicationViewModel() }
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
                    Text("Guarda este folio para continuar tu tramite:", textAlign = TextAlign.Center, color = SaseMuted, fontSize = 13.sp)
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

                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Guarda este folio.",
                            fontSize = 12.sp, color = SaseNavy, fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Presentate en Secretaria con documentos originales y copias.",
                            textAlign = TextAlign.Center, fontSize = 11.sp, color = SaseMuted
                        )
                        Text(
                            text = "Secretaria validara la informacion y finalizara el alta oficial si procede.",
                            textAlign = TextAlign.Center, fontSize = 11.sp, color = SaseMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .background(SaseOrange.copy(alpha = 0.1f)).padding(8.dp)
                        ) {
                            Text(
                                "No se ha generado matricula oficial.",
                                textAlign = TextAlign.Center, fontSize = 11.sp, color = SaseOrange, fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { familyViewModel.resetForm() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Nueva solicitud", color = SaseNavy, fontSize = 12.sp) }
                        Button(
                            onClick = {
                                familyViewModel.resetForm()
                                viewModel.navigateTo(Screen.SecretaryDashboard)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SaseNavy),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Volver al inicio", color = Color.White, fontSize = 12.sp) }
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

    Text("Responsable Principal", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SaseNavy)
    Text("Registra a la madre, padre o tutor legal.", fontSize = 12.sp, color = SaseMuted)

    OutlinedTextField(
        value = responsableNombre,
        onValueChange = { vm.setResponsableNombre(it) },
        label = { Text("Nombre completo del responsable") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(6.dp))

    Text("Parentesco", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SaseNavy)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        listOf("Madre", "Padre", "Tutor", "Otro").forEach { opt ->
            val selected = responsableParentesco == opt
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) SaseBlue else SaseBgSoft)
                    .clickable { vm.setResponsableParentesco(opt) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(opt, color = if (selected) Color.White else SaseNavy, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
        Text("Vive con el alumno", fontSize = 13.sp, color = SaseNavy)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = responsablePuede, onCheckedChange = { vm.setResponsablePuedeRecoger(it) })
        Text("Puede recoger al alumno", fontSize = 13.sp, color = SaseNavy)
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text("Autorizados para Recoger", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SaseNavy)
    Text("Personas adicionales autorizadas para recoger al alumno.", fontSize = 11.sp, color = SaseMuted)

    autorizados.forEach { a ->
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SaseBgSoft).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(a.nombre, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SaseNavy)
                Text("${a.parentesco} — ${a.telefono}", fontSize = 10.sp, color = SaseMuted)
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
                Text("Agregar Autorizado", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SaseNavy)

                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre completo") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = parentesco, onValueChange = { parentesco = it }, label = { Text("Parentesco") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = telefono, onValueChange = { telefono = it.take(10) }, label = { Text("Telefono (10 digitos)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar", color = SaseNavy) }
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

    Text("Contexto Familiar", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SaseNavy)
    Text(
        "Esta información ayuda a la escuela a organizar apoyos y comunicación.",
        fontSize = 12.sp,
        color = SaseMuted
    )

    Text("Médico Escolar", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SaseNavy)
    Text(
        "La familia declara esta informacion. Médico Escolar la validara y complementara despues.",
        fontSize = 12.sp,
        color = SaseMuted
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
            color = SaseNavy,
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
    Text("Trabajo Social", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SaseNavy)
    Text(
        "La familia declara información general. Trabajo Social complementará después información especializada o reservada.",
        fontSize = 12.sp,
        color = SaseMuted
    )

    FormField("Vive con quién", viveConQuien, { vm.setViveConQuien(it) }, false, null)
    CompactOptionGroup(
        label = "Tipo de familia",
        options = listOf("Nuclear", "Extensa", "Monoparental", "Reconstituida", "Otra"),
        selected = tipoFamilia,
        onSelect = { vm.setTipoFamilia(it) }
    )
    DeclarativeCheckbox("Hijo único", hijoUnico, { vm.setHijoUnico(it) })
    if (!hijoUnico) {
        FormField("Lugar entre hermanos", lugarEntreHermanos, { vm.setLugarEntreHermanos(it) }, false, null)
    }
    DeclarativeCheckbox("Hermanos en la escuela", hermanosEnEscuela, { vm.setHermanosEnEscuela(it) })
    FormField("Integrantes del hogar", integrantesHogar, { vm.setIntegrantesHogar(it) }, false, null)
    FormField("Principal sostén económico", principalSostenEconomico, { vm.setPrincipalSostenEconomico(it) }, false, null)
    CompactOptionGroup(
        label = "Ingreso familiar por rangos",
        options = listOf("Hasta 1 SM", "1 a 2 SM", "2 a 4 SM", "Más de 4 SM", "Prefiero no responder"),
        selected = ingresoFamiliarRango,
        onSelect = { vm.setIngresoFamiliarRango(it) }
    )
    CompactOptionGroup(
        label = "Tipo de vivienda",
        options = listOf("Propia", "Rentada", "Prestada", "Compartida", "Otra"),
        selected = tipoVivienda,
        onSelect = { vm.setTipoVivienda(it) }
    )
    DeclarativeCheckbox("Servicios básicos en casa", serviciosBasicos, { vm.setServiciosBasicos(it) })
    DeclarativeCheckbox("Internet en casa", internetCasa, { vm.setInternetCasa(it) })
    CompactOptionGroup(
        label = "Dispositivo para tareas",
        options = listOf("Computadora", "Tableta", "Teléfono", "Compartido", "No disponible"),
        selected = dispositivoTareas,
        onSelect = { vm.setDispositivoTareas(it) }
    )
    FormField("Beca o apoyo social", becaApoyoSocial, { vm.setBecaApoyoSocial(it) }, false, null)
    CompactOptionGroup(
        label = "Medio de transporte",
        options = listOf("Camina", "Transporte público", "Auto familiar", "Transporte escolar", "Otro"),
        selected = medioTransporte,
        onSelect = { vm.setMedioTransporte(it) }
    )
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
    UdeiiSection(vm)
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

    Text("Apoyos Educativos Previos (UDEII)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SaseNavy)
    Text(
        "La familia declara antecedentes escolares. UDEII complementara despues la evaluacion funcional.",
        fontSize = 12.sp, color = SaseMuted
    )

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(SaseViolet.copy(alpha = 0.06f))
            .border(1.dp, SaseViolet.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text("Estos campos son opcionales y no bloquean el envio.", fontSize = 11.sp, color = SaseNavy, fontWeight = FontWeight.Medium)
    }

    Text("Antecedente de apoyo", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SaseNavy)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        listOf("USAER", "CAM", "Otro", "Ninguno").forEach { opt ->
            val selected = antecedenteApoyo == opt
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                    .background(if (selected) SaseViolet else SaseBgSoft)
                    .clickable { vm.setUdeiiAntecedenteApoyo(opt) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(opt, color = if (selected) Color.White else SaseNavy,
                    fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }

    Spacer(modifier = Modifier.height(6.dp))
    Text("Apoyos recibidos", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SaseNavy)
    DeclarativeCheckbox("Terapia de lenguaje", terapiaLenguaje, { vm.setUdeiiTerapiaLenguaje(it) })
    DeclarativeCheckbox("Apoyo psicologico externo", apoyoPsicologico, { vm.setUdeiiApoyoPsicologico(it) })
    DeclarativeCheckbox("Apoyo pedagogico externo", apoyoPedagogico, { vm.setUdeiiApoyoPedagogico(it) })

    Spacer(modifier = Modifier.height(6.dp))
    FormField("Documentos disponibles (descripcion)", documentosDisponibles, { vm.setUdeiiDocumentosDisponibles(it) }, false, null)

    Spacer(modifier = Modifier.height(6.dp))
    Text("Documentacion educativa disponible", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SaseNavy)
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
            .background(SaseBgSoft)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, color = SaseNavy, fontWeight = FontWeight.Medium)
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
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SaseNavy)
        options.chunked(3).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { option ->
                    val isSelected = selected == option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) SaseNavy else SaseBgSoft)
                            .clickable { onSelect(option) }
                            .padding(horizontal = 8.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            option,
                            color = if (isSelected) Color.White else SaseNavy,
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

    Text("Documentos", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SaseNavy)
    Text("Indica los documentos que presentaras en Secretaria para cotejo.", fontSize = 12.sp, color = SaseMuted)

    documentos.forEach { doc ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SaseBgSoft)
                .clickable { vm.toggleDocumento(doc.key) }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Checkbox(checked = doc.declarado, onCheckedChange = { vm.toggleDocumento(doc.key) })
            Spacer(modifier = Modifier.width(6.dp))
            Text(doc.label, fontSize = 12.sp, color = SaseNavy, fontWeight = FontWeight.Medium)
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
    Text("Consentimientos", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SaseNavy)
    Text("Tu aceptacion es declarativa. Secretaria imprimira y recabara firma despues.", fontSize = 12.sp, color = SaseMuted)

    consentimientos.forEach { cons ->
        val hasError = errors.containsKey("consentimiento${cons.key.replaceFirstChar { it.uppercaseChar() }}")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (hasError) SaseRed.copy(alpha = 0.08f) else SaseBgSoft)
                .clickable { vm.toggleConsentimiento(cons.key) }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Checkbox(checked = cons.aceptado, onCheckedChange = { vm.toggleConsentimiento(cons.key) })
            Spacer(modifier = Modifier.width(6.dp))
            Text(cons.label, fontSize = 12.sp, color = SaseNavy, fontWeight = FontWeight.Medium)
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
    val nombre by vm.nombreCompleto.collectAsState()
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

    Text("Resumen y Envio", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SaseNavy)
    Text("Revisa tus datos antes de enviar.", fontSize = 12.sp, color = SaseMuted)

    GlassCard {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SummaryLine("Tramite", tipoTramite)
            SummaryLine("Ciclo", cicloEscolar)
            SummaryLine("Grado", "${grado}°")
            SummaryLine("Nombre", nombre)
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
