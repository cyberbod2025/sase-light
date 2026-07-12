package com.example.data.presolicitud

enum class PreApplicationAdministrativeField {
    PHONE,
    ADDRESS
}

sealed interface PreApplicationAdministrativeFieldChange {
    data object Omitted : PreApplicationAdministrativeFieldChange
    data class Replace(val value: String) : PreApplicationAdministrativeFieldChange
}

data class PreApplicationAdministrativeChanges(
    val phone: PreApplicationAdministrativeFieldChange = PreApplicationAdministrativeFieldChange.Omitted,
    val address: PreApplicationAdministrativeFieldChange = PreApplicationAdministrativeFieldChange.Omitted
)

data class PreApplicationAdministrativeDataSnapshot(
    val phone: String,
    val address: String
)

data class UpdatePreApplicationAdministrativeDataRequest(
    val folio: String,
    val expected: PreApplicationAdministrativeDataSnapshot,
    val changes: PreApplicationAdministrativeChanges
)

enum class PreApplicationAdministrativeValidationError {
    REQUIRED,
    INVALID_FORMAT
}

enum class PreApplicationAdministrativeConflictReason {
    STALE_DATA,
    NOT_EDITABLE,
    OFFICIAL_ENROLLMENT_EXISTS,
    AMBIGUOUS_FOLIO
}

sealed interface UpdatePreApplicationAdministrativeDataResult {
    data class Updated(
        val changedFields: Set<PreApplicationAdministrativeField>
    ) : UpdatePreApplicationAdministrativeDataResult

    data object NoChanges : UpdatePreApplicationAdministrativeDataResult

    data class Invalid(
        val errors: Map<PreApplicationAdministrativeField, PreApplicationAdministrativeValidationError>
    ) : UpdatePreApplicationAdministrativeDataResult

    data object NotFound : UpdatePreApplicationAdministrativeDataResult

    data class Conflict(
        val reason: PreApplicationAdministrativeConflictReason
    ) : UpdatePreApplicationAdministrativeDataResult
}

fun PreApplication.administrativeDataSnapshot(): PreApplicationAdministrativeDataSnapshot =
    PreApplicationAdministrativeDataSnapshot(
        phone = alumnoTelefonoCasa,
        address = alumnoDomicilio
    )
