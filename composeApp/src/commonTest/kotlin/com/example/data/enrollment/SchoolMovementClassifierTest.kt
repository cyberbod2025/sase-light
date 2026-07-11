package com.example.data.enrollment

import com.example.data.MockSaseData
import com.example.data.Student
import com.example.viewmodel.PreApplicationViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SchoolMovementClassifierTest {
    private val activeSchoolYear = "2026-2027"

    @Test
    fun newEntryWithUnknownCurpIsClassifiedAsNewEntry() {
        val result = classify("Nuevo Ingreso", " newc100101hdfabc01 ", emptyList())

        val newEntry = assertIs<SchoolMovementClassification.NewEntry>(result)
        assertEquals("NEWC100101HDFABC01", newEntry.normalizedCurp)
        assertEquals(activeSchoolYear, newEntry.activeSchoolYear)
    }

    @Test
    fun reEnrollmentRecoversTheExactExistingEnrollmentId() {
        val existing = student(enrollmentId = "S310-000321-D")

        val result = classify("Reinscripcion", existing.curp, listOf(existing))

        val reEnrollment = assertIs<SchoolMovementClassification.ReEnrollment>(result)
        assertEquals(existing.id, reEnrollment.studentId)
        assertEquals(existing.enrollmentId, reEnrollment.existingEnrollmentId)
        assertEquals(activeSchoolYear, reEnrollment.activeSchoolYear)
    }

    @Test
    fun reEnrollmentWithoutEnrollmentIdIsClassifiedAsInitialMigration() {
        val historical = student(enrollmentId = "")

        val result = classify("Reinscripcion", historical.curp, listOf(historical))

        val migration = assertIs<SchoolMovementClassification.InitialMigration>(result)
        assertEquals(historical.id, migration.studentId)
        assertEquals(activeSchoolYear, migration.activeSchoolYear)
    }

    @Test
    fun newEntryWithExistingCurpReturnsConflict() {
        val existing = student()

        val conflict = assertIs<SchoolMovementClassification.Conflict>(
            classify("Nuevo Ingreso", existing.curp, listOf(existing))
        )

        assertEquals(SchoolMovementConflictReason.NEW_ENTRY_ALREADY_EXISTS, conflict.reason)
    }

    @Test
    fun reEnrollmentWithUnknownCurpReturnsConflict() {
        val conflict = assertIs<SchoolMovementClassification.Conflict>(
            classify("Reinscripcion", "MISS100101HDFABC01", emptyList())
        )

        assertEquals(SchoolMovementConflictReason.RE_ENROLLMENT_NOT_FOUND, conflict.reason)
    }

    @Test
    fun duplicateMasterStudentsReturnConflictWithoutChoosingOne() {
        val first = student(id = "MASTER-1")
        val second = student(id = "MASTER-2", enrollmentId = "S310-000999-D")

        val conflict = assertIs<SchoolMovementClassification.Conflict>(
            classify("Reinscripcion", first.curp, listOf(first, second))
        )

        assertEquals(SchoolMovementConflictReason.DUPLICATE_MASTER_STUDENTS, conflict.reason)
    }

    @Test
    fun normalizationIgnoresAccidentalSpacesAndLetterCase() {
        val existing = student(curp = "NORM100101HDFABC01", enrollmentId = "S310-000777-D")

        val normalized = classify("  reinscripción  ", "  norm100101hdfabc01  ", listOf(existing))
        val canonical = classify("REINSCRIPCION", existing.curp, listOf(existing))

        assertEquals(canonical, normalized)
    }

    @Test
    fun classificationIsReadOnlyAndIdempotent() {
        val masterStudents = MockSaseData.students.value
        val existing = masterStudents.first { it.enrollmentId.isNotBlank() }
        val masterBefore = masterStudents.toList()
        val preApplicationsBefore = PreApplicationViewModel.sharedPreApplications.value.toList()
        val officialStudentsBefore = PreApplicationViewModel.officialStudents.value.toList()

        val first = classify("Reinscripcion", existing.curp, masterStudents)
        val second = classify("Reinscripcion", existing.curp, masterStudents)

        assertEquals(first, second)
        assertEquals(masterBefore, MockSaseData.students.value)
        assertEquals(preApplicationsBefore, PreApplicationViewModel.sharedPreApplications.value)
        assertEquals(officialStudentsBefore, PreApplicationViewModel.officialStudents.value)
        assertEquals(existing.enrollmentId, MockSaseData.students.value.first { it.id == existing.id }.enrollmentId)
    }

    @Test
    fun blankCurpReturnsTypedConflict() {
        val conflict = assertIs<SchoolMovementClassification.Conflict>(
            classify("Nuevo Ingreso", "   ", emptyList())
        )

        assertEquals(SchoolMovementConflictReason.INVALID_CURP, conflict.reason)
    }

    @Test
    fun unknownApplicationTypeReturnsTypedConflict() {
        val conflict = assertIs<SchoolMovementClassification.Conflict>(
            classify("Cambio de plantel", "TYPE100101HDFABC01", emptyList())
        )

        assertEquals(SchoolMovementConflictReason.UNKNOWN_APPLICATION_TYPE, conflict.reason)
    }

    private fun classify(
        applicationType: String,
        curp: String,
        masterStudents: List<Student>
    ): SchoolMovementClassification = SchoolMovementClassifier.classify(
        declaredApplicationType = applicationType,
        curp = curp,
        activeSchoolYear = activeSchoolYear,
        masterStudents = masterStudents
    )

    private fun student(
        id: String = "MASTER-1",
        curp: String = "STUD100101HDFABC01",
        enrollmentId: String = "S310-000123-D"
    ): Student = Student(
        id = id,
        fullName = "Alumno Maestro",
        group = "1A",
        enrollmentId = enrollmentId,
        curp = curp
    )
}
