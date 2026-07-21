-- =============================================================================
-- DESIGN ONLY — DO NOT APPLY AS MIGRATION
-- -----------------------------------------------------------------------------
-- Diseño greenfield de referencia. NUNCA debe ejecutarse contra el proyecto
-- Supabase existente (plyjvvpkaafnkxmmqkbh): crearía un `profiles` distinto al
-- legado y una política de UPDATE que permitiría auto-elevación de rol.
-- El esquema realmente vigente es el de supabase/migrations/ (M1 + M2).
-- =============================================================================
-- SASE FOUNDATION — FASE 1
-- Identidad institucional, membresías, roles múltiples, scopes, permisos y
-- auditoría append-only. Migración LOCAL: no aplicar a remoto en esta fase.
--
-- Principios:
--   * deny-by-default: RLS habilitada en todas las tablas; solo existen las
--     políticas mínimas declaradas aquí.
--   * el cliente jamás define actor, rol, institución ni timestamp de
--     auditoría: se derivan de auth.uid(), la membresía almacenada y now().
--   * sin datos reales: solo seed técnico idempotente de catálogos.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Tablas
-- -----------------------------------------------------------------------------

create table if not exists public.institutions (
    id          uuid primary key default gen_random_uuid(),
    name        text not null unique check (char_length(name) between 1 and 200),
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);

-- Perfil 1:1 con auth.users. No guarda credenciales ni datos sensibles.
create table if not exists public.profiles (
    id           uuid primary key references auth.users (id) on delete cascade,
    display_name text not null check (char_length(display_name) between 1 and 200),
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now()
);

-- Catálogo global de roles (códigos alineados con InstitutionRole en Kotlin).
create table if not exists public.roles (
    id          uuid primary key default gen_random_uuid(),
    code        text not null unique check (code = upper(code) and char_length(code) between 1 and 60),
    description text,
    created_at  timestamptz not null default now()
);

-- Catálogo global de permisos (códigos alineados con InstitutionPermission).
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

-- Membresía: usuario dentro de una institución. Los roles y alcances cuelgan
-- de la membresía, nunca directamente del usuario.
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

-- Alcances por membresía, separados de los roles.
-- scope_key es null para INSTITUTION (el alcance es la institución completa).
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
    -- nulls not distinct: evita filas duplicadas (membership, 'INSTITUTION', null)
    unique nulls not distinct (membership_id, scope_type, scope_key)
);

-- Auditoría append-only. El actor y el timestamp se derivan en el servidor.
create table if not exists public.audit_events (
    id                  uuid primary key default gen_random_uuid(),
    institution_id      uuid not null references public.institutions (id),
    actor_profile_id    uuid not null references public.profiles (id),
    actor_membership_id uuid not null references public.institutional_memberships (id),
    action              text not null check (char_length(action) between 1 and 120),
    entity_type         text,
    entity_id           text,
    detail              jsonb not null default '{}'::jsonb,
    created_at          timestamptz not null default now()
);

-- -----------------------------------------------------------------------------
-- 2. Índices para comprobaciones de permisos y consultas de auditoría
-- -----------------------------------------------------------------------------

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
create index if not exists idx_audit_events_institution_created
    on public.audit_events (institution_id, created_at desc);

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

drop trigger if exists trg_profiles_updated_at on public.profiles;
create trigger trg_profiles_updated_at
    before update on public.profiles
    for each row execute function public.sase_set_updated_at();

drop trigger if exists trg_memberships_updated_at on public.institutional_memberships;
create trigger trg_memberships_updated_at
    before update on public.institutional_memberships
    for each row execute function public.sase_set_updated_at();

-- -----------------------------------------------------------------------------
-- 4. Funciones auxiliares seguras
--    SECURITY DEFINER con search_path fijado; derivan todo de auth.uid() y de
--    la membresía almacenada. Nunca aceptan un actor desde el cliente.
-- -----------------------------------------------------------------------------

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

-- Única vía de escritura de auditoría: deriva actor, membresía y timestamp
-- en el servidor; rechaza sesiones anónimas y membresías inactivas.
create or replace function public.record_audit_event(
    p_institution_id uuid,
    p_action text,
    p_entity_type text default null,
    p_entity_id text default null,
    p_detail jsonb default '{}'::jsonb
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_actor uuid;
    v_membership uuid;
    v_event uuid;
begin
    v_actor := (select auth.uid());
    if v_actor is null then
        raise exception 'SASE_AUDIT_NO_SESSION';
    end if;
    if p_action is null or char_length(trim(p_action)) = 0 then
        raise exception 'SASE_AUDIT_EMPTY_ACTION';
    end if;

    select m.id into v_membership
    from public.institutional_memberships m
    where m.profile_id = v_actor
      and m.institution_id = p_institution_id
      and m.active
    limit 1;

    if v_membership is null then
        raise exception 'SASE_AUDIT_NO_ACTIVE_MEMBERSHIP';
    end if;

    insert into public.audit_events (
        institution_id, actor_profile_id, actor_membership_id,
        action, entity_type, entity_id, detail
    ) values (
        p_institution_id, v_actor, v_membership,
        trim(p_action), p_entity_type, p_entity_id, coalesce(p_detail, '{}'::jsonb)
    )
    returning id into v_event;

    return v_event;
end;
$$;

revoke all on function public.current_membership(uuid) from public, anon;
revoke all on function public.has_permission(uuid, text) from public, anon;
revoke all on function public.has_scope(uuid, text, text) from public, anon;
revoke all on function public.record_audit_event(uuid, text, text, text, jsonb) from public, anon;
grant execute on function public.current_membership(uuid) to authenticated;
grant execute on function public.has_permission(uuid, text) to authenticated;
grant execute on function public.has_scope(uuid, text, text) to authenticated;
grant execute on function public.record_audit_event(uuid, text, text, text, jsonb) to authenticated;

-- -----------------------------------------------------------------------------
-- 5. Auditoría append-only: bloquear UPDATE/DELETE ordinarios por completo
-- -----------------------------------------------------------------------------

create or replace function public.sase_forbid_audit_mutation()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    raise exception 'SASE_AUDIT_APPEND_ONLY';
end;
$$;

drop trigger if exists trg_audit_events_append_only on public.audit_events;
create trigger trg_audit_events_append_only
    before update or delete on public.audit_events
    for each row execute function public.sase_forbid_audit_mutation();

-- Los triggers por fila no disparan en TRUNCATE: cubrirlo por separado.
drop trigger if exists trg_audit_events_no_truncate on public.audit_events;
create trigger trg_audit_events_no_truncate
    before truncate on public.audit_events
    for each statement execute function public.sase_forbid_audit_mutation();

-- Defensa en profundidad: aunque RLS ya niega el INSERT ordinario (no hay
-- política), se revoca también el privilegio; record_audit_event (definer)
-- no se ve afectada. TRUNCATE se revoca en todo el esquema.
revoke insert, update, delete, truncate on public.audit_events from anon, authenticated;
revoke truncate on all tables in schema public from anon, authenticated;

-- -----------------------------------------------------------------------------
-- 6. RLS: habilitar en todo; deny-by-default; políticas mínimas
-- -----------------------------------------------------------------------------

alter table public.institutions enable row level security;
alter table public.profiles enable row level security;
alter table public.roles enable row level security;
alter table public.permissions enable row level security;
alter table public.role_permissions enable row level security;
alter table public.institutional_memberships enable row level security;
alter table public.membership_roles enable row level security;
alter table public.membership_scopes enable row level security;
alter table public.audit_events enable row level security;

-- Instituciones: visibles solo para quien tiene membresía activa en ellas.
drop policy if exists institutions_select_member on public.institutions;
create policy institutions_select_member on public.institutions
    for select to authenticated
    using (public.current_membership(id) is not null);

-- Perfiles: cada quien ve su propia identidad; gestión requiere MANAGE_USERS
-- dentro de una institución compartida con el perfil consultado.
drop policy if exists profiles_select_own on public.profiles;
create policy profiles_select_own on public.profiles
    for select to authenticated
    using (id = (select auth.uid()));

drop policy if exists profiles_select_managed on public.profiles;
create policy profiles_select_managed on public.profiles
    for select to authenticated
    using (
        exists (
            select 1
            from public.institutional_memberships m
            where m.profile_id = public.profiles.id
              and public.has_permission(m.institution_id, 'MANAGE_USERS')
        )
    );

drop policy if exists profiles_update_own_name on public.profiles;
create policy profiles_update_own_name on public.profiles
    for update to authenticated
    using (id = (select auth.uid()))
    with check (id = (select auth.uid()));

-- Catálogos de roles/permisos: lectura para autenticados; sin políticas de
-- escritura (se administran exclusivamente por migraciones).
drop policy if exists roles_select_authenticated on public.roles;
create policy roles_select_authenticated on public.roles
    for select to authenticated using (true);

drop policy if exists permissions_select_authenticated on public.permissions;
create policy permissions_select_authenticated on public.permissions
    for select to authenticated using (true);

drop policy if exists role_permissions_select_authenticated on public.role_permissions;
create policy role_permissions_select_authenticated on public.role_permissions
    for select to authenticated using (true);

-- Membresías: cada quien consulta la propia; gestión requiere MANAGE_USERS.
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
    with check (public.has_permission(institution_id, 'MANAGE_USERS'));

-- Separación de funciones: nadie gestiona su PROPIA membresía, ni siquiera
-- con MANAGE_USERS/MANAGE_ROLES (bloquea la auto-escalada de privilegios).
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

-- Roles de membresía: consulta propia; gestión requiere MANAGE_ROLES en la
-- institución de la membresía afectada.
drop policy if exists membership_roles_select_own on public.membership_roles;
create policy membership_roles_select_own on public.membership_roles
    for select to authenticated
    using (
        exists (
            select 1 from public.institutional_memberships m
            where m.id = membership_id and m.profile_id = (select auth.uid())
        )
    );

drop policy if exists membership_roles_select_managed on public.membership_roles;
create policy membership_roles_select_managed on public.membership_roles
    for select to authenticated
    using (
        exists (
            select 1 from public.institutional_memberships m
            where m.id = membership_id
              and public.has_permission(m.institution_id, 'MANAGE_ROLES')
        )
    );

-- Los grants de roles excluyen la membresía propia: un administrador no puede
-- concederse a sí mismo roles con acceso a datos sensibles (anti-escalada).
drop policy if exists membership_roles_insert_managed on public.membership_roles;
create policy membership_roles_insert_managed on public.membership_roles
    for insert to authenticated
    with check (
        exists (
            select 1 from public.institutional_memberships m
            where m.id = membership_id
              and m.profile_id <> (select auth.uid())
              and public.has_permission(m.institution_id, 'MANAGE_ROLES')
        )
    );

drop policy if exists membership_roles_delete_managed on public.membership_roles;
create policy membership_roles_delete_managed on public.membership_roles
    for delete to authenticated
    using (
        exists (
            select 1 from public.institutional_memberships m
            where m.id = membership_id
              and m.profile_id <> (select auth.uid())
              and public.has_permission(m.institution_id, 'MANAGE_ROLES')
        )
    );

-- Alcances de membresía: mismo patrón que los roles.
drop policy if exists membership_scopes_select_own on public.membership_scopes;
create policy membership_scopes_select_own on public.membership_scopes
    for select to authenticated
    using (
        exists (
            select 1 from public.institutional_memberships m
            where m.id = membership_id and m.profile_id = (select auth.uid())
        )
    );

drop policy if exists membership_scopes_select_managed on public.membership_scopes;
create policy membership_scopes_select_managed on public.membership_scopes
    for select to authenticated
    using (
        exists (
            select 1 from public.institutional_memberships m
            where m.id = membership_id
              and public.has_permission(m.institution_id, 'MANAGE_ROLES')
        )
    );

-- Igual que con los roles: nadie amplía sus PROPIOS alcances (anti-escalada).
drop policy if exists membership_scopes_insert_managed on public.membership_scopes;
create policy membership_scopes_insert_managed on public.membership_scopes
    for insert to authenticated
    with check (
        exists (
            select 1 from public.institutional_memberships m
            where m.id = membership_id
              and m.profile_id <> (select auth.uid())
              and public.has_permission(m.institution_id, 'MANAGE_ROLES')
        )
    );

drop policy if exists membership_scopes_delete_managed on public.membership_scopes;
create policy membership_scopes_delete_managed on public.membership_scopes
    for delete to authenticated
    using (
        exists (
            select 1 from public.institutional_memberships m
            where m.id = membership_id
              and m.profile_id <> (select auth.uid())
              and public.has_permission(m.institution_id, 'MANAGE_ROLES')
        )
    );

-- Auditoría: lectura solo con VIEW_AUDIT; sin política de INSERT ordinario
-- (la única vía es record_audit_event, SECURITY DEFINER); UPDATE/DELETE
-- bloqueados por trigger y revoke.
drop policy if exists audit_events_select_permitted on public.audit_events;
create policy audit_events_select_permitted on public.audit_events
    for select to authenticated
    using (public.has_permission(institution_id, 'VIEW_AUDIT'));

-- -----------------------------------------------------------------------------
-- 7. Seed técnico idempotente (solo catálogos; sin personas, sin instituciones)
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

-- Matriz rol → permisos (espejo de RolePermissionMatrix.DEFAULT en Kotlin).
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

-- FAMILIA queda deliberadamente sin filas en role_permissions: deny-by-default.
