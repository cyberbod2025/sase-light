package com.example.integration

import com.example.data.MockSaseData
import com.example.data.Student
import com.example.data.StudentAddResult
import com.example.data.auth.MockAuthRepositoryImpl
import com.example.viewmodel.LabViewModel
import com.example.viewmodel.LoginUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Golden-path de autorizacion: login (compuerta) -> accion institucional sobre
 * un Student -> bitacora con autor real. Cierra el ciclo end-to-end entre las
 * dos piezas de la capa de auth entregadas en esta iteracion (login gate y
 * trazabilidad atada a sesion), sin tocar navegacion ni roles de UI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthenticatedAuditTrailIntegrationTest {

    @BeforeTest
    fun reset() {
        MockSaseData.resetForTests()
    }

    @AfterTest
    fun cleanup() {
        MockSaseData.resetForTests()
    }

    private fun TestScope.viewModel(): LabViewModel {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        return LabViewModel(authRepository = MockAuthRepositoryImpl(), coroutineScope = scope)
    }

    @Test
    fun `sin sesion no hay identidad que respalde una accion institucional`() = runTest {
        val vm = viewModel()

        assertNull(vm.session.value, "no debe haber sesion antes de iniciar sesion")
        assertIs<LoginUiState.Idle>(vm.loginState.value)
    }

    @Test
    fun `login exitoso mas accion sobre expediente registra autor real en la bitacora`() = runTest {
        val vm = viewModel()
        val studentId = "MASTER-AUDIT-IT-01"
        assertIs<StudentAddResult.Added>(
            vm.addStudent(
                Student(
                    id = studentId,
                    fullName = "ALUMNO AUDITORIA IT",
                    group = "1A",
                    enrollmentId = "S310-AUDIT01",
                    curp = "AUDT010101HDFABC01"
                )
            )
        )

        // 1) Compuerta: acceso solo tras autenticarse
        vm.signIn("secretaria@example.invalid", "demo1234")
        assertEquals("secretaria@example.invalid", vm.session.value?.profile?.email)

        // 2) Accion institucional real: Secretaria actualiza el expediente
        val student = vm.saseStudents.value.single { it.id == studentId }
        vm.updateStudent(student.copy(group = "1B"))

        // 3) La pantalla podria autodeclararse cualquier rol; la sesion manda
        vm.logSaseAudit("Expediente actualizado", "Dirección", student.fullName)

        val updated = vm.saseStudents.value.single { it.id == studentId }
        assertEquals("1B", updated.group, "la accion institucional debe reflejarse en el expediente")

        val audit = vm.saseAudits.value.first()
        assertEquals("Expediente actualizado", audit.action)
        assertEquals(
            "Secretaría", audit.userRole,
            "el rol registrado debe ser el de la sesion autenticada, no el autodeclarado por la pantalla"
        )
        assertTrue(
            audit.detail.contains("Demo Secretaria"),
            "la bitacora debe dejar constancia de quien (nombre) realizo la accion: ${audit.detail}"
        )
    }

    @Test
    fun `cerrar sesion no borra la evidencia ya registrada`() = runTest {
        val vm = viewModel()
        vm.signIn("direccion@example.invalid", "demo1234")
        vm.logSaseAudit("Caso escalado", "Sistema", "Alumno con seguimiento")

        vm.signOut()

        assertNull(vm.session.value, "la sesion debe cerrarse")
        val audit = vm.saseAudits.value.first()
        assertEquals(
            "Dirección", audit.userRole,
            "la evidencia ya escrita conserva al autor que la genero, aunque la sesion ya haya terminado"
        )
    }
}
