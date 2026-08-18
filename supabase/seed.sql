-- SASE-310 — Semilla EXCLUSIVA del stack local (`supabase start` / `supabase db reset`).
--
-- Este archivo NUNCA se aplica a un proyecto remoto: la CLI de Supabase solo lo
-- ejecuta contra la base local. Es la unica pieza del repositorio que crea
-- cuentas (auth.users) y lo hace a proposito, para que la verificacion de RLS
-- con DOS identidades reales sea reproducible con un solo comando.
--
-- DATOS 100% FICTICIOS. Alumnos inventados, correos `example.invalid`,
-- contrasena unica `demo1234` — misma convencion que MockStaffDirectory.
--
-- Identidades:
--   docente1@example.invalid  DOCENTE, asignado al grupo "1° A"      (caso positivo)
--   docente2@example.invalid  DOCENTE, asignado al grupo "2° B" pero
--                             NO al grupo "1° A"                      (caso negativo)

begin;

-- === Cuentas de Supabase Auth ================================================
-- El trigger public.handle_new_user() crea automaticamente la fila de
-- public.profiles al insertar en auth.users.
--
-- Los cuatro campos de token (confirmation_token, recovery_token,
-- email_change_token_new, email_change) se insertan como '' explicito, no NULL:
-- GoTrue los escanea como Go string (no puntero) al hacer login por password y
-- revienta con "converting NULL to string is unsupported" si quedan NULL.

insert into auth.users (
  instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
  raw_app_meta_data, raw_user_meta_data, created_at, updated_at,
  confirmation_token, recovery_token, email_change_token_new, email_change
)
values
  (
    '00000000-0000-0000-0000-000000000000',
    'aaaaaaaa-0000-4000-8000-000000000001',
    'authenticated', 'authenticated', 'docente1@example.invalid',
    extensions.crypt('demo1234', extensions.gen_salt('bf')), now(),
    '{"provider":"email","providers":["email"]}'::jsonb,
    '{"full_name":"Docente Demo Uno"}'::jsonb,
    now(), now(),
    '', '', '', ''
  ),
  (
    '00000000-0000-0000-0000-000000000000',
    'bbbbbbbb-0000-4000-8000-000000000002',
    'authenticated', 'authenticated', 'docente2@example.invalid',
    extensions.crypt('demo1234', extensions.gen_salt('bf')), now(),
    '{"provider":"email","providers":["email"]}'::jsonb,
    '{"full_name":"Docente Demo Dos"}'::jsonb,
    now(), now(),
    '', '', '', ''
  )
on conflict (id) do nothing;

insert into auth.identities (
  id, user_id, provider_id, identity_data, provider, last_sign_in_at, created_at, updated_at
)
select
  gen_random_uuid(), u.id, u.id::text,
  jsonb_build_object('sub', u.id::text, 'email', u.email, 'email_verified', true),
  'email', now(), now(), now()
from auth.users u
where u.email in ('docente1@example.invalid', 'docente2@example.invalid')
on conflict (provider, provider_id) do nothing;

-- === Membresias institucionales y rol DOCENTE ================================
-- Institucion y rol 'DOCENTE' vienen de 0003_seed_reference_data.sql.

insert into public.institutional_memberships (id, profile_id, institution_id, active)
values
  ('aaaaaaaa-1111-4000-8000-000000000001', 'aaaaaaaa-0000-4000-8000-000000000001',
   '11111111-1111-1111-1111-111111111111', true),
  ('bbbbbbbb-1111-4000-8000-000000000002', 'bbbbbbbb-0000-4000-8000-000000000002',
   '11111111-1111-1111-1111-111111111111', true)
on conflict (profile_id, institution_id) do nothing;

insert into public.membership_roles (membership_id, role_id)
select m.id, r.id
from public.institutional_memberships m
join public.roles r on r.code = 'DOCENTE'
where m.id in (
  'aaaaaaaa-1111-4000-8000-000000000001',
  'bbbbbbbb-1111-4000-8000-000000000002'
)
on conflict (membership_id, role_id) do nothing;

-- === Grupos, roster y asignacion docente =====================================

insert into public.school_groups (id, institution_id, school_cycle_id, name, grade_label)
values
  ('33333333-0000-4000-8000-000000000001', '11111111-1111-1111-1111-111111111111',
   '2026-2027', '1° A', 'Primer grado'),
  ('33333333-0000-4000-8000-000000000002', '11111111-1111-1111-1111-111111111111',
   '2026-2027', '2° B', 'Segundo grado')
on conflict (institution_id, school_cycle_id, name) do nothing;

insert into public.group_students (group_id, full_name, list_number)
values
  ('33333333-0000-4000-8000-000000000001', 'Alumna Ficticia Alfa', 1),
  ('33333333-0000-4000-8000-000000000001', 'Alumno Ficticio Bravo', 2),
  ('33333333-0000-4000-8000-000000000001', 'Alumna Ficticia Charlie', 3),
  ('33333333-0000-4000-8000-000000000001', 'Alumno Ficticio Delta', 4),
  ('33333333-0000-4000-8000-000000000001', 'Alumna Ficticia Eco', 5),
  ('33333333-0000-4000-8000-000000000002', 'Alumno Ficticio Foxtrot', 1),
  ('33333333-0000-4000-8000-000000000002', 'Alumna Ficticia Golf', 2),
  ('33333333-0000-4000-8000-000000000002', 'Alumno Ficticio Hotel', 3)
on conflict (group_id, list_number) do nothing;

-- docente1 -> 1° A   |   docente2 -> 2° B (y NUNCA 1° A)
insert into public.group_teacher_assignments (group_id, membership_id, active)
values
  ('33333333-0000-4000-8000-000000000001', 'aaaaaaaa-1111-4000-8000-000000000001', true),
  ('33333333-0000-4000-8000-000000000002', 'bbbbbbbb-1111-4000-8000-000000000002', true)
on conflict (group_id, membership_id) do nothing;

commit;
