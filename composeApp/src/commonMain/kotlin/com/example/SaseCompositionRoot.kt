package com.example

import com.example.data.auth.MockAuthRepositoryImpl
import com.example.data.auth.SupabaseAuthRepositoryImpl
import com.example.data.repository.MockAuditRepositoryImpl
import com.example.data.repository.MockStudentRepositoryImpl
import com.example.data.repository.SupabaseAuditRepositoryImpl
import com.example.data.repository.SupabaseStudentRepositoryImpl
import com.example.environment.AppEnvironment
import com.example.environment.AppEnvironmentMode
import com.example.environment.platformAppEnvironmentValues
import com.example.viewmodel.LabViewModel
import com.example.viewmodel.PreApplicationViewModel

sealed interface SaseBootstrap {
    data class Ready(
        val environment: AppEnvironment,
        val viewModel: LabViewModel
    ) : SaseBootstrap

    data class ConfigurationFailure(
        val title: String,
        val message: String
    ) : SaseBootstrap
}

/**
 * Único punto donde un ambiente se convierte en dependencias ejecutables.
 * Ningún modo conectado puede degradarse a MockAuthRepositoryImpl.
 */
object SaseCompositionRoot {
    fun create(values: Map<String, String> = platformAppEnvironmentValues()): SaseBootstrap {
        val environment = runCatching { AppEnvironment.from(values) }.getOrElse { error ->
            return SaseBootstrap.ConfigurationFailure(
                title = "Configuración de ambiente inválida",
                message = error.message ?: "No fue posible validar la configuración de arranque."
            )
        }
        return create(environment)
    }

    fun create(environment: AppEnvironment): SaseBootstrap = when (environment.mode) {
        AppEnvironmentMode.DEMO_LOCAL -> {
            val studentRepository = MockStudentRepositoryImpl()
            val authRepository = MockAuthRepositoryImpl()
            val auditRepository = MockAuditRepositoryImpl()
            // El alta oficial (PreApplicationViewModel) es estatico y no pasa
            // por este constructor: se cablea aqui al mismo repositorio que
            // usa LabViewModel para que ambos lean/escriban el mismo padron
            // maestro segun el ambiente (P1 de Codex en PR #49), a la misma
            // sesion activa para que confirmInitialGroup gatee por rol igual
            // que en SUPABASE_STAGING (P2 de Codex, "Gate official-enrollment
            // writes by the active role"), y a la misma bitacora para que sus
            // mutaciones tambien queden auditadas (P1 de Codex, "Audit
            // official-enrollment mutations").
            PreApplicationViewModel.configureRepositories(studentRepository)
            PreApplicationViewModel.configureAuthSessionProvider { authRepository.session.value }
            PreApplicationViewModel.configureAuditRepository(auditRepository)
            SaseBootstrap.Ready(
                environment = environment,
                viewModel = LabViewModel(
                    appEnvironment = environment,
                    authRepository = authRepository,
                    studentRepository = studentRepository,
                    auditRepository = auditRepository
                )
            )
        }

        AppEnvironmentMode.SUPABASE_STAGING -> {
            val supabase = requireNotNull(environment.supabase)
            val authRepository = SupabaseAuthRepositoryImpl(
                baseUrl = supabase.url,
                apiKey = supabase.publishableKey
            )
            // Expedientes y bitacora leen la sesion viva del repositorio de
            // autenticacion: sin sesion no leen ni escriben nada, y la
            // institucion de cada operacion sale siempre de ahi.
            val sessionProvider = { authRepository.session.value }
            val studentRepository = SupabaseStudentRepositoryImpl(
                baseUrl = supabase.url,
                apiKey = supabase.publishableKey,
                sessionProvider = sessionProvider
            )
            val auditRepository = SupabaseAuditRepositoryImpl(
                baseUrl = supabase.url,
                apiKey = supabase.publishableKey,
                sessionProvider = sessionProvider
            )
            PreApplicationViewModel.configureRepositories(studentRepository)
            PreApplicationViewModel.configureAuthSessionProvider(sessionProvider)
            PreApplicationViewModel.configureAuditRepository(auditRepository)
            SaseBootstrap.Ready(
                environment = environment,
                viewModel = LabViewModel(
                    appEnvironment = environment,
                    authRepository = authRepository,
                    studentRepository = studentRepository,
                    auditRepository = auditRepository
                )
            )
        }

        AppEnvironmentMode.PRODUCTION -> SaseBootstrap.ConfigurationFailure(
            title = "Producción bloqueada de forma segura",
            message = "La persistencia institucional y sus políticas RLS aún no están validadas. " +
                "Este binario no habilita datos demo ni escrituras de producción."
        )
    }
}
