package com.example.viewmodel

import com.example.data.InstitutionalStudentRecordKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertFailsWith

class LabViewModelNavigationTest {
    @Test
    fun institutionalRecordRouteKeepsStudentIdAndExactResolutionContext() {
        val viewModel = LabViewModel()
        val key = InstitutionalStudentRecordKey(
            studentId = "MASTER-V2-PRE-TEST",
            schoolYear = "2026-2027",
            sourcePreApplicationFolio = "PRE-TEST",
            enrollmentId = "S310-000001-1"
        )

        viewModel.navigateTo(Screen.StudentRecord(key.studentId, key))

        val route = assertIs<Screen.StudentRecord>(viewModel.currentScreen.value)
        assertEquals(key.studentId, route.studentId)
        assertEquals(key, route.institutionalKey)
        assertNotEquals(key.enrollmentId, route.studentId)
        assertNotEquals(key.sourcePreApplicationFolio, route.studentId)
    }

    @Test
    fun institutionalRecordRouteRejectsDifferentStudentIdentityInKey() {
        val key = InstitutionalStudentRecordKey(
            studentId = "MASTER-V2-OTRO",
            schoolYear = "2026-2027",
            sourcePreApplicationFolio = "PRE-TEST",
            enrollmentId = "S310-000001-1"
        )

        assertFailsWith<IllegalArgumentException> {
            Screen.StudentRecord(studentId = "MASTER-V2-PRE-TEST", institutionalKey = key)
        }
    }
}
