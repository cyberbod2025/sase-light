-- SASE-310 — M1-B: persistencia institucional del nucleo del expediente.
--
-- ESTADO: ESCRITA Y **NO APLICADA** contra el proyecto remoto "SASE-Light"
-- (ref plyjvvpkaafnkxmmqkbh). La autorizacion para mutar el backend fue
-- denegada explicitamente en la sesion que la escribio: se implemento el
-- cliente y sus pruebas contra MockEngine, y este archivo queda para revision
-- humana previa. Aplicar con `supabase db push` o el MCP (apply_migration)
-- SOLO cuando exista autorizacion expresa.
--
-- NO APLICAR `0003_seed_reference_data.sql`: su catalogo de roles/permisos
-- (6 roles, 11 permisos) contradice el catalogo real del remoto (10 roles, 18
-- permisos granulares). Esta migracion se apoya en el catalogo REAL, verificado
-- por introspeccion read-only: VIEW_STUDENT_BASE, EDIT_STUDENT_IDENTITY y
-- VIEW_AUDIT ya existen en public.permissions.
--
-- ALCANCE DELIBERADO (M1-B): solo el nucleo institucional del expediente
-- —identidad administrativa y ubicacion escolar—. Datos medicos,
-- socioeconomicos, de UDEII y de Trabajo Social, asi como las colecciones
-- anidadas (documentos, observaciones, incidencias) quedan FUERA: necesitan
-- tablas propias con RLS por area antes de tocar informacion sensible de
-- menores. El cliente reconstruye esos campos con los valores por omision del
-- modelo `Student` hasta que existan esas tablas.

-- === Nucleo del expediente ====================================================

create table public.students (
  id                    uuid        primary key default gen_random_uuid(),
  institution_id        uuid        not null references public.institutions (id),
  full_name             text        not null,
  student_group         text        not null default '',
  enrollment_id         text,
  curp                  text        not null,
  shift                 text        not null default '',
  school_year           text        not null default '',
  status                text        not null default '',
  pre_application_folio text,
  created_at            timestamptz not null default now(),
  updated_at            timestamptz not null default now(),

  -- La identidad del alumno es unica DENTRO de su institucion; dos escuelas
  -- distintas nunca compiten por la misma CURP en esta tabla.
  constraint students_institution_curp_key unique (institution_id, curp),
  constraint students_full_name_not_blank check (btrim(full_name) <> ''),
  constraint students_curp_not_blank check (btrim(curp) <> '')
);

-- La matricula se asigna en el alta oficial, asi que puede faltar todavia.
-- Un indice parcial permite muchos NULL pero impide dos matriculas iguales.
create unique index students_institution_enrollment_key
  on public.students (institution_id, enrollment_id)
  where enrollment_id is not null;

create index students_institution_idx on public.students (institution_id);

create trigger trg_students_updated_at
  before update on public.students
  for each row execute function public.sase_set_updated_at();

-- === Bitacora institucional tipada ============================================
--
-- `access_audit` (migracion 0002) NO sirve para esto: carece de institution_id,
-- membership_id, rol, entidad y resultado, y su politica de lectura
-- (`leadership_reads_audit`) usa current_app_role(), que es GLOBAL — la
-- direccion de una institucion podria leer la bitacora de otra. Esta tabla
-- aisla por institucion de forma explicita.

create table public.student_audit_events (
  id               bigint      generated always as identity primary key,
  institution_id   uuid        not null references public.institutions (id),
  actor_profile_id uuid        not null references public.profiles (id),
  membership_id    uuid        not null references public.institutional_memberships (id),
  active_role      text        not null,
  action           text        not null,
  entity_type      text        not null,
  entity_id        text        not null,
  -- Marca declarada por el cliente (texto, tal cual la valida
  -- InstitutionalAuditValidator) y marca de servidor, no falsificable.
  occurred_at      text        not null,
  recorded_at      timestamptz not null default now(),
  result           text        not null,
  source_platform  text        not null,

  constraint student_audit_events_result_check
    check (result in ('AUTHORIZED', 'DENIED', 'FAILED'))
);

create index student_audit_events_institution_idx
  on public.student_audit_events (institution_id, recorded_at desc);

-- === RLS ======================================================================

alter table public.students enable row level security;
alter table public.student_audit_events enable row level security;

-- Leer expedientes exige el permiso base en ESA institucion. Sin membresia
-- activa, has_permission() devuelve false: el aislamiento falla cerrado.
create policy students_select_by_permission on public.students
  for select using (has_permission(institution_id, 'VIEW_STUDENT_BASE'));

create policy students_insert_by_permission on public.students
  for insert with check (has_permission(institution_id, 'EDIT_STUDENT_IDENTITY'));

-- `using` impide tomar una fila ajena; `with check` impide moverla a otra
-- institucion en el mismo UPDATE.
create policy students_update_by_permission on public.students
  for update using (has_permission(institution_id, 'EDIT_STUDENT_IDENTITY'))
  with check (has_permission(institution_id, 'EDIT_STUDENT_IDENTITY'));

-- Nadie puede escribir una bitacora a nombre de otro actor ni de otra
-- membresia: el actor debe ser el usuario autenticado y la membresia debe ser
-- la suya, activa, en esa institucion.
create policy student_audit_events_insert_own on public.student_audit_events
  for insert with check (
    actor_profile_id = (select auth.uid())
    and membership_id = public.current_membership(institution_id)
  );

create policy student_audit_events_select_by_permission on public.student_audit_events
  for select using (has_permission(institution_id, 'VIEW_AUDIT'));

-- La bitacora institucional es inmutable: no se declara ninguna politica de
-- UPDATE ni de DELETE, de modo que RLS las rechaza por omision.
