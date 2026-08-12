-- SASE-310 — Cierre de PR #49: persistencia real del nucleo operativo del
-- expediente que 0004 dejo fuera deliberadamente (domicilio, tutor, contacto
-- de emergencia, observaciones, incidencias). Resuelve dos P1 de Codex:
-- "Stop fabricating unpersisted student details" y "Reject updates for
-- fields the backend does not persist".
--
-- ESTADO: **APLICADA** al proyecto remoto SASE-Light (ref plyjvvpkaafnkxmmqkbh)
-- el 2026-08-12, con autorizacion explicita de Hugo. get_advisors (security)
-- verificado tras aplicarla: sin hallazgos nuevos (solo warnings preexistentes
-- de 0002, no relacionados).
--
-- DECISION DE ALCANCE (Hugo, 2026-08-12, D-017): los datos identitarios
-- sensibles (domicilio, tutor, contacto de emergencia, fecha/lugar de
-- nacimiento) NO se agregan como columnas sueltas a `students` bajo el mismo
-- permiso VIEW_STUDENT_BASE que ya tienen Docente/Medico/UDEII/Prefectura/
-- Tutor/Trabajo Social — verificado por introspeccion del catalogo real que
-- el permiso granular correcto ya existe: VIEW_STUDENT_SENSITIVE_IDENTITY
-- (solo SECRETARIA y DIRECCION lo tienen hoy) / EDIT_STUDENT_IDENTITY (solo
-- SECRETARIA). Del mismo modo, incidencias usan VIEW_INCIDENTS/EDIT_INCIDENTS
-- (Prefectura/Tutor/Docente/Direccion/Trabajo Social), que YA distingue de
-- EDIT_STUDENT_IDENTITY — Secretaria no tiene ese permiso en el catalogo
-- real, asi que no puede reportar incidencias por diseno. Ningun permiso
-- nuevo fue necesario: el catalogo de 0002/introspeccion ya cubria estas
-- areas.

-- === Identidad sensible (domicilio, tutor, contacto de emergencia) ==========

create table public.student_sensitive_identity (
  student_id               uuid        primary key references public.students (id) on delete cascade,
  institution_id            uuid        not null references public.institutions (id),
  birth_date                text        not null default '',
  birth_place                text        not null default '',
  address                    text        not null default '',
  zip_code                   text        not null default '',
  tutor_name                 text        not null default '',
  tutor_relation              text        not null default '',
  tutor_phone                 text        not null default '',
  tutor_email                 text        not null default '',
  emergency_contact_name      text        not null default '',
  emergency_contact_relation   text        not null default '',
  emergency_contact_phone      text        not null default '',
  emergency_contact_email      text        not null default '',
  updated_at                  timestamptz not null default now()
);

create index student_sensitive_identity_institution_idx
  on public.student_sensitive_identity (institution_id);

create trigger trg_student_sensitive_identity_updated_at
  before update on public.student_sensitive_identity
  for each row execute function public.sase_set_updated_at();

alter table public.student_sensitive_identity enable row level security;

create policy student_sensitive_identity_select_by_permission on public.student_sensitive_identity
  for select using (has_permission(institution_id, 'VIEW_STUDENT_SENSITIVE_IDENTITY'));

create policy student_sensitive_identity_insert_by_permission on public.student_sensitive_identity
  for insert with check (has_permission(institution_id, 'EDIT_STUDENT_IDENTITY'));

create policy student_sensitive_identity_update_by_permission on public.student_sensitive_identity
  for update using (has_permission(institution_id, 'EDIT_STUDENT_IDENTITY'))
  with check (has_permission(institution_id, 'EDIT_STUDENT_IDENTITY'));

-- === Observaciones institucionales ==========================================

-- author_name se agrego en 0008 (denormalizado, mismo patron que
-- student_incidents.reporter_name — evita un join contra profiles en cada
-- lectura).
create table public.student_observations (
  id               uuid        primary key default gen_random_uuid(),
  student_id       uuid        not null references public.students (id) on delete cascade,
  institution_id   uuid        not null references public.institutions (id),
  author_profile_id uuid       not null references public.profiles (id),
  category         text        not null default '',
  observation_text text        not null,
  created_at       timestamptz not null default now(),

  constraint student_observations_text_not_blank check (btrim(observation_text) <> '')
);

create index student_observations_student_idx
  on public.student_observations (student_id, created_at desc);

alter table public.student_observations enable row level security;

create policy student_observations_select_by_permission on public.student_observations
  for select using (has_permission(institution_id, 'VIEW_STUDENT_SENSITIVE_IDENTITY'));

-- El autor debe ser el usuario autenticado: nadie registra una observacion
-- a nombre de otro miembro del staff.
create policy student_observations_insert_by_permission on public.student_observations
  for insert with check (
    has_permission(institution_id, 'EDIT_STUDENT_IDENTITY')
    and author_profile_id = (select auth.uid())
  );

-- Las observaciones son historial institucional inmutable: no se declara
-- UPDATE ni DELETE, RLS las rechaza por omision.

-- === Incidencias escolares ===================================================

-- CORREGIDO por 0007_fix_student_incidents_schema.sql: el esquema original
-- de abajo (description/status de 3 valores) no correspondia al modelo real
-- del cliente (SaseIncident/IncidentWorkflow: type/date/reporter/status de 4
-- valores/agreementNotes/followUps). 0007 hizo drop+create sobre la tabla
-- vacia (0 filas) inmediatamente despues, con autorizacion explicita de
-- Hugo. Este bloque queda como registro historico de lo que se aplico aqui;
-- el esquema vigente esta en 0007.
create table public.student_incidents (
  id                    uuid        primary key default gen_random_uuid(),
  student_id            uuid        not null references public.students (id) on delete cascade,
  institution_id        uuid        not null references public.institutions (id),
  reported_by_profile_id uuid       not null references public.profiles (id),
  description           text        not null,
  status                text        not null default 'ABIERTA',
  created_at            timestamptz not null default now(),
  updated_at            timestamptz not null default now(),

  constraint student_incidents_description_not_blank check (btrim(description) <> ''),
  constraint student_incidents_status_check
    check (status in ('ABIERTA', 'EN_SEGUIMIENTO', 'CERRADA'))
);

create index student_incidents_student_idx
  on public.student_incidents (student_id, created_at desc);

create trigger trg_student_incidents_updated_at
  before update on public.student_incidents
  for each row execute function public.sase_set_updated_at();

alter table public.student_incidents enable row level security;

create policy student_incidents_select_by_permission on public.student_incidents
  for select using (has_permission(institution_id, 'VIEW_INCIDENTS'));

create policy student_incidents_insert_by_permission on public.student_incidents
  for insert with check (
    has_permission(institution_id, 'EDIT_INCIDENTS')
    and reported_by_profile_id = (select auth.uid())
  );

create policy student_incidents_update_by_permission on public.student_incidents
  for update using (has_permission(institution_id, 'EDIT_INCIDENTS'))
  with check (has_permission(institution_id, 'EDIT_INCIDENTS'));
