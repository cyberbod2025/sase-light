package com.example.data.enrollment

enum class EnrollmentStatus {
    Submitted,
    InReview,
    MissingDocuments,
    ReadyToSign,
    Completed
}

data class Enrollment(
    val id: String,
    val studentFullName: String,
    val curp: String,
    val birthDate: String,
    val gradeGroup: String,
    val schoolYear: String,
    val status: EnrollmentStatus,
    val submittedAt: String,
    val address: Address,
    val contacts: List<Contact>,
    val medicalRecord: MedicalRecord,
    val socioeconomicRecord: SocioeconomicRecord,
    val documents: List<EnrollmentDocument>,
    val consents: List<Consent>,
    val riskFlags: List<RiskFlag>,
    val readyForSignature: Boolean,
    // v1.1A — Photo readiness
    val studentPhotoUrl: String? = null,
    val photoTakenAt: String? = null,
    val photoTakenBy: String? = null,
    val photoForCredential: Boolean = false,
    // v1.1A — Identity & credential
    val presenter: EnrollmentPresenter? = null,
    val authorizedPickups: List<AuthorizedPickup> = emptyList(),
    val identityChecklist: IdentityChecklist = IdentityChecklist()
) {
    val missingDocuments: List<EnrollmentDocument>
        get() = documents.filter { it.status != "Entregado" }

    val isComplete: Boolean
        get() = missingDocuments.isEmpty() && consents.all { it.signed }
}

data class Address(
    val street: String,
    val neighborhood: String,
    val municipality: String,
    val state: String,
    val zipCode: String
)

data class Contact(
    val fullName: String,
    val relation: String,
    val phone: String,
    val email: String,
    val isPrimary: Boolean
)

data class MedicalRecord(
    val bloodType: String,
    val allergies: String,
    val chronicConditions: String,
    val medication: String,
    val emergencyNotes: String
)

data class SocioeconomicRecord(
    val householdSize: Int,
    val primaryIncome: String,
    val internetAccess: Boolean,
    val transportation: String,
    val notes: String
)

data class EnrollmentDocument(
    val name: String,
    val status: String,
    val required: Boolean,
    val reviewedBy: String,
    val notes: String = ""
)

data class Consent(
    val name: String,
    val signed: Boolean,
    val signedBy: String,
    val date: String
)

data class RiskFlag(
    val label: String,
    val severity: String,
    val detail: String
)

// v1.1A — Person who physically presents the student at enrollment
data class EnrollmentPresenter(
    val id: String,
    val enrollmentId: String,
    val studentId: String,
    val fullName: String,
    val relationship: String,
    val phone: String,
    val ineVerified: Boolean,
    val presentAtEnrollment: Boolean,
    val photoUrl: String?,
    val signatureUrl: String?,
    val canPickupStudent: Boolean,
    val registeredBy: String,
    val createdAt: String
)

// v1.1A — People authorized to pick up the student
data class AuthorizedPickup(
    val id: String,
    val studentId: String,
    val fullName: String,
    val relationship: String,
    val phone: String,
    val ineVerified: Boolean,
    val ineCopyUrl: String?,
    val photoUrl: String?,
    val signatureUrl: String?,
    val active: Boolean,
    val notes: String
)

// v1.1A — Identity verification checklist with derived "expediente completo" flag
data class IdentityChecklist(
    val studentPhotographed: Boolean = false,
    val tutorPhotographed: Boolean = false,
    val tutorIdentified: Boolean = false,
    val ineVerified: Boolean = false,
    val authorizedPickupsRegistered: Boolean = false,
    val documentsComplete: Boolean = false
) {
    val expedienteComplete: Boolean
        get() = studentPhotographed && tutorPhotographed && tutorIdentified
                && ineVerified && authorizedPickupsRegistered && documentsComplete
}
