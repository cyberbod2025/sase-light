package com.example.auth

import com.example.security.AccessDecision
import com.example.security.AccessPolicy
import com.example.security.AccessRequest
import com.example.security.InstitutionPermission
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SupabaseSessionRepositoryTest {

    @Test
    fun successfulLoginMapsProfileMembershipRolesAndScopes() = runTest {
        val (repository, _, _) = repositoryWith(activeMembership())

        val result = repository.signInWithCredentials("personal-demo@example.invalid", "clave-demo")

        val success = assertIs<StaffSignInResult.Success>(result)
        assertEquals(USER, success.session.userId)
        assertEquals("PERSONAL DEMO SECRETARIA", success.session.displayName)
        assertEquals(setOf(InstitutionRole.SECRETARIA), success.session.membership.roles)
        val active = assertIs<AuthState.Active>(repository.authState.value)
        assertEquals(success.session, active.session)
        // Los permisos efectivos siguen saliendo de la política pura de Fase 1.
        assertIs<AccessDecision.Allowed>(
            AccessPolicy.evaluate(
                repository.authState.value,
                AccessRequest(
                    INSTITUTION,
                    InstitutionPermission.REVIEW_PRE_APPLICATION,
                    AuthorizationScope.Institution(INSTITUTION)
                )
            )
        )
    }

    @Test
    fun invalidCredentialsFailWithoutSession() = runTest {
        val (repository, auth, _) = repositoryWith(
            membership = activeMembership(),
            authResult = StaffAuthGatewayResult.Failed(StaffAuthFailure.INVALID_CREDENTIALS)
        )
        val failure = assertIs<StaffSignInResult.Failure>(
            repository.signInWithCredentials("personal-demo@example.invalid", "clave-mala")
        )
        assertEquals(StaffAuthFailure.INVALID_CREDENTIALS, failure.failure)
        assertIs<AuthState.NoSession>(repository.authState.value)
        assertEquals(0, auth.signOutCalls)
    }

    @Test
    fun missingProfileFailsAndRevokesRemoteSession() = runTest {
        val (repository, auth, _) = repositoryWith(activeMembership(), profile = null)
        val failure = assertIs<StaffSignInResult.Failure>(signInDemo(repository))
        assertEquals(StaffAuthFailure.PROFILE_UNAVAILABLE, failure.failure)
        assertIs<AuthState.NoSession>(repository.authState.value)
        assertEquals(1, auth.signOutCalls)
    }

    @Test
    fun missingMembershipFailsAndRevokesRemoteSession() = runTest {
        val (repository, auth, _) = repositoryWith(membership = null)
        val failure = assertIs<StaffSignInResult.Failure>(signInDemo(repository))
        assertEquals(StaffAuthFailure.MEMBERSHIP_NOT_FOUND, failure.failure)
        assertIs<AuthState.NoSession>(repository.authState.value)
        assertEquals(1, auth.signOutCalls)
    }

    @Test
    fun inactiveMembershipFailsAndRevokesRemoteSession() = runTest {
        val (repository, auth, _) = repositoryWith(activeMembership().copy(active = false))
        val failure = assertIs<StaffSignInResult.Failure>(signInDemo(repository))
        assertEquals(StaffAuthFailure.MEMBERSHIP_INACTIVE, failure.failure)
        assertIs<AuthState.NoSession>(repository.authState.value)
        assertEquals(1, auth.signOutCalls)
    }

    @Test
    fun networkFailureIsTypedNotThrown() = runTest {
        val (repository, _, _) = repositoryWith(
            membership = activeMembership(),
            authResult = StaffAuthGatewayResult.Failed(StaffAuthFailure.NETWORK_UNAVAILABLE)
        )
        val failure = assertIs<StaffSignInResult.Failure>(signInDemo(repository))
        assertEquals(StaffAuthFailure.NETWORK_UNAVAILABLE, failure.failure)
        assertIs<AuthState.NoSession>(repository.authState.value)
    }

    @Test
    fun signOutWithRevocationClearsStateAndCallsGateway() = runTest {
        val (repository, auth, _) = repositoryWith(activeMembership())
        assertIs<StaffSignInResult.Success>(signInDemo(repository))

        repository.signOutWithRevocation()

        assertIs<AuthState.NoSession>(repository.authState.value)
        assertEquals(1, auth.signOutCalls)
    }

    @Test
    fun restoreSessionRebuildsInstitutionalContext() = runTest {
        val (repository, auth, _) = repositoryWith(activeMembership())
        auth.restoredUserId = USER

        val restored = repository.restoreSession()

        val active = assertIs<AuthState.Active>(restored)
        assertEquals(USER, active.session.userId)
    }

    @Test
    fun restoreWithoutPersistedSessionStaysSignedOut() = runTest {
        val (repository, auth, _) = repositoryWith(activeMembership())
        auth.restoredUserId = null
        assertIs<AuthState.NoSession>(repository.restoreSession())
        assertIs<AuthState.NoSession>(repository.authState.value)
    }

    @Test
    fun failuresExposeOnlyTypedReasonsNeverTokensOrPasswords() = runTest {
        val (repository, auth, _) = repositoryWith(
            membership = activeMembership(),
            authResult = StaffAuthGatewayResult.Failed(StaffAuthFailure.INVALID_CREDENTIALS)
        )
        val failure = assertIs<StaffSignInResult.Failure>(
            repository.signInWithCredentials("personal-demo@example.invalid", "clave-secreta-demo")
        )
        // El error tipado es un enum: no transporta strings del SDK ni credenciales.
        assertEquals(StaffAuthFailure.INVALID_CREDENTIALS, failure.failure)
        assertNull(failure.failure.name.takeIf { it.contains("clave") })
        // El gateway jamás recibió instrucción de retener la contraseña.
        assertEquals("personal-demo@example.invalid", auth.lastEmail)
    }

    @Test
    fun instancesDoNotContaminateEachOther() = runTest {
        val (first, _, _) = repositoryWith(activeMembership())
        val (second, _, _) = repositoryWith(activeMembership())

        assertIs<StaffSignInResult.Success>(signInDemo(first))

        assertIs<AuthState.Active>(first.authState.value)
        assertIs<AuthState.NoSession>(second.authState.value)

        first.resetForTests()
        assertIs<AuthState.NoSession>(first.authState.value)
    }

    // ---------------------------------------------------------------- Helpers

    private companion object {
        val USER = UserId("demo-user-supabase-test")
        val INSTITUTION = InstitutionId("INST-DEMO-310")
        val MEMBERSHIP_ID = MembershipId("demo-membership-supabase-test")
    }

    private fun activeMembership(): InstitutionMembership = InstitutionMembership(
        id = MEMBERSHIP_ID,
        userId = USER,
        institutionId = INSTITUTION,
        roleAssignments = setOf(RoleAssignment(MEMBERSHIP_ID, InstitutionRole.SECRETARIA)),
        scopes = setOf(AuthorizationScope.Institution(INSTITUTION)),
        active = true
    )

    private fun repositoryWith(
        membership: InstitutionMembership?,
        profile: StaffProfile? = StaffProfile(USER, "PERSONAL DEMO SECRETARIA"),
        authResult: StaffAuthGatewayResult = StaffAuthGatewayResult.Authenticated(USER)
    ): Triple<SupabaseSessionRepository, FakeAuthGateway, FakeDirectoryGateway> {
        val auth = FakeAuthGateway(authResult)
        val directory = FakeDirectoryGateway(profile, membership)
        return Triple(SupabaseSessionRepository(auth, directory), auth, directory)
    }

    private suspend fun signInDemo(repository: SupabaseSessionRepository): StaffSignInResult =
        repository.signInWithCredentials("personal-demo@example.invalid", "clave-demo")

    private class FakeAuthGateway(
        private val signInResult: StaffAuthGatewayResult
    ) : StaffAuthGateway {
        var signOutCalls = 0
        var lastEmail: String? = null
        var restoredUserId: UserId? = null

        override suspend fun signIn(email: String, password: String): StaffAuthGatewayResult {
            lastEmail = email
            return signInResult
        }

        override suspend fun signOut() {
            signOutCalls++
        }

        override suspend fun restoreUserId(): UserId? = restoredUserId
    }

    private class FakeDirectoryGateway(
        private val profile: StaffProfile?,
        private val membership: InstitutionMembership?
    ) : StaffDirectoryGateway {
        override suspend fun fetchProfile(userId: UserId): StaffProfile? = profile
        override suspend fun fetchMembership(userId: UserId): InstitutionMembership? = membership
    }
}
