-- SASE-310 — Datos de referencia: institucion, roles y permisos, y su matriz
-- rol->permiso. Espejo exacto de StaffPermissions.matrix en Kotlin
-- (composeApp/src/commonMain/kotlin/com/example/data/auth/StaffPermissions.kt),
-- para que el servidor sea la fuente de verdad y el cliente deje de tener una
-- matriz hardcodeada independiente.
--
-- ESTADO: escrito pero NO aplicado contra el proyecto remoto "SASE-Light"
-- (plyjvvpkaafnkxmmqkbh) en esta sesion — la mutacion de base de datos en vivo
-- fue bloqueada por la politica de seguridad de la sesion (auto-mode classifier),
-- incluso para datos de referencia sin credenciales. Aplicar manualmente con
-- el MCP de Supabase (apply_migration) o `supabase db push` cuando se autorice.
--
-- Deliberadamente NO se seedan cuentas de personal (auth.users/auth.identities)
-- aqui ni en ningun otro archivo: crear cuentas y contrasenas, incluso de demo,
-- esta fuera de lo que este asistente puede hacer por si mismo. Las 7 cuentas
-- demo (direccion@example.invalid ... baja@example.invalid, contrasena
-- demo1234, mismo patron que MockStaffDirectory) deben crearse manualmente via
-- Supabase Studio (Authentication > Users > Add user) o el flujo de signUp de
-- la app; despues, cada una necesita una fila en institutional_memberships +
-- membership_roles apuntando al rol correspondiente de esta migracion.

insert into public.institutions (id, name)
values ('11111111-1111-1111-1111-111111111111', 'Secundaria 310')
on conflict (name) do nothing;

insert into public.roles (code, description) values
  ('DIRECCION', 'Direccion escolar'),
  ('SECRETARIA', 'Secretaria escolar'),
  ('TRABAJO_SOCIAL', 'Trabajo Social'),
  ('MEDICO_ESCOLAR', 'Medico Escolar'),
  ('UDEII', 'Unidad de Educacion Inclusiva'),
  ('DOCENTE', 'Docente')
on conflict (code) do nothing;

insert into public.permissions (code, description) values
  ('PRE_SOLICITUD', 'Revision de pre-solicitudes'),
  ('SECRETARIA', 'Panel de secretaria'),
  ('ALTA_OFICIAL', 'Inscripcion / alta oficial'),
  ('EXPEDIENTE', 'Expediente institucional del alumno'),
  ('CREDENCIAL', 'Credencial del alumno'),
  ('INDICADORES', 'Indicadores institucionales (Direccion)'),
  ('TRABAJO_SOCIAL', 'Area de Trabajo Social'),
  ('SALUD', 'Area de salud / enfermeria'),
  ('UDEII', 'Area UDEII'),
  ('MANAGE_USERS', 'Alta/baja de personal institucional'),
  ('MANAGE_ROLES', 'Asignacion de roles y alcances')
on conflict (code) do nothing;

-- Debe coincidir exactamente con StaffPermissions.matrix; si se edita alla,
-- editar tambien aqui (o mejor: una vez aplicado, el cliente deberia leer esta
-- tabla en vez de mantener el mapa hardcodeado — ver AuthRepository real).
insert into public.role_permissions (role_id, permission_id)
select r.id, p.id from public.roles r join public.permissions p on true
where (r.code, p.code) in (
  ('DIRECCION','INDICADORES'), ('DIRECCION','EXPEDIENTE'), ('DIRECCION','SECRETARIA'),
  ('DIRECCION','ALTA_OFICIAL'), ('DIRECCION','CREDENCIAL'),
  ('DIRECCION','MANAGE_USERS'), ('DIRECCION','MANAGE_ROLES'),
  ('SECRETARIA','PRE_SOLICITUD'), ('SECRETARIA','SECRETARIA'), ('SECRETARIA','ALTA_OFICIAL'),
  ('SECRETARIA','EXPEDIENTE'), ('SECRETARIA','CREDENCIAL'),
  ('TRABAJO_SOCIAL','EXPEDIENTE'), ('TRABAJO_SOCIAL','TRABAJO_SOCIAL'),
  ('MEDICO_ESCOLAR','SALUD'),
  ('UDEII','EXPEDIENTE'), ('UDEII','UDEII'),
  ('DOCENTE','EXPEDIENTE')
)
on conflict (role_id, permission_id) do nothing;
