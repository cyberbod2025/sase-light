package com.example.viewmodel

import com.example.data.Student
import com.example.data.presolicitud.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class PreApplicationViewModel {
    companion object {
        private val _sharedPreApplications = MutableStateFlow(MockPreApplicationData.preApplications)
        val sharedPreApplications: StateFlow<List<PreApplication>> = _sharedPreApplications.asStateFlow()

        fun approvePreApplication(folio: String) {
            _sharedPreApplications.value = _sharedPreApplications.value.map {
                if (it.folio == folio) it.copy(status = PreApplicationStatus.ACEPTADA) else it
            }
        }

        fun markForCorrection(folio: String) {
            _sharedPreApplications.value = _sharedPreApplications.value.map {
                if (it.folio == folio) it.copy(status = PreApplicationStatus.PENDIENTE_CORRECCION) else it
            }
        }

        fun setObservaciones(folio: String, text: String) {
            _sharedPreApplications.value = _sharedPreApplications.value.map {
                if (it.folio == folio) it.copy(observacionesSecretaria = text) else it
            }
        }

        fun setMotivoCorreccion(folio: String, text: String) {
            _sharedPreApplications.value = _sharedPreApplications.value.map {
                if (it.folio == folio) it.copy(motivoCorreccion = text) else it
            }
        }

        fun notifyFamily(folio: String): String {
            val app = _sharedPreApplications.value.find { it.folio == folio } ?: return "Solicitud no encontrada"
            return when (app.status) {
                PreApplicationStatus.ACEPTADA -> "Notificación mock enviada: Solicitud ACEPTADA. Próximos pasos para inscripción."
                PreApplicationStatus.PENDIENTE_CORRECCION -> "Notificación mock enviada: Se requiere corrección. Motivo: ${app.motivoCorreccion.ifBlank { "No especificado" }}"
                PreApplicationStatus.CANCELADA -> "Notificación mock enviada: Solicitud cancelada."
                else -> "Notificación mock enviada: Estado actualizado a ${app.status.label}"
            }
        }

        fun buildProvisionalStudent(preApp: PreApplication): com.example.data.Student {
            val newId = "PROV-${preApp.folio.takeLast(4)}"
            val resp = preApp.responsables.firstOrNull()
            return com.example.data.Student(
                id = newId,
                fullName = preApp.alumnoNombreCompleto,
                group = "${preApp.gradoSolicitado}°",
                enrollmentId = "PRE-${preApp.folio.takeLast(4)}",
                curp = preApp.alumnoCurp,
                status = "Alta pendiente",
                tutorName = resp?.nombreCompleto ?: "",
                tutorRelation = resp?.parentesco ?: "",
                tutorPhone = resp?.telefono ?: "",
                tutorEmail = resp?.correo ?: "",
                birthDate = preApp.alumnoFechaNacimiento,
                address = preApp.alumnoDomicilio,
                documentationStatus = "Pendiente"
            )
        }
    }

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    // Block A — Pre-solicitud
    private val _tipoTramite = MutableStateFlow("Nuevo Ingreso")
    val tipoTramite: StateFlow<String> = _tipoTramite.asStateFlow()

    private val _cicloEscolar = MutableStateFlow("2025-2026")
    val cicloEscolar: StateFlow<String> = _cicloEscolar.asStateFlow()

    private val _gradoSolicitado = MutableStateFlow(0)
    val gradoSolicitado: StateFlow<Int> = _gradoSolicitado.asStateFlow()

    private val _nombreCompleto = MutableStateFlow("")
    val nombreCompleto: StateFlow<String> = _nombreCompleto.asStateFlow()

    private val _curp = MutableStateFlow("")
    val curp: StateFlow<String> = _curp.asStateFlow()

    private val _fechaNacimiento = MutableStateFlow("")
    val fechaNacimiento: StateFlow<String> = _fechaNacimiento.asStateFlow()

    private val _sexo = MutableStateFlow("")
    val sexo: StateFlow<String> = _sexo.asStateFlow()

    private val _nacionalidad = MutableStateFlow("Mexicana")
    val nacionalidad: StateFlow<String> = _nacionalidad.asStateFlow()

    private val _entidadNacimiento = MutableStateFlow("")
    val entidadNacimiento: StateFlow<String> = _entidadNacimiento.asStateFlow()

    private val _telefonoPrincipal = MutableStateFlow("")
    val telefonoPrincipal: StateFlow<String> = _telefonoPrincipal.asStateFlow()

    private val _correo = MutableStateFlow("")
    val correo: StateFlow<String> = _correo.asStateFlow()

    private val _escuelaProcedencia = MutableStateFlow("")
    val escuelaProcedencia: StateFlow<String> = _escuelaProcedencia.asStateFlow()

    private val _aceptaAvisoPrivacidad = MutableStateFlow(false)
    val aceptaAvisoPrivacidad: StateFlow<Boolean> = _aceptaAvisoPrivacidad.asStateFlow()

    // Block B — Domicilio
    private val _domicilio = MutableStateFlow("")
    val domicilio: StateFlow<String> = _domicilio.asStateFlow()

    private val _telefonoCasa = MutableStateFlow("")
    val telefonoCasa: StateFlow<String> = _telefonoCasa.asStateFlow()

    // Block C — Responsable principal
    private val _responsableNombre = MutableStateFlow("")
    val responsableNombre: StateFlow<String> = _responsableNombre.asStateFlow()

    private val _responsableParentesco = MutableStateFlow("")
    val responsableParentesco: StateFlow<String> = _responsableParentesco.asStateFlow()

    private val _responsableTelefono = MutableStateFlow("")
    val responsableTelefono: StateFlow<String> = _responsableTelefono.asStateFlow()

    private val _responsableCorreo = MutableStateFlow("")
    val responsableCorreo: StateFlow<String> = _responsableCorreo.asStateFlow()

    private val _responsableViveConAlumno = MutableStateFlow(true)
    val responsableViveConAlumno: StateFlow<Boolean> = _responsableViveConAlumno.asStateFlow()

    private val _responsablePuedeRecoger = MutableStateFlow(true)
    val responsablePuedeRecoger: StateFlow<Boolean> = _responsablePuedeRecoger.asStateFlow()

    // Block D — Autorizados para recoger
    data class AutorizadoItem(val id: String, val nombre: String, val parentesco: String, val telefono: String)

    private val _autorizados = MutableStateFlow<List<AutorizadoItem>>(emptyList())
    val autorizados: StateFlow<List<AutorizadoItem>> = _autorizados.asStateFlow()

    // Block E — Médico Escolar declarativo familiar
    private val _servicioMedico = MutableStateFlow("")
    val servicioMedico: StateFlow<String> = _servicioMedico.asStateFlow()

    private val _numeroAfiliacionPoliza = MutableStateFlow("")
    val numeroAfiliacionPoliza: StateFlow<String> = _numeroAfiliacionPoliza.asStateFlow()

    private val _tipoSangre = MutableStateFlow("")
    val tipoSangre: StateFlow<String> = _tipoSangre.asStateFlow()

    private val _tieneAlergias = MutableStateFlow(false)
    val tieneAlergias: StateFlow<Boolean> = _tieneAlergias.asStateFlow()

    private val _alergiasDetalle = MutableStateFlow("")
    val alergiasDetalle: StateFlow<String> = _alergiasDetalle.asStateFlow()

    private val _tienePadecimientos = MutableStateFlow(false)
    val tienePadecimientos: StateFlow<Boolean> = _tienePadecimientos.asStateFlow()

    private val _padecimientosDetalle = MutableStateFlow("")
    val padecimientosDetalle: StateFlow<String> = _padecimientosDetalle.asStateFlow()

    private val _tomaMedicamentos = MutableStateFlow(false)
    val tomaMedicamentos: StateFlow<Boolean> = _tomaMedicamentos.asStateFlow()

    private val _medicamentosDetalle = MutableStateFlow("")
    val medicamentosDetalle: StateFlow<String> = _medicamentosDetalle.asStateFlow()

    private val _restriccionActividadFisica = MutableStateFlow(false)
    val restriccionActividadFisica: StateFlow<Boolean> = _restriccionActividadFisica.asStateFlow()

    private val _restriccionActividadFisicaDetalle = MutableStateFlow("")
    val restriccionActividadFisicaDetalle: StateFlow<String> = _restriccionActividadFisicaDetalle.asStateFlow()

    private val _usaLentes = MutableStateFlow(false)
    val usaLentes: StateFlow<Boolean> = _usaLentes.asStateFlow()

    private val _dificultadVisualReferida = MutableStateFlow(false)
    val dificultadVisualReferida: StateFlow<Boolean> = _dificultadVisualReferida.asStateFlow()

    private val _dificultadVisualDetalle = MutableStateFlow("")
    val dificultadVisualDetalle: StateFlow<String> = _dificultadVisualDetalle.asStateFlow()

    private val _dificultadAuditivaReferida = MutableStateFlow(false)
    val dificultadAuditivaReferida: StateFlow<Boolean> = _dificultadAuditivaReferida.asStateFlow()

    private val _dificultadAuditivaDetalle = MutableStateFlow("")
    val dificultadAuditivaDetalle: StateFlow<String> = _dificultadAuditivaDetalle.asStateFlow()

    private val _saludBucalReferida = MutableStateFlow("")
    val saludBucalReferida: StateFlow<String> = _saludBucalReferida.asStateFlow()

    private val _cartillaVacunacionActualizada = MutableStateFlow(false)
    val cartillaVacunacionActualizada: StateFlow<Boolean> = _cartillaVacunacionActualizada.asStateFlow()

    // Block F — Trabajo Social declarativo familiar
    private val _viveConQuien = MutableStateFlow("")
    val viveConQuien: StateFlow<String> = _viveConQuien.asStateFlow()

    private val _tipoFamilia = MutableStateFlow("")
    val tipoFamilia: StateFlow<String> = _tipoFamilia.asStateFlow()

    private val _hijoUnico = MutableStateFlow(false)
    val hijoUnico: StateFlow<Boolean> = _hijoUnico.asStateFlow()

    private val _lugarEntreHermanos = MutableStateFlow("")
    val lugarEntreHermanos: StateFlow<String> = _lugarEntreHermanos.asStateFlow()

    private val _hermanosEnEscuela = MutableStateFlow(false)
    val hermanosEnEscuela: StateFlow<Boolean> = _hermanosEnEscuela.asStateFlow()

    private val _integrantesHogar = MutableStateFlow("")
    val integrantesHogar: StateFlow<String> = _integrantesHogar.asStateFlow()

    private val _principalSostenEconomico = MutableStateFlow("")
    val principalSostenEconomico: StateFlow<String> = _principalSostenEconomico.asStateFlow()

    private val _ingresoFamiliarRango = MutableStateFlow("")
    val ingresoFamiliarRango: StateFlow<String> = _ingresoFamiliarRango.asStateFlow()

    private val _tipoVivienda = MutableStateFlow("")
    val tipoVivienda: StateFlow<String> = _tipoVivienda.asStateFlow()

    private val _serviciosBasicos = MutableStateFlow(false)
    val serviciosBasicos: StateFlow<Boolean> = _serviciosBasicos.asStateFlow()

    private val _internetCasa = MutableStateFlow(false)
    val internetCasa: StateFlow<Boolean> = _internetCasa.asStateFlow()

    private val _dispositivoTareas = MutableStateFlow("")
    val dispositivoTareas: StateFlow<String> = _dispositivoTareas.asStateFlow()

    private val _becaApoyoSocial = MutableStateFlow("")
    val becaApoyoSocial: StateFlow<String> = _becaApoyoSocial.asStateFlow()

    private val _medioTransporte = MutableStateFlow("")
    val medioTransporte: StateFlow<String> = _medioTransporte.asStateFlow()

    private val _dificultadComprarMateriales = MutableStateFlow(false)
    val dificultadComprarMateriales: StateFlow<Boolean> = _dificultadComprarMateriales.asStateFlow()

    private val _personaAtiendeAvisos = MutableStateFlow("")
    val personaAtiendeAvisos: StateFlow<String> = _personaAtiendeAvisos.asStateFlow()

    private val _horarioPreferenteComunicacion = MutableStateFlow("")
    val horarioPreferenteComunicacion: StateFlow<String> = _horarioPreferenteComunicacion.asStateFlow()

    private val _puedeAcudirCitatorios = MutableStateFlow(false)
    val puedeAcudirCitatorios: StateFlow<Boolean> = _puedeAcudirCitatorios.asStateFlow()

    // ── UDEII (Step 3) ──
    private val _udeiiAntecedenteApoyo = MutableStateFlow("")
    val udeiiAntecedenteApoyo: StateFlow<String> = _udeiiAntecedenteApoyo.asStateFlow()

    private val _udeiiTerapiaLenguaje = MutableStateFlow(false)
    val udeiiTerapiaLenguaje: StateFlow<Boolean> = _udeiiTerapiaLenguaje.asStateFlow()

    private val _udeiiApoyoPsicologico = MutableStateFlow(false)
    val udeiiApoyoPsicologico: StateFlow<Boolean> = _udeiiApoyoPsicologico.asStateFlow()

    private val _udeiiApoyoPedagogico = MutableStateFlow(false)
    val udeiiApoyoPedagogico: StateFlow<Boolean> = _udeiiApoyoPedagogico.asStateFlow()

    private val _udeiiDocumentosDisponibles = MutableStateFlow("")
    val udeiiDocumentosDisponibles: StateFlow<String> = _udeiiDocumentosDisponibles.asStateFlow()

    private val _udeiiInformeEscuelaAnterior = MutableStateFlow(false)
    val udeiiInformeEscuelaAnterior: StateFlow<Boolean> = _udeiiInformeEscuelaAnterior.asStateFlow()

    private val _udeiiEvaluacionPsicopedagogica = MutableStateFlow(false)
    val udeiiEvaluacionPsicopedagogica: StateFlow<Boolean> = _udeiiEvaluacionPsicopedagogica.asStateFlow()

    private val _udeiiPlanIntervencion = MutableStateFlow(false)
    val udeiiPlanIntervencion: StateFlow<Boolean> = _udeiiPlanIntervencion.asStateFlow()

    private val _udeiiPortafolio = MutableStateFlow(false)
    val udeiiPortafolio: StateFlow<Boolean> = _udeiiPortafolio.asStateFlow()

    private val _udeiiObservaciones = MutableStateFlow("")
    val udeiiObservaciones: StateFlow<String> = _udeiiObservaciones.asStateFlow()

    // ── Documentos declarativos (Step 4) ──
    data class DocumentoItem(val key: String, val label: String, val declarado: Boolean = false)

    private val _documentos = MutableStateFlow(listOf(
        DocumentoItem("actaNacimiento", "Acta de nacimiento"),
        DocumentoItem("curpDoc", "CURP"),
        DocumentoItem("boleta", "Boleta / Certificado"),
        DocumentoItem("comprobanteDomicilio", "Comprobante de domicilio"),
        DocumentoItem("ineResponsable", "INE del responsable"),
        DocumentoItem("documentoMedico", "Documento médico (si aplica)"),
        DocumentoItem("documentoUdeii", "Documento UDEII/USAER/CAM (si aplica)"),
        DocumentoItem("custodia", "Resolución de custodia (si aplica)"),
        DocumentoItem("otro", "Otro documento relevante")
    ))
    val documentos: StateFlow<List<DocumentoItem>> = _documentos.asStateFlow()

    fun toggleDocumento(key: String) {
        _documentos.value = _documentos.value.map { if (it.key == key) it.copy(declarado = !it.declarado) else it }
    }

    // ── Consentimientos (Step 4) ──
    data class ConsentimientoItem(val key: String, val label: String, val aceptado: Boolean = false)

    private val _consentimientos = MutableStateFlow(listOf(
        ConsentimientoItem("usoDatos", "Uso de datos para expediente"),
        ConsentimientoItem("fotoAlumno", "Fotografía del alumno"),
        ConsentimientoItem("fotoCredencial", "Fotografía para credencial"),
        ConsentimientoItem("fotoAutorizados", "Fotografía de autorizados"),
        ConsentimientoItem("comunicacion", "Comunicación por WhatsApp/teléfono/correo"),
        ConsentimientoItem("reglamento", "Conocimiento del Reglamento Escolar Interno"),
        ConsentimientoItem("marcoConvivencia", "Conocimiento del Marco para la Convivencia"),
        ConsentimientoItem("corresponsabilidad", "Corresponsabilidad familiar")
    ))
    val consentimientos: StateFlow<List<ConsentimientoItem>> = _consentimientos.asStateFlow()

    fun toggleConsentimiento(key: String) {
        _consentimientos.value = _consentimientos.value.map { if (it.key == key) it.copy(aceptado = !it.aceptado) else it }
    }

    // Validation errors
    private val _errors = MutableStateFlow<Map<String, String>>(emptyMap())
    val errors: StateFlow<Map<String, String>> = _errors.asStateFlow()

    // Submission
    private val _submittedFolio = MutableStateFlow<String?>(null)
    val submittedFolio: StateFlow<String?> = _submittedFolio.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    fun nextStep() {
        if (validateStep(_currentStep.value)) {
            if (_currentStep.value < 4) {
                _currentStep.value++
                _errors.value = emptyMap()
            }
        }
    }

    fun previousStep() {
        if (_currentStep.value > 0) {
            _currentStep.value--
            _errors.value = emptyMap()
        }
    }

    fun setTipoTramite(v: String) { _tipoTramite.value = v }
    fun setCicloEscolar(v: String) { _cicloEscolar.value = v }
    fun setGradoSolicitado(v: Int) { _gradoSolicitado.value = v }
    fun setNombreCompleto(v: String) { _nombreCompleto.value = v }
    fun setCurp(v: String) { _curp.value = v.uppercase().take(18) }
    fun setFechaNacimiento(v: String) { _fechaNacimiento.value = v }
    fun setSexo(v: String) { _sexo.value = v }
    fun setNacionalidad(v: String) { _nacionalidad.value = v }
    fun setEntidadNacimiento(v: String) { _entidadNacimiento.value = v }
    fun setTelefonoPrincipal(v: String) { _telefonoPrincipal.value = v.take(10) }
    fun setCorreo(v: String) { _correo.value = v }
    fun setEscuelaProcedencia(v: String) { _escuelaProcedencia.value = v }
    fun setAceptaAvisoPrivacidad(v: Boolean) { _aceptaAvisoPrivacidad.value = v }
    fun setDomicilio(v: String) { _domicilio.value = v }
    fun setTelefonoCasa(v: String) { _telefonoCasa.value = v.take(10) }

    fun setResponsableNombre(v: String) { _responsableNombre.value = v }
    fun setResponsableParentesco(v: String) { _responsableParentesco.value = v }
    fun setResponsableTelefono(v: String) { _responsableTelefono.value = v.take(10) }
    fun setResponsableCorreo(v: String) { _responsableCorreo.value = v }
    fun setResponsableViveConAlumno(v: Boolean) { _responsableViveConAlumno.value = v }
    fun setResponsablePuedeRecoger(v: Boolean) { _responsablePuedeRecoger.value = v }

    fun addAutorizado(nombre: String, parentesco: String, telefono: String) {
        val id = "AUT-${_autorizados.value.size + 1}-${Random.nextInt(100, 999)}"
        _autorizados.value = _autorizados.value + AutorizadoItem(id, nombre, parentesco, telefono.take(10))
    }

    fun removeAutorizado(id: String) {
        _autorizados.value = _autorizados.value.filter { it.id != id }
    }

    fun setServicioMedico(v: String) { _servicioMedico.value = v }
    fun setNumeroAfiliacionPoliza(v: String) { _numeroAfiliacionPoliza.value = v }
    fun setTipoSangre(v: String) { _tipoSangre.value = v }
    fun setTieneAlergias(v: Boolean) { _tieneAlergias.value = v }
    fun setAlergiasDetalle(v: String) { _alergiasDetalle.value = v }
    fun setTienePadecimientos(v: Boolean) { _tienePadecimientos.value = v }
    fun setPadecimientosDetalle(v: String) { _padecimientosDetalle.value = v }
    fun setTomaMedicamentos(v: Boolean) { _tomaMedicamentos.value = v }
    fun setMedicamentosDetalle(v: String) { _medicamentosDetalle.value = v }
    fun setRestriccionActividadFisica(v: Boolean) { _restriccionActividadFisica.value = v }
    fun setRestriccionActividadFisicaDetalle(v: String) { _restriccionActividadFisicaDetalle.value = v }
    fun setUsaLentes(v: Boolean) { _usaLentes.value = v }
    fun setDificultadVisualReferida(v: Boolean) { _dificultadVisualReferida.value = v }
    fun setDificultadVisualDetalle(v: String) { _dificultadVisualDetalle.value = v }
    fun setDificultadAuditivaReferida(v: Boolean) { _dificultadAuditivaReferida.value = v }
    fun setDificultadAuditivaDetalle(v: String) { _dificultadAuditivaDetalle.value = v }
    fun setSaludBucalReferida(v: String) { _saludBucalReferida.value = v }
    fun setCartillaVacunacionActualizada(v: Boolean) { _cartillaVacunacionActualizada.value = v }

    fun setViveConQuien(v: String) { _viveConQuien.value = v }
    fun setTipoFamilia(v: String) { _tipoFamilia.value = v }
    fun setHijoUnico(v: Boolean) { _hijoUnico.value = v }
    fun setLugarEntreHermanos(v: String) { _lugarEntreHermanos.value = v }
    fun setHermanosEnEscuela(v: Boolean) { _hermanosEnEscuela.value = v }
    fun setIntegrantesHogar(v: String) { _integrantesHogar.value = v.filter { it.isDigit() }.take(2) }
    fun setPrincipalSostenEconomico(v: String) { _principalSostenEconomico.value = v }
    fun setIngresoFamiliarRango(v: String) { _ingresoFamiliarRango.value = v }
    fun setTipoVivienda(v: String) { _tipoVivienda.value = v }
    fun setServiciosBasicos(v: Boolean) { _serviciosBasicos.value = v }
    fun setInternetCasa(v: Boolean) { _internetCasa.value = v }
    fun setDispositivoTareas(v: String) { _dispositivoTareas.value = v }
    fun setBecaApoyoSocial(v: String) { _becaApoyoSocial.value = v }
    fun setMedioTransporte(v: String) { _medioTransporte.value = v }
    fun setDificultadComprarMateriales(v: Boolean) { _dificultadComprarMateriales.value = v }
    fun setPersonaAtiendeAvisos(v: String) { _personaAtiendeAvisos.value = v }
    fun setHorarioPreferenteComunicacion(v: String) { _horarioPreferenteComunicacion.value = v }
    fun setPuedeAcudirCitatorios(v: Boolean) { _puedeAcudirCitatorios.value = v }

    fun setUdeiiAntecedenteApoyo(v: String) { _udeiiAntecedenteApoyo.value = v }
    fun setUdeiiTerapiaLenguaje(v: Boolean) { _udeiiTerapiaLenguaje.value = v }
    fun setUdeiiApoyoPsicologico(v: Boolean) { _udeiiApoyoPsicologico.value = v }
    fun setUdeiiApoyoPedagogico(v: Boolean) { _udeiiApoyoPedagogico.value = v }
    fun setUdeiiDocumentosDisponibles(v: String) { _udeiiDocumentosDisponibles.value = v }
    fun setUdeiiInformeEscuelaAnterior(v: Boolean) { _udeiiInformeEscuelaAnterior.value = v }
    fun setUdeiiEvaluacionPsicopedagogica(v: Boolean) { _udeiiEvaluacionPsicopedagogica.value = v }
    fun setUdeiiPlanIntervencion(v: Boolean) { _udeiiPlanIntervencion.value = v }
    fun setUdeiiPortafolio(v: Boolean) { _udeiiPortafolio.value = v }
    fun setUdeiiObservaciones(v: String) { _udeiiObservaciones.value = v }

    private fun validateStep(step: Int): Boolean {
        val errs = mutableMapOf<String, String>()
        when (step) {
            0 -> {
                if (_nombreCompleto.value.isBlank()) errs["nombre"] = "Obligatorio"
                if (_curp.value.length != 18) errs["curp"] = "CURP debe tener 18 caracteres"
                if (_fechaNacimiento.value.isBlank()) errs["fechaNac"] = "Obligatorio"
                if (_gradoSolicitado.value == 0) errs["grado"] = "Selecciona un grado"
                if (_telefonoPrincipal.value.length < 10) errs["telefono"] = "10 dígitos requeridos"
                if (!_aceptaAvisoPrivacidad.value) errs["aviso"] = "Debes aceptar el aviso de privacidad"
            }
            1 -> {
                if (_responsableNombre.value.isBlank()) errs["responsable"] = "Nombre del responsable obligatorio"
                if (_responsableParentesco.value.isBlank()) errs["parentesco"] = "Parentesco obligatorio"
                if (_responsableTelefono.value.length < 10) errs["responsableTel"] = "Teléfono 10 dígitos requerido"
            }
            4 -> {
                val usoDatos = _consentimientos.value.find { it.key == "usoDatos" }?.aceptado == true
                val corresponsabilidad = _consentimientos.value.find { it.key == "corresponsabilidad" }?.aceptado == true
                if (!usoDatos) errs["consentimientoUsoDatos"] = "Debes aceptar el uso de datos para expediente"
                if (!corresponsabilidad) errs["consentimientoCorresponsabilidad"] = "Debes aceptar la corresponsabilidad familiar"
            }
        }
        _errors.value = errs
        return errs.isEmpty()
    }

    fun submitApplication() {
        val errs = mutableMapOf<String, String>()
        if (_nombreCompleto.value.isBlank()) errs["nombre"] = "Obligatorio"
        if (_curp.value.length != 18) errs["curp"] = "CURP debe tener 18 caracteres"
        if (_fechaNacimiento.value.isBlank()) errs["fechaNac"] = "Obligatorio"
        if (_gradoSolicitado.value == 0) errs["grado"] = "Selecciona un grado"
        if (_telefonoPrincipal.value.length < 10) errs["telefono"] = "10 dígitos requeridos"
        if (!_aceptaAvisoPrivacidad.value) errs["aviso"] = "Debes aceptar el aviso de privacidad"
        if (_responsableNombre.value.isBlank()) errs["responsable"] = "Nombre del responsable obligatorio"
        if (_responsableParentesco.value.isBlank()) errs["parentesco"] = "Parentesco obligatorio"
        if (_responsableTelefono.value.length < 10) errs["responsableTel"] = "Teléfono 10 dígitos requerido"
        val usoDatos = _consentimientos.value.find { it.key == "usoDatos" }?.aceptado == true
        val corresponsabilidad = _consentimientos.value.find { it.key == "corresponsabilidad" }?.aceptado == true
        if (!usoDatos) errs["consentimientoUsoDatos"] = "Debes aceptar el uso de datos para expediente"
        if (!corresponsabilidad) errs["consentimientoCorresponsabilidad"] = "Debes aceptar la corresponsabilidad familiar"
        if (errs.isNotEmpty()) {
            _errors.value = errs
            _currentStep.value = when {
                errs.containsKey("consentimientoUsoDatos") || errs.containsKey("consentimientoCorresponsabilidad") -> 4
                errs.containsKey("responsable") || errs.containsKey("parentesco") || errs.containsKey("responsableTel") -> 1
                else -> 0
            }
            return
        }

        _isSubmitting.value = true

        // Build folio: PRE-310-XXXXXX
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val randomPart = (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        _submittedFolio.value = "PRE-310-$randomPart"

        _isSubmitting.value = false
    }

    fun resetForm() {
        _currentStep.value = 0
        _tipoTramite.value = "Nuevo Ingreso"
        _cicloEscolar.value = "2025-2026"
        _nombreCompleto.value = ""
        _curp.value = ""
        _fechaNacimiento.value = ""
        _gradoSolicitado.value = 0
        _sexo.value = ""
        _nacionalidad.value = "Mexicana"
        _entidadNacimiento.value = ""
        _telefonoPrincipal.value = ""
        _correo.value = ""
        _escuelaProcedencia.value = ""
        _aceptaAvisoPrivacidad.value = false
        _domicilio.value = ""
        _telefonoCasa.value = ""
        _responsableNombre.value = ""
        _responsableParentesco.value = ""
        _responsableTelefono.value = ""
        _responsableCorreo.value = ""
        _responsableViveConAlumno.value = true
        _responsablePuedeRecoger.value = true
        _autorizados.value = emptyList()
        _servicioMedico.value = ""
        _numeroAfiliacionPoliza.value = ""
        _tipoSangre.value = ""
        _tieneAlergias.value = false
        _alergiasDetalle.value = ""
        _tienePadecimientos.value = false
        _padecimientosDetalle.value = ""
        _tomaMedicamentos.value = false
        _medicamentosDetalle.value = ""
        _restriccionActividadFisica.value = false
        _restriccionActividadFisicaDetalle.value = ""
        _usaLentes.value = false
        _dificultadVisualReferida.value = false
        _dificultadVisualDetalle.value = ""
        _dificultadAuditivaReferida.value = false
        _dificultadAuditivaDetalle.value = ""
        _saludBucalReferida.value = ""
        _cartillaVacunacionActualizada.value = false
        _viveConQuien.value = ""
        _tipoFamilia.value = ""
        _hijoUnico.value = false
        _lugarEntreHermanos.value = ""
        _hermanosEnEscuela.value = false
        _integrantesHogar.value = ""
        _principalSostenEconomico.value = ""
        _ingresoFamiliarRango.value = ""
        _tipoVivienda.value = ""
        _serviciosBasicos.value = false
        _internetCasa.value = false
        _dispositivoTareas.value = ""
        _becaApoyoSocial.value = ""
        _medioTransporte.value = ""
        _dificultadComprarMateriales.value = false
        _personaAtiendeAvisos.value = ""
        _horarioPreferenteComunicacion.value = ""
        _puedeAcudirCitatorios.value = false
        _udeiiAntecedenteApoyo.value = ""
        _udeiiTerapiaLenguaje.value = false
        _udeiiApoyoPsicologico.value = false
        _udeiiApoyoPedagogico.value = false
        _udeiiDocumentosDisponibles.value = ""
        _udeiiInformeEscuelaAnterior.value = false
        _udeiiEvaluacionPsicopedagogica.value = false
        _udeiiPlanIntervencion.value = false
        _udeiiPortafolio.value = false
        _udeiiObservaciones.value = ""
        _documentos.value = _documentos.value.map { it.copy(declarado = false) }
        _consentimientos.value = _consentimientos.value.map { it.copy(aceptado = false) }
        _errors.value = emptyMap()
        _submittedFolio.value = null
    }
}
