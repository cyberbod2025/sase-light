-- SASE-310 — Cierre de PR #49: persistencia real de pre-solicitudes.
--
-- BLOQUEADOR (Codex, quinta revision sobre 7d83994, "Block mock
-- pre-applications in connected mode"; confirmado por Hugo como bloqueador
-- funcional, no un riesgo aceptable): PreApplicationViewModel seguia
-- inicializando _sharedPreApplications desde MockPreApplicationData sin
-- importar el ambiente. En SUPABASE_STAGING una pre-solicitud real se
-- perdia al reiniciar la app -- la primera mitad del recorrido
-- institucional (familia -> pre-solicitud -> Secretaria -> alta oficial)
-- nunca persistia de verdad.
--
-- DECISION DE IDENTIDAD (Hugo, sesion posterior, corrige la decision
-- original de AskUserQuestion en el cierre de PR #49): el portal familiar
-- NO debe exigir cuenta/login real de Supabase Auth -- la familia debe
-- poder enviar sin friccion, sin crear cuenta. En vez de cuenta real
-- (enum app_role 'familia', GoTrue signUp/signIn), la fila se protege con
-- un TOKEN DE ACCESO unico no adivinable (uuid), generado por el servidor
-- al crear la pre-solicitud y devuelto una sola vez al cliente junto con
-- el folio. La familia lo guarda igual que el folio (se muestra en
-- pantalla, sin envio de correo/SMS -- fuera de alcance) y lo vuelve a
-- capturar para consultar/editar despues. Sin JWT de usuario, PostgREST
-- expone el rol `anon` para el portal familiar: RLS por sesion (auth.uid())
-- no aplica a ese camino, asi que TODO el acceso familiar (lectura y
-- escritura) pasa exclusivamente por RPCs `security definer` que reciben
-- el token como parametro explicito y lo validan a mano contra la columna
-- almacenada. El camino de SECRETARIA (staff autenticado, via
-- has_permission) no cambia: sigue protegido por RLS normal.
--
-- DECISION DE ALCANCE DE TABLAS: el modelo PreApplication (Kotlin) tiene
-- bloques declarativos 1:1 (persona de tramite, ficha medica familiar,
-- contexto socio-familiar, antecedentes UDEII, consentimientos) y
-- colecciones 1:N (responsables, autorizados, documentos declarados).
-- A diferencia del expediente institucional POST-alta (0005/0006), donde
-- salud/UDEII/trabajo-social tienen su propio permiso granular porque
-- distintas areas del personal necesitan ver solo su seccion, aqui TODOS
-- los bloques 1:1 se revisan como una sola unidad por la misma persona
-- (SECRETARIA, via REVIEW_PRE_APPLICATION) -- partirlos en tablas propias
-- no añadiria aislamiento real, solo complejidad. Se aplanan como columnas
-- de `pre_applications`. Las colecciones 1:N si son tablas propias
-- (relacion real, no se puede aplanar sin perder informacion).
--
-- DECISION DE INSTITUCION EN EL ALTA DE FAMILIA: la familia no tiene
-- institutional_memberships (no es personal) ni cuenta de ningun tipo, asi
-- que no hay como derivar su institucion de una membresia real como se
-- hace para staff. El piloto opera una sola escuela real (Secundaria 310);
-- el trigger de insercion resuelve la institucion por nombre fijo en vez
-- de confiar en un institution_id que el cliente familiar pudiera enviar.
-- Documentado como simplificacion valida solo mientras el piloto sea de
-- una escuela: si SASE se expande a mas planteles, este trigger necesita
-- reemplazarse por un mecanismo real de seleccion de institucion en el
-- flujo familiar (fuera de alcance de este cierre).
--
-- ATOMICIDAD (agregado en revision previa a implementar el repositorio
-- Kotlin, mismo criterio que la migracion 0011 establecio para
-- students/student_sensitive_identity): la fila padre `pre_applications`
-- y sus 3 tablas hijas (`pre_application_responsables/autorizados/
-- documentos`) NO deben escribirse en llamadas HTTP separadas -- si la
-- segunda escritura fallara tras exito de la primera, quedaria una
-- pre-solicitud con responsables/documentos a medio guardar sin que la UI
-- lo distinguiera de un guardado completo. Al final de este archivo se
-- agregan `create_pre_application_with_children`/
-- `update_pre_application_with_children`/`get_pre_application_with_children`,
-- RPCs `security definer` (no `invoker`: la familia nunca tiene JWT/rol
-- `authenticated`, asi que RLS por sesion no puede autorizarla -- estas
-- funciones hacen su propia validacion interna del token y son la UNICA
-- via de acceso familiar, de lectura y escritura). El trigger
-- `trg_pre_applications_stamp_owner` sigue aplicando dentro del INSERT que
-- hace la RPC de creacion -- institution_id se deriva igual, nunca via
-- parametro de la RPC; access_token lo genera el default de la columna.
--
-- ESTADO: escrita, NO aplicada. Requiere autorizacion explicita de Hugo
-- antes de tocar el proyecto remoto "SASE-Light" (plyjvvpkaafnkxmmqkbh).

-- === Nucleo de la pre-solicitud ===============================================

create table public.pre_applications (
  folio                          text primary key,
  institution_id                 uuid not null references public.institutions (id),
  access_token                   uuid not null default gen_random_uuid(),
  status                         text not null default 'BORRADOR',
  submitted_at                   text,

  tramite                        text not null default '',
  ciclo_escolar                  text not null default '',
  grado_solicitado               integer not null default 0,

  alumno_nombre_completo         text not null,
  alumno_curp                    text not null,
  alumno_fecha_nacimiento        text not null default '',
  alumno_sexo                    text not null default '',
  alumno_nacionalidad            text not null default '',
  alumno_entidad_nacimiento      text not null default '',
  alumno_domicilio               text not null default '',
  alumno_telefono_casa           text not null default '',
  escuela_procedencia            text not null default '',
  promedio_grado_anterior        double precision,

  persona_tramite_nombre         text not null default '',
  persona_tramite_parentesco     text not null default '',
  persona_tramite_telefono       text not null default '',
  persona_tramite_identificacion text not null default '',
  persona_tramite_contacto_principal boolean not null default false,

  ficha_servicio_medico          text not null default '',
  ficha_numero_afiliacion        text,
  ficha_tipo_sangre              text,
  ficha_alergias                 text not null default '',
  ficha_padecimientos            text not null default '',
  ficha_medicamentos             text not null default '',
  ficha_restriccion_fisica       text not null default '',
  ficha_usa_lentes               boolean not null default false,
  ficha_dificultad_visual_auditiva text not null default '',
  ficha_salud_bucal              text not null default '',
  ficha_cartilla_vacunacion      boolean not null default false,

  contexto_vive_con_quien        text not null default '',
  contexto_tipo_familia          text not null default '',
  contexto_hijo_unico            boolean not null default false,
  contexto_lugar_entre_hermanos  integer not null default 0,
  contexto_hermanos_en_escuela   boolean not null default false,
  contexto_integrantes_hogar     integer not null default 0,
  contexto_sosten_economico      text not null default '',
  contexto_ingreso_rangos        text not null default '',
  contexto_tipo_vivienda         text not null default '',
  contexto_servicios_basicos     boolean not null default false,
  contexto_internet              boolean not null default false,
  contexto_dispositivo_tareas    text not null default '',
  contexto_beca_apoyo            text not null default '',
  contexto_transporte            text not null default '',
  contexto_dificultad_materiales boolean not null default false,
  contexto_atiende_avisos        text not null default '',
  contexto_horario_comunicacion  text not null default '',
  contexto_puede_acudir_citatorios boolean not null default false,

  udeii_antecedente_apoyo        text not null default '',
  udeii_terapia_lenguaje         boolean not null default false,
  udeii_apoyo_psicologico        boolean not null default false,
  udeii_apoyo_pedagogico         boolean not null default false,
  udeii_documentos_disponibles   text not null default '',
  udeii_informe_escuela_anterior boolean not null default false,
  udeii_evaluacion_psicopedagogica boolean not null default false,
  udeii_plan_intervencion        boolean not null default false,
  udeii_portafolio               boolean not null default false,
  udeii_observaciones_familiares text not null default '',

  consentimiento_aviso_privacidad        boolean not null default false,
  consentimiento_uso_datos_expediente    boolean not null default false,
  consentimiento_foto_alumno             boolean not null default false,
  consentimiento_foto_credencial         boolean not null default false,
  consentimiento_foto_autorizados        boolean not null default false,
  consentimiento_comunicacion_whatsapp   boolean not null default false,
  consentimiento_reglamento_interno      boolean not null default false,
  consentimiento_marco_convivencia       boolean not null default false,
  consentimiento_corresponsabilidad_familiar boolean not null default false,

  observaciones_secretaria       text not null default '',
  motivo_correccion              text not null default '',
  readiness_status               text not null default 'PENDING',
  ready_at                       text,
  readiness_notes                text not null default '',

  created_at                     timestamptz not null default now(),
  updated_at                     timestamptz not null default now(),

  constraint pre_applications_status_check
    check (status in ('BORRADOR','ENVIADA','PENDIENTE_CORRECCION','DUPLICADA','ACEPTADA','CANCELADA')),
  constraint pre_applications_readiness_status_check
    check (readiness_status in ('PENDING','BLOCKED','READY','CONVERTED')),
  constraint pre_applications_alumno_nombre_not_blank check (btrim(alumno_nombre_completo) <> ''),
  constraint pre_applications_alumno_curp_not_blank check (btrim(alumno_curp) <> ''),
  constraint pre_applications_access_token_key unique (access_token)
);

create index pre_applications_institution_idx on public.pre_applications (institution_id);
create unique index pre_applications_institution_curp_key
  on public.pre_applications (institution_id, alumno_curp)
  where status <> 'CANCELADA';

create trigger trg_pre_applications_updated_at
  before update on public.pre_applications
  for each row execute function public.sase_set_updated_at();

-- institution_id NUNCA lo fija el cliente: se resuelve server-side por
-- nombre fijo mientras el piloto sea de una sola escuela (ver decision
-- arriba). access_token no requiere trigger: el default de la columna lo
-- genera, y la RPC de creacion no incluye esa columna en su lista de
-- insercion, asi que el cliente no puede sobreescribirlo aunque lo mande.
create or replace function public.sase_stamp_pre_application_institution()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_institution_id uuid;
begin
  select id into v_institution_id from public.institutions where name = 'Secundaria 310';
  if v_institution_id is null then
    raise exception 'SASE_PRE_APPLICATION_INSTITUTION_NOT_CONFIGURED';
  end if;

  new.institution_id := v_institution_id;
  return new;
end;
$$;

create trigger trg_pre_applications_stamp_owner
  before insert on public.pre_applications
  for each row execute function public.sase_stamp_pre_application_institution();

alter table public.pre_applications enable row level security;

-- La familia NUNCA accede por REST directo (no tiene JWT/rol authenticated
-- -- ver decision de identidad arriba): su unico acceso es a traves de las
-- RPCs `security definer` al final de este archivo, que validan el token a
-- mano. Estas politicas de RLS solo cubren al personal (SECRETARIA via
-- REVIEW_PRE_APPLICATION). DIRECCION no recibe ninguna politica: el
-- catalogo real no le da hoy ningun permiso de pre-solicitud (ver
-- comentario de StaffPermissions.actionMatrix en el cliente) -- coherente
-- con que el cliente tampoco le ofrece el area PRE_SOLICITUD. No existe
-- politica de INSERT: la unica via de creacion es la RPC (security
-- definer, bypassa RLS).
create policy pre_applications_select_reviewer on public.pre_applications
  for select using (
     public.has_permission(institution_id, 'PRE_SOLICITUD')
  );

create policy pre_applications_update_reviewer on public.pre_applications
  for update using (
     public.has_permission(institution_id, 'PRE_SOLICITUD')
  )
  with check (
     public.has_permission(institution_id, 'PRE_SOLICITUD')
  );

-- El token es un bearer credential de familia, no un dato de Secretaría.
-- PostgREST debe rechazar una selección explícita de esa columna aunque la
-- fila sea visible por RLS; las RPC security definer siguen pudiendo validarlo.
revoke select (access_token) on table public.pre_applications from anon, authenticated;

-- Todas las mutaciones pasan por las RPC para conservar la propiedad de
-- campos y la atomicidad padre/hijas. Las políticas de escritura directas
-- ampliarían la superficie y permitirían saltarse esos guardrails.
drop policy pre_applications_update_reviewer on public.pre_applications;

-- === Responsables (1:N) ======================================================

create table public.pre_application_responsables (
  id                       uuid primary key default gen_random_uuid(),
  pre_application_folio    text not null references public.pre_applications (folio) on delete cascade,
  institution_id           uuid not null references public.institutions (id),
  position                 integer not null default 0,
  nombre_completo          text not null,
  parentesco               text not null default '',
  telefono                 text not null default '',
  correo                   text,
  domicilio_distinto       boolean not null default false,
  domicilio                text,
  vive_con_alumno          boolean not null default false,
  contacto_principal       boolean not null default false,
  puede_recoger            boolean not null default false,
  ocupacion                text not null default '',
  horario_contacto         text not null default '',
  identificacion_a_presentar text not null default ''
);

create index pre_application_responsables_folio_idx
  on public.pre_application_responsables (pre_application_folio, position);

alter table public.pre_application_responsables enable row level security;

create policy pre_application_responsables_select on public.pre_application_responsables
  for select using (
    exists (
      select 1 from public.pre_applications pa
      where pa.folio = pre_application_folio
         and public.has_permission(pa.institution_id, 'PRE_SOLICITUD')
    )
  );

create policy pre_application_responsables_write on public.pre_application_responsables
  for all using (
    exists (
      select 1 from public.pre_applications pa
      where pa.folio = pre_application_folio
         and public.has_permission(pa.institution_id, 'PRE_SOLICITUD')
    )
  )
  with check (
    exists (
      select 1 from public.pre_applications pa
      where pa.folio = pre_application_folio
         and public.has_permission(pa.institution_id, 'PRE_SOLICITUD')
        and pa.institution_id = institution_id
    )
  );

drop policy pre_application_responsables_write on public.pre_application_responsables;

-- === Autorizados para recoger (1:N) =========================================

create table public.pre_application_autorizados (
  id                       uuid primary key default gen_random_uuid(),
  pre_application_folio    text not null references public.pre_applications (folio) on delete cascade,
  institution_id           uuid not null references public.institutions (id),
  position                 integer not null default 0,
  nombre_completo          text not null,
  parentesco               text not null default '',
  telefono                 text not null default '',
  observaciones            text not null default ''
);

create index pre_application_autorizados_folio_idx
  on public.pre_application_autorizados (pre_application_folio, position);

alter table public.pre_application_autorizados enable row level security;

create policy pre_application_autorizados_select on public.pre_application_autorizados
  for select using (
    exists (
      select 1 from public.pre_applications pa
      where pa.folio = pre_application_folio
         and public.has_permission(pa.institution_id, 'PRE_SOLICITUD')
    )
  );

create policy pre_application_autorizados_write on public.pre_application_autorizados
  for all using (
    exists (
      select 1 from public.pre_applications pa
      where pa.folio = pre_application_folio
         and public.has_permission(pa.institution_id, 'PRE_SOLICITUD')
    )
  )
  with check (
    exists (
      select 1 from public.pre_applications pa
      where pa.folio = pre_application_folio
         and public.has_permission(pa.institution_id, 'PRE_SOLICITUD')
        and pa.institution_id = institution_id
    )
  );

drop policy pre_application_autorizados_write on public.pre_application_autorizados;

-- === Documentos declarados (1:N) ============================================

create table public.pre_application_documentos (
  id                       uuid primary key default gen_random_uuid(),
  pre_application_folio    text not null references public.pre_applications (folio) on delete cascade,
  institution_id           uuid not null references public.institutions (id),
  position                 integer not null default 0,
  nombre                   text not null,
  declarado                boolean not null default false,
  cotejado_secretaria      boolean not null default false,
  validado                 boolean not null default false,
  rechazado                boolean not null default false,
  no_aplica                boolean not null default false,
  observacion              text not null default ''
);

create index pre_application_documentos_folio_idx
  on public.pre_application_documentos (pre_application_folio, position);

alter table public.pre_application_documentos enable row level security;

create policy pre_application_documentos_select on public.pre_application_documentos
  for select using (
    exists (
      select 1 from public.pre_applications pa
      where pa.folio = pre_application_folio
         and public.has_permission(pa.institution_id, 'PRE_SOLICITUD')
    )
  );

create policy pre_application_documentos_write on public.pre_application_documentos
  for all using (
    exists (
      select 1 from public.pre_applications pa
      where pa.folio = pre_application_folio
         and public.has_permission(pa.institution_id, 'PRE_SOLICITUD')
    )
  )
  with check (
    exists (
      select 1 from public.pre_applications pa
      where pa.folio = pre_application_folio
         and public.has_permission(pa.institution_id, 'PRE_SOLICITUD')
        and pa.institution_id = institution_id
    )
  );

drop policy pre_application_documentos_write on public.pre_application_documentos;

-- === RPCs (fila padre + 3 tablas hijas en una transaccion, acceso familiar) ==
--
-- Ambas reciben la fila plana como jsonb (74 columnas via parametros
-- nombrados habria sido tan largo como este bloque sin ganar nada) y las
-- colecciones 1:N como jsonb arrays. `security definer`: la familia nunca
-- tiene JWT de Supabase Auth (ver decision de identidad arriba), asi que
-- RLS por sesion no puede autorizarla -- estas funciones son la unica via
-- de acceso familiar y hacen su propia validacion. folio/institution_id/
-- access_token que el cliente pudiera incluir en p_record se ignoran: el
-- trigger `trg_pre_applications_stamp_owner` fija institution_id siempre
-- dentro del mismo INSERT, y access_token nunca esta en la lista de
-- columnas que la RPC inserta (lo genera el default de la columna).

create or replace function public.create_pre_application_with_children(
  p_record jsonb,
  p_responsables jsonb,
  p_autorizados jsonb,
  p_documentos jsonb
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_folio text;
  v_institution_id uuid;
  v_access_token uuid;
begin
  insert into public.pre_applications as pa (
    folio, status, submitted_at, tramite, ciclo_escolar, grado_solicitado,
    alumno_nombre_completo, alumno_curp, alumno_fecha_nacimiento, alumno_sexo,
    alumno_nacionalidad, alumno_entidad_nacimiento, alumno_domicilio, alumno_telefono_casa,
    escuela_procedencia, promedio_grado_anterior,
    persona_tramite_nombre, persona_tramite_parentesco, persona_tramite_telefono,
    persona_tramite_identificacion, persona_tramite_contacto_principal,
    ficha_servicio_medico, ficha_numero_afiliacion, ficha_tipo_sangre, ficha_alergias,
    ficha_padecimientos, ficha_medicamentos, ficha_restriccion_fisica, ficha_usa_lentes,
    ficha_dificultad_visual_auditiva, ficha_salud_bucal, ficha_cartilla_vacunacion,
    contexto_vive_con_quien, contexto_tipo_familia, contexto_hijo_unico,
    contexto_lugar_entre_hermanos, contexto_hermanos_en_escuela, contexto_integrantes_hogar,
    contexto_sosten_economico, contexto_ingreso_rangos, contexto_tipo_vivienda,
    contexto_servicios_basicos, contexto_internet, contexto_dispositivo_tareas,
    contexto_beca_apoyo, contexto_transporte, contexto_dificultad_materiales,
    contexto_atiende_avisos, contexto_horario_comunicacion, contexto_puede_acudir_citatorios,
    udeii_antecedente_apoyo, udeii_terapia_lenguaje, udeii_apoyo_psicologico,
    udeii_apoyo_pedagogico, udeii_documentos_disponibles, udeii_informe_escuela_anterior,
    udeii_evaluacion_psicopedagogica, udeii_plan_intervencion, udeii_portafolio,
    udeii_observaciones_familiares,
    consentimiento_aviso_privacidad, consentimiento_uso_datos_expediente,
    consentimiento_foto_alumno, consentimiento_foto_credencial, consentimiento_foto_autorizados,
    consentimiento_comunicacion_whatsapp, consentimiento_reglamento_interno,
    consentimiento_marco_convivencia, consentimiento_corresponsabilidad_familiar,
    observaciones_secretaria, motivo_correccion, readiness_status, ready_at, readiness_notes
  )
  select
    x.folio, 'ENVIADA', now()::text, x.tramite, x.ciclo_escolar, x.grado_solicitado,
    x.alumno_nombre_completo, x.alumno_curp, x.alumno_fecha_nacimiento, x.alumno_sexo,
    x.alumno_nacionalidad, x.alumno_entidad_nacimiento, x.alumno_domicilio, x.alumno_telefono_casa,
    x.escuela_procedencia, x.promedio_grado_anterior,
    x.persona_tramite_nombre, x.persona_tramite_parentesco, x.persona_tramite_telefono,
    x.persona_tramite_identificacion, x.persona_tramite_contacto_principal,
    x.ficha_servicio_medico, x.ficha_numero_afiliacion, x.ficha_tipo_sangre, x.ficha_alergias,
    x.ficha_padecimientos, x.ficha_medicamentos, x.ficha_restriccion_fisica, x.ficha_usa_lentes,
    x.ficha_dificultad_visual_auditiva, x.ficha_salud_bucal, x.ficha_cartilla_vacunacion,
    x.contexto_vive_con_quien, x.contexto_tipo_familia, x.contexto_hijo_unico,
    x.contexto_lugar_entre_hermanos, x.contexto_hermanos_en_escuela, x.contexto_integrantes_hogar,
    x.contexto_sosten_economico, x.contexto_ingreso_rangos, x.contexto_tipo_vivienda,
    x.contexto_servicios_basicos, x.contexto_internet, x.contexto_dispositivo_tareas,
    x.contexto_beca_apoyo, x.contexto_transporte, x.contexto_dificultad_materiales,
    x.contexto_atiende_avisos, x.contexto_horario_comunicacion, x.contexto_puede_acudir_citatorios,
    x.udeii_antecedente_apoyo, x.udeii_terapia_lenguaje, x.udeii_apoyo_psicologico,
    x.udeii_apoyo_pedagogico, x.udeii_documentos_disponibles, x.udeii_informe_escuela_anterior,
    x.udeii_evaluacion_psicopedagogica, x.udeii_plan_intervencion, x.udeii_portafolio,
    x.udeii_observaciones_familiares,
    x.consentimiento_aviso_privacidad, x.consentimiento_uso_datos_expediente,
    x.consentimiento_foto_alumno, x.consentimiento_foto_credencial, x.consentimiento_foto_autorizados,
    x.consentimiento_comunicacion_whatsapp, x.consentimiento_reglamento_interno,
    x.consentimiento_marco_convivencia, x.consentimiento_corresponsabilidad_familiar,
    '', '', 'PENDING', null, ''
  from jsonb_to_record(p_record) as x(
    folio text, status text, submitted_at text, tramite text, ciclo_escolar text, grado_solicitado int,
    alumno_nombre_completo text, alumno_curp text, alumno_fecha_nacimiento text, alumno_sexo text,
    alumno_nacionalidad text, alumno_entidad_nacimiento text, alumno_domicilio text, alumno_telefono_casa text,
    escuela_procedencia text, promedio_grado_anterior double precision,
    persona_tramite_nombre text, persona_tramite_parentesco text, persona_tramite_telefono text,
    persona_tramite_identificacion text, persona_tramite_contacto_principal boolean,
    ficha_servicio_medico text, ficha_numero_afiliacion text, ficha_tipo_sangre text, ficha_alergias text,
    ficha_padecimientos text, ficha_medicamentos text, ficha_restriccion_fisica text, ficha_usa_lentes boolean,
    ficha_dificultad_visual_auditiva text, ficha_salud_bucal text, ficha_cartilla_vacunacion boolean,
    contexto_vive_con_quien text, contexto_tipo_familia text, contexto_hijo_unico boolean,
    contexto_lugar_entre_hermanos int, contexto_hermanos_en_escuela boolean, contexto_integrantes_hogar int,
    contexto_sosten_economico text, contexto_ingreso_rangos text, contexto_tipo_vivienda text,
    contexto_servicios_basicos boolean, contexto_internet boolean, contexto_dispositivo_tareas text,
    contexto_beca_apoyo text, contexto_transporte text, contexto_dificultad_materiales boolean,
    contexto_atiende_avisos text, contexto_horario_comunicacion text, contexto_puede_acudir_citatorios boolean,
    udeii_antecedente_apoyo text, udeii_terapia_lenguaje boolean, udeii_apoyo_psicologico boolean,
    udeii_apoyo_pedagogico boolean, udeii_documentos_disponibles text, udeii_informe_escuela_anterior boolean,
    udeii_evaluacion_psicopedagogica boolean, udeii_plan_intervencion boolean, udeii_portafolio boolean,
    udeii_observaciones_familiares text,
    consentimiento_aviso_privacidad boolean, consentimiento_uso_datos_expediente boolean,
    consentimiento_foto_alumno boolean, consentimiento_foto_credencial boolean, consentimiento_foto_autorizados boolean,
    consentimiento_comunicacion_whatsapp boolean, consentimiento_reglamento_interno boolean,
    consentimiento_marco_convivencia boolean, consentimiento_corresponsabilidad_familiar boolean,
    observaciones_secretaria text, motivo_correccion text, readiness_status text, ready_at text, readiness_notes text
  )
  returning pa.folio, pa.institution_id, pa.access_token into v_folio, v_institution_id, v_access_token;

  insert into public.pre_application_responsables (
    pre_application_folio, institution_id, position, nombre_completo, parentesco, telefono, correo,
    domicilio_distinto, domicilio, vive_con_alumno, contacto_principal, puede_recoger, ocupacion,
    horario_contacto, identificacion_a_presentar
  )
  select v_folio, v_institution_id,
     r.position, r.nombre_completo, r.parentesco, r.telefono, r.correo, r.domicilio_distinto, r.domicilio,
    r.vive_con_alumno, r.contacto_principal, r.puede_recoger, r.ocupacion, r.horario_contacto, r.identificacion_a_presentar
  from jsonb_to_recordset(p_responsables) as r(
    position int, nombre_completo text, parentesco text, telefono text, correo text,
    domicilio_distinto boolean, domicilio text, vive_con_alumno boolean, contacto_principal boolean,
    puede_recoger boolean, ocupacion text, horario_contacto text, identificacion_a_presentar text
  );

  insert into public.pre_application_autorizados (
    pre_application_folio, institution_id, position, nombre_completo, parentesco, telefono, observaciones
  )
  select v_folio, v_institution_id,
    a.position, a.nombre_completo, a.parentesco, a.telefono, a.observaciones
  from jsonb_to_recordset(p_autorizados) as a(
    position int, nombre_completo text, parentesco text, telefono text, observaciones text
  );

  insert into public.pre_application_documentos (
    pre_application_folio, institution_id, position, nombre, declarado, cotejado_secretaria, validado, rechazado, no_aplica, observacion
  )
  select v_folio, v_institution_id,
     d.position, d.nombre, d.declarado, false, false, false, false, ''
  from jsonb_to_recordset(p_documentos) as d(
    position int, nombre text, declarado boolean, cotejado_secretaria boolean, validado boolean, rechazado boolean, no_aplica boolean, observacion text
  );

  return jsonb_build_object('folio', v_folio, 'access_token', v_access_token);
end;
$$;

grant execute on function public.create_pre_application_with_children(jsonb, jsonb, jsonb, jsonb) to anon;

-- update_pre_application_with_children: sirve a los dos actores posibles.
-- p_access_token no nulo => camino familiar: se valida a mano contra el
-- token almacenado (no hay RLS por sesion, la funcion es security
-- definer). p_access_token nulo => camino de personal: se exige
-- auth.uid() real y has_permission(institution_id, 'REVIEW_PRE_APPLICATION')
-- explicito (RLS no se aplica automaticamente porque la funcion es
-- definer, asi que este chequeo reemplaza lo que antes hacia la politica).
-- En ambos casos, folio inexistente / token incorrecto / sin permiso
-- devuelven exactamente el mismo error -- nunca se distingue la causa para
-- no filtrar informacion. Reemplazo completo (delete + insert, misma
-- transaccion) de las 3 colecciones hijas, igual que antes.
create or replace function public.update_pre_application_with_children(
  p_folio text,
  p_access_token uuid,
  p_record jsonb,
  p_responsables jsonb,
  p_autorizados jsonb,
  p_documentos jsonb
)
returns text
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_institution_id uuid;
  v_stored_token uuid;
  v_actor uuid;
begin
  select institution_id, access_token into v_institution_id, v_stored_token
  from public.pre_applications where folio = p_folio;

  if v_institution_id is null then
    raise exception 'SASE_PRE_APPLICATION_UPDATE_REJECTED';
  end if;

  if p_access_token is not null then
    if v_stored_token is distinct from p_access_token then
      raise exception 'SASE_PRE_APPLICATION_UPDATE_REJECTED';
    end if;
  else
    v_actor := (select auth.uid());
     if v_actor is null or not public.has_permission(v_institution_id, 'PRE_SOLICITUD') then
      raise exception 'SASE_PRE_APPLICATION_UPDATE_REJECTED';
    end if;
  end if;

  update public.pre_applications as pa set
     status = case when p_access_token is not null then 'ENVIADA' else x.status end,
     submitted_at = case when p_access_token is not null then pa.submitted_at else x.submitted_at end,
    tramite = x.tramite,
    ciclo_escolar = x.ciclo_escolar,
    grado_solicitado = x.grado_solicitado,
    alumno_nombre_completo = x.alumno_nombre_completo,
    alumno_curp = x.alumno_curp,
    alumno_fecha_nacimiento = x.alumno_fecha_nacimiento,
    alumno_sexo = x.alumno_sexo,
    alumno_nacionalidad = x.alumno_nacionalidad,
    alumno_entidad_nacimiento = x.alumno_entidad_nacimiento,
    alumno_domicilio = x.alumno_domicilio,
    alumno_telefono_casa = x.alumno_telefono_casa,
    escuela_procedencia = x.escuela_procedencia,
    promedio_grado_anterior = x.promedio_grado_anterior,
    persona_tramite_nombre = x.persona_tramite_nombre,
    persona_tramite_parentesco = x.persona_tramite_parentesco,
    persona_tramite_telefono = x.persona_tramite_telefono,
    persona_tramite_identificacion = x.persona_tramite_identificacion,
    persona_tramite_contacto_principal = x.persona_tramite_contacto_principal,
    ficha_servicio_medico = x.ficha_servicio_medico,
    ficha_numero_afiliacion = x.ficha_numero_afiliacion,
    ficha_tipo_sangre = x.ficha_tipo_sangre,
    ficha_alergias = x.ficha_alergias,
    ficha_padecimientos = x.ficha_padecimientos,
    ficha_medicamentos = x.ficha_medicamentos,
    ficha_restriccion_fisica = x.ficha_restriccion_fisica,
    ficha_usa_lentes = x.ficha_usa_lentes,
    ficha_dificultad_visual_auditiva = x.ficha_dificultad_visual_auditiva,
    ficha_salud_bucal = x.ficha_salud_bucal,
    ficha_cartilla_vacunacion = x.ficha_cartilla_vacunacion,
    contexto_vive_con_quien = x.contexto_vive_con_quien,
    contexto_tipo_familia = x.contexto_tipo_familia,
    contexto_hijo_unico = x.contexto_hijo_unico,
    contexto_lugar_entre_hermanos = x.contexto_lugar_entre_hermanos,
    contexto_hermanos_en_escuela = x.contexto_hermanos_en_escuela,
    contexto_integrantes_hogar = x.contexto_integrantes_hogar,
    contexto_sosten_economico = x.contexto_sosten_economico,
    contexto_ingreso_rangos = x.contexto_ingreso_rangos,
    contexto_tipo_vivienda = x.contexto_tipo_vivienda,
    contexto_servicios_basicos = x.contexto_servicios_basicos,
    contexto_internet = x.contexto_internet,
    contexto_dispositivo_tareas = x.contexto_dispositivo_tareas,
    contexto_beca_apoyo = x.contexto_beca_apoyo,
    contexto_transporte = x.contexto_transporte,
    contexto_dificultad_materiales = x.contexto_dificultad_materiales,
    contexto_atiende_avisos = x.contexto_atiende_avisos,
    contexto_horario_comunicacion = x.contexto_horario_comunicacion,
    contexto_puede_acudir_citatorios = x.contexto_puede_acudir_citatorios,
    udeii_antecedente_apoyo = x.udeii_antecedente_apoyo,
    udeii_terapia_lenguaje = x.udeii_terapia_lenguaje,
    udeii_apoyo_psicologico = x.udeii_apoyo_psicologico,
    udeii_apoyo_pedagogico = x.udeii_apoyo_pedagogico,
    udeii_documentos_disponibles = x.udeii_documentos_disponibles,
    udeii_informe_escuela_anterior = x.udeii_informe_escuela_anterior,
    udeii_evaluacion_psicopedagogica = x.udeii_evaluacion_psicopedagogica,
    udeii_plan_intervencion = x.udeii_plan_intervencion,
    udeii_portafolio = x.udeii_portafolio,
    udeii_observaciones_familiares = x.udeii_observaciones_familiares,
    consentimiento_aviso_privacidad = x.consentimiento_aviso_privacidad,
    consentimiento_uso_datos_expediente = x.consentimiento_uso_datos_expediente,
    consentimiento_foto_alumno = x.consentimiento_foto_alumno,
    consentimiento_foto_credencial = x.consentimiento_foto_credencial,
    consentimiento_foto_autorizados = x.consentimiento_foto_autorizados,
    consentimiento_comunicacion_whatsapp = x.consentimiento_comunicacion_whatsapp,
    consentimiento_reglamento_interno = x.consentimiento_reglamento_interno,
    consentimiento_marco_convivencia = x.consentimiento_marco_convivencia,
    consentimiento_corresponsabilidad_familiar = x.consentimiento_corresponsabilidad_familiar,
     observaciones_secretaria = case when p_access_token is not null then pa.observaciones_secretaria else x.observaciones_secretaria end,
     motivo_correccion = case when p_access_token is not null then pa.motivo_correccion else x.motivo_correccion end,
     readiness_status = case when p_access_token is not null then 'PENDING' else x.readiness_status end,
     ready_at = case when p_access_token is not null then null else x.ready_at end,
     readiness_notes = case when p_access_token is not null then '' else x.readiness_notes end
  from jsonb_to_record(p_record) as x(
    status text, submitted_at text, tramite text, ciclo_escolar text, grado_solicitado int,
    alumno_nombre_completo text, alumno_curp text, alumno_fecha_nacimiento text, alumno_sexo text,
    alumno_nacionalidad text, alumno_entidad_nacimiento text, alumno_domicilio text, alumno_telefono_casa text,
    escuela_procedencia text, promedio_grado_anterior double precision,
    persona_tramite_nombre text, persona_tramite_parentesco text, persona_tramite_telefono text,
    persona_tramite_identificacion text, persona_tramite_contacto_principal boolean,
    ficha_servicio_medico text, ficha_numero_afiliacion text, ficha_tipo_sangre text, ficha_alergias text,
    ficha_padecimientos text, ficha_medicamentos text, ficha_restriccion_fisica text, ficha_usa_lentes boolean,
    ficha_dificultad_visual_auditiva text, ficha_salud_bucal text, ficha_cartilla_vacunacion boolean,
    contexto_vive_con_quien text, contexto_tipo_familia text, contexto_hijo_unico boolean,
    contexto_lugar_entre_hermanos int, contexto_hermanos_en_escuela boolean, contexto_integrantes_hogar int,
    contexto_sosten_economico text, contexto_ingreso_rangos text, contexto_tipo_vivienda text,
    contexto_servicios_basicos boolean, contexto_internet boolean, contexto_dispositivo_tareas text,
    contexto_beca_apoyo text, contexto_transporte text, contexto_dificultad_materiales boolean,
    contexto_atiende_avisos text, contexto_horario_comunicacion text, contexto_puede_acudir_citatorios boolean,
    udeii_antecedente_apoyo text, udeii_terapia_lenguaje boolean, udeii_apoyo_psicologico boolean,
    udeii_apoyo_pedagogico boolean, udeii_documentos_disponibles text, udeii_informe_escuela_anterior boolean,
    udeii_evaluacion_psicopedagogica boolean, udeii_plan_intervencion boolean, udeii_portafolio boolean,
    udeii_observaciones_familiares text,
    consentimiento_aviso_privacidad boolean, consentimiento_uso_datos_expediente boolean,
    consentimiento_foto_alumno boolean, consentimiento_foto_credencial boolean, consentimiento_foto_autorizados boolean,
    consentimiento_comunicacion_whatsapp boolean, consentimiento_reglamento_interno boolean,
    consentimiento_marco_convivencia boolean, consentimiento_corresponsabilidad_familiar boolean,
    observaciones_secretaria text, motivo_correccion text, readiness_status text, ready_at text, readiness_notes text
  )
  where pa.folio = p_folio
  returning pa.institution_id into v_institution_id;

  if v_institution_id is null then
    raise exception 'SASE_PRE_APPLICATION_UPDATE_REJECTED';
  end if;

   delete from public.pre_application_responsables where pre_application_folio = p_folio;
   delete from public.pre_application_autorizados where pre_application_folio = p_folio;
   if p_access_token is null then
     delete from public.pre_application_documentos where pre_application_folio = p_folio;
   end if;

  insert into public.pre_application_responsables (
    pre_application_folio, institution_id, position, nombre_completo, parentesco, telefono, correo,
    domicilio_distinto, domicilio, vive_con_alumno, contacto_principal, puede_recoger, ocupacion,
    horario_contacto, identificacion_a_presentar
  )
  select p_folio, v_institution_id,
    r.position, r.nombre_completo, r.parentesco, r.telefono, r.correo, r.domicilio_distinto, r.domicilio,
    r.vive_con_alumno, r.contacto_principal, r.puede_recoger, r.ocupacion, r.horario_contacto, r.identificacion_a_presentar
  from jsonb_to_recordset(p_responsables) as r(
    position int, nombre_completo text, parentesco text, telefono text, correo text,
    domicilio_distinto boolean, domicilio text, vive_con_alumno boolean, contacto_principal boolean,
    puede_recoger boolean, ocupacion text, horario_contacto text, identificacion_a_presentar text
  );

  insert into public.pre_application_autorizados (
    pre_application_folio, institution_id, position, nombre_completo, parentesco, telefono, observaciones
  )
  select p_folio, v_institution_id,
    a.position, a.nombre_completo, a.parentesco, a.telefono, a.observaciones
  from jsonb_to_recordset(p_autorizados) as a(
    position int, nombre_completo text, parentesco text, telefono text, observaciones text
  );

   if p_access_token is null then
     insert into public.pre_application_documentos (
    pre_application_folio, institution_id, position, nombre, declarado, cotejado_secretaria, validado, rechazado, no_aplica, observacion
  )
     select p_folio, v_institution_id,
     d.position, d.nombre, d.declarado,
     d.cotejado_secretaria, d.validado, d.rechazado, d.no_aplica, d.observacion
     from jsonb_to_recordset(p_documentos) as d(
    position int, nombre text, declarado boolean, cotejado_secretaria boolean, validado boolean, rechazado boolean, no_aplica boolean, observacion text
   )
     ;
   else
     update public.pre_application_documentos as existing
     set declarado = d.declarado
     from jsonb_to_recordset(p_documentos) as d(
       position int, nombre text, declarado boolean, cotejado_secretaria boolean,
       validado boolean, rechazado boolean, no_aplica boolean, observacion text
     )
     where existing.pre_application_folio = p_folio
       and existing.nombre = d.nombre;
   end if;

  return p_folio;
end;
$$;

grant execute on function public.update_pre_application_with_children(text, uuid, jsonb, jsonb, jsonb, jsonb) to anon, authenticated;

-- get_pre_application_with_children: unica via de LECTURA familiar (sin
-- esto, la familia no podria ni consultar el estado de su tramite -- ver
-- decision de identidad arriba, RLS no aplica sin JWT). SECRETARIA sigue
-- leyendo por REST directo con RLS normal (pre_applications_select_reviewer),
-- esta RPC no la usa. access_token nunca se re-expone en la respuesta: la
-- familia ya lo tiene, no hace falta que el servidor lo repita.
create or replace function public.get_pre_application_with_children(
  p_folio text,
  p_access_token uuid
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_row public.pre_applications%rowtype;
  v_responsables jsonb;
  v_autorizados jsonb;
  v_documentos jsonb;
begin
  select * into v_row from public.pre_applications where folio = p_folio;

  if v_row.folio is null or v_row.access_token is distinct from p_access_token then
    return null;
  end if;

  select coalesce(jsonb_agg(to_jsonb(r) - 'id' - 'pre_application_folio' - 'institution_id' order by r.position), '[]'::jsonb)
    into v_responsables from public.pre_application_responsables r where r.pre_application_folio = p_folio;

  select coalesce(jsonb_agg(to_jsonb(a) - 'id' - 'pre_application_folio' - 'institution_id' order by a.position), '[]'::jsonb)
    into v_autorizados from public.pre_application_autorizados a where a.pre_application_folio = p_folio;

  select coalesce(jsonb_agg(to_jsonb(d) - 'id' - 'pre_application_folio' - 'institution_id' order by d.position), '[]'::jsonb)
    into v_documentos from public.pre_application_documentos d where d.pre_application_folio = p_folio;

  return jsonb_build_object(
    'record', (to_jsonb(v_row) - 'access_token'),
    'responsables', v_responsables,
    'autorizados', v_autorizados,
    'documentos', v_documentos
  );
end;
$$;

grant execute on function public.get_pre_application_with_children(text, uuid) to anon;
