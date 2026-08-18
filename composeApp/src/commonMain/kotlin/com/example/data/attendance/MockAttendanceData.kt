package com.example.data.attendance

import com.example.data.auth.MockStaffDirectory

/**
 * Datos sinteticos de la rebanada de asistencia para [DEMO_LOCAL].
 *
 * Refleja exactamente el escenario de `supabase/seed.sql` (el que se usa para
 * verificar RLS contra Postgres real): dos docentes, dos grupos, alumnos
 * ficticios, un docente asignado al grupo objetivo y otro que NO lo esta.
 *
 * Ningun nombre corresponde a una persona real.
 */
object MockAttendanceData {

    const val GROUP_1A_ID: String = "group-demo-1a"
    const val GROUP_2B_ID: String = "group-demo-2b"

    /** Membresia del docente asignado a 1° A (mismo id que MockStaffDirectory). */
    const val TEACHER_1A_MEMBERSHIP_ID: String = "membership-teacher-demo"

    /** Membresia del docente asignado a 2° B, que NO atiende 1° A. */
    const val TEACHER_2B_MEMBERSHIP_ID: String = "membership-teacher2-demo"

    val groups: List<TeacherGroup> = listOf(
        TeacherGroup(
            id = GROUP_1A_ID,
            name = "1° A",
            gradeLabel = "Primer grado",
            schoolCycleId = MockStaffDirectory.SCHOOL_CYCLE_ID
        ),
        TeacherGroup(
            id = GROUP_2B_ID,
            name = "2° B",
            gradeLabel = "Segundo grado",
            schoolCycleId = MockStaffDirectory.SCHOOL_CYCLE_ID
        )
    )

    val rosters: Map<String, List<GroupStudent>> = mapOf(
        GROUP_1A_ID to listOf(
            GroupStudent("student-demo-1a-01", "Alumna Ficticia Alfa", 1),
            GroupStudent("student-demo-1a-02", "Alumno Ficticio Bravo", 2),
            GroupStudent("student-demo-1a-03", "Alumna Ficticia Charlie", 3),
            GroupStudent("student-demo-1a-04", "Alumno Ficticio Delta", 4),
            GroupStudent("student-demo-1a-05", "Alumna Ficticia Eco", 5)
        ),
        GROUP_2B_ID to listOf(
            GroupStudent("student-demo-2b-01", "Alumno Ficticio Foxtrot", 1),
            GroupStudent("student-demo-2b-02", "Alumna Ficticia Golf", 2),
            GroupStudent("student-demo-2b-03", "Alumno Ficticio Hotel", 3)
        )
    )

    /** membershipId -> grupos asignados. Un grupo sin asignacion no es visible. */
    val assignments: Map<String, List<String>> = mapOf(
        TEACHER_1A_MEMBERSHIP_ID to listOf(GROUP_1A_ID),
        TEACHER_2B_MEMBERSHIP_ID to listOf(GROUP_2B_ID)
    )
}
