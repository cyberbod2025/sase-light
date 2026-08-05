package com.example.viewmodel

import com.example.audit.InstitutionalAuditResult
import com.example.data.MockSaseData
import com.example.data.auth.MockAuthRepositoryImpl
import com.example.data.auth.StaffRole
import com.example.environment.AppEnvironment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * La bitacora institucional deriva actor, membresia y rol exclusivamente de
 * la sesion autenticada. Sin sesion no se escribe evidencia institucional.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LabViewModelAuditTest {

    @BeforeTest
    fun reset() {
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
    fun auditRecordsTypedIdentityFromAuthenticatedSession() = runTest {
        val vm = viewModel()
        vm.signIn("secretaria@example.invalid", "demo1234")

        assertTrue(
            vm.logSaseAudit(
                action = "student_case.escalated",
                entityType = "student_case",
                entityId = "STUDENT-DEMO-X"
            )
        )

        val audit = vm.saseAudits.value.first()
        val event = assertNotNull(audit.institutionalEvent)
        assertEquals("Secretaría", audit.userRole)
        assertEquals("profile-secretary-demo", event.actorProfileId)
        assertEquals("membership-secretary-demo", event.membershipId)
        assertEquals(StaffRole.SECRETARIA, event.activeRole)
        assertEquals(InstitutionalAuditResult.AUTHORIZED, event.result)
    }

    @Test
    fun auditCarriesInstitutionAndEntityWithoutFreeFormAuthorDetail() = runTest {
        val vm = viewModel()
        vm.signInDemo(StaffRole.DIRECCION)

        assertTrue(vm.logSaseAudit("student.updated", "student", "STUDENT-DEMO-Y"))

        val audit = vm.saseAudits.value.first()
        val event = assertNotNull(audit.institutionalEvent)
        assertEquals("Dirección", audit.userRole)
        assertEquals("institution-demo-fictitious", event.institutionId)
        assertEquals("student", event.entityType)
        assertEquals("STUDENT-DEMO-Y", event.entityId)
        assertEquals("student:STUDENT-DEMO-Y [AUTHORIZED]", audit.detail)
    }

    @Test
    fun withoutSessionAuditIsRejectedAndDoesNotMutateTheLog() = runTest {
        val vm = viewModel()
        val initialCount = vm.saseAudits.value.size

        val recorded = vm.logSaseAudit("school_cycle.closed", "school_cycle", "CYCLE-DEMO")

        assertFalse(recorded)
        assertEquals(initialCount, vm.saseAudits.value.size)
    }

    @Test
    fun afterSignOutAuditIsRejectedAndDoesNotInventAnActor() = runTest {
        val vm = viewModel()
        vm.signIn("secretaria@example.invalid", "demo1234")
        vm.signOut()
        val initialCount = vm.saseAudits.value.size

        val recorded = vm.logSaseAudit("session.after_sign_out", "session", "SESSION-DEMO")

        assertFalse(recorded)
        assertEquals(initialCount, vm.saseAudits.value.size)
    }
}
