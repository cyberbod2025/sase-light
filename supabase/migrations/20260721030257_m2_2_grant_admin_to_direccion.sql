-- M2.2 — Alta del primer administrador institucional.
-- Sin esto nadie puede gestionar membresías desde la app. Se concede
-- ADMIN_INSTITUCIONAL como rol ADICIONAL al único perfil activo de Dirección,
-- localizado por consulta (nunca por UUID hardcodeado). No toca membresías,
-- scopes ni el rol DIRECCION existente. Idempotente.
do $$
declare
    v_direccion_count int;
    v_membership uuid;
    v_role uuid;
begin
    select count(*) into v_direccion_count
    from public.profiles p
    where p.role = 'direccion' and p.active;

    if v_direccion_count <> 1 then
        raise exception 'SASE_M2_2_EXPECTED_ONE_DIRECCION: se encontraron %', v_direccion_count;
    end if;

    select m.id into v_membership
    from public.institutional_memberships m
    join public.profiles p on p.id = m.profile_id
    where p.role = 'direccion' and p.active and m.active;

    if v_membership is null then
        raise exception 'SASE_M2_2_MISSING_MEMBERSHIP';
    end if;

    select r.id into v_role from public.roles r where r.code = 'ADMIN_INSTITUCIONAL';

    if v_role is null then
        raise exception 'SASE_M2_2_MISSING_ROLE_CATALOG';
    end if;

    insert into public.membership_roles (membership_id, role_id)
    values (v_membership, v_role)
    on conflict (membership_id, role_id) do nothing;
end;
$$;
