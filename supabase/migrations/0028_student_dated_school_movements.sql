-- SASE-310 — Migración 0028: Historial fechado de movimientos escolares y cortes institucionales
--
-- Registra altas iniciales/extemporáneas, cambios de grupo con fecha de vigencia,
-- traslados a otro plantel (con escuela receptora documentada) y bajas definitivas.
--
-- PREVIENE REGRESIÓN: No confundir traslado con deserción ni calcular cortes con
-- matrícula estática desfasada.
--
-- ESTADO: Preparada y documentada para aplicación en staging tras validación local.

create table if not exists public.student_movement_events (
  id                    uuid primary key default gen_random_uuid(),
  institution_id        uuid not null references public.institutions (id),
  student_id            uuid not null references public.students (id) on delete cascade,
  movement_type         text not null check (movement_type in (
                          'ALTA_INICIAL',
                          'ALTA_EXTEMPORANEA',
                          'CAMBIO_GRUPO',
                          'TRASLADO_SALIENTE',
                          'BAJA_DEFINITIVA'
                        )),
  effective_date        date not null,
  source_group          text,
  target_group          text,
  destination_school    text,
  reason                text not null default 'No especificado',
  folio_oficio          text,
  registered_by_staff_id uuid not null references public.institutional_memberships (id),
  registered_by_name    text not null,
  created_at            timestamptz not null default now(),

  -- Regla de integridad: si es traslado saliente, la escuela de destino es obligatoria
  constraint chk_transfer_destination check (
    movement_type <> 'TRASLADO_SALIENTE' or (destination_school is not null and length(trim(destination_school)) > 0)
  ),

  -- Regla de integridad: si es cambio de grupo, el grupo destino es obligatorio y distinto al origen
  constraint chk_group_change check (
    movement_type <> 'CAMBIO_GRUPO' or (target_group is not null and target_group <> coalesce(source_group, ''))
  )
);

create index if not exists idx_student_movements_student on public.student_movement_events (student_id);
create index if not exists idx_student_movements_effective_date on public.student_movement_events (effective_date);
create index if not exists idx_student_movements_institution on public.student_movement_events (institution_id);

alter table public.student_movement_events enable row level security;

-- Política de lectura: personal institucional con acceso a expedientes o secretaría
create policy student_movement_events_select on public.student_movement_events
  for select
  to authenticated
  using (
    institution_id in (
      select institution_id
      from public.institutional_memberships
      where user_id = auth.uid() and active
    )
  );

-- Política de inserción: restringida a personal con permiso de edición de identidad
create policy student_movement_events_insert on public.student_movement_events
  for insert
  to authenticated
  with check (
    registered_by_staff_id in (
      select id
      from public.institutional_memberships
      where user_id = auth.uid() and active
    )
  );

-- Revocar privilegios amplios por omisión en Postgres
revoke all on public.student_movement_events from public, anon;
grant select, insert on public.student_movement_events to authenticated;

-- =============================================================================
-- PROCEDIMIENTO DE REVERSIÓN (ROLLBACK):
-- En caso de requerir reversión en Supabase Staging, ejecutar en SQL Editor:
--
-- drop policy if exists student_movement_events_insert on public.student_movement_events;
-- drop policy if exists student_movement_events_select on public.student_movement_events;
-- drop table if exists public.student_movement_events cascade;
-- =============================================================================
