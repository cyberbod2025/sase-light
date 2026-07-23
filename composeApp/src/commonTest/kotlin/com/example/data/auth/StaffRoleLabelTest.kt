package com.example.data.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * La etiqueta institucional es solo presentacion, pero debe existir y ser
 * distinta para cada rol: la interfaz muestra quien es el usuario autorizado.
 */
class StaffRoleLabelTest {

    @Test
    fun everyRoleHasANonBlankLabel() {
        StaffRole.entries.forEach { role ->
            assertTrue(role.institutionalLabel().isNotBlank(), "$role sin etiqueta")
        }
    }

    @Test
    fun labelsAreUniquePerRole() {
        val labels = StaffRole.entries.map { it.institutionalLabel() }
        assertEquals(labels.size, labels.toSet().size, "las etiquetas de rol deben ser unicas")
    }

    @Test
    fun knownRolesMapToExpectedSpanishLabels() {
        assertEquals("Dirección", StaffRole.DIRECCION.institutionalLabel())
        assertEquals("Secretaría", StaffRole.SECRETARIA.institutionalLabel())
        assertEquals("Médico Escolar", StaffRole.MEDICO_ESCOLAR.institutionalLabel())
    }
}
