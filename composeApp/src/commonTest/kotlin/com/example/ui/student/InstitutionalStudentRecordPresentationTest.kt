package com.example.ui.student

import com.example.data.InstitutionalRecordDataQuality
import com.example.data.InstitutionalRecordField
import com.example.data.InstitutionalRecordIdentityConflict
import com.example.data.InstitutionalRecordKeyError
import com.example.data.InstitutionalRecordWarning
import com.example.data.InstitutionalStudentRecord
import com.example.data.InstitutionalStudentRecordKey
import com.example.data.InstitutionalStudentRecordResolution
import com.example.data.Student
import com.example.data.enrollment.AnnualEnrollmentInitialStatus
import com.example.data.enrollment.AnnualEnrollmentMovement
import com.example.data.enrollment.AnnualEnrollmentRecord
import com.example.data.enrollment.GroupPlacementRequirement
import com.example.data.presolicitud.ReadinessStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class InstitutionalStudentRecordPresentationTest {
    @Test
    fun resolvedRecordShowsRealAnnualAndPreApplicationValues() {
        val presentation = content(record())

        assertEquals("S310-000001-1", presentation.value("Matrícula"))
        assertEquals("2026-2027", presentation.value("Ciclo escolar"))
        assertEquals("2°", presentation.value("Grado"))
        assertEquals("PRE-TEST", presentation.value("Folio de pre-solicitud"))
        assertEquals("DOMICILIO INSTITUCIONAL", presentation.value("Domicilio"))
        assertEquals("5512345678", presentation.value("Teléfono del hogar"))
    }

    @Test
    fun pendingGroupRemainsVisiblyPending() {
        val presentation = content(record())
        val group = presentation.field("Grupo")

        assertEquals("Pendiente de asignación", group.value)
        assertEquals(InstitutionalRecordDataQuality.PENDING, group.quality)
    }

    @Test
    fun absentContactIsUnavailableAndNeverUsesStudentDefaults() {
        val sparse = record().copy(
            address = field(null, InstitutionalRecordDataQuality.UNAVAILABLE),
            householdPhone = field(null, InstitutionalRecordDataQuality.UNAVAILABLE),
            warnings = setOf(InstitutionalRecordWarning.PRE_APPLICATION_NOT_FOUND)
        )
        val presentation = content(sparse)
        val rendered = presentation.toString()

        assertEquals("No disponible", presentation.value("Domicilio"))
        assertEquals("No disponible", presentation.value("Teléfono del hogar"))
        assertFalse(rendered.contains("Av. Siempre Viva"))
        assertFalse(rendered.contains("2023-2024"))
        assertFalse(rendered.contains("Vigente"))
        assertFalse(rendered.contains("Expediente auditado"))
    }

    @Test
    fun synchronizationWarningRemainsVisible() {
        val presentation = content(
            record().copy(
                readinessStatus = field(ReadinessStatus.READY, InstitutionalRecordDataQuality.PENDING),
                warnings = setOf(InstitutionalRecordWarning.PRE_APPLICATION_SYNCHRONIZATION_INCOMPLETE)
            )
        )

        assertTrue(presentation.warnings.any { it.contains("sincronización") })
        assertEquals(InstitutionalRecordDataQuality.PENDING, presentation.field("Readiness").quality)
    }

    @Test
    fun inconsistentValueIsNotPresentedAsConfirmed() {
        val presentation = content(
            record().copy(
                householdPhone = field("5512345678", InstitutionalRecordDataQuality.INCONSISTENT)
            )
        )

        assertEquals("5512345678", presentation.value("Teléfono del hogar"))
        assertEquals(
            InstitutionalRecordDataQuality.INCONSISTENT,
            presentation.field("Teléfono del hogar").quality
        )
    }

    @Test
    fun presentationRetainsAllFourQualityStates() {
        val presentation = content(
            record().copy(
                address = field(null, InstitutionalRecordDataQuality.UNAVAILABLE),
                householdPhone = field("5512345678", InstitutionalRecordDataQuality.INCONSISTENT)
            )
        )

        assertEquals(InstitutionalRecordDataQuality.entries.toSet(), presentation.fields.map { it.quality }.toSet())
    }

    @Test
    fun blankCurpIsShownAsUnavailableWithTraceabilityWarning() {
        val presentation = content(
            record().copy(
                curp = "",
                warnings = setOf(InstitutionalRecordWarning.MISSING_CURP_TRACEABILITY)
            )
        )

        assertEquals("No disponible", presentation.curp)
        assertTrue(presentation.warnings.any { it.contains("CURP") })
    }

    @Test
    fun studentIdOnlyRouteResolvesWhenAnnualContextIsUnique() {
        val result = resolveInstitutionalStudentRecordForRoute(
            studentId = "MASTER-V2-PRE-TEST",
            institutionalKey = null,
            students = listOf(student()),
            annualEnrollments = listOf(annual()),
            preApplications = emptyList()
        )

        val presentation = content(
            assertIs<InstitutionalStudentRecordResolution.Resolved>(result).record
        )
        val rendered = presentation.toString()
        assertEquals("2026-2027", presentation.value("Ciclo escolar"))
        assertFalse(rendered.contains("2023-2024"))
        assertFalse(rendered.contains("Av. Siempre Viva"))
        assertFalse(rendered.contains("Vigente"))
    }

    @Test
    fun studentIdOnlyRouteReportsMissingOrAmbiguousAnnualContext() {
        val missing = resolveInstitutionalStudentRecordForRoute(
            studentId = "MASTER-V2-PRE-TEST",
            institutionalKey = null,
            students = listOf(student()),
            annualEnrollments = emptyList(),
            preApplications = emptyList()
        )
        val ambiguous = resolveInstitutionalStudentRecordForRoute(
            studentId = "MASTER-V2-PRE-TEST",
            institutionalKey = null,
            students = listOf(student()),
            annualEnrollments = listOf(
                annual(),
                annual().copy(schoolYear = "2027-2028", sourcePreApplicationFolio = "PRE-NUEVA")
            ),
            preApplications = emptyList()
        )

        assertIs<InstitutionalStudentRecordResolution.AnnualEnrollmentNotFound>(missing)
        assertEquals(2, assertIs<InstitutionalStudentRecordResolution.AmbiguousAnnualEnrollment>(ambiguous).matches)
        val ambiguousPresentation = assertIs<InstitutionalStudentRecordPresentation.Terminal>(
            institutionalStudentRecordPresentation(ambiguous)
        )
        assertTrue(ambiguousPresentation.message.contains("selecciona ciclo y folio"))
    }

    @Test
    fun everyTerminalResolutionProducesAnHonestStateInsteadOfRecordData() {
        val key = key()
        val terminalResolutions = listOf(
            InstitutionalStudentRecordResolution.StudentNotFound(key.studentId) to "Estudiante no encontrado",
            InstitutionalStudentRecordResolution.AmbiguousStudent(key.studentId, 2) to "Identidad estudiantil ambigua",
            InstitutionalStudentRecordResolution.AnnualEnrollmentNotFound(key) to "Anualidad no encontrada",
            InstitutionalStudentRecordResolution.AmbiguousAnnualEnrollment(key, 2) to "Anualidad ambigua",
            InstitutionalStudentRecordResolution.AmbiguousPreApplication("PRE-TEST", 2) to "Pre-solicitud ambigua",
            InstitutionalStudentRecordResolution.IdentityConflict(
                setOf(InstitutionalRecordIdentityConflict.CURP_MISMATCH)
            ) to "Conflicto de identidad",
            InstitutionalStudentRecordResolution.InvalidResolutionKey(
                setOf(InstitutionalRecordKeyError.MISSING_SCHOOL_YEAR)
            ) to "Clave institucional inválida"
        )

        terminalResolutions.forEach { (resolution, expectedTitle) ->
            val presentation = assertIs<InstitutionalStudentRecordPresentation.Terminal>(
                institutionalStudentRecordPresentation(resolution)
            )
            assertEquals(expectedTitle, presentation.title)
            assertTrue(presentation.message.isNotBlank())
            assertFalse(presentation.toString().contains("Av. Siempre Viva"))
        }
    }

    private fun content(record: InstitutionalStudentRecord) =
        assertIs<InstitutionalStudentRecordPresentation.Content>(
            institutionalStudentRecordPresentation(InstitutionalStudentRecordResolution.Resolved(record))
        )

    private fun record() = InstitutionalStudentRecord(
        studentId = "MASTER-V2-PRE-TEST",
        fullName = "ALUMNA INSTITUCIONAL",
        curp = "TEST100101MDFABC01",
        enrollmentId = field("S310-000001-1", InstitutionalRecordDataQuality.CONFIRMED),
        schoolYear = field("2026-2027", InstitutionalRecordDataQuality.CONFIRMED),
        grade = field(2, InstitutionalRecordDataQuality.CONFIRMED),
        group = field(null, InstitutionalRecordDataQuality.PENDING),
        annualStatus = field(
            AnnualEnrollmentInitialStatus.PENDING_GROUP_ASSIGNMENT,
            InstitutionalRecordDataQuality.CONFIRMED
        ),
        movement = field(AnnualEnrollmentMovement.NEW_ENTRY, InstitutionalRecordDataQuality.CONFIRMED),
        preApplicationFolio = "PRE-TEST",
        readinessStatus = field(ReadinessStatus.CONVERTED, InstitutionalRecordDataQuality.CONFIRMED),
        address = field("DOMICILIO INSTITUCIONAL", InstitutionalRecordDataQuality.CONFIRMED),
        householdPhone = field("5512345678", InstitutionalRecordDataQuality.CONFIRMED),
        warnings = emptySet()
    )

    private fun key() = InstitutionalStudentRecordKey(
        studentId = "MASTER-V2-PRE-TEST",
        schoolYear = "2026-2027",
        sourcePreApplicationFolio = "PRE-TEST",
        enrollmentId = "S310-000001-1"
    )

    private fun student() = Student(
        id = "MASTER-V2-PRE-TEST",
        fullName = "ALUMNA INSTITUCIONAL",
        group = "",
        enrollmentId = "S310-000001-1",
        curp = "TEST100101MDFABC01",
        preApplicationFolio = "PRE-TEST"
    )

    private fun annual() = AnnualEnrollmentRecord(
        studentId = "MASTER-V2-PRE-TEST",
        normalizedCurp = "TEST100101MDFABC01",
        permanentEnrollmentId = "S310-000001-1",
        schoolYear = "2026-2027",
        sourcePreApplicationFolio = "PRE-TEST",
        movement = AnnualEnrollmentMovement.NEW_ENTRY,
        requestedGrade = 2,
        groupPlacementRequirement = GroupPlacementRequirement.AssignmentRequired,
        status = AnnualEnrollmentInitialStatus.PENDING_GROUP_ASSIGNMENT
    )

    private fun <T> field(value: T?, quality: InstitutionalRecordDataQuality) =
        InstitutionalRecordField(value, quality)

    private fun InstitutionalStudentRecordPresentation.Content.field(label: String) =
        fields.single { it.label == label }

    private fun InstitutionalStudentRecordPresentation.Content.value(label: String) = field(label).value
}
