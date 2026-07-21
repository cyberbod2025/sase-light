-- M2.1 — Cierre de funciones de trigger frente a la API REST.
-- sase_forbid_self_grant y sase_validate_scope solo tienen sentido dentro de
-- su trigger; nadie debe poder invocarlas como RPC. Los helpers usados por las
-- políticas de RLS (has_permission, has_scope, current_membership,
-- actor_has_membership, actor_holds_role, membership_owner,
-- membership_institution, current_app_role) NO se tocan: revocarlos rompería
-- el acceso legítimo.
revoke all on function public.sase_forbid_self_grant() from public, anon, authenticated;
revoke all on function public.sase_validate_scope() from public, anon, authenticated;
