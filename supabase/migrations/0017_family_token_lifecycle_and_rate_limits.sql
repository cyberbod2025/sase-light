-- SASE-310 — ciclo de vida del token familiar y antiabuso del portal.
--
-- Esta migracion no reescribe 0015/0016. Envuelve sus RPC ya aplicadas para
-- conservar la arquitectura anon + security definer y anade expiracion,
-- revocacion, rotacion y limites por huella de solicitud.

alter table public.pre_applications
  add column access_token_expires_at timestamptz,
  add column access_token_revoked_at timestamptz,
  add column access_token_rotated_at timestamptz;

update public.pre_applications
set access_token_expires_at = coalesce(created_at, now()) + interval '30 days'
where access_token_expires_at is null;

alter table public.pre_applications
  alter column access_token_expires_at set default (now() + interval '30 days'),
  alter column access_token_expires_at set not null;

create table public.pre_application_rate_limits (
  request_fingerprint text not null,
  operation text not null,
  window_started_at timestamptz not null,
  attempt_count integer not null default 0,
  constraint pre_application_rate_limits_pk primary key (request_fingerprint, operation),
  constraint pre_application_rate_limits_operation_check
    check (operation in ('CREATE', 'GET', 'UPDATE', 'ROTATE')),
  constraint pre_application_rate_limits_attempt_count_check
    check (attempt_count >= 0)
);

revoke all on table public.pre_application_rate_limits from anon, authenticated;

create or replace function public.sase_pre_application_request_fingerprint()
returns text
language sql
stable
security definer
set search_path = ''
as $$
  select md5(
    coalesce(
      nullif(current_setting('request.headers', true), '')::jsonb ->> 'x-forwarded-for',
      nullif(current_setting('request.headers', true), '')::jsonb ->> 'x-real-ip',
      inet_client_addr()::text,
      'unknown'
    )
  );
$$;

create or replace function public.sase_check_pre_application_rate_limit(
  p_operation text,
  p_limit integer,
  p_window interval
)
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_now timestamptz := now();
  v_attempt_count integer;
begin
  insert into public.pre_application_rate_limits (
    request_fingerprint, operation, window_started_at, attempt_count
  ) values (
    public.sase_pre_application_request_fingerprint(), p_operation, v_now, 1
  )
  on conflict (request_fingerprint, operation) do update set
    window_started_at = case
      when public.pre_application_rate_limits.window_started_at <= v_now - p_window
        then v_now
      else public.pre_application_rate_limits.window_started_at
    end,
    attempt_count = case
      when public.pre_application_rate_limits.window_started_at <= v_now - p_window
        then 1
      else public.pre_application_rate_limits.attempt_count + 1
    end
  returning attempt_count into v_attempt_count;

  return v_attempt_count <= p_limit;
end;
$$;

revoke all on function public.sase_pre_application_request_fingerprint() from public, anon, authenticated;
revoke all on function public.sase_check_pre_application_rate_limit(text, integer, interval) from public, anon, authenticated;

-- Las funciones 0015 quedan como implementacion interna. Las nuevas fachadas
-- aplican los controles sin duplicar el agregado padre/hijas ya validado.
alter function public.create_pre_application_with_children(jsonb, jsonb, jsonb, jsonb)
  rename to create_pre_application_with_children_legacy;
alter function public.get_pre_application_with_children(text, uuid)
  rename to get_pre_application_with_children_legacy;
alter function public.update_pre_application_with_children(text, uuid, jsonb, jsonb, jsonb, jsonb)
  rename to update_pre_application_with_children_legacy;

revoke all on function public.create_pre_application_with_children_legacy(jsonb, jsonb, jsonb, jsonb) from public, anon, authenticated;
revoke all on function public.get_pre_application_with_children_legacy(text, uuid) from public, anon, authenticated;
revoke all on function public.update_pre_application_with_children_legacy(text, uuid, jsonb, jsonb, jsonb, jsonb) from public, anon, authenticated;

create or replace function public.create_pre_application_with_children(
  p_record jsonb,
  p_responsables jsonb,
  p_autorizados jsonb,
  p_documentos jsonb
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
begin
  if not public.sase_check_pre_application_rate_limit('CREATE', 5, interval '10 minutes') then
    raise exception 'SASE_PRE_APPLICATION_RATE_LIMITED';
  end if;

  return public.create_pre_application_with_children_legacy(
    p_record, p_responsables, p_autorizados, p_documentos
  );
end;
$$;

create or replace function public.get_pre_application_with_children(
  p_folio text,
  p_access_token uuid
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
begin
  if not public.sase_check_pre_application_rate_limit('GET', 20, interval '10 minutes') then
    raise exception 'SASE_PRE_APPLICATION_RATE_LIMITED';
  end if;

  if not exists (
    select 1
    from public.pre_applications
    where folio = p_folio
      and access_token = p_access_token
      and access_token_revoked_at is null
      and access_token_expires_at > now()
      and status <> 'CANCELADA'
  ) then
    return null;
  end if;

  return public.get_pre_application_with_children_legacy(p_folio, p_access_token);
end;
$$;

create or replace function public.update_pre_application_with_children(
  p_folio text,
  p_access_token uuid,
  p_record jsonb,
  p_responsables jsonb,
  p_autorizados jsonb,
  p_documentos jsonb
)
returns text
language plpgsql
security definer
set search_path = ''
as $$
begin
  if p_access_token is not null then
    if not public.sase_check_pre_application_rate_limit('UPDATE', 10, interval '10 minutes') then
      raise exception 'SASE_PRE_APPLICATION_RATE_LIMITED';
    end if;

    if not exists (
      select 1
      from public.pre_applications
      where folio = p_folio
        and access_token = p_access_token
        and access_token_revoked_at is null
        and access_token_expires_at > now()
        and status = 'PENDIENTE_CORRECCION'
    ) then
      raise exception 'SASE_PRE_APPLICATION_UPDATE_REJECTED';
    end if;
  end if;

  return public.update_pre_application_with_children_legacy(
    p_folio, p_access_token, p_record, p_responsables, p_autorizados, p_documentos
  );
end;
$$;

create or replace function public.rotate_pre_application_access_token(
  p_folio text,
  p_access_token uuid
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_new_token uuid := gen_random_uuid();
  v_expires_at timestamptz := now() + interval '30 days';
begin
  if not public.sase_check_pre_application_rate_limit('ROTATE', 5, interval '10 minutes') then
    raise exception 'SASE_PRE_APPLICATION_RATE_LIMITED';
  end if;

  update public.pre_applications
  set access_token = v_new_token,
      access_token_expires_at = v_expires_at,
      access_token_revoked_at = null,
      access_token_rotated_at = now()
  where folio = p_folio
    and access_token = p_access_token
    and access_token_revoked_at is null
    and access_token_expires_at > now()
    and status <> 'CANCELADA';

  if not found then
    raise exception 'SASE_PRE_APPLICATION_TOKEN_REJECTED';
  end if;

  return jsonb_build_object(
    'folio', p_folio,
    'access_token', v_new_token,
    'access_token_expires_at', v_expires_at
  );
end;
$$;

create or replace function public.revoke_pre_application_access_token(p_folio text)
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
begin
  if auth.uid() is null then
    raise exception 'SASE_PRE_APPLICATION_TOKEN_REVOKE_REJECTED';
  end if;

  update public.pre_applications
  set access_token_revoked_at = now()
  where folio = p_folio
    and public.has_permission(institution_id, 'REVIEW_PRE_APPLICATION');

  if not found then
    raise exception 'SASE_PRE_APPLICATION_TOKEN_REVOKE_REJECTED';
  end if;

  return true;
end;
$$;

create or replace function public.sase_revoke_pre_application_token_on_cancel()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if new.status = 'CANCELADA' then
    new.access_token_revoked_at := coalesce(new.access_token_revoked_at, now());
  end if;
  return new;
end;
$$;

drop trigger if exists trg_pre_applications_revoke_token_on_cancel on public.pre_applications;
create trigger trg_pre_applications_revoke_token_on_cancel
  before update on public.pre_applications
  for each row execute function public.sase_revoke_pre_application_token_on_cancel();

revoke execute on function public.sase_revoke_pre_application_token_on_cancel() from public, anon, authenticated;

revoke execute on function public.create_pre_application_with_children(jsonb, jsonb, jsonb, jsonb) from public, anon, authenticated;
grant execute on function public.create_pre_application_with_children(jsonb, jsonb, jsonb, jsonb) to anon;

revoke execute on function public.get_pre_application_with_children(text, uuid) from public, anon, authenticated;
grant execute on function public.get_pre_application_with_children(text, uuid) to anon;

revoke execute on function public.update_pre_application_with_children(text, uuid, jsonb, jsonb, jsonb, jsonb) from public, anon, authenticated;
grant execute on function public.update_pre_application_with_children(text, uuid, jsonb, jsonb, jsonb, jsonb) to anon, authenticated;

revoke execute on function public.rotate_pre_application_access_token(text, uuid) from public, authenticated;
grant execute on function public.rotate_pre_application_access_token(text, uuid) to anon;

revoke execute on function public.revoke_pre_application_access_token(text) from public, anon;
grant execute on function public.revoke_pre_application_access_token(text) to authenticated;

revoke execute on function public.sase_revoke_pre_application_token_on_cancel() from public, anon, authenticated;
