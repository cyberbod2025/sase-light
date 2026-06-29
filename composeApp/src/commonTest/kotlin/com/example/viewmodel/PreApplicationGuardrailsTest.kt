package com.example.viewmodel

import com.example.data.MockSaseData
import com.example.data.Student
import com.example.data.StudentAddResult
import com.example.data.presolicitud.AntecedentesUdeii
import com.example.data.presolicitud.AutorizadoPreSolicitud
import com.example.data.presolicitud.ConsentimientosFamiliares
import com.example.data.presolicitud.ContextoSociofamiliar
import com.example.data.presolicitud.DocumentoDeclarado
import com.example.data.presolicitud.FichaMedicaFamiliar
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
    fun startOfficialEnrollmentBlocksDuplicateCurpInOfficialStudents() {
        val folio = "TEST-OFFICIAL-CURP-${uniqueSuffix()}"
        PreApplicationViewModel.simulateCaptureStudentPhoto(folio)
        PreApplicationViewModel.simulateCaptureResponsablePhoto(folio)

        val result = PreApplicationViewModel.startOfficialEnrollment(
            preApplication(
                folio = folio,
                curp = "SARH840603MDFRRN09",
                status = PreApplicationStatus.ACEPTADA
            ),
            selectedGroup = null
        )

        assertIs<OfficialEnrollmentResult.DuplicateCurp>(result)
    }

    @Test
    fun startOfficialEnrollmentBlocksDuplicateMatricula() {
        val curpWithExistingMatriculaPrefix = "SARH840603ZZZZZZ99"
        val submission = PreApplicationViewModel.submitFamilyPreApplication(
            preApplication(curp = curpWithExistingMatriculaPrefix, grado = 1)
        )
        val stored = assertIs<FamilySubmissionResult.Success>(submission).preApplication
        PreApplicationViewModel.approvePreApplication(stored.folio)
        PreApplicationViewModel.simulateCaptureStudentPhoto(stored.folio)
        PreApplicationViewModel.simulateCaptureResponsablePhoto(stored.folio)
        assertIs<ReadinessResult.Success>(PreApplicationViewModel.markReadyForOfficialEnrollment(stored.folio))

        val acceptedStored = PreApplicationViewModel.sharedPreApplications.value
            .first { it.folio == stored.folio }
        val result = PreApplicationViewModel.startOfficialEnrollment(acceptedStored, selectedGroup = null)

        assertIs<OfficialEnrollmentResult.DuplicateMatricula>(result)
    }

    @Test
    fun mockSaseDataAddStudentRejectsDuplicateCurp() {
        val result = MockSaseData.addStudent(
            student(
                curp = "lohm100512mdfprra2",
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
                enrollmentId = "2023-00258"
            )
        )

        assertIs<StudentAddResult.DuplicateEnrollmentId>(result)
    }

    @Test
    fun labViewModelAddStudentReturnsStudentAddResultForFastTrackFlow() {
        val viewModel = LabViewModel()

        val result = viewModel.addStudent(
            student(
                curp = "LOHM100512MDFPRRA2",
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

        val result = PreApplicationViewModel.startOfficialEnrollment(readyStored, selectedGroup = null)

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

        val result = PreApplicationViewModel.startOfficialEnrollment(readyStored, selectedGroup = null)

        val success = assertIs<OfficialEnrollmentResult.Success>(result)
        assertTrue(success.masterStudentCreated)
        assertTrue(PreApplicationViewModel.officialStudents.value.any { it.preApplicationFolio == readyStored.folio })
        assertNotNull(MockSaseData.studentByCurp(readyStored.alumnoCurp))
    }

    @Test
    fun propagatedMasterStudentUsesOfficialMatriculaAndNormalizedCurp() {
        val rawCurp = uniqueCurp("NORMED").lowercase()
        val readyCandidate = submitReadyCandidate(curp = rawCurp)
        assertIs<ReadinessResult.Success>(PreApplicationViewModel.markReadyForOfficialEnrollment(readyCandidate.folio))
        val readyStored = PreApplicationViewModel.sharedPreApplications.value.first { it.folio == readyCandidate.folio }
        val expectedMatricula = com.example.data.presolicitud.OfficialStudent.generateMatricula(readyStored.alumnoCurp, readyStored.gradoSolicitado)

        val result = PreApplicationViewModel.startOfficialEnrollment(readyStored, selectedGroup = null)

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
        val expectedMatricula = com.example.data.presolicitud.OfficialStudent.generateMatricula(
            readyCandidate.alumnoCurp,
            readyCandidate.gradoSolicitado
        ) ?: ""
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

        val result = PreApplicationViewModel.startOfficialEnrollment(readyStored, selectedGroup = null)

        assertIs<OfficialEnrollmentResult.DuplicateMatricula>(result)
        assertTrue(PreApplicationViewModel.officialStudents.value.none { it.preApplicationFolio == readyStored.folio })
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

    private fun uniqueCurp(seed: String): String =
        (seed.take(6).uppercase().padEnd(6, 'X') + uniqueSuffix().padEnd(12, '0')).take(18)

    private fun uniqueSuffix(): String =
        kotlin.random.Random.nextInt(100000, 999999).toString()
}
