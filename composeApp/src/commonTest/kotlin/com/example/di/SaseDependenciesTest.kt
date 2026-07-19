package com.example.di

import com.example.auth.AuthState
import com.example.auth.DemoStaffIdentity
import com.example.auth.InstitutionRole
import com.example.auth.MockSessionRepository
import com.example.data.MockSaseData
import com.example.viewmodel.LabViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class SaseDependenciesTest {

    @Test
    fun defaultLabViewModelPreservesCurrentBehavior() {
        MockSaseData.resetForTests()
        val viewModel = LabViewModel()
        // Los repositorios default siguen sirviendo los datos mock actuales.
        assertEquals(MockSaseData.students.value, viewModel.saseStudents.value)
        assertEquals(MockSaseData.audits.value, viewModel.saseAudits.value)
        // Sin login visible: la app arranca sin sesión, igual que antes.
        assertIs<AuthState.NoSession>(viewModel.authState.value)
    }

    @Test
    fun dependenciesDefaultsProvideMockImplementations() {
        val dependencies = SaseDependencies()
        assertIs<AuthState.NoSession>(dependencies.sessionRepository.authState.value)
        assertEquals(MockSaseData.students.value, dependencies.studentRepository.students.value)
    }

    @Test
    fun labViewModelAcceptsCustomSessionRepository() {
        val sessionRepository = MockSessionRepository()
        sessionRepository.signInAsDemo(DemoStaffIdentity.SECRETARIA_DEMO)

        val viewModel = LabViewModel(sessionRepository = sessionRepository)

        assertSame(sessionRepository, viewModel.sessionRepository)
        val active = assertIs<AuthState.Active>(viewModel.authState.value)
        assertEquals(setOf(InstitutionRole.SECRETARIA), active.session.membership.roles)
    }

    @Test
    fun labViewModelAcceptsDependenciesContainer() {
        val sessionRepository = MockSessionRepository()
        sessionRepository.signInAsDemo(DemoStaffIdentity.DIRECCION_DEMO)
        val dependencies = SaseDependencies(sessionRepository = sessionRepository)

        val viewModel = LabViewModel(dependencies)

        val active = assertIs<AuthState.Active>(viewModel.authState.value)
        assertEquals(setOf(InstitutionRole.DIRECCION), active.session.membership.roles)
    }

    @Test
    fun twoViewModelsCanUseIsolatedSessions() {
        val firstSession = MockSessionRepository()
        val secondSession = MockSessionRepository()
        val first = LabViewModel(sessionRepository = firstSession)
        val second = LabViewModel(sessionRepository = secondSession)

        firstSession.signInAsDemo(DemoStaffIdentity.MEDICO_DEMO)

        assertIs<AuthState.Active>(first.authState.value)
        assertIs<AuthState.NoSession>(second.authState.value)

        secondSession.signInAsDemo(DemoStaffIdentity.UDEII_DEMO)
        firstSession.signOut()

        assertIs<AuthState.NoSession>(first.authState.value)
        val secondActive = assertIs<AuthState.Active>(second.authState.value)
        assertEquals(setOf(InstitutionRole.UDEII), secondActive.session.membership.roles)
    }
}
