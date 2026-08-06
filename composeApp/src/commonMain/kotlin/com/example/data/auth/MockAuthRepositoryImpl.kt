package com.example.data.auth

import com.example.currentEpochMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Repositorio de autenticacion exclusivo de [DEMO_LOCAL]. */
class MockAuthRepositoryImpl(
    private val staff: List<MockStaffCredential> = MockStaffDirectory.DEFAULT,
    private val nowMillis: () -> Long = ::currentEpochMillis
) : AuthRepository {

    private val _session = MutableStateFlow<AuthSession?>(null)
    override val session: StateFlow<AuthSession?> = _session.asStateFlow()
    override val demoAccessAvailable: Boolean = true

    private var pendingCredential: MockStaffCredential? = null

    override suspend fun signIn(email: String, password: String): AuthResult {
        pendingCredential = null
        _session.value = null
        val normalized = email.trim().lowercase()
        val match = staff.firstOrNull { it.profile.email == normalized }
            ?: return AuthResult.Failure(AuthFailureReason.INVALID_CREDENTIALS)
        if (match.password != password) {
            return AuthResult.Failure(AuthFailureReason.INVALID_CREDENTIALS)
        }
        return beginAccess(match)
    }

    override suspend fun signInDemo(role: StaffRole): AuthResult {
        pendingCredential = null
        _session.value = null
        val match = staff.firstOrNull { it.demoEntryRole == role }
            ?: return AuthResult.Failure(AuthFailureReason.DEMO_UNAVAILABLE)
        val assignment = match.roles.firstOrNull { it.role == role }
            ?: return AuthResult.Failure(AuthFailureReason.ROLE_NOT_ASSIGNED)
        return activate(match, assignment.roleId)
    }

    override suspend fun selectRole(roleId: String): AuthResult {
        val pending = pendingCredential
            ?: return AuthResult.Failure(AuthFailureReason.ROLE_NOT_ASSIGNED)
        return activate(pending, roleId)
    }

    override suspend fun switchRole(roleId: String): AuthResult {
        val active = _session.value
            ?: return AuthResult.Failure(AuthFailureReason.ROLE_NOT_ASSIGNED)
        val switched = runCatching { active.switchToAssignedRole(roleId) }.getOrNull()
            ?: return AuthResult.Failure(AuthFailureReason.ROLE_NOT_ASSIGNED)
        _session.value = switched
        return AuthResult.Success(switched)
    }

    override suspend fun resetDemo() {
        pendingCredential = null
        _session.value = null
    }

    override suspend fun signOut() {
        pendingCredential = null
        _session.value = null
    }

    private fun beginAccess(match: MockStaffCredential): AuthResult {
        if (!match.profile.active) {
            return AuthResult.Failure(AuthFailureReason.INACTIVE_ACCOUNT)
        }
        if (match.membershipId == null) {
            return AuthResult.Failure(AuthFailureReason.NO_MEMBERSHIP)
        }
        if (match.roles.isEmpty()) {
            return AuthResult.Failure(AuthFailureReason.NO_ROLE)
        }
        if (match.roles.size > 1) {
            pendingCredential = match
            return AuthResult.RoleSelectionRequired(
                RoleSelectionContext(
                    institutionName = match.institutionName,
                    availableRoles = match.roles
                )
            )
        }
        return activate(match, match.roles.single().roleId)
    }

    private fun activate(match: MockStaffCredential, roleId: String): AuthResult {
        val membershipId = match.membershipId
            ?: return AuthResult.Failure(AuthFailureReason.NO_MEMBERSHIP)
        if (!match.profile.active) {
            return AuthResult.Failure(AuthFailureReason.INACTIVE_ACCOUNT)
        }
        if (match.roles.none { it.roleId == roleId }) {
            return AuthResult.Failure(AuthFailureReason.ROLE_NOT_ASSIGNED)
        }

        val startedAt = nowMillis()
        val institutional = InstitutionalSession.create(
            userId = match.userId,
            profileId = match.profile.id,
            membershipId = membershipId,
            institutionId = match.institutionId,
            institutionName = match.institutionName,
            roleAssignments = match.roles,
            activeRoleId = roleId,
            schoolCycleId = match.schoolCycleId,
            sessionStartedAt = startedAt,
            expiresAt = startedAt + DEMO_SESSION_DURATION_MILLIS
        )
        val session = AuthSession(
            profile = match.profile,
            institutional = institutional,
            accessToken = "demo-local-non-network-token"
        )
        pendingCredential = null
        _session.value = session
        return AuthResult.Success(session)
    }

    private companion object {
        const val DEMO_SESSION_DURATION_MILLIS: Long = 8L * 60L * 60L * 1_000L
    }
}

data class MockStaffCredential(
    val userId: String,
    val profile: StaffProfile,
    val password: String,
    val membershipId: String?,
    val institutionId: String,
    val institutionName: String,
    val schoolCycleId: String?,
    val roles: List<InstitutionalRoleAssignment>,
    val demoEntryRole: StaffRole? = null
)

object MockStaffDirectory {
    const val INSTITUTION_ID: String = "institution-demo-fictitious"
    const val INSTITUTION_NAME: String =
        "INSTITUCIÓN ESCOLAR FICTICIA SASE — DATOS SINTÉTICOS"
    const val SCHOOL_CYCLE_ID: String = "cycle-demo-2026-2027"

    val DEFAULT: List<MockStaffCredential> = listOf(
        credential(
            id = "direction-demo",
            email = "direccion@example.invalid",
            fullName = "Dirección Demo",
            roles = listOf(StaffRole.DIRECCION, StaffRole.SECRETARIA),
            demoEntryRole = StaffRole.DIRECCION
        ),
        credential(
            id = "secretary-demo",
            email = "secretaria@example.invalid",
            fullName = "Secretaría Demo",
            roles = listOf(StaffRole.SECRETARIA),
            demoEntryRole = StaffRole.SECRETARIA
        ),
        credential(
            id = "teacher-demo",
            email = "docente@example.invalid",
            fullName = "Docente Demo",
            roles = listOf(StaffRole.DOCENTE),
            demoEntryRole = StaffRole.DOCENTE
        ),
        credential("social-demo", "trabajosocial@example.invalid", "Trabajo Social Demo", listOf(StaffRole.TRABAJO_SOCIAL)),
        credential("medical-demo", "medico@example.invalid", "Médico Escolar Demo", listOf(StaffRole.MEDICO_ESCOLAR)),
        credential("udeii-demo", "udeii@example.invalid", "UDEII Demo", listOf(StaffRole.UDEII)),
        credential("inactive-demo", "baja@example.invalid", "Cuenta Demo Inactiva", listOf(StaffRole.DOCENTE), active = false),
        credential("without-role", "sinrol@example.invalid", "Personal Demo Sin Rol", emptyList()),
        credential(
            id = "without-membership",
            email = "sinmembresia@example.invalid",
            fullName = "Personal Demo Sin Membresía",
            roles = listOf(StaffRole.DOCENTE),
            hasMembership = false
        )
    )

    private fun credential(
        id: String,
        email: String,
        fullName: String,
        roles: List<StaffRole>,
        active: Boolean = true,
        hasMembership: Boolean = true,
        demoEntryRole: StaffRole? = null
    ): MockStaffCredential = MockStaffCredential(
        userId = "user-$id",
        profile = StaffProfile(
            id = "profile-$id",
            email = email,
            fullName = fullName,
            active = active
        ),
        password = "demo1234",
        membershipId = if (hasMembership) "membership-$id" else null,
        institutionId = INSTITUTION_ID,
        institutionName = INSTITUTION_NAME,
        schoolCycleId = SCHOOL_CYCLE_ID,
        roles = roles.map { role ->
            InstitutionalRoleAssignment(
                roleId = "role-${role.name.lowercase()}",
                role = role
            )
        },
        demoEntryRole = demoEntryRole
    )
}
