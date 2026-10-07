package com.example.data.presolicitud

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FamilyFormCatalogsTest {

    private val allCatalogs = mapOf(
        "servicioMedico" to FamilyFormCatalogs.servicioMedico,
        "tipoSangre" to FamilyFormCatalogs.tipoSangre,
        "saludBucal" to FamilyFormCatalogs.saludBucal,
        "tipoFamilia" to FamilyFormCatalogs.tipoFamilia,
        "tipoVivienda" to FamilyFormCatalogs.tipoVivienda,
        "dispositivoTareas" to FamilyFormCatalogs.dispositivoTareas,
        "medioTransporte" to FamilyFormCatalogs.medioTransporte,
        "personaAtiendeAvisos" to FamilyFormCatalogs.personaAtiendeAvisos,
        "lugarEntreHermanos" to FamilyFormCatalogs.lugarEntreHermanos,
        "integrantesHogar" to FamilyFormCatalogs.integrantesHogar,
        "becaApoyoSocial" to FamilyFormCatalogs.becaApoyoSocial
    )

    @Test
    fun noCatalogIsEmptyHasBlankEntriesOrDuplicates() {
        allCatalogs.forEach { (name, options) ->
            assertTrue(options.isNotEmpty(), "$name esta vacio")
            assertTrue(options.none { it.isBlank() }, "$name tiene opciones en blanco")
            assertEquals(options.size, options.toSet().size, "$name tiene opciones duplicadas")
        }
    }

    @Test
    fun theClearEntryIsNeverAlsoARealOption() {
        allCatalogs.forEach { (name, options) ->
            assertTrue(FamilyFormCatalogs.SIN_ESPECIFICAR !in options, "$name usa la entrada de limpiar como opcion")
        }
    }

    @Test
    fun catalogsOfFieldsStoredInUppercaseAreAlreadyUppercase() {
        // setServicioMedico/setTipoSangre/setPersonaAtiendeAvisos guardan v.uppercase():
        // lo elegido debe ser exactamente lo que queda guardado y se muestra.
        listOf(
            FamilyFormCatalogs.servicioMedico,
            FamilyFormCatalogs.tipoSangre,
            FamilyFormCatalogs.personaAtiendeAvisos
        ).forEach { options -> options.forEach { assertEquals(it.uppercase(), it) } }
    }

    @Test
    fun numericCatalogsOnlyContainWholeNumbersSoTheServerIntegerColumnsAcceptThem() {
        (FamilyFormCatalogs.lugarEntreHermanos + FamilyFormCatalogs.integrantesHogar).forEach {
            assertTrue(it.toIntOrNull() != null && it.toInt() >= 1, "'$it' no es un entero valido >= 1")
        }
    }

    @Test
    fun everyValidationKeyHasAReadableLabelInsteadOfShowingTheInternalKey() {
        // Claves que produce PreApplicationViewModel.validate*(): si se agrega una
        // clave nueva sin etiqueta, el banner volveria a mostrar "servicioMedico".
        val keysUsedByTheForm = listOf(
            "alergiasDetalle", "apellidoPaterno", "aviso", "consentimientoCorresponsabilidad",
            "consentimientoMarcoConvivencia", "consentimientoReglamento", "consentimientoUsoDatos",
            "curp", "documentos", "domicilio", "fechaNac", "grado", "integrantesHogar",
            "medicamentosDetalle", "nombre", "padecimientosDetalle", "parentesco",
            "personaAtiendeAvisos", "personaTramite", "personaTramiteIdentificacion",
            "personaTramiteParentesco", "personaTramiteTelefono", "promedio", "responsable",
            "responsableTel", "telefono", "tipoFamilia", "viveConQuien"
        )
        keysUsedByTheForm.forEach { key ->
            val described = FamilyFormErrorLabels.describe(key)
            assertNotEquals(key, described, "'$key' no tiene etiqueta legible")
            assertTrue("paso" in described, "'$key' no dice en que paso se corrige: $described")
        }
    }

    @Test
    fun anUnmappedKeyIsStillShownNeverBlank() {
        assertEquals("claveNueva", FamilyFormErrorLabels.describe("claveNueva"))
    }
}
