package com.example.data

import com.example.data.enrollment.AnnualEnrollmentFlowResult
import com.example.data.enrollment.AnnualEnrollmentInitialStatus
import com.example.data.enrollment.AnnualEnrollmentMovement
import com.example.data.enrollment.AnnualEnrollmentRecord
import com.example.data.enrollment.GroupPlacementRequirement
import com.example.data.presolicitud.MockPreApplicationData
import com.example.data.presolicitud.PreApplication
import com.example.data.presolicitud.PreApplicationStatus
import com.example.data.presolicitud.ReadinessStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InstitutionalStudentRecordResolverTest {
    @Test
    fun resolvesCurrentAnnualEnrollmentAndInstitutionalContactData() {
        val resolved = resolved(
            resolve(
                student = student(),
                annualEnrollments = listOf(annual()),
                preApplications = listOf(preApplication())
            )
        )

        assertEquals("MASTER-V2-PRE-L6A", resolved.studentId)
        assertEquals("S310-000321-7", resolved.enrollmentId.value)
        assertEquals(InstitutionalRecordDataQuality.CONFIRMED, resolved.enrollmentId.quality)
        assertEquals("2026-2027", resolved.schoolYear.value)
        assertEquals(InstitutionalRecordDataQuality.CONFIRMED, resolved.schoolYear.quality)
        assertEquals(2, resolved.grade.value)
        assertEquals(InstitutionalRecordDataQuality.CONFIRMED, resolved.grade.quality)
        assertNull(resolved.group.value)
        assertEquals(InstitutionalRecordDataQuality.PENDING, resolved.group.quality)
        assertEquals(AnnualEnrollmentInitialStatus.PENDING_GROUP_ASSIGNMENT, resolved.annualStatus.value)
        assertEquals(AnnualEnrollmentMovement.NEW_ENTRY, resolved.movement.value)
        assertEquals("DOMICILIO INSTITUCIONAL CORREGIDO", resolved.address.value)
        assertEquals("5512345678", resolved.householdPhone.value)
        assertEquals(ReadinessStatus.CONVERTED, resolved.readinessStatus.value)
        assertEquals(InstitutionalRecordDataQuality.CONFIRMED, resolved.readinessStatus.quality)
        assertTrue(resolved.warnings.isEmpty())
    }

    @Test
    fun keySelectsCurrentAnnualEnrollmentWithoutUsingListOrder() {
        val historical = annual(schoolYear = "2025-2026", folio = "PRE-HISTORICA")
        val current = annual()

        val firstOrder = resolved(resolve(annualEnrollments = listOf(historical, current)))
        val secondOrder = resolved(resolve(annualEnrollments = listOf(current, historical)))

        assertEquals(current.schoolYear, firstOrder.schoolYear.value)
        assertEquals(current.preApplicationFolio(), firstOrder.preApplicationFolio)
        assertEquals(current.schoolYear, secondOrder.schoolYear.value)
        assertEquals(current.requestedGrade, secondOrder.grade.value)
        assertEquals(current.assignedGroup, secondOrder.group.value)
    }

    @Test
    fun duplicateExactAnnualEnrollmentsAreAmbiguous() {
        val result = resolve(annualEnrollments = listOf(annual(), annual()))

        val ambiguous = assertIs<InstitutionalStudentRecordResolution.AmbiguousAnnualEnrollment>(result)
        assertEquals(2, ambiguous.matches)
    }

    @Test
    fun missingPreApplicationProducesPartialHonestRecord() {
        val resolved = resolved(resolve(preApplications = emptyList()))

        assertTrue(InstitutionalRecordWarning.PRE_APPLICATION_NOT_FOUND in resolved.warnings)
        assertUnavailable(resolved.address)
        assertUnavailable(resolved.householdPhone)
        assertUnavailable(resolved.readinessStatus)
    }

    @Test
    fun missingStudentFolioProducesTraceabilityWarningWithoutMutation() {
        val student = student().copy(preApplicationFolio = null)

        val resolved = resolved(resolve(student = student))

        assertTrue(InstitutionalRecordWarning.MISSING_STUDENT_PRE_APPLICATION_TRACEABILITY in resolved.warnings)
        assertNull(student.preApplicationFolio)
    }

    @Test
    fun blankStudentFolioAlsoProducesMissingTraceabilityWarning() {
        val resolved = resolved(resolve(student = student().copy(preApplicationFolio = " ")))

        assertTrue(InstitutionalRecordWarning.MISSING_STUDENT_PRE_APPLICATION_TRACEABILITY in resolved.warnings)
    }

    @Test
    fun differentStudentFolioIsIdentityConflict() {
        val result = resolve(student = student().copy(preApplicationFolio = "PRE-OTRA"))

        assertConflict(result, InstitutionalRecordIdentityConflict.PRE_APPLICATION_FOLIO_MISMATCH)
    }

    @Test
    fun nonConvertedPreApplicationResolvesWithSynchronizationWarning() {
        val resolved = resolved(
            resolve(preApplications = listOf(preApplication().copy(readinessStatus = ReadinessStatus.READY)))
        )

        assertTrue(InstitutionalRecordWarning.PRE_APPLICATION_SYNCHRONIZATION_INCOMPLETE in resolved.warnings)
        assertEquals(ReadinessStatus.READY, resolved.readinessStatus.value)
        assertEquals(InstitutionalRecordDataQuality.PENDING, resolved.readinessStatus.quality)
        assertEquals(InstitutionalRecordDataQuality.PENDING, resolved.address.quality)
        assertEquals(InstitutionalRecordDataQuality.PENDING, resolved.householdPhone.quality)
    }

    @Test
    fun blankHouseholdContactFieldsAreUnavailable() {
        val resolved = resolved(
            resolve(
                preApplications = listOf(
                    preApplication().copy(alumnoDomicilio = " ", alumnoTelefonoCasa = "")
                )
            )
        )

        assertUnavailable(resolved.address)
        assertUnavailable(resolved.householdPhone)
    }

    @Test
    fun missingAssignedGroupWithRecognizedRequirementIsPending() {
        val resolved = resolved(resolve(annualEnrollments = listOf(annual(group = null))))

        assertNull(resolved.group.value)
        assertEquals(InstitutionalRecordDataQuality.PENDING, resolved.group.quality)
    }

    @Test
    fun missingGroupWithContradictoryRequirementAndStatusIsInconsistent() {
        val contradictory = annual(group = null).copy(
            status = AnnualEnrollmentInitialStatus.PENDING_GROUP_CONTINUITY_DECISION,
            groupPlacementRequirement = GroupPlacementRequirement.AssignmentRequired
        )

        val resolved = resolved(resolve(annualEnrollments = listOf(contradictory)))

        assertNull(resolved.group.value)
        assertEquals(InstitutionalRecordDataQuality.INCONSISTENT, resolved.group.quality)
    }

    @Test
    fun missingGroupWithMatchingContinuityRequirementIsPending() {
        val annual = annual(group = null).copy(
            status = AnnualEnrollmentInitialStatus.PENDING_GROUP_CONTINUITY_DECISION,
            groupPlacementRequirement = GroupPlacementRequirement.ContinuityDecisionRequired("1B", "2B")
        )
        val resolved = resolved(resolve(annualEnrollments = listOf(annual)))

        assertNull(resolved.group.value)
        assertEquals(InstitutionalRecordDataQuality.PENDING, resolved.group.quality)
    }

    @Test
    fun assignedGroupWithPendingAssignmentStatusIsInconsistent() {
        val resolved = resolved(resolve(annualEnrollments = listOf(annual(group = "2B"))))

        assertEquals("2B", resolved.group.value)
        assertEquals(InstitutionalRecordDataQuality.INCONSISTENT, resolved.group.quality)
    }

    @Test
    fun assignedGroupWithPendingContinuityStatusIsInconsistent() {
        val annual = annual(group = "2B").copy(
            status = AnnualEnrollmentInitialStatus.PENDING_GROUP_CONTINUITY_DECISION,
            groupPlacementRequirement = GroupPlacementRequirement.ContinuityDecisionRequired("1B", "2B")
        )
        val resolved = resolved(resolve(annualEnrollments = listOf(annual)))

        assertEquals("2B", resolved.group.value)
        assertEquals(InstitutionalRecordDataQuality.INCONSISTENT, resolved.group.quality)
    }

    @Test
    fun allConstructibleAnnualStatusesArePendingForGroupProjection() {
        assertEquals(
            setOf(
                AnnualEnrollmentInitialStatus.PENDING_GROUP_ASSIGNMENT,
                AnnualEnrollmentInitialStatus.PENDING_GROUP_CONTINUITY_DECISION
            ),
            AnnualEnrollmentInitialStatus.entries.toSet()
        )

        AnnualEnrollmentInitialStatus.entries.forEach { status ->
            val requirement = when (status) {
                AnnualEnrollmentInitialStatus.PENDING_GROUP_ASSIGNMENT ->
                    GroupPlacementRequirement.AssignmentRequired
                AnnualEnrollmentInitialStatus.PENDING_GROUP_CONTINUITY_DECISION ->
                    GroupPlacementRequirement.ContinuityDecisionRequired("1B", "2B")
            }
            val resolved = resolved(
                resolve(
                    annualEnrollments = listOf(
                        annual(group = "2B").copy(status = status, groupPlacementRequirement = requirement)
                    )
                )
            )

            assertEquals(InstitutionalRecordDataQuality.INCONSISTENT, resolved.group.quality)
        }
    }

    @Test
    fun missingStudentIsTyped() {
        assertIs<InstitutionalStudentRecordResolution.StudentNotFound>(
            resolve(students = emptyList())
        )
    }

    @Test
    fun duplicateStudentsAreAmbiguous() {
        val result = resolve(students = listOf(student(), student()))

        assertEquals(2, assertIs<InstitutionalStudentRecordResolution.AmbiguousStudent>(result).matches)
    }

    @Test
    fun missingAnnualEnrollmentIsTyped() {
        assertIs<InstitutionalStudentRecordResolution.AnnualEnrollmentNotFound>(
            resolve(annualEnrollments = emptyList())
        )
    }

    @Test
    fun blankStudentIdIsTypedAndDoesNotBroadenLookup() {
        val invalid = assertIs<InstitutionalStudentRecordResolution.InvalidResolutionKey>(
            resolve(key = key().copy(studentId = ""))
        )

        assertEquals(setOf(InstitutionalRecordKeyError.MISSING_STUDENT_ID), invalid.errors)
    }

    @Test
    fun blankSchoolYearIsTypedAndDoesNotBroadenLookup() {
        val invalid = assertIs<InstitutionalStudentRecordResolution.InvalidResolutionKey>(
            resolve(key = key().copy(schoolYear = ""))
        )

        assertEquals(setOf(InstitutionalRecordKeyError.MISSING_SCHOOL_YEAR), invalid.errors)
    }

    @Test
    fun blankFolioIsTypedAndDoesNotBroadenLookup() {
        val invalidKey = key().copy(sourcePreApplicationFolio = "")

        val invalid = assertIs<InstitutionalStudentRecordResolution.InvalidResolutionKey>(
            resolve(key = invalidKey)
        )
        assertEquals(setOf(InstitutionalRecordKeyError.MISSING_PRE_APPLICATION_FOLIO), invalid.errors)
    }

    @Test
    fun duplicateCanonicalPreApplicationsAreAmbiguous() {
        val result = resolve(
            preApplications = listOf(
                preApplication(),
                preApplication().copy(folio = " pre-l6a ")
            )
        )

        assertEquals(2, assertIs<InstitutionalStudentRecordResolution.AmbiguousPreApplication>(result).matches)
    }

    @Test
    fun contradictoryCurpIsIdentityConflict() {
        assertConflict(
            resolve(annualEnrollments = listOf(annual(curp = "OTRA100101HDFABC01"))),
            InstitutionalRecordIdentityConflict.CURP_MISMATCH
        )
    }

    @Test
    fun missingCurpEvidenceProducesWarningInsteadOfConflict() {
        val resolved = resolved(
            resolve(
                student = student().copy(curp = ""),
                annualEnrollments = listOf(annual(curp = "")),
                preApplications = listOf(preApplication().copy(alumnoCurp = ""))
            )
        )

        assertTrue(InstitutionalRecordWarning.MISSING_CURP_TRACEABILITY in resolved.warnings)
    }

    @Test
    fun missingAnnualCurpProducesWarningInsteadOfConflict() {
        val resolved = resolved(resolve(annualEnrollments = listOf(annual(curp = ""))))

        assertTrue(InstitutionalRecordWarning.MISSING_CURP_TRACEABILITY in resolved.warnings)
    }

    @Test
    fun missingPreApplicationCurpProducesWarningInsteadOfConflict() {
        val resolved = resolved(
            resolve(preApplications = listOf(preApplication().copy(alumnoCurp = "")))
        )

        assertTrue(InstitutionalRecordWarning.MISSING_CURP_TRACEABILITY in resolved.warnings)
    }

    @Test
    fun missingStudentCurpProducesWarningInsteadOfConflict() {
        val resolved = resolved(resolve(student = student().copy(curp = "")))

        assertTrue(InstitutionalRecordWarning.MISSING_CURP_TRACEABILITY in resolved.warnings)
    }

    @Test
    fun curpCaseAndWhitespaceDifferencesAreEquivalent() {
        val resolved = resolved(
            resolve(
                student = student().copy(curp = " l6aa100101mdfabc01 "),
                annualEnrollments = listOf(annual(curp = "L6AA100101MDFABC01")),
                preApplications = listOf(preApplication().copy(alumnoCurp = "L6AA 100101MDFABC01"))
            )
        )

        assertFalse(InstitutionalRecordWarning.MISSING_CURP_TRACEABILITY in resolved.warnings)
    }

    @Test
    fun availableAnnualAndPreApplicationCurpsStillConflictWhenStudentCurpIsMissing() {
        assertConflict(
            resolve(
                student = student().copy(curp = ""),
                annualEnrollments = listOf(annual()),
                preApplications = listOf(
                    preApplication().copy(alumnoCurp = "OTRA100101MDFABC01")
                )
            ),
            InstitutionalRecordIdentityConflict.CURP_MISMATCH
        )
    }

    @Test
    fun availableStudentAndPreApplicationCurpsStillConflictWhenAnnualCurpIsMissing() {
        assertConflict(
            resolve(
                annualEnrollments = listOf(annual(curp = "")),
                preApplications = listOf(
                    preApplication().copy(alumnoCurp = "OTRA100101MDFABC01")
                )
            ),
            InstitutionalRecordIdentityConflict.CURP_MISMATCH
        )
    }

    @Test
    fun availableStudentAndAnnualCurpsStillConflictWhenPreApplicationCurpIsMissing() {
        assertConflict(
            resolve(
                student = student().copy(curp = "OTRA100101MDFABC01"),
                preApplications = listOf(preApplication().copy(alumnoCurp = ""))
            ),
            InstitutionalRecordIdentityConflict.CURP_MISMATCH
        )
    }

    @Test
    fun contradictoryEnrollmentIdIsIdentityConflict() {
        assertConflict(
            resolve(student = student().copy(enrollmentId = "S310-999999-9")),
            InstitutionalRecordIdentityConflict.ENROLLMENT_ID_MISMATCH
        )
    }

    @Test
    fun keyAndStudentEnrollmentIdsStillConflictWhenAnnualEnrollmentIdIsMissing() {
        assertConflict(
            resolve(
                student = student().copy(enrollmentId = "S310-999999-9"),
                annualEnrollments = listOf(annual().copy(permanentEnrollmentId = ""))
            ),
            InstitutionalRecordIdentityConflict.ENROLLMENT_ID_MISMATCH
        )
    }

    @Test
    fun contextualAnnualEnrollmentForDifferentStudentIsIdentityConflict() {
        assertConflict(
            resolve(
                annualEnrollments = listOf(
                    annual().copy(studentId = "MASTER-V2-OTRO")
                )
            ),
            InstitutionalRecordIdentityConflict.STUDENT_ID_MISMATCH
        )
    }

    @Test
    fun blankAnnualFieldsAndInvalidGradeAreNotConfirmed() {
        val sparseAnnual = annual().copy(
            permanentEnrollmentId = "",
            requestedGrade = 8
        )
        val sparseKey = key()
        val sparseStudent = student().copy(enrollmentId = "")

        val resolved = resolved(
            resolve(
                key = sparseKey,
                student = sparseStudent,
                annualEnrollments = listOf(sparseAnnual)
            )
        )

        assertUnavailable(resolved.enrollmentId)
        assertEquals(InstitutionalRecordDataQuality.INCONSISTENT, resolved.grade.quality)
        assertTrue(InstitutionalRecordWarning.MISSING_ENROLLMENT_ID_TRACEABILITY in resolved.warnings)
    }

    @Test
    fun contradictoryPreApplicationContextIsVisibleAndNotConfirmed() {
        val contradictory = preApplication().copy(
            cicloEscolar = "2025-2026",
            gradoSolicitado = 3,
            alumnoNombreCompleto = "OTRA ALUMNA"
        )

        val resolved = resolved(resolve(preApplications = listOf(contradictory)))

        assertTrue(InstitutionalRecordWarning.PRE_APPLICATION_CONTEXT_MISMATCH in resolved.warnings)
        assertEquals(InstitutionalRecordDataQuality.INCONSISTENT, resolved.readinessStatus.quality)
        assertEquals(InstitutionalRecordDataQuality.INCONSISTENT, resolved.address.quality)
        assertEquals(InstitutionalRecordDataQuality.INCONSISTENT, resolved.householdPhone.quality)
    }

    @Test
    fun contradictoryPreApplicationSchoolYearIsVisibleByItself() {
        ReadinessStatus.entries.forEach { readiness ->
            val resolved = resolved(
                resolve(
                    preApplications = listOf(
                        preApplication().copy(
                            cicloEscolar = "2025-2026",
                            readinessStatus = readiness
                        )
                    )
                )
            )

            assertTrue(InstitutionalRecordWarning.PRE_APPLICATION_CONTEXT_MISMATCH in resolved.warnings)
            assertEquals(InstitutionalRecordDataQuality.INCONSISTENT, resolved.address.quality)
            assertEquals(InstitutionalRecordDataQuality.INCONSISTENT, resolved.householdPhone.quality)
        }
    }

    @Test
    fun contextualConflictDominatesMissingContactValues() {
        ReadinessStatus.entries.forEach { readiness ->
            val resolved = resolved(
                resolve(
                    preApplications = listOf(
                        preApplication().copy(
                            cicloEscolar = "2025-2026",
                            readinessStatus = readiness,
                            alumnoDomicilio = "",
                            alumnoTelefonoCasa = ""
                        )
                    )
                )
            )

            assertEquals(InstitutionalRecordDataQuality.INCONSISTENT, resolved.address.quality)
            assertEquals(InstitutionalRecordDataQuality.INCONSISTENT, resolved.householdPhone.quality)
            assertNull(resolved.address.value)
            assertNull(resolved.householdPhone.value)
        }
    }

    @Test
    fun studentNameFormattingDifferencesAreEquivalent() {
        val resolved = resolved(
            resolve(
                student = student().copy(fullName = "Alumna   Institucional"),
                preApplications = listOf(
                    preApplication().copy(alumnoNombreCompleto = " ALUMNA INSTITUCIONAL ")
                )
            )
        )

        assertFalse(InstitutionalRecordWarning.PRE_APPLICATION_CONTEXT_MISMATCH in resolved.warnings)
    }

    @Test
    fun substantiveStudentNameDifferenceIsVisibleByItself() {
        val resolved = resolved(
            resolve(preApplications = listOf(preApplication().copy(alumnoNombreCompleto = "OTRA ALUMNA")))
        )

        assertTrue(InstitutionalRecordWarning.PRE_APPLICATION_CONTEXT_MISMATCH in resolved.warnings)
    }

    @Test
    fun incompletePreApplicationContextIsPendingRatherThanContradictory() {
        val incomplete = preApplication().copy(
            cicloEscolar = "",
            alumnoNombreCompleto = "",
            tramite = ""
        )

        val resolved = resolved(resolve(preApplications = listOf(incomplete)))

        assertTrue(InstitutionalRecordWarning.PRE_APPLICATION_CONTEXT_INCOMPLETE in resolved.warnings)
        assertEquals(InstitutionalRecordDataQuality.PENDING, resolved.readinessStatus.quality)
        assertEquals(InstitutionalRecordDataQuality.PENDING, resolved.address.quality)
        assertEquals(InstitutionalRecordDataQuality.PENDING, resolved.householdPhone.quality)
    }

    @Test
    fun missingPreApplicationSchoolYearIsIncompleteByItself() {
        val resolved = resolved(
            resolve(preApplications = listOf(preApplication().copy(cicloEscolar = "")))
        )

        assertTrue(InstitutionalRecordWarning.PRE_APPLICATION_CONTEXT_INCOMPLETE in resolved.warnings)
        assertFalse(InstitutionalRecordWarning.PRE_APPLICATION_CONTEXT_MISMATCH in resolved.warnings)
        assertEquals(InstitutionalRecordDataQuality.PENDING, resolved.address.quality)
        assertEquals(InstitutionalRecordDataQuality.PENDING, resolved.householdPhone.quality)
    }

    @Test
    fun incompleteContextDoesNotPromoteMissingContactValues() {
        val resolved = resolved(
            resolve(
                preApplications = listOf(
                    preApplication().copy(
                        cicloEscolar = "",
                        alumnoDomicilio = "",
                        alumnoTelefonoCasa = ""
                    )
                )
            )
        )

        assertTrue(InstitutionalRecordWarning.PRE_APPLICATION_CONTEXT_INCOMPLETE in resolved.warnings)
        assertUnavailable(resolved.address)
        assertUnavailable(resolved.householdPhone)
    }

    @Test
    fun missingPreApplicationNameIsIncompleteByItself() {
        val resolved = resolved(
            resolve(preApplications = listOf(preApplication().copy(alumnoNombreCompleto = "")))
        )

        assertTrue(InstitutionalRecordWarning.PRE_APPLICATION_CONTEXT_INCOMPLETE in resolved.warnings)
        assertFalse(InstitutionalRecordWarning.PRE_APPLICATION_CONTEXT_MISMATCH in resolved.warnings)
    }

    @Test
    fun missingPreApplicationMovementIsIncompleteByItself() {
        val resolved = resolved(
            resolve(preApplications = listOf(preApplication().copy(tramite = "")))
        )

        assertTrue(InstitutionalRecordWarning.PRE_APPLICATION_CONTEXT_INCOMPLETE in resolved.warnings)
        assertFalse(InstitutionalRecordWarning.PRE_APPLICATION_CONTEXT_MISMATCH in resolved.warnings)
    }

    @Test
    fun schoolYearOuterWhitespaceIsEquivalent() {
        val resolved = resolved(
            resolve(preApplications = listOf(preApplication().copy(cicloEscolar = " 2026-2027 ")))
        )

        assertFalse(InstitutionalRecordWarning.PRE_APPLICATION_CONTEXT_MISMATCH in resolved.warnings)
    }

    @Test
    fun movementCaseAndRepeatedWhitespaceAreEquivalent() {
        val resolved = resolved(
            resolve(preApplications = listOf(preApplication().copy(tramite = " nuevo   ingreso ")))
        )

        assertFalse(InstitutionalRecordWarning.PRE_APPLICATION_CONTEXT_MISMATCH in resolved.warnings)
    }

    @Test
    fun knownContextContradictionRemainsInconsistentWhenOtherEvidenceIsMissing() {
        val mixedEvidence = preApplication().copy(
            cicloEscolar = "",
            alumnoNombreCompleto = "OTRA ALUMNA"
        )

        val resolved = resolved(resolve(preApplications = listOf(mixedEvidence)))

        assertTrue(InstitutionalRecordWarning.PRE_APPLICATION_CONTEXT_INCOMPLETE in resolved.warnings)
        assertTrue(InstitutionalRecordWarning.PRE_APPLICATION_CONTEXT_MISMATCH in resolved.warnings)
        assertEquals(InstitutionalRecordDataQuality.INCONSISTENT, resolved.readinessStatus.quality)
        assertEquals(InstitutionalRecordDataQuality.INCONSISTENT, resolved.address.quality)
        assertEquals(InstitutionalRecordDataQuality.INCONSISTENT, resolved.householdPhone.quality)
    }

    @Test
    fun contradictoryPreApplicationMovementIsVisible() {
        val resolved = resolved(
            resolve(preApplications = listOf(preApplication().copy(tramite = "REINSCRIPCION")))
        )

        assertTrue(InstitutionalRecordWarning.PRE_APPLICATION_CONTEXT_MISMATCH in resolved.warnings)
    }

    @Test
    fun contactQualityFollowsAllReadinessStates() {
        ReadinessStatus.entries.forEach { readiness ->
            val resolved = resolved(
                resolve(preApplications = listOf(preApplication().copy(readinessStatus = readiness)))
            )
            val expected = if (readiness == ReadinessStatus.CONVERTED) {
                InstitutionalRecordDataQuality.CONFIRMED
            } else {
                InstitutionalRecordDataQuality.PENDING
            }

            assertEquals(expected, resolved.address.quality, "Domicilio en $readiness")
            assertEquals(expected, resolved.householdPhone.quality, "Teléfono en $readiness")
            if (readiness != ReadinessStatus.CONVERTED) {
                assertTrue(
                    InstitutionalRecordWarning.PRE_APPLICATION_SYNCHRONIZATION_INCOMPLETE in resolved.warnings
                )
            }
        }
    }

    @Test
    fun blankContactRemainsUnavailableInAllReadinessStates() {
        ReadinessStatus.entries.forEach { readiness ->
            val resolved = resolved(
                resolve(
                    preApplications = listOf(
                        preApplication().copy(
                            readinessStatus = readiness,
                            alumnoDomicilio = "",
                            alumnoTelefonoCasa = ""
                        )
                    )
                )
            )

            assertUnavailable(resolved.address)
            assertUnavailable(resolved.householdPhone)
            if (readiness != ReadinessStatus.CONVERTED) {
                assertTrue(
                    InstitutionalRecordWarning.PRE_APPLICATION_SYNCHRONIZATION_INCOMPLETE in resolved.warnings
                )
            }
        }
    }

    @Test
    fun partialV2StudentDoesNotLeakConcreteStudentDefaults() {
        val student = student().copy(
            schoolYear = "2023-2024",
            address = "Av. Siempre Viva 123, Col. Centro, CDMX",
            healthAlergies = "Polvo, lácteos",
            schoolInsurance = "Vigente",
            attendancePercent = 92,
            documentationStatus = "Completa"
        )

        val resolved = resolved(resolve(student = student))
        val projectedValues = resolved.toString()

        assertEquals("2026-2027", resolved.schoolYear.value)
        assertEquals("DOMICILIO INSTITUCIONAL CORREGIDO", resolved.address.value)
        assertFalse(projectedValues.contains("2023-2024"))
        assertFalse(projectedValues.contains("Av. Siempre Viva 123"))
        assertFalse(projectedValues.contains("Polvo, lácteos"))
        assertFalse(projectedValues.contains("Vigente"))
        assertFalse(projectedValues.contains("92"))
        assertFalse(projectedValues.contains("Completa"))
        assertFalse(projectedValues.contains("Sí, por Secretaría"))
    }

    @Test
    fun completedPreservesMatchingEnrollmentId() {
        val key = completedResult(annual().permanentEnrollmentId).requiredKey()

        assertEquals(annual().permanentEnrollmentId, key.enrollmentId)
        assertIs<InstitutionalStudentRecordResolution.Resolved>(resolve(key = key))
    }

    @Test
    fun completedContradictoryEnrollmentIdIsIdentityConflict() {
        assertConflict(
            resolve(key = completedResult("S310-999999-9").requiredKey()),
            InstitutionalRecordIdentityConflict.ENROLLMENT_ID_MISMATCH
        )
    }

    @Test
    fun completedMissingEnrollmentIdIsInvalidKey() {
        assertMissingEnrollmentIdIsInvalid(
            completedResult("").requiredKey()
        )
    }

    @Test
    fun needsDecisionPreservesMatchingEnrollmentId() {
        val key = needsDecisionResult(annual().permanentEnrollmentId).requiredKey()

        assertEquals(annual().permanentEnrollmentId, key.enrollmentId)
        assertIs<InstitutionalStudentRecordResolution.Resolved>(resolve(key = key))
    }

    @Test
    fun needsDecisionContradictoryEnrollmentIdIsIdentityConflict() {
        assertConflict(
            resolve(key = needsDecisionResult("S310-999999-9").requiredKey()),
            InstitutionalRecordIdentityConflict.ENROLLMENT_ID_MISMATCH
        )
    }

    @Test
    fun needsDecisionMissingEnrollmentIdIsInvalidKey() {
        assertMissingEnrollmentIdIsInvalid(
            needsDecisionResult("").requiredKey()
        )
    }

    @Test
    fun alreadyCompletedPreservesMatchingEnrollmentId() {
        val key = alreadyCompletedResult(annual().permanentEnrollmentId).requiredKey()

        assertEquals(annual().permanentEnrollmentId, key.enrollmentId)
        assertIs<InstitutionalStudentRecordResolution.Resolved>(resolve(key = key))
    }

    @Test
    fun alreadyCompletedContradictoryEnrollmentIdIsIdentityConflict() {
        assertConflict(
            resolve(key = alreadyCompletedResult("S310-999999-9").requiredKey()),
            InstitutionalRecordIdentityConflict.ENROLLMENT_ID_MISMATCH
        )
    }

    @Test
    fun alreadyCompletedMissingEnrollmentIdIsInvalidKey() {
        assertMissingEnrollmentIdIsInvalid(
            alreadyCompletedResult("").requiredKey()
        )
    }

    @Test
    fun malformedNavigableResultDoesNotBroadenAnnualLookup() {
        val malformed = AnnualEnrollmentFlowResult.Completed(
            movement = AnnualEnrollmentMovement.NEW_ENTRY,
            studentId = student().id,
            enrollmentId = annual().permanentEnrollmentId,
            schoolYear = "",
            folio = "",
            status = annual().status,
            groupRequirement = annual().groupPlacementRequirement,
            message = "Registrada"
        )

        val key = malformed.toInstitutionalStudentRecordKey() ?: error("Resultado sin clave")
        val invalid = assertIs<InstitutionalStudentRecordResolution.InvalidResolutionKey>(resolve(key = key))
        assertTrue(InstitutionalRecordKeyError.MISSING_SCHOOL_YEAR in invalid.errors)
        assertTrue(InstitutionalRecordKeyError.MISSING_PRE_APPLICATION_FOLIO in invalid.errors)
    }

    private fun completedResult(enrollmentId: String): AnnualEnrollmentFlowResult.Completed =
        AnnualEnrollmentFlowResult.Completed(
            movement = annual().movement,
            studentId = annual().studentId,
            enrollmentId = enrollmentId,
            schoolYear = annual().schoolYear,
            folio = annual().sourcePreApplicationFolio,
            status = annual().status,
            groupRequirement = annual().groupPlacementRequirement,
            message = "Registrada"
        )

    private fun needsDecisionResult(enrollmentId: String): AnnualEnrollmentFlowResult.NeedsDecision =
        AnnualEnrollmentFlowResult.NeedsDecision(
            studentId = annual().studentId,
            enrollmentId = enrollmentId,
            schoolYear = annual().schoolYear,
            folio = annual().sourcePreApplicationFolio,
            previousGroup = "1B",
            suggestedGroup = "2B",
            reason = "Pendiente"
        )

    private fun alreadyCompletedResult(enrollmentId: String): AnnualEnrollmentFlowResult.AlreadyCompleted =
        AnnualEnrollmentFlowResult.AlreadyCompleted(
            annual().copy(permanentEnrollmentId = enrollmentId)
        )

    private fun AnnualEnrollmentFlowResult.requiredKey(): InstitutionalStudentRecordKey =
        toInstitutionalStudentRecordKey() ?: error("Resultado navegable sin clave")

    private fun assertMissingEnrollmentIdIsInvalid(key: InstitutionalStudentRecordKey) {
        val invalid = assertIs<InstitutionalStudentRecordResolution.InvalidResolutionKey>(resolve(key = key))
        assertEquals(setOf(InstitutionalRecordKeyError.MISSING_ENROLLMENT_ID), invalid.errors)
    }

    private fun resolve(
        key: InstitutionalStudentRecordKey = key(),
        student: Student = student(),
        students: List<Student> = listOf(student),
        annualEnrollments: List<AnnualEnrollmentRecord> = listOf(annual()),
        preApplications: List<PreApplication> = listOf(preApplication())
    ): InstitutionalStudentRecordResolution = resolveInstitutionalStudentRecord(
        key = key,
        students = students,
        annualEnrollments = annualEnrollments,
        preApplications = preApplications
    )

    private fun key() = InstitutionalStudentRecordKey(
        studentId = "MASTER-V2-PRE-L6A",
        schoolYear = "2026-2027",
        sourcePreApplicationFolio = "PRE-L6A",
        enrollmentId = "S310-000321-7"
    )

    private fun student() = Student(
        id = "MASTER-V2-PRE-L6A",
        fullName = "ALUMNA INSTITUCIONAL",
        group = "",
        enrollmentId = "S310-000321-7",
        curp = "L6AA100101MDFABC01",
        preApplicationFolio = "PRE-L6A"
    )

    private fun annual(
        schoolYear: String = "2026-2027",
        folio: String = "PRE-L6A",
        curp: String = "L6AA100101MDFABC01",
        group: String? = null
    ) = AnnualEnrollmentRecord(
        studentId = "MASTER-V2-PRE-L6A",
        normalizedCurp = curp,
        permanentEnrollmentId = "S310-000321-7",
        schoolYear = schoolYear,
        sourcePreApplicationFolio = folio,
        movement = AnnualEnrollmentMovement.NEW_ENTRY,
        requestedGrade = 2,
        groupPlacementRequirement = GroupPlacementRequirement.AssignmentRequired,
        status = AnnualEnrollmentInitialStatus.PENDING_GROUP_ASSIGNMENT,
        assignedGroup = group
    )

    private fun preApplication() = MockPreApplicationData.preApplications.first().copy(
        folio = "PRE-L6A",
        alumnoNombreCompleto = "ALUMNA INSTITUCIONAL",
        alumnoCurp = "L6AA100101MDFABC01",
        alumnoDomicilio = "DOMICILIO INSTITUCIONAL CORREGIDO",
        alumnoTelefonoCasa = "5512345678",
        cicloEscolar = "2026-2027",
        gradoSolicitado = 2,
        readinessStatus = ReadinessStatus.CONVERTED
    )

    private fun resolved(
        result: InstitutionalStudentRecordResolution
    ): InstitutionalStudentRecord =
        assertIs<InstitutionalStudentRecordResolution.Resolved>(result).record

    private fun assertConflict(
        result: InstitutionalStudentRecordResolution,
        expected: InstitutionalRecordIdentityConflict
    ) {
        val conflict = assertIs<InstitutionalStudentRecordResolution.IdentityConflict>(result)
        assertTrue(expected in conflict.conflicts)
    }

    private fun assertUnavailable(field: InstitutionalRecordField<*>) {
        assertNull(field.value)
        assertEquals(InstitutionalRecordDataQuality.UNAVAILABLE, field.quality)
    }

    @Test
    fun isEditableTrueWhenEnviadaAndPending() {
        val preApp = preApplication().copy(
            status = PreApplicationStatus.ENVIADA,
            readinessStatus = ReadinessStatus.PENDING
        )
        val resolved = assertIs<InstitutionalStudentRecordResolution.Resolved>(
            resolve(preApplications = listOf(preApp))
        )
        assertTrue(resolved.isEditable, "isEditable should be true when ENVIADA and PENDING")
        assertTrue(resolved.acceptFolioVisible, "acceptFolioVisible should be true when ENVIADA")
    }

    @Test
    fun isEditableFalseWhenReadinessReady() {
        val preApp = preApplication().copy(
            status = PreApplicationStatus.ENVIADA,
            readinessStatus = ReadinessStatus.READY
        )
        val resolved = assertIs<InstitutionalStudentRecordResolution.Resolved>(
            resolve(preApplications = listOf(preApp))
        )
        assertFalse(resolved.isEditable, "isEditable should be false when readiness is READY")
        assertTrue(resolved.acceptFolioVisible, "acceptFolioVisible should still be true when status is ENVIADA")
    }

    @Test
    fun acceptFolioVisibleFalseWhenStatusAceptada() {
        val preApp = preApplication().copy(
            status = PreApplicationStatus.ACEPTADA,
            readinessStatus = ReadinessStatus.PENDING
        )
        val resolved = assertIs<InstitutionalStudentRecordResolution.Resolved>(
            resolve(preApplications = listOf(preApp))
        )
        assertFalse(resolved.acceptFolioVisible, "acceptFolioVisible should be false when status is ACEPTADA")
        assertTrue(resolved.isEditable, "isEditable should be true when status is ACEPTADA (H1)")
    }

    @Test
    fun bothFalseWhenConverted() {
        val preApp = preApplication().copy(
            status = PreApplicationStatus.ENVIADA,
            readinessStatus = ReadinessStatus.CONVERTED
        )
        val resolved = assertIs<InstitutionalStudentRecordResolution.Resolved>(
            resolve(preApplications = listOf(preApp))
        )
        assertFalse(resolved.isEditable, "isEditable should be false when CONVERTED")
        assertFalse(resolved.acceptFolioVisible, "acceptFolioVisible should be false when CONVERTED")
    }

    @Test
    fun bothFalseWhenBothReadyAndAceptada() {
        val preApp = preApplication().copy(
            status = PreApplicationStatus.ACEPTADA,
            readinessStatus = ReadinessStatus.READY
        )
        val resolved = assertIs<InstitutionalStudentRecordResolution.Resolved>(
            resolve(preApplications = listOf(preApp))
        )
        assertFalse(resolved.isEditable, "isEditable should be false when READY and ACEPTADA")
        assertFalse(resolved.acceptFolioVisible, "acceptFolioVisible should be false when status is ACEPTADA")
    }

    private fun AnnualEnrollmentRecord.preApplicationFolio(): String = sourcePreApplicationFolio
}
