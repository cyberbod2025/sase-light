-- SASE-310 — CREATE informa el vencimiento; ROTATE al nivel de GET.
--
-- Dos hallazgos reales de revision de codigo (Codex, PR #52, P2):
--
-- 1. El token que emite CREATE tambien vence a los 30 dias (default de
--    0017), pero el wrapper devolvia solo `folio` y `access_token` (la
--    respuesta de la legacy). La confirmacion de envio le pedia a la
--    familia guardar el codigo sin decirle que vence: quien no consulte en
--    30 dias queda fuera sin aviso. Ahora la respuesta incluye
--    `access_token_expires_at`, como ya hace ROTATE.
--
-- 2. Cada consulta exitosa del portal hace un GET y un ROTATE (mas hasta 3
--    reintentos de ROTATE si falla). 0021 dejo GET en 40 pero ROTATE en 20,
--    contradiciendo su propio comentario ("ROTATE a la par de GET porque
--    cada consulta dispara un ROTATE"): el techo real de una red compartida
--    era 20, y a partir de ahi el GET pasaba pero el portal reportaba error
--    por la rotacion estrangulada. ROTATE sube a 40, igual que GET.
--
-- Se conservan tal cual: en CREATE, el bloque que captura not_null/check/
-- foreign_key/texto invalido y NO unique_violation (0020); en ROTATE, la
-- logica idempotente de dos casos (0022).
--
-- ESTADO: escrita y aplicada a staging en la misma sesion que la escribe.

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
declare
  v_created jsonb;
  v_expires_at timestamptz;
begin
  if not public.sase_check_pre_application_rate_limit('CREATE', 20, interval '10 minutes') then
    raise exception 'SASE_PRE_APPLICATION_RATE_LIMITED';
  end if;

  begin
    v_created := public.create_pre_application_with_children_legacy(
      p_record, p_responsables, p_autorizados, p_documentos
    );
  exception
    when not_null_violation or check_violation
      or foreign_key_violation or invalid_text_representation then
      return null;
  end;

  select access_token_expires_at into v_expires_at
  from public.pre_applications
  where folio = v_created ->> 'folio';

  return v_created || jsonb_build_object('access_token_expires_at', v_expires_at);
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
  v_result_token uuid;
  v_result_expires_at timestamptz;
begin
  if not public.sase_check_pre_application_rate_limit('ROTATE', 40, interval '10 minutes') then
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
    and access_token = p_access_token
    and access_token_revoked_at is null
    and access_token_expires_at > now()
    and status <> 'CANCELADA'
  returning access_token, access_token_expires_at into v_result_token, v_result_expires_at;

  if found then
    return jsonb_build_object(
      'folio', p_folio,
      'access_token', v_result_token,
      'access_token_expires_at', v_result_expires_at
    );
  end if;

  select access_token, access_token_expires_at
  into v_result_token, v_result_expires_at
  from public.pre_applications
  where folio = p_folio
    and previous_access_token = p_access_token
    and previous_access_token_valid_until > now()
    and access_token_revoked_at is null
    and access_token_expires_at > now()
    and status <> 'CANCELADA';

  if v_result_token is null then
    return null;
  end if;

  return jsonb_build_object(
    'folio', p_folio,
    'access_token', v_result_token,
    'access_token_expires_at', v_result_expires_at
  );
end;
$$;

revoke execute on function public.create_pre_application_with_children(jsonb, jsonb, jsonb, jsonb) from public, anon, authenticated;
grant execute on function public.create_pre_application_with_children(jsonb, jsonb, jsonb, jsonb) to anon;

revoke execute on function public.rotate_pre_application_access_token(text, uuid) from public, authenticated;
grant execute on function public.rotate_pre_application_access_token(text, uuid) to anon;
