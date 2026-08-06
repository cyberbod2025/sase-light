package com.example.data.auth

/**
 * Vincula el identificador persistido de un rol institucional con el rol que
 * entiende el cliente. La relacion evita seleccionar un [StaffRole] que no
 * pertenezca realmente a la membresia activa.
 */
data class InstitutionalRoleAssignment(
    val roleId: String,
    val role: StaffRole
) {
    init {
        require(roleId.isNotBlank()) { "roleId must not be blank" }
    }
}

/**
 * Identidad institucional efectiva durante una sesion autenticada.
 *
 * El constructor es privado para mantener alineados el rol activo y sus
 * permisos. Los cambios de rol solo pueden hacerse mediante
 * [switchToAssignedRole], que valida la asignacion y reconstruye los permisos
 * desde [StaffPermissions].
 */
class InstitutionalSession private constructor(
    val userId: String,
    val profileId: String,
    val membershipId: String,
    val institutionId: String,
    val institutionName: String,
    val roleAssignments: List<InstitutionalRoleAssignment>,
    val activeRoleId: String,
    val activeRole: StaffRole,
    val permissions: Set<SaseArea>,
    val schoolCycleId: String?,
    val sessionStartedAt: Long,
    val expiresAt: Long?
) {
    val assignedRoleIds: Set<String> = roleAssignments.mapTo(linkedSetOf()) { it.roleId }
    val assignedRoles: Set<StaffRole> = roleAssignments.mapTo(linkedSetOf()) { it.role }

    /**
     * Devuelve una nueva sesion con otro rol ya asignado a la membresia.
     * No existe una variante que acepte un [StaffRole] libre.
     */
    fun switchToAssignedRole(roleId: String): InstitutionalSession {
        val assignment = roleAssignments.firstOrNull { it.roleId == roleId }
            ?: throw IllegalArgumentException("Role $roleId is not assigned to this membership")

        return InstitutionalSession(
            userId = userId,
            profileId = profileId,
            membershipId = membershipId,
            institutionId = institutionId,
            institutionName = institutionName,
            roleAssignments = roleAssignments,
            activeRoleId = assignment.roleId,
            activeRole = assignment.role,
            permissions = StaffPermissions.areasFor(assignment.role).toSet(),
            schoolCycleId = schoolCycleId,
            sessionStartedAt = sessionStartedAt,
            expiresAt = expiresAt
        )
    }

    fun isExpired(nowEpochMillis: Long): Boolean =
        expiresAt?.let { nowEpochMillis >= it } ?: false

    companion object {
        /**
         * Construye la sesion a partir de asignaciones institucionales ya
         * resueltas. El rol inicial debe pertenecer a esas asignaciones.
         */
        fun create(
            userId: String,
            profileId: String,
            membershipId: String,
            institutionId: String,
            institutionName: String,
            roleAssignments: List<InstitutionalRoleAssignment>,
            activeRoleId: String,
            schoolCycleId: String?,
            sessionStartedAt: Long,
            expiresAt: Long? = null
        ): InstitutionalSession {
            require(userId.isNotBlank()) { "userId must not be blank" }
            require(profileId.isNotBlank()) { "profileId must not be blank" }
            require(membershipId.isNotBlank()) { "membershipId must not be blank" }
            require(institutionId.isNotBlank()) { "institutionId must not be blank" }
            require(institutionName.isNotBlank()) { "institutionName must not be blank" }
            require(roleAssignments.isNotEmpty()) { "At least one assigned role is required" }
            require(roleAssignments.map { it.roleId }.distinct().size == roleAssignments.size) {
                "Assigned role IDs must be unique"
            }

            val assignments = roleAssignments.toList()
            val activeAssignment = assignments.firstOrNull { it.roleId == activeRoleId }
                ?: throw IllegalArgumentException("Role $activeRoleId is not assigned to this membership")

            return InstitutionalSession(
                userId = userId,
                profileId = profileId,
                membershipId = membershipId,
                institutionId = institutionId,
                institutionName = institutionName,
                roleAssignments = assignments,
                activeRoleId = activeAssignment.roleId,
                activeRole = activeAssignment.role,
                permissions = StaffPermissions.areasFor(activeAssignment.role).toSet(),
                schoolCycleId = schoolCycleId,
                sessionStartedAt = sessionStartedAt,
                expiresAt = expiresAt
            )
        }
    }
}
