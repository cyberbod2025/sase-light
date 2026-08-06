package com.example.audit

import com.example.data.auth.StaffRole

/**
 * Resultado observable de una decision institucional.
 *
 * Este modelo no persiste ni transmite eventos; solo define datos locales
 * seguros para que una capa autorizada los consuma en el futuro.
 */
enum class InstitutionalAuditResult {
    AUTHORIZED,
    DENIED,
    FAILED
}

/**
 * Evento minimo de auditoria institucional.
 *
 * No admite un campo libre de detalle para evitar que contrasenas, tokens,
 * CURP completas o datos medicos terminen dentro de la bitacora.
 */
data class InstitutionalAuditEvent(
    val institutionId: String,
    val actorProfileId: String,
    val membershipId: String,
    val activeRole: StaffRole,
    val action: String,
    val entityType: String,
    val entityId: String,
    val timestamp: String,
    val result: InstitutionalAuditResult,
    val sourcePlatform: String
) {
    /** Evita que [toString] replique contenido rechazado en logs de diagnostico. */
    override fun toString(): String = "InstitutionalAuditEvent(" +
        "institutionId=${InstitutionalAuditSanitizer.redactForDisplay(institutionId)}, " +
        "actorProfileId=${InstitutionalAuditSanitizer.redactForDisplay(actorProfileId)}, " +
        "membershipId=${InstitutionalAuditSanitizer.redactForDisplay(membershipId)}, " +
        "activeRole=$activeRole, " +
        "action=${InstitutionalAuditSanitizer.redactForDisplay(action)}, " +
        "entityType=${InstitutionalAuditSanitizer.redactForDisplay(entityType)}, " +
        "entityId=${InstitutionalAuditSanitizer.redactForDisplay(entityId)}, " +
        "timestamp=${InstitutionalAuditSanitizer.redactForDisplay(timestamp)}, " +
        "result=$result, " +
        "sourcePlatform=${InstitutionalAuditSanitizer.redactForDisplay(sourcePlatform)})"
}

enum class InstitutionalAuditField {
    INSTITUTION_ID,
    ACTOR_PROFILE_ID,
    MEMBERSHIP_ID,
    ACTION,
    ENTITY_TYPE,
    ENTITY_ID,
    TIMESTAMP,
    SOURCE_PLATFORM
}

enum class InstitutionalAuditViolationCode {
    REQUIRED,
    SENSITIVE_CONTENT
}

/** Una violacion nunca conserva ni repite el valor rechazado. */
data class InstitutionalAuditViolation(
    val field: InstitutionalAuditField,
    val code: InstitutionalAuditViolationCode
)

data class InstitutionalAuditValidation(
    val violations: List<InstitutionalAuditViolation>
) {
    val isValid: Boolean
        get() = violations.isEmpty()
}

/** Sanitizador puro para representaciones diagnosticas; no sustituye validacion. */
object InstitutionalAuditSanitizer {
    const val REDACTED: String = "[REDACTED]"

    fun redactForDisplay(value: String): String {
        val normalized = value.trim()
        return if (InstitutionalSensitiveContent.contains(normalized)) REDACTED else normalized
    }
}

/** Validador puro y fail-closed para eventos construidos localmente. */
object InstitutionalAuditValidator {
    fun validate(event: InstitutionalAuditEvent): InstitutionalAuditValidation {
        val violations = mutableListOf<InstitutionalAuditViolation>()
        val requiredText = listOf(
            InstitutionalAuditField.INSTITUTION_ID to event.institutionId,
            InstitutionalAuditField.ACTOR_PROFILE_ID to event.actorProfileId,
            InstitutionalAuditField.MEMBERSHIP_ID to event.membershipId,
            InstitutionalAuditField.ACTION to event.action,
            InstitutionalAuditField.ENTITY_TYPE to event.entityType,
            InstitutionalAuditField.ENTITY_ID to event.entityId,
            InstitutionalAuditField.TIMESTAMP to event.timestamp,
            InstitutionalAuditField.SOURCE_PLATFORM to event.sourcePlatform
        )

        requiredText.forEach { (field, value) ->
            val code = when {
                value.isBlank() -> InstitutionalAuditViolationCode.REQUIRED
                InstitutionalSensitiveContent.contains(value) ->
                    InstitutionalAuditViolationCode.SENSITIVE_CONTENT
                else -> null
            }

            if (code != null) {
                violations += InstitutionalAuditViolation(field = field, code = code)
            }
        }

        return InstitutionalAuditValidation(violations = violations)
    }
}

/** Detecta formas que nunca deben formar parte de un evento institucional. */
private object InstitutionalSensitiveContent {
    private val sensitiveKeyValue = Regex(
        pattern = """(?:password|contrase(?:n|ñ)a|access[_-]?token|refresh[_-]?token|token|authorization|curp|medical[_-]?data|datos[_ -]?medicos|clinical[_-]?notes|diagnosis|diagnostico|allerg(?:y|ies)|alergias|blood[_-]?type|tipo[_-]?sangre)[\"']?\s*[:=]""",
        option = RegexOption.IGNORE_CASE
    )
    private val bearerCredential = Regex(
        pattern = """\bbearer\s+\S+""",
        option = RegexOption.IGNORE_CASE
    )
    private val tokenShape = Regex(
        pattern = """\beyJ[A-Za-z0-9_-]*\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\b"""
    )
    private val fullCurpShape = Regex(
        pattern = """\b[A-Z][AEIOUX][A-Z]{2}\d{6}[HM][A-Z]{5}[A-Z0-9]\d\b""",
        option = RegexOption.IGNORE_CASE
    )

    fun contains(value: String): Boolean =
        sensitiveKeyValue.containsMatchIn(value) ||
            bearerCredential.containsMatchIn(value) ||
            tokenShape.containsMatchIn(value) ||
            fullCurpShape.containsMatchIn(value)
}
