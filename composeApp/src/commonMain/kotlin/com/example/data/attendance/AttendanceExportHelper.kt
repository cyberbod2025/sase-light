package com.example.data.attendance

import com.example.exportTextFileToDisk

data class AttendanceExportArtifact(
    val fileName: String,
    val filePath: String,
    val content: String
)

/**
 * Generador y exportador de reportes de asistencia y puntualidad.
 *
 * Separa estrictamente:
 * 1. Reporte Agregado PMC (para Colectivo Docente y Consejo Técnico Escolar):
 *    SIN NOMBRES ni datos personales nominales.
 * 2. Reporte Nominal Restringido (para Dirección y Control Escolar):
 *    Detalle por alumno con desglose individual de presencias, inasistencias y retardos.
 */
object AttendanceExportHelper {

    /**
     * Genera el Reporte Agregado PMC para el colectivo escolar.
     * CERO DATOS PERSONALES: Solo estadísticas consolidadas a nivel grupo/periodo.
     */
    fun generatePmcAggregatedCsv(
        report: ClassAttendanceReport,
        institutionName: String = "Escuela Secundaria Técnica No. 1"
    ): String {
        val sb = StringBuilder()
        sb.appendLine("REPORTE INSTITUCIONAL PMC - ASISTENCIA Y PUNTUALIDAD COLECTIVA")
        sb.appendLine("Institucion,$institutionName")
        sb.appendLine("Grupo,${report.groupName}")
        sb.appendLine("Periodo Evaluado,${report.periodStart} al ${report.periodEnd}")
        sb.appendLine("Tipo de Reporte,Agregado Colectivo PMC (Sin datos personales nominales)")
        sb.appendLine("Estado de Umbrales,${report.thresholds.statusNotice}")
        sb.appendLine()

        sb.appendLine("INDICADORES GENERALES DE ASISTENCIA Y PUNTUALIDAD")
        sb.appendLine("Indicador,Valor,Formula / Criterio Metodologico")
        sb.appendLine("Sesiones Programadas,${report.totalSessionsScheduled},Total de sesiones agendadas en calendario para el periodo")
        sb.appendLine("Sesiones Realizadas (Lectivas),${report.sessionsHeld},Sesiones de clase efectivamente impartidas")
        sb.appendLine("Sesiones Canceladas / Suspendidas,${report.sessionsCancelled},Sesiones canceladas (no computan inasistencia a los alumnos)")
        sb.appendLine("Sesiones No Lectivas / CTE,${report.sessionsNonTeaching},Actividades no lectivas o CTE (no computan inasistencia)")
        sb.appendLine("Total de Presencias Registradas,${report.totalAttendanceEvents},Suma total de asistencias de alumnos en sesiones realizadas")
        sb.appendLine("Total de Inasistencias Registradas,${report.totalAbsenceEvents},Suma total de faltas efectivas en sesiones realizadas")
        sb.appendLine("Eventos Totales de Retardo,${report.totalTardinessEvents},Numero acumulado de llegadas tarde en personas presentes")
        sb.appendLine("Alumnos Distintos con Retardo,${report.distinctStudentsWithTardiness},Cantidad de estudiantes individuales que tuvieron al menos un retardo")
        sb.appendLine("Tasa de Asistencia Colectiva,${report.overallAttendanceRatePct}%,(Presencias Registradas / (Presencias + Inasistencias)) * 100")
        sb.appendLine("Umbral Referencial de Asistencia,${report.thresholds.minAttendanceRatePct}%,${report.thresholds.statusNotice}")
        sb.appendLine("Umbral Referencial de Retardos,${report.thresholds.maxTardinessEvents} retardos,${report.thresholds.statusNotice}")
        return sb.toString()
    }

    /**
     * Genera el Padrón Nominal Restringido de Asistencia y Puntualidad.
     * Solo para uso interno de Dirección, Tutoría y Trabajo Social.
     */
    fun generateRestrictedNominalCsv(
        report: ClassAttendanceReport,
        institutionName: String = "Escuela Secundaria Técnica No. 1"
    ): String {
        val sb = StringBuilder()
        sb.appendLine("PADRON NOMINAL RESTRINGIDO - SEGUIMIENTO DE ASISTENCIA Y PUNTUALIDAD")
        sb.appendLine("Institucion,$institutionName")
        sb.appendLine("Grupo,${report.groupName}")
        sb.appendLine("Periodo,${report.periodStart} al ${report.periodEnd}")
        sb.appendLine("Clasificacion,CONFIDENCIAL - USO INTERNO EXCLUSIVO")
        sb.appendLine()

        sb.appendLine("Numero_Lista,ID_Alumno,Nombre_Completo,Grupo,Sesiones_Aplicables,Asistencias,Inasistencias,Pendientes,Eventos_Retardo,Porcentaje_Asistencia,Alerta_Asistencia,Alerta_Puntualidad,Resumen_Estatus")
        for (item in report.studentMetrics.sortedBy { it.listNumber }) {
            sb.appendLine(
                listOf(
                    item.listNumber.toString(),
                    item.studentId,
                    "\"${item.fullName}\"",
                    item.assignedGroup,
                    item.applicableSessions.toString(),
                    item.attendedSessions.toString(),
                    item.absentSessions.toString(),
                    item.pendingSessions.toString(),
                    item.tardinessEvents.toString(),
                    "${item.attendanceRatePct}%",
                    if (item.hasAttendanceAlert) "SI" else "NO",
                    if (item.hasTardinessAlert) "SI" else "NO",
                    "\"${item.alertSummary}\""
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    /**
     * Exporta el Reporte Agregado a un archivo `.csv` en disco.
     */
    fun exportAggregatedCsvToDisk(
        report: ClassAttendanceReport,
        filePrefix: String = "reporte_pmc_asistencia"
    ): AttendanceExportArtifact {
        val fileName = "${filePrefix}_${report.groupId}_${report.periodEnd}.csv"
        val content = generatePmcAggregatedCsv(report)
        val path = exportTextFileToDisk(fileName, content)
        return AttendanceExportArtifact(fileName, path, content)
    }

    /**
     * Exporta el Padrón Nominal Restringido a un archivo `.csv` en disco.
     */
    fun exportRestrictedNominalCsvToDisk(
        report: ClassAttendanceReport,
        filePrefix: String = "padron_nominal_asistencia"
    ): AttendanceExportArtifact {
        val fileName = "${filePrefix}_${report.groupId}_${report.periodEnd}.csv"
        val content = generateRestrictedNominalCsv(report)
        val path = exportTextFileToDisk(fileName, content)
        return AttendanceExportArtifact(fileName, path, content)
    }
}
