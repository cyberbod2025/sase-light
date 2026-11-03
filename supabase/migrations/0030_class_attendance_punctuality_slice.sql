-- Migración 0030: Rebanada Vertical de Asistencia y Puntualidad (PMC)
--
-- Coordinación de correlativo y orden de dependencias:
--   0016: pre_application_privilege_hardening.sql (última migración en rama main)
--   0017-0027: Ciclo de vida de tokens familiares (PR #52 y #53)
--   0028: RESERVADA para histórico de asistencia por clase
--   0029: student_dated_school_movements.sql (movimientos fechados de matrícula en PR #54)
--   0030: class_attendance_punctuality_slice.sql (esta migración para feat/pmc-attendance-punctuality-slice)
--
-- Estado:
--   - Preparada para validación con Docker/PostgreSQL local.
--   - PENDIENTE DE APLICACIÓN AUTORIZADA en Supabase Staging remoto (plyjvvpkaafnkxmmqkbh).
--
-- Principios de Dominio Institucional:
--   1. Módulos dobles: unicidad por (group_id, membership_id, session_date, module_number).
--   2. Estatus operativo de sesión: PROGRAMADA, CANCELADA, NO_LECTIVA.
--   3. Presencia física diferenciada de puntualidad: PRESENTE / AUSENTE.
--   4. Retardo como atributo exclusivo de una persona PRESENTE con minutos de retardo.
--   5. Captura parcial o completa atómica mediante RPC SECURITY DEFINER.
--   6. Aislamiento institucional estricto y verificación de asignación docente.

-- === Precondiciones fail-fast ================================================
DO $$
BEGIN
    IF to_regclass('public.institutions') IS NULL THEN
        RAISE EXCEPTION 'SASE_ATTENDANCE_PRECONDITION_FAILED: falta tabla public.institutions';
    END IF;
    IF to_regclass('public.institutional_memberships') IS NULL THEN
        RAISE EXCEPTION 'SASE_ATTENDANCE_PRECONDITION_FAILED: falta tabla public.institutional_memberships';
    END IF;
    IF to_regclass('public.roles') IS NULL THEN
        RAISE EXCEPTION 'SASE_ATTENDANCE_PRECONDITION_FAILED: falta tabla public.roles';
    END IF;
    IF to_regprocedure('public.current_membership(uuid)') IS NULL THEN
        RAISE EXCEPTION 'SASE_ATTENDANCE_PRECONDITION_FAILED: falta funcion public.current_membership(uuid)';
    END IF;
END;
$$;

-- === Tipos Enumerados =======================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'session_operational_status') THEN
        CREATE TYPE public.session_operational_status AS ENUM ('PROGRAMADA', 'CANCELADA', 'NO_LECTIVA');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'attendance_presence_status') THEN
        CREATE TYPE public.attendance_presence_status AS ENUM ('PRESENTE', 'AUSENTE');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'attendance_punctuality_status') THEN
        CREATE TYPE public.attendance_punctuality_status AS ENUM ('A_TIEMPO', 'RETARDO', 'NO_APLICA');
    END IF;
END;
$$;

-- === Tablas =================================================================

CREATE TABLE IF NOT EXISTS public.school_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES public.institutions(id) ON DELETE CASCADE,
    school_cycle_id TEXT NOT NULL,
    name TEXT NOT NULL,
    grade_label TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE (institution_id, school_cycle_id, name)
);

CREATE TABLE IF NOT EXISTS public.group_students (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES public.school_groups(id) ON DELETE CASCADE,
    student_id UUID REFERENCES public.students(id) ON DELETE SET NULL,
    full_name TEXT NOT NULL,
    list_number INTEGER NOT NULL CHECK (list_number > 0),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE (group_id, list_number)
);

CREATE TABLE IF NOT EXISTS public.group_teacher_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES public.school_groups(id) ON DELETE CASCADE,
    membership_id UUID NOT NULL REFERENCES public.institutional_memberships(id) ON DELETE CASCADE,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE (group_id, membership_id)
);

CREATE TABLE IF NOT EXISTS public.class_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES public.school_groups(id) ON DELETE CASCADE,
    membership_id UUID NOT NULL REFERENCES public.institutional_memberships(id) ON DELETE CASCADE,
    session_date DATE NOT NULL,
    module_number INTEGER NOT NULL DEFAULT 1 CHECK (module_number BETWEEN 1 AND 8),
    session_status public.session_operational_status NOT NULL DEFAULT 'PROGRAMADA',
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE (group_id, membership_id, session_date, module_number)
);

CREATE TABLE IF NOT EXISTS public.class_attendance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_session_id UUID NOT NULL REFERENCES public.class_sessions(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES public.group_students(id) ON DELETE CASCADE,
    presence public.attendance_presence_status NOT NULL,
    punctuality public.attendance_punctuality_status NOT NULL DEFAULT 'A_TIEMPO',
    minutes_late INTEGER NOT NULL DEFAULT 0 CHECK (minutes_late >= 0),
    justification TEXT,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE (class_session_id, student_id),
    CHECK (
        (presence = 'AUSENTE' AND punctuality = 'NO_APLICA') OR
        (presence = 'PRESENTE' AND punctuality IN ('A_TIEMPO', 'RETARDO'))
    )
);

-- === Índices =================================================================
CREATE INDEX IF NOT EXISTS idx_school_groups_inst ON public.school_groups(institution_id, school_cycle_id);
CREATE INDEX IF NOT EXISTS idx_group_students_group ON public.group_students(group_id, active);
CREATE INDEX IF NOT EXISTS idx_group_teacher_assignments_m ON public.group_teacher_assignments(membership_id, active);
CREATE INDEX IF NOT EXISTS idx_class_sessions_group_date_mod ON public.class_sessions(group_id, session_date, module_number);
CREATE INDEX IF NOT EXISTS idx_class_attendance_session ON public.class_attendance(class_session_id);

-- === Habilitación de RLS =====================================================
ALTER TABLE public.school_groups ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.group_students ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.group_teacher_assignments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.class_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.class_attendance ENABLE ROW LEVEL SECURITY;

-- === Funciones de Autorización (SECURITY DEFINER) =============================

CREATE OR REPLACE FUNCTION public.sase_actor_can_manage_attendance_for_group(p_group_id UUID)
RETURNS UUID
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = ''
AS $$
    SELECT m.id
    FROM public.institutional_memberships m
    JOIN public.school_groups g ON g.id = p_group_id AND g.institution_id = m.institution_id
    JOIN public.membership_roles mr ON mr.membership_id = m.id
    JOIN public.roles r ON r.id = mr.role_id
    WHERE m.user_id = auth.uid()
      AND m.active = true
      AND (
          (r.code = 'DOCENTE' AND EXISTS (
              SELECT 1 FROM public.group_teacher_assignments a
              WHERE a.group_id = p_group_id AND a.membership_id = m.id AND a.active = true
          ))
          OR r.code IN ('DIRECCION', 'SECRETARIA', 'CONTROL_ESCOLAR')
      )
    LIMIT 1;
$$;

-- === Políticas de Lectura ====================================================

DROP POLICY IF EXISTS school_groups_select ON public.school_groups;
CREATE POLICY school_groups_select ON public.school_groups
    FOR SELECT TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.institutional_memberships m
            WHERE m.institution_id = school_groups.institution_id
              AND m.user_id = auth.uid()
              AND m.active = true
        )
    );

DROP POLICY IF EXISTS group_students_select ON public.group_students;
CREATE POLICY group_students_select ON public.group_students
    FOR SELECT TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.school_groups g
            JOIN public.institutional_memberships m ON m.institution_id = g.institution_id
            WHERE g.id = group_students.group_id
              AND m.user_id = auth.uid()
              AND m.active = true
        )
    );

DROP POLICY IF EXISTS class_sessions_select ON public.class_sessions;
CREATE POLICY class_sessions_select ON public.class_sessions
    FOR SELECT TO authenticated
    USING (public.sase_actor_can_manage_attendance_for_group(group_id) IS NOT NULL);

DROP POLICY IF EXISTS class_attendance_select ON public.class_attendance;
CREATE POLICY class_attendance_select ON public.class_attendance
    FOR SELECT TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.class_sessions s
            WHERE s.id = class_attendance.class_session_id
              AND public.sase_actor_can_manage_attendance_for_group(s.group_id) IS NOT NULL
        )
    );

-- === Funciones RPC Operativas ================================================

CREATE OR REPLACE FUNCTION public.sase_class_session_snapshot(p_session_id UUID)
RETURNS JSONB
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = ''
AS $$
DECLARE
    v_session RECORD;
    v_roster JSONB;
BEGIN
    SELECT s.id, s.group_id, g.name AS group_name, g.grade_label, s.session_date,
           s.module_number, s.session_status, s.notes
    INTO v_session
    FROM public.class_sessions s
    JOIN public.school_groups g ON g.id = s.group_id
    WHERE s.id = p_session_id;

    IF v_session.id IS NULL THEN
        RAISE EXCEPTION 'SASE_ATTENDANCE_NOT_FOUND' USING errcode = 'P0002';
    END IF;

    SELECT COALESCE(
        jsonb_agg(
            jsonb_build_object(
                'student_id', st.id,
                'full_name', st.full_name,
                'list_number', st.list_number,
                'presence', att.presence,
                'punctuality', att.punctuality,
                'minutes_late', COALESCE(att.minutes_late, 0),
                'justification', att.justification,
                'recorded', (att.presence IS NOT NULL)
            ) ORDER BY st.list_number
        ),
        '[]'::jsonb
    )
    INTO v_roster
    FROM public.group_students st
    LEFT JOIN public.class_attendance att
        ON att.class_session_id = p_session_id AND att.student_id = st.id
    WHERE st.group_id = v_session.group_id AND st.active = true;

    RETURN jsonb_build_object(
        'session_id', v_session.id,
        'group_id', v_session.group_id,
        'group_name', v_session.group_name,
        'grade_label', v_session.grade_label,
        'session_date', v_session.session_date::text,
        'module_number', v_session.module_number,
        'session_status', v_session.session_status::text,
        'notes', v_session.notes,
        'roster', v_roster
    );
END;
$$;

CREATE OR REPLACE FUNCTION public.sase_open_or_schedule_class_session(
    p_group_id UUID,
    p_date DATE,
    p_module_number INTEGER DEFAULT 1,
    p_status TEXT DEFAULT 'PROGRAMADA',
    p_notes TEXT DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql SECURITY DEFINER SET search_path = ''
AS $$
DECLARE
    v_membership UUID;
    v_session_id UUID;
    v_status public.session_operational_status;
BEGIN
    IF p_group_id IS NULL OR p_date IS NULL OR p_module_number NOT BETWEEN 1 AND 8 THEN
        RAISE EXCEPTION 'SASE_ATTENDANCE_INVALID_ARGS' USING errcode = '22023';
    END IF;

    v_membership := public.sase_actor_can_manage_attendance_for_group(p_group_id);
    IF v_membership IS NULL THEN
        RAISE EXCEPTION 'SASE_ATTENDANCE_FORBIDDEN' USING errcode = '42501';
    END IF;

    BEGIN
        v_status := p_status::public.session_operational_status;
    EXCEPTION WHEN invalid_text_representation THEN
        v_status := 'PROGRAMADA'::public.session_operational_status;
    END;

    SELECT id INTO v_session_id
    FROM public.class_sessions
    WHERE group_id = p_group_id
      AND membership_id = v_membership
      AND session_date = p_date
      AND module_number = p_module_number;

    IF v_session_id IS NULL THEN
        INSERT INTO public.class_sessions (group_id, membership_id, session_date, module_number, session_status, notes)
        VALUES (p_group_id, v_membership, p_date, p_module_number, v_status, p_notes)
        RETURNING id INTO v_session_id;
    END IF;

    RETURN public.sase_class_session_snapshot(v_session_id);
END;
$$;

CREATE OR REPLACE FUNCTION public.sase_update_class_session_status(
    p_session_id UUID,
    p_status TEXT,
    p_notes TEXT DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql SECURITY DEFINER SET search_path = ''
AS $$
DECLARE
    v_group_id UUID;
    v_status public.session_operational_status;
BEGIN
    SELECT group_id INTO v_group_id
    FROM public.class_sessions
    WHERE id = p_session_id;

    IF v_group_id IS NULL THEN
        RAISE EXCEPTION 'SASE_ATTENDANCE_NOT_FOUND' USING errcode = 'P0002';
    END IF;

    IF public.sase_actor_can_manage_attendance_for_group(v_group_id) IS NULL THEN
        RAISE EXCEPTION 'SASE_ATTENDANCE_FORBIDDEN' USING errcode = '42501';
    END IF;

    v_status := p_status::public.session_operational_status;

    UPDATE public.class_sessions
    SET session_status = v_status,
        notes = COALESCE(p_notes, notes),
        updated_at = timezone('utc'::text, now())
    WHERE id = p_session_id;

    RETURN public.sase_class_session_snapshot(p_session_id);
END;
$$;

CREATE OR REPLACE FUNCTION public.sase_save_class_attendance(
    p_session_id UUID,
    p_entries JSONB
)
RETURNS JSONB
LANGUAGE plpgsql SECURITY DEFINER SET search_path = ''
AS $$
DECLARE
    v_group_id UUID;
    v_entry RECORD;
    v_presence public.attendance_presence_status;
    v_punctuality public.attendance_punctuality_status;
    v_student_id UUID;
    v_minutes_late INTEGER;
    v_justification TEXT;
BEGIN
    IF p_session_id IS NULL OR p_entries IS NULL OR jsonb_typeof(p_entries) <> 'array' THEN
        RAISE EXCEPTION 'SASE_ATTENDANCE_INVALID_ARGS' USING errcode = '22023';
    END IF;

    SELECT group_id INTO v_group_id
    FROM public.class_sessions
    WHERE id = p_session_id;

    IF v_group_id IS NULL THEN
        RAISE EXCEPTION 'SASE_ATTENDANCE_NOT_FOUND' USING errcode = 'P0002';
    END IF;

    IF public.sase_actor_can_manage_attendance_for_group(v_group_id) IS NULL THEN
        RAISE EXCEPTION 'SASE_ATTENDANCE_FORBIDDEN' USING errcode = '42501';
    END IF;

    FOR v_entry IN SELECT * FROM jsonb_array_elements(p_entries) LOOP
        v_student_id := (v_entry.value ->> 'student_id')::UUID;
        
        IF (v_entry.value ->> 'presence') IS NOT NULL THEN
            v_presence := (v_entry.value ->> 'presence')::public.attendance_presence_status;
            v_punctuality := COALESCE((v_entry.value ->> 'punctuality')::public.attendance_punctuality_status, 
                                      CASE WHEN v_presence = 'PRESENTE' THEN 'A_TIEMPO'::public.attendance_punctuality_status ELSE 'NO_APLICA'::public.attendance_punctuality_status END);
            v_minutes_late := COALESCE((v_entry.value ->> 'minutes_late')::INTEGER, 0);
            v_justification := v_entry.value ->> 'justification';

            -- Validar coherencia: ausente no puede tener retardo
            IF v_presence = 'AUSENTE' THEN
                v_punctuality := 'NO_APLICA';
                v_minutes_late := 0;
            END IF;

            INSERT INTO public.class_attendance (class_session_id, student_id, presence, punctuality, minutes_late, justification, recorded_at)
            VALUES (p_session_id, v_student_id, v_presence, v_punctuality, v_minutes_late, v_justification, timezone('utc'::text, now()))
            ON CONFLICT (class_session_id, student_id) DO UPDATE
            SET presence = EXCLUDED.presence,
                punctuality = EXCLUDED.punctuality,
                minutes_late = EXCLUDED.minutes_late,
                justification = EXCLUDED.justification,
                recorded_at = timezone('utc'::text, now());
        END IF;
    END LOOP;

    UPDATE public.class_sessions
    SET updated_at = timezone('utc'::text, now())
    WHERE id = p_session_id;

    RETURN public.sase_class_session_snapshot(p_session_id);
END;
$$;

-- === Privilegios de ejecución ================================================
REVOKE ALL ON FUNCTION public.sase_actor_can_manage_attendance_for_group(UUID) FROM public, anon, authenticated;
REVOKE ALL ON FUNCTION public.sase_class_session_snapshot(UUID) FROM public, anon, authenticated;
REVOKE ALL ON FUNCTION public.sase_open_or_schedule_class_session(UUID, DATE, INTEGER, TEXT, TEXT) FROM public, anon, authenticated;
REVOKE ALL ON FUNCTION public.sase_update_class_session_status(UUID, TEXT, TEXT) FROM public, anon, authenticated;
REVOKE ALL ON FUNCTION public.sase_save_class_attendance(UUID, JSONB) FROM public, anon, authenticated;

GRANT EXECUTE ON FUNCTION public.sase_actor_can_manage_attendance_for_group(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.sase_open_or_schedule_class_session(UUID, DATE, INTEGER, TEXT, TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.sase_update_class_session_status(UUID, TEXT, TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.sase_save_class_attendance(UUID, JSONB) TO authenticated;
