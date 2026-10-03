-- SASE-310 — cierra el hueco de rate limiting para UPDATE con token null.
--
-- Hallazgo real de revision de codigo (Codex, PR #52, P2): en la version
-- de 0021, update_pre_application_with_children solo llama a
-- sase_check_pre_application_rate_limit cuando `p_access_token is not
-- null` (rama familiar). Pero esta RPC esta otorgada a `anon` ademas de
-- `authenticated` -- un llamador anonimo puede mandar `p_access_token:
-- null` a proposito, entrar a la rama de abajo (pensada para
-- SECRETARIA/staff autenticado), y disparar repetidamente el rechazo por
-- `auth.uid() is null` de la funcion legacy sin que ningun contador lo
-- registre. No hay techo para ese trafico.
--
-- "El token es null" no distingue al anonimo del personal real: un
-- anonimo tambien puede mandar null. La distincion correcta es la sesion
-- de Supabase Auth (`auth.uid()`), mismo patron que
-- revoke_pre_application_access_token (0017).
--
-- Tres casos y su politica (decision de Hugo para el caso 3: limite mas
-- generoso, no igual ni ninguno):
--   1. token no null                       -> familia anonima: 20 / 10 min
--      (contador 'UPDATE', igual que 0021)
--   2. token null y auth.uid() null        -> anonimo mandando null a
--      proposito: 20 / 10 min (mismo contador 'UPDATE'). Esta llamada
--      nunca puede tener exito (la rama staff exige sesion), asi que se
--      devuelve null directamente SIN llamar a la legacy: si la legacy
--      lanzara su excepcion de rechazo, el rollback de la transaccion
--      deshaceria tambien el incremento del contador recien hecho (mismo
--      problema que 0018 corrigio en UPDATE/ROTATE) y el limite nunca
--      avanzaria.
--   3. token null y auth.uid() no null     -> Secretaria con sesion real:
--      100 / 10 min, contador PROPIO 'UPDATE_STAFF'. Separado a
--      proposito: Secretaria y las familias comparten IP en la red de la
--      escuela, y un dia de revision intensa no debe agotar la cuota de
--      las familias (ni al reves). Si la legacy rechaza por falta de
--      permiso, el rollback si deshace ese incremento -- aceptable, es
--      personal autenticado, no la superficie de abuso anonimo.
--
-- ESTADO: escrita y aplicada a staging en la misma sesion que la escribe,
-- antes de mergear el PR.

alter table public.pre_application_rate_limits
  drop constraint pre_application_rate_limits_operation_check;

alter table public.pre_application_rate_limits
  add constraint pre_application_rate_limits_operation_check
    check (operation in ('CREATE', 'GET', 'UPDATE', 'UPDATE_STAFF', 'ROTATE'));

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

  if auth.uid() is null then
    if not public.sase_check_pre_application_rate_limit('UPDATE', 20, interval '10 minutes') then
      raise exception 'SASE_PRE_APPLICATION_RATE_LIMITED';
    end if;
    return null;
  end if;

  if not public.sase_check_pre_application_rate_limit('UPDATE_STAFF', 100, interval '10 minutes') then
    raise exception 'SASE_PRE_APPLICATION_RATE_LIMITED';
  end if;

  return public.update_pre_application_with_children_legacy(
    p_folio, p_access_token, p_record, p_responsables, p_autorizados, p_documentos
  );
end;
$$;

revoke execute on function public.update_pre_application_with_children(text, uuid, jsonb, jsonb, jsonb, jsonb) from public, anon, authenticated;
grant execute on function public.update_pre_application_with_children(text, uuid, jsonb, jsonb, jsonb, jsonb) to anon, authenticated;
