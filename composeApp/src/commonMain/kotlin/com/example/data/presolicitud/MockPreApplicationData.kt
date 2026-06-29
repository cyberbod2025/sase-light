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
            alumnoNombreCompleto = "Ana Maria Rosas Perez",
            alumnoCurp = "ROPA110523MDFRRN01",
            alumnoFechaNacimiento = "23/May/2011",
            alumnoSexo = "Femenino",
            alumnoNacionalidad = "Mexicana",
            alumnoEntidadNacimiento = "CDMX",
            alumnoDomicilio = "Av. Siempre Viva 123, Cuauhtemoc",
            alumnoTelefonoCasa = "55 1234 5678",
            escuelaProcedencia = "Primaria Benito Juarez",
            responsables = listOf(
                Responsable(
                    nombreCompleto = "Juan Rosas",
                    parentesco = "Padre",
                    telefono = "55 8765 4321",
                    correo = "juan.rosas@email.com",
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
            alumnoNombreCompleto = "Carlos Ivan Martinez",
            alumnoCurp = "MACA120315HDFRRM05",
            alumnoFechaNacimiento = "15/Mar/2012",
            alumnoSexo = "Masculino",
            alumnoNacionalidad = "Mexicana",
            alumnoEntidadNacimiento = "Edomex",
            alumnoDomicilio = "Calle 4, Nezahualcoyotl",
            alumnoTelefonoCasa = "55 9876 5432",
            escuelaProcedencia = "Primaria 5 de Mayo",
            responsables = listOf(
                Responsable(
                    nombreCompleto = "Maria Martinez",
                    parentesco = "Madre",
                    telefono = "55 1111 2222",
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
            alumnoNombreCompleto = "Luis Perez Guzman",
            alumnoCurp = "PEGL100810HDFRRN09",
            alumnoFechaNacimiento = "10/Ago/2010",
            alumnoSexo = "Masculino",
            alumnoNacionalidad = "Mexicana",
            alumnoEntidadNacimiento = "CDMX",
            alumnoDomicilio = "Av. Universidad 123",
            alumnoTelefonoCasa = "55 3333 4444",
            escuelaProcedencia = "Secundaria Diurna 1",
            responsables = listOf(
                Responsable(
                    nombreCompleto = "Jose Perez",
                    parentesco = "Padre",
                    telefono = "55 5555 6666",
                    correo = "jose.perez@email.com",
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
                AutorizadoPreSolicitud("Marta Guzman", "Abuela", "55 7777 8888", "Recoge los viernes")
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
