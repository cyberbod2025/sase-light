-- SASE-310 — ventana de transicion completa para tokens anteriores a 0017.
--
-- Hallazgo real de revision de codigo (Codex, PR #52, P1): el backfill de
-- 0017 fijo `access_token_expires_at = created_at + 30 dias` para las filas
-- existentes. Un token de mas de 30 dias quedo vencido en el mismo instante
-- en que se aplico la migracion, y uno de 25 dias recibio solo 5 de aviso.
-- GET, UPDATE y ROTATE exigen `access_token_expires_at > now()`, asi que
-- esas familias quedaron fuera sin haber hecho nada, conservando una
-- credencial que hasta ese momento era valida. Verificado en staging al
-- recibir el hallazgo: las 3 filas existentes (creadas desde 2026-08-24)
-- estaban vencidas y activas.
--
-- 0017 ya esta aplicada, asi que no se reescribe: esta migracion corrige
-- el estado resultante. Las filas de 0017 en adelante conservan su
-- vencimiento relativo a la creacion (es el default de la columna y no se
-- toca).
--
-- Alcance exacto: solo filas creadas ANTES de aplicar 0017 (corte fijo =
-- version 20260926131601 de la migracion en staging), cuyo token nunca se
-- roto, no esta revocado y no esta cancelada, y cuya ventana actual es
-- menor a 30 dias. Se les da 30 dias completos desde ahora. Es idempotente
-- y no resucita tokens revocados ni cancelados.
--
-- ESTADO: escrita y aplicada a staging en la misma sesion que la escribe.

update public.pre_applications
set access_token_expires_at = now() + interval '30 days'
where created_at < timestamptz '2026-09-26 13:16:01+00'
  and access_token_rotated_at is null
  and access_token_revoked_at is null
  and status <> 'CANCELADA'
  and access_token_expires_at < now() + interval '30 days';
