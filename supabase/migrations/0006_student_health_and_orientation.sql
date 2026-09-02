-- SASE-310 — Cierre de PR #49: datos sensibles por area (salud, orientacion
-- UDEII/BAP), en tablas propias con RLS separado del resto del expediente —
-- decision explicita de Hugo (D-017, 2026-08-12): "tablas separadas por area
-- con RLS propio", consistente con el alcance que 0004 dejo fuera a proposito
-- ("necesitan tablas propias con RLS por area... antes de tocar informacion
-- sensible de menores").
--
-- ESTADO: **APLICADA** al proyecto remoto SASE-Light (ref plyjvvpkaafnkxmmqkbh)
-- el 2026-08-12, con autorizacion explicita de Hugo. get_advisors (security)
-- verificado tras aplicarla: sin hallazgos nuevos (solo warnings preexistentes
-- de 0002, no relacionados).
--
-- Ningun permiso nuevo fue necesario: el catalogo real (introspeccion
-- 2026-08-12) ya tiene VIEW_MEDICAL/EDIT_MEDICAL (rol MEDICO_ESCOLAR) y
-- VIEW_BAP/EDIT_BAP (rol UDEII) exactamente para esto.

-- === Salud (Medico Escolar) ==================================================

create table public.student_health_records (
  student_id       uuid        primary key references public.students (id) on delete cascade,
  institution_id   uuid        not null references public.institutions (id),
  allergies        text        not null default '',
  notes            text        not null default '',
  medications      text        not null default '',
  health_passes    text        not null default '',
  insurance_status text        not null default '',
  updated_at       timestamptz not null default now()
);

create index student_health_records_institution_idx
  on public.student_health_records (institution_id);

create trigger trg_student_health_records_updated_at
  before update on public.student_health_records
  for each row execute function public.sase_set_updated_at();

alter table public.student_health_records enable row level security;

create policy student_health_records_select_by_permission on public.student_health_records
  for select using (has_permission(institution_id, 'VIEW_MEDICAL'));

create policy student_health_records_insert_by_permission on public.student_health_records
  for insert with check (has_permission(institution_id, 'EDIT_MEDICAL'));

create policy student_health_records_update_by_permission on public.student_health_records
  for update using (has_permission(institution_id, 'EDIT_MEDICAL'))
  with check (has_permission(institution_id, 'EDIT_MEDICAL'));

-- === Orientacion / UDEII (BAP) ===============================================

create table public.student_orientation_records (
  student_id          uuid        primary key references public.students (id) on delete cascade,
  institution_id      uuid        not null references public.institutions (id),
  status              text        not null default '',
  last_appointment    text        not null default '',
  intervention_plan   text        not null default '',
  responsible_name    text        not null default '',
  updated_at          timestamptz not null default now()
);

create index student_orientation_records_institution_idx
  on public.student_orientation_records (institution_id);

create trigger trg_student_orientation_records_updated_at
  before update on public.student_orientation_records
  for each row execute function public.sase_set_updated_at();

alter table public.student_orientation_records enable row level security;

create policy student_orientation_records_select_by_permission on public.student_orientation_records
  for select using (has_permission(institution_id, 'VIEW_BAP'));

create policy student_orientation_records_insert_by_permission on public.student_orientation_records
  for insert with check (has_permission(institution_id, 'EDIT_BAP'));

create policy student_orientation_records_update_by_permission on public.student_orientation_records
  for update using (has_permission(institution_id, 'EDIT_BAP'))
  with check (has_permission(institution_id, 'EDIT_BAP'));
