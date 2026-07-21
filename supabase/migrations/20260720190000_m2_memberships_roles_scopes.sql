-- =============================================================================
-- SASE-310 — M2: membresías, roles y alcances sobre el esquema legado
-- =============================================================================
-- Reconcilia el modelo canónico de autorización (Fase 1) con la base ya
-- aplicada en el proyecto Supabase existente, que trae de m1_identity_access:
--
--   public.profiles      (id, full_name, role app_role, active, created_at)
--   public.access_audit  (bitácora de login/logout/denied)
--   enum  public.app_role (familia, secretaria, direccion, medico,
--                          trabajo_social, udeei, docente)
--
-- Principios:
--   * No se borra ni transforma destructivamente nada existente.
--   * `profiles.role` se CONSERVA como asignación inicial y compatibilidad
--     temporal; deja de ser la fuente de verdad de autorización, que pasa a
--     ser membresía + roles + alcances.
--   * No se crean `staff_profiles` ni `staff_role`.
--   * `access_audit` se mantiene tal cual; esta migración no duplica auditoría.
--   * Ejecución idempotente: repetirla no produce duplicados ni errores.
--   * Deny-by-default: sin política explícita y sin privilegio, no hay acceso.
--
-- Nombres y columnas alineados con SupabaseStaffGateways.kt.
--
-- REQUISITO DE APLICACIÓN: esta migración asume que el histórico local está
-- reconciliado con el remoto. `20260719001000_institutional_auth_foundation.sql`
-- es un diseño greenfield NO aplicado a este proyecto y NO debe ejecutarse
-- sobre él (crearía un `profiles` distinto y una política de UPDATE que
-- permitiría auto-elevación de `profiles.role`). Ver informe de Fase 2B.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 0. Precondiciones — fallo explícito y seguro si el legado no es el esperado
-- -----------------------------------------------------------------------------
do $$
begin
    if to_regclass('public.profiles') is null then
        raise exception 'SASE_M2_MISSING_PROFILES: aplicar primero m1_identity_access';
    end if;

    if to_regtype('public.app_role') is null then
        raise exception 'SASE_M2_MISSING_APP_ROLE: enum public.app_role inexistente';
    end if;

    if not exists (
        select 1 from information_schema.columns
        where table_schema = 'public' and table_name = 'profiles'
          and column_name in ('full_name', 'role')
        group by table_name having count(*) = 2
    ) then
        raise exception 'SASE_M2_UNEXPECTED_PROFILES: se esperaban las columnas full_name y role';
    end if;

    -- El legado debe llegar con RLS activo; si no, esta migración no debe
    -- "arreglarlo" en silencio.
    if not exists (
        select 1 from pg_class c
        join pg_namespace n on n.oid = c.relnamespace
        where n.nspname = 'public' and c.relname = 'profiles' and c.relrowsecurity
    ) then
        raise exception 'SASE_M2_PROFILES_RLS_DISABLED';
    end if;

    -- Si ya existiera display_name pero NO como columna generada, abortar:
    -- copiarla a mano crearía una segunda fuente de verdad del nombre.
    if exists (
        select 1 from information_schema.columns
        where table_schema = 'public' and table_name = 'profiles'
          and column_name = 'display_name' and is_generated <> 'ALWAYS'
    ) then
        raise exception 'SASE_M2_DISPLAY_NAME_NOT_GENERATED';
    end if;
end;
$$;

-- -----------------------------------------------------------------------------
-- 1. Compatibilidad de `profiles` con el gateway (display_name)
-- -----------------------------------------------------------------------------
-- El gateway lee `display_name`; el legado guarda `full_name`. En lugar de
-- copiar el valor (que crearía dos fuentes de verdad divergentes), se expone
-- una columna GENERADA: siempre refleja `full_name`, no admite escritura y no
-- puede desincronizarse. El gateway solo lee, nunca escribe este campo.
alter table public.profiles
    add column if not exists display_name text
    generated always as (full_name) stored;

comment on column public.profiles.role is
    'LEGADO/COMPAT: asignación inicial usada por el backfill de M2. La '
    'autorización efectiva se resuelve por institutional_memberships + '
    'membership_roles + membership_scopes. No leer para decidir permisos.';

-- -----------------------------------------------------------------------------
-- 2. Modelo canónico
-- -----------------------------------------------------------------------------
create table if not exists public.institutions (
    id          uuid primary key default gen_random_uuid(),
    name        text not null unique check (char_length(name) between 1 and 200),
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);

create table if not exists public.roles (
    id          uuid primary key default gen_random_uuid(),
    code        text not null unique check (code = upper(code) and char_length(code) between 1 and 60),
    description text,
    created_at  timestamptz not null default now()
);

create table if not exists public.permissions (
    id          uuid primary key default gen_random_uuid(),
    code        text not null unique check (code = upper(code) and char_length(code) between 1 and 60),
    description text,
    created_at  timestamptz not null default now()
);

create table if not exists public.role_permissions (
    role_id       uuid not null references public.roles (id) on delete cascade,
    permission_id uuid not null references public.permissions (id) on delete cascade,
    created_at    timestamptz not null default now(),
    primary key (role_id, permission_id)
);

-- Una persona dentro de una institución. Roles y alcances cuelgan de aquí,
-- nunca del usuario directamente.
create table if not exists public.institutional_memberships (
    id             uuid primary key default gen_random_uuid(),
    profile_id     uuid not null references public.profiles (id) on delete cascade,
    institution_id uuid not null references public.institutions (id) on delete cascade,
    active         boolean not null default true,
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now(),
    unique (profile_id, institution_id)
);

-- Varios roles por membresía.
create table if not exists public.membership_roles (
    membership_id uuid not null references public.institutional_memberships (id) on delete cascade,
    role_id       uuid not null references public.roles (id) on delete cascade,
    created_at    timestamptz not null default now(),
    primary key (membership_id, role_id)
);

-- Alcances por membresía, independientes de los roles.
create table if not exists public.membership_scopes (
    id            uuid primary key default gen_random_uuid(),
    membership_id uuid not null references public.institutional_memberships (id) on delete cascade,
    scope_type    text not null check (scope_type in ('INSTITUTION', 'SCHOOL_CYCLE', 'GROUP', 'STUDENT', 'OWN_RECORD')),
    scope_key     text,
    created_at    timestamptz not null default now(),
    constraint membership_scopes_key_shape check (
        (scope_type = 'INSTITUTION' and scope_key is null)
        or (scope_type <> 'INSTITUTION' and scope_key is not null)
    ),
    -- Convención del cliente: GROUP usa "cicloId/grupoId".
    constraint membership_scopes_group_key_shape check (
        scope_type <> 'GROUP' or scope_key ~ '^[^/]+/[^/]+$'
    ),
    unique nulls not distinct (membership_id, scope_type, scope_key)
);

create index if not exists idx_memberships_profile
    on public.institutional_memberships (profile_id) where active;
create index if not exists idx_memberships_institution
    on public.institutional_memberships (institution_id) where active;
create index if not exists idx_membership_roles_membership
    on public.membership_roles (membership_id);
create index if not exists idx_membership_scopes_membership
    on public.membership_scopes (membership_id);
create index if not exists idx_role_permissions_role
    on public.role_permissions (role_id);

-- -----------------------------------------------------------------------------
-- 3. updated_at automático
-- -----------------------------------------------------------------------------
create or replace function public.sase_set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    new.updated_at := now();
    return new;
end;
$$;

drop trigger if exists trg_institutions_updated_at on public.institutions;
create trigger trg_institutions_updated_at
    before update on public.institutions
    for each row execute function public.sase_set_updated_at();

drop trigger if exists trg_memberships_updated_at on public.institutional_memberships;
create trigger trg_memberships_updated_at
    before update on public.institutional_memberships
    for each row execute function public.sase_set_updated_at();

-- -----------------------------------------------------------------------------
-- 4. Funciones de autorización (servidor, nunca el cliente)
-- -----------------------------------------------------------------------------
-- Todas SECURITY DEFINER con search_path vacío: derivan el actor de auth.uid()
-- y leen el directorio almacenado saltando RLS, lo que evita recursión de
-- políticas y que un permiso dependa de poder leer la tabla que lo concede.

create or replace function public.membership_institution(p_membership_id uuid)
returns uuid
language sql
stable
security definer
set search_path = ''
as $$
    select m.institution_id from public.institutional_memberships m where m.id = p_membership_id;
$$;

create or replace function public.membership_owner(p_membership_id uuid)
returns uuid
language sql
stable
security definer
set search_path = ''
as $$
    select m.profile_id from public.institutional_memberships m where m.id = p_membership_id;
$$;

create or replace function public.current_membership(p_institution_id uuid)
returns uuid
language sql
stable
security definer
set search_path = ''
as $$
    select m.id
    from public.institutional_memberships m
    where m.profile_id = (select auth.uid())
      and m.institution_id = p_institution_id
      and m.active
    limit 1;
$$;

create or replace function public.has_permission(p_institution_id uuid, p_permission_code text)
returns boolean
language sql
stable
security definer
set search_path = ''
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

create or replace function public.has_scope(
    p_institution_id uuid,
    p_scope_type text,
    p_scope_key text
)
returns boolean
language sql
stable
security definer
set search_path = ''
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

-- Nadie concede un rol que no posee: cierra la vía de la "cuenta títere"
-- (crear a otro y darle un rol superior al propio).
create or replace function public.actor_holds_role(p_institution_id uuid, p_role_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1
        from public.institutional_memberships m
        join public.membership_roles mr on mr.membership_id = m.id
        where m.profile_id = (select auth.uid())
          and m.institution_id = p_institution_id
          and m.active
          and mr.role_id = p_role_id
    );
$$;

-- Los catálogos solo son visibles para quien pertenece a alguna institución.
create or replace function public.actor_has_membership()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1 from public.institutional_memberships m
        where m.profile_id = (select auth.uid()) and m.active
    );
$$;

revoke all on function public.membership_institution(uuid) from public, anon;
revoke all on function public.membership_owner(uuid) from public, anon;
revoke all on function public.current_membership(uuid) from public, anon;
revoke all on function public.has_permission(uuid, text) from public, anon;
revoke all on function public.has_scope(uuid, text, text) from public, anon;
revoke all on function public.actor_holds_role(uuid, uuid) from public, anon;
revoke all on function public.actor_has_membership() from public, anon;
revoke all on function public.sase_set_updated_at() from public, anon;

grant execute on function public.membership_institution(uuid) to authenticated;
grant execute on function public.membership_owner(uuid) to authenticated;
grant execute on function public.current_membership(uuid) to authenticated;
grant execute on function public.has_permission(uuid, text) to authenticated;
grant execute on function public.has_scope(uuid, text, text) to authenticated;
grant execute on function public.actor_holds_role(uuid, uuid) to authenticated;
grant execute on function public.actor_has_membership() to authenticated;

-- -----------------------------------------------------------------------------
-- 5. Antiautoescalada y validación de alcances — barrera independiente de RLS
-- -----------------------------------------------------------------------------
-- Nadie modifica sus propios roles ni alcances, tenga el permiso que tenga.
-- SECURITY DEFINER para no depender de que el actor pueda leer la membresía;
-- si el dueño no se puede determinar, falla CERRADO.
create or replace function public.sase_forbid_self_grant()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_actor uuid;
    v_owner uuid;
begin
    v_actor := (select auth.uid());

    -- Contexto administrativo (migración por CLI/service): no hay sesión de
    -- usuario, luego no hay autoconcesión posible.
    if v_actor is null or current_user in ('postgres', 'supabase_admin', 'supabase_migration_user') then
        if tg_op = 'DELETE' then return old; else return new; end if;
    end if;

    -- Fila destino (INSERT/UPDATE) y fila origen (UPDATE/DELETE) por separado:
    -- OLD no está asignado en INSERT y NEW no lo está en DELETE.
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

revoke all on function public.sase_forbid_self_grant() from public, anon;

drop trigger if exists trg_membership_roles_no_self_grant on public.membership_roles;
create trigger trg_membership_roles_no_self_grant
    before insert or update or delete on public.membership_roles
    for each row execute function public.sase_forbid_self_grant();

drop trigger if exists trg_membership_scopes_no_self_grant on public.membership_scopes;
create trigger trg_membership_scopes_no_self_grant
    before insert or update or delete on public.membership_scopes
    for each row execute function public.sase_forbid_self_grant();

-- OWN_RECORD debe apuntar al expediente de la propia persona de la membresía;
-- una clave arbitraria daría acceso al expediente de otra.
create or replace function public.sase_validate_scope()
returns trigger
language plpgsql
security definer
set search_path = ''
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

revoke all on function public.sase_validate_scope() from public, anon;

drop trigger if exists trg_membership_scopes_validate on public.membership_scopes;
create trigger trg_membership_scopes_validate
    before insert or update on public.membership_scopes
    for each row execute function public.sase_validate_scope();

-- -----------------------------------------------------------------------------
-- 6. Privilegios de tabla — RLS no cubre TRUNCATE ni privilegios de esquema
-- -----------------------------------------------------------------------------
revoke all on public.institutions              from anon, authenticated;
revoke all on public.roles                     from anon, authenticated;
revoke all on public.permissions               from anon, authenticated;
revoke all on public.role_permissions          from anon, authenticated;
revoke all on public.institutional_memberships from anon, authenticated;
revoke all on public.membership_roles          from anon, authenticated;
revoke all on public.membership_scopes         from anon, authenticated;

-- Solo lo que alguna política puede llegar a autorizar.
grant select on public.institutions              to authenticated;
grant select on public.roles                     to authenticated;
grant select on public.permissions               to authenticated;
grant select on public.role_permissions          to authenticated;
grant select, insert, update on public.institutional_memberships to authenticated;
grant select, insert, delete on public.membership_roles          to authenticated;
grant select, insert, delete on public.membership_scopes         to authenticated;

-- -----------------------------------------------------------------------------
-- 7. RLS deny-by-default
-- -----------------------------------------------------------------------------
alter table public.institutions              enable row level security;
alter table public.roles                     enable row level security;
alter table public.permissions               enable row level security;
alter table public.role_permissions          enable row level security;
alter table public.institutional_memberships enable row level security;
alter table public.membership_roles          enable row level security;
alter table public.membership_scopes         enable row level security;

-- Catálogos: legibles solo por quien tiene membresía activa. Sin política de
-- escritura: solo migraciones/service_role los modifican.
drop policy if exists roles_select_member on public.roles;
create policy roles_select_member on public.roles
    for select to authenticated using (public.actor_has_membership());

drop policy if exists permissions_select_member on public.permissions;
create policy permissions_select_member on public.permissions
    for select to authenticated using (public.actor_has_membership());

drop policy if exists role_permissions_select_member on public.role_permissions;
create policy role_permissions_select_member on public.role_permissions
    for select to authenticated using (public.actor_has_membership());

-- Restos de la primera redacción de esta migración, si llegó a aplicarse.
drop policy if exists roles_select_authenticated on public.roles;
drop policy if exists permissions_select_authenticated on public.permissions;
drop policy if exists role_permissions_select_authenticated on public.role_permissions;

-- Instituciones: solo aquellas donde se tiene membresía activa.
drop policy if exists institutions_select_member on public.institutions;
create policy institutions_select_member on public.institutions
    for select to authenticated
    using (public.current_membership(institutions.id) is not null);

-- Membresías: la propia siempre; las ajenas solo con MANAGE_USERS en la MISMA
-- institución (aislamiento entre instituciones).
drop policy if exists memberships_select_own on public.institutional_memberships;
create policy memberships_select_own on public.institutional_memberships
    for select to authenticated
    using (profile_id = (select auth.uid()));

drop policy if exists memberships_select_managed on public.institutional_memberships;
create policy memberships_select_managed on public.institutional_memberships
    for select to authenticated
    using (public.has_permission(institution_id, 'MANAGE_USERS'));

drop policy if exists memberships_insert_managed on public.institutional_memberships;
create policy memberships_insert_managed on public.institutional_memberships
    for insert to authenticated
    with check (
        public.has_permission(institution_id, 'MANAGE_USERS')
        and profile_id <> (select auth.uid())
    );

drop policy if exists memberships_update_managed on public.institutional_memberships;
create policy memberships_update_managed on public.institutional_memberships
    for update to authenticated
    using (
        public.has_permission(institution_id, 'MANAGE_USERS')
        and profile_id <> (select auth.uid())
    )
    with check (
        public.has_permission(institution_id, 'MANAGE_USERS')
        and profile_id <> (select auth.uid())
    );

-- Roles de membresía. Las políticas usan funciones definer para no exigir que
-- el actor pueda además leer la membresía ajena (MANAGE_ROLES es suficiente).
drop policy if exists membership_roles_select_own on public.membership_roles;
create policy membership_roles_select_own on public.membership_roles
    for select to authenticated
    using (public.membership_owner(membership_id) = (select auth.uid()));

drop policy if exists membership_roles_select_managed on public.membership_roles;
create policy membership_roles_select_managed on public.membership_roles
    for select to authenticated
    using (public.has_permission(public.membership_institution(membership_id), 'MANAGE_ROLES'));

drop policy if exists membership_roles_insert_managed on public.membership_roles;
create policy membership_roles_insert_managed on public.membership_roles
    for insert to authenticated
    with check (
        public.membership_owner(membership_id) <> (select auth.uid())
        and public.has_permission(public.membership_institution(membership_id), 'MANAGE_ROLES')
        -- Nadie concede un rol que no posee.
        and public.actor_holds_role(public.membership_institution(membership_id), role_id)
    );

drop policy if exists membership_roles_delete_managed on public.membership_roles;
create policy membership_roles_delete_managed on public.membership_roles
    for delete to authenticated
    using (
        public.membership_owner(membership_id) <> (select auth.uid())
        and public.has_permission(public.membership_institution(membership_id), 'MANAGE_ROLES')
    );

-- Alcances de membresía: mismas reglas.
drop policy if exists membership_scopes_select_own on public.membership_scopes;
create policy membership_scopes_select_own on public.membership_scopes
    for select to authenticated
    using (public.membership_owner(membership_id) = (select auth.uid()));

drop policy if exists membership_scopes_select_managed on public.membership_scopes;
create policy membership_scopes_select_managed on public.membership_scopes
    for select to authenticated
    using (public.has_permission(public.membership_institution(membership_id), 'MANAGE_ROLES'));

drop policy if exists membership_scopes_insert_managed on public.membership_scopes;
create policy membership_scopes_insert_managed on public.membership_scopes
    for insert to authenticated
    with check (
        public.membership_owner(membership_id) <> (select auth.uid())
        and public.has_permission(public.membership_institution(membership_id), 'MANAGE_ROLES')
    );

drop policy if exists membership_scopes_delete_managed on public.membership_scopes;
create policy membership_scopes_delete_managed on public.membership_scopes
    for delete to authenticated
    using (
        public.membership_owner(membership_id) <> (select auth.uid())
        and public.has_permission(public.membership_institution(membership_id), 'MANAGE_ROLES')
    );

-- -----------------------------------------------------------------------------
-- 8. Catálogos (idempotentes, espejo de la matriz Kotlin)
-- -----------------------------------------------------------------------------
insert into public.roles (code, description) values
    ('ADMIN_INSTITUCIONAL', 'Administración de cuentas, roles y configuración'),
    ('DIRECCION', 'Dirección escolar'),
    ('SECRETARIA', 'Secretaría escolar'),
    ('TRABAJO_SOCIAL', 'Trabajo Social'),
    ('MEDICO_ESCOLAR', 'Médico Escolar'),
    ('UDEII', 'Unidad de Educación Inclusiva'),
    ('DOCENTE', 'Docente frente a grupo'),
    ('TUTOR', 'Tutoría de grupo'),
    ('PREFECTURA', 'Prefectura'),
    ('FAMILIA', 'Familia (sin permisos institucionales en esta fase)')
on conflict (code) do nothing;

insert into public.permissions (code, description) values
    ('VIEW_STUDENT_BASE', 'Consultar identidad institucional mínima'),
    ('VIEW_STUDENT_SENSITIVE_IDENTITY', 'Consultar identidad sensible'),
    ('EDIT_STUDENT_IDENTITY', 'Editar identidad del alumno'),
    ('REVIEW_PRE_APPLICATION', 'Revisar pre-solicitudes'),
    ('CONVERT_PRE_APPLICATION', 'Convertir pre-solicitud en alta oficial'),
    ('VIEW_ENROLLMENT', 'Consultar inscripciones'),
    ('MANAGE_ENROLLMENT', 'Gestionar inscripciones'),
    ('VIEW_MEDICAL', 'Consultar información médica'),
    ('EDIT_MEDICAL', 'Editar información médica'),
    ('VIEW_BAP', 'Consultar BAP/UDEII'),
    ('EDIT_BAP', 'Editar BAP/UDEII'),
    ('VIEW_SOCIAL_CONTEXT', 'Consultar contexto socioeconómico'),
    ('EDIT_SOCIAL_CONTEXT', 'Editar contexto socioeconómico'),
    ('VIEW_INCIDENTS', 'Consultar incidencias'),
    ('EDIT_INCIDENTS', 'Registrar/editar incidencias'),
    ('VIEW_AUDIT', 'Consultar auditoría institucional'),
    ('MANAGE_USERS', 'Administrar usuarios y membresías'),
    ('MANAGE_ROLES', 'Administrar roles y alcances')
on conflict (code) do nothing;

with matrix (role_code, permission_code) as (
    values
        ('ADMIN_INSTITUCIONAL', 'MANAGE_USERS'),
        ('ADMIN_INSTITUCIONAL', 'MANAGE_ROLES'),
        ('ADMIN_INSTITUCIONAL', 'VIEW_AUDIT'),
        ('DIRECCION', 'VIEW_STUDENT_BASE'),
        ('DIRECCION', 'VIEW_STUDENT_SENSITIVE_IDENTITY'),
        ('DIRECCION', 'VIEW_ENROLLMENT'),
        ('DIRECCION', 'VIEW_INCIDENTS'),
        ('DIRECCION', 'VIEW_AUDIT'),
        ('SECRETARIA', 'VIEW_STUDENT_BASE'),
        ('SECRETARIA', 'VIEW_STUDENT_SENSITIVE_IDENTITY'),
        ('SECRETARIA', 'EDIT_STUDENT_IDENTITY'),
        ('SECRETARIA', 'REVIEW_PRE_APPLICATION'),
        ('SECRETARIA', 'CONVERT_PRE_APPLICATION'),
        ('SECRETARIA', 'VIEW_ENROLLMENT'),
        ('SECRETARIA', 'MANAGE_ENROLLMENT'),
        ('TRABAJO_SOCIAL', 'VIEW_STUDENT_BASE'),
        ('TRABAJO_SOCIAL', 'VIEW_SOCIAL_CONTEXT'),
        ('TRABAJO_SOCIAL', 'EDIT_SOCIAL_CONTEXT'),
        ('TRABAJO_SOCIAL', 'VIEW_INCIDENTS'),
        ('MEDICO_ESCOLAR', 'VIEW_STUDENT_BASE'),
        ('MEDICO_ESCOLAR', 'VIEW_MEDICAL'),
        ('MEDICO_ESCOLAR', 'EDIT_MEDICAL'),
        ('UDEII', 'VIEW_STUDENT_BASE'),
        ('UDEII', 'VIEW_BAP'),
        ('UDEII', 'EDIT_BAP'),
        ('DOCENTE', 'VIEW_STUDENT_BASE'),
        ('DOCENTE', 'VIEW_INCIDENTS'),
        ('TUTOR', 'VIEW_STUDENT_BASE'),
        ('TUTOR', 'VIEW_INCIDENTS'),
        ('TUTOR', 'EDIT_INCIDENTS'),
        ('PREFECTURA', 'VIEW_STUDENT_BASE'),
        ('PREFECTURA', 'VIEW_INCIDENTS'),
        ('PREFECTURA', 'EDIT_INCIDENTS')
)
insert into public.role_permissions (role_id, permission_id)
select r.id, p.id
from matrix
join public.roles r on r.code = matrix.role_code
join public.permissions p on p.code = matrix.permission_code
on conflict (role_id, permission_id) do nothing;

-- FAMILIA queda deliberadamente sin filas: deny-by-default.

-- -----------------------------------------------------------------------------
-- 9. Backfill idempotente desde profiles.role
-- -----------------------------------------------------------------------------
-- Institución única de esta fase, con id determinista para que el backfill sea
-- reproducible entre entornos.
insert into public.institutions (id, name)
values ('00000000-0000-0000-0000-000000000310', 'Secundaria 310')
on conflict (id) do nothing;

-- Traducción legado -> catálogo canónico. Cubre los SIETE valores de app_role.
create or replace function public.sase_app_role_to_code(p_role public.app_role)
returns text
language sql
immutable
set search_path = ''
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

revoke all on function public.sase_app_role_to_code(public.app_role) from public, anon;

do $$
declare
    v_institution constant uuid := '00000000-0000-0000-0000-000000000310';
    v_unmapped int;
begin
    -- Un valor de app_role sin traducción debe detener la migración, no
    -- degradar silenciosamente los permisos de alguien.
    select count(*) into v_unmapped
    from public.profiles p
    where public.sase_app_role_to_code(p.role) is null;

    if v_unmapped > 0 then
        raise exception 'SASE_M2_UNMAPPED_ROLE: % perfiles con app_role sin equivalente', v_unmapped;
    end if;

    -- 9.1 Membresía por perfil de PERSONAL. `familia` se excluye a propósito:
    -- no tiene permisos institucionales y su acceso llega en una fase futura;
    -- sin membresía, el login del gateway la deniega (MEMBERSHIP_NOT_FOUND).
    insert into public.institutional_memberships (profile_id, institution_id, active)
    select p.id, v_institution, p.active
    from public.profiles p
    where p.role <> 'familia'
    on conflict (profile_id, institution_id) do nothing;

    -- 9.2 Rol inicial derivado de profiles.role (nunca del cliente).
    insert into public.membership_roles (membership_id, role_id)
    select m.id, r.id
    from public.institutional_memberships m
    join public.profiles p on p.id = m.profile_id
    join public.roles r on r.code = public.sase_app_role_to_code(p.role)
    where m.institution_id = v_institution
      and p.role <> 'familia'
    on conflict (membership_id, role_id) do nothing;

    -- 9.3 Alcance institucional inicial. Alcances más finos (ciclo, grupo,
    -- alumno) los asigna después Dirección; no se inventan aquí.
    insert into public.membership_scopes (membership_id, scope_type, scope_key)
    select m.id, 'INSTITUTION', null
    from public.institutional_memberships m
    where m.institution_id = v_institution
      and not exists (
          select 1 from public.membership_scopes s
          where s.membership_id = m.id
            and s.scope_type = 'INSTITUTION'
            and s.scope_key is null
      );
end;
$$;

-- =============================================================================
-- Notas de operación
-- -----------------------------------------------------------------------------
-- * El backfill es de ARRANQUE, no de sincronización continua: reejecutarlo no
--   revoca roles ni actualiza `active` de membresías ya creadas. A partir de
--   aquí, los cambios de rol se hacen sobre membership_roles, no sobre
--   profiles.role (que queda como registro histórico de la asignación inicial).
-- * Pendiente deliberado (M3): auditoría institucional rica (audit_events +
--   record_audit_event). `access_audit` sigue siendo la bitácora de acceso; no
--   se duplica aquí para no mantener dos bitácoras sin necesidad demostrada.
-- =============================================================================
