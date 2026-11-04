package com.example.data.attendance

import com.example.data.auth.AuthSession
import com.example.data.auth.InstitutionalRoleAssignment
import com.example.data.auth.InstitutionalSession
import com.example.data.auth.StaffProfile
import com.example.data.auth.StaffRole
import com.example.data.enrollment.DepartureCategory
import com.example.data.enrollment.SchoolMovementRecord
import com.example.data.enrollment.SchoolMovementType
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AttendanceDomainAndEvaluatorTest {

    private fun createTeacherSession(
        membershipId: String = MockAttendanceData.TEACHER_1A_MEMBERSHIP_ID,
        role: StaffRole = StaffRole.DOCENTE
    ): AuthSession {
        return AuthSession(
            profile = StaffProfile(
                id = "profile-$membershipId",
                email = "docente@example.invalid",
                fullName = "Docente Titular Demo",
                active = true
            ),
            institutional = InstitutionalSession.create(
                userId = "user-$membershipId",
                profileId = "profile-$membershipId",
                membershipId = membershipId,
                institutionId = "INST-001",
                institutionName = "Institución Ficticia",
                roleAssignments = listOf(
                    InstitutionalRoleAssignment(roleId = "role-${role.name.lowercase()}", role = role)
                ),
                activeRoleId = "role-${role.name.lowercase()}",
                schoolCycleId = "cycle-demo",
                sessionStartedAt = 1000L,
                expiresAt = 10000L
            ),
            accessToken = "token-test"
        )
    }

    @Test
    fun testModulosDoblesEnMismaFecha() = runBlocking {
        val repo = MockAttendanceRepositoryImpl()
        val session = createTeacherSession()
        val date = "2026-10-15"

        // Programar Módulo 1
        val resM1 = repo.scheduleOrOpenSession(session, MockAttendanceData.GROUP_1A_ID, date, moduleNumber = 1)
        val snapM1 = assertIs<AttendanceResult.Success<ClassAttendanceSnapshot>>(resM1).value
        assertEquals(1, snapM1.moduleNumber)
        assertEquals("session-${MockAttendanceData.GROUP_1A_ID}-$date-m1", snapM1.sessionId)

        // Programar Módulo 2 en la misma fecha
        val resM2 = repo.scheduleOrOpenSession(session, MockAttendanceData.GROUP_1A_ID, date, moduleNumber = 2)
        val snapM2 = assertIs<AttendanceResult.Success<ClassAttendanceSnapshot>>(resM2).value
        assertEquals(2, snapM2.moduleNumber)
        assertEquals("session-${MockAttendanceData.GROUP_1A_ID}-$date-m2", snapM2.sessionId)

        // Ambos módulos coexisten independientemente
        assertTrue(snapM1.sessionId != snapM2.sessionId)
    }

    @Test
    fun testSesionCanceladaONoLectivaNoComputaInasistencias() = runBlocking {
        val repo = MockAttendanceRepositoryImpl()
        val session = createTeacherSession()

        // 1 sesión normal realizada
        repo.scheduleOrOpenSession(session, MockAttendanceData.GROUP_1A_ID, "2026-10-01", 1)
        repo.saveAttendance(
            session,
            "session-${MockAttendanceData.GROUP_1A_ID}-2026-10-01-m1",
            listOf(
                AttendanceRecordEntry("1", AttendancePresence.PRESENTE),
                AttendanceRecordEntry("2", AttendancePresence.AUSENTE)
            )
        )

        // 1 sesión cancelada por contingencia
        val resCanc = repo.scheduleOrOpenSession(
            session, MockAttendanceData.GROUP_1A_ID, "2026-10-02", 1,
            status = SessionStatus.CANCELADA,
            notes = "Suspensión preventiva por lluvia torrencial"
        )
        assertIs<AttendanceResult.Success<ClassAttendanceSnapshot>>(resCanc)

        // 1 sesión no lectiva por Consejo Técnico Escolar
        val resCte = repo.scheduleOrOpenSession(
            session, MockAttendanceData.GROUP_1A_ID, "2026-10-03", 1,
            status = SessionStatus.NO_LECTIVA,
            notes = "Sesión Ordinaria CTE"
        )
        assertIs<AttendanceResult.Success<ClassAttendanceSnapshot>>(resCte)

        // Consultar reporte del 01 al 05 de octubre
        val resReport = repo.getGroupAttendanceReport(session, MockAttendanceData.GROUP_1A_ID, "2026-10-01", "2026-10-05")
        val report = assertIs<AttendanceResult.Success<ClassAttendanceReport>>(resReport).value

        assertEquals(3, report.totalSessionsScheduled)
        assertEquals(1, report.sessionsHeld)
        assertEquals(1, report.sessionsCancelled)
        assertEquals(1, report.sessionsNonTeaching)

        // El alumno 2 solo tiene 1 inasistencia (la de la sesión realizada), las canceladas no computan falta
        val student2 = report.studentMetrics.first { it.studentId == "2" }
        assertEquals(1, student2.applicableSessions)
        assertEquals(0, student2.attendedSessions)
        assertEquals(1, student2.absentSessions)
    }

    @Test
    fun testAltaExtemporaneaNoTieneSesionesPreviasAplicables() = runBlocking {
        // Alumno 3 ingresa como alta extemporánea el 15 de octubre
        val movements = listOf(
            SchoolMovementRecord(
                id = "mov-alta-03",
                studentId = "3",
                movementType = SchoolMovementType.ALTA_EXTEMPORANEA,
                effectiveDate = "2026-10-15",
                toGroup = "1° A"
            )
        )
        val repo = MockAttendanceRepositoryImpl(movementsProvider = { movements })
        val session = createTeacherSession()

        // Sesión 1: 10 de octubre (previa a su ingreso)
        repo.scheduleOrOpenSession(session, MockAttendanceData.GROUP_1A_ID, "2026-10-10", 1)
        repo.saveAttendance(
            session,
            "session-${MockAttendanceData.GROUP_1A_ID}-2026-10-10-m1",
            listOf(AttendanceRecordEntry("1", AttendancePresence.PRESENTE))
        )

        // Sesión 2: 20 de octubre (posterior a su ingreso)
        repo.scheduleOrOpenSession(session, MockAttendanceData.GROUP_1A_ID, "2026-10-20", 1)
        repo.saveAttendance(
            session,
            "session-${MockAttendanceData.GROUP_1A_ID}-2026-10-20-m1",
            listOf(
                AttendanceRecordEntry("1", AttendancePresence.PRESENTE),
                AttendanceRecordEntry("3", AttendancePresence.PRESENTE)
            )
        )

        val resReport = repo.getGroupAttendanceReport(session, MockAttendanceData.GROUP_1A_ID, "2026-10-01", "2026-10-31")
        val report = assertIs<AttendanceResult.Success<ClassAttendanceReport>>(resReport).value

        val student3 = report.studentMetrics.first { it.studentId == "3" }
        // Para el alumno 3, solo la sesión del 20 de octubre es aplicable (1 sesión)
        assertEquals(1, student3.applicableSessions)
        assertEquals(1, student3.attendedSessions)
        assertEquals(0, student3.absentSessions)
        assertEquals(100.0, student3.attendanceRatePct)
    }

    @Test
    fun testTrasladoSalienteNoTieneSesionesPosterioresAplicables() = runBlocking {
        // Alumno 4 inscrito al inicio de ciclo y se traslada el 15 de octubre
        val movements = listOf(
            SchoolMovementRecord(
                id = "mov-inscr-04",
                studentId = "4",
                movementType = SchoolMovementType.INSCRIPCION_INICIAL,
                effectiveDate = "2026-08-28",
                toGroup = "1° A"
            ),
            SchoolMovementRecord(
                id = "mov-traslado-04",
                studentId = "4",
                movementType = SchoolMovementType.TRASLADO_SALIENTE,
                effectiveDate = "2026-10-15",
                fromGroup = "1° A",
                destinationSchool = "EST 45",
                departureCategory = DepartureCategory.TRASLADO
            )
        )
        val repo = MockAttendanceRepositoryImpl(movementsProvider = { movements })
        val session = createTeacherSession()

        // Sesión 1: 10 de octubre (cuando estaba vigente)
        repo.scheduleOrOpenSession(session, MockAttendanceData.GROUP_1A_ID, "2026-10-10", 1)
        repo.saveAttendance(
            session,
            "session-${MockAttendanceData.GROUP_1A_ID}-2026-10-10-m1",
            listOf(AttendanceRecordEntry("4", AttendancePresence.PRESENTE))
        )

        // Sesión 2: 20 de octubre (posterior al traslado)
        repo.scheduleOrOpenSession(session, MockAttendanceData.GROUP_1A_ID, "2026-10-20", 1)
        repo.saveAttendance(
            session,
            "session-${MockAttendanceData.GROUP_1A_ID}-2026-10-20-m1",
            listOf(AttendanceRecordEntry("1", AttendancePresence.PRESENTE))
        )

        val resReport = repo.getGroupAttendanceReport(session, MockAttendanceData.GROUP_1A_ID, "2026-10-01", "2026-10-31")
        val report = assertIs<AttendanceResult.Success<ClassAttendanceReport>>(resReport).value

        val student4 = report.studentMetrics.first { it.studentId == "4" }
        // Para el alumno 4, solo la sesión del 10 de octubre es aplicable (1 sesión)
        assertEquals(1, student4.applicableSessions)
        assertEquals(1, student4.attendedSessions)
        assertEquals(0, student4.absentSessions)
        assertEquals(100.0, student4.attendanceRatePct)
    }

    @Test
    fun testCapturaParcialNoConviertePendientesEnFaltas() = runBlocking {
        val repo = MockAttendanceRepositoryImpl()
        val session = createTeacherSession()

        val snapRes = repo.scheduleOrOpenSession(session, MockAttendanceData.GROUP_1A_ID, "2026-10-15", 1)
        val snap = assertIs<AttendanceResult.Success<ClassAttendanceSnapshot>>(snapRes).value

        // Guardar captura únicamente para 2 de los 5 alumnos
        val partialEntries = listOf(
            AttendanceRecordEntry("1", AttendancePresence.PRESENTE),
            AttendanceRecordEntry("2", AttendancePresence.PRESENTE)
        )
        val saveRes = repo.saveAttendance(session, snap.sessionId, partialEntries)
        val updatedSnap = assertIs<AttendanceResult.Success<ClassAttendanceSnapshot>>(saveRes).value

        assertEquals(CaptureState.PARCIAL, updatedSnap.captureState)
        assertEquals(2, updatedSnap.presentCount)
        assertEquals(0, updatedSnap.absentCount)
        assertEquals(3, updatedSnap.pendingCount)

        // Al consultar el reporte, los alumnos 3, 4 y 5 tienen pendingSessions = 1 y absentSessions = 0
        val resReport = repo.getGroupAttendanceReport(session, MockAttendanceData.GROUP_1A_ID, "2026-10-01", "2026-10-31")
        val report = assertIs<AttendanceResult.Success<ClassAttendanceReport>>(resReport).value

        val student3 = report.studentMetrics.first { it.studentId == "3" }
        assertEquals(0, student3.absentSessions)
        assertEquals(1, student3.pendingSessions)
        assertFalse(student3.hasAttendanceAlert)
    }

    @Test
    fun testLlegadaTardiaYConteoDiferenciadoDeRetardos() = runBlocking {
        val repo = MockAttendanceRepositoryImpl()
        val session = createTeacherSession()

        // Sesión 1: Alumno 1 llega con retardo de 10 min
        repo.scheduleOrOpenSession(session, MockAttendanceData.GROUP_1A_ID, "2026-10-15", 1)
        repo.saveAttendance(
            session,
            "session-${MockAttendanceData.GROUP_1A_ID}-2026-10-15-m1",
            listOf(
                AttendanceRecordEntry("1", AttendancePresence.PRESENTE, PunctualityStatus.RETARDO, minutesLate = 10),
                AttendanceRecordEntry("2", AttendancePresence.PRESENTE, PunctualityStatus.A_TIEMPO)
            )
        )

        // Sesión 2: Alumno 1 vuelve a llegar con retardo de 15 min; Alumno 2 llega a tiempo
        repo.scheduleOrOpenSession(session, MockAttendanceData.GROUP_1A_ID, "2026-10-16", 1)
        repo.saveAttendance(
            session,
            "session-${MockAttendanceData.GROUP_1A_ID}-2026-10-16-m1",
            listOf(
                AttendanceRecordEntry("1", AttendancePresence.PRESENTE, PunctualityStatus.RETARDO, minutesLate = 15),
                AttendanceRecordEntry("2", AttendancePresence.PRESENTE, PunctualityStatus.A_TIEMPO)
            )
        )

        val resReport = repo.getGroupAttendanceReport(session, MockAttendanceData.GROUP_1A_ID, "2026-10-01", "2026-10-31")
        val report = assertIs<AttendanceResult.Success<ClassAttendanceReport>>(resReport).value

        // Conteo diferenciado:
        // totalTardinessEvents = 2 (dos llegadas tarde en total)
        // distinctStudentsWithTardiness = 1 (sólo el Alumno 1 tuvo retardos)
        assertEquals(2, report.totalTardinessEvents)
        assertEquals(1, report.distinctStudentsWithTardiness)

        val student1 = report.studentMetrics.first { it.studentId == "1" }
        assertEquals(2, student1.tardinessEvents)
        assertEquals(2, student1.attendedSessions)
        assertEquals(0, student1.absentSessions)
    }

    @Test
    fun testReaperturaYCorreccionDeAsistencia() = runBlocking {
        val repo = MockAttendanceRepositoryImpl()
        val session = createTeacherSession()

        // Captura inicial: Alumno 1 AUSENTE
        repo.scheduleOrOpenSession(session, MockAttendanceData.GROUP_1A_ID, "2026-10-15", 1)
        repo.saveAttendance(
            session,
            "session-${MockAttendanceData.GROUP_1A_ID}-2026-10-15-m1",
            listOf(AttendanceRecordEntry("1", AttendancePresence.AUSENTE))
        )

        // Reapertura
        val reopenRes = repo.scheduleOrOpenSession(session, MockAttendanceData.GROUP_1A_ID, "2026-10-15", 1)
        val snapReopen = assertIs<AttendanceResult.Success<ClassAttendanceSnapshot>>(reopenRes).value
        val entry1 = snapReopen.entries.first { it.student.id == "1" }
        assertEquals(AttendancePresence.AUSENTE, entry1.presence)

        // Corrección: el alumno justificó y se corrige a PRESENTE con retardo
        val correctRes = repo.saveAttendance(
            session,
            snapReopen.sessionId,
            listOf(
                AttendanceRecordEntry(
                    studentId = "1",
                    presence = AttendancePresence.PRESENTE,
                    punctuality = PunctualityStatus.RETARDO,
                    minutesLate = 12,
                    justification = "Justificante médico de prefectura"
                )
            )
        )
        val snapCorrected = assertIs<AttendanceResult.Success<ClassAttendanceSnapshot>>(correctRes).value
        val correctedEntry1 = snapCorrected.entries.first { it.student.id == "1" }
        assertEquals(AttendancePresence.PRESENTE, correctedEntry1.presence)
        assertEquals(PunctualityStatus.RETARDO, correctedEntry1.punctuality)
        assertEquals(12, correctedEntry1.minutesLate)
        assertEquals("Justificante médico de prefectura", correctedEntry1.justification)
    }

    @Test
    fun testSeparacionDeCsvAgregadoYNominalRestringido() = runBlocking {
        val repo = MockAttendanceRepositoryImpl()
        val session = createTeacherSession()

        repo.scheduleOrOpenSession(session, MockAttendanceData.GROUP_1A_ID, "2026-10-15", 1)
        repo.saveAttendance(
            session,
            "session-${MockAttendanceData.GROUP_1A_ID}-2026-10-15-m1",
            listOf(
                AttendanceRecordEntry("1", AttendancePresence.PRESENTE),
                AttendanceRecordEntry("2", AttendancePresence.AUSENTE)
            )
        )

        val resReport = repo.getGroupAttendanceReport(session, MockAttendanceData.GROUP_1A_ID, "2026-10-01", "2026-10-31")
        val report = assertIs<AttendanceResult.Success<ClassAttendanceReport>>(resReport).value

        // 1. Reporte Agregado PMC
        val pmcCsv = AttendanceExportHelper.generatePmcAggregatedCsv(report)
        assertFalse(pmcCsv.contains("ALUMNO DEMO 01"), "El reporte agregado PMC no debe exponer nombres")
        assertFalse(pmcCsv.contains("ALUMNO DEMO 02"), "El reporte agregado PMC no debe exponer nombres")
        assertTrue(pmcCsv.contains("REPORTE INSTITUCIONAL PMC"))
        assertTrue(pmcCsv.contains("Tasa de Asistencia Colectiva"))
        assertTrue(pmcCsv.contains("Pendiente de aprobación formal por Consejo Técnico Escolar"))

        // 2. Padrón Nominal Restringido
        val nominalCsv = AttendanceExportHelper.generateRestrictedNominalCsv(report)
        assertTrue(nominalCsv.contains("ALUMNO DEMO 01"), "El padrón nominal restringido sí debe listar alumnos")
        assertTrue(nominalCsv.contains("PADRON NOMINAL RESTRINGIDO"))
        assertTrue(nominalCsv.contains("CONFIDENCIAL - USO INTERNO EXCLUSIVO"))
    }

    @Test
    fun testCambioDeGrupoRespetaPertenenciaAntesYDespuesDeFechaEfectivaSinDuplicarSesiones() = runBlocking {
        // Alumno 1 inicia en 1-A y el 2026-10-01 cambia a 2-B
        val moves = listOf(
            SchoolMovementRecord(
                id = "MOV-INI-01",
                studentId = "1",
                movementType = SchoolMovementType.INSCRIPCION_INICIAL,
                effectiveDate = "2026-08-28",
                recordedAt = "2026-08-28T09:00:00Z",
                recordedBy = "SECRETARIA",
                toGroup = "1-A"
            ),
            SchoolMovementRecord(
                id = "MOV-CHG-01",
                studentId = "1",
                movementType = SchoolMovementType.CAMBIO_DE_GRUPO,
                effectiveDate = "2026-10-01",
                recordedAt = "2026-09-29T10:00:00Z",
                recordedBy = "DIRECCION",
                fromGroup = "1-A",
                toGroup = "2-B",
                classificationCriteria = "Reasignación académica solicitada por tutor"
            )
        )

        val repo = MockAttendanceRepositoryImpl(movementsProvider = { moves })
        val teacher1ASession = createTeacherSession(MockAttendanceData.TEACHER_1A_MEMBERSHIP_ID)
        val teacher2BSession = createTeacherSession(MockAttendanceData.TEACHER_2B_MEMBERSHIP_ID)

        // 1. Sesión en Grupo 1-A ANTES del cambio de grupo (2026-09-15)
        repo.scheduleOrOpenSession(teacher1ASession, MockAttendanceData.GROUP_1A_ID, "2026-09-15", 1)
        repo.saveAttendance(
            teacher1ASession,
            "session-${MockAttendanceData.GROUP_1A_ID}-2026-09-15-m1",
            listOf(AttendanceRecordEntry(studentId = "1", presence = AttendancePresence.PRESENTE))
        )

        // 2. Sesión en Grupo 1-A DESPUÉS del cambio de grupo (2026-10-10)
        repo.scheduleOrOpenSession(teacher1ASession, MockAttendanceData.GROUP_1A_ID, "2026-10-10", 1)
        val snap1AOct = repo.scheduleOrOpenSession(teacher1ASession, MockAttendanceData.GROUP_1A_ID, "2026-10-10", 1)
        val roster1AOct = assertIs<AttendanceResult.Success<ClassAttendanceSnapshot>>(snap1AOct).value.entries
        assertFalse(
            roster1AOct.any { it.student.id == "1" },
            "El alumno 1 no debe aparecer en la lista de 1-A después de su fecha efectiva de cambio"
        )

        // 3. Sesión en Grupo 2-B DESPUÉS del cambio de grupo (2026-10-10)
        val snap2BOct = repo.scheduleOrOpenSession(teacher2BSession, MockAttendanceData.GROUP_2B_ID, "2026-10-10", 1)
        val roster2BOct = assertIs<AttendanceResult.Success<ClassAttendanceSnapshot>>(snap2BOct).value.entries
        assertTrue(
            roster2BOct.any { it.student.id == "1" },
            "El alumno 1 debe figurar activo en el Grupo 2-B en fecha posterior a su cambio"
        )
        repo.saveAttendance(
            teacher2BSession,
            "session-${MockAttendanceData.GROUP_2B_ID}-2026-10-10-m1",
            listOf(AttendanceRecordEntry(studentId = "1", presence = AttendancePresence.PRESENTE))
        )

        // 4. Reporte de 1-A (septiembre a octubre): el alumno solo tiene 1 sesión aplicable en 1-A (la de septiembre)
        val rep1ARes = repo.getGroupAttendanceReport(teacher1ASession, MockAttendanceData.GROUP_1A_ID, "2026-09-01", "2026-10-31")
        val rep1A = assertIs<AttendanceResult.Success<ClassAttendanceReport>>(rep1ARes).value
        val metricStudent1In1A = rep1A.studentMetrics.first { it.studentId == "1" }
        assertEquals(1, metricStudent1In1A.applicableSessions, "En 1-A solo le aplica la sesión anterior al cambio")
        assertEquals(1, metricStudent1In1A.attendedSessions)
        assertEquals(0, metricStudent1In1A.absentSessions)

        // 5. Reporte de 2-B (septiembre a octubre): el alumno solo tiene 1 sesión aplicable en 2-B (la de octubre)
        val rep2BRes = repo.getGroupAttendanceReport(teacher2BSession, MockAttendanceData.GROUP_2B_ID, "2026-09-01", "2026-10-31")
        val rep2B = assertIs<AttendanceResult.Success<ClassAttendanceReport>>(rep2BRes).value
        val metricStudent1In2B = rep2B.studentMetrics.first { it.studentId == "1" }
        assertEquals(1, metricStudent1In2B.applicableSessions, "En 2-B solo le aplica la sesión posterior al cambio")
        assertEquals(1, metricStudent1In2B.attendedSessions)
        assertEquals(0, metricStudent1In2B.absentSessions)

        // Total consolidado: 2 sesiones distintas aplicadas (1 en cada grupo), CERO duplicidades
        assertEquals(2, metricStudent1In1A.applicableSessions + metricStudent1In2B.applicableSessions)
    }

    @Test
    fun testPorcentajeConCapturasIncompletasMuestraCoberturaYNoEsDefinitivo() = runBlocking {
        val repo = MockAttendanceRepositoryImpl()
        val session = createTeacherSession()

        // 2 sesiones lectivas en Grupo 1-A (5 alumnos en lista = 10 oportunidades aplicables en total)
        repo.scheduleOrOpenSession(session, MockAttendanceData.GROUP_1A_ID, "2026-10-01", 1)
        repo.scheduleOrOpenSession(session, MockAttendanceData.GROUP_1A_ID, "2026-10-02", 1)

        // En la sesión 1, solo se capturan 3 de los 5 alumnos (captura parcial)
        repo.saveAttendance(
            session,
            "session-${MockAttendanceData.GROUP_1A_ID}-2026-10-01-m1",
            listOf(
                AttendanceRecordEntry("1", AttendancePresence.PRESENTE),
                AttendanceRecordEntry("2", AttendancePresence.PRESENTE),
                AttendanceRecordEntry("3", AttendancePresence.AUSENTE)
            )
        )
        // En la sesión 2, no se captura a nadie (sesión pendiente de captura)

        val repRes = repo.getGroupAttendanceReport(session, MockAttendanceData.GROUP_1A_ID, "2026-10-01", "2026-10-02")
        val report = assertIs<AttendanceResult.Success<ClassAttendanceReport>>(repRes).value

        // Oportunidades aplicables vs capturadas vs cobertura
        assertEquals(10, report.totalApplicableOpportunities, "5 alumnos x 2 sesiones = 10 oportunidades")
        assertEquals(3, report.totalCapturedRecords, "Solo 3 alumnos capturados en sesión 1")
        assertEquals(7, report.totalPendingRecords, "2 pendientes en sesión 1 + 5 pendientes en sesión 2 = 7")
        assertEquals(2, report.totalAttendanceEvents, "2 presentes")
        assertEquals(1, report.totalAbsenceEvents, "1 ausente")
        assertEquals(30.0, report.coveragePct, "3 capturados de 10 aplicables = 30% cobertura")
        assertFalse(report.isCompleteCoverage, "Con capturas pendientes la cobertura no es completa")
        assertTrue(report.rateStatusNotice.contains("Preliminar"), "El reporte debe presentarse explícitamente como preliminar")
        assertTrue(report.rateStatusNotice.contains("No definitiva"))

        // La tasa se calcula exclusivamente sobre los 3 decididos (2 presentes de 3 capturados = 66.6%)
        assertEquals(66.6, report.overallAttendanceRatePct)

        // Verificación individual: las capturas pendientes NO se imputan como inasistencia
        val student4 = report.studentMetrics.first { it.studentId == "4" }
        assertEquals(2, student4.applicableSessions)
        assertEquals(0, student4.attendedSessions)
        assertEquals(0, student4.absentSessions, "No debe tener faltas imputadas por falta de pase de lista")
        assertEquals(2, student4.pendingSessions)
        assertEquals(0, student4.capturedRecords)
        assertEquals(0.0, student4.coveragePct)
        assertFalse(student4.isDefinitive)
    }
}
