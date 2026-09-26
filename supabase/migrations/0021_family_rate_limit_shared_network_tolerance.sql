-- SASE-310 — limites de rate limiting mas tolerantes a redes compartidas.
--
-- Hallazgo real de revision de codigo (Codex, PR #52, P2): la huella de
-- solicitud (`sase_pre_application_request_fingerprint`, 0017) es solo la
-- IP del cliente. Toda familia detras del mismo WiFi de la escuela, del
-- mismo enrutador de una oficina o del mismo NAT de un operador comparte
-- un unico contador por operacion. Con CREATE en 5 intentos/10 min, una
-- sesion de inscripcion normal -- varias familias reales enviando su
-- pre-solicitud desde la misma red durante una jornada de inscripcion --
-- podia bloquear a familias legitimas que nunca excedieron nada
-- individualmente.
--
-- Corregir esto del todo requeriria una dimension adicional por
-- cliente/dispositivo (identificador persistente generado por el cliente,
-- enviado en cada llamada) -- cambio de mayor alcance en cliente y
-- servidor, desproporcionado para P2 en la escala de este piloto (una
-- sola escuela). Se opta por la alternativa que el propio hallazgo senala
-- como valida: una politica de limites sustancialmente mas tolerante a
-- redes compartidas, mateniendo un techo significativo contra abuso
-- automatizado real (un script todavia no puede intentar sin limite).
--
--   Operacion | limite anterior | limite nuevo
--   CREATE    | 5 / 10 min      | 20 / 10 min
--   GET       | 20 / 10 min     | 40 / 10 min
--   UPDATE    | 10 / 10 min     | 20 / 10 min
--   ROTATE    | 5 / 10 min      | 20 / 10 min
--
-- (ROTATE sube a la par de GET porque cada consulta exitosa dispara
-- exactamente un ROTATE -- ver 0017 -- asi que necesita el mismo margen
-- para no convertirse en el cuello de botella real.)
--
-- ESTADO: escrita y aplicada a staging en la misma sesion que la escribe,
-- antes de mergear el PR.

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
  if not public.sase_check_pre_application_rate_limit('CREATE', 20, interval '10 minutes') then
    raise exception 'SASE_PRE_APPLICATION_RATE_LIMITED';
  end if;

  begin
    return public.create_pre_application_with_children_legacy(
      p_record, p_responsables, p_autorizados, p_documentos
    );
  exception
    when not_null_violation or check_violation
      or foreign_key_violation or invalid_text_representation then
      return null;
  end;
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
declare
  v_effective_token uuid;
begin
  if not public.sase_check_pre_application_rate_limit('GET', 40, interval '10 minutes') then
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
    and status <> 'CANCELADA';

  if v_effective_token is null then
    return null;
  end if;

  return public.get_pre_application_with_children_legacy(p_folio, v_effective_token);
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
    if not public.sase_check_pre_application_rate_limit('UPDATE', 20, interval '10 minutes') then
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
      return null;
    end if;

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
  if not public.sase_check_pre_application_rate_limit('ROTATE', 20, interval '10 minutes') then
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
    return null;
  end if;

  return jsonb_build_object(
    'folio', p_folio,
    'access_token', v_new_token,
    'access_token_expires_at', v_expires_at
  );
end;
$$;

revoke execute on function public.create_pre_application_with_children(jsonb, jsonb, jsonb, jsonb) from public, anon, authenticated;
grant execute on function public.create_pre_application_with_children(jsonb, jsonb, jsonb, jsonb) to anon;

revoke execute on function public.get_pre_application_with_children(text, uuid) from public, anon, authenticated;
grant execute on function public.get_pre_application_with_children(text, uuid) to anon;

revoke execute on function public.update_pre_application_with_children(text, uuid, jsonb, jsonb, jsonb, jsonb) from public, anon, authenticated;
grant execute on function public.update_pre_application_with_children(text, uuid, jsonb, jsonb, jsonb, jsonb) to anon, authenticated;

revoke execute on function public.rotate_pre_application_access_token(text, uuid) from public, authenticated;
grant execute on function public.rotate_pre_application_access_token(text, uuid) to anon;
