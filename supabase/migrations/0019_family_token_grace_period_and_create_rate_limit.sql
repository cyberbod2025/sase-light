-- SASE-310 — token de gracia tras rotacion + contador de CREATE que
-- sobrevive a un rechazo de la insercion.
--
-- Dos hallazgos reales de revision de codigo (Codex, PR #52) sobre 0017/0018:
--
-- 1. `rotate_pre_application_access_token` invalida el token viejo en el
--    mismo UPDATE que activa el nuevo. Si la respuesta se pierde despues
--    de que el servidor ya confirmo (corte de red, app cerrada a medio
--    request), la familia se queda con un codigo que el servidor ya
--    considera invalido -- bloqueo permanente sin ruta de recuperacion.
--    Correccion: el token inmediatamente anterior a cada rotacion queda
--    valido por una ventana de gracia corta (10 minutos). Un reintento
--    dentro de esa ventana con el codigo viejo sigue funcionando para
--    consultar (GET), actualizar (UPDATE) y volver a rotar (ROTATE) --
--    cada rotacion adicional desplaza la ventana de gracia hacia el
--    token recien superado, asi que la familia siempre puede recuperarse
--    reintentando, sin abrir una vulnerabilidad permanente (la ventana
--    vieja expira sola).
--
-- 2. `create_pre_application_with_children` (0017) tiene el mismo problema
--    que 0018 corrigio en UPDATE/ROTATE, pero via una fuente distinta: el
--    incremento del contador de rate limit pasa primero, y si el INSERT de
--    `create_pre_application_with_children_legacy` falla despues (folio o
--    CURP duplicados, dato invalido), esa excepcion no capturada aborta
--    TODA la transaccion, deshaciendo tambien el incremento. Adivinar/
--    repetir folios duplicados nunca hacia avanzar el contador de CREATE.
--    Correccion: se captura el rechazo de la insercion (solo los errores
--    que un cliente puede disparar con datos invalidos/duplicados, no
--    errores internos inesperados) dentro de un bloque anidado, que en
--    Postgres solo deshace lo hecho DENTRO de ese bloque -- el incremento
--    del contador, hecho antes del bloque, se conserva. Se devuelve null
--    en el rechazo, mismo patron que 0018.
--
-- ESTADO: escrita y aplicada a staging en la misma sesion que la escribe,
-- antes de mergear el PR.

alter table public.pre_applications
  add column previous_access_token uuid,
  add column previous_access_token_valid_until timestamptz;

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
      and (
        access_token = p_access_token
        or (
          previous_access_token = p_access_token
          and previous_access_token_valid_until > now()
        )
      )
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
declare
  v_effective_token uuid;
begin
  if p_access_token is not null then
    if not public.sase_check_pre_application_rate_limit('UPDATE', 10, interval '10 minutes') then
      raise exception 'SASE_PRE_APPLICATION_RATE_LIMITED';
    end if;

    select access_token into v_effective_token
    from public.pre_applications
    where folio = p_folio
      and (
        access_token = p_access_token
        or (
          previous_access_token = p_access_token
          and previous_access_token_valid_until > now()
        )
      )
      and access_token_revoked_at is null
      and access_token_expires_at > now()
      and status = 'PENDIENTE_CORRECCION';

    if v_effective_token is null then
      -- No se lanza excepcion: deshaceria el incremento del contador de
      -- rate limit que la linea de arriba acaba de confirmar (ver 0018).
      return null;
    end if;

    -- La RPC legacy exige que el token recibido coincida exactamente con
    -- el vigente; si la familia entro con el token de gracia, se usa el
    -- vigente real para esa llamada -- ya quedo autorizada arriba.
    return public.update_pre_application_with_children_legacy(
      p_folio, v_effective_token, p_record, p_responsables, p_autorizados, p_documentos
    );
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
  v_grace_until timestamptz := now() + interval '10 minutes';
begin
  if not public.sase_check_pre_application_rate_limit('ROTATE', 5, interval '10 minutes') then
    raise exception 'SASE_PRE_APPLICATION_RATE_LIMITED';
  end if;

  update public.pre_applications
  set previous_access_token = access_token,
      previous_access_token_valid_until = v_grace_until,
      access_token = v_new_token,
      access_token_expires_at = v_expires_at,
      access_token_revoked_at = null,
      access_token_rotated_at = now()
  where folio = p_folio
    and (
      access_token = p_access_token
      or (
        previous_access_token = p_access_token
        and previous_access_token_valid_until > now()
      )
    )
    and access_token_revoked_at is null
    and access_token_expires_at > now()
    and status <> 'CANCELADA';

  if not found then
    -- No se lanza excepcion: mismo motivo que en update_pre_application_with_children.
    return null;
  end if;

  return jsonb_build_object(
    'folio', p_folio,
    'access_token', v_new_token,
    'access_token_expires_at', v_expires_at
  );
end;
$$;

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

  begin
    return public.create_pre_application_with_children_legacy(
      p_record, p_responsables, p_autorizados, p_documentos
    );
  exception
    when unique_violation or not_null_violation or check_violation
      or foreign_key_violation or invalid_text_representation then
      -- Rechazo disparable por el cliente (folio/CURP duplicados, dato
      -- invalido) -- se captura en este bloque anidado para que solo se
      -- deshaga lo hecho DENTRO de el; el incremento del contador de
      -- rate limit, hecho antes de este bloque, se conserva. Un error
      -- realmente inesperado (no listado arriba) sigue propagandose sin
      -- capturar, visible como falla real en vez de silenciarse.
      return null;
  end;
end;
$$;

-- has_permission solo evalua institution_id + REVIEW_PRE_APPLICATION; el
-- token de gracia es un mecanismo puramente familiar (anon), asi que la
-- revocacion por Secretaria y la revocacion automatica al cancelar deben
-- cerrar tambien esa puerta, no solo el token vigente.
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
  set access_token_revoked_at = now(),
      previous_access_token = null,
      previous_access_token_valid_until = null
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
    new.previous_access_token := null;
    new.previous_access_token_valid_until := null;
  end if;
  return new;
end;
$$;

revoke execute on function public.get_pre_application_with_children(text, uuid) from public, anon, authenticated;
grant execute on function public.get_pre_application_with_children(text, uuid) to anon;

revoke execute on function public.update_pre_application_with_children(text, uuid, jsonb, jsonb, jsonb, jsonb) from public, anon, authenticated;
grant execute on function public.update_pre_application_with_children(text, uuid, jsonb, jsonb, jsonb, jsonb) to anon, authenticated;

revoke execute on function public.rotate_pre_application_access_token(text, uuid) from public, authenticated;
grant execute on function public.rotate_pre_application_access_token(text, uuid) to anon;

revoke execute on function public.create_pre_application_with_children(jsonb, jsonb, jsonb, jsonb) from public, anon, authenticated;
grant execute on function public.create_pre_application_with_children(jsonb, jsonb, jsonb, jsonb) to anon;

revoke execute on function public.revoke_pre_application_access_token(text) from public, anon;
grant execute on function public.revoke_pre_application_access_token(text) to authenticated;

revoke execute on function public.sase_revoke_pre_application_token_on_cancel() from public, anon, authenticated;
