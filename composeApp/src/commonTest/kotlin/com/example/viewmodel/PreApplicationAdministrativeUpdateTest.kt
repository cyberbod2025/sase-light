package com.example.viewmodel

import com.example.data.MockSaseData
import com.example.data.enrollment.EnrollmentFlowMode
import com.example.data.presolicitud.PreApplication
import com.example.data.presolicitud.PreApplicationAdministrativeChanges
import com.example.data.presolicitud.PreApplicationAdministrativeConflictReason
import com.example.data.presolicitud.PreApplicationAdministrativeField
import com.example.data.presolicitud.PreApplicationAdministrativeFieldChange
import com.example.data.presolicitud.PreApplicationAdministrativeValidationError
import com.example.data.presolicitud.PreApplicationStatus
import com.example.data.presolicitud.UpdatePreApplicationAdministrativeDataRequest
import com.example.data.presolicitud.UpdatePreApplicationAdministrativeDataResult
import com.example.data.presolicitud.administrativeDataSnapshot
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PreApplicationAdministrativeUpdateTest {
    @BeforeTest
    fun resetSharedState() {
        PreApplicationViewModel.resetSharedStateForTests()
    }

    private fun editablePreApplication(): PreApplication =
        PreApplicationViewModel.sharedPreApplications.value.first {
            it.status == PreApplicationStatus.ENVIADA
        }

    private fun stored(folio: String): PreApplication =
        PreApplicationViewModel.sharedPreApplications.value.single { it.folio == folio }

    private fun request(
        preApplication: PreApplication,
        phone: PreApplicationAdministrativeFieldChange = PreApplicationAdministrativeFieldChange.Omitted,
        address: PreApplicationAdministrativeFieldChange = PreApplicationAdministrativeFieldChange.Omitted
    ) = UpdatePreApplicationAdministrativeDataRequest(
        folio = preApplication.folio,
        expected = preApplication.administrativeDataSnapshot(),
        changes = PreApplicationAdministrativeChanges(phone = phone, address = address)
    )

    @Test
    fun validPhoneIsTrimmedAndUpdated() {
        val original = editablePreApplication()
        val result = PreApplicationViewModel.updatePreApplicationAdministrativeData(
            request(original, phone = PreApplicationAdministrativeFieldChange.Replace(" 5512345678 "))
        )

        val updated = assertIs<UpdatePreApplicationAdministrativeDataResult.Updated>(result)
        assertEquals(setOf(PreApplicationAdministrativeField.PHONE), updated.changedFields)
        assertEquals("5512345678", stored(original.folio).alumnoTelefonoCasa)
        assertEquals(original.alumnoDomicilio, stored(original.folio).alumnoDomicilio)
    }

    @Test
    fun sameNormalizedPhoneIsNoChange() {
        val original = editablePreApplication()
        val result = PreApplicationViewModel.updatePreApplicationAdministrativeData(
            request(
                original,
                phone = PreApplicationAdministrativeFieldChange.Replace(" ${original.alumnoTelefonoCasa} ")
            )
        )

        assertIs<UpdatePreApplicationAdministrativeDataResult.NoChanges>(result)
        assertEquals(original, stored(original.folio))
    }

    @Test
    fun blankPhoneIsInvalidAndPreservesState() {
        val original = editablePreApplication()
        val allBefore = PreApplicationViewModel.sharedPreApplications.value.toList()
        val result = PreApplicationViewModel.updatePreApplicationAdministrativeData(
            request(original, phone = PreApplicationAdministrativeFieldChange.Replace("   "))
        )

        val invalid = assertIs<UpdatePreApplicationAdministrativeDataResult.Invalid>(result)
        assertEquals(
            PreApplicationAdministrativeValidationError.REQUIRED,
            invalid.errors[PreApplicationAdministrativeField.PHONE]
        )
        assertEquals(allBefore, PreApplicationViewModel.sharedPreApplications.value)
    }

    @Test
    fun nonTenDigitPhoneIsInvalidAndPreservesState() {
        val original = editablePreApplication()
        val allBefore = PreApplicationViewModel.sharedPreApplications.value.toList()
        val result = PreApplicationViewModel.updatePreApplicationAdministrativeData(
            request(original, phone = PreApplicationAdministrativeFieldChange.Replace("55-1234-5678"))
        )

        val invalid = assertIs<UpdatePreApplicationAdministrativeDataResult.Invalid>(result)
        assertEquals(
            PreApplicationAdministrativeValidationError.INVALID_FORMAT,
            invalid.errors[PreApplicationAdministrativeField.PHONE]
        )
        assertEquals(allBefore, PreApplicationViewModel.sharedPreApplications.value)
    }

    @Test
    fun validAddressIsTrimmedWithoutDestructiveNormalization() {
        val original = editablePreApplication()
        val address = "Calle Niño Héroes #12, Int. 3"
        val result = PreApplicationViewModel.updatePreApplicationAdministrativeData(
            request(original, address = PreApplicationAdministrativeFieldChange.Replace("  $address  "))
        )

        val updated = assertIs<UpdatePreApplicationAdministrativeDataResult.Updated>(result)
        assertEquals(setOf(PreApplicationAdministrativeField.ADDRESS), updated.changedFields)
        assertEquals(address, stored(original.folio).alumnoDomicilio)
        assertEquals(original.alumnoTelefonoCasa, stored(original.folio).alumnoTelefonoCasa)
    }

    @Test
    fun sameNormalizedAddressIsNoChange() {
        val original = editablePreApplication()
        val result = PreApplicationViewModel.updatePreApplicationAdministrativeData(
            request(
                original,
                address = PreApplicationAdministrativeFieldChange.Replace(" ${original.alumnoDomicilio} ")
            )
        )

        assertIs<UpdatePreApplicationAdministrativeDataResult.NoChanges>(result)
        assertEquals(original, stored(original.folio))
    }

    @Test
    fun blankAddressIsInvalidAndPreservesState() {
        val original = editablePreApplication()
        val allBefore = PreApplicationViewModel.sharedPreApplications.value.toList()
        val result = PreApplicationViewModel.updatePreApplicationAdministrativeData(
            request(original, address = PreApplicationAdministrativeFieldChange.Replace("\t "))
        )

        val invalid = assertIs<UpdatePreApplicationAdministrativeDataResult.Invalid>(result)
        assertEquals(
            PreApplicationAdministrativeValidationError.REQUIRED,
            invalid.errors[PreApplicationAdministrativeField.ADDRESS]
        )
        assertEquals(allBefore, PreApplicationViewModel.sharedPreApplications.value)
    }

    @Test
    fun validPhoneAndAddressAreAppliedTogether() {
        val original = editablePreApplication()
        val result = PreApplicationViewModel.updatePreApplicationAdministrativeData(
            request(
                original,
                phone = PreApplicationAdministrativeFieldChange.Replace("5511112233"),
                address = PreApplicationAdministrativeFieldChange.Replace("Nuevo domicilio 45-B")
            )
        )

        val updated = assertIs<UpdatePreApplicationAdministrativeDataResult.Updated>(result)
        assertEquals(
            setOf(PreApplicationAdministrativeField.PHONE, PreApplicationAdministrativeField.ADDRESS),
            updated.changedFields
        )
        assertEquals("5511112233", stored(original.folio).alumnoTelefonoCasa)
        assertEquals("Nuevo domicilio 45-B", stored(original.folio).alumnoDomicilio)
    }

    @Test
    fun reopeningFromSharedStoreUsesUpdatedAdministrativeDataAndFreshSnapshot() {
        val original = editablePreApplication()
        assertIs<UpdatePreApplicationAdministrativeDataResult.Updated>(
            PreApplicationViewModel.updatePreApplicationAdministrativeData(
                request(
                    original,
                    phone = PreApplicationAdministrativeFieldChange.Replace("5511112233"),
                    address = PreApplicationAdministrativeFieldChange.Replace("Domicilio al reabrir")
                )
            )
        )

        val reopened = stored(original.folio)
        assertEquals("5511112233", reopened.alumnoTelefonoCasa)
        assertEquals("Domicilio al reabrir", reopened.alumnoDomicilio)
        val reopenedSnapshot = reopened.administrativeDataSnapshot()
        assertEquals("5511112233", reopenedSnapshot.phone)
        assertEquals("Domicilio al reabrir", reopenedSnapshot.address)
        assertIs<UpdatePreApplicationAdministrativeDataResult.NoChanges>(
            PreApplicationViewModel.updatePreApplicationAdministrativeData(
                request(
                    reopened,
                    phone = PreApplicationAdministrativeFieldChange.Replace(reopenedSnapshot.phone),
                    address = PreApplicationAdministrativeFieldChange.Replace(reopenedSnapshot.address)
                )
            )
        )
    }

    @Test
    fun invalidPhonePreventsValidAddressFromBeingApplied() {
        val original = editablePreApplication()
        val allBefore = PreApplicationViewModel.sharedPreApplications.value.toList()
        val result = PreApplicationViewModel.updatePreApplicationAdministrativeData(
            request(
                original,
                phone = PreApplicationAdministrativeFieldChange.Replace("123"),
                address = PreApplicationAdministrativeFieldChange.Replace("Domicilio valido")
            )
        )

        assertIs<UpdatePreApplicationAdministrativeDataResult.Invalid>(result)
        assertEquals(allBefore, PreApplicationViewModel.sharedPreApplications.value)
    }

    @Test
    fun invalidAddressPreventsValidPhoneFromBeingApplied() {
        val original = editablePreApplication()
        val allBefore = PreApplicationViewModel.sharedPreApplications.value.toList()
        val result = PreApplicationViewModel.updatePreApplicationAdministrativeData(
            request(
                original,
                phone = PreApplicationAdministrativeFieldChange.Replace("5511112233"),
                address = PreApplicationAdministrativeFieldChange.Replace("")
            )
        )

        assertIs<UpdatePreApplicationAdministrativeDataResult.Invalid>(result)
        assertEquals(allBefore, PreApplicationViewModel.sharedPreApplications.value)
    }

    @Test
    fun omittedFieldsAndEqualValuesProduceNoChanges() {
        val original = editablePreApplication()
        assertIs<UpdatePreApplicationAdministrativeDataResult.NoChanges>(
            PreApplicationViewModel.updatePreApplicationAdministrativeData(request(original))
        )
        assertIs<UpdatePreApplicationAdministrativeDataResult.NoChanges>(
            PreApplicationViewModel.updatePreApplicationAdministrativeData(
                request(
                    original,
                    phone = PreApplicationAdministrativeFieldChange.Replace(original.alumnoTelefonoCasa),
                    address = PreApplicationAdministrativeFieldChange.Replace(original.alumnoDomicilio)
                )
            )
        )
        assertEquals(original, stored(original.folio))
    }

    @Test
    fun missingFolioReturnsNotFoundWithoutMutation() {
        val original = editablePreApplication()
        val allBefore = PreApplicationViewModel.sharedPreApplications.value.toList()
        val result = PreApplicationViewModel.updatePreApplicationAdministrativeData(
            request(original, phone = PreApplicationAdministrativeFieldChange.Replace("5511112233"))
                .copy(folio = "PRE-INEXISTENTE")
        )

        assertIs<UpdatePreApplicationAdministrativeDataResult.NotFound>(result)
        assertEquals(allBefore, PreApplicationViewModel.sharedPreApplications.value)
    }

    @Test
    fun exactReplayReturnsNoChangesAndDoesNotDuplicateState() {
        val original = editablePreApplication()
        val updateRequest = request(
            original,
            phone = PreApplicationAdministrativeFieldChange.Replace("5511112233"),
            address = PreApplicationAdministrativeFieldChange.Replace("Domicilio de replay")
        )
        assertIs<UpdatePreApplicationAdministrativeDataResult.Updated>(
            PreApplicationViewModel.updatePreApplicationAdministrativeData(updateRequest)
        )
        val afterFirst = PreApplicationViewModel.sharedPreApplications.value.toList()

        assertIs<UpdatePreApplicationAdministrativeDataResult.NoChanges>(
            PreApplicationViewModel.updatePreApplicationAdministrativeData(updateRequest)
        )
        assertEquals(afterFirst, PreApplicationViewModel.sharedPreApplications.value)
    }

    @Test
    fun staleExpectedValueReturnsConflictWithoutMutation() {
        val original = editablePreApplication()
        assertIs<UpdatePreApplicationAdministrativeDataResult.Updated>(
            PreApplicationViewModel.updatePreApplicationAdministrativeData(
                request(original, phone = PreApplicationAdministrativeFieldChange.Replace("5511112233"))
            )
        )
        val afterFirst = PreApplicationViewModel.sharedPreApplications.value.toList()

        val conflict = PreApplicationViewModel.updatePreApplicationAdministrativeData(
            request(original, phone = PreApplicationAdministrativeFieldChange.Replace("5599998877"))
        )

        val typedConflict = assertIs<UpdatePreApplicationAdministrativeDataResult.Conflict>(conflict)
        assertEquals(PreApplicationAdministrativeConflictReason.STALE_DATA, typedConflict.reason)
        assertEquals(afterFirst, PreApplicationViewModel.sharedPreApplications.value)
    }

    @Test
    fun nonEditableApplicationReturnsConflictWithoutMutation() {
        val original = PreApplicationViewModel.sharedPreApplications.value.first {
            it.status == PreApplicationStatus.ACEPTADA
        }
        val allBefore = PreApplicationViewModel.sharedPreApplications.value.toList()
        val result = PreApplicationViewModel.updatePreApplicationAdministrativeData(
            request(original, phone = PreApplicationAdministrativeFieldChange.Replace("5511112233"))
        )

        val conflict = assertIs<UpdatePreApplicationAdministrativeDataResult.Conflict>(result)
        assertEquals(PreApplicationAdministrativeConflictReason.NOT_EDITABLE, conflict.reason)
        assertEquals(allBefore, PreApplicationViewModel.sharedPreApplications.value)
    }

    @Test
    fun updateChangesOnlyAuthorizedFieldsAndNoNeighboringStores() {
        val original = editablePreApplication()
        val allBefore = PreApplicationViewModel.sharedPreApplications.value.toList()
        val officialsBefore = PreApplicationViewModel.officialStudents.value.toList()
        val studentsBefore = MockSaseData.students.value.toList()
        val enrollmentsBefore = MockSaseData.annualEnrollments.value.toList()
        val result = PreApplicationViewModel.updatePreApplicationAdministrativeData(
            request(
                original,
                phone = PreApplicationAdministrativeFieldChange.Replace("5511112233"),
                address = PreApplicationAdministrativeFieldChange.Replace("Domicilio autorizado")
            )
        )

        assertIs<UpdatePreApplicationAdministrativeDataResult.Updated>(result)
        val updated = stored(original.folio)
        assertEquals(
            original,
            updated.copy(
                alumnoTelefonoCasa = original.alumnoTelefonoCasa,
                alumnoDomicilio = original.alumnoDomicilio
            )
        )
        assertEquals(allBefore.size, PreApplicationViewModel.sharedPreApplications.value.size)
        assertEquals(
            allBefore.filterNot { it.folio == original.folio },
            PreApplicationViewModel.sharedPreApplications.value.filterNot { it.folio == original.folio }
        )
        assertEquals(officialsBefore, PreApplicationViewModel.officialStudents.value)
        assertEquals(studentsBefore, MockSaseData.students.value)
        assertEquals(enrollmentsBefore, MockSaseData.annualEnrollments.value)
    }

    @Test
    fun sameInputFromSameStateProducesDeterministicResult() {
        fun execute(): UpdatePreApplicationAdministrativeDataResult {
            PreApplicationViewModel.resetSharedStateForTests()
            val original = editablePreApplication()
            return PreApplicationViewModel.updatePreApplicationAdministrativeData(
                request(original, phone = PreApplicationAdministrativeFieldChange.Replace("5511112233"))
            )
        }

        assertEquals(execute(), execute())
    }

    @Test
    fun enrollmentModesRemainAvailableWithV2AsDefault() {
        assertEquals(EnrollmentFlowMode.ANNUAL_V2, PreApplicationViewModel.enrollmentFlowMode.value)
        assertEquals("LEGACY", EnrollmentFlowMode.LEGACY.name)
    }
}
