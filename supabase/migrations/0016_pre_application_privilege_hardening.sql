-- SASE-310 — Cierre de PR #51: hardening de privilegios detectado en la
-- verificacion post-migracion de 0015 (ya aplicada a staging como
-- 20260822144753_0015_pre_applications_persistence). Esta migracion NO
-- agrega funcionalidad, tablas ni cambios de modelo: solo cierra superficie
-- de privilegios que Supabase otorga por default y que 0015 no revoco
-- explicitamente en las tablas hijas ni en las funciones nuevas.
--
-- PROBLEMA 1 — TABLAS HIJAS CON PRIVILEGIOS AMPLIOS POR DEFAULT: Supabase
-- otorga por default (ALTER DEFAULT PRIVILEGES del esquema public) SELECT/
-- INSERT/UPDATE/DELETE/TRUNCATE/REFERENCES/TRIGGER a anon/authenticated
-- sobre toda tabla nueva. 0015 ya revoco esto en `pre_applications` (columna
-- por columna, sin exponer access_token), pero las 3 tablas hijas
-- (pre_application_responsables/autorizados/documentos) se quedaron con el
-- grant amplio por default aunque sus policies de escritura directa fueron
-- eliminadas (drop policy ..._write al final de cada bloque en 0015). RLS
-- protege SELECT/UPDATE/DELETE via policy, pero TRUNCATE no pasa por RLS en
-- absoluto -- cualquier rol con privilegio TRUNCATE puede vaciar la tabla
-- sin que ninguna policy lo evalue. Minimo privilegio: anon no necesita
-- tocar estas tablas directo (opera solo via las RPC security definer);
-- authenticated (Secretaria, via REVIEW_PRE_APPLICATION) solo necesita
-- SELECT directo para cargar las colecciones bajo sus policies de lectura.
--
-- PROBLEMA 2 — EXECUTE HEREDADO DE PUBLIC EN LAS FUNCIONES NUEVAS: mismo
-- patron que 0014 ya corrigio para las funciones de trigger de incidencias/
-- observaciones -- Postgres otorga EXECUTE implicito via PUBLIC a toda
-- funcion nueva en public, y anon/authenticated lo heredan aunque nunca se
-- les haya otorgado explicitamente. Las 4 funciones de 0015 quedaron con
-- ese EXECUTE heredado sin cerrar:
--   a) sase_stamp_pre_application_institution(): funcion de trigger (igual
--      que 0014), no debe ser invocable como RPC por nadie.
--   b) create_pre_application_with_children(...): solo la familia (anon,
--      via token) debe poder crear.
--   c) get_pre_application_with_children(...): solo la familia (anon, via
--      token) debe poder leer por esta via -- Secretaria lee por REST
--      directo con RLS normal (pre_applications_select_reviewer).
--   d) update_pre_application_with_children(...): sirve a ambos actores
--      (familia via token, Secretaria via auth.uid()+REVIEW_PRE_APPLICATION
--      -- ver validacion interna en 0015), asi que necesita EXECUTE de
--      anon Y authenticated.
-- 0015 ya tenia `grant execute ... to anon` / `to anon, authenticated`
-- explicitos para b/c/d, pero nunca revoco el EXECUTE heredado de PUBLIC
-- primero, asi que el grant explicito quedo siendo redundante con una
-- superficie mas amplia (PUBLIC) que seguia abierta por debajo.
--
-- ESTADO: escrita, NO aplicada. Requiere autorizacion explicita de Hugo
-- antes de tocar el proyecto remoto "SASE-Light" (plyjvvpkaafnkxmmqkbh),
-- igual que 0015.

-- Precondiciones fail-fast: esta migracion es solo hardening de privilegios
-- sobre objetos que 0015 ya debe haber creado. No recrea tablas, funciones
-- ni policies -- si algo de esto falta, 0015 no se aplico (o se aplico mal)
-- y este archivo debe abortar antes de tocar ningun privilegio.
do $$
begin
  if to_regclass('public.pre_applications') is null then
    raise exception 'SASE_PRE_APPLICATION_PRIVILEGE_HARDENING_PRECONDITION_FAILED: falta tabla public.pre_applications';
  end if;

  if to_regclass('public.pre_application_responsables') is null then
    raise exception 'SASE_PRE_APPLICATION_PRIVILEGE_HARDENING_PRECONDITION_FAILED: falta tabla public.pre_application_responsables';
  end if;

  if to_regclass('public.pre_application_autorizados') is null then
    raise exception 'SASE_PRE_APPLICATION_PRIVILEGE_HARDENING_PRECONDITION_FAILED: falta tabla public.pre_application_autorizados';
  end if;

  if to_regclass('public.pre_application_documentos') is null then
    raise exception 'SASE_PRE_APPLICATION_PRIVILEGE_HARDENING_PRECONDITION_FAILED: falta tabla public.pre_application_documentos';
  end if;

  if to_regprocedure('public.create_pre_application_with_children(jsonb, jsonb, jsonb, jsonb)') is null then
    raise exception 'SASE_PRE_APPLICATION_PRIVILEGE_HARDENING_PRECONDITION_FAILED: falta funcion public.create_pre_application_with_children(jsonb, jsonb, jsonb, jsonb)';
  end if;

  if to_regprocedure('public.update_pre_application_with_children(text, uuid, jsonb, jsonb, jsonb, jsonb)') is null then
    raise exception 'SASE_PRE_APPLICATION_PRIVILEGE_HARDENING_PRECONDITION_FAILED: falta funcion public.update_pre_application_with_children(text, uuid, jsonb, jsonb, jsonb, jsonb)';
  end if;

  if to_regprocedure('public.get_pre_application_with_children(text, uuid)') is null then
    raise exception 'SASE_PRE_APPLICATION_PRIVILEGE_HARDENING_PRECONDITION_FAILED: falta funcion public.get_pre_application_with_children(text, uuid)';
  end if;

  if to_regprocedure('public.sase_stamp_pre_application_institution()') is null then
    raise exception 'SASE_PRE_APPLICATION_PRIVILEGE_HARDENING_PRECONDITION_FAILED: falta funcion public.sase_stamp_pre_application_institution()';
  end if;
end;
$$;

-- === Tablas hijas: minimo privilegio ==========================================
--
-- anon no recibe ningun privilegio directo (opera exclusivamente via las
-- RPC security definer, que bypassan RLS y hacen su propia validacion de
-- token). authenticated recibe solo SELECT: Secretaria necesita cargar las
-- colecciones bajo las policies ..._select ya existentes, pero toda
-- escritura sigue pasando unicamente por las RPC (las policies ..._write
-- fueron eliminadas en 0015; sin un grant de INSERT/UPDATE/DELETE/TRUNCATE
-- a nivel tabla, ni siquiera un bypass de RLS mal configurado podria
-- escribir aqui por REST directo).

revoke all on table public.pre_application_responsables from anon, authenticated;
grant select on table public.pre_application_responsables to authenticated;

revoke all on table public.pre_application_autorizados from anon, authenticated;
grant select on table public.pre_application_autorizados to authenticated;

revoke all on table public.pre_application_documentos from anon, authenticated;
grant select on table public.pre_application_documentos to authenticated;

-- === Funciones nuevas: cerrar EXECUTE heredado de PUBLIC ======================
--
-- Se revoca primero de PUBLIC/anon/authenticated (igual que 0014 tuvo que
-- hacer explicitamente con PUBLIC para que get_advisors dejara de marcar el
-- hallazgo) y despues se vuelve a conceder solo lo que cada funcion
-- necesita segun quien la invoca.

revoke execute on function public.sase_stamp_pre_application_institution() from public, anon, authenticated;
-- Funcion de trigger: nadie vuelve a recibir EXECUTE directo.

revoke execute on function public.create_pre_application_with_children(jsonb, jsonb, jsonb, jsonb) from public, anon, authenticated;
grant execute on function public.create_pre_application_with_children(jsonb, jsonb, jsonb, jsonb) to anon;

revoke execute on function public.get_pre_application_with_children(text, uuid) from public, anon, authenticated;
grant execute on function public.get_pre_application_with_children(text, uuid) to anon;

revoke execute on function public.update_pre_application_with_children(text, uuid, jsonb, jsonb, jsonb, jsonb) from public, anon, authenticated;
grant execute on function public.update_pre_application_with_children(text, uuid, jsonb, jsonb, jsonb, jsonb) to anon, authenticated;
