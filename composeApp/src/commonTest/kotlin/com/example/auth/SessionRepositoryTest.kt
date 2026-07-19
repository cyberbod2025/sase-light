package com.example.auth

import com.example.security.AccessDecision
import com.example.security.AccessDenialReason
import com.example.security.AccessPolicy
import com.example.security.AccessRequest
import com.example.security.InstitutionPermission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SessionRepositoryTest {

    @Test
    fun newRepositoryStartsWithoutSession() {
        val repository = MockSessionRepository()
        assertIs<AuthState.NoSession>(repository.authState.value)
    }

    @Test
    fun demoSignInProducesActiveSessionWithExpectedRoles() {
        val repository = MockSessionRepository()
        val state = repository.signInAsDemo(DemoStaffIdentity.SECRETARIA_DEMO)
        val active = assertIs<AuthState.Active>(state)
        assertEquals(setOf(InstitutionRole.SECRETARIA), active.session.membership.roles)
        assertTrue(active.session.membership.active)
        assertEquals(active, repository.authState.value)
    }

    @Test
    fun demoSessionsAreDeterministic() {
        val first = MockSessionRepository.demoSession(DemoStaffIdentity.MEDICO_DEMO)
        val second = MockSessionRepository.demoSession(DemoStaffIdentity.MEDICO_DEMO)
        assertEquals(first, second)
    }

    @Test
    fun combinedDemoIdentityCarriesBothRoles() {
        val session = MockSessionRepository.demoSession(DemoStaffIdentity.DOCENTE_TUTOR_DEMO)
        assertEquals(
            setOf(InstitutionRole.DOCENTE, InstitutionRole.TUTOR),
            session.membership.roles
        )
    }

    @Test
    fun signOutRemovesPermissions() {
        val repository = MockSessionRepository()
        repository.signInAsDemo(DemoStaffIdentity.SECRETARIA_DEMO)
        assertIs<AccessDecision.Allowed>(evaluateReview(repository.authState.value))

        repository.signOut()

        assertIs<AuthState.NoSession>(repository.authState.value)
        val denied = assertIs<AccessDecision.Denied>(evaluateReview(repository.authState.value))
        assertEquals(AccessDenialReason.NO_SESSION, denied.reason)
    }

    @Test
    fun resetRestoresInitialStateExactly() {
        val repository = MockSessionRepository()
        repository.signInAsDemo(DemoStaffIdentity.DIRECCION_DEMO)
        repository.resetForTests()
        assertEquals(AuthState.NoSession, repository.authState.value)
    }

    @Test
    fun independentInstancesDoNotContaminateEachOther() {
        val first = MockSessionRepository()
        val second = MockSessionRepository()

        first.signInAsDemo(DemoStaffIdentity.UDEII_DEMO)

        assertIs<AuthState.Active>(first.authState.value)
        assertIs<AuthState.NoSession>(second.authState.value)

        second.signInAsDemo(DemoStaffIdentity.DOCENTE_DEMO)
        first.signOut()

        val secondActive = assertIs<AuthState.Active>(second.authState.value)
        assertEquals(setOf(InstitutionRole.DOCENTE), secondActive.session.membership.roles)
    }

    @Test
    fun familiaDemoSessionHasNoInstitutionalScope() {
        val session = MockSessionRepository.demoSession(DemoStaffIdentity.FAMILIA_DEMO)
        assertEquals(
            setOf<AuthorizationScope>(AuthorizationScope.OwnRecord(session.userId)),
            session.membership.scopes
        )
        val denied = assertIs<AccessDecision.Denied>(
            AccessPolicy.evaluate(
                AuthState.Active(session),
                AccessRequest(
                    institutionId = MockSessionRepository.DEMO_INSTITUTION_ID,
                    permission = InstitutionPermission.VIEW_STUDENT_BASE,
                    resourceScope = AuthorizationScope.Institution(MockSessionRepository.DEMO_INSTITUTION_ID)
                )
            )
        )
        assertEquals(AccessDenialReason.MISSING_PERMISSION, denied.reason)
    }

    @Test
    fun demoIdentitiesContainNoRealContactData() {
        for (identity in DemoStaffIdentity.entries) {
            assertTrue(identity.displayName.contains("DEMO"), "${identity.name} debe ser sintética")
            val session = MockSessionRepository.demoSession(identity)
            assertTrue(session.userId.value.startsWith("demo-user-"))
            assertTrue(session.membership.id.value.startsWith("demo-membership-"))
        }
    }

    private fun evaluateReview(state: AuthState): AccessDecision =
        AccessPolicy.evaluate(
            state,
            AccessRequest(
                institutionId = MockSessionRepository.DEMO_INSTITUTION_ID,
                permission = InstitutionPermission.REVIEW_PRE_APPLICATION,
                resourceScope = AuthorizationScope.Institution(MockSessionRepository.DEMO_INSTITUTION_ID)
            )
        )
}
