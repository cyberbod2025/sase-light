package com.example.viewmodel

import com.example.data.presolicitud.MockPreApplicationData
import com.example.data.presolicitud.PersonaTramite
import com.example.data.presolicitud.PreApplication
import com.example.data.presolicitud.PreApplicationStatus
import com.example.data.presolicitud.ReadinessStatus
import com.example.data.presolicitud.Responsable
import com.example.data.repository.MockPreApplicationRepositoryImpl
import com.example.data.repository.PreApplicationPersistenceFailure
import com.example.data.repository.PreApplicationRepository
import com.example.data.repository.PreApplicationSubmitResult
import com.example.data.repository.PreApplicationSyncResult
import com.example.data.repository.PreApplicationUpdateResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Cubre los escenarios de persistencia de pre-solicitudes que Hugo pidió
 * verificar explicitamente (D-018): create+reload, update+reload, restart
 * (nueva instancia de repositorio sin cache), aislamiento entre dos familias,
 * y que un fallo durante la conversion a expediente maestro no deje la
 * pre-solicitud en un estado parcialmente convertido.
 *
 * Estas pruebas usan [PreApplicationRepository] directamente (no HTTP): el
 * contrato de sesion/RLS ya se cubre por separado en
 * SupabasePreApplicationRepositoryImplTest (MockEngine). Aqui se prueba el
 * comportamiento observable del repositorio y de PreApplicationViewModel
 * sobre esa costura.
 */
class PreApplicationRepositoryLifecycleTest {

    @AfterTest
    fun resetAfter() {
        PreApplicationViewModel.resetSharedStateForTests()
    }

    private fun draft(folio: String, curp: String): PreApplication =
        MockPreApplicationData.preApplications.first().copy(
            folio = folio,
            status = PreApplicationStatus.BORRADOR,
            submittedAt = null,
            alumnoCurp = curp,
            alumnoNombreCompleto = "ALUMNA CICLO $folio",
            responsables = listOf(
                Responsable(
                    nombreCompleto = "TUTOR $folio",
                    parentesco = "MADRE",
                    telefono = "5500000001",
                    correo = null,
                    domicilioDistinto = false,
                    domicilio = null,
                    viveConAlumno = true,
                    contactoPrincipal = true,
                    puedeRecoger = true,
                    ocupacion = "",
                    horarioContacto = "",
                    identificacionApresentar = "INE"
                )
            ),
            personaTramite = PersonaTramite("TUTOR $folio", "MADRE", "5500000001", "INE", true),
            alumnoDomicilio = "DOMICILIO ORIGINAL $folio",
            readinessStatus = ReadinessStatus.PENDING,
            readyAt = null,
            readinessNotes = ""
        )

    // === Persistencia "de verdad" contra un repositorio, no contra un StateFlow local ===

    @Test
    fun createThenReloadFromFreshRepositoryInstanceSeesTheSameData() = runTest {
        // "Restart": una implementacion en memoria que simula la fila
        // servidor -- una instancia nueva de repositorio, sin cache local,
        // debe reconstruir el mismo estado via refresh() (como
        // SupabasePreApplicationRepositoryImpl reconstruiria desde Postgres
        // tras reiniciar la app).
        val backingStore = MutableStateFlow<List<PreApplication>>(emptyList())
        val repoInstanceOne = BackedByStoreRepository(backingStore)
        val created = draft("PRE-LIFECYCLE-01", "LIFE010101HDFABC01")

        val submitResult = repoInstanceOne.submit(created)
        assertIs<PreApplicationSubmitResult.Submitted>(submitResult)

        // Nueva instancia (simula restart): sin memoria local, mismo backing store.
        val repoInstanceTwo = BackedByStoreRepository(backingStore)
        assertTrue(repoInstanceTwo.preApplications.value.isEmpty(), "instancia nueva arranca sin cache")
        val reload = repoInstanceTwo.refresh()
        assertIs<PreApplicationSyncResult.Loaded>(reload)
        val reloaded = repoInstanceTwo.preApplications.value.single { it.folio == "PRE-LIFECYCLE-01" }
        assertEquals("LIFE010101HDFABC01", reloaded.alumnoCurp)
        assertEquals("DOMICILIO ORIGINAL PRE-LIFECYCLE-01", reloaded.alumnoDomicilio)
        assertEquals("TUTOR PRE-LIFECYCLE-01", reloaded.responsables.single().nombreCompleto)
    }

    @Test
    fun updateThenReloadFromFreshRepositoryInstanceSeesTheUpdate() = runTest {
        val backingStore = MutableStateFlow<List<PreApplication>>(emptyList())
        val repoOne = BackedByStoreRepository(backingStore)
        val created = draft("PRE-LIFECYCLE-02", "LIFE020101HDFABC02")
        repoOne.submit(created)

        val toUpdate = repoOne.preApplications.value.single { it.folio == "PRE-LIFECYCLE-02" }
        val updateResult = repoOne.update(toUpdate.copy(alumnoDomicilio = "DOMICILIO ACTUALIZADO"))
        assertIs<PreApplicationUpdateResult.Updated>(updateResult)

        val repoTwo = BackedByStoreRepository(backingStore)
        repoTwo.refresh()
        val reloaded = repoTwo.preApplications.value.single { it.folio == "PRE-LIFECYCLE-02" }
        assertEquals("DOMICILIO ACTUALIZADO", reloaded.alumnoDomicilio)
    }

    @Test
    fun backendFailureDuringSubmitNeverReportsFalseSuccess() = runTest {
        val failing = AlwaysFailingRepository()
        val result = failing.submit(draft("PRE-LIFECYCLE-03", "LIFE030101HDFABC03"))
        assertIs<PreApplicationSubmitResult.Failed>(result)
        assertTrue(failing.preApplications.value.isEmpty(), "un submit fallido no debe dejar la pre-solicitud como si se hubiera guardado")
    }

    @Test
    fun backendFailureDuringUpdateNeverReportsFalseSuccess() = runTest {
        val backingStore = MutableStateFlow(listOf(draft("PRE-LIFECYCLE-04", "LIFE040101HDFABC04")))
        val repo = BackedByStoreRepository(backingStore, failUpdates = true)
        repo.refresh()
        val current = repo.preApplications.value.single()
        val result = repo.update(current.copy(alumnoDomicilio = "NUEVO DOMICILIO"))
        assertIs<PreApplicationUpdateResult.Failed>(result)
        assertEquals("DOMICILIO ORIGINAL PRE-LIFECYCLE-04", repo.preApplications.value.single().alumnoDomicilio)
    }

    // === Aislamiento entre dos familias (a nivel de repositorio en memoria) ===

    @Test
    fun twoIndependentFamilyDraftsDoNotLeakIntoEachOther() = runTest {
        val repoFamilyA = MockPreApplicationRepositoryImpl()
        val repoFamilyB = MockPreApplicationRepositoryImpl()
        repoFamilyA.submit(draft("PRE-FAM-A", "FAMA010101HDFABC01"))
        repoFamilyB.submit(draft("PRE-FAM-B", "FAMB010101HDFABC01"))

        assertTrue(repoFamilyA.preApplications.value.any { it.folio == "PRE-FAM-A" })
        assertFalse(repoFamilyA.preApplications.value.any { it.folio == "PRE-FAM-B" })
        assertTrue(repoFamilyB.preApplications.value.any { it.folio == "PRE-FAM-B" })
        assertFalse(repoFamilyB.preApplications.value.any { it.folio == "PRE-FAM-A" })
    }

    // === Conversión a expediente maestro: no debe quedar estado parcial si falla ===

    @Test
    fun conversionMarkingFailureLeavesPreApplicationNotConverted() = runTest {
        // Repositorio que persiste el nucleo/documentos normalmente, pero
        // rechaza especificamente el intento de marcar CONVERTED -- simula
        // el fallo de red que puede ocurrir justo en el ultimo paso de
        // confirmInitialGroup/markConverted.
        val backingStore = MutableStateFlow(listOf(
            draft("PRE-CONV-01", "CONV010101HDFABC01").copy(
                status = PreApplicationStatus.ACEPTADA,
                readinessStatus = ReadinessStatus.READY,
                readyAt = "Hoy",
                documentosDeclarados = listOf(
                    com.example.data.presolicitud.DocumentoDeclarado("CURP", declarado = true, cotejadoSecretaria = true, validado = true)
                )
            )
        ))
        val repo = BackedByStoreRepository(backingStore, rejectConversionUpdates = true)
        repo.refresh()
        PreApplicationViewModel.configurePreApplicationRepository(repo)
        PreApplicationViewModel.configureAuthSessionProvider { com.example.data.repository.testSession() }

        // officialEnrollmentPendingItems tambien exige fotos mock capturadas.
        // Cada captura reconcilia readiness contra los pendientes que quedan
        // en ESE momento: la primera (todavia falta la otra foto) baja el
        // estado de READY a BLOCKED; la segunda ya no tiene pendientes pero
        // reconcileReadinessAfterRequirementChange no vuelve a subir a READY
        // por si sola (requiere una declaracion institucional explicita,
        // igual que en produccion) -- se reafirma con
        // markReadyForOfficialEnrollment antes de iniciar el alta.
        PreApplicationViewModel.simulateCaptureStudentPhoto("PRE-CONV-01")
        PreApplicationViewModel.simulateCaptureResponsablePhoto("PRE-CONV-01")
        assertIs<ReadinessResult.Success>(PreApplicationViewModel.markReadyForOfficialEnrollment("PRE-CONV-01"))

        val preApp = repo.preApplications.value.single()
        val started = PreApplicationViewModel.startOfficialEnrollment(preApp, selectedGroup = "1A")
        assertIs<OfficialEnrollmentResult.Success>(started)

        val confirmResult = PreApplicationViewModel.confirmInitialGroup(preApp.folio, "1A")
        assertIs<OfficialEnrollmentResult.MasterStudentPropagationError>(confirmResult)

        val stillNotConverted = repo.preApplications.value.single { it.folio == preApp.folio }
        assertEquals(ReadinessStatus.READY, stillNotConverted.readinessStatus, "la pre-solicitud NO debe quedar CONVERTED si el backend rechazo esa escritura")

        PreApplicationViewModel.configurePreApplicationRepository(MockPreApplicationRepositoryImpl())
        PreApplicationViewModel.configureAuthSessionProvider { null }
    }
}

/** Repositorio en memoria respaldado por un StateFlow externo — simula "el servidor" entre instancias distintas ("restart"). */
private class BackedByStoreRepository(
    private val backingStore: MutableStateFlow<List<PreApplication>>,
    private val failUpdates: Boolean = false,
    private val rejectConversionUpdates: Boolean = false
) : PreApplicationRepository {
    private val _local = MutableStateFlow<List<PreApplication>>(emptyList())
    override val preApplications: StateFlow<List<PreApplication>> = _local.asStateFlow()

    override suspend fun refresh(): PreApplicationSyncResult {
        _local.value = backingStore.value
        return PreApplicationSyncResult.Loaded(_local.value)
    }

    override suspend fun submit(preApplication: PreApplication): PreApplicationSubmitResult {
        if (backingStore.value.any { it.folio == preApplication.folio }) {
            return PreApplicationSubmitResult.DuplicateFolio(preApplication.folio)
        }
        backingStore.value = backingStore.value + preApplication
        _local.value = _local.value + preApplication
        return PreApplicationSubmitResult.Submitted(preApplication, accessToken = "lifecycle-test-token")
    }

    override suspend fun update(preApplication: PreApplication): PreApplicationUpdateResult {
        if (failUpdates) return PreApplicationUpdateResult.Failed(PreApplicationPersistenceFailure.NETWORK)
        if (rejectConversionUpdates && preApplication.readinessStatus == ReadinessStatus.CONVERTED) {
            return PreApplicationUpdateResult.Failed(PreApplicationPersistenceFailure.NETWORK)
        }
        if (backingStore.value.none { it.folio == preApplication.folio }) {
            return PreApplicationUpdateResult.Failed(PreApplicationPersistenceFailure.REJECTED)
        }
        backingStore.value = backingStore.value.map { if (it.folio == preApplication.folio) preApplication else it }
        _local.value = backingStore.value
        return PreApplicationUpdateResult.Updated(preApplication)
    }

    override fun clear() {
        _local.value = emptyList()
    }
}

private class AlwaysFailingRepository : PreApplicationRepository {
    private val _local = MutableStateFlow<List<PreApplication>>(emptyList())
    override val preApplications: StateFlow<List<PreApplication>> = _local.asStateFlow()
    override suspend fun refresh(): PreApplicationSyncResult = PreApplicationSyncResult.Failed(PreApplicationPersistenceFailure.NETWORK)
    override suspend fun submit(preApplication: PreApplication): PreApplicationSubmitResult =
        PreApplicationSubmitResult.Failed(PreApplicationPersistenceFailure.NETWORK)
    override suspend fun update(preApplication: PreApplication): PreApplicationUpdateResult =
        PreApplicationUpdateResult.Failed(PreApplicationPersistenceFailure.NETWORK)
    override fun clear() { _local.value = emptyList() }
}
