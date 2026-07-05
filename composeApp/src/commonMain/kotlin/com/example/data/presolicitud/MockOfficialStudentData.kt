package com.example.data.presolicitud

object MockOfficialStudentData {
    val officialStudents = listOf(
        // Alumno de 1er grado (sin grupo por regla de negocio)
        OfficialStudent(
            id = "OFF-101",
            preApplicationFolio = "PRE-X1A2",
            status = OfficialStudentStatus.ALTA_OFICIAL_SIN_GRUPO,
            gradoIngreso = 1,
            grupoSugerido = null,
            curp = "CURP-DEMO-01",
            alumnoNombreCompleto = "ALUMNO DEMO 01",
            alumnoSexo = "H",
            alumnoEdad = 6,
            promedio = 8.5,
            matriculaOficial = OfficialStudent.generateMatricula("CURP-DEMO-01", 1),
            fechaCreacion = "Hace 1 dia",
            validacionSecretaria = ValidacionArea("Secretaría", true, "Rosaura M.", "Hace 1 dia", "Documentos completos y cotejados"),
            validacionMedico = ValidacionArea("Médico Escolar", true, "Dr. Martinez", "Hoy", "Sin alergias, certificado recibido"),
            validacionTrabajoSocial = ValidacionArea("Trabajo Social", false),
            validacionUdeii = ValidacionArea("UDEII", false),
            validacionDireccion = ValidacionArea("Dirección", false)
        ),
        // Alumno de 2do grado (con grupo sugerido)
        OfficialStudent(
            id = "OFF-102",
            preApplicationFolio = "PRE-C9Z3",
            status = OfficialStudentStatus.ALTA_OFICIAL_CON_GRUPO,
            gradoIngreso = 2,
            grupoSugerido = "2B", // Sugerido por el sistema
            grupoAsignado = "2B",
            curp = "CURP-DEMO-03",
            alumnoNombreCompleto = "ALUMNO DEMO 03",
            alumnoSexo = "M",
            alumnoEdad = 8,
            promedio = 9.0,
            matriculaOficial = OfficialStudent.generateMatricula("CURP-DEMO-03", 2),
            fechaCreacion = "Hace 3 dias",
            validacionSecretaria = ValidacionArea("Secretaría", true, "Rosaura M.", "Hace 3 dias", "Expediente integrado"),
            validacionMedico = ValidacionArea("Médico Escolar", true, "Dr. Martinez", "Hace 2 dias", "Asma leve controlada"),
            validacionTrabajoSocial = ValidacionArea("Trabajo Social", true, "Lic. Torres", "Ayer", "Familia nuclear estable"),
            validacionUdeii = ValidacionArea("UDEII", true, "Mtro. Sanchez", "Ayer", "Sin observaciones de aprendizaje"),
            validacionDireccion = ValidacionArea("Dirección", true, "Directora Elena", "Hoy", "Grupo 2B aprobado por cupo y balance")
        ),
        // Alumno en proceso (documentos pendientes)
        OfficialStudent(
            id = "OFF-103",
            preApplicationFolio = "PRE-B8Y2",
            status = OfficialStudentStatus.DOCS_PENDIENTES,
            gradoIngreso = 1,
            grupoSugerido = null,
            curp = "CURP-DEMO-02",
            alumnoNombreCompleto = "ALUMNO DEMO 02",
            alumnoSexo = "H",
            alumnoEdad = 6,
            promedio = null,
            matriculaOficial = null, // Aun no se genera matricula hasta tener el ALTA OFICIAL
            fechaCreacion = "Hoy",
            validacionSecretaria = ValidacionArea("Secretaría", false, observaciones = "Falta comprobante de domicilio reciente"),
            validacionMedico = ValidacionArea("Médico Escolar", false),
            validacionTrabajoSocial = ValidacionArea("Trabajo Social", false),
            validacionUdeii = ValidacionArea("UDEII", false),
            validacionDireccion = ValidacionArea("Dirección", false)
        )
    )
}
