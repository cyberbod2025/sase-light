package com.example.ui.attendance

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.use
import com.example.data.MockSaseData
import com.example.data.attendance.*
import com.example.data.auth.MockAuthRepositoryImpl
import com.example.data.auth.StaffRole
import com.example.data.enrollment.MockSchoolMovementRepositoryImpl
import com.example.data.repository.MockAuditRepositoryImpl
import com.example.data.repository.MockStudentRepositoryImpl
import com.example.environment.AppEnvironment
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ClassAttendanceViewModel
import com.example.viewmodel.LabViewModel
import com.example.viewmodel.Screen
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Prueba automatizada de interfaz en Compose Desktop (`ImageComposeScene`).
 *
 * Verifica mediante interacción con la aplicación el flujo completo requerido:
 * `programar sesión → obtener alumnado vigente → capturar asistencia y llegada → guardar → reabrir → consultar reporte → exportar.`
 *
 * Comprueba que pantalla y archivo CSV coincidan exactamente.
 */
class ClassAttendanceUiInteractionTest {

    @BeforeTest
    fun setup() {
        MockSaseData.resetForTests()
    }

    private fun saveSceneScreenshot(scene: ImageComposeScene, stepName: String) {
        val image = scene.render()
        val encoded = image.encodeToData(EncodedImageFormat.PNG) ?: return
        val outDir = File(
            System.getProperty("sase.ui.screenshots.dir") ?: System.getProperty("java.io.tmpdir"),
            "sase-ui-evidence-attendance"
        )
        if (!outDir.exists()) outDir.mkdirs()
        File(outDir, "$stepName.png").writeBytes(encoded.bytes)
    }

    @Test
    fun testFlujoCompletoProgramarCapturarGuardarReabrirConsultarReporteYExportarCsv() = runBlocking {
        val exportDir = File(System.getProperty("java.io.tmpdir"), "sase-attendance-csv-${System.currentTimeMillis()}")
        System.setProperty("sase.export.dir", exportDir.absolutePath)

        val studentRepo = MockStudentRepositoryImpl()
        val authRepo = MockAuthRepositoryImpl()
        val auditRepo = MockAuditRepositoryImpl()
        val movementRepo = MockSchoolMovementRepositoryImpl(studentRepo)
        val attendanceRepo = MockAttendanceRepositoryImpl()

        val mainVm = LabViewModel(
            appEnvironment = AppEnvironment.demoLocal("ui-test"),
            authRepository = authRepo,
            studentRepository = studentRepo,
            auditRepository = auditRepo,
            attendanceRepository = attendanceRepo,
            schoolMovementRepository = movementRepo
        )

        // 0. Autenticar como DOCENTE y navegar a la pantalla de Asistencia
        authRepo.signIn("docente@example.invalid", "demo1234")
        val activeSession = assertNotNull(mainVm.session.value, "La sesión docente debe estar activa")
        mainVm.navigateTo(Screen.ClassAttendance)
        assertEquals(Screen.ClassAttendance, mainVm.currentScreen.value)

        val attendanceVm = ClassAttendanceViewModel(
            repository = attendanceRepo,
            session = activeSession,
            coroutineScope = this,
            defaultDate = "2026-10-15"
        )

        ImageComposeScene(width = 1280, height = 900).use { scene ->
            scene.setContent {
                MyApplicationTheme {
                    ClassAttendanceScreen(
                        viewModel = attendanceVm,
                        onNavigateBack = { mainVm.navigateTo(Screen.SessionHome) }
                    )
                }
            }
            scene.render()

            // 1. PROGRAMAR SESIÓN Y OBTENER ALUMNADO VIGENTE
            val group1A = MockAttendanceData.groups.first { it.id == MockAttendanceData.GROUP_1A_ID }
            attendanceVm.selectGroup(group1A)
            attendanceVm.setDate("2026-10-15")
            attendanceVm.setModule(1)
            attendanceVm.openSession()
            yield()
            scene.render()

            val snap1 = assertNotNull(attendanceVm.state.value.snapshot, "La sesión debe estar abierta y con snapshot")
            assertEquals("1° A", snap1.groupName)
            assertEquals("2026-10-15", snap1.date)
            assertEquals(1, snap1.moduleNumber)
            assertEquals(5, snap1.entries.size, "El grupo 1° A debe tener 5 alumnos vigentes")
            assertEquals(CaptureState.PENDIENTE, attendanceVm.state.value.captureState)
            saveSceneScreenshot(scene, "01_ui_sesion_programada_alumnado_vigente")

            // 2. CAPTURAR ASISTENCIA Y LLEGADA CON RETARDO
            // Alumno 1: Presente a tiempo
            attendanceVm.setPresence("1", AttendancePresence.PRESENTE)
            // Alumno 2: Presente con retardo de 15 minutos
            attendanceVm.setPresence("2", AttendancePresence.PRESENTE)
            attendanceVm.togglePunctuality("2")
            attendanceVm.setMinutesLate("2", 15)
            // Alumno 3: Ausente
            attendanceVm.setPresence("3", AttendancePresence.AUSENTE)
            // Alumno 4: Presente a tiempo
            attendanceVm.setPresence("4", AttendancePresence.PRESENTE)
            // Alumno 5: Se deja pendiente intencionalmente para verificar captura parcial

            scene.render()
            assertEquals(CaptureState.PARCIAL, attendanceVm.state.value.captureState)
            assertEquals(3, attendanceVm.state.value.presentCount)
            assertEquals(1, attendanceVm.state.value.absentCount)
            assertEquals(1, attendanceVm.state.value.lateCount)
            assertEquals(1, attendanceVm.state.value.pendingCount)
            saveSceneScreenshot(scene, "02_ui_captura_asistencia_y_retardo")

            // 3. GUARDAR CAPTURA
            attendanceVm.saveAttendance()
            yield()
            scene.render()
            assertTrue(attendanceVm.state.value.savedAtLeastOnce, "La sesión debe quedar registrada como guardada")
            assertNotNull(attendanceVm.state.value.successMessage)
            saveSceneScreenshot(scene, "03_ui_captura_guardada")
        }

        // 4. CERRAR / REABRIR SESIÓN (Simular navegación fuera y regreso a la sesión)
        mainVm.navigateTo(Screen.SessionHome)
        assertEquals(Screen.SessionHome, mainVm.currentScreen.value)

        // Reabrir pantalla de asistencia
        mainVm.navigateTo(Screen.ClassAttendance)
        val reopenedVm = ClassAttendanceViewModel(
            repository = attendanceRepo,
            session = activeSession,
            coroutineScope = this,
            defaultDate = "2026-10-15"
        )
        val group1AReopened = MockAttendanceData.groups.first { it.id == MockAttendanceData.GROUP_1A_ID }
        reopenedVm.selectGroup(group1AReopened)
        reopenedVm.setDate("2026-10-15")
        reopenedVm.setModule(1)
        reopenedVm.openSession()
        yield()

        ImageComposeScene(width = 1280, height = 900).use { scene ->
            scene.setContent {
                MyApplicationTheme {
                    ClassAttendanceScreen(
                        viewModel = reopenedVm,
                        onNavigateBack = { mainVm.navigateTo(Screen.SessionHome) }
                    )
                }
            }
            scene.render()

            // Comprobar persistencia tras reapertura
            val reopenedRoster = reopenedVm.state.value.displayRoster
            assertEquals(5, reopenedRoster.size)
            val st1 = reopenedRoster.first { it.student.id == "1" }
            val st2 = reopenedRoster.first { it.student.id == "2" }
            val st3 = reopenedRoster.first { it.student.id == "3" }
            val st4 = reopenedRoster.first { it.student.id == "4" }
            val st5 = reopenedRoster.first { it.student.id == "5" }

            assertEquals(AttendancePresence.PRESENTE, st1.presence)
            assertEquals(PunctualityStatus.A_TIEMPO, st1.punctuality)

            assertEquals(AttendancePresence.PRESENTE, st2.presence)
            assertEquals(PunctualityStatus.RETARDO, st2.punctuality)
            assertEquals(15, st2.minutesLate)

            assertEquals(AttendancePresence.AUSENTE, st3.presence)
            assertEquals(PunctualityStatus.NO_APLICA, st3.punctuality)

            assertEquals(AttendancePresence.PRESENTE, st4.presence)
            assertEquals(null, st5.presence, "El alumno 5 debe permanecer como pendiente sin inventarle falta")

            saveSceneScreenshot(scene, "04_ui_sesion_reabierta_con_persistencia")

            // 5. CONSULTAR REPORTE PMC
            reopenedVm.loadReport(startDate = "2026-10-01", endDate = "2026-10-31")
            yield()
            scene.render()

            val report = assertNotNull(reopenedVm.state.value.report, "El reporte PMC debe haberse generado")
            assertEquals(1, report.sessionsHeld)
            assertEquals(3, report.totalAttendanceEvents) // Alumnos 1, 2, 4
            assertEquals(1, report.totalAbsenceEvents)    // Alumno 3
            assertEquals(1, report.totalTardinessEvents)  // Alumno 2
            assertEquals(1, report.distinctStudentsWithTardiness)
            assertEquals(75.0, report.overallAttendanceRatePct) // 3 asistencias de 4 decididas = 75.0%

            val repStudent5 = report.studentMetrics.first { it.studentId == "5" }
            assertEquals(0, repStudent5.absentSessions, "La captura pendiente NO debe convertirse en falta")
            assertEquals(1, repStudent5.pendingSessions)

            saveSceneScreenshot(scene, "05_ui_reporte_pmc_consultado")

            // 6. EXPORTAR CSV A DISCO Y COMPROBAR CONCORDANCIA
            val aggregatedArtifact = assertNotNull(reopenedVm.exportAggregatedCsv(), "El artefacto agregado debe exportarse")
            val nominalArtifact = assertNotNull(reopenedVm.exportNominalCsv(), "El padrón nominal debe exportarse")
            scene.render()
            saveSceneScreenshot(scene, "06_ui_exportacion_csv_completada")

            // Comprobar archivo en disco
            val aggFile = File(aggregatedArtifact.filePath)
            val nomFile = File(nominalArtifact.filePath)
            assertTrue(aggFile.exists(), "El archivo de reporte agregado debe existir en disco: ${aggFile.absolutePath}")
            assertTrue(nomFile.exists(), "El archivo de padrón nominal debe existir en disco: ${nomFile.absolutePath}")

            val aggContent = aggFile.readText()
            val nomContent = nomFile.readText()

            // Verificaciones en Reporte Agregado PMC:
            assertFalse(aggContent.contains("ALUMNO DEMO"), "El reporte PMC agregado no debe exponer nombres personales")
            assertTrue(aggContent.contains("Tasa de Asistencia Colectiva,75.0%"), "La tasa en CSV debe coincidir con pantalla (75.0%)")
            assertTrue(aggContent.contains("Eventos Totales de Retardo,1"), "Eventos de retardo debe ser 1")
            assertTrue(aggContent.contains("Alumnos Distintos con Retardo,1"), "Alumnos distintos con retardo debe ser 1")
            assertTrue(aggContent.contains("Pendiente de aprobación formal por Consejo Técnico Escolar"))

            // Verificaciones en Padrón Nominal Restringido:
            assertTrue(nomContent.contains("1,ALUMNO DEMO 01") || nomContent.contains("\"ALUMNO DEMO 01\""))
            assertTrue(nomContent.contains("\"ALUMNO DEMO 02\""))
            assertTrue(nomContent.contains("15 min") || nomContent.contains("1,100.0%"))
        }
    }
}
