package com.example.data

import kotlin.test.Test
import kotlin.test.assertEquals

class SaseStudentAnalyticsTest {
    @Test
    fun calculatesZeroMetricsForEmptyList() {
        val metrics = calculateStudentMetrics(emptyList())

        assertEquals(0, metrics.totalCount)
        assertEquals(0, metrics.activeCount)
        assertEquals(0, metrics.pendingDocsCount)
        assertEquals(0, metrics.atRiskCount)
        assertEquals(0, metrics.highAttendanceCount)
        assertEquals(0, metrics.attendanceRate)
    }

    @Test
    fun calculatesTotalCount() {
        val metrics = calculateStudentMetrics(
            listOf(
                student(id = "1"),
                student(id = "2"),
                student(id = "3")
            )
        )

        assertEquals(3, metrics.totalCount)
    }

    @Test
    fun countsActiveAndNewStudentsAsActive() {
        val metrics = calculateStudentMetrics(
            listOf(
                student(id = "1", status = "Activo"),
                student(id = "2", status = "Nuevo ingreso"),
                student(id = "3", status = "En riesgo")
            )
        )

        assertEquals(2, metrics.activeCount)
    }

    @Test
    fun countsStudentsWithPendingDocuments() {
        val metrics = calculateStudentMetrics(
            listOf(
                student(id = "1", documentationStatus = "Incompleta"),
                student(id = "2", documents = listOf(SaseDocument("CURP", "Hoy", "Pendiente"))),
                student(id = "3", documentationStatus = "Completa")
            )
        )

        assertEquals(2, metrics.pendingDocsCount)
    }

    @Test
    fun countsMediumAndHighRiskStudents() {
        val metrics = calculateStudentMetrics(
            listOf(
                student(id = "1", riskLevel = "Alto"),
                student(id = "2", riskLevel = "Medio"),
                student(id = "3", riskLevel = "Bajo")
            )
        )

        assertEquals(2, metrics.atRiskCount)
    }

    @Test
    fun countsHighAttendanceStudentsAndRate() {
        val metrics = calculateStudentMetrics(
            listOf(
                student(id = "1", attendancePercent = 90),
                student(id = "2", attendancePercent = 95),
                student(id = "3", attendancePercent = 89),
                student(id = "4", attendancePercent = 70)
            )
        )

        assertEquals(2, metrics.highAttendanceCount)
        assertEquals(50, metrics.attendanceRate)
    }

    private fun student(
        id: String,
        status: String = "Activo",
        documentationStatus: String = "Completa",
        documents: List<SaseDocument> = emptyList(),
        riskLevel: String = "Bajo",
        attendancePercent: Int = 100
    ): Student = Student(
        id = id,
        fullName = "Student $id",
        group = "1A",
        enrollmentId = "ENR-$id",
        curp = "CURP$id",
        status = status,
        documentationStatus = documentationStatus,
        documents = documents,
        riskLevel = riskLevel,
        attendancePercent = attendancePercent
    )
}
