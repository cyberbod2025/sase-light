package com.example.data.presolicitud

object MockPreApplicationData {
    val preApplications = listOf(
        // Borrador familiar
        PreApplication(
            folio = "PRE-A7X9",
            status = PreApplicationStatus.BORRADOR,
            submittedAt = null,
            tramite = "Nuevo Ingreso",
            cicloEscolar = "2024-2025",
            gradoSolicitado = 1,
            alumnoNombreCompleto = "ALUMNO DEMO 01",
            alumnoCurp = "CURP-DEMO-01",
            alumnoFechaNacimiento = "23/May/2011",
            alumnoSexo = "Femenino",
            alumnoNacionalidad = "Mexicana",
            alumnoEntidadNacimiento = "CDMX",
            alumnoDomicilio = "DOMICILIO DEMO",
            alumnoTelefonoCasa = "TEL-DEMO-01",
            escuelaProcedencia = "Primaria Benito Juarez",
            responsables = listOf(
                Responsable(
                    nombreCompleto = "TUTOR DEMO 01",
                    parentesco = "Padre",
                    telefono = "TEL-DEMO-01A",
                    correo = "demo01@example.invalid",
                    domicilioDistinto = false,
                    domicilio = null,
                    viveConAlumno = true,
                    contactoPrincipal = true,
                    puedeRecoger = true,
                    ocupacion = "Empleado",
                    horarioContacto = "Matutino",
                    identificacionApresentar = "INE"
                )
            ),
            autorizados = emptyList(),
            fichaMedicaFamiliar = FichaMedicaFamiliar("IMSS", "1234567890", "O+", "Ninguna", "Ninguno", "Ninguno", "Ninguna", false, "Ninguna", "Buena", true),
            contextoSociofamiliar = ContextoSociofamiliar("Padres", "Nuclear", false, 1, false, 3, "Padre", "Media", "Propia", true, true, "Computadora", "Ninguna", "Auto", false, "Padre", "Matutino", true),
            antecedentesUdeii = AntecedentesUdeii("Ninguno", false, false, false, "Ninguno", false, false, false, false, "Ninguna"),
            documentosDeclarados = emptyList(),
            consentimientos = ConsentimientosFamiliares(true, true, true, true, true, true, true, true, true)
        ),
        // Enviada a revision
        PreApplication(
            folio = "PRE-B8Y2",
            status = PreApplicationStatus.ENVIADA,
            submittedAt = "Ayer 15:30",
            tramite = "Nuevo Ingreso",
            cicloEscolar = "2024-2025",
            gradoSolicitado = 1,
            alumnoNombreCompleto = "ALUMNO DEMO 02",
            alumnoCurp = "CURP-DEMO-02",
            alumnoFechaNacimiento = "15/Mar/2012",
            alumnoSexo = "Masculino",
            alumnoNacionalidad = "Mexicana",
            alumnoEntidadNacimiento = "Edomex",
            alumnoDomicilio = "DOMICILIO DEMO",
            alumnoTelefonoCasa = "TEL-DEMO-02",
            escuelaProcedencia = "Primaria 5 de Mayo",
            responsables = listOf(
                Responsable(
                    nombreCompleto = "TUTOR DEMO 02",
                    parentesco = "Madre",
                    telefono = "TEL-DEMO-02A",
                    correo = null,
                    domicilioDistinto = false,
                    domicilio = null,
                    viveConAlumno = true,
                    contactoPrincipal = true,
                    puedeRecoger = true,
                    ocupacion = "Hogar",
                    horarioContacto = "Vespertino",
                    identificacionApresentar = "INE"
                )
            ),
            autorizados = emptyList(),
            fichaMedicaFamiliar = FichaMedicaFamiliar("Seguro Popular", null, null, "Ninguna", "Ninguno", "Ninguno", "Ninguna", false, "Ninguna", "Regular", true),
            contextoSociofamiliar = ContextoSociofamiliar("Madre", "Monoparental", true, 1, false, 2, "Madre", "Baja", "Rentada", true, false, "Celular", "Ninguna", "Transporte publico", true, "Madre", "Vespertino", false),
            antecedentesUdeii = AntecedentesUdeii("Ninguno", false, false, false, "Ninguno", false, false, false, false, "Ninguna"),
            documentosDeclarados = listOf(
                DocumentoDeclarado("Acta de Nacimiento", true, false),
                DocumentoDeclarado("CURP", true, false),
                DocumentoDeclarado("Boleta", true, false)
            ),
            consentimientos = ConsentimientosFamiliares(true, true, true, true, true, true, true, true, true)
        ),
        // Aceptada por secretaria
        PreApplication(
            folio = "PRE-C9Z3",
            status = PreApplicationStatus.ACEPTADA,
            submittedAt = "Hace 2 dias",
            tramite = "Nuevo Ingreso",
            cicloEscolar = "2024-2025",
            gradoSolicitado = 2, // Para probar la sugerencia de grupo
            alumnoNombreCompleto = "ALUMNO DEMO 03",
            alumnoCurp = "CURP-DEMO-03",
            alumnoFechaNacimiento = "10/Ago/2010",
            alumnoSexo = "Masculino",
            alumnoNacionalidad = "Mexicana",
            alumnoEntidadNacimiento = "CDMX",
            alumnoDomicilio = "DOMICILIO DEMO",
            alumnoTelefonoCasa = "TEL-DEMO-03",
            escuelaProcedencia = "Secundaria Diurna 1",
            responsables = listOf(
                Responsable(
                    nombreCompleto = "TUTOR DEMO 03",
                    parentesco = "Padre",
                    telefono = "TEL-DEMO-03A",
                    correo = "demo03@example.invalid",
                    domicilioDistinto = false,
                    domicilio = null,
                    viveConAlumno = true,
                    contactoPrincipal = true,
                    puedeRecoger = true,
                    ocupacion = "Contador",
                    horarioContacto = "Cualquiera",
                    identificacionApresentar = "INE"
                )
            ),
            autorizados = listOf(
                AutorizadoPreSolicitud("AUTORIZADO DEMO 01", "Abuela", "TEL-DEMO-03B", "Recoge los viernes")
            ),
            fichaMedicaFamiliar = FichaMedicaFamiliar("ISSTE", "0987654321", "B+", "Polvo", "Asma leve", "Inhalador PRN", "Ninguna", true, "Miopia", "Buena", true),
            contextoSociofamiliar = ContextoSociofamiliar("Ambos Padres", "Nuclear", false, 2, true, 4, "Ambos", "Media", "Propia", true, true, "Tableta", "Ninguna", "Auto", false, "Padre", "Cualquiera", true),
            antecedentesUdeii = AntecedentesUdeii("Ninguno", false, false, false, "Ninguno", false, false, false, false, "Ninguna"),
            documentosDeclarados = listOf(
                DocumentoDeclarado("Acta de Nacimiento", true, true),
                DocumentoDeclarado("CURP", true, true),
                DocumentoDeclarado("Boleta", true, true),
                DocumentoDeclarado("Certificado Medico", true, true)
            ),
            consentimientos = ConsentimientosFamiliares(true, true, true, true, true, true, true, true, true)
        )
    )
}
