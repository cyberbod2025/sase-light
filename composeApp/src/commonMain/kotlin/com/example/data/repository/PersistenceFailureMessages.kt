package com.example.data.repository

/**
 * Convierte una falla de persistencia en un mensaje para la persona usuaria
 * que dice QUE paso, sin ambiguedad.
 *
 * Antes, todas las fallas terminaban en "No fue posible guardar... Intenta de
 * nuevo": el motivo (sin red, dato invalido, limite de envios, sesion vencida)
 * se descartaba, y quien capturaba no sabia si reintentar, corregir un dato o
 * esperar. Cada categoria tiene aqui su propio mensaje y, cuando el servidor
 * informo un motivo concreto, se agrega traducido.
 */
object PersistenceFailureMessages {

    /** [action]: lo que se intentaba hacer, en infinitivo ("guardar la pre-solicitud"). */
    fun describe(reason: PreApplicationPersistenceFailure, detail: String?, action: String): String =
        when (reason) {
            PreApplicationPersistenceFailure.NETWORK ->
                if (detail != null) {
                    "No se pudo $action. $detail"
                } else {
                    "No se pudo $action: el servidor no respondió (sin conexión o servicio no disponible). " +
                        "Revisa tu internet e intenta de nuevo en unos minutos; lo que capturaste sigue en pantalla."
                }
            PreApplicationPersistenceFailure.REJECTED ->
                if (detail != null) {
                    "El servidor rechazó la información y no se pudo $action. Motivo: ${humanizeServerDetail(detail)}"
                } else {
                    "El servidor rechazó la información y no se pudo $action, pero no indicó el motivo. " +
                        "Revisa que todos los datos estén completos y correctos."
                }
            PreApplicationPersistenceFailure.RATE_LIMITED ->
                "No se pudo $action: se hicieron demasiados intentos desde esta red. " +
                    "Espera unos minutos e intenta de nuevo; lo que capturaste sigue en pantalla."
            PreApplicationPersistenceFailure.NO_SESSION ->
                "No se pudo $action: la sesión terminó o el código de acceso ya no es válido. " +
                    "Vuelve a consultar con tu folio y código."
        }

    /**
     * Traduce los mensajes tipicos de Postgres/PostgREST a espanol claro. Lo que
     * no reconoce se devuelve tal cual: un mensaje tecnico exacto es mejor que
     * uno inventado.
     */
    fun humanizeServerDetail(raw: String): String {
        notNull.find(raw)?.let { return "falta un dato obligatorio (${humanizeColumn(it.groupValues[1])})." }
        invalidSyntax.find(raw)?.let {
            return "el valor «${it.groupValues[2]}» no es válido para un campo de tipo ${typeName(it.groupValues[1])}."
        }
        check.find(raw)?.let { return "un dato no cumple una regla de validación (${it.groupValues[1]})." }
        foreignKey.find(raw)?.let { return "un dato hace referencia a un registro que no existe (${it.groupValues[1]})." }
        unique.find(raw)?.let { return "ya existe un registro con ese dato (${it.groupValues[1]})." }
        if (raw.contains("row-level security", ignoreCase = true)) {
            return "no tienes permiso para guardar este registro."
        }
        return raw
    }

    /** "contexto_integrantes_hogar" -> "contexto familiar: integrantes hogar". */
    fun humanizeColumn(column: String): String {
        val prefixes = listOf(
            "alumno_" to "alumno: ",
            "contexto_" to "contexto familiar: ",
            "ficha_" to "ficha médica: ",
            "udeii_" to "UDEII: ",
            "persona_tramite_" to "quien realiza el trámite: ",
            "consentimiento_" to "consentimiento: "
        )
        val (prefix, label) = prefixes.firstOrNull { column.startsWith(it.first) } ?: ("" to "")
        return label + column.removePrefix(prefix).replace('_', ' ')
    }

    private fun typeName(sqlType: String): String = when (sqlType.trim().lowercase()) {
        "integer", "bigint", "smallint" -> "número entero"
        "double precision", "numeric", "real" -> "número"
        "boolean" -> "sí/no"
        "date", "timestamp with time zone", "timestamp without time zone" -> "fecha"
        "uuid" -> "identificador"
        else -> sqlType
    }

    private val notNull = Regex("""null value in column "([^"]+)"""")
    private val invalidSyntax = Regex("""invalid input syntax for type ([^:]+): "([^"]*)"""")
    private val check = Regex("""violates check constraint "([^"]+)"""")
    private val foreignKey = Regex("""violates foreign key constraint "([^"]+)"""")
    private val unique = Regex("""violates unique constraint "([^"]+)"""")
}
