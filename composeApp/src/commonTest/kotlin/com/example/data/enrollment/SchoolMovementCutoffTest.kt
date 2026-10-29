package com.example.data.enrollment

import com.example.data.Student
import com.example.data.auth.StaffAction
import com.example.data.auth.StaffPermissions
import com.example.data.auth.StaffRole
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class SchoolMovementCutoffTest {

    private val student1 = Student(
        id = "STUDENT-001",
        fullName = "JUAN PEREZ LOPEZ",
        group = "1°A",
        enrollmentId = "S310-JUAP100101-26",
        curp = "JUAP100101HDFRRN01"
    )

    private val student2 = Student(
        id = "STUDENT-002",
        fullName = "MARIA RUIZ DIAZ",
        group = "1°A",
        enrollmentId = "S310-MARD100202-26",
        curp = "MARD100202MDFRRN02"
    )

    private val student3 = Student(
        id = "STUDENT-003",
        fullName = "CARLOS SOLANO VELA",
        group = "1°B",
        enrollmentId = "S310-CASV100303-26",
        curp = "CASV100303HDFRRN03"
    )

    private val student4 = Student(
        id = "STUDENT-004",
        fullName = "ANA TORRES MORA",
        group = "1°B",
        enrollmentId = "S310-ANTM100404-26",
        curp = "ANTM100404MDFRRN04"
    )

    private val testStudents = listOf(student1, student2, student3, student4)

    @Test
    fun testAltaYTrasladoDuranteElPeriodoNoComputaComoAbandono() = runBlocking {
        val movements = listOf(
            SchoolMovementRecord(
                id = "MOV-01",
                studentId = student1.id,
                curp = student1.curp,
                studentFullName = student1.fullName,
                movementType = SchoolMovementType.ALTA_INICIAL,
                effectiveDate = "2026-08-25",
                targetGroup = "1°A",
                registeredByStaffId = "STAFF-01",
                registeredByName = "Secretaría",
                timestamp = 1000L
            ),
            SchoolMovementRecord(
                id = "MOV-02",
                studentId = student2.id,
                curp = student2.curp,
                studentFullName = student2.fullName,
                movementType = SchoolMovementType.ALTA_INICIAL,
                effectiveDate = "2026-08-25",
                targetGroup = "1°A",
                registeredByStaffId = "STAFF-01",
                registeredByName = "Secretaría",
                timestamp = 1001L
            ),
            SchoolMovementRecord(
                id = "MOV-03",
                studentId = student2.id,
                curp = student2.curp,
                studentFullName = student2.fullName,
                movementType = SchoolMovementType.TRASLADO_SALIENTE,
                effectiveDate = "2026-10-15",
                sourceGroup = "1°A",
                destinationSchool = "Escuela Secundaria Técnica 45",
                folioOficio = "OF-2026-089",
                reason = "Cambio de domicilio familiar a otra alcaldía",
                registeredByStaffId = "STAFF-01",
                registeredByName = "Secretaría",
                timestamp = 2000L
            )
        )

        val repo = MockSchoolMovementRepositoryImpl(movements)

        // 1. Corte al 2026-10-01 (previo al traslado)
        val cutoffOct01 = repo.calculateCutoff("2026-10-01", listOf(student1, student2))
        assertEquals(2, cutoffOct01.activeEnrollmentCount, "Ambos deben estar activos al 1 de octubre")
        assertEquals(0, cutoffOct01.transfersOutCount, "No debe haber traslados antes del 15 de octubre")
        assertEquals(0, cutoffOct01.dropoutsCount, "No debe haber bajas")
        assertEquals(2, cutoffOct01.groupBreakdown["1°A"])

        // 2. Corte al 2026-10-18 (posterior al traslado)
        val cutoffOct18 = repo.calculateCutoff("2026-10-18", listOf(student1, student2))
        assertEquals(1, cutoffOct18.activeEnrollmentCount, "Solo el estudiante 1 debe continuar activo")
        assertEquals(1, cutoffOct18.transfersOutCount, "El estudiante 2 debe computar como traslado documentado")
        assertEquals(0, cutoffOct18.dropoutsCount, "Un traslado a otra escuela NO debe computar como deserción/baja")
        assertEquals(1, cutoffOct18.groupBreakdown["1°A"])

        val student2Status = cutoffOct18.records.first { it.studentId == student2.id }
        assertEquals(StudentCutoffStatusType.TRASLADO, student2Status.statusType)
        assertTrue(student2Status.destinationOrReason.contains("Técnica 45"))
    }

    @Test
    fun testCambioDeGrupoRespetaVigenciaTemporal() = runBlocking {
        val movements = listOf(
            SchoolMovementRecord(
                id = "MOV-10",
                studentId = student3.id,
                curp = student3.curp,
                studentFullName = student3.fullName,
                movementType = SchoolMovementType.ALTA_INICIAL,
                effectiveDate = "2026-08-25",
                targetGroup = "1°A",
                registeredByStaffId = "STAFF-01",
                registeredByName = "Secretaría",
                timestamp = 1000L
            ),
            SchoolMovementRecord(
                id = "MOV-11",
                studentId = student3.id,
                curp = student3.curp,
                studentFullName = student3.fullName,
                movementType = SchoolMovementType.CAMBIO_GRUPO,
                effectiveDate = "2026-10-10",
                sourceGroup = "1°A",
                targetGroup = "1°B",
                reason = "Ajuste de cupo y balance de turno",
                registeredByStaffId = "STAFF-01",
                registeredByName = "Secretaría",
                timestamp = 1500L
            )
        )

        val repo = MockSchoolMovementRepositoryImpl(movements)

        // Corte antes del cambio (2026-10-05)
        val snapshotBefore = repo.calculateCutoff("2026-10-05", listOf(student3))
        assertEquals(1, snapshotBefore.activeEnrollmentCount)
        assertEquals("1°A", snapshotBefore.records.first().assignedGroup, "Debe pertenecer a 1°A antes del 10 de octubre")
        assertEquals(1, snapshotBefore.groupBreakdown["1°A"])
        assertEquals(null, snapshotBefore.groupBreakdown["1°B"])

        // Corte después del cambio (2026-10-15)
        val snapshotAfter = repo.calculateCutoff("2026-10-15", listOf(student3))
        assertEquals(1, snapshotAfter.activeEnrollmentCount)
        assertEquals("1°B", snapshotAfter.records.first().assignedGroup, "Debe pertenecer a 1°B a partir del 10 de octubre")
        assertEquals(null, snapshotAfter.groupBreakdown["1°A"])
        assertEquals(1, snapshotAfter.groupBreakdown["1°B"])
    }

    @Test
    fun testFechaAnteriorAlAltaNoCuentaEnCorteNiEnDenominador() = runBlocking {
        val movements = listOf(
            SchoolMovementRecord(
                id = "MOV-20",
                studentId = student4.id,
                curp = student4.curp,
                studentFullName = student4.fullName,
                movementType = SchoolMovementType.ALTA_EXTEMPORANEA,
                effectiveDate = "2026-10-01",
                targetGroup = "1°B",
                reason = "Inscripción extemporánea por cambio de residencia",
                registeredByStaffId = "STAFF-01",
                registeredByName = "Secretaría",
                timestamp = 1000L
            )
        )

        val repo = MockSchoolMovementRepositoryImpl(movements)

        // Corte previo al ingreso (2026-09-15)
        val snapshotPrev = repo.calculateCutoff("2026-09-15", listOf(student4))
        assertEquals(0, snapshotPrev.activeEnrollmentCount, "No debe sumar a matrícula activa antes del 1 de octubre")
        assertEquals(1, snapshotPrev.unenteredCount, "Debe registrarse como pendiente de ingreso")
        assertEquals(0, snapshotPrev.dropoutsCount, "No debe considerarse baja")
        assertEquals(StudentCutoffStatusType.NO_INGRESADO, snapshotPrev.records.first().statusType)

        // Corte posterior al ingreso (2026-10-05)
        val snapshotPost = repo.calculateCutoff("2026-10-05", listOf(student4))
        assertEquals(1, snapshotPost.activeEnrollmentCount)
        assertEquals(StudentCutoffStatusType.ACTIVO, snapshotPost.records.first().statusType)
    }

    @Test
    fun testBajaDefinitivaComputaComoDesercionPeroNoComoTraslado() = runBlocking {
        val movements = listOf(
            SchoolMovementRecord(
                id = "MOV-30",
                studentId = student1.id,
                curp = student1.curp,
                studentFullName = student1.fullName,
                movementType = SchoolMovementType.ALTA_INICIAL,
                effectiveDate = "2026-08-25",
                targetGroup = "1°A",
                registeredByStaffId = "STAFF-01",
                registeredByName = "Secretaría",
                timestamp = 1000L
            ),
            SchoolMovementRecord(
                id = "MOV-31",
                studentId = student1.id,
                curp = student1.curp,
                studentFullName = student1.fullName,
                movementType = SchoolMovementType.BAJA_DEFINITIVA,
                effectiveDate = "2026-10-12",
                sourceGroup = "1°A",
                reason = "Abandono escolar documentado",
                registeredByStaffId = "STAFF-01",
                registeredByName = "Secretaría",
                timestamp = 2000L
            )
        )

        val repo = MockSchoolMovementRepositoryImpl(movements)
        val snapshot = repo.calculateCutoff("2026-10-18", listOf(student1))

        assertEquals(0, snapshot.activeEnrollmentCount)
        assertEquals(1, snapshot.dropoutsCount, "Debe registrarse en dropoutsCount")
        assertEquals(0, snapshot.transfersOutCount, "No debe computar en transfersOutCount")
        assertEquals(StudentCutoffStatusType.BAJA, snapshot.records.first().statusType)
    }

    @Test
    fun testExportacionCsvCoincideConSnapshot() = runBlocking {
        val movements = listOf(
            SchoolMovementRecord(
                id = "MOV-40",
                studentId = student1.id,
                curp = student1.curp,
                studentFullName = student1.fullName,
                movementType = SchoolMovementType.ALTA_INICIAL,
                effectiveDate = "2026-08-25",
                targetGroup = "1°A",
                registeredByStaffId = "STAFF-01",
                registeredByName = "Secretaría",
                timestamp = 1000L
            ),
            SchoolMovementRecord(
                id = "MOV-41",
                studentId = student2.id,
                curp = student2.curp,
                studentFullName = student2.fullName,
                movementType = SchoolMovementType.ALTA_INICIAL,
                effectiveDate = "2026-08-25",
                targetGroup = "1°A",
                registeredByStaffId = "STAFF-01",
                registeredByName = "Secretaría",
                timestamp = 1001L
            ),
            SchoolMovementRecord(
                id = "MOV-42",
                studentId = student2.id,
                curp = student2.curp,
                studentFullName = student2.fullName,
                movementType = SchoolMovementType.TRASLADO_SALIENTE,
                effectiveDate = "2026-10-15",
                sourceGroup = "1°A",
                destinationSchool = "Secundaria 15",
                reason = "Traslado",
                registeredByStaffId = "STAFF-01",
                registeredByName = "Secretaría",
                timestamp = 2000L
            )
        )

        val repo = MockSchoolMovementRepositoryImpl(movements)
        val snapshot = repo.calculateCutoff("2026-10-18", listOf(student1, student2))
        val csv = SchoolMovementExportHelper.generateCutoffCsv(snapshot, "Secretaría de Prueba")

        assertTrue(csv.contains("Fecha de corte: 2026-10-18"))
        assertTrue(csv.contains("Matricula activa al corte: 1"))
        assertTrue(csv.contains("Traslados a otro plantel (no abandono): 1"))
        assertTrue(csv.contains("Bajas definitivas (abandono documentado): 0"))
        assertTrue(csv.contains("JUAN PEREZ LOPEZ"))
        assertTrue(csv.contains("MARIA RUIZ DIAZ"))
        assertTrue(csv.contains("Secundaria 15"))
    }

    @Test
    fun testPermisosDeMovimientosRestringidosPorRol() {
        assertTrue(StaffPermissions.canPerform(StaffRole.SECRETARIA, StaffAction.RECORD_SCHOOL_MOVEMENT))
        assertTrue(StaffPermissions.canPerform(StaffRole.DIRECCION, StaffAction.RECORD_SCHOOL_MOVEMENT))

        assertFalse(StaffPermissions.canPerform(StaffRole.DOCENTE, StaffAction.RECORD_SCHOOL_MOVEMENT))
        assertFalse(StaffPermissions.canPerform(StaffRole.TRABAJO_SOCIAL, StaffAction.RECORD_SCHOOL_MOVEMENT))
        assertFalse(StaffPermissions.canPerform(StaffRole.MEDICO_ESCOLAR, StaffAction.RECORD_SCHOOL_MOVEMENT))
        assertFalse(StaffPermissions.canPerform(StaffRole.UDEII, StaffAction.RECORD_SCHOOL_MOVEMENT))
    }

    @Test
    fun testControlNegativoTrasladoSinEscuelaDestinoEsRechazado() {
        val error = assertFailsWith<IllegalArgumentException> {
            SchoolMovementRecord(
                id = "INV-01",
                studentId = "ST-01",
                curp = "CURP123456HDFRRN01",
                studentFullName = "Estudiante Inválido",
                movementType = SchoolMovementType.TRASLADO_SALIENTE,
                effectiveDate = "2026-10-15",
                sourceGroup = "1°A",
                destinationSchool = null, // Inválido: omite escuela destino
                registeredByStaffId = "STAFF-01",
                registeredByName = "Secretaría",
                timestamp = 1000L
            )
        }
        assertTrue(error.message!!.contains("exige documentar la escuela de destino"))
    }

    @Test
    fun testControlNegativoCambioDeGrupoConMismoOrigenYDestino() = runBlocking {
        val repo = MockSchoolMovementRepositoryImpl()
        val record = SchoolMovementRecord(
            id = "INV-02",
            studentId = "ST-02",
            curp = "CURP123456HDFRRN02",
            studentFullName = "Estudiante Repetido",
            movementType = SchoolMovementType.CAMBIO_GRUPO,
            effectiveDate = "2026-10-15",
            sourceGroup = "1°A",
            targetGroup = "1°A", // Inválido: mismo grupo
            registeredByStaffId = "STAFF-01",
            registeredByName = "Secretaría",
            timestamp = 1000L
        )

        val result = repo.recordMovement(record)
        assertTrue(result is SchoolMovementResult.InvalidMovement)
        assertTrue((result as SchoolMovementResult.InvalidMovement).message.contains("no puede ser igual"))
    }
}
