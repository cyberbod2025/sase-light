package com.example.integration

import com.example.data.InstitutionalRecordDataQuality
import com.example.data.InstitutionalStudentRecordResolution
import com.example.data.MockSaseData
import com.example.data.Student
import com.example.data.StudentAddResult
import com.example.data.enrollment.AnnualEnrollmentFlowCoordinator
import com.example.data.enrollment.AnnualEnrollmentFlowRequest
import com.example.data.enrollment.AnnualEnrollmentFlowResult
import com.example.data.enrollment.AnnualEnrollmentInitialStatus
import com.example.data.enrollment.AnnualEnrollmentMovement
import com.example.data.enrollment.AnnualEnrollmentRecord
import com.example.data.enrollment.GroupPlacementRequirement
import com.example.data.presolicitud.DocumentoDeclarado
import com.example.data.presolicitud.MockPreApplicationData
import com.example.data.presolicitud.PreApplication
import com.example.data.presolicitud.PreApplicationAdministrativeChanges
import com.example.data.presolicitud.PreApplicationAdministrativeFieldChange
import com.example.data.presolicitud.PreApplicationStatus
import com.example.data.presolicitud.ReadinessStatus
import com.example.data.presolicitud.UpdatePreApplicationAdministrativeDataRequest
import com.example.data.presolicitud.UpdatePreApplicationAdministrativeDataResult
import com.example.data.presolicitud.administrativeDataSnapshot
import com.example.ui.presolicitud.institutionalEnrollmentMessage
import com.example.ui.presolicitud.institutionalEnrollmentRecordAction
import com.example.ui.student.InstitutionalStudentRecordPresentation
import com.example.ui.student.institutionalStudentRecordPresentation
import com.example.ui.student.resolveInstitutionalStudentRecordForRoute
import com.example.viewmodel.FamilySubmissionResult
import com.example.viewmodel.InstitutionalAnnualEnrollmentResult
import com.example.viewmodel.InstitutionalEnrollmentGuardCause
import com.example.viewmodel.LabViewModel
import com.example.viewmodel.PreApplicationSynchronizationCause
import com.example.viewmodel.PreApplicationViewModel
import com.example.viewmodel.ReadinessResult
import com.example.viewmodel.Screen
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InstitutionalGoldenPathTest {
    @BeforeTest
    fun resetBefore() {
        PreApplicationViewModel.resetSharedStateForTests()
    }

    @AfterTest
    fun resetAfter() {
        PreApplicationViewModel.resetSharedStateForTests()
    }

    @Test
    fun completedConversionNavigationPresentationAndReplayKeepOneIdentity() {
        val ready = prepareReadyPreApplication(
            folio = "PRE-L7-NEW",
            curp = "LSEV100101HDFABC01",
            movement = "NUEVO INGRESO"
        )

        val completed = assertIs<InstitutionalAnnualEnrollmentResult.Completed>(process(ready))
        val action = assertNotNull(institutionalEnrollmentRecordAction(completed))
        val route = navigate(action.key.studentId, action.key)
        val content = resolvedContent(route)

        assertEquals("MASTER-V2-PRE-L7-NEW", route.studentId)
        assertEquals(route.studentId, action.key.studentId)
        assertEquals(completed.annualResult.enrollmentId, action.key.enrollmentId)
        assertEquals("2026-2027", content.value("Ciclo escolar"))
        assertEquals("PRE-L7-NEW", content.value("Folio de pre-solicitud"))
        assertEquals("DOMICILIO INSTITUCIONAL L7", content.value("Domicilio"))
        assertEquals("5512345678", content.value("Teléfono del hogar"))
        assertEquals("Convertida", content.value("Readiness"))
        assertEquals(InstitutionalRecordDataQuality.PENDING, content.field("Grupo").quality)
        assertNoLegacyDefaults(content)

        val studentsBeforeReplay = MockSaseData.students.value.toList()
        val annualsBeforeReplay = MockSaseData.annualEnrollments.value.toList()
        val replay = assertIs<InstitutionalAnnualEnrollmentResult.AlreadyCompleted>(process(ready))
        val replayAction = assertNotNull(institutionalEnrollmentRecordAction(replay))

        assertEquals(action.key, replayAction.key)
        assertEquals(studentsBeforeReplay, MockSaseData.students.value)
        assertEquals(annualsBeforeReplay, MockSaseData.annualEnrollments.value)
        assertEquals(1, MockSaseData.students.value.count { it.id == route.studentId })
        assertEquals(1, MockSaseData.annualEnrollments.value.count { it.studentId == route.studentId })
    }

    @Test
    fun needsDecisionKeepsConfirmedIdentityWhileGroupRemainsPending() {
        val ready = prepareReadyPreApplication(
            folio = "PRE-L7-RE",
            curp = "LSER100101HDFABC02",
            movement = "REINSCRIPCION",
            grade = 2
        )
        assertIs<StudentAddResult.Added>(
            MockSaseData.addStudent(
                Student(
                    id = "MASTER-L7-RE",
                    fullName = ready.alumnoNombreCompleto,
                    group = "1A",
                    enrollmentId = "S310-000001-1",
                    curp = ready.alumnoCurp,
                    preApplicationFolio = ready.folio
                )
            )
        )

        val needsDecision = assertIs<InstitutionalAnnualEnrollmentResult.NeedsDecision>(process(ready))
        val action = assertNotNull(institutionalEnrollmentRecordAction(needsDecision))
        val content = resolvedContent(navigate(action.key.studentId, action.key))

        assertEquals("MASTER-L7-RE", action.key.studentId)
        assertEquals("S310-000001-1", action.key.enrollmentId)
        assertEquals("1A", needsDecision.annualResult.previousGroup)
        assertEquals("2A", needsDecision.annualResult.suggestedGroup)
        assertEquals(InstitutionalRecordDataQuality.CONFIRMED, content.field("Matrícula").quality)
        assertEquals(InstitutionalRecordDataQuality.CONFIRMED, content.field("Ciclo escolar").quality)
        assertEquals(InstitutionalRecordDataQuality.PENDING, content.field("Grupo").quality)
        assertFalse(institutionalEnrollmentMessage(needsDecision).contains("completada", ignoreCase = true))
        assertNoLegacyDefaults(content)
    }

    @Test
    fun blockedAndUnsynchronizedOutcomesStayNonNavigableWithoutSilentMutation() {
        val accepted = prepareAcceptedPreApplication(
            folio = "PRE-L7-BLOCK",
            curp = "LSEB100101HDFABC03",
            movement = "NUEVO INGRESO"
        )
        val studentsBeforeBlock = MockSaseData.students.value.toList()
        val annualsBeforeBlock = MockSaseData.annualEnrollments.value.toList()

        val rejected = assertIs<InstitutionalAnnualEnrollmentResult.GuardRejected>(process(accepted))
        assertEquals(InstitutionalEnrollmentGuardCause.NOT_READY, rejected.cause)
        assertNull(institutionalEnrollmentRecordAction(rejected))
        assertTrue(institutionalEnrollmentMessage(rejected).startsWith("No se inició"))
        assertEquals(studentsBeforeBlock, MockSaseData.students.value)
        assertEquals(annualsBeforeBlock, MockSaseData.annualEnrollments.value)

        PreApplicationViewModel.resetSharedStateForTests()
        val ready = prepareReadyPreApplication(
            folio = "PRE-L7-UNSYNC",
            curp = "LSEU100101HDFABC04",
            movement = "NUEVO INGRESO"
        )
        assertIs<AnnualEnrollmentFlowResult.Completed>(
            AnnualEnrollmentFlowCoordinator.process(flowRequest(ready))
        )
        val studentsBeforeRetry = MockSaseData.students.value.toList()
        val annualsBeforeRetry = MockSaseData.annualEnrollments.value.toList()
        val preApplicationsBeforeRetry = PreApplicationViewModel.sharedPreApplications.value.toList()

        val incomplete = assertIs<InstitutionalAnnualEnrollmentResult.SynchronizationIncomplete>(process(ready))

        assertEquals(PreApplicationSynchronizationCause.PREVIOUSLY_UNSYNCHRONIZED, incomplete.cause)
        assertNull(institutionalEnrollmentRecordAction(incomplete))
        assertTrue(institutionalEnrollmentMessage(incomplete).contains("requiere revisión"))
        assertEquals(studentsBeforeRetry, MockSaseData.students.value)
        assertEquals(annualsBeforeRetry, MockSaseData.annualEnrollments.value)
        assertEquals(preApplicationsBeforeRetry, PreApplicationViewModel.sharedPreApplications.value)
        assertEquals(
            ReadinessStatus.READY,
            PreApplicationViewModel.sharedPreApplications.value.single { it.folio == ready.folio }.readinessStatus
        )
    }

    @Test
    fun studentIdOnlyRouteRejectsHistoricalAmbiguityWithoutSelectingByOrder() {
        val student = Student(
            id = "MASTER-L7-HISTORY",
            fullName = "ALUMNA HISTÓRICA L7",
            group = "",
            enrollmentId = "S310-000888-8",
            curp = "LSEH100101MDFABC05"
        )
        val current = annual(
            student = student,
            schoolYear = "2026-2027",
            folio = "PRE-L7-CURRENT"
        )
        val historical = annual(
            student = student,
            schoolYear = "2025-2026",
            folio = "PRE-L7-HISTORY"
        )

        listOf(listOf(historical, current), listOf(current, historical)).forEach { annuals ->
            val resolution = resolveInstitutionalStudentRecordForRoute(
                studentId = student.id,
                institutionalKey = null,
                students = listOf(student),
                annualEnrollments = annuals,
                preApplications = emptyList()
            )

            assertEquals(
                2,
                assertIs<InstitutionalStudentRecordResolution.AmbiguousAnnualEnrollment>(resolution).matches
            )
        }
    }

    private fun prepareReadyPreApplication(
        folio: String,
        curp: String,
        movement: String,
        grade: Int = 1
    ): PreApplication {
        val accepted = prepareAcceptedPreApplication(folio, curp, movement, grade)
        return assertIs<ReadinessResult.Success>(
            PreApplicationViewModel.markReadyForOfficialEnrollment(accepted.folio)
        ).preApplication
    }

    private fun prepareAcceptedPreApplication(
        folio: String,
        curp: String,
        movement: String,
        grade: Int = 1
    ): PreApplication {
        val submitted = assertIs<FamilySubmissionResult.Success>(
            PreApplicationViewModel.submitFamilyPreApplication(
                draft(folio, curp, movement, grade)
            )
        ).preApplication
        assertIs<UpdatePreApplicationAdministrativeDataResult.Updated>(
            PreApplicationViewModel.updatePreApplicationAdministrativeData(
                UpdatePreApplicationAdministrativeDataRequest(
                    folio = submitted.folio,
                    expected = submitted.administrativeDataSnapshot(),
                    changes = PreApplicationAdministrativeChanges(
                        phone = PreApplicationAdministrativeFieldChange.Replace("5512345678"),
                        address = PreApplicationAdministrativeFieldChange.Replace("DOMICILIO INSTITUCIONAL L7")
                    )
                )
            )
        )
        PreApplicationViewModel.approvePreApplication(submitted.folio)
        PreApplicationViewModel.toggleDocumentCotejado(submitted.folio, "CURP")
        PreApplicationViewModel.simulateCaptureStudentPhoto(submitted.folio)
        PreApplicationViewModel.simulateCaptureResponsablePhoto(submitted.folio)
        return PreApplicationViewModel.sharedPreApplications.value.single { it.folio == submitted.folio }
    }

    private fun draft(folio: String, curp: String, movement: String, grade: Int): PreApplication =
        MockPreApplicationData.preApplications.first().copy(
            folio = folio,
            status = PreApplicationStatus.BORRADOR,
            submittedAt = null,
            tramite = movement,
            cicloEscolar = "2026-2027",
            gradoSolicitado = grade,
            alumnoNombreCompleto = "ALUMNA INSTITUCIONAL $folio",
            alumnoCurp = curp,
            alumnoDomicilio = "DOMICILIO FAMILIAR L7",
            alumnoTelefonoCasa = "5500000000",
            promedioGradoAnterior = 9.0,
            documentosDeclarados = listOf(
                DocumentoDeclarado("CURP", declarado = true, cotejadoSecretaria = false)
            ),
            readinessStatus = ReadinessStatus.PENDING,
            readyAt = null,
            readinessNotes = ""
        )

    private fun process(preApplication: PreApplication): InstitutionalAnnualEnrollmentResult =
        PreApplicationViewModel.processAnnualEnrollmentV2(
            declaredMovement = preApplication.tramite,
            normalizedCurp = preApplication.alumnoCurp,
            folio = preApplication.folio,
            requestedGrade = preApplication.gradoSolicitado,
            previousGroup = null,
            schoolYear = preApplication.cicloEscolar,
            studentFullName = preApplication.alumnoNombreCompleto
        )

    private fun flowRequest(preApplication: PreApplication) = AnnualEnrollmentFlowRequest(
        declaredMovement = preApplication.tramite,
        normalizedCurp = preApplication.alumnoCurp,
        sourcePreApplicationFolio = preApplication.folio,
        requestedGrade = preApplication.gradoSolicitado,
        previousGroup = null,
        schoolYear = preApplication.cicloEscolar,
        newStudentId = "MASTER-V2-${preApplication.folio}",
        studentFullName = preApplication.alumnoNombreCompleto,
        actor = "Secretaría",
        occurredAt = "HOY 12:00"
    )

    private fun navigate(
        studentId: String,
        key: com.example.data.InstitutionalStudentRecordKey
    ): Screen.StudentRecord {
        val viewModel = LabViewModel()
        viewModel.navigateTo(
            Screen.StudentRecord(
                studentId = studentId,
                institutionalKey = key,
                returnTo = Screen.SecretariaPreApplicationDashboard
            )
        )
        return assertIs(viewModel.currentScreen.value)
    }

    private fun resolvedContent(route: Screen.StudentRecord): InstitutionalStudentRecordPresentation.Content {
        val resolution = resolveInstitutionalStudentRecordForRoute(
            studentId = route.studentId,
            institutionalKey = route.institutionalKey,
            students = MockSaseData.students.value,
            annualEnrollments = MockSaseData.annualEnrollments.value,
            preApplications = PreApplicationViewModel.sharedPreApplications.value
        )
        return assertIs(
            institutionalStudentRecordPresentation(
                assertIs<InstitutionalStudentRecordResolution.Resolved>(resolution)
            )
        )
    }

    private fun annual(
        student: Student,
        schoolYear: String,
        folio: String
    ) = AnnualEnrollmentRecord(
        studentId = student.id,
        normalizedCurp = student.curp,
        permanentEnrollmentId = student.enrollmentId,
        schoolYear = schoolYear,
        sourcePreApplicationFolio = folio,
        movement = AnnualEnrollmentMovement.RE_ENROLLMENT,
        requestedGrade = 2,
        groupPlacementRequirement = GroupPlacementRequirement.ContinuityDecisionRequired("1A", "2A"),
        status = AnnualEnrollmentInitialStatus.PENDING_GROUP_CONTINUITY_DECISION
    )

    private fun InstitutionalStudentRecordPresentation.Content.field(label: String) =
        fields.single { it.label == label }

    private fun InstitutionalStudentRecordPresentation.Content.value(label: String) = field(label).value

    private fun assertNoLegacyDefaults(content: InstitutionalStudentRecordPresentation.Content) {
        val rendered = content.toString()
        assertFalse(rendered.contains("2023-2024"))
        assertFalse(rendered.contains("Av. Siempre Viva"))
        assertFalse(rendered.contains("Vigente"))
        assertFalse(rendered.contains("Expediente auditado"))
        assertFalse(rendered.contains("92"))
    }
}
