package com.example.data.presolicitud

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class OfficialStudentModelsTest {

    @Test
    fun syntheticCurpIsDetectedAfterNormalization() {
        assertTrue(isSyntheticCurp("  ABCD010101HDFXXX00  "))
        assertFalse(isSyntheticCurp("ABCD010101HDFRRS09"))
    }

    @Test
    fun syntheticCurpCannotGenerateOfficialMatricula() {
        val matricula = OfficialStudent.generateMatricula(
            curp = "ABCD010101HDFXXX00",
            ingresoAnioCorto = 26
        )

        assertNull(matricula)
    }

    @Test
    fun validCurpStillGeneratesOfficialMatricula() {
        val matricula = OfficialStudent.generateMatricula(
            curp = "ABCD010101HDFRRS09",
            ingresoAnioCorto = 26
        )

        assertEquals("S310-ABCD010101-26", matricula)
    }
}
