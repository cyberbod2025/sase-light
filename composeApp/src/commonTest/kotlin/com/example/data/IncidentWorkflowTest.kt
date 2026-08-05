package com.example.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IncidentWorkflowTest {

    private fun reported(): SaseIncident = IncidentWorkflow.report(
        type = "Conducta",
        description = "El alumno interrumpio la clase",
        date = "Hoy",
        reportedByStaffId = "staff-01",
        reportedByName = "Demo Secretaria",
        idGenerator = { "INC-1" }
    )

    @Test
    fun reportCreatesAnAbiertaIncidentWithRealAuthor() {
        val incident = reported()

        assertEquals(IncidentStatus.ABIERTA.label, incident.status)
        assertEquals("staff-01", incident.reportedByStaffId)
        assertEquals("Demo Secretaria", incident.reporter)
        assertEquals("El alumno interrumpio la clase", incident.agreementNotes)
        assertEquals("INC-1", incident.id)
        assertTrue(incident.followUps.isEmpty())
    }

    @Test
    fun legalLadderGoesAbiertaCitatorioAcuerdoCerrada() {
        assertEquals(IncidentStatus.CITATORIO_ENVIADO, IncidentWorkflow.nextStatus(IncidentStatus.ABIERTA))
        assertEquals(IncidentStatus.ACUERDO_FIRMADO, IncidentWorkflow.nextStatus(IncidentStatus.CITATORIO_ENVIADO))
        assertEquals(IncidentStatus.CERRADA, IncidentWorkflow.nextStatus(IncidentStatus.ACUERDO_FIRMADO))
        assertNull(IncidentWorkflow.nextStatus(IncidentStatus.CERRADA))
    }

    @Test
    fun advanceFromAbiertaSendsCitatorioAndAppendsFollowUp() {
        val result = IncidentWorkflow.advance(reported(), "Se cito a la familia para el viernes")

        val success = assertIs<IncidentTransitionResult.Success>(result)
        assertEquals(IncidentStatus.CITATORIO_ENVIADO.label, success.incident.status)
        assertEquals(listOf("Se cito a la familia para el viernes"), success.incident.followUps)
    }

    @Test
    fun advanceToAcuerdoFirmadoSetsAgreementNotes() {
        val citatorioSent = assertIs<IncidentTransitionResult.Success>(
            IncidentWorkflow.advance(reported(), "Citatorio enviado")
        ).incident

        val result = IncidentWorkflow.advance(citatorioSent, "La familia se compromete a supervision diaria")

        val success = assertIs<IncidentTransitionResult.Success>(result)
        assertEquals(IncidentStatus.ACUERDO_FIRMADO.label, success.incident.status)
        assertEquals("La familia se compromete a supervision diaria", success.incident.agreementNotes)
    }

    @Test
    fun advanceThroughFullLadderReachesCerrada() {
        var incident = reported()
        listOf("Citatorio enviado", "Acuerdo firmado", "Seguimiento cerrado sin recurrencia").forEach { note ->
            incident = assertIs<IncidentTransitionResult.Success>(IncidentWorkflow.advance(incident, note)).incident
        }

        assertEquals(IncidentStatus.CERRADA.label, incident.status)
        assertTrue(IncidentWorkflow.isClosed(incident))
    }

    @Test
    fun advancingAClosedIncidentIsIllegalAndDoesNotMutate() {
        var incident = reported()
        repeat(3) {
            incident = assertIs<IncidentTransitionResult.Success>(IncidentWorkflow.advance(incident, "paso")).incident
        }
        assertTrue(IncidentWorkflow.isClosed(incident))

        val result = IncidentWorkflow.advance(incident, "intento tardio")

        val illegal = assertIs<IncidentTransitionResult.IllegalTransition>(result)
        assertEquals(IncidentStatus.CERRADA, illegal.from)
        assertEquals(IncidentStatus.CERRADA.label, incident.status, "la incidencia no debe mutar tras un intento ilegal")
    }

    @Test
    fun advancingAnIncidentWithUnknownStatusIsIllegal() {
        val corrupted = reported().copy(status = "Estado inventado por otra pantalla")

        val result = IncidentWorkflow.advance(corrupted, "nota")

        assertIs<IncidentTransitionResult.IllegalTransition>(result)
    }

    @Test
    fun blankNoteStillAdvancesButDoesNotAppendEmptyFollowUp() {
        val result = IncidentWorkflow.advance(reported(), "   ")

        val success = assertIs<IncidentTransitionResult.Success>(result)
        assertEquals(IncidentStatus.CITATORIO_ENVIADO.label, success.incident.status)
        assertTrue(success.incident.followUps.isEmpty())
    }
}
