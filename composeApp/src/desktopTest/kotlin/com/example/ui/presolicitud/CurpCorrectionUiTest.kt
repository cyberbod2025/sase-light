package com.example.ui.presolicitud

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.example.data.MockSaseData
import com.example.data.presolicitud.ReadinessStatus
import com.example.ui.SaseAppContent
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.CurpConflictSource
import com.example.viewmodel.CurpCorrectionPolicy
import com.example.viewmodel.CurpCorrectionResult
import com.example.viewmodel.CurpValidationResult
import com.example.viewmodel.LabViewModel
import com.example.viewmodel.PreApplicationViewModel
import com.example.viewmodel.Screen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CurpCorrectionUiTest {

    @Test
    fun cancelAfterEditingClosesDialogWithoutMutationOrSuccess() = runIsolatedUi { viewModel ->
        val before = snapshot()
        val originalCurp = conflictRecord().alumnoCurp

        openConflictFixture(viewModel)
        openCorrectionDialog()
        replaceCurp("newd020202hdfxyz88")
        onNodeWithTag(CANCEL).performClick()
        waitForIdle()

        onNodeWithTag(DIALOG).assertDoesNotExist()
        onNodeWithTag(CONFLICT_PANEL).assertIsDisplayed()
        onNodeWithText(SUCCESS_MESSAGE).assertDoesNotExist()
        assertEquals(before, snapshot())

        openCorrectionDialog()
        onNodeWithTag(INPUT).assertTextContains(originalCurp)
        onNodeWithTag(CANCEL).performClick()
    }

    @Test
    fun invalidFormatKeepsDialogConflictAndInstitutionalState() = runIsolatedUi { viewModel ->
        val before = snapshot()

        openConflictFixture(viewModel)
        openCorrectionDialog()
        replaceCurp("ABC")
        onNodeWithTag(SAVE).performClick()
        waitForIdle()

        onNodeWithTag(DIALOG).assertIsDisplayed()
        onNodeWithTag(INPUT).assertTextContains("ABC")
        onNodeWithText(CurpCorrectionPolicy.INVALID_FORMAT_MESSAGE).assertIsDisplayed()
        onNodeWithTag(CONFLICT_PANEL).assertExists()
        onNodeWithTag(OFFICIAL_ENROLLMENT).assertDoesNotExist()
        onNodeWithText(SUCCESS_MESSAGE).assertDoesNotExist()
        assertEquals(before, snapshot())
    }

    @Test
    fun duplicateCurpIsRejectedWithoutMutation() = runIsolatedUi { viewModel ->
        val before = snapshot()
        val controlled = assertIs<CurpCorrectionResult.Duplicate>(
            PreApplicationViewModel.correctPreApplicationCurp(FOLIO, DUPLICATE_CURP)
        )
        assertTrue(
            controlled.conflict.matches.any { it.source == CurpConflictSource.PRE_APPLICATION }
        )
        assertEquals(before, snapshot())

        openConflictFixture(viewModel)
        openCorrectionDialog()
        replaceCurp(DUPLICATE_CURP)
        onNodeWithTag(SAVE).performClick()
        waitForIdle()

        onNodeWithTag(DIALOG).assertIsDisplayed()
        onNodeWithText(DUPLICATE_MESSAGE).assertIsDisplayed()
        onNodeWithTag(CONFLICT_PANEL).assertExists()
        onNodeWithTag(OFFICIAL_ENROLLMENT).assertDoesNotExist()
        onNodeWithText(SUCCESS_MESSAGE).assertDoesNotExist()
        assertEquals(before, snapshot())
    }

    @Test
    fun validCurpNormalizesRemovesConflictAndEnablesOfficialEnrollment() = runIsolatedUi { viewModel ->
        val before = snapshot()
        val untouchedPreApplications =
            PreApplicationViewModel.sharedPreApplications.value.filterNot { it.folio == FOLIO }
        val validation = assertIs<CurpValidationResult.Valid>(
            CurpCorrectionPolicy.validate(VALID_CURP)
        )
        assertEquals(VALID_CURP, validation.normalizedCurp)
        val institutionalCurps = (
            PreApplicationViewModel.sharedPreApplications.value
                .filterNot { it.folio == FOLIO }
                .map { it.alumnoCurp } +
                PreApplicationViewModel.officialStudents.value.map { it.curp } +
                MockSaseData.students.value.map { it.curp }
            ).map(CurpCorrectionPolicy::normalize)
        assertFalse(VALID_CURP in institutionalCurps)

        openConflictFixture(viewModel)
        openCorrectionDialog()
        val enteredCurp = "  ${VALID_CURP.lowercase()}  "
        replaceCurp(enteredCurp)
        onNodeWithTag(INPUT).assertTextContains(enteredCurp)
        onNodeWithTag(SAVE).performClick()
        waitForIdle()

        onNodeWithTag(DIALOG).assertDoesNotExist()
        onNodeWithTag(CONFLICT_PANEL).assertDoesNotExist()
        onNodeWithText(CurpCorrectionPolicy.INVALID_FORMAT_MESSAGE).assertDoesNotExist()
        onNodeWithText(DUPLICATE_MESSAGE).assertDoesNotExist()
        onNodeWithTag(OFFICIAL_ENROLLMENT).performScrollTo().assertIsDisplayed().assertIsEnabled()
        val corrected = conflictRecord()
        assertEquals(VALID_CURP, corrected.alumnoCurp)
        assertEquals(ReadinessStatus.READY, corrected.readinessStatus)
        assertEquals(null, PreApplicationViewModel.curpDuplicateInfo(FOLIO, corrected.alumnoCurp))
        assertTrue(PreApplicationViewModel.isReadyForOfficialEnrollment(corrected))
        assertEquals(
            untouchedPreApplications,
            PreApplicationViewModel.sharedPreApplications.value.filterNot { it.folio == FOLIO }
        )
        assertEquals(before.officialStudents, snapshot().officialStudents)
        assertEquals(before.masterStudents, snapshot().masterStudents)
        assertEquals(before.annualEnrollments, snapshot().annualEnrollments)
    }

    @Test
    fun freshRunnerRestoresOriginalConflictFixture() = runIsolatedUi { viewModel ->
        val original = conflictRecord()
        assertNotNull(PreApplicationViewModel.curpDuplicateInfo(FOLIO, original.alumnoCurp))
        assertFalse(original.alumnoCurp == VALID_CURP)

        openConflictFixture(viewModel)

        onNodeWithTag(CONFLICT_PANEL).assertIsDisplayed()
        onNodeWithTag(OFFICIAL_ENROLLMENT).assertDoesNotExist()
        assertEquals(ReadinessStatus.READY, conflictRecord().readinessStatus)
    }

    private fun runIsolatedUi(block: ComposeUiTest.(LabViewModel) -> Unit) {
        synchronized(stateLock) {
            PreApplicationViewModel.resetSharedStateForTests()
            try {
                val viewModel = LabViewModel()
                runDesktopComposeUiTest(width = 1100, height = 700) {
                    setContent {
                        MyApplicationTheme {
                            SaseAppContent(viewModel)
                        }
                    }
                    waitForIdle()
                    block(viewModel)
                }
            } finally {
                PreApplicationViewModel.resetSharedStateForTests()
            }
        }
    }

    private fun ComposeUiTest.openConflictFixture(viewModel: LabViewModel) {
        onNodeWithTag(SIDEBAR_PREAPPLICATIONS).assertIsDisplayed().performClick()
        waitForIdle()
        runOnIdle {
            assertEquals(Screen.SecretariaPreApplicationDashboard, viewModel.currentScreen.value)
        }
        onNodeWithTag(SIDEBAR_PREAPPLICATIONS).assertIsSelected()
        onNodeWithTag(SEARCH).performTextInput(FOLIO)
        waitForIdle()
        onNodeWithTag(ROW).performScrollTo().assertIsDisplayed().performClick()
        waitForIdle()
        onNodeWithTag(ROW).assertIsSelected()
        onNodeWithTag(CONFLICT_PANEL).performScrollTo().assertIsDisplayed()
    }

    private fun ComposeUiTest.openCorrectionDialog() {
        onNodeWithTag(CORRECT_CURP).performScrollTo().assertIsDisplayed().performClick()
        waitForIdle()
        onNodeWithTag(DIALOG).assertIsDisplayed()
    }

    private fun ComposeUiTest.replaceCurp(value: String) {
        onNodeWithTag(INPUT).performTextClearance()
        onNodeWithTag(INPUT).performTextInput(value)
    }

    private fun conflictRecord() =
        PreApplicationViewModel.sharedPreApplications.value.single { it.folio == FOLIO }

    private fun snapshot() = InstitutionalSnapshot(
        preApplications = PreApplicationViewModel.sharedPreApplications.value.toList(),
        officialStudents = PreApplicationViewModel.officialStudents.value.toList(),
        masterStudents = MockSaseData.students.value.toList(),
        annualEnrollments = MockSaseData.annualEnrollments.value.toList(),
    )

    private data class InstitutionalSnapshot(
        val preApplications: List<*>,
        val officialStudents: List<*>,
        val masterStudents: List<*>,
        val annualEnrollments: List<*>,
    )

    private companion object {
        val stateLock = Any()

        const val FOLIO = "PRE-CONFLICT-001"
        const val VALID_CURP = "NEWD020202HDFXYZ88"
        const val DUPLICATE_CURP = "NEWC010101HDFABC99"
        const val SUCCESS_MESSAGE = "CURP actualizada correctamente"
        const val DUPLICATE_MESSAGE = "CURP ya registrada en otra pre-solicitud."

        const val SIDEBAR_PREAPPLICATIONS = "secretary_sidebar_preapplications"
        const val SEARCH = "preapplication_search"
        const val ROW = "preapplication_row_PRE-CONFLICT-001"
        const val CONFLICT_PANEL = "curp_conflict_panel"
        const val CORRECT_CURP = "correct_curp_button"
        const val DIALOG = "curp_correction_dialog"
        const val INPUT = "curp_correction_input"
        const val CANCEL = "curp_correction_cancel"
        const val SAVE = "curp_correction_save"
        const val OFFICIAL_ENROLLMENT = "official_enrollment_action"
    }
}
