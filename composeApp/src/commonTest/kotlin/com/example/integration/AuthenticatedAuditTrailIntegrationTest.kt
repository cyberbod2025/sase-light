package com.example.integration

import com.example.data.MockSaseData
import com.example.data.Student
import com.example.data.StudentAddResult
import com.example.data.auth.MockAuthRepositoryImpl
import com.example.data.auth.StaffRole
import com.example.environment.AppEnvironment
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
import kotlin.test.assertNotNull
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
        return LabViewModel(
            appEnvironment = AppEnvironment.demoLocal("test"),
            authRepository = MockAuthRepositoryImpl(),
            coroutineScope = scope
        )
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

        // 1) Compuerta: la escritura solo existe tras autenticar Secretaría.
        vm.signIn("secretaria@example.invalid", "demo1234")
        assertEquals("secretaria@example.invalid", vm.session.value?.profile?.email)

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

        // 2) Accion institucional real: Secretaria actualiza el expediente
        val student = vm.saseStudents.value.single { it.id == studentId }
        assertTrue(vm.updateStudent(student.copy(group = "1B")))

        val updated = vm.saseStudents.value.single { it.id == studentId }
        assertEquals("1B", updated.group, "la accion institucional debe reflejarse en el expediente")

        val audit = vm.saseAudits.value.first()
        val event = assertNotNull(audit.institutionalEvent)
        assertEquals("student.updated", event.action)
        assertEquals("profile-secretary-demo", event.actorProfileId)
        assertEquals("membership-secretary-demo", event.membershipId)
        assertEquals(StaffRole.SECRETARIA, event.activeRole)
        assertEquals("student", event.entityType)
        assertEquals(studentId, event.entityId)
    }

    @Test
    fun `cerrar sesion no borra la evidencia ya registrada`() = runTest {
        val vm = viewModel()
        vm.signInDemo(StaffRole.DIRECCION)
        assertTrue(vm.logSaseAudit("student_case.escalated", "student_case", "STUDENT-DEMO-01"))

        vm.signOut()

        assertNull(vm.session.value, "la sesion debe cerrarse")
        val audit = vm.saseAudits.value.first()
        val event = assertNotNull(audit.institutionalEvent)
        assertEquals(
            "Dirección", audit.userRole,
            "la evidencia ya escrita conserva al autor que la genero, aunque la sesion ya haya terminado"
        )
        assertEquals("profile-direction-demo", event.actorProfileId)
        assertEquals(StaffRole.DIRECCION, event.activeRole)
    }
}
