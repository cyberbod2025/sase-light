-- M1 SASE Light — Identidad y acceso (staff únicamente, sin datos de menores)

-- Roles espejo del enum AppRole del cliente KMP
create type public.app_role as enum (
  'familia', 'secretaria', 'direccion', 'medico', 'trabajo_social', 'udeei', 'docente'
);

-- Perfil 1:1 con auth.users
create table public.profiles (
  id uuid primary key references auth.users (id) on delete cascade,
  full_name text not null,
  role public.app_role not null default 'familia',
  active boolean not null default true,
  created_at timestamptz not null default now()
);

alter table public.profiles enable row level security;

-- Rol del usuario autenticado, sin recursión de RLS
create or replace function public.current_app_role()
returns public.app_role
language sql
stable
security definer
set search_path = ''
as $$
  select role from public.profiles where id = auth.uid()
$$;

revoke execute on function public.current_app_role() from anon, public;
grant execute on function public.current_app_role() to authenticated;

-- Cada usuario lee su propio perfil
create policy "read_own_profile" on public.profiles
  for select to authenticated
  using (id = auth.uid());

-- Dirección y Secretaría leen todos los perfiles
create policy "leadership_reads_all_profiles" on public.profiles
  for select to authenticated
  using (public.current_app_role() in ('direccion', 'secretaria'));

-- Sin política de INSERT/UPDATE/DELETE para clientes:
-- la gestión de perfiles y roles se hace desde el dashboard (service role),
-- lo que impide la auto-elevación de privilegios.

-- Alta automática de perfil al crearse una cuenta.
-- Rol por defecto: familia (mínimo privilegio); la elevación es manual/institucional.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.profiles (id, full_name, role)
  values (
    new.id,
    coalesce(nullif(trim(new.raw_user_meta_data ->> 'full_name'), ''), new.email, 'SIN NOMBRE'),
    'familia'
  );
  return new;
end;
$$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- Bitácora de accesos
create table public.access_audit (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users (id) on delete cascade,
  action text not null check (action in ('login', 'logout', 'denied')),
  at timestamptz not null default now(),
  detail text
);

alter table public.access_audit enable row level security;

-- Cada usuario registra únicamente sus propios eventos
create policy "insert_own_access_events" on public.access_audit
  for insert to authenticated
  with check (user_id = auth.uid());

-- Solo Dirección y Secretaría consultan la bitácora
create policy "leadership_reads_audit" on public.access_audit
  for select to authenticated
  using (public.current_app_role() in ('direccion', 'secretaria'));

-- Sin UPDATE/DELETE: la bitácora es de solo inserción desde la interfaz normal.
