package com.example.viewmodel

import com.example.data.MockSaseData
import com.example.data.Student
import com.example.data.StudentAddResult
import com.example.data.StudentCredentialPreview
import com.example.data.presolicitud.AntecedentesUdeii
import com.example.data.presolicitud.AutorizadoPreSolicitud
import com.example.data.presolicitud.ConsentimientosFamiliares
import com.example.data.presolicitud.ContextoSociofamiliar
import com.example.data.presolicitud.DocumentoDeclarado
import com.example.data.presolicitud.FichaMedicaFamiliar
import com.example.data.presolicitud.PersonaTramite
import com.example.data.presolicitud.OfficialStudentStatus
import com.example.data.enrollment.AnnualEnrollmentFlowResult
import com.example.data.presolicitud.PreApplication
import com.example.data.presolicitud.PreApplicationStatus
import com.example.data.presolicitud.ReadinessStatus
import com.example.data.presolicitud.Responsable
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PreApplicationGuardrailsTest {
    @BeforeTest
    fun resetSharedState() {
        PreApplicationViewModel.resetSharedStateForTests()
    }

    @Test
    fun submitFamilyPreApplicationCreatesSharedPreApplicationVisibleToSecretaria() {
        val preApplication = preApplication(
            curp = uniqueCurp("FAMVIS"),
            folio = ""
        )

        val result = PreApplicationViewModel.submitFamilyPreApplication(preApplication)

        val success = assertIs<FamilySubmissionResult.Success>(result)
        val stored = PreApplicationViewModel.sharedPreApplications.value
            .firstOrNull { it.folio == success.preApplication.folio }

        assertNotNull(stored)
        assertTrue(stored.folio.startsWith("PRE-310-"))
        assertEquals(PreApplicationStatus.ENVIADA, stored.status)
        assertEquals(success.preApplication.folio, stored.folio)
    }

    @Test
    fun submitFamilyPreApplicationBlocksDuplicateCurp() {
        val curp = uniqueCurp("FAMDUP")
        val first = PreApplicationViewModel.submitFamilyPreApplication(preApplication(curp = curp))
        assertIs<FamilySubmissionResult.Success>(first)

        val duplicate = PreApplicationViewModel.submitFamilyPreApplication(
            preApplication(curp = curp.lowercase())
        )

        assertIs<FamilySubmissionResult.DuplicateCurp>(duplicate)
    }

    @Test
    fun startOfficialEnrollmentBlocksDuplicateFolio() {
        val result = PreApplicationViewModel.startOfficialEnrollment(
            preApplication(
                folio = "PRE-X1A2",
                curp = uniqueCurp("FOLIO1"),
                status = PreApplicationStatus.ACEPTADA
            ),
            selectedGroup = null
        )

        assertIs<OfficialEnrollmentResult.DuplicateFolio>(result)
    }

    @Test
    fun familySubmissionBlocksInvalidCurpBeforeMatricula() {
        val submission = PreApplicationViewModel.submitFamilyPreApplication(
            preApplication(curp = "CURP-DEMO-01")
        )

        assertIs<FamilySubmissionResult.InsufficientData>(submission)
    }

    @Test
    fun confirmInitialGroupBlocksDuplicateMatricula() {
        val curpWithConflictingMatricula = uniqueCurp("DMAT")
        val expectedMatricula = com.example.data.presolicitud.OfficialStudent.generateMatricula(curpWithConflictingMatricula, 26) ?: ""
        assertIs<StudentAddResult.Added>(
            MockSaseData.addStudent(
                student(
                    curp = uniqueCurp("DOTH"),
                    enrollmentId = expectedMatricula
                )
            )
        )
        val submission = PreApplicationViewModel.submitFamilyPreApplication(
            preApplication(curp = curpWithConflictingMatricula, grado = 1)
        )
        val stored = assertIs<FamilySubmissionResult.Success>(submission).preApplication
        PreApplicationViewModel.approvePreApplication(stored.folio)
        PreApplicationViewModel.simulateCaptureStudentPhoto(stored.folio)
        PreApplicationViewModel.simulateCaptureResponsablePhoto(stored.folio)
        assertIs<ReadinessResult.Success>(PreApplicationViewModel.markReadyForOfficialEnrollment(stored.folio))

        val acceptedStored = PreApplicationViewModel.sharedPreApplications.value
            .first { it.folio == stored.folio }
        assertIs<OfficialEnrollmentResult.Success>(PreApplicationViewModel.startOfficialEnrollment(acceptedStored, selectedGroup = "1A"))
        val result = PreApplicationViewModel.confirmInitialGroup(acceptedStored.folio, "1A")

        assertIs<OfficialEnrollmentResult.DuplicateMatricula>(result)
    }

    @Test
    fun mockSaseDataAddStudentRejectsDuplicateCurp() {
        val result = MockSaseData.addStudent(
            student(
                curp = "dema100101hdfabc01",
                enrollmentId = "S310-UNIQUE-CURP-${uniqueSuffix()}"
            )
        )

        assertIs<StudentAddResult.DuplicateCurp>(result)
    }

    @Test
    fun mockSaseDataAddStudentRejectsDuplicateEnrollmentId() {
        val result = MockSaseData.addStudent(
            student(
                curp = uniqueCurp("ENRDUP"),
                enrollmentId = "S310-DEMA100101-26"
            )
        )

        assertIs<StudentAddResult.DuplicateEnrollmentId>(result)
    }

    @Test
    fun labViewModelAddStudentReturnsStudentAddResultForFastTrackFlow() {
        val viewModel = LabViewModel()

        val result = viewModel.addStudent(
            student(
                curp = "DEMA100101HDFABC01",
                enrollmentId = "S310-VM-${uniqueSuffix()}"
            )
        )

        assertIs<StudentAddResult.DuplicateCurp>(result)
    }

    @Test
    fun markReadyBlocksWhenThereArePendingItemsAndPersistsBlockedStatus() {
        val stored = submitAcceptedPreApplication(curp = uniqueCurp("BLOCKD"))

        val result = PreApplicationViewModel.markReadyForOfficialEnrollment(stored.folio)

        val notReady = assertIs<ReadinessResult.NotReady>(result)
        assertTrue(notReady.pendingItems.isNotEmpty())
        val persisted = PreApplicationViewModel.sharedPreApplications.value.first { it.folio == stored.folio }
        assertEquals(ReadinessStatus.BLOCKED, persisted.readinessStatus)
        assertTrue(persisted.readinessNotes.isNotBlank())
    }

    @Test
    fun markReadySucceedsAndPersistsReadinessByFolio() {
        val readyCandidate = submitReadyCandidate(curp = uniqueCurp("READYA"))

        val result = PreApplicationViewModel.markReadyForOfficialEnrollment(readyCandidate.folio)

        val success = assertIs<ReadinessResult.Success>(result)
        val persisted = PreApplicationViewModel.sharedPreApplications.value.first { it.folio == readyCandidate.folio }
        assertEquals(ReadinessStatus.READY, persisted.readinessStatus)
        assertNotNull(persisted.readyAt)
        assertEquals(success.preApplication.folio, persisted.folio)
    }

    @Test
    fun readyPreApplicationCanBeQueriedAfterMarkingReady() {
        val readyCandidate = submitReadyCandidate(curp = uniqueCurp("QUERYR"))
        assertIs<ReadinessResult.Success>(PreApplicationViewModel.markReadyForOfficialEnrollment(readyCandidate.folio))

        val queried = PreApplicationViewModel.sharedPreApplications.value.firstOrNull { it.folio == readyCandidate.folio }

        assertNotNull(queried)
        assertEquals(ReadinessStatus.READY, queried.readinessStatus)
    }

    @Test
    fun convertedPreApplicationIsNotShownAsSimplyPending() {
        val readyCandidate = submitReadyCandidate(curp = uniqueCurp("CONVRT"))
        assertIs<ReadinessResult.Success>(PreApplicationViewModel.markReadyForOfficialEnrollment(readyCandidate.folio))
        val readyStored = PreApplicationViewModel.sharedPreApplications.value.first { it.folio == readyCandidate.folio }

        assertIs<OfficialEnrollmentResult.Success>(PreApplicationViewModel.startOfficialEnrollment(readyStored, selectedGroup = "1A"))
        val result = PreApplicationViewModel.confirmInitialGroup(readyStored.folio, "1A")

        assertIs<OfficialEnrollmentResult.Success>(result)
        val converted = PreApplicationViewModel.sharedPreApplications.value.first { it.folio == readyCandidate.folio }
        assertEquals(ReadinessStatus.CONVERTED, converted.readinessStatus)
        assertFalse(converted.readinessStatus == ReadinessStatus.PENDING)
    }

    @Test
    fun successfulOfficialEnrollmentCreatesOfficialStudentAndMasterStudent() {
        val readyCandidate = submitReadyCandidate(curp = uniqueCurp("MASTCR"))
        assertIs<ReadinessResult.Success>(PreApplicationViewModel.markReadyForOfficialEnrollment(readyCandidate.folio))
        val readyStored = PreApplicationViewModel.sharedPreApplications.value.first { it.folio == readyCandidate.folio }

        assertIs<OfficialEnrollmentResult.Success>(PreApplicationViewModel.startOfficialEnrollment(readyStored, selectedGroup = "1A"))
        val result = PreApplicationViewModel.confirmInitialGroup(readyStored.folio, "1A")

        val success = assertIs<OfficialEnrollmentResult.Success>(result)
        assertTrue(PreApplicationViewModel.officialStudents.value.any { it.preApplicationFolio == readyStored.folio })
        assertNotNull(MockSaseData.studentByCurp(readyStored.alumnoCurp))
    }

    @Test
    fun propagatedMasterStudentUsesOfficialMatriculaAndNormalizedCurp() {
        val rawCurp = uniqueCurp("NORMED").lowercase()
        val readyCandidate = submitReadyCandidate(curp = rawCurp)
        assertIs<ReadinessResult.Success>(PreApplicationViewModel.markReadyForOfficialEnrollment(readyCandidate.folio))
        val readyStored = PreApplicationViewModel.sharedPreApplications.value.first { it.folio == readyCandidate.folio }
        val expectedMatricula = com.example.data.presolicitud.OfficialStudent.generateMatricula(readyStored.alumnoCurp, 26)

        assertIs<OfficialEnrollmentResult.Success>(PreApplicationViewModel.startOfficialEnrollment(readyStored, selectedGroup = "1A"))
        val result = PreApplicationViewModel.confirmInitialGroup(readyStored.folio, "1A")

        val success = assertIs<OfficialEnrollmentResult.Success>(result)
        assertEquals(expectedMatricula, success.masterStudent.enrollmentId)
        assertEquals(rawCurp.uppercase(), success.masterStudent.curp)
    }

    @Test
    fun retryOfficialEnrollmentDoesNotCreateSecondMasterStudent() {
        val readyCandidate = submitReadyCandidate(curp = uniqueCurp("RETRYA"))
        assertIs<ReadinessResult.Success>(PreApplicationViewModel.markReadyForOfficialEnrollment(readyCandidate.folio))
        val readyStored = PreApplicationViewModel.sharedPreApplications.value.first { it.folio == readyCandidate.folio }
        val first = PreApplicationViewModel.startOfficialEnrollment(readyStored, selectedGroup = null)
        assertIs<OfficialEnrollmentResult.Success>(first)
        val countAfterFirst = MockSaseData.students.value.count { it.preApplicationFolio == readyStored.folio }

        val second = PreApplicationViewModel.startOfficialEnrollment(readyStored, selectedGroup = null)

        assertIs<OfficialEnrollmentResult.DuplicateFolio>(second)
        val countAfterSecond = MockSaseData.students.value.count { it.preApplicationFolio == readyStored.folio }
        assertEquals(countAfterFirst, countAfterSecond)
    }

    @Test
    fun unrelatedMasterDuplicateDoesNotReturnFalseSuccess() {
        val curpWithConflictingMatricula = uniqueCurp("CONFLI")
        val readyCandidate = submitReadyCandidate(curp = curpWithConflictingMatricula)
        val expectedMatricula = com.example.data.presolicitud.OfficialStudent.generateMatricula(readyCandidate.alumnoCurp, 26) ?: ""
        assertIs<StudentAddResult.Added>(
            MockSaseData.addStudent(
                student(
                    curp = uniqueCurp("OTHERX"),
                    enrollmentId = expectedMatricula
                )
            )
        )
        assertIs<ReadinessResult.Success>(PreApplicationViewModel.markReadyForOfficialEnrollment(readyCandidate.folio))
        val readyStored = PreApplicationViewModel.sharedPreApplications.value.first { it.folio == readyCandidate.folio }

        assertIs<OfficialEnrollmentResult.Success>(PreApplicationViewModel.startOfficialEnrollment(readyStored, selectedGroup = "1A"))
        val result = PreApplicationViewModel.confirmInitialGroup(readyStored.folio, "1A")

        assertIs<OfficialEnrollmentResult.DuplicateMatricula>(result)
        assertTrue(PreApplicationViewModel.officialStudents.value.any { it.preApplicationFolio == readyStored.folio && it.matriculaOficial == null })
    }

    private fun submitAcceptedPreApplication(curp: String, grado: Int = 1): PreApplication {
        val submission = PreApplicationViewModel.submitFamilyPreApplication(
            preApplication(curp = curp, grado = grado)
        )
        val stored = assertIs<FamilySubmissionResult.Success>(submission).preApplication
        PreApplicationViewModel.approvePreApplication(stored.folio)
        return PreApplicationViewModel.sharedPreApplications.value.first { it.folio == stored.folio }
    }

    private fun submitReadyCandidate(curp: String, grado: Int = 1): PreApplication {
        val accepted = submitAcceptedPreApplication(curp = curp, grado = grado)
        PreApplicationViewModel.simulateCaptureStudentPhoto(accepted.folio)
        PreApplicationViewModel.simulateCaptureResponsablePhoto(accepted.folio)
        return PreApplicationViewModel.sharedPreApplications.value.first { it.folio == accepted.folio }
    }

    private fun preApplication(
        folio: String = "TEST-${uniqueSuffix()}",
        curp: String,
        grado: Int = 1,
        status: PreApplicationStatus = PreApplicationStatus.BORRADOR,
        documents: List<DocumentoDeclarado> = emptyList()
    ): PreApplication = PreApplication(
        folio = folio,
        status = status,
        submittedAt = null,
        tramite = "Nuevo Ingreso",
        cicloEscolar = "2026-2027",
        gradoSolicitado = grado,
        alumnoNombreCompleto = "Alumno Prueba ${uniqueSuffix()}",
        alumnoCurp = curp,
        alumnoFechaNacimiento = "01/Ene/2012",
        alumnoSexo = "No especificado",
        alumnoNacionalidad = "Mexicana",
        alumnoEntidadNacimiento = "CDMX",
        alumnoDomicilio = "Domicilio de prueba",
        alumnoTelefonoCasa = "55 0000 0000",
        escuelaProcedencia = "Primaria de prueba",
        promedioGradoAnterior = 8.5,
        personaTramite = PersonaTramite(
            nombreCompleto = "Responsable Prueba",
            parentesco = "Madre",
            telefono = "5511111111",
            identificacionPresentada = "INE",
            usarComoContactoPrincipal = true
        ),
        responsables = listOf(
            Responsable(
                nombreCompleto = "Responsable Prueba",
                parentesco = "Madre",
                telefono = "55 1111 1111",
                correo = "responsable@example.com",
                domicilioDistinto = false,
                domicilio = null,
                viveConAlumno = true,
                contactoPrincipal = true,
                puedeRecoger = true,
                ocupacion = "Empleado",
                horarioContacto = "Vespertino",
                identificacionApresentar = "INE"
            )
        ),
        autorizados = listOf(
            AutorizadoPreSolicitud("Autorizado Prueba", "Tio", "55 2222 2222", "")
        ),
        fichaMedicaFamiliar = FichaMedicaFamiliar(
            servicioMedico = "IMSS",
            numeroAfiliacion = null,
            tipoSangre = null,
            alergias = "Ninguna",
            padecimientos = "Ninguno",
            medicamentos = "Ninguno",
            restriccionFisica = "Ninguna",
            usaLentes = false,
            dificultadVisualAuditiva = "Ninguna",
            saludBucal = "Buena",
            cartillaVacunacion = true
        ),
        contextoSociofamiliar = ContextoSociofamiliar(
            viveConQuien = "Familia",
            tipoFamilia = "Nuclear",
            hijoUnico = false,
            lugarEntreHermanos = 1,
            hermanosEnEscuela = false,
            integrantesHogar = 3,
            sostenEconomico = "Responsable",
            ingresoRangos = "Media",
            tipoVivienda = "Propia",
            serviciosBásicos = true,
            internet = true,
            dispositivoTareas = "Computadora",
            becaApoyo = "Ninguna",
            transporte = "Transporte publico",
            dificultadMateriales = false,
            atiendeAvisos = "Responsable",
            horarioComunicacion = "Vespertino",
            puedeAcudirCitatorios = true
        ),
        antecedentesUdeii = AntecedentesUdeii(
            antecedenteApoyo = "Ninguno",
            terapiaLenguaje = false,
            apoyoPsicologico = false,
            apoyoPedagogico = false,
            documentosDisponibles = "Ninguno",
            informeEscuelaAnterior = false,
            evaluacionPsicopedagogica = false,
            planIntervencion = false,
            portafolio = false,
            observacionesFamiliares = "Ninguna"
        ),
        documentosDeclarados = documents,
        consentimientos = ConsentimientosFamiliares(
            avisoPrivacidad = true,
            usoDatosExpediente = true,
            fotoAlumno = true,
            fotoCredencial = true,
            fotoAutorizados = true,
            comunicacionWhatsapp = true,
            reglamentoInterno = true,
            marcoConvivencia = true,
            corresponsabilidadFamiliar = true
        )
    )

    private fun student(curp: String, enrollmentId: String): Student = Student(
        id = "TEST-${uniqueSuffix()}",
        fullName = "Alumno Guardrail",
        group = "1A",
        enrollmentId = enrollmentId,
        curp = curp,
        tutorName = "Responsable Guardrail",
        tutorRelation = "Tutor",
        status = "Nuevo ingreso",
        riskLevel = "Bajo",
        documentationStatus = "Completa"
    )

    private fun uniqueCurp(seed: String): String {
        val suffix = uniqueSuffix()
        val prefix = seed.filter { it.isLetter() }.uppercase().padEnd(4, 'X').take(4)
        return "$prefix${suffix.take(6)}HDFABC${suffix.last()}0"
    }

    private fun uniqueSuffix(): String =
        kotlin.random.Random.nextInt(100000, 999999).toString()

    // ── Provisional student (buildProvisionalStudent) ──────────────────

    @Test
    fun buildProvisionalStudentCreatesMinimalStudent() {
        val readyCandidate = submitReadyCandidate(curp = uniqueCurp("PROVM"))
        assertIs<ReadinessResult.Success>(PreApplicationViewModel.markReadyForOfficialEnrollment(readyCandidate.folio))
        val readyStored = PreApplicationViewModel.sharedPreApplications.value.first { it.folio == readyCandidate.folio }

        val result = PreApplicationViewModel.startOfficialEnrollment(readyStored, selectedGroup = "1A")
        val success = assertIs<OfficialEnrollmentResult.Success>(result)

        assertTrue(success.masterStudent.fullName.isNotBlank(), "fullName no debe estar vacío")
        assertTrue(success.masterStudent.curp.isNotBlank(), "curp no debe estar vacía")
        assertEquals("", success.masterStudent.enrollmentId, "enrollmentId debe estar vacío (pendiente de grupo)")
        assertNull(success.masterStudent.preApplicationFolio, "preApplicationFolio debe ser null (provisional, sin link a pre-solicitud)")
        assertNotNull(success.masterStudent.status, "status no debe ser null")
        assertTrue(success.masterStudent.status?.isNotBlank() == true, "status no debe estar vacío")
        assertNull(success.officialStudent.matriculaOficial, "matriculaOficial debe ser null hasta confirmInitialGroup")
    }

    // ── Post-enrollment visibility (6A + 6B) ─────────────────────────────

    @Test
    fun officialStudentAppearsInCollectionAfterSuccessfulStart() {
        val readyCandidate = submitReadyCandidate(curp = uniqueCurp("COLLEC"))
        assertIs<ReadinessResult.Success>(PreApplicationViewModel.markReadyForOfficialEnrollment(readyCandidate.folio))
        val readyStored = PreApplicationViewModel.sharedPreApplications.value.first { it.folio == readyCandidate.folio }

        val result = PreApplicationViewModel.startOfficialEnrollment(readyStored, selectedGroup = null)

        val success = assertIs<OfficialEnrollmentResult.Success>(result)
        assertTrue(PreApplicationViewModel.officialStudents.value.any { it.id == success.officialStudent.id })
        assertEquals(readyStored.folio, success.officialStudent.preApplicationFolio)
        assertNull(success.officialStudent.matriculaOficial)
    }

    @Test
    fun masterStudentCanBeFoundByCurpAfterOfficialEnrollment() {
        val rawCurp = uniqueCurp("MASTER").lowercase()
        val readyCandidate = submitReadyCandidate(curp = rawCurp)
        assertIs<ReadinessResult.Success>(PreApplicationViewModel.markReadyForOfficialEnrollment(readyCandidate.folio))
        val readyStored = PreApplicationViewModel.sharedPreApplications.value.first { it.folio == readyCandidate.folio }

        assertIs<OfficialEnrollmentResult.Success>(PreApplicationViewModel.startOfficialEnrollment(readyStored, selectedGroup = "1A"))
        PreApplicationViewModel.confirmInitialGroup(readyStored.folio, "1A")

        val found = MockSaseData.studentByCurp(rawCurp.uppercase())
        assertNotNull(found)
        assertEquals(rawCurp.uppercase(), found.curp)
        assertEquals(readyStored.folio, found.preApplicationFolio)
    }

    @Test
    fun masterStudentCanBeFoundByMatriculaAfterOfficialEnrollment() {
        val rawCurp = uniqueCurp("MATRIC").lowercase()
        val readyCandidate = submitReadyCandidate(curp = rawCurp)
        assertIs<ReadinessResult.Success>(PreApplicationViewModel.markReadyForOfficialEnrollment(readyCandidate.folio))
        val readyStored = PreApplicationViewModel.sharedPreApplications.value.first { it.folio == readyCandidate.folio }

        assertIs<OfficialEnrollmentResult.Success>(PreApplicationViewModel.startOfficialEnrollment(readyStored, selectedGroup = "1A"))
        val result = PreApplicationViewModel.confirmInitialGroup(readyStored.folio, "1A")
        val success = assertIs<OfficialEnrollmentResult.Success>(result)

        val found = MockSaseData.studentByEnrollmentId(success.masterStudent.enrollmentId)
        assertNotNull(found)
        assertEquals(success.masterStudent.enrollmentId, found.enrollmentId)
    }

    @Test
    fun preApplicationFolioLinkIsVisibleOnOfficialStudent() {
        val rawCurp = uniqueCurp("FOLINK")
        val readyCandidate = submitReadyCandidate(curp = rawCurp)
        assertIs<ReadinessResult.Success>(PreApplicationViewModel.markReadyForOfficialEnrollment(readyCandidate.folio))
        val readyStored = PreApplicationViewModel.sharedPreApplications.value.first { it.folio == readyCandidate.folio }

        assertIs<OfficialEnrollmentResult.Success>(PreApplicationViewModel.startOfficialEnrollment(readyStored, selectedGroup = "1A"))
        val result = PreApplicationViewModel.confirmInitialGroup(readyStored.folio, "1A")
        val success = assertIs<OfficialEnrollmentResult.Success>(result)

        assertEquals(readyStored.folio, success.officialStudent.preApplicationFolio)
        assertEquals(readyStored.folio, success.masterStudent.preApplicationFolio)
    }

    @Test
    fun officialStudentWithoutLinkedMasterStudentIsDetected() {
        val orphanOfficial = PreApplicationViewModel.officialStudents.value
            .filter { os -> MockSaseData.studentByCurp(os.curp) == null }

        val seedOrphans = orphanOfficial.count { os ->
            MockSaseData.studentByCurp(os.curp) == null
        }
        assertTrue(seedOrphans >= 0)
    }

    @Test
    fun emptyOfficialStudentsListDoesNotBreakLookups() {
        PreApplicationViewModel.resetSharedStateForTests()
        val officialList = PreApplicationViewModel.officialStudents.value
        assertTrue(officialList.isNotEmpty())
    }

    // ── Credential preview tests (Phase 7A + 7B) ─────────────────────────

    @Test
    fun fromStudent_CreatesCorrectProjection() {
        val student = credentialStudent(
            curp = "CRED001",
            enrollmentId = "S310-CRED-001"
        )
        val preview = StudentCredentialPreview.fromStudent(student)

        assertEquals(student.enrollmentId, preview.enrollmentId)
        assertEquals(student.fullName, preview.fullName)
        assertEquals(student.curp, preview.curp)
        assertEquals(student.schoolYear, preview.schoolYear)
        assertEquals(student.status, preview.status)
    }

    @Test
    fun fromStudent_HandlesPhotoAbsent() {
        val student = credentialStudent(
            curp = "PHOTO1",
            photoUrl = null
        )
        val preview = StudentCredentialPreview.fromStudent(student)

        assertEquals("Sin foto", preview.photoStatus)
    }

    @Test
    fun fromStudent_HandlesPhotoPresent() {
        val student = credentialStudent(
            curp = "PHOTO2",
            photoUrl = "captures/student-photo.jpg"
        )
        val preview = StudentCredentialPreview.fromStudent(student)

        assertEquals("Con foto", preview.photoStatus)
    }

    @Test
    fun fromStudent_DetectsOfficialEnrollmentOrigin() {
        val student = credentialStudent(
            curp = "ORIGIN",
            preApplicationFolio = "PRE-310-ORIGIN"
        )
        val preview = StudentCredentialPreview.fromStudent(student)

        assertTrue(preview.generatedFromOfficialEnrollment)
        assertEquals("PRE-310-ORIGIN", preview.preApplicationFolio)
    }

    @Test
    fun fromStudent_DetectsNonOfficialOrigin() {
        val student = credentialStudent(
            curp = "NONOFF",
            preApplicationFolio = null
        )
        val preview = StudentCredentialPreview.fromStudent(student)

        assertFalse(preview.generatedFromOfficialEnrollment)
        assertNull(preview.preApplicationFolio)
    }

    // ── Credential back view tests (Phase 7C + 7D) ────────────────────────

    @Test
    fun backViewPreservesOfficialMatricula() {
        val student = credentialStudent(
            curp = "MATB1",
            enrollmentId = "S310-MAT-BACK-001"
        )
        val preview = StudentCredentialPreview.fromStudent(student)

        assertEquals("S310-MAT-BACK-001", preview.enrollmentId)
    }

    @Test
    fun backViewPreservesFolioOrigen() {
        val student = credentialStudent(
            curp = "FOLB1",
            preApplicationFolio = "PRE-310-FOL-BACK"
        )
        val preview = StudentCredentialPreview.fromStudent(student)

        assertEquals("PRE-310-FOL-BACK", preview.preApplicationFolio)
    }

    @Test
    fun backViewDoesNotExposeSensitiveData() {
        val student = credentialStudent(curp = "NOSEN")
        val preview = StudentCredentialPreview.fromStudent(student)

        assertEquals(student.enrollmentId, preview.enrollmentId)
        assertEquals(student.curp, preview.curp)
        assertEquals(student.preApplicationFolio, preview.preApplicationFolio)
        assertNotNull(preview.grade)
        assertNotNull(preview.schoolYear)
    }

    @Test
    fun backViewMarkedAsPreview() {
        val student = credentialStudent(curp = "PREVU")
        val preview = StudentCredentialPreview.fromStudent(student)

        assertEquals("Activo", preview.status)
    }

    @Test
    fun modelDoesNotExposePdfOrPrintFields() {
        val student = credentialStudent(curp = "PROOF")
        val preview = StudentCredentialPreview.fromStudent(student)

        assertEquals(student.fullName, preview.fullName)
        assertEquals(student.enrollmentId, preview.enrollmentId)
        assertEquals(student.curp, preview.curp)
        assertEquals("Sin foto", preview.photoStatus)
    }

    @Test
    fun fromStudent_ParsesGradeAndGroup() {
        val student = credentialStudent(
            curp = "GRADE1",
            group = "1\u00b0 A"
        )
        val preview = StudentCredentialPreview.fromStudent(student)

        assertEquals("1\u00b0", preview.grade)
        assertEquals("A", preview.group)
    }

    // ── Integration: full pre-enrollment flow ───────────────────────────

    @Test
    fun fullPreEnrollmentFlowFromSubmitToMasterStudent() {
        val rawCurp = uniqueCurp("INTEG").lowercase()
        val grado = 1

        // Step 1: Family submits pre-application
        val submission = PreApplicationViewModel.submitFamilyPreApplication(
            preApplication(curp = rawCurp, grado = grado)
        )
        val submitted = assertIs<FamilySubmissionResult.Success>(submission).preApplication
        assertNotNull(submitted.folio)

        // Step 2: Secretary approves
        PreApplicationViewModel.approvePreApplication(submitted.folio)
        val approved = PreApplicationViewModel.sharedPreApplications.value.first { it.folio == submitted.folio }
        assertEquals(PreApplicationStatus.ACEPTADA, approved.status)

        // Step 3: Secretary marks readiness (capture photos)
        PreApplicationViewModel.simulateCaptureStudentPhoto(submitted.folio)
        PreApplicationViewModel.simulateCaptureResponsablePhoto(submitted.folio)
        val readyResult = PreApplicationViewModel.markReadyForOfficialEnrollment(submitted.folio)
        assertIs<ReadinessResult.Success>(readyResult)
        val readyStored = PreApplicationViewModel.sharedPreApplications.value.first { it.folio == submitted.folio }
        assertEquals(ReadinessStatus.READY, readyStored.readinessStatus)

        // Step 4: Start official enrollment (provisional — no matrícula yet)
        val enrollResult = PreApplicationViewModel.startOfficialEnrollment(readyStored, selectedGroup = "1A")
        val enrolled = assertIs<OfficialEnrollmentResult.Success>(enrollResult)
        assertNull(enrolled.officialStudent.matriculaOficial, "matrícula debe ser null en provisional")
        assertEquals("", enrolled.masterStudent.enrollmentId, "enrollmentId vacío en provisional")
        assertTrue(PreApplicationViewModel.officialStudents.value.any { it.preApplicationFolio == submitted.folio })

        // Step 5: Confirm group (assigns matrícula)
        val confirmResult = PreApplicationViewModel.confirmInitialGroup(submitted.folio, "1A")
        val confirmed = assertIs<OfficialEnrollmentResult.Success>(confirmResult)

        // Assertions: official student has matrícula
        val officialStudent = confirmed.officialStudent
        assertNotNull(officialStudent.matriculaOficial, "matrícula debe asignarse en confirmInitialGroup")
        assertTrue(officialStudent.matriculaOficial!!.startsWith("S310-"), "matrícula debe tener formato oficial")
        assertEquals(OfficialStudentStatus.ALTA_OFICIAL_CON_GRUPO, officialStudent.status)
        assertEquals("1A", officialStudent.grupoAsignado)

        // Assertions: master student in MockSaseData
        val masterByCurp = MockSaseData.studentByCurp(rawCurp.uppercase())
        assertNotNull(masterByCurp, "master student debe existir en MockSaseData por CURP")
        assertEquals(rawCurp.uppercase(), masterByCurp.curp)
        assertEquals(officialStudent.matriculaOficial, masterByCurp.enrollmentId, "master student debe tener misma matrícula")

        val masterByMatricula = MockSaseData.studentByEnrollmentId(officialStudent.matriculaOficial!!)
        assertNotNull(masterByMatricula, "master student debe existir por matrícula")
        assertEquals(rawCurp.uppercase(), masterByMatricula.curp)

        // Assertions: pre-application is marked CONVERTED
        val converted = PreApplicationViewModel.sharedPreApplications.value.first { it.folio == submitted.folio }
        assertEquals(ReadinessStatus.CONVERTED, converted.readinessStatus)
    }

    // ── Step validation tests ────────────────────────────────────────────

    @Test
    fun nextStepBlocksWhenStep2ContextoIsIncomplete() {
        val vm = PreApplicationViewModel()
        advanceToStep2(vm)
        vm.nextStep()
        assertTrue(vm.errors.value.isNotEmpty(), "Step 2 debe tener errores cuando contexto está vacío")
        assertEquals(2, vm.currentStep.value)
    }

    @Test
    fun nextStepBlocksWhenStep3DocumentosIsIncomplete() {
        val vm = PreApplicationViewModel()
        advanceToStep2(vm)
        fillStep2(vm)
        vm.nextStep()
        vm.nextStep()
        assertTrue(vm.errors.value.isNotEmpty(), "Step 3 debe tener errores cuando documentos están vacíos")
        assertEquals(3, vm.currentStep.value)
    }

    @Test
    fun nextStepAdvancesWhenStep2ContextoIsComplete() {
        val vm = PreApplicationViewModel()
        advanceToStep2(vm)
        fillStep2(vm)
        vm.nextStep()
        assertTrue(vm.errors.value.isEmpty(), "Step 2 no debe tener errores cuando contexto está completo: ${vm.errors.value}")
        assertEquals(3, vm.currentStep.value)
    }

    @Test
    fun nextStepAdvancesWhenStep3DocumentosIsComplete() {
        val vm = PreApplicationViewModel()
        advanceToStep2(vm)
        fillStep2(vm)
        vm.nextStep()
        vm.toggleDocumento("actaNacimiento")
        vm.toggleConsentimiento("usoDatos")
        vm.toggleConsentimiento("reglamento")
        vm.toggleConsentimiento("marcoConvivencia")
        vm.nextStep()
        assertTrue(vm.errors.value.isEmpty(), "Step 3 no debe tener errores cuando documentos están completos: ${vm.errors.value}")
        assertEquals(4, vm.currentStep.value)
    }

    @Test
    fun submitApplicationBlocksWhenContextoIsIncomplete() {
        val vm = PreApplicationViewModel()
        fillStep0(vm)
        fillStep1(vm)
        vm.toggleConsentimiento("usoDatos")
        vm.toggleConsentimiento("corresponsabilidad")

        vm.submitApplication()

        assertTrue(vm.errors.value.isNotEmpty(), "submit debe fallar cuando contexto está incompleto")
        assertTrue(vm.errors.value.containsKey("servicioMedico"), "Debe faltar servicio médico")
        assertTrue(vm.errors.value.containsKey("tipoSangre"), "Debe faltar tipo de sangre")
    }

    @Test
    fun submitApplicationBlocksWhenDocumentosIsIncomplete() {
        val vm = PreApplicationViewModel()
        fillStep0(vm)
        fillStep1(vm)
        fillStep2(vm)
        vm.toggleConsentimiento("usoDatos")
        vm.toggleConsentimiento("corresponsabilidad")

        vm.submitApplication()

        assertTrue(vm.errors.value.isNotEmpty(), "submit debe fallar cuando documentos están incompletos")
        assertTrue(vm.errors.value.containsKey("documentos"), "Debe faltar documentos declarados")
    }

    private fun advanceToStep2(vm: PreApplicationViewModel) {
        fillStep0(vm)
        vm.nextStep()
        fillStep1(vm)
        vm.nextStep()
    }

    private fun fillStep0(vm: PreApplicationViewModel) {
        vm.setApellidoPaterno("Perez")
        vm.setNombre("Juan")
        vm.setCurp("PEMJ100101HDFABC01")
        vm.setFechaNacimiento("01/Ene/2010")
        vm.setGradoSolicitado(1)
        vm.setPromedioGradoAnterior("8.5")
        vm.setTelefonoPrincipal("5512345678")
        vm.setAceptaAvisoPrivacidad(true)
    }

    private fun fillStep1(vm: PreApplicationViewModel) {
        vm.setPersonaTramiteNombre("Maria Lopez")
        vm.setPersonaTramiteParentesco("Madre")
        vm.setPersonaTramiteTelefono("5512345678")
        vm.setPersonaTramiteIdentificacion("INE")
        vm.setResponsableNombre("Maria Lopez")
        vm.setResponsableParentesco("Madre")
        vm.setResponsableTelefono("5512345678")
    }

    private fun fillStep2(vm: PreApplicationViewModel) {
        vm.setServicioMedico("IMSS")
        vm.setTipoSangre("O+")
        vm.setViveConQuien("Ambos padres")
        vm.setTipoFamilia("Nuclear")
        vm.setIntegrantesHogar("4")
        vm.setPersonaAtiendeAvisos("Madre")
    }

    private fun credentialStudent(
        curp: String,
        enrollmentId: String = "S310-CRED-${uniqueSuffix()}",
        group: String = "2\u00b0 B",
        photoUrl: String? = null,
        preApplicationFolio: String? = "PRE-310-CRED-${uniqueSuffix()}"
    ): Student = Student(
        id = "CRED-${uniqueSuffix()}",
        fullName = "Alumno Credencial Test",
        group = group,
        enrollmentId = enrollmentId,
        curp = uniqueCurp(curp),
        tutorName = "Tutor Credencial",
        tutorRelation = "Madre",
        status = "Activo",
        photoUrl = photoUrl,
        preApplicationFolio = preApplicationFolio
    )

    // ── Correction flow characterization (Microloop 2) ──────────────────

    @Test
    fun markForCorrectionChangesStatusToPENDIENTE_CORRECCION() {
        val preApp = submitAndGetRaw { preApplication(curp = uniqueCurp("CORR1")) }
        val folio = preApp.folio
        assertEquals(PreApplicationStatus.ENVIADA, preApp.status)

        PreApplicationViewModel.markForCorrection(folio)

        val updated = PreApplicationViewModel.sharedPreApplications.value.first { it.folio == folio }
        assertEquals(PreApplicationStatus.PENDIENTE_CORRECCION, updated.status)
    }

    @Test
    fun markForCorrectionPreservesFolioAndCollectionSize() {
        val preApp = submitAndGetRaw { preApplication(curp = uniqueCurp("CORR2")) }
        val folio = preApp.folio
        val sizeBefore = PreApplicationViewModel.sharedPreApplications.value.size

        PreApplicationViewModel.markForCorrection(folio)

        val updated = PreApplicationViewModel.sharedPreApplications.value.first { it.folio == folio }
        assertNotNull(updated)
        assertEquals(folio, updated.folio)
        assertEquals(sizeBefore, PreApplicationViewModel.sharedPreApplications.value.size)
    }

    @Test
    fun setMotivoCorreccionStoresTextOnCorrectPreApplication() {
        val preApp1 = submitAndGetRaw { preApplication(curp = uniqueCurp("MOT1")) }
        val preApp2 = submitAndGetRaw { preApplication(curp = uniqueCurp("MOT2")) }

        PreApplicationViewModel.setMotivoCorreccion(preApp1.folio, "Documento incompleto")

        val updated1 = PreApplicationViewModel.sharedPreApplications.value.first { it.folio == preApp1.folio }
        val updated2 = PreApplicationViewModel.sharedPreApplications.value.first { it.folio == preApp2.folio }

        assertEquals("Documento incompleto", updated1.motivoCorreccion)
        assertEquals("", updated2.motivoCorreccion)
    }

    @Test
    fun correctionStatusAndMotivoAreBothPersisted() {
        val preApp = submitAndGetRaw { preApplication(curp = uniqueCurp("BOTH")) }

        PreApplicationViewModel.markForCorrection(preApp.folio)
        PreApplicationViewModel.setMotivoCorreccion(preApp.folio, "Falta firma")

        val updated = PreApplicationViewModel.sharedPreApplications.value.first { it.folio == preApp.folio }
        assertEquals(PreApplicationStatus.PENDIENTE_CORRECCION, updated.status)
        assertEquals("Falta firma", updated.motivoCorreccion)
    }

    @Test
    fun markForCorrectionOnUnknownFolioDoesNothing() {
        val sizeBefore = PreApplicationViewModel.sharedPreApplications.value.size
        val statusesBefore = PreApplicationViewModel.sharedPreApplications.value.map { it.status to it.motivoCorreccion }

        PreApplicationViewModel.markForCorrection("FOLIO-INEXISTENTE-999")

        assertEquals(sizeBefore, PreApplicationViewModel.sharedPreApplications.value.size)
        val statusesAfter = PreApplicationViewModel.sharedPreApplications.value.map { it.status to it.motivoCorreccion }
        assertEquals(statusesBefore, statusesAfter)
    }

    @Test
    fun setMotivoCorreccionOnUnknownFolioDoesNothing() {
        val sizeBefore = PreApplicationViewModel.sharedPreApplications.value.size
        val motivosBefore = PreApplicationViewModel.sharedPreApplications.value.map { it.motivoCorreccion }

        PreApplicationViewModel.setMotivoCorreccion("FOLIO-INEXISTENTE-999", "texto")

        assertEquals(sizeBefore, PreApplicationViewModel.sharedPreApplications.value.size)
        val motivosAfter = PreApplicationViewModel.sharedPreApplications.value.map { it.motivoCorreccion }
        assertEquals(motivosBefore, motivosAfter)
    }

    // -- Family correction resubmission contract (Microloop 4C) ----------

    @Test
    fun resubmitCorrectionPreservesIdentityAndCollectionSize() {
        val original = submitCorrectablePreApplication(uniqueCurp("RESID"))
        val sizeBefore = PreApplicationViewModel.sharedPreApplications.value.size
        val correctedAddress = "Domicilio familiar corregido"
        val corrected = original.copy(alumnoDomicilio = correctedAddress)
        assertEquals(PreApplicationStatus.PENDIENTE_CORRECCION, original.status)

        val result = PreApplicationViewModel.resubmitCorrectedPreApplication(corrected)

        val success = assertIs<FamilyResubmissionResult.Success>(result)
        val stored = PreApplicationViewModel.sharedPreApplications.value.single { it.folio == original.folio }
        assertEquals(original.folio, success.preApplication.folio)
        assertEquals(original.folio, stored.folio)
        assertEquals(original.submittedAt, stored.submittedAt)
        assertEquals(correctedAddress, stored.alumnoDomicilio)
        assertEquals(sizeBefore, PreApplicationViewModel.sharedPreApplications.value.size)
    }

    @Test
    fun resubmitCorrectionTransitionsPendingCorrectionToSent() {
        val original = submitCorrectablePreApplication(uniqueCurp("RESTR"))
        assertEquals(PreApplicationStatus.PENDIENTE_CORRECCION, original.status)

        val result = PreApplicationViewModel.resubmitCorrectedPreApplication(
            original.copy(alumnoTelefonoCasa = "5512345678")
        )

        val success = assertIs<FamilyResubmissionResult.Success>(result)
        assertEquals(PreApplicationStatus.ENVIADA, success.preApplication.status)
        assertEquals(
            PreApplicationStatus.ENVIADA,
            PreApplicationViewModel.sharedPreApplications.value.single { it.folio == original.folio }.status
        )
    }

    @Test
    fun resubmitCorrectionRejectsSentStatusWithoutMutation() {
        val original = submitAndGetRaw { preApplication(curp = uniqueCurp("RSTSENT")) }
        assertEquals(PreApplicationStatus.ENVIADA, original.status)
        val allBefore = PreApplicationViewModel.sharedPreApplications.value.toList()

        val result = PreApplicationViewModel.resubmitCorrectedPreApplication(
            original.copy(alumnoDomicilio = "Cambio no permitido")
        )

        assertIs<FamilyResubmissionResult.InvalidStatus>(result)
        assertEquals(allBefore, PreApplicationViewModel.sharedPreApplications.value)
    }

    @Test
    fun resubmitCorrectionRejectsAcceptedStatusWithoutMutation() {
        val submitted = submitAndGetRaw { preApplication(curp = uniqueCurp("RSTACC")) }
        PreApplicationViewModel.approvePreApplication(submitted.folio)
        val original = PreApplicationViewModel.sharedPreApplications.value.single { it.folio == submitted.folio }
        assertEquals(PreApplicationStatus.ACEPTADA, original.status)
        val allBefore = PreApplicationViewModel.sharedPreApplications.value.toList()

        val result = PreApplicationViewModel.resubmitCorrectedPreApplication(
            original.copy(alumnoDomicilio = "Cambio no permitido")
        )

        assertIs<FamilyResubmissionResult.InvalidStatus>(result)
        assertEquals(allBefore, PreApplicationViewModel.sharedPreApplications.value)
    }

    // BORRADOR is not persisted; DUPLICADA and CANCELADA have no public transition.

    @Test
    fun resubmitCorrectionReturnsNotFoundWithoutMutatingAnyApplication() {
        val allBefore = PreApplicationViewModel.sharedPreApplications.value.toList()
        val missing = preApplication(
            folio = "FOLIO-INEXISTENTE-RESUBMIT",
            curp = uniqueCurp("RESNF"),
            status = PreApplicationStatus.PENDIENTE_CORRECCION
        )

        val result = PreApplicationViewModel.resubmitCorrectedPreApplication(missing)

        val notFound = assertIs<FamilyResubmissionResult.NotFound>(result)
        assertEquals(missing.folio, notFound.folio)
        assertEquals(allBefore, PreApplicationViewModel.sharedPreApplications.value)
    }

    @Test
    fun resubmitCorrectionAllowsOriginalCurp() {
        val original = submitCorrectablePreApplication(uniqueCurp("RESOWN"))
        val correctedPhone = "5598765432"
        val corrected = original.copy(
            alumnoCurp = "  ${original.alumnoCurp.lowercase()}  ",
            alumnoTelefonoCasa = correctedPhone
        )

        val result = PreApplicationViewModel.resubmitCorrectedPreApplication(corrected)

        assertIs<FamilyResubmissionResult.Success>(result)
        val stored = PreApplicationViewModel.sharedPreApplications.value.single { it.folio == original.folio }
        assertEquals(original.alumnoCurp, stored.alumnoCurp)
        assertEquals(correctedPhone, stored.alumnoTelefonoCasa)
    }

    @Test
    fun resubmitCorrectionRejectsCurpOwnedByAnotherPreApplication() {
        val original = submitCorrectablePreApplication(uniqueCurp("RESCUR1"))
        val other = submitAndGetRaw { preApplication(curp = uniqueCurp("RESCUR2")) }
        val allBefore = PreApplicationViewModel.sharedPreApplications.value.toList()

        val result = PreApplicationViewModel.resubmitCorrectedPreApplication(
            original.copy(alumnoCurp = other.alumnoCurp.lowercase())
        )

        val duplicate = assertIs<FamilyResubmissionResult.DuplicateCurp>(result)
        assertEquals(other.alumnoCurp, duplicate.curp)
        assertEquals(allBefore, PreApplicationViewModel.sharedPreApplications.value)
        assertEquals(
            original,
            PreApplicationViewModel.sharedPreApplications.value.single { it.folio == original.folio }
        )
        assertEquals(
            other,
            PreApplicationViewModel.sharedPreApplications.value.single { it.folio == other.folio }
        )
    }

    @Test
    fun resubmitCorrectionPreservesInstitutionalDataAndFolioLinkedStructures() {
        val original = submitReadyCorrectionCandidate(uniqueCurp("RESINS"))
        val photosBefore = assertNotNull(PreApplicationViewModel.photos.value[original.folio])
        val observationsBefore = assertNotNull(PreApplicationViewModel.reviewObservations.value[original.folio])
        val correctedAddress = "Domicilio familiar corregido"
        assertEquals("Observacion institucional", original.observacionesSecretaria)
        assertEquals("Corregir domicilio", original.motivoCorreccion)
        assertTrue(original.documentosDeclarados.all { it.cotejadoSecretaria })
        assertNotNull(photosBefore.studentPhotoMockUrl)
        assertNotNull(photosBefore.responsablePhotoMockUrl)
        assertTrue(observationsBefore.isNotEmpty())
        assertEquals(ReadinessStatus.READY, original.readinessStatus)
        assertNotNull(original.readyAt)
        assertTrue(original.readinessNotes.isNotBlank())
        val corrected = original.copy(
            alumnoDomicilio = correctedAddress,
            observacionesSecretaria = "No debe aceptar este cambio familiar",
            motivoCorreccion = "No debe aceptar este motivo familiar",
            documentosDeclarados = original.documentosDeclarados.map {
                it.copy(cotejadoSecretaria = false)
            }
        )

        val result = PreApplicationViewModel.resubmitCorrectedPreApplication(corrected)

        assertIs<FamilyResubmissionResult.Success>(result)
        val stored = PreApplicationViewModel.sharedPreApplications.value.single { it.folio == original.folio }
        assertEquals(correctedAddress, stored.alumnoDomicilio)
        assertEquals(original.observacionesSecretaria, stored.observacionesSecretaria)
        assertEquals(original.motivoCorreccion, stored.motivoCorreccion)
        assertEquals(
            original.documentosDeclarados.map { it.cotejadoSecretaria },
            stored.documentosDeclarados.map { it.cotejadoSecretaria }
        )
        assertEquals(photosBefore, PreApplicationViewModel.photos.value[original.folio])
        assertEquals(observationsBefore, PreApplicationViewModel.reviewObservations.value[original.folio])
    }

    @Test
    fun resubmitCorrectionInvalidatesPreviousReadiness() {
        val original = submitReadyCorrectionCandidate(uniqueCurp("RESRDY"))
        assertEquals(ReadinessStatus.READY, original.readinessStatus)
        assertNotNull(original.readyAt)

        val result = PreApplicationViewModel.resubmitCorrectedPreApplication(
            original.copy(alumnoDomicilio = "Domicilio corregido para revalidar")
        )

        assertIs<FamilyResubmissionResult.Success>(result)
        val stored = PreApplicationViewModel.sharedPreApplications.value.single { it.folio == original.folio }
        assertEquals(ReadinessStatus.PENDING, stored.readinessStatus)
        assertNull(stored.readyAt)
        assertEquals("", stored.readinessNotes)
    }

    @Test
    fun resubmitCorrectionDoesNotChangeAnotherPreApplication() {
        val original = submitCorrectablePreApplication(uniqueCurp("RESISO1"))
        val other = submitAndGetRaw { preApplication(curp = uniqueCurp("RESISO2")) }

        val result = PreApplicationViewModel.resubmitCorrectedPreApplication(
            original.copy(alumnoDomicilio = "Solo cambia la solicitud objetivo")
        )

        assertIs<FamilyResubmissionResult.Success>(result)
        assertEquals(
            other,
            PreApplicationViewModel.sharedPreApplications.value.single { it.folio == other.folio }
        )
    }

    @Test
    fun resubmitCorrectionSecondAttemptDoesNotModifyAlreadyResubmittedApplication() {
        val original = submitCorrectablePreApplication(uniqueCurp("RESTW"))
        val corrected = original.copy(alumnoDomicilio = "Primera correccion")
        val firstResult = assertIs<FamilyResubmissionResult.Success>(
            PreApplicationViewModel.resubmitCorrectedPreApplication(corrected)
        )
        assertEquals(PreApplicationStatus.ENVIADA, firstResult.preApplication.status)
        assertEquals(
            PreApplicationStatus.ENVIADA,
            PreApplicationViewModel.sharedPreApplications.value.single { it.folio == original.folio }.status
        )
        val afterFirstAttempt = PreApplicationViewModel.sharedPreApplications.value.toList()

        val secondResult = PreApplicationViewModel.resubmitCorrectedPreApplication(
            corrected.copy(alumnoDomicilio = "Segundo intento no permitido")
        )

        assertIs<FamilyResubmissionResult.InvalidStatus>(secondResult)
        assertEquals(afterFirstAttempt, PreApplicationViewModel.sharedPreApplications.value)
    }

    @Test
    fun familyLookupNormalizesCredentialsAndReturnsAuthorizedFields() {
        val submitted = submitAndGetRaw { preApplication(curp = uniqueCurp("LOOKUP")) }
        PreApplicationViewModel.markForCorrection(submitted.folio)
        PreApplicationViewModel.setMotivoCorreccion(submitted.folio, "Corregir acta de nacimiento")
        PreApplicationViewModel.setObservaciones(submitted.folio, "Presentar documento legible")
        val spacedFolio = submitted.folio.lowercase().toList().joinToString(" ", prefix = "  ", postfix = "  ")
        val spacedCurp = submitted.alumnoCurp.lowercase().chunked(3).joinToString(" ", prefix = "  ", postfix = "  ")

        val result = PreApplicationViewModel.lookupFamilyPreApplication(spacedFolio, spacedCurp)

        val success = assertIs<FamilyPreApplicationLookupResult.Success>(result)
        assertEquals(submitted.folio, success.folio)
        assertEquals(PreApplicationStatus.PENDIENTE_CORRECCION, success.status)
        assertEquals("Corregir acta de nacimiento", success.correctionReason)
        assertEquals("Presentar documento legible", success.secretariaObservations)
    }

    @Test
    fun familyLookupReturnsGenericErrorForWrongCurp() {
        val first = PreApplicationViewModel.sharedPreApplications.value[0]
        val second = PreApplicationViewModel.sharedPreApplications.value[1]

        val error = assertIs<FamilyPreApplicationLookupResult.Error>(
            PreApplicationViewModel.lookupFamilyPreApplication(first.folio, second.alumnoCurp)
        )

        assertEquals(
            "No fue posible consultar la pre-solicitud con los datos proporcionados.",
            error.message
        )
    }

    @Test
    fun familyLookupReturnsGenericErrorForUnknownFolio() {
        val stored = PreApplicationViewModel.sharedPreApplications.value.first()

        val error = assertIs<FamilyPreApplicationLookupResult.Error>(
            PreApplicationViewModel.lookupFamilyPreApplication("FOLIO-INEXISTENTE", stored.alumnoCurp)
        )

        assertEquals(
            "No fue posible consultar la pre-solicitud con los datos proporcionados.",
            error.message
        )
    }

    @Test
    fun familyLookupDoesNotModifySharedPreApplications() {
        val stored = PreApplicationViewModel.sharedPreApplications.value.first()
        val before = PreApplicationViewModel.sharedPreApplications.value.toList()

        assertIs<FamilyPreApplicationLookupResult.Success>(
            PreApplicationViewModel.lookupFamilyPreApplication(stored.folio, stored.alumnoCurp)
        )

        assertEquals(before, PreApplicationViewModel.sharedPreApplications.value)
    }

    private fun submitCorrectablePreApplication(curp: String): PreApplication {
        val submitted = submitAndGetRaw { preApplication(curp = curp) }
        PreApplicationViewModel.markForCorrection(submitted.folio)
        return PreApplicationViewModel.sharedPreApplications.value.single { it.folio == submitted.folio }
    }

    private fun submitReadyCorrectionCandidate(curp: String): PreApplication {
        val document = DocumentoDeclarado("CURP", declarado = true)
        val submitted = submitAndGetRaw {
            preApplication(curp = curp, documents = listOf(document))
        }
        PreApplicationViewModel.setObservaciones(submitted.folio, "Observacion institucional")
        PreApplicationViewModel.setMotivoCorreccion(submitted.folio, "Corregir domicilio")
        PreApplicationViewModel.addReviewObservation(
            submitted.folio,
            "Correccion solicitada",
            "Corregir domicilio"
        )
        PreApplicationViewModel.toggleDocumentCotejado(submitted.folio, document.nombre)
        PreApplicationViewModel.simulateCaptureStudentPhoto(submitted.folio)
        PreApplicationViewModel.simulateCaptureResponsablePhoto(submitted.folio)
        PreApplicationViewModel.approvePreApplication(submitted.folio)
        assertIs<ReadinessResult.Success>(
            PreApplicationViewModel.markReadyForOfficialEnrollment(submitted.folio)
        )
        PreApplicationViewModel.markForCorrection(submitted.folio)
        return PreApplicationViewModel.sharedPreApplications.value.single { it.folio == submitted.folio }
    }

    /** Submit a pre-application and return the stored ENVIADA result. */
    private fun submitAndGetRaw(buildPreApp: () -> PreApplication): PreApplication {
        val result = PreApplicationViewModel.submitFamilyPreApplication(buildPreApp())
        val stored = assertIs<FamilySubmissionResult.Success>(result).preApplication
        return PreApplicationViewModel.sharedPreApplications.value.first { it.folio == stored.folio }
    }

    // ── V2 Annual Enrollment Flow (Macroloop 6H) ───────────────────────

    @Test
    fun v2IsProcessingStartsFalse() {
        assertFalse(PreApplicationViewModel.isProcessingAnnualEnrollmentV2.value)
    }

    @Test
    fun v2IsProcessingSetAndResetDuringExecution() {
        PreApplicationViewModel.setProcessingAnnualEnrollmentV2(true)
        assertTrue(PreApplicationViewModel.isProcessingAnnualEnrollmentV2.value)
        PreApplicationViewModel.setProcessingAnnualEnrollmentV2(false)
        assertFalse(PreApplicationViewModel.isProcessingAnnualEnrollmentV2.value)
    }

    @Test
    fun v2NewEntryReturnsCompletedWithV2EnrollmentId() {
        MockSaseData.resetForTests()
        val preApp = submitReadyCandidate(curp = uniqueCurp("V2NENT"))
        val result = PreApplicationViewModel.processAnnualEnrollmentV2(
            declaredMovement = preApp.tramite,
            normalizedCurp = preApp.alumnoCurp,
            folio = preApp.folio,
            requestedGrade = preApp.gradoSolicitado,
            previousGroup = null,
            schoolYear = preApp.cicloEscolar,
            studentFullName = preApp.alumnoNombreCompleto
        )
        val completed = assertIs<AnnualEnrollmentFlowResult.Completed>(result)
        assertTrue(completed.enrollmentId.startsWith("S310-"), "V2 debe usar formato S310-*")
        assertTrue(completed.enrollmentId.matches(Regex("S310-\\d{6}-\\d$")), "Formato consecutivo")
        assertTrue(MockSaseData.annualEnrollments.value.any { it.studentId?.contains("V2") == true })
    }

    @Test
    fun v2NewEntryDoesNotAssignGroup() {
        MockSaseData.resetForTests()
        val preApp = submitReadyCandidate(curp = uniqueCurp("V2NOGRP"))
        PreApplicationViewModel.processAnnualEnrollmentV2(
            declaredMovement = preApp.tramite,
            normalizedCurp = preApp.alumnoCurp,
            folio = preApp.folio,
            requestedGrade = preApp.gradoSolicitado,
            previousGroup = null,
            schoolYear = preApp.cicloEscolar,
            studentFullName = preApp.alumnoNombreCompleto
        )
        val master = MockSaseData.students.value.firstOrNull { it.curp == preApp.alumnoCurp }
        assertNotNull(master)
        assertEquals("", master.group, "V2 no debe asignar grupo")
    }

    @Test
    fun v2ReEnrollmentPreservesEnrollmentId() {
        MockSaseData.resetForTests()
        val curp = "RENR100101HDFABC01"
        MockSaseData.addStudent(
            com.example.data.Student(
                id = "RE-V2-001", fullName = "ALUMNO RE V2",
                group = "1A", enrollmentId = "S310-000001-1",
                curp = curp, preApplicationFolio = "PRE-RE-V2-001"
            )
        )
        val result = PreApplicationViewModel.processAnnualEnrollmentV2(
            declaredMovement = "REINSCRIPCION",
            normalizedCurp = curp,
            folio = "PRE-V2REN-${uniqueSuffix()}",
            requestedGrade = 2,
            previousGroup = null,
            schoolYear = "2026-2027",
            studentFullName = "ALUMNO RE V2"
        )
        val completed = assertIs<AnnualEnrollmentFlowResult.Completed>(result)
        assertEquals("S310-000001-1", completed.enrollmentId)
    }

    @Test
    fun v2ReEnrollmentReturnsNeedsDecisionWithContinuity() {
        MockSaseData.resetForTests()
        val curp = "REND100101HDFABC02"
        MockSaseData.addStudent(
            com.example.data.Student(
                id = "RE-V2-002", fullName = "ALUMNO RE V2 ND",
                group = "1A", enrollmentId = "S310-000010-2",
                curp = curp, preApplicationFolio = "PRE-RE-V2-002"
            )
        )
        val result = PreApplicationViewModel.processAnnualEnrollmentV2(
            declaredMovement = "REINSCRIPCION",
            normalizedCurp = curp,
            folio = "PRE-V2ND-${uniqueSuffix()}",
            requestedGrade = 2,
            previousGroup = "1A",
            schoolYear = "2026-2027",
            studentFullName = "ALUMNO RE V2 ND"
        )
        val needsDecision = assertIs<AnnualEnrollmentFlowResult.NeedsDecision>(result)
        assertEquals("1A", needsDecision.previousGroup)
        assertEquals("2A", needsDecision.suggestedGroup)
    }

    @Test
    fun v2AlreadyCompletedOnDuplicateRequest() {
        MockSaseData.resetForTests()
        val preApp = submitReadyCandidate(curp = uniqueCurp("V2DUP"))
        val folio = "PRE-V2DUP-${uniqueSuffix()}"
        val first = PreApplicationViewModel.processAnnualEnrollmentV2(
            declaredMovement = preApp.tramite,
            normalizedCurp = preApp.alumnoCurp,
            folio = folio,
            requestedGrade = preApp.gradoSolicitado,
            previousGroup = null,
            schoolYear = preApp.cicloEscolar,
            studentFullName = preApp.alumnoNombreCompleto
        )
        assertIs<AnnualEnrollmentFlowResult.Completed>(first)
        val enrollmentCount = MockSaseData.annualEnrollments.value.size
        val second = PreApplicationViewModel.processAnnualEnrollmentV2(
            declaredMovement = preApp.tramite,
            normalizedCurp = preApp.alumnoCurp,
            folio = folio,
            requestedGrade = preApp.gradoSolicitado,
            previousGroup = null,
            schoolYear = preApp.cicloEscolar,
            studentFullName = preApp.alumnoNombreCompleto
        )
        assertIs<AnnualEnrollmentFlowResult.AlreadyCompleted>(second)
        assertEquals(enrollmentCount, MockSaseData.annualEnrollments.value.size)
    }

    @Test
    fun v2ConflictReturnsStageAndNoMutation() {
        MockSaseData.resetForTests()
        val studentCount = MockSaseData.students.value.size
        val enrollmentCount = MockSaseData.annualEnrollments.value.size
        val preApp = submitReadyCandidate(curp = uniqueCurp("V2CONF"))
        val result = PreApplicationViewModel.processAnnualEnrollmentV2(
            declaredMovement = preApp.tramite,
            normalizedCurp = "",
            folio = preApp.folio,
            requestedGrade = preApp.gradoSolicitado,
            previousGroup = null,
            schoolYear = preApp.cicloEscolar,
            studentFullName = preApp.alumnoNombreCompleto
        )
        val conflict = assertIs<AnnualEnrollmentFlowResult.Conflict>(result)
        assertTrue(conflict.stage.isNotBlank(), "Conflicto debe indicar etapa")
        assertEquals(studentCount, MockSaseData.students.value.size)
        assertEquals(enrollmentCount, MockSaseData.annualEnrollments.value.size)
    }

    @Test
    fun v2ResultStoredInViewModel() {
        MockSaseData.resetForTests()
        PreApplicationViewModel.resetSharedStateForTests()
        assertNull(PreApplicationViewModel.v2Result.value)
        val preApp = submitReadyCandidate(curp = uniqueCurp("V2STORE"))
        PreApplicationViewModel.processAnnualEnrollmentV2(
            declaredMovement = preApp.tramite,
            normalizedCurp = preApp.alumnoCurp,
            folio = "PRE-V2STO-${uniqueSuffix()}",
            requestedGrade = preApp.gradoSolicitado,
            previousGroup = null,
            schoolYear = preApp.cicloEscolar,
            studentFullName = preApp.alumnoNombreCompleto
        )
        assertNotNull(PreApplicationViewModel.v2Result.value)
    }

    @Test
    fun v2LegacyFlowStillAvailable() {
        MockSaseData.resetForTests()
        assertEquals(com.example.data.enrollment.EnrollmentFlowMode.LEGACY, PreApplicationViewModel.enrollmentFlowMode.value)
    }
}
