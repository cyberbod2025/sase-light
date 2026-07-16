package com.example.viewmodel

import com.example.data.MockSaseData
import com.example.data.presolicitud.PreApplication
import com.example.data.presolicitud.ReadinessStatus
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Cobertura D4 para [PreApplicationViewModel.updatePreApplicationCurp].
 *
 * Contrato vigente (documentado, no inventado):
 * - Normaliza con trim().uppercase().
 * - Rechaza EN SILENCIO (Unit, sin señal) toda CURP normalizada de longitud != 18;
 *   el formato completo (regex) se valida SOLO en la UI (CurpCorrectionDialog).
 * - Folio inexistente: no-op silencioso (updatePreApp mapea sin coincidencias;
 *   la reconciliación retorna en indexOfFirst < 0).
 * - No rechaza CURP duplicada: la protección vive en officialEnrollmentPendingItems
 *   (curpDuplicateInfo) + reconcileReadinessAfterRequirementChange, que degrada
 *   READY→BLOCKED, y en las guardas de alta (PENDING_REQUIREMENTS).
 *
 * Nota de fixtures (post-D6): los fixtures READY tienen fotos en
 * demoPhotoStates() y documentos validados, por lo que su readiness es
 * coherente con officialEnrollmentPendingItems. PRE-CONFLICT-001 conserva
 * deliberadamente su único pendiente: la CURP duplicada.
 */
class PreApplicationCurpCorrectionTest {

    private companion object {
        const val FOLIO_NEW = "PRE-NEW-001"
        const val FOLIO_CONFLICT = "PRE-CONFLICT-001"
        const val CURP_UNIQUE_A = "NEWD020202HDFXYZ88"
        const val CURP_UNIQUE_B = "NEWE030303MDFQRS77"
        const val CURP_DUPLICATED_MASTER = "DEMA100101HDFABC01"
    }

    @BeforeTest
    fun resetSharedState() {
        PreApplicationViewModel.resetSharedStateForTests()
    }

    private fun stored(folio: String): PreApplication =
        PreApplicationViewModel.sharedPreApplications.value.single { it.folio == folio }

    // ── Caso A + B — CURP válida nueva, con espacios y minúsculas ─────────

    @Test
    fun validCurpWithSpacesAndLowercaseIsNormalizedAndPersisted() {
        PreApplicationViewModel.updatePreApplicationCurp(FOLIO_NEW, "  newd020202hdfxyz88  ")

        assertEquals(CURP_UNIQUE_A, stored(FOLIO_NEW).alumnoCurp)
    }

    @Test
    fun validCurpUpdateOnCoherentReadyPreservesReadiness() {
        // Post-D6 el fixture READY no tiene pendientes: una corrección válida
        // que no introduce bloqueos reconcilia sin degradar (contrato: READY
        // con pendientes vacíos permanece READY).
        assertEquals(ReadinessStatus.READY, stored(FOLIO_NEW).readinessStatus)
        assertTrue(PreApplicationViewModel.officialEnrollmentPendingItems(stored(FOLIO_NEW)).isEmpty())

        PreApplicationViewModel.updatePreApplicationCurp(FOLIO_NEW, CURP_UNIQUE_A)

        val after = stored(FOLIO_NEW)
        assertEquals(CURP_UNIQUE_A, after.alumnoCurp)
        assertEquals(ReadinessStatus.READY, after.readinessStatus)
    }

    // ── Caso C — Longitud inválida: rechazo silencioso, sin efectos ───────

    @Test
    fun curpWithInvalidLengthLeavesRecordAndReadinessUntouched() {
        val before = stored(FOLIO_NEW)

        PreApplicationViewModel.updatePreApplicationCurp(FOLIO_NEW, "NEWD020202HDFXYZ8")   // 17
        PreApplicationViewModel.updatePreApplicationCurp(FOLIO_NEW, "NEWD020202HDFXYZ889") // 19
        PreApplicationViewModel.updatePreApplicationCurp(FOLIO_NEW, "   ")                 // vacía

        // Sin cambio de CURP, sin reconciliación (readiness intacta) y sin
        // señal de rechazo: la API retorna Unit (comportamiento actual).
        assertEquals(before, stored(FOLIO_NEW))
    }

    // ── Caso D — Folio inexistente: no-op sin excepción ───────────────────

    @Test
    fun unknownFolioIsASilentNoOpAndTouchesNothing() {
        val before = PreApplicationViewModel.sharedPreApplications.value

        PreApplicationViewModel.updatePreApplicationCurp("PRE-NO-EXISTE", CURP_UNIQUE_A)

        assertEquals(before, PreApplicationViewModel.sharedPreApplications.value)
    }

    // ── Caso E — CURP duplicada por llamada directa ───────────────────────

    @Test
    fun duplicateCurpByDirectCallIsPermittedButReadinessDegrades() {
        // La API NO rechaza la duplicada (la validación visual solo protege la UI).
        // La protección de dominio es la degradación de readiness + guardas de alta.
        assertEquals(ReadinessStatus.READY, stored(FOLIO_NEW).readinessStatus)

        PreApplicationViewModel.updatePreApplicationCurp(FOLIO_NEW, CURP_DUPLICATED_MASTER)

        val after = stored(FOLIO_NEW)
        assertEquals(CURP_DUPLICATED_MASTER, after.alumnoCurp, "El guardado directo se permite (documentado)")
        assertEquals(ReadinessStatus.BLOCKED, after.readinessStatus)
        assertTrue(
            after.readinessNotes!!.contains("CURP ya registrada"),
            "Las notas deben nombrar el conflicto de CURP"
        )
        assertFalse(PreApplicationViewModel.isReadyForOfficialEnrollment(after))
    }

    @Test
    fun correctingCurpResolvesTheDuplicateConflict() {
        // PRE-CONFLICT-001 nace con CURP duplicada del padrón (NUEVO INGRESO).
        assertNotNull(
            PreApplicationViewModel.curpDuplicateInfo(
                FOLIO_CONFLICT, stored(FOLIO_CONFLICT).alumnoCurp, stored(FOLIO_CONFLICT).tramite
            )
        )

        PreApplicationViewModel.updatePreApplicationCurp(FOLIO_CONFLICT, "  newe030303mdfqrs77 ")

        val after = stored(FOLIO_CONFLICT)
        assertEquals(CURP_UNIQUE_B, after.alumnoCurp)
        assertNull(
            PreApplicationViewModel.curpDuplicateInfo(FOLIO_CONFLICT, after.alumnoCurp, after.tramite),
            "El conflicto debe desaparecer tras la corrección"
        )
    }

    // ── Caso F — Reconciliación READY→BLOCKED y corrección parcial ────────

    @Test
    fun readinessDegradesWhenCurpChangeIntroducesBlockAndCurpPendingClearsAfterFix() {
        // 1) READY + CURP duplicada → BLOCKED con pendiente de CURP.
        PreApplicationViewModel.updatePreApplicationCurp(FOLIO_NEW, CURP_DUPLICATED_MASTER)
        val blocked = stored(FOLIO_NEW)
        assertEquals(ReadinessStatus.BLOCKED, blocked.readinessStatus)
        assertTrue(blocked.readinessNotes!!.contains("CURP ya registrada"))

        // 2) Corrección a CURP única con cero pendientes restantes (post-D6):
        //    se alcanza la rama del contrato "BLOCKED + pendientes vacíos" —
        //    permanece BLOCKED y exige declaración institucional explícita.
        PreApplicationViewModel.updatePreApplicationCurp(FOLIO_NEW, CURP_UNIQUE_A)
        val corrected = stored(FOLIO_NEW)
        assertEquals(CURP_UNIQUE_A, corrected.alumnoCurp)
        assertEquals(ReadinessStatus.BLOCKED, corrected.readinessStatus)
        assertFalse(corrected.readinessNotes!!.contains("CURP ya registrada"))
        assertTrue(
            corrected.readinessNotes!!.contains("Pendientes resueltos"),
            "Debe exigir la declaración institucional READY, no auto-promoverse"
        )
        assertTrue(PreApplicationViewModel.officialEnrollmentPendingItems(corrected).isEmpty())
    }

    // ── D5 — Pre-solicitud CONVERTED: identidad institucional inmutable ──

    @Test
    fun convertedPreApplicationCurpIsImmutableAndTouchesNoInstitutionalRecord() {
        // PRE-X1A2 es CONVERTED en el fixture: ya generó alumno oficial y
        // pertenece al dominio institucional. La corrección de CURP desde la
        // pre-solicitud debe ser un no-op total (D5).
        val folioConverted = "PRE-X1A2"
        val before = stored(folioConverted)
        assertEquals(ReadinessStatus.CONVERTED, before.readinessStatus)
        val officialBefore = PreApplicationViewModel.officialStudents.value
        val annualsBefore = MockSaseData.annualEnrollments.value
        val masterBefore = MockSaseData.students.value

        PreApplicationViewModel.updatePreApplicationCurp(folioConverted, CURP_UNIQUE_B)

        assertEquals(before, stored(folioConverted), "La pre-solicitud CONVERTED no debe cambiar")
        assertEquals(officialBefore, PreApplicationViewModel.officialStudents.value, "Alumno oficial intacto")
        assertEquals(annualsBefore, MockSaseData.annualEnrollments.value, "Anualidad intacta")
        assertEquals(masterBefore, MockSaseData.students.value, "Padrón maestro intacto")
    }

    @Test
    fun pendingItemsReflectCurpStateConsistentlyWithDomainRule() {
        // La misma regla de dominio (curpDuplicateInfo) alimenta pendingItems:
        // sin duplicado no hay ítem de CURP; con duplicado sí (NUEVO INGRESO).
        val pendingsBefore = PreApplicationViewModel.officialEnrollmentPendingItems(stored(FOLIO_NEW))
        assertFalse(pendingsBefore.any { it.contains("CURP") })

        PreApplicationViewModel.updatePreApplicationCurp(FOLIO_NEW, CURP_DUPLICATED_MASTER)

        val pendingsAfter = PreApplicationViewModel.officialEnrollmentPendingItems(stored(FOLIO_NEW))
        assertTrue(pendingsAfter.any { it.contains("CURP ya registrada") })
    }
}
