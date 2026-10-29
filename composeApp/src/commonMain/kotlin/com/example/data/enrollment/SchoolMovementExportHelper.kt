package com.example.data.enrollment

/**
 * Generador de reportes institucionales estructurados y exportables (CSV / Texto).
 * Diseñado para ser determinista, legible y reproducible.
 */
object SchoolMovementExportHelper {

    fun generateCutoffCsv(
        snapshot: EnrollmentCutoffSnapshot,
        generatedByName: String,
        schoolCycle: String = "2026-2027"
    ): String {
        val sb = StringBuilder()
        sb.appendLine("# REPORTE INSTITUCIONAL DE CORTE DE MATRICULA - SECUNDARIA 310")
        sb.appendLine("# Fecha de corte: ${snapshot.cutoffDate}")
        sb.appendLine("# Ciclo escolar: $schoolCycle")
        sb.appendLine("# Generado por: $generatedByName")
        sb.appendLine("# Universo historico evaluado: ${snapshot.totalHistoricalUniverse}")
        sb.appendLine("# Matricula activa al corte: ${snapshot.activeEnrollmentCount}")
        sb.appendLine("# Traslados a otro plantel (no abandono): ${snapshot.transfersOutCount}")
        sb.appendLine("# Bajas definitivas (abandono documentado): ${snapshot.dropoutsCount}")
        sb.appendLine("# Alumnos aun no ingresados a esta fecha: ${snapshot.unenteredCount}")
        sb.appendLine("# Cobertura activa: ${"%.2f".format(snapshot.activeCoveragePercent)}%")
        sb.appendLine("#")
        sb.appendLine("ID Alumno,Nombre Completo,CURP,Matricula,Estado al Corte,Grupo Vigente,Fecha Movimiento,Motivo o Destino")

        for (record in snapshot.records) {
            val safeName = escapeCsv(record.fullName)
            val safeReason = escapeCsv(record.destinationOrReason)
            sb.appendLine("${record.studentId},$safeName,${record.curp},${record.enrollmentId},${record.statusType.label},${record.assignedGroup},${record.effectiveDate},$safeReason")
        }

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
