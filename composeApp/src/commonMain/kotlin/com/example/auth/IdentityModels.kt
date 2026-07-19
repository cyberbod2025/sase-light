package com.example.auth

import kotlin.jvm.JvmInline

/**
 * Identificadores institucionales tipados. Evitan strings mágicos y
 * preparan el terreno para UUIDs reales sin depender de Supabase ni Compose.
 */
@JvmInline
value class UserId(val value: String)

@JvmInline
value class InstitutionId(val value: String)

@JvmInline
value class MembershipId(val value: String)

@JvmInline
value class SchoolCycleId(val value: String)

@JvmInline
value class GroupId(val value: String)

@JvmInline
value class StudentId(val value: String)

/**
 * Roles institucionales de SASE. FAMILIA existe como concepto de dominio pero
 * no recibe permisos institucionales en esta fase (deny-by-default).
 */
enum class InstitutionRole {
    ADMIN_INSTITUCIONAL,
    DIRECCION,
    SECRETARIA,
    TRABAJO_SOCIAL,
    MEDICO_ESCOLAR,
    UDEII,
    DOCENTE,
    TUTOR,
    PREFECTURA,
    FAMILIA
}

/**
 * Alcances de autorización. Un permiso solo es efectivo dentro del alcance
 * concedido a la membresía; la cobertura se evalúa en AccessPolicy.
 */
sealed interface AuthorizationScope {
    data class Institution(val institutionId: InstitutionId) : AuthorizationScope

    data class SchoolCycle(
        val institutionId: InstitutionId,
        val cycleId: SchoolCycleId
    ) : AuthorizationScope

    data class Group(
        val institutionId: InstitutionId,
        val cycleId: SchoolCycleId,
        val groupId: GroupId
    ) : AuthorizationScope

    data class Student(
        val institutionId: InstitutionId,
        val studentId: StudentId
    ) : AuthorizationScope

    data class OwnRecord(val userId: UserId) : AuthorizationScope
}

/** Un rol concedido a una membresía concreta. Una membresía puede tener varios. */
data class RoleAssignment(
    val membershipId: MembershipId,
    val role: InstitutionRole
)

/**
 * Membresía de una persona dentro de una institución: usuario → membresía →
 * uno o varios roles → uno o varios alcances. Los permisos efectivos son la
 * unión de los roles dentro de los alcances asignados.
 */
data class InstitutionMembership(
    val id: MembershipId,
    val userId: UserId,
    val institutionId: InstitutionId,
    val roleAssignments: Set<RoleAssignment>,
    val scopes: Set<AuthorizationScope>,
    val active: Boolean
) {
    val roles: Set<InstitutionRole>
        get() = roleAssignments.mapTo(mutableSetOf()) { it.role }
}

/** Contexto de la sesión activa: identidad + membresía institucional vigente. */
data class SessionContext(
    val userId: UserId,
    val displayName: String,
    val membership: InstitutionMembership
)

/** Estado de autenticación observable por la aplicación. */
sealed interface AuthState {
    data object NoSession : AuthState
    data class Active(val session: SessionContext) : AuthState
}
