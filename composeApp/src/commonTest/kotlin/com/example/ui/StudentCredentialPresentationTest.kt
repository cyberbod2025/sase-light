package com.example.ui

import com.example.data.presolicitud.MockOfficialStudentData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StudentCredentialPresentationTest {
    @Test
    fun studentWithoutEnrollmentIsNotCountedAsCompletedOfficialEnrollment() {
        val eligible = credentialEligibleStudents(MockOfficialStudentData.officialStudents)

        assertEquals(1, eligible.size)
        assertFalse(eligible.any { it.matriculaOficial.isNullOrBlank() })
        assertTrue(eligible.none { it.id == "OFF-DEMO-05" })
    }

    @Test
    fun everyCompleteCredentialAlwaysHasAVisibleEnrollmentId() {
        val eligible = credentialEligibleStudents(MockOfficialStudentData.officialStudents)

        eligible.forEach { official ->
            assertNotNull(officialCredentialEnrollment(official))
        }
    }
}
