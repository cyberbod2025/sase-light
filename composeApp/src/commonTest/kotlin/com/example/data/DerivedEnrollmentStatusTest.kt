package com.example.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DerivedEnrollmentStatusTest {

    /** Datos locales: ningún test de invariantes depende de fixtures globales. */
    private fun localStudent(
        id: String = "T1",
        fullName: String = "ALUMNO LOCAL",
        group: String = "1°A",
        enrollmentId: String = "",
        curp: String = "TESA100101HDFABC01",
        status: String = "Activo",
        documentationStatus: String = "Completa"
    ) = Student(
        id = id,
        fullName = fullName,
        group = group,
        enrollmentId = enrollmentId,
        curp = curp,
        status = status,
        documentationStatus = documentationStatus
    )

    // ── Estados terminales de alta oficial ────────────────────────────────

    @Test
    fun enrolledStudentWithGroupDerivesEnrolledWithGroup() {
        val student = localStudent(
            group = "1°A",
            enrollmentId = "S310-DEMA100101-26",
            status = "Alta oficial con grupo"
        )
        assertEquals(DerivedEnrollmentStatus.ENROLLED_WITH_GROUP, deriveEnrollmentStatus(student))
    }

    @Test
    fun enrolledStudentWithoutGroupDerivesEnrolledWithoutGroup() {
        val student = localStudent(
            group = "",
            enrollmentId = "S310-DEMB110202-26",
            status = "Alta oficial sin grupo"
        )
        assertEquals(DerivedEnrollmentStatus.ENROLLED_WITHOUT_GROUP, deriveEnrollmentStatus(student))
    }

    @Test
    fun nonCanonicalEnrollmentIdWithGroupDerivesEnrolledWithoutGroup() {
        // Matrícula presente pero fuera del formato canónico S310-XXXXXXXXXX-NN:
        // el alta con grupo exige matrícula canónica.
        val student = localStudent(
            group = "1°A",
            enrollmentId = "LEGACY-123",
            status = "Alta oficial con grupo"
        )
        assertEquals(DerivedEnrollmentStatus.ENROLLED_WITHOUT_GROUP, deriveEnrollmentStatus(student))
    }

    // ── Bloqueo documental: nunca READY ───────────────────────────────────

    @Test
    fun incompleteDocsNeverDeriveReady() {
        val student = localStudent(documentationStatus = "Incompleta", status = "Documentos pendientes")
        val derived = deriveEnrollmentStatus(student)
        assertEquals(DerivedEnrollmentStatus.DOCUMENTS_BLOCKED, derived)
        assertFalse(derived.readyForEnrollment)
    }

    @Test
    fun docsInReviewNeverDeriveReady() {
        val student = localStudent(documentationStatus = "En revisión", status = "Pre-solicitud recibida")
        val derived = deriveEnrollmentStatus(student)
        assertEquals(DerivedEnrollmentStatus.DOCUMENTS_BLOCKED, derived)
        assertFalse(derived.readyForEnrollment)
    }

    // ── Bloqueo de grupo ──────────────────────────────────────────────────

    @Test
    fun unassignedGroupDerivesGroupBlocked() {
        val student = localStudent(group = "Por asignar", status = "Pre-solicitud recibida")
        assertEquals(DerivedEnrollmentStatus.GROUP_BLOCKED, deriveEnrollmentStatus(student))
    }

    @Test
    fun tentativeGroupPendingConfirmationDerivesGroupBlocked() {
        // Grupo pre-asignado pero no confirmado: el literal canónico de estado
        // es la única señal disponible en Student (comparación exacta).
        val student = localStudent(group = "1°B", status = "Pendiente asignación manual de grupo")
        assertEquals(DerivedEnrollmentStatus.GROUP_BLOCKED, deriveEnrollmentStatus(student))
    }

    @Test
    fun groupBlockDoesNotOverrideDocumentsBlock() {
        // Bloqueo más grave (documental) prevalece sobre grupo pendiente.
        val student = localStudent(
            group = "Por asignar",
            status = "Documentos pendientes",
            documentationStatus = "Incompleta"
        )
        assertEquals(DerivedEnrollmentStatus.DOCUMENTS_BLOCKED, deriveEnrollmentStatus(student))
    }

    // ── Precedencia booleana (regresión del bug A || B && C) ─────────────

    @Test
    fun statusMentioningGroupDoesNotBlockWhenGroupIsConfirmed() {
        // Con el código anterior, "grupo" dentro de status participaba en una
        // expresión sin paréntesis (A || B && C). Un status que menciona
        // "grupo" con grupo real confirmado NO debe bloquear.
        val student = localStudent(
            group = "2°A",
            enrollmentId = "",
            status = "Reasignación de grupo completada"
        )
        assertEquals(DerivedEnrollmentStatus.READY_FOR_ENROLLMENT, deriveEnrollmentStatus(student))
    }

    // ── Sin bloqueos → READY ──────────────────────────────────────────────

    @Test
    fun noBlocksDeriveReadyForEnrollment() {
        val student = localStudent(
            group = "1°A",
            enrollmentId = "",
            status = "Lista para alta oficial",
            documentationStatus = "Completa"
        )
        val derived = deriveEnrollmentStatus(student)
        assertEquals(DerivedEnrollmentStatus.READY_FOR_ENROLLMENT, derived)
        assertTrue(derived.readyForEnrollment)
    }

    // ── Invariantes del enum ──────────────────────────────────────────────

    @Test
    fun everyEnumStateIsReachable() {
        val producers = mapOf(
            DerivedEnrollmentStatus.DOCUMENTS_BLOCKED to
                localStudent(documentationStatus = "Incompleta"),
            DerivedEnrollmentStatus.GROUP_BLOCKED to
                localStudent(group = "Por asignar"),
            DerivedEnrollmentStatus.READY_FOR_ENROLLMENT to
                localStudent(group = "1°A", enrollmentId = ""),
            DerivedEnrollmentStatus.ENROLLED_WITHOUT_GROUP to
                localStudent(group = "", enrollmentId = "S310-TESA100101-26"),
            DerivedEnrollmentStatus.ENROLLED_WITH_GROUP to
                localStudent(group = "1°A", enrollmentId = "S310-TESA100101-26")
        )
        // Cobertura total: si se agrega un estado al enum sin productor, falla.
        assertEquals(DerivedEnrollmentStatus.entries.toSet(), producers.keys)
        producers.forEach { (expected, student) ->
            assertEquals(expected, deriveEnrollmentStatus(student))
        }
    }

    @Test
    fun readyFlagIsTrueOnlyForReadyAndEnrolledWithGroup() {
        val readyStates = DerivedEnrollmentStatus.entries.filter { it.readyForEnrollment }
        assertEquals(
            setOf(
                DerivedEnrollmentStatus.READY_FOR_ENROLLMENT,
                DerivedEnrollmentStatus.ENROLLED_WITH_GROUP
            ),
            readyStates.toSet()
        )
    }

    @Test
    fun prioritiesAreStrictlyIncreasingAndUnique() {
        val priorities = DerivedEnrollmentStatus.entries.map { it.priority }
        assertEquals(priorities.toSet().size, priorities.size)
        assertEquals(priorities.sorted(), priorities)
    }

    // ── Alineación con datos demo del padrón (integración ligera) ────────

    @Test
    fun demo01DerivesEnrolledWithGroup() {
        val demo = MockSaseData.students.value.first { it.id == "1" }
        assertEquals(DerivedEnrollmentStatus.ENROLLED_WITH_GROUP, deriveEnrollmentStatus(demo))
    }

    @Test
    fun demo03DerivesDocumentsBlocked() {
        val demo = MockSaseData.students.value.first { it.id == "3" }
        assertEquals(DerivedEnrollmentStatus.DOCUMENTS_BLOCKED, deriveEnrollmentStatus(demo))
    }

    @Test
    fun demo04DerivesReadyForEnrollment() {
        val demo = MockSaseData.students.value.first { it.id == "4" }
        assertEquals(DerivedEnrollmentStatus.READY_FOR_ENROLLMENT, deriveEnrollmentStatus(demo))
    }

    @Test
    fun demo05DerivesGroupBlocked() {
        val demo = MockSaseData.students.value.first { it.id == "5" }
        assertEquals(DerivedEnrollmentStatus.GROUP_BLOCKED, deriveEnrollmentStatus(demo))
    }
}
