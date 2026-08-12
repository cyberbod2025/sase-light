-- SASE-310 — Cierre de PR #49: resuelve P1 #1 de la segunda revision de
-- Codex sobre 05af073 ("Align connected action permissions with RLS").
--
-- CONTEXTO: el catalogo real (introspeccion 2026-08-12) NO otorga
-- EDIT_INCIDENTS a SECRETARIA ni a DIRECCION -- solo a PREFECTURA y TUTOR,
-- dos roles que no existen en el StaffRole de Kotlin. El cliente
-- (StaffPermissions.actionMatrix) sin embargo permite a SECRETARIA
-- REPORT_INCIDENT/ADVANCE_INCIDENT -- un boton que hoy falla siempre contra
-- Supabase staging.
--
-- DECISION (Hugo, via AskUserQuestion en el cierre de PR #49): SECRETARIA es
-- el rol de piloto ya construido que reporta incidencias en la UI
-- (commit 5a21eda, "real incident workflow with authenticated authorship").
-- Se amplia el catalogo real para que coincida con lo que el producto ya
-- construyo, en vez de apagar la funcionalidad o inventar roles/pantallas
-- nuevas (PREFECTURA/TUTOR) fuera de alcance de este cierre.
--
-- DIRECCION NO recibe este permiso: queda como rol de supervision/lectura
-- (VIEW_INCIDENTS ya lo tiene), consistente con que el catalogo real no le
-- da ningun EDIT_* hoy.
--
-- ESTADO: escrita, NO aplicada. Requiere autorizacion explicita de Hugo
-- antes de tocar el proyecto remoto "SASE-Light" (plyjvvpkaafnkxmmqkbh).

insert into public.role_permissions (role_id, permission_id)
select r.id, p.id
from public.roles r
join public.permissions p on p.code = 'EDIT_INCIDENTS'
where r.code = 'SECRETARIA'
on conflict (role_id, permission_id) do nothing;
