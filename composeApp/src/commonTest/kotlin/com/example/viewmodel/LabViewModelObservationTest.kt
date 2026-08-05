package com.example.viewmodel

import com.example.data.MockSaseData
import com.example.data.Student
import com.example.data.StudentAddResult
import com.example.data.auth.MockAuthRepositoryImpl
import com.example.environment.AppEnvironment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * addObservation debe usar el autor real de la sesion (no un string fijo
 * elegido por la pantalla), igual que reportIncident/logSaseAudit.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LabViewModelObservationTest {

    @BeforeTest
    fun reset() = MockSaseData.resetForTests()

    @AfterTest
    fun cleanup() = MockSaseData.resetForTests()

    private fun TestScope.viewModel(): LabViewModel {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        return LabViewModel(
            appEnvironment = AppEnvironment.demoLocal("test"),
            authRepository = MockAuthRepositoryImpl(),
            coroutineScope = scope
        )
    }

    private fun seedStudent(id: String): Student {
        val student = Student(
            id = id,
            fullName = "ALUMNO OBS TEST",
            group = "1A",
            enrollmentId = "S310-OBS01",
            curp = "OBST010101HDFABC01"
        )
        assertTrue(MockSaseData.addStudent(student) is StudentAddResult.Added)
        return student
    }

    @Test
    fun addObservationUsesAuthenticatedStaffNameAsAuthor() = runTest {
        val vm = viewModel()
        vm.signIn("secretaria@example.invalid", "demo1234")
        val student = seedStudent("MASTER-OBS-01")

        val ok = vm.addObservation(student.id, "Requiere apoyo en matematicas", "Académica")

        assertTrue(ok)
        val updated = vm.saseStudents.value.single { it.id == student.id }
        val obs = updated.observations.first()
        assertEquals("Secretaría Demo", obs.author)
        assertEquals("Requiere apoyo en matematicas", obs.text)
    }

    @Test
    fun addObservationWithoutSessionDoesNothing() = runTest {
        val vm = viewModel()
        val student = seedStudent("MASTER-OBS-02")

        val ok = vm.addObservation(student.id, "Nota sin sesion", "Académica")

        assertFalse(ok)
        val updated = vm.saseStudents.value.single { it.id == student.id }
        assertTrue(updated.observations.isEmpty())
    }
}
