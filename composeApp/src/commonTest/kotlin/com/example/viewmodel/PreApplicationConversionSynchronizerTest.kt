package com.example.viewmodel

import com.example.data.presolicitud.MockPreApplicationData
import com.example.data.presolicitud.PreApplicationStatus
import com.example.data.presolicitud.ReadinessStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PreApplicationConversionSynchronizerTest {
    private fun readySource() = MockPreApplicationData.preApplications.first().copy(
        status = PreApplicationStatus.ACEPTADA,
        readinessStatus = ReadinessStatus.READY
    )

    @Test
    fun firstCasFailureAndCompatibleRereadUseExactlyTwoAttempts() {
        val source = readySource()
        var state = listOf(source)
        var attempts = 0

        val result = synchronizePreApplicationConversion(
            source = source,
            readState = { state },
            compareAndSet = { _, updated ->
                attempts += 1
                if (attempts == 1) false else true.also { state = updated }
            }
        )

        assertIs<PreApplicationConversionResult.Converted>(result)
        assertEquals(2, attempts)
        assertEquals(ReadinessStatus.CONVERTED, state.single().readinessStatus)
    }

    @Test
    fun incompatibleRereadStopsWithoutSecondCas() {
        val source = readySource()
        var state = listOf(source)
        var attempts = 0

        val result = synchronizePreApplicationConversion(
            source = source,
            readState = { state },
            compareAndSet = { _, _ ->
                attempts += 1
                state = listOf(source.copy(readinessStatus = ReadinessStatus.BLOCKED))
                false
            }
        )

        val incomplete = assertIs<PreApplicationConversionResult.Incomplete>(result)
        assertEquals(PreApplicationSynchronizationCause.READINESS_CHANGED, incomplete.cause)
        assertEquals(1, attempts)
    }

    @Test
    fun secondCasFailureReturnsIncompleteWithoutThirdAttempt() {
        val source = readySource()
        var attempts = 0

        val result = synchronizePreApplicationConversion(
            source = source,
            readState = { listOf(source) },
            compareAndSet = { _, _ ->
                attempts += 1
                false
            }
        )

        val incomplete = assertIs<PreApplicationConversionResult.Incomplete>(result)
        assertEquals(PreApplicationSynchronizationCause.CAS_FAILED, incomplete.cause)
        assertEquals(2, attempts)
    }

    @Test
    fun concurrentNameChangeIsIdentityConflictWithoutSecondCas() {
        val source = readySource()
        var state = listOf(source)
        var attempts = 0

        val result = synchronizePreApplicationConversion(
            source = source,
            readState = { state },
            compareAndSet = { _, _ ->
                attempts += 1
                state = listOf(source.copy(alumnoNombreCompleto = "OTRO NOMBRE"))
                false
            }
        )

        val incomplete = assertIs<PreApplicationConversionResult.Incomplete>(result)
        assertEquals(PreApplicationSynchronizationCause.IDENTITY_CHANGED, incomplete.cause)
        assertEquals(1, attempts)
    }

    @Test
    fun alreadyConvertedReturnsWithoutCasOrMetadataChanges() {
        val source = readySource().copy(
            readinessStatus = ReadinessStatus.CONVERTED,
            readyAt = "Hoy 10:00",
            readinessNotes = "Nota existente"
        )
        var attempts = 0

        val result = synchronizePreApplicationConversion(
            source = source,
            readState = { listOf(source) },
            compareAndSet = { _, _ ->
                attempts += 1
                false
            }
        )

        val converted = assertIs<PreApplicationConversionResult.AlreadyConverted>(result)
        assertEquals(0, attempts)
        assertEquals("Hoy 10:00", converted.preApplication.readyAt)
        assertEquals("Nota existente", converted.preApplication.readinessNotes)
    }
}
