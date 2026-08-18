-- SASE-310 — Rebanada vertical: asistencia por clase y grupo (docente).
--
-- Se apoya en el modelo de identidad ya vigente en 0002
-- (profiles / institutions / institutional_memberships / roles /
-- membership_roles) y en los datos de referencia de 0003 (rol 'DOCENTE').
-- NO introduce un modelo de autenticacion ni de rol paralelo.
--
-- Modelo:
--   school_groups              grupo escolar de una institucion y un ciclo
--   group_students             lista (roster) del grupo — alumnos ficticios de demo
--   group_teacher_assignments  que membresia docente atiende que grupo
--   class_sessions             una sesion por (grupo, docente, fecha)
--   class_attendance           un registro por (sesion, alumno)
--
-- Escritura: EXCLUSIVAMENTE por RPC security definer atomica. Las tablas de
-- asistencia no tienen politicas INSERT/UPDATE/DELETE para `authenticated`, de
-- modo que un cliente no puede construir un guardado parcial ni saltarse la
-- validacion de roster completo.

-- === Tipos ===================================================================

create type public.attendance_status as enum ('PRESENTE', 'AUSENTE', 'RETARDO');

-- === Tablas ==================================================================

create table public.school_groups (
  id              uuid primary key default gen_random_uuid(),
  institution_id  uuid        not null references public.institutions (id),
  school_cycle_id text        not null,
  name            text        not null,
  grade_label     text        not null,
  active          boolean     not null default true,
  created_at      timestamptz not null default now(),
  updated_at      timestamptz not null default now(),
  unique (institution_id, school_cycle_id, name)
);

create table public.group_students (
  id          uuid primary key default gen_random_uuid(),
  group_id    uuid        not null references public.school_groups (id) on delete cascade,
  full_name   text        not null,
  list_number integer     not null check (list_number > 0),
  active      boolean     not null default true,
  created_at  timestamptz not null default now(),
  unique (group_id, list_number)
);

create table public.group_teacher_assignments (
  id            uuid primary key default gen_random_uuid(),
  group_id      uuid        not null references public.school_groups (id) on delete cascade,
  membership_id uuid        not null references public.institutional_memberships (id),
  active        boolean     not null default true,
  created_at    timestamptz not null default now(),
  unique (group_id, membership_id)
);

-- Una sola sesion por grupo + docente + fecha: la unicidad la garantiza la
-- base, no el cliente. Reabrir la misma fecha recupera la misma fila.
create table public.class_sessions (
  id            uuid primary key default gen_random_uuid(),
  group_id      uuid        not null references public.school_groups (id),
  membership_id uuid        not null references public.institutional_memberships (id),
  session_date  date        not null,
  created_at    timestamptz not null default now(),
  updated_at    timestamptz not null default now(),
  unique (group_id, membership_id, session_date)
);

-- Un solo registro por sesion + alumno: modificar no duplica.
create table public.class_attendance (
  id               uuid primary key default gen_random_uuid(),
  class_session_id uuid                     not null references public.class_sessions (id) on delete cascade,
  student_id       uuid                     not null references public.group_students (id),
  status           public.attendance_status not null,
  recorded_at      timestamptz              not null default now(),
  unique (class_session_id, student_id)
);

create index idx_group_students_group on public.group_students (group_id);
create index idx_group_teacher_assignments_membership on public.group_teacher_assignments (membership_id);
create index idx_class_sessions_group_date on public.class_sessions (group_id, session_date);
create index idx_class_attendance_session on public.class_attendance (class_session_id);

create trigger trg_school_groups_updated_at
  before update on public.school_groups
  for each row execute function public.sase_set_updated_at();

create trigger trg_class_sessions_updated_at
  before update on public.class_sessions
  for each row execute function public.sase_set_updated_at();

-- === Funciones de autorizacion ==============================================

-- Membresia DOCENTE activa del actor para ese grupo, o null. Unico lugar donde
-- se decide "este docente atiende este grupo".
create or replace function public.sase_actor_teaching_membership(p_group_id uuid)
returns uuid
language sql stable security definer set search_path = ''
as $$
  select m.id
  from public.institutional_memberships m
  join public.membership_roles mr on mr.membership_id = m.id
  join public.roles r on r.id = mr.role_id
  join public.group_teacher_assignments a on a.membership_id = m.id
  join public.school_groups g on g.id = a.group_id
  where m.profile_id = (select auth.uid())
    and m.active
    and r.code = 'DOCENTE'
    and a.active
    and a.group_id = p_group_id
    and g.institution_id = m.institution_id
    and g.active
  limit 1
$$;

create or replace function public.sase_actor_teaches_group(p_group_id uuid)
returns boolean
language sql stable security definer set search_path = ''
as $$
  select public.sase_actor_teaching_membership(p_group_id) is not null
$$;

-- === RLS =====================================================================

alter table public.school_groups enable row level security;
alter table public.group_students enable row level security;
alter table public.group_teacher_assignments enable row level security;
alter table public.class_sessions enable row level security;
alter table public.class_attendance enable row level security;

alter table public.school_groups force row level security;
alter table public.group_students force row level security;
alter table public.group_teacher_assignments force row level security;
alter table public.class_sessions force row level security;
alter table public.class_attendance force row level security;

create policy school_groups_select_assigned_teacher on public.school_groups
  for select using (public.sase_actor_teaches_group(id));

create policy group_students_select_assigned_teacher on public.group_students
  for select using (public.sase_actor_teaches_group(group_id));

create policy group_teacher_assignments_select_own on public.group_teacher_assignments
  for select using (public.membership_owner(membership_id) = (select auth.uid()));

-- Un docente solo ve SUS sesiones de SUS grupos: ni la sesion de otro docente
-- del mismo grupo, ni ninguna sesion de un grupo que no atiende.
create policy class_sessions_select_own on public.class_sessions
  for select using (
    public.membership_owner(membership_id) = (select auth.uid())
    and public.sase_actor_teaches_group(group_id)
  );

create policy class_attendance_select_own on public.class_attendance
  for select using (
    exists (
      select 1
      from public.class_sessions s
      where s.id = class_attendance.class_session_id
        and public.membership_owner(s.membership_id) = (select auth.uid())
        and public.sase_actor_teaches_group(s.group_id)
    )
  );

-- Deliberadamente NO existen politicas INSERT/UPDATE/DELETE: toda escritura
-- pasa por sase_open_class_session / sase_save_class_attendance.

-- === Privilegios de tabla ====================================================
--
-- REGRESION CONOCIDA QUE ESTA MIGRACION EVITA: la RLS de fila NO filtra
-- TRUNCATE, REFERENCES ni TRIGGER. Un GRANT ALL (o el privilegio heredado por
-- PUBLIC) dejaria a cualquier sesion autenticada truncar la tabla o colgarle un
-- trigger, con las politicas intactas. Por eso se revoca explicitamente TODO
-- antes de otorgar unicamente SELECT.

revoke all on table public.school_groups              from public, anon, authenticated;
revoke all on table public.group_students             from public, anon, authenticated;
revoke all on table public.group_teacher_assignments  from public, anon, authenticated;
revoke all on table public.class_sessions             from public, anon, authenticated;
revoke all on table public.class_attendance           from public, anon, authenticated;

grant select on table public.school_groups             to authenticated;
grant select on table public.group_students            to authenticated;
grant select on table public.group_teacher_assignments to authenticated;
grant select on table public.class_sessions            to authenticated;
grant select on table public.class_attendance          to authenticated;

-- === RPCs ====================================================================

-- Vista canonica de una sesion: grupo, fecha y roster COMPLETO con el estado
-- capturado (por defecto PRESENTE cuando aun no hay registro). Es un helper
-- interno: no se otorga EXECUTE a ningun rol de cliente, porque al ser security
-- definer devolveria el roster sin evaluar la asignacion del docente.
create or replace function public.sase_class_session_snapshot(p_session_id uuid)
returns jsonb
language sql stable security definer set search_path = ''
as $$
  select jsonb_build_object(
    'session_id', s.id,
    'group_id', s.group_id,
    'group_name', g.name,
    'grade_label', g.grade_label,
    'membership_id', s.membership_id,
    'session_date', to_char(s.session_date, 'YYYY-MM-DD'),
    'saved_at', to_char(s.updated_at at time zone 'UTC', 'YYYY-MM-DD"T"HH24:MI:SSZ'),
    'roster', coalesce(
      (
        select jsonb_agg(
          jsonb_build_object(
            'student_id', st.id,
            'full_name', st.full_name,
            'list_number', st.list_number,
            'status', coalesce(a.status::text, 'PRESENTE'),
            'recorded', (a.id is not null)
          )
          order by st.list_number
        )
        from public.group_students st
        left join public.class_attendance a
          on a.class_session_id = s.id and a.student_id = st.id
        where st.group_id = s.group_id and st.active
      ),
      '[]'::jsonb
    )
  )
  from public.class_sessions s
  join public.school_groups g on g.id = s.group_id
  where s.id = p_session_id
$$;

-- Inicia o recupera la sesion de clase de una fecha. Idempotente: llamarla dos
-- veces para el mismo grupo+docente+fecha devuelve SIEMPRE la misma sesion.
create or replace function public.sase_open_class_session(p_group_id uuid, p_date date)
returns jsonb
language plpgsql security definer set search_path = ''
as $$
declare
  v_membership uuid;
  v_session_id uuid;
begin
  if p_group_id is null or p_date is null then
    raise exception 'SASE_ATTENDANCE_ARGS_REQUIRED' using errcode = '22023';
  end if;

  v_membership := public.sase_actor_teaching_membership(p_group_id);
  if v_membership is null then
    raise exception 'SASE_ATTENDANCE_FORBIDDEN' using errcode = '42501';
  end if;

  select s.id into v_session_id
  from public.class_sessions s
  where s.group_id = p_group_id
    and s.membership_id = v_membership
    and s.session_date = p_date;

  if v_session_id is null then
    begin
      insert into public.class_sessions (group_id, membership_id, session_date)
      values (p_group_id, v_membership, p_date)
      returning id into v_session_id;
    exception when unique_violation then
      -- Dos aperturas simultaneas de la misma fecha: gana la primera y la
      -- segunda recupera esa misma sesion. Nunca se crea una duplicada.
      select s.id into v_session_id
      from public.class_sessions s
      where s.group_id = p_group_id
        and s.membership_id = v_membership
        and s.session_date = p_date;
    end;
  end if;

  return public.sase_class_session_snapshot(v_session_id);
end;
$$;

-- Guarda la captura COMPLETA del grupo en una sola operacion logica.
--
-- REGRESION CONOCIDA QUE ESTA FUNCION EVITA: guardar solo el diff. Si el
-- docente deja a todo el grupo en PRESENTE sin tocar nada, esa captura debe
-- persistir igual. Por eso el servidor EXIGE que p_entries cubra exactamente el
-- roster activo del grupo; un envio parcial se rechaza entero (la funcion es
-- atomica: o se guarda todo el grupo o no se guarda nada).
create or replace function public.sase_save_class_attendance(p_session_id uuid, p_entries jsonb)
returns jsonb
language plpgsql security definer set search_path = ''
as $$
declare
  v_group_id     uuid;
  v_membership   uuid;
  v_owner        uuid;
  v_entry_count  integer;
  v_unique_count integer;
  v_roster_count integer;
begin
  if p_session_id is null then
    raise exception 'SASE_ATTENDANCE_ARGS_REQUIRED' using errcode = '22023';
  end if;
  if p_entries is null or jsonb_typeof(p_entries) <> 'array' then
    raise exception 'SASE_ATTENDANCE_ENTRIES_INVALID' using errcode = '22023';
  end if;

  select s.group_id, s.membership_id
    into v_group_id, v_owner
  from public.class_sessions s
  where s.id = p_session_id;

  -- Sesion inexistente y sesion ajena devuelven el MISMO error: no se filtra
  -- la existencia de sesiones de otros docentes.
  if v_group_id is null then
    raise exception 'SASE_ATTENDANCE_FORBIDDEN' using errcode = '42501';
  end if;

  v_membership := public.sase_actor_teaching_membership(v_group_id);
  if v_membership is null or v_membership <> v_owner then
    raise exception 'SASE_ATTENDANCE_FORBIDDEN' using errcode = '42501';
  end if;

  select count(*), count(distinct t.student_id)
    into v_entry_count, v_unique_count
  from (
    select (e ->> 'student_id')::uuid as student_id
    from jsonb_array_elements(p_entries) e
  ) t;

  if v_entry_count <> v_unique_count then
    raise exception 'SASE_ATTENDANCE_DUPLICATE_STUDENT' using errcode = '22023';
  end if;

  select count(*) into v_roster_count
  from public.group_students
  where group_id = v_group_id and active;

  if v_entry_count <> v_roster_count then
    raise exception 'SASE_ATTENDANCE_INCOMPLETE_ROSTER' using errcode = '22023';
  end if;

  if exists (
    select 1
    from (
      select (e ->> 'student_id')::uuid as student_id
      from jsonb_array_elements(p_entries) e
    ) t
    left join public.group_students st
      on st.id = t.student_id and st.group_id = v_group_id and st.active
    where st.id is null
  ) then
    raise exception 'SASE_ATTENDANCE_STUDENT_NOT_IN_GROUP' using errcode = '22023';
  end if;

  insert into public.class_attendance (class_session_id, student_id, status)
  select
    p_session_id,
    (e ->> 'student_id')::uuid,
    (e ->> 'status')::public.attendance_status
  from jsonb_array_elements(p_entries) e
  on conflict (class_session_id, student_id) do update
    set status = excluded.status,
        recorded_at = now();

  update public.class_sessions set updated_at = now() where id = p_session_id;

  return public.sase_class_session_snapshot(p_session_id);
end;
$$;

-- === Privilegios de funcion ==================================================
--
-- Por defecto PostgreSQL otorga EXECUTE a PUBLIC en toda funcion nueva. Se
-- revoca y se otorga solo a `authenticated`; el helper de snapshot no se otorga
-- a nadie.

revoke all on function public.sase_actor_teaching_membership(uuid) from public, anon, authenticated;
revoke all on function public.sase_actor_teaches_group(uuid)       from public, anon, authenticated;
revoke all on function public.sase_class_session_snapshot(uuid)    from public, anon, authenticated;
revoke all on function public.sase_open_class_session(uuid, date)  from public, anon, authenticated;
revoke all on function public.sase_save_class_attendance(uuid, jsonb) from public, anon, authenticated;

-- Las expresiones de una politica RLS se evaluan con los privilegios de quien
-- ejecuta la consulta, asi que `authenticated` SI necesita EXECUTE sobre los
-- dos helpers de autorizacion. No filtran nada: ambos responden unicamente
-- sobre la membresia del propio auth.uid().

grant execute on function public.sase_actor_teaching_membership(uuid) to authenticated;
grant execute on function public.sase_actor_teaches_group(uuid)       to authenticated;
grant execute on function public.sase_open_class_session(uuid, date)     to authenticated;
grant execute on function public.sase_save_class_attendance(uuid, jsonb) to authenticated;
