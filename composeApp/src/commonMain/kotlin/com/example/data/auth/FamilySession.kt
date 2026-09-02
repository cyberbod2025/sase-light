package com.example.data.auth

/**
 * Identidad de una familia frente al portal de pre-solicitud. NO es un
 * [AuthSession]: la familia no tiene cuenta de Supabase Auth, no tiene
 * membresia institucional, no tiene [StaffRole], no pasa por
 * [StaffPermissions] -- es un actor distinto, sin login, identificado
 * unicamente por un token de acceso opaco no adivinable (uuid) que el
 * servidor genera al crear la pre-solicitud (migracion 0015,
 * `create_pre_application_with_children`) y devuelve una sola vez.
 *
 * La familia guarda [folio] y [accessToken] (se muestran en pantalla junto
 * con el resto de la confirmacion, igual que el folio ya se mostraba antes)
 * y los vuelve a capturar para consultar o editar su tramite despues -- no
 * hay recuperacion automatica (correo/SMS), es responsabilidad de la
 * familia conservarlos.
 *
 * `institution_id` de la fila que la familia escribe en `pre_applications`
 * lo deriva siempre el servidor (trigger
 * `sase_stamp_pre_application_institution`, migracion 0015), nunca el
 * cliente -- por eso esta clase no expone ningun campo de institucion.
 */
data class FamilySession(
    val folio: String,
    val accessToken: String
)
