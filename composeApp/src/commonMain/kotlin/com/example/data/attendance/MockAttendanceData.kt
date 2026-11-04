package com.example.data.attendance

import com.example.data.auth.MockStaffDirectory

/**
 * Catálogo y datos sintéticos para asistencia y puntualidad en [DEMO_LOCAL].
 *
 * Mantiene coherencia con `MockStaffDirectory` y `MockSaseData`.
 * Todos los datos son sintéticos y ficticios.
 */
object MockAttendanceData {

    const val GROUP_1A_ID: String = "group-demo-1a"
    const val GROUP_2B_ID: String = "group-demo-2b"

    /** Membresía docente asignada a 1° A. */
    const val TEACHER_1A_MEMBERSHIP_ID: String = "membership-teacher-demo"

    /** Segunda membresía docente asignada a 2° B (caso negativo para 1° A). */
    const val TEACHER_2B_MEMBERSHIP_ID: String = "membership-teacher2-demo"

    val groups: List<TeacherGroup> = listOf(
        TeacherGroup(
            id = GROUP_1A_ID,
            name = "1° A",
            gradeLabel = "Primer grado",
            schoolCycleId = MockStaffDirectory.SCHOOL_CYCLE_ID,
            institutionId = MockStaffDirectory.INSTITUTION_ID
        ),
        TeacherGroup(
            id = GROUP_2B_ID,
            name = "2° B",
            gradeLabel = "Segundo grado",
            schoolCycleId = MockStaffDirectory.SCHOOL_CYCLE_ID,
            institutionId = MockStaffDirectory.INSTITUTION_ID
        )
    )

    val defaultRosters: Map<String, List<GroupStudent>> = mapOf(
        GROUP_1A_ID to listOf(
            GroupStudent(id = "1", fullName = "ALUMNO DEMO 01", listNumber = 1),
            GroupStudent(id = "2", fullName = "ALUMNO DEMO 02", listNumber = 2),
            GroupStudent(id = "3", fullName = "ALUMNO DEMO 03", listNumber = 3),
            GroupStudent(id = "4", fullName = "ALUMNO DEMO 04", listNumber = 4),
            GroupStudent(id = "5", fullName = "ALUMNO DEMO 05", listNumber = 5)
        ),
        GROUP_2B_ID to listOf(
            GroupStudent(id = "6", fullName = "ALUMNO DEMO 06", listNumber = 1),
            GroupStudent(id = "7", fullName = "ALUMNO DEMO 07", listNumber = 2),
            GroupStudent(id = "8", fullName = "ALUMNO DEMO 08", listNumber = 3)
        )
    )

    /** Asignación de grupos por membresía docente. */
    val teacherAssignments: Map<String, List<String>> = mapOf(
        TEACHER_1A_MEMBERSHIP_ID to listOf(GROUP_1A_ID),
        TEACHER_2B_MEMBERSHIP_ID to listOf(GROUP_2B_ID)
    )
}
