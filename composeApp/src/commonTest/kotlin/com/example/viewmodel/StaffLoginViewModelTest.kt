package com.example.viewmodel

import com.example.auth.AuthBackend
import com.example.auth.AuthState
import com.example.auth.BackendUnavailableReason
import com.example.auth.DemoStaffIdentity
import com.example.auth.MockSessionRepository
import com.example.auth.MockStaffDirectory
import com.example.auth.SessionRepositoryProvision
import com.example.auth.SupabaseConfigurationState
import com.example.auth.SessionRepositoryFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StaffLoginViewModelTest {

    private fun ready(repository: MockSessionRepository = MockSessionRepository()) =
        StaffLoginViewModel(SessionRepositoryProvision.Ready(AuthBackend.MOCK, repository))

    @Test
    fun startsWithoutSessionAndIdle() {
        val repository = MockSessionRepository()
        val viewModel = ready(repository)
        assertIs<LoginUiState.Idle>(viewModel.uiState.value)
        assertIs<AuthState.NoSession>(repository.authState.value)
    }

    @Test
    fun successfulSignInOpensSessionWithRolesFromRepository() = runTest {
        val repository = MockSessionRepository()
        val viewModel = ready(repository)

        viewModel.signIn("secretaria@example.invalid", MockStaffDirectory.DEMO_PASSWORD)

        val active = assertIs<AuthState.Active>(repository.authState.value)
        assertEquals(
            DemoStaffIdentity.SECRETARIA_DEMO.roles,
            active.session.membership.roles
        )
        assertIs<LoginUiState.Idle>(viewModel.uiState.value)
    }

    @Test
    fun emailIsNormalizedBeforeLookup() = runTest {
        val repository = MockSessionRepository()
        ready(repository).signIn("  DIRECCION@Example.Invalid ", MockStaffDirectory.DEMO_PASSWORD)
        assertIs<AuthState.Active>(repository.authState.value)
    }

    @Test
    fun wrongPasswordKeepsSessionClosed() = runTest {
        val repository = MockSessionRepository()
        val viewModel = ready(repository)

        viewModel.signIn("secretaria@example.invalid", "incorrecta")

        assertIs<AuthState.NoSession>(repository.authState.value)
        assertEquals(
            LoginErrorReason.INVALID_CREDENTIALS,
            assertIs<LoginUiState.Error>(viewModel.uiState.value).reason
        )
    }

    @Test
    fun unknownEmailIsIndistinguishableFromWrongPassword() = runTest {
        val viewModel = ready()
        viewModel.signIn("nadie@example.invalid", MockStaffDirectory.DEMO_PASSWORD)
        assertEquals(
            LoginErrorReason.INVALID_CREDENTIALS,
            assertIs<LoginUiState.Error>(viewModel.uiState.value).reason
        )
    }

    @Test
    fun inactiveAccountIsRejectedWithItsOwnReason() = runTest {
        val repository = MockSessionRepository()
        val viewModel = ready(repository)

        viewModel.signIn("baja@example.invalid", MockStaffDirectory.DEMO_PASSWORD)

        assertIs<AuthState.NoSession>(repository.authState.value)
        assertEquals(
            LoginErrorReason.INACTIVE_MEMBERSHIP,
            assertIs<LoginUiState.Error>(viewModel.uiState.value).reason
        )
    }

    @Test
    fun blankInputDoesNotReachTheRepository() = runTest {
        val repository = MockSessionRepository()
        val viewModel = ready(repository)

        viewModel.signIn("   ", "")

        assertIs<AuthState.NoSession>(repository.authState.value)
        assertIs<LoginUiState.Error>(viewModel.uiState.value)
    }

    @Test
    fun editingAfterAnErrorClearsIt() = runTest {
        val viewModel = ready()
        viewModel.signIn("secretaria@example.invalid", "incorrecta")
        assertIs<LoginUiState.Error>(viewModel.uiState.value)

        viewModel.onInputChanged()

        assertIs<LoginUiState.Idle>(viewModel.uiState.value)
    }

    @Test
    fun restoreSessionRecoversPersistedIdentity() = runTest {
        val repository = MockSessionRepository(
            persistedIdentity = DemoStaffIdentity.DIRECCION_DEMO
        )
        val viewModel = ready(repository)

        viewModel.restoreSession()

        val active = assertIs<AuthState.Active>(repository.authState.value)
        assertTrue(active.session.membership.active)
    }

    @Test
    fun restoreSessionWithoutPersistedIdentityKeepsGateClosed() = runTest {
        val repository = MockSessionRepository()
        ready(repository).restoreSession()
        assertIs<AuthState.NoSession>(repository.authState.value)
    }

    @Test
    fun signOutClosesTheSession() = runTest {
        val repository = MockSessionRepository()
        val viewModel = ready(repository)
        viewModel.signIn("docente@example.invalid", MockStaffDirectory.DEMO_PASSWORD)
        assertIs<AuthState.Active>(repository.authState.value)

        viewModel.signOut()

        assertIs<AuthState.NoSession>(repository.authState.value)
    }

    @Test
    fun unavailableBackendDisablesLoginWithoutSilentFallback() = runTest {
        val provision = SessionRepositoryFactory.create(
            backend = AuthBackend.SUPABASE,
            configuration = SupabaseConfigurationState.NotConfigured
        )
        val viewModel = StaffLoginViewModel(provision)

        assertEquals(
            BackendUnavailableReason.CONFIGURATION_MISSING,
            assertIs<LoginUiState.BackendUnavailable>(viewModel.uiState.value).reason
        )
        assertNull(viewModel.sessionRepository)

        // Un intento de acceso no cambia el estado ni abre sesión alguna.
        viewModel.signIn("secretaria@example.invalid", MockStaffDirectory.DEMO_PASSWORD)
        assertIs<LoginUiState.BackendUnavailable>(viewModel.uiState.value)
    }
}
