package com.example.viewmodel

import com.example.data.MockSaseData
import com.example.data.auth.MockAuthRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * La bitacora institucional debe registrar QUIEN hizo cada accion a partir de
 * la sesion autenticada, no del rol autodeclarado por el caller. Sin sesion,
 * el rol del caller queda como respaldo.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LabViewModelAuditTest {

    @BeforeTest
    fun reset() {
        MockSaseData.resetForTests()
    }

    private fun TestScope.viewModel(): LabViewModel {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        return LabViewModel(authRepository = MockAuthRepositoryImpl(), coroutineScope = scope)
    }

    @Test
    fun auditRecordsAuthenticatedRoleNotCallerDeclaredRole() = runTest {
        val vm = viewModel()
        vm.signIn("secretaria@example.invalid", "demo1234")

        // El caller intenta autodeclararse Direccion; la sesion manda.
        vm.logSaseAudit("Caso escalado", "Dirección", "Alumno X")

        val audit = vm.saseAudits.value.first()
        assertEquals("Secretaría", audit.userRole)
    }

    @Test
    fun auditDetailIncludesAuthenticatedAuthorName() = runTest {
        val vm = viewModel()
        vm.signIn("direccion@example.invalid", "demo1234")

        vm.logSaseAudit("Expediente actualizado", "Secretaría", "Alumno Y")

        val audit = vm.saseAudits.value.first()
        assertEquals("Dirección", audit.userRole)
        assertTrue(
            audit.detail.contains("Demo Direccion"),
            "el detalle debe incluir el nombre del autor autenticado: ${audit.detail}"
        )
    }

    @Test
    fun withoutSessionCallerRoleIsKeptAsFallback() = runTest {
        val vm = viewModel()

        vm.logSaseAudit("Proceso automatico", "Sistema", "Cierre de ciclo")

        val audit = vm.saseAudits.value.first()
        assertEquals("Sistema", audit.userRole)
        assertEquals("Cierre de ciclo", audit.detail)
    }

    @Test
    fun afterSignOutAuditsFallBackToCallerRole() = runTest {
        val vm = viewModel()
        vm.signIn("secretaria@example.invalid", "demo1234")
        vm.signOut()

        vm.logSaseAudit("Accion posterior", "Sistema", "Sin sesion")

        val audit = vm.saseAudits.value.first()
        assertEquals("Sistema", audit.userRole)
    }
}
