package com.example.ui.auth

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.example.auth.AuthState
import com.example.auth.MockStaffDirectory
import com.example.ui.SaseAppContent
import com.example.ui.SaseAppTestTags
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LabViewModel
import com.example.viewmodel.PreApplicationViewModel
import kotlin.test.Test
import kotlin.test.assertIs

@OptIn(ExperimentalTestApi::class)
class LoginGateUiTest {

    @Test
    fun withoutSessionOnlyTheLoginScreenIsComposed() = runGateUi { _ ->
        onNodeWithTag(LoginTestTags.SCREEN).assertIsDisplayed()
        // Ninguna pantalla institucional existe en el árbol, ni oculta.
        onNodeWithTag(SaseAppTestTags.SIGN_OUT).assertDoesNotExist()
        onNodeWithText("Expedientes").assertDoesNotExist()
        onNodeWithText("Pre-Solicitudes").assertDoesNotExist()
    }

    @Test
    fun submitStaysDisabledUntilBothFieldsAreFilled() = runGateUi { _ ->
        onNodeWithTag(LoginTestTags.SUBMIT).assertIsNotEnabled()
        onNodeWithTag(LoginTestTags.EMAIL).performTextInput("secretaria@example.invalid")
        onNodeWithTag(LoginTestTags.SUBMIT).assertIsNotEnabled()
    }

    @Test
    fun wrongCredentialsShowMessageAndKeepGateClosed() = runGateUi { viewModel ->
        onNodeWithTag(LoginTestTags.EMAIL).performTextInput("secretaria@example.invalid")
        onNodeWithTag(LoginTestTags.PASSWORD).performTextInput("incorrecta")
        onNodeWithTag(LoginTestTags.SUBMIT).performClick()
        waitForIdle()

        onNodeWithTag(LoginTestTags.ERROR).assertIsDisplayed()
        onNodeWithTag(LoginTestTags.SCREEN).assertIsDisplayed()
        assertIs<AuthState.NoSession>(viewModel.authState.value)
    }

    @Test
    fun inactiveAccountCannotEnter() = runGateUi { viewModel ->
        onNodeWithTag(LoginTestTags.EMAIL).performTextInput("baja@example.invalid")
        onNodeWithTag(LoginTestTags.PASSWORD).performTextInput(MockStaffDirectory.DEMO_PASSWORD)
        onNodeWithTag(LoginTestTags.SUBMIT).performClick()
        waitForIdle()

        onNodeWithTag(LoginTestTags.SCREEN).assertIsDisplayed()
        assertIs<AuthState.NoSession>(viewModel.authState.value)
    }

    @Test
    fun validCredentialsOpenTheInstitutionalAppAndLogoutReturnsToLogin() = runGateUi { viewModel ->
        onNodeWithTag(LoginTestTags.EMAIL).performTextInput("secretaria@example.invalid")
        onNodeWithTag(LoginTestTags.PASSWORD).performTextInput(MockStaffDirectory.DEMO_PASSWORD)
        onNodeWithTag(LoginTestTags.SUBMIT).performClick()
        waitForIdle()

        assertIs<AuthState.Active>(viewModel.authState.value)
        onNodeWithTag(LoginTestTags.SCREEN).assertDoesNotExist()
        onNodeWithTag(SaseAppTestTags.SIGN_OUT).assertIsDisplayed()

        onNodeWithTag(SaseAppTestTags.SIGN_OUT).performClick()
        waitForIdle()

        assertIs<AuthState.NoSession>(viewModel.authState.value)
        onNodeWithTag(LoginTestTags.SCREEN).assertIsDisplayed()
    }

    private fun runGateUi(
        block: androidx.compose.ui.test.ComposeUiTest.(LabViewModel) -> Unit
    ) {
        synchronized(gateLock) {
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

    private companion object {
        val gateLock = Any()
    }
}
