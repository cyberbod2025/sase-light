package com.example.auth

/**
 * Credencial demo del personal. Datos sintéticos: correos `example.invalid` y
 * una contraseña de demostración compartida. No representa a ninguna persona
 * real de la escuela ni se persiste en ningún sitio.
 */
data class MockStaffCredential(
    val email: String,
    val password: String,
    val identity: DemoStaffIdentity,
    val active: Boolean = true
)

/**
 * Directorio demo del backend MOCK. Cubre las áreas institucionales más una
 * cuenta dada de baja, para poder demostrar el rechazo por membresía inactiva.
 */
object MockStaffDirectory {

    const val DEMO_PASSWORD: String = "demo-sase-310"

    val DEFAULT: List<MockStaffCredential> = listOf(
        credential("direccion@example.invalid", DemoStaffIdentity.DIRECCION_DEMO),
        credential("secretaria@example.invalid", DemoStaffIdentity.SECRETARIA_DEMO),
        credential("trabajosocial@example.invalid", DemoStaffIdentity.TRABAJO_SOCIAL_DEMO),
        credential("medico@example.invalid", DemoStaffIdentity.MEDICO_DEMO),
        credential("udeii@example.invalid", DemoStaffIdentity.UDEII_DEMO),
        credential("docente@example.invalid", DemoStaffIdentity.DOCENTE_DEMO),
        credential("tutor@example.invalid", DemoStaffIdentity.DOCENTE_TUTOR_DEMO),
        credential("baja@example.invalid", DemoStaffIdentity.DOCENTE_DEMO, active = false)
    )

    private fun credential(
        email: String,
        identity: DemoStaffIdentity,
        active: Boolean = true
    ) = MockStaffCredential(
        email = email,
        password = DEMO_PASSWORD,
        identity = identity,
        active = active
    )
}
