-- SASE-310 — el contador de rate limiting debe sobrevivir a un rechazo.
--
-- Hallazgo real de revision de codigo (Codex, PR #52) sobre 0017: en
-- Postgres, un `raise exception` no capturado aborta TODA la transaccion
-- de la llamada RPC -- incluido el incremento del contador de
-- `pre_application_rate_limits` que `sase_check_pre_application_rate_limit`
-- ya habia hecho unas lineas antes, dentro de la MISMA transaccion. En
-- `update_pre_application_with_children` y `rotate_pre_application_access_token`,
-- el chequeo de rate limit pasa primero (incrementa y permite continuar) y
-- SOLO DESPUES se valida el token/estado; si esa validacion posterior
-- rechaza con `raise exception`, el rollback de toda la transaccion
-- deshace tambien el incremento que la propia llamada rechazada acababa
-- de hacer. Un atacante adivinando tokens contra UPDATE o ROTATE nunca
-- hace avanzar el contador con sus intentos fallidos -- puede adivinar
-- indefinidamente sin tocar el limite.
--
-- (El caso de "propio rate limit excedido" -- `raise exception
-- 'SASE_PRE_APPLICATION_RATE_LIMITED'` -- NO tiene este problema: el
-- valor persistido queda anclado en el limite tras cada rechazo, porque
-- cada intento siguiente recalcula desde ese mismo valor anclado y se
-- vuelve a rechazar. No se toca esa rama.)
--
-- Correccion: los dos rechazos por token/estado invalido dejan de lanzar
-- excepcion y devuelven `null` en su lugar -- exactamente el patron que
-- `get_pre_application_with_children` (0017) ya usaba para su propio
-- rechazo de token, sin este problema. La transaccion completa (incluido
-- el incremento del contador) se confirma normalmente. El cliente Kotlin
-- no necesita cambios: al intentar deserializar un cuerpo `null` como
-- `String` (update) o `RotatePreApplicationAccessTokenResponse` (rotate,
-- objeto no-nulo) ya lanza una excepcion de serializacion que el codigo
-- existente captura y traduce a `REJECTED` -- el mismo resultado que antes,
-- solo que ahora el rate limit de verdad cuenta el intento.
--
-- ESTADO: escrita, pendiente de aplicar a staging (se aplica en la misma
-- sesion que la escribe, antes de mergear el PR).

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
      -- No se lanza excepcion: un raise aqui deshace el incremento del
      -- contador de rate limit que la linea de arriba acaba de confirmar
      -- dentro de esta misma transaccion (ver comentario de cabecera).
      return null;
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

revoke execute on function public.update_pre_application_with_children(text, uuid, jsonb, jsonb, jsonb, jsonb) from public, anon, authenticated;
grant execute on function public.update_pre_application_with_children(text, uuid, jsonb, jsonb, jsonb, jsonb) to anon, authenticated;

revoke execute on function public.rotate_pre_application_access_token(text, uuid) from public, authenticated;
grant execute on function public.rotate_pre_application_access_token(text, uuid) to anon;
