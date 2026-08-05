package com.example.data

/**
 * Estados legales de una incidencia disciplinaria/asistencial. El texto que
 * se guarda en `SaseIncident.status` (y que la UI ya compara/muestra) es
 * [label] — asi el modelo de datos sigue siendo un String simple mientras el
 * workflow que lo produce deja de serlo.
 */
enum class IncidentStatus(val label: String) {
    ABIERTA("En seguimiento"),
    CITATORIO_ENVIADO("Citatorio enviado"),
    ACUERDO_FIRMADO("Acuerdo firmado"),
    CERRADA("Atendida");

    companion object {
        fun fromLabel(label: String): IncidentStatus? = entries.find { it.label == label }
    }
}

sealed class IncidentTransitionResult {
    data class Success(val incident: SaseIncident) : IncidentTransitionResult()
    data class IllegalTransition(val from: IncidentStatus, val attempted: String) : IncidentTransitionResult()
}

/**
 * Escalera legal de una incidencia: reporte -> citatorio -> acuerdo -> cierre.
 * Cada paso exige venir exactamente del estado anterior; no se puede saltar
 * pasos ni retroceder. Quien avanza el estado (actorStaffId/actorName) queda
 * registrado junto con la nota del paso, para que "quien tomo la decision"
 * sea verificable en el propio expediente, no solo en la bitacora aparte.
 */
object IncidentWorkflow {

    private val legalOrder = listOf(
        IncidentStatus.ABIERTA,
        IncidentStatus.CITATORIO_ENVIADO,
        IncidentStatus.ACUERDO_FIRMADO,
        IncidentStatus.CERRADA
    )

    fun report(
        type: String,
        description: String,
        date: String,
        reportedByStaffId: String,
        reportedByName: String,
        idGenerator: () -> String
    ): SaseIncident = SaseIncident(
        id = idGenerator(),
        date = date,
        type = type,
        reporter = reportedByName,
        status = IncidentStatus.ABIERTA.label,
        reportedByStaffId = reportedByStaffId,
        agreementNotes = description.takeIf { it.isNotBlank() }
    )

    /** Siguiente estado legal, o null si ya esta en el ultimo (CERRADA es terminal). */
    fun nextStatus(current: IncidentStatus): IncidentStatus? {
        val index = legalOrder.indexOf(current)
        return legalOrder.getOrNull(index + 1)
    }

    /**
     * Avanza al siguiente estado legal. `note` se anexa a `followUps`
     * (o reemplaza `agreementNotes` en el paso ACUERDO_FIRMADO, que es
     * exactamente el texto del acuerdo). Retorna [IncidentTransitionResult.IllegalTransition]
     * sin mutar nada si la incidencia ya esta CERRADA o su status no es
     * reconocido.
     */
    fun advance(incident: SaseIncident, note: String): IncidentTransitionResult {
        val current = IncidentStatus.fromLabel(incident.status)
            ?: return IncidentTransitionResult.IllegalTransition(IncidentStatus.ABIERTA, incident.status)
        val next = nextStatus(current)
            ?: return IncidentTransitionResult.IllegalTransition(current, incident.status)

        val updated = when (next) {
            IncidentStatus.ACUERDO_FIRMADO -> incident.copy(
                status = next.label,
                agreementNotes = note.takeIf { it.isNotBlank() } ?: incident.agreementNotes
            )
            IncidentStatus.CITATORIO_ENVIADO, IncidentStatus.CERRADA -> incident.copy(
                status = next.label,
                followUps = if (note.isNotBlank()) incident.followUps + note else incident.followUps
            )
            IncidentStatus.ABIERTA -> incident // nunca alcanzable: ABIERTA no es "next" de nada
        }
        return IncidentTransitionResult.Success(updated)
    }

    fun isClosed(incident: SaseIncident): Boolean =
        IncidentStatus.fromLabel(incident.status) == IncidentStatus.CERRADA
}
