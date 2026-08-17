package com.example.viewmodel

import com.example.data.presolicitud.MockPreApplicationData
import com.example.data.presolicitud.PreApplicationStatus
import com.example.data.presolicitud.ReadinessStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PreApplicationConversionSynchronizerTest {
    private fun readySource() = MockPreApplicationData.preApplications.first().copy(
        status = PreApplicationStatus.ACEPTADA,
        readinessStatus = ReadinessStatus.READY
    )

    @Test
    fun firstCasFailureAndCompatibleRereadUseExactlyTwoAttempts() = runTest {
        val source = readySource()
        var state = listOf(source)
        var attempts = 0

        val result = synchronizePreApplicationConversion(
            source = source,
            readState = { state },
            commit = { candidate ->
                attempts += 1
                if (attempts == 1) false else true.also { state = listOf(candidate) }
            }
        )

        assertIs<PreApplicationConversionResult.Converted>(result)
        assertEquals(2, attempts)
        assertEquals(ReadinessStatus.CONVERTED, state.single().readinessStatus)
    }

    @Test
    fun incompatibleRereadStopsWithoutSecondCas() = runTest {
        val source = readySource()
        var state = listOf(source)
        var attempts = 0

        val result = synchronizePreApplicationConversion(
            source = source,
            readState = { state },
            commit = { _ ->
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
    fun secondCasFailureReturnsIncompleteWithoutThirdAttempt() = runTest {
        val source = readySource()
        var attempts = 0

        val result = synchronizePreApplicationConversion(
            source = source,
            readState = { listOf(source) },
            commit = { _ ->
                attempts += 1
                false
            }
        )

        val incomplete = assertIs<PreApplicationConversionResult.Incomplete>(result)
        assertEquals(PreApplicationSynchronizationCause.CAS_FAILED, incomplete.cause)
        assertEquals(2, attempts)
    }

    @Test
    fun concurrentNameChangeIsIdentityConflictWithoutSecondCas() = runTest {
        val source = readySource()
        var state = listOf(source)
        var attempts = 0

        val result = synchronizePreApplicationConversion(
            source = source,
            readState = { state },
            commit = { _ ->
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
    fun alreadyConvertedReturnsWithoutCasOrMetadataChanges() = runTest {
        val source = readySource().copy(
            readinessStatus = ReadinessStatus.CONVERTED,
            readyAt = "Hoy 10:00",
            readinessNotes = "Nota existente"
        )
        var attempts = 0

        val result = synchronizePreApplicationConversion(
            source = source,
            readState = { listOf(source) },
            commit = { _ ->
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
