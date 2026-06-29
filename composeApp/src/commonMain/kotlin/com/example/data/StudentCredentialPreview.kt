package com.example.data

data class StudentCredentialPreview(
    val enrollmentId: String,
    val preApplicationFolio: String?,
    val fullName: String,
    val curp: String,
    val grade: String,
    val group: String?,
    val schoolYear: String,
    val status: String,
    val photoStatus: String,
    val generatedFromOfficialEnrollment: Boolean
) {
    companion object {
        fun fromStudent(student: Student): StudentCredentialPreview {
            val grade = student.group.takeWhile { it != '\u00b0' && it != '\u00BA' }.ifEmpty { "-" }
            val groupLetter = student.group.dropWhile { it != '\u00b0' && it != '\u00BA' }.drop(1).trim()
            return StudentCredentialPreview(
                enrollmentId = student.enrollmentId,
                preApplicationFolio = student.preApplicationFolio,
                fullName = student.fullName,
                curp = student.curp,
                grade = if (grade.all { it.isDigit() }) "${grade}\u00b0" else grade,
                group = groupLetter.ifEmpty { student.group },
                schoolYear = student.schoolYear,
                status = student.status,
                photoStatus = if (student.photoUrl != null) "Con foto" else "Sin foto",
                generatedFromOfficialEnrollment = student.preApplicationFolio != null
            )
        }
    }
}
