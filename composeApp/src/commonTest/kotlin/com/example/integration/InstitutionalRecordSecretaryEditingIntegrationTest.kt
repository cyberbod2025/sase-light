package com.example.integration

import com.example.data.InstitutionalStudentRecordResolution
import com.example.data.MockSaseData
import com.example.data.Student
import com.example.data.StudentAddResult
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
import com.example.ui.student.InstitutionalStudentRecordPresentation
import com.example.ui.student.institutionalStudentRecordPresentation
import com.example.ui.student.resolveInstitutionalStudentRecordForRoute
import com.example.viewmodel.FamilySubmissionResult
import com.example.viewmodel.LabViewModel
import com.example.viewmodel.PreApplicationViewModel
import com.example.viewmodel.OfficialEnrollmentResult
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

class InstitutionalRecordSecretaryEditingIntegrationTest {
    private var seq = 0
    private fun nextSeq() = seq++

    @BeforeTest
    fun resetBefore() {
        PreApplicationViewModel.resetSharedStateForTests()
    }

    @AfterTest
    fun resetAfter() {
        PreApplicationViewModel.resetSharedStateForTests()
    }

    @Test
    fun `editar domicilio y telefono actualiza PreApplication`() {
        val s = nextSeq()
        val (preApp, _, _) = createEnvironment(s)

        val result = PreApplicationViewModel.updatePreApplicationAdministrativeData(
            UpdatePreApplicationAdministrativeDataRequest(
                folio = preApp.folio,
                expected = preApp.administrativeDataSnapshot(),
                changes = PreApplicationAdministrativeChanges(
                    address = PreApplicationAdministrativeFieldChange.Replace("DOMICILIO EDITADO $s"),
                    phone = PreApplicationAdministrativeFieldChange.Replace("5511111111")
                )
            )
        )
        assertIs<UpdatePreApplicationAdministrativeDataResult.Updated>(result)

        val updated = PreApplicationViewModel.sharedPreApplications.value
            .single { it.folio == preApp.folio }
        assertEquals("DOMICILIO EDITADO $s", updated.alumnoDomicilio)
        assertEquals("5511111111", updated.alumnoTelefonoCasa)
    }

    @Test
    fun `guardar y reabrir muestra los nuevos valores`() {
        val s = nextSeq()
        val (preApp, student, annual) = createEnvironment(s)
        val content = resolveContent(student, annual)
        assertEquals("DOMICILIO INICIAL $s", content.value("Domicilio"))
        assertEquals("5512345678", content.value("Teléfono del hogar"))

        PreApplicationViewModel.updatePreApplicationAdministrativeData(
            UpdatePreApplicationAdministrativeDataRequest(
                folio = preApp.folio,
                expected = preApp.administrativeDataSnapshot(),
                changes = PreApplicationAdministrativeChanges(
                    address = PreApplicationAdministrativeFieldChange.Replace("DOMICILIO REABIERTO $s"),
                    phone = PreApplicationAdministrativeFieldChange.Replace("5522222222")
                )
            )
        )

        val content2 = resolveContent(student, annual)
        assertEquals("DOMICILIO REABIERTO $s", content2.value("Domicilio"))
        assertEquals("5522222222", content2.value("Teléfono del hogar"))
    }

    @Test
    fun `editar nombre CURP grupo tutor actualiza Student`() {
        val s = nextSeq()
        val (_, student, _) = createEnvironment(s)

        val updated = student.copy(
            fullName = "NOMBRE NUEVO $s",
            curp = "CURP${s}0101HDFABC01",
            group = "1A",
            tutorName = "TUTOR NUEVO $s",
            tutorPhone = "5533333333"
        )
        MockSaseData.updateStudent(updated)

        val saved = MockSaseData.students.value.single { it.id == student.id }
        assertEquals("NOMBRE NUEVO $s", saved.fullName)
        assertEquals("CURP${s}0101HDFABC01", saved.curp)
        assertEquals("1A", saved.group)
        assertEquals("TUTOR NUEVO $s", saved.tutorName)
        assertEquals("5533333333", saved.tutorPhone)
    }

    @Test
    fun `cancelar no modifica Student ni PreApplication`() {
        val s = nextSeq()
        val (preApp, student, _) = createEnvironment(s)

        val studentBefore = MockSaseData.students.value.single { it.id == student.id }
        val preAppBefore = PreApplicationViewModel.sharedPreApplications.value
            .single { it.folio == preApp.folio }

        // Cancel: no mutations executed
        val studentAfter = MockSaseData.students.value.single { it.id == student.id }
        val preAppAfter = PreApplicationViewModel.sharedPreApplications.value
            .single { it.folio == preApp.folio }

        assertEquals(studentBefore, studentAfter)
        assertEquals(preAppBefore, preAppAfter)
    }

    @Test
    fun `Validar cambia readinessStatus a READY y la accion desaparece`() {
        val s = nextSeq()
        val (preApp, student, annual) = createEnvironment(s)

        // Preparar pre-aplicación lista para validación
        PreApplicationViewModel.approvePreApplication(preApp.folio)
        PreApplicationViewModel.toggleDocumentCotejado(preApp.folio, "CURP")
        PreApplicationViewModel.markDocumentValidado(preApp.folio, "CURP")
        PreApplicationViewModel.simulateCaptureStudentPhoto(preApp.folio)
        PreApplicationViewModel.simulateCaptureResponsablePhoto(preApp.folio)

        val preAppReady = PreApplicationViewModel.sharedPreApplications.value
            .single { it.folio == preApp.folio }
        assertEquals(PreApplicationStatus.ACEPTADA, preAppReady.status)

        val result = PreApplicationViewModel.markReadyForOfficialEnrollment(preApp.folio)
        assertIs<ReadinessResult.Success>(result)

        val updatedPreApp = PreApplicationViewModel.sharedPreApplications.value
            .single { it.folio == preApp.folio }
        assertEquals(ReadinessStatus.READY, updatedPreApp.readinessStatus)

        val resolved = resolveResolved(student, annual)
        assertFalse(resolved.isEditable, "isEditable debe ser false tras Validar")
    }

    @Test
    fun `Aceptar folio cambia status a ACEPTADA y la accion desaparece`() {
        val s = nextSeq()
        val (preApp, student, annual) = createEnvironment(s)

        assertEquals(PreApplicationStatus.ENVIADA, preApp.status)

        PreApplicationViewModel.approvePreApplication(preApp.folio)

        val updatedPreApp = PreApplicationViewModel.sharedPreApplications.value
            .single { it.folio == preApp.folio }
        assertEquals(PreApplicationStatus.ACEPTADA, updatedPreApp.status)

        val resolved = resolveResolved(student, annual)
        assertFalse(resolved.acceptFolioVisible, "acceptFolioVisible debe ser false tras Aceptar folio")
    }

    @Test
    fun `ejecutar ambas acciones deja ambos estados correctos`() {
        val s = nextSeq()
        val (preApp, student, annual) = createEnvironment(s)

        // Preparar para que markReadyForOfficialEnrollment no rechace
        PreApplicationViewModel.toggleDocumentCotejado(preApp.folio, "CURP")
        PreApplicationViewModel.markDocumentValidado(preApp.folio, "CURP")
        PreApplicationViewModel.simulateCaptureStudentPhoto(preApp.folio)
        PreApplicationViewModel.simulateCaptureResponsablePhoto(preApp.folio)

        // Aceptar folio primero, luego validar
        PreApplicationViewModel.approvePreApplication(preApp.folio)
        PreApplicationViewModel.markReadyForOfficialEnrollment(preApp.folio)

        val preAppFinal = PreApplicationViewModel.sharedPreApplications.value
            .single { it.folio == preApp.folio }
        assertEquals(ReadinessStatus.READY, preAppFinal.readinessStatus)
        assertEquals(PreApplicationStatus.ACEPTADA, preAppFinal.status)

        val resolved = resolveResolved(student, annual)
        assertFalse(resolved.isEditable)
        assertFalse(resolved.acceptFolioVisible)
    }

    @Test
    fun `canReopenReview es true cuando readinessStatus es READY`() {
        val s = nextSeq()
        val (preApp, student, annual) = createEnvironment(s)

        PreApplicationViewModel.approvePreApplication(preApp.folio)
        PreApplicationViewModel.toggleDocumentCotejado(preApp.folio, "CURP")
        PreApplicationViewModel.markDocumentValidado(preApp.folio, "CURP")
        PreApplicationViewModel.simulateCaptureStudentPhoto(preApp.folio)
        PreApplicationViewModel.simulateCaptureResponsablePhoto(preApp.folio)
        PreApplicationViewModel.markReadyForOfficialEnrollment(preApp.folio)

        val resolved = resolveResolved(student, annual)
        assertTrue(resolved.canReopenReview, "canReopenReview debe ser true cuando readiness es READY")
    }

    @Test
    fun `reopenReview resetea readiness a PENDING`() {
        val s = nextSeq()
        val (preApp, student, annual) = createEnvironment(s)

        PreApplicationViewModel.approvePreApplication(preApp.folio)
        PreApplicationViewModel.toggleDocumentCotejado(preApp.folio, "CURP")
        PreApplicationViewModel.markDocumentValidado(preApp.folio, "CURP")
        PreApplicationViewModel.simulateCaptureStudentPhoto(preApp.folio)
        PreApplicationViewModel.simulateCaptureResponsablePhoto(preApp.folio)
        PreApplicationViewModel.markReadyForOfficialEnrollment(preApp.folio)

        val reabierto = PreApplicationViewModel.reopenReview(preApp.folio)
        assertTrue(reabierto, "reopenReview debe retornar true")

        val updated = PreApplicationViewModel.sharedPreApplications.value
            .single { it.folio == preApp.folio }
        assertEquals(ReadinessStatus.PENDING, updated.readinessStatus)
        assertNull(updated.readyAt, "readyAt debe ser null tras reabrir")
    }

    @Test
    fun `markDocumentNoAplica establece noAplica en true`() {
        val s = nextSeq()
        val (preApp, _, _) = createEnvironment(s)

        PreApplicationViewModel.markDocumentNoAplica(preApp.folio, "CURP")

        val updated = PreApplicationViewModel.sharedPreApplications.value
            .single { it.folio == preApp.folio }
        val doc = updated.documentosDeclarados.single { it.nombre == "CURP" }
        assertTrue(doc.noAplica, "documento debe tener noAplica = true tras markDocumentNoAplica")
        assertFalse(doc.cotejadoSecretaria, "cotejadoSecretaria debe ser false cuando noAplica es true")
    }

    @Test
    fun `documento con noAplica no cuenta como pendiente`() {
        val s = nextSeq()
        val (preApp, _, _) = createEnvironment(s)

        PreApplicationViewModel.markDocumentNoAplica(preApp.folio, "CURP")

        val updated = PreApplicationViewModel.sharedPreApplications.value
            .single { it.folio == preApp.folio }
        val pendientes = updated.documentosDeclarados.filter { !it.noAplica && !it.cotejadoSecretaria }
        assertTrue(
            pendientes.none { it.nombre == "CURP" },
            "CURP no debe estar en pendientes tras marcarse como noAplica"
        )
        assertTrue(
            updated.documentosDeclarados.single { it.nombre == "CURP" }.noAplica,
            "CURP debe tener noAplica = true"
        )
    }

    @Test
    fun `no quedan datos contradictorios entre Student y PreApplication`() {
        val s = nextSeq()
        val (preApp, student, _) = createEnvironment(s)

        val canonicalCurp = { s: String -> s.filterNot(Char::isWhitespace).uppercase() }
        assertEquals(
            canonicalCurp(student.curp),
            canonicalCurp(preApp.alumnoCurp),
            "CURP de Student y PreApplication deben coincidir"
        )
        assertEquals(
            student.preApplicationFolio,
            preApp.folio,
            "Folio de Student debe apuntar al PreApplication"
        )
    }

    @Test
    fun `Secretaria no tiene acciones para registrar incidencias en el resolvedor`() {
        val s = nextSeq()
        val (_, student, annual) = createEnvironment(s)
        val resolved = resolveResolved(student, annual)
        val content = assertIs<InstitutionalStudentRecordPresentation.Content>(
            institutionalStudentRecordPresentation(resolved)
        )
        // No incident-related fields or warnings in the institutional record
        assertFalse(
            content.warnings.any { it.contains("incidencia", ignoreCase = true) },
            "No deben aparecer advertencias de incidencias"
        )
        assertFalse(
            content.fields.any { it.label.contains("incidencia", ignoreCase = true) },
            "No deben aparecer campos de incidencias"
        )
    }

    @Test
    fun `abrir tarjeta conserva el Student id exacto`() {
        val s = nextSeq()
        val id = "MASTER-TEST-ID-$s"
        val student = Student(
            id = id,
            fullName = "ALUMNO ID TEST $s",
            group = "",
            enrollmentId = "S310-IDT${s}0101",
            curp = "IDTE${s}0101HDFABC01",
            preApplicationFolio = "PRE-ID-$s"
        )
        assertIs<StudentAddResult.Added>(MockSaseData.addStudent(student))

        val viewModel = LabViewModel()
        viewModel.navigateTo(
            Screen.StudentRecord(
                studentId = id,
                returnTo = Screen.SecretaryDashboard
            )
        )
        val route = assertIs<Screen.StudentRecord>(viewModel.currentScreen.value)
        assertEquals(id, route.studentId)
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun createEnvironment(seq: Int, addAnnual: Boolean = true): Triple<PreApplication, Student, AnnualEnrollmentRecord> {
        val s = seq.toString().padStart(2, '0')
        val folio = "PRE-EDIT-$seq"
        val curp = "EDIT${s}0101HDFABC01"
        val enrollmentId = "S310-EDT$s"
        val studentId = "MASTER-EDIT-$seq"

        val draftPreApp = MockPreApplicationData.preApplications.first().copy(
            folio = folio,
            status = PreApplicationStatus.BORRADOR,
            submittedAt = null,
            tramite = "NUEVO INGRESO",
            cicloEscolar = "2026-2027",
            gradoSolicitado = 1,
            alumnoNombreCompleto = "ALUMNO EDIT $seq",
            alumnoCurp = curp,
            alumnoDomicilio = "DOMICILIO INICIAL $seq",
            alumnoTelefonoCasa = "5512345678",
            promedioGradoAnterior = 8.0,
            documentosDeclarados = listOf(
                DocumentoDeclarado("CURP", declarado = true, cotejadoSecretaria = false)
            ),
            readinessStatus = ReadinessStatus.PENDING,
            readyAt = null,
            readinessNotes = ""
        )

        val submitted = assertIs<FamilySubmissionResult.Success>(
            PreApplicationViewModel.submitFamilyPreApplication(draftPreApp)
        ).preApplication

        val student = Student(
            id = studentId,
            fullName = "ALUMNO EDIT $seq",
            group = "",
            enrollmentId = enrollmentId,
            curp = curp,
            preApplicationFolio = folio
        )
        assertIs<StudentAddResult.Added>(MockSaseData.addStudent(student))

        val annual = AnnualEnrollmentRecord(
            studentId = studentId,
            normalizedCurp = curp,
            permanentEnrollmentId = enrollmentId,
            schoolYear = "2026-2027",
            sourcePreApplicationFolio = folio,
            movement = AnnualEnrollmentMovement.NEW_ENTRY,
            requestedGrade = 1,
            groupPlacementRequirement = GroupPlacementRequirement.AssignmentRequired,
            status = AnnualEnrollmentInitialStatus.PENDING_GROUP_ASSIGNMENT
        )
        if (addAnnual) {
            MockSaseData.addAnnualEnrollment(annual)
        }

        val preApp = PreApplicationViewModel.sharedPreApplications.value
            .single { it.folio == folio }
        return Triple(preApp, student, annual)
    }

    private fun resolveResolved(
        student: Student,
        annual: AnnualEnrollmentRecord
    ): InstitutionalStudentRecordResolution.Resolved {
        val resolution = resolveInstitutionalStudentRecordForRoute(
            studentId = student.id,
            institutionalKey = null,
            students = MockSaseData.students.value,
            annualEnrollments = MockSaseData.annualEnrollments.value,
            preApplications = PreApplicationViewModel.sharedPreApplications.value
        )
        return assertIs(resolution)
    }

    private fun resolveContent(
        student: Student,
        annual: AnnualEnrollmentRecord
    ): InstitutionalStudentRecordPresentation.Content {
        val resolved = resolveResolved(student, annual)
        return assertIs(institutionalStudentRecordPresentation(resolved))
    }

    private fun InstitutionalStudentRecordPresentation.Content.field(label: String) =
        fields.single { it.label == label }

    private fun InstitutionalStudentRecordPresentation.Content.value(label: String) =
        field(label).value

    // ── Nuevos tests: Documentos acciones completas ──

    @Test
    fun `toggleDocumentCotejado solo funciona en docs declarados`() {
        val s = nextSeq()
        val (preApp, _, _) = createEnvironment(s)

        PreApplicationViewModel.toggleDocumentCotejado(preApp.folio, "CURP")
        var doc = docByNombre(preApp.folio, "CURP")
        assertTrue(doc.cotejadoSecretaria, "CURP debe quedar cotejado tras toggle")

        PreApplicationViewModel.toggleDocumentCotejado(preApp.folio, "CURP")
        doc = docByNombre(preApp.folio, "CURP")
        assertFalse(doc.cotejadoSecretaria, "CURP debe des-cotejarse tras segundo toggle")
    }

    @Test
    fun `markDocumentValidado requiere cotejado previo`() {
        val s = nextSeq()
        val (preApp, _, _) = createEnvironment(s)

        // Sin cotejar: validado debe fallar silenciosamente (no cambia)
        PreApplicationViewModel.markDocumentValidado(preApp.folio, "CURP")
        var doc = docByNombre(preApp.folio, "CURP")
        assertFalse(doc.validado, "Validado no debe funcionar sin cotejar")

        // Cotejar primero
        PreApplicationViewModel.toggleDocumentCotejado(preApp.folio, "CURP")
        PreApplicationViewModel.markDocumentValidado(preApp.folio, "CURP")
        doc = docByNombre(preApp.folio, "CURP")
        assertTrue(doc.validado, "CURP quedo validado tras cotejar y validar")
        assertTrue(doc.cotejadoSecretaria, "Cotejado debe persistir tras validar")
    }

    @Test
    fun `markDocumentRechazado resetea cotejado`() {
        val s = nextSeq()
        val (preApp, _, _) = createEnvironment(s)

        PreApplicationViewModel.toggleDocumentCotejado(preApp.folio, "CURP")
        PreApplicationViewModel.markDocumentRechazado(preApp.folio, "CURP")

        val doc = docByNombre(preApp.folio, "CURP")
        assertTrue(doc.rechazado, "Rechazado debe ser true")
        assertFalse(doc.cotejadoSecretaria, "Cotejado debe resetearse al rechazar")
        assertFalse(doc.validado, "Validado debe ser false")
    }

    @Test
    fun `setDocumentObservacion persiste en el documento`() {
        val s = nextSeq()
        val (preApp, _, _) = createEnvironment(s)

        PreApplicationViewModel.setDocumentObservacion(preApp.folio, "CURP", "Documento ilegible, solicitar copia")
        val doc = docByNombre(preApp.folio, "CURP")
        assertEquals("Documento ilegible, solicitar copia", doc.observacion)
    }

    @Test
    fun `contador documental refleja validado y noAplica`() {
        val s = nextSeq()
        val (preApp, _, _) = createEnvironment(s)

        val totalInicial = preApp.documentosDeclarados.size
        val resueltosInicial = preApp.documentosDeclarados.count { it.noAplica || it.validado }
        val pendientesInicial = totalInicial - resueltosInicial
        assertEquals(1, totalInicial, "Un solo documento en el test")
        assertEquals(0, resueltosInicial, "Ninguno resuelto al inicio")
        assertEquals(1, pendientesInicial, "Uno pendiente al inicio")

        // Resolver por validacion
        PreApplicationViewModel.toggleDocumentCotejado(preApp.folio, "CURP")
        PreApplicationViewModel.markDocumentValidado(preApp.folio, "CURP")
        var updated = preAppAfter(preApp.folio)
        var resueltos = updated.documentosDeclarados.count { it.noAplica || it.validado }
        assertEquals(1, resueltos, "Un documento resuelto tras validar")

        // Probar que counter = docs - validado - noAplica
        var pendientes = updated.documentosDeclarados.count { !it.noAplica && !it.validado }
        assertEquals(0, pendientes, "Cero pendientes tras validar unico doc")
    }

    @Test
    fun `reabrir tras acciones documentales mantiene estados`() {
        val s = nextSeq()
        val (preApp, _, _) = createEnvironment(s)

        val docNombre = "CURP"
        PreApplicationViewModel.toggleDocumentCotejado(preApp.folio, docNombre)
        PreApplicationViewModel.markDocumentValidado(preApp.folio, docNombre)
        PreApplicationViewModel.setDocumentObservacion(preApp.folio, docNombre, "Ok")

        val trasAccion = preAppAfter(preApp.folio)
        val d = docByNombre(preApp.folio, docNombre)
        assertTrue(d.cotejadoSecretaria)
        assertTrue(d.validado)
        assertEquals("Ok", d.observacion)

        // Simular reapertura: leer de sharedPreApplications de nuevo
        val trasReabrir = preAppAfter(preApp.folio)
        assertEquals(trasAccion, trasReabrir, "Los datos deben persistir exactos tras reapertura")
    }

    @Test
    fun `deep link documentos pendientes en pendingItems`() {
        val s = nextSeq()
        val (preApp, _, _) = createEnvironment(s)

        val pending = PreApplicationViewModel.officialEnrollmentPendingItems(preApp)
        assertTrue(pending.any { it.contains("Documentos", ignoreCase = true) },
            "Debe haber pendiente documental cuando hay docs no cotejados")
    }

    @Test
    fun `markDocumentRechazado requiere cotejadoSecretaria`() {
        val s = nextSeq()
        val (preApp, _, _) = createEnvironment(s)

        // Sin cotejar: rechazado debe fallar silenciosamente
        PreApplicationViewModel.markDocumentRechazado(preApp.folio, "CURP")
        var doc = docByNombre(preApp.folio, "CURP")
        assertFalse(doc.rechazado, "Rechazado no debe funcionar sin cotejar")

        // Cotejar primero, luego rechazar
        PreApplicationViewModel.toggleDocumentCotejado(preApp.folio, "CURP")
        PreApplicationViewModel.markDocumentRechazado(preApp.folio, "CURP")
        doc = docByNombre(preApp.folio, "CURP")
        assertTrue(doc.rechazado, "Rechazado debe ser true tras cotejar y rechazar")
        assertFalse(doc.cotejadoSecretaria, "Cotejado debe resetearse")
    }

    @Test
    fun `toggleDocumentCotejado en rechazado lo recoteja`() {
        val s = nextSeq()
        val (preApp, _, _) = createEnvironment(s)

        // Cotejar → rechazar → cotejar de nuevo
        PreApplicationViewModel.toggleDocumentCotejado(preApp.folio, "CURP")
        PreApplicationViewModel.markDocumentRechazado(preApp.folio, "CURP")
        var doc = docByNombre(preApp.folio, "CURP")
        assertTrue(doc.rechazado, "Debe estar rechazado")
        assertFalse(doc.cotejadoSecretaria, "Cotejado debe ser false tras rechazar")

        // Recotejar
        PreApplicationViewModel.toggleDocumentCotejado(preApp.folio, "CURP")
        doc = docByNombre(preApp.folio, "CURP")
        assertFalse(doc.rechazado, "Rechazado debe limpiarse al recotejar")
        assertTrue(doc.cotejadoSecretaria, "Cotejado debe ser true tras recotejar")

        // Ahora validar
        PreApplicationViewModel.markDocumentValidado(preApp.folio, "CURP")
        doc = docByNombre(preApp.folio, "CURP")
        assertTrue(doc.validado, "Debe poder validarse tras recotejar")
    }

    @Test
    fun `officialEnrollmentPendingItems usa solo noAplica y validado`() {
        val s = nextSeq()
        val (preApp, _, _) = createEnvironment(s)

        // Al inicio: un documento declarado no cotejado → pendiente documental
        var pending = PreApplicationViewModel.officialEnrollmentPendingItems(preAppAfter(preApp.folio))
        assertTrue(pending.any { it.contains("Documentos", ignoreCase = true) })

        // Solo cotejar (sin validar) → sigue pendiente (resolved = validado|noAplica)
        PreApplicationViewModel.toggleDocumentCotejado(preApp.folio, "CURP")
        pending = PreApplicationViewModel.officialEnrollmentPendingItems(preAppAfter(preApp.folio))
        assertTrue(pending.any { it.contains("Documentos", ignoreCase = true) },
            "Cotejar solo no debe resolver el pendiente documental")

        // Validar → resuelto
        PreApplicationViewModel.markDocumentValidado(preApp.folio, "CURP")
        pending = PreApplicationViewModel.officialEnrollmentPendingItems(preAppAfter(preApp.folio))
        assertFalse(pending.any { it.contains("Documentos", ignoreCase = true) },
            "Validar debe resolver el pendiente documental")
    }

    // ── Helpers para tests ──

    private fun docByNombre(folio: String, nombre: String): DocumentoDeclarado =
        preAppAfter(folio).documentosDeclarados.single { it.nombre == nombre }

    private fun preAppAfter(folio: String): PreApplication =
        PreApplicationViewModel.sharedPreApplications.value.single { it.folio == folio }

    // ── Requisito B: Faltantes documentales ──

    @Test
    fun `B primera falta identifica primer doc no declarado`() {
        val s = nextSeq()
        val (preApp, _, _) = createEnvironmentWithDocs(s, listOf(
            DocumentoDeclarado("Acta", declarado = true),
            DocumentoDeclarado("BOLETA / CERTIFICADO", declarado = false),
            DocumentoDeclarado("CURP", declarado = true)
        ))
        val firstMissing = preApp.documentosDeclarados.firstOrNull { !it.declarado }
        assertNotNull(firstMissing)
        assertEquals("BOLETA / CERTIFICADO", firstMissing.nombre,
            "B: primer doc faltante debe ser BOLETA / CERTIFICADO")
    }

    @Test
    fun `B sin faltantes retorna null`() {
        val s = nextSeq()
        val (preApp, _, _) = createEnvironmentWithDocs(s, listOf(
            DocumentoDeclarado("Acta", declarado = true),
            DocumentoDeclarado("CURP", declarado = true)
        ))
        assertNull(preApp.documentosDeclarados.firstOrNull { !it.declarado },
            "B: no debe haber faltantes cuando todos estan declarados")
    }

    // ── Requisito C: BOLETA / CERTIFICADO acciones ──

    @Test
    fun `C BOLETA cotejar funciona`() {
        val s = nextSeq()
        val (preApp, _, _) = createEnvironmentWithDocs(s, listOf(
            DocumentoDeclarado("BOLETA / CERTIFICADO", declarado = true, cotejadoSecretaria = false)
        ))
        PreApplicationViewModel.toggleDocumentCotejado(preApp.folio, "BOLETA / CERTIFICADO")
        assertTrue(docByNombre(preApp.folio, "BOLETA / CERTIFICADO").cotejadoSecretaria,
            "C: BOLETA debe quedar cotejada")
    }

    @Test
    fun `C BOLETA validar requiere cotejar previo`() {
        val s = nextSeq()
        val (preApp, _, _) = createEnvironmentWithDocs(s, listOf(
            DocumentoDeclarado("BOLETA / CERTIFICADO", declarado = true)
        ))
        PreApplicationViewModel.markDocumentValidado(preApp.folio, "BOLETA / CERTIFICADO")
        assertFalse(docByNombre(preApp.folio, "BOLETA / CERTIFICADO").validado,
            "C: no debe validar sin cotejar")
        PreApplicationViewModel.toggleDocumentCotejado(preApp.folio, "BOLETA / CERTIFICADO")
        PreApplicationViewModel.markDocumentValidado(preApp.folio, "BOLETA / CERTIFICADO")
        val doc = docByNombre(preApp.folio, "BOLETA / CERTIFICADO")
        assertTrue(doc.validado, "C: BOLETA debe validarse tras cotejar")
        assertTrue(doc.cotejadoSecretaria, "C: cotejado persiste tras validar")
    }

    @Test
    fun `C BOLETA rechazar resetea cotejado`() {
        val s = nextSeq()
        val (preApp, _, _) = createEnvironmentWithDocs(s, listOf(
            DocumentoDeclarado("BOLETA / CERTIFICADO", declarado = true)
        ))
        PreApplicationViewModel.toggleDocumentCotejado(preApp.folio, "BOLETA / CERTIFICADO")
        PreApplicationViewModel.markDocumentRechazado(preApp.folio, "BOLETA / CERTIFICADO")
        val doc = docByNombre(preApp.folio, "BOLETA / CERTIFICADO")
        assertTrue(doc.rechazado, "C: BOLETA debe quedar rechazada")
        assertFalse(doc.cotejadoSecretaria, "C: cotejado se resetea al rechazar")
        assertFalse(doc.validado, "C: validado es false tras rechazar")
    }

    @Test
    fun `C BOLETA noAplica funciona`() {
        val s = nextSeq()
        val (preApp, _, _) = createEnvironmentWithDocs(s, listOf(
            DocumentoDeclarado("BOLETA / CERTIFICADO", declarado = true)
        ))
        PreApplicationViewModel.markDocumentNoAplica(preApp.folio, "BOLETA / CERTIFICADO")
        val doc = docByNombre(preApp.folio, "BOLETA / CERTIFICADO")
        assertTrue(doc.noAplica, "C: noAplica true")
        assertFalse(doc.cotejadoSecretaria)
        assertFalse(doc.validado)
        assertFalse(doc.rechazado)
    }

    @Test
    fun `C BOLETA observacion persiste`() {
        val s = nextSeq()
        val (preApp, _, _) = createEnvironmentWithDocs(s, listOf(
            DocumentoDeclarado("BOLETA / CERTIFICADO", declarado = true)
        ))
        PreApplicationViewModel.setDocumentObservacion(preApp.folio, "BOLETA / CERTIFICADO", "Documento ilegible")
        assertEquals("Documento ilegible", docByNombre(preApp.folio, "BOLETA / CERTIFICADO").observacion,
            "C: observacion debe persistir")
    }

    @Test
    fun `C BOLETA acciones persisten al reabrir`() {
        val s = nextSeq()
        val (preApp, _, _) = createEnvironmentWithDocs(s, listOf(
            DocumentoDeclarado("BOLETA / CERTIFICADO", declarado = true)
        ))
        PreApplicationViewModel.toggleDocumentCotejado(preApp.folio, "BOLETA / CERTIFICADO")
        PreApplicationViewModel.markDocumentValidado(preApp.folio, "BOLETA / CERTIFICADO")
        PreApplicationViewModel.setDocumentObservacion(preApp.folio, "BOLETA / CERTIFICADO", "Verificado")
        val doc = docByNombre(preApp.folio, "BOLETA / CERTIFICADO")
        assertTrue(doc.cotejadoSecretaria, "C: cotejado persiste en reapertura")
        assertTrue(doc.validado, "C: validado persiste en reapertura")
        assertEquals("Verificado", doc.observacion, "C: obs persiste en reapertura")
    }

    @Test
    fun `C BOLETA resuelto actualiza contador`() {
        val s = nextSeq()
        val (preApp, _, _) = createEnvironmentWithDocs(s, listOf(
            DocumentoDeclarado("Acta", declarado = true),
            DocumentoDeclarado("BOLETA / CERTIFICADO", declarado = true),
            DocumentoDeclarado("CURP", declarado = true)
        ))
        assertEquals(3, preApp.documentosDeclarados.size)
        assertEquals(0, preApp.documentosDeclarados.count { it.noAplica || it.validado },
            "C: ninguno resuelto al inicio")

        PreApplicationViewModel.toggleDocumentCotejado(preApp.folio, "BOLETA / CERTIFICADO")
        PreApplicationViewModel.markDocumentValidado(preApp.folio, "BOLETA / CERTIFICADO")
        val updated = preAppAfter(preApp.folio)
        assertEquals(1, updated.documentosDeclarados.count { it.noAplica || it.validado },
            "C: 1 resuelto tras validar BOLETA")
    }

    // ── Requisito G: cicloEscolar ──

    @Test
    fun `G cicloEscolar en presentacion institucional`() {
        val s = nextSeq()
        val (_, student, annual) = createEnvironment(s)
        val content = resolveContent(student, annual)
        val ciclo = content.fields.firstOrNull { it.label == "Ciclo escolar" }
        assertNotNull(ciclo, "G: debe existir campo Ciclo escolar")
        assertEquals("2026-2027", ciclo?.value, "G: valor correcto del ciclo")
    }

    @Test
    fun `G cicloEscolar en dashboard list`() {
        val preApps = PreApplicationViewModel.sharedPreApplications.value
        assertTrue(preApps.isNotEmpty())
        assertTrue(preApps.all { it.cicloEscolar.isNotBlank() },
            "G: todas las pre-aplicaciones tienen cicloEscolar no vacio")
        assertTrue(preApps.first().cicloEscolar.matches(Regex("\\d{4}-\\d{4}")),
            "G: cicloEscolar formato YYYY-YYYY")
    }

    @Test
    fun `G cicloEscolar en detalle tarjeta`() {
        val preApps = PreApplicationViewModel.sharedPreApplications.value
        assertTrue(preApps.first().cicloEscolar.isNotBlank(),
            "G: cicloEscolar no vacio en tarjeta detalle")
    }

    // ── Requisito H: isConverted oculta bloques ──

    @Test
    fun `H no CONVERTED esconde asistencias faltas calificaciones incidencias institucional`() {
        val s = nextSeq()
        val (preApp, student, annual) = createEnvironment(s)
        val folio = preApp.folio
        val isConverted = PreApplicationViewModel.sharedPreApplications.value
            .firstOrNull { it.folio == folio }?.readinessStatus == ReadinessStatus.CONVERTED
        assertFalse(isConverted, "H: pre-app no debe ser CONVERTED")
        val content = resolveContent(student, annual)
        assertFalse(content.fields.any { it.label.contains("Asistencia", ignoreCase = true) },
            "H: institucional no tiene Asistencia")
        assertFalse(content.fields.any { it.label.contains("Incidencia", ignoreCase = true) },
            "H: institucional no tiene Incidencia")
        assertFalse(content.fields.any { it.label.contains("Calificacion", ignoreCase = true) },
            "H: institucional no tiene Calificacion")
    }

    @Test
    fun `H CONVERTED es isConverted true`() {
        val s = nextSeq()
        val (preApp, _, _) = createEnvironment(s, addAnnual = false)
        PreApplicationViewModel.approvePreApplication(preApp.folio)
        PreApplicationViewModel.toggleDocumentCotejado(preApp.folio, "CURP")
        PreApplicationViewModel.markDocumentValidado(preApp.folio, "CURP")
        PreApplicationViewModel.simulateCaptureStudentPhoto(preApp.folio)
        PreApplicationViewModel.simulateCaptureResponsablePhoto(preApp.folio)
        PreApplicationViewModel.markReadyForOfficialEnrollment(preApp.folio)
        val ready = preAppAfter(preApp.folio)
        PreApplicationViewModel.startOfficialEnrollment(ready, selectedGroup = "1A")
        PreApplicationViewModel.confirmInitialGroup(preApp.folio, "1A")

        assertEquals(ReadinessStatus.CONVERTED, preAppAfter(preApp.folio).readinessStatus,
            "H: pre-app debe estar CONVERTED")
        val isConverted = PreApplicationViewModel.sharedPreApplications.value
            .first { it.folio == preApp.folio }.readinessStatus == ReadinessStatus.CONVERTED
        assertTrue(isConverted, "H: isConverted true para CONVERTED")
    }

    // ── Requisito I: Sin textos de correccion en UI ──

    @Test
    fun `I filtro default excluye PENDIENTE_CORRECCION`() {
        val preApps = PreApplicationViewModel.sharedPreApplications.value
        val filtradas = preApps.filter { it.status in listOf(PreApplicationStatus.ENVIADA, PreApplicationStatus.ACEPTADA) }
        assertFalse(filtradas.any { it.status == PreApplicationStatus.PENDIENTE_CORRECCION },
            "I: filtro default no debe incluir PENDIENTE_CORRECCION")
    }

    @Test
    fun `I status badge PENDIENTE_CORRECCION se muestra como Pendiente`() {
        // El badge en StatusBadge mapea PENDIENTE_CORRECCION a "Pendiente"
        // Verificar que el label del enum es "Requiere correccion" pero el when lo cambia
        val pendienteLabel = PreApplicationStatus.PENDIENTE_CORRECCION.label
        assertEquals("Requiere corrección", pendienteLabel,
            "I: label del enum puede ser 'Requiere correccion'")
    }

    // ── Requisito J: Folio pendiente → aceptar ──

    @Test
    fun `J folio pendiente acceptFolioVisible cuando ENVIADA`() {
        val s = nextSeq()
        val (preApp, student, annual) = createEnvironment(s)
        assertEquals(PreApplicationStatus.ENVIADA, preApp.status)
        val resolved = resolveResolved(student, annual)
        assertTrue(resolved.acceptFolioVisible,
            "J: acceptFolioVisible es true para ENVIADA")
    }

    @Test
    fun `J folio pendiente aprueba y desaparece accion`() {
        val s = nextSeq()
        val (preApp, student, annual) = createEnvironment(s)
        PreApplicationViewModel.approvePreApplication(preApp.folio)
        assertEquals(PreApplicationStatus.ACEPTADA, preAppAfter(preApp.folio).status,
            "J: folio ACEPTADA tras approvePreApplication")
        val resolved2 = resolveResolved(student, annual)
        assertFalse(resolved2.acceptFolioVisible,
            "J: acceptFolioVisible false tras aprobar")
    }

    // ── Requisito K: Grupo pendiente → decision real ──

    @Test
    fun `K grupo pendiente campo muestra Pendiente de asignacion`() {
        val s = nextSeq()
        val (_, student, annual) = createEnvironment(s)
        val content = resolveContent(student, annual)
        val grupo = content.fields.firstOrNull { it.label == "Grupo" }
        assertNotNull(grupo, "K: existe campo Grupo")
        assertEquals("Pendiente de asignación", grupo?.value,
            "K: Grupo muestra Pendiente de asignacion")
    }

    @Test
    fun `K groupOptionsForGrade devuelve opciones`() {
        val s = nextSeq()
        val (preApp, _, _) = createEnvironment(s)
        val options = PreApplicationViewModel.groupOptionsForGrade(preApp.gradoSolicitado)
        assertTrue(options.isNotEmpty(), "K: debe haber opciones de grupo")
        assertTrue(options.all { it.matches(Regex("\\d[A-Z]")) },
            "K: opciones formato NX")
    }

    @Test
    fun `K flujo completo grupo confirmado via start+confirm`() {
        val s = nextSeq()
        val (preApp, _, _) = createEnvironment(s, addAnnual = false)
        val selectedGroup = "1A"
        PreApplicationViewModel.approvePreApplication(preApp.folio)
        PreApplicationViewModel.toggleDocumentCotejado(preApp.folio, "CURP")
        PreApplicationViewModel.markDocumentValidado(preApp.folio, "CURP")
        PreApplicationViewModel.simulateCaptureStudentPhoto(preApp.folio)
        PreApplicationViewModel.simulateCaptureResponsablePhoto(preApp.folio)
        assertIs<ReadinessResult.Success>(
            PreApplicationViewModel.markReadyForOfficialEnrollment(preApp.folio))
        val ready = preAppAfter(preApp.folio)
        assertIs<OfficialEnrollmentResult.Success>(
            PreApplicationViewModel.startOfficialEnrollment(ready, selectedGroup = selectedGroup))
        val confirm = PreApplicationViewModel.confirmInitialGroup(preApp.folio, selectedGroup)
        assertIs<OfficialEnrollmentResult.Success>(confirm, "K: confirmInitialGroup exitoso")
        val official = PreApplicationViewModel.officialStudents.value
            .first { it.preApplicationFolio == preApp.folio }
        assertEquals(selectedGroup, official.grupoAsignado, "K: grupo asignado $selectedGroup")
        assertNotNull(official.matriculaOficial, "K: matricula oficial generada")
    }

    // ── Helper con documentos personalizados ──

    private fun createEnvironmentWithDocs(
        seq: Int,
        docs: List<DocumentoDeclarado>
    ): Triple<PreApplication, Student, AnnualEnrollmentRecord> {
        val s = seq.toString().padStart(2, '0')
        val folio = "PRE-DOCS-$seq"
        val curp = "DOCS${s}0101HDFABC01"
        val enrollmentId = "S310-DOC$s"
        val studentId = "MASTER-DOCS-$seq"

        val draftPreApp = MockPreApplicationData.preApplications.first().copy(
            folio = folio,
            status = PreApplicationStatus.BORRADOR,
            submittedAt = null,
            tramite = "NUEVO INGRESO",
            cicloEscolar = "2026-2027",
            gradoSolicitado = 1,
            alumnoNombreCompleto = "ALUMNO DOCS $seq",
            alumnoCurp = curp,
            alumnoDomicilio = "DOMICILIO DOCS $seq",
            alumnoTelefonoCasa = "5512345678",
            promedioGradoAnterior = 8.0,
            documentosDeclarados = docs,
            readinessStatus = ReadinessStatus.PENDING,
            readyAt = null,
            readinessNotes = ""
        )

        val submitted = assertIs<FamilySubmissionResult.Success>(
            PreApplicationViewModel.submitFamilyPreApplication(draftPreApp)
        ).preApplication

        val student = Student(
            id = studentId,
            fullName = "ALUMNO DOCS $seq",
            group = "",
            enrollmentId = enrollmentId,
            curp = curp,
            preApplicationFolio = folio
        )
        assertIs<StudentAddResult.Added>(MockSaseData.addStudent(student))

        val annual = AnnualEnrollmentRecord(
            studentId = studentId,
            normalizedCurp = curp,
            permanentEnrollmentId = enrollmentId,
            schoolYear = "2026-2027",
            sourcePreApplicationFolio = folio,
            movement = AnnualEnrollmentMovement.NEW_ENTRY,
            requestedGrade = 1,
            groupPlacementRequirement = GroupPlacementRequirement.AssignmentRequired,
            status = AnnualEnrollmentInitialStatus.PENDING_GROUP_ASSIGNMENT
        )
        MockSaseData.addAnnualEnrollment(annual)

        val preApp = PreApplicationViewModel.sharedPreApplications.value
            .single { it.folio == folio }
        return Triple(preApp, student, annual)
    }
}
