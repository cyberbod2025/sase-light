-- SASE-310 — correccion inmediata de 0005: `student_incidents` se diseno
-- contra un modelo de incidencia equivocado (description/status de 3
-- valores). El modelo real en el cliente (`SaseIncident` +
-- `IncidentWorkflow`) usa type/date/reporter/status de 4 valores
-- (En seguimiento/Citatorio enviado/Acuerdo firmado/Atendida),
-- reportedByStaffId, agreementNotes y followUps (lista). Tabla vacia
-- (0 filas, verificado antes de este cambio): drop+recreate seguro, no hay
-- datos que perder.

drop table public.student_incidents;

create table public.student_incidents (
  id                     uuid        primary key default gen_random_uuid(),
  student_id             uuid        not null references public.students (id) on delete cascade,
  institution_id         uuid        not null references public.institutions (id),
  incident_type          text        not null,
  incident_date          text        not null default '',
  status                 text        not null default 'En seguimiento',
  reporter_name          text        not null default '',
  reported_by_profile_id uuid        references public.profiles (id),
  agreement_notes        text,
  follow_ups             text[]      not null default '{}',
  created_at             timestamptz not null default now(),
  updated_at             timestamptz not null default now(),

  constraint student_incidents_type_not_blank check (btrim(incident_type) <> ''),
  constraint student_incidents_status_check
    check (status in ('En seguimiento', 'Citatorio enviado', 'Acuerdo firmado', 'Atendida'))
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
  for insert with check (has_permission(institution_id, 'EDIT_INCIDENTS'));

create policy student_incidents_update_by_permission on public.student_incidents
  for update using (has_permission(institution_id, 'EDIT_INCIDENTS'))
  with check (has_permission(institution_id, 'EDIT_INCIDENTS'));
