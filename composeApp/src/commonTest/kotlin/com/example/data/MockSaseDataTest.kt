package com.example.data

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MockSaseDataTest {
    @BeforeTest
    fun reset() {
        MockSaseData.resetForTests()
    }

    @Test
    fun addStudentRejectsDuplicateCURP() {
        val result = MockSaseData.addStudent(
            student(curp = "DEMA100101HDFABC01", enrollmentId = uniqId("UNIQ1"))
        )
        assertIs<StudentAddResult.DuplicateCurp>(result)
    }

    @Test
    fun addStudentRejectsDuplicateCURPIgnoringCaseAndWhitespace() {
        val result = MockSaseData.addStudent(
            student(curp = "  dema100101hdfabc01  ", enrollmentId = uniqId("UNIQ2"))
        )
        assertIs<StudentAddResult.DuplicateCurp>(result)
    }

    @Test
    fun addStudentRejectsDuplicateEnrollmentId() {
        val result = MockSaseData.addStudent(
            student(curp = uniqCurp("DUENR"), enrollmentId = "S310-DEMA100101-26")
        )
        assertIs<StudentAddResult.DuplicateEnrollmentId>(result)
    }

    @Test
    fun addStudentRejectsDuplicateEnrollmentIdIgnoringCaseAndWhitespace() {
        val result = MockSaseData.addStudent(
            student(curp = uniqCurp("DUENR2"), enrollmentId = "  S310-DEMA100101-26  ")
        )
        assertIs<StudentAddResult.DuplicateEnrollmentId>(result)
    }

    @Test
    fun addStudentRejectsBlankCURP() {
        val result = MockSaseData.addStudent(
            student(curp = "   ", enrollmentId = uniqId("BLANK1"))
        )
        assertIs<StudentAddResult.InvalidData>(result)
    }

    @Test
    fun addStudentRejectsBlankEnrollmentId() {
        val result = MockSaseData.addStudent(
            student(curp = uniqCurp("BLENR"), enrollmentId = "")
        )
        assertIs<StudentAddResult.InvalidData>(result)
    }

    @Test
    fun studentByCurpNormalizesTrimUppercase() {
        val found = MockSaseData.studentByCurp("  dema100101hdfabc01  ")
        assertNotNull(found)
        assertEquals("DEMA100101HDFABC01", found.curp)
    }

    @Test
    fun studentByCurpReturnsNullForUnknownCurp() {
        val found = MockSaseData.studentByCurp("CURP-NONEXISTENT-999")
        assertNull(found)
    }

    @Test
    fun updateStudentModifiesExistingStudent() {
        val student = MockSaseData.studentByCurp("DEMA100101HDFABC01")
        assertNotNull(student)
        val updated = student.copy(fullName = "NOMBRE ACTUALIZADO")
        MockSaseData.updateStudent(updated)
        val reloaded = MockSaseData.studentByCurp("DEMA100101HDFABC01")
        assertNotNull(reloaded)
        assertEquals("NOMBRE ACTUALIZADO", reloaded.fullName)
    }

    @Test
    fun updateStudentDoesNotCreateNewEntry() {
        val countBefore = MockSaseData.students.value.size
        val nonExistent = student(
            id = "NONEXISTENT",
            curp = uniqCurp("NOCREATE"),
            enrollmentId = uniqId("NOCREATE")
        )
        MockSaseData.updateStudent(nonExistent)
        assertEquals(countBefore, MockSaseData.students.value.size)
    }

    @Test
    fun updateStudentDoesNotDuplicateExistingStudent() {
        val countBefore = MockSaseData.students.value.size
        val student = MockSaseData.studentByCurp("DEMA100101HDFABC01")
        assertNotNull(student)
        MockSaseData.updateStudent(student)
        assertEquals(countBefore, MockSaseData.students.value.size)
    }

    @Test
    fun addStudentSuccessfullyInsertsNewStudent() {
        val curp = uniqCurp("NEWSTU")
        val enrollmentId = uniqId("NEWSTU")
        val result = MockSaseData.addStudent(
            student(curp = curp, enrollmentId = enrollmentId)
        )
        val added = assertIs<StudentAddResult.Added>(result)
        assertEquals(curp.uppercase(), added.student.curp)
        assertEquals(enrollmentId.uppercase(), added.student.enrollmentId)
        assertNotNull(MockSaseData.studentByCurp(curp))
    }

    private fun student(curp: String, enrollmentId: String, id: String = "TEST-${suffix()}"): Student = Student(
        id = id,
        fullName = "Mock Test Student",
        group = "1A",
        enrollmentId = enrollmentId,
        curp = curp,
        tutorName = "Test Tutor",
        tutorRelation = "Tutor",
        status = "Nuevo ingreso",
        riskLevel = "Bajo",
        documentationStatus = "Completa"
    )

    private fun uniqCurp(seed: String): String =
        (seed.take(6).uppercase().padEnd(6, 'X') + suffix().padEnd(12, '0')).take(18)

    private fun uniqId(seed: String): String = "S310-TEST-$seed-${suffix()}"

    private fun suffix(): String =
        kotlin.random.Random.nextInt(100000, 999999).toString()
}
