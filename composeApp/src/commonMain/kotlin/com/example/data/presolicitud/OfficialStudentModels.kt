package com.example.data.presolicitud

enum class OfficialStudentStatus(val label: String) {
    PROVISIONAL("Provisional"),
    EN_REVISION("En revisión"),
    DOCS_PENDIENTES("Documentos pendientes"),
    MEDICO_PENDIENTE("Validación Médica pendiente"),
    TS_PENDIENTE("Validación Trabajo Social pendiente"),
    UDEII_PENDIENTE("Validación UDEII pendiente"),
    LISTO_FIRMA("Listo para firma"),
    ALTA_OFICIAL_SIN_GRUPO("Alta oficial sin grupo"),
    PENDIENTE_ASIGNACION_GRUPO("Pendiente asignación de grupo"),
    ALTA_OFICIAL_CON_GRUPO("Alta oficial con grupo"),
    CERRADO("Cerrado")
}

data class OfficialStudent(
    val id: String, // UUID interno
    val preApplicationFolio: String, // Referencia a la pre-solicitud de origen
    val status: OfficialStudentStatus,
    val gradoIngreso: Int,
    val grupoSugerido: String?, // Sólo sugerido para 2° y 3°, para 1° queda nulo
    val grupoAsignado: String? = null,
    val curp: String,
    val alumnoNombreCompleto: String,
    val alumnoSexo: String = "", // H / M
    val alumnoEdad: Int = 0,
    val promedio: Double? = null,
    // La matrícula se genera con S310-[10 chars CURP]-G[Grado]
    // Sólo se asigna si pasa a Alta Oficial
    val matriculaOficial: String? = null,
    // Trazabilidad
    val creadoPor: String = "Secretaría",
    val fechaCreacion: String,
    
    // Validaciones de áreas especializadas (simplificado para la fase 1)
    val validacionSecretaria: ValidacionArea = ValidacionArea("Secretaría"),
    val validacionMedico: ValidacionArea = ValidacionArea("Médico Escolar"),
    val validacionTrabajoSocial: ValidacionArea = ValidacionArea("Trabajo Social"),
    val validacionUdeii: ValidacionArea = ValidacionArea("UDEII"),
    val validacionDireccion: ValidacionArea = ValidacionArea("Dirección")
) {
    companion object {
        fun generateMatricula(curp: String, grado: Int): String? {
            if (curp.length < 10) return null // Inválida o incompleta
            val prefix = curp.substring(0, 10).uppercase()
            return "S310-$prefix-G$grado"
        }
    }
}

data class ValidacionArea(
    val area: String,
    val validado: Boolean = false,
    val validadoPor: String? = null,
    val fechaValidacion: String? = null,
    val observaciones: String = ""
)
