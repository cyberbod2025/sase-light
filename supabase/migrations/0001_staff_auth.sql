-- SASE-310 — Etapa 1 de backend real: autenticacion y roles de personal.
-- Alcance deliberado: SOLO personal. Ningun dato de alumnos ni de familias
-- entra a la base hasta que roles y RLS esten probados.

create type staff_role as enum (
  'DIRECCION',
  'SECRETARIA',
  'TRABAJO_SOCIAL',
  'MEDICO_ESCOLAR',
  'UDEII',
  'DOCENTE'
);

-- Perfil institucional ligado 1:1 a auth.users.
create table staff_profiles (
  id          uuid primary key references auth.users (id) on delete cascade,
  email       text        not null unique,
  full_name   text        not null,
  role        staff_role  not null,
  active      boolean     not null default true,
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now()
);

create index staff_profiles_role_idx on staff_profiles (role) where active;

-- Helpers usados por las politicas. security definer para poder leer la
-- tabla sin recursion de RLS.
create function current_staff_role()
returns staff_role
language sql
stable
security definer
set search_path = public
as $$
  select role from staff_profiles where id = auth.uid() and active
$$;

create function is_direccion()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select coalesce(current_staff_role() = 'DIRECCION', false)
$$;

alter table staff_profiles enable row level security;

-- Cada quien lee su propio perfil.
create policy staff_read_own
  on staff_profiles for select
  using (id = auth.uid());

-- Direccion lee el directorio completo.
create policy staff_read_all_direccion
  on staff_profiles for select
  using (is_direccion());

-- Solo Direccion da de alta, modifica o desactiva personal.
create policy staff_write_direccion
  on staff_profiles for all
  using (is_direccion())
  with check (is_direccion());

create function touch_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger staff_profiles_touch_updated_at
  before update on staff_profiles
  for each row execute function touch_updated_at();
