package com.example.viewmodel

import com.example.data.auth.AuthFailureReason
import com.example.data.auth.MockAuthRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Flujo de la compuerta de login a traves de [LabViewModel]. El repositorio mock
 * es sincrono; un [UnconfinedTestDispatcher] atado al scheduler del test ejecuta
 * el `launch` de signIn/signOut de inmediato, dejando el flujo determinista.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LabViewModelAuthTest {

    private fun TestScope.viewModel(): LabViewModel {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        return LabViewModel(authRepository = MockAuthRepositoryImpl(), coroutineScope = scope)
    }

    @Test
    fun validCredentialsOpenSessionAndStateReturnsToIdle() = runTest {
        val vm = viewModel()

        vm.signIn("secretaria@example.invalid", "demo1234")

        assertEquals("secretaria@example.invalid", vm.session.value?.profile?.email)
        assertIs<LoginUiState.Idle>(vm.loginState.value)
    }

    @Test
    fun invalidCredentialsKeepSessionNullAndShowError() = runTest {
        val vm = viewModel()

        vm.signIn("secretaria@example.invalid", "incorrecta")

        assertNull(vm.session.value)
        val state = assertIs<LoginUiState.Error>(vm.loginState.value)
        assertEquals(AuthFailureReason.INVALID_CREDENTIALS, state.reason)
    }

    @Test
    fun inactiveAccountKeepsSessionNullAndShowsInactiveError() = runTest {
        val vm = viewModel()

        vm.signIn("baja@example.invalid", "demo1234")

        assertNull(vm.session.value)
        val state = assertIs<LoginUiState.Error>(vm.loginState.value)
        assertEquals(AuthFailureReason.INACTIVE_ACCOUNT, state.reason)
    }

    @Test
    fun signOutClearsSessionAndResetsStateToIdle() = runTest {
        val vm = viewModel()
        vm.signIn("direccion@example.invalid", "demo1234")
        assertEquals("direccion@example.invalid", vm.session.value?.profile?.email)

        vm.signOut()

        assertNull(vm.session.value)
        assertIs<LoginUiState.Idle>(vm.loginState.value)
    }

    @Test
    fun loadingDoesNotGetStuckAfterCompletion() = runTest {
        val vm = viewModel()

        vm.signIn("secretaria@example.invalid", "demo1234")

        // Tras completar, el estado nunca debe quedar atorado en Loading.
        assertIs<LoginUiState.Idle>(vm.loginState.value)
    }

    @Test
    fun retryAfterErrorRecoversToIdleOnSuccess() = runTest {
        val vm = viewModel()

        vm.signIn("secretaria@example.invalid", "incorrecta")
        assertIs<LoginUiState.Error>(vm.loginState.value)

        vm.signIn("secretaria@example.invalid", "demo1234")

        assertIs<LoginUiState.Idle>(vm.loginState.value)
        assertEquals("secretaria@example.invalid", vm.session.value?.profile?.email)
    }
}
