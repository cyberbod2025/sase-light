package com.example.data.presolicitud

object MockOfficialStudentData {
    val officialStudents = listOf(
        OfficialStudent(
            id = "OFF-DEMO-01",
            preApplicationFolio = "PRE-X1A2",
            status = OfficialStudentStatus.ALTA_OFICIAL_CON_GRUPO,
            gradoIngreso = 1,
            grupoSugerido = "1A",
            grupoAsignado = "1A",
            curp = "DEMA100101HDFABC01",
            alumnoNombreCompleto = "ALUMNO DEMO 01",
            alumnoSexo = "H",
            alumnoEdad = 14,
            promedio = 9.1,
            matriculaOficial = "S310-DEMA100101-26",
            fechaCreacion = "HACE 1 DIA",
            validacionSecretaria = ValidacionArea("Secretaría", true, "SECRETARIA DEMO", "HACE 1 DIA", "DOCUMENTOS COMPLETOS Y COTEJADOS"),
            validacionMedico = ValidacionArea("Médico Escolar", true, "MEDICO DEMO", "HACE 1 DIA", "FICHA DEMO REVISADA"),
            validacionTrabajoSocial = ValidacionArea("Trabajo Social", true, "TRABAJO SOCIAL DEMO", "HACE 1 DIA", "SIN PENDIENTES DEMO"),
            validacionUdeii = ValidacionArea("UDEII", true, "UDEII DEMO", "HACE 1 DIA", "SIN PENDIENTES DEMO"),
            validacionDireccion = ValidacionArea("Dirección", true, "DIRECCION DEMO", "HACE 1 DIA", "GRUPO 1A APROBADO")
        ),
        OfficialStudent(
            id = "OFF-DEMO-03",
            preApplicationFolio = "PRE-C9Z3",
            status = OfficialStudentStatus.DOCS_PENDIENTES,
            gradoIngreso = 2,
            grupoSugerido = "2B",
            grupoAsignado = null,
            curp = "DEMC120303HDFABC03",
            alumnoNombreCompleto = "ALUMNO DEMO 03",
            alumnoSexo = "H",
            alumnoEdad = 12,
            promedio = 7.8,
            matriculaOficial = null,
            fechaCreacion = "AYER",
            validacionSecretaria = ValidacionArea("Secretaría", true, "SECRETARIA DEMO", "AYER", "DATOS ACEPTADOS; DOCUMENTOS Y FOTOS PENDIENTES"),
            validacionMedico = ValidacionArea("Médico Escolar", false),
            validacionTrabajoSocial = ValidacionArea("Trabajo Social", false),
            validacionUdeii = ValidacionArea("UDEII", false),
            validacionDireccion = ValidacionArea("Dirección", false)
        ),
        OfficialStudent(
            id = "OFF-DEMO-05",
            preApplicationFolio = "PRE-E5M5",
            status = OfficialStudentStatus.PENDIENTE_ASIGNACION_GRUPO,
            gradoIngreso = 1,
            grupoSugerido = "1B",
            grupoAsignado = null,
            curp = "DEME140505HDFABC05",
            alumnoNombreCompleto = "ALUMNO DEMO 05",
            alumnoSexo = "H",
            alumnoEdad = 11,
            promedio = 8.4,
            matriculaOficial = null,
            fechaCreacion = "HOY 11:30",
            validacionSecretaria = ValidacionArea("Secretaría", true, "SECRETARIA DEMO", "HOY 11:30", "LISTO; REQUIERE CONFIRMAR GRUPO"),
            validacionMedico = ValidacionArea("Médico Escolar", true, "MEDICO DEMO", "HOY 11:30", "FICHA DEMO REVISADA"),
            validacionTrabajoSocial = ValidacionArea("Trabajo Social", true, "TRABAJO SOCIAL DEMO", "HOY 11:30", "SIN PENDIENTES DEMO"),
            validacionUdeii = ValidacionArea("UDEII", true, "UDEII DEMO", "HOY 11:30", "SIN PENDIENTES DEMO"),
            validacionDireccion = ValidacionArea("Dirección", false, observaciones = "GRUPO SUGERIDO: 1B. ASIGNACIÓN MANUAL DISPONIBLE")
        )
    )
}
