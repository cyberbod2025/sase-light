package com.example.security

import com.example.auth.AuthorizationScope
import com.example.auth.InstitutionId

/**
 * Permisos institucionales explícitos. Ningún rol obtiene permisos que no
 * estén declarados en la matriz; no existen permisos implícitos.
 */
enum class InstitutionPermission {
    VIEW_STUDENT_BASE,
    VIEW_STUDENT_SENSITIVE_IDENTITY,
    EDIT_STUDENT_IDENTITY,
    REVIEW_PRE_APPLICATION,
    CONVERT_PRE_APPLICATION,
    VIEW_ENROLLMENT,
    MANAGE_ENROLLMENT,
    VIEW_MEDICAL,
    EDIT_MEDICAL,
    VIEW_BAP,
    EDIT_BAP,
    VIEW_SOCIAL_CONTEXT,
    EDIT_SOCIAL_CONTEXT,
    VIEW_INCIDENTS,
    EDIT_INCIDENTS,
    VIEW_AUDIT,
    MANAGE_USERS,
    MANAGE_ROLES
}

/** Solicitud de acceso: institución + permiso + recurso al que se aplica. */
data class AccessRequest(
    val institutionId: InstitutionId,
    val permission: InstitutionPermission,
    val resourceScope: AuthorizationScope
)

/**
 * Razones de denegación no sensibles: no revelan qué permisos existen ni
 * qué datos protege el recurso.
 */
enum class AccessDenialReason {
    NO_SESSION,
    INACTIVE_MEMBERSHIP,
    INSTITUTION_MISMATCH,
    UNKNOWN_ROLE,
    MISSING_PERMISSION,
    SCOPE_MISMATCH
}

/**
 * Resultado explícito de la evaluación. Las denegaciones esperadas son
 * valores, nunca excepciones.
 */
sealed interface AccessDecision {
    data object Allowed : AccessDecision
    data class Denied(val reason: AccessDenialReason) : AccessDecision
}
