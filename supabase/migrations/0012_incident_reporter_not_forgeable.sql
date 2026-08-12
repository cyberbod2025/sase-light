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
-- CORRECCION (segunda revision de Codex sobre e5f5b89, P1 "Stamp incident
-- attribution on updates too"): el trigger original solo cubria BEFORE
-- INSERT. La politica de UPDATE (student_incidents_update_by_permission,
-- 0007) solo exige EDIT_INCIDENTS, sin restringir columnas -- cualquier
-- miembro con ese permiso podia usar advanceIncident() (o un PATCH directo)
-- para reescribir reported_by_profile_id/reporter_name despues del insert.
-- El trigger ahora tambien corre BEFORE UPDATE: en insercion estampa al
-- actor actual (como antes); en actualizacion CONGELA los valores
-- originales (NEW := OLD) sin importar que envie el cliente, porque el
-- reportero de una incidencia es quien la creo, no quien la actualiza
-- despues (p.ej. Direccion avanzando el seguimiento no debe convertirse en
-- el "reportero").
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
  if tg_op = 'UPDATE' then
    -- El reportero es quien creo la incidencia, no quien la actualiza
    -- despues: se congelan los valores originales pase lo que pase en NEW.
    new.reported_by_profile_id := old.reported_by_profile_id;
    new.reporter_name := old.reporter_name;
    return new;
  end if;

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
  before insert or update on public.student_incidents
  for each row execute function public.sase_stamp_incident_reporter();

alter table public.student_incidents
  alter column reported_by_profile_id set not null;

alter policy student_incidents_insert_by_permission on public.student_incidents
  with check (
    has_permission(institution_id, 'EDIT_INCIDENTS')
    and reported_by_profile_id = (select auth.uid())
  );
