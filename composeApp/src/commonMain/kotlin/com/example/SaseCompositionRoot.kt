package com.example

import com.example.data.auth.MockAuthRepositoryImpl
import com.example.data.auth.SupabaseAuthRepositoryImpl
import com.example.data.repository.MockAuditRepositoryImpl
import com.example.data.repository.MockStudentRepositoryImpl
import com.example.environment.AppEnvironment
import com.example.environment.AppEnvironmentMode
import com.example.environment.platformAppEnvironmentValues
import com.example.viewmodel.LabViewModel

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
        AppEnvironmentMode.DEMO_LOCAL -> SaseBootstrap.Ready(
            environment = environment,
            viewModel = LabViewModel(
                appEnvironment = environment,
                authRepository = MockAuthRepositoryImpl(),
                studentRepository = MockStudentRepositoryImpl(),
                auditRepository = MockAuditRepositoryImpl()
            )
        )

        AppEnvironmentMode.SUPABASE_STAGING -> {
            val supabase = requireNotNull(environment.supabase)
            SaseBootstrap.Ready(
                environment = environment,
                viewModel = LabViewModel(
                    appEnvironment = environment,
                    authRepository = SupabaseAuthRepositoryImpl(
                        baseUrl = supabase.url,
                        apiKey = supabase.publishableKey
                    ),
                    studentRepository = MockStudentRepositoryImpl(),
                    auditRepository = MockAuditRepositoryImpl()
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
