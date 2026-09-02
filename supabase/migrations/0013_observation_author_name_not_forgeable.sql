-- SASE-310 — Cierre de PR #49: resuelve P1 de la tercera revision de Codex
-- sobre e5f5b89 ("Derive observation author names server-side").
--
-- REGRESION DE ALCANCE: 0008 agrego `author_name` a `student_observations`
-- como columna denormalizada (mismo patron que `student_incidents.reporter_name`
-- en 0005/0007), pero a diferencia de las incidencias, esta columna nunca
-- tuvo un trigger de servidor que la derive -- queda enteramente controlada
-- por el cliente. La politica de insercion (0005) ya exige
-- `author_profile_id = auth.uid()`, pero eso no impide que ese mismo actor
-- envie un `author_name` distinto al suyo: cualquier miembro con
-- EDIT_STUDENT_IDENTITY puede insertar una observacion inmutable atribuida
-- (en el nombre mostrado) a otro miembro del staff.
--
-- SOLUCION: mismo patron que 0012 para incidencias -- un trigger BEFORE
-- INSERT ignora `author_name` del cliente y lo reemplaza siempre por
-- `profiles.full_name` del actor autenticado (ya validado como
-- `author_profile_id` por la politica existente). Solo BEFORE INSERT: las
-- observaciones son historial inmutable, sin politica de UPDATE (0005 lo
-- deja asi a proposito), asi que no hay una segunda superficie que congelar.
--
-- ESTADO: APLICADA al proyecto remoto "SASE-Light" (plyjvvpkaafnkxmmqkbh)
-- el 2026-08-12 con autorizacion explicita de Hugo. Verificado tras
-- aplicarla: intento de insertar con author_name falso (author_profile_id
-- correcto) quedo sobrescrito por el nombre real del actor; intento con
-- author_profile_id ajeno rechazado por la politica RLS existente
-- (violacion de student_observations_insert_by_permission).

create or replace function public.sase_stamp_observation_author_name()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_full_name text;
begin
  select full_name into v_full_name from public.profiles where id = new.author_profile_id;
  new.author_name := coalesce(v_full_name, '');
  return new;
end;
$$;

create trigger trg_student_observations_stamp_author_name
  before insert on public.student_observations
  for each row execute function public.sase_stamp_observation_author_name();
