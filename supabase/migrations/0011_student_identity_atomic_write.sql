-- SASE-310 — Cierre de PR #49: resuelve P1 #2 y P1 #4 de la segunda
-- revision de Codex sobre 05af073 ("Persist sensitive identity during
-- student creation" / "Make core and sensitive student updates atomic").
--
-- PROBLEMA: el cliente escribia el nucleo (`students`) y la identidad
-- sensible (`student_sensitive_identity`) en dos llamadas HTTP separadas.
-- Si la segunda fallaba tras exito de la primera, el expediente quedaba con
-- un tutor/domicilio a medio escribir (o del todo ausente en el alta) sin
-- que la UI lo distinguiera de un guardado completo.
--
-- SOLUCION: dos funciones RPC que ejecutan ambas escrituras dentro de la
-- misma transaccion implicita de PostgREST. `security invoker` (no
-- `definer`): ambas corren con el rol y el JWT del actor, asi que las
-- politicas RLS existentes de `students` y `student_sensitive_identity`
-- (EDIT_STUDENT_IDENTITY, migraciones 0004/0005) siguen siendo la unica
-- autorizacion -- esta migracion NO otorga ningun permiso nuevo. Si
-- cualquiera de las dos escrituras es rechazada por RLS o lanza una
-- excepcion (incluida una violacion de unique constraint), la funcion
-- completa aborta y Postgres revierte automaticamente TODO lo que la
-- funcion alcanzo a escribir en esa invocacion -- no hace falta un BEGIN/
-- COMMIT explicito, una funcion es una sola sentencia para el cliente.
--
-- El `institution_id` de la identidad sensible se toma SIEMPRE de la fila
-- de `students` ya insertada/actualizada (nunca de un parametro del
-- cliente), para que sea imposible adjuntar una identidad sensible a la
-- institucion equivocada.
--
-- CORRECCION (revision previa a aplicar, Hugo 2026-08-12): la version
-- original de `create_student_core_and_identity` afirmaba esta garantia en
-- el comentario de arriba pero no la cumplia -- reutilizaba `p_institution_id`
-- (el parametro del cliente) directamente para el INSERT de
-- `student_sensitive_identity` en vez de leerlo de la fila de `students` ya
-- insertada. `update_student_core_and_identity` si lo hacia bien
-- (`returning ... institution_id into v_institution_id`). Ahora ambas
-- funciones derivan `institution_id` exclusivamente de la fila servidor via
-- `returning`, nunca del parametro del cliente para la segunda escritura.
--
-- ESTADO: escrita, NO aplicada. Requiere autorizacion explicita de Hugo
-- antes de tocar el proyecto remoto "SASE-Light" (plyjvvpkaafnkxmmqkbh).

create or replace function public.create_student_core_and_identity(
  p_institution_id uuid,
  p_full_name text,
  p_student_group text,
  p_enrollment_id text,
  p_curp text,
  p_shift text,
  p_school_year text,
  p_status text,
  p_pre_application_folio text,
  p_birth_date text,
  p_birth_place text,
  p_address text,
  p_zip_code text,
  p_tutor_name text,
  p_tutor_relation text,
  p_tutor_phone text,
  p_tutor_email text,
  p_emergency_contact_name text,
  p_emergency_contact_relation text,
  p_emergency_contact_phone text,
  p_emergency_contact_email text
)
returns table (
  id uuid,
  institution_id uuid,
  full_name text,
  student_group text,
  enrollment_id text,
  curp text,
  shift text,
  school_year text,
  status text,
  pre_application_folio text,
  birth_date text,
  birth_place text,
  address text,
  zip_code text,
  tutor_name text,
  tutor_relation text,
  tutor_phone text,
  tutor_email text,
  emergency_contact_name text,
  emergency_contact_relation text,
  emergency_contact_phone text,
  emergency_contact_email text
)
language plpgsql
security invoker
set search_path = ''
as $$
declare
  v_student_id uuid;
  v_institution_id uuid;
begin
  insert into public.students (
    institution_id, full_name, student_group, enrollment_id, curp, shift,
    school_year, status, pre_application_folio
  ) values (
    p_institution_id, p_full_name, p_student_group, p_enrollment_id, p_curp, p_shift,
    p_school_year, p_status, p_pre_application_folio
  )
  returning public.students.id, public.students.institution_id into v_student_id, v_institution_id;

  -- v_institution_id (leido de vuelta de la fila insertada), NO
  -- p_institution_id (el parametro del cliente): la segunda escritura nunca
  -- confia otra vez en lo que el cliente afirmo.
  insert into public.student_sensitive_identity (
    student_id, institution_id, birth_date, birth_place, address, zip_code,
    tutor_name, tutor_relation, tutor_phone, tutor_email,
    emergency_contact_name, emergency_contact_relation, emergency_contact_phone, emergency_contact_email
  ) values (
    v_student_id, v_institution_id, p_birth_date, p_birth_place, p_address, p_zip_code,
    p_tutor_name, p_tutor_relation, p_tutor_phone, p_tutor_email,
    p_emergency_contact_name, p_emergency_contact_relation, p_emergency_contact_phone, p_emergency_contact_email
  );

  return query
  select
    s.id, s.institution_id, s.full_name, s.student_group, s.enrollment_id,
    s.curp, s.shift, s.school_year, s.status, s.pre_application_folio,
    ssi.birth_date, ssi.birth_place, ssi.address, ssi.zip_code,
    ssi.tutor_name, ssi.tutor_relation, ssi.tutor_phone, ssi.tutor_email,
    ssi.emergency_contact_name, ssi.emergency_contact_relation,
    ssi.emergency_contact_phone, ssi.emergency_contact_email
  from public.students s
  join public.student_sensitive_identity ssi on ssi.student_id = s.id
  where s.id = v_student_id;
end;
$$;

grant execute on function public.create_student_core_and_identity(
  uuid, text, text, text, text, text, text, text, text,
  text, text, text, text, text, text, text, text, text, text, text, text
) to authenticated;

create or replace function public.update_student_core_and_identity(
  p_student_id uuid,
  p_full_name text,
  p_student_group text,
  p_enrollment_id text,
  p_curp text,
  p_shift text,
  p_school_year text,
  p_status text,
  p_pre_application_folio text,
  p_birth_date text,
  p_birth_place text,
  p_address text,
  p_zip_code text,
  p_tutor_name text,
  p_tutor_relation text,
  p_tutor_phone text,
  p_tutor_email text,
  p_emergency_contact_name text,
  p_emergency_contact_relation text,
  p_emergency_contact_phone text,
  p_emergency_contact_email text
)
returns table (
  id uuid,
  institution_id uuid,
  full_name text,
  student_group text,
  enrollment_id text,
  curp text,
  shift text,
  school_year text,
  status text,
  pre_application_folio text,
  birth_date text,
  birth_place text,
  address text,
  zip_code text,
  tutor_name text,
  tutor_relation text,
  tutor_phone text,
  tutor_email text,
  emergency_contact_name text,
  emergency_contact_relation text,
  emergency_contact_phone text,
  emergency_contact_email text
)
language plpgsql
security invoker
set search_path = ''
as $$
declare
  v_institution_id uuid;
begin
  update public.students
  set full_name = p_full_name,
      student_group = p_student_group,
      enrollment_id = p_enrollment_id,
      curp = p_curp,
      shift = p_shift,
      school_year = p_school_year,
      status = p_status,
      pre_application_folio = p_pre_application_folio
  where public.students.id = p_student_id
  returning public.students.institution_id into v_institution_id;

  -- RLS oculta la fila (institucion equivocada / sin permiso) o el
  -- expediente no existe: en ambos casos 0 filas afectadas. Se rechaza en
  -- vez de reportar un guardado -- ninguna escritura parcial debe seguir.
  if v_institution_id is null then
    raise exception 'SASE_STUDENT_UPDATE_REJECTED';
  end if;

  insert into public.student_sensitive_identity as ssi (
    student_id, institution_id, birth_date, birth_place, address, zip_code,
    tutor_name, tutor_relation, tutor_phone, tutor_email,
    emergency_contact_name, emergency_contact_relation, emergency_contact_phone, emergency_contact_email
  ) values (
    p_student_id, v_institution_id, p_birth_date, p_birth_place, p_address, p_zip_code,
    p_tutor_name, p_tutor_relation, p_tutor_phone, p_tutor_email,
    p_emergency_contact_name, p_emergency_contact_relation, p_emergency_contact_phone, p_emergency_contact_email
  )
  on conflict (student_id) do update set
    birth_date = excluded.birth_date,
    birth_place = excluded.birth_place,
    address = excluded.address,
    zip_code = excluded.zip_code,
    tutor_name = excluded.tutor_name,
    tutor_relation = excluded.tutor_relation,
    tutor_phone = excluded.tutor_phone,
    tutor_email = excluded.tutor_email,
    emergency_contact_name = excluded.emergency_contact_name,
    emergency_contact_relation = excluded.emergency_contact_relation,
    emergency_contact_phone = excluded.emergency_contact_phone,
    emergency_contact_email = excluded.emergency_contact_email;

  return query
  select
    s.id, s.institution_id, s.full_name, s.student_group, s.enrollment_id,
    s.curp, s.shift, s.school_year, s.status, s.pre_application_folio,
    ssi.birth_date, ssi.birth_place, ssi.address, ssi.zip_code,
    ssi.tutor_name, ssi.tutor_relation, ssi.tutor_phone, ssi.tutor_email,
    ssi.emergency_contact_name, ssi.emergency_contact_relation,
    ssi.emergency_contact_phone, ssi.emergency_contact_email
  from public.students s
  join public.student_sensitive_identity ssi on ssi.student_id = s.id
  where s.id = p_student_id;
end;
$$;

grant execute on function public.update_student_core_and_identity(
  uuid, text, text, text, text, text, text, text, text,
  text, text, text, text, text, text, text, text, text, text, text, text
) to authenticated;
