package com.example.data.enrollment

import com.example.data.auth.StaffRole

/**
 * Generador de reportes institucionales y padrones escolares para el PMC y Control Escolar.
 */
object SchoolMovementExportHelper {

    /**
     * Genera el Reporte Agregado Institucional para el Programa de Mejora Continua (PMC).
     * CERO DATOS PERSONALES: No incluye nombres, CURP ni identificadores nominales.
     * Puede compartirse con el Colectivo Docente, Consejo Técnico Escolar (CTE) y Supervisión.
     */
    fun generatePmcAggregatedCsv(
        snapshot: EnrollmentCutoffSnapshot,
        institutionName: String = "Escuela Secundaria Técnica No. 1"
    ): String {
        val sb = StringBuilder()
        sb.appendLine("REPORTE INSTITUCIONAL PMC - MOVIMIENTOS Y RETENCION ESCOLAR")
        sb.appendLine("Institucion,$institutionName")
        sb.appendLine("Fecha de Corte Evaluada,${snapshot.cutoffDate}")
        sb.appendLine("Fecha Base de Cohorte,${snapshot.cohortStartDate}")
        sb.appendLine("Tipo de Reporte,Agregado Institucional (Sin datos personales nominales)")
        sb.appendLine()

        sb.appendLine("INDICADORES GENERALES INSTITUCIONALES")
        sb.appendLine("Indicador,Valor,Formula / Criterio Metodologico")
        sb.appendLine("Matricula Activa al Corte,${snapshot.totalActiveStudents},Alumnos formalmente inscritos y activos a la fecha de corte")
        sb.appendLine("Cohorte Base al Inicio,${snapshot.initialCohortTotal},Alumnos activos al inicio del ciclo escolar")
        sb.appendLine("Alumnos Retenidos de la Cohorte,${snapshot.cohortRetainedTotal},Alumnos de la cohorte base que continuan activos al corte")
        sb.appendLine("Tasa de Retencion de Cohorte,${snapshot.cohortRetentionRate}%,(Retenidos Cohorte / (Cohorte Base - Traslados Acreditados)) * 100")
        sb.appendLine("Variacion Neta de Matricula,${snapshot.netEnrollmentVariation},Matricula Activa al Corte - Cohorte Base")
        sb.appendLine("Nuevas Altas en Periodo,${snapshot.newEnrollmentsInPeriod},Altas extemporaneas y reincorporaciones posteriores al inicio")
        sb.appendLine("Traslados Salientes Acreditados,${snapshot.transfersOutTotal},Alumnos con cambio formal a otra escuela con destino comprobable")
        sb.appendLine("Bajas Administrativas,${snapshot.administrativeDropoutsTotal},Tramites administrativos formales (no clasificables como abandono)")
        sb.appendLine("Abandonos Escolares Confirmados,${snapshot.confirmedDropoutsTotal},Desercion corroborada tras agotar protocolos institucionales")
        sb.appendLine("Casos Pendientes de Aclaracion,${snapshot.pendingClarificationTotal},Alumnos en proceso de investigacion domiciliaria o administrativa")
        sb.appendLine()

        sb.appendLine("DESGLOSE POR GRADO Y GRUPO ESCOLAR")
        sb.appendLine("Grupo,Matricula Activa,Cohorte Base,Retenidos Cohorte,Retencion Cohorte (%),Traslados Salientes,Bajas Administrativas,Abandonos Confirmados,Pendientes Aclaracion")
        for (g in snapshot.groupSummaries) {
            sb.appendLine("${g.groupName},${g.activeCount},${g.initialCohortCount},${g.cohortRetainedCount},${g.cohortRetentionRate}%,${g.transfersCount},${g.administrativeDropoutsCount},${g.confirmedDropoutsCount},${g.pendingClarificationCount}")
        }
        sb.appendLine()

        sb.appendLine("NOTAS METODOLOGICAS")
        sb.appendLine("1. Los traslados salientes acreditados no se computan como abandono escolar ni afectan negativamente la tasa de retencion de cohorte.")
        sb.appendLine("2. Las bajas administrativas se mantienen diferenciadas del abandono escolar confirmado para evitar atribuciones sin respaldo.")
        sb.appendLine("3. La retencion de cohorte mide la permanencia de los alumnos originales, a diferencia de la variacion de matricula que suma nuevas altas.")

        return sb.toString()
    }

    /**
     * Genera el Padrón Nominal Detallado de Matrícula al Corte.
     * DOCUMENTO RESTRINGIDO: Contiene datos personales confidenciales.
     * Acceso exclusivo para Secretaría y Dirección.
     */
    fun generateRestrictedNominalCsv(
        snapshot: EnrollmentCutoffSnapshot,
        institutionName: String = "Escuela Secundaria Técnica No. 1",
        requesterRole: StaffRole
    ): String {
        require(requesterRole == StaffRole.SECRETARIA || requesterRole == StaffRole.DIRECCION) {
            "Acceso no autorizado: El padrón nominal contiene datos personales y está reservado a Secretaría y Dirección."
        }

        val sb = StringBuilder()
        sb.appendLine("PADRON NOMINAL DETALLADO DE MATRICULA AL CORTE - USO EXCLUSIVO CONTROL ESCOLAR")
        sb.appendLine("Institucion,$institutionName")
        sb.appendLine("Fecha de Corte,${snapshot.cutoffDate}")
        sb.appendLine("Clasificacion de Informacion,Confidencial - Restringida por Ley de Proteccion de Datos")
        sb.appendLine("Generado Por Rol,$requesterRole")
        sb.appendLine()

        sb.appendLine("ID Estudiante,Matricula / Folio,CURP,Nombre Completo,Grupo Vigente,Estatus al Corte,Fecha Efectiva,Tipo de Movimiento,Escuela Destino,Fuente Documental,Criterio / Motivo")
        for (s in snapshot.studentDetails) {
            val dest = s.destinationSchool.orEmpty().replace(",", ";")
            val src = s.documentationSource.orEmpty().replace(",", ";")
            val crit = s.classificationCriteria.orEmpty().replace(",", ";")
            val fullNameEscaped = s.fullName.replace(",", " ")
            sb.appendLine("${s.studentId},${s.officialId},${s.curp},\"$fullNameEscaped\",${s.assignedGroup},${s.status.label},${s.effectiveMovementDate},${s.movementType.displayName},\"$dest\",\"$src\",\"$crit\"")
        }

        return sb.toString()
    }

    /**
     * Compatibilidad hacia atrás: genera reporte general según si se solicitan datos nominales o agregados.
     */
    fun generateCutoffCsv(
        snapshot: EnrollmentCutoffSnapshot,
        institutionName: String = "Escuela Secundaria Técnica No. 1"
    ): String = generatePmcAggregatedCsv(snapshot, institutionName)
}
