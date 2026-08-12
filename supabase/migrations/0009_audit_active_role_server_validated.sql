-- SASE-310 — Cierre de PR #49: auditoria no falsificable (P1 #5 de Codex).
--
-- La politica de insercion de 0004 solo validaba actor_profile_id y
-- membership_id contra la sesion; active_role, action, entity_type,
-- entity_id, occurred_at y result quedaban client-controlled. actor_profile_id
-- y membership_id YA eran server-derived (auth.uid()/current_membership()) —
-- ese vector estaba cerrado. `result` ya tenia un check de dominio en 0004.
-- El vector real: un miembro autenticado podia insertar un evento
-- atribuyendose un active_role que nunca tuvo asignado (p.ej. DIRECCION).
--
-- `action`/`entity_type` NO se restringen a un catalogo cerrado: el cliente
-- (InstitutionalAuditValidator) no define uno — accion como
-- "school_incident.advanced.${status}" es dinamica por diseno. Inventar un
-- catalogo aqui contradiria la arquitectura real del cliente.
--
-- Solucion: ALTER POLICY (no requiere drop) añade una condicion a la
-- politica existente: el rol declarado debe corresponder a un rol
-- REALMENTE asignado a la membresia activa del actor via membership_roles.
-- Se aplica a CUALQUIER via de insercion (tabla directa o una futura RPC),
-- porque es la propia politica RLS.

alter policy student_audit_events_insert_own on public.student_audit_events
  with check (
    actor_profile_id = (select auth.uid())
    and membership_id = public.current_membership(institution_id)
    and exists (
      select 1
      from public.membership_roles mr
      join public.roles r on r.id = mr.role_id
      where mr.membership_id = public.current_membership(institution_id)
        and r.code = active_role
    )
  );
