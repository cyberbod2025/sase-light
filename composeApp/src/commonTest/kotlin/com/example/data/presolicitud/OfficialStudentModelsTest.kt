package com.example.data.presolicitud

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * El portal familiar sugiere una CURP terminada en "XXX00" cuando la familia
 * todavía no aporta el documento oficial. Ese valor provisional no puede
 * escalar a matrícula institucional.
 */
class OfficialStudentModelsTest {

    @Test
    fun syntheticCurpIsDetectedAfterNormalization() {
        assertTrue(isSyntheticCurp("  abcd010101hdfxxx00  "))
        assertTrue(isSyntheticCurp("ABCD010101HDFXXX00"))
        assertFalse(isSyntheticCurp("ABCD010101HDFRRS09"))
    }

    @Test
    fun syntheticCurpCannotGenerateOfficialMatricula() {
        assertNull(
            OfficialStudent.generateMatricula(
                curp = "ABCD010101HDFXXX00",
                ingresoAnioCorto = 26
            )
        )
    }

    @Test
    fun realCurpGeneratesOfficialMatricula() {
        assertEquals(
            "S310-ABCD010101-26",
            OfficialStudent.generateMatricula(
                curp = "  abcd010101hdfrrs09  ",
                ingresoAnioCorto = 26
            )
        )
    }

    @Test
    fun incompleteCurpOrInvalidYearIsRejected() {
        assertNull(OfficialStudent.generateMatricula(curp = "ABCD0101", ingresoAnioCorto = 26))
        assertNull(OfficialStudent.generateMatricula(curp = "ABCD010101HDFRRS09", ingresoAnioCorto = 100))
        assertNull(OfficialStudent.generateMatricula(curp = "ABCD010101HDFRRS09", ingresoAnioCorto = -1))
    }
}
