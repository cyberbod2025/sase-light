package com.example.viewmodel

import com.example.data.presolicitud.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class PreApplicationViewModel {

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
        }
        _errors.value = errs
        return errs.isEmpty()
    }

    fun submitApplication() {
        val success = validateStep(0)
        if (!success) {
            _currentStep.value = 0
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
        _errors.value = emptyMap()
        _submittedFolio.value = null
    }
}
