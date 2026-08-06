package com.example.viewmodel

import com.example.audit.InstitutionalAuditEvent
import com.example.data.MockSaseData
import com.example.data.SaseAudit
import com.example.data.Student
import com.example.data.StudentAddResult
import com.example.data.StudentPersistenceFailure
import com.example.data.StudentUpdateResult
import com.example.data.auth.MockAuthRepositoryImpl
import com.example.data.repository.AuditRepository
import com.example.data.repository.StudentRepository
import com.example.data.repository.StudentSyncResult
import com.example.environment.AppEnvironment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Contrato de persistencia visto desde el ViewModel: un rechazo del
 * almacenamiento nunca se presenta como guardado, y ninguna mutacion ocurre si
 * su bitacora no puede asentarse.
 */
private class RecordingStudentRepository(
    private val onUpdate: (Student) -> StudentUpdateResult = { StudentUpdateResult.Updated(it) },
    private val onAdd: (Student) -> StudentAddResult = { StudentAddResult.Added(it) }
) : StudentRepository {
    val updates = mutableListOf<Student>()
    val adds = mutableListOf<Student>()
    var refreshes = 0

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    override val students: StateFlow<List<Student>> = _students.asStateFlow()

    fun seed(vararg students: Student) {
        _students.value = students.toList()
    }

    override suspend fun refresh(): StudentSyncResult {
        refreshes++
        return StudentSyncResult.Loaded(_students.value)
    }

    override suspend fun updateStudent(student: Student): StudentUpdateResult {
        updates += student
        return onUpdate(student)
    }

    override suspend fun addStudent(student: Student): StudentAddResult {
        adds += student
        return onAdd(student)
    }
}

private class RecordingAuditRepository(
    private val accepts: Boolean = true
) : AuditRepository {
    val logged = mutableListOf<InstitutionalAuditEvent>()

    private val _audits = MutableStateFlow<List<SaseAudit>>(emptyList())
    override val audits: StateFlow<List<SaseAudit>> = _audits.asStateFlow()

    override suspend fun refresh(): Boolean = true

    override suspend fun logAudit(event: InstitutionalAuditEvent): Boolean {
        logged += event
        if (!accepts) return false
        _audits.value = listOf(SaseAudit.fromInstitutionalEvent(event)) + _audits.value
        return true
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LabViewModelPersistenceContractTest {

    @BeforeTest
    fun reset() = MockSaseData.resetForTests()

    @AfterTest
    fun cleanup() = MockSaseData.resetForTests()

    private fun TestScope.viewModel(
        students: StudentRepository,
        audits: AuditRepository
    ): LabViewModel = LabViewModel(
        appEnvironment = AppEnvironment.demoLocal("test"),
        authRepository = MockAuthRepositoryImpl(),
        studentRepository = students,
        auditRepository = audits,
        coroutineScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
    )

    private fun student(id: String = "student-1") = Student(
        id = id,
        fullName = "ALUMNA SINTETICA",
        group = "1A",
        enrollmentId = "S310-0001",
        curp = "SINT010101MDFABC01"
    )

    @Test
    fun `una actualizacion rechazada por el backend no se reporta como guardada`() = runTest {
        val repository = RecordingStudentRepository(
            onUpdate = { StudentUpdateResult.Failed(StudentPersistenceFailure.REJECTED) }
        )
        val audits = RecordingAuditRepository()
        val vm = viewModel(repository, audits)
        vm.signIn("secretaria@example.invalid", "demo1234")

        val saved = vm.updateStudent(student())

        assertFalse(saved, "un rechazo del almacenamiento no puede devolver exito")
        // El intento queda asentado como fallido, no como autorizado.
        val last = audits.logged.last { it.action == "student.updated" }
        assertEquals("FAILED", last.result.name)
    }

    @Test
    fun `una actualizacion confirmada si se reporta como guardada y se audita autorizada`() = runTest {
        val repository = RecordingStudentRepository()
        val audits = RecordingAuditRepository()
        val vm = viewModel(repository, audits)
        vm.signIn("secretaria@example.invalid", "demo1234")

        assertTrue(vm.updateStudent(student()))

        assertEquals(1, repository.updates.size)
        val last = audits.logged.last { it.action == "student.updated" }
        assertEquals("AUTHORIZED", last.result.name)
    }

    @Test
    fun `si la bitacora no puede asentarse el expediente no se toca`() = runTest {
        val repository = RecordingStudentRepository()
        val audits = RecordingAuditRepository()
        val vm = viewModel(repository, audits)
        vm.signIn("secretaria@example.invalid", "demo1234")

        // Un identificador con forma de CURP hace irregistrable el evento; sin
        // evidencia posible, la mutacion no debe intentarse siquiera.
        val saved = vm.updateStudent(student(id = "SINT010101MDFABC01"))

        assertFalse(saved)
        assertTrue(repository.updates.isEmpty(), "no se muta sin bitacora asentable")
    }

    @Test
    fun `sin sesion no se escribe ni se audita`() = runTest {
        val repository = RecordingStudentRepository()
        val audits = RecordingAuditRepository()
        val vm = viewModel(repository, audits)

        assertFalse(vm.updateStudent(student()))
        assertIs<StudentAddResult.InvalidData>(vm.addStudent(student()))

        assertTrue(repository.updates.isEmpty())
        assertTrue(repository.adds.isEmpty())
        assertTrue(audits.logged.isEmpty())
    }

    @Test
    fun `un alta rechazada por el backend se propaga como fallo de persistencia`() = runTest {
        val repository = RecordingStudentRepository(
            onAdd = { StudentAddResult.Failed(StudentPersistenceFailure.NETWORK) }
        )
        val vm = viewModel(repository, RecordingAuditRepository())
        vm.signIn("secretaria@example.invalid", "demo1234")

        val result = vm.addStudent(student())

        assertEquals(
            StudentPersistenceFailure.NETWORK,
            assertIs<StudentAddResult.Failed>(result).reason
        )
    }

    @Test
    fun `iniciar sesion carga el expediente institucional y expone el estado real`() = runTest {
        val repository = RecordingStudentRepository()
        repository.seed(student())
        val vm = viewModel(repository, RecordingAuditRepository())

        assertIs<StudentSyncUiState.Idle>(vm.studentSync.value)

        vm.signIn("secretaria@example.invalid", "demo1234")

        assertEquals(1, repository.refreshes, "la sesion debe disparar la carga institucional")
        assertIs<StudentSyncUiState.Ready>(vm.studentSync.value)
        assertEquals(listOf("student-1"), vm.saseStudents.value.map { it.id })
    }

    @Test
    fun `un fallo de carga se comunica como error y no como institucion vacia`() = runTest {
        val failing = object : StudentRepository {
            override val students: StateFlow<List<Student>> = MutableStateFlow(emptyList())
            override suspend fun refresh(): StudentSyncResult =
                StudentSyncResult.Failed(StudentPersistenceFailure.NETWORK)
            override suspend fun updateStudent(student: Student) =
                StudentUpdateResult.Failed(StudentPersistenceFailure.NETWORK)
            override suspend fun addStudent(student: Student) =
                StudentAddResult.Failed(StudentPersistenceFailure.NETWORK)
        }
        val vm = viewModel(failing, RecordingAuditRepository())

        vm.signIn("secretaria@example.invalid", "demo1234")

        val error = assertIs<StudentSyncUiState.Error>(vm.studentSync.value)
        assertEquals(StudentPersistenceFailure.NETWORK, error.reason)
    }
}
