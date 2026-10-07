-- SASE-310 — CREATE dice que dato fue invalido en vez de devolver null.
--
-- Hallazgo de pruebas manuales de Hugo: cuando un envio falla, el sistema debe
-- indicar el error preciso, sin ambiguedades. Desde 0020/0021, un dato
-- invalido en CREATE (faltante, fuera de regla, tipo equivocado) se capturaba
-- para conservar el contador de rate limit y se devolvia `null`: el cliente
-- solo podia decir "rechazado", sin decir cual dato ni por que.
--
-- Ahora la captura sigue igual (el rate limit se conserva, unique_violation
-- sigue propagandose como 409 para distinguir folio de CURP duplicados), pero
-- en lugar de `null` se devuelve un objeto con la causa real que informa
-- Postgres:
--   {"error":"INVALID_DATA","message":"...","column":"...","constraint":"..."}
-- El cliente lo lee (SupabasePreApplicationRepositoryImpl.submit) y lo muestra.
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
  v_message text;
  v_column text;
  v_constraint text;
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
      get stacked diagnostics
        v_message = message_text,
        v_column = column_name,
        v_constraint = constraint_name;
      return jsonb_build_object(
        'error', 'INVALID_DATA',
        'message', v_message,
        'column', v_column,
        'constraint', v_constraint
      );
  end;

  select access_token_expires_at into v_expires_at
  from public.pre_applications
  where folio = v_created ->> 'folio';

  return v_created || jsonb_build_object('access_token_expires_at', v_expires_at);
end;
$$;

revoke execute on function public.create_pre_application_with_children(jsonb, jsonb, jsonb, jsonb) from public, anon, authenticated;
grant execute on function public.create_pre_application_with_children(jsonb, jsonb, jsonb, jsonb) to anon;
