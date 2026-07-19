package com.example.security

import com.example.auth.AuthState
import com.example.auth.AuthorizationScope
import com.example.auth.InstitutionId
import com.example.auth.InstitutionRole

/**
 * Matriz rol → permisos, deny-by-default. Un rol ausente de la matriz no
 * concede nada (UNKNOWN_ROLE); un rol presente concede exactamente lo listado.
 * FAMILIA es un rol conocido sin permisos institucionales en esta fase.
 */
object RolePermissionMatrix {
    val DEFAULT: Map<InstitutionRole, Set<InstitutionPermission>> = mapOf(
        InstitutionRole.ADMIN_INSTITUCIONAL to setOf(
            InstitutionPermission.MANAGE_USERS,
            InstitutionPermission.MANAGE_ROLES,
            InstitutionPermission.VIEW_AUDIT
        ),
        InstitutionRole.DIRECCION to setOf(
            InstitutionPermission.VIEW_STUDENT_BASE,
            InstitutionPermission.VIEW_STUDENT_SENSITIVE_IDENTITY,
            InstitutionPermission.VIEW_ENROLLMENT,
            InstitutionPermission.VIEW_INCIDENTS,
            InstitutionPermission.VIEW_AUDIT
        ),
        InstitutionRole.SECRETARIA to setOf(
            InstitutionPermission.VIEW_STUDENT_BASE,
            InstitutionPermission.VIEW_STUDENT_SENSITIVE_IDENTITY,
            InstitutionPermission.EDIT_STUDENT_IDENTITY,
            InstitutionPermission.REVIEW_PRE_APPLICATION,
            InstitutionPermission.CONVERT_PRE_APPLICATION,
            InstitutionPermission.VIEW_ENROLLMENT,
            InstitutionPermission.MANAGE_ENROLLMENT
        ),
        InstitutionRole.TRABAJO_SOCIAL to setOf(
            InstitutionPermission.VIEW_STUDENT_BASE,
            InstitutionPermission.VIEW_SOCIAL_CONTEXT,
            InstitutionPermission.EDIT_SOCIAL_CONTEXT,
            InstitutionPermission.VIEW_INCIDENTS
        ),
        InstitutionRole.MEDICO_ESCOLAR to setOf(
            InstitutionPermission.VIEW_STUDENT_BASE,
            InstitutionPermission.VIEW_MEDICAL,
            InstitutionPermission.EDIT_MEDICAL
        ),
        InstitutionRole.UDEII to setOf(
            InstitutionPermission.VIEW_STUDENT_BASE,
            InstitutionPermission.VIEW_BAP,
            InstitutionPermission.EDIT_BAP
        ),
        InstitutionRole.DOCENTE to setOf(
            InstitutionPermission.VIEW_STUDENT_BASE,
            InstitutionPermission.VIEW_INCIDENTS
        ),
        InstitutionRole.TUTOR to setOf(
            InstitutionPermission.VIEW_STUDENT_BASE,
            InstitutionPermission.VIEW_INCIDENTS,
            InstitutionPermission.EDIT_INCIDENTS
        ),
        InstitutionRole.PREFECTURA to setOf(
            InstitutionPermission.VIEW_STUDENT_BASE,
            InstitutionPermission.VIEW_INCIDENTS,
            InstitutionPermission.EDIT_INCIDENTS
        ),
        InstitutionRole.FAMILIA to emptySet()
    )
}

/**
 * Política pura de autorización: sin estado global, sin Compose, sin Supabase.
 * Orden de evaluación: sesión → membresía activa → institución → roles →
 * permiso → alcance. Todo lo no concedido explícitamente se deniega.
 */
object AccessPolicy {

    fun evaluate(
        authState: AuthState,
        request: AccessRequest,
        matrix: Map<InstitutionRole, Set<InstitutionPermission>> = RolePermissionMatrix.DEFAULT
    ): AccessDecision {
        val session = when (authState) {
            is AuthState.NoSession -> return AccessDecision.Denied(AccessDenialReason.NO_SESSION)
            is AuthState.Active -> authState.session
        }
        val membership = session.membership
        if (!membership.active) {
            return AccessDecision.Denied(AccessDenialReason.INACTIVE_MEMBERSHIP)
        }
        if (membership.institutionId != request.institutionId) {
            return AccessDecision.Denied(AccessDenialReason.INSTITUTION_MISMATCH)
        }
        // Coherencia institución↔recurso: un recurso de otra institución nunca
        // se evalúa, aunque la membresía tuviera scopes mal construidos.
        val resourceInstitution = request.resourceScope.institutionIdOrNull()
        if (resourceInstitution != null && resourceInstitution != request.institutionId) {
            return AccessDecision.Denied(AccessDenialReason.SCOPE_MISMATCH)
        }
        val knownRoles = membership.roles.filter { matrix.containsKey(it) }
        if (knownRoles.isEmpty()) {
            return AccessDecision.Denied(AccessDenialReason.UNKNOWN_ROLE)
        }
        val effectivePermissions = knownRoles.flatMapTo(mutableSetOf()) { matrix.getValue(it) }
        if (request.permission !in effectivePermissions) {
            return AccessDecision.Denied(AccessDenialReason.MISSING_PERMISSION)
        }
        val scopeSatisfied = membership.scopes.any { granted -> granted.covers(request.resourceScope) }
        if (!scopeSatisfied) {
            return AccessDecision.Denied(AccessDenialReason.SCOPE_MISMATCH)
        }
        return AccessDecision.Allowed
    }

    private fun AuthorizationScope.institutionIdOrNull(): InstitutionId? = when (this) {
        is AuthorizationScope.Institution -> institutionId
        is AuthorizationScope.SchoolCycle -> institutionId
        is AuthorizationScope.Group -> institutionId
        is AuthorizationScope.Student -> institutionId
        is AuthorizationScope.OwnRecord -> null
    }

    /**
     * Cobertura estructural de alcances. La cobertura jerárquica solo desciende
     * dentro de la misma institución; OwnRecord nunca se cubre por alcances
     * institucionales ni a la inversa.
     */
    internal fun AuthorizationScope.covers(resource: AuthorizationScope): Boolean = when (this) {
        is AuthorizationScope.Institution -> when (resource) {
            is AuthorizationScope.Institution -> resource.institutionId == institutionId
            is AuthorizationScope.SchoolCycle -> resource.institutionId == institutionId
            is AuthorizationScope.Group -> resource.institutionId == institutionId
            is AuthorizationScope.Student -> resource.institutionId == institutionId
            is AuthorizationScope.OwnRecord -> false
        }
        is AuthorizationScope.SchoolCycle -> when (resource) {
            is AuthorizationScope.SchoolCycle -> resource == this
            is AuthorizationScope.Group ->
                resource.institutionId == institutionId && resource.cycleId == cycleId
            else -> false
        }
        is AuthorizationScope.Group -> resource == this
        is AuthorizationScope.Student -> resource == this
        is AuthorizationScope.OwnRecord -> resource == this
    }
}
