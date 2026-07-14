package com.example.viewmodel

import com.example.data.MockSaseData
import com.example.data.SaseAudit
import com.example.data.SaseDocument
import com.example.data.Student
import com.example.data.StudentAddResult
import com.example.data.enrollment.AnnualEnrollmentFlowCoordinator
import com.example.data.enrollment.EnrollmentFlowMode
import com.example.data.enrollment.AnnualEnrollmentFlowRequest
import com.example.data.enrollment.AnnualEnrollmentFlowResult
import com.example.data.enrollment.AnnualEnrollmentRecord
import com.example.data.enrollment.AnnualEnrollmentPersistenceAdapter
import com.example.data.enrollment.AnnualEnrollmentPersistenceResult
import com.example.data.enrollment.AnnualEnrollmentPlanningResult
import com.example.data.presolicitud.*
import com.example.formatTimestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

private const val FAMILY_LOOKUP_ERROR =
    "No fue posible consultar la pre-solicitud con los datos proporcionados."

internal fun interface InstitutionalPreApplicationSynchronizer {
    fun synchronize(
        source: PreApplication,
        readState: () -> List<PreApplication>,
        compareAndSet: (List<PreApplication>, List<PreApplication>) -> Boolean
    ): PreApplicationConversionResult
}

sealed class FamilyPreApplicationLookupResult {
    data class Success(
        val folio: String,
        val status: PreApplicationStatus,
        val correctionReason: String,
        val secretariaObservations: String
    ) : FamilyPreApplicationLookupResult()

    data class Error(
        val message: String = FAMILY_LOOKUP_ERROR
    ) : FamilyPreApplicationLookupResult()
}

sealed class FamilySubmissionResult {
    abstract val message: String

    data class Success(
        val preApplication: PreApplication,
        override val message: String = "Pre-solicitud enviada."
    ) : FamilySubmissionResult()

    data class DuplicateCurp(
        val curp: String,
        override val message: String = "Ya existe una pre-solicitud o alta oficial con esta CURP."
    ) : FamilySubmissionResult()

    data class DuplicateFolio(
        val folio: String,
        override val message: String = "Ya existe una pre-solicitud con este folio."
    ) : FamilySubmissionResult()

    data class InsufficientData(
        override val message: String
    ) : FamilySubmissionResult()
}

sealed class FamilyResubmissionResult {
    data class Success(
        val preApplication: PreApplication
    ) : FamilyResubmissionResult()

    data class NotFound(
        val folio: String
    ) : FamilyResubmissionResult()

    data class InvalidStatus(
        val folio: String,
        val status: PreApplicationStatus
    ) : FamilyResubmissionResult()

    data class DuplicateCurp(
        val curp: String
    ) : FamilyResubmissionResult()
}

sealed class OfficialEnrollmentResult {
    abstract val message: String

    data class Success(
        val officialStudent: OfficialStudent,
        val masterStudent: Student,
        val masterStudentCreated: Boolean,
        override val message: String = if (masterStudentCreated) {
            "Alta oficial iniciada y expediente maestro creado."
        } else {
            "Alta oficial iniciada y expediente maestro actualizado."
        }
    ) : OfficialEnrollmentResult()

    data class DuplicateFolio(
        val folio: String,
        override val message: String = "Esta pre-solicitud ya fue convertida en alta oficial."
    ) : OfficialEnrollmentResult()

    data class DuplicateCurp(
        val curp: String,
        override val message: String = "Ya existe un alumno con esta CURP."
    ) : OfficialEnrollmentResult()

    data class DuplicateMatricula(
        val matricula: String,
        override val message: String = "Ya existe un alumno con esta matrícula."
    ) : OfficialEnrollmentResult()

    data class NotReady(
        val pendingItems: List<String>,
        override val message: String = "La pre-solicitud aún no está lista para alta oficial."
    ) : OfficialEnrollmentResult()

    data class PreApplicationNotFound(
        val folio: String,
        override val message: String = "Pre-solicitud no encontrada."
    ) : OfficialEnrollmentResult()

    data class MasterStudentPropagationError(
        override val message: String
    ) : OfficialEnrollmentResult()

    data class Error(
        override val message: String
    ) : OfficialEnrollmentResult()
}

sealed class ReadinessResult {
    abstract val message: String

    data class Success(
        val preApplication: PreApplication,
        override val message: String = "Pre-solicitud declarada lista para alta oficial."
    ) : ReadinessResult()

    data class NotReady(
        val pendingItems: List<String>,
        override val message: String = "La pre-solicitud conserva pendientes bloqueantes."
    ) : ReadinessResult()

    data class AlreadyReady(
        val preApplication: PreApplication,
        override val message: String = "La pre-solicitud ya estaba lista para alta oficial."
    ) : ReadinessResult()

    data class AlreadyConverted(
        val preApplication: PreApplication,
        override val message: String = "La pre-solicitud ya fue convertida a alta oficial."
    ) : ReadinessResult()

    data class NotFound(
        val folio: String,
        override val message: String = "Pre-solicitud no encontrada."
    ) : ReadinessResult()

    data class Error(
        override val message: String
    ) : ReadinessResult()
}

sealed class CorrectionRequestResult {
    abstract val message: String

    data class Success(
        val preApplication: PreApplication,
        override val message: String = "Corrección solicitada a la familia."
    ) : CorrectionRequestResult()

    data class AlreadyRequested(
        val preApplication: PreApplication,
        override val message: String = "La corrección ya estaba solicitada con el mismo motivo."
    ) : CorrectionRequestResult()

    data object InvalidReason : CorrectionRequestResult() {
        override val message: String = "El motivo de corrección es obligatorio."
    }

    data class NotFound(
        val folio: String,
        override val message: String = "Pre-solicitud no encontrada."
    ) : CorrectionRequestResult()

    data object AmbiguousFolio : CorrectionRequestResult() {
        override val message: String = "No se puede solicitar la corrección: el folio está duplicado."
    }

    data class NotEditable(
        val status: PreApplicationStatus,
        override val message: String = "La pre-solicitud ya no permite solicitar correcciones."
    ) : CorrectionRequestResult()

    data object AlreadyConverted : CorrectionRequestResult() {
        override val message: String = "La pre-solicitud ya fue convertida a alta oficial."
    }
}

class PreApplicationViewModel {
    companion object {
        private val _sharedPreApplications = MutableStateFlow(MockPreApplicationData.preApplications)
        val sharedPreApplications: StateFlow<List<PreApplication>> = _sharedPreApplications.asStateFlow()
        private val preApplicationFolioChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private const val preApplicationTimestampPrefix = "Hoy "
        private val officialCurpPattern = Regex("^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]\\d$")

        fun approvePreApplication(folio: String) {
            updatePreApp(folio) { it.copy(status = PreApplicationStatus.ACEPTADA) }
            reconcileReadinessAfterRequirementChange(folio)
        }

        fun setObservaciones(folio: String, text: String) {
            updatePreApp(folio) { it.copy(observacionesSecretaria = text) }
        }

        fun requestCorrection(folio: String, reason: String): CorrectionRequestResult {
            val normalizedFolio = folio.trim().uppercase()
            val normalizedReason = reason.trim()
            if (normalizedReason.isBlank()) {
                return CorrectionRequestResult.InvalidReason
            }

            while (true) {
                val currentPreApplications = _sharedPreApplications.value
                val matchingIndexes = currentPreApplications.indices.filter { index ->
                    currentPreApplications[index].folio.trim().uppercase() == normalizedFolio
                }
                if (matchingIndexes.isEmpty()) {
                    return CorrectionRequestResult.NotFound(normalizedFolio)
                }
                if (matchingIndexes.size > 1) {
                    return CorrectionRequestResult.AmbiguousFolio
                }

                val index = matchingIndexes.single()
                val current = currentPreApplications[index]
                if (current.readinessStatus == ReadinessStatus.CONVERTED ||
                    _officialStudents.value.any { it.preApplicationFolio == current.folio }
                ) {
                    return CorrectionRequestResult.AlreadyConverted
                }
                if (current.status !in setOf(
                        PreApplicationStatus.ENVIADA,
                        PreApplicationStatus.ACEPTADA,
                        PreApplicationStatus.PENDIENTE_CORRECCION
                    )
                ) {
                    return CorrectionRequestResult.NotEditable(current.status)
                }
                if (current.status == PreApplicationStatus.PENDIENTE_CORRECCION &&
                    current.motivoCorreccion.trim() == normalizedReason &&
                    current.readinessStatus == ReadinessStatus.PENDING
                ) {
                    return CorrectionRequestResult.AlreadyRequested(current)
                }

                val updated = current.copy(
                    status = PreApplicationStatus.PENDIENTE_CORRECCION,
                    motivoCorreccion = normalizedReason,
                    readinessStatus = ReadinessStatus.PENDING,
                    readyAt = null,
                    readinessNotes = ""
                )
                val updatedPreApplications = currentPreApplications.toMutableList().apply {
                    this[index] = updated
                }
                if (_sharedPreApplications.compareAndSet(currentPreApplications, updatedPreApplications)) {
                    return CorrectionRequestResult.Success(updated)
                }
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

        data class PreApplicationPhotoState(
            val studentPhotoMockUrl: String? = null,
            val responsablePhotoMockUrl: String? = null
        )

        data class SecretariaReviewObservation(
            val id: String,
            val folio: String,
            val category: String,
            val note: String,
            val createdAt: String
        )

        private fun demoPhotoStates(): Map<String, PreApplicationPhotoState> = mapOf(
            "PRE-X1A2" to PreApplicationPhotoState("mock://photo/student/PRE-X1A2.jpg", "mock://photo/responsable/PRE-X1A2.jpg"),
            "PRE-D4L4" to PreApplicationPhotoState("mock://photo/student/PRE-D4L4.jpg", "mock://photo/responsable/PRE-D4L4.jpg"),
            "PRE-E5M5" to PreApplicationPhotoState("mock://photo/student/PRE-E5M5.jpg", "mock://photo/responsable/PRE-E5M5.jpg")
        )

        private val _photos = MutableStateFlow(demoPhotoStates())
        val photos: StateFlow<Map<String, PreApplicationPhotoState>> = _photos.asStateFlow()

        private val _reviewObservations = MutableStateFlow<Map<String, List<SecretariaReviewObservation>>>(emptyMap())
        val reviewObservations: StateFlow<Map<String, List<SecretariaReviewObservation>>> = _reviewObservations.asStateFlow()

        private val _officialStudents = MutableStateFlow(MockOfficialStudentData.officialStudents)
        val officialStudents: StateFlow<List<OfficialStudent>> = _officialStudents.asStateFlow()

        private val _enrollmentFlowMode = MutableStateFlow(EnrollmentFlowMode.ANNUAL_V2)
        val enrollmentFlowMode: StateFlow<EnrollmentFlowMode> = _enrollmentFlowMode.asStateFlow()

        fun setEnrollmentFlowMode(mode: EnrollmentFlowMode) {
            _enrollmentFlowMode.value = mode
        }

        private val _v2Result = MutableStateFlow<InstitutionalAnnualEnrollmentResult?>(null)
        val v2Result: StateFlow<InstitutionalAnnualEnrollmentResult?> = _v2Result.asStateFlow()

        private val _isProcessingAnnualEnrollmentV2 = MutableStateFlow(false)
        val isProcessingAnnualEnrollmentV2: StateFlow<Boolean> = _isProcessingAnnualEnrollmentV2.asStateFlow()

        fun setProcessingAnnualEnrollmentV2(processing: Boolean) {
            _isProcessingAnnualEnrollmentV2.value = processing
        }

        fun resetSharedStateForTests() {
            _sharedPreApplications.value = MockPreApplicationData.preApplications
            _photos.value = demoPhotoStates()
            _reviewObservations.value = emptyMap()
            _officialStudents.value = MockOfficialStudentData.officialStudents
            _enrollmentFlowMode.value = EnrollmentFlowMode.ANNUAL_V2
            _v2Result.value = null
            _isProcessingAnnualEnrollmentV2.value = false
            MockSaseData.resetForTests()
        }

        fun toggleDocumentCotejado(folio: String, docNombre: String) {
            updatePreApp(folio) { app ->
                app.copy(documentosDeclarados = app.documentosDeclarados.map { doc ->
                    if (doc.nombre != docNombre || !doc.declarado || doc.noAplica || doc.validado) doc
                    else if (doc.rechazado) doc.copy(rechazado = false, cotejadoSecretaria = true)
                    else doc.copy(cotejadoSecretaria = !doc.cotejadoSecretaria)
                })
            }
            reconcileReadinessAfterRequirementChange(folio)
        }

        fun markDocumentNoAplica(folio: String, docNombre: String) {
            updatePreApp(folio) { app ->
                app.copy(documentosDeclarados = app.documentosDeclarados.map { doc ->
                    if (doc.nombre != docNombre) doc
                    else doc.copy(noAplica = true, cotejadoSecretaria = false, validado = false, rechazado = false)
                })
            }
            reconcileReadinessAfterRequirementChange(folio)
        }

        fun markDocumentValidado(folio: String, docNombre: String) {
            updatePreApp(folio) { app ->
                app.copy(documentosDeclarados = app.documentosDeclarados.map { doc ->
                    if (doc.nombre != docNombre || !doc.declarado || !doc.cotejadoSecretaria) doc
                    else doc.copy(validado = true, rechazado = false)
                })
            }
            reconcileReadinessAfterRequirementChange(folio)
        }

        fun markDocumentRechazado(folio: String, docNombre: String) {
            updatePreApp(folio) { app ->
                app.copy(documentosDeclarados = app.documentosDeclarados.map { doc ->
                    if (doc.nombre != docNombre || !doc.declarado || !doc.cotejadoSecretaria) doc
                    else doc.copy(rechazado = true, validado = false, cotejadoSecretaria = false)
                })
            }
            reconcileReadinessAfterRequirementChange(folio)
        }

        fun setDocumentObservacion(folio: String, docNombre: String, observacion: String) {
            updatePreApp(folio) { app ->
                app.copy(documentosDeclarados = app.documentosDeclarados.map { doc ->
                    if (doc.nombre != docNombre) doc
                    else doc.copy(observacion = observacion.take(220))
                })
            }
            reconcileReadinessAfterRequirementChange(folio)
        }

        fun simulateCaptureStudentPhoto(folio: String) {
            val current = _photos.value.toMutableMap()
            current[folio] = (current[folio] ?: PreApplicationPhotoState()).copy(studentPhotoMockUrl = "mock://photo/student/$folio.jpg")
            _photos.value = current
            reconcileReadinessAfterRequirementChange(folio)
        }

        fun simulateCaptureResponsablePhoto(folio: String) {
            val current = _photos.value.toMutableMap()
            current[folio] = (current[folio] ?: PreApplicationPhotoState()).copy(responsablePhotoMockUrl = "mock://photo/responsable/$folio.jpg")
            _photos.value = current
            reconcileReadinessAfterRequirementChange(folio)
        }

        fun addReviewObservation(folio: String, category: String, note: String) {
            val cleanNote = note.trim().take(220)
            if (cleanNote.isBlank()) return

            val observation = SecretariaReviewObservation(
                id = "OBS-${folio.takeLast(4)}-${Random.nextInt(100, 999)}",
                folio = folio,
                category = category,
                note = cleanNote,
                createdAt = "Ahora"
            )
            val current = _reviewObservations.value.toMutableMap()
            current[folio] = listOf(observation) + current[folio].orEmpty()
            _reviewObservations.value = current
        }

        private fun generateUniquePreApplicationFolio(existingFolios: Set<String>): String {
            repeat(50) {
                val candidate = buildString {
                    append("PRE-310-")
                    repeat(6) {
                        append(preApplicationFolioChars[Random.nextInt(preApplicationFolioChars.length)])
                    }
                }
                if (candidate !in existingFolios) return candidate
            }
            return "PRE-310-${preApplicationFolioChars.take(6)}"
        }

        private fun normalizeCurp(curp: String): String = curp.trim().uppercase()

        private fun normalizeFamilyLookupValue(value: String): String =
            value.filterNot { it.isWhitespace() }.uppercase()

        fun lookupFamilyPreApplication(
            folio: String,
            curp: String
        ): FamilyPreApplicationLookupResult {
            val normalizedFolio = normalizeFamilyLookupValue(folio)
            val normalizedCurp = normalizeFamilyLookupValue(curp)
            if (normalizedFolio.isBlank() || normalizedCurp.isBlank()) {
                return FamilyPreApplicationLookupResult.Error()
            }

            val preApplication = _sharedPreApplications.value.firstOrNull { preApplication ->
                normalizeFamilyLookupValue(preApplication.folio) == normalizedFolio &&
                    normalizeFamilyLookupValue(preApplication.alumnoCurp) == normalizedCurp
            } ?: return FamilyPreApplicationLookupResult.Error()

            return FamilyPreApplicationLookupResult.Success(
                folio = preApplication.folio,
                status = preApplication.status,
                correctionReason = preApplication.motivoCorreccion,
                secretariaObservations = preApplication.observacionesSecretaria
            )
        }

        private fun normalizeMatricula(matricula: String): String = matricula.trim().uppercase()

        private fun isOfficialCurpComplete(curp: String): Boolean = normalizeCurp(curp).matches(officialCurpPattern)

        private fun ingresoAnioCorto(cicloEscolar: String): Int? =
            Regex("\\d{4}").find(cicloEscolar)?.value?.takeLast(2)?.toIntOrNull()

        fun updatePreApplicationAdministrativeData(
            request: UpdatePreApplicationAdministrativeDataRequest
        ): UpdatePreApplicationAdministrativeDataResult {
            val normalizedFolio = request.folio.trim().uppercase()
            while (true) {
                val currentPreApplications = _sharedPreApplications.value
                val matchingIndexes = currentPreApplications.indices.filter { index ->
                    currentPreApplications[index].folio.trim().uppercase() == normalizedFolio
                }
                if (matchingIndexes.isEmpty()) {
                    return UpdatePreApplicationAdministrativeDataResult.NotFound
                }
                if (matchingIndexes.size > 1) {
                    return UpdatePreApplicationAdministrativeDataResult.Conflict(
                        PreApplicationAdministrativeConflictReason.AMBIGUOUS_FOLIO
                    )
                }

                val targetIndex = matchingIndexes.single()
                val current = currentPreApplications[targetIndex]
                if (current.status != PreApplicationStatus.ENVIADA &&
                    current.status != PreApplicationStatus.PENDIENTE_CORRECCION
                ) {
                    return UpdatePreApplicationAdministrativeDataResult.Conflict(
                        PreApplicationAdministrativeConflictReason.NOT_EDITABLE
                    )
                }
                if (current.readinessStatus == ReadinessStatus.CONVERTED ||
                    _officialStudents.value.any { it.preApplicationFolio == current.folio }
                ) {
                    return UpdatePreApplicationAdministrativeDataResult.Conflict(
                        PreApplicationAdministrativeConflictReason.OFFICIAL_ENROLLMENT_EXISTS
                    )
                }

                val phone = when (val change = request.changes.phone) {
                    PreApplicationAdministrativeFieldChange.Omitted -> null
                    is PreApplicationAdministrativeFieldChange.Replace -> change.value.trim()
                }
                val address = when (val change = request.changes.address) {
                    PreApplicationAdministrativeFieldChange.Omitted -> null
                    is PreApplicationAdministrativeFieldChange.Replace -> change.value.trim()
                }
                val errors = buildMap {
                    if (phone != null) {
                        when {
                            phone.isBlank() -> put(
                                PreApplicationAdministrativeField.PHONE,
                                PreApplicationAdministrativeValidationError.REQUIRED
                            )
                            !phone.matches(Regex("\\d{10}")) -> put(
                                PreApplicationAdministrativeField.PHONE,
                                PreApplicationAdministrativeValidationError.INVALID_FORMAT
                            )
                        }
                    }
                    if (address != null && address.isBlank()) {
                        put(
                            PreApplicationAdministrativeField.ADDRESS,
                            PreApplicationAdministrativeValidationError.REQUIRED
                        )
                    }
                }
                if (errors.isNotEmpty()) {
                    return UpdatePreApplicationAdministrativeDataResult.Invalid(errors)
                }

                val changedFields = buildSet {
                    if (phone != null && phone != current.alumnoTelefonoCasa.trim()) {
                        add(PreApplicationAdministrativeField.PHONE)
                    }
                    if (address != null && address != current.alumnoDomicilio.trim()) {
                        add(PreApplicationAdministrativeField.ADDRESS)
                    }
                }
                if (changedFields.isEmpty()) {
                    return UpdatePreApplicationAdministrativeDataResult.NoChanges
                }

                val stalePhone = PreApplicationAdministrativeField.PHONE in changedFields &&
                    current.alumnoTelefonoCasa.trim() != request.expected.phone.trim()
                val staleAddress = PreApplicationAdministrativeField.ADDRESS in changedFields &&
                    current.alumnoDomicilio.trim() != request.expected.address.trim()
                if (stalePhone || staleAddress) {
                    return UpdatePreApplicationAdministrativeDataResult.Conflict(
                        PreApplicationAdministrativeConflictReason.STALE_DATA
                    )
                }

                val updated = current.copy(
                    alumnoTelefonoCasa = phone ?: current.alumnoTelefonoCasa,
                    alumnoDomicilio = address ?: current.alumnoDomicilio
                )
                val updatedPreApplications = currentPreApplications.toMutableList().apply {
                    this[targetIndex] = updated
                }
                if (_sharedPreApplications.compareAndSet(currentPreApplications, updatedPreApplications)) {
                    return UpdatePreApplicationAdministrativeDataResult.Updated(changedFields)
                }
            }
        }

        private fun updatePreApp(
            folio: String,
            transform: (PreApplication) -> PreApplication
        ) {
            _sharedPreApplications.value = _sharedPreApplications.value.map {
                if (it.folio == folio) transform(it) else it
            }
        }

        private fun buildStoredPreApplication(preApplication: PreApplication): PreApplication {
            val normalizedCurp = normalizeCurp(preApplication.alumnoCurp)
            val existingFolios = _sharedPreApplications.value.map { it.folio }.toSet()
            val startingFolio = preApplication.folio.trim()
            val finalFolio = if (startingFolio.isNotBlank() && startingFolio !in existingFolios) {
                startingFolio
            } else {
                generateUniquePreApplicationFolio(existingFolios)
            }

            return preApplication.copy(
                folio = finalFolio,
                status = PreApplicationStatus.ENVIADA,
                submittedAt = preApplication.submittedAt ?: "$preApplicationTimestampPrefix${com.example.formatTimestamp("hh:mm a")}",
                alumnoCurp = normalizedCurp
            )
        }

        fun submitFamilyPreApplication(preApplication: PreApplication): FamilySubmissionResult {
            val normalizedCurp = normalizeCurp(preApplication.alumnoCurp)
            if (preApplication.alumnoNombreCompleto.isBlank() ||
                normalizedCurp.length != 18 ||
                preApplication.alumnoFechaNacimiento.isBlank() ||
                preApplication.alumnoDomicilio.isBlank() ||
                preApplication.responsables.isEmpty() ||
                preApplication.gradoSolicitado !in 1..3 ||
                preApplication.promedioGradoAnterior?.let { it in 5.0..10.0 } != true ||
                preApplication.personaTramite.nombreCompleto.isBlank() ||
                preApplication.personaTramite.parentesco.isBlank() ||
                preApplication.personaTramite.telefono.length < 10 ||
                preApplication.personaTramite.identificacionPresentada.isBlank()
            ) {
                return FamilySubmissionResult.InsufficientData("Faltan datos obligatorios para enviar la pre-solicitud.")
            }

            val duplicateFolio = preApplication.folio.trim().takeIf { it.isNotBlank() }?.let { folio ->
                _sharedPreApplications.value.any { it.folio == folio }
            } == true
            if (duplicateFolio) {
                return FamilySubmissionResult.DuplicateFolio(preApplication.folio.trim())
            }

            val duplicateCurp = _sharedPreApplications.value.any { normalizeCurp(it.alumnoCurp) == normalizedCurp } ||
                _officialStudents.value.any { normalizeCurp(it.curp) == normalizedCurp } ||
                MockSaseData.students.value.any { normalizeCurp(it.curp) == normalizedCurp }
            if (duplicateCurp) {
                return FamilySubmissionResult.DuplicateCurp(normalizedCurp)
            }

            val stored = buildStoredPreApplication(preApplication)
            _sharedPreApplications.value = _sharedPreApplications.value + stored
            return FamilySubmissionResult.Success(stored)
        }

        fun resubmitCorrectedPreApplication(
            preApplication: PreApplication
        ): FamilyResubmissionResult {
            val normalizedFolio = preApplication.folio.trim()
            val normalizedCurp = normalizeCurp(preApplication.alumnoCurp)
            val currentPreApplications = _sharedPreApplications.value
            val storedIndex = currentPreApplications.indexOfFirst { it.folio == normalizedFolio }
            if (storedIndex == -1) {
                return FamilyResubmissionResult.NotFound(normalizedFolio)
            }

            val stored = currentPreApplications[storedIndex]
            if (stored.status != PreApplicationStatus.PENDIENTE_CORRECCION) {
                return FamilyResubmissionResult.InvalidStatus(stored.folio, stored.status)
            }

            val duplicateCurp = currentPreApplications.any {
                it.folio != stored.folio && normalizeCurp(it.alumnoCurp) == normalizedCurp
            }
            if (duplicateCurp) {
                return FamilyResubmissionResult.DuplicateCurp(normalizedCurp)
            }

            val familyDocumentsByName = preApplication.documentosDeclarados.associateBy { it.nombre }
            val resubmitted = stored.copy(
                status = PreApplicationStatus.ENVIADA,
                tramite = preApplication.tramite,
                cicloEscolar = preApplication.cicloEscolar,
                gradoSolicitado = preApplication.gradoSolicitado,
                alumnoNombreCompleto = preApplication.alumnoNombreCompleto,
                alumnoCurp = normalizedCurp,
                alumnoFechaNacimiento = preApplication.alumnoFechaNacimiento,
                alumnoSexo = preApplication.alumnoSexo,
                alumnoNacionalidad = preApplication.alumnoNacionalidad,
                alumnoEntidadNacimiento = preApplication.alumnoEntidadNacimiento,
                alumnoDomicilio = preApplication.alumnoDomicilio,
                alumnoTelefonoCasa = preApplication.alumnoTelefonoCasa,
                escuelaProcedencia = preApplication.escuelaProcedencia,
                promedioGradoAnterior = preApplication.promedioGradoAnterior,
                personaTramite = preApplication.personaTramite,
                responsables = preApplication.responsables,
                autorizados = preApplication.autorizados,
                fichaMedicaFamiliar = preApplication.fichaMedicaFamiliar,
                contextoSociofamiliar = preApplication.contextoSociofamiliar,
                antecedentesUdeii = preApplication.antecedentesUdeii,
                documentosDeclarados = stored.documentosDeclarados.map { storedDocument ->
                    val familyDocument = familyDocumentsByName[storedDocument.nombre]
                    if (familyDocument == null) storedDocument
                    else storedDocument.copy(declarado = familyDocument.declarado)
                },
                consentimientos = preApplication.consentimientos,
                readinessStatus = ReadinessStatus.PENDING,
                readyAt = null,
                readinessNotes = ""
            )

            val updatedPreApplications = currentPreApplications.toMutableList()
            updatedPreApplications[storedIndex] = resubmitted
            _sharedPreApplications.value = updatedPreApplications
            return FamilyResubmissionResult.Success(resubmitted)
        }

        fun officialEnrollmentPendingItems(preApp: PreApplication): List<String> {
            val photoState = _photos.value[preApp.folio]
            return buildList {
                curpDuplicateInfo(preApp.folio, preApp.alumnoCurp, preApp.tramite)?.let { add(it) }
                if (preApp.status != PreApplicationStatus.ACEPTADA) {
                    add("Pre-solicitud aceptada por Secretaría")
                }
                if (preApp.documentosDeclarados.any { !it.noAplica && !it.validado }) {
                    add("Documentos requeridos cotejados")
                }
                if (preApp.promedioGradoAnterior?.let { it in 5.0..10.0 } != true) {
                    add("Promedio del grado anterior capturado y verificado")
                }
                if (preApp.personaTramite.nombreCompleto.isBlank() ||
                    preApp.personaTramite.parentesco.isBlank() ||
                    preApp.personaTramite.telefono.length < 10 ||
                    preApp.personaTramite.identificacionPresentada.isBlank()
                ) {
                    add("Persona que inscribe/reinscribe capturada")
                }
                if (photoState?.studentPhotoMockUrl == null) {
                    add("Foto alumno mock capturada")
                }
                if (photoState?.responsablePhotoMockUrl == null) {
                    add("Foto responsable mock capturada")
                }
            }
        }

        fun isReadyForOfficialEnrollment(preApp: PreApplication): Boolean =
            officialEnrollmentPendingItems(preApp).isEmpty()

        private fun reconcileReadinessAfterRequirementChange(folio: String) {
            while (true) {
                val currentPreApplications = _sharedPreApplications.value
                val index = currentPreApplications.indexOfFirst { it.folio == folio }
                if (index < 0) return
                val current = currentPreApplications[index]
                if (current.readinessStatus == ReadinessStatus.CONVERTED) return

                val pendingItems = officialEnrollmentPendingItems(current)
                val updated = when {
                    pendingItems.isNotEmpty() && current.readinessStatus in setOf(
                        ReadinessStatus.BLOCKED,
                        ReadinessStatus.READY
                    ) -> current.copy(
                        readinessStatus = ReadinessStatus.BLOCKED,
                        readyAt = null,
                        readinessNotes = pendingItems.joinToString("; ")
                    )
                    pendingItems.isEmpty() && current.readinessStatus == ReadinessStatus.BLOCKED ->
                        current.copy(
                            readyAt = null,
                            readinessNotes = "Pendientes resueltos; requiere declaración institucional READY."
                        )
                    else -> return
                }
                val updatedPreApplications = currentPreApplications.toMutableList().apply {
                    this[index] = updated
                }
                if (_sharedPreApplications.compareAndSet(currentPreApplications, updatedPreApplications)) return
            }
        }

        fun markReadyForOfficialEnrollment(folio: String): ReadinessResult {
            val preApp = _sharedPreApplications.value.firstOrNull { it.folio == folio }
                ?: return ReadinessResult.NotFound(folio)
            if (preApp.readinessStatus == ReadinessStatus.CONVERTED) {
                return ReadinessResult.AlreadyConverted(preApp)
            }
            if (preApp.readinessStatus == ReadinessStatus.READY) {
                return ReadinessResult.AlreadyReady(preApp)
            }

            val pendingItems = officialEnrollmentPendingItems(preApp)
            if (pendingItems.isNotEmpty()) {
                val blocked = preApp.copy(
                    readinessStatus = ReadinessStatus.BLOCKED,
                    readinessNotes = pendingItems.joinToString("; ")
                )
                updatePreApp(folio) { blocked }
                return ReadinessResult.NotReady(pendingItems)
            }

            val ready = preApp.copy(
                readinessStatus = ReadinessStatus.READY,
                readyAt = "$preApplicationTimestampPrefix${com.example.formatTimestamp("hh:mm a")}",
                readinessNotes = "Validación institucional lista: documentos y fotos mock completos."
            )
            updatePreApp(folio) { ready }
            return ReadinessResult.Success(ready)
        }

        fun reopenReview(folio: String): Boolean {
            val preApp = _sharedPreApplications.value.firstOrNull { it.folio.trim().uppercase() == folio.trim().uppercase() }
                ?: return false
            if (preApp.readinessStatus != ReadinessStatus.READY) return false
            updatePreApp(folio) {
                it.copy(
                    readinessStatus = ReadinessStatus.PENDING,
                    readyAt = null,
                    readinessNotes = "Revisión reabierta por Secretaría."
                )
            }
            return true
        }

        fun officialEnrollmentForFolio(folio: String): OfficialStudent? =
            _officialStudents.value.find { it.preApplicationFolio == folio }

        fun groupOptionsForGrade(grado: Int): List<String> = when (grado) {
            1 -> listOf("1A", "1B", "1C", "1D")
            2 -> listOf("2A", "2B", "2C", "2D")
            3 -> listOf("3A", "3B", "3C", "3D")
            else -> emptyList()
        }

        fun promedioLabelForGrade(grado: Int): String = when (grado) {
            1 -> "Promedio de primaria"
            2 -> "Promedio de 1°"
            3 -> "Promedio de 2°"
            else -> "Promedio del grado anterior"
        }

        private const val MAX_CAPACITY_PER_GROUP = 30

        fun suggestInitialGroup(
            grado: Int,
            sexo: String = "",
            edad: Int = 0,
            promedio: Double? = null
        ): String? {
            val options = groupOptionsForGrade(grado)
            if (options.isEmpty()) return null

            val candidates = _officialStudents.value
                .filter { it.gradoIngreso == grado && it.status == OfficialStudentStatus.ALTA_OFICIAL_CON_GRUPO }

            val groupData = options.associateWith { group ->
                val members = candidates.filter { (it.grupoAsignado ?: it.grupoSugerido) == group }
                val countH = members.count { it.alumnoSexo == "H" || it.alumnoSexo == "Masculino" }
                val countM = members.count { it.alumnoSexo == "M" || it.alumnoSexo == "Femenino" }
                val avgAge = members.mapNotNull { it.alumnoEdad.takeIf { e -> e > 0 } }
                    .let { ages -> if (ages.isNotEmpty()) ages.average() else 0.0 }
                val avgPromedio = members.mapNotNull { it.promedio }
                    .let { vals -> if (vals.isNotEmpty()) vals.average() else null }
                GroupStats(count = members.size, countH = countH, countM = countM, avgAge = avgAge, avgPromedio = avgPromedio)
            }

            val gradeAgeAvg = candidates.mapNotNull { it.alumnoEdad.takeIf { e -> e > 0 } }
                .let { ages -> if (ages.isNotEmpty()) ages.average() else 0.0 }
            val gradePromedioAvg = candidates.mapNotNull { it.promedio }
                .let { vals -> if (vals.isNotEmpty()) vals.average() else null }

            return options
                .filter { (groupData[it]?.count ?: 0) < MAX_CAPACITY_PER_GROUP }
                .minByOrNull { group ->
                    val stats = groupData[group]!!
                    val normalizedSexo = when (sexo) {
                        "Masculino", "H" -> "H"
                        "Femenino", "M" -> "M"
                        else -> ""
                    }
                    val sexPenalty = if (sexo.isNotBlank()) {
                        val newCountH = stats.countH + if (normalizedSexo == "H") 1 else 0
                        val newCountM = stats.countM + if (normalizedSexo == "M") 1 else 0
                        kotlin.math.abs(newCountH - newCountM).toDouble()
                    } else 0.0

                    val agePenalty = if (edad > 0 && gradeAgeAvg > 0) {
                        val newAvgAge = if (stats.avgAge > 0) {
                            (stats.avgAge * stats.count + edad) / (stats.count + 1)
                        } else edad.toDouble()
                        kotlin.math.abs(newAvgAge - gradeAgeAvg)
                    } else 0.0

                    val avgPenalty = if (promedio != null && gradePromedioAvg != null) {
                        val newAvgProm = if (stats.avgPromedio != null) {
                            (stats.avgPromedio * stats.count + promedio) / (stats.count + 1)
                        } else promedio
                        kotlin.math.abs(newAvgProm - gradePromedioAvg)
                    } else 0.0

                    (sexPenalty * 3.0) + (agePenalty * 2.0) + (avgPenalty * 1.0)
                }
        }

        private data class GroupStats(
            val count: Int,
            val countH: Int,
            val countM: Int,
            val avgAge: Double,
            val avgPromedio: Double?
        )

        fun calculateAgeFromBirthDate(fechaNacimiento: String): Int {
            return try {
                val months = mapOf(
                    "Ene" to 1, "Feb" to 2, "Mar" to 3, "Abr" to 4, "May" to 5, "Jun" to 6,
                    "Jul" to 7, "Ago" to 8, "Sep" to 9, "Oct" to 10, "Nov" to 11, "Dic" to 12
                )
                val parts = fechaNacimiento.split("/")
                if (parts.size != 3) return 0
                val day = parts[0].toIntOrNull() ?: return 0
                val month = months[parts[1]] ?: return 0
                val year = parts[2].toIntOrNull() ?: return 0
                val currentParts = formatTimestamp("MMM/dd/yyyy").split("/")
                if (currentParts.size != 3) return 0
                val currentMonth = months[currentParts[0]] ?: 6
                val currentDay = currentParts[1].toIntOrNull() ?: 15
                val currentYear = currentParts[2].toIntOrNull() ?: 2026
                var age = currentYear - year
                if (currentMonth < month || (currentMonth == month && currentDay < day)) {
                    age--
                }
                age.coerceIn(0, 25)
            } catch (_: Exception) { 0 }
        }

        private fun officialStudentByCurp(curp: String): OfficialStudent? =
            _officialStudents.value.firstOrNull { normalizeCurp(it.curp) == normalizeCurp(curp) }

        private fun officialStudentByMatricula(matricula: String): OfficialStudent? =
            _officialStudents.value.firstOrNull {
                it.matriculaOficial?.let(::normalizeMatricula) == normalizeMatricula(matricula)
            }

        private fun masterStudentByCurp(curp: String): Student? =
            MockSaseData.students.value.firstOrNull { normalizeCurp(it.curp) == normalizeCurp(curp) }

        private fun masterStudentByMatricula(matricula: String): Student? =
            MockSaseData.students.value.firstOrNull {
                normalizeMatricula(it.enrollmentId) == normalizeMatricula(matricula)
            }

        private fun preApplicationByCurp(curp: String): PreApplication? =
            _sharedPreApplications.value.firstOrNull { normalizeCurp(it.alumnoCurp) == normalizeCurp(curp) }

        fun curpDuplicateInfo(folio: String, curp: String, tramite: String = ""): String? {
            if (tramite.uppercase() == "REINSCRIPCION") return null
            val normalized = normalizeCurp(curp)
            val inMaster = MockSaseData.students.value.firstOrNull {
                normalizeCurp(it.curp) == normalized && it.preApplicationFolio?.let { p -> normalizeCurp(p) != normalizeCurp(folio) } == true
            }
            if (inMaster != null) return "CURP ya registrada en el padrón maestro (${inMaster.fullName})"
            val inOfficial = _officialStudents.value.firstOrNull {
                normalizeCurp(it.curp) == normalized && normalizeCurp(it.preApplicationFolio) != normalizeCurp(folio)
            }
            if (inOfficial != null) return "CURP ya registrada en alta oficial (${inOfficial.alumnoNombreCompleto})"
            return null
        }

        fun startOfficialEnrollment(preApp: PreApplication, selectedGroup: String?): OfficialEnrollmentResult {
            val existing = officialEnrollmentForFolio(preApp.folio)
            if (existing != null) {
                return OfficialEnrollmentResult.DuplicateFolio(preApp.folio)
            }

            val sourcePreApplication = _sharedPreApplications.value.firstOrNull { it.folio == preApp.folio }
                ?: return OfficialEnrollmentResult.PreApplicationNotFound(preApp.folio)
            val annualV2Matches = MockSaseData.annualEnrollments.value.filter {
                it.sourcePreApplicationFolio.trim().uppercase() == sourcePreApplication.folio.trim().uppercase() &&
                    it.schoolYear.trim() == sourcePreApplication.cicloEscolar.trim()
            }
            if (annualV2Matches.size > 1) {
                return OfficialEnrollmentResult.Error(
                    "Existen varias anualidades V2 para el mismo folio y ciclo escolar."
                )
            }
            if (annualV2Matches.size == 1) {
                return OfficialEnrollmentResult.DuplicateFolio(sourcePreApplication.folio)
            }
            val pendingItems = officialEnrollmentPendingItems(sourcePreApplication)
            if (pendingItems.isNotEmpty()) {
                return OfficialEnrollmentResult.NotReady(pendingItems)
            }
            if (sourcePreApplication.readinessStatus != ReadinessStatus.READY) {
                return OfficialEnrollmentResult.NotReady(listOf("Readiness institucional declarada"))
            }

            val normalizedCurp = normalizeCurp(preApp.alumnoCurp)
            if (normalizeCurp(sourcePreApplication.alumnoCurp) != normalizedCurp) {
                return OfficialEnrollmentResult.Error("La pre-solicitud ya no coincide con la bandeja institucional compartida.")
            }
            if (!isOfficialCurpComplete(normalizedCurp) || preApp.gradoSolicitado !in 1..3) {
                return OfficialEnrollmentResult.Error("La matrícula se asignará cuando la CURP y el alta oficial estén completas.")
            }
            if (officialStudentByCurp(normalizedCurp) != null) {
                return OfficialEnrollmentResult.DuplicateCurp(normalizedCurp)
            }

            val existingMasterByCurp = masterStudentByCurp(normalizedCurp)
            if (existingMasterByCurp != null && existingMasterByCurp.preApplicationFolio != preApp.folio) {
                return OfficialEnrollmentResult.DuplicateCurp(normalizedCurp)
            }

            val edadAlumno = calculateAgeFromBirthDate(preApp.alumnoFechaNacimiento)
            val suggestedGroup = selectedGroup?.takeIf { it.isNotBlank() }
                ?: suggestInitialGroup(preApp.gradoSolicitado, preApp.alumnoSexo, edadAlumno, preApp.promedioGradoAnterior)

            val initialStatus = when (preApp.gradoSolicitado) {
                1, 2, 3 -> OfficialStudentStatus.PENDIENTE_ASIGNACION_GRUPO
                else -> OfficialStudentStatus.PENDIENTE_ASIGNACION_GRUPO
            }

            val officialStudent = OfficialStudent(
                id = "OFF-${preApp.folio.takeLast(4)}-${Random.nextInt(100, 999)}",
                preApplicationFolio = preApp.folio,
                status = initialStatus,
                gradoIngreso = preApp.gradoSolicitado,
                grupoSugerido = suggestedGroup,
                grupoAsignado = null,
                curp = normalizedCurp,
                alumnoNombreCompleto = preApp.alumnoNombreCompleto,
                alumnoSexo = preApp.alumnoSexo,
                alumnoEdad = edadAlumno,
                promedio = preApp.promedioGradoAnterior,
                matriculaOficial = null,
                fechaCreacion = "$preApplicationTimestampPrefix${com.example.formatTimestamp("hh:mm a")}",
                validacionSecretaria = ValidacionArea(
                    area = "Secretaría",
                    validado = true,
                    validadoPor = "Secretaría",
                    fechaValidacion = "$preApplicationTimestampPrefix${com.example.formatTimestamp("hh:mm a")}",
                    observaciones = "Alta oficial iniciada desde pre-solicitud ${preApp.folio}"
                )
            )

            _officialStudents.value = _officialStudents.value + officialStudent
            return OfficialEnrollmentResult.Success(
                officialStudent = officialStudent,
                masterStudent = buildProvisionalStudent(sourcePreApplication),
                masterStudentCreated = false,
                message = "Alta oficial iniciada. Matrícula pendiente hasta confirmar grupo institucional."
            )
        }

        private fun markConverted(folio: String) {
            updatePreApp(folio) {
                it.copy(
                    readinessStatus = ReadinessStatus.CONVERTED,
                    readinessNotes = "Alta oficial generada y expediente maestro sincronizado."
                )
            }
        }

        private fun propagateOfficialEnrollmentToMasterStudent(
            preApp: PreApplication,
            officialStudent: OfficialStudent
        ): StudentAddResult {
            val existingMaster = MockSaseData.studentByCurp(officialStudent.curp)
                ?: officialStudent.matriculaOficial?.let { MockSaseData.studentByEnrollmentId(it) }
            if (existingMaster?.preApplicationFolio == preApp.folio) {
                val updated = masterStudentFromOfficial(preApp, officialStudent, existingMaster.id)
                MockSaseData.updateStudent(updated)
                return StudentAddResult.DuplicateCurp(updated.curp, updated)
            }
            return MockSaseData.addStudent(masterStudentFromOfficial(preApp, officialStudent))
        }

        private fun masterStudentFromOfficial(
            preApp: PreApplication,
            officialStudent: OfficialStudent,
            existingId: String? = null
        ): Student {
            val responsable = preApp.responsables.firstOrNull()
            val group = officialStudent.grupoAsignado ?: officialStudent.grupoSugerido.orEmpty()
            return Student(
                id = existingId ?: "MASTER-${preApp.folio.takeLast(4)}",
                fullName = preApp.alumnoNombreCompleto,
                group = group,
                enrollmentId = officialStudent.matriculaOficial.orEmpty(),
                curp = officialStudent.curp,
                schoolYear = preApp.cicloEscolar,
                status = when (officialStudent.status) {
                    OfficialStudentStatus.ALTA_OFICIAL_SIN_GRUPO -> "Alta oficial sin grupo"
                    OfficialStudentStatus.PENDIENTE_ASIGNACION_GRUPO -> "Alta oficial / pendiente asignación de grupo"
                    else -> "Alta oficial"
                },
                riskLevel = "Bajo",
                documentationStatus = "Completa",
                birthDate = preApp.alumnoFechaNacimiento,
                address = preApp.alumnoDomicilio,
                tutorName = responsable?.nombreCompleto.orEmpty(),
                tutorRelation = responsable?.parentesco.orEmpty(),
                tutorPhone = responsable?.telefono.orEmpty(),
                tutorEmail = responsable?.correo.orEmpty(),
                documents = preApp.documentosDeclarados.map { doc ->
                    SaseDocument(doc.nombre, officialStudent.fechaCreacion, if (doc.cotejadoSecretaria) "Vigente" else "En revisión")
                },
                audits = listOf(
                    SaseAudit("Alta oficial propagada", "Secretaría", officialStudent.fechaCreacion, "Origen ${preApp.folio}")
                ),
                preApplicationFolio = preApp.folio
            )
        }

        fun confirmInitialGroup(folio: String, selectedGroup: String): OfficialEnrollmentResult {
            val cleanGroup = selectedGroup.trim().uppercase()
            if (cleanGroup.isBlank()) {
                return OfficialEnrollmentResult.Error("Selecciona un grupo para confirmar.")
            }

            val sourcePreApplication = _sharedPreApplications.value.firstOrNull { it.folio == folio }
                ?: return OfficialEnrollmentResult.PreApplicationNotFound(folio)
            val pendingItems = officialEnrollmentPendingItems(sourcePreApplication)
            if (pendingItems.isNotEmpty()) {
                return OfficialEnrollmentResult.NotReady(pendingItems)
            }
            if (sourcePreApplication.readinessStatus != ReadinessStatus.READY && sourcePreApplication.readinessStatus != ReadinessStatus.CONVERTED) {
                return OfficialEnrollmentResult.NotReady(listOf("Readiness institucional declarada"))
            }

            var updatedStudent: OfficialStudent? = null
            _officialStudents.value = _officialStudents.value.map { student ->
                if (student.preApplicationFolio != folio) return@map student
                if (!isOfficialCurpComplete(student.curp) || student.gradoIngreso !in 1..3) {
                    return OfficialEnrollmentResult.Error("La matrícula se asignará cuando la CURP y el alta oficial estén completas.")
                }
                val ingreso = ingresoAnioCorto(sourcePreApplication.cicloEscolar)
                    ?: return OfficialEnrollmentResult.Error("La matrícula se asignará cuando el año de ingreso y el alta oficial estén completos.")
                val matricula = OfficialStudent.generateMatricula(student.curp, ingreso)
                    ?: return OfficialEnrollmentResult.Error("La matrícula se asignará cuando la CURP y el alta oficial estén completas.")
                val existingOfficialByMatricula = officialStudentByMatricula(matricula)
                if (existingOfficialByMatricula != null && existingOfficialByMatricula.preApplicationFolio != folio) {
                    return OfficialEnrollmentResult.DuplicateMatricula(matricula)
                }
                val existingMasterByMatricula = masterStudentByMatricula(matricula)
                if (existingMasterByMatricula != null && existingMasterByMatricula.preApplicationFolio != folio) {
                    return OfficialEnrollmentResult.DuplicateMatricula(matricula)
                }
                updatedStudent = student.copy(
                    status = OfficialStudentStatus.ALTA_OFICIAL_CON_GRUPO,
                    grupoAsignado = cleanGroup,
                    matriculaOficial = normalizeMatricula(matricula),
                    validacionDireccion = ValidacionArea(
                        area = "Dirección",
                        validado = true,
                        validadoPor = "Secretaría/Dirección",
                        fechaValidacion = "Hoy",
                        observaciones = "Grupo $cleanGroup confirmado por cupo básico mock"
                    )
                )
                updatedStudent ?: student
            }

            val currentStudent = updatedStudent
                ?: return OfficialEnrollmentResult.Error("No se encontró alta oficial para este folio.")
            val existingMaster = MockSaseData.studentByCurp(currentStudent.curp)
            val syncedMaster = if (existingMaster != null) {
                val updatedMaster = existingMaster.copy(
                    group = cleanGroup,
                    enrollmentId = currentStudent.matriculaOficial.orEmpty(),
                    status = "Alta oficial con grupo"
                )
                MockSaseData.updateStudent(updatedMaster)
                updatedMaster
            } else {
                val newMaster = Student(
                    id = "MASTER-${folio.takeLast(4)}",
                    fullName = currentStudent.alumnoNombreCompleto,
                    group = cleanGroup,
                    enrollmentId = currentStudent.matriculaOficial.orEmpty(),
                    curp = currentStudent.curp,
                    status = "Alta oficial con grupo",
                    preApplicationFolio = folio
                )
                when (val addResult = MockSaseData.addStudent(newMaster)) {
                    is StudentAddResult.Added -> addResult.student
                    is StudentAddResult.DuplicateCurp -> addResult.existing
                    is StudentAddResult.DuplicateEnrollmentId -> return OfficialEnrollmentResult.DuplicateMatricula(addResult.enrollmentId)
                    is StudentAddResult.InvalidData -> return OfficialEnrollmentResult.MasterStudentPropagationError(addResult.message)
                }
            }
            markConverted(folio)
            return OfficialEnrollmentResult.Success(
                officialStudent = currentStudent,
                masterStudent = syncedMaster,
                masterStudentCreated = false,
                message = "Grupo inicial confirmado y expediente maestro sincronizado."
            )
        }

        fun buildProvisionalStudent(preApp: PreApplication): com.example.data.Student {
            val newId = "PROV-${preApp.folio.takeLast(4)}"
            val resp = preApp.responsables.firstOrNull()
            return com.example.data.Student(
                id = newId,
                fullName = preApp.alumnoNombreCompleto,
                group = "", // No se asigna grupo — pendiente alta oficial
                enrollmentId = "", // No se genera matrícula
                curp = preApp.alumnoCurp,
                status = "Expediente provisional / En revisión",
                tutorName = resp?.nombreCompleto ?: "",
                tutorRelation = resp?.parentesco ?: "",
                tutorPhone = resp?.telefono ?: "",
                tutorEmail = resp?.correo ?: "",
                birthDate = preApp.alumnoFechaNacimiento,
                address = preApp.alumnoDomicilio,
                documentationStatus = "Pendiente"
            )
        }

        fun commitAnnualEnrollment(
            planningResult: AnnualEnrollmentPlanningResult,
            newStudentId: String?,
            studentFullName: String,
            actor: String = "Secretaría",
            occurredAt: String = "HOY ${com.example.formatTimestamp("hh:mm a")}"
        ): AnnualEnrollmentPersistenceResult = AnnualEnrollmentPersistenceAdapter.commit(
            planningResult = planningResult,
            newStudentId = newStudentId,
            studentFullName = studentFullName,
            actor = actor,
            occurredAt = occurredAt
        )

        fun processAnnualEnrollmentV2(
            declaredMovement: String,
            normalizedCurp: String,
            folio: String,
            requestedGrade: Int,
            previousGroup: String?,
            schoolYear: String,
            studentFullName: String
        ): InstitutionalAnnualEnrollmentResult = processAnnualEnrollmentV2WithSynchronizer(
            declaredMovement = declaredMovement,
            normalizedCurp = normalizedCurp,
            folio = folio,
            requestedGrade = requestedGrade,
            previousGroup = previousGroup,
            schoolYear = schoolYear,
            studentFullName = studentFullName,
            conversionSynchronizer = InstitutionalPreApplicationSynchronizer(::synchronizePreApplicationConversion)
        )

        internal fun processAnnualEnrollmentV2WithSynchronizer(
            declaredMovement: String,
            normalizedCurp: String,
            folio: String,
            requestedGrade: Int,
            previousGroup: String?,
            schoolYear: String,
            studentFullName: String,
            conversionSynchronizer: InstitutionalPreApplicationSynchronizer
        ): InstitutionalAnnualEnrollmentResult {
            fun reject(
                cause: InstitutionalEnrollmentGuardCause,
                message: String
            ): InstitutionalAnnualEnrollmentResult {
                val rejected = InstitutionalAnnualEnrollmentResult.GuardRejected(cause, message)
                _v2Result.value = rejected
                return rejected
            }

            val normalizedFolio = folio.trim().uppercase()
            val matchingPreApplications = _sharedPreApplications.value.filter {
                it.folio.trim().uppercase() == normalizedFolio
            }
            if (matchingPreApplications.isEmpty()) {
                return reject(
                    InstitutionalEnrollmentGuardCause.PRE_APPLICATION_NOT_FOUND,
                    "La pre-solicitud no existe en la bandeja institucional."
                )
            }
            if (matchingPreApplications.size > 1) {
                return reject(
                    InstitutionalEnrollmentGuardCause.AMBIGUOUS_FOLIO,
                    "El folio está duplicado en la bandeja institucional."
                )
            }

            val source = matchingPreApplications.single()
            if (source.status != PreApplicationStatus.ACEPTADA) {
                return reject(
                    InstitutionalEnrollmentGuardCause.NOT_ACCEPTED,
                    "La pre-solicitud debe estar aceptada antes de procesar V2."
                )
            }

            val sourceMovement = source.tramite.trim().uppercase().replace('Ó', 'O')
            val requestedMovement = declaredMovement.trim().uppercase().replace('Ó', 'O')
            if (requestedMovement != sourceMovement ||
                normalizeCurp(normalizedCurp) != normalizeCurp(source.alumnoCurp) ||
                requestedGrade != source.gradoSolicitado ||
                schoolYear.trim() != source.cicloEscolar.trim() ||
                studentFullName.trim() != source.alumnoNombreCompleto.trim()
            ) {
                return reject(
                    InstitutionalEnrollmentGuardCause.SOURCE_MISMATCH,
                    "Los datos solicitados no coinciden con la pre-solicitud institucional."
                )
            }

            val existingAnnualByFolio = MockSaseData.annualEnrollments.value.filter {
                it.sourcePreApplicationFolio.trim().uppercase() == source.folio.trim().uppercase()
            }
            if (existingAnnualByFolio.isEmpty()) {
                if (source.readinessStatus != ReadinessStatus.READY) {
                    return reject(
                        InstitutionalEnrollmentGuardCause.NOT_READY,
                        "La pre-solicitud debe estar declarada READY antes de procesar V2."
                    )
                }
                val pendingItems = officialEnrollmentPendingItems(source)
                if (pendingItems.isNotEmpty()) {
                    return reject(
                        InstitutionalEnrollmentGuardCause.PENDING_REQUIREMENTS,
                        pendingItems.joinToString("; ")
                    )
                }
            }

            val canonicalPreviousGroup = masterStudentByCurp(source.alumnoCurp)?.group?.takeIf { it.isNotBlank() }
            val newStudentId = if (sourceMovement == "NUEVO INGRESO") {
                "MASTER-V2-${source.folio.trim().uppercase()}"
            } else null
            val request = AnnualEnrollmentFlowRequest(
                declaredMovement = source.tramite,
                normalizedCurp = source.alumnoCurp,
                sourcePreApplicationFolio = source.folio,
                requestedGrade = source.gradoSolicitado,
                previousGroup = canonicalPreviousGroup,
                schoolYear = source.cicloEscolar,
                newStudentId = newStudentId,
                studentFullName = source.alumnoNombreCompleto,
                actor = "Secretaría",
                occurredAt = "HOY ${com.example.formatTimestamp("hh:mm a")}"
            )
            val annualResult = AnnualEnrollmentFlowCoordinator.process(request)
            val institutionalResult = when (annualResult) {
                is AnnualEnrollmentFlowResult.Conflict ->
                    InstitutionalAnnualEnrollmentResult.AnnualConflict(annualResult)
                is AnnualEnrollmentFlowResult.AlreadyCompleted -> {
                    val live = _sharedPreApplications.value.filter {
                        it.folio.trim().uppercase() == source.folio.trim().uppercase()
                    }
                    if (live.size == 1 && live.single().readinessStatus == ReadinessStatus.CONVERTED) {
                        InstitutionalAnnualEnrollmentResult.AlreadyCompleted(annualResult)
                    } else {
                        InstitutionalAnnualEnrollmentResult.SynchronizationIncomplete(
                            annualResult = annualResult,
                            annualEnrollment = annualResult.enrollmentRecord,
                            cause = if (live.size == 1 && live.single().readinessStatus == ReadinessStatus.READY) {
                                PreApplicationSynchronizationCause.PREVIOUSLY_UNSYNCHRONIZED
                            } else {
                                synchronizationCauseFor(live, source)
                            },
                            message = "La anualidad existe, pero la pre-solicitud no está sincronizada como CONVERTED."
                        )
                    }
                }
                is AnnualEnrollmentFlowResult.Completed ->
                    synchronizeAnnualResult(source, annualResult, conversionSynchronizer)
                is AnnualEnrollmentFlowResult.NeedsDecision ->
                    synchronizeAnnualResult(source, annualResult, conversionSynchronizer)
            }
            _v2Result.value = institutionalResult
            return institutionalResult
        }

        private fun synchronizeAnnualResult(
            source: PreApplication,
            annualResult: AnnualEnrollmentFlowResult,
            conversionSynchronizer: InstitutionalPreApplicationSynchronizer
        ): InstitutionalAnnualEnrollmentResult {
            val synchronization = conversionSynchronizer.synchronize(
                source = source,
                readState = { _sharedPreApplications.value },
                compareAndSet = { expected, updated ->
                    _sharedPreApplications.compareAndSet(expected, updated)
                }
            )
            return when (synchronization) {
                is PreApplicationConversionResult.Converted,
                is PreApplicationConversionResult.AlreadyConverted -> when (annualResult) {
                    is AnnualEnrollmentFlowResult.Completed ->
                        InstitutionalAnnualEnrollmentResult.Completed(annualResult)
                    is AnnualEnrollmentFlowResult.NeedsDecision ->
                        InstitutionalAnnualEnrollmentResult.NeedsDecision(annualResult)
                    else -> error("Resultado anual no sincronizable: $annualResult")
                }
                is PreApplicationConversionResult.Incomplete ->
                    InstitutionalAnnualEnrollmentResult.SynchronizationIncomplete(
                        annualResult = annualResult,
                        annualEnrollment = annualEnrollmentFor(annualResult),
                        cause = synchronization.cause,
                        message = "La anualidad fue persistida, pero la pre-solicitud no pudo sincronizarse."
                    )
            }
        }

        private fun annualEnrollmentFor(result: AnnualEnrollmentFlowResult): AnnualEnrollmentRecord? {
            val folio = when (result) {
                is AnnualEnrollmentFlowResult.Completed -> result.folio
                is AnnualEnrollmentFlowResult.NeedsDecision -> result.folio
                is AnnualEnrollmentFlowResult.AlreadyCompleted -> result.enrollmentRecord.sourcePreApplicationFolio
                is AnnualEnrollmentFlowResult.Conflict -> return null
            }
            return MockSaseData.annualEnrollments.value.singleOrNull {
                it.sourcePreApplicationFolio.trim().uppercase() == folio.trim().uppercase()
            }
        }

        private fun synchronizationCauseFor(
            live: List<PreApplication>,
            source: PreApplication
        ): PreApplicationSynchronizationCause = when {
            live.isEmpty() -> PreApplicationSynchronizationCause.PRE_APPLICATION_NOT_FOUND
            live.size > 1 -> PreApplicationSynchronizationCause.AMBIGUOUS_FOLIO
            live.single().status != PreApplicationStatus.ACEPTADA ->
                PreApplicationSynchronizationCause.STATUS_CHANGED
            live.single().alumnoCurp.filterNot(Char::isWhitespace).uppercase() !=
                source.alumnoCurp.filterNot(Char::isWhitespace).uppercase() ||
                live.single().alumnoNombreCompleto.trim() != source.alumnoNombreCompleto.trim() ||
                live.single().cicloEscolar.trim() != source.cicloEscolar.trim() ||
                live.single().gradoSolicitado != source.gradoSolicitado ||
                live.single().tramite.trim().uppercase().replace('Ó', 'O') !=
                source.tramite.trim().uppercase().replace('Ó', 'O') ->
                PreApplicationSynchronizationCause.IDENTITY_CHANGED
            else -> PreApplicationSynchronizationCause.READINESS_CHANGED
        }
    }

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    // Block A — Pre-solicitud
    private val _tipoTramite = MutableStateFlow("Nuevo Ingreso")
    val tipoTramite: StateFlow<String> = _tipoTramite.asStateFlow()

    private val _cicloEscolar = MutableStateFlow("2026-2027")
    val cicloEscolar: StateFlow<String> = _cicloEscolar.asStateFlow()

    private val _gradoSolicitado = MutableStateFlow(0)
    val gradoSolicitado: StateFlow<Int> = _gradoSolicitado.asStateFlow()

    private val _apellidoPaterno = MutableStateFlow("")
    val apellidoPaterno: StateFlow<String> = _apellidoPaterno.asStateFlow()

    private val _apellidoMaterno = MutableStateFlow("")
    val apellidoMaterno: StateFlow<String> = _apellidoMaterno.asStateFlow()

    private val _nombre = MutableStateFlow("")
    val nombre: StateFlow<String> = _nombre.asStateFlow()

    private val _nombreCompleto = MutableStateFlow("")
    val nombreCompleto: StateFlow<String> = _nombreCompleto.asStateFlow()

    private val _curp = MutableStateFlow("")
    val curp: StateFlow<String> = _curp.asStateFlow()

    private val _diaNacimiento = MutableStateFlow("")
    val diaNacimiento: StateFlow<String> = _diaNacimiento.asStateFlow()

    private val _mesNacimiento = MutableStateFlow("")
    val mesNacimiento: StateFlow<String> = _mesNacimiento.asStateFlow()

    private val _anioNacimiento = MutableStateFlow("")
    val anioNacimiento: StateFlow<String> = _anioNacimiento.asStateFlow()

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

    private val _promedioGradoAnterior = MutableStateFlow("")
    val promedioGradoAnterior: StateFlow<String> = _promedioGradoAnterior.asStateFlow()

    private val _curpSugerida = MutableStateFlow("")
    val curpSugerida: StateFlow<String> = _curpSugerida.asStateFlow()

    private val _curpEditedByUser = MutableStateFlow(false)

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

    private val _personaTramiteNombre = MutableStateFlow("")
    val personaTramiteNombre: StateFlow<String> = _personaTramiteNombre.asStateFlow()

    private val _personaTramiteParentesco = MutableStateFlow("")
    val personaTramiteParentesco: StateFlow<String> = _personaTramiteParentesco.asStateFlow()

    private val _personaTramiteTelefono = MutableStateFlow("")
    val personaTramiteTelefono: StateFlow<String> = _personaTramiteTelefono.asStateFlow()

    private val _personaTramiteIdentificacion = MutableStateFlow("")
    val personaTramiteIdentificacion: StateFlow<String> = _personaTramiteIdentificacion.asStateFlow()

    private val _usarPersonaTramiteComoContacto = MutableStateFlow(false)
    val usarPersonaTramiteComoContacto: StateFlow<Boolean> = _usarPersonaTramiteComoContacto.asStateFlow()

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
    private val _isUdeii = MutableStateFlow(false)
    val isUdeii: StateFlow<Boolean> = _isUdeii.asStateFlow()

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
        DocumentoItem("reglamentoEscolar", "Reglamento Escolar (placeholder PDF)"),
        DocumentoItem("marcoConvivenciaDoc", "Marco para la Convivencia (placeholder PDF)"),
        DocumentoItem("avisoPrivacidadDoc", "Aviso de Privacidad (placeholder PDF)"),
        DocumentoItem("corresponsabilidadDoc", "Corresponsabilidad Familiar (placeholder PDF)"),
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
    fun setApellidoPaterno(v: String) {
        _apellidoPaterno.value = v.uppercase()
        rebuildNombreCompleto()
        rebuildCurpSuggestion()
    }
    fun setApellidoMaterno(v: String) {
        _apellidoMaterno.value = v.uppercase()
        rebuildNombreCompleto()
        rebuildCurpSuggestion()
    }
    fun setNombre(v: String) {
        _nombre.value = v.uppercase()
        rebuildNombreCompleto()
        rebuildCurpSuggestion()
    }
    fun setNombreCompleto(v: String) { _nombreCompleto.value = v.uppercase() }
    private fun rebuildNombreCompleto() {
        val parts = listOf(_apellidoPaterno.value.trim(), _apellidoMaterno.value.trim(), _nombre.value.trim()).filter { it.isNotEmpty() }
        _nombreCompleto.value = parts.joinToString(" ")
    }
    fun setCurp(v: String) {
        val upper = v.filter { it.isLetterOrDigit() }.uppercase().take(18)
        _curp.value = upper
        _curpEditedByUser.value = true
        if (upper.length == 18) {
            autoFillFromCurp(upper)
        }
    }

    private fun autoFillFromCurp(curp: String) {
        val SexoMap = mapOf("H" to "Masculino", "M" to "Femenino")
        val EntidadMap = mapOf(
            "AS" to "Aguascalientes", "BC" to "Baja California", "BS" to "Baja California Sur",
            "CC" to "Campeche", "CL" to "Coahuila", "CM" to "Colima", "CS" to "Chiapas",
            "CH" to "Chihuahua", "DF" to "Ciudad de México", "DG" to "Durango", "GT" to "Guanajuato",
            "GR" to "Guerrero", "HG" to "Hidalgo", "JC" to "Jalisco", "MC" to "México",
            "MN" to "Michoacán", "MS" to "Morelos", "NT" to "Nayarit", "NL" to "Nuevo León",
            "OC" to "Oaxaca", "PL" to "Puebla", "QT" to "Querétaro", "QR" to "Quintana Roo",
            "SP" to "San Luis Potosí", "SL" to "Sinaloa", "SR" to "Sonora", "TC" to "Tabasco",
            "TS" to "Tamaulipas", "TL" to "Tlaxcala", "VZ" to "Veracruz", "YN" to "Yucatán",
            "ZS" to "Zacatecas", "NE" to "Nacido en el Extranjero"
        )
        try {
            val yy = curp.substring(4, 6).toInt()
            val mm = curp.substring(6, 8).toInt()
            val dd = curp.substring(8, 10).toInt()
            val year = if (yy > 50) 1900 + yy else 2000 + yy
            if (mm in 1..12 && dd in 1..31) {
                val meses = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
                _diaNacimiento.value = dd.toString().padStart(2, '0')
                _mesNacimiento.value = meses[mm - 1]
                _anioNacimiento.value = year.toString()
                rebuildFechaNacimiento()
            } else {
                _diaNacimiento.value = ""
                _mesNacimiento.value = ""
                _anioNacimiento.value = ""
                _fechaNacimiento.value = ""
            }
        } catch (_: Exception) {
            _diaNacimiento.value = ""
            _mesNacimiento.value = ""
            _anioNacimiento.value = ""
            _fechaNacimiento.value = ""
        }
        val sexoCode = curp.substring(10, 11)
        SexoMap[sexoCode]?.let { _sexo.value = it }
        val entidadCode = curp.substring(11, 13)
        EntidadMap[entidadCode]?.let { _entidadNacimiento.value = it }
    }
    fun setDiaNacimiento(v: String) {
        _diaNacimiento.value = v
        rebuildFechaNacimiento()
        rebuildCurpSuggestion()
    }
    fun setMesNacimiento(v: String) {
        _mesNacimiento.value = v
        rebuildFechaNacimiento()
        rebuildCurpSuggestion()
    }
    fun setAnioNacimiento(v: String) {
        _anioNacimiento.value = v
        rebuildFechaNacimiento()
        rebuildCurpSuggestion()
    }
    fun setFechaNacimiento(v: String) { _fechaNacimiento.value = v }
    private fun rebuildFechaNacimiento() {
        val d = _diaNacimiento.value
        val m = _mesNacimiento.value
        val a = _anioNacimiento.value
        if (d.isNotEmpty() && m.isNotEmpty() && a.isNotEmpty()) {
            _fechaNacimiento.value = "$d/$m/$a"
        } else {
            _fechaNacimiento.value = ""
        }
    }
    fun setSexo(v: String) { _sexo.value = v; rebuildCurpSuggestion() }
    fun setNacionalidad(v: String) { _nacionalidad.value = v.uppercase() }
    fun setEntidadNacimiento(v: String) { _entidadNacimiento.value = v.uppercase(); rebuildCurpSuggestion() }
    fun setTelefonoPrincipal(v: String) { _telefonoPrincipal.value = v.filter { it.isDigit() }.take(10) }
    fun setCorreo(v: String) { _correo.value = v }
    fun setEscuelaProcedencia(v: String) { _escuelaProcedencia.value = v.uppercase() }
    fun setPromedioGradoAnterior(v: String) { _promedioGradoAnterior.value = v.filter { it.isDigit() || it == '.' }.take(4) }
    fun setAceptaAvisoPrivacidad(v: Boolean) { _aceptaAvisoPrivacidad.value = v }
    fun setDomicilio(v: String) { _domicilio.value = v.uppercase() }
    fun setTelefonoCasa(v: String) { _telefonoCasa.value = v.filter { it.isDigit() }.take(10) }

    fun setResponsableNombre(v: String) { _responsableNombre.value = v.uppercase() }
    fun setResponsableParentesco(v: String) { _responsableParentesco.value = v }
    fun setResponsableTelefono(v: String) { _responsableTelefono.value = v.filter { it.isDigit() }.take(10) }
    fun setResponsableCorreo(v: String) { _responsableCorreo.value = v }
    fun setResponsableViveConAlumno(v: Boolean) { _responsableViveConAlumno.value = v }
    fun setResponsablePuedeRecoger(v: Boolean) { _responsablePuedeRecoger.value = v }

    fun setPersonaTramiteNombre(v: String) {
        _personaTramiteNombre.value = v.uppercase()
        syncPersonaTramiteToContactoIfNeeded()
    }
    fun setPersonaTramiteParentesco(v: String) {
        _personaTramiteParentesco.value = v
        syncPersonaTramiteToContactoIfNeeded()
    }
    fun setPersonaTramiteTelefono(v: String) {
        _personaTramiteTelefono.value = v.filter { it.isDigit() }.take(10)
        syncPersonaTramiteToContactoIfNeeded()
    }
    fun setPersonaTramiteIdentificacion(v: String) { _personaTramiteIdentificacion.value = v.uppercase() }
    fun setUsarPersonaTramiteComoContacto(v: Boolean) {
        _usarPersonaTramiteComoContacto.value = v
        syncPersonaTramiteToContactoIfNeeded()
    }

    private fun syncPersonaTramiteToContactoIfNeeded() {
        if (!_usarPersonaTramiteComoContacto.value) return
        _responsableNombre.value = _personaTramiteNombre.value
        _responsableParentesco.value = _personaTramiteParentesco.value
        _responsableTelefono.value = _personaTramiteTelefono.value
    }

    private fun rebuildCurpSuggestion() {
        val yy = _anioNacimiento.value.takeLast(2)
        val monthIndex = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic").indexOf(_mesNacimiento.value)
        val month = if (monthIndex >= 0) (monthIndex + 1).toString().padStart(2, '0') else ""
        val day = _diaNacimiento.value.padStart(2, '0').takeIf { it.length == 2 }.orEmpty()
        val sex = when (_sexo.value) { "Masculino" -> "H"; "Femenino" -> "M"; else -> "" }
        val entity = when {
            _entidadNacimiento.value.contains("MEXICO") || _entidadNacimiento.value.contains("MÉXICO") -> "MC"
            _entidadNacimiento.value.contains("CDMX") || _entidadNacimiento.value.contains("CIUDAD") -> "DF"
            _entidadNacimiento.value.contains("EXTRANJ") -> "NE"
            else -> _entidadNacimiento.value.take(2).padEnd(2, 'X')
        }
        val firstSurname = _apellidoPaterno.value.filter { it.isLetter() }.padEnd(2, 'X')
        val secondSurname = _apellidoMaterno.value.filter { it.isLetter() }.firstOrNull()?.toString() ?: "X"
        val nameInitial = _nombre.value.filter { it.isLetter() }.firstOrNull()?.toString() ?: "X"
        val suggestion = (firstSurname.take(2) + secondSurname + nameInitial + yy + month + day + sex + entity + "XXX00").uppercase().take(18)
        _curpSugerida.value = suggestion
        if (!_curpEditedByUser.value && suggestion.length == 18) _curp.value = suggestion
    }

    fun addAutorizado(nombre: String, parentesco: String, telefono: String) {
        val id = "AUT-${_autorizados.value.size + 1}-${Random.nextInt(100, 999)}"
        _autorizados.value = _autorizados.value + AutorizadoItem(id, nombre.uppercase(), parentesco.uppercase(), telefono.filter { it.isDigit() }.take(10))
    }

    fun removeAutorizado(id: String) {
        _autorizados.value = _autorizados.value.filter { it.id != id }
    }

    fun setServicioMedico(v: String) { _servicioMedico.value = v.uppercase() }
    fun setNumeroAfiliacionPoliza(v: String) { _numeroAfiliacionPoliza.value = v.uppercase() }
    fun setTipoSangre(v: String) { _tipoSangre.value = v.uppercase() }
    fun setTieneAlergias(v: Boolean) { _tieneAlergias.value = v }
    fun setAlergiasDetalle(v: String) { _alergiasDetalle.value = v.uppercase() }
    fun setTienePadecimientos(v: Boolean) { _tienePadecimientos.value = v }
    fun setPadecimientosDetalle(v: String) { _padecimientosDetalle.value = v.uppercase() }
    fun setTomaMedicamentos(v: Boolean) { _tomaMedicamentos.value = v }
    fun setMedicamentosDetalle(v: String) { _medicamentosDetalle.value = v.uppercase() }
    fun setRestriccionActividadFisica(v: Boolean) { _restriccionActividadFisica.value = v }
    fun setRestriccionActividadFisicaDetalle(v: String) { _restriccionActividadFisicaDetalle.value = v }
    fun setUsaLentes(v: Boolean) { _usaLentes.value = v }
    fun setDificultadVisualReferida(v: Boolean) { _dificultadVisualReferida.value = v }
    fun setDificultadVisualDetalle(v: String) { _dificultadVisualDetalle.value = v }
    fun setDificultadAuditivaReferida(v: Boolean) { _dificultadAuditivaReferida.value = v }
    fun setDificultadAuditivaDetalle(v: String) { _dificultadAuditivaDetalle.value = v }
    fun setSaludBucalReferida(v: String) { _saludBucalReferida.value = v }
    fun setCartillaVacunacionActualizada(v: Boolean) { _cartillaVacunacionActualizada.value = v }

    fun setViveConQuien(v: String) { _viveConQuien.value = v.uppercase() }
    fun setTipoFamilia(v: String) { _tipoFamilia.value = v }
    fun setHijoUnico(v: Boolean) { _hijoUnico.value = v }
    fun setLugarEntreHermanos(v: String) { _lugarEntreHermanos.value = v }
    fun setHermanosEnEscuela(v: Boolean) { _hermanosEnEscuela.value = v }
    fun setIntegrantesHogar(v: String) { _integrantesHogar.value = v.filter { it.isDigit() }.take(2) }
    fun setPrincipalSostenEconomico(v: String) { _principalSostenEconomico.value = v.uppercase() }
    fun setIngresoFamiliarRango(v: String) { _ingresoFamiliarRango.value = v }
    fun setTipoVivienda(v: String) { _tipoVivienda.value = v }
    fun setServiciosBasicos(v: Boolean) { _serviciosBasicos.value = v }
    fun setInternetCasa(v: Boolean) { _internetCasa.value = v }
    fun setDispositivoTareas(v: String) { _dispositivoTareas.value = v }
    fun setBecaApoyoSocial(v: String) { _becaApoyoSocial.value = v.uppercase() }
    fun setMedioTransporte(v: String) { _medioTransporte.value = v }
    fun setDificultadComprarMateriales(v: Boolean) { _dificultadComprarMateriales.value = v }
    fun setPersonaAtiendeAvisos(v: String) { _personaAtiendeAvisos.value = v.uppercase() }
    fun setHorarioPreferenteComunicacion(v: String) { _horarioPreferenteComunicacion.value = v }
    fun setPuedeAcudirCitatorios(v: Boolean) { _puedeAcudirCitatorios.value = v }

    fun setIsUdeii(v: Boolean) { _isUdeii.value = v }
    fun setUdeiiAntecedenteApoyo(v: String) { _udeiiAntecedenteApoyo.value = v.uppercase() }
    fun setUdeiiTerapiaLenguaje(v: Boolean) { _udeiiTerapiaLenguaje.value = v }
    fun setUdeiiApoyoPsicologico(v: Boolean) { _udeiiApoyoPsicologico.value = v }
    fun setUdeiiApoyoPedagogico(v: Boolean) { _udeiiApoyoPedagogico.value = v }
    fun setUdeiiDocumentosDisponibles(v: String) { _udeiiDocumentosDisponibles.value = v.uppercase() }
    fun setUdeiiInformeEscuelaAnterior(v: Boolean) { _udeiiInformeEscuelaAnterior.value = v }
    fun setUdeiiEvaluacionPsicopedagogica(v: Boolean) { _udeiiEvaluacionPsicopedagogica.value = v }
    fun setUdeiiPlanIntervencion(v: Boolean) { _udeiiPlanIntervencion.value = v }
    fun setUdeiiPortafolio(v: Boolean) { _udeiiPortafolio.value = v }
    fun setUdeiiObservaciones(v: String) { _udeiiObservaciones.value = v.uppercase() }

    private fun validateStep(step: Int): Boolean {
        val errs = mutableMapOf<String, String>()
        when (step) {
            0 -> {
                if (_apellidoPaterno.value.isBlank()) errs["apellidoPaterno"] = "Apellido paterno obligatorio"
                if (_nombre.value.isBlank()) errs["nombre"] = "Nombre(s) obligatorio"
                if (_curp.value.length != 18) errs["curp"] = "CURP debe tener 18 caracteres"
                if (_fechaNacimiento.value.isBlank()) errs["fechaNac"] = "Obligatorio"
                if (_gradoSolicitado.value == 0) errs["grado"] = "Selecciona un grado"
                val promedio = _promedioGradoAnterior.value.toDoubleOrNull()
                if (_gradoSolicitado.value in 1..3 && (promedio == null || promedio !in 5.0..10.0)) {
                    errs["promedio"] = "Promedio requerido entre 5.0 y 10.0"
                }
                if (_telefonoPrincipal.value.length < 10) errs["telefono"] = "10 dígitos requeridos"
                if (!_aceptaAvisoPrivacidad.value) errs["aviso"] = "Debes aceptar el aviso de privacidad"
            }
            1 -> {
                if (_personaTramiteNombre.value.isBlank()) errs["personaTramite"] = "Persona que realiza el trámite obligatoria"
                if (_personaTramiteParentesco.value.isBlank()) errs["personaTramiteParentesco"] = "Parentesco obligatorio"
                if (_personaTramiteTelefono.value.length < 10) errs["personaTramiteTelefono"] = "Teléfono 10 dígitos requerido"
                if (_personaTramiteIdentificacion.value.isBlank()) errs["personaTramiteIdentificacion"] = "Identificación presentada obligatoria"
                if (_responsableNombre.value.isBlank()) errs["responsable"] = "Nombre del responsable obligatorio"
                if (_responsableParentesco.value.isBlank()) errs["parentesco"] = "Parentesco obligatorio"
                if (_responsableTelefono.value.length < 10) errs["responsableTel"] = "Teléfono 10 dígitos requerido"
            }
            2 -> {
                if (_servicioMedico.value.isBlank()) errs["servicioMedico"] = "Servicio médico obligatorio"
                if (_tipoSangre.value.isBlank()) errs["tipoSangre"] = "Tipo de sangre obligatorio"
                if (_tieneAlergias.value && _alergiasDetalle.value.isBlank()) errs["alergiasDetalle"] = "Detalle de alergias obligatorio"
                if (_tienePadecimientos.value && _padecimientosDetalle.value.isBlank()) errs["padecimientosDetalle"] = "Detalle de padecimientos obligatorio"
                if (_tomaMedicamentos.value && _medicamentosDetalle.value.isBlank()) errs["medicamentosDetalle"] = "Detalle de medicamentos obligatorio"
                if (_viveConQuien.value.isBlank()) errs["viveConQuien"] = "Indica con quién vive el alumno"
                if (_tipoFamilia.value.isBlank()) errs["tipoFamilia"] = "Tipo de familia obligatorio"
                val hogar = _integrantesHogar.value.toIntOrNull()
                if (hogar == null || hogar < 1) errs["integrantesHogar"] = "Número de integrantes inválido"
                if (_personaAtiendeAvisos.value.isBlank()) errs["personaAtiendeAvisos"] = "Persona que atiende avisos obligatoria"
            }
            3 -> {
                if (_documentos.value.none { it.declarado }) errs["documentos"] = "Debes declarar al menos un documento"
                val aceptaUsoDatos = _consentimientos.value.find { it.key == "usoDatos" }?.aceptado == true
                val aceptaReglamento = _consentimientos.value.find { it.key == "reglamento" }?.aceptado == true
                val aceptaMarcoConvivencia = _consentimientos.value.find { it.key == "marcoConvivencia" }?.aceptado == true
                if (!aceptaUsoDatos) errs["consentimientoUsoDatos"] = "Debes aceptar el uso de datos para expediente"
                if (!aceptaReglamento) errs["consentimientoReglamento"] = "Debes aceptar el reglamento escolar"
                if (!aceptaMarcoConvivencia) errs["consentimientoMarcoConvivencia"] = "Debes aceptar el marco para la convivencia"
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
        if (_apellidoPaterno.value.isBlank()) errs["apellidoPaterno"] = "Apellido paterno obligatorio"
        if (_nombre.value.isBlank()) errs["nombre"] = "Nombre(s) obligatorio"
        if (_curp.value.length != 18) errs["curp"] = "CURP debe tener 18 caracteres"
        if (_fechaNacimiento.value.isBlank()) errs["fechaNac"] = "Obligatorio"
        if (_gradoSolicitado.value == 0) errs["grado"] = "Selecciona un grado"
        val promedio = _promedioGradoAnterior.value.toDoubleOrNull()
        if (_gradoSolicitado.value in 1..3 && (promedio == null || promedio !in 5.0..10.0)) {
            errs["promedio"] = "Promedio requerido entre 5.0 y 10.0"
        }
        if (_telefonoPrincipal.value.length < 10) errs["telefono"] = "10 dígitos requeridos"
        if (!_aceptaAvisoPrivacidad.value) errs["aviso"] = "Debes aceptar el aviso de privacidad"
        if (_personaTramiteNombre.value.isBlank()) errs["personaTramite"] = "Persona que realiza el trámite obligatoria"
        if (_personaTramiteParentesco.value.isBlank()) errs["personaTramiteParentesco"] = "Parentesco obligatorio"
        if (_personaTramiteTelefono.value.length < 10) errs["personaTramiteTelefono"] = "Teléfono 10 dígitos requerido"
        if (_personaTramiteIdentificacion.value.isBlank()) errs["personaTramiteIdentificacion"] = "Identificación presentada obligatoria"
        if (_responsableNombre.value.isBlank()) errs["responsable"] = "Nombre del responsable obligatorio"
        if (_responsableParentesco.value.isBlank()) errs["parentesco"] = "Parentesco obligatorio"
        if (_responsableTelefono.value.length < 10) errs["responsableTel"] = "Teléfono 10 dígitos requerido"
        if (_servicioMedico.value.isBlank()) errs["servicioMedico"] = "Servicio médico obligatorio"
        if (_tipoSangre.value.isBlank()) errs["tipoSangre"] = "Tipo de sangre obligatorio"
        if (_tieneAlergias.value && _alergiasDetalle.value.isBlank()) errs["alergiasDetalle"] = "Detalle de alergias obligatorio"
        if (_tienePadecimientos.value && _padecimientosDetalle.value.isBlank()) errs["padecimientosDetalle"] = "Detalle de padecimientos obligatorio"
        if (_tomaMedicamentos.value && _medicamentosDetalle.value.isBlank()) errs["medicamentosDetalle"] = "Detalle de medicamentos obligatorio"
        if (_viveConQuien.value.isBlank()) errs["viveConQuien"] = "Indica con quién vive el alumno"
        if (_tipoFamilia.value.isBlank()) errs["tipoFamilia"] = "Tipo de familia obligatorio"
        val hogar = _integrantesHogar.value.toIntOrNull()
        if (hogar == null || hogar < 1) errs["integrantesHogar"] = "Número de integrantes inválido"
        if (_personaAtiendeAvisos.value.isBlank()) errs["personaAtiendeAvisos"] = "Persona que atiende avisos obligatoria"
        if (_documentos.value.none { it.declarado }) errs["documentos"] = "Debes declarar al menos un documento"
        val usoDatos = _consentimientos.value.find { it.key == "usoDatos" }?.aceptado == true
        val corresponsabilidad = _consentimientos.value.find { it.key == "corresponsabilidad" }?.aceptado == true
        if (!usoDatos) errs["consentimientoUsoDatos"] = "Debes aceptar el uso de datos para expediente"
        if (!corresponsabilidad) errs["consentimientoCorresponsabilidad"] = "Debes aceptar la corresponsabilidad familiar"
        if (errs.isNotEmpty()) {
            _errors.value = errs
            _currentStep.value = when {
                errs.containsKey("consentimientoUsoDatos") || errs.containsKey("consentimientoCorresponsabilidad") -> 4
                errs.keys.any { it.startsWith("personaTramite") } || errs.containsKey("responsable") || errs.containsKey("parentesco") || errs.containsKey("responsableTel") -> 1
                errs.keys.any { it in setOf("servicioMedico", "tipoSangre", "alergiasDetalle", "padecimientosDetalle", "medicamentosDetalle", "viveConQuien", "tipoFamilia", "integrantesHogar", "personaAtiendeAvisos") } -> 2
                errs.containsKey("documentos") -> 3
                else -> 0
            }
            return
        }

        _isSubmitting.value = true
        val submission = submitFamilyPreApplication(
            PreApplication(
                folio = "",
                status = PreApplicationStatus.BORRADOR,
                submittedAt = null,
                tramite = _tipoTramite.value,
                cicloEscolar = _cicloEscolar.value,
                gradoSolicitado = _gradoSolicitado.value,
                alumnoNombreCompleto = _nombreCompleto.value.trim(),
                alumnoCurp = _curp.value.trim().uppercase(),
                alumnoFechaNacimiento = _fechaNacimiento.value.trim(),
                alumnoSexo = _sexo.value,
                alumnoNacionalidad = _nacionalidad.value,
                alumnoEntidadNacimiento = _entidadNacimiento.value,
                alumnoDomicilio = _domicilio.value.trim(),
                alumnoTelefonoCasa = _telefonoCasa.value,
                escuelaProcedencia = _escuelaProcedencia.value,
                promedioGradoAnterior = promedio,
                personaTramite = PersonaTramite(
                    nombreCompleto = _personaTramiteNombre.value.trim(),
                    parentesco = _personaTramiteParentesco.value.trim(),
                    telefono = _personaTramiteTelefono.value,
                    identificacionPresentada = _personaTramiteIdentificacion.value.trim(),
                    usarComoContactoPrincipal = _usarPersonaTramiteComoContacto.value
                ),
                responsables = listOf(
                    Responsable(
                        nombreCompleto = _responsableNombre.value.trim(),
                        parentesco = _responsableParentesco.value.trim(),
                        telefono = _responsableTelefono.value,
                        correo = _responsableCorreo.value.ifBlank { null },
                        domicilioDistinto = false,
                        domicilio = null,
                        viveConAlumno = _responsableViveConAlumno.value,
                        contactoPrincipal = true,
                        puedeRecoger = _responsablePuedeRecoger.value,
                        ocupacion = "",
                        horarioContacto = "",
                        identificacionApresentar = ""
                    )
                ),
                autorizados = _autorizados.value.map {
                    AutorizadoPreSolicitud(
                        nombreCompleto = it.nombre,
                        parentesco = it.parentesco,
                        telefono = it.telefono,
                        observaciones = ""
                    )
                },
                fichaMedicaFamiliar = FichaMedicaFamiliar(
                    servicioMedico = _servicioMedico.value,
                    numeroAfiliacion = _numeroAfiliacionPoliza.value.ifBlank { null },
                    tipoSangre = _tipoSangre.value.ifBlank { null },
                    alergias = if (_tieneAlergias.value) _alergiasDetalle.value else "",
                    padecimientos = if (_tienePadecimientos.value) _padecimientosDetalle.value else "",
                    medicamentos = if (_tomaMedicamentos.value) _medicamentosDetalle.value else "",
                    restriccionFisica = if (_restriccionActividadFisica.value) _restriccionActividadFisicaDetalle.value else "",
                    usaLentes = _usaLentes.value,
                    dificultadVisualAuditiva = buildString {
                        if (_dificultadVisualReferida.value) append(_dificultadVisualDetalle.value)
                        if (_dificultadAuditivaReferida.value) {
                            if (isNotBlank()) append(" / ")
                            append(_dificultadAuditivaDetalle.value)
                        }
                    },
                    saludBucal = _saludBucalReferida.value,
                    cartillaVacunacion = _cartillaVacunacionActualizada.value
                ),
                contextoSociofamiliar = ContextoSociofamiliar(
                    viveConQuien = _viveConQuien.value,
                    tipoFamilia = _tipoFamilia.value,
                    hijoUnico = _hijoUnico.value,
                    lugarEntreHermanos = _lugarEntreHermanos.value.filter { it.isDigit() }.toIntOrNull() ?: 0,
                    hermanosEnEscuela = _hermanosEnEscuela.value,
                    integrantesHogar = _integrantesHogar.value.toIntOrNull() ?: 0,
                    sostenEconomico = _principalSostenEconomico.value,
                    ingresoRangos = _ingresoFamiliarRango.value,
                    tipoVivienda = _tipoVivienda.value,
                    serviciosBásicos = _serviciosBasicos.value,
                    internet = _internetCasa.value,
                    dispositivoTareas = _dispositivoTareas.value,
                    becaApoyo = _becaApoyoSocial.value,
                    transporte = _medioTransporte.value,
                    dificultadMateriales = _dificultadComprarMateriales.value,
                    atiendeAvisos = _personaAtiendeAvisos.value,
                    horarioComunicacion = _horarioPreferenteComunicacion.value,
                    puedeAcudirCitatorios = _puedeAcudirCitatorios.value
                ),
                antecedentesUdeii = AntecedentesUdeii(
                    antecedenteApoyo = _udeiiAntecedenteApoyo.value,
                    terapiaLenguaje = _udeiiTerapiaLenguaje.value,
                    apoyoPsicologico = _udeiiApoyoPsicologico.value,
                    apoyoPedagogico = _udeiiApoyoPedagogico.value,
                    documentosDisponibles = _udeiiDocumentosDisponibles.value,
                    informeEscuelaAnterior = _udeiiInformeEscuelaAnterior.value,
                    evaluacionPsicopedagogica = _udeiiEvaluacionPsicopedagogica.value,
                    planIntervencion = _udeiiPlanIntervencion.value,
                    portafolio = _udeiiPortafolio.value,
                    observacionesFamiliares = _udeiiObservaciones.value
                ),
                documentosDeclarados = _documentos.value.map { DocumentoDeclarado(it.label, it.declarado, false) },
                consentimientos = ConsentimientosFamiliares(
                    avisoPrivacidad = _consentimientos.value.find { it.key == "usoDatos" }?.aceptado == true,
                    usoDatosExpediente = _consentimientos.value.find { it.key == "usoDatos" }?.aceptado == true,
                    fotoAlumno = _consentimientos.value.find { it.key == "fotoAlumno" }?.aceptado == true,
                    fotoCredencial = _consentimientos.value.find { it.key == "fotoCredencial" }?.aceptado == true,
                    fotoAutorizados = _consentimientos.value.find { it.key == "fotoAutorizados" }?.aceptado == true,
                    comunicacionWhatsapp = _consentimientos.value.find { it.key == "comunicacion" }?.aceptado == true,
                    reglamentoInterno = _consentimientos.value.find { it.key == "reglamento" }?.aceptado == true,
                    marcoConvivencia = _consentimientos.value.find { it.key == "marcoConvivencia" }?.aceptado == true,
                    corresponsabilidadFamiliar = _consentimientos.value.find { it.key == "corresponsabilidad" }?.aceptado == true
                )
            )
        )

        when (submission) {
            is FamilySubmissionResult.Success -> {
                _submittedFolio.value = submission.preApplication.folio
                _errors.value = emptyMap()
            }
            else -> {
                _submittedFolio.value = null
                _errors.value = mapOf("submit" to submission.message)
            }
        }

        _isSubmitting.value = false
    }

    fun resetForm() {
        _currentStep.value = 0
        _tipoTramite.value = "Nuevo Ingreso"
        _cicloEscolar.value = "2026-2027"
        _apellidoPaterno.value = ""
        _apellidoMaterno.value = ""
        _nombre.value = ""
        _nombreCompleto.value = ""
        _curp.value = ""
        _curpSugerida.value = ""
        _curpEditedByUser.value = false
        _diaNacimiento.value = ""
        _mesNacimiento.value = ""
        _anioNacimiento.value = ""
        _fechaNacimiento.value = ""
        _gradoSolicitado.value = 0
        _sexo.value = ""
        _nacionalidad.value = "Mexicana"
        _entidadNacimiento.value = ""
        _telefonoPrincipal.value = ""
        _correo.value = ""
        _escuelaProcedencia.value = ""
        _promedioGradoAnterior.value = ""
        _aceptaAvisoPrivacidad.value = false
        _domicilio.value = ""
        _telefonoCasa.value = ""
        _responsableNombre.value = ""
        _responsableParentesco.value = ""
        _responsableTelefono.value = ""
        _responsableCorreo.value = ""
        _responsableViveConAlumno.value = true
        _responsablePuedeRecoger.value = true
        _personaTramiteNombre.value = ""
        _personaTramiteParentesco.value = ""
        _personaTramiteTelefono.value = ""
        _personaTramiteIdentificacion.value = ""
        _usarPersonaTramiteComoContacto.value = false
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
        _isUdeii.value = false
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
