-- SASE-310 — Cierre de PR #49: resuelve P1 #5 de la segunda revision de
-- Codex sobre 05af073 ("Restore authenticated reporter validation for
-- incidents").
--
-- REGRESION: 0007 recreo `student_incidents` (drop+create sobre tabla
-- vacia) para corregir su esquema y, al hacerlo, perdio la condicion
-- `reported_by_profile_id = auth.uid()` que 0005 si tenia en su politica de
-- insercion; ademas la columna quedo nullable y `reporter_name` siempre fue
-- texto libre enviado por el cliente. Con eso, cualquier miembro con
-- EDIT_INCIDENTS podia reportar una incidencia a nombre de otro perfil, con
-- un nombre de staff inventado, o sin reportero alguno.
--
-- SOLUCION (dos capas, no una sola):
--  1. Un trigger BEFORE INSERT ignora lo que el cliente haya enviado en
--     reported_by_profile_id/reporter_name y los reemplaza SIEMPRE por
--     auth.uid() y profiles.full_name del actor autenticado. reporter_name
--     deja de ser una identidad autoritativa controlada por el cliente: es
--     una copia denormalizada de lo que el servidor ya determino.
--  2. La politica de insercion vuelve a exigir
--     reported_by_profile_id = auth.uid() (como 0005), para que si el
--     trigger alguna vez se elimina por error, RLS siga rechazando una
--     atribucion falsificada en vez de aceptarla en silencio.
--
-- Sin sesion autenticada (auth.uid() nulo) el trigger rechaza el insert
-- directamente: no se permite un reportero null.
--
-- Tabla `student_incidents` esta vacia en remoto a la fecha de esta
-- migracion (verificado antes de escribirla, mismo criterio que 0007/0008);
-- el ALTER COLUMN ... SET NOT NULL no requiere backfill.
--
-- ESTADO: escrita, NO aplicada. Requiere autorizacion explicita de Hugo
-- antes de tocar el proyecto remoto "SASE-Light" (plyjvvpkaafnkxmmqkbh).
-- Antes de aplicar, reverificar que la tabla sigue vacia en remoto: si ya
-- hay incidencias reales, el ALTER COLUMN...SET NOT NULL fallaria sobre
-- cualquier fila con reported_by_profile_id nulo y requeriria backfill
-- previo (fuera del alcance de esta migracion).

create or replace function public.sase_stamp_incident_reporter()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_actor uuid;
  v_full_name text;
begin
  v_actor := (select auth.uid());
  if v_actor is null then
    raise exception 'SASE_INCIDENT_REPORTER_REQUIRES_SESSION';
  end if;

  select full_name into v_full_name from public.profiles where id = v_actor;

  new.reported_by_profile_id := v_actor;
  new.reporter_name := coalesce(v_full_name, '');
  return new;
end;
$$;

create trigger trg_student_incidents_stamp_reporter
  before insert on public.student_incidents
  for each row execute function public.sase_stamp_incident_reporter();

alter table public.student_incidents
  alter column reported_by_profile_id set not null;

alter policy student_incidents_insert_by_permission on public.student_incidents
  with check (
    has_permission(institution_id, 'EDIT_INCIDENTS')
    and reported_by_profile_id = (select auth.uid())
  );
