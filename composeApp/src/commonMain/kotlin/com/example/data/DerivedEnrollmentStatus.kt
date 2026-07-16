package com.example.data

/**
 * Estado de inscripción derivable EXCLUSIVAMENTE desde los datos
 * estructurados de [Student] (padrón).
 *
 * Estados eliminados de forma deliberada (D3):
 * - CURP_CONFLICT: el padrón garantiza CURP única por invariante
 *   ([MockSaseData.addStudent] rechaza duplicados con
 *   [StudentAddResult.DuplicateCurp]); el conflicto pre-solicitud↔padrón
 *   tiene su fuente de verdad en PreApplicationViewModel.curpDuplicateInfo
 *   y se refleja en officialEnrollmentPendingItems. Representarlo aquí
 *   duplicaría la regla de dominio sin datos para evaluarla.
 * - PHOTOS_BLOCKED: las fotos viven en el flujo de pre-solicitud
 *   (_photos[folio]); Student no contiene fotos ni referencia a folio.
 *
 * Prioridad (dominio): un bloqueo documental impide avanzar antes que la
 * asignación de grupo; la alta oficial es estado terminal y prevalece
 * sobre cualquier bloqueo previo.
 */
enum class DerivedEnrollmentStatus(
    val label: String,
    val priority: Int,
    val readyForEnrollment: Boolean
) {
    DOCUMENTS_BLOCKED("Documentos pendientes", 1, false),
    GROUP_BLOCKED("Grupo por asignar", 2, false),
    READY_FOR_ENROLLMENT("Lista para alta oficial", 3, true),
    ENROLLED_WITHOUT_GROUP("Alta oficial sin grupo", 4, false),
    ENROLLED_WITH_GROUP("Alta oficial con grupo", 5, true)
}

/** Formato canónico de matrícula oficial (ver PermanentEnrollmentIdAllocator). */
private val OFFICIAL_ENROLLMENT_ID_FORMAT = Regex("^S310-[A-Z0-9]{10}-\\d{2}$")

private const val GROUP_UNASSIGNED = "Por asignar"

/**
 * Literal canónico usado por el padrón mock para una asignación de grupo
 * tentativa aún no confirmada (espejo de
 * OfficialStudentStatus.PENDIENTE_ASIGNACION_GRUPO). Comparación EXACTA,
 * no por contención, para evitar falsos positivos como
 * "Alta oficial con grupo". Puente temporal hasta que Student exponga la
 * confirmación de grupo como campo estructurado.
 */
private const val STATUS_GROUP_PENDING_CONFIRMATION = "Pendiente asignación manual de grupo"

fun deriveEnrollmentStatus(student: Student): DerivedEnrollmentStatus {
    val hasOfficialEnrollment = student.enrollmentId.isNotBlank()
    val hasCanonicalEnrollmentId = OFFICIAL_ENROLLMENT_ID_FORMAT.matches(student.enrollmentId)
    val groupAssigned = (student.group.isNotBlank()) && (student.group != GROUP_UNASSIGNED)
    val groupConfirmed = groupAssigned && (student.status != STATUS_GROUP_PENDING_CONFIRMATION)

    return when {
        (hasOfficialEnrollment && hasCanonicalEnrollmentId && groupAssigned) ->
            DerivedEnrollmentStatus.ENROLLED_WITH_GROUP

        hasOfficialEnrollment ->
            DerivedEnrollmentStatus.ENROLLED_WITHOUT_GROUP

        (student.documentationStatus != "Completa") ->
            DerivedEnrollmentStatus.DOCUMENTS_BLOCKED

        (!groupConfirmed) ->
            DerivedEnrollmentStatus.GROUP_BLOCKED

        else ->
            DerivedEnrollmentStatus.READY_FOR_ENROLLMENT
    }
}
