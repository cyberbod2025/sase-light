-- SASE-310 — el GET familiar ya no filtra el token de gracia.
--
-- Hallazgo real de revision de codigo (Codex, PR #52, P2 pero con impacto
-- de seguridad real): `get_pre_application_with_children_legacy` (0015)
-- construye su respuesta con `to_jsonb(v_row) - 'access_token'` -- excluye
-- EXPLICITAMENTE solo esa columna. `previous_access_token` (0019) es una
-- columna nueva de `pre_applications`, asi que pasa a formar parte de
-- `v_row%rowtype` y queda incluida sin querer en cada respuesta exitosa de
-- GET desde la segunda consulta en adelante (la primera vez que rota, ya
-- hay un valor en `previous_access_token`).
--
-- Esto es un problema real, no solo cosmetico: durante su ventana de
-- gracia de 10 minutos (0019-0022), `previous_access_token` sigue siendo
-- un bearer token valido -- exactamente el mismo tipo de credencial que
-- `access_token` ya se cuida de nunca devolver. Filtrarlo en el cuerpo de
-- la respuesta contradice el limite de "credencial de un solo uso, nunca
-- vuelve en la respuesta" que la migracion 0015 declaraba explicitamente.
--
-- Correccion: excluir tambien `previous_access_token` (y de paso las
-- demas columnas de ciclo de vida del token que tampoco le aportan nada
-- al cliente familiar: `previous_access_token_valid_until`,
-- `access_token_expires_at`, `access_token_revoked_at`,
-- `access_token_rotated_at`) de la proyeccion. Se reescribe la funcion
-- completa (mismo cuerpo que 0015, solo cambia la lista de exclusion) en
-- vez de solo el fragmento, para que quede clara la forma final completa.
--
-- ESTADO: escrita y aplicada a staging en la misma sesion que la escribe,
-- antes de mergear el PR.

create or replace function public.get_pre_application_with_children_legacy(
  p_folio text,
  p_access_token uuid
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_row public.pre_applications%rowtype;
  v_responsables jsonb;
  v_autorizados jsonb;
  v_documentos jsonb;
begin
  select * into v_row from public.pre_applications where folio = p_folio;

  if v_row.folio is null or v_row.access_token is distinct from p_access_token then
    return null;
  end if;

  select coalesce(jsonb_agg(to_jsonb(r) - 'id' - 'pre_application_folio' - 'institution_id' order by r.position), '[]'::jsonb)
    into v_responsables from public.pre_application_responsables r where r.pre_application_folio = p_folio;

  select coalesce(jsonb_agg(to_jsonb(a) - 'id' - 'pre_application_folio' - 'institution_id' order by a.position), '[]'::jsonb)
    into v_autorizados from public.pre_application_autorizados a where a.pre_application_folio = p_folio;

  select coalesce(jsonb_agg(to_jsonb(d) - 'id' - 'pre_application_folio' - 'institution_id' order by d.position), '[]'::jsonb)
    into v_documentos from public.pre_application_documentos d where d.pre_application_folio = p_folio;

  return jsonb_build_object(
    'record', (
      to_jsonb(v_row)
        - 'access_token'
        - 'previous_access_token'
        - 'previous_access_token_valid_until'
        - 'access_token_expires_at'
        - 'access_token_revoked_at'
        - 'access_token_rotated_at'
    ),
    'responsables', v_responsables,
    'autorizados', v_autorizados,
    'documentos', v_documentos
  );
end;
$$;

revoke all on function public.get_pre_application_with_children_legacy(text, uuid) from public, anon, authenticated;
