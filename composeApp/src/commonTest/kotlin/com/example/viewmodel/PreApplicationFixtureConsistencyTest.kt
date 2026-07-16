package com.example.viewmodel

import com.example.data.presolicitud.MockPreApplicationData
import com.example.data.presolicitud.ReadinessStatus
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Invariante D6: un fixture declarado READY no debe arrastrar pendientes
 * institucionales — con una única excepción deliberada:
 *
 * - PRE-CONFLICT-001 modela el escenario demo "declarada lista y luego se
 *   detecta conflicto de CURP"; su ÚNICO pendiente permitido es la CURP
 *   duplicada. Cualquier otro pendiente (fotos, documentos, promedio,
 *   persona) rompería la historia que ese fixture debe contar.
 *
 * Si un fixture nuevo se declara READY sin sus requisitos completos, este
 * test falla y señala el folio exacto.
 */
class PreApplicationFixtureConsistencyTest {

    @BeforeTest
    fun resetSharedState() {
        PreApplicationViewModel.resetSharedStateForTests()
    }

    @Test
    fun readyFixturesHaveNoPendingItemsExceptIntentionalCurpConflict() {
        val readyFixtures = PreApplicationViewModel.sharedPreApplications.value
            .filter { it.readinessStatus == ReadinessStatus.READY }
        assertTrue(readyFixtures.isNotEmpty(), "Debe existir al menos un fixture READY")

        readyFixtures.forEach { preApp ->
            val pendings = PreApplicationViewModel.officialEnrollmentPendingItems(preApp)
            if (preApp.folio == "PRE-CONFLICT-001") {
                assertEquals(
                    1, pendings.size,
                    "PRE-CONFLICT-001 debe tener exactamente un pendiente (CURP): $pendings"
                )
                assertTrue(
                    pendings.single().contains("CURP ya registrada"),
                    "El único pendiente de PRE-CONFLICT-001 debe ser la CURP duplicada: $pendings"
                )
            } else {
                assertTrue(
                    pendings.isEmpty(),
                    "Fixture READY '${preApp.folio}' con pendientes: $pendings"
                )
            }
        }
    }

    @Test
    fun convertedFixtureRemainsLinkedToOfficialRecords() {
        // PRE-X1A2 (CONVERTED) debe seguir vinculado a un alumno oficial con
        // el mismo folio de origen; garantiza que los ajustes de fixtures no
        // rompan la cadena institucional usada por D5.
        val converted = PreApplicationViewModel.sharedPreApplications.value
            .single { it.readinessStatus == ReadinessStatus.CONVERTED }
        assertEquals("PRE-X1A2", converted.folio)
        assertTrue(
            PreApplicationViewModel.officialStudents.value.any {
                it.preApplicationFolio == converted.folio
            },
            "El fixture CONVERTED debe tener alumno oficial vinculado"
        )
    }

    @Test
    fun fixtureListStillExposesAllExpectedFolios() {
        val folios = MockPreApplicationData.preApplications.map { it.folio }
        val expected = listOf(
            "PRE-X1A2", "PRE-B8Y2", "PRE-C9Z3", "PRE-D4L4", "PRE-E5M5",
            "PRE-NEW-001", "PRE-CONFLICT-001", "PRE-REENROLL-001"
        )
        assertEquals(expected, folios)
    }
}
