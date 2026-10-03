-- SASE-310 — corrige 2 hallazgos de la revision de Codex sobre 0019.
--
-- 1. (P1, real) `get_pre_application_with_children` (0019) autoriza el
--    token de gracia en su propio `exists(...)`, pero le sigue pasando
--    `p_access_token` -- que puede ser el token VIEJO, si eso fue lo que
--    autorizo la llamada -- a `get_pre_application_with_children_legacy`,
--    que exige coincidencia EXACTA con el `access_token` vigente
--    (`v_row.access_token is distinct from p_access_token`, 0015) y
--    devuelve null. Resultado: la ventana de gracia que 0019 acaba de
--    introducir no funcionaba para GET -- exactamente el escenario que
--    pretendia arreglar (respuesta de rotate perdida) seguia bloqueando
--    a la familia en la siguiente consulta. UPDATE ya resolvia esto
--    mismo calculando `v_effective_token` (el token VIGENTE real) y
--    pasando ese a la legacy en vez del recibido; GET no lo hacia.
--    Correccion: mismo patron en GET.
--
-- 2. (P2, pero con impacto real) `create_pre_application_with_children`
--    (0019) capturaba `unique_violation` para conservar el contador de
--    rate limit en un folio/CURP duplicado, pero eso rompia una funcion
--    que ya funcionaba: `SupabasePreApplicationRepositoryImpl.submit`
--    distingue `DuplicateFolio` de `DuplicateCurp` leyendo el nombre del
--    constraint en el mensaje de un 409 sin capturar. Al capturar el
--    unique_violation y devolver `null` (200 OK), esa deteccion colapsaba
--    a un `REJECTED` generico para TODO folio duplicado (CURP duplicada
--    ya caia a REJECTED de todas formas para una familia anonima, sin
--    sesion de staff para resolver el registro existente -- eso no es
--    nuevo). Los tests de Kotlin no lo vieron porque mockean HTTP
--    directamente, no la base real -- se detecto probando en vivo contra
--    staging. Correccion: `unique_violation` deja de capturarse en CREATE
--    -- vuelve a propagarse como excepcion no capturada (mismo 409 +
--    mensaje que el cliente ya sabe leer). Se acepta, documentado, que un
--    folio/CURP duplicado en CREATE no hace avanzar el contador de rate
--    limit (el gap que 0019 intentaba cerrar del todo): el riesgo
--    practico es bajo porque folio es elegido por el cliente y CURP
--    requiere adivinar un valor real ya existente, no una cadena vacia o
--    invalida cualquiera -- muy distinto de adivinar un UUID de token.
--    Los demas rechazos que SI capturaba (`not_null_violation`,
--    `check_violation`, `foreign_key_violation`, `invalid_text_representation`)
--    no tienen ningun manejo especial en el cliente (ya caian a
--    `REJECTED` generico con o sin 0019) y se mantienen: para esos casos
--    el contador si se conserva.
--
-- ESTADO: escrita y aplicada a staging en la misma sesion que la escribe,
-- antes de mergear el PR.

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
  if not public.sase_check_pre_application_rate_limit('GET', 20, interval '10 minutes') then
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
    when not_null_violation or check_violation
      or foreign_key_violation or invalid_text_representation then
      -- unique_violation NO se captura aqui: debe seguir propagandose
      -- como 409 sin capturar para que el cliente distinga folio
      -- duplicado de CURP duplicada (ver comentario de cabecera).
      return null;
  end;
end;
$$;

revoke execute on function public.get_pre_application_with_children(text, uuid) from public, anon, authenticated;
grant execute on function public.get_pre_application_with_children(text, uuid) to anon;

revoke execute on function public.create_pre_application_with_children(jsonb, jsonb, jsonb, jsonb) from public, anon, authenticated;
grant execute on function public.create_pre_application_with_children(jsonb, jsonb, jsonb, jsonb) to anon;
