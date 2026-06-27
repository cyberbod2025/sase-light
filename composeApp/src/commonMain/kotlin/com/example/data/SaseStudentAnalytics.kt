package com.example.data

data class SaseStudentMetrics(
    val totalCount: Int,
    val activeCount: Int,
    val pendingDocsCount: Int,
    val atRiskCount: Int,
    val highAttendanceCount: Int,
    val attendanceRate: Int
)

fun calculateStudentMetrics(students: List<Student>): SaseStudentMetrics {
    val totalCount = students.size
    val activeCount = students.count { it.status == "Activo" || it.status == "Nuevo ingreso" }
    val pendingDocsCount = students.count { student ->
        student.documentationStatus != "Completa" || student.documents.any { doc -> doc.status == "Pendiente" }
    }
    val atRiskCount = students.count { it.riskLevel == "Alto" || it.riskLevel == "Medio" }
    val highAttendanceCount = students.count { it.attendancePercent >= 90 }
    val attendanceRate = if (totalCount > 0) (highAttendanceCount * 100) / totalCount else 0

    return SaseStudentMetrics(
        totalCount = totalCount,
        activeCount = activeCount,
        pendingDocsCount = pendingDocsCount,
        atRiskCount = atRiskCount,
        highAttendanceCount = highAttendanceCount,
        attendanceRate = attendanceRate
    )
}
