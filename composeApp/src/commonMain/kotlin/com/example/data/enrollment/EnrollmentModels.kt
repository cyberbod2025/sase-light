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
    val readyForSignature: Boolean
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
