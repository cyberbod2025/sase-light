-- Migración 0029: Movimientos Fechados de Matrícula Escolar y Trazabilidad PMC
--
-- Coordinación de correlativo y orden de dependencias:
--   0016: pre_application_privilege_hardening.sql (última migración en rama main)
--   0017-0027: Ciclo de vida de tokens familiares y endurecimiento de presolicitudes (PR #52 y PR #53, aplicadas en staging)
--   0028: RESERVADA para feat/attendance-class-slice (PR #50 rebase de asistencia por clase)
--   0029: student_dated_school_movements.sql (esta migración para PR #54)
--
-- Estado en Supabase Staging: PENDIENTE DE APLICACIÓN AUTORIZADA
-- Propósito:
--   Registrar historial fechado de movimientos de matrícula escolar (altas,
--   reincorporaciones, traslados salientes, bajas administrativas, abandonos confirmados
--   y cambios de grupo con fecha efectiva de vigencia).
--   Permite snapshots deterministas al corte para el Programa de Mejora Continua (PMC).
--   Separa formalmente traslados y bajas administrativas del abandono confirmado.

CREATE TABLE IF NOT EXISTS public.student_movement_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id TEXT NOT NULL,
    student_id TEXT NOT NULL REFERENCES public.students(id) ON DELETE CASCADE,
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
    supersedes_movement_id UUID NULL REFERENCES public.student_movement_events(id) ON DELETE SET NULL,
    correction_reason TEXT NULL,
    notes TEXT NULL,

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

    -- Un traslado saliente acreditado exige documentar la escuela receptora
    CONSTRAINT check_transfer_destination CHECK (
        movement_type <> 'TRASLADO_SALIENTE' OR (
            destination_school IS NOT NULL AND length(trim(destination_school)) > 0
        )
    ),

    -- Un abandono escolar confirmado exige documentar la fuente y el criterio fáctico
    CONSTRAINT check_confirmed_dropout_documentation CHECK (
        movement_type <> 'ABANDONO_CONFIRMADO' OR (
            documentation_source IS NOT NULL AND length(trim(documentation_source)) > 0 AND
            classification_criteria IS NOT NULL AND length(trim(classification_criteria)) > 0
        )
    ),

    -- Un cambio de grupo exige que origen y destino sean distintos
    CONSTRAINT check_group_change_different CHECK (
        movement_type <> 'CAMBIO_DE_GRUPO' OR (
            from_group IS NOT NULL AND to_group IS NOT NULL AND from_group <> to_group
        )
    ),

    -- Una corrección formal exige señalar el movimiento reemplazado y el motivo
    CONSTRAINT check_correction_fields CHECK (
        is_correction = FALSE OR (
            supersedes_movement_id IS NOT NULL AND
            correction_reason IS NOT NULL AND length(trim(correction_reason)) > 0
        )
    )
);

-- Índices de consulta de historial por estudiante y por fecha de corte institucional
CREATE INDEX IF NOT EXISTS idx_student_movement_student_date
    ON public.student_movement_events(student_id, effective_date ASC, recorded_at ASC);

CREATE INDEX IF NOT EXISTS idx_student_movement_institution_date
    ON public.student_movement_events(institution_id, effective_date ASC);

CREATE INDEX IF NOT EXISTS idx_student_movement_superseded
    ON public.student_movement_events(supersedes_movement_id)
    WHERE supersedes_movement_id IS NOT NULL;

-- Seguridad RLS (Row Level Security)
ALTER TABLE public.student_movement_events ENABLE ROW LEVEL SECURITY;

-- Lectura: Personal con membresía activa en la institución
CREATE POLICY "movement_events_read_staff" ON public.student_movement_events
    FOR SELECT
    TO authenticated
    USING (
        institution_id IN (
            SELECT institution_id
            FROM public.staff_memberships
            WHERE user_id = auth.uid()
        )
    );

-- Modificación (Inserción): Restringida a personal de Secretaría y Dirección
CREATE POLICY "movement_events_modify_staff" ON public.student_movement_events
    FOR INSERT
    TO authenticated
    WITH CHECK (
        institution_id IN (
            SELECT institution_id
            FROM public.staff_memberships
            WHERE user_id = auth.uid()
              AND role IN ('SECRETARIA', 'DIRECCION')
        )
    );

-- Restricción de privilegios: anon no tiene acceso; authenticated opera bajo RLS
REVOKE ALL ON public.student_movement_events FROM public, anon;
GRANT SELECT, INSERT ON public.student_movement_events TO authenticated;

-- Procedimiento de reversión (Rollback documentado)
-- Para revertir esta migración en base de datos:
-- DROP POLICY IF EXISTS "movement_events_modify_staff" ON public.student_movement_events;
-- DROP POLICY IF EXISTS "movement_events_read_staff" ON public.student_movement_events;
-- DROP TABLE IF EXISTS public.student_movement_events CASCADE;
