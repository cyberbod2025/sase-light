package com.example.data.presolicitud

enum class PreApplicationStatus(val label: String) {
    BORRADOR("Borrador familiar"),
    ENVIADA("Enviada a revisión"),
    PENDIENTE_CORRECCION("Requiere corrección"),
    DUPLICADA("Posible duplicada"),
    ACEPTADA("Aceptada por Secretaría"),
    CANCELADA("Cancelada")
}

enum class ReadinessStatus(val label: String) {
    PENDING("Pendiente"),
    BLOCKED("Con pendientes"),
    READY("Lista para alta oficial"),
    CONVERTED("Convertida a alta oficial")
}

data class PreApplication(
    val folio: String,
    val status: PreApplicationStatus,
    val submittedAt: String?,
    // Bloque A: Pre-solicitud inicial
    val tramite: String,
    val cicloEscolar: String,
    val gradoSolicitado: Int,
    // Bloque B: Datos del Alumno
    val alumnoNombreCompleto: String,
    val alumnoCurp: String,
    val alumnoFechaNacimiento: String,
    val alumnoSexo: String,
    val alumnoNacionalidad: String,
    val alumnoEntidadNacimiento: String,
    val alumnoDomicilio: String,
    val alumnoTelefonoCasa: String,
    val escuelaProcedencia: String,
    val promedioGradoAnterior: Double? = null,
    val personaTramite: PersonaTramite = PersonaTramite(),
    // Bloque C: Responsables
    val responsables: List<Responsable>,
    // Bloque D: Autorizados para recoger
    val autorizados: List<AutorizadoPreSolicitud>,
    // Bloque E: Ficha Medica (Declarativa Familiar)
    val fichaMedicaFamiliar: FichaMedicaFamiliar,
    // Bloque F: Trabajo Social (Declarativa Familiar)
    val contextoSociofamiliar: ContextoSociofamiliar,
    // Bloque G: UDEII (Declarativa Familiar)
    val antecedentesUdeii: AntecedentesUdeii,
    // Bloque H: Documentos Declarados
    val documentosDeclarados: List<DocumentoDeclarado>,
    // Bloque I: Consentimientos
    val consentimientos: ConsentimientosFamiliares,
    // Bloque J: Revisión de Secretaría
    val observacionesSecretaria: String = "",
    val motivoCorreccion: String = "",
    val readinessStatus: ReadinessStatus = ReadinessStatus.PENDING,
    val readyAt: String? = null,
    val readinessNotes: String = ""
)

data class PersonaTramite(
    val nombreCompleto: String = "",
    val parentesco: String = "",
    val telefono: String = "",
    val identificacionPresentada: String = "",
    val usarComoContactoPrincipal: Boolean = false
)

data class Responsable(
    val nombreCompleto: String,
    val parentesco: String,
    val telefono: String,
    val correo: String?,
    val domicilioDistinto: Boolean,
    val domicilio: String?,
    val viveConAlumno: Boolean,
    val contactoPrincipal: Boolean,
    val puedeRecoger: Boolean,
    val ocupacion: String,
    val horarioContacto: String,
    val identificacionApresentar: String
)

data class AutorizadoPreSolicitud(
    val nombreCompleto: String,
    val parentesco: String,
    val telefono: String,
    val observaciones: String
)

data class FichaMedicaFamiliar(
    val servicioMedico: String,
    val numeroAfiliacion: String?,
    val tipoSangre: String?,
    val alergias: String,
    val padecimientos: String,
    val medicamentos: String,
    val restriccionFisica: String,
    val usaLentes: Boolean,
    val dificultadVisualAuditiva: String,
    val saludBucal: String,
    val cartillaVacunacion: Boolean
)

data class ContextoSociofamiliar(
    val viveConQuien: String,
    val tipoFamilia: String,
    val hijoUnico: Boolean,
    val lugarEntreHermanos: Int,
    val hermanosEnEscuela: Boolean,
    val integrantesHogar: Int,
    val sostenEconomico: String,
    val ingresoRangos: String,
    val tipoVivienda: String,
    val serviciosBásicos: Boolean,
    val internet: Boolean,
    val dispositivoTareas: String,
    val becaApoyo: String,
    val transporte: String,
    val dificultadMateriales: Boolean,
    val atiendeAvisos: String,
    val horarioComunicacion: String,
    val puedeAcudirCitatorios: Boolean
)

data class AntecedentesUdeii(
    val antecedenteApoyo: String, // UDEII, USAER, CAM, etc.
    val terapiaLenguaje: Boolean,
    val apoyoPsicologico: Boolean,
    val apoyoPedagogico: Boolean,
    val documentosDisponibles: String,
    val informeEscuelaAnterior: Boolean,
    val evaluacionPsicopedagogica: Boolean,
    val planIntervencion: Boolean,
    val portafolio: Boolean,
    val observacionesFamiliares: String
)

data class DocumentoDeclarado(
    val nombre: String,
    val declarado: Boolean,
    val cotejadoSecretaria: Boolean = false
)

data class ConsentimientosFamiliares(
    val avisoPrivacidad: Boolean,
    val usoDatosExpediente: Boolean,
    val fotoAlumno: Boolean,
    val fotoCredencial: Boolean,
    val fotoAutorizados: Boolean,
    val comunicacionWhatsapp: Boolean,
    val reglamentoInterno: Boolean,
    val marcoConvivencia: Boolean,
    val corresponsabilidadFamiliar: Boolean
)
