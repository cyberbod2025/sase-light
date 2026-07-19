package com.example.security

import com.example.auth.AuthState
import com.example.auth.AuthorizationScope
import com.example.auth.GroupId
import com.example.auth.InstitutionId
import com.example.auth.InstitutionMembership
import com.example.auth.InstitutionRole
import com.example.auth.MembershipId
import com.example.auth.RoleAssignment
import com.example.auth.SchoolCycleId
import com.example.auth.SessionContext
import com.example.auth.StudentId
import com.example.auth.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AccessPolicyTest {

    // ------------------------------------------------------------------ Roles

    @Test
    fun secretariaCanReviewPreApplications() {
        val decision = evaluate(InstitutionRole.SECRETARIA, InstitutionPermission.REVIEW_PRE_APPLICATION)
        assertIs<AccessDecision.Allowed>(decision)
    }

    @Test
    fun secretariaCannotEditMedicalInformation() {
        val decision = evaluate(InstitutionRole.SECRETARIA, InstitutionPermission.EDIT_MEDICAL)
        assertDenied(decision, AccessDenialReason.MISSING_PERMISSION)
    }

    @Test
    fun medicoCanViewAndEditMedicalInformation() {
        assertIs<AccessDecision.Allowed>(evaluate(InstitutionRole.MEDICO_ESCOLAR, InstitutionPermission.VIEW_MEDICAL))
        assertIs<AccessDecision.Allowed>(evaluate(InstitutionRole.MEDICO_ESCOLAR, InstitutionPermission.EDIT_MEDICAL))
    }

    @Test
    fun medicoCannotEditStudentIdentity() {
        val decision = evaluate(InstitutionRole.MEDICO_ESCOLAR, InstitutionPermission.EDIT_STUDENT_IDENTITY)
        assertDenied(decision, AccessDenialReason.MISSING_PERMISSION)
    }

    @Test
    fun docenteCannotManageUsers() {
        val decision = evaluate(InstitutionRole.DOCENTE, InstitutionPermission.MANAGE_USERS)
        assertDenied(decision, AccessDenialReason.MISSING_PERMISSION)
    }

    @Test
    fun direccionCanViewAudit() {
        assertIs<AccessDecision.Allowed>(evaluate(InstitutionRole.DIRECCION, InstitutionPermission.VIEW_AUDIT))
    }

    @Test
    fun tutorHasPermissionsDocenteDoesNotHave() {
        assertIs<AccessDecision.Allowed>(evaluate(InstitutionRole.TUTOR, InstitutionPermission.EDIT_INCIDENTS))
        assertDenied(
            evaluate(InstitutionRole.DOCENTE, InstitutionPermission.EDIT_INCIDENTS),
            AccessDenialReason.MISSING_PERMISSION
        )
    }

    @Test
    fun familiaGetsNoInstitutionalPermissions() {
        for (permission in InstitutionPermission.entries) {
            assertDenied(
                evaluate(InstitutionRole.FAMILIA, permission),
                AccessDenialReason.MISSING_PERMISSION,
                "FAMILIA no debe obtener $permission"
            )
        }
    }

    @Test
    fun roleAbsentFromMatrixIsDeniedAsUnknown() {
        val reducedMatrix = mapOf(
            InstitutionRole.SECRETARIA to setOf(InstitutionPermission.REVIEW_PRE_APPLICATION)
        )
        val decision = AccessPolicy.evaluate(
            authState = activeSession(setOf(InstitutionRole.DOCENTE)),
            request = request(InstitutionPermission.VIEW_STUDENT_BASE),
            matrix = reducedMatrix
        )
        assertDenied(decision, AccessDenialReason.UNKNOWN_ROLE)
    }

    // -------------------------------------------------------- Roles combinados

    @Test
    fun docentePlusTutorCombinesAuthorizedPermissions() {
        val state = activeSession(setOf(InstitutionRole.DOCENTE, InstitutionRole.TUTOR))
        assertIs<AccessDecision.Allowed>(
            AccessPolicy.evaluate(state, request(InstitutionPermission.EDIT_INCIDENTS))
        )
        assertIs<AccessDecision.Allowed>(
            AccessPolicy.evaluate(state, request(InstitutionPermission.VIEW_STUDENT_BASE))
        )
    }

    @Test
    fun secondRoleDoesNotRemovePermissionsOfFirst() {
        val onlySecretaria = activeSession(setOf(InstitutionRole.SECRETARIA))
        val secretariaPlusDocente = activeSession(setOf(InstitutionRole.SECRETARIA, InstitutionRole.DOCENTE))
        assertIs<AccessDecision.Allowed>(
            AccessPolicy.evaluate(onlySecretaria, request(InstitutionPermission.CONVERT_PRE_APPLICATION))
        )
        assertIs<AccessDecision.Allowed>(
            AccessPolicy.evaluate(secretariaPlusDocente, request(InstitutionPermission.CONVERT_PRE_APPLICATION))
        )
    }

    @Test
    fun administrativeRoleDoesNotGrantMedicalAccess() {
        val state = activeSession(setOf(InstitutionRole.ADMIN_INSTITUCIONAL, InstitutionRole.DIRECCION))
        assertDenied(
            AccessPolicy.evaluate(state, request(InstitutionPermission.VIEW_MEDICAL)),
            AccessDenialReason.MISSING_PERMISSION
        )
    }

    @Test
    fun rolesFromAnotherInstitutionDoNotCombine() {
        // La membresía activa pertenece a OTHER_INSTITUTION aunque tenga el
        // rol correcto: la evaluación contra INSTITUTION debe rechazarla antes
        // de combinar nada.
        val foreignMembership = membership(
            roles = setOf(InstitutionRole.MEDICO_ESCOLAR),
            institutionId = OTHER_INSTITUTION,
            scopes = setOf(AuthorizationScope.Institution(OTHER_INSTITUTION))
        )
        val decision = AccessPolicy.evaluate(
            AuthState.Active(SessionContext(USER, "PERSONAL DEMO", foreignMembership)),
            request(InstitutionPermission.VIEW_MEDICAL)
        )
        assertDenied(decision, AccessDenialReason.INSTITUTION_MISMATCH)
    }

    // ----------------------------------------------------------------- Scopes

    @Test
    fun correctInstitutionAllowsEvaluation() {
        assertIs<AccessDecision.Allowed>(evaluate(InstitutionRole.SECRETARIA, InstitutionPermission.VIEW_ENROLLMENT))
    }

    @Test
    fun differentInstitutionIsDenied() {
        val decision = AccessPolicy.evaluate(
            activeSession(setOf(InstitutionRole.SECRETARIA)),
            AccessRequest(
                institutionId = OTHER_INSTITUTION,
                permission = InstitutionPermission.VIEW_ENROLLMENT,
                resourceScope = AuthorizationScope.Institution(OTHER_INSTITUTION)
            )
        )
        assertDenied(decision, AccessDenialReason.INSTITUTION_MISMATCH)
    }

    @Test
    fun assignedGroupAllowsGroupAccess() {
        val state = groupScopedSession(setOf(InstitutionRole.DOCENTE), GROUP_1A)
        val decision = AccessPolicy.evaluate(
            state,
            request(InstitutionPermission.VIEW_STUDENT_BASE, resource = groupScope(GROUP_1A))
        )
        assertIs<AccessDecision.Allowed>(decision)
    }

    @Test
    fun unassignedGroupIsDenied() {
        val state = groupScopedSession(setOf(InstitutionRole.DOCENTE), GROUP_1A)
        val decision = AccessPolicy.evaluate(
            state,
            request(InstitutionPermission.VIEW_STUDENT_BASE, resource = groupScope(GROUP_2B))
        )
        assertDenied(decision, AccessDenialReason.SCOPE_MISMATCH)
    }

    @Test
    fun cycleScopeDoesNotAuthorizeAnotherCycle() {
        val membership = membership(
            roles = setOf(InstitutionRole.DIRECCION),
            scopes = setOf(AuthorizationScope.SchoolCycle(INSTITUTION, CYCLE_2026))
        )
        val state = AuthState.Active(SessionContext(USER, "PERSONAL DEMO", membership))
        assertIs<AccessDecision.Allowed>(
            AccessPolicy.evaluate(
                state,
                request(
                    InstitutionPermission.VIEW_ENROLLMENT,
                    resource = AuthorizationScope.SchoolCycle(INSTITUTION, CYCLE_2026)
                )
            )
        )
        assertDenied(
            AccessPolicy.evaluate(
                state,
                request(
                    InstitutionPermission.VIEW_ENROLLMENT,
                    resource = AuthorizationScope.SchoolCycle(INSTITUTION, CYCLE_2027)
                )
            ),
            AccessDenialReason.SCOPE_MISMATCH
        )
    }

    @Test
    fun studentAccessRequiresCompatibleScope() {
        val studentResource = AuthorizationScope.Student(INSTITUTION, StudentId("student-demo-001"))
        // Alcance institucional cubre al estudiante de su institución.
        assertIs<AccessDecision.Allowed>(
            AccessPolicy.evaluate(
                activeSession(setOf(InstitutionRole.SECRETARIA)),
                request(InstitutionPermission.VIEW_STUDENT_BASE, resource = studentResource)
            )
        )
        // Alcance de grupo NO cubre un recurso de estudiante en esta fase.
        assertDenied(
            AccessPolicy.evaluate(
                groupScopedSession(setOf(InstitutionRole.DOCENTE), GROUP_1A),
                request(InstitutionPermission.VIEW_STUDENT_BASE, resource = studentResource)
            ),
            AccessDenialReason.SCOPE_MISMATCH
        )
    }

    @Test
    fun wronglyAttachedForeignScopeCannotCrossInstitutions() {
        // Membresía de INSTITUTION con un scope mal construido de OTRA
        // institución: la coherencia institución↔recurso debe denegar aunque
        // el scope concedido "cubra" estructuralmente el recurso.
        val corrupted = membership(
            roles = setOf(InstitutionRole.SECRETARIA),
            scopes = setOf(AuthorizationScope.Institution(OTHER_INSTITUTION))
        )
        val decision = AccessPolicy.evaluate(
            AuthState.Active(SessionContext(USER, "PERSONAL DEMO", corrupted)),
            AccessRequest(
                institutionId = INSTITUTION,
                permission = InstitutionPermission.VIEW_ENROLLMENT,
                resourceScope = AuthorizationScope.Institution(OTHER_INSTITUTION)
            )
        )
        assertDenied(decision, AccessDenialReason.SCOPE_MISMATCH)
    }

    // ----------------------------------------------------------------- Sesión

    @Test
    fun noSessionIsDenied() {
        val decision = AccessPolicy.evaluate(
            AuthState.NoSession,
            request(InstitutionPermission.VIEW_STUDENT_BASE)
        )
        assertDenied(decision, AccessDenialReason.NO_SESSION)
    }

    @Test
    fun inactiveMembershipIsDenied() {
        val inactive = membership(roles = setOf(InstitutionRole.SECRETARIA), active = false)
        val decision = AccessPolicy.evaluate(
            AuthState.Active(SessionContext(USER, "PERSONAL DEMO", inactive)),
            request(InstitutionPermission.REVIEW_PRE_APPLICATION)
        )
        assertDenied(decision, AccessDenialReason.INACTIVE_MEMBERSHIP)
    }

    // ---------------------------------------------------------------- Helpers

    private companion object {
        val INSTITUTION = InstitutionId("INST-TEST-310")
        val OTHER_INSTITUTION = InstitutionId("INST-TEST-999")
        val USER = UserId("test-user-001")
        val CYCLE_2026 = SchoolCycleId("CICLO-2026-2027")
        val CYCLE_2027 = SchoolCycleId("CICLO-2027-2028")
        val GROUP_1A = GroupId("GRUPO-1A")
        val GROUP_2B = GroupId("GRUPO-2B")
    }

    private fun membership(
        roles: Set<InstitutionRole>,
        institutionId: InstitutionId = INSTITUTION,
        scopes: Set<AuthorizationScope> = setOf(AuthorizationScope.Institution(institutionId)),
        active: Boolean = true
    ): InstitutionMembership {
        val membershipId = MembershipId("test-membership-001")
        return InstitutionMembership(
            id = membershipId,
            userId = USER,
            institutionId = institutionId,
            roleAssignments = roles.mapTo(mutableSetOf()) { RoleAssignment(membershipId, it) },
            scopes = scopes,
            active = active
        )
    }

    private fun activeSession(roles: Set<InstitutionRole>): AuthState.Active =
        AuthState.Active(SessionContext(USER, "PERSONAL DEMO", membership(roles)))

    private fun groupScopedSession(roles: Set<InstitutionRole>, group: GroupId): AuthState.Active =
        AuthState.Active(
            SessionContext(
                USER,
                "PERSONAL DEMO",
                membership(roles = roles, scopes = setOf(groupScope(group)))
            )
        )

    private fun groupScope(group: GroupId): AuthorizationScope.Group =
        AuthorizationScope.Group(INSTITUTION, CYCLE_2026, group)

    private fun request(
        permission: InstitutionPermission,
        resource: AuthorizationScope = AuthorizationScope.Institution(INSTITUTION)
    ): AccessRequest = AccessRequest(INSTITUTION, permission, resource)

    private fun evaluate(role: InstitutionRole, permission: InstitutionPermission): AccessDecision =
        AccessPolicy.evaluate(activeSession(setOf(role)), request(permission))

    private fun assertDenied(
        decision: AccessDecision,
        expectedReason: AccessDenialReason,
        message: String? = null
    ) {
        val denied = assertIs<AccessDecision.Denied>(decision, message)
        assertEquals(expectedReason, denied.reason, message)
    }
}
