# 05 — Modelos y datos actuales

---
**Versión de contexto:** 0.1  
**Commit base:** 4f4bc84  
**Fecha de generación:** 2026-06-30  
**Uso previsto:** contexto para agentes IA externos  
**Datos sensibles:** no incluidos intencionalmente  
---

## Convenciones

- ✅ **Mock**: Datos simulados en memoria, sin persistencia
- ✅ **Real (estructural)**: Clase/modelo existente en el código
- ✅ **Proyección**: Computada desde otro modelo, no persistida
- ⏳ **Placeholder**: Existe estructuralmente pero sin implementación funcional
- ❌ **No implementado**: No existe en el código

---

## Modelo 1: `Student` (expediente maestro)

**Archivo**: `composeApp/.../data/SaseEntities.kt`
**Tipo**: `data class` — Real (estructural)
**Persistencia**: ✅ Mock (`MockSaseData`)
**Propósito**: Es el expediente maestro del alumno. Se crea durante la alta oficial.

| Campo | Tipo | Origen | Notas |
|-------|------|--------|-------|
| `id` | String | Generado | ID interno |
| `fullName` | String | Propagado | Desde pre-solicitud |
| `group` | String | Asignado | Ej: "2°B" |
| `enrollmentId` | String | Generado | Matrícula oficial |
| `curp` | String | Propagado | Normalizado a mayúsculas |
| `shift` | String | Default | "Vespertino" |
| `schoolYear` | String | Propagado | Ej: "2023-2024" |
| `status` | String | Default | "Activo" |
| `riskLevel` | String | Default | "Bajo" |
| `bap` | String | Default | "No" |
| `schoolInsurance` | String | Default | "Vigente" |
| `documentationStatus` | String | Default | "Completa" |
| `birthDate` | String | Propagado | |
| `age` | Int | Default | 14 |
| `birthPlace` | String | Default | |
| `address` | String | Default | ⚠️ Contiene datos de domicilio mock |
| `zipCode` | String | Default | |
| `tutorName` | String | Propagado | |
| `tutorRelation` | String | Propagado | |
| `tutorPhone` | String | Propagado | ⚠️ Teléfono mock |
| `tutorEmail` | String | Propagado | |
| `attendancePercent` | Int | Default | |
| `attendances` | Int | Default | |
| `excusedAbsences` | Int | Default | |
| `unexcusedAbsences` | Int | Default | |
| `healthAlergies` | String | Default | ⚠️ Contiene datos médicos mock |
| `healthNotes` | String | Default | |
| `healthMeds` | String | Default | |
| `healthPasses` | String | Default | |
| `orientationStatus` | String | Default | |
| `orientationLastAppointment` | String | Default | |
| `orientationInterventionPlan` | String | Default | |
| `orientationResponsible` | String | Default | |
| `documents` | List<SaseDocument> | Mock | Documentos del expediente |
| `observations` | List<SaseObservation> | Mock | Observaciones académicas |
| `schoolIncidents` | List<SaseIncident> | Mock | Incidentes escolares |
| `audits` | List<SaseAudit> | Mock | Trazabilidad de acciones |
| `photoUrl` | String? | Placeholder | No hay captura real |
| `preApplicationFolio` | String? | Propagado | Vínculo con pre-solicitud |

---

## Modelo 2: `PreApplication` (pre-solicitud familiar)

**Archivo**: `composeApp/.../data/presolicitud/PreApplicationModels.kt`
**Tipo**: `data class` — Real (estructural)
**Persistencia**: ✅ Mock (`MockPreApplicationData`)
**Propósito**: Almacena la solicitud inicial enviada por la familia.

**Bloques de datos**:
- A: Pre-solicitud inicial (trámite, ciclo, grado)
- B: Datos del alumno (nombre, CURP, fecha nacimiento, sexo, nacionalidad, entidad, domicilio, teléfono, escuela procedencia)
- C: Responsables (lista de `Responsable`)
- D: Autorizados para recoger (lista de `AutorizadoPreSolicitud`)
- E: Ficha médica familiar (`FichaMedicaFamiliar`)
- F: Contexto sociofamiliar (`ContextoSociofamiliar`)
- G: Antecedentes UDEII (`AntecedentesUdeii`)
- H: Documentos declarados (`List<DocumentoDeclarado>`)
- I: Consentimientos (`ConsentimientosFamiliares`)
- J: Revisión de Secretaría (observaciones, readiness)

**Estados**: `BORRADOR` → `ENVIADA` → `PENDIENTE_CORRECCION` / `ACEPTADA` / `DUPLICADA` / `CANCELADA`

---

## Modelo 3: `OfficialStudent` (alumno oficial)

**Archivo**: `composeApp/.../data/presolicitud/OfficialStudentModels.kt`
**Tipo**: `data class` — Real (estructural)
**Persistencia**: ✅ Mock (`MockOfficialStudentData`)
**Propósito**: Representa al alumno después de la alta oficial, antes/después de asignación de grupo.

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | String | UUID interno |
| `preApplicationFolio` | String | Referencia a pre-solicitud |
| `status` | OfficialStudentStatus | Estado actual |
| `gradoIngreso` | Int | Grado al que ingresa |
| `grupoSugerido` | String? | Solo para 2° y 3° |
| `grupoAsignado` | String? | Asignado por Secretaría |
| `curp` | String | CURP del alumno |
| `alumnoNombreCompleto` | String | Nombre completo |
| `matriculaOficial` | String? | Se asigna en alta oficial |
| `validacionSecretaria` | ValidacionArea | ⏳ Placeholder funcional |
| `validacionMedico` | ValidacionArea | ⏳ Placeholder funcional |
| `validacionTrabajoSocial` | ValidacionArea | ⏳ Placeholder funcional |
| `validacionUdeii` | ValidacionArea | ⏳ Placeholder funcional |
| `validacionDireccion` | ValidacionArea | ⏳ Placeholder funcional |

**Estados**: `PROVISIONAL` → `EN_REVISION` → ... → `ALTA_OFICIAL_SIN_GRUPO` → `PENDIENTE_ASIGNACION_GRUPO` → `ALTA_OFICIAL_CON_GRUPO` → `CERRADO`

**Generación de matrícula**: `S310-[10 chars CURP]-G[Grado]`

---

## Modelo 4: `StudentCredentialPreview` (proyección de credencial)

**Archivo**: `composeApp/.../data/StudentCredentialPreview.kt`
**Tipo**: `data class` — Proyección computada
**Persistencia**: No (se computa desde `Student` vía `fromStudent()`)
**Propósito**: Datos necesarios para renderizar frente y reverso de credencial.

| Campo | Tipo | Origen |
|-------|------|--------|
| `enrollmentId` | String | `Student.enrollmentId` |
| `preApplicationFolio` | String? | `Student.preApplicationFolio` |
| `fullName` | String | `Student.fullName` |
| `curp` | String | `Student.curp` |
| `grade` | String | Parseado de `Student.group` (ej: "1°") |
| `group` | String? | Parseado de `Student.group` (ej: "A") |
| `schoolYear` | String | `Student.schoolYear` |
| `status` | String | `Student.status` |
| `photoStatus` | String | "Con foto" / "Sin foto" según `Student.photoUrl` |
| `generatedFromOfficialEnrollment` | Boolean | `true` si `preApplicationFolio != null` |

**No incluye**: domicilio, teléfono, responsables, observaciones, datos médicos, UDEEI, Trabajo Social.

---

## Modelo 5: `Enrollment` (inscripción digital)

**Archivo**: `composeApp/.../data/enrollment/EnrollmentModels.kt`
**Tipo**: `data class` — Real (estructural)
**Persistencia**: ✅ Mock (`MockEnrollmentData`)
**Propósito**: Representa el proceso de inscripción digital.

Incluye: `Address`, `Contact`, `MedicalRecord`, `SocioeconomicRecord`, `EnrollmentDocument`, `Consent`, `RiskFlag`, `IdentityChecklist`, `EnrollmentPresenter`, `AuthorizedPickup`.

---

## Modelos auxiliares

| Modelo | Archivo | Propósito |
|--------|---------|-----------|
| `SaseDocument` | `SaseEntities.kt` | Documento del expediente |
| `SaseObservation` | `SaseEntities.kt` | Observación académica |
| `SaseIncident` | `SaseEntities.kt` | Incidente escolar |
| `SaseAudit` | `SaseEntities.kt` | Auditoría / trazabilidad |
| `ValidacionArea` | `OfficialStudentModels.kt` | Validación por área especializada |
| `FichaMedicaFamiliar` | `PreApplicationModels.kt` | Datos médicos declarativos |
| `ContextoSociofamiliar` | `PreApplicationModels.kt` | Contexto social declarativo |
| `AntecedentesUdeii` | `PreApplicationModels.kt` | Antecedentes de apoyo educativo |
| `ConsentimientosFamiliares` | `PreApplicationModels.kt` | Consentimientos firmados |
| `Responsable` | `PreApplicationModels.kt` | Responsable del alumno |
| `AutorizadoPreSolicitud` | `PreApplicationModels.kt` | Persona autorizada para recoger |

## Relaciones entre modelos

```
PreApplication (1) ───tiene───→ List<Responsable>
PreApplication (1) ───tiene───→ List<AutorizadoPreSolicitud>
PreApplication (1) ───tiene───→ FichaMedicaFamiliar
PreApplication (1) ───tiene───→ ContextoSociofamiliar
PreApplication (1) ───tiene───→ AntecedentesUdeii
PreApplication (1) ───tiene───→ List<DocumentoDeclarado>
PreApplication (1) ───tiene───→ ConsentimientosFamiliares

PreApplication.folio ───────→ OfficialStudent.preApplicationFolio
PreApplication.alumnoCurp ──→ OfficialStudent.curp
PreApplication.folio ───────→ Student.preApplicationFolio

OfficialStudent (1) ───propaga───→ Student (1) [creado en alta oficial]

Student (1) ───computa───→ StudentCredentialPreview (1) [proyección, no persistida]
```

## Nota sobre datos sensibles

El modelo `Student` contiene datos que **NO deben exponerse** en la credencial:

| Dato sensible | En Student | En Credencial |
|--------------|------------|---------------|
| Domicilio | `address` | ❌ No |
| Teléfono | `tutorPhone` | ❌ No |
| Responsable | `tutorName` | ❌ No |
| Alergias | `healthAlergies` | ❌ No |
| Observaciones | `observations` | ❌ No |
| Incidentes | `schoolIncidents` | ❌ No |
| Datos médicos | `healthNotes`, `healthMeds` | ❌ No |
