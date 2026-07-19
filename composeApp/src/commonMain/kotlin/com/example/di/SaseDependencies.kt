package com.example.di

import com.example.auth.MockSessionRepository
import com.example.auth.SessionRepository
import com.example.data.repository.AuditRepository
import com.example.data.repository.MockAuditRepositoryImpl
import com.example.data.repository.MockStudentRepositoryImpl
import com.example.data.repository.StudentRepository
import com.example.security.AccessPolicy

/**
 * Contenedor ligero de dependencias, sin framework de DI y sin estado global
 * mutable: cada instancia agrupa las implementaciones que usará un consumidor.
 * Los defaults conservan el comportamiento mock actual de la aplicación.
 */
class SaseDependencies(
    val studentRepository: StudentRepository = MockStudentRepositoryImpl(),
    val auditRepository: AuditRepository = MockAuditRepositoryImpl(),
    val sessionRepository: SessionRepository = MockSessionRepository(),
    val accessPolicy: AccessPolicy = AccessPolicy
)
