package com.example.ui.enrollment

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.use
import com.example.data.MockSaseData
import com.example.data.auth.MockAuthRepositoryImpl
import com.example.data.auth.StaffRole
import com.example.data.enrollment.MockSchoolMovementRepositoryImpl
import com.example.data.enrollment.SchoolMovementType
import com.example.data.enrollment.StudentCutoffStatus
import com.example.data.repository.MockAuditRepositoryImpl
import com.example.data.repository.MockStudentRepositoryImpl
import com.example.environment.AppEnvironment
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LabViewModel
import com.example.viewmodel.Screen
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Prueba automatizada de interfaz (Compose Desktop `ImageComposeScene`) que verifica
 * mediante interacción con la aplicación la secuencia completa:
 *
 * `captura → guardado → reapertura → historial → corte → descarga efectiva del CSV a disco`
 *
 * Comprueba que los indicadores en pantalla y el archivo `.csv` descargado en el sistema
 * de archivos coincidan exactamente.
 */
class EnrollmentCutoffUiInteractionTest {

    @BeforeTest
    fun setup() {
        MockSaseData.resetForTests()
    }

    private fun saveSceneScreenshot(scene: ImageComposeScene, stepName: String) {
        val image = scene.render()
        val encoded = image.encodeToData(EncodedImageFormat.PNG) ?: return
        val outDir = File(System.getProperty("sase.ui.screenshots.dir") ?: System.getProperty("java.io.tmpdir"), "sase-ui-evidence")
        if (!outDir.exists()) outDir.mkdirs()
        File(outDir, "$stepName.png").writeBytes(encoded.bytes)
    }

    @Test
    fun testInteraccionCompletaAplicacionCapturaGuardadoReaperturaHistorialCorteYDescargaCsv() = runBlocking {
        val exportDir = File(System.getProperty("java.io.tmpdir"), "sase-test-csv-downloads-${System.currentTimeMillis()}")
        System.setProperty("sase.export.dir", exportDir.absolutePath)

        val studentRepo = MockStudentRepositoryImpl()
        val authRepo = MockAuthRepositoryImpl()
        val auditRepo = MockAuditRepositoryImpl()
        val movementRepo = MockSchoolMovementRepositoryImpl(studentRepo)

        val vm = LabViewModel(
            appEnvironment = AppEnvironment.demoLocal("ui-test"),
            authRepository = authRepo,
            studentRepository = studentRepo,
            auditRepository = auditRepo,
            schoolMovementRepository = movementRepo
        )

        // 0. Autenticar sesión como SECRETARIA de forma síncrona y navegar a la pantalla de Corte Institucional
        authRepo.signIn("secretaria@example.invalid", "demo1234")
        assertNotNull(vm.session.value, "La sesión de SECRETARIA debe estar activa")
        vm.navigateTo(Screen.EnrollmentCutoffReport)
        assertEquals(Screen.EnrollmentCutoffReport, vm.currentScreen.value)

        val uiState1 = EnrollmentCutoffUiStateHolder(vm)
        uiState1.refreshCutoffAndHistory()

        val initialSnapshot = assertNotNull(uiState1.snapshot, "El corte inicial debe estar calculado en pantalla")
        val initialActive = initialSnapshot.totalActiveStudents
        val initialTransfers = initialSnapshot.transfersOutTotal
        val initialHistorySize = uiState1.movementsHistory.size

        // 1. CAPTURA Y GUARDADO en la pantalla renderizada con ImageComposeScene
        ImageComposeScene(width = 1280, height = 900).use { scene ->
            scene.setContent {
                MyApplicationTheme {
                    EnrollmentCutoffReportScreen(viewModel = vm, externalStateHolder = uiState1)
                }
            }
            scene.render()

            // Abrir modal de captura de movimiento para el alumno "4" (ALUMNO DEMO 04)
            uiState1.openCaptureModal(defaultStudentId = "4")
            assertTrue(uiState1.showCaptureForm, "El formulario de captura debe abrirse en la UI")
            uiState1.captureId = "MOV-UI-005"
            uiState1.captureStudentId = "4"
            uiState1.captureMovementType = SchoolMovementType.TRASLADO_SALIENTE
            uiState1.captureEffectiveDate = "2026-10-20"
            uiState1.captureFromGroup = "1°A"
            uiState1.captureDestinationSchool = "Escuela Secundaria General No. 8, CCT 09DES0008K"
            uiState1.captureDocumentationSource = "Oficio de Traslado SEP-2026-901"
            uiState1.captureClassificationCriteria = "Cambio de domicilio familiar comprobado"
            scene.render()
            saveSceneScreenshot(scene, "01_ui_captura_movimiento")

            // Guardar movimiento desde la UI
            val saveResult = uiState1.submitCapturedMovement()
            assertTrue(saveResult.isSuccess, "El guardado desde la UI debe ser exitoso")
            assertFalse(uiState1.showCaptureForm, "El formulario debe cerrarse tras guardar")
            assertTrue(
                uiState1.statusMessage?.contains("MOV-UI-005") == true,
                "El mensaje de estado en pantalla debe confirmar el guardado de MOV-UI-005"
            )
            scene.render()
            saveSceneScreenshot(scene, "02_ui_movimiento_guardado")
        }

        // 2. CERRAR / REABRIR PANTALLA (Salir al tablero de Secretaría y volver a abrir Corte de Matrícula)
        vm.navigateTo(Screen.SecretaryDashboard)
        assertEquals(Screen.SecretaryDashboard, vm.currentScreen.value)
        vm.navigateTo(Screen.EnrollmentCutoffReport)
        assertEquals(Screen.EnrollmentCutoffReport, vm.currentScreen.value)

        val uiStateReopened = EnrollmentCutoffUiStateHolder(vm)
        uiStateReopened.refreshCutoffAndHistory()

        ImageComposeScene(width = 1280, height = 900).use { sceneReopened ->
            sceneReopened.setContent {
                MyApplicationTheme {
                    EnrollmentCutoffReportScreen(viewModel = vm, externalStateHolder = uiStateReopened)
                }
            }
            sceneReopened.render()
            saveSceneScreenshot(sceneReopened, "03_ui_reapertura_historial_y_corte")

            // 3. CONSULTAR HISTORIAL en la pantalla reabierta
            assertEquals(
                initialHistorySize + 1,
                uiStateReopened.movementsHistory.size,
                "El historial en pantalla tras reabrir debe incluir el nuevo movimiento"
            )
            val savedInHistory = uiStateReopened.movementsHistory.firstOrNull { it.id == "MOV-UI-005" }
            assertNotNull(savedInHistory, "MOV-UI-005 debe aparecer en el historial fechado de la pantalla")
            assertEquals("4", savedInHistory.studentId)
            assertEquals(SchoolMovementType.TRASLADO_SALIENTE, savedInHistory.movementType)
            assertEquals("Escuela Secundaria General No. 8, CCT 09DES0008K", savedInHistory.destinationSchool)

            // 4. VERIFICAR CORTE en la pantalla reabierta
            val reopenedSnapshot = assertNotNull(uiStateReopened.snapshot)
            assertEquals(
                initialActive - 1,
                reopenedSnapshot.totalActiveStudents,
                "La matrícula activa en pantalla debe reducirse en 1 tras el traslado del alumno 4"
            )
            assertEquals(
                initialTransfers + 1,
                reopenedSnapshot.transfersOutTotal,
                "El contador de traslados salientes en pantalla debe incrementarse en 1"
            )
            val stu4Detail = reopenedSnapshot.studentDetails.first { it.studentId == "4" }
            assertEquals(StudentCutoffStatus.TRASLADADO, stu4Detail.status)

            // 5. DESCARGA EFECTIVA DEL CSV A DISCO y comprobación Pantalla == Archivo CSV
            val pmcArtifact = assertNotNull(uiStateReopened.downloadAggregatedPmcCsv(), "Debe descargarse el CSV agregado PMC")
            sceneReopened.render()
            saveSceneScreenshot(sceneReopened, "04_ui_descarga_csv_agregado")

            val pmcFile = File(pmcArtifact.filePath)
            assertTrue(pmcFile.exists(), "El archivo CSV agregado debe existir físicamente en disco: ${pmcFile.absolutePath}")
            val pmcDiskContent = pmcFile.readText(Charsets.UTF_8)
            assertTrue(pmcDiskContent.isNotBlank())

            // Comprobar que cada métrica de la pantalla coincide con el archivo CSV en disco
            assertTrue(
                pmcDiskContent.contains("Matricula Activa al Corte,${reopenedSnapshot.totalActiveStudents},"),
                "El archivo CSV en disco debe coincidir con la Matrícula Activa de la pantalla (${reopenedSnapshot.totalActiveStudents})"
            )
            assertTrue(
                pmcDiskContent.contains("Cohorte Base al Inicio,${reopenedSnapshot.initialCohortTotal},"),
                "El archivo CSV en disco debe coincidir con la Cohorte Base de la pantalla (${reopenedSnapshot.initialCohortTotal})"
            )
            assertTrue(
                pmcDiskContent.contains("Tasa de Retencion de Cohorte,${reopenedSnapshot.cohortRetentionRate}%,"),
                "El archivo CSV en disco debe coincidir con la Tasa de Retención de la pantalla (${reopenedSnapshot.cohortRetentionRate}%)"
            )
            assertTrue(
                pmcDiskContent.contains("Traslados Salientes Acreditados,${reopenedSnapshot.transfersOutTotal},"),
                "El archivo CSV en disco debe coincidir con los Traslados Salientes de la pantalla (${reopenedSnapshot.transfersOutTotal})"
            )
            assertTrue(
                pmcDiskContent.contains("Bajas Administrativas,${reopenedSnapshot.administrativeDropoutsTotal},"),
                "El archivo CSV en disco debe coincidir con las Bajas Administrativas de la pantalla (${reopenedSnapshot.administrativeDropoutsTotal})"
            )
            assertTrue(
                pmcDiskContent.contains("Abandonos Escolares Confirmados,${reopenedSnapshot.confirmedDropoutsTotal},"),
                "El archivo CSV en disco debe coincidir con los Abandonos Confirmados de la pantalla (${reopenedSnapshot.confirmedDropoutsTotal})"
            )

            // Descargar también el Padrón Nominal restringido (.CSV) y verificar coincidencia en disco
            val nominalArtifact = assertNotNull(uiStateReopened.downloadRestrictedNominalCsv(), "Debe descargarse el CSV nominal")
            val nominalFile = File(nominalArtifact.filePath)
            assertTrue(nominalFile.exists(), "El archivo CSV nominal debe existir físicamente en disco: ${nominalFile.absolutePath}")
            val nominalDiskContent = nominalFile.readText(Charsets.UTF_8)
            assertTrue(
                nominalDiskContent.contains("ALUMNO DEMO 04") &&
                    nominalDiskContent.contains("Escuela Secundaria General No. 8; CCT 09DES0008K") &&
                    nominalDiskContent.contains("Oficio de Traslado SEP-2026-901"),
                "El archivo CSV nominal en disco debe contener el registro detallado de ALUMNO DEMO 04 tal como se muestra en pantalla"
            )
        }
    }
}
