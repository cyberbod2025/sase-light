-- Migración 0029: Movimientos Fechados de Matrícula Escolar y Trazabilidad PMC
--
-- Coordinación de correlativo y orden de dependencias:
--   0016: pre_application_privilege_hardening.sql (última migración en rama main)
--   0017-0027: Ciclo de vida de tokens familiares y endurecimiento de presolicitudes (PR #52 y PR #53, aplicadas en staging)
--   0028: RESERVADA para feat/attendance-class-slice (PR #50 rebase de asistencia por clase)
--   0029: student_dated_school_movements.sql (esta migración para PR #54)
--
-- Estado:
--   - Verificada en vivo sobre instancia efímera local de Supabase (PostgreSQL 17.6 + PostgREST + GoTrue).
--   - PENDIENTE DE APLICACIÓN AUTORIZADA en Supabase Staging remoto (plyjvvpkaafnkxmmqkbh).
--
-- Propósito:
--   Registrar historial fechado de movimientos de matrícula escolar (altas,
--   reincorporaciones, traslados salientes, bajas administrativas, abandonos confirmados,
--   casos pendientes de aclaración y cambios de grupo con fecha efectiva de vigencia).
--   Permite snapshots deterministas al corte para el Programa de Mejora Continua (PMC).
--   Separa formalmente traslados y bajas administrativas del abandono confirmado.

-- Precondiciones fail-fast sobre el esquema base (0002 y 0004)
DO $$
BEGIN
    IF to_regclass('public.institutions') IS NULL THEN
        RAISE EXCEPTION 'SASE_MOVEMENTS_PRECONDITION_FAILED: falta tabla public.institutions';
    END IF;
    IF to_regclass('public.students') IS NULL THEN
        RAISE EXCEPTION 'SASE_MOVEMENTS_PRECONDITION_FAILED: falta tabla public.students';
    END IF;
    IF to_regclass('public.institutional_memberships') IS NULL THEN
        RAISE EXCEPTION 'SASE_MOVEMENTS_PRECONDITION_FAILED: falta tabla public.institutional_memberships';
    END IF;
    IF to_regclass('public.membership_roles') IS NULL THEN
        RAISE EXCEPTION 'SASE_MOVEMENTS_PRECONDITION_FAILED: falta tabla public.membership_roles';
    END IF;
    IF to_regclass('public.roles') IS NULL THEN
        RAISE EXCEPTION 'SASE_MOVEMENTS_PRECONDITION_FAILED: falta tabla public.roles';
    END IF;
    IF to_regprocedure('public.current_membership(uuid)') IS NULL THEN
        RAISE EXCEPTION 'SASE_MOVEMENTS_PRECONDITION_FAILED: falta funcion public.current_membership(uuid)';
    END IF;
END;
$$;

CREATE TABLE IF NOT EXISTS public.student_movement_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES public.institutions(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES public.students(id) ON DELETE CASCADE,
    movement_type TEXT NOT NULL,
    effective_date DATE NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    recorded_by TEXT NOT NULL,
    from_group TEXT NULL,
    to_group TEXT NULL,
    destination_school TEXT NULL,
    departure_category TEXT NULL,
    documentation_source TEXT NULL,
    classification_criteria TEXT NULL,
    is_correction BOOLEAN NOT NULL DEFAULT FALSE,
    supersedes_movement_id UUID NULL REFERENCES public.student_movement_events(id) ON DELETE RESTRICT,
    correction_reason TEXT NULL,
    notes TEXT NULL,

    -- Autoria obligatoria no vacia
    CONSTRAINT check_recorded_by_not_blank CHECK (
        length(btrim(recorded_by)) > 0
    ),

    -- Restricción de tipos de movimiento válidos
    CONSTRAINT valid_movement_type CHECK (
        movement_type IN (
            'INSCRIPCION_INICIAL',
            'ALTA_EXTEMPORANEA',
            'REINCORPORACION',
            'CAMBIO_DE_GRUPO',
            'TRASLADO_SALIENTE',
            'BAJA_ADMINISTRATIVA',
            'ABANDONO_CONFIRMADO',
            'PENDIENTE_ACLARACION'
        )
    ),

    -- Restricción de categorías de salida
    CONSTRAINT valid_departure_category CHECK (
        departure_category IS NULL OR departure_category IN (
            'TRASLADO',
            'BAJA_ADMINISTRATIVA',
            'ABANDONO_CONFIRMADO',
            'PENDIENTE_ACLARACION'
        )
    ),

    -- Separación estricta entre baja administrativa / traslado y abandono confirmado
    CONSTRAINT check_departure_category_consistency CHECK (
        (movement_type = 'TRASLADO_SALIENTE' AND (departure_category IS NULL OR departure_category = 'TRASLADO'))
        OR (movement_type = 'BAJA_ADMINISTRATIVA' AND (departure_category IS NULL OR departure_category = 'BAJA_ADMINISTRATIVA'))
        OR (movement_type = 'ABANDONO_CONFIRMADO' AND (departure_category IS NULL OR departure_category = 'ABANDONO_CONFIRMADO'))
        OR (movement_type = 'PENDIENTE_ACLARACION' AND (departure_category IS NULL OR departure_category = 'PENDIENTE_ACLARACION'))
        OR (movement_type IN ('INSCRIPCION_INICIAL', 'ALTA_EXTEMPORANEA', 'REINCORPORACION', 'CAMBIO_DE_GRUPO') AND departure_category IS NULL)
    ),

    -- Un traslado saliente acreditado exige documentar la escuela receptora
    CONSTRAINT check_transfer_destination CHECK (
        movement_type <> 'TRASLADO_SALIENTE' OR (
            destination_school IS NOT NULL AND length(btrim(destination_school)) > 0
        )
    ),

    -- Un abandono escolar confirmado exige documentar la fuente y el criterio fáctico
    CONSTRAINT check_confirmed_dropout_documentation CHECK (
        movement_type <> 'ABANDONO_CONFIRMADO' OR (
            documentation_source IS NOT NULL AND length(btrim(documentation_source)) > 0 AND
            classification_criteria IS NOT NULL AND length(btrim(classification_criteria)) > 0
        )
    ),

    -- Una baja administrativa exige documentar procedencia y criterio administrativo
    CONSTRAINT check_admin_dropout_documentation CHECK (
        movement_type <> 'BAJA_ADMINISTRATIVA' OR (
            documentation_source IS NOT NULL AND length(btrim(documentation_source)) > 0 AND
            classification_criteria IS NOT NULL AND length(btrim(classification_criteria)) > 0
        )
    ),

    -- Un cambio de grupo exige que origen y destino sean distintos y no vacíos
    CONSTRAINT check_group_change_different CHECK (
        movement_type <> 'CAMBIO_DE_GRUPO' OR (
            from_group IS NOT NULL AND to_group IS NOT NULL AND
            length(btrim(from_group)) > 0 AND length(btrim(to_group)) > 0 AND
            from_group <> to_group
        )
    ),

    -- Una corrección formal exige señalar el movimiento reemplazado y el motivo; un movimiento normal no puede apuntar a supersedes_movement_id
    CONSTRAINT check_correction_fields CHECK (
        (is_correction = FALSE AND supersedes_movement_id IS NULL) OR (
            is_correction = TRUE AND
            supersedes_movement_id IS NOT NULL AND
            correction_reason IS NOT NULL AND length(btrim(correction_reason)) > 0
        )
    )
);

-- Índices de consulta de historial por estudiante y por fecha de corte institucional
CREATE INDEX IF NOT EXISTS idx_student_movement_student_date
    ON public.student_movement_events(student_id, effective_date ASC, recorded_at ASC, id ASC);

CREATE INDEX IF NOT EXISTS idx_student_movement_institution_date
    ON public.student_movement_events(institution_id, effective_date ASC);

-- Prevención de duplicados: un alumno no puede tener dos eventos no-correctivos del mismo tipo en la misma fecha efectiva
CREATE UNIQUE INDEX IF NOT EXISTS idx_student_movement_no_duplicate_event
    ON public.student_movement_events(student_id, movement_type, effective_date)
    WHERE is_correction = FALSE;

-- Prevención de doble reemplazo: un movimiento histórico solo puede ser superado una vez
CREATE UNIQUE INDEX IF NOT EXISTS idx_student_movement_unique_supersedes
    ON public.student_movement_events(supersedes_movement_id)
    WHERE supersedes_movement_id IS NOT NULL;

-- Función evaluadora de autorización RLS (SECURITY DEFINER, mismo patrón que public.has_permission en 0002):
-- Verifica que el actor autenticado tenga membresía activa en la institución y posea el rol SECRETARIA o DIRECCION,
-- sin requerir que el rol authenticated tenga permiso SELECT directo sobre la tabla interna public.membership_roles.
CREATE OR REPLACE FUNCTION public.sase_can_manage_school_movements(p_institution_id UUID)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM public.institutional_memberships m
        JOIN public.membership_roles mr ON mr.membership_id = m.id
        JOIN public.roles r ON r.id = mr.role_id
        WHERE m.profile_id = (SELECT auth.uid())
          AND m.institution_id = p_institution_id
          AND m.active
          AND r.code IN ('SECRETARIA', 'DIRECCION')
    );
$$;

REVOKE EXECUTE ON FUNCTION public.sase_can_manage_school_movements(UUID) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.sase_can_manage_school_movements(UUID) TO authenticated;

-- Trigger de integridad cruzada: valida que el alumno pertenezca a la institución del evento
-- y que cualquier corrección apunte a un movimiento del mismo alumno e institución.
CREATE OR REPLACE FUNCTION public.sase_validate_student_movement_event()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_student_institution_id UUID;
    v_superseded_student_id UUID;
    v_superseded_institution_id UUID;
BEGIN
    IF (SELECT auth.uid()) IS NOT NULL AND NOT public.sase_can_manage_school_movements(NEW.institution_id) THEN
        RAISE EXCEPTION 'new row violates row-level security policy for table "student_movement_events"'
            USING ERRCODE = '42501';
    END IF;

    SELECT s.institution_id
      INTO v_student_institution_id
      FROM public.students s
     WHERE s.id = NEW.student_id;

    IF v_student_institution_id IS NULL THEN
        RAISE EXCEPTION 'SASE_MOVEMENT_STUDENT_NOT_FOUND';
    END IF;

    IF v_student_institution_id <> NEW.institution_id THEN
        RAISE EXCEPTION 'SASE_MOVEMENT_STUDENT_INSTITUTION_MISMATCH';
    END IF;

    IF NEW.supersedes_movement_id IS NOT NULL THEN
        IF NEW.supersedes_movement_id = NEW.id THEN
            RAISE EXCEPTION 'SASE_MOVEMENT_SELF_SUPERSEDE_FORBIDDEN';
        END IF;

        SELECT m.student_id, m.institution_id
          INTO v_superseded_student_id, v_superseded_institution_id
          FROM public.student_movement_events m
         WHERE m.id = NEW.supersedes_movement_id;

        IF v_superseded_student_id IS NULL THEN
            RAISE EXCEPTION 'SASE_MOVEMENT_SUPERSEDED_NOT_FOUND';
        END IF;

        IF v_superseded_student_id <> NEW.student_id OR v_superseded_institution_id <> NEW.institution_id THEN
            RAISE EXCEPTION 'SASE_MOVEMENT_SUPERSEDES_MISMATCH';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.sase_validate_student_movement_event() FROM public, anon, authenticated;

DROP TRIGGER IF EXISTS trg_validate_student_movement_event ON public.student_movement_events;
CREATE TRIGGER trg_validate_student_movement_event
    BEFORE INSERT OR UPDATE ON public.student_movement_events
    FOR EACH ROW
    EXECUTE FUNCTION public.sase_validate_student_movement_event();

-- Seguridad RLS (Row Level Security)
ALTER TABLE public.student_movement_events ENABLE ROW LEVEL SECURITY;

-- Lectura del detalle nominal de movimientos: restringida a Secretaría y Dirección con membresía activa en la institución
DROP POLICY IF EXISTS "movement_events_read_staff" ON public.student_movement_events;
CREATE POLICY "movement_events_read_staff" ON public.student_movement_events
    FOR SELECT
    TO authenticated
    USING (public.sase_can_manage_school_movements(institution_id));

-- Inserción de movimientos: restringida a Secretaría y Dirección con membresía activa en la institución
DROP POLICY IF EXISTS "movement_events_modify_staff" ON public.student_movement_events;
CREATE POLICY "movement_events_modify_staff" ON public.student_movement_events
    FOR INSERT
    TO authenticated
    WITH CHECK (public.sase_can_manage_school_movements(institution_id));

-- Restricción de privilegios: anon no tiene acceso; authenticated solo tiene SELECT e INSERT bajo RLS (historial inmutable sin UPDATE/DELETE)
REVOKE ALL ON TABLE public.student_movement_events FROM public, anon, authenticated;
GRANT SELECT, INSERT ON TABLE public.student_movement_events TO authenticated;

-- Procedimiento de reversión (Rollback documentado)
-- Para revertir esta migración en base de datos:
-- DROP TRIGGER IF EXISTS trg_validate_student_movement_event ON public.student_movement_events;
-- DROP FUNCTION IF EXISTS public.sase_validate_student_movement_event();
-- DROP POLICY IF EXISTS "movement_events_modify_staff" ON public.student_movement_events;
-- DROP POLICY IF EXISTS "movement_events_read_staff" ON public.student_movement_events;
-- DROP FUNCTION IF EXISTS public.sase_can_manage_school_movements(UUID);
-- DROP TABLE IF EXISTS public.student_movement_events CASCADE;
