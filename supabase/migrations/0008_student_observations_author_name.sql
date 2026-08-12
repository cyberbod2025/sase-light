-- SASE-310 — correccion menor de 0005: `student_observations` guarda
-- `author_profile_id` (uuid) pero el modelo del cliente (`SaseObservation.author`)
-- necesita un nombre para mostrar sin tener que unir contra `profiles` en
-- cada lectura. `student_incidents` ya denormaliza esto (`reporter_name`);
-- se alinea el mismo patron aqui. Tabla vacia (0 filas, verificado antes de
-- este cambio): ALTER TABLE aditivo, no destructivo.

alter table public.student_observations
  add column author_name text not null default '';
