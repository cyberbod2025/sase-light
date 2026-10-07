package com.example.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PersistenceFailureMessagesTest {

    @Test
    fun everyFailureCategoryHasItsOwnDistinctMessage() {
        val messages = PreApplicationPersistenceFailure.values().map {
            PersistenceFailureMessages.describe(it, null, "guardar la pre-solicitud")
        }
        assertEquals(messages.size, messages.toSet().size, "Dos causas distintas no pueden compartir mensaje: $messages")
    }

    @Test
    fun theMessageNamesTheActionThatFailed() {
        PreApplicationPersistenceFailure.values().forEach {
            assertTrue(
                PersistenceFailureMessages.describe(it, null, "guardar la pre-solicitud").contains("guardar la pre-solicitud"),
                "El mensaje de $it debe decir que accion fallo"
            )
        }
    }

    @Test
    fun noCategoryFallsBackToTheOldGenericTryAgainMessage() {
        PreApplicationPersistenceFailure.values().forEach {
            val message = PersistenceFailureMessages.describe(it, null, "guardar")
            assertNotEquals("No fue posible guardar la pre-solicitud. Intenta de nuevo.", message)
        }
    }

    @Test
    fun rejectedWithServerDetailExplainsTheReason() {
        val message = PersistenceFailureMessages.describe(
            PreApplicationPersistenceFailure.REJECTED,
            "null value in column \"grado_solicitado\" of relation \"pre_applications\" violates not-null constraint",
            "guardar la pre-solicitud"
        )
        assertTrue(message.contains("falta un dato obligatorio"), message)
        assertTrue(message.contains("grado solicitado"), message)
    }

    @Test
    fun rejectedWithoutDetailAdmitsThatTheReasonIsUnknownInsteadOfInventingOne() {
        val message = PersistenceFailureMessages.describe(PreApplicationPersistenceFailure.REJECTED, null, "guardar")
        assertTrue(message.contains("no indicó el motivo"), message)
    }

    @Test
    fun rateLimitedDoesNotLeakTheInternalCode() {
        val message = PersistenceFailureMessages.describe(PreApplicationPersistenceFailure.RATE_LIMITED, null, "guardar")
        assertTrue(!message.contains("SASE_"), message)
        assertTrue(message.contains("demasiados intentos"), message)
    }

    @Test
    fun humanizesTheCommonPostgresMessages() {
        assertTrue(
            PersistenceFailureMessages.humanizeServerDetail("invalid input syntax for type integer: \"abc\"")
                .contains("«abc» no es válido para un campo de tipo número entero")
        )
        assertTrue(
            PersistenceFailureMessages.humanizeServerDetail("new row for relation \"x\" violates check constraint \"promedio_range\"")
                .contains("promedio_range")
        )
        assertTrue(
            PersistenceFailureMessages.humanizeServerDetail("duplicate key value violates unique constraint \"pre_applications_pkey\"")
                .contains("ya existe un registro")
        )
        assertTrue(
            PersistenceFailureMessages.humanizeServerDetail("new row violates row-level security policy for table \"x\"")
                .contains("no tienes permiso")
        )
    }

    @Test
    fun unknownServerMessagesPassThroughUntouched() {
        assertEquals("algo inesperado", PersistenceFailureMessages.humanizeServerDetail("algo inesperado"))
    }

    @Test
    fun columnNamesBecomeReadablePhrases() {
        assertEquals("contexto familiar: integrantes hogar", PersistenceFailureMessages.humanizeColumn("contexto_integrantes_hogar"))
        assertEquals("alumno: curp", PersistenceFailureMessages.humanizeColumn("alumno_curp"))
        assertEquals("grado solicitado", PersistenceFailureMessages.humanizeColumn("grado_solicitado"))
    }
}
