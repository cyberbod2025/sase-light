-- SASE-310 — Reconciliacion de esquema: Usuario -> Membresia -> Rol -> Permisos -> Alcances.
--
-- CONTEXTO: el proyecto Supabase "SASE-Light" (ref plyjvvpkaafnkxmmqkbh) ya
-- tiene este esquema aplicado en remoto, via 5 migraciones que nunca se
-- commitearon a este repositorio:
--   20260716141809 m1_identity_access
--   20260716141910 m1_lockdown_trigger_function
--   20260721021208 m2_memberships_roles_scopes
--   20260721024127 m2_1_lock_trigger_functions
--   20260721030257 m2_2_grant_admin_to_direccion
--
-- Este archivo NO se ha ejecutado contra el proyecto remoto (ya existe alla).
-- Es una reconstruccion fiel obtenida por introspeccion read-only (information_schema,
-- pg_policy, pg_proc) del estado actual, para que el repositorio deje de estar
-- desincronizado del backend real. No se reclama que replique el split exacto
-- de las 5 migraciones originales (esas nunca se leyeron), solo el esquema
-- resultante. `0001_staff_auth.sql` (staff_profiles/staff_role) quedo obsoleto:
-- ese enum/tabla NO existen en remoto; el modelo real es el de abajo.
--
-- Reemplaza `staff_profiles` + `staff_role` (enum fijo) por un modelo
-- relacional: roles y permisos son FILAS (roles.code, permissions.code), no
-- un enum de Kotlin, para poder asignarse/editarse sin recompilar la app.

-- === Tipos ===================================================================

create type app_role as enum (
  'familia', 'secretaria', 'direccion', 'medico', 'trabajo_social', 'udeei', 'docente'
);

-- === Tablas ===================================================================

-- Perfil ligado 1:1 a auth.users. role = etiqueta de alta/coarse (default
-- 'familia' al registrarse); la autorizacion institucional fina vive en
-- institutional_memberships/membership_roles/membership_scopes, no aqui.
create table public.profiles (
  id           uuid primary key references auth.users (id) on delete cascade,
  full_name    text        not null,
  role         app_role    not null default 'familia',
  active       boolean     not null default true,
  created_at   timestamptz not null default now(),
  display_name text
);

create table public.institutions (
  id         uuid primary key default gen_random_uuid(),
  name       text        not null unique,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- Un perfil pertenece a una institucion via una membresia (activa/inactiva).
create table public.institutional_memberships (
  id             uuid primary key default gen_random_uuid(),
  profile_id     uuid        not null references public.profiles (id),
  institution_id uuid        not null references public.institutions (id),
  active         boolean     not null default true,
  created_at     timestamptz not null default now(),
  updated_at     timestamptz not null default now(),
  unique (profile_id, institution_id)
);

create table public.roles (
  id          uuid primary key default gen_random_uuid(),
  code        text        not null unique,
  description text,
  created_at  timestamptz not null default now()
);

create table public.permissions (
  id          uuid primary key default gen_random_uuid(),
  code        text        not null unique,
  description text,
  created_at  timestamptz not null default now()
);

create table public.role_permissions (
  role_id       uuid        not null references public.roles (id),
  permission_id uuid        not null references public.permissions (id),
  created_at    timestamptz not null default now(),
  primary key (role_id, permission_id)
);

-- Que roles institucionales tiene una membresia (M:N: una membresia puede
-- acumular varios roles).
create table public.membership_roles (
  membership_id uuid        not null references public.institutional_memberships (id),
  role_id       uuid        not null references public.roles (id),
  created_at    timestamptz not null default now(),
  primary key (membership_id, role_id)
);

-- Alcances/Scopes de una membresia (p.ej. OWN_RECORD limita a un solo
-- expediente propio). scope_key es nullable para scopes sin clave.
create table public.membership_scopes (
  id            uuid primary key default gen_random_uuid(),
  membership_id uuid not null references public.institutional_memberships (id),
  scope_type    text not null,
  scope_key     text,
  created_at    timestamptz not null default now(),
  unique (membership_id, scope_type, scope_key)
);

-- Bitacora institucional (equivalente servidor de SaseAudit / logSaseAudit).
create table public.access_audit (
  id      bigint generated always as identity primary key,
  user_id uuid        not null,
  action  text        not null,
  at      timestamptz not null default now(),
  detail  text
);

-- === Funciones ================================================================

create or replace function public.current_app_role()
returns app_role
language sql stable security definer set search_path = ''
as $$
  select role from public.profiles where id = auth.uid()
$$;

create or replace function public.current_membership(p_institution_id uuid)
returns uuid
language sql stable security definer set search_path = ''
as $$
  select id from public.institutional_memberships
  where profile_id = auth.uid() and institution_id = p_institution_id and active
$$;

create or replace function public.actor_has_membership()
returns boolean
language sql stable security definer set search_path = ''
as $$
  select exists (
    select 1 from public.institutional_memberships
    where profile_id = auth.uid() and active
  )
$$;

create or replace function public.membership_owner(p_membership_id uuid)
returns uuid
language sql stable security definer set search_path = ''
as $$
  select profile_id from public.institutional_memberships where id = p_membership_id
$$;

create or replace function public.membership_institution(p_membership_id uuid)
returns uuid
language sql stable security definer set search_path = ''
as $$
  select institution_id from public.institutional_memberships where id = p_membership_id
$$;

create or replace function public.actor_holds_role(p_institution_id uuid, p_role_id uuid)
returns boolean
language sql stable security definer set search_path = ''
as $$
  select exists (
    select 1 from public.institutional_memberships m
    join public.membership_roles mr on mr.membership_id = m.id
    where m.profile_id = auth.uid()
      and m.institution_id = p_institution_id
      and m.active
      and mr.role_id = p_role_id
  )
$$;

create or replace function public.has_permission(p_institution_id uuid, p_permission_code text)
returns boolean
language sql stable security definer set search_path = ''
as $$
    select exists (
        select 1
        from public.institutional_memberships m
        join public.membership_roles mr on mr.membership_id = m.id
        join public.role_permissions rp on rp.role_id = mr.role_id
        join public.permissions p on p.id = rp.permission_id
        where m.profile_id = (select auth.uid())
          and m.institution_id = p_institution_id
          and m.active
          and p.code = p_permission_code
    );
$$;

create or replace function public.has_scope(p_institution_id uuid, p_scope_type text, p_scope_key text)
returns boolean
language sql stable security definer set search_path = ''
as $$
    select exists (
        select 1
        from public.institutional_memberships m
        join public.membership_scopes ms on ms.membership_id = m.id
        where m.profile_id = (select auth.uid())
          and m.institution_id = p_institution_id
          and m.active
          and ms.scope_type = p_scope_type
          and (ms.scope_key = p_scope_key or (ms.scope_key is null and p_scope_key is null))
    );
$$;

-- Puente app_role (enum de alta) -> codigo de rol institucional (roles.code).
create or replace function public.sase_app_role_to_code(p_role app_role)
returns text
language sql immutable set search_path = ''
as $$
    select case p_role
        when 'direccion'      then 'DIRECCION'
        when 'secretaria'     then 'SECRETARIA'
        when 'trabajo_social' then 'TRABAJO_SOCIAL'
        when 'medico'         then 'MEDICO_ESCOLAR'
        when 'udeei'          then 'UDEII'
        when 'docente'        then 'DOCENTE'
        when 'familia'        then 'FAMILIA'
    end;
$$;

create or replace function public.sase_set_updated_at()
returns trigger
language plpgsql set search_path = ''
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

-- Impide que un actor se otorgue/retire roles o alcances a si mismo (incluso
-- siendo Direccion): toda concesion de privilegio requiere OTRA identidad.
-- Se salta unicamente para procesos de migracion/admin (postgres/supabase_admin/
-- supabase_migration_user), nunca para sesiones de usuario final.
create or replace function public.sase_forbid_self_grant()
returns trigger
language plpgsql security definer set search_path = ''
as $$
declare
    v_actor uuid;
    v_owner uuid;
begin
    v_actor := (select auth.uid());

    if v_actor is null or current_user in ('postgres', 'supabase_admin', 'supabase_migration_user') then
        if tg_op = 'DELETE' then return old; else return new; end if;
    end if;

    if tg_op <> 'DELETE' then
        v_owner := public.membership_owner(new.membership_id);
        if v_owner is null then
            raise exception 'SASE_MEMBERSHIP_UNKNOWN';
        end if;
        if v_owner = v_actor then
            raise exception 'SASE_SELF_GRANT_FORBIDDEN';
        end if;
    end if;

    if tg_op <> 'INSERT' then
        v_owner := public.membership_owner(old.membership_id);
        if v_owner is null then
            raise exception 'SASE_MEMBERSHIP_UNKNOWN';
        end if;
        if v_owner = v_actor then
            raise exception 'SASE_SELF_GRANT_FORBIDDEN';
        end if;
    end if;

    if tg_op = 'DELETE' then return old; else return new; end if;
end;
$$;

-- OWN_RECORD es el unico scope_type validado hoy: su scope_key debe ser
-- exactamente el profile_id del dueno de la membresia (nadie puede fijar un
-- OWN_RECORD apuntando a otro expediente).
create or replace function public.sase_validate_scope()
returns trigger
language plpgsql security definer set search_path = ''
as $$
declare
    v_owner uuid;
begin
    if new.scope_type = 'OWN_RECORD' then
        v_owner := public.membership_owner(new.membership_id);
        if v_owner is null or new.scope_key is distinct from v_owner::text then
            raise exception 'SASE_SCOPE_OWN_RECORD_MISMATCH';
        end if;
    end if;
    return new;
end;
$$;

-- Alta automatica de perfil al registrarse en Supabase Auth. role='familia'
-- por defecto: la promocion a un rol de staff ocurre via institutional_memberships
-- + membership_roles, nunca en el registro mismo.
create or replace function public.handle_new_user()
returns trigger
language plpgsql security definer set search_path = ''
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

-- === Triggers ==================================================================

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

create trigger trg_institutions_updated_at
  before update on public.institutions
  for each row execute function public.sase_set_updated_at();

create trigger trg_memberships_updated_at
  before update on public.institutional_memberships
  for each row execute function public.sase_set_updated_at();

create trigger trg_membership_roles_no_self_grant
  before insert or update or delete on public.membership_roles
  for each row execute function public.sase_forbid_self_grant();

create trigger trg_membership_scopes_no_self_grant
  before insert or delete or update on public.membership_scopes
  for each row execute function public.sase_forbid_self_grant();

create trigger trg_membership_scopes_validate
  before insert or update on public.membership_scopes
  for each row execute function public.sase_validate_scope();

-- === RLS =======================================================================

alter table public.profiles enable row level security;
alter table public.institutions enable row level security;
alter table public.institutional_memberships enable row level security;
alter table public.roles enable row level security;
alter table public.permissions enable row level security;
alter table public.role_permissions enable row level security;
alter table public.membership_roles enable row level security;
alter table public.membership_scopes enable row level security;
alter table public.access_audit enable row level security;

create policy read_own_profile on public.profiles
  for select using (id = auth.uid());

create policy leadership_reads_all_profiles on public.profiles
  for select using (current_app_role() = any (array['direccion','secretaria']::app_role[]));

create policy institutions_select_member on public.institutions
  for select using (current_membership(id) is not null);

create policy memberships_select_own on public.institutional_memberships
  for select using (profile_id = (select auth.uid()));

create policy memberships_select_managed on public.institutional_memberships
  for select using (has_permission(institution_id, 'MANAGE_USERS'));

create policy memberships_insert_managed on public.institutional_memberships
  for insert with check (has_permission(institution_id, 'MANAGE_USERS') and profile_id <> (select auth.uid()));

create policy memberships_update_managed on public.institutional_memberships
  for update using (has_permission(institution_id, 'MANAGE_USERS') and profile_id <> (select auth.uid()))
  with check (has_permission(institution_id, 'MANAGE_USERS') and profile_id <> (select auth.uid()));

create policy roles_select_member on public.roles
  for select using (actor_has_membership());

create policy permissions_select_member on public.permissions
  for select using (actor_has_membership());

create policy role_permissions_select_member on public.role_permissions
  for select using (actor_has_membership());

create policy membership_roles_select_own on public.membership_roles
  for select using (membership_owner(membership_id) = (select auth.uid()));

create policy membership_roles_select_managed on public.membership_roles
  for select using (has_permission(membership_institution(membership_id), 'MANAGE_ROLES'));

create policy membership_roles_insert_managed on public.membership_roles
  for insert with check (
    membership_owner(membership_id) <> (select auth.uid())
    and has_permission(membership_institution(membership_id), 'MANAGE_ROLES')
    and actor_holds_role(membership_institution(membership_id), role_id)
  );

create policy membership_roles_delete_managed on public.membership_roles
  for delete using (
    membership_owner(membership_id) <> (select auth.uid())
    and has_permission(membership_institution(membership_id), 'MANAGE_ROLES')
  );

create policy membership_scopes_select_own on public.membership_scopes
  for select using (membership_owner(membership_id) = (select auth.uid()));

create policy membership_scopes_select_managed on public.membership_scopes
  for select using (has_permission(membership_institution(membership_id), 'MANAGE_ROLES'));

create policy membership_scopes_insert_managed on public.membership_scopes
  for insert with check (
    membership_owner(membership_id) <> (select auth.uid())
    and has_permission(membership_institution(membership_id), 'MANAGE_ROLES')
  );

create policy membership_scopes_delete_managed on public.membership_scopes
  for delete using (
    membership_owner(membership_id) <> (select auth.uid())
    and has_permission(membership_institution(membership_id), 'MANAGE_ROLES')
  );

create policy insert_own_access_events on public.access_audit
  for insert with check (user_id = auth.uid());

create policy leadership_reads_audit on public.access_audit
  for select using (current_app_role() = any (array['direccion','secretaria']::app_role[]));
