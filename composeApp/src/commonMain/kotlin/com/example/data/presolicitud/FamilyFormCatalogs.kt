package com.example.data.presolicitud

/**
 * Opciones cerradas del formulario de pre-solicitud familiar.
 *
 * Antes varios de estos datos eran texto libre (o grupos de casillas que
 * parecian de seleccion unica pero permitian marcar varias): la familia
 * escribia "imss", "I.M.S.S." o "seguro", y Medico Escolar / Trabajo Social
 * recibian valores imposibles de agrupar. Un catalogo cerrado es lo que hace
 * que el dato sea comparable entre alumnos.
 *
 * Las listas de campos cuyo setter del ViewModel guarda en MAYUSCULAS
 * (servicio medico, tipo de sangre, persona que atiende avisos) estan escritas
 * ya en mayusculas: lo que se elige es exactamente lo que se guarda.
 */
object FamilyFormCatalogs {
    /** Texto de la entrada que limpia un campo opcional. */
    const val SIN_ESPECIFICAR = "Sin especificar"

    // --- Medico Escolar ---
    val servicioMedico = listOf(
        "IMSS", "ISSSTE", "IMSS-BIENESTAR", "PEMEX / DEFENSA / MARINA",
        "SEGURO PRIVADO", "SIN SERVICIO MEDICO", "OTRO"
    )
    val tipoSangre = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-", "NO LO SE")
    val saludBucal = listOf("Sin problemas", "Caries", "Ortodoncia (brackets)", "Otro problema")

    // --- Trabajo Social ---
    val tipoFamilia = listOf("Nuclear", "Extensa", "Monoparental", "Reconstituida", "Otra")
    val tipoVivienda = listOf("Propia", "Rentada", "Prestada", "Compartida", "Otra")
    val dispositivoTareas = listOf("Computadora", "Tableta", "Teléfono", "Compartido", "No disponible")
    val medioTransporte = listOf("Camina", "Transporte público", "Auto familiar", "Transporte escolar", "Otro")
    val personaAtiendeAvisos = listOf("MADRE", "PADRE", "TUTOR", "ABUELOS", "OTRO FAMILIAR")
    val lugarEntreHermanos = (1..12).map { it.toString() }
    val integrantesHogar = (1..15).map { it.toString() }

    // TODO(human): ajusta este catalogo a los programas de apoyo que de verdad
    // existen en tu escuela / zona. Lo que dejo es una base razonable; la
    // familia solo podra elegir entre estas opciones.
    val becaApoyoSocial = listOf(
        "Ninguno", "Beca Benito Juárez", "Otra beca", "Otro apoyo social"
    )
}

/**
 * Etiqueta legible de cada clave interna de validacion, con el paso del
 * formulario donde se corrige. El banner de errores mostraba la clave tal cual
 * ("servicioMedico", "personaTramiteTelefono"), que a una familia no le dice
 * que campo es ni donde esta.
 */
object FamilyFormErrorLabels {
    private val labels = mapOf(
        "nombre" to ("Nombre del alumno" to "Datos"),
        "apellidoPaterno" to ("Apellido paterno" to "Datos"),
        "curp" to ("CURP" to "Datos"),
        "fechaNac" to ("Fecha de nacimiento" to "Datos"),
        "grado" to ("Grado solicitado" to "Datos"),
        "promedio" to ("Promedio del grado anterior" to "Datos"),
        "telefono" to ("Teléfono principal" to "Datos"),
        "domicilio" to ("Domicilio" to "Datos"),
        "aviso" to ("Aviso de privacidad" to "Datos"),
        "personaTramite" to ("Nombre de quien realiza el trámite" to "Contactos"),
        "personaTramiteParentesco" to ("Parentesco de quien realiza el trámite" to "Contactos"),
        "personaTramiteTelefono" to ("Teléfono de quien realiza el trámite" to "Contactos"),
        "personaTramiteIdentificacion" to ("Identificación de quien realiza el trámite" to "Contactos"),
        "responsable" to ("Nombre del responsable" to "Contactos"),
        "parentesco" to ("Parentesco del responsable" to "Contactos"),
        "responsableTel" to ("Teléfono del responsable" to "Contactos"),
        "alergiasDetalle" to ("Detalle de alergias" to "Contexto"),
        "padecimientosDetalle" to ("Detalle de padecimientos" to "Contexto"),
        "medicamentosDetalle" to ("Detalle de medicamentos" to "Contexto"),
        "viveConQuien" to ("Con quién vive el alumno" to "Contexto"),
        "tipoFamilia" to ("Tipo de familia" to "Contexto"),
        "integrantesHogar" to ("Integrantes del hogar" to "Contexto"),
        "personaAtiendeAvisos" to ("Persona que atiende avisos escolares" to "Contexto"),
        "documentos" to ("Documentos declarados" to "Docs"),
        "consentimientoUsoDatos" to ("Consentimiento de uso de datos" to "Envío"),
        "consentimientoReglamento" to ("Consentimiento del reglamento interno" to "Envío"),
        "consentimientoMarcoConvivencia" to ("Consentimiento del marco de convivencia" to "Envío"),
        "consentimientoCorresponsabilidad" to ("Consentimiento de corresponsabilidad" to "Envío")
    )

    /** "Teléfono principal (paso Datos)" -- o la propia clave si no esta mapeada, nunca vacio. */
    fun describe(key: String): String =
        labels[key]?.let { (label, step) -> "$label (paso $step)" } ?: key
}
