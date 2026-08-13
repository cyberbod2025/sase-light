-- SASE-310 — Cierre de PR #49: resuelve el hallazgo de get_advisors
-- (security) tras aplicar 0010-0013 -- "Public Can Execute SECURITY
-- DEFINER Function" / "Signed-In Users Can Execute SECURITY DEFINER
-- Function" sobre sase_stamp_incident_reporter() y
-- sase_stamp_observation_author_name().
--
-- Ambas son funciones de trigger (`returns trigger`): Postgres ya rechaza
-- invocarlas directo ("trigger functions can only be called as triggers"),
-- verificado antes de escribir esta migracion, asi que no eran explotables
-- via /rest/v1/rpc/... tal cual estaban. Aun asi, el grant implicito de
-- EXECUTE a `anon`/`authenticated` (default de Postgres para funciones en
-- `public`) las deja innecesariamente expuestas como endpoints RPC en la
-- superficie publica de PostgREST -- higiene de superficie, no un exploit
-- real. Se revoca el EXECUTE implicito: solo deben ejecutarse como
-- triggers (el dueno de la tabla/rol de trigger las invoca sin necesitar
-- este grant).
--
-- CORRECCION EN APLICACION: `revoke ... from anon, authenticated` no basta
-- -- el grant real (verificado en information_schema.routine_privileges)
-- estaba en `PUBLIC`, del que `anon`/`authenticated` heredan implicitamente
-- como cualquier rol. get_advisors siguio marcando el hallazgo hasta que
-- se revoco de `PUBLIC` directamente; verificado despues que solo quedan
-- `postgres`/`service_role` con EXECUTE (ninguno expuesto via PostgREST).
--
-- ESTADO: APLICADA al proyecto remoto "SASE-Light" (plyjvvpkaafnkxmmqkbh)
-- el 2026-08-12 con autorizacion explicita de Hugo.

revoke execute on function public.sase_stamp_incident_reporter() from public;
revoke execute on function public.sase_stamp_observation_author_name() from public;
