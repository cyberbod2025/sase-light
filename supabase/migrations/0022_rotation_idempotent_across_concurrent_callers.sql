-- SASE-310 — rotacion idempotente entre llamadores concurrentes reales.
--
-- Hallazgo real de revision de codigo (Codex, PR #52, P1), distinto del
-- que el Mutex del cliente (commit anterior) ya cerraba: ese Mutex
-- solo serializa llamadas dentro de UN proceso de la app. Dos INSTANCIAS
-- reales -- la familia con la app abierta en el celular y en la laptop,
-- o dos toques casi simultaneos desde dispositivos distintos -- pueden
-- llamar a `rotate_pre_application_access_token` de verdad en paralelo,
-- fuera del alcance de cualquier Mutex en un solo proceso.
--
-- Con el diseno anterior (0019-0021), ambas llamadas con el mismo token
-- vigente T0 podian tener exito: la primera rota T0->T1 (T0 pasa a
-- `previous_access_token`, con ventana de gracia); la segunda, al
-- reintentar su UPDATE tras el lock de fila, ya no encuentra T0 como
-- vigente pero SI lo encuentra como `previous_access_token` (todavia
-- dentro de la ventana) y rota otra vez, T1->T2. Resultado: al primer
-- llamador se le dijo "guarda T1", pero T1 queda relegado a token de
-- gracia de solo 10 minutos -- pasado ese plazo, sin ruta de
-- recuperacion si esa fue la unica copia que guardo.
--
-- Correccion: separar "coincide con el vigente" (rota de verdad) de
-- "coincide solo con el de gracia" (no rota otra vez -- devuelve el
-- vigente real tal cual esta, para que el llamador converja). Un UPDATE
-- que solo tiene exito contra el token vigente se sigue serializando por
-- el lock de fila de Postgres; cuando pierde esa carrera, el fallback de
-- solo lectura contra `previous_access_token` no vuelve a mutar nada, asi
-- que no puede generar una tercera generacion de token. Dos, tres o mas
-- llamadores concurrentes con el mismo token de entrada terminan viendo
-- el mismo token vigente, no una cadena de reemplazos huerfanos.
--
-- ESTADO: escrita y aplicada a staging en la misma sesion que la escribe,
-- antes de mergear el PR.

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
  if not public.sase_check_pre_application_rate_limit('ROTATE', 20, interval '10 minutes') then
    raise exception 'SASE_PRE_APPLICATION_RATE_LIMITED';
  end if;

  -- Caso 1: el token recibido es el VIGENTE -- rotacion real. El lock de
  -- fila de este UPDATE serializa a cualquier otro llamador concurrente
  -- que tambien presente el mismo token vigente.
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

  -- Caso 2: no coincidio con el vigente (pudo ser porque otro llamador
  -- concurrente ya roto justo antes). Si coincide con el token de
  -- GRACIA, no se rota de nuevo -- solo se informa cual es el vigente
  -- real tal cual esta, sin mutar nada, para que este llamador converja
  -- con el que si gano la carrera en vez de generar una tercera
  -- generacion de token.
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

revoke execute on function public.rotate_pre_application_access_token(text, uuid) from public, authenticated;
grant execute on function public.rotate_pre_application_access_token(text, uuid) to anon;
