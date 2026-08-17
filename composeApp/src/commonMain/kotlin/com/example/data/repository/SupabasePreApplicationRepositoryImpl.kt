package com.example.data.repository

import com.example.data.auth.AuthSession
import com.example.data.auth.FamilySession
import com.example.data.presolicitud.AntecedentesUdeii
import com.example.data.presolicitud.AutorizadoPreSolicitud
import com.example.data.presolicitud.ConsentimientosFamiliares
import com.example.data.presolicitud.ContextoSociofamiliar
import com.example.data.presolicitud.DocumentoDeclarado
import com.example.data.presolicitud.FichaMedicaFamiliar
import com.example.data.presolicitud.PersonaTramite
import com.example.data.presolicitud.PreApplication
import com.example.data.presolicitud.PreApplicationStatus
import com.example.data.presolicitud.ReadinessStatus
import com.example.data.presolicitud.Responsable
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Fila plana de `pre_applications` tal como la devuelve un SELECT (migracion 0015). */
@Serializable
internal data class PreApplicationRow(
    val folio: String,
    @SerialName("institution_id") val institutionId: String,
    val status: String,
    @SerialName("submitted_at") val submittedAt: String? = null,
    val tramite: String = "",
    @SerialName("ciclo_escolar") val cicloEscolar: String = "",
    @SerialName("grado_solicitado") val gradoSolicitado: Int = 0,
    @SerialName("alumno_nombre_completo") val alumnoNombreCompleto: String,
    @SerialName("alumno_curp") val alumnoCurp: String,
    @SerialName("alumno_fecha_nacimiento") val alumnoFechaNacimiento: String = "",
    @SerialName("alumno_sexo") val alumnoSexo: String = "",
    @SerialName("alumno_nacionalidad") val alumnoNacionalidad: String = "",
    @SerialName("alumno_entidad_nacimiento") val alumnoEntidadNacimiento: String = "",
    @SerialName("alumno_domicilio") val alumnoDomicilio: String = "",
    @SerialName("alumno_telefono_casa") val alumnoTelefonoCasa: String = "",
    @SerialName("escuela_procedencia") val escuelaProcedencia: String = "",
    @SerialName("promedio_grado_anterior") val promedioGradoAnterior: Double? = null,
    @SerialName("persona_tramite_nombre") val personaTramiteNombre: String = "",
    @SerialName("persona_tramite_parentesco") val personaTramiteParentesco: String = "",
    @SerialName("persona_tramite_telefono") val personaTramiteTelefono: String = "",
    @SerialName("persona_tramite_identificacion") val personaTramiteIdentificacion: String = "",
    @SerialName("persona_tramite_contacto_principal") val personaTramiteContactoPrincipal: Boolean = false,
    @SerialName("ficha_servicio_medico") val fichaServicioMedico: String = "",
    @SerialName("ficha_numero_afiliacion") val fichaNumeroAfiliacion: String? = null,
    @SerialName("ficha_tipo_sangre") val fichaTipoSangre: String? = null,
    @SerialName("ficha_alergias") val fichaAlergias: String = "",
    @SerialName("ficha_padecimientos") val fichaPadecimientos: String = "",
    @SerialName("ficha_medicamentos") val fichaMedicamentos: String = "",
    @SerialName("ficha_restriccion_fisica") val fichaRestriccionFisica: String = "",
    @SerialName("ficha_usa_lentes") val fichaUsaLentes: Boolean = false,
    @SerialName("ficha_dificultad_visual_auditiva") val fichaDificultadVisualAuditiva: String = "",
    @SerialName("ficha_salud_bucal") val fichaSaludBucal: String = "",
    @SerialName("ficha_cartilla_vacunacion") val fichaCartillaVacunacion: Boolean = false,
    @SerialName("contexto_vive_con_quien") val contextoViveConQuien: String = "",
    @SerialName("contexto_tipo_familia") val contextoTipoFamilia: String = "",
    @SerialName("contexto_hijo_unico") val contextoHijoUnico: Boolean = false,
    @SerialName("contexto_lugar_entre_hermanos") val contextoLugarEntreHermanos: Int = 0,
    @SerialName("contexto_hermanos_en_escuela") val contextoHermanosEnEscuela: Boolean = false,
    @SerialName("contexto_integrantes_hogar") val contextoIntegrantesHogar: Int = 0,
    @SerialName("contexto_sosten_economico") val contextoSostenEconomico: String = "",
    @SerialName("contexto_ingreso_rangos") val contextoIngresoRangos: String = "",
    @SerialName("contexto_tipo_vivienda") val contextoTipoVivienda: String = "",
    @SerialName("contexto_servicios_basicos") val contextoServiciosBasicos: Boolean = false,
    @SerialName("contexto_internet") val contextoInternet: Boolean = false,
    @SerialName("contexto_dispositivo_tareas") val contextoDispositivoTareas: String = "",
    @SerialName("contexto_beca_apoyo") val contextoBecaApoyo: String = "",
    @SerialName("contexto_transporte") val contextoTransporte: String = "",
    @SerialName("contexto_dificultad_materiales") val contextoDificultadMateriales: Boolean = false,
    @SerialName("contexto_atiende_avisos") val contextoAtiendeAvisos: String = "",
    @SerialName("contexto_horario_comunicacion") val contextoHorarioComunicacion: String = "",
    @SerialName("contexto_puede_acudir_citatorios") val contextoPuedeAcudirCitatorios: Boolean = false,
    @SerialName("udeii_antecedente_apoyo") val udeiiAntecedenteApoyo: String = "",
    @SerialName("udeii_terapia_lenguaje") val udeiiTerapiaLenguaje: Boolean = false,
    @SerialName("udeii_apoyo_psicologico") val udeiiApoyoPsicologico: Boolean = false,
    @SerialName("udeii_apoyo_pedagogico") val udeiiApoyoPedagogico: Boolean = false,
    @SerialName("udeii_documentos_disponibles") val udeiiDocumentosDisponibles: String = "",
    @SerialName("udeii_informe_escuela_anterior") val udeiiInformeEscuelaAnterior: Boolean = false,
    @SerialName("udeii_evaluacion_psicopedagogica") val udeiiEvaluacionPsicopedagogica: Boolean = false,
    @SerialName("udeii_plan_intervencion") val udeiiPlanIntervencion: Boolean = false,
    @SerialName("udeii_portafolio") val udeiiPortafolio: Boolean = false,
    @SerialName("udeii_observaciones_familiares") val udeiiObservacionesFamiliares: String = "",
    @SerialName("consentimiento_aviso_privacidad") val consentimientoAvisoPrivacidad: Boolean = false,
    @SerialName("consentimiento_uso_datos_expediente") val consentimientoUsoDatosExpediente: Boolean = false,
    @SerialName("consentimiento_foto_alumno") val consentimientoFotoAlumno: Boolean = false,
    @SerialName("consentimiento_foto_credencial") val consentimientoFotoCredencial: Boolean = false,
    @SerialName("consentimiento_foto_autorizados") val consentimientoFotoAutorizados: Boolean = false,
    @SerialName("consentimiento_comunicacion_whatsapp") val consentimientoComunicacionWhatsapp: Boolean = false,
    @SerialName("consentimiento_reglamento_interno") val consentimientoReglamentoInterno: Boolean = false,
    @SerialName("consentimiento_marco_convivencia") val consentimientoMarcoConvivencia: Boolean = false,
    @SerialName("consentimiento_corresponsabilidad_familiar") val consentimientoCorresponsabilidadFamiliar: Boolean = false,
    @SerialName("observaciones_secretaria") val observacionesSecretaria: String = "",
    @SerialName("motivo_correccion") val motivoCorreccion: String = "",
    @SerialName("readiness_status") val readinessStatus: String = "PENDING",
    @SerialName("ready_at") val readyAt: String? = null,
    @SerialName("readiness_notes") val readinessNotes: String = ""
)

@Serializable
internal data class ResponsableRow(
    val position: Int = 0,
    @SerialName("nombre_completo") val nombreCompleto: String,
    val parentesco: String = "",
    val telefono: String = "",
    val correo: String? = null,
    @SerialName("domicilio_distinto") val domicilioDistinto: Boolean = false,
    val domicilio: String? = null,
    @SerialName("vive_con_alumno") val viveConAlumno: Boolean = false,
    @SerialName("contacto_principal") val contactoPrincipal: Boolean = false,
    @SerialName("puede_recoger") val puedeRecoger: Boolean = false,
    val ocupacion: String = "",
    @SerialName("horario_contacto") val horarioContacto: String = "",
    @SerialName("identificacion_a_presentar") val identificacionAPresentar: String = "",
    @SerialName("pre_application_folio") val preApplicationFolio: String = ""
)

@Serializable
internal data class AutorizadoRow(
    val position: Int = 0,
    @SerialName("nombre_completo") val nombreCompleto: String,
    val parentesco: String = "",
    val telefono: String = "",
    val observaciones: String = "",
    @SerialName("pre_application_folio") val preApplicationFolio: String = ""
)

@Serializable
internal data class DocumentoRow(
    val position: Int = 0,
    val nombre: String,
    val declarado: Boolean = false,
    @SerialName("cotejado_secretaria") val cotejadoSecretaria: Boolean = false,
    val validado: Boolean = false,
    val rechazado: Boolean = false,
    @SerialName("no_aplica") val noAplica: Boolean = false,
    val observacion: String = "",
    @SerialName("pre_application_folio") val preApplicationFolio: String = ""
)

/** Respuesta de `create_pre_application_with_children` (migracion 0015): el folio nuevo y su token de acceso, unica vez que el token se devuelve. */
@Serializable
internal data class CreatePreApplicationResponse(
    val folio: String,
    @SerialName("access_token") val accessToken: String
)

/** Respuesta de `get_pre_application_with_children` (migracion 0015): unica via de lectura familiar (sin RLS por sesion, ver comentario de la migracion). */
@Serializable
internal data class PreApplicationWithChildrenResponse(
    val record: PreApplicationRow,
    val responsables: List<ResponsableRow> = emptyList(),
    val autorizados: List<AutorizadoRow> = emptyList(),
    val documentos: List<DocumentoRow> = emptyList()
)

internal fun PreApplicationRow.toDomain(
    responsables: List<ResponsableRow>,
    autorizados: List<AutorizadoRow>,
    documentos: List<DocumentoRow>
): PreApplication = PreApplication(
    folio = folio,
    status = runCatching { PreApplicationStatus.valueOf(status) }.getOrDefault(PreApplicationStatus.BORRADOR),
    submittedAt = submittedAt,
    tramite = tramite,
    cicloEscolar = cicloEscolar,
    gradoSolicitado = gradoSolicitado,
    alumnoNombreCompleto = alumnoNombreCompleto,
    alumnoCurp = alumnoCurp,
    alumnoFechaNacimiento = alumnoFechaNacimiento,
    alumnoSexo = alumnoSexo,
    alumnoNacionalidad = alumnoNacionalidad,
    alumnoEntidadNacimiento = alumnoEntidadNacimiento,
    alumnoDomicilio = alumnoDomicilio,
    alumnoTelefonoCasa = alumnoTelefonoCasa,
    escuelaProcedencia = escuelaProcedencia,
    promedioGradoAnterior = promedioGradoAnterior,
    personaTramite = PersonaTramite(
        nombreCompleto = personaTramiteNombre,
        parentesco = personaTramiteParentesco,
        telefono = personaTramiteTelefono,
        identificacionPresentada = personaTramiteIdentificacion,
        usarComoContactoPrincipal = personaTramiteContactoPrincipal
    ),
    responsables = responsables.sortedBy { it.position }.map {
        Responsable(
            nombreCompleto = it.nombreCompleto,
            parentesco = it.parentesco,
            telefono = it.telefono,
            correo = it.correo,
            domicilioDistinto = it.domicilioDistinto,
            domicilio = it.domicilio,
            viveConAlumno = it.viveConAlumno,
            contactoPrincipal = it.contactoPrincipal,
            puedeRecoger = it.puedeRecoger,
            ocupacion = it.ocupacion,
            horarioContacto = it.horarioContacto,
            identificacionApresentar = it.identificacionAPresentar
        )
    },
    autorizados = autorizados.sortedBy { it.position }.map {
        AutorizadoPreSolicitud(
            nombreCompleto = it.nombreCompleto,
            parentesco = it.parentesco,
            telefono = it.telefono,
            observaciones = it.observaciones
        )
    },
    fichaMedicaFamiliar = FichaMedicaFamiliar(
        servicioMedico = fichaServicioMedico,
        numeroAfiliacion = fichaNumeroAfiliacion,
        tipoSangre = fichaTipoSangre,
        alergias = fichaAlergias,
        padecimientos = fichaPadecimientos,
        medicamentos = fichaMedicamentos,
        restriccionFisica = fichaRestriccionFisica,
        usaLentes = fichaUsaLentes,
        dificultadVisualAuditiva = fichaDificultadVisualAuditiva,
        saludBucal = fichaSaludBucal,
        cartillaVacunacion = fichaCartillaVacunacion
    ),
    contextoSociofamiliar = ContextoSociofamiliar(
        viveConQuien = contextoViveConQuien,
        tipoFamilia = contextoTipoFamilia,
        hijoUnico = contextoHijoUnico,
        lugarEntreHermanos = contextoLugarEntreHermanos,
        hermanosEnEscuela = contextoHermanosEnEscuela,
        integrantesHogar = contextoIntegrantesHogar,
        sostenEconomico = contextoSostenEconomico,
        ingresoRangos = contextoIngresoRangos,
        tipoVivienda = contextoTipoVivienda,
        serviciosBásicos = contextoServiciosBasicos,
        internet = contextoInternet,
        dispositivoTareas = contextoDispositivoTareas,
        becaApoyo = contextoBecaApoyo,
        transporte = contextoTransporte,
        dificultadMateriales = contextoDificultadMateriales,
        atiendeAvisos = contextoAtiendeAvisos,
        horarioComunicacion = contextoHorarioComunicacion,
        puedeAcudirCitatorios = contextoPuedeAcudirCitatorios
    ),
    antecedentesUdeii = AntecedentesUdeii(
        antecedenteApoyo = udeiiAntecedenteApoyo,
        terapiaLenguaje = udeiiTerapiaLenguaje,
        apoyoPsicologico = udeiiApoyoPsicologico,
        apoyoPedagogico = udeiiApoyoPedagogico,
        documentosDisponibles = udeiiDocumentosDisponibles,
        informeEscuelaAnterior = udeiiInformeEscuelaAnterior,
        evaluacionPsicopedagogica = udeiiEvaluacionPsicopedagogica,
        planIntervencion = udeiiPlanIntervencion,
        portafolio = udeiiPortafolio,
        observacionesFamiliares = udeiiObservacionesFamiliares
    ),
    documentosDeclarados = documentos.sortedBy { it.position }.map {
        DocumentoDeclarado(
            nombre = it.nombre,
            declarado = it.declarado,
            cotejadoSecretaria = it.cotejadoSecretaria,
            validado = it.validado,
            rechazado = it.rechazado,
            noAplica = it.noAplica,
            observacion = it.observacion
        )
    },
    consentimientos = ConsentimientosFamiliares(
        avisoPrivacidad = consentimientoAvisoPrivacidad,
        usoDatosExpediente = consentimientoUsoDatosExpediente,
        fotoAlumno = consentimientoFotoAlumno,
        fotoCredencial = consentimientoFotoCredencial,
        fotoAutorizados = consentimientoFotoAutorizados,
        comunicacionWhatsapp = consentimientoComunicacionWhatsapp,
        reglamentoInterno = consentimientoReglamentoInterno,
        marcoConvivencia = consentimientoMarcoConvivencia,
        corresponsabilidadFamiliar = consentimientoCorresponsabilidadFamiliar
    ),
    observacionesSecretaria = observacionesSecretaria,
    motivoCorreccion = motivoCorreccion,
    readinessStatus = runCatching { ReadinessStatus.valueOf(readinessStatus) }.getOrDefault(ReadinessStatus.PENDING),
    readyAt = readyAt,
    readinessNotes = readinessNotes
)

/** Construye el jsonb plano que esperan las RPC `x(...)` de la migracion 0015. */
internal fun PreApplication.toRecordJson(): JsonObject = buildJsonObject {
    put("folio", folio)
    put("status", status.name)
    submittedAt?.let { put("submitted_at", it) } ?: put("submitted_at", JsonPrimitive(null as String?))
    put("tramite", tramite)
    put("ciclo_escolar", cicloEscolar)
    put("grado_solicitado", gradoSolicitado)
    put("alumno_nombre_completo", alumnoNombreCompleto)
    put("alumno_curp", alumnoCurp.trim().uppercase())
    put("alumno_fecha_nacimiento", alumnoFechaNacimiento)
    put("alumno_sexo", alumnoSexo)
    put("alumno_nacionalidad", alumnoNacionalidad)
    put("alumno_entidad_nacimiento", alumnoEntidadNacimiento)
    put("alumno_domicilio", alumnoDomicilio)
    put("alumno_telefono_casa", alumnoTelefonoCasa)
    put("escuela_procedencia", escuelaProcedencia)
    put("promedio_grado_anterior", promedioGradoAnterior)
    put("persona_tramite_nombre", personaTramite.nombreCompleto)
    put("persona_tramite_parentesco", personaTramite.parentesco)
    put("persona_tramite_telefono", personaTramite.telefono)
    put("persona_tramite_identificacion", personaTramite.identificacionPresentada)
    put("persona_tramite_contacto_principal", personaTramite.usarComoContactoPrincipal)
    put("ficha_servicio_medico", fichaMedicaFamiliar.servicioMedico)
    put("ficha_numero_afiliacion", fichaMedicaFamiliar.numeroAfiliacion)
    put("ficha_tipo_sangre", fichaMedicaFamiliar.tipoSangre)
    put("ficha_alergias", fichaMedicaFamiliar.alergias)
    put("ficha_padecimientos", fichaMedicaFamiliar.padecimientos)
    put("ficha_medicamentos", fichaMedicaFamiliar.medicamentos)
    put("ficha_restriccion_fisica", fichaMedicaFamiliar.restriccionFisica)
    put("ficha_usa_lentes", fichaMedicaFamiliar.usaLentes)
    put("ficha_dificultad_visual_auditiva", fichaMedicaFamiliar.dificultadVisualAuditiva)
    put("ficha_salud_bucal", fichaMedicaFamiliar.saludBucal)
    put("ficha_cartilla_vacunacion", fichaMedicaFamiliar.cartillaVacunacion)
    put("contexto_vive_con_quien", contextoSociofamiliar.viveConQuien)
    put("contexto_tipo_familia", contextoSociofamiliar.tipoFamilia)
    put("contexto_hijo_unico", contextoSociofamiliar.hijoUnico)
    put("contexto_lugar_entre_hermanos", contextoSociofamiliar.lugarEntreHermanos)
    put("contexto_hermanos_en_escuela", contextoSociofamiliar.hermanosEnEscuela)
    put("contexto_integrantes_hogar", contextoSociofamiliar.integrantesHogar)
    put("contexto_sosten_economico", contextoSociofamiliar.sostenEconomico)
    put("contexto_ingreso_rangos", contextoSociofamiliar.ingresoRangos)
    put("contexto_tipo_vivienda", contextoSociofamiliar.tipoVivienda)
    put("contexto_servicios_basicos", contextoSociofamiliar.serviciosBásicos)
    put("contexto_internet", contextoSociofamiliar.internet)
    put("contexto_dispositivo_tareas", contextoSociofamiliar.dispositivoTareas)
    put("contexto_beca_apoyo", contextoSociofamiliar.becaApoyo)
    put("contexto_transporte", contextoSociofamiliar.transporte)
    put("contexto_dificultad_materiales", contextoSociofamiliar.dificultadMateriales)
    put("contexto_atiende_avisos", contextoSociofamiliar.atiendeAvisos)
    put("contexto_horario_comunicacion", contextoSociofamiliar.horarioComunicacion)
    put("contexto_puede_acudir_citatorios", contextoSociofamiliar.puedeAcudirCitatorios)
    put("udeii_antecedente_apoyo", antecedentesUdeii.antecedenteApoyo)
    put("udeii_terapia_lenguaje", antecedentesUdeii.terapiaLenguaje)
    put("udeii_apoyo_psicologico", antecedentesUdeii.apoyoPsicologico)
    put("udeii_apoyo_pedagogico", antecedentesUdeii.apoyoPedagogico)
    put("udeii_documentos_disponibles", antecedentesUdeii.documentosDisponibles)
    put("udeii_informe_escuela_anterior", antecedentesUdeii.informeEscuelaAnterior)
    put("udeii_evaluacion_psicopedagogica", antecedentesUdeii.evaluacionPsicopedagogica)
    put("udeii_plan_intervencion", antecedentesUdeii.planIntervencion)
    put("udeii_portafolio", antecedentesUdeii.portafolio)
    put("udeii_observaciones_familiares", antecedentesUdeii.observacionesFamiliares)
    put("consentimiento_aviso_privacidad", consentimientos.avisoPrivacidad)
    put("consentimiento_uso_datos_expediente", consentimientos.usoDatosExpediente)
    put("consentimiento_foto_alumno", consentimientos.fotoAlumno)
    put("consentimiento_foto_credencial", consentimientos.fotoCredencial)
    put("consentimiento_foto_autorizados", consentimientos.fotoAutorizados)
    put("consentimiento_comunicacion_whatsapp", consentimientos.comunicacionWhatsapp)
    put("consentimiento_reglamento_interno", consentimientos.reglamentoInterno)
    put("consentimiento_marco_convivencia", consentimientos.marcoConvivencia)
    put("consentimiento_corresponsabilidad_familiar", consentimientos.corresponsabilidadFamiliar)
    put("observaciones_secretaria", observacionesSecretaria)
    put("motivo_correccion", motivoCorreccion)
    put("readiness_status", readinessStatus.name)
    readyAt?.let { put("ready_at", it) } ?: put("ready_at", JsonPrimitive(null as String?))
    put("readiness_notes", readinessNotes)
}

internal fun PreApplication.toResponsablesJson(): JsonArray = buildJsonArray {
    responsables.forEachIndexed { index, r ->
        add(buildJsonObject {
            put("position", index)
            put("nombre_completo", r.nombreCompleto)
            put("parentesco", r.parentesco)
            put("telefono", r.telefono)
            r.correo?.let { put("correo", it) } ?: put("correo", JsonPrimitive(null as String?))
            put("domicilio_distinto", r.domicilioDistinto)
            r.domicilio?.let { put("domicilio", it) } ?: put("domicilio", JsonPrimitive(null as String?))
            put("vive_con_alumno", r.viveConAlumno)
            put("contacto_principal", r.contactoPrincipal)
            put("puede_recoger", r.puedeRecoger)
            put("ocupacion", r.ocupacion)
            put("horario_contacto", r.horarioContacto)
            put("identificacion_a_presentar", r.identificacionApresentar)
        })
    }
}

internal fun PreApplication.toAutorizadosJson(): JsonArray = buildJsonArray {
    autorizados.forEachIndexed { index, a ->
        add(buildJsonObject {
            put("position", index)
            put("nombre_completo", a.nombreCompleto)
            put("parentesco", a.parentesco)
            put("telefono", a.telefono)
            put("observaciones", a.observaciones)
        })
    }
}

internal fun PreApplication.toDocumentosJson(): JsonArray = buildJsonArray {
    documentosDeclarados.forEachIndexed { index, d ->
        add(buildJsonObject {
            put("position", index)
            put("nombre", d.nombre)
            put("declarado", d.declarado)
            put("cotejado_secretaria", d.cotejadoSecretaria)
            put("validado", d.validado)
            put("rechazado", d.rechazado)
            put("no_aplica", d.noAplica)
            put("observacion", d.observacion)
        })
    }
}

/**
 * Contexto de autorizacion resuelto para esta invocacion. Unifica a los dos
 * actores que tocan pre-solicitudes -- SECRETARIA (via [AuthSession], JWT
 * real de Supabase Auth, filtra por institucion, autorizado por RLS via
 * REVIEW_PRE_APPLICATION) y la familia (via [FamilySession], SIN cuenta ni
 * JWT -- solo el token de acceso opaco de su propia pre-solicitud, migracion
 * 0015) -- sin forzar a ninguno de los dos por el modelo del otro. Para
 * SECRETARIA, RLS sigue siendo la autorizacion real. Para la familia, al no
 * existir JWT, RLS no puede aplicar: toda su lectura/escritura pasa por las
 * RPCs `security definer` que validan el token a mano (ver
 * [fetchByToken]/`update`).
 */
private sealed class PreApplicationAuthContext {
    abstract val accessToken: String

    data class Staff(val session: AuthSession) : PreApplicationAuthContext() {
        override val accessToken: String get() = session.accessToken
    }

    data class Family(val session: FamilySession) : PreApplicationAuthContext() {
        override val accessToken: String get() = session.accessToken
    }
}

/**
 * Pre-solicitudes persistidas en Supabase via PostgREST + las RPC atomicas
 * de la migracion 0015. Ver [PreApplicationAuthContext] sobre el doble
 * actor (personal/familia). Reglas que esta clase no delega en la interfaz:
 *  - Sin sesion (ni staff ni familia) no se lee ni se escribe nada.
 *  - `institution_id`/`family_profile_id` los deriva SIEMPRE el servidor
 *    (trigger de la migracion 0015), nunca el modelo recibido.
 *  - submit()/update() usan las RPC atomicas: fila padre + 3 colecciones
 *    hijas en una sola transaccion de servidor (mismo criterio que 0011).
 */
class SupabasePreApplicationRepositoryImpl(
    private val baseUrl: String,
    private val apiKey: String,
    private val staffSessionProvider: () -> AuthSession?,
    private val familySessionProvider: () -> FamilySession?,
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) : PreApplicationRepository {

    private val _preApplications = MutableStateFlow<List<PreApplication>>(emptyList())
    override val preApplications: StateFlow<List<PreApplication>> = _preApplications.asStateFlow()

    private companion object {
        const val PARENT_COLUMNS = "folio,institution_id,status,submitted_at,tramite," +
            "ciclo_escolar,grado_solicitado,alumno_nombre_completo,alumno_curp,alumno_fecha_nacimiento," +
            "alumno_sexo,alumno_nacionalidad,alumno_entidad_nacimiento,alumno_domicilio,alumno_telefono_casa," +
            "escuela_procedencia,promedio_grado_anterior,persona_tramite_nombre,persona_tramite_parentesco," +
            "persona_tramite_telefono,persona_tramite_identificacion,persona_tramite_contacto_principal," +
            "ficha_servicio_medico,ficha_numero_afiliacion,ficha_tipo_sangre,ficha_alergias,ficha_padecimientos," +
            "ficha_medicamentos,ficha_restriccion_fisica,ficha_usa_lentes,ficha_dificultad_visual_auditiva," +
            "ficha_salud_bucal,ficha_cartilla_vacunacion,contexto_vive_con_quien,contexto_tipo_familia," +
            "contexto_hijo_unico,contexto_lugar_entre_hermanos,contexto_hermanos_en_escuela," +
            "contexto_integrantes_hogar,contexto_sosten_economico,contexto_ingreso_rangos,contexto_tipo_vivienda," +
            "contexto_servicios_basicos,contexto_internet,contexto_dispositivo_tareas,contexto_beca_apoyo," +
            "contexto_transporte,contexto_dificultad_materiales,contexto_atiende_avisos," +
            "contexto_horario_comunicacion,contexto_puede_acudir_citatorios,udeii_antecedente_apoyo," +
            "udeii_terapia_lenguaje,udeii_apoyo_psicologico,udeii_apoyo_pedagogico,udeii_documentos_disponibles," +
            "udeii_informe_escuela_anterior,udeii_evaluacion_psicopedagogica,udeii_plan_intervencion," +
            "udeii_portafolio,udeii_observaciones_familiares,consentimiento_aviso_privacidad," +
            "consentimiento_uso_datos_expediente,consentimiento_foto_alumno,consentimiento_foto_credencial," +
            "consentimiento_foto_autorizados,consentimiento_comunicacion_whatsapp,consentimiento_reglamento_interno," +
            "consentimiento_marco_convivencia,consentimiento_corresponsabilidad_familiar,observaciones_secretaria," +
            "motivo_correccion,readiness_status,ready_at,readiness_notes"
        const val RESPONSABLE_COLUMNS = "pre_application_folio,position,nombre_completo,parentesco,telefono," +
            "correo,domicilio_distinto,domicilio,vive_con_alumno,contacto_principal,puede_recoger,ocupacion," +
            "horario_contacto,identificacion_a_presentar"
        const val AUTORIZADO_COLUMNS = "pre_application_folio,position,nombre_completo,parentesco,telefono,observaciones"
        const val DOCUMENTO_COLUMNS = "pre_application_folio,position,nombre,declarado,cotejado_secretaria," +
            "validado,rechazado,no_aplica,observacion"
        const val CURP_CONSTRAINT = "pre_applications_institution_curp_key"
    }

    private fun currentAuth(): PreApplicationAuthContext? =
        staffSessionProvider()?.let { PreApplicationAuthContext.Staff(it) }
            ?: familySessionProvider()?.let { PreApplicationAuthContext.Family(it) }

    override suspend fun refresh(): PreApplicationSyncResult =
        when (val auth = currentAuth()) {
            null -> PreApplicationSyncResult.Failed(PreApplicationPersistenceFailure.NO_SESSION)
            is PreApplicationAuthContext.Staff -> refreshForStaff(auth)
            is PreApplicationAuthContext.Family -> refreshForFamily(auth)
        }

    private suspend fun refreshForStaff(auth: PreApplicationAuthContext.Staff): PreApplicationSyncResult {
      return try {
        val parents = fetchTable<PreApplicationRow>(auth, "pre_applications", PARENT_COLUMNS)
            ?: return PreApplicationSyncResult.Failed(PreApplicationPersistenceFailure.NETWORK)
        val responsables = fetchTable<ResponsableRow>(auth, "pre_application_responsables", RESPONSABLE_COLUMNS)
            ?: return PreApplicationSyncResult.Failed(PreApplicationPersistenceFailure.NETWORK)
        val autorizados = fetchTable<AutorizadoRow>(auth, "pre_application_autorizados", AUTORIZADO_COLUMNS)
            ?: return PreApplicationSyncResult.Failed(PreApplicationPersistenceFailure.NETWORK)
        val documentos = fetchTable<DocumentoRow>(auth, "pre_application_documentos", DOCUMENTO_COLUMNS)
            ?: return PreApplicationSyncResult.Failed(PreApplicationPersistenceFailure.NETWORK)

        val responsablesByFolio = responsables.groupBy { it.preApplicationFolio }
        val autorizadosByFolio = autorizados.groupBy { it.preApplicationFolio }
        val documentosByFolio = documentos.groupBy { it.preApplicationFolio }

        val loaded = parents.map {
            it.toDomain(
                responsables = responsablesByFolio[it.folio].orEmpty(),
                autorizados = autorizadosByFolio[it.folio].orEmpty(),
                documentos = documentosByFolio[it.folio].orEmpty()
            )
        }

        // Sesion pudo cambiar mientras esta peticion estaba en vuelo -- no
        // publicar una respuesta de una sesion ya obsoleta (mismo
        // criterio que SupabaseStudentRepositoryImpl.refresh()).
        if (currentAuth()?.accessToken != auth.accessToken) {
            return PreApplicationSyncResult.Failed(PreApplicationPersistenceFailure.NO_SESSION)
        }
        _preApplications.value = loaded
        PreApplicationSyncResult.Loaded(loaded)
      } catch (e: Exception) {
        PreApplicationSyncResult.Failed(PreApplicationPersistenceFailure.NETWORK)
      }
    }

    /**
     * La familia nunca lista pre-solicitudes: solo tiene el token de la
     * unica que le pertenece (ver [FamilySession]). Sin RLS por sesion, la
     * unica via de lectura es [fetchByToken] contra la RPC `security
     * definer` -- un GET directo con `Authorization: Bearer` no aplica aqui.
     */
    private suspend fun refreshForFamily(auth: PreApplicationAuthContext.Family): PreApplicationSyncResult {
        val loaded = fetchByToken(auth.session.folio, auth.session.accessToken)
            ?: return PreApplicationSyncResult.Failed(PreApplicationPersistenceFailure.REJECTED)

        if (currentAuth()?.accessToken != auth.accessToken) {
            return PreApplicationSyncResult.Failed(PreApplicationPersistenceFailure.NO_SESSION)
        }
        _preApplications.value = listOf(loaded)
        return PreApplicationSyncResult.Loaded(listOf(loaded))
    }

    private suspend inline fun <reified T> fetchTable(
        auth: PreApplicationAuthContext.Staff,
        table: String,
        columns: String
    ): List<T>? {
        val response = httpClient.get("$baseUrl/rest/v1/$table") {
            authHeaders(auth)
            url {
                parameters.append("select", columns)
                parameters.append("institution_id", "eq.${auth.session.institutionId}")
            }
        }
        return if (response.status == HttpStatusCode.OK) response.body<List<T>>() else null
    }

    /**
     * Unica via de LECTURA familiar: RPC `security definer` que valida el
     * token a mano (sin JWT, RLS no puede autorizar a la familia -- ver
     * migracion 0015). `access_token` nunca vuelve en la respuesta.
     */
    private suspend fun fetchByToken(folio: String, accessToken: String): PreApplication? {
      return try {
        val response = httpClient.post("$baseUrl/rest/v1/rpc/get_pre_application_with_children") {
            header("apikey", apiKey)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("p_folio", folio)
                put("p_access_token", accessToken)
            })
        }
        if (response.status != HttpStatusCode.OK) return null
        val body = response.body<PreApplicationWithChildrenResponse?>() ?: return null
        body.record.toDomain(body.responsables, body.autorizados, body.documentos)
      } catch (e: Exception) {
        null
      }
    }

    override suspend fun submit(preApplication: PreApplication): PreApplicationSubmitResult {
        val auth = currentAuth() ?: return PreApplicationSubmitResult.Failed(PreApplicationPersistenceFailure.NO_SESSION)
        if (preApplication.alumnoNombreCompleto.isBlank() || preApplication.alumnoCurp.isBlank()) {
            return PreApplicationSubmitResult.Failed(PreApplicationPersistenceFailure.REJECTED)
        }

        val response = try {
            httpClient.post("$baseUrl/rest/v1/rpc/create_pre_application_with_children") {
                authHeaders(auth)
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("p_record", preApplication.toRecordJson())
                    put("p_responsables", preApplication.toResponsablesJson())
                    put("p_autorizados", preApplication.toAutorizadosJson())
                    put("p_documentos", preApplication.toDocumentosJson())
                })
            }
        } catch (e: Exception) {
            return PreApplicationSubmitResult.Failed(PreApplicationPersistenceFailure.NETWORK)
        }

        if (response.status == HttpStatusCode.Conflict) {
            val detail = try { response.bodyAsText() } catch (e: Exception) { "" }
            if (!detail.contains(CURP_CONSTRAINT)) {
                return PreApplicationSubmitResult.DuplicateFolio(preApplication.folio)
            }
            // La familia no tiene via de lectura por CURP (RLS no la
            // autoriza fuera de su propio folio+token, que aqui todavia no
            // conoce): solo SECRETARIA puede resolver el registro existente.
            val normalizedCurp = preApplication.alumnoCurp.trim().uppercase()
            val existing = (auth as? PreApplicationAuthContext.Staff)?.let { findByCurp(it, normalizedCurp) }
            return existing?.let { PreApplicationSubmitResult.DuplicateCurp(normalizedCurp, it) }
                ?: PreApplicationSubmitResult.Failed(PreApplicationPersistenceFailure.REJECTED)
        }
        if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.Created) {
            return PreApplicationSubmitResult.Failed(response.status.toPreApplicationFailure())
        }

        val createdRef = try {
            response.body<CreatePreApplicationResponse>()
        } catch (e: Exception) {
            return PreApplicationSubmitResult.Failed(PreApplicationPersistenceFailure.REJECTED)
        }

        if (currentAuth()?.accessToken != auth.accessToken) {
            return PreApplicationSubmitResult.Failed(PreApplicationPersistenceFailure.NO_SESSION)
        }

        val created = when (auth) {
            is PreApplicationAuthContext.Staff -> fetchOne(auth, createdRef.folio)
            is PreApplicationAuthContext.Family -> fetchByToken(createdRef.folio, createdRef.accessToken)
        } ?: return PreApplicationSubmitResult.Failed(PreApplicationPersistenceFailure.REJECTED)
        _preApplications.value = (_preApplications.value.filterNot { it.folio == created.folio } + created)
        return PreApplicationSubmitResult.Submitted(created, createdRef.accessToken)
    }

    override suspend fun update(preApplication: PreApplication): PreApplicationUpdateResult {
        val auth = currentAuth() ?: return PreApplicationUpdateResult.Failed(PreApplicationPersistenceFailure.NO_SESSION)
        val familyToken = (auth as? PreApplicationAuthContext.Family)?.session?.accessToken

        val response = try {
            httpClient.post("$baseUrl/rest/v1/rpc/update_pre_application_with_children") {
                authHeaders(auth)
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("p_folio", preApplication.folio)
                    familyToken?.let { put("p_access_token", it) } ?: put("p_access_token", JsonPrimitive(null as String?))
                    put("p_record", preApplication.toRecordJson())
                    put("p_responsables", preApplication.toResponsablesJson())
                    put("p_autorizados", preApplication.toAutorizadosJson())
                    put("p_documentos", preApplication.toDocumentosJson())
                })
            }
        } catch (e: Exception) {
            return PreApplicationUpdateResult.Failed(PreApplicationPersistenceFailure.NETWORK)
        }

        if (response.status != HttpStatusCode.OK) {
            return PreApplicationUpdateResult.Failed(response.status.toPreApplicationFailure())
        }

        val folio = try {
            response.body<String>()
        } catch (e: Exception) {
            return PreApplicationUpdateResult.Failed(PreApplicationPersistenceFailure.REJECTED)
        }

        if (currentAuth()?.accessToken != auth.accessToken) {
            return PreApplicationUpdateResult.Failed(PreApplicationPersistenceFailure.NO_SESSION)
        }

        val updated = when (auth) {
            is PreApplicationAuthContext.Staff -> fetchOne(auth, folio)
            is PreApplicationAuthContext.Family -> fetchByToken(folio, auth.session.accessToken)
        } ?: return PreApplicationUpdateResult.Failed(PreApplicationPersistenceFailure.REJECTED)
        _preApplications.value = _preApplications.value.map { if (it.folio == updated.folio) updated else it }
        return PreApplicationUpdateResult.Updated(updated)
    }

    private suspend fun fetchOne(auth: PreApplicationAuthContext.Staff, folio: String): PreApplication? {
        val parent = try {
            val response = httpClient.get("$baseUrl/rest/v1/pre_applications") {
                authHeaders(auth)
                url {
                    parameters.append("select", PARENT_COLUMNS)
                    parameters.append("folio", "eq.$folio")
                    parameters.append("limit", "1")
                }
            }
            if (response.status != HttpStatusCode.OK) return null
            response.body<List<PreApplicationRow>>().firstOrNull()
        } catch (e: Exception) {
            null
        } ?: return null

        val responsables = fetchChildren<ResponsableRow>(auth, "pre_application_responsables", RESPONSABLE_COLUMNS, folio)
        val autorizados = fetchChildren<AutorizadoRow>(auth, "pre_application_autorizados", AUTORIZADO_COLUMNS, folio)
        val documentos = fetchChildren<DocumentoRow>(auth, "pre_application_documentos", DOCUMENTO_COLUMNS, folio)
        return parent.toDomain(responsables, autorizados, documentos)
    }

    private suspend inline fun <reified T> fetchChildren(
        auth: PreApplicationAuthContext.Staff,
        table: String,
        columns: String,
        folio: String
    ): List<T> = try {
        val response = httpClient.get("$baseUrl/rest/v1/$table") {
            authHeaders(auth)
            url {
                parameters.append("select", columns)
                parameters.append("pre_application_folio", "eq.$folio")
                parameters.append("order", "position.asc")
            }
        }
        if (response.status == HttpStatusCode.OK) response.body<List<T>>() else emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    private suspend fun findByCurp(auth: PreApplicationAuthContext.Staff, curp: String): PreApplication? = try {
        val response = httpClient.get("$baseUrl/rest/v1/pre_applications") {
            authHeaders(auth)
            url {
                parameters.append("select", PARENT_COLUMNS)
                parameters.append("alumno_curp", "eq.$curp")
                parameters.append("limit", "1")
            }
        }
        if (response.status == HttpStatusCode.OK) {
            val row = response.body<List<PreApplicationRow>>().firstOrNull() ?: return null
            fetchOne(auth, row.folio)
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }

    override fun clear() {
        _preApplications.value = emptyList()
    }

    /**
     * SECRETARIA manda su JWT real como Bearer -- RLS lo necesita para
     * `auth.uid()`/`has_permission`. La familia nunca tiene JWT: solo
     * `apikey` (rol `anon`); su identidad viaja como parametro explicito de
     * la RPC (`p_access_token`), no como header.
     */
    private fun HttpRequestBuilder.authHeaders(auth: PreApplicationAuthContext) {
        header("apikey", apiKey)
        if (auth is PreApplicationAuthContext.Staff) {
            header("Authorization", "Bearer ${auth.session.accessToken}")
        }
    }
}

internal fun HttpStatusCode.toPreApplicationFailure(): PreApplicationPersistenceFailure =
    if (value in 400..499) PreApplicationPersistenceFailure.REJECTED
    else PreApplicationPersistenceFailure.NETWORK
